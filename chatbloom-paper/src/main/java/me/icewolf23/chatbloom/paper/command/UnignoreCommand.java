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

public final class UnignoreCommand implements TabExecutor {

    private final ChatBloom plugin;

    public UnignoreCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
            return true;
        }
        if (!player.hasPermission("chatbloom.command.unignore")) {
            player.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/unignore <player>")));
            return true;
        }
        OfflinePlayer target = lookupPlayer(args[0]);
        if (target == null) {
            player.sendMessage(plugin.formats().configMessage("errors.player-not-found", player));
            return true;
        }
        if (!plugin.services().privacyService().isIgnoring(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(plugin.formats().configMessage("privacy.not-ignoring", player, Placeholder.unparsed("target_name", target.getName() == null ? args[0] : target.getName())));
            return true;
        }
        plugin.services().privacyService().removeIgnore(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(plugin.formats().configMessage("privacy.ignore-removed", player, Placeholder.unparsed("target_name", target.getName() == null ? args[0] : target.getName())));
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
        if (!(sender instanceof Player) || args.length != 1) {
            return Collections.emptyList();
        }
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }
}
