package me.icewolf23.chatbloom.paper.command;

import icewolf23x.chatBloom.ChatBloom;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MuteCommand implements TabExecutor {

    private static final Pattern DURATION = Pattern.compile("^(\\d+)([smhd])$", Pattern.CASE_INSENSITIVE);

    private final ChatBloom plugin;

    public MuteCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatbloom.command.mute")) {
            sender.sendMessage(plugin.formats().configMessage("errors.no-permission", sender instanceof Player player ? player : null));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(plugin.formats().configMessage("errors.invalid-usage", sender instanceof Player player ? player : null, Placeholder.unparsed("usage", "/mute <player> [duration] [reason...]")));
            return true;
        }
        OfflinePlayer target = lookupPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.formats().configMessage("errors.player-not-found", sender instanceof Player player ? player : null));
            return true;
        }
        Instant expiresAt = null;
        int reasonStart = 1;
        if (args.length >= 2) {
            Instant parsed = parseDuration(args[1]);
            if (parsed != null) {
                expiresAt = parsed;
                reasonStart = 2;
            }
        }
        String reason = reasonStart < args.length
            ? String.join(" ", Arrays.copyOfRange(args, reasonStart, args.length))
            : plugin.configs().moderation().getString("mute.default-reason", "No reason provided");
        UUID actorId = sender instanceof Player player ? player.getUniqueId() : new UUID(0L, 0L);
        plugin.services().moderationService().mute(target.getUniqueId(), expiresAt, reason, actorId);
        String name = target.getName() == null ? args[0] : target.getName();
        sender.sendMessage(plugin.formats().configMessage("moderation.mute-success", sender instanceof Player player ? player : null, Placeholder.unparsed("target_name", name), Placeholder.unparsed("reason", reason)));
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

    private Instant parseDuration(String token) {
        Matcher matcher = DURATION.matcher(token);
        if (!matcher.matches()) {
            return null;
        }
        long amount = Long.parseLong(matcher.group(1));
        long millis = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "s" -> amount * 1000L;
            case "m" -> amount * 60_000L;
            case "h" -> amount * 3_600_000L;
            case "d" -> amount * 86_400_000L;
            default -> 0L;
        };
        return millis <= 0L ? null : Instant.ofEpochMilli(System.currentTimeMillis() + millis);
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
