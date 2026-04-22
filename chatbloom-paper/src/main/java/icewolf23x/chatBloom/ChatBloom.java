package icewolf23x.chatBloom;

import icewolf23x.chatBloom.chatitem.ChatItemService;
import icewolf23x.chatBloom.chatitem.SnapshotRegistry;
import icewolf23x.chatBloom.config.ConfigurationService;
import icewolf23x.chatBloom.data.GlobalStateStore;
import icewolf23x.chatBloom.data.PlayerDataStore;
import icewolf23x.chatBloom.hook.HookService;
import icewolf23x.chatBloom.service.ChatService;
import icewolf23x.chatBloom.service.CooldownService;
import icewolf23x.chatBloom.service.FormatService;
import icewolf23x.chatBloom.service.LegacyFormattingService;
import icewolf23x.chatBloom.service.NotificationService;
import icewolf23x.chatBloom.service.PrivateMessageService;
import icewolf23x.chatBloom.service.WordFilterService;
import me.icewolf23.chatbloom.common.config.DeploymentConfig;
import me.icewolf23.chatbloom.paper.bootstrap.BridgeRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.CommandRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.ConfigRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.ListenerRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.RepositoryRegistry;
import me.icewolf23.chatbloom.paper.bootstrap.ServiceRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ChatBloom extends JavaPlugin {

    protected ConfigRegistry configRegistry;
    protected RepositoryRegistry repositoryRegistry;
    protected ServiceRegistry serviceRegistry;
    protected BridgeRegistry bridgeRegistry;
    protected CommandRegistry commandRegistry;
    protected ListenerRegistry listenerRegistry;
    protected HookService hookService;
    protected DeploymentConfig bootDeploymentConfig;

    public abstract void reloadPlugin();

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
