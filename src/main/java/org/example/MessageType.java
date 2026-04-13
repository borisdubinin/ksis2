package org.example;

public enum MessageType {

    CHAT((byte) 1),
    HELLO((byte) 2),
    HISTORY((byte) 3),
    LEAVE((byte) 4);

    public final byte code;

    MessageType(byte code) {
        this.code = code;
    }

    public static MessageType fromCode(byte code) {
        for (MessageType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown message type: " + code);
    }
}