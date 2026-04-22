package me.icewolf23.chatbloom.paper.chatitem;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class PreviewInventoryHolder implements InventoryHolder {

    private final UUID snapshotId;
    private Inventory inventory;

    public PreviewInventoryHolder(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public UUID snapshotId() {
        return snapshotId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
