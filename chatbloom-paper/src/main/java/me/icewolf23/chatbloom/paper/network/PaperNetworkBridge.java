package me.icewolf23.chatbloom.paper.network;

import me.icewolf23.chatbloom.common.network.ChatMessagePacket;
import me.icewolf23.chatbloom.common.network.NetworkBridge;
import me.icewolf23.chatbloom.common.network.PrivateMessagePacket;

public final class PaperNetworkBridge implements NetworkBridge {

    private final boolean enabled;

    public PaperNetworkBridge(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void publishChat(ChatMessagePacket packet) {
        if (!enabled) {
            return;
        }
    }

    @Override
    public void publishPrivateMessage(PrivateMessagePacket packet) {
        if (!enabled) {
            return;
        }
    }
}
