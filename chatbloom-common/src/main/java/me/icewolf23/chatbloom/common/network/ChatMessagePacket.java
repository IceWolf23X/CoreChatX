package me.icewolf23.chatbloom.common.network;

import java.time.Instant;
import java.util.UUID;

public record ChatMessagePacket(
    UUID senderId,
    String senderName,
    String serverId,
    String channelId,
    String plainText,
    Instant sentAt
) {
}
