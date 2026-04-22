package me.icewolf23.chatbloom.paper.model;

public record CustomPingDefinition(
    String key,
    String trigger,
    String usePermission,
    String receivePermission,
    boolean bypassToggle,
    String tokenFormat
) {
}
