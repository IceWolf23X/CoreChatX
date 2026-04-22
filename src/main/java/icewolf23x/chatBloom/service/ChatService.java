package icewolf23x.chatBloom.service;

import icewolf23x.chatBloom.ChatBloom;
import icewolf23x.chatBloom.model.CustomPingDefinition;
import icewolf23x.chatBloom.model.FilteredToken;
import icewolf23x.chatBloom.model.ProcessedMessage;
import icewolf23x.chatBloom.util.TokenDecoration;
import icewolf23x.chatBloom.util.TextSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ChatService {

    private final ChatBloom plugin;
    private final Map<String, CustomPingDefinition> customPings = new HashMap<>();
    private boolean publicChatEnabled;
    private boolean mentionsEnabled;
    private String noMessageSentPath = "chat.no-message-sent";

    public ChatService(ChatBloom plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.publicChatEnabled = plugin.configuration().chat().getBoolean("public-chat.enabled", true);
        this.mentionsEnabled = plugin.configuration().chat().getBoolean("mentions.enabled", true);
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

    public void handlePublicChat(Player sender, String rawMessage) {
        if (!publicChatEnabled) {
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

        ProcessedMessage rendered = processMessage(sender, sanitized);
        if (rendered.plainText().trim().isEmpty()) {
            sender.sendMessage(plugin.formats().configMessage(noMessageSentPath, sender));
            return;
        }

        plugin.cooldowns().markPublicAccepted(sender);

        Component finalMessage = plugin.formats().publicChat(sender, rendered.component());
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(finalMessage);
        }
        notifyTargets(sender.getName(), rendered.notificationTargets(), rendered.bypassTargets());
        logConsole(finalMessage);
    }

    public ProcessedMessage processMessage(Player sender, String rawMessage) {
        String sanitized = plugin.legacyFormatting().sanitizeForPlayer(sender, rawMessage);
        return processSanitizedMessage(sender, sanitized);
    }

    public ProcessedMessage processSanitizedMessage(Player sender, String sanitized) {
        return processSanitizedMessage(sender, sender, sanitized, true);
    }

    public ProcessedMessage processSanitizedMessage(CommandSender sender, String sanitized, boolean allowChatItems) {
        return processSanitizedMessage(sender, sender instanceof Player player ? player : null, sanitized, allowChatItems);
    }

    private ProcessedMessage processSanitizedMessage(CommandSender sender, Player playerContext, String sanitized, boolean allowChatItems) {
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

            TokenRender tokenRender = renderCoreToken(sender, playerContext, decorated.core(), allowChatItems, onlinePlayers, notificationTargets, bypassTargets);
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
        Map<String, Player> onlinePlayers,
        Set<Player> notificationTargets,
        Set<Player> bypassTargets
    ) {
        String plainCore = plugin.legacyFormatting().stripCodes(core);
        String normalized = plainCore.toLowerCase(Locale.ROOT);

        CustomPingDefinition customPing = customPings.get(normalized);
        if (customPing != null && isAllowedToUse(sender, customPing)) {
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

    private void notifyTargets(String senderName, Set<Player> targets, Set<Player> bypassTargets) {
        for (Player target : targets) {
            boolean bypass = bypassTargets.contains(target);
            plugin.notifications().notifyMention(senderName, target, bypass);
        }
    }

    private void logConsole(Component component) {
        if (!plugin.configuration().main().getBoolean("logging.public-chat", true)) {
            return;
        }
        plugin.getLogger().info("[CHAT] " + PlainTextComponentSerializer.plainText().serialize(component));
    }

    private boolean isAllowedToUse(CommandSender sender, CustomPingDefinition definition) {
        return definition.usePermission().isBlank() || sender.hasPermission(definition.usePermission());
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
