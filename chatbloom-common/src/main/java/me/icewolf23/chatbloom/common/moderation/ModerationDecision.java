package me.icewolf23.chatbloom.common.moderation;

public record ModerationDecision(
    boolean allowed,
    String messageKey
) {
    public static ModerationDecision allow() {
        return new ModerationDecision(true, null);
    }

    public static ModerationDecision cancel(String messageKey) {
        return new ModerationDecision(false, messageKey);
    }
}
