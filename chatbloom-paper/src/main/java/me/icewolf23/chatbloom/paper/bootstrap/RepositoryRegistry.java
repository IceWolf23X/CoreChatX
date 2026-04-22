package me.icewolf23.chatbloom.paper.bootstrap;
import icewolf23x.chatBloom.data.GlobalStateStore;
import icewolf23x.chatBloom.data.PlayerDataStore;
import me.icewolf23.chatbloom.common.storage.repository.ActiveChannelRepository;
import me.icewolf23.chatbloom.common.storage.repository.GlobalStateRepository;
import me.icewolf23.chatbloom.common.storage.repository.IgnoreRepository;
import me.icewolf23.chatbloom.common.storage.repository.MuteRepository;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;
import me.icewolf23.chatbloom.paper.storage.YamlActiveChannelRepository;
import me.icewolf23.chatbloom.paper.storage.YamlGlobalStateRepository;
import me.icewolf23.chatbloom.paper.storage.YamlIgnoreRepository;
import me.icewolf23.chatbloom.paper.storage.YamlMuteRepository;
import me.icewolf23.chatbloom.paper.storage.YamlPlayerStateRepository;

import java.io.File;

public final class RepositoryRegistry {

    private final ChatBloomPaperPlugin plugin;
    private PlayerDataStore playerDataStore;
    private GlobalStateStore globalStateStore;
    private PlayerStateRepository playerStateRepository;
    private GlobalStateRepository globalStateRepository;
    private IgnoreRepository ignoreRepository;
    private MuteRepository muteRepository;
    private ActiveChannelRepository activeChannelRepository;

    public RepositoryRegistry(ChatBloomPaperPlugin plugin, ConfigRegistry configRegistry) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.playerDataStore = new PlayerDataStore(plugin);
        this.globalStateStore = new GlobalStateStore(plugin);
        this.playerStateRepository = new YamlPlayerStateRepository(playerDataStore);
        this.globalStateRepository = new YamlGlobalStateRepository(globalStateStore);
        this.ignoreRepository = new YamlIgnoreRepository(new File(plugin.getDataFolder(), "ignoredata.yml"));
        this.muteRepository = new YamlMuteRepository(new File(plugin.getDataFolder(), "mutedata.yml"));
        this.activeChannelRepository = new YamlActiveChannelRepository(new File(plugin.getDataFolder(), "channeldata.yml"));
    }

    public void reload() {
        initialize();
    }

    public void shutdown() {
        if (playerDataStore != null) {
            playerDataStore.save();
        }
        if (globalStateStore != null) {
            globalStateStore.save();
        }
    }

    public PlayerDataStore playerDataStore() {
        return playerDataStore;
    }

    public GlobalStateStore globalStateStore() {
        return globalStateStore;
    }

    public PlayerStateRepository playerStateRepository() {
        return playerStateRepository;
    }

    public GlobalStateRepository globalStateRepository() {
        return globalStateRepository;
    }

    public IgnoreRepository ignoreRepository() {
        return ignoreRepository;
    }

    public MuteRepository muteRepository() {
        return muteRepository;
    }

    public ActiveChannelRepository activeChannelRepository() {
        return activeChannelRepository;
    }
}
