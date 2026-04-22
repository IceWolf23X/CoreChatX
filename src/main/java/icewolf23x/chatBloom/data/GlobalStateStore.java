package icewolf23x.chatBloom.data;

import icewolf23x.chatBloom.ChatBloom;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class GlobalStateStore {

    private final ChatBloom plugin;
    private final File file;
    private FileConfiguration configuration;
    private int firstJoinCount;

    public GlobalStateStore(ChatBloom plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "state.yml");
        reload();
    }

    public void reload() {
        this.configuration = YamlConfiguration.loadConfiguration(file);
        this.firstJoinCount = configuration.getInt("first-joins.count", 0);
    }

    public int incrementFirstJoinCount() {
        firstJoinCount++;
        configuration.set("first-joins.count", firstJoinCount);
        save();
        return firstJoinCount;
    }

    public int getFirstJoinCount() {
        return firstJoinCount;
    }

    public void save() {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save state.yml: " + exception.getMessage());
        }
    }
}
