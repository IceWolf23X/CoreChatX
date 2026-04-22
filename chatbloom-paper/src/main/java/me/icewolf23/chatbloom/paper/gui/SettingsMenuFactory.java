package me.icewolf23.chatbloom.paper.gui;

import me.icewolf23.chatbloom.common.model.PlayerSettingsRecord;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class SettingsMenuFactory {

    public static final Component TITLE = Component.text("ChatBloom Settings");
    private static final int SIZE = 27;
    public static final int SLOT_PING_SOUND = 10;
    public static final int SLOT_PING_ACTIONBAR = 11;
    public static final int SLOT_PRIVATE_MESSAGES = 12;
    public static final int SLOT_MENTION_NOTIFICATIONS = 13;
    public static final int SLOT_STAFF_CHAT = 14;
    public static final int SLOT_SOCIAL_SPY = 15;

    public Inventory create(PlayerSettingsRecord state) {
        Inventory inventory = Bukkit.createInventory(null, SIZE, TITLE);
        inventory.setItem(SLOT_PING_SOUND, toggleItem(Material.NOTE_BLOCK, "Ping Sound", state.pingSoundEnabled()));
        inventory.setItem(SLOT_PING_ACTIONBAR, toggleItem(Material.PAPER, "Ping Actionbar", state.pingActionbarEnabled()));
        inventory.setItem(SLOT_PRIVATE_MESSAGES, toggleItem(Material.WRITABLE_BOOK, "Private Messages", state.pmEnabled()));
        inventory.setItem(SLOT_MENTION_NOTIFICATIONS, toggleItem(Material.BELL, "Mention Notifications", state.mentionNotificationsEnabled()));
        inventory.setItem(SLOT_STAFF_CHAT, toggleItem(Material.GOLDEN_HELMET, "Staff Chat", state.staffChatEnabled()));
        inventory.setItem(SLOT_SOCIAL_SPY, toggleItem(Material.ENDER_EYE, "Social Spy", state.socialSpyEnabled()));
        return inventory;
    }

    public boolean isSettingsInventory(Component title) {
        return TITLE.equals(title);
    }

    private ItemStack toggleItem(Material material, String label, boolean enabled) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(label + ": " + (enabled ? "Enabled" : "Disabled")));
        meta.lore(List.of(Component.text(enabled ? "Click to disable." : "Click to enable.")));
        item.setItemMeta(meta);
        return item;
    }
}
