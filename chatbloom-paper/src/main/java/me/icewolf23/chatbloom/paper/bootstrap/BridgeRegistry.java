package me.icewolf23.chatbloom.paper.bootstrap;
import me.icewolf23.chatbloom.common.channel.ChannelScope;
import me.icewolf23.chatbloom.common.config.DeploymentMode;
import me.icewolf23.chatbloom.paper.network.PaperNetworkBridge;
import org.bukkit.Bukkit;

public final class BridgeRegistry {

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
        String serverId = configRegistry.deployment().serverId();
        if (serverId.isEmpty()) {
            serverId = "paper-backend";
            plugin.getLogger().warning("deployment.proxy.server-id is blank. Falling back to '" + serverId + "'.");
        }
        String proxyChannel = configRegistry.deployment().networkChannel();
        PaperNetworkBridge bridge = new PaperNetworkBridge(plugin, proxyMode, proxyChannel, serverId);
        serviceRegistry.networkBridge(bridge);
        serviceRegistry.bridgeServerId(serverId);
        if (proxyMode) {
            Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, proxyChannel);
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, proxyChannel, bridge);
            plugin.getLogger().info("Proxy mode active on Paper backend '" + serverId + "'.");
            plugin.getLogger().info("ChatBloom proxy channel active: '" + proxyChannel + "'.");
            plugin.getLogger().info("ChatBloom proxy routing enabled for NETWORK channels and cross-server PMs.");
            boolean hasNetworkChannel = serviceRegistry.channelService().channels().stream()
                .anyMatch(channel -> channel.enabled() && channel.scope() == ChannelScope.NETWORK);
            if (!hasNetworkChannel) {
                plugin.getLogger().warning("Proxy mode is enabled, but no channel is configured with scope NETWORK. Cross-server chat fanout will remain inactive until at least one enabled NETWORK channel exists.");
            }
        } else {
            plugin.getLogger().info("Standalone mode active.");
        }
    }

    public void reload() {
        initialize();
    }

    public void shutdown() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin);
    }
}
