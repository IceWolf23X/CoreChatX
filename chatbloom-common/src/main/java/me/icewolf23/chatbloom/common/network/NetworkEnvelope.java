package me.icewolf23.chatbloom.common.network;

public record NetworkEnvelope(
    int protocolVersion,
    NetworkPacketType packetType,
    String sourceServer,
    String targetServer,
    String payloadJson
) {
}
