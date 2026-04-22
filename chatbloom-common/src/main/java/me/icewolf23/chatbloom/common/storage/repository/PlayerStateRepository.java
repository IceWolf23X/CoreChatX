package me.icewolf23.chatbloom.common.storage.repository;

import java.util.UUID;
import me.icewolf23.chatbloom.common.model.PlayerSettingsRecord;

public interface PlayerStateRepository {
    PlayerSettingsRecord load(UUID playerId);

    void save(PlayerSettingsRecord record);
}
