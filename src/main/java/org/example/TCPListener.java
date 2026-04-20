package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPListener {

    private final PeerManager peerManager;
    private final ServerSocket serverSocket;

    public TCPListener(PeerManager peerManager) {
        try {
            this.peerManager = peerManager;
            serverSocket = new ServerSocket(0);
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            throw new RuntimeException("TCPServer initializing error: " + e.getMessage(), e);
        }
    }

    public void start() {
        Thread t = new Thread(this::listen, "tcp-server");
        t.setDaemon(true);
        t.start();
    }

    public int getTcpPort() {
        return serverSocket.getLocalPort();
    }

    private void listen() {
        try {
            while (true) {
                Socket client = serverSocket.accept();

                Thread handler = new Thread(
                        () -> peerManager.acceptNewConnection(client),
                        "conn-handler-" + client.getInetAddress().getHostAddress()
                );
                handler.setDaemon(true);
                handler.start();
            }
        } catch (IOException e) {

            System.err.println("TCPServer error: " + e.getMessage());
        }
    }

    public void close() throws Exception {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
    }
}