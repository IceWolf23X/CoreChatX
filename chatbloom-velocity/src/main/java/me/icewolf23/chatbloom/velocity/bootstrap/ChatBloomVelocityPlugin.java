package me.icewolf23.chatbloom.velocity.bootstrap;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.icewolf23.chatbloom.common.network.DecodedProxyMessage;
import me.icewolf23.chatbloom.common.network.PluginMessageCodec;
import me.icewolf23.chatbloom.velocity.network.ProxyRoutingService;
import org.slf4j.Logger;

@Plugin(
    id = "chatbloom",
    name = "ChatBloom",
    version = "2026.1.0"
)
public final class ChatBloomVelocityPlugin {

    private static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("chatbloom", "main");

    private final ProxyServer proxyServer;
    private final Logger logger;
    private ProxyRoutingService routingService;

    @Inject
    public ChatBloomVelocityPlugin(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        proxyServer.getChannelRegistrar().register(CHANNEL);
        this.routingService = new ProxyRoutingService(proxyServer, logger, CHANNEL);
        logger.info("ChatBloom Velocity routing initialized on channel {}.", CHANNEL.getId());
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection sourceConnection)) {
            return;
        }
        try {
            if (routingService == null) {
                logger.warn("Ignoring ChatBloom proxy message because routing service is not initialized yet.");
                return;
            }
            DecodedProxyMessage decoded = PluginMessageCodec.decode(event.getData());
            routingService.handle(decoded);
        } catch (Exception exception) {
            logger.warn("Failed to route ChatBloom proxy message from {}: {}", sourceConnection.getServerInfo().getName(), exception.getMessage());
        }
    }
}
