package me.icewolf23.chatbloom.common.model;

import java.util.UUID;

public record PlayerSettingsRecord(
    UUID playerId,
    boolean pingSoundEnabled,
    boolean pingActionbarEnabled,
    boolean socialSpyEnabled,
    boolean pmEnabled
) {
}
