package me.icewolf23.chatbloom.common.moderation;

import java.time.Instant;
import java.util.UUID;

public record MuteRecord(
    UUID playerId,
    Instant expiresAt,
    String reason,
    UUID actorId
) {
    public boolean isActive(Instant now) {
        return expiresAt == null || expiresAt.isAfter(now);
    }
}
