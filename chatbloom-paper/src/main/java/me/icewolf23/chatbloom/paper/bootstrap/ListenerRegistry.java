package me.icewolf23.chatbloom.paper.bootstrap;

import icewolf23x.chatBloom.ChatBloom;
import icewolf23x.chatBloom.listener.ChatListener;
import icewolf23x.chatBloom.listener.ConnectionListener;
import icewolf23x.chatBloom.listener.InventoryPreviewListener;

public final class ListenerRegistry {

    private final ChatBloom plugin;

    public ListenerRegistry(ChatBloom plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        var pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new ChatListener(plugin), plugin);
        pluginManager.registerEvents(new ConnectionListener(plugin), plugin);
        pluginManager.registerEvents(new InventoryPreviewListener(plugin), plugin);
    }
}
