package icewolf23x.chatBloom.service;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

final class RemotePmTracker {

    private final Map<UUID, PendingRemoteMessage> pending = new HashMap<>();
    private final Duration timeout;

    RemotePmTracker(Duration timeout) {
        this.timeout = timeout;
    }

    void put(UUID requestId, CommandSender sender, String senderName, String targetName, String sanitizedMessage) {
        pending.put(requestId, new PendingRemoteMessage(sender, senderName, targetName, sanitizedMessage, Instant.now()));
    }

    PendingRemoteMessage remove(UUID requestId) {
        return pending.remove(requestId);
    }

    void pruneExpired() {
        Instant cutoff = Instant.now().minus(timeout);
        Iterator<Map.Entry<UUID, PendingRemoteMessage>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().createdAt().isBefore(cutoff)) {
                iterator.remove();
            }
        }
    }

    void pruneForOfflineSenders() {
        Iterator<Map.Entry<UUID, PendingRemoteMessage>> iterator = pending.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingRemoteMessage value = iterator.next().getValue();
            if (value.sender() instanceof Player player && !player.isOnline()) {
                iterator.remove();
            }
        }
    }

    record PendingRemoteMessage(
        CommandSender sender,
        String senderName,
        String targetName,
        String sanitizedMessage,
        Instant createdAt
    ) {
    }
}
