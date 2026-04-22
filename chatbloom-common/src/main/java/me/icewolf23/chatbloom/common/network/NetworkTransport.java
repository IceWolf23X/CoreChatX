package me.icewolf23.chatbloom.common.network;

public interface NetworkTransport {
    boolean isAvailable();

    void send(NetworkEnvelope envelope);
}
