package icewolf23x.chatBloom.data;

public final class PlayerSettings {

    private boolean pingSoundEnabled = true;
    private boolean pingActionbarEnabled = true;
    private boolean socialSpyEnabled = false;
    private boolean pmEnabled = true;

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
}
