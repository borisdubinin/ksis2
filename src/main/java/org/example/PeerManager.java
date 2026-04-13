package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
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
    private volatile boolean stopping = false;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalTime.class, (com.google.gson.JsonSerializer<LocalTime>)
                    (src, typeOfSrc, context) -> context.serialize(src.toString()))
            .registerTypeAdapter(LocalTime.class, (com.google.gson.JsonDeserializer<LocalTime>)
                    (json, typeOfT, context) -> LocalTime.parse(json.getAsString()))
            .create();
    private static final Type EVENT_LIST_TYPE = new TypeToken<List<Event>>() {
    }.getType();

    public PeerManager(String myName, ChatHistory chatHistory) {
        this.myName = myName;
        this.chatHistory = chatHistory;
    }

    public void shutdown() {
        stopping = true;
        for (TCPConnection conn : peers.values()) {
            conn.send(new Message(MessageType.LEAVE, ""));
            conn.close();
        }
        peers.clear();
    }

    public void handleIncoming(Socket socket) {
        if (stopping) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            return;
        }
        try {
            TCPConnection conn = new TCPConnection(socket, this::onMessage, this::onDisconnect);
            String ip = conn.getPeerIp();

            TCPConnection existing = peers.putIfAbsent(ip, conn);
            if (existing != null) {
                conn.close();
                return;
            }

            // представляемся первыми — входящая сторона тоже должна знать наше имя
            conn.send(new Message(MessageType.HELLO, myName));

        } catch (IOException e) {
            System.err.println("Failed to accept connection: " + e.getMessage());
        }
    }

    public void connectTo(InetAddress address, int port, String name) {
        if (stopping) return;
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
            TCPConnection existing = peers.putIfAbsent(ip, conn);
            if (existing != null) {
                conn.close();
                return;
            }
            conn.send(new Message(MessageType.HELLO, myName));
            sendFullHistory(conn);

        } catch (IOException e) {
            System.err.println("Could not connect to " + ip + ": " + e.getMessage());
        }
    }

    private void sendFullHistory(TCPConnection conn) {
        List<Event> history = chatHistory.getHistory();
        String json = gson.toJson(history, EVENT_LIST_TYPE);
        conn.send(new Message(MessageType.HISTORY, json));
    }

    public void broadcast(String text) {
        if (stopping) return;
        Message msg = new Message(MessageType.CHAT, text);
        for (TCPConnection conn : peers.values()) {
            conn.send(msg);
        }
    }

    public Collection<TCPConnection> getPeers() {
        return Collections.unmodifiableCollection(peers.values());
    }

    private void onMessage(TCPConnection conn, Message msg) {
        if (stopping) return;
        switch (msg.type()) {
            case HELLO -> {
                conn.setPeerName(msg.body());
                chatHistory.add(Event.peerJoined(conn.getPeerName(), conn.getPeerIp()));
            }
            case CHAT -> chatHistory.add((Event.messageReceived(conn.getPeerName(), conn.getPeerIp(), msg.body())));
            case HISTORY -> {
                if (historyReceived.compareAndSet(false, true)) {
                    List<Event> history = gson.fromJson(msg.body(), EVENT_LIST_TYPE);
                    this.chatHistory.setHistory(history);
                }
            }
            case LEAVE -> conn.close();
        }
    }

    private void onDisconnect(TCPConnection conn) {
        peers.remove(conn.getPeerIp());
        if (!stopping) {
            chatHistory.add(Event.peerLeft(conn.getPeerName(), conn.getPeerIp()));
        }
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
}