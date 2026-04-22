package me.icewolf23.chatbloom.paper.platform;

import me.icewolf23.chatbloom.common.moderation.ModerationDecision;
import me.icewolf23.chatbloom.common.moderation.ModerationService;
import me.icewolf23.chatbloom.common.moderation.MuteRecord;
import me.icewolf23.chatbloom.common.model.GlobalStateRecord;
import me.icewolf23.chatbloom.common.pipeline.ChatPipelineContext;
import me.icewolf23.chatbloom.common.storage.repository.GlobalStateRepository;
import me.icewolf23.chatbloom.common.storage.repository.MuteRepository;

import java.time.Clock;
import java.util.UUID;

public final class DefaultModerationService implements ModerationService {

    private final MuteRepository muteRepository;
    private final GlobalStateRepository globalStateRepository;
    private final Clock clock;
    private final boolean muteEnabled;
    private final boolean muteChatEnabled;
    private boolean chatMuted;

    public DefaultModerationService(
        MuteRepository muteRepository,
        GlobalStateRepository globalStateRepository,
        Clock clock,
        boolean muteEnabled,
        boolean muteChatEnabled
    ) {
        this.muteRepository = muteRepository;
        this.globalStateRepository = globalStateRepository;
        this.clock = clock;
        this.muteEnabled = muteEnabled;
        this.muteChatEnabled = muteChatEnabled;
        this.chatMuted = globalStateRepository.load().chatMuted();
    }

    @Override
    public ModerationDecision evaluatePublicMessage(ChatPipelineContext context) {
        if (!muteEnabled) {
            return ModerationDecision.allow();
        }
        return activeMute(context.senderId()).isPresent()
            ? ModerationDecision.cancel("moderation.muted-public")
            : ModerationDecision.allow();
    }

    @Override
    public ModerationDecision evaluatePrivateMessage(ChatPipelineContext context) {
        if (!muteEnabled) {
            return ModerationDecision.allow();
        }
        return activeMute(context.senderId())
            .filter(MuteRecord::blocksPrivateMessages)
            .map(ignored -> ModerationDecision.cancel("moderation.muted-private"))
            .orElseGet(ModerationDecision::allow);
    }

    @Override
    public boolean isChatMuted() {
        return muteChatEnabled && chatMuted;
    }

    @Override
    public void setChatMuted(boolean muted) {
        if (!muteChatEnabled) {
            return;
        }
        GlobalStateRecord current = globalStateRepository.load();
        this.chatMuted = muted;
        globalStateRepository.save(new GlobalStateRecord(current.firstJoinCount(), muted));
    }

    @Override
    public boolean isMuted(UUID playerId) {
        if (!muteEnabled) {
            return false;
        }
        return activeMute(playerId).isPresent();
    }

    @Override
    public void mute(UUID playerId, Long expiresAtMillis, String reason, UUID actorId, boolean blocksPrivateMessages) {
        if (!muteEnabled) {
            return;
        }
        muteRepository.saveMute(new MuteRecord(
            playerId,
            clock.millis(),
            expiresAtMillis,
            reason,
            actorId,
            blocksPrivateMessages
        ));
    }

    @Override
    public void unmute(UUID playerId) {
        if (!muteEnabled) {
            return;
        }
        muteRepository.clearMute(playerId);
    }

    private java.util.Optional<MuteRecord> activeMute(UUID playerId) {
        return muteRepository.findActiveMute(playerId, clock.millis());
    }
}
