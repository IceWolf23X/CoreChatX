package me.icewolf23.chatbloom.paper.bootstrap;
import me.icewolf23.chatbloom.common.config.DeploymentMode;
import me.icewolf23.chatbloom.paper.network.PaperNetworkBridge;
import org.bukkit.Bukkit;

public final class BridgeRegistry {

    private static final String PROXY_CHANNEL = "chatbloom:main";

    private final ChatBloomPaperPlugin plugin;
    private final ConfigRegistry configRegistry;
    private final ServiceRegistry serviceRegistry;

    public BridgeRegistry(ChatBloomPaperPlugin plugin, ConfigRegistry configRegistry, ServiceRegistry serviceRegistry) {
        this.plugin = plugin;
        this.configRegistry = configRegistry;
        this.serviceRegistry = serviceRegistry;
    }

    public void initialize() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin);
        boolean proxyMode = configRegistry.deployment().mode() == DeploymentMode.PROXY;
        String serverId = configRegistry.configurationService().main().getString("deployment.proxy.server-id", "paper-backend");
        PaperNetworkBridge bridge = new PaperNetworkBridge(plugin, proxyMode, PROXY_CHANNEL, serverId);
        serviceRegistry.networkBridge(bridge);
        serviceRegistry.bridgeServerId(serverId);
        if (proxyMode) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, PROXY_CHANNEL);
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, PROXY_CHANNEL, bridge);
            plugin.getLogger().info("Proxy mode active on Paper backend '" + serverId + "' using channel '" + PROXY_CHANNEL + "'.");
        } else {
            plugin.getLogger().info("Standalone mode active.");
        }
    }

    public void reload() {
        initialize();
    }
}
