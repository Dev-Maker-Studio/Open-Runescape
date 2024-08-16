package com.jagex.game.runetek5.config.gametip;

/**
 * 
 * @author Brainpower 28/03/2022
 *
 */
public class GameTipStage {

    public final boolean hasUpdates;

    public final GameTipUpdateDetails[] gameTipUpdateDetails;

    public GameTipStage(boolean hasUpdates, GameTipUpdateDetails[] gameTipUpdateDetails) {
        this.hasUpdates = hasUpdates;
        this.gameTipUpdateDetails = gameTipUpdateDetails;
    }
}