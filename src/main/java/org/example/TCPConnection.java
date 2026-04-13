package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TCPConnection {

    private final Socket socket;
    private final String peerIp;

    private volatile String peerName = null; // приходит в первом HELLO-пакете

    private final BiConsumer<TCPConnection, Message> onMessage;

    private final Consumer<TCPConnection> onDisconnect;

    public TCPConnection(Socket socket,
                         BiConsumer<TCPConnection, Message> onMessage,
                         Consumer<TCPConnection> onDisconnect) throws IOException {
        this.socket = socket;
        this.peerIp = ((InetSocketAddress) socket.getRemoteSocketAddress())
                .getAddress().getHostAddress();
        this.onMessage = onMessage;
        this.onDisconnect = onDisconnect;

        Thread reader = new Thread(this::readLoop, "reader-" + peerIp);
        reader.setDaemon(true);
        reader.start();
    }

    public synchronized void send(Message msg) {
        if (socket.isClosed()) return;
        try {
            Protocol.write(socket.getOutputStream(), msg);
        } catch (IOException e) {
            close();
        }
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {

        }
    }

    public String getPeerIp() {
        return peerIp;
    }

    public String getPeerName() {
        return peerName;
    }

    public void setPeerName(String name) {
        this.peerName = name;
    }

    private void readLoop() {
        try (InputStream in = socket.getInputStream()) {
            while (!socket.isClosed()) {
                Message msg = Protocol.read(in);
                if (msg == null) break;
                onMessage.accept(this, msg);
            }
        } catch (IOException ignored) {
        } finally {
            close();
            onDisconnect.accept(this);
        }
    }
}