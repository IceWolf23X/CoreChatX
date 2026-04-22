package me.icewolf23.chatbloom.paper.storage;

import icewolf23x.chatBloom.data.PlayerDataStore;
import icewolf23x.chatBloom.data.PlayerSettings;
import me.icewolf23.chatbloom.common.model.PlayerSettingsRecord;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;

import java.util.UUID;

public final class YamlPlayerStateRepository implements PlayerStateRepository {

    private final PlayerDataStore playerDataStore;

    public YamlPlayerStateRepository(PlayerDataStore playerDataStore) {
        this.playerDataStore = playerDataStore;
    }

    @Override
    public PlayerSettingsRecord load(UUID playerId) {
        PlayerSettings settings = playerDataStore.get(playerId);
        return new PlayerSettingsRecord(
            playerId,
            settings.isPingSoundEnabled(),
            settings.isPingActionbarEnabled(),
            settings.isSocialSpyEnabled(),
            settings.isPmEnabled()
        );
    }

    @Override
    public void save(PlayerSettingsRecord record) {
        PlayerSettings settings = playerDataStore.get(record.playerId());
        settings.setPingSoundEnabled(record.pingSoundEnabled());
        settings.setPingActionbarEnabled(record.pingActionbarEnabled());
        settings.setSocialSpyEnabled(record.socialSpyEnabled());
        settings.setPmEnabled(record.pmEnabled());
        playerDataStore.save(record.playerId());
    }
}
