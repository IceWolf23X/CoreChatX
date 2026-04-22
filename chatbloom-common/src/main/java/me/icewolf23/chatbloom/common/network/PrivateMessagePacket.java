package me.icewolf23.chatbloom.common.network;

import java.time.Instant;
import java.util.UUID;

public record PrivateMessagePacket(
    UUID senderId,
    UUID targetId,
    String senderName,
    String targetName,
    String plainText,
    Instant sentAt
) {
}
