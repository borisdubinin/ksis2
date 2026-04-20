package org.example;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPDiscovery {

    public static final int UDP_PORT = 5556;
    private final String myName;
    private final PeerManager peerManager;
    private final int tcpListenerPort;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private DatagramSocket receiveSocket;

    public UDPDiscovery(String myName, PeerManager peerManager, int tcpListenerPort) {
        this.myName = myName;
        this.peerManager = peerManager;
        this.tcpListenerPort = tcpListenerPort;
    }

    public void start() {
        Thread receiver = new Thread(this::receiveLoop, "udp-receiver");
        receiver.setDaemon(false);
        receiver.start();
        sendAnnouncement();
    }

    public void stop() {
        running.set(false);
        if (receiveSocket != null && !receiveSocket.isClosed()) {
            receiveSocket.close();
        }
    }

    private void sendAnnouncement() {
        if (!running.get()) return;
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            DatagramPacket packet = createDatagramPacket();
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("UDP announce error: " + e.getMessage());
        }
    }

    private void receiveLoop() {
        try {
            receiveSocket = new DatagramSocket(UDP_PORT);
            receiveSocket.setBroadcast(true);
            byte[] buf = new byte[256];

            while (running.get() && !receiveSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    receiveSocket.receive(packet);
                } catch (SocketException e) {
                    if (!running.get()) break;
                    throw e;
                }
                InetAddress senderAddr = packet.getAddress();
                if (isSelfAddress(senderAddr)) continue;
                int remoteTcpPort;
                String name;
                try(DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(packet.getData(), 0, packet.getLength()))) {
                    remoteTcpPort = dis.readInt();
                    name = dis.readUTF();
                }

                peerManager.initiateNewConnection(senderAddr, remoteTcpPort, name);
            }
        } catch (IOException e) {
            System.err.println("UDP receive error: " + e.getMessage());
        }
    }

    private boolean isSelfAddress(InetAddress addr) {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    if (addrs.nextElement().equals(addr)) return true;
                }
            }
        } catch (SocketException ignored) {}
        return false;
    }

    private DatagramPacket createDatagramPacket() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeInt(tcpListenerPort);
            dos.writeUTF(myName);

            byte[] data = baos.toByteArray();
            return new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), UDP_PORT);
        }
    }
}