package me.icewolf23.chatbloom.paper.command;

import me.icewolf23.chatbloom.paper.ChatBloom;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public final class MuteChatCommand implements TabExecutor {

    private final ChatBloom plugin;

    public MuteChatCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatbloom.command.mutechat")) {
            sender.sendMessage(plugin.formats().configMessage("errors.no-permission", sender instanceof Player player ? player : null));
            return true;
        }
        if (!plugin.configs().moderation().getBoolean("mutechat.enabled", true)) {
            sender.sendMessage(plugin.formats().configMessage("moderation.mutechat-disabled", sender instanceof Player player ? player : null));
            return true;
        }
        if (args.length != 0) {
            sender.sendMessage(plugin.formats().configMessage("errors.invalid-usage", sender instanceof Player player ? player : null, Placeholder.unparsed("usage", "/mutechat")));
            return true;
        }
        boolean muted = !plugin.services().moderationService().isChatMuted();
        plugin.services().moderationService().setChatMuted(muted);
        sender.sendMessage(plugin.formats().configMessage("moderation.chat-muted-toggled", sender instanceof Player player ? player : null, Placeholder.component("state", plugin.formats().configMessage(muted ? "ping.state-on" : "ping.state-off", sender instanceof Player player ? player : null))));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
