package me.icewolf23.chatbloom.paper.command;

import icewolf23x.chatBloom.ChatBloom;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class UnmuteCommand implements TabExecutor {

    private final ChatBloom plugin;

    public UnmuteCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatbloom.command.unmute")) {
            sender.sendMessage(plugin.formats().configMessage("errors.no-permission", sender instanceof Player player ? player : null));
            return true;
        }
        if (!plugin.configs().moderation().getBoolean("mute.enabled", true)) {
            sender.sendMessage(plugin.formats().configMessage("moderation.mute-disabled", sender instanceof Player player ? player : null));
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(plugin.formats().configMessage("errors.invalid-usage", sender instanceof Player player ? player : null, Placeholder.unparsed("usage", "/unmute <player>")));
            return true;
        }
        OfflinePlayer target = lookupPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.formats().configMessage("errors.player-not-found", sender instanceof Player player ? player : null));
            return true;
        }
        plugin.services().moderationService().unmute(target.getUniqueId());
        sender.sendMessage(plugin.formats().configMessage("moderation.unmute-success", sender instanceof Player player ? player : null, Placeholder.unparsed("target_name", target.getName() == null ? args[0] : target.getName())));
        return true;
    }

    private OfflinePlayer lookupPlayer(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return online;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        return offline.hasPlayedBefore() || offline.isOnline() ? offline : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }
}
