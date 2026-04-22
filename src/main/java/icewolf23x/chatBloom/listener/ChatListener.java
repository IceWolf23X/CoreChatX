package icewolf23x.chatBloom.listener;

import icewolf23x.chatBloom.ChatBloom;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatListener implements Listener {

    private final ChatBloom plugin;

    public ChatListener(ChatBloom plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!plugin.chatService().isPublicChatEnabled()) {
            return;
        }
        event.setCancelled(true);
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.originalMessage());
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                plugin.chatService().handlePublicChat(event.getPlayer(), rawMessage);
            }
        });
    }
}
