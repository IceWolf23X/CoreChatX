package me.icewolf23.chatbloom.common.network;

import java.time.Instant;
import java.util.UUID;

public record PrivateMessageResultPacket(
    UUID requestId,
    String sourceServerId,
    UUID senderId,
    UUID targetId,
    String targetName,
    String plainText,
    boolean delivered,
    String reasonKey,
    Instant sentAt
) {
}
