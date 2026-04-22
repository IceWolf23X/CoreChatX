package me.icewolf23.chatbloom.common.config;

public record DeploymentConfig(
    DeploymentMode mode,
    boolean requireFullRestartOnModeChange
) {
}
