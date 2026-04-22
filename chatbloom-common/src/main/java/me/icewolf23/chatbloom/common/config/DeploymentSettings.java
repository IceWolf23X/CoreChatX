package me.icewolf23.chatbloom.common.config;

public record DeploymentSettings(
    DeploymentMode mode,
    boolean requireRestartOnModeChange,
    String serverId,
    String networkChannel,
    boolean bridgesAllowed,
    boolean networkFeaturesAllowed,
    long pendingPmTimeoutSeconds
) {
}
