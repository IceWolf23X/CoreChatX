package me.icewolf23.chatbloom.common.channel;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ChannelService {
    Optional<ChatChannel> findChannel(String id);

    Collection<ChatChannel> channels();

    ChatChannel getDefaultChannel();

    ChatChannel resolveActiveChannel(UUID playerId);

    void setActiveChannel(UUID playerId, String channelId);

    String getActiveChannel(UUID playerId);
}
