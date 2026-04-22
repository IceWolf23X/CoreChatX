package me.icewolf23.chatbloom.common.storage.repository;

import java.util.UUID;

public interface ActiveChannelRepository {
    String loadActiveChannel(UUID playerId);

    void saveActiveChannel(UUID playerId, String channelId);
}
