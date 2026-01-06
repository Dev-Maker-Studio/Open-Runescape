package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.gametip.component.OutlinedNewsItem
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem
import com.jagex.game.runetek5.network.newsitem.NewsItemFetcher
import com.jagex.game.runetek5.rendering.Renderers

/**
 * Drawable news item component for the login screen.
 */
class DrawableNewsItem(
    @JvmField val newsItemFetcher: NewsItemFetcher,
    @JvmField val outlinedNewsItem: OutlinedNewsItem
) : DrawableItem {

    override fun isCached(): Boolean {
        return newsItemFetcher.fetch()
    }

    override fun draw(bool1: Boolean) {
        val newsitem = newsItemFetcher.get(outlinedNewsItem.newsitemId) ?: return

        var x = outlinedNewsItem.horizontalAnchor.calculateDrawX(outlinedNewsItem.width, Client.GAME_WIDTH) + outlinedNewsItem.offsetX
        var y = outlinedNewsItem.verticalAnchor.calculateDrawY(outlinedNewsItem.height, Client.GAME_HEIGHT) + outlinedNewsItem.offsetY

        if (outlinedNewsItem.drawOutline) {
            Renderers.activeToolkit!!.drawRectangleOutline(
                x, y, outlinedNewsItem.width, outlinedNewsItem.height,
                outlinedNewsItem.backgroundColor, 0
            )
        }

        y += drawText(MiniMenu.minimenuHeader!!, newsitem.line1, x, y) * 12
        y += 8

        if (outlinedNewsItem.drawOutline) {
            Renderers.activeToolkit!!.drawTransparentLine(
                x, y, outlinedNewsItem.width + x - 1, y,
                outlinedNewsItem.backgroundColor, 0
            )
        }

        y++
        y += drawText(MiniMenu.minimenuHeader!!, newsitem.line2, x, y) * 12
        y += 5
        drawText(MiniMenu.minimenuHeader!!, newsitem.line3, x, y)
    }

    /**
     * Draws text using the specified renderer.
     *
     * @param renderer The font renderer
     * @param text The text to draw
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The number of lines drawn
     */
    fun drawText(renderer: AbstractFontRenderer, text: String, x: Int, y: Int): Int {
        return renderer.drawText(
            text,
            x + 5,
            y + 5,
            outlinedNewsItem.width - 10,
            outlinedNewsItem.height - 10,
            outlinedNewsItem.textColor,
            outlinedNewsItem.shadeColor,
            0, 0, 0, null, null, null, 0, 0
        )
    }

    override fun initialize() {
        // NOOP
    }
}
