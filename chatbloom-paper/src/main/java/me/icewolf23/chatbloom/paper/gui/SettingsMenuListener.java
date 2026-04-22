package me.icewolf23.chatbloom.paper.gui;

import me.icewolf23.chatbloom.common.model.PlayerSettingsRecord;
import me.icewolf23.chatbloom.common.storage.repository.PlayerStateRepository;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
        if (!settingsMenuFactory.isSettingsInventory(event.getView().title())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory topInventory = event.getView().getTopInventory();
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(topInventory)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= topInventory.getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        PlayerSettingsRecord current = playerStateRepository.load(player.getUniqueId());
        PlayerSettingsRecord updated = switch (rawSlot) {
            case SettingsMenuFactory.SLOT_PING_SOUND -> new PlayerSettingsRecord(
                current.playerId(),
                !current.pingSoundEnabled(),
                current.pingActionbarEnabled(),
                current.socialSpyEnabled(),
                current.pmEnabled(),
                current.mentionNotificationsEnabled(),
                current.staffChatEnabled(),
                current.localeTag()
            );
            case SettingsMenuFactory.SLOT_PING_ACTIONBAR -> new PlayerSettingsRecord(
                current.playerId(),
                current.pingSoundEnabled(),
                !current.pingActionbarEnabled(),
                current.socialSpyEnabled(),
                current.pmEnabled(),
                current.mentionNotificationsEnabled(),
                current.staffChatEnabled(),
                current.localeTag()
            );
            case SettingsMenuFactory.SLOT_PRIVATE_MESSAGES -> new PlayerSettingsRecord(
                current.playerId(),
                current.pingSoundEnabled(),
                current.pingActionbarEnabled(),
                current.socialSpyEnabled(),
                !current.pmEnabled(),
                current.mentionNotificationsEnabled(),
                current.staffChatEnabled(),
                current.localeTag()
            );
            case SettingsMenuFactory.SLOT_MENTION_NOTIFICATIONS -> new PlayerSettingsRecord(
                current.playerId(),
                current.pingSoundEnabled(),
                current.pingActionbarEnabled(),
                current.socialSpyEnabled(),
                current.pmEnabled(),
                !current.mentionNotificationsEnabled(),
                current.staffChatEnabled(),
                current.localeTag()
            );
            case SettingsMenuFactory.SLOT_STAFF_CHAT -> new PlayerSettingsRecord(
                current.playerId(),
                current.pingSoundEnabled(),
                current.pingActionbarEnabled(),
                current.socialSpyEnabled(),
                current.pmEnabled(),
                current.mentionNotificationsEnabled(),
                !current.staffChatEnabled(),
                current.localeTag()
            );
            case SettingsMenuFactory.SLOT_SOCIAL_SPY -> new PlayerSettingsRecord(
                current.playerId(),
                current.pingSoundEnabled(),
                current.pingActionbarEnabled(),
                !current.socialSpyEnabled(),
                current.pmEnabled(),
                current.mentionNotificationsEnabled(),
                current.staffChatEnabled(),
                current.localeTag()
            );
            default -> null;
        };

        if (updated == null) {
            return;
        }

        playerStateRepository.save(updated);
        player.openInventory(settingsMenuFactory.create(updated));
    }
}
