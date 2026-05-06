# CoreChatX FAQ

This FAQ answers the questions a server owner is most likely to ask before or during setup.
For exact config keys and defaults, use `PLUGIN_CONFIGURATION_INSTRUCTIONS.md`.
For a shorter feature overview, use `README.md`.

---

## General

### What is CoreChatX?

CoreChatX is a communication suite for Minecraft servers.
It combines public chat, private messages, channels, mentions, pings, chat item previews, chat bubbles, moderation tools, player settings, locales, Discord, Telegram, and Velocity network support into one system.

### What is the current plugin identity?

Current public identity:
- Paper plugin name: `CoreChatX`
- Velocity plugin id: `corechatx`
- Paper jar: `corechatx-paper-<version>.jar`
- Velocity jar: `corechatx-velocity-<version>.jar`
- main command: `/corechatx`
- main command alias: `/ccx`
- permission namespace: `corechatx.*`
- default network channel: `corechatx:main`

### Was this project previously named differently?

The current project name is CoreChatX.
Current docs, jars, package names, permissions, commands, plugin ids, and default network channels use CoreChatX/corechatx naming.

For old installs, treat differently named folders and configs as previous install data.
For a clean CoreChatX setup, let the plugin generate fresh defaults and manually reapply only the settings you still want.

### Is CoreChatX only a chat formatting plugin?

No.
Formatting is only one part of it.
CoreChatX is designed to replace the usual stack of separate chat, PM, mention, channel, bridge, bubble, and item-preview plugins with one consistent communication layer.

### Which platforms does it support?

The current project targets:
- Paper `1.21.11` for backend servers
- Velocity for proxy/network support

It ships separate Paper and Velocity jars.
Install each jar only on the platform it was built for.

The `corechatx-common` jar is an internal shared build module.
Do not install it directly on Paper or Velocity.

### Do I need Velocity?

No.
CoreChatX works on a standalone Paper server.
Velocity is needed only if you want cross-backend network features such as network channels, cross-server private messages, synced communication settings, global completions, and remote ChatItem preview retrieval.

### Do I install the plugin on every server?

For standalone Paper, install only the Paper jar on that server.

For a Velocity network:
- install the Paper jar on every Paper backend that should use CoreChatX
- install the Velocity jar on the Velocity proxy

Do not install the Paper jar on Velocity.
Do not install the Velocity jar on Paper.

---

## Setup

### What is the difference between `STANDALONE` and `PROXY`?

`STANDALONE` is for one Paper server.
Network transport is inactive, and `NETWORK` channels behave like local chat.

`PROXY` is for Paper backends connected through Velocity.
It enables the backend to participate in cross-server CoreChatX features when `network-features-allowed` is also true.

### What must match on every network backend?

Every participating backend must share the same `deployment.network-channel`.
Velocity must use the same value in `velocity-config.properties`.

Every backend must also have a unique `deployment.server-id`.
Duplicated server ids make routing ambiguous and should be fixed before testing with players.

### Why do some config changes require restart?

Identity and transport settings are initialized at boot.
Changing them with reload would leave parts of the runtime using old values.

Restart after changing:
- `deployment.mode`
- `deployment.server-id`
- `deployment.network-channel`
- Velocity `network-channel`

Most wording, formatting, feature toggles, channel rules, filters, pings, keywords, bubbles, and bridge templates can be changed with `/corechatx reload`.

### Can I delete configs to reset them?

Yes, for generated admin config files.
Stop the server, delete the config file you want to reset, then start again.
CoreChatX recreates the bundled default.

Be careful with runtime data files such as `playerdata.yml`, `channeldata.yml`, `ignoredata.yml`, and `mutedata.yml`.
Those are live player/plugin state, not simple defaults.

### What does `bridges-allowed` do?

It is the global master gate for Discord and Telegram runtime.
When false, bridge outbound and inbound services do not run even if the individual bridge config says enabled.

### What does `network-features-allowed` do?

It is the backend-side gate for proxy/network behavior.
In `PROXY` mode, setting it to false keeps the backend bootable but disables cross-server CoreChatX features.

---

## Formatting And Placeholders

### Does CoreChatX use Kyori Adventure?

Yes.
CoreChatX is built around Kyori Adventure components for modern chat rendering.

### Does CoreChatX support MiniMessage?

Yes.
Server-controlled templates can use MiniMessage for colors, gradients, hover text, click actions, suggested commands, copied text, and rich component formatting.

### Does CoreChatX support PlaceholderAPI?

Yes, where PlaceholderAPI is installed, enabled, and the string is a supported server-controlled template.
This allows formats, hovers, keywords, and other configured text to react to ranks, stats, economy, worlds, progression, and installed expansions.

### Do player-written messages parse PlaceholderAPI?

No.
Raw player chat bodies do not pass through PlaceholderAPI.
That prevents players from typing arbitrary `%placeholder%` tokens and forcing the server to resolve them.

### Which player is used for mention placeholder context?

Mention token rendering uses the mentioned player as PlaceholderAPI context.
This is important for hover text and dynamic mention formatting.

For cross-backend mentions, CoreChatX uses the mentioned player's `OfflinePlayer` context where possible.
That allows many offline-capable PlaceholderAPI expansions to resolve useful data even when the player is online on another backend.

### Why does a placeholder render in one format but not another?

Common causes:
- PlaceholderAPI is not installed on that backend
- `hooks.placeholderapi` is false
- the required expansion is missing
- the placeholder does not support `OfflinePlayer`
- the string is raw player input instead of a trusted server template

---

## Channels, PMs, Mentions, And Settings

### What can channels control?

Channels can control send permissions, receive permissions, message format, scope, mention support, ChatItem support, chat bubble support, and bridge export eligibility.

### Can only some channels go to Discord or Telegram?

Yes.
Use channel routing and `export-to-bridges` so only intentional channels are exported.
This avoids bridges acting like separate chat systems with their own rules.

### Do private messages work cross-server?

In a correctly configured Velocity network, cross-server PM routing can use the proxy route.
In standalone mode, PMs are local to the backend.

### Do mentions work cross-server?

CoreChatX can preserve mention metadata across network chat, and the proxy-side player directory helps find players on other backends.
The final behavior still depends on channel settings, player notification settings, ignore/privacy settings, and permissions.

### Can players disable parts of the chat experience?

Yes.
Player settings include communication preferences such as PM toggle, mention notifications, ping behavior, locale, and chat bubble visibility.
In proxy mode, these settings can follow backend switches.

---

## ChatItems And Chat Bubbles

### What are ChatItems?

ChatItems let players type configured tokens such as item, inventory, or armor placeholders and expose interactive previews in chat.

### Do ChatItems work across servers?

Yes, in network mode they use lightweight references in the rendered chat message and retrieve the needed snapshot on demand through Velocity.
This avoids stuffing large item or inventory payloads into every network chat packet.

### Are huge custom inventories guaranteed to work cross-server?

They are protected by size and safety limits.
The goal is to support real usage without letting oversized metadata destabilize proxy messaging.
If a snapshot is too large, expired, rejected, or unavailable, the click fails closed with the configured expired-preview feedback.

### What is `max-snapshots-per-message`?

It is a defensive protocol bound for how many unique network snapshot bundle references one chat message may carry.
Normal parsing usually produces one shared bundle for the message, even if several ChatItem tokens appear.

### Why can old ChatItem previews expire?

Snapshots are temporary.
They exist so players can click a recent chat preview, not so the server stores every old item preview forever.
Expiry limits memory usage and avoids stale data.

### Do chat bubbles appear across the whole network?

No.
In proxy mode, bubbles stay local to the backend where the sender is physically playing.
An overhead bubble is a local visual effect, not something that can sensibly float above a player on another server.

---

## Discord And Telegram

### Does CoreChatX support Discord?

Yes.
It supports outbound Minecraft-to-Discord messages and inbound Discord-to-Minecraft relay when configured.

Discord inbound uses the Discord Gateway through JDA.
It does not use REST polling for inbound chat.

### What does Discord inbound need?

At minimum:
- a valid bot token
- inbound enabled in `discord.yml`
- a route to an existing CoreChatX channel
- the required Discord intents, including Message Content where message content is needed

### Does CoreChatX support Telegram?

Yes.
It supports outbound Minecraft-to-Telegram messages, inbound Telegram-to-Minecraft relay, and Telegram forum topic routing.

### Does Telegram use MTProto or Telegram4J?

No.
Telegram uses the Telegram Bot API with long polling through `getUpdates`.
There is no MTProto bridge, no Telegram4J runtime, no webhook mode, and no embedded HTTP server.

### Can Telegram messages route by forum topic?

Yes.
Outbound targets can include `message-thread-id`, and inbound routing can map chat ids and topic thread ids to CoreChatX channels.

---

## Permissions And Commands

### Are all permissions fixed in `plugin.yml`?

No.
Some permissions are intentionally configurable in YAML files.
Examples include channel send/receive permissions, ping permissions, keyword permissions, privacy bypass permissions, and ChatItem token permissions.

### What happens if a configured permission is malformed?

Where validation exists, CoreChatX warns and disables only the affected binding or keyword.
The whole plugin should not be treated as broken because one dynamic permission node is invalid.

### Are player commands available by default?

Player-facing commands such as `/corechatx`, `/corechatx settings`, `/corechatx locale`, private messages, replies, pings, channel access, ignore, PM toggle, and chat settings default to normal player access where appropriate.
Administrative and moderation commands remain operator-only by default.

---

## Storage And Data

### Is SQL the active storage backend?

No.
SQL settings currently exist as groundwork for future storage work.
The active public runtime behavior uses the current generated data files documented in the configuration instructions.

### Which files should I back up?

Back up the whole CoreChatX folder before major changes.
At minimum, preserve:
- config files you customized
- `playerdata.yml`
- `channeldata.yml`
- `ignoredata.yml`
- `mutedata.yml`
- bridge configs containing routing ids

Keep private tokens out of public backups and logs.

---

## Build And Testing

### What does "smoke tested" mean?

It means a small practical test was run to prove the main path starts and behaves at a basic level.
It is not the same as exhaustive QA.
For example, starting the server, sending one message, checking one command, or verifying one bridge path can be a smoke test.

### Why do I see Maven Shade warnings while building?

Shaded dependency jars can contain overlapping metadata or service resources.
Some warnings are normal, but dependency changes should still be reviewed.
The important checks are that tests pass, the package builds, and the produced jars start on the intended platform.

### What should I test before inviting real players?

Test:
- startup on every backend and proxy
- public chat
- channels
- private messages and replies
- mentions and custom pings
- PlaceholderAPI templates
- keywords
- ChatItems
- chat bubbles
- moderation commands
- network chat if using Velocity
- cross-server PMs if using Velocity
- Discord and Telegram paths if enabled

Do this before a public rollout, not after players find the broken route for you.

---

## Common Problems

### "My network channel does not work."

Check that every Paper backend and Velocity use the exact same lowercase namespaced key, for example `corechatx:main`.
Also verify that every backend has `deployment.mode: "PROXY"` and `deployment.network-features-allowed: true`.

### "Discord or Telegram is duplicating messages."

In proxy mode, outbound export should happen from the source backend only.
Check that you did not also configure another plugin or another backend path to export the same channel independently.

### "A PlaceholderAPI hover shows the wrong player."

For mentions, CoreChatX uses the mentioned player as context.
If a hover still shows unexpected data, check the placeholder expansion itself and whether it supports offline player resolution.

### "ChatItems say the preview expired."

The snapshot may have expired, been rejected by size limits, or failed to arrive before the network timeout.
This is expected fail-closed behavior.
Increase limits only after checking the size and complexity of the custom items involved.

### "Can I use CoreChatX together with another chat plugin?"

Technically, maybe.
Operationally, be careful.
Two chat plugins trying to format, cancel, bridge, moderate, or route the same message will eventually disagree.
CoreChatX is designed to own the communication flow so the result feels like one system.
