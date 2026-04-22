
# ChatBloom — Implementation Spec for Codex (v3, targeted code-heavy)

**Purpose:** this document is meant to be given directly to Codex as an implementation spec and execution blueprint.

**How to use this file with Codex**
- **Run 1:** use this file to set up the architecture and foundations only.
- **Run 2:** use this file again to finish the implementation of the planned modules.
- **Run 3:** use this file again to perform a deep review, polish pass, and consistency check.

**Important note about rendering**
- ChatBloom already supports MiniMessage-driven rendering in config-owned templates.
- Because of that, features like hover text, click actions, player hovercards, and clickable player actions are **not considered missing foundational features**.
- Convenience wrappers for those may be added later, but they are **not** priority architecture tasks.

---

# 0. Executive summary

ChatBloom already covers a good single-server chat core:

- public chat formatting
- input sanitization
- permission-based `&` formatting
- mentions and custom pings
- private messages
- social spy
- broadcasts
- cooldowns
- word filter
- ChatItems
- persistent player/global state
- PlaceholderAPI and LuckPerms fallback handling
- MiniMessage-based config rendering

The next step is to evolve it into a **modular chat suite** with a clean deployment story:

- **Paper standalone** mode must work by itself
- **Proxy-enabled mode** must be optional
- **Two jars** must be produced:
  - `chatbloom-paper.jar`
  - `chatbloom-velocity.jar`
- **Three modules** must exist in the codebase:
  - `chatbloom-common`
  - `chatbloom-paper`
  - `chatbloom-velocity`

This means the plugin must remain useful on a single server, but be architected so a proxy-aware deployment can be activated later by configuration and full restart.

---

# 1. Locked decisions Codex must follow

## 1.1 Deployment model is frozen

This is not negotiable.

### Required project model
- **Three code modules**
  - `chatbloom-common`
  - `chatbloom-paper`
  - `chatbloom-velocity`

### Required build outputs
- **Two runtime jars**
  - `chatbloom-paper-<version>.jar`
  - `chatbloom-velocity-<version>.jar`

### Required runtime modes
- **Standalone mode**
  - only `chatbloom-paper.jar` is installed
  - no proxy dependency exists
  - plugin works as a complete single-server chat plugin
- **Proxy-enabled mode**
  - `chatbloom-velocity.jar` is installed on the proxy
  - `chatbloom-paper.jar` is installed on all Paper backends
  - network-aware features become available
  - changing from standalone to proxy mode, or vice versa, **must require a full restart**
  - this mode must **not** be hot-switched by `/chatbloom reload`

## 1.2 Existing behavior must not be broken casually

The following must continue to work unless a replacement is explicitly requested:

- public chat formatting
- MiniMessage config support
- existing mention/custom ping flow
- `/msg`, `/reply`, `/socialspy`
- broadcast flow
- current sanitization philosophy
- ChatItems snapshot behavior
- reload support
- YAML-first configuration style
- current permission philosophy

## 1.3 Common module must stay platform-neutral

`chatbloom-common` must **not** depend on:

- Paper / Bukkit APIs
- Velocity APIs

It may contain:

- domain models
- config DTOs
- repository interfaces
- service interfaces
- network packet DTOs
- serializers
- shared enums/constants
- common utility classes
- internal event contracts

## 1.4 Rendering source of truth

Use **Adventure `Component`** as the source of truth for final rendered output.

Rules:
- raw user input starts as plain text
- sanitization happens before formatting
- MiniMessage is used only in controlled plugin-owned templates
- do not trust raw player-authored MiniMessage
- prefer component construction or controlled deserialization, not loose string concatenation

## 1.5 Persistence model

Persistence must go through repositories.

Default:
- YAML repositories enabled

Optional:
- SQL repositories enabled later via config:
  - SQLite
  - MySQL
  - MariaDB
  - PostgreSQL

## 1.6 Bridge scope for now

Allowed bridge scope for this plan:
- Discord
- Telegram

Explicitly **out of scope for now**:
- Twitch
- generic webhook bridge system
- arbitrary external HTTP integrations

---

# 2. Project structure that Codex must create

## 2.1 Repository/module structure

```text
chatbloom/
  pom.xml
  chatbloom-common/
    pom.xml
    src/main/java/...
  chatbloom-paper/
    pom.xml
    src/main/java/...
    src/main/resources/plugin.yml
  chatbloom-velocity/
    pom.xml
    src/main/java/...
    src/main/resources/velocity-plugin.json
```

## 2.2 Java package structure

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

## 2.3 Maven parent `pom.xml` baseline

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
        <adventure.version>4.17.0</adventure.version>
    </properties>
</project>
```

## 2.4 `chatbloom-common/pom.xml` baseline

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
            <version>${adventure.version}</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains</groupId>
            <artifactId>annotations</artifactId>
            <version>26.0.2</version>
        </dependency>
    </dependencies>
</project>
```

## 2.5 `chatbloom-paper/pom.xml` baseline

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

        <dependency>
            <groupId>net.kyori</groupId>
            <artifactId>adventure-text-minimessage</artifactId>
            <version>${adventure.version}</version>
        </dependency>
    </dependencies>
</project>
```

## 2.6 `chatbloom-velocity/pom.xml` baseline

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

# 3. Runtime mode model

## 3.1 Required mode enum

```java
package me.icewolf23.chatbloom.common.model;

public enum DeploymentMode {
    STANDALONE,
    PROXY
}
```

## 3.2 Required shared deployment config DTO

```java
package me.icewolf23.chatbloom.common.config;

public record DeploymentSettings(
        DeploymentMode mode,
        boolean requireRestartOnModeChange,
        String serverId,
        String networkChannel,
        boolean bridgesAllowed,
        boolean networkFeaturesAllowed
) {}
```

## 3.3 Required `config.yml` baseline

This file must exist on Paper. On Velocity, the same conceptual deployment block may exist in the proxy config, adapted to proxy needs.

```yml
deployment:
  mode: "STANDALONE" # STANDALONE or PROXY
  require-restart-on-mode-change: true
  server-id: "survival"
  network-channel: "chatbloom:main"
  bridges-allowed: true
  network-features-allowed: false
```

### Rules
- `STANDALONE` must be the default
- `PROXY` must be opt-in
- if the admin changes `mode`, the plugin must log that a **full restart is required**
- `/chatbloom reload` must reload config, but **must not** attempt to live-switch runtime architecture
- if Paper is configured for `PROXY` but no proxy transport is available, log a clear warning and degrade safely

---

# 4. Bootstrap architecture

## 4.1 Required registries

Codex must create at minimum:

- `ConfigRegistry`
- `RepositoryRegistry`
- `ServiceRegistry`
- `CommandRegistry`
- `ListenerRegistry`
- `BridgeRegistry`

## 4.2 Shared service interfaces

```java
package me.icewolf23.chatbloom.common.channel;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ChannelService {
    Optional<ChatChannelDefinition> find(String channelId);
    ChatChannelDefinition getRequired(String channelId);
    String getDefaultChannelId();
    String getActiveChannelId(UUID playerId);
    void setActiveChannelId(UUID playerId, String channelId);
    Collection<ChatChannelDefinition> listAvailableChannels(UUID playerId);
}
```

```java
package me.icewolf23.chatbloom.common.privacy;

import java.util.Set;
import java.util.UUID;

public interface PrivacyService {
    boolean isIgnoring(UUID owner, UUID candidate);
    void ignore(UUID owner, UUID candidate);
    void unignore(UUID owner, UUID candidate);
    Set<UUID> getIgnoredPlayers(UUID owner);

    boolean isPrivateMessagesEnabled(UUID playerId);
    void setPrivateMessagesEnabled(UUID playerId, boolean enabled);

    boolean shouldAllowPrivateMessage(UUID senderId, UUID targetId, boolean senderBypass);
    boolean shouldNotifyMention(UUID senderId, UUID targetId, boolean senderBypass);
}
```

```java
package me.icewolf23.chatbloom.common.moderation;

import java.util.Optional;
import java.util.UUID;

public interface MuteService {
    Optional<MuteRecord> getActiveMute(UUID playerId, long now);
    void mute(UUID target, Long expiresAtMillis, String reason, UUID actorId, boolean blocksPm);
    void unmute(UUID target);
    boolean isMuted(UUID target, long now);
}
```

## 4.3 Common domain records

```java
package me.icewolf23.chatbloom.common.channel;

public enum ChannelScope {
    GLOBAL,
    LOCAL_RADIUS,
    WORLD,
    STAFF,
    CUSTOM,
    NETWORK
}
```

```java
package me.icewolf23.chatbloom.common.channel;

public record ChatChannelDefinition(
        String id,
        boolean enabled,
        ChannelScope scope,
        Integer radius,
        String sendPermission,
        String receivePermission,
        boolean allowMentions,
        boolean allowChatItems,
        boolean exportToBridges,
        String format
) {}
```

```java
package me.icewolf23.chatbloom.common.model;

import java.util.UUID;

public record PlayerChatState(
        UUID playerId,
        String activeChannelId,
        boolean pingSoundEnabled,
        boolean pingActionbarEnabled,
        boolean privateMessagesEnabled,
        boolean mentionNotificationsEnabled,
        boolean staffChatEnabled,
        String localeTag
) {}
```

```java
package me.icewolf23.chatbloom.common.moderation;

import java.util.UUID;

public record MuteRecord(
        UUID playerId,
        long createdAtMillis,
        Long expiresAtMillis,
        String reason,
        UUID actorId,
        boolean blocksPrivateMessages
) {
    public boolean isActive(long now) {
        return expiresAtMillis == null || expiresAtMillis > now;
    }
}
```

## 4.4 Paper plugin bootstrap (near-complete baseline)

```java
package me.icewolf23.chatbloom.paper.bootstrap;

import me.icewolf23.chatbloom.common.config.DeploymentSettings;
import me.icewolf23.chatbloom.paper.command.CommandRegistry;
import me.icewolf23.chatbloom.paper.listener.ListenerRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatBloomPaperPlugin extends JavaPlugin {

    private ConfigRegistry configRegistry;
    private RepositoryRegistry repositoryRegistry;
    private ServiceRegistry serviceRegistry;
    private BridgeRegistry bridgeRegistry;
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configRegistry = new ConfigRegistry(this);
        this.configRegistry.loadAll();

        DeploymentSettings deployment = this.configRegistry.mainConfig().deployment();
        getLogger().info("ChatBloom deployment mode: " + deployment.mode());

        this.repositoryRegistry = new RepositoryRegistry(this, configRegistry);
        this.repositoryRegistry.initialize();

        this.serviceRegistry = new ServiceRegistry(this, configRegistry, repositoryRegistry);
        this.serviceRegistry.initialize();

        this.bridgeRegistry = new BridgeRegistry(this, configRegistry, serviceRegistry);
        this.bridgeRegistry.initialize();

        this.commandRegistry = new CommandRegistry(this, configRegistry, serviceRegistry, bridgeRegistry);
        this.commandRegistry.registerAll();

        this.listenerRegistry = new ListenerRegistry(this, configRegistry, serviceRegistry, bridgeRegistry);
        this.listenerRegistry.registerAll();

        if (deployment.mode().name().equals("PROXY")) {
            getLogger().warning("Proxy mode is enabled. Make sure chatbloom-velocity is installed on the proxy and all Paper servers were restarted after changing mode.");
        } else {
            getLogger().info("Standalone mode active.");
        }
    }

    @Override
    public void onDisable() {
        if (bridgeRegistry != null) {
            bridgeRegistry.shutdown();
        }
        if (repositoryRegistry != null) {
            repositoryRegistry.shutdown();
        }
    }

    public ConfigRegistry configs() {
        return configRegistry;
    }

    public ServiceRegistry services() {
        return serviceRegistry;
    }
}
```

## 4.5 Velocity plugin bootstrap (near-complete baseline)

```java
package me.icewolf23.chatbloom.velocity.bootstrap;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(
        id = "chatbloom",
        name = "ChatBloom",
        version = "1.0.0-SNAPSHOT"
)
public final class ChatBloomVelocityPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigRegistry configRegistry;
    private RepositoryRegistry repositoryRegistry;
    private ServiceRegistry serviceRegistry;
    private BridgeRegistry bridgeRegistry;
    private CommandRegistry commandRegistry;
    private ListenerRegistry listenerRegistry;

    @Inject
    public ChatBloomVelocityPlugin(ProxyServer proxyServer, Logger logger, @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.configRegistry = new ConfigRegistry(dataDirectory, logger);
        this.configRegistry.loadAll();

        this.repositoryRegistry = new RepositoryRegistry(configRegistry, logger);
        this.repositoryRegistry.initialize();

        this.serviceRegistry = new ServiceRegistry(proxyServer, logger, configRegistry, repositoryRegistry);
        this.serviceRegistry.initialize();

        this.bridgeRegistry = new BridgeRegistry(proxyServer, logger, configRegistry, serviceRegistry);
        this.bridgeRegistry.initialize();

        this.commandRegistry = new CommandRegistry(proxyServer, logger, configRegistry, serviceRegistry, bridgeRegistry);
        this.commandRegistry.registerAll();

        this.listenerRegistry = new ListenerRegistry(proxyServer, logger, configRegistry, serviceRegistry, bridgeRegistry);
        this.listenerRegistry.registerAll();

        logger.info("ChatBloom Velocity bootstrap completed.");
    }
}
```

---

# 5. Configuration ownership

Codex must keep config ownership clear.

## 5.1 Existing files that must remain valid
- `config.yml`
- `messages.yml`
- `chat.yml`
- `pings.yml`
- `filter.yml`
- `chatitems.yml`
- `playerdata.yml`
- `state.yml`

## 5.2 New files to add
- `channels.yml`
- `privacy.yml`
- `moderation.yml`
- `storage.yml`
- `discord.yml`
- `telegram.yml`
- `locales/<locale>.yml`

## 5.3 Ownership rules
- `config.yml` → deployment mode, global toggles, hook flags, bridge enable toggles
- `chat.yml` → public chat format, mention rendering, channel fallback formatting
- `messages.yml` → user-facing messages and command feedback
- `pings.yml` → mention and custom ping behavior
- `channels.yml` → channel registry and scope behavior
- `privacy.yml` → ignore/PM privacy rules
- `moderation.yml` → mute, mutechat, anti-spam
- `storage.yml` → backend selection and SQL options
- `discord.yml` → Discord bridge config
- `telegram.yml` → Telegram bridge config

---

# 6. Repositories and persistence

## 6.1 Repository interfaces

```java
package me.icewolf23.chatbloom.common.storage.repository;

import java.util.Optional;
import java.util.UUID;
import me.icewolf23.chatbloom.common.model.PlayerChatState;

public interface PlayerStateRepository {
    Optional<PlayerChatState> find(UUID playerId);
    PlayerChatState loadOrCreate(UUID playerId);
    void save(PlayerChatState state);
}
```

```java
package me.icewolf23.chatbloom.common.storage.repository;

import java.util.Set;
import java.util.UUID;

public interface IgnoreRepository {
    Set<UUID> getIgnoredPlayers(UUID playerId);
    void addIgnoredPlayer(UUID playerId, UUID ignoredPlayerId);
    void removeIgnoredPlayer(UUID playerId, UUID ignoredPlayerId);
    boolean isIgnoring(UUID playerId, UUID ignoredPlayerId);
}
```

```java
package me.icewolf23.chatbloom.common.storage.repository;

import java.util.Optional;
import java.util.UUID;
import me.icewolf23.chatbloom.common.moderation.MuteRecord;

public interface MuteRepository {
    Optional<MuteRecord> findActive(UUID playerId, long now);
    void save(MuteRecord record);
    void clear(UUID playerId);
}
```

## 6.2 YAML repository baseline

```java
package me.icewolf23.chatbloom.paper.storage.yaml;

import java.util.Optional;
import java.util.UUID;
import me.icewolf23.chatbloom.common.model.PlayerChatState;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlPlayerStateRepository implements PlayerStateRepository {

    private final YamlConfiguration yaml;

    public YamlPlayerStateRepository(YamlConfiguration yaml) {
        this.yaml = yaml;
    }

    @Override
    public Optional<PlayerChatState> find(UUID playerId) {
        String path = "players." + playerId;
        if (!yaml.contains(path)) {
            return Optional.empty();
        }

        return Optional.of(new PlayerChatState(
                playerId,
                yaml.getString(path + ".active-channel", "global"),
                yaml.getBoolean(path + ".ping-sound-enabled", true),
                yaml.getBoolean(path + ".ping-actionbar-enabled", true),
                yaml.getBoolean(path + ".private-messages-enabled", true),
                yaml.getBoolean(path + ".mention-notifications-enabled", true),
                yaml.getBoolean(path + ".staff-chat-enabled", false),
                yaml.getString(path + ".locale", "en_us")
        ));
    }

    @Override
    public PlayerChatState loadOrCreate(UUID playerId) {
        return find(playerId).orElse(new PlayerChatState(
                playerId,
                "global",
                true,
                true,
                true,
                true,
                false,
                "en_us"
        ));
    }

    @Override
    public void save(PlayerChatState state) {
        String path = "players." + state.playerId();
        yaml.set(path + ".active-channel", state.activeChannelId());
        yaml.set(path + ".ping-sound-enabled", state.pingSoundEnabled());
        yaml.set(path + ".ping-actionbar-enabled", state.pingActionbarEnabled());
        yaml.set(path + ".private-messages-enabled", state.privateMessagesEnabled());
        yaml.set(path + ".mention-notifications-enabled", state.mentionNotificationsEnabled());
        yaml.set(path + ".staff-chat-enabled", state.staffChatEnabled());
        yaml.set(path + ".locale", state.localeTag());
    }
}
```

## 6.3 SQL schema baseline

```sql
create table if not exists cb_player_state (
    player_id varchar(36) primary key,
    active_channel varchar(64) not null,
    ping_sound_enabled boolean not null,
    ping_actionbar_enabled boolean not null,
    private_messages_enabled boolean not null,
    mention_notifications_enabled boolean not null,
    staff_chat_enabled boolean not null,
    locale_tag varchar(32) not null
);

create table if not exists cb_ignore (
    player_id varchar(36) not null,
    ignored_player_id varchar(36) not null,
    created_at bigint not null,
    primary key (player_id, ignored_player_id)
);

create table if not exists cb_mute (
    player_id varchar(36) primary key,
    created_at bigint not null,
    expires_at bigint null,
    reason text not null,
    actor_id varchar(36) not null,
    blocks_private_messages boolean not null
);
```

---

# 7. Internal event bus

## 7.1 Contracts

```java
package me.icewolf23.chatbloom.common.event;

public interface ChatBloomEvent {}
```

```java
package me.icewolf23.chatbloom.common.event;

import java.util.function.Consumer;

public interface ChatBloomEventBus {
    <T extends ChatBloomEvent> void subscribe(Class<T> type, Consumer<T> consumer);
    void publish(ChatBloomEvent event);
}
```

## 7.2 Simple implementation

```java
package me.icewolf23.chatbloom.common.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SimpleChatBloomEventBus implements ChatBloomEventBus {

    private final Map<Class<?>, List<Consumer<?>>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public <T extends ChatBloomEvent> void subscribe(Class<T> type, Consumer<T> consumer) {
        subscriptions.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(consumer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void publish(ChatBloomEvent event) {
        var consumers = subscriptions.get(event.getClass());
        if (consumers == null) {
            return;
        }
        for (Consumer<?> consumer : consumers) {
            ((Consumer<ChatBloomEvent>) consumer).accept(event);
        }
    }
}
```

---

# 8. Chat pipeline

## 8.1 Pipeline context

```java
package me.icewolf23.chatbloom.common.pipeline;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;

public final class ChatMessageContext {

    private final UUID senderId;
    private final String senderName;
    private final boolean consoleSender;
    private final String rawInput;
    private String sanitizedInput;
    private String activeChannelId;
    private final long createdAtMillis;
    private final Set<UUID> targetPlayerIds = new HashSet<>();
    private final Map<String, Object> attributes = new HashMap<>();

    private Component renderedMessage;
    private Component rejectionMessage;

    public ChatMessageContext(UUID senderId, String senderName, boolean consoleSender, String rawInput, String activeChannelId, long createdAtMillis) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.consoleSender = consoleSender;
        this.rawInput = rawInput;
        this.sanitizedInput = rawInput;
        this.activeChannelId = activeChannelId;
        this.createdAtMillis = createdAtMillis;
    }

    public UUID senderId() { return senderId; }
    public String senderName() { return senderName; }
    public boolean consoleSender() { return consoleSender; }
    public String rawInput() { return rawInput; }
    public String sanitizedInput() { return sanitizedInput; }
    public void setSanitizedInput(String sanitizedInput) { this.sanitizedInput = sanitizedInput; }
    public String activeChannelId() { return activeChannelId; }
    public void setActiveChannelId(String activeChannelId) { this.activeChannelId = activeChannelId; }
    public long createdAtMillis() { return createdAtMillis; }
    public Set<UUID> targetPlayerIds() { return targetPlayerIds; }
    public Map<String, Object> attributes() { return attributes; }
    public Component renderedMessage() { return renderedMessage; }
    public void setRenderedMessage(Component renderedMessage) { this.renderedMessage = renderedMessage; }
    public Component rejectionMessage() { return rejectionMessage; }
    public void setRejectionMessage(Component rejectionMessage) { this.rejectionMessage = rejectionMessage; }
}
```

## 8.2 Step contract

```java
package me.icewolf23.chatbloom.common.pipeline;

import org.jetbrains.annotations.Nullable;
import net.kyori.adventure.text.Component;

public record ChatPipelineResult(boolean continueProcessing, @Nullable Component rejectionMessage) {
    public static ChatPipelineResult ok() {
        return new ChatPipelineResult(true, null);
    }

    public static ChatPipelineResult stop(Component component) {
        return new ChatPipelineResult(false, component);
    }
}
```

```java
package me.icewolf23.chatbloom.common.pipeline;

public interface ChatPipelineStep {
    ChatPipelineResult process(ChatMessageContext context);
}
```

## 8.3 Pipeline orchestrator

```java
package me.icewolf23.chatbloom.common.pipeline;

import java.util.List;

public final class ChatPipelineOrchestrator {

    private final List<ChatPipelineStep> steps;

    public ChatPipelineOrchestrator(List<ChatPipelineStep> steps) {
        this.steps = List.copyOf(steps);
    }

    public ChatPipelineResult execute(ChatMessageContext context) {
        for (ChatPipelineStep step : steps) {
            ChatPipelineResult result = step.process(context);
            if (!result.continueProcessing()) {
                context.setRejectionMessage(result.rejectionMessage());
                return result;
            }
        }
        return ChatPipelineResult.ok();
    }
}
```

## 8.4 Recommended step order

1. load sender state
2. sanitize input
3. resolve active channel
4. apply mute / mutechat / anti-spam
5. parse mentions and custom pings
6. parse ChatItems
7. resolve recipients
8. render final component
9. deliver
10. audit
11. bridge dispatch

## 8.5 Example sanitization step

```java
package me.icewolf23.chatbloom.paper.pipeline;

import me.icewolf23.chatbloom.common.pipeline.ChatMessageContext;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineResult;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineStep;

public final class SanitizationStep implements ChatPipelineStep {

    private final InputSanitizer sanitizer;

    public SanitizationStep(InputSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    @Override
    public ChatPipelineResult process(ChatMessageContext context) {
        String sanitized = sanitizer.sanitize(context.rawInput());
        context.setSanitizedInput(sanitized);
        return ChatPipelineResult.ok();
    }
}
```

## 8.6 Example moderation step

```java
package me.icewolf23.chatbloom.paper.pipeline;

import me.icewolf23.chatbloom.common.locale.LocaleService;
import me.icewolf23.chatbloom.common.moderation.MuteService;
import me.icewolf23.chatbloom.common.pipeline.ChatMessageContext;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineResult;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineStep;

public final class ModerationGateStep implements ChatPipelineStep {

    private final MuteService muteService;
    private final GlobalMuteChatService globalMuteChatService;
    private final LocaleService localeService;
    private final PermissionAdapter permissionAdapter;

    public ModerationGateStep(MuteService muteService,
                              GlobalMuteChatService globalMuteChatService,
                              LocaleService localeService,
                              PermissionAdapter permissionAdapter) {
        this.muteService = muteService;
        this.globalMuteChatService = globalMuteChatService;
        this.localeService = localeService;
        this.permissionAdapter = permissionAdapter;
    }

    @Override
    public ChatPipelineResult process(ChatMessageContext context) {
        long now = System.currentTimeMillis();

        if (globalMuteChatService.isActive() && !permissionAdapter.hasMuteChatBypass(context.senderId())) {
            return ChatPipelineResult.stop(localeService.message(context.senderId(), "moderation.mutechat.active"));
        }

        if (muteService.isMuted(context.senderId(), now) && !permissionAdapter.hasMuteBypass(context.senderId())) {
            return ChatPipelineResult.stop(localeService.message(context.senderId(), "moderation.mute.active"));
        }

        return ChatPipelineResult.ok();
    }
}
```

---

# 9. Channel system

## 9.1 `channels.yml` baseline

```yml
default-channel: "global"

channels:
  global:
    enabled: true
    scope: "GLOBAL"
    send-permission: ""
    receive-permission: ""
    allow-mentions: true
    allow-chatitems: true
    export-to-bridges: true
    format: "{plugin_prefix} <white>{player_name}</white><dark_gray> » </dark_gray>{message}"

  local:
    enabled: true
    scope: "LOCAL_RADIUS"
    radius: 120
    send-permission: ""
    receive-permission: ""
    allow-mentions: true
    allow-chatitems: true
    export-to-bridges: false
    format: "<gray>[Local]</gray> <white>{player_name}</white><dark_gray> » </dark_gray>{message}"

  staff:
    enabled: true
    scope: "STAFF"
    send-permission: "chatbloom.staffchat.use"
    receive-permission: "chatbloom.staff"
    allow-mentions: false
    allow-chatitems: false
    export-to-bridges: false
    format: "<gold>[Staff]</gold> <white>{player_name}</white><dark_gray> » </dark_gray>{message}"
```

## 9.2 Channel service implementation baseline

```java
package me.icewolf23.chatbloom.paper.channel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.icewolf23.chatbloom.common.channel.ChannelService;
import me.icewolf23.chatbloom.common.channel.ChatChannelDefinition;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;

public final class DefaultChannelService implements ChannelService {

    private final Map<String, ChatChannelDefinition> channelsById;
    private final String defaultChannelId;
    private final PlayerStateRepository playerStateRepository;
    private final ChannelAccessEvaluator accessEvaluator;

    public DefaultChannelService(Collection<ChatChannelDefinition> channels,
                                 String defaultChannelId,
                                 PlayerStateRepository playerStateRepository,
                                 ChannelAccessEvaluator accessEvaluator) {
        this.channelsById = new LinkedHashMap<>();
        for (ChatChannelDefinition def : channels) {
            this.channelsById.put(def.id().toLowerCase(), def);
        }
        this.defaultChannelId = defaultChannelId.toLowerCase();
        this.playerStateRepository = playerStateRepository;
        this.accessEvaluator = accessEvaluator;
    }

    @Override
    public Optional<ChatChannelDefinition> find(String channelId) {
        if (channelId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(channelsById.get(channelId.toLowerCase()));
    }

    @Override
    public ChatChannelDefinition getRequired(String channelId) {
        return find(channelId).orElseThrow(() -> new IllegalArgumentException("Unknown channel: " + channelId));
    }

    @Override
    public String getDefaultChannelId() {
        return defaultChannelId;
    }

    @Override
    public String getActiveChannelId(UUID playerId) {
        var state = playerStateRepository.loadOrCreate(playerId);
        String current = state.activeChannelId();
        if (find(current).isEmpty()) {
            return defaultChannelId;
        }
        return current;
    }

    @Override
    public void setActiveChannelId(UUID playerId, String channelId) {
        ChatChannelDefinition definition = getRequired(channelId);
        var old = playerStateRepository.loadOrCreate(playerId);
        var updated = new me.icewolf23.chatbloom.common.model.PlayerChatState(
                old.playerId(),
                definition.id(),
                old.pingSoundEnabled(),
                old.pingActionbarEnabled(),
                old.privateMessagesEnabled(),
                old.mentionNotificationsEnabled(),
                old.staffChatEnabled(),
                old.localeTag()
        );
        playerStateRepository.save(updated);
    }

    @Override
    public Collection<ChatChannelDefinition> listAvailableChannels(UUID playerId) {
        return channelsById.values().stream()
                .filter(ChatChannelDefinition::enabled)
                .filter(def -> accessEvaluator.canUseChannel(playerId, def))
                .toList();
    }
}
```

## 9.3 Local recipient resolver

```java
package me.icewolf23.chatbloom.paper.channel;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Player;

public final class BukkitLocalRecipientResolver {

    public Set<Player> resolve(Player sender, int radius, boolean requireSameWorld) {
        Set<Player> recipients = new HashSet<>();
        recipients.add(sender);

        for (Player online : sender.getServer().getOnlinePlayers()) {
            if (online.getUniqueId().equals(sender.getUniqueId())) {
                continue;
            }

            if (requireSameWorld && !online.getWorld().equals(sender.getWorld())) {
                continue;
            }

            if (online.getLocation().distanceSquared(sender.getLocation()) <= (radius * radius)) {
                recipients.add(online);
            }
        }

        return recipients;
    }
}
```

---

# 10. Privacy system

## 10.1 `privacy.yml` baseline

```yml
ignore:
  enabled: true
  max-entries-per-player: 200

private-messages:
  allow-toggle: true
  default-enabled: true
  staff-bypass-permission: "chatbloom.privacy.bypass"

mentions:
  respect-ignore-list: true
  respect-mention-toggle: true
```

## 10.2 Privacy service baseline

```java
package me.icewolf23.chatbloom.paper.privacy;

import java.util.Set;
import java.util.UUID;
import me.icewolf23.chatbloom.common.config.PrivacyConfig;
import me.icewolf23.chatbloom.common.model.PlayerChatState;
import me.icewolf23.chatbloom.common.privacy.PrivacyService;
import me.icewolf23.chatbloom.common.storage.repository.IgnoreRepository;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;

public final class DefaultPrivacyService implements PrivacyService {

    private final PrivacyConfig config;
    private final IgnoreRepository ignoreRepository;
    private final PlayerStateRepository playerStateRepository;

    public DefaultPrivacyService(PrivacyConfig config,
                                 IgnoreRepository ignoreRepository,
                                 PlayerStateRepository playerStateRepository) {
        this.config = config;
        this.ignoreRepository = ignoreRepository;
        this.playerStateRepository = playerStateRepository;
    }

    @Override
    public boolean isIgnoring(UUID owner, UUID candidate) {
        return ignoreRepository.isIgnoring(owner, candidate);
    }

    @Override
    public void ignore(UUID owner, UUID candidate) {
        ignoreRepository.addIgnoredPlayer(owner, candidate);
    }

    @Override
    public void unignore(UUID owner, UUID candidate) {
        ignoreRepository.removeIgnoredPlayer(owner, candidate);
    }

    @Override
    public Set<UUID> getIgnoredPlayers(UUID owner) {
        return ignoreRepository.getIgnoredPlayers(owner);
    }

    @Override
    public boolean isPrivateMessagesEnabled(UUID playerId) {
        return playerStateRepository.loadOrCreate(playerId).privateMessagesEnabled();
    }

    @Override
    public void setPrivateMessagesEnabled(UUID playerId, boolean enabled) {
        PlayerChatState old = playerStateRepository.loadOrCreate(playerId);
        PlayerChatState updated = new PlayerChatState(
                old.playerId(),
                old.activeChannelId(),
                old.pingSoundEnabled(),
                old.pingActionbarEnabled(),
                enabled,
                old.mentionNotificationsEnabled(),
                old.staffChatEnabled(),
                old.localeTag()
        );
        playerStateRepository.save(updated);
    }

    @Override
    public boolean shouldAllowPrivateMessage(UUID senderId, UUID targetId, boolean senderBypass) {
        if (senderBypass) {
            return true;
        }
        if (ignoreRepository.isIgnoring(targetId, senderId)) {
            return false;
        }
        return isPrivateMessagesEnabled(targetId);
    }

    @Override
    public boolean shouldNotifyMention(UUID senderId, UUID targetId, boolean senderBypass) {
        if (senderBypass) {
            return true;
        }
        if (config.mentionsRespectIgnoreList() && ignoreRepository.isIgnoring(targetId, senderId)) {
            return false;
        }
        return playerStateRepository.loadOrCreate(targetId).mentionNotificationsEnabled();
    }
}
```

## 10.3 Ignore command baseline

```java
package me.icewolf23.chatbloom.paper.command.privacy;

import me.icewolf23.chatbloom.common.locale.LocaleService;
import me.icewolf23.chatbloom.common.privacy.PrivacyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class IgnoreCommand implements CommandExecutor {

    private final PrivacyService privacyService;
    private final PlayerLookupService playerLookupService;
    private final LocaleService localeService;

    public IgnoreCommand(PrivacyService privacyService,
                         PlayerLookupService playerLookupService,
                         LocaleService localeService) {
        this.privacyService = privacyService;
        this.playerLookupService = playerLookupService;
        this.localeService = localeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(localeService.message(player.getUniqueId(), "privacy.ignore.usage"));
            return true;
        }

        var target = playerLookupService.findOnlineOrKnown(args[0]);
        if (target == null) {
            player.sendMessage(localeService.message(player.getUniqueId(), "general.player_not_found"));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(localeService.message(player.getUniqueId(), "privacy.ignore.self"));
            return true;
        }

        if (privacyService.isIgnoring(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(localeService.message(player.getUniqueId(), "privacy.ignore.already"));
            return true;
        }

        privacyService.ignore(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(localeService.message(player.getUniqueId(), "privacy.ignore.success")
                .replaceText(builder -> builder.matchLiteral("{target_name}").replacement(target.getName())));
        return true;
    }
}
```

---

# 11. Moderation system

## 11.1 `moderation.yml` baseline

```yml
mute:
  enabled: true
  block-private-messages: true
  default-reason: "No reason provided"
  notify-staff-on-mute: true

mutechat:
  enabled: true
  persist-state: true

anti-spam:
  anti-repeat:
    enabled: true
    max-equal-window: 2
  anti-caps:
    enabled: true
    min-length: 8
    max-uppercase-ratio: 0.7
  anti-links:
    enabled: false
  anti-advertising:
    enabled: false
```

## 11.2 Mute service baseline

```java
package me.icewolf23.chatbloom.paper.moderation;

import java.util.Optional;
import java.util.UUID;
import me.icewolf23.chatbloom.common.moderation.MuteRecord;
import me.icewolf23.chatbloom.common.moderation.MuteService;
import me.icewolf23.chatbloom.common.storage.repository.MuteRepository;

public final class DefaultMuteService implements MuteService {

    private final MuteRepository muteRepository;

    public DefaultMuteService(MuteRepository muteRepository) {
        this.muteRepository = muteRepository;
    }

    @Override
    public Optional<MuteRecord> getActiveMute(UUID playerId, long now) {
        return muteRepository.findActive(playerId, now).filter(m -> m.isActive(now));
    }

    @Override
    public void mute(UUID target, Long expiresAtMillis, String reason, UUID actorId, boolean blocksPm) {
        muteRepository.save(new MuteRecord(
                target,
                System.currentTimeMillis(),
                expiresAtMillis,
                reason,
                actorId,
                blocksPm
        ));
    }

    @Override
    public void unmute(UUID target) {
        muteRepository.clear(target);
    }

    @Override
    public boolean isMuted(UUID target, long now) {
        return getActiveMute(target, now).isPresent();
    }
}
```

## 11.3 Anti-repeat policy baseline

```java
package me.icewolf23.chatbloom.paper.moderation.policy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AntiRepeatPolicy {

    private final int maxEqualWindow;
    private final Map<UUID, Deque<String>> recentMessages = new HashMap<>();

    public AntiRepeatPolicy(int maxEqualWindow) {
        this.maxEqualWindow = maxEqualWindow;
    }

    public boolean violates(UUID playerId, String normalizedMessage) {
        Deque<String> history = recentMessages.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        long duplicates = history.stream().filter(normalizedMessage::equals).count();
        return duplicates >= maxEqualWindow;
    }

    public void record(UUID playerId, String normalizedMessage) {
        Deque<String> history = recentMessages.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        history.addLast(normalizedMessage);
        while (history.size() > 5) {
            history.removeFirst();
        }
    }
}
```

## 11.4 Mute command baseline

```java
package me.icewolf23.chatbloom.paper.command.moderation;

import java.time.Duration;
import java.util.UUID;
import me.icewolf23.chatbloom.common.locale.LocaleService;
import me.icewolf23.chatbloom.common.moderation.MuteService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class MuteCommand implements CommandExecutor {

    private final PlayerLookupService playerLookupService;
    private final MuteService muteService;
    private final DurationParser durationParser;
    private final LocaleService localeService;

    public MuteCommand(PlayerLookupService playerLookupService,
                       MuteService muteService,
                       DurationParser durationParser,
                       LocaleService localeService) {
        this.playerLookupService = playerLookupService;
        this.muteService = muteService;
        this.durationParser = durationParser;
        this.localeService = localeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /mute <player> <duration|permanent> [reason...]");
            return true;
        }

        var target = playerLookupService.findOnlineOrKnown(args[0]);
        if (target == null) {
            sender.sendMessage("Player not found.");
            return true;
        }

        String durationToken = args[1];
        Long expiresAtMillis = null;

        if (!durationToken.equalsIgnoreCase("permanent")) {
            Duration duration = durationParser.parseRequired(durationToken);
            expiresAtMillis = System.currentTimeMillis() + duration.toMillis();
        }

        String reason = args.length >= 3
                ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length))
                : "No reason provided";

        UUID actorId = sender instanceof org.bukkit.entity.Player p ? p.getUniqueId() : new UUID(0L, 0L);
        muteService.mute(target.getUniqueId(), expiresAtMillis, reason, actorId, true);

        sender.sendMessage("Muted " + target.getName() + ".");
        return true;
    }
}
```

---

# 12. Settings GUI foundation

## 12.1 Goals
The first GUI pass does not need to be beautiful. It needs to be stable, persistent, and useful.

### Initial toggles
- ping sound
- ping actionbar
- private messages enabled
- mention notifications enabled
- staff chat enabled
- locale selector later

## 12.2 Inventory UI baseline

```java
package me.icewolf23.chatbloom.paper.gui;

import me.icewolf23.chatbloom.common.model.PlayerChatState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class SettingsMenuFactory {

    public Inventory create(Player player, PlayerChatState state) {
        Inventory inv = Bukkit.createInventory(null, 27, "ChatBloom Settings");

        inv.setItem(10, toggleItem(Material.NOTE_BLOCK, "Ping Sound", state.pingSoundEnabled()));
        inv.setItem(11, toggleItem(Material.PAPER, "Ping Actionbar", state.pingActionbarEnabled()));
        inv.setItem(12, toggleItem(Material.WRITABLE_BOOK, "Private Messages", state.privateMessagesEnabled()));
        inv.setItem(13, toggleItem(Material.BELL, "Mention Notifications", state.mentionNotificationsEnabled()));
        inv.setItem(14, toggleItem(Material.GOLDEN_HELMET, "Staff Chat", state.staffChatEnabled()));

        return inv;
    }

    private ItemStack toggleItem(Material material, String label, boolean enabled) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(label + ": " + (enabled ? "Enabled" : "Disabled")));
        item.setItemMeta(meta);
        return item;
    }
}
```

## 12.3 Inventory click listener baseline

```java
package me.icewolf23.chatbloom.paper.gui;

import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class SettingsMenuListener implements Listener {

    private final PlayerStateRepository playerStateRepository;

    public SettingsMenuListener(PlayerStateRepository playerStateRepository) {
        this.playerStateRepository = playerStateRepository;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().title() == null || !event.getView().title().toString().contains("ChatBloom Settings")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            return;
        }

        var state = playerStateRepository.loadOrCreate(player.getUniqueId());
        int slot = event.getRawSlot();

        var updated = switch (slot) {
            case 10 -> new me.icewolf23.chatbloom.common.model.PlayerChatState(
                    state.playerId(), state.activeChannelId(), !state.pingSoundEnabled(), state.pingActionbarEnabled(),
                    state.privateMessagesEnabled(), state.mentionNotificationsEnabled(), state.staffChatEnabled(), state.localeTag());
            case 11 -> new me.icewolf23.chatbloom.common.model.PlayerChatState(
                    state.playerId(), state.activeChannelId(), state.pingSoundEnabled(), !state.pingActionbarEnabled(),
                    state.privateMessagesEnabled(), state.mentionNotificationsEnabled(), state.staffChatEnabled(), state.localeTag());
            case 12 -> new me.icewolf23.chatbloom.common.model.PlayerChatState(
                    state.playerId(), state.activeChannelId(), state.pingSoundEnabled(), state.pingActionbarEnabled(),
                    !state.privateMessagesEnabled(), state.mentionNotificationsEnabled(), state.staffChatEnabled(), state.localeTag());
            case 13 -> new me.icewolf23.chatbloom.common.model.PlayerChatState(
                    state.playerId(), state.activeChannelId(), state.pingSoundEnabled(), state.pingActionbarEnabled(),
                    state.privateMessagesEnabled(), !state.mentionNotificationsEnabled(), state.staffChatEnabled(), state.localeTag());
            case 14 -> new me.icewolf23.chatbloom.common.model.PlayerChatState(
                    state.playerId(), state.activeChannelId(), state.pingSoundEnabled(), state.pingActionbarEnabled(),
                    state.privateMessagesEnabled(), state.mentionNotificationsEnabled(), !state.staffChatEnabled(), state.localeTag());
            default -> null;
        };

        if (updated == null) {
            return;
        }

        playerStateRepository.save(updated);
        player.closeInventory();
    }
}
```

---

# 13. Network mode foundation

## 13.1 Design goals

Proxy mode must be a **layer on top of** standalone Paper behavior, not a replacement of the local architecture.

### Required idea
- local Paper behavior remains valid
- proxy mode adds routing and shared authority where needed
- switching mode requires install + config + restart

## 13.2 Required common network contracts

```java
package me.icewolf23.chatbloom.common.network;

public enum NetworkPacketType {
    CHAT_MESSAGE,
    PRIVATE_MESSAGE,
    CHANNEL_SWITCH,
    CHATITEM_SNAPSHOT_CREATE,
    CHATITEM_SNAPSHOT_REQUEST,
    CHATITEM_SNAPSHOT_RESPONSE,
    PLAYER_STATE_SYNC,
    MUTE_STATE_SYNC,
    IGNORE_STATE_SYNC,
    BRIDGE_FORWARD
}
```

```java
package me.icewolf23.chatbloom.common.network;

public record NetworkEnvelope(
        int protocolVersion,
        NetworkPacketType packetType,
        String sourceServer,
        String targetServer,
        String payloadJson
) {}
```

```java
package me.icewolf23.chatbloom.common.network;

public interface NetworkTransport {
    boolean isAvailable();
    void send(NetworkEnvelope envelope);
}
```

## 13.3 Chat message packet baseline

```java
package me.icewolf23.chatbloom.common.network.packet;

public record ChatMessagePacket(
        String senderUuid,
        String senderName,
        String channelId,
        String plainText,
        String renderedJson,
        long createdAtMillis
) {}
```

## 13.4 Paper-side network bridge stub

```java
package me.icewolf23.chatbloom.paper.network;

import me.icewolf23.chatbloom.common.network.NetworkEnvelope;
import me.icewolf23.chatbloom.common.network.NetworkTransport;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperNetworkTransport implements NetworkTransport {

    private final JavaPlugin plugin;
    private boolean available;

    public PaperNetworkTransport(JavaPlugin plugin) {
        this.plugin = plugin;
        this.available = false;
    }

    public void initialize(boolean proxyModeEnabled) {
        this.available = proxyModeEnabled;
        if (proxyModeEnabled) {
            plugin.getLogger().info("Paper network transport initialized in proxy-aware mode.");
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void send(NetworkEnvelope envelope) {
        if (!available) {
            plugin.getLogger().warning("Tried to send network envelope while transport is unavailable.");
            return;
        }

        // Real implementation to be added in proxy phase.
        plugin.getLogger().info("Stub send packet type=" + envelope.packetType());
    }
}
```

## 13.5 Velocity-side routing service stub

```java
package me.icewolf23.chatbloom.velocity.network;

import java.util.HashMap;
import java.util.Map;
import me.icewolf23.chatbloom.common.network.NetworkEnvelope;
import me.icewolf23.chatbloom.common.network.NetworkPacketType;

public final class ProxyRoutingService {

    private final Map<String, String> playerServerMap = new HashMap<>();

    public void handle(NetworkEnvelope envelope) {
        if (envelope.packetType() == NetworkPacketType.CHAT_MESSAGE) {
            // Later: decode payload, fan out to target backends or network recipients.
        }
    }

    public void updatePlayerServer(String playerUuid, String serverId) {
        playerServerMap.put(playerUuid, serverId);
    }

    public String getServerOf(String playerUuid) {
        return playerServerMap.get(playerUuid);
    }
}
```

---

# 14. Discord and Telegram bridge scope

## 14.1 Discord
- allowed in this plan
- off by default
- should export only permitted channel content
- local-only channels should not export by default
- proxy mode should eventually become the bridge authority in network deployments

## 14.2 Telegram
- allowed in this plan
- off by default
- same channel export restrictions as Discord
- do not build generic webhook infrastructure now

## 14.3 Shared bridge contract

```java
package me.icewolf23.chatbloom.common.bridge;

public interface OutboundBridge {
    String id();
    boolean isEnabled();
    void forward(BridgeMessage message);
}
```

```java
package me.icewolf23.chatbloom.common.bridge;

public record BridgeMessage(
        String sourceType,
        String sourceServer,
        String channelId,
        String senderName,
        String plainText,
        long createdAtMillis
) {}
```

---

# 15. Definition of Done for Run 1

Run 1 is successful when all of these are true:

- multi-module structure exists
- `chatbloom-common`, `chatbloom-paper`, `chatbloom-velocity` compile
- Paper jar runs in standalone mode
- deployment mode config exists
- proxy mode is represented structurally but not required to be fully complete
- registries exist and are wired
- repository abstraction exists
- YAML repositories exist for player state, ignore list, and mute state
- internal event bus exists
- pipeline orchestrator exists
- channel service foundation exists
- privacy service foundation exists
- mute service foundation exists
- settings GUI foundation exists
- no core existing behavior is broken

---

# 16. Test scenarios Codex must think about

## 16.1 Standalone mode
- Paper jar installed alone, proxy mode disabled
- plugin boots cleanly
- existing chat flow still works
- no proxy warnings except none needed

## 16.2 Mode switching
- admin changes `deployment.mode` from `STANDALONE` to `PROXY`
- `/chatbloom reload` does not hard-switch runtime model
- plugin logs restart requirement clearly
- after full restart, proxy stubs initialize correctly

## 16.3 Channels
- player active channel persists
- invalid removed channel falls back to default
- local channel radius filtering works
- staff channel respects send/receive permission

## 16.4 Privacy
- A ignores B
- B cannot PM A
- B mention does not notify A if config says so
- ignore self is rejected
- duplicate ignore is harmless

## 16.5 Moderation
- timed mute persists and expires correctly
- permanent mute survives restart
- mute blocks public chat
- optional PM block works if enabled

## 16.6 GUI
- toggles save correctly
- GUI clicks do not move items
- reopening shows updated state

## 16.7 Proxy foundation
- Paper in proxy mode without transport logs warning, does not crash
- Velocity jar initializes cleanly
- common packet classes serialize without platform dependencies

---

# 17. Anti-patterns Codex must avoid

Do **not** do these:

- one giant `ChatManager` class holding everything
- static singletons for every service
- mixing Bukkit classes into `chatbloom-common`
- making proxy mode hot-reloadable
- storing business logic directly in command classes
- storing rendering logic directly inside listeners
- writing raw SQL in unrelated services
- hiding state in random utility classes
- building bridge logic directly into core moderation/channel classes

---

# 18. Immediate implementation priority

If Codex needs a stricter order inside Run 1, use this exact order:

1. create modules and build structure
2. move shared models/interfaces into `chatbloom-common`
3. create Paper bootstrap and keep standalone behavior working
4. create Velocity bootstrap skeleton
5. create config registry + repository registry + service registry
6. add deployment mode config and restart policy
7. add repository abstraction and YAML implementations
8. add event bus
9. add pipeline contracts and orchestrator
10. add channel service foundation
11. add privacy service foundation
12. add mute service foundation
13. add settings GUI foundation
14. compile, clean up, verify no regressions

---

# 19. Final instruction to Codex

This document is not asking for random experimentation.

It is asking for a disciplined first implementation pass with these exact goals:

- keep ChatBloom valid as a strong standalone Paper plugin
- restructure it into a three-module codebase
- produce two jars
- lay the foundations for proxy-aware mode
- add the first stable abstractions for channels, privacy, moderation, GUI, storage, and bridges
- avoid regressions
- optimize for long-term maintainability over short-term hacks

If a choice is ambiguous, prefer:
- smaller services
- clearer ownership
- safer defaults
- compatibility with existing behavior
- future proxy-readiness without forcing proxy mode on standalone servers
