package me.icewolf23.chatbloom.common.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public final class PluginMessageCodec {

    private static final int PROTOCOL_VERSION = 1;

    private PluginMessageCodec() {
    }

    public static byte[] encodeChat(ProxyMessageType type, ChatMessagePacket packet) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(outputStream);
            data.writeInt(PROTOCOL_VERSION);
            data.writeUTF(type.name());
            writeUuid(data, packet.senderId());
            data.writeUTF(packet.senderName());
            data.writeUTF(packet.serverId());
            data.writeUTF(packet.channelId());
            data.writeUTF(packet.plainText());
            data.writeUTF(packet.rankPrefixTemplate() == null ? "" : packet.rankPrefixTemplate());
            data.writeLong(packet.sentAt().toEpochMilli());
            data.flush();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode chat plugin message", exception);
        }
    }

    public static byte[] encodePrivateMessage(ProxyMessageType type, PrivateMessagePacket packet) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(outputStream);
            data.writeInt(PROTOCOL_VERSION);
            data.writeUTF(type.name());
            writeUuid(data, packet.requestId());
            data.writeUTF(packet.sourceServerId());
            writeUuid(data, packet.senderId());
            writeNullableUuid(data, packet.targetId());
            data.writeUTF(packet.senderName());
            data.writeUTF(packet.targetName());
            data.writeUTF(packet.plainText());
            data.writeBoolean(packet.senderBypass());
            data.writeLong(packet.sentAt().toEpochMilli());
            data.flush();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode private-message plugin message", exception);
        }
    }

    public static byte[] encodePrivateMessageResult(PrivateMessageResultPacket packet) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(outputStream);
            data.writeInt(PROTOCOL_VERSION);
            data.writeUTF(ProxyMessageType.PM_RESULT_TO_SOURCE.name());
            writeUuid(data, packet.requestId());
            data.writeUTF(packet.sourceServerId());
            writeUuid(data, packet.senderId());
            writeNullableUuid(data, packet.targetId());
            data.writeUTF(packet.targetName());
            data.writeUTF(packet.plainText());
            data.writeBoolean(packet.delivered());
            data.writeUTF(packet.reasonKey() == null ? "" : packet.reasonKey());
            data.writeLong(packet.sentAt().toEpochMilli());
            data.flush();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to encode private-message result plugin message", exception);
        }
    }

    public static DecodedProxyMessage decode(byte[] payload) {
        try {
            DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload));
            int protocolVersion = data.readInt();
            if (protocolVersion != PROTOCOL_VERSION) {
                throw new IllegalStateException("Unsupported ChatBloom plugin message protocol: " + protocolVersion);
            }
            ProxyMessageType type = ProxyMessageType.valueOf(data.readUTF());
            return switch (type) {
                case NETWORK_CHAT_FORWARD, NETWORK_CHAT_DELIVER -> new DecodedProxyMessage(
                    type,
                    new ChatMessagePacket(
                        readUuid(data),
                        data.readUTF(),
                        data.readUTF(),
                        data.readUTF(),
                        data.readUTF(),
                        data.readUTF(),
                        Instant.ofEpochMilli(data.readLong())
                    ),
                    null,
                    null
                );
                case PM_REQUEST, PM_DELIVER_TO_TARGET -> new DecodedProxyMessage(
                    type,
                    null,
                    new PrivateMessagePacket(
                        readUuid(data),
                        data.readUTF(),
                        readUuid(data),
                        readNullableUuid(data),
                        data.readUTF(),
                        data.readUTF(),
                        data.readUTF(),
                        data.readBoolean(),
                        Instant.ofEpochMilli(data.readLong())
                    ),
                    null
                );
                case PM_RESULT_TO_SOURCE -> new DecodedProxyMessage(
                    type,
                    null,
                    null,
                    new PrivateMessageResultPacket(
                        readUuid(data),
                        data.readUTF(),
                        readUuid(data),
                        readNullableUuid(data),
                        data.readUTF(),
                        data.readUTF(),
                        data.readBoolean(),
                        emptyToNull(data.readUTF()),
                        Instant.ofEpochMilli(data.readLong())
                    )
                );
            };
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to decode ChatBloom plugin message", exception);
        }
    }

    private static void writeUuid(DataOutputStream data, UUID value) throws IOException {
        data.writeLong(value.getMostSignificantBits());
        data.writeLong(value.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream data) throws IOException {
        return new UUID(data.readLong(), data.readLong());
    }

    private static void writeNullableUuid(DataOutputStream data, UUID value) throws IOException {
        data.writeBoolean(value != null);
        if (value != null) {
            writeUuid(data, value);
        }
    }

    private static UUID readNullableUuid(DataInputStream data) throws IOException {
        return data.readBoolean() ? readUuid(data) : null;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
