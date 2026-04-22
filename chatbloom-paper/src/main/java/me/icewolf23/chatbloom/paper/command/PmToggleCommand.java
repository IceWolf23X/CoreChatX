package me.icewolf23.chatbloom.paper.command;

import me.icewolf23.chatbloom.paper.ChatBloom;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class PmToggleCommand implements TabExecutor {

    private final ChatBloom plugin;

    public PmToggleCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
            return true;
        }
        if (!player.hasPermission("chatbloom.command.pmtoggle")) {
            player.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/pmtoggle")));
            return true;
        }
        boolean enabled = !plugin.services().privacyService().isPmEnabled(player.getUniqueId());
        plugin.services().privacyService().setPmEnabled(player.getUniqueId(), enabled);
        player.sendMessage(plugin.formats().configMessage("privacy.pm-toggled", player, Placeholder.component("state", plugin.formats().configMessage(enabled ? "ping.state-on" : "ping.state-off", player))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
