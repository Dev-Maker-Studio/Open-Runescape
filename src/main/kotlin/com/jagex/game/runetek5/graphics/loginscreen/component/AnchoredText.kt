package com.jagex.game.runetek5.graphics.loginscreen.component

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.gametip.component.AnchoredTextMeta
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers

class AnchoredText(
    @JvmField val spriteResource: Js5ResourceProvider,
    @JvmField val fontResource: Js5ResourceProvider,
    @JvmField val item: AnchoredTextMeta
) : DrawableItem {

    @JvmField var frontRenderer: AbstractFontRenderer? = null

    override fun draw(draw: Boolean) {
        if (draw) {
            val x = this.item.anchorX.calculateDrawX(this.item.width, client.GAME_WIDTH) + this.item.offsetX
            val y = this.item.anchorY.calculateDrawY(this.item.height, client.GAME_HEIGHT) + this.item.offsetY
            this.frontRenderer!!.drawText(this.item.tipText, x, y, this.item.width, this.item.height, this.item.textColor, this.item.textShadowColor,
                this.item.textAllignment, this.item.textVerticalAllignment, this.item.lineSpacing, null, null, null, 0, 0)
        }
    }

    override fun isCached(): Boolean {
        var cached = this.spriteResource.isFileCached(this.item.fileId)
        if (!this.fontResource.isFileCached(this.item.fileId)) {
            cached = false
        }
        return cached
    }

    override fun initialize() {
        val fontMetrics = FontMetrics.list(this.fontResource, this.item.fileId)
        this.frontRenderer = Renderers.activeToolkit.createFontRenderer(fontMetrics, SpriteType.listGroup(this.spriteResource, this.item.fileId), true)
    }
}
