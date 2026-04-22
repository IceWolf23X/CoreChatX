package me.icewolf23.chatbloom.paper.platform;

import me.icewolf23.chatbloom.common.moderation.ModerationDecision;
import me.icewolf23.chatbloom.common.moderation.ModerationService;
import me.icewolf23.chatbloom.common.moderation.MuteRecord;
import me.icewolf23.chatbloom.common.model.GlobalStateRecord;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import me.icewolf23.chatbloom.common.storage.repository.GlobalStateRepository;
import me.icewolf23.chatbloom.common.storage.repository.MuteRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public final class DefaultModerationService implements ModerationService {

    private final MuteRepository muteRepository;
    private final GlobalStateRepository globalStateRepository;
    private final Clock clock;
    private boolean chatMuted;

    public DefaultModerationService(MuteRepository muteRepository, GlobalStateRepository globalStateRepository, Clock clock) {
        this.muteRepository = muteRepository;
        this.globalStateRepository = globalStateRepository;
        this.clock = clock;
        this.chatMuted = globalStateRepository.load().chatMuted();
    }

    @Override
    public ModerationDecision evaluatePublicMessage(ChatPipelineContext context) {
        return isMuted(context.senderId()) ? ModerationDecision.cancel("moderation.muted-public") : ModerationDecision.allow();
    }

    @Override
    public ModerationDecision evaluatePrivateMessage(ChatPipelineContext context) {
        return isMuted(context.senderId()) ? ModerationDecision.cancel("moderation.muted-private") : ModerationDecision.allow();
    }

    @Override
    public boolean isChatMuted() {
        return chatMuted;
    }

    @Override
    public void setChatMuted(boolean muted) {
        GlobalStateRecord current = globalStateRepository.load();
        this.chatMuted = muted;
        globalStateRepository.save(new GlobalStateRecord(current.firstJoinCount(), muted));
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
