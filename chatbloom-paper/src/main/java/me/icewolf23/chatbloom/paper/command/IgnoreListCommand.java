package me.icewolf23.chatbloom.paper.command;

import me.icewolf23.chatbloom.paper.ChatBloom;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class IgnoreListCommand implements TabExecutor {

    private final ChatBloom plugin;

    public IgnoreListCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
            return true;
        }
        if (!player.hasPermission("chatbloom.command.ignorelist")) {
            player.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
            return true;
        }
        if (!plugin.configs().privacy().getBoolean("ignore.enabled", true)) {
            player.sendMessage(plugin.formats().configMessage("privacy.ignore-disabled", player));
            return true;
        }
        List<String> names = plugin.services().privacyService().ignoredPlayers(player.getUniqueId()).stream()
            .map(this::nameOf)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        String rendered = names.isEmpty() ? "none" : String.join(", ", names);
        player.sendMessage(plugin.formats().configMessage("privacy.ignore-list", player, Placeholder.unparsed("targets", rendered)));
        return true;
    }

    private String nameOf(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        return player.getName() == null ? uuid.toString() : player.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
