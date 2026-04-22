package me.icewolf23.chatbloom.paper.pipeline;

import me.icewolf23.chatbloom.common.pipeline.ChatPipeline;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineResult;

public final class LegacyPaperChatPipeline implements ChatPipeline {

    @Override
    public ChatPipelineResult execute(ChatPipelineContext context) {
        return ChatPipelineResult.success(context);
    }
}
