package me.icewolf23.chatbloom.paper.storage;

import me.icewolf23.chatbloom.common.moderation.MuteRecord;
import me.icewolf23.chatbloom.common.storage.repository.MuteRepository;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public final class YamlMuteRepository implements MuteRepository {

    private final File file;
    private final YamlConfiguration yaml;

    public YamlMuteRepository(File file) {
        this.file = file;
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public Optional<MuteRecord> findActiveMute(UUID playerId, long now) {
        String path = "players." + playerId;
        if (!yaml.contains(path)) {
            return Optional.empty();
        }
        Long until = yaml.contains(path + ".until") ? yaml.getLong(path + ".until") : null;
        MuteRecord record = new MuteRecord(
            playerId,
            yaml.getLong(path + ".created-at", 0L),
            until == null || until <= 0L ? null : until,
            yaml.getString(path + ".reason", "No reason provided"),
            parseUuid(yaml.getString(path + ".actor")),
            yaml.getBoolean(path + ".blocks-private-messages", true)
        );
        return record.isActive(now) ? Optional.of(record) : Optional.empty();
    }

    @Override
    public void saveMute(MuteRecord muteRecord) {
        String path = "players." + muteRecord.playerId();
        yaml.set(path + ".created-at", muteRecord.createdAtMillis());
        yaml.set(path + ".until", muteRecord.expiresAtMillis());
        yaml.set(path + ".reason", muteRecord.reason());
        yaml.set(path + ".actor", muteRecord.actorId() == null ? null : muteRecord.actorId().toString());
        yaml.set(path + ".blocks-private-messages", muteRecord.blocksPrivateMessages());
        saveNow();
    }

    @Override
    public void clearMute(UUID playerId) {
        yaml.set("players." + playerId, null);
        saveNow();
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void saveNow() {
        try {
            ensureParentDirectory();
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save mute data to " + file.getAbsolutePath(), exception);
        }
    }

    private void ensureParentDirectory() {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create directory " + parent.getAbsolutePath());
        }
    }
}
