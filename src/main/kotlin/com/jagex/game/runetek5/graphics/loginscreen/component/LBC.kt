package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.client.LoadingStage
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.gametip.component.AnchoredElement
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.utils.Utils

abstract class LBC(
    @JvmField val spriteResource: Js5ResourceProvider,
    @JvmField val fontResource: Js5ResourceProvider,
    @JvmField val anchoredElement: AnchoredElement
) : DrawableItem {

    @JvmField var fontRenderer: AbstractFontRenderer? = null
    @JvmField var lastLoadPercent: Int = 0
    @JvmField var lastLoadPercentChange: Long = 0

    override fun initialize() {
        val fontmetrics = FontMetrics.list(fontResource, anchoredElement.fileId)
        fontRenderer = Renderers.activeToolkit!!.createFontRenderer(fontmetrics, SpriteType.listGroup(spriteResource, anchoredElement.fileId), true)
    }

    override fun isCached(): Boolean {
        var cached = spriteResource.isFileCached(anchoredElement.fileId)
        if (!fontResource.isFileCached(anchoredElement.fileId)) {
            cached = false
        }
        return cached
    }

    abstract fun drawBackground(x: Int, y: Int)

    fun loadPercentDecimalFullout(): Int {
        val loadPercentage = LoadingStage.plainLoginScreenRenderer!!.getLoadingPercentage()
        var loadPercentDecimalFullout = loadPercentage * 100
        if (loadPercentage == lastLoadPercent && loadPercentage != 0) {
            val loadingStageCompletion = LoadingStage.plainLoginScreenRenderer!!.getLoadingStageCompletion()
            if (loadingStageCompletion > loadPercentage) {
                val timeDelta = lastLoadPercentChange - LoadingStage.plainLoginScreenRenderer!!.loadingStageChangedTime()
                if (timeDelta > 0L) {
                    val timeForCompletion = 10000L * timeDelta / loadPercentage * (loadingStageCompletion - loadPercentage)
                    val elapsedTime = (Utils.time() - lastLoadPercentChange) * 10000L
                    if (elapsedTime < timeForCompletion) {
                        loadPercentDecimalFullout = (100L * elapsedTime * (loadingStageCompletion - loadPercentage) / timeForCompletion + (loadPercentage * 100)).toInt()
                    } else {
                        loadPercentDecimalFullout = loadingStageCompletion * 100
                    }
                }
            }
        } else {
            lastLoadPercent = loadPercentage
            lastLoadPercentChange = Utils.time()
        }
        return loadPercentDecimalFullout
    }

    override fun draw(draw: Boolean) {
        val x = anchoredElement.horizontalAnchor.calculateDrawX(anchoredElement.width, Client.GAME_WIDTH) + anchoredElement.offsetX
        val y = anchoredElement.verticalAnchor.calculateDrawY(anchoredElement.height, Client.GAME_HEIGHT) + anchoredElement.offsetY
        drawOutline(draw, x, y)
        drawBackground(x, y)
        var loadingText = LoadingStage.plainLoginScreenRenderer!!.getLoadingText()
        if (Utils.time() - lastLoadPercentChange > 10000L) {
            loadingText = loadingText + " (" + LoadingStage.plainLoginScreenRenderer!!.getLoadingStage()!!.number + ")"
        }
        fontRenderer!!.renderCentered(loadingText, anchoredElement.width / 2 + x, anchoredElement.height / 2 + anchoredElement.anInt4092 + y + 4, anchoredElement.anInt4091, -1)
    }

    abstract fun drawOutline(draw: Boolean, x: Int, y: Int)
}
