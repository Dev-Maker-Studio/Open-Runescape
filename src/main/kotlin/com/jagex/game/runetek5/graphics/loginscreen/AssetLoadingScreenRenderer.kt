package com.jagex.game.runetek5.graphics.loginscreen

import com.jagex.game.runetek5.client.ClientStartup
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.LoadingStage
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.FrameRenderException
import com.jagex.game.runetek5.tools.TimeTools
import com.jagex.game.runetek5.utils.Utils

/**
 * Loading screen renderer that runs on a background thread to display loading progress.
 */
class AssetLoadingScreenRenderer : Runnable {

    @JvmField var clear: Boolean = false
    @JvmField var loadingStageChangedTime: Long = 0
    @JvmField var loadingText: String? = null
    @JvmField var loadingPercentage: Int = 0
    @JvmField var loadingStage: LoadingStage? = null
    @JvmField var cycles: Int = 0
    @JvmField var backComponent: LoginScreenComponent? = null
    @JvmField var frontComponent: LoginScreenComponent = LegacyAssetLoadingScreen()
    @Volatile var finished: Boolean = false
    @JvmField var lastComponentUpdate: Long = 0

    fun getCycle(): Int = cycles

    @Synchronized
    fun requestFillrect() {
        clear = true
    }

    @Synchronized
    fun initialize(loadingStageChangedTime: Long, loadingText: String?, loadingPercentage: Int, loadingStage: LoadingStage?) {
        this.loadingStageChangedTime = loadingStageChangedTime
        this.loadingText = loadingText
        this.loadingPercentage = loadingPercentage
        this.loadingStage = loadingStage
    }

    fun getLoadingStageCompletion(): Int {
        val stage = loadingStage ?: return 0

        val stageNumber = stage.number
        if (stage.aBool4718 && loadingPercentage < stage.secondaryCompletionDisplay) {
            return loadingPercentage + 1
        }
        if (stageNumber < 0 || stageNumber >= LoadingStage.LOADING_STAGES!!.size - 1) {
            return 100
        }
        if (stage.primaryCompletionDisplay != loadingPercentage) {
            return stage.primaryCompletionDisplay
        }
        return stage.secondaryCompletionDisplay
    }

    fun getLoadingPercentage(): Int = loadingPercentage

    fun getLoadingText(): String? = loadingText

    fun getLoadingStage(): LoadingStage? = loadingStage

    override fun run() {
        while (!finished) {
            val cycleStart = Utils.time()
            synchronized(this) {
                try {
                    ++cycles
                    if (frontComponent is LegacyAssetLoadingScreen) {
                        frontComponent.draw()
                    } else {
                        val currentTime = Utils.time()
                        val back = backComponent
                        if (Renderers.activeToolkit != null &&
                            back != null &&
                            back.timeBetweenUpdatesMs() != 0 &&
                            lastComponentUpdate >= currentTime - back.timeBetweenUpdatesMs()) {

                            val dstAlpha = (255L * (currentTime - lastComponentUpdate) / back.timeBetweenUpdatesMs()).toInt()
                            val srcAlpha = 255 - dstAlpha
                            val dstColor = (dstAlpha shl 24) or 0xffffff
                            val srcColor = (srcAlpha shl 24) or 0xffffff

                            WindowMode.fillWindowRect()
                            Renderers.activeToolkit!!.applyClearFlags(1, 0)
                            val sprite = Renderers.activeToolkit!!.generateEmptySprite(
                                GameShell.screenCanvasWidth,
                                GameShell.screenCanvasHeight * -969250379
                            )
                            val display = Renderers.activeToolkit!!.getImageProducer()
                            display.setFramebuffer(0, sprite.getFramebuffer())
                            Renderers.activeToolkit!!.setDisplay(display)
                            back.draw()
                            Renderers.activeToolkit!!.resizeDisplay(display)
                            sprite.draw(0, 0, 0, srcColor, 1)
                            Renderers.activeToolkit!!.setDisplay(display)
                            Renderers.activeToolkit!!.applyClearFlags(1, 0)
                            frontComponent.draw()
                            Renderers.activeToolkit!!.resizeDisplay(display)
                            sprite.draw(0, 0, 0, dstColor, 1)
                        } else {
                            if (back != null) {
                                clear = true
                                back.cleanup()
                                backComponent = null
                            }
                            if (clear) {
                                WindowMode.fillWindowRect()
                                Renderers.activeToolkit?.applyClearFlags(1, 0)
                            }
                            frontComponent.draw()
                        }

                        try {
                            // Legacy loading screen gets replaced by background + top screen load bar
                            if (Renderers.activeToolkit != null && frontComponent !is LegacyAssetLoadingScreen) {
                                Renderers.activeToolkit!!.renderFrame()
                            }
                        } catch (renderException: FrameRenderException) {
                            PacketEncoder.sendErrorToServer(
                                renderException.message + ClientStartup.activeClient!!.getDebugString(),
                                renderException
                            )
                            Client.loadAssets(0, true)
                        }
                    }
                    clear = false
                    if (Renderers.activeToolkit != null &&
                        frontComponent !is LegacyAssetLoadingScreen &&
                        loadingStage!!.number < LoadingStage.INITIALIZE_PLAYER.number) {
                        WindowMode.adjustActiveWindow()
                    }
                } catch (exception: Exception) {
                    continue
                }
            }
            val cycleEnd = Utils.time()
            val sleepDuration = 20L - (cycleEnd - cycleStart)
            if (sleepDuration > 0) {
                TimeTools.sleep(sleepDuration)
            }
        }
    }

    fun stop() {
        finished = true
    }

    fun loadingStageChangedTime(): Long = loadingStageChangedTime

    @Synchronized
    fun setDisplayedGametip(gametip: LoginScreenComponent) {
        backComponent = frontComponent
        frontComponent = gametip
        lastComponentUpdate = Utils.time()
    }

    @Synchronized
    fun replaceComponent(): Boolean {
        return frontComponent.finished(lastComponentUpdate)
    }
}
