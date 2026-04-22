package me.icewolf23.chatbloom.common.channel;

public record ChatChannelDefinition(
    String id,
    boolean enabled,
    ChannelScope scope,
    Integer radius,
    String sendPermission,
    String receivePermission,
    boolean allowMentions,
    boolean allowChatItems,
    boolean exportToBridges,
    String format
) {
}
