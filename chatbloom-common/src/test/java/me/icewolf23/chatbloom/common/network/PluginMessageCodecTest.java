package me.icewolf23.chatbloom.common.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PluginMessageCodecTest {

    private static final UUID SENDER_ID = UUID.fromString("0c98c24e-c2a1-41dc-bd46-5c7fda1ca809");
    private static final UUID TARGET_ID = UUID.fromString("60ae7640-d415-4f38-8264-1ff32ba7da73");
    private static final UUID REQUEST_ID = UUID.fromString("df3fcb7e-e2ab-4805-b3c2-04456c8db51b");
    private static final Instant SENT_AT = Instant.parse("2026-05-08T10:15:30Z");

    @Test
    void chatMessageRoundTripPreservesPayload() {
        ChatMessagePacket packet = new ChatMessagePacket(
            SENDER_ID,
            "IceWolf23",
            "survival-1",
            "global",
            "Hello network",
            "<gray>[Admin]</gray>",
            SENT_AT
        );

        DecodedProxyMessage decoded = PluginMessageCodec.decode(
            PluginMessageCodec.encodeChat(ProxyMessageType.NETWORK_CHAT_FORWARD, packet)
        );

        assertEquals(ProxyMessageType.NETWORK_CHAT_FORWARD, decoded.type());
        assertEquals(packet, decoded.chatMessagePacket());
        assertNull(decoded.privateMessagePacket());
        assertNull(decoded.privateMessageResultPacket());
    }

    @Test
    void chatMessageConvertsNullRankPrefixToEmptyString() {
        ChatMessagePacket packet = new ChatMessagePacket(
            SENDER_ID,
            "IceWolf23",
            "survival-1",
            "global",
            "Hello network",
            null,
            SENT_AT
        );

        ChatMessagePacket decoded = PluginMessageCodec.decode(
            PluginMessageCodec.encodeChat(ProxyMessageType.NETWORK_CHAT_DELIVER, packet)
        ).chatMessagePacket();

        assertEquals("", decoded.rankPrefixTemplate());
    }

    @Test
    void privateMessageRoundTripPreservesNullableTarget() {
        PrivateMessagePacket packet = new PrivateMessagePacket(
            REQUEST_ID,
            "survival-1",
            SENDER_ID,
            null,
            "IceWolf23",
            "TargetPlayer",
            "Secret hello",
            true,
            SENT_AT
        );

        DecodedProxyMessage decoded = PluginMessageCodec.decode(
            PluginMessageCodec.encodePrivateMessage(ProxyMessageType.PM_REQUEST, packet)
        );

        assertEquals(ProxyMessageType.PM_REQUEST, decoded.type());
        assertEquals(packet, decoded.privateMessagePacket());
        assertNull(decoded.chatMessagePacket());
        assertNull(decoded.privateMessageResultPacket());
    }

    @Test
    void privateMessageResultRoundTripPreservesDeliveryFailureReason() {
        PrivateMessageResultPacket packet = new PrivateMessageResultPacket(
            REQUEST_ID,
            "survival-1",
            SENDER_ID,
            TARGET_ID,
            "TargetPlayer",
            "Secret hello",
            false,
            "pm.target.offline",
            SENT_AT
        );

        DecodedProxyMessage decoded = PluginMessageCodec.decode(
            PluginMessageCodec.encodePrivateMessageResult(packet)
        );

        assertEquals(ProxyMessageType.PM_RESULT_TO_SOURCE, decoded.type());
        assertEquals(packet, decoded.privateMessageResultPacket());
        assertFalse(decoded.privateMessageResultPacket().delivered());
        assertNull(decoded.chatMessagePacket());
        assertNull(decoded.privateMessagePacket());
    }

    @Test
    void privateMessageResultConvertsEmptyReasonToNull() {
        PrivateMessageResultPacket packet = new PrivateMessageResultPacket(
            REQUEST_ID,
            "survival-1",
            SENDER_ID,
            TARGET_ID,
            "TargetPlayer",
            "Secret hello",
            true,
            null,
            SENT_AT
        );

        PrivateMessageResultPacket decoded = PluginMessageCodec.decode(
            PluginMessageCodec.encodePrivateMessageResult(packet)
        ).privateMessageResultPacket();

        assertTrue(decoded.delivered());
        assertNull(decoded.reasonKey());
    }

    @Test
    void decodeRejectsUnsupportedProtocolVersion() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream data = new DataOutputStream(outputStream);
        data.writeInt(2);
        data.writeUTF(ProxyMessageType.NETWORK_CHAT_FORWARD.name());
        data.flush();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> PluginMessageCodec.decode(outputStream.toByteArray())
        );

        assertEquals("Unsupported ChatBloom plugin message protocol: 2", exception.getMessage());
    }
}
