package me.icewolf23.chatbloom.paper.service;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.icewolf23.chatbloom.paper.util.TokenDecoration;
import me.icewolf23.chatbloom.common.network.PrivateMessagePacket;
import me.icewolf23.chatbloom.common.network.PrivateMessageResultPacket;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PrivateMessageService {

    private static final UUID CONSOLE_UUID = new UUID(0L, 0L);

    private final ChatBloom plugin;
    private final Map<UUID, ReplyTarget> replyTargets = new HashMap<>();
    private final RemotePmTracker remotePmTracker;

    public PrivateMessageService(ChatBloom plugin) {
        this.plugin = plugin;
        long timeoutSeconds = Math.max(5L, plugin.configuration().main().getLong("deployment.proxy.pending-pm-timeout-seconds", 20L));
        this.remotePmTracker = new RemotePmTracker(Duration.ofSeconds(timeoutSeconds));
    }

    public void reload() {
    }

    public boolean sendPrivateMessage(Player sender, Player target, String rawMessage) {
        return sendLocalPrivateMessage(sender, sender.getName(), target, rawMessage);
    }

    public boolean sendPrivateMessage(CommandSender sender, String senderName, Player target, String rawMessage) {
        return sendLocalPrivateMessage(sender, senderName, target, rawMessage);
    }

    public boolean sendPrivateMessage(CommandSender sender, String senderName, String targetName, String rawMessage) {
        Player localTarget = findOnlinePlayer(targetName);
        if (localTarget != null) {
            return sendLocalPrivateMessage(sender, senderName, localTarget, rawMessage);
        }
        return sendRemotePrivateMessage(sender, senderName, null, targetName, rawMessage);
    }

    public boolean sendReply(Player sender, String rawMessage) {
        ReplyTarget target = replyTargets.get(sender.getUniqueId());
        if (target == null) {
            sender.sendMessage(plugin.formats().configMessage("errors.no-reply-target", sender));
            return false;
        }
        if (!target.remote()) {
            Player localTarget = target.targetId() == null ? null : Bukkit.getPlayer(target.targetId());
            if (localTarget == null || !localTarget.isOnline()) {
                sender.sendMessage(plugin.formats().configMessage("errors.no-reply-target", sender));
                return false;
            }
            return sendLocalPrivateMessage(sender, sender.getName(), localTarget, rawMessage);
        }
        return sendRemotePrivateMessage(sender, sender.getName(), target.targetId(), target.targetName(), rawMessage);
    }

    public void acceptRemotePrivateMessage(PrivateMessagePacket packet) {
        Player target = packet.targetId() == null ? findOnlinePlayer(packet.targetName()) : Bukkit.getPlayer(packet.targetId());
        if (target == null || !target.isOnline()) {
            sendRemoteResult(new PrivateMessageResultPacket(
                packet.requestId(),
                packet.sourceServerId(),
                packet.senderId(),
                packet.targetId(),
                packet.targetName(),
                packet.plainText(),
                false,
                "errors.player-not-found",
                Instant.now()
            ));
            return;
        }

        if (!plugin.services().privacyService().isPmEnabled(target.getUniqueId()) && !packet.senderBypass()) {
            sendRemoteResult(new PrivateMessageResultPacket(
                packet.requestId(),
                packet.sourceServerId(),
                packet.senderId(),
                target.getUniqueId(),
                target.getName(),
                packet.plainText(),
                false,
                "private-messages.disabled-target",
                Instant.now()
            ));
            return;
        }

        if (plugin.services().privacyService().isIgnoring(target.getUniqueId(), packet.senderId()) && !packet.senderBypass()) {
            sendRemoteResult(new PrivateMessageResultPacket(
                packet.requestId(),
                packet.sourceServerId(),
                packet.senderId(),
                target.getUniqueId(),
                target.getName(),
                packet.plainText(),
                false,
                "private-messages.blocked-by-target",
                Instant.now()
            ));
            return;
        }

        MessageRender render = renderPrivateMessage(packet.plainText());
        if (render.plain().trim().isEmpty()) {
            sendRemoteResult(new PrivateMessageResultPacket(
                packet.requestId(),
                packet.sourceServerId(),
                packet.senderId(),
                target.getUniqueId(),
                target.getName(),
                packet.plainText(),
                false,
                "errors.empty-message",
                Instant.now()
            ));
            return;
        }

        target.sendMessage(plugin.formats().privateMessageToTarget(packet.senderName(), target, render.component()));
        plugin.notifications().notifyPrivateMessage(target);
        replyTargets.put(target.getUniqueId(), new ReplyTarget(packet.senderId(), packet.senderName(), true));
        notifySocialSpy(null, packet.senderName(), target.getName(), render.component());
        logConsole(packet.senderName(), target.getName(), render.plain());

        sendRemoteResult(new PrivateMessageResultPacket(
            packet.requestId(),
            packet.sourceServerId(),
            packet.senderId(),
            target.getUniqueId(),
            target.getName(),
            packet.plainText(),
            true,
            null,
            Instant.now()
        ));
    }

    public void handlePrivateMessageResult(PrivateMessageResultPacket packet) {
        remotePmTracker.pruneExpired();
        remotePmTracker.pruneForOfflineSenders();
        RemotePmTracker.PendingRemoteMessage pending = remotePmTracker.remove(packet.requestId());
        if (pending == null) {
            if (plugin.configuration().main().getBoolean("debug", false)) {
                plugin.getLogger().info("[DEBUG] Ignored stale or unknown remote PM result " + packet.requestId());
            }
            return;
        }
        Player senderPlayer = pending.sender() instanceof Player onlinePlayer && onlinePlayer.isOnline() ? onlinePlayer : null;
        if (pending.sender() instanceof Player && senderPlayer == null) {
            if (plugin.configuration().main().getBoolean("debug", false)) {
                plugin.getLogger().info("[DEBUG] Dropped remote PM result " + packet.requestId() + " because the sender is no longer online.");
            }
            return;
        }
        if (!packet.delivered()) {
            pending.sender().sendMessage(plugin.formats().configMessage(
                packet.reasonKey() == null ? "errors.player-not-found" : packet.reasonKey(),
                senderPlayer
            ));
            return;
        }

        MessageRender render = renderPrivateMessage(pending.sanitizedMessage());
        if (render.plain().trim().isEmpty()) {
            pending.sender().sendMessage(plugin.formats().configMessage("errors.empty-message", senderPlayer));
            return;
        }

        pending.sender().sendMessage(plugin.formats().privateMessageToSender(
            pending.sender(),
            pending.senderName(),
            packet.targetName(),
            render.component()
        ));
        if (senderPlayer != null) {
            plugin.cooldowns().markPrivateAccepted(senderPlayer);
            replyTargets.put(senderPlayer.getUniqueId(), new ReplyTarget(packet.targetId(), packet.targetName(), true));
        }
        notifySocialSpy(senderPlayer, pending.senderName(), packet.targetName(), render.component());
        logConsole(pending.senderName(), packet.targetName(), render.plain());
    }

    private boolean sendLocalPrivateMessage(CommandSender sender, String senderName, Player target, String rawMessage) {
        Player senderPlayer = sender instanceof Player player ? player : null;

        if (senderPlayer != null && senderPlayer.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.formats().configMessage("errors.cannot-message-self", senderPlayer));
            return false;
        }

        ChatPipelineContext moderationContext = new ChatPipelineContext(
            senderPlayer == null ? CONSOLE_UUID : senderPlayer.getUniqueId(),
            senderName,
            rawMessage
        );
        var moderationDecision = plugin.services().moderationService().evaluatePrivateMessage(moderationContext);
        if (!moderationDecision.allowed()) {
            sender.sendMessage(plugin.formats().configMessage(moderationDecision.messageKey(), senderPlayer));
            return false;
        }

        boolean staffBypass = isStaffBypass(senderPlayer);

        if (!plugin.services().privacyService().isPmEnabled(target.getUniqueId()) && !staffBypass) {
            sender.sendMessage(plugin.formats().configMessage("private-messages.disabled-target", senderPlayer));
            return false;
        }

        if (senderPlayer != null && plugin.services().privacyService().isIgnoring(target.getUniqueId(), senderPlayer.getUniqueId()) && !staffBypass) {
            sender.sendMessage(plugin.formats().configMessage("private-messages.blocked-by-target", senderPlayer));
            return false;
        }

        long remaining = senderPlayer == null ? 0L : plugin.cooldowns().remainingPrivate(senderPlayer);
        if (senderPlayer != null && remaining > 0L) {
            long seconds = Math.max(1L, (long) Math.ceil(remaining / 1000D));
            sender.sendMessage(plugin.formats().configMessage("cooldown.pm", senderPlayer, Placeholder.unparsed("seconds", Long.toString(seconds))));
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

        sender.sendMessage(plugin.formats().privateMessageToSender(sender, senderName, target, render.component()));
        target.sendMessage(plugin.formats().privateMessageToTarget(senderName, target, render.component()));
        plugin.notifications().notifyPrivateMessage(target);
        if (senderPlayer != null) {
            plugin.cooldowns().markPrivateAccepted(senderPlayer);
            replyTargets.put(senderPlayer.getUniqueId(), new ReplyTarget(target.getUniqueId(), target.getName(), false));
            replyTargets.put(target.getUniqueId(), new ReplyTarget(senderPlayer.getUniqueId(), senderPlayer.getName(), false));
        }
        notifySocialSpy(senderPlayer, senderName, target.getName(), render.component());
        logConsole(senderName, target.getName(), render.plain());
        return true;
    }

    private boolean sendRemotePrivateMessage(CommandSender sender, String senderName, UUID targetId, String targetName, String rawMessage) {
        Player senderPlayer = sender instanceof Player player ? player : null;
        if (senderPlayer != null && targetId != null && senderPlayer.getUniqueId().equals(targetId)) {
            sender.sendMessage(plugin.formats().configMessage("errors.cannot-message-self", senderPlayer));
            return false;
        }
        if (senderPlayer != null && targetId == null && sender.getName().equalsIgnoreCase(targetName)) {
            sender.sendMessage(plugin.formats().configMessage("errors.cannot-message-self", senderPlayer));
            return false;
        }
        if (!plugin.services().networkBridge().isEnabled()) {
            sender.sendMessage(plugin.formats().configMessage("errors.player-not-found", senderPlayer));
            return false;
        }

        ChatPipelineContext moderationContext = new ChatPipelineContext(
            senderPlayer == null ? CONSOLE_UUID : senderPlayer.getUniqueId(),
            senderName,
            rawMessage
        );
        var moderationDecision = plugin.services().moderationService().evaluatePrivateMessage(moderationContext);
        if (!moderationDecision.allowed()) {
            sender.sendMessage(plugin.formats().configMessage(moderationDecision.messageKey(), senderPlayer));
            return false;
        }

        long remaining = senderPlayer == null ? 0L : plugin.cooldowns().remainingPrivate(senderPlayer);
        if (senderPlayer != null && remaining > 0L) {
            long seconds = Math.max(1L, (long) Math.ceil(remaining / 1000D));
            sender.sendMessage(plugin.formats().configMessage("cooldown.pm", senderPlayer, Placeholder.unparsed("seconds", Long.toString(seconds))));
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

        UUID requestId = UUID.randomUUID();
        boolean sent = plugin.services().networkBridge().publishPrivateMessage(new PrivateMessagePacket(
            requestId,
            plugin.services().bridgeServerId(),
            senderPlayer == null ? CONSOLE_UUID : senderPlayer.getUniqueId(),
            targetId,
            senderName,
            targetName,
            sanitized,
            isStaffBypass(senderPlayer),
            Instant.now()
        ));
        if (!sent) {
            sender.sendMessage(plugin.formats().configMessage("private-messages.remote-unavailable", senderPlayer));
            return false;
        }
        remotePmTracker.pruneExpired();
        remotePmTracker.pruneForOfflineSenders();
        remotePmTracker.put(requestId, sender, senderName, targetName, sanitized);
        return true;
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

    private void notifySocialSpy(Player sender, String senderName, String targetName, Component message) {
        Component spy = plugin.formats().spyMessage(senderName, targetName, message);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if ((sender != null && online.getUniqueId().equals(sender.getUniqueId())) || online.getName().equalsIgnoreCase(targetName)) {
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

    private void logConsole(String senderName, String targetName, String plainText) {
        if (!plugin.configuration().main().getBoolean("logging.private-messages", true)) {
            return;
        }
        plugin.getLogger().info("[PM] " + senderName + " -> " + targetName + ": " + plainText);
    }

    private boolean isStaffBypass(Player senderPlayer) {
        return senderPlayer != null
            && plugin.configs().privacy().getBoolean("private-messages.allow-staff-bypass", true)
            && senderPlayer.hasPermission(plugin.configs().privacy().getString("private-messages.staff-bypass-permission", "chatbloom.staff"));
    }

    private Player findOnlinePlayer(String name) {
        return Bukkit.getOnlinePlayers().stream()
            .filter(online -> online.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    private void sendRemoteResult(PrivateMessageResultPacket packet) {
        boolean sent = plugin.services().networkBridge().publishPrivateMessageResult(packet);
        if (!sent) {
            plugin.getLogger().warning("ChatBloom proxy transport could not return PM result " + packet.requestId() + " to backend '" + packet.sourceServerId() + "'.");
        }
    }

    private record ReplyTarget(UUID targetId, String targetName, boolean remote) {
    }
    private record MessageRender(Component component, String plain) {
    }
}
