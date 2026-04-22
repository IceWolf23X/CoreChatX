package icewolf23x.chatBloom.listener;

import icewolf23x.chatBloom.ChatBloom;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConnectionListener implements Listener {

    private final ChatBloom plugin;

    public ConnectionListener(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        Player player = event.getPlayer();
        plugin.playerData().get(player.getUniqueId());

        boolean firstJoin = !player.hasPlayedBefore();
        if (firstJoin && plugin.configuration().main().getBoolean("first-join.enabled", true)) {
            int count = plugin.configuration().main().getBoolean("first-join.counter-enabled", true)
                ? plugin.globalState().incrementFirstJoinCount()
                : plugin.globalState().getFirstJoinCount();
            broadcast(plugin.formats().firstJoin(player, count));
            return;
        }
        if (plugin.configuration().main().getBoolean("join-quit.join-enabled", true)) {
            broadcast(plugin.formats().join(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        if (plugin.configuration().main().getBoolean("join-quit.quit-enabled", true)) {
            broadcast(plugin.formats().quit(event.getPlayer()));
        }
    }

    private void broadcast(Component component) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(component));
        Bukkit.getConsoleSender().sendMessage(component);
    }
}
