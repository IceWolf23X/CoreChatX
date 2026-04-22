package me.icewolf23.chatbloom.common.network;

public interface NetworkBridge {
    boolean isEnabled();

    void publishChat(ChatMessagePacket packet);

    void publishPrivateMessage(PrivateMessagePacket packet);
}
