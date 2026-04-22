package me.icewolf23.chatbloom.common.config;

public record DeploymentConfig(
    DeploymentMode mode,
    boolean requireFullRestartOnModeChange,
    String serverId,
    String networkChannel,
    boolean bridgesAllowed,
    boolean networkFeaturesAllowed,
    long pendingPmTimeoutSeconds
) {
    public DeploymentConfig(DeploymentSettings settings) {
        this(
            settings.mode(),
            settings.requireRestartOnModeChange(),
            settings.serverId(),
            settings.networkChannel(),
            settings.bridgesAllowed(),
            settings.networkFeaturesAllowed(),
            settings.pendingPmTimeoutSeconds()
        );
    }
}
