package org.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record Event(Type type, String name, String ip, String text, LocalTime time) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public enum Type {PEER_JOINED, PEER_LEFT, MESSAGE_RECEIVED, MESSAGE_SENT}

    public static Event peerJoined(String name, String ip) {
        return new Event(Type.PEER_JOINED, name, ip, null, LocalTime.now());
    }

    public static Event peerLeft(String name, String ip) {
        return new Event(Type.PEER_LEFT, name, ip, null, LocalTime.now());
    }

    public static Event messageReceived(String name, String ip, String text) {
        return new Event(Type.MESSAGE_RECEIVED, name, ip, text, LocalTime.now());
    }

    public static Event messageSent(String text) {
        return new Event(Type.MESSAGE_SENT, null, null, text, LocalTime.now());
    }

    public String format() {
        String ts = time.format(FMT);
        return switch (type) {
            case PEER_JOINED -> "[%s] + %s (%s) подключился".formatted(ts, name, ip);
            case PEER_LEFT -> "[%s] - %s (%s) отключился".formatted(ts, name, ip);
            case MESSAGE_RECEIVED -> "[%s] %s (%s): %s".formatted(ts, name, ip, text);
            case MESSAGE_SENT -> "[%s] я: %s".formatted(ts, text);
        };
    }
}