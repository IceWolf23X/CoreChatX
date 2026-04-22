package me.icewolf23.chatbloom.paper.storage;

import me.icewolf23.chatbloom.common.storage.repository.IgnoreRepository;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class YamlIgnoreRepository implements IgnoreRepository {

    private final File file;
    private final YamlConfiguration yaml;

    public YamlIgnoreRepository(File file) {
        this.file = file;
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public Set<UUID> loadIgnored(UUID playerId) {
        Set<UUID> ignored = new HashSet<>();
        for (String raw : yaml.getStringList("players." + playerId)) {
            try {
                ignored.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignoredException) {
            }
        }
        return ignored;
    }

    @Override
    public void saveIgnored(UUID playerId, Set<UUID> ignored) {
        yaml.set("players." + playerId, ignored.stream().map(UUID::toString).toList());
        saveNow();
    }

    private void saveNow() {
        try {
            ensureParentDirectory();
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save ignore data to " + file.getAbsolutePath(), exception);
        }
    }

    private void ensureParentDirectory() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create directory " + parent.getAbsolutePath());
        }
    }
}
