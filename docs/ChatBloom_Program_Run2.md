# ChatBloom — Codex Run 2
## Complete the implementation on top of the foundations from Run 1

**Purpose:** this file is the instruction set for the **second 5h Codex run**.

This run assumes Run 1 has already finished successfully and the codebase already has:

- `chatbloom-common`
- `chatbloom-paper`
- `chatbloom-velocity`
- deployment mode config
- repositories
- pipeline foundation
- baseline services
- proxy/network contracts

The goal of this run is to **finish the implementation of the planned features**, not to redesign the architecture again.

---

# 0. Primary objective

Use the architecture from Run 1 to implement the missing feature set cleanly and consistently.

The implementation priority for this run is:

1. channels
2. privacy controls
3. moderation tools
4. settings GUI
5. proxy-enabled network behavior
6. Discord bridge
7. Telegram bridge
8. audit/log consistency
9. documentation/config polish inside code/comments

---

# 1. Absolute constraints

## 1.1 Do not rewrite Run 1 architecture unless truly required

This run must build on the foundations already created.

Avoid:
- re-splitting packages
- inventing a second architecture
- replacing repositories with a different pattern
- collapsing services back into listeners or commands

## 1.2 Keep standalone Paper mode working

After Run 2:
- standalone mode must still function as a first-class deployment
- all network-only features must degrade gracefully when proxy mode is off
- code must not assume a proxy is always present

## 1.3 Proxy mode remains optional and restart-based

If users enable proxy mode:
- they must install `chatbloom-velocity.jar` on the proxy
- they must install `chatbloom-paper.jar` on all backends
- they must configure the deployment mode correctly
- they must fully restart affected instances

Do not make `/chatbloom reload` hot-switch runtime mode.

---

# 2. Feature scope for this run

This run should finish these systems as much as realistically possible.

## 2.1 Channel system
Implement:
- `channels.yml`
- active channel persistence
- default channel fallback
- send permission
- receive permission
- channel switching command(s)
- quick-send support if practical
- recipient resolution
- local/radius channel behavior in standalone mode
- server-scoped channel behavior
- proxy-ready channel routing in proxy mode
- staff channel

### Expected examples
- player defaults to `global`
- player switches to `local`
- player in `staff` channel only talks to staff recipients
- if stored active channel is missing after reload, fallback to default
- if channel is disabled, deny switching and fallback safely

## 2.2 Privacy system
Implement:
- ignore list
- `/ignore <player>`
- `/unignore <player>`
- `/ignorelist`
- PM enable/disable toggle
- staff bypass behavior if configured
- PM delivery checks
- mention/ping respect for ignore if desired by spec/config
- persistence through repositories

### Expected examples
- player A ignores player B -> B's PMs do not reach A
- player A disables PMs -> normal PMs fail with configured message
- player A can still receive PM if staff bypass is enabled and sender qualifies

## 2.3 Moderation system
Implement:
- mute
- unmute
- temporary mute
- permanent mute
- global mute chat toggle if part of current plan
- clear chat if part of current plan
- anti-repeat
- anti-caps
- anti-link / anti-advertising if included in master spec
- clean message feedback to the sender
- config-driven bypasses

### Expected examples
- muted player cannot use public chat
- muted player cannot use PM if config says mute applies to PM
- repeated same message triggers anti-repeat
- staff with bypass permission is unaffected where configured

## 2.4 Settings GUI
Implement:
- GUI entry command
- inventory menu
- toggle buttons
- persistent toggles
- visual feedback
- reopen/update logic if needed

### Initial toggles
- ping sound
- ping actionbar
- PM enabled
- maybe active channel quick selector if easy and clean

## 2.5 Proxy-enabled behavior
Implement the actual usable proxy mode foundation started in Run 1.

At minimum:
- Paper can send network packets when proxy mode is enabled
- Velocity receives and routes them
- cross-server PM works
- cross-server channels work at least for core cases
- staff/global channel routing works
- proxy mode logs clearly when active
- network features are unavailable but safe in standalone mode

## 2.6 Discord bridge
Implement if realistically reachable after core systems.

At minimum:
- bridge contract
- config section
- enable/disable toggle
- outbound chat relay
- inbound message handling
- sanitization for inbound external text
- graceful disable when not configured

## 2.7 Telegram bridge
Implement after Discord if realistically reachable.

At minimum:
- config section
- enable/disable toggle
- outbound relay
- inbound relay if practical
- sanitized inbound text
- graceful disable when missing config

---

# 3. Commands to finish in this run

The exact names may adapt to the existing command philosophy, but the feature coverage should exist.

## 3.1 Channels
- `/channel`
- `/channel set <id>`
- `/channel list`
- optional quick-send alias if clean

## 3.2 Privacy
- `/ignore <player>`
- `/unignore <player>`
- `/ignorelist`
- `/pmtoggle` or equivalent if that matches current naming better

## 3.3 Moderation
- `/mute <player> [duration] [reason]`
- `/unmute <player>`
- `/mutechat`
- `/clearchat`

## 3.4 Settings
- `/chatsettings` or `/cbsettings`

If existing command namespace suggests nesting under `/chatbloom`, follow project consistency.

---

# 4. Implementation details Codex should follow

## 4.1 Channel service usage

Recommended pattern:

```java
public final class ChannelSwitchCommand {

    private final ChannelService channelService;

    public ChannelSwitchCommand(ChannelService channelService) {
        this.channelService = channelService;
    }

    public CommandResult execute(UUID playerId, String channelId) {
        Optional<ChatChannel> channel = channelService.findChannel(channelId);
        if (channel.isEmpty()) {
            return CommandResult.failure("messages.channel-not-found");
        }

        channelService.setActiveChannel(playerId, channel.get().id());
        return CommandResult.success("messages.channel-switched");
    }
}
```

## 4.2 Privacy checks during PM flow

Recommended logic:

```java
public final class PrivateMessageUseCase {

    private final PrivacyService privacyService;
    private final ModerationService moderationService;
    private final PrivateMessageDelivery delivery;

    public PrivateMessageUseCase(PrivacyService privacyService,
                                 ModerationService moderationService,
                                 PrivateMessageDelivery delivery) {
        this.privacyService = privacyService;
        this.moderationService = moderationService;
        this.delivery = delivery;
    }

    public CommandResult send(UUID senderId, UUID targetId, String rawMessage) {
        if (moderationService.isMuted(senderId)) {
            return CommandResult.failure("messages.muted");
        }

        if (privacyService.isIgnoring(targetId, senderId)) {
            return CommandResult.failure("messages.target-ignoring-you");
        }

        if (!privacyService.isPmEnabled(targetId)) {
            return CommandResult.failure("messages.pm-disabled");
        }

        delivery.deliver(senderId, targetId, rawMessage);
        return CommandResult.success("messages.pm-sent");
    }
}
```

## 4.3 Anti-repeat policy example

```java
public final class AntiRepeatPolicy {

    private final Map<UUID, Deque<String>> recentMessages = new HashMap<>();

    public boolean shouldBlock(UUID playerId, String normalizedMessage, int historySize) {
        Deque<String> history = recentMessages.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        boolean blocked = history.stream().anyMatch(normalizedMessage::equalsIgnoreCase);

        history.addLast(normalizedMessage);
        while (history.size() > historySize) {
            history.removeFirst();
        }

        return blocked;
    }
}
```

## 4.4 Settings GUI toggle baseline

```java
public final class SettingsMenuBuilder {

    public Inventory build(PlayerSettingsRecord record) {
        Inventory inventory = Bukkit.createInventory(null, 27, "ChatBloom Settings");

        inventory.setItem(11, toggleItem("Ping Sound", record.pingSoundEnabled()));
        inventory.setItem(13, toggleItem("Ping Actionbar", record.pingActionbarEnabled()));
        inventory.setItem(15, toggleItem("Private Messages", record.pmEnabled()));

        return inventory;
    }

    private ItemStack toggleItem(String title, boolean enabled) {
        ItemStack stack = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(title + ": " + (enabled ? "ON" : "OFF")));
        stack.setItemMeta(meta);
        return stack;
    }
}
```

## 4.5 Proxy routing example

```java
public final class ProxyRoutingService {

    private final PlayerLocator playerLocator;
    private final ProxyAudienceService audienceService;

    public void routePrivateMessage(PrivateMessagePacket packet) {
        Optional<ConnectedPlayerRef> target = playerLocator.find(packet.targetId());
        target.ifPresent(ref -> audienceService.sendPrivateMessage(ref, packet));
    }

    public void routeChannelMessage(ChatMessagePacket packet) {
        Set<ConnectedPlayerRef> recipients = playerLocator.findChannelRecipients(packet.channelId(), packet.serverId());
        for (ConnectedPlayerRef recipient : recipients) {
            audienceService.sendChat(recipient, packet);
        }
    }
}
```

---

# 5. Repository and persistence expectations

This run should finish the repository-backed feature persistence for the implemented systems.

Persist at least:
- active channel
- ignore list
- PM enabled toggle
- ping settings
- mute records
- any moderation toggles that are meant to survive restart

If SQL support is already partially scaffolded from Run 1, it is acceptable to:
- keep YAML as default
- finish SQL interfaces only partially
- leave SQL wiring guarded and optional

Do not leave persistence half-randomly spread across listeners or command classes.

---

# 6. Proxy mode expectations

## 6.1 In standalone mode
- no proxy jar required
- no proxy transport required
- cross-server features disabled or unavailable
- local/server features still work

## 6.2 In proxy mode
- Paper forwards chat/PM/channel events through the bridge layer
- Velocity becomes the network routing authority
- backends remain responsible for local player interaction, GUI, sounds, actionbar, ChatItems rendering
- logs should clearly indicate network mode is active

## 6.3 ChatItems in proxy mode
If realistic in Run 2, finish the proxy-aware snapshot routing.

If not realistic cleanly, it is acceptable to:
- keep ChatItems local-only in this run
- leave clear TODOs and safe guardrails
- avoid fake half-working network preview behavior

Clean incomplete behavior is better than broken “almost works” behavior.

---

# 7. Definition of Done for Run 2

Run 2 is complete only if all of the following are true:

- standalone Paper mode still works
- channels are actually usable
- privacy controls are actually usable
- moderation controls are actually usable
- settings GUI works and persists data
- proxy-enabled mode performs real routing for at least chat/PM core flows
- Discord bridge is either implemented cleanly or left safely disabled with clear code boundaries
- Telegram bridge is either implemented cleanly or left safely disabled with clear code boundaries
- commands are wired and permissions are respected
- config defaults are sane
- there are no obvious giant regressions from Run 1
- code compiles cleanly in all modules

---

# 8. Test scenarios for this run

Codex must think through at least these cases:

## 8.1 Channels
- player joins with no active channel stored
- player joins with invalid stored channel
- player switches to disabled channel
- player sends in local channel with no nearby recipients
- staff channel only reaches staff recipients

## 8.2 Privacy
- sender is ignored by target
- target has PMs disabled
- player ignores someone, relogs, ignore still persists
- `/reply` after ignore state changes

## 8.3 Moderation
- temp mute expires correctly
- permanent mute persists
- anti-repeat false positives stay reasonable
- anti-caps only triggers when enabled
- bypass permissions work

## 8.4 Settings GUI
- toggle click updates storage
- reopen menu shows persisted state
- GUI interaction is cancelled properly
- null item / wrong slot is ignored safely

## 8.5 Proxy mode
- paper in proxy mode but velocity absent
- velocity present but packet unsupported
- cross-server PM routing
- cross-server channel routing
- proxy mode disabled while network config still exists

## 8.6 Bridges
- Discord/Telegram disabled in config
- token/chat id missing
- inbound text contains suspicious formatting
- bridge failure does not crash the plugin

---

# 9. Anti-patterns to avoid

Do not:
- re-architect everything again
- hide feature state in command classes
- make bridges mandatory
- assume SQL is available
- force all features to be network-aware immediately if that harms stability
- merge standalone/proxy behavior into messy `if` jungles everywhere
- leave user messages hardcoded in random classes

---

# 10. Final instruction to Codex

Complete the implementation cleanly on top of Run 1.

Focus first on:
1. channels
2. privacy
3. moderation
4. settings GUI
5. proxy routing core

Only after those are solid should you spend time on:
- Discord
- Telegram
- extra polish

If a feature cannot be finished safely in this run, leave it:
- clearly bounded
- safely disabled
- documented in code comments/TODOs
- not half-broken
