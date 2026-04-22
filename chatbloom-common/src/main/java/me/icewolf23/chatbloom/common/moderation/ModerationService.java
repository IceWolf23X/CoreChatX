package me.icewolf23.chatbloom.common.moderation;

import java.util.UUID;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;

public interface ModerationService {
    ModerationDecision evaluatePublicMessage(ChatPipelineContext context);

    ModerationDecision evaluatePrivateMessage(ChatPipelineContext context);

    boolean isChatMuted();

    void setChatMuted(boolean muted);

    boolean isMuted(UUID playerId);

    void mute(UUID playerId, Long expiresAtMillis, String reason, UUID actorId, boolean blocksPrivateMessages);

    void unmute(UUID playerId);
}
