package me.icewolf23.chatbloom.paper.command;

import me.icewolf23.chatbloom.paper.ChatBloom;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class MessageCommand implements TabExecutor {

    private final ChatBloom plugin;

    public MessageCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player onlinePlayer ? onlinePlayer : null;
        if (!sender.hasPermission("chatbloom.command.msg")) {
            sender.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/msg <player> <message>")));
            return true;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.privateMessages().sendPrivateMessage(sender, sender.getName(), args[0], message);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                .sorted()
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
