package me.icewolf23.chatbloom.paper.command;

import icewolf23x.chatBloom.ChatBloom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class ClearChatCommand implements TabExecutor {

    private final ChatBloom plugin;

    public ClearChatCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatbloom.command.clearchat")) {
            sender.sendMessage(plugin.formats().configMessage("errors.no-permission", sender instanceof Player player ? player : null));
            return true;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int index = 0; index < 100; index++) {
                player.sendMessage(Component.empty());
            }
            player.sendMessage(plugin.formats().configMessage("moderation.clear-chat-notice", player, Placeholder.unparsed("sender_name", sender.getName())));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
