# ChatBloom — Codex Run 1
## Foundations, multi-module split, standalone Paper mode, proxy-ready architecture

**Purpose:** this file is the instruction set for the **first 5h Codex run**.

This run must **not** attempt to fully finish the whole plugin suite.  
This run must instead create the **correct foundations** so the second run can complete the implementation cleanly.

---

# 0. Primary objective

Transform ChatBloom from a single-module Paper-only codebase into a **clean multi-module project** with:

- **three code modules**
  - `chatbloom-common`
  - `chatbloom-paper`
  - `chatbloom-velocity`

- **two runtime jars**
  - `chatbloom-paper.jar`
  - `chatbloom-velocity.jar`

- **two runtime deployment modes**
  - **standalone Paper mode**  
    `chatbloom-paper.jar` only
  - **proxy-enabled mode**  
    `chatbloom-paper.jar` on all backends + `chatbloom-velocity.jar` on the proxy

This run must leave the plugin in a state where:

- Paper standalone mode is already functional
- proxy mode is **configurable and structurally supported**
- switching between standalone/proxy mode requires a **full restart**
- `/chatbloom reload` must **not** hot-switch deployment mode
- the current user-facing behavior is preserved as much as possible

---

# 1. Absolute constraints

## 1.1 Do not break current core behavior

Unless absolutely required by the refactor, preserve:

- public chat formatting
- MiniMessage-owned template rendering
- current sanitization philosophy
- mentions/custom pings
- `/msg`, `/reply`, `/socialspy`
- broadcasts
- cooldowns
- word filter
- ChatItems snapshot behavior
- reload support
- PlaceholderAPI/LuckPerms fallback behavior
- current permission philosophy

## 1.2 Do not fully implement every planned feature in this run

This run is for **foundations**.

Allowed:
- architecture split
- bootstrap
- service contracts
- repositories
- pipeline skeleton
- config ownership
- core domain models
- network DTOs and stubs
- baseline implementations for channels/privacy/moderation if needed for architecture

Not allowed:
- trying to finish every feature from the master spec in one pass
- giant unsafe rewrite of the whole plugin
- forcing proxy mode as mandatory
- introducing Twitch or generic webhook systems

## 1.3 ChatBloom must still work as a standalone Paper plugin

This is critical.

If only `chatbloom-paper.jar` is installed:
- the plugin must still function as a normal chat plugin
- network-only features must remain disabled or gracefully unavailable
- config comments/defaults must make this clear

## 1.4 Proxy mode must be optional and explicit

Proxy-aware operation must be opt-in.

Required config idea:

```yml
deployment:
  mode: "STANDALONE" # STANDALONE or PROXY
```

Rules:
- if mode changes, a **full restart** is required
- reload does not change deployment mode
- invalid mode values must fail safely with clear logs
- velocity-specific code must never load inside Paper-only runtime paths

---

# 2. Deliverables for this run

At the end of Run 1, Codex must leave the repo with these deliverables:

1. multi-module build structure
2. `chatbloom-common` with shared domain/interfaces/contracts
3. `chatbloom-paper` bootstrap functional
4. `chatbloom-velocity` bootstrap present and compilable
5. standalone Paper mode working
6. proxy mode config present but not necessarily fully feature-complete
7. repository abstraction introduced
8. YAML repositories working as default persistence
9. chat pipeline abstraction introduced
10. internal event bus introduced
11. baseline channel/privacy/moderation service skeletons added
12. network packet contracts and transport stubs added
13. code compiling cleanly across all modules

---

# 3. Module structure to create

## 3.1 Parent structure

```text
chatbloom/
  pom.xml
  chatbloom-common/
    pom.xml
  chatbloom-paper/
    pom.xml
    src/main/resources/plugin.yml
  chatbloom-velocity/
    pom.xml
    src/main/resources/velocity-plugin.json
```

## 3.2 Package structure

```text
me.icewolf23.chatbloom
  ├─ common
  │   ├─ api
  │   ├─ audit
  │   ├─ bridge
  │   ├─ channel
  │   ├─ command
  │   ├─ config
  │   ├─ event
  │   ├─ locale
  │   ├─ model
  │   ├─ moderation
  │   ├─ network
  │   ├─ pipeline
  │   ├─ privacy
  │   ├─ storage
  │   └─ util
  ├─ paper
  │   ├─ bootstrap
  │   ├─ chatitem
  │   ├─ command
  │   ├─ delivery
  │   ├─ gui
  │   ├─ listener
  │   ├─ network
  │   └─ platform
  └─ velocity
      ├─ bootstrap
      ├─ bridge
      ├─ command
      ├─ delivery
      ├─ network
      └─ platform
```

---

# 4. Mandatory architecture choices

## 4.1 `chatbloom-common` must remain platform-neutral

It must not depend on Paper/Bukkit or Velocity APIs.

Allowed content:
- enums
- records
- interfaces
- repository contracts
- pipeline contracts
- internal event contracts
- DTOs
- network packet models
- serializers
- utility helpers

## 4.2 Final rendered output must use Adventure `Component`

Rules:
- raw player input begins as plain text
- sanitization happens before formatting
- MiniMessage only applies to plugin-controlled templates
- do not trust raw player-authored MiniMessage
- avoid loose string concatenation for final output

## 4.3 Persistence must go through repositories

Default runtime persistence:
- YAML

Future-ready abstraction:
- SQLite
- MySQL
- MariaDB
- PostgreSQL

Run 1 only needs:
- repository interfaces
- YAML implementations
- SQL schema/bootstrap placeholders if helpful

---

# 5. POM and build setup baseline

Create a Maven parent and three modules.

## 5.1 Parent POM baseline

```xml
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>me.icewolf23</groupId>
    <artifactId>chatbloom-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>chatbloom-common</module>
        <module>chatbloom-paper</module>
        <module>chatbloom-velocity</module>
    </modules>

    <properties>
        <maven.compiler.release>21</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
</project>
```

## 5.2 Common module baseline

```xml
<project>
    <parent>
        <groupId>me.icewolf23</groupId>
        <artifactId>chatbloom-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>chatbloom-common</artifactId>

    <dependencies>
        <dependency>
            <groupId>net.kyori</groupId>
            <artifactId>adventure-api</artifactId>
            <version>4.17.0</version>
        </dependency>
    </dependencies>
</project>
```

## 5.3 Paper module baseline

```xml
<project>
    <parent>
        <groupId>me.icewolf23</groupId>
        <artifactId>chatbloom-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>chatbloom-paper</artifactId>

    <dependencies>
        <dependency>
            <groupId>me.icewolf23</groupId>
            <artifactId>chatbloom-common</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21.11-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

## 5.4 Velocity module baseline

```xml
<project>
    <parent>
        <groupId>me.icewolf23</groupId>
        <artifactId>chatbloom-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>chatbloom-velocity</artifactId>

    <dependencies>
        <dependency>
            <groupId>me.icewolf23</groupId>
            <artifactId>chatbloom-common</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>com.velocitypowered</groupId>
            <artifactId>velocity-api</artifactId>
            <version>3.4.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

---

# 6. Runtime mode model

## 6.1 Required enum

```java
public enum DeploymentMode {
    STANDALONE,
    PROXY
}
```

## 6.2 Required config DTO

```java
public record DeploymentConfig(
        DeploymentMode mode,
        boolean requireFullRestartOnModeChange
) {}
```

## 6.3 Required config baseline

```yml
deployment:
  mode: "STANDALONE"
  require-full-restart-on-mode-change: true
```

---

# 7. Bootstrap that must be created

## 7.1 Paper bootstrap goals

Paper bootstrap must:
- load configs
- resolve deployment mode
- build repositories
- build services
- register listeners
- register commands
- register GUI handlers
- register network bridge stub
- log clearly whether it runs in standalone or proxy mode

### Paper bootstrap baseline

```java
public final class ChatBloomPaperPlugin extends JavaPlugin {

    private ServiceRegistry services;
    private ListenerRegistry listeners;
    private CommandRegistry commands;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        DeploymentConfig deployment = loadDeploymentConfig();
        this.services = new ServiceRegistry();
        this.listeners = new ListenerRegistry(this);
        this.commands = new CommandRegistry(this);

        RepositoryRegistry repositories = createRepositories();
        EventBus eventBus = new SimpleEventBus();
        Clock clock = Clock.systemUTC();

        ChannelService channelService = createChannelService(repositories, clock);
        PrivacyService privacyService = createPrivacyService(repositories);
        ModerationService moderationService = createModerationService(repositories, clock);
        ChatPipeline pipeline = createChatPipeline(channelService, privacyService, moderationService);

        services.put(ChannelService.class, channelService);
        services.put(PrivacyService.class, privacyService);
        services.put(ModerationService.class, moderationService);
        services.put(ChatPipeline.class, pipeline);
        services.put(EventBus.class, eventBus);

        registerPaperListeners();
        registerPaperCommands();
        registerGuiHandlers();

        getLogger().info("ChatBloom Paper enabled in mode: " + deployment.mode());
    }
}
```

## 7.2 Velocity bootstrap goals

Velocity bootstrap must:
- compile and start cleanly
- load config
- understand proxy mode ownership
- prepare routing/bridge services
- not attempt to fully own all features yet unless foundation is ready

### Velocity bootstrap baseline

```java
@Plugin(
        id = "chatbloom",
        name = "ChatBloom",
        version = "1.0.0-SNAPSHOT",
        authors = {"IceWolf23"}
)
public final class ChatBloomVelocityPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private ServiceRegistry services;

    @Inject
    public ChatBloomVelocityPlugin(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.services = new ServiceRegistry();
        logger.info("ChatBloom Velocity bootstrap initialized.");
    }
}
```

---

# 8. Service contracts that must exist

Create interfaces in `chatbloom-common`.

```java
public interface ChannelService {
    Optional<ChatChannel> findChannel(String id);
    ChatChannel getDefaultChannel();
    Set<UUID> resolveRecipients(ChannelMessageContext context);
    void setActiveChannel(UUID playerId, String channelId);
    String getActiveChannel(UUID playerId);
}
```

```java
public interface PrivacyService {
    boolean isIgnoring(UUID actor, UUID target);
    void addIgnore(UUID actor, UUID target);
    void removeIgnore(UUID actor, UUID target);
    boolean isPmEnabled(UUID playerId);
    void setPmEnabled(UUID playerId, boolean enabled);
}
```

```java
public interface ModerationService {
    ModerationDecision evaluatePublicMessage(ChatPipelineContext context);
    ModerationDecision evaluatePrivateMessage(ChatPipelineContext context);
    boolean isMuted(UUID playerId);
    void mute(UUID playerId, Instant until, String reason, UUID actor);
    void unmute(UUID playerId);
}
```

```java
public interface NetworkBridge {
    boolean isEnabled();
    void publishChat(ChatMessagePacket packet);
    void publishPrivateMessage(PrivateMessagePacket packet);
}
```

---

# 9. Repositories that must exist now

## 9.1 Common contracts

```java
public interface PlayerSettingsRepository {
    PlayerSettingsRecord load(UUID playerId);
    void save(PlayerSettingsRecord record);
}
```

```java
public interface ActiveChannelRepository {
    String loadActiveChannel(UUID playerId);
    void saveActiveChannel(UUID playerId, String channelId);
}
```

```java
public interface IgnoreRepository {
    Set<UUID> loadIgnored(UUID playerId);
    void saveIgnored(UUID playerId, Set<UUID> ignored);
}
```

```java
public interface MuteRepository {
    Optional<MuteRecord> findActiveMute(UUID playerId, Instant now);
    void saveMute(MuteRecord muteRecord);
    void clearMute(UUID playerId);
}
```

## 9.2 YAML implementation minimum

Write YAML implementations for the above repositories first.

Example shape:

```java
public final class YamlPlayerSettingsRepository implements PlayerSettingsRepository {

    private final File file;
    private final YamlConfiguration yaml;

    public YamlPlayerSettingsRepository(File file) {
        this.file = file;
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public PlayerSettingsRecord load(UUID playerId) {
        String path = "players." + playerId;
        return new PlayerSettingsRecord(
                playerId,
                yaml.getBoolean(path + ".pingSound", true),
                yaml.getBoolean(path + ".pingActionbar", true),
                yaml.getBoolean(path + ".pmEnabled", true)
        );
    }

    @Override
    public void save(PlayerSettingsRecord record) {
        String path = "players." + record.playerId();
        yaml.set(path + ".pingSound", record.pingSoundEnabled());
        yaml.set(path + ".pingActionbar", record.pingActionbarEnabled());
        yaml.set(path + ".pmEnabled", record.pmEnabled());
        saveNow();
    }

    private void saveNow() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save player settings YAML", exception);
        }
    }
}
```

---

# 10. Internal event bus

Add a very small internal bus so services do not hard-wire everything directly.

```java
public interface EventBus {
    <T> void subscribe(Class<T> type, Consumer<T> consumer);
    void publish(Object event);
}
```

```java
public final class SimpleEventBus implements EventBus {

    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    @Override
    public synchronized <T> void subscribe(Class<T> type, Consumer<T> consumer) {
        listeners.computeIfAbsent(type, ignored -> new ArrayList<>()).add(consumer);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void publish(Object event) {
        List<Consumer<?>> consumers = listeners.getOrDefault(event.getClass(), List.of());
        for (Consumer<?> consumer : consumers) {
            ((Consumer<Object>) consumer).accept(event);
        }
    }
}
```

---

# 11. Chat pipeline skeleton

Create a reusable pipeline instead of hardcoding every concern in listeners.

## 11.1 Pipeline context

```java
public final class ChatPipelineContext {

    private final UUID senderId;
    private final String senderName;
    private final String rawInput;
    private String sanitizedInput;
    private String channelId;
    private Component renderedMessage;
    private boolean cancelled;
    private String cancelReasonKey;

    public ChatPipelineContext(UUID senderId, String senderName, String rawInput) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.rawInput = rawInput;
        this.sanitizedInput = rawInput;
    }

    // getters/setters omitted for brevity
}
```

## 11.2 Step contract

```java
public interface ChatPipelineStep {
    void apply(ChatPipelineContext context);
}
```

## 11.3 Pipeline orchestrator

```java
public final class DefaultChatPipeline implements ChatPipeline {

    private final List<ChatPipelineStep> steps;

    public DefaultChatPipeline(List<ChatPipelineStep> steps) {
        this.steps = List.copyOf(steps);
    }

    @Override
    public ChatPipelineResult execute(ChatPipelineContext context) {
        for (ChatPipelineStep step : steps) {
            step.apply(context);
            if (context.isCancelled()) {
                return ChatPipelineResult.cancelled(context.getCancelReasonKey());
            }
        }
        return ChatPipelineResult.success(context);
    }
}
```

## 11.4 Required initial step order

1. raw capture
2. sanitization
3. channel resolution
4. privacy checks if relevant
5. moderation checks
6. mention/custom ping parsing
7. formatting/render preparation
8. delivery

---

# 12. Feature foundations that must exist now

These do not all need to be fully feature-complete yet, but the architecture must exist.

## 12.1 Channels foundation

Create `channels.yml` and baseline service.

Example config:

```yml
channels:
  global:
    enabled: true
    default: true
    scope: "SERVER"
    permission-send: ""
    permission-receive: ""
  local:
    enabled: true
    default: false
    scope: "RADIUS"
    radius: 100
    permission-send: "chatbloom.channel.local"
    permission-receive: ""
  staff:
    enabled: true
    default: false
    scope: "SERVER"
    permission-send: "chatbloom.channel.staff"
    permission-receive: "chatbloom.staff"
```

Service baseline:

```java
public final class DefaultChannelService implements ChannelService {

    private final Map<String, ChatChannel> channels;
    private final ActiveChannelRepository activeChannelRepository;

    public DefaultChannelService(Map<String, ChatChannel> channels,
                                 ActiveChannelRepository activeChannelRepository) {
        this.channels = new HashMap<>(channels);
        this.activeChannelRepository = activeChannelRepository;
    }

    @Override
    public Optional<ChatChannel> findChannel(String id) {
        return Optional.ofNullable(channels.get(id.toLowerCase(Locale.ROOT)));
    }

    @Override
    public ChatChannel getDefaultChannel() {
        return channels.values().stream()
                .filter(ChatChannel::defaultChannel)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public void setActiveChannel(UUID playerId, String channelId) {
        activeChannelRepository.saveActiveChannel(playerId, channelId);
    }

    @Override
    public String getActiveChannel(UUID playerId) {
        String stored = activeChannelRepository.loadActiveChannel(playerId);
        return stored == null || stored.isBlank() ? getDefaultChannel().id() : stored;
    }
}
```

## 12.2 Privacy foundation

Create `privacy.yml`:

```yml
private-messages:
  enabled-by-default: true
  allow-staff-bypass: true

ignore:
  enabled: true
  max-ignored-players: 200
```

Service baseline:

```java
public final class DefaultPrivacyService implements PrivacyService {

    private final IgnoreRepository ignoreRepository;
    private final PlayerSettingsRepository playerSettingsRepository;

    public DefaultPrivacyService(IgnoreRepository ignoreRepository,
                                 PlayerSettingsRepository playerSettingsRepository) {
        this.ignoreRepository = ignoreRepository;
        this.playerSettingsRepository = playerSettingsRepository;
    }

    @Override
    public boolean isIgnoring(UUID actor, UUID target) {
        return ignoreRepository.loadIgnored(actor).contains(target);
    }

    @Override
    public void addIgnore(UUID actor, UUID target) {
        Set<UUID> ignored = new HashSet<>(ignoreRepository.loadIgnored(actor));
        ignored.add(target);
        ignoreRepository.saveIgnored(actor, ignored);
    }

    @Override
    public void removeIgnore(UUID actor, UUID target) {
        Set<UUID> ignored = new HashSet<>(ignoreRepository.loadIgnored(actor));
        ignored.remove(target);
        ignoreRepository.saveIgnored(actor, ignored);
    }

    @Override
    public boolean isPmEnabled(UUID playerId) {
        return playerSettingsRepository.load(playerId).pmEnabled();
    }

    @Override
    public void setPmEnabled(UUID playerId, boolean enabled) {
        PlayerSettingsRecord old = playerSettingsRepository.load(playerId);
        playerSettingsRepository.save(new PlayerSettingsRecord(
                old.playerId(),
                old.pingSoundEnabled(),
                old.pingActionbarEnabled(),
                enabled
        ));
    }
}
```

## 12.3 Moderation foundation

Create `moderation.yml`:

```yml
mute:
  enabled: true

anti-repeat:
  enabled: true
  similarity-window: 3
  block-identical: true

anti-caps:
  enabled: false
```

Mute service baseline:

```java
public final class DefaultModerationService implements ModerationService {

    private final MuteRepository muteRepository;
    private final Clock clock;

    public DefaultModerationService(MuteRepository muteRepository, Clock clock) {
        this.muteRepository = muteRepository;
        this.clock = clock;
    }

    @Override
    public ModerationDecision evaluatePublicMessage(ChatPipelineContext context) {
        if (isMuted(context.getSenderId())) {
            return ModerationDecision.cancel("messages.muted");
        }
        return ModerationDecision.allow();
    }

    @Override
    public ModerationDecision evaluatePrivateMessage(ChatPipelineContext context) {
        if (isMuted(context.getSenderId())) {
            return ModerationDecision.cancel("messages.muted");
        }
        return ModerationDecision.allow();
    }

    @Override
    public boolean isMuted(UUID playerId) {
        return muteRepository.findActiveMute(playerId, Instant.now(clock)).isPresent();
    }

    @Override
    public void mute(UUID playerId, Instant until, String reason, UUID actor) {
        muteRepository.saveMute(new MuteRecord(playerId, until, reason, actor));
    }

    @Override
    public void unmute(UUID playerId) {
        muteRepository.clearMute(playerId);
    }
}
```

---

# 13. Network foundation to add now

Do not fully finish proxy mode in this run, but create the required contracts now.

## 13.1 Common packet DTOs

```java
public record ChatMessagePacket(
        UUID senderId,
        String senderName,
        String serverId,
        String channelId,
        String plainText,
        Instant sentAt
) {}
```

```java
public record PrivateMessagePacket(
        UUID senderId,
        UUID targetId,
        String senderName,
        String targetName,
        String plainText,
        Instant sentAt
) {}
```

## 13.2 Paper-side bridge stub

```java
public final class PaperNetworkBridge implements NetworkBridge {

    private final boolean enabled;

    public PaperNetworkBridge(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void publishChat(ChatMessagePacket packet) {
        if (!enabled) {
            return;
        }
        // TODO proxy transport wiring
    }

    @Override
    public void publishPrivateMessage(PrivateMessagePacket packet) {
        if (!enabled) {
            return;
        }
        // TODO proxy transport wiring
    }
}
```

## 13.3 Velocity routing stub

```java
public final class ProxyRoutingService {

    public void routeChat(ChatMessagePacket packet) {
        // TODO resolve recipients across network
    }

    public void routePrivateMessage(PrivateMessagePacket packet) {
        // TODO resolve target backend and forward
    }
}
```

---

# 14. Definition of Done for Run 1

Run 1 is complete only if all of the following are true:

- repo is split into `common`, `paper`, `velocity`
- all modules compile
- Paper jar starts and works in standalone mode
- deployment mode config exists
- switching standalone/proxy mode requires restart and is documented in code/comments
- repository abstraction exists
- YAML repositories are live
- pipeline abstraction exists
- event bus exists
- channel/privacy/moderation service contracts exist
- network DTOs and stubs exist
- no giant mega-class was introduced as a shortcut
- current plugin behavior still mostly works in standalone mode

---

# 15. Test scenarios for this run

Codex must think through these cases while implementing:

- plugin starts with `deployment.mode: STANDALONE`
- plugin starts with `deployment.mode: PROXY`
- config changes mode, then `/chatbloom reload` is used
- invalid deployment mode string exists
- old YAML files still load
- PM works in standalone mode
- mentions still work in standalone mode
- ChatItems still work in standalone mode
- ignore data persists
- mute data persists
- active channel persists
- velocity module compiles even if not feature-complete yet

---

# 16. Anti-patterns to avoid

Do not:
- keep everything inside one giant manager class
- mix Paper APIs into `chatbloom-common`
- hardcode every future feature into listeners
- force proxy mode for everyone
- remove MiniMessage support from config-owned rendering
- break current commands casually
- hide all logic inside anonymous lambdas
- create build outputs that only work in one deployment style

---

# 17. Final instruction to Codex

Implement **only the foundations and architectural refactor** described here.

Be ambitious about code structure, but conservative about behavior changes.

The priority is to leave the project with:
- correct module boundaries
- correct runtime mode model
- correct service/repository abstractions
- correct Paper standalone behavior
- correct proxy-ready foundation

Do **not** try to finish every planned feature in this run.
