package com.jagex.game.runetek5.graphics.loginscreen;

import com.jagex.game.runetek5.config.gametip.GameTipType;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.Utils;

public class DrawableGameTip implements LoginScreenComponent {

    public final GameTipType tipsType;

    public final int displayDurationMs;

    public final int timeBetweenUpdatesMs;

    public final DrawableItem[] drawableItems;

    public AbstractToolkit toolkit;

    public boolean dirty;

    public DrawableGameTip(GameTipType tipsType, int displayDurationMs, int timeBetweenUpdatesMs, GameTipsLoader gameTipsLoader) {
        this.tipsType = tipsType;
        this.displayDurationMs = displayDurationMs;
        this.timeBetweenUpdatesMs = timeBetweenUpdatesMs;
        this.drawableItems = new DrawableItem[this.tipsType.tipDetailsCache.length];
        for (int index = 0; index < this.drawableItems.length; index++) {
        	this.drawableItems[index] = gameTipsLoader.fromDetails(this.tipsType.tipDetailsCache[index]);
        }
    }

    @Override
    public void initialize() {
        if (this.toolkit != Renderers.activeToolkit) {
            this.toolkit = Renderers.activeToolkit;
            this.dirty = true;
        }
        this.toolkit.applyClearFlags(3, 0);
        DrawableItem[] items = this.drawableItems;
        for (int index = 0; index < items.length; index++) {
            DrawableItem item = items[index];
            if (item != null) {
                item.initialize();
            }
        }
    }

    @Override
    public void draw() {
        DrawableItem[] items = this.drawableItems;
        for (int index = 0; index < items.length; index++) {
            DrawableItem item = items[index];
            if (item != null) {
                item.draw(true);
            }
        }
        this.dirty = false;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public boolean finished(long initTime) {
        return Utils.time() >= initTime + this.displayDurationMs;
    }

    @Override
    public int timeBetweenUpdatesMs() {
        return this.timeBetweenUpdatesMs;
    }

    @Override
    public int calculateLoadingPercent() {
        int cachedEntries = 0;
        DrawableItem[] items = this.drawableItems;
        for (int index = 0; index < items.length; index++) {
            DrawableItem item = items[index];
            if (item == null || item.isCached()) {
                ++cachedEntries;
            }
        }
        return cachedEntries * 100 / this.drawableItems.length;
    }
}
