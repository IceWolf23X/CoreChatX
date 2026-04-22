package me.icewolf23.chatbloom.common.pipeline;

import java.util.UUID;
import net.kyori.adventure.text.Component;

public final class ChatPipelineContext {

    private final UUID senderId;
    private final String senderName;
    private final String rawInput;
    private String sanitizedInput;
    private Component renderedMessage;
    private boolean cancelled;
    private String cancelReasonKey;

    public ChatPipelineContext(UUID senderId, String senderName, String rawInput) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.rawInput = rawInput;
        this.sanitizedInput = rawInput;
    }

    public UUID senderId() {
        return senderId;
    }

    public String senderName() {
        return senderName;
    }

    public String rawInput() {
        return rawInput;
    }

    public String sanitizedInput() {
        return sanitizedInput;
    }

    public void sanitizedInput(String sanitizedInput) {
        this.sanitizedInput = sanitizedInput;
    }

    public Component renderedMessage() {
        return renderedMessage;
    }

    public void renderedMessage(Component renderedMessage) {
        this.renderedMessage = renderedMessage;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public void cancel(String cancelReasonKey) {
        this.cancelled = true;
        this.cancelReasonKey = cancelReasonKey;
    }

    public String cancelReasonKey() {
        return cancelReasonKey;
    }
}
