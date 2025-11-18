package com.jagex.game.runetek5.config.gametip;

import java.util.Random;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

/**
 * 
 * @author Brainpower - 28/03/2022
 *
 */
public class GameTipTypeList {

    public final Js5ResourceProvider tipsResource;
    public final GameTipStage[] tipStages;
    public final int tipsFileId;

    public GameTipTypeList(Js5ResourceProvider resourceProvider) {
        this.tipsResource = resourceProvider;
        Packet buffer = new Packet(this.tipsResource.getFile(0, 0));
        int size = buffer.buffer.length >= 1 ? buffer.readUByte() : -1;
        if (size < 4) {
        	this.tipStages = new GameTipStage[0];
            this.tipsFileId = -1;
        } else {
            int length = buffer.readUByte();
            TipRenderDetails[] tipRenderDetails = TipRenderDetails.values();
            boolean hasStages = true;
            if (tipRenderDetails.length == length) {
                for (TipRenderDetails tipRenderDetail : tipRenderDetails) {
                    int transformType = buffer.readUByte();
                    if (transformType != tipRenderDetail.transformType) {
                        hasStages = false;
                        break;
                    }
                }
            } else {
                hasStages = false;
            }

            if (hasStages) {
                int definedStageCount = buffer.readUByte();
                int totalStageCount = buffer.readUByte();
                this.tipsFileId = buffer.readShort();
                int displayDurationMs = buffer.readUMedium();
                int timeBetweenUpdatesMs = buffer.readUShort();

                this.tipStages = new GameTipStage[totalStageCount + 1];

                for (int stageIndex = 0; stageIndex < definedStageCount; stageIndex++) {
                    int stage = buffer.readUByte();
                    boolean hasUpdates = buffer.readUByte() == 1;
                    int updateDetailCount = buffer.readUShort();
                    GameTipUpdateDetails[] updateDetails;
                    if (this.tipsFileId != -1) {
                        updateDetails = new GameTipUpdateDetails[updateDetailCount + 1];
                        updateDetails[0] = new GameTipUpdateDetails(this.tipsFileId, displayDurationMs, timeBetweenUpdatesMs);

                        for (int index = 0; index < updateDetailCount; index++) {
                        	int file = buffer.readUShort();
                        	int duration = buffer.readUMedium();
                        	int delay = buffer.readUShort();
                            updateDetails[index + 1] = new GameTipUpdateDetails(file, duration, delay);
                        }
                    } else {
                        updateDetails = new GameTipUpdateDetails[updateDetailCount];

                        for (int index = 0; index < updateDetailCount; index++) {
                        	int file = buffer.readUShort();
                        	int duration = buffer.readUMedium();
                        	int delay = buffer.readUShort();
                            updateDetails[index] = new GameTipUpdateDetails(file, duration, delay);
                        }

                        this.tipStages[stage] = new GameTipStage(hasUpdates, updateDetails);
                    }

                    this.tipStages[stage] = new GameTipStage(hasUpdates, updateDetails);
                }

                for (int index = 0; index < totalStageCount + 1; index++) {
                    if (this.tipStages[index] == null) {
                    	this.tipStages[index] = decodeDefaultStage(displayDurationMs, timeBetweenUpdatesMs);
                    }
                }
            } else {
            	this.tipStages = new GameTipStage[0];
            	this.tipsFileId = -1;
            }
        }
    }

    public GameTipStage decodeDefaultStage(int displayDurationMs, int timeBetweenUpdatesMs) {
        if (this.tipsFileId == -1) {
            return new GameTipStage(false, new GameTipUpdateDetails[0]);
        } else {
            GameTipUpdateDetails updateDetails = new GameTipUpdateDetails(this.tipsFileId, displayDurationMs, timeBetweenUpdatesMs);
            return new GameTipStage(false, new GameTipUpdateDetails[]{updateDetails});
        }
    }

    public GameTipType list(int fileId) {
        byte[] data = this.tipsResource.getFile(fileId, 0);
        GameTipType tipsType = new GameTipType();
        tipsType.decode(new Packet(data));
        return tipsType;
    }

    public GameTipUpdateDetails[] getUpdateDetails(int startStagePreference) {
        if (startStagePreference >= 0 && startStagePreference < this.tipStages.length) {
            GameTipStage stage = this.tipStages[startStagePreference];
            if (stage.hasUpdates && stage.gameTipUpdateDetails.length > 1) {
                int start = this.tipsFileId == -1 ? 0 : 1;
                Random random = new Random();
                GameTipUpdateDetails[] updateDetailsCache = new GameTipUpdateDetails[stage.gameTipUpdateDetails.length];
                System.arraycopy(stage.gameTipUpdateDetails, 0, updateDetailsCache, 0, updateDetailsCache.length);
                for (int src = start; src < updateDetailsCache.length; src++) {
                    int dst = StaticMethods.method7931(random, updateDetailsCache.length - start) + start;
                    GameTipUpdateDetails updateDetails = stage.gameTipUpdateDetails[src];
                    updateDetailsCache[src] = updateDetailsCache[dst];
                    updateDetailsCache[dst] = updateDetails;
                }
                return updateDetailsCache;
            } 
            return stage.gameTipUpdateDetails;
        } 
        return decodeDefaultStage(0, 0).gameTipUpdateDetails;
    }

    public boolean hasTipsFile() {
        return this.tipsFileId != -1;
    }
}