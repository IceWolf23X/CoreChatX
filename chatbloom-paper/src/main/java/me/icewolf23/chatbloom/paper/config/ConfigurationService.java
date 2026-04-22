package me.icewolf23.chatbloom.paper.config;

import me.icewolf23.chatbloom.paper.ChatBloom;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class ConfigurationService {

    private static final String[] FILES = {
        "config.yml",
        "messages.yml",
        "chat.yml",
        "pings.yml",
        "filter.yml",
        "chatitems.yml"
    };

    private final ChatBloom plugin;
    private final Map<String, FileConfiguration> configurations = new HashMap<>();

    public ConfigurationService(ChatBloom plugin) {
        this.plugin = plugin;
        reloadAll();
    }

    public void reloadAll() {
        configurations.clear();
        plugin.reloadConfig();
        configurations.put("config.yml", plugin.getConfig());
        for (String fileName : FILES) {
            if ("config.yml".equals(fileName)) {
                continue;
            }
            File file = new File(plugin.getDataFolder(), fileName);
            if (!file.exists()) {
                plugin.saveResource(fileName, false);
            }
            configurations.put(fileName, YamlConfiguration.loadConfiguration(file));
        }
    }

    public FileConfiguration main() {
        return configurations.get("config.yml");
    }

    public FileConfiguration messages() {
        return configurations.get("messages.yml");
    }

    public FileConfiguration chat() {
        return configurations.get("chat.yml");
    }

    public FileConfiguration pings() {
        return configurations.get("pings.yml");
    }

    public FileConfiguration filter() {
        return configurations.get("filter.yml");
    }

    public FileConfiguration chatItems() {
        return configurations.get("chatitems.yml");
    }
}
