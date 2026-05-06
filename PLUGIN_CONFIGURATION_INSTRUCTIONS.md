# CoreChatX Configuration Instructions

This file is a complete reference for the current public config shape of CoreChatX.
It is intentionally split into `Paper backend` and `Velocity proxy` so the install remains readable.

The goal of this document is simple:
- describe the files that actually exist today
- mirror the current defaults bundled in the jars
- explain what each line does
- call out what is reloadable and what still needs a restart

Related documents:
- `README.md` gives the commercial feature overview
- `FAQ.md` answers the common admin and setup questions

If a config file is deleted, CoreChatX recreates the bundled default on next boot.
That is the supported way to regenerate a clean config set.

---

## 0. Before You Configure

Use this document as the operational reference for a real installation.
It is intentionally more detailed than the feature list because it documents the files, runtime gates, restart requirements, and safety limits that matter when the plugin is already installed on a server.

Minimum expectations:
- Java 21 runtime
- Paper `1.21.11` for backend servers
- Velocity for multi-backend network installs
- the Paper CoreChatX jar installed only on Paper backends
- the Velocity CoreChatX jar installed only on Velocity

Optional integrations:
- LuckPerms for rank/group metadata and permission assignment
- PlaceholderAPI for dynamic placeholders in server-controlled templates
- Discord bot token and required gateway intents for the Discord bridge
- Telegram bot token for the Telegram Bot API long-polling bridge

Core terminology:
- `Paper backend` means a real Minecraft server running the Paper jar
- `Velocity proxy` means the proxy process running the Velocity jar
- `STANDALONE` means one Paper server without cross-backend CoreChatX routing
- `PROXY` means a Paper backend connected to a Velocity network
- `server-id` is the unique CoreChatX identity of one Paper backend
- `network-channel` is the plugin messaging channel shared by Paper and Velocity
- `bridge` means Discord or Telegram inbound/outbound relay
- `runtime data file` means generated player/plugin state, not a decorative default config

High-level runtime model:
- Paper owns chat parsing, formatting, commands, moderation, player settings, chat items, bubbles, pings, and bridge rendering
- Velocity relays network packets, player directory data, global completions, TAB entries, and remote ChatItem snapshot requests
- external bridges are configured on Paper and can export only the channels that opt in
- player-authored chat is sanitized and controlled before it becomes a rendered Adventure component
- server-controlled templates can use MiniMessage and, where supported, PlaceholderAPI

Keep this distinction in mind:
- admin config strings are trusted server templates
- raw player messages are untrusted input
- runtime data files are live state

### CoreChatX project identity

Current public identity:
- product/plugin name: `CoreChatX`
- Paper plugin name: `CoreChatX`
- Velocity plugin id: `corechatx`
- Java package root: `me.icewolf23.corechatx`
- Maven parent artifact: `corechatx-parent`
- Maven runtime artifacts: `corechatx-paper` and `corechatx-velocity`
- shared common module: `corechatx-common`
- default Paper command: `/corechatx`
- default Paper command alias: `/ccx`
- default settings command alias: `/ccxsettings`
- default permission namespace: `corechatx.*`
- default plugin messaging channel: `corechatx:main`

Clean install rule:
- install `corechatx-paper-<version>.jar` on Paper
- install `corechatx-velocity-<version>.jar` on Velocity
- keep Paper data in `plugins/CoreChatX/`
- keep Velocity data in `plugins/corechatx/`

Old install rule:
- do not mix old jars, old plugin ids, old permission namespaces, or old network channels with CoreChatX
- treat older differently named config folders as old install data
- for a clean CoreChatX setup, let the plugin generate fresh defaults and manually reapply only the settings you still want

---

## 1. Layout Overview

### Paper backend folder

```text
plugins/CoreChatX/
```

### Velocity proxy folder

```text
plugins/corechatx/
```

### Files created from bundled defaults on Paper

```text
config.yml
messages.yml
chat.yml
pings.yml
filter.yml
chatitems.yml
keywords.yml
chatbubbles.yml
playerdata.yml
state.yml
channels.yml
privacy.yml
moderation.yml
storage.yml
discord.yml
telegram.yml
locales/en_us.yml
```

### Additional Paper runtime files created on demand

```text
channeldata.yml
ignoredata.yml
mutedata.yml
```

### Files created from bundled defaults on Velocity

```text
velocity-config.properties
```

---

## 2. Reload vs Restart

Safe with `/corechatx reload`:
- `config.yml` for non-identity runtime toggles such as `debug`, hooks, logging, bridge/runtime gates, join/quit, first-join, and reload summary behavior
- `messages.yml`
- `chat.yml`
- `pings.yml`
- `filter.yml`
- `chatitems.yml`
- `keywords.yml`
- `chatbubbles.yml`
- `channels.yml`
- `privacy.yml`
- `moderation.yml`
- `storage.yml`
- `discord.yml`
- `telegram.yml`
- locale files under `locales/`

Requires full restart to truly take effect:
- `config.yml -> deployment.mode`
- `config.yml -> deployment.server-id`
- `config.yml -> deployment.network-channel`
- switching a backend between standalone and proxy deployment
- changing Velocity `velocity-config.properties`; there is no proxy-side config reload command
- when Velocity `velocity-config.properties -> network-channel` changes, affected Paper backends must also use the same new `deployment.network-channel`

Important deployment rule:
- Paper backends and the Velocity proxy must use the exact same network channel

Artifact model:
- CoreChatX currently ships as two separate runtime jars.
- CoreChatX Paper jar: install `corechatx-paper-<version>.jar` on each Paper backend.
- CoreChatX Velocity jar: install `corechatx-velocity-<version>.jar` on the Velocity proxy.
- Do not install the Paper jar on Velocity, and do not install the Velocity jar on Paper.
- source builds also produce `corechatx-common-<version>.jar`, but that is an internal shared module and is not the runtime jar to install on servers

---

## 3. Quick Install Patterns

### Standalone Paper

Use the default backend config:

```yml
deployment:
  mode: "STANDALONE"
  server-id: "paper-1"
  network-channel: "corechatx:main"
  bridges-allowed: true
  network-features-allowed: false
```

In this setup:
- no Velocity module is required
- `NETWORK` channels behave as normal local backend chat
- cross-server PM routing is inactive
- network player directory, global TAB entries, and cross-backend chat completions are inactive

### Velocity network

On each Paper backend:
- set `deployment.mode: "PROXY"`
- set a unique `deployment.server-id`
- keep the same `deployment.network-channel` on every backend
- set `deployment.network-features-allowed: true`

On Velocity:
- install the Velocity CoreChatX jar
- set the same `network-channel` in `velocity-config.properties`

Example backend identities:
- `alpha`
- `beta`
- `survival-1`
- `hub`

Network behavior enabled by this setup:
- `NETWORK` channels can cross backend boundaries
- cross-server PM delivery can use the proxy route
- player communication settings are synchronized through Velocity
- active channel state, ping toggles, PM toggle, social spy, mention notifications, locale, staff-chat state, and ignore lists can follow backend switches
- the proxy can provide global player name completions and TAB entries for players connected to configured backends
- network chat preserves rendered Adventure components, ChatItem preview refs, mentions, and activated custom ping metadata across backends
- Velocity treats the real backend connection as the source of truth for proxy packets; backend-declared source ids cannot override it
- malformed, oversized, or oversized-generated plugin-message payloads are rejected before routing

### Production setup checklist

For a clean standalone Paper install:
- install only the Paper jar
- keep `deployment.mode: "STANDALONE"`
- keep `deployment.network-features-allowed: false`
- configure formats, channels, messages, moderation, chat items, keywords, bubbles, and optional bridges on that backend
- restart after changing deployment identity fields
- use `/corechatx reload` for normal wording, formatting, channel, filter, and feature tuning

For a clean Velocity network install:
- install the Paper jar on every backend
- install the Velocity jar on the proxy
- set every backend to `deployment.mode: "PROXY"`
- give every backend a unique `deployment.server-id`
- use the exact same lowercase `deployment.network-channel` on every backend
- use the exact same `network-channel` in `plugins/corechatx/velocity-config.properties`
- set `deployment.network-features-allowed: true` on each backend that should participate in cross-server features
- restart the affected Paper backends and Velocity after changing identity or network channel values

For bridge installs:
- keep `deployment.bridges-allowed: true`
- enable only the bridge services you actually use
- enable outbound export only on the CoreChatX channels that should leave Minecraft
- configure inbound routes so external messages land in an intentional CoreChatX channel
- in proxy mode, run inbound bridge listeners only on the backend instances that should locally display inbound messages

For PlaceholderAPI-heavy installs:
- install PlaceholderAPI on every backend that must render placeholders
- install the expansions required by your formats
- keep raw player messages separate from admin templates
- remember that mention token placeholders use the mentioned player as context
- test offline-player placeholder behavior before relying on it for cross-backend hover text

For ChatItems on a network:
- keep ChatItems enabled only on the channels where previews are wanted
- tune byte limits only if you understand the size of your custom item metadata
- prefer the bundled on-demand snapshot flow over embedding huge item data into every network chat packet
- accept that expired or rejected snapshots should fail closed with a normal expired-preview message

---

## 4. Paper Backend Files

## 4.1 `config.yml`

Purpose:
- deployment mode
- backend identity
- proxy transport gates
- bridge master switch
- hook toggles
- logging toggles
- general reload/save behavior

Current bundled default:

```yml
# Core plugin settings.

deployment:
  # STANDALONE = Paper-only local mode
  # PROXY = optional proxy-aware mode, requires full restart after changes
  mode: "STANDALONE"

  # If the deployment mode changes across a reload, the plugin warns that a restart is required.
  # Keep this true unless you are changing the code and deliberately redefining deployment semantics.
  require-full-restart-on-mode-change: true

  # Logical backend id used in proxy mode.
  # This value is included in proxy packets and bridge source labels.
  # Every Paper backend in a network must use a unique value.
  server-id: "paper-1"

  # Shared plugin messaging channel between Paper and Velocity.
  # Every backend and the proxy plugin must use the exact same value.
  network-channel: "corechatx:main"

  # Global master switch for all Discord and Telegram bridge runtime.
  # If false, bridge configs may still exist, but outbound and inbound bridge services do not run.
  bridges-allowed: true

  # Proxy-network feature gate.
  # If false, a backend can still boot in PROXY mode, but proxy transport is intentionally disabled.
  # Cross-server chat, PM routing, player directory sync, and state sync will not run.
  network-features-allowed: false

  # Timeout for remote PM requests that wait for proxy delivery/acknowledgement.
  pending-pm-timeout-seconds: 20

# General debug flag.
# Keep false in normal production use.
debug: false

hooks:
  # If true and PlaceholderAPI is installed, supported config formats are passed through PlaceholderAPI.
  # This applies to almost every player-facing configurable string that CoreChatX renders,
  # including chat formats, message templates, bridge formats, and many GUI/feedback strings.
  placeholderapi: true

  # If true and LuckPerms is installed, CoreChatX reads primary-group and prefix data for rank-aware chat formatting.
  luckperms: true

logging:
  # Console logging for public chat events that CoreChatX chooses to log.
  public-chat: true

  # Console logging for PM-related events that CoreChatX chooses to log.
  private-messages: true

  # Console logging for /broadcast.
  broadcasts: true

  # Console logging for reload actions.
  reload: true

  # Console logging for plugin warnings/errors.
  errors: true

player-data:
  # Player toggles are saved immediately anyway.
  # This flag controls the fallback save on plugin disable.
  auto-save-on-disable: true

first-join:
  # Enables the first-join system globally.
  enabled: true

  # Enables the numbered first-join counter stored in state.yml.
  counter-enabled: true

join-quit:
  # Enables join messages.
  join-enabled: true

  # Enables quit messages.
  quit-enabled: true

reload:
  # If true, /corechatx reload shows a short summary to the command sender.
  show-summary: true
```

Operational notes:
- `deployment.mode`, `deployment.server-id`, and `deployment.network-channel` require restart
- the startup log prints a `CORECHATX` banner with version, author, Paper architecture, project-link placeholder, then the deployment summary and transport state
- invalid proxy channel config does not crash the whole plugin; it degrades transport and logs the reason
- network channels must use lowercase Minecraft namespaced-key style, such as `corechatx:main` or `corechatx:network/main`
- invalid channel examples include `CoreChatX:main`, `core chat x:main`, `corechatx`, and `corechatx:Main`
- `deployment.bridges-allowed: false` disables both outbound bridge dispatch and inbound Discord/Telegram runtime
- `deployment.network-features-allowed: false` leaves `PROXY` deployment bootable but intentionally disables proxy transport and all cross-server features
- `deployment.pending-pm-timeout-seconds` is clamped to at least 5 seconds
- with `hooks.placeholderapi: true`, almost every player-facing configurable string supports PlaceholderAPI placeholders when PlaceholderAPI is installed

---

## 4.2 `chat.yml`

Purpose:
- public chat formatting
- group-specific format overrides
- mention token formatting
- public and PM cooldowns

Current bundled default:

```yml
# Public chat formatting and moderation settings.

public-chat:
  # Master switch for the normal public chat pipeline.
  enabled: true

  # Base public chat format.
  # Supported placeholders:
  # {plugin_prefix}, {channel_prefix}, {rank_prefix}, {player_name}, {message}
  format: "{plugin_prefix} {channel_prefix}{rank_prefix}<white>{player_name}</white><dark_gray><italic>» </italic></dark_gray>{message}"

  # Optional group-specific format overrides keyed by LuckPerms primary group.
  # These are used only when the active channel has no custom format in channels.yml.
  # Keys should match the LuckPerms primary group in lowercase.
  group-formats:
    owner: "{plugin_prefix} {channel_prefix}{rank_prefix}<white>{player_name}</white><dark_gray><italic>» </italic></dark_gray>{message}"
    admin: "{plugin_prefix} {channel_prefix}{rank_prefix}<white>{player_name}</white><dark_gray><italic>» </italic></dark_gray>{message}"
    mod: "{plugin_prefix} {channel_prefix}{rank_prefix}<white>{player_name}</white><dark_gray><italic>» </italic></dark_gray>{message}"
    vip: "{plugin_prefix} {channel_prefix}{rank_prefix}<white>{player_name}</white><dark_gray><italic>» </italic></dark_gray>{message}"
    default: "{plugin_prefix} {channel_prefix}{rank_prefix}<gray>{player_name}</gray><dark_gray><italic>» </italic></dark_gray>{message}"

  # Decorative prefix component shown before public chat lines.
  plugin-prefix: "<dark_gray>[</dark_gray><gradient:#79d6b8:#5aa9ff>Chat</gradient><dark_gray>]</dark_gray>"

  # Prefix template used for non-global channels.
  # {channel_name} is replaced with the current channel id.
  channel-prefix-format: "<dark_gray>[</dark_gray><white>{channel_name}</white><dark_gray>]</dark_gray> "

  # Used only when LuckPerms prefix support is unavailable or disabled.
  fallback-rank-prefix: ""

mentions:
  # Enables player-name mention detection and mention rendering.
  enabled: true

  # Visible token style for a matched mention target.
  token-format: "<#79d6b8>@{player_name}</#79d6b8>"

cooldowns:
  public:
    # Enables public chat cooldowns.
    enabled: true

    # Required wait time in seconds between public messages unless the sender bypasses the cooldown.
    seconds: 2

  private-messages:
    # Enables PM cooldowns.
    enabled: false

    # Required wait time between PMs when the PM cooldown is enabled.
    seconds: 2
```

Format priority:
1. `channels.<id>.format`
2. `public-chat.group-formats.<luckperms-primary-group>`
3. `public-chat.format`

Important notes:
- `{rank_prefix}` works in every `group-formats.<group>` entry
- if LuckPerms is not available, group-specific formats are skipped and CoreChatX falls back to the base format
- if you already manage rank prefixes in LuckPerms, prefer `{rank_prefix}` over hardcoded titles in this file
- PlaceholderAPI placeholders can be used in most admin-controlled rendered strings, including chat formats and group formats, when PlaceholderAPI support is enabled
- raw player-authored message bodies do not pass through PlaceholderAPI; this prevents players from resolving arbitrary `%placeholder%` tokens inside normal chat text
- `mentions.token-format` resolves `{player_name}` to the mentioned player's name
- PlaceholderAPI in `mentions.token-format` uses the mentioned player as context, not the sender
- for cross-backend mentions, CoreChatX uses the mentioned player's `OfflinePlayer` context so PlaceholderAPI expansions with offline support can still resolve
- negative cooldown seconds are treated as `0`

---

## 4.3 `messages.yml`

Purpose:
- general user-facing text
- command feedback
- PM layouts
- moderation messages
- join/quit messages
- chat item feedback

Current bundled default:

```yml
# Main message file.
# Supported placeholders vary by message, but common ones include:
# {prefix}, {player_name}, {target_name}, {sender_name}, {message}, {seconds}, {count}, {setting}, {state}

prefix: "<dark_gray>[</dark_gray><gradient:#79d6b8:#5aa9ff>CoreChatX</gradient><dark_gray>]</dark_gray>"

errors:
  no-permission: "{prefix} <red>You do not have permission to do that.</red>"
  players-only: "{prefix} <red>Only players can use this command.</red>"
  player-not-found: "{prefix} <red>That player is not online.</red>"
  cannot-message-self: "{prefix} <red>You cannot message yourself.</red>"
  empty-message: "{prefix} <red>Your message is empty after sanitization.</red>"
  no-reply-target: "{prefix} <red>You do not have anyone to reply to.</red>"
  invalid-chatitem-id: "{prefix} <red>This chat item preview is no longer available.</red>"
  invalid-usage: "{prefix} <red>Usage: {usage}</red>"

reload:
  success: "{prefix} <green>Reload complete.</green>"
  summary: "{prefix} <gray>Modules reloaded: chat, pings, PMs, filter, chat items, player data hooks.</gray>"

commands:
  corechatx-help:
    # Header shown by /corechatx when the sender has permission to access the root help.
    header: "{prefix} <gray>Available CoreChatX subcommands:</gray>"

    # Individual help lines. The command executor only shows the lines the sender can actually use.
    reload: "<gray>/corechatx reload</gray>"
    settings: "<gray>/corechatx settings</gray>"
    locale: "<gray>/corechatx locale [tag]</gray>"

locale:
  current: "{prefix} <gray>Your active locale is <white>{locale}</white>.</gray>"
  changed: "{prefix} <gray>Your locale is now <white>{locale}</white>.</gray>"
  invalid: "{prefix} <red>Locale <white>{locale}</white> is not available on this server.</red>"

channels:
  list: "{prefix} <gray>Available channels: <white>{channels}</white></gray>"
  switched: "{prefix} <gray>Your active channel is now <white>{channel_name}</white>.</gray>"
  not-found: "{prefix} <red>Channel <white>{channel_name}</white> does not exist.</red>"
  disabled: "{prefix} <red>Channel <white>{channel_name}</white> is currently disabled.</red>"
  no-send-permission: "{prefix} <red>You cannot send messages to that channel.</red>"

privacy:
  ignore-disabled: "{prefix} <red>Ignore commands are currently disabled.</red>"
  ignore-self: "{prefix} <red>You cannot ignore yourself.</red>"
  ignore-limit: "{prefix} <red>You cannot ignore more than <white>{limit}</white> players.</red>"
  already-ignoring: "{prefix} <yellow>You are already ignoring <white>{target_name}</white>.</yellow>"
  not-ignoring: "{prefix} <yellow>You are not ignoring <white>{target_name}</white>.</yellow>"
  ignore-added: "{prefix} <gray>You are now ignoring <white>{target_name}</white>.</gray>"
  ignore-removed: "{prefix} <gray>You are no longer ignoring <white>{target_name}</white>.</gray>"
  ignore-list: "{prefix} <gray>Ignored players: <white>{targets}</white></gray>"
  pm-toggled: "{prefix} <gray>Private messages are now <white>{state}</white>.</gray>"

moderation:
  mute-disabled: "{prefix} <red>Mute commands are currently disabled.</red>"
  mutechat-disabled: "{prefix} <red>Global chat mute is currently disabled.</red>"
  muted-public: "{prefix} <red>You are muted and cannot use public chat right now.</red>"
  muted-private: "{prefix} <red>You are muted and cannot send private messages right now.</red>"
  chat-muted: "{prefix} <red>Public chat is currently muted.</red>"
  chat-muted-toggled: "{prefix} <gray>Global chat mute is now <white>{state}</white>.</gray>"
  anti-repeat: "{prefix} <red>Please do not repeat the same message.</red>"
  anti-caps: "{prefix} <red>Please avoid excessive caps.</red>"
  mute-success: "{prefix} <gray>Muted <white>{target_name}</white>.<gray> Reason: <white>{reason}</white></gray>"
  unmute-success: "{prefix} <gray>Unmuted <white>{target_name}</white>.</gray>"
  clear-chat-notice: "{prefix} <gray>Chat was cleared by <white>{sender_name}</white>.</gray>"
  clear-chat-sender: "{prefix} <gray>Cleared chat for <white>{count}</white> online player(s).</gray>"

cooldown:
  public: "{prefix} <yellow>You must wait <white>{seconds}</white> more second(s) before chatting again.</yellow>"
  pm: "{prefix} <yellow>You must wait <white>{seconds}</white> more second(s) before sending another private message.</yellow>"

ping:
  toggles:
    status: "{prefix} <gray>Ping <white>{setting}</white> notifications are currently <white>{state}</white>.</gray>"
    sound-changed: "{prefix} <gray>Ping sound notifications are now <white>{state}</white>.</gray>"
    actionbar-changed: "{prefix} <gray>Ping actionbar notifications are now <white>{state}</white>.</gray>"
  state-on: "<green>enabled</green>"
  state-off: "<red>disabled</red>"
  notification-actionbar: "<gold>Ping:</gold> <yellow>{sender_name}</yellow> mentioned you."
  notification-sound: "ENTITY_EXPERIENCE_ORB_PICKUP"
  notification-volume: 0.85
  notification-pitch: 1.25

private-messages:
  to-sender: "<dark_gray>[</dark_gray><light_purple>PM</light_purple><dark_gray>]</dark_gray> <gray>you -> </gray><white>{target_name}</white><dark_gray>: </dark_gray>{message}"
  to-target: "<dark_gray>[</dark_gray><light_purple>PM</light_purple><dark_gray>]</dark_gray> <white>{sender_name}</white><gray> -> you</gray><dark_gray>: </dark_gray>{message}"
  spy: "<dark_gray>[</dark_gray><red>SPY</red><dark_gray>]</dark_gray> <white>{sender_name}</white><gray> -> </gray><white>{target_name}</white><dark_gray>: </dark_gray>{message}"
  disabled-target: "{prefix} <red>That player has private messages disabled.</red>"
  blocked-by-target: "{prefix} <red>That player is not accepting messages from you.</red>"
  remote-unavailable: "{prefix} <red>The proxy transport could not forward that private message right now.</red>"
  received-sound: "BLOCK_NOTE_BLOCK_BELL"
  received-volume: 0.85
  received-pitch: 1.1
  socialspy-on: "{prefix} <gray>Social spy is now <green>enabled</green>.</gray>"
  socialspy-off: "{prefix} <gray>Social spy is now <red>disabled</red>.</gray>"

broadcast:
  format: "<dark_gray>[</dark_gray><gold>Broadcast</gold><dark_gray>]</dark_gray> <white>{sender_name}</white><dark_gray>: </dark_gray><gold>{message}</gold>"

chat:
  no-message-sent: "{prefix} <yellow>Nothing was sent because the message is empty after sanitization.</yellow>"

join-quit:
  join: "<dark_gray>[</dark_gray><green>+</green><dark_gray>]</dark_gray> <white>{player_name}</white>"
  quit: "<dark_gray>[</dark_gray><red>-</red><dark_gray>]</dark_gray> <white>{player_name}</white>"
  first-join: "<dark_gray>[</dark_gray><gradient:#79d6b8:#5aa9ff>Welcome</gradient><dark_gray>]</dark_gray> <white>{player_name}</white><gray> is joining for the first time as player </gray><white>#{count}</white><gray>.</gray>"

chatitems:
  click-to-open: "<gray>Click to open the saved preview.</gray>"
  expired: "{prefix} <red>This preview has expired or was cleared during reload/restart.</red>"
```

Locale note:
- `messages.yml` remains the final fallback source for message keys
- locale files can override individual keys without forcing you to duplicate the whole file

---

## 4.4 `channels.yml`

Purpose:
- channel definitions
- delivery scope
- per-channel permission gates
- per-channel mention and chat-item behavior
- per-channel bridge export control
- optional per-channel chat format

Current bundled default:

```yml
# Channel defaults.
# In STANDALONE mode, NETWORK still behaves as a normal local channel unless the proxy bridge is enabled.
# In PROXY mode, only channels with scope NETWORK are forwarded cross-server.
# SERVER and LOCAL_RADIUS always stay backend-local.

channels:
  global:
    # Enables the channel.
    enabled: true

    # Makes this the default active channel for players.
    default: true

    # NETWORK means the channel can cross servers when proxy routing is active.
    scope: "NETWORK"

    # Permission required to send into this channel.
    # Blank means no special send permission is required.
    permission-send: ""

    # Permission required to receive this channel.
    # Blank means no extra receive permission is required.
    permission-receive: ""

    # Enables mention parsing and custom ping rendering in this channel.
    allow-mentions: true

    # Enables [item] / [inv] / [ec] style token processing in this channel.
    allow-chatitems: true

    # Enables overhead chat bubbles for successful player messages in this channel.
    allow-chat-bubbles: true

    # Allows outbound export to Discord/Telegram for locally-originating messages from this channel.
    export-to-bridges: true

    # Optional channel-specific format override.
    # Leave blank to use chat.yml resolution.
    format: ""

  local:
    enabled: true
    default: false

    # LOCAL_RADIUS restricts delivery to players near the sender on the same backend.
    scope: "LOCAL_RADIUS"

    # Delivery radius used when scope is LOCAL_RADIUS.
    radius: 100

    permission-send: "corechatx.channel.local"
    permission-receive: ""
    allow-mentions: true
    allow-chatitems: true
    allow-chat-bubbles: true
    export-to-bridges: false
    format: ""

  staff:
    enabled: true
    default: false
    scope: "NETWORK"
    permission-send: "corechatx.channel.staff"
    permission-receive: "corechatx.staff"
    allow-mentions: true
    allow-chatitems: false
    allow-chat-bubbles: false
    export-to-bridges: true
    format: ""
```

Important behavior:
- invalid dynamic permission nodes are detected and warned during load
- only the broken binding is disabled; the plugin does not crash for one malformed permission
- `export-to-bridges: true` means locally-originating messages in that channel are eligible for Discord/Telegram outbound export
- in proxy mode, outbound bridge export is performed only from the source backend so a network message is not exported once per backend

---

## 4.5 `pings.yml`

Purpose:
- mention notification toggles
- custom ping definitions
- permission rules for custom pings

Current bundled default:

```yml
# Mention and custom ping behaviour.

mentions:
  # Global switch for playing ping sounds when a valid ping is delivered.
  notify-sound: true

  # Global switch for actionbar ping notifications.
  notify-actionbar: true

custom-pings:
  all:
    # Visible token typed by players in chat.
    trigger: "@all"

    # Permission required to use this token.
    use-permission: "corechatx.ping.use.all"

    # Permission a recipient must have to be targeted.
    # Blank means everyone online is a valid target.
    receive-permission: ""

    # If true, bypasses the recipient's own ping sound/actionbar toggle.
    bypass-toggle: false

    # Visible rendering of the token in chat.
    token-format: "<#79d6b8>{trigger}</#79d6b8>"

  help:
    trigger: "@help"
    use-permission: "corechatx.ping.use.help"
    receive-permission: "corechatx.ping.receive.help"
    bypass-toggle: false
    token-format: "<#5aa9ff>{trigger}</#5aa9ff>"

  staff:
    trigger: "@staff"
    use-permission: "corechatx.ping.use.staff"
    receive-permission: "corechatx.staff"
    bypass-toggle: true
    token-format: "<#ff8f8f>{trigger}</#ff8f8f>"
```

Validation note:
- `use-permission` and `receive-permission` are validated
- malformed dynamic permission nodes are warned and individually disabled

Network note:
- when a custom ping is activated in a `NETWORK` channel, CoreChatX forwards the activated ping metadata with the message so remote backends can deliver the matching actionbar/sound notification locally

---

## 4.6 `privacy.yml`

Purpose:
- PM defaults
- staff PM bypass behavior
- ignore system configuration

Current bundled default:

```yml
# Privacy defaults for Run 2.

private-messages:
  # Default PM state for players with no stored data yet.
  enabled-by-default: true

  # If true, players with the configured permission may bypass supported PM privacy restrictions.
  allow-staff-bypass: true

  # Permission node used for the staff bypass logic.
  staff-bypass-permission: "corechatx.staff"

ignore:
  # Enables /ignore, /unignore, and /ignorelist behavior.
  enabled: true

  # Maximum ignore entries per player.
  max-ignored-players: 200

  # If true, ignored players also stop mention/custom-ping notifications.
  block-mentions-from-ignored: true
```

Validation note:
- `staff-bypass-permission` is also validated
- if malformed, only that bypass binding is disabled and the plugin logs a warning
- `ignore.max-ignored-players` is clamped to `0` or higher

---

## 4.7 `moderation.yml`

Purpose:
- mute behavior
- mutechat master switch
- anti-repeat
- anti-caps

Current bundled default:

```yml
# Moderation defaults for Run 2.

mute:
  # Enables /mute and /unmute behavior.
  enabled: true

  # If true, active mutes also block /msg and /reply.
  block-private-messages: true

  # Fallback reason used when a mute command is issued without a custom reason.
  default-reason: "No reason provided"

mutechat:
  # Enables the /mutechat feature globally.
  enabled: true

anti-repeat:
  # Enables anti-repeat checks for public chat.
  enabled: true

  # Number of recent messages considered for exact duplicate comparison.
  history-window: 3

  # If true, exact duplicates in the comparison window are blocked.
  block-identical: true

anti-caps:
  # Enables anti-caps checks.
  enabled: false

  # Minimum plain-text message length before anti-caps is evaluated.
  min-length: 8

  # Uppercase ratio threshold after which the message is blocked.
  max-uppercase-ratio: 0.7
```

Bounds note:
- `anti-repeat.history-window` is clamped to at least `1`
- `anti-caps.min-length` is clamped to at least `1`

---

## 4.8 `filter.yml`

Purpose:
- word filter behavior
- replacement style
- optional hover over censored words

Current bundled default:

```yml
# Word filter configuration.

# Enables the word filter.
enabled: true

# The filter censors words and still sends the message.
blocked-words:
  - idiota
  - stupido

# Character repeated across the blocked-word length.
replacement-character: "*"

hover-original:
  # If true, the censored token can expose the original word in hover text.
  enabled: false

  # Hover template shown when hover-original is enabled.
  text: "<gray>Original term:</gray> <red>{word}</red>"
```

Important behavior:
- this is a censoring filter, not a whole-message dropper by default

---

## 4.9 `chatitems.yml`

Purpose:
- token aliases
- permission nodes per token family
- visible token formatting
- preview expiration
- preview inventory titles

Current bundled default:

```yml
# Chat item token configuration.

tokens:
  item:
    # Aliases that trigger held-item preview handling.
    aliases: [ "[item]", "[i]" ]

    # Permission required to turn the token into a preview.
    permission: "corechatx.chatitem.item"

    # Rendered token when a normal held item is previewed.
    token-format: "<#79d6b8>[item]</#79d6b8>"

    # Rendered token when the held item is a shulker box and the sender also has shulker permission.
    shulker-token-format: "<#5fc7c2>[shulker]</#5fc7c2>"

  armor:
    aliases: [ "[armor]" ]
    permission: "corechatx.chatitem.armor"
    token-format: "<#6fb6ff>[armor]</#6fb6ff>"

  hotbar:
    aliases: [ "[hotbar]" ]
    permission: "corechatx.chatitem.hotbar"
    token-format: "<#8bc8ff>[hotbar]</#8bc8ff>"

  inventory:
    aliases: [ "[inventory]", "[inv]" ]
    permission: "corechatx.chatitem.inventory"
    token-format: "<#5aa9ff>[inventory]</#5aa9ff>"

  enderchest:
    aliases: [ "[enderchest]", "[ender]", "[ec]" ]
    permission: "corechatx.chatitem.enderchest"
    token-format: "<#7db8ff>[enderchest]</#7db8ff>"

previews:
  # Snapshot lifetime in minutes.
  expire-after-minutes: 30

  titles:
    # {player_name} is the owner of the snapshot, not the viewer.
    item: "<#79d6b8>{player_name}'s item</#79d6b8>"
    shulker: "<#5fc7c2>{player_name}'s shulker</#5fc7c2>"
    armor: "<#6fb6ff>{player_name}'s armor</#6fb6ff>"
    hotbar: "<#8bc8ff>{player_name}'s hotbar</#8bc8ff>"
    inventory: "<#5aa9ff>{player_name}'s inventory</#5aa9ff>"
    enderchest: "<#7db8ff>{player_name}'s ender chest</#7db8ff>"

network:
  # Network mode sends only lightweight snapshot bundle refs in chat packets.
  # Multiple ChatItem tokens in the same message share one snapshot bundle with per-token preview views.
  # The full bundle is fetched from the source backend when a remote player clicks a view.

  # How long a backend waits for Velocity to return a remote snapshot bundle after a click.
  request-timeout-seconds: 5

  # Maximum unique snapshot bundle refs attached to one network chat message.
  # Normal player chat currently uses one shared bundle per message; this is a defensive cap.
  # The plugin-message protocol currently hard-caps the effective value to 3.
  max-snapshots-per-message: 3

  # Hard payload limits for remote snapshot bundle serialization/import.
  max-compressed-bytes: 30000
  max-uncompressed-bytes: 2097152
  max-item-bytes: 262144
```

Behavior notes:
- if the sender lacks the required permission, the token stays plain text
- snapshots are runtime objects and are cleared on reload/restart
- multiple ChatItem tokens in the same player message share one snapshot UUID/bundle; each token click carries the requested view, such as `item`, `armor`, or `inventory`
- snapshot UUIDs are treated as short-lived capability ids: anyone who received the rendered chat component can open the available preview views until it expires, but ids are random, not listed, and not persisted
- in proxy mode, chat packets carry only lightweight ChatItem bundle refs; on click, Velocity requests the full bundle from the source backend and forwards the response to the requesting backend
- Velocity can cache a returned remote bundle until it expires, so later clicks on other views from the same message do not have to ask the source backend again
- because current per-message parsing shares one bundle, normal chat generally emits one bundle ref even when the message contains several ChatItem tokens; `network.max-snapshots-per-message` remains a defensive protocol bound
- if the remote bundle is missing, expired, rejected, or not returned before `network.request-timeout-seconds`, the player receives the normal expired-preview feedback
- oversized or invalid ChatItem bundle payloads are rejected without blocking the chat message itself
- `network.max-compressed-bytes`, `network.max-uncompressed-bytes`, and `network.max-item-bytes` protect Paper-side bundle serialization and import during remote click handling
- `network.request-timeout-seconds` is clamped to at least `1`
- ChatItem byte limits are clamped to at least `1024` bytes

---

## 4.9.1 `keywords.yml`

Purpose:
- interactive reusable chat tokens
- one MiniMessage renderer per keyword
- optional permission gates
- optional PlaceholderAPI expansion per keyword
- optional channel restrictions

Current bundled default:

```yml
# Interactive keyword token configuration.
# Each keyword replaces one or more literal aliases with one MiniMessage renderer.

keywords:
  discord:
    enabled: true
    tokens: [ "[discord]", "[dc]" ]
    renderer: "<blue><hover:show_text:'<gray>Join our Discord</gray>'><click:open_url:'https://discord.gg/example'>discord</click></hover></blue>"
    allow-placeholderapi: false
    permission: "corechatx.keywords.use.discord"
    enabled-channels: []
    disabled-channels: []

  rules:
    enabled: true
    tokens: [ "[rules]" ]
    renderer: "<yellow><hover:show_text:'<gray>Click to read the rules</gray>'><click:run_command:'/rules'>rules</click></hover></yellow>"
    allow-placeholderapi: false
    permission: ""
    enabled-channels: []
    disabled-channels: []
```

Behavior notes:
- token aliases are matched literally and case-sensitively
- if a sender lacks the configured permission, the token remains plain text
- if `enabled-channels` is non-empty, the keyword only works in those channels
- if the current channel is listed in `disabled-channels`, the token remains plain text
- if `allow-placeholderapi: true`, PlaceholderAPI is applied to the renderer before MiniMessage deserialization when PlaceholderAPI is installed and enabled
- in proxy mode, network chat and remote PMs transport the rendered component from the source backend
- invalid keyword definitions only disable themselves and log a warning

---

## 4.9.2 `chatbubbles.yml`

Purpose:
- optional overhead chat bubbles for successful player public chat
- per-player default toggle
- channel/world filtering
- TextDisplay visual settings
- lifetime, stacking, wrapping, and cleanup behavior

Current bundled default:

```yml
# Chat bubbles / overhead chat configuration.

enabled: true
default-enabled: true

permission: "corechatx.chatbubbles.use"

enabled-channels: []
disabled-channels: [ "staff" ]

max-active-bubbles: 3

base-height: 0.75
stack-offset: 0.32

max-visible-distance: 32

base-duration-ticks: 80
ticks-per-character: 2
max-duration-ticks: 160

max-plain-length: 80
max-line-length: 28

shadow: true
see-through: false

text-color: "#FFFFFF"
background-color: "#80000000"

hide-while-sneaking: false
hide-if-invisible: true

world-filter:
  enabled: false
  disabled-worlds: []

renderer:
  format: "{message}"
```

Behavior notes:
- bubbles are created only after public chat passes normal CoreChatX checks
- the bubble text is derived from the already processed message body, not raw input
- `allow-chat-bubbles: false` in `channels.yml` disables bubbles for that channel
- player settings include a persistent chat bubbles toggle
- in proxy mode, bubbles stay local to the Paper backend where the sender physically is
- bubble entities are removed on expiry, player quit, reload, and plugin disable
- numeric bubble limits are bounded defensively: counts and text lengths stay at least `1`, `ticks-per-character` stays at least `0`, and distances/offsets cannot become invalid negative values

---

## 4.10 `discord.yml`

Purpose:
- Discord outbound formatting and routing
- Discord inbound gateway relay settings
- route mapping from Discord channels into CoreChatX channels

Current bundled default:

```yml
# Discord outbound bridge.
# Only locally-sent messages from channels with export-to-bridges: true are forwarded.
# In network mode, CoreChatX exports only once on the source backend to avoid duplicates.

discord:
  # Master switch for Discord support.
  enabled: false

  # Bot token used for Discord API access.
  bot-token: ""

  # Default Discord channel id used when no per-channel override exists.
  default-channel-id: ""

  # Export format.
  # Common placeholders:
  # {source_server}, {channel_id}, {sender_name}, {plain_text}
  format: "[{source_server}] [{channel_id}] {sender_name}: {plain_text}"

  # Optional mapping from CoreChatX channel id -> Discord channel id.
  channel-overrides: {}

  inbound:
    # Enables inbound Discord relay.
    enabled: false

    # Default CoreChatX channel used if no explicit route matches the Discord channel id.
    default-channel: "global"

    # Maximum accepted inbound message length after sanitization/clamping.
    max-length: 400

    # Optional route map from Discord channel id -> CoreChatX channel id.
    channel-routes: {}
```

Important operational note:
- Discord inbound requires `MESSAGE CONTENT` intent on the bot
- Discord inbound uses the Discord Gateway through JDA, not REST polling
- `deployment.bridges-allowed: false` prevents Discord outbound and inbound runtime from starting even if `discord.enabled` or `discord.inbound.enabled` is true
- `discord.enabled: false` disables Discord inbound even if `discord.inbound.enabled: true`
- Discord outbound applies a small backoff when Discord responds with HTTP 429 rate limits
- Discord outbound clamps formatted messages to a safe API-sized payload and logs when truncation happens
- the bot must also have access to the guild channels listed in `discord.default-channel-id` or `discord.inbound.channel-routes`
- CoreChatX ignores inbound messages from bots and webhooks
- inbound messages are sanitized and clamped by `discord.inbound.max-length` before they enter Minecraft chat
- in proxy mode, enable inbound on the backend instances that should locally display Discord messages

---

## 4.11 `telegram.yml`

Purpose:
- Telegram outbound formatting and routing
- Telegram Bot API long-polling inbound relay settings
- route mapping from Telegram chats or forum topics into CoreChatX channels

Current bundled default:

```yml
# Telegram bridge.
# Uses Telegram Bot API only.
# Inbound uses long polling through getUpdates(timeout=...).
# Outbound uses sendMessage.
# No webhook, no Telegram4J, no MTProto.

telegram:
  enabled: false

  bot-token: ""

  # Fallback target for outbound messages when no per-channel target is configured.
  # Can be a numeric chat id or a Bot API-supported @username target.
  default-chat-id: ""

  format: "[{source_server}] [{channel_id}] {sender_name}: {plain_text}"

  outbound:
    # Per-CoreChatX-channel Telegram targets.
    # message-thread-id targets a Telegram forum topic when greater than 0.
    channel-targets:
      global:
        chat-id: ""
        message-thread-id: 0
      staff:
        chat-id: ""
        message-thread-id: 0

  inbound:
    enabled: false

    # Long polling timeout passed to Telegram getUpdates.
    timeout-seconds: 30

    # Small delay between normal long-poll cycles.
    retry-delay-seconds: 3

    # Backoff delay after network/API/parsing errors.
    error-backoff-seconds: 10

    skip-pending-on-start: true
    max-length: 400
    default-channel: "global"

    # Logs detected safe route keys to help configure groups/topics.
    debug-route-detection: false

    # Inbound route map.
    # Keys can be "chatId" or "chatId:messageThreadId".
    routes:
      # "-1001111111111": "global"
      # "-1001111111111:25": "staff"
```

Important operational note:
- Telegram uses Telegram Bot API long polling through `getUpdates`
- Telegram does not use Telegram4J
- Telegram does not use MTProto
- Telegram does not use webhook mode and does not start an embedded HTTP server
- no `api-id` or `api-hash` are required; only `telegram.bot-token` is needed
- outbound supports per-channel Telegram targets through `telegram.outbound.channel-targets`
- outbound supports Telegram forum topics through `message-thread-id`
- Telegram outbound clamps formatted messages to a safe Bot API-sized payload and logs when truncation happens
- inbound routes can use `chatId` or `chatId:messageThreadId`
- `debug-route-detection: true` logs safe detected route keys to help admins configure group/topic routing
- `deployment.bridges-allowed: false` prevents Telegram outbound and inbound runtime from starting even if `telegram.enabled` or `telegram.inbound.enabled` is true
- `skip-pending-on-start: true` prevents old queued updates from flooding Minecraft when the bridge starts
- Telegram long-polling requests use a bounded HTTP timeout so reloads cannot leave an old polling request hanging indefinitely
- Telegram inbound timing values are clamped: `timeout-seconds` to `1..60`, `retry-delay-seconds` to `0..30`, and `error-backoff-seconds` to `1..300`
- inbound messages are sanitized and clamped by `telegram.inbound.max-length` before they enter Minecraft chat

---

## 4.12 `storage.yml`

Purpose:
- storage backend declaration
- future-facing SQL config placeholders

Current bundled default:

```yml
# Storage foundation for Run 1.
# YAML remains the active backend in this phase.

storage:
  # Current public implementation uses YAML repositories.
  backend: "YAML"

  sql:
    # Placeholder section for future SQL work.
    enabled: false
    type: "SQLITE"
    host: "localhost"
    port: 3306
    database: "corechatx"
    username: "user"
    password: "password"
```

Important accuracy note:
- SQL settings exist as groundwork
- the active implementation in the codebase is still YAML-backed
- YAML runtime stores are saved through a temporary file and replace move to reduce corruption risk during writes

---

## 4.13 `locales/en_us.yml`

Purpose:
- locale override file
- per-key localized message overrides

Current bundled default:

```yml
# Locale foundation for later locale-aware messaging.
# The plugin currently still uses messages.yml as the active source of truth.

general:
  player_not_found: "<red>Player not found.</red>"

commands:
  corechatx-help:
    header: "{prefix} <gray>Available CoreChatX subcommands:</gray>"
    reload: "<gray>/corechatx reload</gray>"
    settings: "<gray>/corechatx settings</gray>"
    locale: "<gray>/corechatx locale [tag]</gray>"

privacy:
  ignore:
    usage: "<red>Usage: /ignore <player></red>"
    self: "<red>You cannot ignore yourself.</red>"
    already: "<yellow>You are already ignoring that player.</yellow>"
    success: "<gray>You are now ignoring <white>{target_name}</white>.</gray>"

moderation:
  clear-chat-sender: "{prefix} <gray>Cleared chat for <white>{count}</white> online player(s).</gray>"
```

How locale resolution works now:
1. selected player locale file
2. `en_us.yml`
3. `messages.yml`

That means locale files are override layers, not the only message source.

---

## 5. Paper Runtime Data Files

These files are not your main admin-facing config, but they are part of the live plugin state.

## 5.1 `playerdata.yml`

Purpose:
- per-player saved settings

Bundled default:

```yml
players: {}
```

Typical keys written at runtime per UUID:
- `ping-sound`
- `ping-actionbar`
- `social-spy`
- `pm-enabled`
- `mention-notifications`
- `staff-chat-enabled`
- `chat-bubbles-enabled`
- `locale`

This file is normally not edited by hand.

Proxy sync note:
- in `PROXY` mode, the saved communication state is published through Velocity when supported settings change
- receiving backends still respect their own permissions; a synced active channel is not forced if the player cannot use that channel on the destination backend

---

## 5.2 `state.yml`

Purpose:
- global runtime state

Bundled default:

```yml
first-joins:
  count: 0
chat:
  muted: false
```

This file stores:
- first-join counter
- current global chat-muted state

---

## 5.3 `channeldata.yml`

Purpose:
- per-player active channel selection

Created:
- on demand, when players actually switch away from the implicit default

Typical structure:

```yml
players:
  00000000-0000-0000-0000-000000000000:
    active-channel: "staff"
```

---

## 5.4 `ignoredata.yml`

Purpose:
- ignore lists

Created:
- on demand, when ignore data exists

Typical structure:

```yml
players:
  00000000-0000-0000-0000-000000000000:
    - "11111111-1111-1111-1111-111111111111"
    - "22222222-2222-2222-2222-222222222222"
```

---

## 5.5 `mutedata.yml`

Purpose:
- active mute records

Created:
- on demand, when mute data exists

Typical structure:

```yml
players:
  00000000-0000-0000-0000-000000000000:
    created-at: 1710000000000
    until: 1710003600000
    reason: "Spam"
    actor: "33333333-3333-3333-3333-333333333333"
    blocks-private-messages: true
```

For permanent mutes, `until` may be missing or non-positive depending on how the record was created.

---

## 6. Velocity Proxy

The proxy side is intentionally minimal.

Folder:

```text
plugins/corechatx/
```

## 6.1 `velocity-config.properties`

Purpose:
- declare the shared plugin messaging channel used by the Velocity module
- control proxy-provided player chat completions and TAB entries

Current bundled default:

```properties
# CoreChatX Velocity proxy settings.
# Keep this value aligned with deployment.network-channel on all Paper backends.
network-channel=corechatx:main

# Adds global player names and @names to client chat completions.
player-directory.chat-completions=true

# Adds proxy-wide online players to TAB where Velocity exposes them.
player-directory.tab-entries=true
```

Important rules:
- this must exactly match every backend `deployment.network-channel`
- changing this file requires a Velocity restart
- changing `network-channel` also requires restart on affected backends after their `deployment.network-channel` is aligned
- Velocity startup prints the same `CORECHATX` banner shape with version, author, Velocity architecture, and project-link placeholder
- if invalid, the proxy logs degraded routing state instead of silently pretending everything is fine
- the same lowercase namespaced-key validation used by Paper is applied here
- Velocity normalizes packet source identity to the real backend connection and logs mismatches
- `player-directory.chat-completions: false` leaves client chat completions untouched by CoreChatX
- `player-directory.tab-entries: false` leaves client TAB entries untouched by CoreChatX

---

## 7. Regenerating a Clean Config Set

If you want a clean reset:

### Paper backend

Delete the generated admin config files:

```text
config.yml
messages.yml
chat.yml
channels.yml
privacy.yml
moderation.yml
pings.yml
filter.yml
chatitems.yml
keywords.yml
chatbubbles.yml
discord.yml
telegram.yml
storage.yml
locales/
```

Then restart the server.
CoreChatX recreates the bundled defaults automatically.

If you are using proxy mode, you must then reapply:
- `deployment.mode: "PROXY"`
- unique `deployment.server-id`
- `deployment.network-features-allowed: true`

### Velocity proxy

Delete:

```text
plugins/corechatx/velocity-config.properties
```

Then restart Velocity.
The proxy module recreates its bundled default file.

---

## 8. Config-driven Permissions

Some permissions are defined in config files and can be changed by the server owner.

Examples:
- `pings.yml -> custom-pings.<id>.use-permission`
- `pings.yml -> custom-pings.<id>.receive-permission`
- `keywords.yml -> keywords.<id>.permission`
- `channels.yml -> channels.<id>.permission-send`
- `channels.yml -> channels.<id>.permission-receive`
- `privacy.yml -> private-messages.staff-bypass-permission`
- `chatitems.yml -> tokens.<type>.permission`

These nodes may not all be listed in `plugin.yml` because they are configurable values, not fixed plugin API.

Validation behavior:
- `channels.yml`, `pings.yml`, and `privacy.yml` validate malformed dynamic permission nodes and disable only the broken binding through an internal disabled permission node
- `keywords.yml` validates `keywords.<id>.permission`; an invalid permission disables that keyword definition
- `chatitems.yml` token permissions are read as configured, so keep them as valid Bukkit permission nodes

Bundled fixed command permissions are intentionally split:
- player-facing commands such as `/corechatx`, `/corechatx settings`, `/corechatx locale`, `/msg`, `/reply`, `/ping`, `/channel`, `/ignore`, `/unignore`, `/ignorelist`, `/pmtoggle`, and `/chatsettings` default to `true`
- administrative and moderation commands such as reload, broadcast, social spy, mute, unmute, mutechat, and clearchat remain `op` by default

---

## 9. Practical Admin Notes

- Keep `messages.yml` as the main wording file unless you actively need locale-specific overrides.
- Use LuckPerms for rank titles/prefixes, and use CoreChatX for layout.
- Keep `group-formats` keyed to the LuckPerms primary group in lowercase.
- Treat SQL settings as groundwork, not as the active public storage backend.
- When testing a network, always verify backend `server-id` values are unique.
- If a custom permission node in `channels.yml`, `pings.yml`, or `privacy.yml` is malformed, CoreChatX warns and disables only that specific binding; malformed keyword permissions disable only that keyword.
- If you delete runtime data files such as `playerdata.yml` or `mutedata.yml`, you are deleting live player/plugin state, not just decorative cache.

Operational guidance:
- treat `config.yml -> deployment.*` and `velocity-config.properties -> network-channel` as boot identity, not casual reload settings
- keep one source of truth for each visual decision; do not duplicate the same format across many plugins
- use channels for routing rules instead of hardcoding bridge behavior in several places
- use PlaceholderAPI in trusted templates, not as a way to let players execute arbitrary placeholder expansion through chat
- keep bridge tokens private and never paste them into public support logs
- back up runtime data before manually editing player settings, mutes, ignores, or active channel state

---

## 10. Troubleshooting Quick Reference

### Startup banner does not appear

Check that the correct jar is installed for the platform.
The Paper jar must be on Paper, and the Velocity jar must be on Velocity.
On startup, CoreChatX prints a `CORECHATX` banner with version, author, architecture, and a project-link placeholder.

### `/corechatx reload` does not apply a change

Some settings are identity or transport settings and require restart:
- `deployment.mode`
- `deployment.server-id`
- `deployment.network-channel`
- Velocity `network-channel`

If the change affects runtime transport, restart the affected backend or proxy instead of relying on reload.

### Network messages do not cross servers

Verify:
- every backend uses `deployment.mode: "PROXY"`
- every backend has a unique `deployment.server-id`
- every backend has `deployment.network-features-allowed: true`
- every backend and Velocity use the exact same lowercase `network-channel`
- the Velocity jar is installed and started
- at least one eligible player connection exists for plugin-message transport when Paper needs a packet carrier

Also check that the channel itself is configured as `scope: NETWORK`.
In standalone mode, `NETWORK` channels fall back to normal local chat behavior.

### Discord or Telegram messages do not send

Verify:
- `deployment.bridges-allowed: true`
- the specific bridge `enabled` value is true
- the CoreChatX channel has `export-to-bridges: true`
- tokens, channel ids, chat ids, and topic ids are valid
- outbound routing points to the intended external target

In proxy mode, outbound export is performed only from the source backend to avoid duplicate external messages.

### Discord or Telegram inbound does not appear in Minecraft

Verify:
- inbound is enabled for that bridge
- the inbound route points to an existing CoreChatX channel
- the backend running the inbound listener is one that should locally display the message
- for Discord, the bot has the required Message Content intent when message content is needed
- for Telegram, the bot is reachable through Bot API long polling

Telegram uses Bot API `getUpdates`.
It does not use MTProto, Telegram4J, webhook mode, or an embedded HTTP server.

### PlaceholderAPI placeholders do not render

Verify:
- PlaceholderAPI is installed on that backend
- `hooks.placeholderapi: true`
- the expansion that owns the placeholder is installed and working
- the string being edited is a supported server-controlled template

Raw player message bodies do not pass through PlaceholderAPI.
Mention token PlaceholderAPI context is the mentioned player, using `OfflinePlayer` where possible for cross-backend mentions.

### Mentions do not notify players

Verify:
- mentions are enabled in `chat.yml`
- the channel has `allow-mentions: true`
- the target player allows mention notifications
- ignore and privacy settings are not blocking the notification
- permissions and staff bypass rules are configured as intended

### ChatItems work locally but not across servers

Verify:
- backend and Velocity network settings match
- ChatItems are enabled for the channel
- remote snapshot request limits are not rejecting the item bundle
- the snapshot has not expired
- `network-features-allowed` is true on participating backends

Cross-server ChatItems use lightweight refs in chat and on-demand snapshot retrieval through Velocity.
They intentionally avoid embedding a full inventory payload into every chat packet.

### Chat bubbles are missing

Verify:
- chat bubbles are enabled in `chatbubbles.yml`
- the player has the bubble permission
- the player has not disabled bubbles in settings
- the channel has `allow-chat-bubbles: true`
- the message passed normal CoreChatX checks

In proxy mode, bubbles stay local to the backend where the sender is physically playing.

### A config permission warning appears

Check dynamic permission nodes in:
- `channels.yml`
- `pings.yml`
- `privacy.yml`
- `keywords.yml`
- `chatitems.yml`

Malformed nodes disable only the affected binding or keyword where validation is available.
They do not require deleting the whole config.

### A runtime data file looks wrong

Stop the server, back up the file, then inspect it.
Runtime files such as `playerdata.yml`, `channeldata.yml`, `ignoredata.yml`, and `mutedata.yml` are live state.
Deleting or editing them changes player/plugin data.

---

## 11. Production Validation Checklist

Before opening a server to players:
- boot every Paper backend once and confirm the `CORECHATX` startup banner
- boot Velocity once and confirm the proxy-side `CORECHATX` startup banner
- run a normal public chat message
- run a channel switch and verify send/receive permissions
- test `/msg` and `/reply`
- test mentions, custom pings, and notification toggles
- test one PlaceholderAPI value in a server-controlled template
- test one keyword hover/click action
- test one ChatItem preview locally
- in proxy mode, test one network channel message from each backend
- in proxy mode, test one cross-backend PM
- in proxy mode, test one cross-backend ChatItem preview click
- test Discord outbound and inbound if enabled
- test Telegram outbound, inbound, and topic routing if enabled
- test mute, mutechat, clear chat, social spy, ignore, and PM toggle behavior
- run `/corechatx reload` after a harmless wording change and confirm it applies

For source builds, useful checks are:

```bash
mvn clean test
mvn clean package -DskipTests
git diff --check
```

Build-time warnings from Maven Shade about overlapping metadata/resources can be normal for shaded dependency jars.
They should still be reviewed when dependencies change.

---

## 12. Related Documents

- `README.md`: feature overview written for server owners and presentation pages
- `FAQ.md`: common questions and short answers
- this file: exact config keys, defaults, restart rules, and operational notes

This document is aligned to the current codebase and bundled defaults as of the present project state.
