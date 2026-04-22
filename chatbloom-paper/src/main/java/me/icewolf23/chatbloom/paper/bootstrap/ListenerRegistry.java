package me.icewolf23.chatbloom.paper.bootstrap;

import me.icewolf23.chatbloom.paper.listener.ChatListener;
import me.icewolf23.chatbloom.paper.listener.ConnectionListener;
import me.icewolf23.chatbloom.paper.listener.InventoryPreviewListener;
import me.icewolf23.chatbloom.paper.gui.SettingsMenuListener;

public final class ListenerRegistry {

    private final ChatBloomPaperPlugin plugin;

    public ListenerRegistry(ChatBloomPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        var pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new ChatListener(plugin), plugin);
        pluginManager.registerEvents(new ConnectionListener(plugin), plugin);
        pluginManager.registerEvents(new InventoryPreviewListener(plugin), plugin);
        pluginManager.registerEvents(new SettingsMenuListener(plugin.repositories().playerStateRepository(), plugin.services().settingsMenuFactory()), plugin);
    }
}
