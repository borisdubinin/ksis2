package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.Socket;
import java.time.LocalTime;
import java.util.List;

public class TCPConnection {

    private final Socket socket;
    private volatile String peerName = null;
    private volatile boolean isClosing = false;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalTime.class, (com.google.gson.JsonSerializer<LocalTime>)
                    (src, typeOfSrc, context) -> context.serialize(src.toString()))
            .registerTypeAdapter(LocalTime.class, (com.google.gson.JsonDeserializer<LocalTime>)
                    (json, typeOfT, context) -> LocalTime.parse(json.getAsString()))
            .create();
    private static final Type EVENT_LIST_TYPE = new TypeToken<List<Event>>() {
    }.getType();

    public TCPConnection(Socket socket) throws IOException {
        this.socket = socket;

        Thread reader = new Thread(this::readLoop, "reader-" + getPeerIP());
        reader.setDaemon(true);
        reader.start();
    }

    public boolean isClosed() {
        return isClosing;
    }

    public synchronized void send(Message message) {
        if (socket.isClosed()) return;
        try {
            Protocol.write(socket.getOutputStream(), message);
        } catch (IOException e) {
            System.err.println("Failed to send message to " + getPeerIP() + ": " + e.getMessage());
        }
    }

    public void sendFullHistory() {
        List<Event> history = ChatHistory.getEvents();
        String json = gson.toJson(history, EVENT_LIST_TYPE);
        send(new Message(MessageType.HISTORY, json));
    }

    public void close(boolean shouldAddPeerLeftEvent) {
        if (isClosing) return;
        isClosing = true;

        try {
            if (shouldAddPeerLeftEvent) ChatHistory.add(Event.peerLeft(getPeerName(), getPeerIP()));
            if (!socket.isClosed()) {
                socket.shutdownOutput();
                socket.close();
            }
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public String getPeerIP() {
        return socket.getInetAddress().getHostAddress();
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
                Message message = Protocol.read(in);
                if (message == null) break;
                acceptMessage(message);
            }
        } catch (IOException ignored) {
        } finally {
            close(true);
        }
    }

    private void acceptMessage(Message message) {
        switch (message.type()) {
            case NAME -> {
                setPeerName(message.body());
            }
            case MESSAGE -> ChatHistory.add((Event.message(getPeerName(), getPeerIP(), message.body())));
            case HISTORY -> {
                synchronized (PeerManager.getHistoryReceived()) {
                    if (!PeerManager.getHistoryReceived().get()) {
                        PeerManager.setHistoryReceived(true);
                        List<Event> history = gson.fromJson(message.body(), EVENT_LIST_TYPE);
                        ChatHistory.addEventsFromHistory(history);
                    }
                }
            }
        }
    }
}