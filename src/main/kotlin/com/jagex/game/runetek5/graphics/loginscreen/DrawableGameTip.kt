package com.jagex.game.runetek5.graphics.loginscreen

import com.jagex.game.runetek5.config.gametip.GameTipType
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.Utils

/**
 * Drawable game tip component for login screen.
 */
class DrawableGameTip(
    @JvmField val tipsType: GameTipType,
    @JvmField val displayDurationMs: Int,
    @JvmField val timeBetweenUpdatesMs: Int,
    gameTipsLoader: GameTipsLoader
) : LoginScreenComponent {

    @JvmField val drawableItems: Array<DrawableItem?>
    @JvmField var toolkit: AbstractToolkit? = null
    @JvmField var dirty: Boolean = false

    init {
        drawableItems = arrayOfNulls(tipsType.tipDetailsCache!!.size)
        for (index in drawableItems.indices) {
            drawableItems[index] = gameTipsLoader.fromDetails(tipsType.tipDetailsCache!![index])
        }
    }

    override fun initialize() {
        if (toolkit !== Renderers.activeToolkit) {
            toolkit = Renderers.activeToolkit
            dirty = true
        }
        toolkit!!.applyClearFlags(3, 0)
        for (item in drawableItems) {
            item?.initialize()
        }
    }

    override fun draw() {
        for (item in drawableItems) {
            item?.draw(true)
        }
        dirty = false
    }

    override fun cleanup() {
    }

    override fun finished(initTime: Long): Boolean {
        return Utils.time() >= initTime + displayDurationMs
    }

    override fun timeBetweenUpdatesMs(): Int {
        return timeBetweenUpdatesMs
    }

    override fun calculateLoadingPercent(): Int {
        var cachedEntries = 0
        for (item in drawableItems) {
            if (item == null || item.isCached()) {
                ++cachedEntries
            }
        }
        return cachedEntries * 100 / drawableItems.size
    }
}
