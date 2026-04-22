package me.icewolf23.chatbloom.paper.pipeline;

import java.util.List;
import me.icewolf23.chatbloom.common.pipeline.ChatPipeline;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineResult;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineStep;
import me.icewolf23.chatbloom.common.pipeline.DefaultChatPipeline;

public final class LegacyPaperChatPipeline implements ChatPipeline {

    private final DefaultChatPipeline delegate;

    public LegacyPaperChatPipeline(List<ChatPipelineStep> steps) {
        this.delegate = new DefaultChatPipeline(steps);
    }

    @Override
    public ChatPipelineResult execute(ChatPipelineContext context) {
        return delegate.execute(context);
    }
}
