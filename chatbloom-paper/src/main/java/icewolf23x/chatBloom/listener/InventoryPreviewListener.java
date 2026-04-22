package icewolf23x.chatBloom.listener;

import icewolf23x.chatBloom.ChatBloom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class InventoryPreviewListener implements Listener {

    private final ChatBloom plugin;

    public InventoryPreviewListener(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreviewClick(InventoryClickEvent event) {
        if (plugin.chatItems().isPreviewInventory(event.getView().getTopInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreviewDrag(InventoryDragEvent event) {
        if (plugin.chatItems().isPreviewInventory(event.getView().getTopInventory().getHolder())) {
            event.setCancelled(true);
        }
    }
}
