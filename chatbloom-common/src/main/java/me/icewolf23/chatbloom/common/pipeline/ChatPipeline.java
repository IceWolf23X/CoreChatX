package me.icewolf23.chatbloom.common.pipeline;

public interface ChatPipeline {
    ChatPipelineResult execute(ChatPipelineContext context);
}
