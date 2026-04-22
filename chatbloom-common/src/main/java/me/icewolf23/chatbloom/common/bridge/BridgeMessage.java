package me.icewolf23.chatbloom.common.bridge;

public record BridgeMessage(
    String sourceType,
    String sourceServer,
    String channelId,
    String senderName,
    String plainText,
    long createdAtMillis
) {
}
