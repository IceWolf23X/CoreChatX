package me.icewolf23.chatbloom.common.privacy;

import java.util.UUID;

public interface PrivacyService {
    boolean isIgnoring(UUID actor, UUID target);

    void addIgnore(UUID actor, UUID target);

    void removeIgnore(UUID actor, UUID target);

    boolean isPmEnabled(UUID playerId);

    void setPmEnabled(UUID playerId, boolean enabled);
}
