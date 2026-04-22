package me.icewolf23.chatbloom.paper.gui;

import me.icewolf23.chatbloom.common.model.PlayerSettingsRecord;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class SettingsMenuListener implements Listener {

    private final PlayerStateRepository playerStateRepository;
    private final SettingsMenuFactory settingsMenuFactory;

    public SettingsMenuListener(PlayerStateRepository playerStateRepository, SettingsMenuFactory settingsMenuFactory) {
        this.playerStateRepository = playerStateRepository;
        this.settingsMenuFactory = settingsMenuFactory;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!settingsMenuFactory.isSettingsInventory(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        PlayerSettingsRecord current = playerStateRepository.load(player.getUniqueId());
        PlayerSettingsRecord updated = switch (event.getRawSlot()) {
            case SettingsMenuFactory.SLOT_PING_SOUND -> new PlayerSettingsRecord(
                current.playerId(),
                !current.pingSoundEnabled(),
                current.pingActionbarEnabled(),
                current.socialSpyEnabled(),
                current.pmEnabled()
            );
            case SettingsMenuFactory.SLOT_PING_ACTIONBAR -> new PlayerSettingsRecord(
                current.playerId(),
                current.pingSoundEnabled(),
                !current.pingActionbarEnabled(),
                current.socialSpyEnabled(),
                current.pmEnabled()
            );
            case SettingsMenuFactory.SLOT_PRIVATE_MESSAGES -> new PlayerSettingsRecord(
                current.playerId(),
                current.pingSoundEnabled(),
                current.pingActionbarEnabled(),
                current.socialSpyEnabled(),
                !current.pmEnabled()
            );
            case SettingsMenuFactory.SLOT_SOCIAL_SPY -> new PlayerSettingsRecord(
                current.playerId(),
                current.pingSoundEnabled(),
                current.pingActionbarEnabled(),
                !current.socialSpyEnabled(),
                current.pmEnabled()
            );
            default -> null;
        };

        if (updated == null) {
            return;
        }

        playerStateRepository.save(updated);
        player.openInventory(settingsMenuFactory.create(player, updated));
    }
}
