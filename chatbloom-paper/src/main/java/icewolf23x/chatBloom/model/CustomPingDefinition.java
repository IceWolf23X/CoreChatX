package icewolf23x.chatBloom.model;

public record CustomPingDefinition(
    String key,
    String trigger,
    String usePermission,
    String receivePermission,
    boolean bypassToggle,
    String tokenFormat
) {
}
