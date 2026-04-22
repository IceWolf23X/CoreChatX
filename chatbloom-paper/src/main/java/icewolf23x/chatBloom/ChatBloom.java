package icewolf23x.chatBloom;

import icewolf23x.chatBloom.chatitem.ChatItemService;
import icewolf23x.chatBloom.chatitem.SnapshotRegistry;
import icewolf23x.chatBloom.config.ConfigurationService;
import icewolf23x.chatBloom.data.GlobalStateStore;
import icewolf23x.chatBloom.data.PlayerDataStore;
import icewolf23x.chatBloom.service.ChatService;
import icewolf23x.chatBloom.service.CooldownService;
import icewolf23x.chatBloom.service.FormatService;
import icewolf23x.chatBloom.service.LegacyFormattingService;
import icewolf23x.chatBloom.service.NotificationService;
import icewolf23x.chatBloom.service.PrivateMessageService;
import icewolf23x.chatBloom.service.WordFilterService;
import me.icewolf23.chatbloom.common.config.DeploymentConfig;
import me.icewolf23.chatbloom.common.config.DeploymentMode;
import me.icewolf23.chatbloom.paper.bootstrap.BridgeRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.CommandRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.ConfigRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.ListenerRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.RepositoryRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.ServiceRegistry;
import icewolf23x.chatBloom.hook.HookService;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatBloom extends JavaPlugin {

    private ConfigRegistry configRegistry;
    private RepositoryRegistry repositoryRegistry;
    private ServiceRegistry serviceRegistry;
    private BridgeRegistry bridgeRegistry;
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;
    private HookService hookService;
    private DeploymentConfig bootDeploymentConfig;

    @Override
    public void onEnable() {
        saveDefaultConfigFiles();
        try {
            this.configRegistry = new ConfigRegistry(this);
            this.configRegistry.loadAll();
            this.bootDeploymentConfig = configRegistry.deployment();

            getLogger().info("ChatBloom deployment mode: " + bootDeploymentConfig.mode());

            this.repositoryRegistry = new RepositoryRegistry(this, configRegistry);
            this.repositoryRegistry.initialize();
            this.serviceRegistry = new ServiceRegistry(this, configRegistry, repositoryRegistry);
            this.serviceRegistry.initialize();
            this.bridgeRegistry = new BridgeRegistry(this, configRegistry, serviceRegistry);
            this.bridgeRegistry.initialize();
            this.commandRegistry = new CommandRegistry(this);
            this.commandRegistry.registerAll();
            this.listenerRegistry = new ListenerRegistry(this);
            this.listenerRegistry.registerAll();
            this.hookService = new HookService(this);
            hookService.refresh();

            if (bootDeploymentConfig.mode() == DeploymentMode.PROXY) {
                getLogger().warning("Proxy mode is configured. A full restart is required after deployment mode changes, and the proxy transport remains stubbed in Run 1.");
            }
        } catch (Exception exception) {
            getLogger().severe("ChatBloom could not start cleanly: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        boolean autoSave = configRegistry == null || configuration().main().getBoolean("player-data.auto-save-on-disable", true);
        if (repositoryRegistry != null && autoSave) {
            repositoryRegistry.shutdown();
        }
        if (serviceRegistry != null && serviceRegistry.snapshotRegistry() != null) {
            serviceRegistry.snapshotRegistry().clear();
        }
    }

    public void reloadPlugin() {
        repositoryRegistry.shutdown();
        configRegistry.reloadAll();
        if (configRegistry.deployment().mode() != bootDeploymentConfig.mode() && configRegistry.deployment().requireFullRestartOnModeChange()) {
            getLogger().warning("Deployment mode changed from " + bootDeploymentConfig.mode() + " to " + configRegistry.deployment().mode() + ". A full restart is required before the new mode becomes active.");
        }
        repositoryRegistry.reload();
        hookService.refresh();
        serviceRegistry.reload();
        bridgeRegistry.reload();
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
        saveResourceIfAbsent("channels.yml");
        saveResourceIfAbsent("privacy.yml");
        saveResourceIfAbsent("moderation.yml");
        saveResourceIfAbsent("storage.yml");
        saveResourceIfAbsent("discord.yml");
        saveResourceIfAbsent("telegram.yml");
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

    public ConfigurationService configuration() {
        return configRegistry.configurationService();
    }

    public PlayerDataStore playerData() {
        return repositoryRegistry.playerDataStore();
    }

    public GlobalStateStore globalState() {
        return repositoryRegistry.globalStateStore();
    }

    public HookService hooks() {
        return hookService;
    }

    public WordFilterService wordFilter() {
        return serviceRegistry.wordFilterService();
    }

    public LegacyFormattingService legacyFormatting() {
        return serviceRegistry.legacyFormattingService();
    }

    public CooldownService cooldowns() {
        return serviceRegistry.cooldownService();
    }

    public SnapshotRegistry snapshots() {
        return serviceRegistry.snapshotRegistry();
    }

    public ChatItemService chatItems() {
        return serviceRegistry.chatItemService();
    }

    public FormatService formats() {
        return serviceRegistry.formatService();
    }

    public NotificationService notifications() {
        return serviceRegistry.notificationService();
    }

    public ChatService chatService() {
        return serviceRegistry.chatService();
    }

    public PrivateMessageService privateMessages() {
        return serviceRegistry.privateMessageService();
    }

    public ConfigRegistry configs() {
        return configRegistry;
    }

    public RepositoryRegistry repositories() {
        return repositoryRegistry;
    }

    public ServiceRegistry services() {
        return serviceRegistry;
    }
}
