package me.icewolf23.chatbloom.paper.platform;

import me.icewolf23.chatbloom.common.channel.ChannelScope;
import me.icewolf23.chatbloom.common.channel.ChatChannel;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PaperChannelAudienceResolver {

    private final PlayerStateRepository playerStateRepository;

    public PaperChannelAudienceResolver(PlayerStateRepository playerStateRepository) {
        this.playerStateRepository = playerStateRepository;
    }

    public Set<Player> resolveRecipients(Player sender, ChatChannel channel) {
        Set<Player> recipients = new LinkedHashSet<>();
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (!canReceive(candidate, channel)) {
                continue;
            }
            if (candidate.getUniqueId().equals(sender.getUniqueId())) {
                recipients.add(candidate);
                continue;
            }
            if (channel.scope() == ChannelScope.RADIUS || channel.scope() == ChannelScope.LOCAL_RADIUS) {
                if (!candidate.getWorld().equals(sender.getWorld())) {
                    continue;
                }
                int radius = channel.radius() == null ? 100 : channel.radius();
                if (candidate.getLocation().distanceSquared(sender.getLocation()) > (double) radius * radius) {
                    continue;
                }
            }
            if (channel.scope() == ChannelScope.WORLD && !candidate.getWorld().equals(sender.getWorld())) {
                continue;
            }
            recipients.add(candidate);
        }
        recipients.add(sender);
        return recipients;
    }

    public Set<Player> resolveRemoteRecipients(ChatChannel channel) {
        Set<Player> recipients = new LinkedHashSet<>();
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (canReceive(candidate, channel)) {
                recipients.add(candidate);
            }
        }
        return recipients;
    }

    public boolean canSend(Player player, ChatChannel channel) {
        return channel.enabled()
            && (channel.sendPermission().isBlank() || player.hasPermission(channel.sendPermission()))
            && isStaffChannelEnabled(player, channel);
    }

    public boolean canReceive(Player player, ChatChannel channel) {
        return isStaffChannelEnabled(player, channel)
            && (channel.receivePermission().isBlank() || player.hasPermission(channel.receivePermission()));
    }

    private boolean isStaffChannelEnabled(Player player, ChatChannel channel) {
        return !"staff".equalsIgnoreCase(channel.id()) || playerStateRepository.load(player.getUniqueId()).staffChatEnabled();
    }
}
