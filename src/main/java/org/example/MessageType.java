package org.example;

public enum MessageType {

    MESSAGE((byte) 1),
    NAME((byte) 2),
    HISTORY((byte) 3);

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