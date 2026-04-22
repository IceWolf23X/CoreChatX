package me.icewolf23.chatbloom.paper.bootstrap;

import icewolf23x.chatBloom.command.BroadcastCommand;
import icewolf23x.chatBloom.command.ChatBloomCommand;
import icewolf23x.chatBloom.command.MessageCommand;
import icewolf23x.chatBloom.command.PingCommand;
import icewolf23x.chatBloom.command.ReplyCommand;
import icewolf23x.chatBloom.command.SocialSpyCommand;
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
    }

    private void register(String commandName, TabExecutor executor) {
        PluginCommand command = Objects.requireNonNull(plugin.getCommand(commandName), "Missing command " + commandName);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
