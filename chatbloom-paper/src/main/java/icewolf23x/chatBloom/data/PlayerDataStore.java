package icewolf23x.chatBloom.data;

import icewolf23x.chatBloom.ChatBloom;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDataStore {

    private final ChatBloom plugin;
    private final File file;
    private final Map<UUID, PlayerSettings> cache = new HashMap<>();
    private FileConfiguration configuration;

    public PlayerDataStore(ChatBloom plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
        reload();
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.cache.clear();
        if (!configuration.isConfigurationSection("players")) {
            configuration.createSection("players");
        }
        for (String rawUuid : configuration.getConfigurationSection("players").getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(rawUuid);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping invalid playerdata entry with malformed UUID: " + rawUuid);
                continue;
            }
            PlayerSettings settings = new PlayerSettings();
            String base = "players." + rawUuid + ".";
            settings.setPingSoundEnabled(configuration.getBoolean(base + "ping-sound", true));
            settings.setPingActionbarEnabled(configuration.getBoolean(base + "ping-actionbar", true));
            settings.setSocialSpyEnabled(configuration.getBoolean(base + "social-spy", false));
            settings.setPmEnabled(configuration.getBoolean(base + "pm-enabled", true));
            cache.put(uuid, settings);
        }
    }

    public PlayerSettings get(UUID uuid) {
        return cache.computeIfAbsent(uuid, ignored -> new PlayerSettings());
    }

    public void save() {
        configuration.set("players", null);
        for (Map.Entry<UUID, PlayerSettings> entry : cache.entrySet()) {
            String base = "players." + entry.getKey() + ".";
            PlayerSettings settings = entry.getValue();
            configuration.set(base + "ping-sound", settings.isPingSoundEnabled());
            configuration.set(base + "ping-actionbar", settings.isPingActionbarEnabled());
            configuration.set(base + "social-spy", settings.isSocialSpyEnabled());
            configuration.set(base + "pm-enabled", settings.isPmEnabled());
        }
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save playerdata.yml: " + exception.getMessage());
        }
    }

    public void save(UUID uuid) {
        get(uuid);
        save();
    }
}
