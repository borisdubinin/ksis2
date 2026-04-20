package org.example;

import java.net.UnknownHostException;
import java.util.Scanner;

public class UI {

    private final PeerManager peerManager;
    private final UDPDiscovery udpDiscovery;
    private final TCPListener tcpListener;

    public UI(PeerManager peerManager, UDPDiscovery udpDiscovery, TCPListener tcpListener) {
        this.peerManager = peerManager;
        this.udpDiscovery = udpDiscovery;
        this.tcpListener = tcpListener;
    }

    public void run() throws UnknownHostException {
        readInputLoop();
    }

    private void readInputLoop() throws UnknownHostException {
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("/")) {
                handleCommand(line);
            } else {
                peerManager.sendMessageToAll(line);
            }
        }
    }

    private void handleCommand(String line) {
        switch (line) {
            case "/peers" -> printPeers();
            case "/quit" -> quit();
            case "/history" -> printHistory();
            default -> System.out.println("Неизвестная команда.");
        }
    }

    private void quit() {
        System.out.println("Завершение работы...");
        peerManager.closeAllConnections();
        udpDiscovery.stop();
        try {
            tcpListener.close();
        } catch (Exception e) {
            System.err.println("Ошибка при закрытии TCPListener: " + e.getMessage());
        }
        System.out.println("Выход.");
        System.exit(0);
    }

    private void printPeers() {
        var peers = peerManager.getPeers();
        if (peers.isEmpty()) {
            System.out.println("Нет активных подключений.");
            return;
        }
        peers.forEach(p ->System.out.printf("  %s (%s)%n", p.getPeerName(), p.getPeerIP()));
    }

    private void printHistory() {
        for(Event event : ChatHistory.getEvents()) {
            System.out.println(event.format());
        }
    }
}