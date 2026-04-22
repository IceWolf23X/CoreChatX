package me.icewolf23.chatbloom.paper.bootstrap;

import icewolf23x.chatBloom.ChatBloom;
import me.icewolf23.chatbloom.common.config.DeploymentMode;
import me.icewolf23.chatbloom.paper.network.PaperNetworkBridge;

public final class BridgeRegistry {

    private final ChatBloom plugin;
    private final ConfigRegistry configRegistry;
    private final ServiceRegistry serviceRegistry;

    public BridgeRegistry(ChatBloom plugin, ConfigRegistry configRegistry, ServiceRegistry serviceRegistry) {
        this.plugin = plugin;
        this.configRegistry = configRegistry;
        this.serviceRegistry = serviceRegistry;
    }

    public void initialize() {
        boolean proxyMode = configRegistry.deployment().mode() == DeploymentMode.PROXY;
        serviceRegistry.networkBridge(new PaperNetworkBridge(false));
        if (proxyMode) {
            plugin.getLogger().warning("Proxy mode is configured, but the Paper transport is still a stub in Run 1. Standalone-safe behavior remains active until proxy transport is implemented.");
        } else {
            plugin.getLogger().info("Standalone mode active.");
        }
    }

    public void reload() {
        initialize();
    }
}
