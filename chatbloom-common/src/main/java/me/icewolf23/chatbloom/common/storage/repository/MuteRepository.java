package me.icewolf23.chatbloom.common.storage.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import me.icewolf23.chatbloom.common.moderation.MuteRecord;

public interface MuteRepository {
    Optional<MuteRecord> findActiveMute(UUID playerId, Instant now);

    void saveMute(MuteRecord muteRecord);

    void clearMute(UUID playerId);
}
