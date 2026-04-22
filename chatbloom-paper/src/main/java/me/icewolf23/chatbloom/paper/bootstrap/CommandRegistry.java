package me.icewolf23.chatbloom.paper.bootstrap;

import icewolf23x.chatBloom.command.BroadcastCommand;
import icewolf23x.chatBloom.command.ChatBloomCommand;
import icewolf23x.chatBloom.command.MessageCommand;
import icewolf23x.chatBloom.command.PingCommand;
import icewolf23x.chatBloom.command.ReplyCommand;
import icewolf23x.chatBloom.command.SocialSpyCommand;
import me.icewolf23.chatbloom.paper.command.ChannelCommand;
import me.icewolf23.chatbloom.paper.command.ChatSettingsCommand;
import me.icewolf23.chatbloom.paper.command.ClearChatCommand;
import me.icewolf23.chatbloom.paper.command.IgnoreCommand;
import me.icewolf23.chatbloom.paper.command.IgnoreListCommand;
import me.icewolf23.chatbloom.paper.command.MuteChatCommand;
import me.icewolf23.chatbloom.paper.command.MuteCommand;
import me.icewolf23.chatbloom.paper.command.PmToggleCommand;
import me.icewolf23.chatbloom.paper.command.UnignoreCommand;
import me.icewolf23.chatbloom.paper.command.UnmuteCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import java.util.Objects;

public final class CommandRegistry {

    private final ChatBloomPaperPlugin plugin;

    public CommandRegistry(ChatBloomPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        register("chatbloom", new ChatBloomCommand(plugin));
        register("msg", new MessageCommand(plugin));
        register("reply", new ReplyCommand(plugin));
        register("socialspy", new SocialSpyCommand(plugin));
        register("broadcast", new BroadcastCommand(plugin));
        register("ping", new PingCommand(plugin));
        register("channel", new ChannelCommand(plugin));
        register("ignore", new IgnoreCommand(plugin));
        register("unignore", new UnignoreCommand(plugin));
        register("ignorelist", new IgnoreListCommand(plugin));
        register("pmtoggle", new PmToggleCommand(plugin));
        register("chatsettings", new ChatSettingsCommand(plugin));
        register("mute", new MuteCommand(plugin));
        register("unmute", new UnmuteCommand(plugin));
        register("mutechat", new MuteChatCommand(plugin));
        register("clearchat", new ClearChatCommand(plugin));
    }

    private void register(String commandName, TabExecutor executor) {
        PluginCommand command = Objects.requireNonNull(plugin.getCommand(commandName), "Missing command " + commandName);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
