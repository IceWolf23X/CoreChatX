package me.icewolf23.chatbloom.common.pipeline;

public record ChatPipelineResult(
    boolean successful,
    String cancelReasonKey,
    ChatPipelineContext context
) {
    public static ChatPipelineResult success(ChatPipelineContext context) {
        return new ChatPipelineResult(true, null, context);
    }

    public static ChatPipelineResult cancelled(String cancelReasonKey, ChatPipelineContext context) {
        return new ChatPipelineResult(false, cancelReasonKey, context);
    }
}
