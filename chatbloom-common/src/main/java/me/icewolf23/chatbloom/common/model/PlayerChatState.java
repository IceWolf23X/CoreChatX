package me.icewolf23.chatbloom.common.model;

import java.util.UUID;

public record PlayerChatState(
    UUID playerId,
    String activeChannelId,
    boolean pingSoundEnabled,
    boolean pingActionbarEnabled,
    boolean privateMessagesEnabled,
    boolean mentionNotificationsEnabled,
    boolean staffChatEnabled,
    String localeTag
) {
}
