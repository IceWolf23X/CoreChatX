package me.icewolf23.chatbloom.paper.command;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.icewolf23.chatbloom.paper.data.PlayerSettings;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;

public final class SocialSpyCommand implements TabExecutor {

    private final ChatBloom plugin;

    public SocialSpyCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
            return true;
        }
        if (!player.hasPermission("chatbloom.command.socialspy")) {
            player.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
            return true;
        }
        if (args.length != 0) {
            player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/socialspy")));
            return true;
        }
        PlayerSettings settings = plugin.playerData().get(player.getUniqueId());
        settings.setSocialSpyEnabled(!settings.isSocialSpyEnabled());
        plugin.playerData().save(player.getUniqueId());
        player.sendMessage(plugin.formats().configMessage(settings.isSocialSpyEnabled() ? "private-messages.socialspy-on" : "private-messages.socialspy-off", player));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
