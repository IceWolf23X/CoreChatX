package me.icewolf23.chatbloom.paper.command;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.icewolf23.chatbloom.paper.data.PlayerSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PingCommand implements TabExecutor {

    private final ChatBloom plugin;

    public PingCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
            return true;
        }
        if (!player.hasPermission("chatbloom.command.ping")) {
            player.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("/ping sound <on|off|toggle|status>");
            player.sendMessage("/ping actionbar <on|off|toggle|status>");
            return true;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        if (!mode.equals("sound") && !mode.equals("actionbar")) {
            player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/ping <sound|actionbar> [on|off|toggle|status]")));
            return true;
        }

        PlayerSettings settings = plugin.playerData().get(player.getUniqueId());
        boolean current = mode.equals("sound") ? settings.isPingSoundEnabled() : settings.isPingActionbarEnabled();
        String operation = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "toggle";
        boolean updated = switch (operation) {
            case "on" -> true;
            case "off" -> false;
            case "status" -> current;
            case "toggle" -> !current;
            default -> current;
        };
        if (!operation.equals("status") && !operation.equals("on") && !operation.equals("off") && !operation.equals("toggle")) {
            player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/ping <sound|actionbar> [on|off|toggle|status]")));
            return true;
        }

        if (mode.equals("sound")) {
            settings.setPingSoundEnabled(updated);
        } else {
            settings.setPingActionbarEnabled(updated);
        }
        if (!operation.equals("status")) {
            plugin.playerData().save(player.getUniqueId());
            plugin.playerData().reload();
            settings = plugin.playerData().get(player.getUniqueId());
        }
        boolean actualState = mode.equals("sound") ? settings.isPingSoundEnabled() : settings.isPingActionbarEnabled();
        Component state = plugin.formats().configMessage(actualState ? "ping.state-on" : "ping.state-off", player);
        if (operation.equals("status")) {
            player.sendMessage(plugin.formats().configMessage(
                "ping.toggles.status",
                player,
                Placeholder.unparsed("setting", mode),
                Placeholder.component("state", state)
            ));
        } else {
            player.sendMessage(plugin.formats().configMessage(
                mode.equals("sound") ? "ping.toggles.sound-changed" : "ping.toggles.actionbar-changed",
                player,
                Placeholder.component("state", state)
            ));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            complete("sound", args[0], suggestions);
            complete("actionbar", args[0], suggestions);
        } else if (args.length == 2) {
            complete("on", args[1], suggestions);
            complete("off", args[1], suggestions);
            complete("toggle", args[1], suggestions);
            complete("status", args[1], suggestions);
        }
        return suggestions;
    }

    private void complete(String value, String partial, List<String> suggestions) {
        if (value.toLowerCase(Locale.ROOT).startsWith(partial.toLowerCase(Locale.ROOT))) {
            suggestions.add(value);
        }
    }
}
