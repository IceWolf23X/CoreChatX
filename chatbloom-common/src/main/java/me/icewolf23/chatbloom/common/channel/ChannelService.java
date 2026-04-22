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

    default Optional<ChatChannelDefinition> find(String channelId) {
        return findChannel(channelId).map(ChatChannel::toDefinition);
    }

    default ChatChannelDefinition getRequired(String channelId) {
        return find(channelId).orElseThrow(() -> new IllegalArgumentException("Unknown channel: " + channelId));
    }

    default String getDefaultChannelId() {
        return getDefaultChannel().id();
    }

    default String getActiveChannelId(UUID playerId) {
        return getActiveChannel(playerId);
    }

    default void setActiveChannelId(UUID playerId, String channelId) {
        setActiveChannel(playerId, channelId);
    }

    default Collection<ChatChannelDefinition> listAvailableChannels(UUID playerId) {
        return channels().stream().map(ChatChannel::toDefinition).toList();
    }
}
