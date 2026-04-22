package me.icewolf23.chatbloom.paper.platform;

import me.icewolf23.chatbloom.common.moderation.ModerationDecision;
import me.icewolf23.chatbloom.common.moderation.ModerationService;
import me.icewolf23.chatbloom.common.moderation.MuteRecord;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import me.icewolf23.chatbloom.common.storage.repository.MuteRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class DefaultModerationService implements ModerationService {

    private final MuteRepository muteRepository;
    private final Clock clock;

    public DefaultModerationService(MuteRepository muteRepository, Clock clock) {
        this.muteRepository = muteRepository;
        this.clock = clock;
    }

    @Override
    public ModerationDecision evaluatePublicMessage(ChatPipelineContext context) {
        return isMuted(context.senderId()) ? ModerationDecision.cancel("messages.muted") : ModerationDecision.allow();
    }

    @Override
    public ModerationDecision evaluatePrivateMessage(ChatPipelineContext context) {
        return isMuted(context.senderId()) ? ModerationDecision.cancel("messages.muted") : ModerationDecision.allow();
    }

    @Override
    public boolean isMuted(UUID playerId) {
        return muteRepository.findActiveMute(playerId, Instant.now(clock)).isPresent();
    }

    @Override
    public void mute(UUID playerId, Instant until, String reason, UUID actorId) {
        muteRepository.saveMute(new MuteRecord(playerId, until, reason, actorId));
    }

    @Override
    public void unmute(UUID playerId) {
        muteRepository.clearMute(playerId);
    }
}
