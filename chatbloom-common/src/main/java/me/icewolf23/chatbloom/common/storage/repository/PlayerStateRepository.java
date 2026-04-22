package me.icewolf23.chatbloom.common.storage.repository;

import java.util.UUID;
import me.icewolf23.chatbloom.common.model.PlayerSettingsRecord;

public interface PlayerStateRepository {
    PlayerSettingsRecord load(UUID playerId);

    void save(PlayerSettingsRecord record);

    default java.util.Optional<PlayerSettingsRecord> find(UUID playerId) {
        return java.util.Optional.ofNullable(load(playerId));
    }

    default PlayerSettingsRecord loadOrCreate(UUID playerId) {
        return load(playerId);
    }
}
