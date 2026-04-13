package org.example;

import java.util.Scanner;

public class UI {

    private final PeerManager peerManager;
    private final ChatHistory history;
    private final UDPDiscovery discovery;
    private final TCPListener tcpListener;

    public UI(PeerManager peerManager, ChatHistory history, UDPDiscovery discovery, TCPListener tcpListener) {
        this.peerManager = peerManager;
        this.history = history;
        this.discovery = discovery;
        this.tcpListener = tcpListener;
    }

    public void run() {
        readInputLoop();
    }

    private void readInputLoop() {
        Scanner scanner = new Scanner(System.in);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("/")) {
                handleCommand(line);
            } else {
                sendMessage(line);
            }
        }
    }

    private void sendMessage(String text) {
        peerManager.broadcast(text);
        history.add(Event.messageSent(text));
    }

    private void handleCommand(String line) {
        switch (line) {
            case "/peers" -> printPeers();
            case "/quit" -> quit();
            default -> System.out.println("Неизвестная команда.");
        }
    }

    private void quit() {
        System.out.println("Завершение работы...");
        peerManager.shutdown();          // разослать LEAVE и закрыть TCP-соединения
        discovery.stop();                // остановить UDP приём
        try {
            tcpListener.close();         // закрыть ServerSocket
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
        peers.forEach(p ->System.out.printf("  %s (%s)%n", p.getPeerName(), p.getPeerIp()));
    }
}