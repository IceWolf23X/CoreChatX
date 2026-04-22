package me.icewolf23.chatbloom.common.network;

public interface NetworkBridge {
    boolean isEnabled();

    boolean publishChat(ChatMessagePacket packet);

    boolean publishPrivateMessage(PrivateMessagePacket packet);

    boolean publishPrivateMessageResult(PrivateMessageResultPacket packet);
}
