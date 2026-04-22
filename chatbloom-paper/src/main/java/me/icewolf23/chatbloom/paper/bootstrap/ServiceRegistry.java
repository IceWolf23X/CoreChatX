package me.icewolf23.chatbloom.paper.bootstrap;
import me.icewolf23.chatbloom.common.locale.LocaleService;
import me.icewolf23.chatbloom.paper.chatitem.ChatItemService;
import me.icewolf23.chatbloom.paper.chatitem.SnapshotRegistry;
import me.icewolf23.chatbloom.paper.bridge.BridgeDispatchService;
import me.icewolf23.chatbloom.paper.locale.PaperLocaleService;
import me.icewolf23.chatbloom.paper.service.ChatService;
import me.icewolf23.chatbloom.paper.service.CooldownService;
import me.icewolf23.chatbloom.paper.service.FormatService;
import me.icewolf23.chatbloom.paper.service.LegacyFormattingService;
import me.icewolf23.chatbloom.paper.service.NotificationService;
import me.icewolf23.chatbloom.paper.service.PrivateMessageService;
import me.icewolf23.chatbloom.paper.service.WordFilterService;
import me.icewolf23.chatbloom.common.channel.ChannelService;
import me.icewolf23.chatbloom.common.event.EventBus;
import me.icewolf23.chatbloom.common.event.SimpleEventBus;
import me.icewolf23.chatbloom.common.moderation.ModerationService;
import me.icewolf23.chatbloom.common.network.NetworkBridge;
import me.icewolf23.chatbloom.common.pipeline.ChatPipeline;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineStep;
import me.icewolf23.chatbloom.paper.gui.SettingsMenuFactory;
import me.icewolf23.chatbloom.common.privacy.PrivacyService;
import me.icewolf23.chatbloom.paper.network.PaperNetworkBridge;
import me.icewolf23.chatbloom.paper.pipeline.ActiveChannelResolutionStep;
import me.icewolf23.chatbloom.paper.pipeline.LegacyPaperChatPipeline;
import me.icewolf23.chatbloom.paper.pipeline.PaperChatPipelineEntry;
import me.icewolf23.chatbloom.paper.pipeline.PublicModerationStep;
import me.icewolf23.chatbloom.paper.platform.DefaultChannelService;
import me.icewolf23.chatbloom.paper.platform.DefaultModerationService;
import me.icewolf23.chatbloom.paper.platform.DefaultPrivacyService;
import me.icewolf23.chatbloom.paper.platform.PaperChannelAudienceResolver;

import java.time.Clock;
import java.util.List;

public final class ServiceRegistry {

    private final ChatBloomPaperPlugin plugin;
    private final ConfigRegistry configRegistry;
    private final RepositoryRegistry repositoryRegistry;
    private EventBus eventBus;
    private ChannelService channelService;
    private PrivacyService privacyService;
    private ModerationService moderationService;
    private ChatPipeline chatPipeline;
    private PaperChatPipelineEntry chatPipelineEntry;
    private NetworkBridge networkBridge;
    private LocaleService localeService;
    private BridgeDispatchService bridgeDispatchService;
    private SettingsMenuFactory settingsMenuFactory;
    private PaperChannelAudienceResolver channelAudienceResolver;
    private WordFilterService wordFilterService;
    private LegacyFormattingService legacyFormattingService;
    private CooldownService cooldownService;
    private SnapshotRegistry snapshotRegistry;
    private ChatItemService chatItemService;
    private FormatService formatService;
    private NotificationService notificationService;
    private ChatService chatService;
    private PrivateMessageService privateMessageService;
    private String bridgeServerId = "";

    public ServiceRegistry(ChatBloomPaperPlugin plugin, ConfigRegistry configRegistry, RepositoryRegistry repositoryRegistry) {
        this.plugin = plugin;
        this.configRegistry = configRegistry;
        this.repositoryRegistry = repositoryRegistry;
    }

    public void initialize() {
        this.eventBus = new SimpleEventBus();
        this.channelService = new DefaultChannelService(configRegistry.channels(), repositoryRegistry.activeChannelRepository());
        this.privacyService = new DefaultPrivacyService(
            repositoryRegistry.ignoreRepository(),
            repositoryRegistry.playerStateRepository(),
            configRegistry.privacy().getBoolean("ignore.enabled", true)
        );
        this.moderationService = new DefaultModerationService(
            repositoryRegistry.muteRepository(),
            repositoryRegistry.globalStateRepository(),
            Clock.systemUTC(),
            configRegistry.moderation().getBoolean("mute.enabled", true),
            configRegistry.moderation().getBoolean("mutechat.enabled", true)
        );
        List<ChatPipelineStep> publicChatSteps = List.of(
            new ActiveChannelResolutionStep(channelService),
            new PublicModerationStep(moderationService)
        );
        this.wordFilterService = new WordFilterService(plugin);
        this.legacyFormattingService = new LegacyFormattingService(plugin);
        this.cooldownService = new CooldownService(plugin);
        this.snapshotRegistry = new SnapshotRegistry(plugin);
        this.chatItemService = new ChatItemService(plugin);
        this.localeService = new PaperLocaleService(plugin, repositoryRegistry.playerStateRepository());
        this.formatService = new FormatService(plugin);
        this.bridgeDispatchService = new BridgeDispatchService(plugin);
        this.notificationService = new NotificationService(plugin);
        this.chatService = new ChatService(plugin);
        this.privateMessageService = new PrivateMessageService(plugin);
        this.chatPipeline = new LegacyPaperChatPipeline(publicChatSteps);
        this.chatPipelineEntry = new PaperChatPipelineEntry(chatPipeline, chatService);
        this.networkBridge = new PaperNetworkBridge(
            plugin,
            false,
            configRegistry.deployment().networkChannel(),
            configRegistry.deployment().serverId()
        );
        this.settingsMenuFactory = new SettingsMenuFactory();
        this.channelAudienceResolver = new PaperChannelAudienceResolver(repositoryRegistry.playerStateRepository());
        this.bridgeServerId = "";
    }

    public void reload() {
        if (snapshotRegistry != null) {
            snapshotRegistry.clear();
        }
        initialize();
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public ChannelService channelService() {
        return channelService;
    }

    public PrivacyService privacyService() {
        return privacyService;
    }

    public ModerationService moderationService() {
        return moderationService;
    }

    public ChatPipeline chatPipeline() {
        return chatPipeline;
    }

    public PaperChatPipelineEntry chatPipelineEntry() {
        return chatPipelineEntry;
    }

    public NetworkBridge networkBridge() {
        return networkBridge;
    }

    public LocaleService localeService() {
        return localeService;
    }

    public BridgeDispatchService bridgeDispatchService() {
        return bridgeDispatchService;
    }

    public void networkBridge(NetworkBridge networkBridge) {
        this.networkBridge = networkBridge;
    }

    public String bridgeServerId() {
        return bridgeServerId;
    }

    public void bridgeServerId(String bridgeServerId) {
        this.bridgeServerId = bridgeServerId == null ? "" : bridgeServerId;
    }

    public SettingsMenuFactory settingsMenuFactory() {
        return settingsMenuFactory;
    }

    public PaperChannelAudienceResolver channelAudienceResolver() {
        return channelAudienceResolver;
    }

    public WordFilterService wordFilterService() {
        return wordFilterService;
    }

    public LegacyFormattingService legacyFormattingService() {
        return legacyFormattingService;
    }

    public CooldownService cooldownService() {
        return cooldownService;
    }

    public SnapshotRegistry snapshotRegistry() {
        return snapshotRegistry;
    }

    public ChatItemService chatItemService() {
        return chatItemService;
    }

    public FormatService formatService() {
        return formatService;
    }

    public NotificationService notificationService() {
        return notificationService;
    }

    public ChatService chatService() {
        return chatService;
    }

    public PrivateMessageService privateMessageService() {
        return privateMessageService;
    }
}
