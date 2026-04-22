package me.icewolf23.chatbloom.paper.platform;

import me.icewolf23.chatbloom.common.model.PlayerSettingsRecord;
import me.icewolf23.chatbloom.common.privacy.PrivacyService;
import me.icewolf23.chatbloom.common.storage.repository.IgnoreRepository;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DefaultPrivacyService implements PrivacyService {

    private final IgnoreRepository ignoreRepository;
    private final PlayerStateRepository playerStateRepository;
    private final boolean ignoreEnabled;

    public DefaultPrivacyService(IgnoreRepository ignoreRepository, PlayerStateRepository playerStateRepository, boolean ignoreEnabled) {
        this.ignoreRepository = ignoreRepository;
        this.playerStateRepository = playerStateRepository;
        this.ignoreEnabled = ignoreEnabled;
    }

    @Override
    public boolean isIgnoring(UUID actor, UUID target) {
        if (!ignoreEnabled) {
            return false;
        }
        return ignoreRepository.loadIgnored(actor).contains(target);
    }

    @Override
    public Set<UUID> ignoredPlayers(UUID actor) {
        if (!ignoreEnabled) {
            return Set.of();
        }
        return Set.copyOf(ignoreRepository.loadIgnored(actor));
    }

    @Override
    public void addIgnore(UUID actor, UUID target) {
        if (!ignoreEnabled) {
            return;
        }
        Set<UUID> ignored = new HashSet<>(ignoreRepository.loadIgnored(actor));
        ignored.add(target);
        ignoreRepository.saveIgnored(actor, ignored);
    }

    @Override
    public void removeIgnore(UUID actor, UUID target) {
        if (!ignoreEnabled) {
            return;
        }
        Set<UUID> ignored = new HashSet<>(ignoreRepository.loadIgnored(actor));
        ignored.remove(target);
        ignoreRepository.saveIgnored(actor, ignored);
    }

    @Override
    public boolean isPmEnabled(UUID playerId) {
        return playerStateRepository.load(playerId).pmEnabled();
    }

    @Override
    public void setPmEnabled(UUID playerId, boolean enabled) {
        PlayerSettingsRecord current = playerStateRepository.load(playerId);
        playerStateRepository.save(new PlayerSettingsRecord(
            current.playerId(),
            current.pingSoundEnabled(),
            current.pingActionbarEnabled(),
            current.socialSpyEnabled(),
            enabled
        ));
    }
}
