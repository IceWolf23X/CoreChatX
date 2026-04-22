package icewolf23x.chatBloom.chatitem;

import icewolf23x.chatBloom.model.ChatItemType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public record ChatItemSnapshot(
    UUID id,
    UUID owner,
    ChatItemType type,
    String title,
    int size,
    List<ItemStack> contents,
    long createdAt
) {
}
