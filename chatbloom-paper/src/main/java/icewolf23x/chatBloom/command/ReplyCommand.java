package icewolf23x.chatBloom.command;

import icewolf23x.chatBloom.ChatBloom;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public final class ReplyCommand implements TabExecutor {

    private final ChatBloom plugin;

    public ReplyCommand(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.formats().configMessage("errors.players-only", null));
            return true;
        }
        if (!player.hasPermission("chatbloom.command.reply")) {
            player.sendMessage(plugin.formats().configMessage("errors.no-permission", player));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(plugin.formats().configMessage("errors.invalid-usage", player, Placeholder.unparsed("usage", "/reply <message>")));
            return true;
        }
        plugin.privateMessages().sendReply(player, String.join(" ", Arrays.asList(args)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
