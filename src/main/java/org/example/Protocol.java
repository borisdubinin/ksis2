package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Protocol {

    public static void write(OutputStream out, Message msg) throws IOException {
        byte[] body = msg.body().getBytes(StandardCharsets.UTF_8);
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeByte(msg.type().code);   // 1 байт: тип
        dos.writeInt(body.length);        // 4 байта: длина
        dos.write(body);                  // N байт: тело
        dos.flush();
    }

    public static Message read(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);

        int typeByte;
        try {
            typeByte = dis.readUnsignedByte();
        } catch (EOFException e) {
            return null;
        }

        MessageType type = MessageType.fromCode((byte) typeByte);
        int length = dis.readInt();

        if (length < 0 || length > 1024 * 1024) { // защита: максимум 1 МБ
            throw new IOException("Invalid message length: " + length);
        }

        byte[] body = dis.readNBytes(length);
        if (body.length < length) {
            return null;
        }

        return new Message(type, new String(body, StandardCharsets.UTF_8));
    }
}