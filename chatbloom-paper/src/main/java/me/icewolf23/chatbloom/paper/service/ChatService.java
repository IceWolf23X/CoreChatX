package me.icewolf23.chatbloom.paper.service;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.icewolf23.chatbloom.paper.model.CustomPingDefinition;
import me.icewolf23.chatbloom.paper.model.FilteredToken;
import me.icewolf23.chatbloom.paper.model.ProcessedMessage;
import me.icewolf23.chatbloom.paper.util.TokenDecoration;
import me.icewolf23.chatbloom.paper.util.TextSanitizer;
import me.icewolf23.chatbloom.common.channel.ChannelScope;
import me.icewolf23.chatbloom.common.network.ChatMessagePacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatService {

    private final ChatBloom plugin;
    private final Map<String, CustomPingDefinition> customPings = new HashMap<>();
    private final Map<UUID, Deque<String>> recentMessages = new HashMap<>();
    private boolean publicChatEnabled;
    private boolean mentionsEnabled;
    private String noMessageSentPath = "chat.no-message-sent";
    private boolean antiRepeatEnabled;
    private int antiRepeatWindow;
    private boolean antiRepeatBlockIdentical;
    private boolean antiCapsEnabled;
    private int antiCapsMinLength;
    private double antiCapsMaxRatio;

    public ChatService(ChatBloom plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.publicChatEnabled = plugin.configuration().chat().getBoolean("public-chat.enabled", true);
        this.mentionsEnabled = plugin.configuration().chat().getBoolean("mentions.enabled", true);
        this.antiRepeatEnabled = plugin.configs().moderation().getBoolean("anti-repeat.enabled", true);
        this.antiRepeatWindow = Math.max(1, plugin.configs().moderation().getInt("anti-repeat.similarity-window", 3));
        this.antiRepeatBlockIdentical = plugin.configs().moderation().getBoolean("anti-repeat.block-identical", true);
        this.antiCapsEnabled = plugin.configs().moderation().getBoolean("anti-caps.enabled", false);
        this.antiCapsMinLength = Math.max(1, plugin.configs().moderation().getInt("anti-caps.min-length", 8));
        this.antiCapsMaxRatio = plugin.configs().moderation().getDouble("anti-caps.max-uppercase-ratio", 0.7D);
        this.customPings.clear();
        ConfigurationSection section = plugin.configuration().pings().getConfigurationSection("custom-pings");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String base = "custom-pings." + key + ".";
            String trigger = plugin.configuration().pings().getString(base + "trigger", "@" + key);
            String usePermission = plugin.configuration().pings().getString(base + "use-permission", "");
            String receivePermission = plugin.configuration().pings().getString(base + "receive-permission", "");
            boolean bypassToggle = plugin.configuration().pings().getBoolean(base + "bypass-toggle", false);
            String tokenFormat = plugin.configuration().pings().getString(base + "token-format", "<yellow>" + trigger + "</yellow>");
            customPings.put(trigger.toLowerCase(Locale.ROOT), new CustomPingDefinition(key, trigger, usePermission, receivePermission, bypassToggle, tokenFormat));
        }
    }

    public boolean isPublicChatEnabled() {
        return publicChatEnabled;
    }

    public ChatBloom plugin() {
        return plugin;
    }

    public void handlePublicChat(Player sender, String rawMessage) {
        handlePublicChat(sender, rawMessage, plugin.services().channelService().getActiveChannel(sender.getUniqueId()));
    }

    public void handlePublicChat(Player sender, String rawMessage, String channelId) {
        if (!publicChatEnabled) {
            return;
        }

        if (plugin.services().moderationService().isChatMuted() && !sender.hasPermission("chatbloom.moderation.bypass.mutechat")) {
            sender.sendMessage(plugin.formats().configMessage("moderation.chat-muted", sender));
            return;
        }

        var channel = plugin.services().channelService().resolveActiveChannel(sender.getUniqueId());
        if (!channel.id().equalsIgnoreCase(channelId)) {
            channelId = channel.id();
        }
        if (!plugin.services().channelAudienceResolver().canSend(sender, channel)) {
            sender.sendMessage(plugin.formats().configMessage("channels.no-send-permission", sender));
            return;
        }

        long remaining = plugin.cooldowns().remainingPublic(sender);
        if (remaining > 0L) {
            long seconds = Math.max(1L, (long) Math.ceil(remaining / 1000D));
            sender.sendMessage(plugin.formats().configMessage("cooldown.public", sender, net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("seconds", Long.toString(seconds))));
            return;
        }

        String sanitized = plugin.legacyFormatting().sanitizeForPlayer(sender, rawMessage);
        if (sanitized.trim().isEmpty()) {
            sender.sendMessage(plugin.formats().configMessage(noMessageSentPath, sender));
            return;
        }

        String normalizedPlain = normalizeForModeration(plugin.legacyFormatting().stripCodes(sanitized));
        if (shouldBlockAntiRepeat(sender, normalizedPlain)) {
            sender.sendMessage(plugin.formats().configMessage("moderation.anti-repeat", sender));
            return;
        }
        if (shouldBlockAntiCaps(sender, normalizedPlain)) {
            sender.sendMessage(plugin.formats().configMessage("moderation.anti-caps", sender));
            return;
        }

        ProcessedMessage rendered = processMessage(sender, sanitized);
        if (rendered.plainText().trim().isEmpty()) {
            sender.sendMessage(plugin.formats().configMessage(noMessageSentPath, sender));
            return;
        }

        plugin.cooldowns().markPublicAccepted(sender);
        recordModerationHistory(sender, normalizedPlain);

        Component finalMessage = plugin.formats().publicChat(sender, rendered.component(), channel.id());
        Set<Player> recipients = plugin.services().channelAudienceResolver().resolveRecipients(sender, channel);
        for (Player player : recipients) {
            player.sendMessage(finalMessage);
        }
        Set<Player> notificationTargets = new LinkedHashSet<>(rendered.notificationTargets());
        notificationTargets.retainAll(recipients);
        Set<Player> bypassTargets = new LinkedHashSet<>(rendered.bypassTargets());
        bypassTargets.retainAll(recipients);
        notifyTargets(sender.getUniqueId(), sender.getName(), notificationTargets, bypassTargets);
        if (channel.scope() == ChannelScope.NETWORK && plugin.services().networkBridge().isEnabled()) {
            boolean forwarded = plugin.services().networkBridge().publishChat(new ChatMessagePacket(
                sender.getUniqueId(),
                sender.getName(),
                plugin.services().bridgeServerId(),
                channel.id(),
                sanitized,
                plugin.formats().rankPrefixTemplate(sender),
                Instant.now()
            ));
            if (!forwarded) {
                plugin.getLogger().warning("ChatBloom proxy fanout failed for channel '" + channel.id() + "' from sender '" + sender.getName() + "'. Local delivery still succeeded.");
            }
        }
        logConsole(channel.id(), finalMessage);
    }

    public void handleRemoteNetworkChat(ChatMessagePacket packet) {
        var channel = plugin.services().channelService().findChannel(packet.channelId())
            .orElseGet(() -> plugin.services().channelService().getDefaultChannel());
        if (!channel.enabled() || channel.scope() != ChannelScope.NETWORK) {
            return;
        }
        ProcessedMessage rendered = processRemoteSanitizedMessage(packet.plainText());
        if (rendered.plainText().trim().isEmpty()) {
            return;
        }
        Component finalMessage = plugin.formats().publicChatRemote(packet.senderName(), packet.rankPrefixTemplate(), rendered.component(), channel.id());
        Set<Player> recipients = plugin.services().channelAudienceResolver().resolveRemoteRecipients(channel);
        for (Player player : recipients) {
            player.sendMessage(finalMessage);
        }
        Set<Player> notificationTargets = new LinkedHashSet<>(rendered.notificationTargets());
        notificationTargets.retainAll(recipients);
        Set<Player> bypassTargets = new LinkedHashSet<>(rendered.bypassTargets());
        bypassTargets.retainAll(recipients);
        notifyTargets(packet.senderId(), packet.senderName(), notificationTargets, bypassTargets);
        logConsole(channel.id(), finalMessage);
    }

    public ProcessedMessage processMessage(Player sender, String rawMessage) {
        String sanitized = plugin.legacyFormatting().sanitizeForPlayer(sender, rawMessage);
        return processSanitizedMessage(sender, sanitized);
    }

    public ProcessedMessage processSanitizedMessage(Player sender, String sanitized) {
        return processSanitizedMessage(sender, sender, sanitized, true, false);
    }

    public ProcessedMessage processSanitizedMessage(CommandSender sender, String sanitized, boolean allowChatItems) {
        return processSanitizedMessage(sender, sender instanceof Player player ? player : null, sanitized, allowChatItems, false);
    }

    public ProcessedMessage processRemoteSanitizedMessage(String sanitized) {
        return processSanitizedMessage(null, null, sanitized, false, true);
    }

    private ProcessedMessage processSanitizedMessage(CommandSender sender, Player playerContext, String sanitized, boolean allowChatItems, boolean remoteSender) {
        Map<String, Player> onlinePlayers = onlinePlayersByLowerName();
        Set<Player> notificationTargets = new LinkedHashSet<>();
        Set<Player> bypassTargets = new LinkedHashSet<>();
        List<Component> parts = new ArrayList<>();
        StringBuilder plain = new StringBuilder();

        for (String token : splitPreservingWhitespace(sanitized)) {
            if (token.isBlank() && token.chars().allMatch(Character::isWhitespace)) {
                parts.add(Component.text(token));
                plain.append(token);
                continue;
            }

            TokenDecoration decorated = TokenDecoration.from(token);
            if (decorated.core().isEmpty()) {
                parts.add(Component.text(token));
                plain.append(token);
                continue;
            }

            TokenRender tokenRender = renderCoreToken(sender, playerContext, decorated.core(), allowChatItems, remoteSender, onlinePlayers, notificationTargets, bypassTargets);
            parts.add(Component.text(decorated.prefix()).append(tokenRender.component()).append(Component.text(decorated.suffix())));
            plain.append(decorated.prefix()).append(tokenRender.plain()).append(decorated.suffix());
        }

        Component joined = Component.join(JoinConfiguration.noSeparators(), parts);
        return new ProcessedMessage(joined, plain.toString(), notificationTargets, bypassTargets);
    }

    private TokenRender renderCoreToken(
        CommandSender sender,
        Player playerContext,
        String core,
        boolean allowChatItems,
        boolean remoteSender,
        Map<String, Player> onlinePlayers,
        Set<Player> notificationTargets,
        Set<Player> bypassTargets
    ) {
        String plainCore = plugin.legacyFormatting().stripCodes(core);
        String normalized = plainCore.toLowerCase(Locale.ROOT);

        CustomPingDefinition customPing = customPings.get(normalized);
        if (customPing != null && isAllowedToUse(sender, customPing, remoteSender)) {
            String displayTrigger = canonicalizeTrigger(customPing.trigger());
            Component rendered = plugin.formats().customPingToken(playerContext, customPing.tokenFormat(), displayTrigger);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (playerContext != null && player.getUniqueId().equals(playerContext.getUniqueId())) {
                    continue;
                }
                if (!canReceive(customPing, player)) {
                    continue;
                }
                notificationTargets.add(player);
                if (customPing.bypassToggle()) {
                    bypassTargets.add(player);
                }
            }
            return new TokenRender(displayTrigger, rendered);
        }

        if (allowChatItems && playerContext != null && plugin.chatItems().isToken(normalized)) {
            Component rendered = plugin.chatItems().createTokenComponent(playerContext, core);
            return new TokenRender(PlainTextComponentSerializer.plainText().serialize(rendered), rendered);
        }

        if (mentionsEnabled) {
            String mentionKey = normalized.startsWith("@") ? normalized.substring(1) : normalized;
            Player mentioned = onlinePlayers.get(mentionKey);
            if (mentioned != null) {
                Component rendered = plugin.formats().mentionToken(playerContext, mentioned.getName());
                if (playerContext == null || !mentioned.getUniqueId().equals(playerContext.getUniqueId())) {
                    notificationTargets.add(mentioned);
                }
                return new TokenRender("@" + mentioned.getName(), rendered);
            }
        }

        FilteredToken filtered = plugin.wordFilter().filterToken(plainCore);
        if (filtered.plainText().equals(plainCore)) {
            return new TokenRender(plainCore, plugin.formats().legacyAware(core));
        }
        return new TokenRender(filtered.plainText(), filtered.rendered());
    }

    private void notifyTargets(UUID senderId, String senderName, Set<Player> targets, Set<Player> bypassTargets) {
        boolean blockIgnored = plugin.configs().privacy().getBoolean("ignore.block-mentions-from-ignored", true);
        for (Player target : targets) {
            if (blockIgnored && plugin.services().privacyService().isIgnoring(target.getUniqueId(), senderId)) {
                continue;
            }
            boolean bypass = bypassTargets.contains(target);
            plugin.notifications().notifyMention(senderName, target, bypass);
        }
    }

    private boolean shouldBlockAntiRepeat(Player sender, String normalizedPlain) {
        if (!antiRepeatEnabled || !antiRepeatBlockIdentical || normalizedPlain.isBlank() || sender.hasPermission("chatbloom.moderation.bypass.repeat")) {
            return false;
        }
        Deque<String> history = recentMessages.computeIfAbsent(sender.getUniqueId(), ignored -> new ArrayDeque<>());
        return history.stream().anyMatch(normalizedPlain::equalsIgnoreCase);
    }

    private boolean shouldBlockAntiCaps(Player sender, String normalizedPlain) {
        if (!antiCapsEnabled || sender.hasPermission("chatbloom.moderation.bypass.caps")) {
            return false;
        }
        long letters = normalizedPlain.chars().filter(Character::isLetter).count();
        if (letters < antiCapsMinLength) {
            return false;
        }
        long uppercase = normalizedPlain.chars().filter(Character::isUpperCase).count();
        return letters > 0 && ((double) uppercase / letters) > antiCapsMaxRatio;
    }

    private void recordModerationHistory(Player sender, String normalizedPlain) {
        if (!antiRepeatEnabled || normalizedPlain.isBlank()) {
            return;
        }
        Deque<String> history = recentMessages.computeIfAbsent(sender.getUniqueId(), ignored -> new ArrayDeque<>());
        history.addLast(normalizedPlain);
        while (history.size() > antiRepeatWindow) {
            history.removeFirst();
        }
    }

    private String normalizeForModeration(String input) {
        return TextSanitizer.sanitize(input).trim().replaceAll("\\s+", " ");
    }

    private void logConsole(String channelId, Component component) {
        if (!plugin.configuration().main().getBoolean("logging.public-chat", true)) {
            return;
        }
        plugin.getLogger().info("[CHAT:" + channelId + "] " + PlainTextComponentSerializer.plainText().serialize(component));
    }

    private boolean isAllowedToUse(CommandSender sender, CustomPingDefinition definition, boolean remoteSender) {
        if (remoteSender) {
            return true;
        }
        return definition.usePermission().isBlank() || (sender != null && sender.hasPermission(definition.usePermission()));
    }

    private boolean canReceive(CustomPingDefinition definition, Player target) {
        return definition.receivePermission().isBlank() || target.hasPermission(definition.receivePermission());
    }

    private Map<String, Player> onlinePlayersByLowerName() {
        Map<String, Player> players = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.put(player.getName().toLowerCase(Locale.ROOT), player);
        }
        return players;
    }

    private String canonicalizeTrigger(String trigger) {
        if (trigger == null || trigger.isBlank()) {
            return "";
        }
        if (trigger.length() == 1) {
            return trigger.toUpperCase(Locale.ROOT);
        }
        if (trigger.startsWith("@") && trigger.length() > 1) {
            String body = trigger.substring(1).toLowerCase(Locale.ROOT);
            return "@" + Character.toUpperCase(body.charAt(0)) + body.substring(1);
        }
        String body = trigger.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(body.charAt(0)) + body.substring(1);
    }

    private List<String> splitPreservingWhitespace(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean whitespace = false;
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            boolean nowWhitespace = Character.isWhitespace(character);
            if (current.isEmpty()) {
                whitespace = nowWhitespace;
            } else if (whitespace != nowWhitespace) {
                tokens.add(current.toString());
                current.setLength(0);
                whitespace = nowWhitespace;
            }
            current.append(character);
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private record TokenRender(String plain, Component component) {
    }
}
