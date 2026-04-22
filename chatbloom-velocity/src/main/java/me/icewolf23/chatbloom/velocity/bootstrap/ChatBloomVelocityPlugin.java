package me.icewolf23.chatbloom.velocity.bootstrap;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import me.icewolf23.chatbloom.common.network.ChatMessagePacket;
import me.icewolf23.chatbloom.common.network.DecodedProxyMessage;
import me.icewolf23.chatbloom.common.network.PluginMessageCodec;
import me.icewolf23.chatbloom.common.network.PrivateMessagePacket;
import me.icewolf23.chatbloom.common.network.PrivateMessageResultPacket;
import me.icewolf23.chatbloom.common.network.ProxyMessageType;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

@Plugin(
    id = "chatbloom",
    name = "ChatBloom",
    version = "2026.1.0"
)
public final class ChatBloomVelocityPlugin {

    private static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("chatbloom", "main");

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public ChatBloomVelocityPlugin(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        proxyServer.getChannelRegistrar().register(CHANNEL);
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
            DecodedProxyMessage decoded = PluginMessageCodec.decode(event.getData());
            switch (decoded.type()) {
                case NETWORK_CHAT_FORWARD -> routeNetworkChat(decoded.chatMessagePacket());
                case PM_REQUEST -> routePrivateMessageRequest(decoded.privateMessagePacket());
                case PM_RESULT_TO_SOURCE -> routePrivateMessageResult(decoded.privateMessageResultPacket());
                default -> {
                }
            }
        } catch (Exception exception) {
            logger.warn("Failed to route ChatBloom proxy message from {}: {}", sourceConnection.getServerInfo().getName(), exception.getMessage());
        }
    }

    public ProxyServer proxyServer() {
        return proxyServer;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    private void routeNetworkChat(ChatMessagePacket packet) {
        byte[] payload = PluginMessageCodec.encodeChat(ProxyMessageType.NETWORK_CHAT_DELIVER, packet);
        for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
            if (registeredServer.getServerInfo().getName().equalsIgnoreCase(packet.serverId())) {
                continue;
            }
            if (registeredServer.getPlayersConnected().isEmpty()) {
                continue;
            }
            registeredServer.sendPluginMessage(CHANNEL, payload);
        }
    }

    private void routePrivateMessageRequest(PrivateMessagePacket packet) {
        Optional<Player> target = packet.targetId() != null
            ? proxyServer.getPlayer(packet.targetId())
            : proxyServer.getPlayer(packet.targetName());
        if (target.isEmpty()) {
            sendPrivateMessageResult(new PrivateMessageResultPacket(
                packet.requestId(),
                packet.sourceServerId(),
                packet.senderId(),
                packet.targetId(),
                packet.targetName(),
                packet.plainText(),
                false,
                "errors.player-not-found",
                Instant.now()
            ));
            return;
        }

        Optional<ServerConnection> targetConnection = target.get().getCurrentServer();
        if (targetConnection.isEmpty()) {
            sendPrivateMessageResult(new PrivateMessageResultPacket(
                packet.requestId(),
                packet.sourceServerId(),
                packet.senderId(),
                target.get().getUniqueId(),
                target.get().getUsername(),
                packet.plainText(),
                false,
                "errors.player-not-found",
                Instant.now()
            ));
            return;
        }

        PrivateMessagePacket deliverPacket = new PrivateMessagePacket(
            packet.requestId(),
            packet.sourceServerId(),
            packet.senderId(),
            target.get().getUniqueId(),
            packet.senderName(),
            target.get().getUsername(),
            packet.plainText(),
            packet.senderBypass(),
            packet.sentAt()
        );
        targetConnection.get().getServer().sendPluginMessage(CHANNEL, PluginMessageCodec.encodePrivateMessage(ProxyMessageType.PM_DELIVER_TO_TARGET, deliverPacket));
    }

    private void routePrivateMessageResult(PrivateMessageResultPacket packet) {
        sendPrivateMessageResult(packet);
    }

    private void sendPrivateMessageResult(PrivateMessageResultPacket packet) {
        proxyServer.getServer(packet.sourceServerId()).ifPresent(server ->
            server.sendPluginMessage(CHANNEL, PluginMessageCodec.encodePrivateMessageResult(packet))
        );
    }
}
