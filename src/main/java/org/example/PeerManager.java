package org.example;

import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerManager {

    // ключ — IP-адрес, значение — соединение с этим узлом
    private final ConcurrentHashMap<String, TCPConnection> peers = new ConcurrentHashMap<>();

    private final String myName;
    private final ChatHistory chatHistory;
    private final AtomicBoolean historyReceived = new AtomicBoolean(false);

    public PeerManager(String myName, ChatHistory chatHistory) {
        this.myName = myName;
        this.chatHistory = chatHistory;
    }

    public void handleIncoming(Socket socket) {
        try {
            TCPConnection conn = new TCPConnection(socket, this::onMessage, this::onDisconnect);
            String ip = conn.getPeerIp();

            TCPConnection existing = peers.putIfAbsent(ip, conn);
            if (existing != null) { conn.close(); return; }

            // представляемся первыми — входящая сторона тоже должна знать наше имя
            conn.send(new Message(MessageType.HELLO, myName));

        } catch (IOException e) {
            System.err.println("Failed to accept connection: " + e.getMessage());
        }
    }

    public void connectTo(InetAddress address, int port, String name) {
        String ip = address.getHostAddress();

        if (peers.containsKey(ip)) return; // уже подключены

        // Защита от подключения к самому себе
        try {
            if (isSelf(address)) return;
        } catch (SocketException e) {
            System.err.println("Could not check self address: " + e.getMessage());
            return;
        }

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 3000);
            TCPConnection conn = new TCPConnection(socket, this::onMessage, this::onDisconnect);
            conn.setPeerName(name);

            peers.put(ip, conn);

            conn.send(new Message(MessageType.HELLO, myName));
            sendFullHistory(conn);

        } catch (IOException e) {
            System.err.println("Could not connect to " + ip + ": " + e.getMessage());
        }
    }

    private void sendFullHistory(TCPConnection conn) {
        List<Event> history = chatHistory.getHistory();
        String historyData = serializeHistory(history);
        conn.send(new Message(MessageType.HISTORY, historyData));
    }

    public void broadcast(String text) {
        Message msg = new Message(MessageType.CHAT, text);
        for (TCPConnection conn : peers.values()) {
            conn.send(msg);
        }
    }

    public Collection<TCPConnection> getPeers() {
        return Collections.unmodifiableCollection(peers.values());
    }

    private void onMessage(TCPConnection conn, Message msg) {
        switch (msg.type()) {
            case HELLO -> {
                conn.setPeerName(msg.body());
                chatHistory.add(Event.peerJoined(conn.getPeerName(), conn.getPeerIp()));
            }
            case CHAT -> {
                chatHistory.add((Event.messageReceived(conn.getPeerName(), conn.getPeerIp(), msg.body())));
            }
            case HISTORY -> {
                if (historyReceived.compareAndSet(false, true)) {
                    List<Event> history = deserializeHistory(msg.body());
                    this.chatHistory.setHistory(history);
                }
            }
        }
    }

    private void onDisconnect(TCPConnection conn) {
        peers.remove(conn.getPeerIp());
        chatHistory.add(Event.peerLeft(conn.getPeerName(), conn.getPeerIp()));
    }

    // Проверяет, является ли адрес одним из наших локальных адресов.
    private boolean isSelf(InetAddress address) throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> addrs = iface.getInetAddresses();
            while (addrs.hasMoreElements()) {
                if (addrs.nextElement().equals(address)) return true;
            }
        }
        return false;
    }

    private String serializeHistory(List<Event> events) {
        StringBuilder sb = new StringBuilder();
        for (Event e : events) {
            sb.append(e.type()).append('|');
            sb.append(e.name() == null ? "" : e.name()).append('|');
            sb.append(e.ip() == null ? "" : e.ip()).append('|');
            sb.append(e.text() == null ? "" : e.text().replace("\n", "\\n")).append('|');
            sb.append(e.time().toString()).append('\n');
        }
        return sb.toString();
    }

    private List<Event> deserializeHistory(String data) {
        List<Event> events = new ArrayList<>();
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\|", 5);
            if (parts.length < 5) continue;
            Event.Type type = Event.Type.valueOf(parts[0]);
            String name = parts[1].isEmpty() ? null : parts[1];
            String ip = parts[2].isEmpty() ? null : parts[2];
            String text = parts[3].isEmpty() ? null : parts[3].replace("\\n", "\n");
            LocalTime time = LocalTime.parse(parts[4]);
            events.add(new Event(type, name, ip, text, time));
        }
        return events;
    }
}