package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.client.LoadingStage;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.gametip.component.AnchoredElement;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.utils.Utils;

public abstract class LBC implements DrawableItem {

    public AbstractFontRenderer fontRenderer;

    public int lastLoadPercent;

    public long lastLoadPercentChange;

    public final Js5ResourceProvider spriteResource;

    public final Js5ResourceProvider fontResource;

    public final AnchoredElement anchoredElement;

    public LBC(Js5ResourceProvider spriteResource, Js5ResourceProvider fontResource, AnchoredElement anchoredElement) {
        this.spriteResource = spriteResource;
        this.fontResource = fontResource;
        this.anchoredElement = anchoredElement;
    }

    @Override
    public void initialize() {
        FontMetrics fontmetrics = FontMetrics.list(this.fontResource, this.anchoredElement.fileId);
        this.fontRenderer = Renderers.activeToolkit.createFontRenderer(fontmetrics, SpriteType.listGroup(this.spriteResource, this.anchoredElement.fileId), true);
    }

    @Override
    public boolean isCached() {
        boolean cached = this.spriteResource.isFileCached(this.anchoredElement.fileId);
        if (!this.fontResource.isFileCached(this.anchoredElement.fileId)) {
            cached = false;
        }
        return cached;
    }

    public abstract void drawBackground(int var2, int var3);

    public int loadPercentDecimalFullout() {
        int loadPercentage = LoadingStage.plainLoginScreenRenderer.getLoadingPercentage();
        int loadPercentDecimalFullout = loadPercentage * 100;
        if (loadPercentage == this.lastLoadPercent && loadPercentage != 0) {
            int loadingStageCompletion = LoadingStage.plainLoginScreenRenderer.getLoadingStageCompletion();
            if (loadingStageCompletion > loadPercentage) {
                long timeDelta = this.lastLoadPercentChange - LoadingStage.plainLoginScreenRenderer.loadingStageChangedTime();
                if (timeDelta > 0L) {
                    long long_7 = 10000L * timeDelta / loadPercentage * (loadingStageCompletion - loadPercentage);
                    long long_9 = (Utils.time() - this.lastLoadPercentChange) * 10000L;
                    if (long_9 < long_7) {
                        loadPercentDecimalFullout = (int) (100L * long_9 * (loadingStageCompletion - loadPercentage) / long_7 + (loadPercentage * 100));
                    } else {
                        loadPercentDecimalFullout = loadingStageCompletion * 100;
                    }
                }
            }
        } else {
            this.lastLoadPercent = loadPercentage;
            this.lastLoadPercentChange = Utils.time();
        }
        return loadPercentDecimalFullout;
    }

    @Override
    public void draw(boolean draw) {
        int x = this.anchoredElement.horizontalAnchor.calculateDrawX(this.anchoredElement.width, client.GAME_WIDTH) + this.anchoredElement.offsetX;
        int y = this.anchoredElement.verticalAnchor.calculateDrawY(this.anchoredElement.height, client.GAME_HEIGHT) + this.anchoredElement.offsetY;
        drawOutline(draw, x, y);
        drawBackground(x, y);
        String loadingText = LoadingStage.plainLoginScreenRenderer.getLoadingText();
        if (Utils.time() - this.lastLoadPercentChange > 10000L) {
            loadingText = loadingText + " (" + LoadingStage.plainLoginScreenRenderer.getLoadingStage().number + ")";
        }
        this.fontRenderer.renderCentered(loadingText, this.anchoredElement.width / 2 + x, this.anchoredElement.height / 2 + this.anchoredElement.anInt4092 + y + 4, this.anchoredElement.anInt4091, -1);
    }

    public abstract void drawOutline(boolean var1, int x, int y);
}
