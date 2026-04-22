package me.icewolf23.chatbloom.paper.command;

import me.icewolf23.chatbloom.paper.ChatBloom;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ChatBloomCommand implements TabExecutor {

    private final ChatBloom plugin;

    public ChatBloomCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("chatbloom.command.chatbloom")) {
                sender.sendMessage(plugin.formats().configMessage("errors.no-permission", sender instanceof Player player ? player : null));
                return true;
            }
            sender.sendMessage("/chatbloom reload");
            sender.sendMessage("/chatbloom settings");
            sender.sendMessage("/chatbloom item <uuid>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chatbloom.command.reload")) {
                sender.sendMessage(plugin.formats().configMessage("errors.no-permission", sender instanceof Player player ? player : null));
                return true;
            }
            try {
                plugin.reloadPlugin();
                if (plugin.configuration().main().getBoolean("logging.reload", true)) {
                    plugin.getLogger().info("Configuration reloaded by " + sender.getName() + ".");
                }
                sender.sendMessage(plugin.formats().configMessage("reload.success", sender instanceof Player player ? player : null));
                if (plugin.configuration().main().getBoolean("reload.show-summary", true)) {
                    sender.sendMessage(plugin.formats().configMessage("reload.summary", sender instanceof Player player ? player : null));
                }
            } catch (Exception exception) {
                if (plugin.configuration().main().getBoolean("logging.errors", true)) {
                    plugin.getLogger().warning("Reload failed for " + sender.getName() + ": " + exception.getMessage());
                }
                sender.sendMessage(plugin.formats().configMessage("errors.invalid-usage", sender instanceof Player player ? player : null, Placeholder.unparsed("usage", "Reload failed: " + exception.getMessage())));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("settings")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
                return true;
            }
            if (!player.hasPermission("chatbloom.command.settings")) {
                player.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
                return true;
            }
            player.openInventory(plugin.services().settingsMenuFactory().create(plugin.repositories().playerStateRepository().load(player.getUniqueId())));
            return true;
        }

        if (args[0].equalsIgnoreCase("item")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/chatbloom item <uuid>")));
                return true;
            }
            try {
                UUID snapshotId = UUID.fromString(args[1]);
                plugin.chatItems().openSnapshot(player, snapshotId);
            } catch (IllegalArgumentException exception) {
                player.sendMessage(plugin.formats().configMessage("errors.invalid-chatitem-id", player));
            }
            return true;
        }

        if (!sender.hasPermission("chatbloom.command.chatbloom")) {
            sender.sendMessage(plugin.formats().configMessage("errors.no-permission", sender instanceof Player player ? player : null));
            return true;
        }
        sender.sendMessage(plugin.formats().configMessage("errors.invalid-usage", sender instanceof Player player ? player : null, Placeholder.unparsed("usage", "/chatbloom <reload|settings|item>")));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            complete("reload", args[0], suggestions);
            complete("settings", args[0], suggestions);
            complete("item", args[0], suggestions);
        }
        return suggestions;
    }

    private void complete(String value, String partial, List<String> suggestions) {
        if (value.toLowerCase(Locale.ROOT).startsWith(partial.toLowerCase(Locale.ROOT))) {
            suggestions.add(value);
        }
    }
}
