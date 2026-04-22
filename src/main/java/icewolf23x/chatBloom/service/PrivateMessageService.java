package icewolf23x.chatBloom.service;

import icewolf23x.chatBloom.ChatBloom;
import icewolf23x.chatBloom.util.TokenDecoration;
import icewolf23x.chatBloom.util.TextSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PrivateMessageService {

    private final ChatBloom plugin;
    private final Map<UUID, UUID> replyTargets = new HashMap<>();

    public PrivateMessageService(ChatBloom plugin) {
        this.plugin = plugin;
    }

    public void reload() {
    }

    public boolean sendPrivateMessage(Player sender, Player target, String rawMessage) {
        return sendPrivateMessage(sender, sender.getName(), target, rawMessage);
    }

    public boolean sendPrivateMessage(CommandSender sender, String senderName, Player target, String rawMessage) {
        Player senderPlayer = sender instanceof Player player ? player : null;

        if (senderPlayer != null && senderPlayer.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.formats().configMessage("errors.cannot-message-self", senderPlayer));
            return false;
        }

        long remaining = senderPlayer == null ? 0L : plugin.cooldowns().remainingPrivate(senderPlayer);
        if (senderPlayer != null && remaining > 0L) {
            long seconds = Math.max(1L, (long) Math.ceil(remaining / 1000D));
            sender.sendMessage(plugin.formats().configMessage("cooldown.pm", senderPlayer, net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("seconds", Long.toString(seconds))));
            return false;
        }

        String sanitized = plugin.legacyFormatting().sanitizeForSender(sender, rawMessage);
        if (sanitized.trim().isEmpty()) {
            sender.sendMessage(plugin.formats().configMessage("errors.empty-message", senderPlayer));
            return false;
        }

        MessageRender render = renderPrivateMessage(sanitized);
        if (render.plain().trim().isEmpty()) {
            sender.sendMessage(plugin.formats().configMessage("errors.empty-message", senderPlayer));
            return false;
        }

        Component toSender = plugin.formats().privateMessageToSender(sender, senderName, target, render.component());
        Component toTarget = plugin.formats().privateMessageToTarget(senderName, target, render.component());
        sender.sendMessage(toSender);
        target.sendMessage(toTarget);
        plugin.notifications().notifyPrivateMessage(target);
        if (senderPlayer != null) {
            plugin.cooldowns().markPrivateAccepted(senderPlayer);
            replyTargets.put(senderPlayer.getUniqueId(), target.getUniqueId());
            replyTargets.put(target.getUniqueId(), senderPlayer.getUniqueId());
        }
        notifySocialSpy(senderPlayer, senderName, target, render.component());
        logConsole(senderName, target, render.plain());
        return true;
    }

    public Player findReplyTarget(Player sender) {
        UUID targetId = replyTargets.get(sender.getUniqueId());
        if (targetId == null) {
            return null;
        }
        return Bukkit.getPlayer(targetId);
    }

    private MessageRender renderPrivateMessage(String sanitized) {
        List<Component> parts = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        for (String token : sanitized.split(" ", -1)) {
            if (!plain.isEmpty()) {
                parts.add(Component.space());
                plain.append(' ');
            }
            TokenDecoration decoration = TokenDecoration.from(token);
            String plainCore = plugin.legacyFormatting().stripCodes(decoration.core());
            var filtered = plugin.wordFilter().filterToken(plainCore);
            Component rendered = filtered.plainText().equals(plainCore)
                ? plugin.formats().legacyAware(decoration.core())
                : filtered.rendered();
            parts.add(Component.text(decoration.prefix()).append(rendered).append(Component.text(decoration.suffix())));
            plain.append(decoration.prefix()).append(filtered.plainText()).append(decoration.suffix());
        }
        return new MessageRender(Component.join(JoinConfiguration.noSeparators(), parts), plain.toString());
    }

    private void notifySocialSpy(Player sender, String senderName, Player target, Component message) {
        Component spy = plugin.formats().spyMessage(senderName, target, message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if ((sender != null && online.getUniqueId().equals(sender.getUniqueId())) || online.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            if (!online.hasPermission("chatbloom.command.socialspy")) {
                continue;
            }
            if (!plugin.playerData().get(online.getUniqueId()).isSocialSpyEnabled()) {
                continue;
            }
            online.sendMessage(spy);
        }
    }

    private void logConsole(String senderName, Player target, String plainText) {
        if (!plugin.configuration().main().getBoolean("logging.private-messages", true)) {
            return;
        }
        plugin.getLogger().info("[PM] " + senderName + " -> " + target.getName() + ": " + plainText);
    }

    private record MessageRender(Component component, String plain) {
    }
}
