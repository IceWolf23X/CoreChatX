package icewolf23x.chatBloom;

import icewolf23x.chatBloom.chatitem.ChatItemService;
import icewolf23x.chatBloom.chatitem.SnapshotRegistry;
import icewolf23x.chatBloom.command.BroadcastCommand;
import icewolf23x.chatBloom.command.ChatBloomCommand;
import icewolf23x.chatBloom.command.MessageCommand;
import icewolf23x.chatBloom.command.PingCommand;
import icewolf23x.chatBloom.command.ReplyCommand;
import icewolf23x.chatBloom.command.SocialSpyCommand;
import icewolf23x.chatBloom.config.ConfigurationService;
import icewolf23x.chatBloom.data.GlobalStateStore;
import icewolf23x.chatBloom.data.PlayerDataStore;
import icewolf23x.chatBloom.hook.HookService;
import icewolf23x.chatBloom.listener.ChatListener;
import icewolf23x.chatBloom.listener.ConnectionListener;
import icewolf23x.chatBloom.listener.InventoryPreviewListener;
import icewolf23x.chatBloom.service.ChatService;
import icewolf23x.chatBloom.service.CooldownService;
import icewolf23x.chatBloom.service.FormatService;
import icewolf23x.chatBloom.service.LegacyFormattingService;
import icewolf23x.chatBloom.service.NotificationService;
import icewolf23x.chatBloom.service.PrivateMessageService;
import icewolf23x.chatBloom.service.WordFilterService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ChatBloom extends JavaPlugin {

    private ConfigurationService configurationService;
    private PlayerDataStore playerDataStore;
    private GlobalStateStore globalStateStore;
    private HookService hookService;
    private WordFilterService wordFilterService;
    private LegacyFormattingService legacyFormattingService;
    private CooldownService cooldownService;
    private SnapshotRegistry snapshotRegistry;
    private ChatItemService chatItemService;
    private FormatService formatService;
    private NotificationService notificationService;
    private ChatService chatService;
    private PrivateMessageService privateMessageService;

    @Override
    public void onEnable() {
        saveDefaultConfigFiles();
        try {
            this.configurationService = new ConfigurationService(this);
            this.playerDataStore = new PlayerDataStore(this);
            this.globalStateStore = new GlobalStateStore(this);
            this.hookService = new HookService(this);
            this.wordFilterService = new WordFilterService(this);
            this.legacyFormattingService = new LegacyFormattingService(this);
            this.cooldownService = new CooldownService(this);
            this.snapshotRegistry = new SnapshotRegistry(this);
            this.chatItemService = new ChatItemService(this);
            this.formatService = new FormatService(this);
            this.notificationService = new NotificationService(this);
            this.chatService = new ChatService(this);
            this.privateMessageService = new PrivateMessageService(this);

            registerListeners();
            registerCommands();
            hookService.refresh();
        } catch (Exception exception) {
            getLogger().severe("ChatBloom could not start cleanly: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        boolean autoSave = configurationService == null || configurationService.main().getBoolean("player-data.auto-save-on-disable", true);
        if (playerDataStore != null && autoSave) {
            playerDataStore.save();
        }
        if (globalStateStore != null && autoSave) {
            globalStateStore.save();
        }
        if (snapshotRegistry != null) {
            snapshotRegistry.clear();
        }
    }

    public void reloadPlugin() {
        playerDataStore.save();
        globalStateStore.save();
        configurationService.reloadAll();
        playerDataStore.reload();
        globalStateStore.reload();
        hookService.refresh();
        wordFilterService.reload();
        legacyFormattingService.reload();
        cooldownService.reload();
        snapshotRegistry.reload();
        chatItemService.reload();
        formatService.reload();
        notificationService.reload();
        chatService.reload();
        privateMessageService.reload();
    }

    private void saveDefaultConfigFiles() {
        saveResourceIfAbsent("config.yml");
        saveResourceIfAbsent("messages.yml");
        saveResourceIfAbsent("chat.yml");
        saveResourceIfAbsent("pings.yml");
        saveResourceIfAbsent("filter.yml");
        saveResourceIfAbsent("chatitems.yml");
        saveResourceIfAbsent("playerdata.yml");
        saveResourceIfAbsent("state.yml");
    }

    private void saveResourceIfAbsent(String name) {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        java.io.File file = new java.io.File(getDataFolder(), name);
        if (!file.exists()) {
            saveResource(name, false);
        }
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new ChatListener(this), this);
        pluginManager.registerEvents(new ConnectionListener(this), this);
        pluginManager.registerEvents(new InventoryPreviewListener(this), this);
    }

    private void registerCommands() {
        setExecutor("chatbloom", new ChatBloomCommand(this));
        setExecutor("msg", new MessageCommand(this));
        setExecutor("reply", new ReplyCommand(this));
        setExecutor("socialspy", new SocialSpyCommand(this));
        setExecutor("broadcast", new BroadcastCommand(this));
        setExecutor("ping", new PingCommand(this));
    }

    private void setExecutor(String commandName, org.bukkit.command.TabExecutor executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(commandName), "Missing command " + commandName);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public ConfigurationService configuration() {
        return configurationService;
    }

    public PlayerDataStore playerData() {
        return playerDataStore;
    }

    public GlobalStateStore globalState() {
        return globalStateStore;
    }

    public HookService hooks() {
        return hookService;
    }

    public WordFilterService wordFilter() {
        return wordFilterService;
    }

    public LegacyFormattingService legacyFormatting() {
        return legacyFormattingService;
    }

    public CooldownService cooldowns() {
        return cooldownService;
    }

    public SnapshotRegistry snapshots() {
        return snapshotRegistry;
    }

    public ChatItemService chatItems() {
        return chatItemService;
    }

    public FormatService formats() {
        return formatService;
    }

    public NotificationService notifications() {
        return notificationService;
    }

    public ChatService chatService() {
        return chatService;
    }

    public PrivateMessageService privateMessages() {
        return privateMessageService;
    }
}
