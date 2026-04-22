package me.icewolf23.chatbloom.common.network;

public record DecodedProxyMessage(
    ProxyMessageType type,
    ChatMessagePacket chatMessagePacket,
    PrivateMessagePacket privateMessagePacket,
    PrivateMessageResultPacket privateMessageResultPacket
) {
}
