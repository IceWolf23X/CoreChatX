package me.icewolf23.chatbloom.velocity.network;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import me.icewolf23.chatbloom.common.network.ChatMessagePacket;
import me.icewolf23.chatbloom.common.network.DecodedProxyMessage;
import me.icewolf23.chatbloom.common.network.PluginMessageCodec;
import me.icewolf23.chatbloom.common.network.PrivateMessagePacket;
import me.icewolf23.chatbloom.common.network.PrivateMessageResultPacket;
import me.icewolf23.chatbloom.common.network.ProxyMessageType;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Optional;

public final class ProxyRoutingService {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final ChannelIdentifier channel;

    public ProxyRoutingService(ProxyServer proxyServer, Logger logger, ChannelIdentifier channel) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.channel = channel;
    }

    public void handle(DecodedProxyMessage decoded) {
        switch (decoded.type()) {
            case NETWORK_CHAT_FORWARD -> routeNetworkChat(decoded.chatMessagePacket());
            case PM_REQUEST -> routePrivateMessageRequest(decoded.privateMessagePacket());
            case PM_RESULT_TO_SOURCE -> routePrivateMessageResult(decoded.privateMessageResultPacket());
            default -> {
            }
        }
    }

    public void routeNetworkChat(ChatMessagePacket packet) {
        byte[] payload = PluginMessageCodec.encodeChat(ProxyMessageType.NETWORK_CHAT_DELIVER, packet);
        for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
            if (registeredServer.getServerInfo().getName().equalsIgnoreCase(packet.serverId())) {
                continue;
            }
            if (registeredServer.getPlayersConnected().isEmpty()) {
                continue;
            }
            if (!registeredServer.sendPluginMessage(channel, payload)) {
                logger.warn("ChatBloom proxy fanout to backend '{}' failed for channel '{}' from source backend '{}'.",
                    registeredServer.getServerInfo().getName(),
                    packet.channelId(),
                    packet.serverId()
                );
            }
        }
    }

    public void routePrivateMessageRequest(PrivateMessagePacket packet) {
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
        if (!targetConnection.get().getServer().sendPluginMessage(channel, PluginMessageCodec.encodePrivateMessage(ProxyMessageType.PM_DELIVER_TO_TARGET, deliverPacket))) {
            logger.warn("ChatBloom proxy failed to deliver PM request {} to backend '{}' for target '{}'.",
                packet.requestId(),
                targetConnection.get().getServer().getServerInfo().getName(),
                target.get().getUsername()
            );
            sendPrivateMessageResult(new PrivateMessageResultPacket(
                packet.requestId(),
                packet.sourceServerId(),
                packet.senderId(),
                target.get().getUniqueId(),
                target.get().getUsername(),
                packet.plainText(),
                false,
                "private-messages.remote-unavailable",
                Instant.now()
            ));
        }
    }

    public void routePrivateMessageResult(PrivateMessageResultPacket packet) {
        sendPrivateMessageResult(packet);
    }

    public void sendPrivateMessageResult(PrivateMessageResultPacket packet) {
        proxyServer.getServer(packet.sourceServerId()).ifPresentOrElse(server -> {
            if (!server.sendPluginMessage(channel, PluginMessageCodec.encodePrivateMessageResult(packet))) {
                logger.warn("ChatBloom proxy failed to return PM result {} to source backend '{}'.",
                    packet.requestId(),
                    packet.sourceServerId()
                );
            }
        }, () -> logger.warn("ChatBloom proxy could not return PM result {} because source backend '{}' is not registered.",
            packet.requestId(),
            packet.sourceServerId()
        ));
    }
}
