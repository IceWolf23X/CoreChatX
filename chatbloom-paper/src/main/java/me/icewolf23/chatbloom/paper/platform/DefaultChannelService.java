package me.icewolf23.chatbloom.paper.platform;

import me.icewolf23.chatbloom.common.channel.ChannelScope;
import me.icewolf23.chatbloom.common.channel.ChatChannel;
import me.icewolf23.chatbloom.common.channel.ChannelService;
import me.icewolf23.chatbloom.common.storage.repository.ActiveChannelRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DefaultChannelService implements ChannelService {

    private final Map<String, ChatChannel> channels = new HashMap<>();
    private final ActiveChannelRepository activeChannelRepository;

    public DefaultChannelService(FileConfiguration configuration, ActiveChannelRepository activeChannelRepository) {
        this.activeChannelRepository = activeChannelRepository;
        loadChannels(configuration);
    }

    @Override
    public Optional<ChatChannel> findChannel(String id) {
        return Optional.ofNullable(channels.get(id.toLowerCase(Locale.ROOT)));
    }

    @Override
    public ChatChannel getDefaultChannel() {
        return channels.values().stream()
            .filter(ChatChannel::defaultChannel)
            .findFirst()
            .orElseGet(() -> new ChatChannel("global", true, true, ChannelScope.SERVER, null, "", ""));
    }

    @Override
    public void setActiveChannel(UUID playerId, String channelId) {
        activeChannelRepository.saveActiveChannel(playerId, channelId);
    }

    @Override
    public String getActiveChannel(UUID playerId) {
        String stored = activeChannelRepository.loadActiveChannel(playerId);
        return stored == null || stored.isBlank() ? getDefaultChannel().id() : stored;
    }

    private void loadChannels(FileConfiguration configuration) {
        channels.clear();
        ConfigurationSection section = configuration.getConfigurationSection("channels");
        if (section == null) {
            channels.put("global", new ChatChannel("global", true, true, ChannelScope.SERVER, null, "", ""));
            return;
        }
        for (String key : section.getKeys(false)) {
            String base = "channels." + key + ".";
            boolean enabled = configuration.getBoolean(base + "enabled", true);
            boolean defaultChannel = configuration.getBoolean(base + "default", false);
            String scopeRaw = configuration.getString(base + "scope", ChannelScope.SERVER.name());
            ChannelScope scope;
            try {
                scope = ChannelScope.valueOf(scopeRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                scope = ChannelScope.SERVER;
            }
            Integer radius = configuration.contains(base + "radius") ? configuration.getInt(base + "radius") : null;
            String sendPermission = configuration.getString(base + "permission-send", "");
            String receivePermission = configuration.getString(base + "permission-receive", "");
            channels.put(key.toLowerCase(Locale.ROOT), new ChatChannel(key, enabled, defaultChannel, scope, radius, sendPermission, receivePermission));
        }
        if (channels.values().stream().noneMatch(ChatChannel::defaultChannel)) {
            channels.put("global", new ChatChannel("global", true, true, ChannelScope.SERVER, null, "", ""));
        }
    }
}
