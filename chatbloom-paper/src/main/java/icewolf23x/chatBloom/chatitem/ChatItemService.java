package icewolf23x.chatBloom.chatitem;

import icewolf23x.chatBloom.ChatBloom;
import icewolf23x.chatBloom.model.ChatItemType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ChatItemService {

    private final ChatBloom plugin;
    private final Map<ChatItemType, TokenDefinition> tokens = new EnumMap<>(ChatItemType.class);
    private final Map<String, ChatItemType> aliasLookup = new HashMap<>();

    public ChatItemService(ChatBloom plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        tokens.clear();
        aliasLookup.clear();
        register(ChatItemType.ITEM, "tokens.item.aliases", "tokens.item.permission", "tokens.item.token-format");
        register(ChatItemType.ARMOR, "tokens.armor.aliases", "tokens.armor.permission", "tokens.armor.token-format");
        register(ChatItemType.HOTBAR, "tokens.hotbar.aliases", "tokens.hotbar.permission", "tokens.hotbar.token-format");
        register(ChatItemType.INVENTORY, "tokens.inventory.aliases", "tokens.inventory.permission", "tokens.inventory.token-format");
        register(ChatItemType.ENDERCHEST, "tokens.enderchest.aliases", "tokens.enderchest.permission", "tokens.enderchest.token-format");
    }

    public boolean isToken(String input) {
        return aliasLookup.containsKey(input.toLowerCase(Locale.ROOT));
    }

    public Component createTokenComponent(Player sender, String tokenText) {
        ChatItemType type = aliasLookup.get(tokenText.toLowerCase(Locale.ROOT));
        if (type == null) {
            return Component.text(tokenText);
        }

        TokenDefinition definition = tokens.get(type);
        if (definition == null || !sender.hasPermission(definition.permission())) {
            return Component.text(tokenText);
        }

        Optional<PreparedSnapshot> prepared = prepareSnapshot(sender, type);
        if (prepared.isEmpty()) {
            return Component.text(tokenText);
        }

        PreparedSnapshot snapshot = prepared.get();
        plugin.snapshots().put(snapshot.snapshot());
        Component hover = plugin.formats().configMessage("chatitems.click-to-open", sender);
        Component rendered = plugin.formats().chatItemToken(sender, snapshot.format(), snapshot.visibleLabel());
        return rendered
            .hoverEvent(HoverEvent.showText(hover))
            .clickEvent(ClickEvent.runCommand("/chatbloom item " + snapshot.snapshot().id()));
    }

    public boolean openSnapshot(Player viewer, UUID snapshotId) {
        Optional<ChatItemSnapshot> optionalSnapshot = plugin.snapshots().find(snapshotId);
        if (optionalSnapshot.isEmpty()) {
            viewer.sendMessage(plugin.formats().configMessage("chatitems.expired", viewer));
            return false;
        }
        ChatItemSnapshot snapshot = optionalSnapshot.get();
        PreviewInventoryHolder holder = new PreviewInventoryHolder(snapshot.id());
        String ownerName = Optional.ofNullable(Bukkit.getOfflinePlayer(snapshot.owner()).getName()).orElse("Player");
        Inventory inventory = Bukkit.createInventory(holder, snapshot.size(), plugin.formats().previewTitle(snapshot.type(), viewer, ownerName));
        holder.setInventory(inventory);
        List<ItemStack> contents = snapshot.contents();
        for (int slot = 0; slot < Math.min(inventory.getSize(), contents.size()); slot++) {
            ItemStack stack = contents.get(slot);
            if (stack != null) {
                inventory.setItem(slot, stack.clone());
            }
        }
        viewer.openInventory(inventory);
        return true;
    }

    public boolean isPreviewInventory(InventoryHolder holder) {
        return holder instanceof PreviewInventoryHolder;
    }

    private void register(ChatItemType type, String aliasPath, String permissionPath, String formatPath) {
        var config = plugin.configuration().chatItems();
        List<String> aliases = config.getStringList(aliasPath);
        String permission = config.getString(permissionPath, "chatbloom.chatitem." + type.name().toLowerCase(Locale.ROOT));
        String format = config.getString(formatPath, "<green>{token}</green>");
        TokenDefinition definition = new TokenDefinition(permission, format, Set.copyOf(aliases));
        tokens.put(type, definition);
        for (String alias : aliases) {
            aliasLookup.put(alias.toLowerCase(Locale.ROOT), type);
        }
    }

    private Optional<PreparedSnapshot> prepareSnapshot(Player sender, ChatItemType requestedType) {
        return switch (requestedType) {
            case ITEM -> prepareItemOrShulkerSnapshot(sender);
            case ARMOR -> Optional.of(buildArmorSnapshot(sender));
            case HOTBAR -> Optional.of(buildHotbarSnapshot(sender));
            case INVENTORY -> Optional.of(buildInventorySnapshot(sender));
            case ENDERCHEST -> Optional.of(buildEnderChestSnapshot(sender));
            case SHULKER -> Optional.empty();
        };
    }

    private Optional<PreparedSnapshot> prepareItemOrShulkerSnapshot(Player sender) {
        ItemStack held = sender.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return Optional.empty();
        }

        boolean shulkerCandidate = isShulkerBox(held) && sender.hasPermission("chatbloom.chatitem.shulker");
        if (shulkerCandidate) {
            return Optional.of(buildShulkerSnapshot(sender, held));
        }
        return Optional.of(buildSingleItemSnapshot(sender, held));
    }

    private PreparedSnapshot buildSingleItemSnapshot(Player sender, ItemStack item) {
        List<ItemStack> contents = emptySizedList(27);
        contents.set(13, item.clone());
        ChatItemSnapshot snapshot = new ChatItemSnapshot(
            UUID.randomUUID(),
            sender.getUniqueId(),
            ChatItemType.ITEM,
            "item",
            27,
            contents,
            System.currentTimeMillis()
        );
        return new PreparedSnapshot(snapshot, tokenFormat(ChatItemType.ITEM), "[item]");
    }

    private PreparedSnapshot buildShulkerSnapshot(Player sender, ItemStack item) {
        List<ItemStack> contents = emptySizedList(27);
        if (item.getItemMeta() instanceof BlockStateMeta blockStateMeta) {
            BlockState blockState = blockStateMeta.getBlockState();
            if (blockState instanceof ShulkerBox shulkerBox) {
                ItemStack[] stored = shulkerBox.getInventory().getContents();
                for (int slot = 0; slot < Math.min(27, stored.length); slot++) {
                    ItemStack stack = stored[slot];
                    if (stack != null) {
                        contents.set(slot, stack.clone());
                    }
                }
            }
        }
        ChatItemSnapshot snapshot = new ChatItemSnapshot(
            UUID.randomUUID(),
            sender.getUniqueId(),
            ChatItemType.SHULKER,
            "shulker",
            27,
            contents,
            System.currentTimeMillis()
        );
        String format = plugin.configuration().chatItems().getString("tokens.item.shulker-token-format", "<dark_aqua>[shulker]</dark_aqua>");
        return new PreparedSnapshot(snapshot, format, "[shulker]");
    }

    private PreparedSnapshot buildArmorSnapshot(Player sender) {
        List<ItemStack> contents = emptySizedList(27);
        PlayerInventory inventory = sender.getInventory();
        contents.set(10, cloneOrNull(inventory.getHelmet()));
        contents.set(12, cloneOrNull(inventory.getChestplate()));
        contents.set(14, cloneOrNull(inventory.getLeggings()));
        contents.set(16, cloneOrNull(inventory.getBoots()));
        contents.set(22, cloneOrNull(inventory.getItemInOffHand()));
        ChatItemSnapshot snapshot = new ChatItemSnapshot(
            UUID.randomUUID(),
            sender.getUniqueId(),
            ChatItemType.ARMOR,
            "armor",
            27,
            contents,
            System.currentTimeMillis()
        );
        return new PreparedSnapshot(snapshot, tokenFormat(ChatItemType.ARMOR), "[armor]");
    }

    private PreparedSnapshot buildHotbarSnapshot(Player sender) {
        List<ItemStack> contents = emptySizedList(9);
        ItemStack[] hotbar = sender.getInventory().getContents();
        for (int slot = 0; slot < 9; slot++) {
            contents.set(slot, cloneOrNull(hotbar[slot]));
        }
        ChatItemSnapshot snapshot = new ChatItemSnapshot(
            UUID.randomUUID(),
            sender.getUniqueId(),
            ChatItemType.HOTBAR,
            "hotbar",
            9,
            contents,
            System.currentTimeMillis()
        );
        return new PreparedSnapshot(snapshot, tokenFormat(ChatItemType.HOTBAR), "[hotbar]");
    }

    private PreparedSnapshot buildInventorySnapshot(Player sender) {
        List<ItemStack> contents = emptySizedList(54);
        ItemStack[] rawContents = sender.getInventory().getContents();
        for (int slot = 0; slot < Math.min(36, rawContents.length); slot++) {
            contents.set(slot, cloneOrNull(rawContents[slot]));
        }
        PlayerInventory inventory = sender.getInventory();
        contents.set(45, cloneOrNull(inventory.getHelmet()));
        contents.set(46, cloneOrNull(inventory.getChestplate()));
        contents.set(47, cloneOrNull(inventory.getLeggings()));
        contents.set(48, cloneOrNull(inventory.getBoots()));
        contents.set(49, cloneOrNull(inventory.getItemInOffHand()));
        ChatItemSnapshot snapshot = new ChatItemSnapshot(
            UUID.randomUUID(),
            sender.getUniqueId(),
            ChatItemType.INVENTORY,
            "inventory",
            54,
            contents,
            System.currentTimeMillis()
        );
        return new PreparedSnapshot(snapshot, tokenFormat(ChatItemType.INVENTORY), "[inventory]");
    }

    private PreparedSnapshot buildEnderChestSnapshot(Player sender) {
        ItemStack[] rawContents = sender.getEnderChest().getContents();
        int size = Math.max(9, ((rawContents.length + 8) / 9) * 9);
        List<ItemStack> contents = emptySizedList(size);
        for (int slot = 0; slot < Math.min(size, rawContents.length); slot++) {
            contents.set(slot, cloneOrNull(rawContents[slot]));
        }
        ChatItemSnapshot snapshot = new ChatItemSnapshot(
            UUID.randomUUID(),
            sender.getUniqueId(),
            ChatItemType.ENDERCHEST,
            "enderchest",
            size,
            contents,
            System.currentTimeMillis()
        );
        return new PreparedSnapshot(snapshot, tokenFormat(ChatItemType.ENDERCHEST), "[enderchest]");
    }

    private String tokenFormat(ChatItemType type) {
        TokenDefinition definition = tokens.get(type);
        return definition == null ? "<green>{token}</green>" : definition.format();
    }

    private boolean isShulkerBox(ItemStack item) {
        Material material = item.getType();
        return material.name().endsWith("SHULKER_BOX");
    }

    private List<ItemStack> emptySizedList(int size) {
        List<ItemStack> list = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            list.add(null);
        }
        return list;
    }

    private ItemStack cloneOrNull(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }

    private record TokenDefinition(String permission, String format, Set<String> aliases) {
    }

    private record PreparedSnapshot(ChatItemSnapshot snapshot, String format, String visibleLabel) {
    }
}
