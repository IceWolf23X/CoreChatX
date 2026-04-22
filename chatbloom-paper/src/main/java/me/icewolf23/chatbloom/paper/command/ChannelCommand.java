package me.icewolf23.chatbloom.paper.command;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.icewolf23.chatbloom.common.channel.ChatChannel;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ChannelCommand implements TabExecutor {

    private final ChatBloom plugin;

    public ChannelCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
            return true;
        }
        if (!player.hasPermission("chatbloom.command.channel")) {
            player.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            showChannelList(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/channel set <id>")));
                return true;
            }
            switchChannel(player, args[1]);
            return true;
        }
        player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/channel <list|set <id>>")));
        return true;
    }

    private void showChannelList(Player player) {
        String active = plugin.services().channelService().getActiveChannel(player.getUniqueId());
        List<String> available = plugin.services().channelService().channels().stream()
            .filter(ChatChannel::enabled)
            .filter(channel -> plugin.services().channelAudienceResolver().canSend(player, channel))
            .sorted(Comparator.comparing(ChatChannel::id, String.CASE_INSENSITIVE_ORDER))
            .map(channel -> channel.id().equalsIgnoreCase(active) ? "*" + channel.id() : channel.id())
            .toList();
        player.sendMessage(plugin.formats().configMessage("channels.list", player, Placeholder.unparsed("channels", String.join(", ", available))));
    }

    private void switchChannel(Player player, String requestedChannel) {
        ChatChannel channel = plugin.services().channelService().findChannel(requestedChannel).orElse(null);
        if (channel == null) {
            player.sendMessage(plugin.formats().configMessage("channels.not-found", player, Placeholder.unparsed("channel_name", requestedChannel)));
            return;
        }
        if (!channel.enabled()) {
            player.sendMessage(plugin.formats().configMessage("channels.disabled", player, Placeholder.unparsed("channel_name", channel.id())));
            return;
        }
        if (!plugin.services().channelAudienceResolver().canSend(player, channel)) {
            player.sendMessage(plugin.formats().configMessage("channels.no-send-permission", player, Placeholder.unparsed("channel_name", channel.id())));
            return;
        }
        plugin.services().channelService().setActiveChannel(player.getUniqueId(), channel.id());
        player.sendMessage(plugin.formats().configMessage("channels.switched", player, Placeholder.unparsed("channel_name", channel.id())));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!(sender instanceof Player player)) {
            return suggestions;
        }
        if (args.length == 1) {
            complete("list", args[0], suggestions);
            complete("set", args[0], suggestions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            for (ChatChannel channel : plugin.services().channelService().channels()) {
                if (!channel.enabled()) {
                    continue;
                }
                if (!plugin.services().channelAudienceResolver().canSend(player, channel)) {
                    continue;
                }
                complete(channel.id(), args[1], suggestions);
            }
        }
        return suggestions;
    }

    private void complete(String value, String partial, List<String> suggestions) {
        if (value.toLowerCase(Locale.ROOT).startsWith(partial.toLowerCase(Locale.ROOT))) {
            suggestions.add(value);
        }
    }
}
