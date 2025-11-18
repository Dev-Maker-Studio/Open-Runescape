package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.config.gametip.component.TipsBackground
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem
import com.jagex.game.runetek5.rendering.Renderers

/**
 *
 * @author Steven - 01/01/2023
 *
 */
class NewsItemBackground(
    @JvmField val background: TipsBackground
) : DrawableItem {

    override fun draw(draw: Boolean) {
        if (draw) {
            Renderers.activeToolkit.drawFilledRectangle(0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379, this.background.color, 0)
        }
    }

    override fun isCached(): Boolean {
        return true
    }

    override fun initialize() {
        // NOOP
    }
}
