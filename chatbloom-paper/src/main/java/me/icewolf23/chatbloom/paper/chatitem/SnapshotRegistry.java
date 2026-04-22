package me.icewolf23.chatbloom.paper.chatitem;

import me.icewolf23.chatbloom.paper.ChatBloom;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SnapshotRegistry {

    private final ChatBloom plugin;
    private final Map<UUID, ChatItemSnapshot> snapshots = new HashMap<>();
    private long expireAfterMillis;

    public SnapshotRegistry(ChatBloom plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        long minutes = plugin.configuration().chatItems().getLong("previews.expire-after-minutes", 30L);
        this.expireAfterMillis = Math.max(1L, minutes) * 60_000L;
        clear();
    }

    public void put(ChatItemSnapshot snapshot) {
        purgeExpired();
        snapshots.put(snapshot.id(), snapshot);
    }

    public Optional<ChatItemSnapshot> find(UUID id) {
        purgeExpired();
        return Optional.ofNullable(snapshots.get(id));
    }

    public void clear() {
        snapshots.clear();
    }

    private void purgeExpired() {
        long threshold = System.currentTimeMillis() - expireAfterMillis;
        snapshots.values().removeIf(snapshot -> snapshot.createdAt() < threshold);
    }
}
