package org.example;

public class P2PChat {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Использование: java chat <имя>");
            System.exit(1);
        }

        String myName = args[0];

        if (myName.length() > 100) {
            System.out.println("Имя не может быть длиннее 100 символов");
            System.exit(1);
        }

        ChatHistory history = new ChatHistory();
        PeerManager peerManager = new PeerManager(myName, history);
        try (TCPListener tcpListener = new TCPListener(peerManager::handleIncoming)) {
            UDPDiscovery discovery = new UDPDiscovery(myName, peerManager, tcpListener.getTcpPort());
            UI ui = new UI(peerManager, history, discovery, tcpListener);

            tcpListener.start();
            discovery.start();

            ui.run();
        }
    }
}
