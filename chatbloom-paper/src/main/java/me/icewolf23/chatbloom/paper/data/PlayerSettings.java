package me.icewolf23.chatbloom.paper.data;

public final class PlayerSettings {

    private boolean pingSoundEnabled = true;
    private boolean pingActionbarEnabled = true;
    private boolean socialSpyEnabled = false;
    private boolean pmEnabled = true;
    private boolean mentionNotificationsEnabled = true;
    private boolean staffChatEnabled = true;
    private String localeTag = "en_us";

    public boolean isPingSoundEnabled() {
        return pingSoundEnabled;
    }

    public void setPingSoundEnabled(boolean pingSoundEnabled) {
        this.pingSoundEnabled = pingSoundEnabled;
    }

    public boolean isPingActionbarEnabled() {
        return pingActionbarEnabled;
    }

    public void setPingActionbarEnabled(boolean pingActionbarEnabled) {
        this.pingActionbarEnabled = pingActionbarEnabled;
    }

    public boolean isSocialSpyEnabled() {
        return socialSpyEnabled;
    }

    public void setSocialSpyEnabled(boolean socialSpyEnabled) {
        this.socialSpyEnabled = socialSpyEnabled;
    }

    public boolean isPmEnabled() {
        return pmEnabled;
    }

    public void setPmEnabled(boolean pmEnabled) {
        this.pmEnabled = pmEnabled;
    }

    public boolean isMentionNotificationsEnabled() {
        return mentionNotificationsEnabled;
    }

    public void setMentionNotificationsEnabled(boolean mentionNotificationsEnabled) {
        this.mentionNotificationsEnabled = mentionNotificationsEnabled;
    }

    public boolean isStaffChatEnabled() {
        return staffChatEnabled;
    }

    public void setStaffChatEnabled(boolean staffChatEnabled) {
        this.staffChatEnabled = staffChatEnabled;
    }

    public String getLocaleTag() {
        return localeTag;
    }

    public void setLocaleTag(String localeTag) {
        this.localeTag = localeTag;
    }
}
