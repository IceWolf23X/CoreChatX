# ChatBloom — Codex Run 3
## Deep review, cleanup, hardening, consistency, and final polish

**Purpose:** this file is the instruction set for the **third 5h Codex run**.

This run assumes:
- Run 1 created the architecture and module split
- Run 2 completed most of the implementation

This run is **not** for inventing major new features.
This run is for making the codebase safer, cleaner, more consistent, and easier to maintain.

---

# 0. Primary objective

Perform a deep review of the entire ChatBloom codebase and improve it in place without changing the intended product scope.

Priority order:

1. compile/build correctness
2. runtime safety
3. consistency across modules
4. edge-case handling
5. config clarity
6. maintainability
7. polish
8. final code comments/TODO cleanup
9. documentation comments where useful

---

# 1. Absolute constraints

## 1.1 Do not add new major systems unless required to fix broken architecture

This run should not suddenly introduce:
- Twitch support
- webhook systems
- new unrelated social systems
- totally new persistence patterns
- a second architecture

## 1.2 Preserve the deployment model

Keep:
- `chatbloom-common`
- `chatbloom-paper`
- `chatbloom-velocity`
- two jars
- standalone Paper mode
- optional proxy-enabled mode
- restart-required mode switching

## 1.3 Prioritize safe cleanup over feature inflation

This run is about:
- reducing risk
- fixing hidden problems
- improving readability
- ensuring feature boundaries are clean

---

# 2. What this run must review

## 2.1 Build and module hygiene
Check:
- all modules compile cleanly
- no accidental platform dependency leaked into `chatbloom-common`
- unused dependencies removed
- duplicated code reduced where appropriate
- package naming stays consistent

## 2.2 Bootstrap correctness
Check:
- Paper bootstrap only registers what belongs to Paper
- Velocity bootstrap only registers what belongs to Velocity
- deployment mode is validated clearly
- restart-required behavior is enforced clearly
- logs are explicit and helpful

## 2.3 Config ownership and clarity
Check:
- each setting lives in the right file
- comments/defaults are coherent
- old config compatibility is not broken carelessly
- invalid config values fail safely
- missing config sections produce sane defaults where appropriate

## 2.4 Repository layer
Check:
- repositories are the only persistence access path
- YAML save/load logic is safe
- no feature silently bypasses repositories
- file write behavior is not dangerously duplicated
- missing data defaults are consistent

## 2.5 Pipeline and services
Check:
- pipeline steps are ordered logically
- services own domain logic, not listeners
- listeners are thin and platform-specific
- commands delegate to services/use cases
- cancel reasons/messages are consistent

## 2.6 Channels / privacy / moderation
Check:
- state persistence
- permission checks
- bypass behavior
- fallback behavior
- null safety
- offline/invalid target handling
- clear user feedback

## 2.7 GUI
Check:
- click cancellation
- slot validation
- stale inventory handling
- toggles persist correctly
- title/component consistency

## 2.8 Proxy mode
Check:
- standalone mode remains clean
- proxy mode failure handling is safe
- routing logic is not duplicated
- packet DTOs remain versionable and simple
- disabled proxy features do not explode in standalone mode

## 2.9 Bridges
Check:
- bridge failures do not crash the plugin
- missing credentials/config disable cleanly
- inbound text is sanitized
- outbound format is consistent
- logs are useful but not spammy

---

# 3. Improvements Codex should actively make

## 3.1 Replace weak spots with stronger local abstractions
Examples:
- repeated config path strings -> constants or config accessors
- repeated message key usage -> message key constants
- repeated permission strings -> permission constants
- repeated null/empty checks -> helper methods where sane

## 3.2 Improve defensive coding
Examples:
- guard invalid UUID parse
- guard missing target player
- guard null clicked inventory item
- guard missing config sections
- guard impossible state transitions
- guard packet handling when transport is disabled

## 3.3 Improve maintainability
Examples:
- extract use-case classes where command classes got too fat
- reduce duplicated component building
- reduce duplicated sender feedback logic
- reduce repeated YAML save boilerplate where clean

## 3.4 Improve logs
Desired logs:
- deployment mode active
- proxy bridge enabled/disabled
- Discord/Telegram enabled/disabled
- repository type active
- fallback behavior warnings only when useful
- config problems that are actionable

---

# 4. Concrete review checklist

## 4.1 Common module audit
Verify:
- no Bukkit imports
- no Velocity imports
- only shared DTO/contracts/helpers
- record/class naming consistency
- serializers are self-contained

## 4.2 Paper module audit
Verify:
- listeners are thin
- GUI logic is isolated
- Paper-only APIs do not leak into common
- ChatItems remain stable
- command registration stays coherent

## 4.3 Velocity module audit
Verify:
- routing is centralized
- bridge startup is isolated
- packet handling is centralized
- no random feature logic spread across listeners/subscribers

## 4.4 Command audit
Verify:
- permission checks are consistent
- sender type checks are explicit
- response messages are not hardcoded all over
- argument parsing failures are friendly

## 4.5 Persistence audit
Verify:
- no stale save methods
- no duplicate file writes per tiny action if avoidable
- repository outputs remain consistent after reload/restart

---

# 5. Code examples for the kind of cleanup expected

## 5.1 Permission constants

```java
public final class ChatBloomPermissions {

    public static final String STAFF = "chatbloom.staff";

    public static final String CHANNEL_LOCAL = "chatbloom.channel.local";
    public static final String CHANNEL_STAFF = "chatbloom.channel.staff";

    public static final String IGNORE = "chatbloom.command.ignore";
    public static final String MUTE = "chatbloom.command.mute";

    private ChatBloomPermissions() {
    }
}
```

## 5.2 Message key constants

```java
public final class MessageKeys {

    public static final String PLAYER_NOT_FOUND = "messages.player-not-found";
    public static final String PM_DISABLED = "messages.pm-disabled";
    public static final String TARGET_IGNORING_YOU = "messages.target-ignoring-you";
    public static final String CHANNEL_NOT_FOUND = "messages.channel-not-found";
    public static final String MUTED = "messages.muted";

    private MessageKeys() {
    }
}
```

## 5.3 Safer command pattern

```java
public abstract class BasePlayerCommand {

    protected boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can use this command."));
            return false;
        }
        return true;
    }

    protected boolean requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You do not have permission."));
            return false;
        }
        return true;
    }
}
```

## 5.4 Safer GUI click handling

```java
@EventHandler
public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
        return;
    }

    if (!settingsGuiTracker.isTracked(player.getUniqueId(), event.getView())) {
        return;
    }

    event.setCancelled(true);

    ItemStack clicked = event.getCurrentItem();
    if (clicked == null || clicked.getType().isAir()) {
        return;
    }

    int slot = event.getRawSlot();
    if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
        return;
    }

    settingsService.handleSlotClick(player, slot);
}
```

---

# 6. Definition of Done for Run 3

Run 3 is complete only if all of the following are true:

- all modules compile cleanly
- no obvious platform leakage exists across module boundaries
- standalone mode still works
- proxy mode still works according to its current implemented scope
- command, config, repository, and service ownership are consistent
- there are fewer duplicated strings and magic values
- error handling is improved
- null safety and argument validation are improved
- bridge failure behavior is safe
- logs are more actionable
- the codebase is easier to review and extend than before Run 3

---

# 7. Test scenarios Codex must think through during review

## 7.1 Standalone mode
- clean startup
- reload behavior
- chat still formats correctly
- PMs still work
- mentions still work
- ChatItems still work
- settings GUI still works

## 7.2 Proxy mode
- startup with proxy mode enabled
- startup with proxy mode configured but proxy unavailable
- cross-server PM edge cases
- routing when target disconnects mid-flow
- disabled bridge but packets still attempted

## 7.3 Privacy
- ignored sender
- PM disabled target
- ignore persistence after restart
- ignore removal after restart

## 7.4 Moderation
- mute expiry
- mute persistence
- anti-repeat reset behavior
- bypass permissions
- command parse errors

## 7.5 Config
- missing section
- invalid enum
- empty string values
- legacy config values
- comments/default defaults still coherent

## 7.6 Bridges
- token missing
- channel/chat id missing
- malformed inbound message
- external service unavailable
- plugin disable while bridge active

---

# 8. Anti-patterns to avoid in this run

Do not:
- introduce a brand new architecture again
- rewrite everything just for style
- add flashy but unrequested features
- hardcode more strings than before
- move domain logic back into listeners
- over-abstract tiny things that were already clear
- leave the code more complex than before

---

# 9. Final instruction to Codex

Use this run to make ChatBloom safer, cleaner, more maintainable, and more predictable.

Prefer:
- cleanup
- hardening
- consistency
- defensive coding
- ownership clarity
- polish

over:
- unnecessary feature creep
- giant rewrites
- architecture churn

This run should make the project feel **production-closer**, not just “more coded”.
