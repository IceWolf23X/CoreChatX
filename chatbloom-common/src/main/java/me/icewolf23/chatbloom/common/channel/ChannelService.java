package me.icewolf23.chatbloom.common.channel;

import java.util.Optional;
import java.util.UUID;

public interface ChannelService {
    Optional<ChatChannel> findChannel(String id);

    ChatChannel getDefaultChannel();

    void setActiveChannel(UUID playerId, String channelId);

    String getActiveChannel(UUID playerId);
}
