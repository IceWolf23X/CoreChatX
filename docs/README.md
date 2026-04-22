# ChatBloom

ChatBloom is a Paper plugin that replaces the usual mix of separate chat utilities with one configurable system for public chat, mentions, private messages, staff tools, moderation, and chat item previews.

It is built for servers that want one predictable chat pipeline instead of several overlapping plugins handling formatting, pings, PMs, cooldowns, filters, and inventory previews independently.

## Highlights

- One plugin for public chat, mentions, PMs, broadcasts, moderation, and ChatItems
- Safe player input handling with optional permission-based `&` formatting
- Built-in ping toggles, PM spy, persistent player settings, and first-join tracking
- Optional PlaceholderAPI and LuckPerms support with clean fallbacks

## Key Features

- **Public Chat Formatting**  
  Replaces vanilla chat with a controlled format that can include plugin prefix, group prefix, player name, and processed message content.

- **Safe Player Input**  
  Player text is sanitized before rendering. MiniMessage, injected formatting, hex abuse, and unsupported codes are stripped before the final message is built.

- **Permission-Based `&` Formatting**  
  Legacy `&` color and style codes can be allowed per permission node. Authorized players can use them; unauthorized codes are removed automatically.

- **Mentions and Custom Pings**  
  Normal player mentions and configurable tokens such as `@All`, `@Help`, and `@Staff` are supported. Notifications are deduplicated per target and respect player toggle settings unless explicitly bypassed.

- **Private Messaging Tools**  
  Includes `/msg`, `/reply`, and `/socialspy`, with formatted output, PM sounds, logging, and persistent spy state.

- **Staff Broadcasts**  
  Includes `/broadcast` and `/bc`, with support for mentions and custom pings. Console can also send broadcasts.

- **Chat Item Previews**  
  Supports clickable read-only previews for item, shulker, armor, hotbar, inventory, and ender chest snapshots. Preview data is captured at send time and does not change afterward.

- **Moderation Utilities**  
  Includes public chat cooldowns, PM cooldowns, a word filter with optional hover reveal, and logging for chat, PMs, reloads, and broadcasts.

- **Persistent Data**  
  Stores ping preferences, social spy state, and a global first-join counter across restarts.

## Requirements

- Paper `1.21.11`
- Java `21`

## Before You Install

- ChatBloom is meant to be the plugin that owns your main server chat flow on a single Paper server
- It is built against Paper APIs and should not be presented as a plain Bukkit or Spigot plugin
- If another plugin already formats public chat, handles PMs, injects mentions, or renders inventory previews, disable one side before using both together
- All bundled permission nodes default to `op`, so players only receive the features you explicitly grant

## Optional Integrations

- **PlaceholderAPI**  
  With PlaceholderAPI installed, `%placeholders%` used inside ChatBloom config formats are resolved before the final message is rendered. This is useful in places such as public chat format lines, hover text, broadcast formats, PM formats, and other configured templates.  
  PlaceholderAPI is applied to plugin-controlled output, not to raw player-authored text.  
  Without PlaceholderAPI, ChatBloom still works normally, but external `%placeholders%` written in those config strings remain visible as literal text in the final rendered output.

- **LuckPerms**  
  With LuckPerms installed, ChatBloom can read the player's prefix and inject it into the public chat format through `{rank_prefix}`.  
  The integration is intentionally focused on prefix lookup for chat formatting rather than advanced group-specific logic.  
  Without LuckPerms, `{rank_prefix}` falls back to the configured `public-chat.fallback-rank-prefix` value in `chat.yml`, or stays empty if no fallback is configured.

ChatBloom does not require either plugin to run.

## Core Behavior

- Player input is sanitized before rendering
- MiniMessage tags, injected click or hover events, and unsupported formatting are stripped from player text
- Legacy `&` formatting can be selectively allowed through dedicated permission nodes
- Mentions and custom pings are deduplicated per target for notifications
- If an optional feature cannot be applied, the message still sends whenever possible

## Chat Items

Supported default tokens:

- `[item]`
- `[i]`
- `[armor]`
- `[hotbar]`
- `[inventory]`
- `[inv]`
- `[enderchest]`
- `[ender]`
- `[ec]`

If the sender is holding a shulker box and has permission, `[item]` automatically upgrades to a shulker preview.

If the sender does not have permission for a token, that token stays as normal text and no preview is created.

If the sender has permission but there is nothing valid to show, such as an empty hand for `[item]`, the token also stays as normal text.

Console senders do not create ChatItem previews.

## Commands

Normal and staff commands:

- `/chatbloom reload`
- `/msg <player> <message>`
- `/reply <message>`
- `/socialspy`
- `/broadcast <message>`
- `/bc <message>`
- `/ping sound <on|off|toggle|status>`
- `/ping actionbar <on|off|toggle|status>`

Internal technical command:

- `/chatbloom item <uuid>`

`/chatbloom item <uuid>` is used internally by clickable ChatItem previews and is not meant to be a normal user-facing command.

## Console Support

- `/broadcast` works from console
- `/msg` works from console
- Console-origin mentions and custom pings notify players with `Console` as sender
- ChatItems are intentionally disabled for non-player senders

## Permissions

All bundled permissions default to `op`, so server owners can explicitly grant only what they want.

Command permissions:

- `chatbloom.command.chatbloom`
- `chatbloom.command.reload`
- `chatbloom.command.msg`
- `chatbloom.command.reply`
- `chatbloom.command.socialspy`
- `chatbloom.command.broadcast`
- `chatbloom.command.ping`

Cooldown bypass permissions:

- `chatbloom.cooldown.bypass.public`
- `chatbloom.cooldown.bypass.pm`

Ping and staff-related permissions:

- `chatbloom.ping.use.all`
- `chatbloom.ping.use.help`
- `chatbloom.ping.use.staff`
- `chatbloom.staff`

`chatbloom.staff` is used by the default `@Staff` ping as its receive-permission. It is not a broad internal admin bypass; by default it simply decides who is considered a valid `@Staff` ping recipient.

ChatItem permissions:

- `chatbloom.chatitem.item`
- `chatbloom.chatitem.shulker`
- `chatbloom.chatitem.armor`
- `chatbloom.chatitem.hotbar`
- `chatbloom.chatitem.inventory`
- `chatbloom.chatitem.enderchest`

Legacy formatting permissions:

- `chatbloom.format.color.*`
- `chatbloom.format.style.*`

Formatting permission examples:

- `chatbloom.format.color.red` allows `&c`
- `chatbloom.format.color.yellow` allows `&e`
- `chatbloom.format.style.bold` allows `&l`
- `chatbloom.format.style.italic` allows `&o`
- `chatbloom.format.style.reset` allows `&r`

Each legacy code is checked independently. If a player types a code without the matching permission, that code is removed from the message instead of being rendered.

## Configuration

ChatBloom uses separate YAML files to keep configuration organized:

- `config.yml`  
  Main plugin behavior, hooks, logging, reload behavior, join and first-join toggles.
- `messages.yml`  
  General plugin messages, PM format, broadcast format, join or quit text, and ping notifications.
- `chat.yml`  
  Public chat format, mention styling, and chat or PM cooldown settings.
- `pings.yml`  
  Mention notification settings and custom ping definitions such as `@All`, `@Help`, and `@Staff`.
- `filter.yml`  
  Word filter toggle, blocked words, replacement character, and optional hover reveal.
- `chatitems.yml`  
  ChatItem token aliases, permissions, token styling, preview titles, and preview expiration.
- `playerdata.yml`  
  Stored per-player state such as ping toggles and social spy state.
- `state.yml`  
  Global persistent plugin state, currently used for the first-join counter.

The default files include inline comments for the main options and placeholders.

## Internal Placeholders

ChatBloom config files use internal placeholders in the `{placeholder_name}` format.

Common internal placeholders include:

- `{prefix}`  
  The main ChatBloom prefix from `messages.yml`
- `{plugin_prefix}`  
  The public chat plugin prefix from `chat.yml`
- `{rank_prefix}`  
  The resolved LuckPerms prefix or the configured fallback
- `{player_name}`  
  The relevant player name for chat, join or quit, mentions, or preview titles
- `{sender_name}`  
  The sender name used in PMs, broadcasts, and ping notifications
- `{target_name}`  
  The PM target name
- `{message}`  
  The already processed message component
- `{seconds}`  
  Cooldown messages
- `{count}`  
  First-join counter messages
- `{setting}`  
  `/ping ... status` output
- `{state}`  
  Toggle state output such as enabled or disabled
- `{trigger}`  
  Custom ping token rendering such as `@Help`
- `{token}`  
  ChatItem token label such as `[item]`
- `{word}`  
  Word filter hover content

The exact placeholders available depend on the specific config path being rendered.

## Configuration Examples

Example `chat.yml` public chat format:

```yml
public-chat:
  format: "{plugin_prefix} {rank_prefix}<white>{player_name}</white><dark_gray><italic>> </italic></dark_gray>{message}"
  plugin-prefix: "<dark_gray>[</dark_gray><gradient:#79d6b8:#5aa9ff>Chat</gradient><dark_gray>]</dark_gray>"
  fallback-rank-prefix: ""
```

Example `chat.yml` mention styling:

```yml
mentions:
  enabled: true
  token-format: "<#79d6b8>@{player_name}</#79d6b8>"
```

Example `pings.yml` custom pings:

```yml
custom-pings:
  all:
    trigger: "@all"
    use-permission: "chatbloom.ping.use.all"
    receive-permission: ""
    bypass-toggle: false
    token-format: "<#79d6b8>{trigger}</#79d6b8>"
  staff:
    trigger: "@staff"
    use-permission: "chatbloom.ping.use.staff"
    receive-permission: "chatbloom.staff"
    bypass-toggle: true
    token-format: "<#ff8f8f>{trigger}</#ff8f8f>"
```

Example `filter.yml` word filter:

```yml
enabled: true
replacement-character: "*"
blocked-words:
  - "badword"
hover-original:
  enabled: false
  text: "<gray>{word}</gray>"
```

Example permission-based `&` formatting:

```text
chatbloom.format.color.red
chatbloom.format.color.yellow
chatbloom.format.style.bold
chatbloom.format.style.italic
```

With those permissions, a player can use formatting like:

```text
&cRed text &eYellow text &lBold &oItalic
```

Any legacy `&` code without permission is removed automatically.

Example `chatitems.yml` token setup:

```yml
tokens:
  item:
    aliases: [ "[item]", "[i]" ]
    permission: "chatbloom.chatitem.item"
    token-format: "<#79d6b8>[item]</#79d6b8>"
  inventory:
    aliases: [ "[inventory]", "[inv]" ]
    permission: "chatbloom.chatitem.inventory"
    token-format: "<#5aa9ff>[inventory]</#5aa9ff>"
```

## Installation

1. Drop the jar into your server `plugins` folder.
2. Start the server once to generate configuration files.
3. Adjust the YAML files to match your server style and permissions setup.
4. Install PlaceholderAPI or LuckPerms if you want those integrations.
5. Use `/chatbloom reload` after config edits, or restart the server after jar updates.

## Troubleshooting

- If `%placeholders%` appear literally in chat or messages, PlaceholderAPI is missing, disabled, or the placeholder itself is unavailable on the server
- If `{rank_prefix}` stays empty, LuckPerms is missing, disabled, or no fallback rank prefix is configured in `chat.yml`
- If a ChatItem token does nothing, check both the token permission and whether the sender had something valid to snapshot
- If an old clickable preview stops opening, it likely expired or was cleared by reload or restart
- If `/broadcast` or `/msg` is sent from console, ChatItems are intentionally not created

## Summary

ChatBloom is meant to replace the usual stack of separate chat utilities with one consistent Paper plugin that covers formatting, pings, PMs, moderation, and item previews in a predictable way.
