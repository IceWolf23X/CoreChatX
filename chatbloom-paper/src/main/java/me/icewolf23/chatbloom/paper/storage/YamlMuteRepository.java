package me.icewolf23.chatbloom.paper.storage;

import me.icewolf23.chatbloom.common.moderation.MuteRecord;
import me.icewolf23.chatbloom.common.storage.repository.MuteRepository;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
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
    public Optional<MuteRecord> findActiveMute(UUID playerId, Instant now) {
        String path = "players." + playerId;
        if (!yaml.contains(path)) {
            return Optional.empty();
        }
        Long until = yaml.contains(path + ".until") ? yaml.getLong(path + ".until") : null;
        MuteRecord record = new MuteRecord(
            playerId,
            until == null || until <= 0L ? null : Instant.ofEpochMilli(until),
            yaml.getString(path + ".reason", "No reason provided"),
            parseUuid(yaml.getString(path + ".actor"))
        );
        return record.isActive(now) ? Optional.of(record) : Optional.empty();
    }

    @Override
    public void saveMute(MuteRecord muteRecord) {
        String path = "players." + muteRecord.playerId();
        yaml.set(path + ".until", muteRecord.expiresAt() == null ? null : muteRecord.expiresAt().toEpochMilli());
        yaml.set(path + ".reason", muteRecord.reason());
        yaml.set(path + ".actor", muteRecord.actorId() == null ? null : muteRecord.actorId().toString());
        saveNow();
    }

    @Override
    public void clearMute(UUID playerId) {
        yaml.set("players." + playerId, null);
        saveNow();
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return new UUID(0L, 0L);
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return new UUID(0L, 0L);
        }
    }

    private void saveNow() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save mutedata.yml", exception);
        }
    }
}
