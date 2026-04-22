package me.icewolf23.chatbloom.common.channel;

public record ChatChannel(
    String id,
    boolean enabled,
    boolean defaultChannel,
    ChannelScope scope,
    Integer radius,
    String sendPermission,
    String receivePermission
) {
}
