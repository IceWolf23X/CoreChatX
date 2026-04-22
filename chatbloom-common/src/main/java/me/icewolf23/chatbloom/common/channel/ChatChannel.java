package me.icewolf23.chatbloom.common.channel;

public record ChatChannel(
    String id,
    boolean enabled,
    boolean defaultChannel,
    ChannelScope scope,
    Integer radius,
    String sendPermission,
    String receivePermission,
    boolean allowMentions,
    boolean allowChatItems,
    boolean exportToBridges,
    String format
) {
    public ChatChannel(
        String id,
        boolean enabled,
        boolean defaultChannel,
        ChannelScope scope,
        Integer radius,
        String sendPermission,
        String receivePermission
    ) {
        this(id, enabled, defaultChannel, scope, radius, sendPermission, receivePermission, true, true, false, "");
    }

    public ChatChannelDefinition toDefinition() {
        return new ChatChannelDefinition(
            id,
            enabled,
            scope,
            radius,
            sendPermission,
            receivePermission,
            allowMentions,
            allowChatItems,
            exportToBridges,
            format
        );
    }
}
