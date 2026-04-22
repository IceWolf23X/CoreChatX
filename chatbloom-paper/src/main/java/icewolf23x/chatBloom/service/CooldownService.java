package icewolf23x.chatBloom.service;

import icewolf23x.chatBloom.ChatBloom;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CooldownService {

    private final ChatBloom plugin;
    private final Map<UUID, Long> publicChatCooldowns = new HashMap<>();
    private final Map<UUID, Long> privateMessageCooldowns = new HashMap<>();
    private boolean publicEnabled;
    private long publicMillis;
    private boolean privateEnabled;
    private long privateMillis;

    public CooldownService(ChatBloom plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        var config = plugin.configuration().chat();
        this.publicEnabled = config.getBoolean("cooldowns.public.enabled", true);
        this.publicMillis = Math.max(0L, config.getLong("cooldowns.public.seconds", 2L) * 1000L);
        this.privateEnabled = config.getBoolean("cooldowns.private-messages.enabled", false);
        this.privateMillis = Math.max(0L, config.getLong("cooldowns.private-messages.seconds", 2L) * 1000L);
    }

    public long remainingPublic(Player player) {
        if (!publicEnabled || player.hasPermission("chatbloom.cooldown.bypass.public")) {
            return 0L;
        }
        return Math.max(0L, publicChatCooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis());
    }

    public long remainingPrivate(Player player) {
        if (!privateEnabled || player.hasPermission("chatbloom.cooldown.bypass.pm")) {
            return 0L;
        }
        return Math.max(0L, privateMessageCooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis());
    }

    public void markPublicAccepted(Player player) {
        if (publicEnabled && !player.hasPermission("chatbloom.cooldown.bypass.public")) {
            publicChatCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + publicMillis);
        }
    }

    public void markPrivateAccepted(Player player) {
        if (privateEnabled && !player.hasPermission("chatbloom.cooldown.bypass.pm")) {
            privateMessageCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + privateMillis);
        }
    }
}
