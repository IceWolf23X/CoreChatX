package me.icewolf23.chatbloom.common.pipeline;

public interface ChatPipelineStep {
    void apply(ChatPipelineContext context);
}
