package me.icewolf23.chatbloom.paper.service;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.icewolf23.chatbloom.paper.model.ChatItemType;
import me.icewolf23.chatbloom.paper.util.TemplatePlaceholderNormalizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class FormatService {

    private final ChatBloom plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private Component messagePrefix = Component.empty();

    public FormatService(ChatBloom plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.messagePrefix = miniMessage.deserialize(
            TemplatePlaceholderNormalizer.normalize(plugin.configuration().messages().getString("prefix", "<gray>[ChatBloom]</gray>"))
        );
    }

    public Component messagePrefix() {
        return messagePrefix;
    }

    public Component configMessage(String path, Player player, TagResolver... resolvers) {
        String template = plugin.configuration().messages().getString(path, "{prefix}");
        return deserialize(template, player, resolvers);
    }

    public Component publicChat(Player player, Component message) {
        return publicChat(player, message, "global");
    }

    public Component publicChat(Player player, Component message, String channelId) {
        Component pluginPrefix = deserialize(
            plugin.configuration().chat().getString("public-chat.plugin-prefix", ""),
            player
        );
        Component channelPrefix = Component.empty();
        if (channelId != null && !channelId.isBlank() && !"global".equalsIgnoreCase(channelId)) {
            channelPrefix = deserialize(
                plugin.configuration().chat().getString("public-chat.channel-prefix-format", "<dark_gray>[</dark_gray><white>{channel_name}</white><dark_gray>]</dark_gray> "),
                player,
                Placeholder.unparsed("channel_name", channelId)
            );
        }
        Component rankPrefix = plugin.hooks().hasLuckPerms()
            ? plugin.hooks().groupPrefix(player)
            : deserialize(plugin.configuration().chat().getString("public-chat.fallback-rank-prefix", ""), player);
        String template = plugin.configuration().chat().getString("public-chat.format", "{player_name}: {message}");
        return deserialize(
            template,
            player,
            Placeholder.component("plugin_prefix", pluginPrefix),
            Placeholder.component("channel_prefix", channelPrefix),
            Placeholder.component("rank_prefix", rankPrefix),
            Placeholder.unparsed("player_name", player.getName()),
            Placeholder.component("message", message)
        );
    }

    public Component publicChatRemote(String senderName, String rankPrefixTemplate, Component message, String channelId) {
        Component pluginPrefix = deserialize(
            plugin.configuration().chat().getString("public-chat.plugin-prefix", ""),
            null
        );
        Component channelPrefix = Component.empty();
        if (channelId != null && !channelId.isBlank() && !"global".equalsIgnoreCase(channelId)) {
            channelPrefix = deserialize(
                plugin.configuration().chat().getString("public-chat.channel-prefix-format", "<dark_gray>[</dark_gray><white>{channel_name}</white><dark_gray>]</dark_gray> "),
                null,
                Placeholder.unparsed("channel_name", channelId)
            );
        }
        Component rankPrefix = rankPrefixTemplate == null || rankPrefixTemplate.isBlank()
            ? Component.empty()
            : LegacyComponentSerializer.legacyAmpersand().deserialize(rankPrefixTemplate);
        String template = plugin.configuration().chat().getString("public-chat.format", "{player_name}: {message}");
        return deserialize(
            template,
            null,
            Placeholder.component("plugin_prefix", pluginPrefix),
            Placeholder.component("channel_prefix", channelPrefix),
            Placeholder.component("rank_prefix", rankPrefix),
            Placeholder.unparsed("player_name", senderName),
            Placeholder.component("message", message)
        );
    }

    public Component mentionToken(Player viewer, String name) {
        String template = plugin.configuration().chat().getString("mentions.token-format", "<yellow>@{player_name}</yellow>");
        return deserialize(template, viewer, Placeholder.unparsed("player_name", name));
    }

    public String rankPrefixTemplate(Player player) {
        Component rankPrefix = plugin.hooks().hasLuckPerms()
            ? plugin.hooks().groupPrefix(player)
            : deserialize(plugin.configuration().chat().getString("public-chat.fallback-rank-prefix", ""), player);
        return LegacyComponentSerializer.legacyAmpersand().serialize(rankPrefix);
    }

    public Component customPingToken(Player viewer, String format, String trigger) {
        return deserialize(format, viewer, Placeholder.unparsed("trigger", trigger));
    }

    public Component chatItemToken(Player sender, String format, String label) {
        return deserialize(format, sender, Placeholder.unparsed("token", label));
    }

    public Component broadcast(Player sender, Component message) {
        return broadcast(sender, sender.getName(), message);
    }

    public Component broadcast(CommandSender sender, String senderName, Component message) {
        return deserialize(
            plugin.configuration().messages().getString("broadcast.format", "{message}"),
            sender instanceof Player player ? player : null,
            Placeholder.unparsed("sender_name", senderName),
            Placeholder.component("message", message)
        );
    }

    public Component privateMessageToSender(Player sender, Player target, Component message) {
        return privateMessageToSender(sender, sender.getName(), target, message);
    }

    public Component privateMessageToSender(CommandSender sender, String senderName, Player target, Component message) {
        return privateMessageToSender(sender, senderName, target.getName(), message);
    }

    public Component privateMessageToSender(CommandSender sender, String senderName, String targetName, Component message) {
        return deserialize(
            plugin.configuration().messages().getString("private-messages.to-sender", "{message}"),
            sender instanceof Player player ? player : null,
            Placeholder.unparsed("sender_name", senderName),
            Placeholder.unparsed("target_name", targetName),
            Placeholder.component("message", message)
        );
    }

    public Component privateMessageToTarget(Player sender, Player target, Component message) {
        return privateMessageToTarget(sender.getName(), target, message);
    }

    public Component privateMessageToTarget(String senderName, Player target, Component message) {
        return deserialize(
            plugin.configuration().messages().getString("private-messages.to-target", "{message}"),
            target,
            Placeholder.unparsed("sender_name", senderName),
            Placeholder.unparsed("target_name", target.getName()),
            Placeholder.component("message", message)
        );
    }

    public Component spyMessage(Player sender, Player target, Component message) {
        return spyMessage(sender.getName(), target, message);
    }

    public Component spyMessage(String senderName, Player target, Component message) {
        return spyMessage(senderName, target.getName(), message);
    }

    public Component spyMessage(String senderName, String targetName, Component message) {
        return deserialize(
            plugin.configuration().messages().getString("private-messages.spy", "{message}"),
            null,
            Placeholder.unparsed("sender_name", senderName),
            Placeholder.unparsed("target_name", targetName),
            Placeholder.component("message", message)
        );
    }

    public Component join(Player player) {
        return deserialize(
            plugin.configuration().messages().getString("join-quit.join", "{player_name}"),
            player,
            Placeholder.unparsed("player_name", player.getName())
        );
    }

    public Component quit(Player player) {
        return deserialize(
            plugin.configuration().messages().getString("join-quit.quit", "{player_name}"),
            player,
            Placeholder.unparsed("player_name", player.getName())
        );
    }

    public Component firstJoin(Player player, int count) {
        return deserialize(
            plugin.configuration().messages().getString("join-quit.first-join", "{player_name}"),
            player,
            Placeholder.unparsed("player_name", player.getName()),
            Placeholder.unparsed("count", Integer.toString(count))
        );
    }

    public Component previewTitle(String path, Player owner) {
        return deserialize(
            plugin.configuration().chatItems().getString(path, owner.getName()),
            owner,
            Placeholder.unparsed("player_name", owner.getName())
        );
    }

    public Component previewTitle(ChatItemType type, Player player, String ownerName) {
        String path = switch (type) {
            case ITEM -> "previews.titles.item";
            case SHULKER -> "previews.titles.shulker";
            case ARMOR -> "previews.titles.armor";
            case HOTBAR -> "previews.titles.hotbar";
            case INVENTORY -> "previews.titles.inventory";
            case ENDERCHEST -> "previews.titles.enderchest";
        };
        return deserialize(
            plugin.configuration().chatItems().getString(path, ownerName),
            player,
            Placeholder.unparsed("player_name", ownerName)
        );
    }

    public Component legacyAware(String raw) {
        if (raw == null || raw.isBlank()) {
            return Component.empty();
        }
        if (raw.indexOf('&') >= 0) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        }
        return miniMessage.deserialize(raw);
    }

    public Component deserialize(String template, Player player, TagResolver... resolvers) {
        if (template == null || template.isBlank()) {
            return Component.empty();
        }
        String parsedTemplate = plugin.hooks().applyPlaceholders(player, TemplatePlaceholderNormalizer.normalize(template));
        return miniMessage.deserialize(parsedTemplate, mergeWithPrefix(resolvers));
    }

    private TagResolver mergeWithPrefix(TagResolver... resolvers) {
        List<TagResolver> allResolvers = new ArrayList<>();
        allResolvers.add(Placeholder.component("prefix", messagePrefix));
        for (TagResolver resolver : resolvers) {
            allResolvers.add(resolver);
        }
        return TagResolver.resolver(allResolvers);
    }
}
