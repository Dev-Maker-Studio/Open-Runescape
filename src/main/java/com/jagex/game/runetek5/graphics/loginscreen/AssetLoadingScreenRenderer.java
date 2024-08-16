package com.jagex.game.runetek5.graphics.loginscreen;

import com.jagex.game.runetek5.client.ClientStartup;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.LoadingStage;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.FrameRenderException;
import com.jagex.game.runetek5.tools.TimeTools;
import com.jagex.game.runetek5.utils.Utils;

public class AssetLoadingScreenRenderer implements Runnable {

    public boolean clear;
    public long loadingStageChangedTime;
    public String loadingText;
    public int loadingPercentage;
    public LoadingStage loadingStage;
    public int cycles;
    public LoginScreenComponent backComponent;
    public LoginScreenComponent frontComponent = new LegacyAssetLoadingScreen();
    public volatile boolean finished;
    public long lastComponentUpdate;

    public int getCycle() {
        return this.cycles;
    }

    public synchronized void requestFillrect() {
        this.clear = true;
    }

    public synchronized void initialize(long loadingStageChangedTime, String loadingText, int loadingPercentage, LoadingStage loadingStage) {
        this.loadingStageChangedTime = loadingStageChangedTime;
        this.loadingText = loadingText;
        this.loadingPercentage = loadingPercentage;
        this.loadingStage = loadingStage;
    }

    public int getLoadingStageCompletion() {
        if (this.loadingStage == null) {
            return 0;
        } 
        int stageNumber = this.loadingStage.number;
        if (this.loadingStage.aBool4718 && this.loadingPercentage < this.loadingStage.secondaryCompletionDisplay) {
            return this.loadingPercentage + 1;
        }
        if (stageNumber < 0 || stageNumber >= LoadingStage.LOADING_STAGES.length - 1) {
            return 100;
        }
        if (this.loadingStage.primaryCompletionDisplay != this.loadingPercentage) {
            return this.loadingStage.primaryCompletionDisplay;
        }
        return this.loadingStage.secondaryCompletionDisplay;
    }

    public int getLoadingPercentage() {
        return this.loadingPercentage;
    }

    public String getLoadingText() {
        return this.loadingText;
    }

    public LoadingStage getLoadingStage() {
        return this.loadingStage;
    }

    @Override
    public void run() {
        while (!this.finished) {
            long cycleStart = Utils.time();
            synchronized (this) {
                try {
                    ++this.cycles;
                    if (this.frontComponent instanceof LegacyAssetLoadingScreen) {
                        this.frontComponent.draw();
                    } else {
                        long currentTime = Utils.time();
                        if (Renderers.activeToolkit != null && this.backComponent != null && this.backComponent.timeBetweenUpdatesMs() != 0 && this.lastComponentUpdate >= currentTime - this.backComponent.timeBetweenUpdatesMs()) {
                            int dstAlpha = (int) (255L * (currentTime - this.lastComponentUpdate) / this.backComponent.timeBetweenUpdatesMs());
                            int srcAlpha = 255 - dstAlpha;
                            int dstColor = dstAlpha << 24 | 0xffffff;
                            int srcColor = srcAlpha << 24 | 0xffffff;
                            WindowMode.fillWindowRect();
                            Renderers.activeToolkit.applyClearFlags(1, 0);
                            NativeSprite sprite = Renderers.activeToolkit.generateEmptySprite(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379);
                            DirectDisplay display = Renderers.activeToolkit.getImageProducer();
                            display.setFramebuffer(0, sprite.getFramebuffer());
                            Renderers.activeToolkit.setDisplay(display);
                            this.backComponent.draw();
                            Renderers.activeToolkit.resizeDisplay(display);
                            sprite.draw(0, 0, 0, srcColor, 1);
                            Renderers.activeToolkit.setDisplay(display);
                            Renderers.activeToolkit.applyClearFlags(1, 0);
                            this.frontComponent.draw();
                            Renderers.activeToolkit.resizeDisplay(display);
                            sprite.draw(0, 0, 0, dstColor, 1);
                        } else {
                            if (this.backComponent != null) {
                                this.clear = true;
                                this.backComponent.cleanup();
                                this.backComponent = null;
                            }
                            if (this.clear) {
                                WindowMode.fillWindowRect();
                                if (Renderers.activeToolkit != null) {
                                    Renderers.activeToolkit.applyClearFlags(1, 0);
                                }
                            }
                            this.frontComponent.draw();
                        }
                        try {
                        	/** Legacy loading screen gets replaced by background + top screen load bar **/
                            if (Renderers.activeToolkit != null && !(this.frontComponent instanceof LegacyAssetLoadingScreen)) {
                                Renderers.activeToolkit.renderFrame();
                            }
                        } catch (FrameRenderException renderException) {
                            PacketEncoder.sendErrorToServer(renderException.getMessage() + ClientStartup.activeClient.getDebugString(), renderException);
                            client.loadAssets(0, true);
                        }
                    }
                    this.clear = false;
                    if (Renderers.activeToolkit != null && !(this.frontComponent instanceof LegacyAssetLoadingScreen) && this.loadingStage.number < LoadingStage.INITIALIZE_PLAYER.number) {
                        WindowMode.adjustActiveWindow();
                    }
                } catch (Exception exception) {
                    continue;
                }
            }
            long cycleEnd = Utils.time();
            int sleepDuration = (int) (20L - (cycleEnd - cycleStart));
            if (sleepDuration > 0) {
                TimeTools.sleep(sleepDuration);
            }
        }
    }

    public void stop() {
        this.finished = true;
    }

    public long loadingStageChangedTime() {
        return this.loadingStageChangedTime;
    }

    public synchronized void setDisplayedGametip(LoginScreenComponent gametip) {
        this.backComponent = this.frontComponent;
        this.frontComponent = gametip;
        this.lastComponentUpdate = Utils.time();
    }

    public synchronized boolean replaceComponent() {
        return this.frontComponent.finished(this.lastComponentUpdate);
    }
}
