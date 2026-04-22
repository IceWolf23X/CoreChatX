package me.icewolf23.chatbloom.paper.hook;

import me.icewolf23.chatbloom.paper.ChatBloom;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class HookService {

    private final ChatBloom plugin;
    private boolean placeholderApiEnabled;
    private LuckPerms luckPerms;

    public HookService(ChatBloom plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        boolean papiAllowed = plugin.configuration().main().getBoolean("hooks.placeholderapi", true);
        this.placeholderApiEnabled = papiAllowed && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        boolean luckPermsAllowed = plugin.configuration().main().getBoolean("hooks.luckperms", true);
        this.luckPerms = null;
        if (luckPermsAllowed && Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                this.luckPerms = LuckPermsProvider.get();
            } catch (IllegalStateException ignored) {
                this.luckPerms = null;
            }
        }
    }

    public String applyPlaceholders(Player player, String input) {
        if (!placeholderApiEnabled || player == null || input == null || input.isEmpty()) {
            return input;
        }
        return PlaceholderAPI.setPlaceholders(player, input);
    }

    public Component groupPrefix(Player player) {
        if (luckPerms == null) {
            return Component.empty();
        }
        var user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
        if (user == null) {
            return Component.empty();
        }
        CachedMetaData metaData = user.getCachedData().getMetaData();
        String prefix = metaData.getPrefix();
        if (prefix == null || prefix.isBlank()) {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + " ");
    }

    public boolean hasPlaceholderApi() {
        return placeholderApiEnabled;
    }

    public boolean hasLuckPerms() {
        return luckPerms != null;
    }
}
