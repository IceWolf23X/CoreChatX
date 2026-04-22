package me.icewolf23.chatbloom.paper.pipeline;

import me.icewolf23.chatbloom.common.moderation.ModerationService;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineStep;

public final class PublicModerationStep implements ChatPipelineStep {

    private final ModerationService moderationService;

    public PublicModerationStep(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @Override
    public void apply(ChatPipelineContext context) {
        var decision = moderationService.evaluatePublicMessage(context);
        if (!decision.allowed()) {
            context.cancel(decision.messageKey());
        }
    }
}
