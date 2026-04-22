package me.icewolf23.chatbloom.paper.bridge;

import me.icewolf23.chatbloom.common.bridge.BridgeMessage;
import me.icewolf23.chatbloom.common.bridge.OutboundBridge;
import me.icewolf23.chatbloom.common.channel.ChatChannel;
import me.icewolf23.chatbloom.paper.ChatBloom;

import java.util.List;

public final class BridgeDispatchService {

    private final ChatBloom plugin;
    private final List<OutboundBridge> outboundBridges;

    public BridgeDispatchService(ChatBloom plugin) {
        this.plugin = plugin;
        this.outboundBridges = List.of(
            new DiscordOutboundBridge(plugin),
            new TelegramOutboundBridge(plugin)
        );
    }

    public void dispatchChannelMessage(ChatChannel channel, String senderName, String plainText) {
        if (channel == null || !channel.exportToBridges() || !plugin.configs().deployment().bridgesAllowed()) {
            return;
        }
        BridgeMessage message = new BridgeMessage(
            "chat",
            plugin.services().bridgeServerId().isBlank() ? plugin.configs().deployment().serverId() : plugin.services().bridgeServerId(),
            channel.id(),
            senderName,
            plainText,
            System.currentTimeMillis()
        );
        outboundBridges.stream()
            .filter(OutboundBridge::isEnabled)
            .forEach(bridge -> bridge.forward(message));
    }
}
