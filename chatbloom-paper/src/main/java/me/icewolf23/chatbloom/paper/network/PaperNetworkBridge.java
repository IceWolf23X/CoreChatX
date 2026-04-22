package me.icewolf23.chatbloom.paper.network;

import me.icewolf23.chatbloom.common.network.ChatMessagePacket;
import me.icewolf23.chatbloom.common.network.DecodedProxyMessage;
import me.icewolf23.chatbloom.common.network.NetworkBridge;
import me.icewolf23.chatbloom.common.network.PluginMessageCodec;
import me.icewolf23.chatbloom.common.network.PrivateMessagePacket;
import me.icewolf23.chatbloom.common.network.PrivateMessageResultPacket;
import me.icewolf23.chatbloom.common.network.ProxyMessageType;
import me.icewolf23.chatbloom.paper.bootstrap.ChatBloomPaperPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public final class PaperNetworkBridge implements NetworkBridge, PluginMessageListener {

    private final ChatBloomPaperPlugin plugin;
    private final boolean enabled;
    private final String channel;
    private final String serverId;

    public PaperNetworkBridge(ChatBloomPaperPlugin plugin, boolean enabled, String channel, String serverId) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.channel = channel;
        this.serverId = serverId;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public String channel() {
        return channel;
    }

    public String serverId() {
        return serverId;
    }

    @Override
    public void publishChat(ChatMessagePacket packet) {
        if (!enabled) {
            return;
        }
        sendPluginMessage(
            PluginMessageCodec.encodeChat(ProxyMessageType.NETWORK_CHAT_FORWARD, packet),
            packet.senderId() == null ? null : Bukkit.getPlayer(packet.senderId())
        );
    }

    @Override
    public void publishPrivateMessage(PrivateMessagePacket packet) {
        if (!enabled) {
            return;
        }
        sendPluginMessage(
            PluginMessageCodec.encodePrivateMessage(ProxyMessageType.PM_REQUEST, packet),
            packet.senderId() == null ? null : Bukkit.getPlayer(packet.senderId())
        );
    }

    @Override
    public void publishPrivateMessageResult(PrivateMessageResultPacket packet) {
        if (!enabled) {
            return;
        }
        sendPluginMessage(PluginMessageCodec.encodePrivateMessageResult(packet), null);
    }

    @Override
    public void onPluginMessageReceived(String incomingChannel, Player player, byte[] message) {
        if (!enabled || !channel.equalsIgnoreCase(incomingChannel)) {
            return;
        }
        try {
            DecodedProxyMessage decoded = PluginMessageCodec.decode(message);
            switch (decoded.type()) {
                case NETWORK_CHAT_DELIVER -> plugin.chatService().handleRemoteNetworkChat(decoded.chatMessagePacket());
                case PM_DELIVER_TO_TARGET -> plugin.privateMessages().acceptRemotePrivateMessage(decoded.privateMessagePacket());
                case PM_RESULT_TO_SOURCE -> plugin.privateMessages().handlePrivateMessageResult(decoded.privateMessageResultPacket());
                default -> {
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to process inbound ChatBloom proxy message: " + exception.getMessage());
        }
    }

    private void sendPluginMessage(byte[] payload, Player preferredCarrier) {
        Player carrier = preferredCarrier != null && preferredCarrier.isOnline() ? preferredCarrier : Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) {
            plugin.getLogger().warning("ChatBloom proxy transport could not send a packet because no online player is available as a plugin-message carrier.");
            return;
        }
        carrier.sendPluginMessage(plugin, channel, payload);
    }
}
