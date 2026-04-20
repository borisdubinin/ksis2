package org.example;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PeerManager {

    private final ConcurrentLinkedQueue<TCPConnection> peers = new ConcurrentLinkedQueue<>();
    private final String myName;
    private static final AtomicBoolean historyReceived = new AtomicBoolean(false);

    public PeerManager(String myName) {
        this.myName = myName;
    }

    public Collection<TCPConnection> getPeers() {
        peers.removeIf(TCPConnection::isClosed);
        return Collections.unmodifiableCollection(peers);
    }

    public static AtomicBoolean getHistoryReceived() {
        return historyReceived;
    }

    public static void setHistoryReceived(boolean isHistoryReceived) {
        historyReceived.set(isHistoryReceived);
    }

    public void acceptNewConnection(Socket socket) {
        try {
            TCPConnection conn = new TCPConnection(socket);
            peers.add(conn);
        } catch (IOException e) {
            System.err.println("Failed to accept connection: " + e.getMessage());
        }
    }

    public void initiateNewConnection(InetAddress address, int port, String name) {
        boolean alreadyConnected = peers.stream()
                .filter(c -> !c.isClosed())
                .anyMatch(c -> c.getPeerIP().equals(address.getHostAddress()));
        if (alreadyConnected) return;
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 3000);
            TCPConnection conn = new TCPConnection(socket);
            conn.setPeerName(name);
            conn.send(new Message(MessageType.NAME, myName));
            conn.sendFullHistory();
            peers.add(conn);
            ChatHistory.add(Event.peerJoined(name, address.getHostAddress()));
        } catch (IOException e) {
            System.err.println("Could not connect to " + address.getHostAddress() + ": " + e.getMessage());
        }
    }

    public void sendMessageToAll(String text) throws UnknownHostException {
        Message message = new Message(MessageType.MESSAGE, text);
        ChatHistory.add(Event.message(myName, InetAddress.getLocalHost().getHostAddress(), text));
        for (TCPConnection conn : peers) {
            conn.send(message);
        }
    }

    public void closeAllConnections() {
        for(TCPConnection connection : peers) {
            connection.close(false);
        }
        peers.clear();
    }
}