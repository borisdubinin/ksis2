package org.example;

import java.util.Scanner;

public class UI {

    private final PeerManager peerManager;
    private final ChatHistory history;

    public UI(PeerManager peerManager, ChatHistory history) {
        this.peerManager = peerManager;
        this.history     = history;
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
        Event sent = Event.messageSent(text);
        history.add(sent);
    }

    private void handleCommand(String line) {
        switch (line) {
            case "/peers" -> printPeers();
            case "/quit" -> {
                System.out.println("Выход.");
                System.exit(0);
            }
            default -> System.out.println("Неизвестная команда.");
        }
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