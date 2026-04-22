package me.icewolf23.chatbloom.common.pipeline;

import java.util.List;

public final class DefaultChatPipeline implements ChatPipeline {

    private final List<ChatPipelineStep> steps;

    public DefaultChatPipeline(List<ChatPipelineStep> steps) {
        this.steps = List.copyOf(steps);
    }

    @Override
    public ChatPipelineResult execute(ChatPipelineContext context) {
        for (ChatPipelineStep step : steps) {
            step.apply(context);
            if (context.cancelled()) {
                return ChatPipelineResult.cancelled(context.cancelReasonKey(), context);
            }
        }
        return ChatPipelineResult.success(context);
    }
}
