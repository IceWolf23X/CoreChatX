package me.icewolf23.chatbloom.paper.bootstrap;
import icewolf23x.chatBloom.config.ConfigurationService;
import me.icewolf23.chatbloom.common.config.DeploymentConfig;
import me.icewolf23.chatbloom.common.config.DeploymentMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class ConfigRegistry {

    private final ChatBloomPaperPlugin plugin;
    private ConfigurationService configurationService;
    private FileConfiguration channels;
    private FileConfiguration privacy;
    private FileConfiguration moderation;
    private FileConfiguration storage;
    private FileConfiguration discord;
    private FileConfiguration telegram;
    private DeploymentConfig deployment;

    public ConfigRegistry(ChatBloomPaperPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        this.configurationService = new ConfigurationService(plugin);
        this.channels = loadYaml("channels.yml");
        this.privacy = loadYaml("privacy.yml");
        this.moderation = loadYaml("moderation.yml");
        this.storage = loadYaml("storage.yml");
        this.discord = loadYaml("discord.yml");
        this.telegram = loadYaml("telegram.yml");
        this.deployment = parseDeployment(configurationService.main());
    }

    public void reloadAll() {
        loadAll();
    }

    public ConfigurationService configurationService() {
        return configurationService;
    }

    public FileConfiguration channels() {
        return channels;
    }

    public FileConfiguration privacy() {
        return privacy;
    }

    public FileConfiguration moderation() {
        return moderation;
    }

    public FileConfiguration storage() {
        return storage;
    }

    public FileConfiguration discord() {
        return discord;
    }

    public FileConfiguration telegram() {
        return telegram;
    }

    public DeploymentConfig deployment() {
        return deployment;
    }

    private FileConfiguration loadYaml(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private DeploymentConfig parseDeployment(FileConfiguration main) {
        String rawMode = main.getString("deployment.mode", DeploymentMode.STANDALONE.name());
        DeploymentMode mode;
        try {
            mode = DeploymentMode.valueOf(rawMode.toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid deployment.mode '" + rawMode + "'. Falling back to STANDALONE.");
            mode = DeploymentMode.STANDALONE;
        }
        boolean requireRestart = main.getBoolean("deployment.require-full-restart-on-mode-change", true);
        return new DeploymentConfig(mode, requireRestart);
    }
}
