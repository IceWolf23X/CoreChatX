package me.icewolf23.chatbloom.common.bridge;

public interface OutboundBridge {
    String id();

    boolean isEnabled();

    void forward(BridgeMessage message);
}
