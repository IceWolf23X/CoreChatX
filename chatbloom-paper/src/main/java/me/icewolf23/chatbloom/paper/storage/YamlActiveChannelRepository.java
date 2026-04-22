package me.icewolf23.chatbloom.paper.storage;

import me.icewolf23.chatbloom.common.storage.repository.ActiveChannelRepository;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class YamlActiveChannelRepository implements ActiveChannelRepository {

    private final File file;
    private final YamlConfiguration yaml;

    public YamlActiveChannelRepository(File file) {
        this.file = file;
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public String loadActiveChannel(UUID playerId) {
        return yaml.getString("players." + playerId + ".active-channel", "");
    }

    @Override
    public void saveActiveChannel(UUID playerId, String channelId) {
        yaml.set("players." + playerId + ".active-channel", channelId);
        saveNow();
    }

    private void saveNow() {
        try {
            ensureParentDirectory();
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save active channel data to " + file.getAbsolutePath(), exception);
        }
    }

    private void ensureParentDirectory() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create directory " + parent.getAbsolutePath());
        }
    }
}
