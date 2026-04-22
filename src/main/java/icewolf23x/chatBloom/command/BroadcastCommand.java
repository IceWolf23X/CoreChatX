package icewolf23x.chatBloom.command;

import icewolf23x.chatBloom.ChatBloom;
import icewolf23x.chatBloom.model.ProcessedMessage;
import icewolf23x.chatBloom.util.TextSanitizer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.List;

public final class BroadcastCommand implements TabExecutor {

    private final ChatBloom plugin;

    public BroadcastCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatbloom.command.broadcast")) {
            sender.sendMessage(plugin.formats().configMessage("errors.no-permission", sender instanceof org.bukkit.entity.Player player ? player : null));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(plugin.formats().configMessage("errors.invalid-usage", sender instanceof org.bukkit.entity.Player player ? player : null, Placeholder.unparsed("usage", "/broadcast <message>")));
            return true;
        }
        String sanitized = plugin.legacyFormatting().sanitizeForSender(sender, String.join(" ", Arrays.asList(args)));
        if (sanitized.trim().isEmpty()) {
            sender.sendMessage(plugin.formats().configMessage("errors.empty-message", sender instanceof org.bukkit.entity.Player player ? player : null));
            return true;
        }
        ProcessedMessage processed = plugin.chatService().processSanitizedMessage(sender, sanitized, false);
        if (processed.plainText().trim().isEmpty()) {
            sender.sendMessage(plugin.formats().configMessage("errors.empty-message", sender instanceof org.bukkit.entity.Player player ? player : null));
            return true;
        }
        Component broadcast = plugin.formats().broadcast(sender, sender.getName(), processed.component());
        Bukkit.getOnlinePlayers().forEach(target -> target.sendMessage(broadcast));
        for (var target : processed.notificationTargets()) {
            boolean bypass = processed.bypassTargets().contains(target);
            plugin.notifications().notifyMention(sender.getName(), target, bypass);
        }
        Bukkit.getConsoleSender().sendMessage(broadcast);
        if (plugin.configuration().main().getBoolean("logging.broadcasts", true)) {
            plugin.getLogger().info("[BROADCAST] " + PlainTextComponentSerializer.plainText().serialize(broadcast));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
