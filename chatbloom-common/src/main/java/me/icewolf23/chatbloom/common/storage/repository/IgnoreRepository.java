package me.icewolf23.chatbloom.common.storage.repository;

import java.util.Set;
import java.util.UUID;

public interface IgnoreRepository {
    Set<UUID> loadIgnored(UUID playerId);

    void saveIgnored(UUID playerId, Set<UUID> ignored);
}
