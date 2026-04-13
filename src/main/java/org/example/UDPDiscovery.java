package org.example;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPDiscovery {

    public static final int UDP_PORT = 5556;

    private final String myName;
    private final PeerManager peerManager;
    private final int tcpListenerPort;
    private final ChatHistory chatHistory;

    public UDPDiscovery(String myName, PeerManager peerManager, int tcpListenerPort, ChatHistory chatHistory) {
        this.myName = myName;
        this.peerManager = peerManager;
        this.tcpListenerPort = tcpListenerPort;
        this.chatHistory = chatHistory;
    }

    public void start() {
        Thread receiver = new Thread(this::receiveLoop, "udp-receiver");
        receiver.setDaemon(true);
        receiver.start();

        sendAnnounce();
    }

    // Отправляем broadcast со своим именем
    private void sendAnnounce() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            DatagramPacket packet = createDatagramPacket();
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("UDP announce error: " + e.getMessage());
        }
    }

    // Слушаем чужие объявления и подключаемся к ним по TCP.
    private void receiveLoop() {
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            socket.setBroadcast(true);
            byte[] buf = new byte[256];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                InetAddress senderAddr = packet.getAddress();
                int tcpListenerPort;
                String name;
                try(DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(packet.getData(), 0, packet.getLength()))) {
                    tcpListenerPort = dis.readInt();
                    name = dis.readUTF();
                }

                peerManager.connectTo(senderAddr, tcpListenerPort, name);
            }
        } catch (IOException e) {
            System.err.println("UDP receive error: " + e.getMessage());
        }
    }

    private DatagramPacket createDatagramPacket() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(tcpListenerPort);
            dos.writeUTF(myName);

            byte[] data = baos.toByteArray();
            return new DatagramPacket(data, data.length, InetAddress.getByName("192.168.10.255"), UDP_PORT);
        }
    }
}