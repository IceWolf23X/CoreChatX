# CoreChatX Feature List

CoreChatX is a complete communication suite for Minecraft servers.
It brings chat, private messages, channels, mentions, pings, chat item previews, chat bubbles, moderation, player settings, locales, and external bridges into one polished system.

The idea is simple: server chat should feel like one product, not a stack of separate plugins that each need their own configuration, commands, and maintenance.

This page is a commercial-style feature overview.
Exact config keys, defaults, limits, and operational details are documented in `PLUGIN_CONFIGURATION_INSTRUCTIONS.md`.

---

## Why CoreChatX

Many servers end up installing one plugin for chat, another for private messages, another for mentions, another for channels, another for Discord, another for Telegram, another for chat bubbles, another for item previews, and then one more just to keep the whole setup consistent.

That works, technically.
It also turns chat into a maintenance hobby.

CoreChatX is built for server owners who want the whole communication experience to feel consistent:

- one visual language
- one permission model
- one player settings flow
- one network-aware chat layer
- one place to manage public chat, PMs, channels, pings, bridges, moderation, and rich interactions

It is not trying to be "yet another chat plugin".
It is trying to replace scattered chat-related plugin stacks with one clean, friendly, server-ready system.

---

## Current State

CoreChatX currently:

- supports Paper `1.21.11`
- ships separate `corechatx-paper` and `corechatx-velocity` jars
- uses `CoreChatX` as the Paper plugin name
- uses `corechatx` as the Velocity plugin id
- works on standalone Paper servers
- works on Velocity networks with multiple Paper backends
- has been tested on a live Paper test server
- has been tested on a small live Velocity network
- supports Discord inbound and outbound messages
- supports Telegram inbound and outbound messages
- supports Telegram forum topic routing
- includes optional chat bubbles above players
- keeps player communication settings aligned across server switches

---

## One Suite, Not Ten Plugins

CoreChatX covers the core communication stack:

- public chat
- private messages
- replies
- channels
- mentions
- custom pings
- interactive keywords
- chat item previews
- chat bubbles
- moderation tools
- player settings
- locale support
- Discord bridge
- Telegram bridge
- standalone and Velocity network setups

The value is not just the feature count.
The value is that these features know about each other.

Channels can affect formatting and bridge export.
Mentions can respect player settings.
Private messages can work with social spy and ignore rules.
Network chat can carry rich Adventure components and ChatItem preview references.
Bridges can follow channel routing instead of acting like a separate side project.

That is the difference between a chat system and separate plugins with overlapping commands and disconnected configuration.

---

## Polished Public Chat

CoreChatX gives public chat the kind of foundation players actually notice:

- clean chat formats
- rank-aware layouts
- channel-aware layouts
- LuckPerms prefix support
- PlaceholderAPI support in server-controlled templates
- join, quit, and first-join messages
- message cleanup and sanitization
- anti-repeat checks
- anti-caps checks
- word filtering

Player-written messages stay safe.
Server-owned templates can use dynamic placeholders, while normal player input is kept under control before it becomes a rich chat component.

---

## Kyori Adventure And MiniMessage

CoreChatX is built around Kyori Adventure and MiniMessage formatting.
That means server owners can create modern chat components without being trapped in old color-code-only formatting.

MiniMessage support makes it easy to use:

- colors
- gradients
- hover text
- click actions
- suggested commands
- copy-to-clipboard actions
- reusable formatting patterns
- clean rich text across chat, messages, previews, and bridge output

This gives CoreChatX a modern Paper-native feel instead of a legacy chat setup with richer formatting bolted on afterward.

---

## PlaceholderAPI-Powered Interactions

CoreChatX can combine MiniMessage with PlaceholderAPI inside server-controlled templates.

That opens the door to fast, flexible interactions based on:

- player data
- ranks and groups
- economy values
- server stats
- world data
- progression systems
- installed PlaceholderAPI expansions
- custom server logic exposed through placeholders

In practice, this means server owners can build dynamic chat interactions without extra glue just to make a hover line or click action show something useful.

If PlaceholderAPI can expose it, CoreChatX can help turn it into part of the chat experience.

---

## Interactive Keywords

Interactive keywords let players type simple tokens such as:

- `[discord]`
- `[rules]`
- `[store]`
- `[vote]`
- `[map]`

CoreChatX can turn those tokens into polished clickable chat elements.

They can include:

- hover text
- click commands
- suggested commands
- URLs
- copied text
- optional permissions
- channel restrictions
- optional PlaceholderAPI rendering

This is the kind of small feature that often becomes "add another plugin".
Here, it is just part of the chat suite.

---

## Mentions And Pings

Mentions and pings are built directly into the message flow.

They support:

- player mentions
- `@Player` and exact-name mention behavior
- custom ping triggers
- sound notifications
- actionbar notifications
- per-player ping toggles
- staff-friendly ping rules
- cross-server mention awareness
- mention formatting based on the mentioned player

Because mentions are part of CoreChatX itself, they can work with formatting, notifications, privacy choices, and Velocity network presence instead of pretending every server is isolated.

---

## Channels

CoreChatX supports multiple channel styles:

- global chat
- local chat
- staff chat
- network-wide chat
- role-based chat spaces
- custom server-defined channels

Channels can decide:

- who can send
- who can receive
- which features are enabled
- how messages look
- whether chat bubbles are allowed
- whether ChatItems are allowed
- whether messages can be exported to Discord or Telegram

This keeps channel behavior centralized instead of scattered across several plugins with separate ideas of how chat should behave.

---

## Private Messages

Private messaging is part of the same communication system as public chat.

Included tools:

- direct messages
- reply command
- social spy
- ignore controls
- PM toggle
- delivery feedback
- cross-server private messages

The result is a PM system that understands the rest of the plugin: privacy, moderation, network routing, formatting, and player settings all live in the same ecosystem.

---

## Moderation Tools

CoreChatX includes the everyday moderation controls a real server needs:

- player mute
- unmute
- global chat mute
- clear chat
- anti-repeat checks
- anti-caps checks
- word filtering
- staff bypass permissions

Moderation is handled inside the chat layer itself.
That means it works naturally with channels, private messages, network behavior, and player settings.

This avoids making separate moderation and chat tools duplicate or guess each other's state.

---

## Chat Item Previews

Players can share clickable previews for:

- held items
- shulker contents
- armor
- hotbar
- inventory
- ender chest

Multiple ChatItem tokens in the same message can reuse the same captured player state while still opening the correct view.

On a Velocity network, players on another backend can still click and open the preview through lightweight snapshot references and on-demand retrieval.
That keeps network chat cleaner and avoids stuffing heavy item data into the main chat packet.

---

## Chat Bubbles

CoreChatX can show recent chat messages above player heads.

Bubble features include:

- player toggle
- channel rules
- world rules
- sneaking checks
- invisibility checks
- configurable duration
- message wrapping
- stacked recent messages
- distance-based visibility
- cleanup on quit, reload, and shutdown

Chat bubbles use the already processed message, so filtering, channels, mentions, and keywords have already done their job.

On a network, bubbles stay local to the server where the sender is physically playing.
That keeps the feature useful without pretending an overhead bubble can float across servers.

---

## Player Controls

Players get direct control over important communication preferences:

- settings menu
- ping toggles
- PM toggle
- channel switching
- chat bubble toggle
- locale selection
- ignore list controls

This makes CoreChatX feel like a player-facing feature, not just an admin-side config file with a chat command attached.

---

## Locale Support

CoreChatX includes a practical locale foundation:

- per-player locale selection
- locale override files
- fallback to the main message file
- cleaner control over player-facing wording

This is useful for multilingual communities and for servers that want every message to sound like it belongs to their brand.

---

## Discord And Telegram Bridges

CoreChatX includes built-in bridge support.

Important disclosure: Discord and Telegram bridges are optional and only run when the server owner enables and configures them.
When enabled, CoreChatX can send selected chat content, player display names, channel labels, and related message formatting to the Discord or Telegram services configured by the server owner.
Inbound bridge messages can also bring Discord or Telegram messages back into configured Minecraft channels.

Bridge features include:

- Discord outbound messages
- Discord inbound messages
- Telegram outbound messages
- Telegram inbound messages
- Telegram forum topic routing
- per-channel bridge targets
- channel-level bridge export control
- global bridge master switch
- cleaner reload and shutdown behavior
- safe message length handling

The bridge layer is treated as part of the chat system, not as a separate external pipe with unrelated routing rules.

That matters because channel rules, formatting, network behavior, and bridge routing should agree with each other.
If they do not, players notice.
Admins notice faster.

---

## Velocity Network Support

CoreChatX supports both standalone Paper servers and Velocity networks.

In network mode, it can keep these features working across servers:

- network-wide chat channels
- cross-server private messages
- player communication settings
- player name suggestions
- TAB entries where available
- mentions and pings
- interactive keyword behavior
- ChatItem preview access
- bridge export without duplicate network messages

This is where the "one suite" approach matters most.
A network setup should not require a separate plugin stack on every backend just to make messages, toggles, mentions, and PMs understand that players moved servers.

---

## Commands

CoreChatX includes commands for:

- plugin management
- `/corechatx` root command
- `/ccx` root command alias
- private messages
- replies
- social spy
- broadcast
- ping controls
- channel controls
- ignore controls
- PM toggle
- settings menu
- mute and unmute
- global chat mute
- clear chat

The commands are designed around the same communication layer, so they feel connected instead of random.

---

## Admin Experience

CoreChatX includes practical behavior for server owners:

- clear startup information
- version and author shown on startup
- Paper or Velocity architecture shown on startup
- `CORECHATX` startup banner
- `corechatx.*` permission namespace
- `corechatx:main` default network channel
- safer config validation
- cleaner failure handling
- safer network message handling
- bridge lifecycle handling on reload and shutdown
- config regeneration support

These are not flashy player-facing features, but they make day-to-day operation much easier to trust and maintain.

---

## Verified

The current project state has been tested through:

- build and test passes
- standalone Paper startup
- Velocity startup
- Paper startup in network mode
- command checks
- config regeneration checks
- live Paper test environment checks
- live Velocity network test environment checks

---

## Future Direction

CoreChatX is designed to grow into a broader communication hub for common server social systems, such as:

- clans and guilds
- towns and settlements
- lands and territories
- parties and lightweight groups

The goal is to make chat aware of the communities players actually build, while keeping communication centralized, consistent, and pleasant to use.

---

## Summary

CoreChatX gives a server:

- polished public chat
- Kyori Adventure MiniMessage formatting
- PlaceholderAPI-powered dynamic templates
- interactive keywords
- mentions and pings
- channels
- private messages
- moderation tools
- ChatItem previews
- chat bubbles
- player settings
- locale support
- Discord and Telegram bridges
- standalone Paper support
- Velocity network support

In short, CoreChatX is for servers that want communication to feel intentional.

If your chat setup needs several plugins, multiple bridge configs, and extra placeholder glue just to produce one consistent message, CoreChatX is built to make that experience simpler.
