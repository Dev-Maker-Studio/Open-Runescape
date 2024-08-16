package com.jagex.game.runetek5.config.gametip;

public class GameTipUpdateDetails {

    public final int tipFileId;
    public final int displayDurationMs;
    public final int timeBetweenUpdatesMs;

    public GameTipUpdateDetails(int tipsFileId, int displayDurationMs, int timeBetweenUpdatesMs) {
        this.tipFileId = tipsFileId;
        this.displayDurationMs = displayDurationMs;
        this.timeBetweenUpdatesMs = timeBetweenUpdatesMs;
    }

}
