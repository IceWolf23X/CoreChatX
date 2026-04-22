package me.icewolf23.chatbloom.paper.pipeline;

import icewolf23x.chatBloom.service.ChatService;
import me.icewolf23.chatbloom.common.pipeline.ChatPipeline;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import org.bukkit.entity.Player;

public final class PaperChatPipelineEntry {

    private final ChatPipeline chatPipeline;
    private final ChatService chatService;

    public PaperChatPipelineEntry(ChatPipeline chatPipeline, ChatService chatService) {
        this.chatPipeline = chatPipeline;
        this.chatService = chatService;
    }

    public void handlePublicChat(Player player, String rawMessage) {
        ChatPipelineContext context = new ChatPipelineContext(player.getUniqueId(), player.getName(), rawMessage);
        var result = chatPipeline.execute(context);
        if (!result.successful()) {
            if (result.cancelReasonKey() != null && !result.cancelReasonKey().isBlank()) {
                player.sendMessage(chatService.plugin().formats().configMessage(result.cancelReasonKey(), player));
            }
            return;
        }
        chatService.handlePublicChat(player, context.rawInput(), context.channelId());
    }
}
