package me.icewolf23.chatbloom.common.moderation;

import java.util.UUID;

public record MuteRecord(
    UUID playerId,
    long createdAtMillis,
    Long expiresAtMillis,
    String reason,
    UUID actorId,
    boolean blocksPrivateMessages
) {
    public boolean isActive(long now) {
        return expiresAtMillis == null || expiresAtMillis > now;
    }
}
