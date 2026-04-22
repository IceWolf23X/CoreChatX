package me.icewolf23.chatbloom.paper.service;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.icewolf23.chatbloom.paper.data.PlayerSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class NotificationService {

    private final ChatBloom plugin;
    private Sound pingSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private float pingVolume = 0.85f;
    private float pingPitch = 1.25f;
    private Sound privateMessageSound = Sound.BLOCK_NOTE_BLOCK_BELL;
    private float privateMessageVolume = 0.85f;
    private float privateMessagePitch = 1.1f;

    public NotificationService(ChatBloom plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        var messages = plugin.configuration().messages();
        this.pingSound = sound(messages.getString("ping.notification-sound", Sound.ENTITY_EXPERIENCE_ORB_PICKUP.name()), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        this.pingVolume = (float) messages.getDouble("ping.notification-volume", 0.85D);
        this.pingPitch = (float) messages.getDouble("ping.notification-pitch", 1.25D);
        this.privateMessageSound = sound(messages.getString("private-messages.received-sound", Sound.BLOCK_NOTE_BLOCK_BELL.name()), Sound.BLOCK_NOTE_BLOCK_BELL);
        this.privateMessageVolume = (float) messages.getDouble("private-messages.received-volume", 0.85D);
        this.privateMessagePitch = (float) messages.getDouble("private-messages.received-pitch", 1.1D);
    }

    public void notifyMention(String senderName, Player target, boolean bypassToggle) {
        notifyPing(senderName, target, bypassToggle);
    }

    public void notifyCustomPing(String senderName, Player target, boolean bypassToggle) {
        notifyPing(senderName, target, bypassToggle);
    }

    private void notifyPing(String senderName, Player target, boolean bypassToggle) {
        PlayerSettings settings = plugin.playerData().get(target.getUniqueId());
        if (!bypassToggle && !settings.isMentionNotificationsEnabled()) {
            return;
        }
        boolean allowSound = bypassToggle || settings.isPingSoundEnabled();
        boolean allowActionbar = bypassToggle || settings.isPingActionbarEnabled();

        if (plugin.configuration().main().getBoolean("debug", false)) {
            plugin.getLogger().info(
                "[DEBUG] Ping notification target=" + target.getName()
                    + ", sender=" + senderName
                    + ", bypass=" + bypassToggle
                    + ", soundEnabled=" + settings.isPingSoundEnabled()
                    + ", actionbarEnabled=" + settings.isPingActionbarEnabled()
            );
        }

        if (allowSound && plugin.configuration().pings().getBoolean("mentions.notify-sound", true)) {
            target.playSound(target.getLocation(), pingSound, pingVolume, pingPitch);
        }
        if (allowActionbar && plugin.configuration().pings().getBoolean("mentions.notify-actionbar", true)) {
            Component actionbar = plugin.formats().configMessage(
                "ping.notification-actionbar",
                target,
                Placeholder.unparsed("sender_name", senderName)
            );
            target.sendActionBar(actionbar);
        }
    }

    public void notifyPrivateMessage(Player target) {
        target.playSound(target.getLocation(), privateMessageSound, privateMessageVolume, privateMessagePitch);
    }

    private Sound sound(String input, Sound fallback) {
        try {
            return Sound.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
