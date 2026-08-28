package com.jagex.game.runetek5.rendering

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY
import com.jagex.game.runetek5.scene.FrameRenderException
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * MessageBox || TopLeftBlackBox
 * @author Brainpower
 *
 */
object MessageBox {

    @JvmField var paddingTop = 0
    @JvmField var paddingLeft = 0
    @JvmField var assetsLoaded = false
    @JvmField var minimalTextWidth = 0
    @JvmField var boxId = 0
    @JvmField var anchorX: AnchorX? = null
    @JvmField var anchorY: AnchorY? = null
    @JvmField var outlineCornerSprite: SpriteType? = null // diagonal corner outline sprite, used 4x once for each corner `,
    @JvmField var outlineStraightSideSprite: SpriteType? = null // straight sides (left/right/bottom/top) outline
    @JvmField var boxTextCharacterSprites: Array<SpriteType>? = null
    @JvmField var filledBackgroundSpriteId = 0
    @JvmField var boxFontMetrics: FontMetrics? = null
    @JvmField var textColor = 0
    @JvmField var outlineCornerSpriteId = 0
    @JvmField var outlineStraightSideSpriteId = 0
    @JvmField var boxFilledBackground: SpriteType? = null // box background sprite without outlines
    @JvmField var minimalTextHeight = 0

    @JvmStatic
    fun initialize(horizontal: AnchorX, vertical: AnchorY, paddingLeft: Int, paddingTop: Int, minimalTextWidth: Int, minimalTextHeight: Int, outlineCorner: Int, outlineStraight: Int, outlineFill: Int, textColor: Int, boxId: Int) {
        MessageBox.anchorX = horizontal
        MessageBox.anchorY = vertical
        MessageBox.paddingLeft = paddingLeft
        MessageBox.paddingTop = paddingTop
        MessageBox.minimalTextWidth = minimalTextWidth
        MessageBox.minimalTextHeight = minimalTextHeight
        MessageBox.outlineCornerSpriteId = outlineCorner
        MessageBox.outlineStraightSideSpriteId = outlineStraight
        MessageBox.filledBackgroundSpriteId = outlineFill
        MessageBox.outlineCornerSprite = null
        MessageBox.outlineStraightSideSprite = null
        MessageBox.boxFilledBackground = null
        MessageBox.textColor = textColor
        MessageBox.boxId = boxId
        MessageBox.prepareAssets()
        MessageBox.assetsLoaded = true
    }

    @JvmStatic
    fun prepareAssets(): Boolean {
        var fullyCached = true
        if (outlineCornerSprite == null) {
            if (Resource.SPRITES?.isFileCached(outlineCornerSpriteId) == true) {
                outlineCornerSprite = SpriteType.listSingle(Resource.SPRITES!!, outlineCornerSpriteId)
            } else {
                fullyCached = false
            }
        }
        if (outlineStraightSideSprite == null) {
            if (Resource.SPRITES?.isFileCached(outlineStraightSideSpriteId) == true) {
                outlineStraightSideSprite = SpriteType.listSingle(Resource.SPRITES!!, outlineStraightSideSpriteId)
            } else {
                fullyCached = false
            }
        }
        if (boxFilledBackground == null) {
            if (Resource.SPRITES?.isFileCached(filledBackgroundSpriteId) == true) {
                boxFilledBackground = SpriteType.listSingle(Resource.SPRITES!!, filledBackgroundSpriteId)
            } else {
                fullyCached = false
            }
        }
        if (boxFontMetrics == null) {
            if (Resource.FONT_METRICS?.isFileCached(boxId) == true) {
                boxFontMetrics = FontMetrics.list(Resource.FONT_METRICS!!, boxId)
            } else {
                fullyCached = false
            }
        }
        if (boxTextCharacterSprites == null) {
            if (Resource.SPRITES?.isFileCached(boxId) == true) {
                boxTextCharacterSprites = SpriteType.listGroup(Resource.SPRITES!!, boxId)
            } else {
                fullyCached = false
            }
        }
        return fullyCached
    }

    // draw_top_left_box
    @JvmStatic
    fun draw(text: String?, renderFrame: Boolean, toolkit: AbstractToolkit?, fontRenderer: AbstractFontRenderer?, fontMetrics: FontMetrics?) {
        if (text == null || toolkit == null || fontRenderer == null || fontMetrics == null) {
            return
        }
        var boxMetrics = fontMetrics
        var boxRenderer = fontRenderer
        val ready = !assetsLoaded || prepareAssets()
        if (ready) {
            if (assetsLoaded) {
                boxMetrics = boxFontMetrics!!
                boxRenderer = toolkit.createFontRenderer(boxMetrics, boxTextCharacterSprites, true)
                var boxW = boxMetrics.getTextWidthMultiline(text, 250, null)
                var boxH = boxMetrics.getTextHeightMultiline(text, 250, boxMetrics.verticalSpacing, null)
                val borderWidth = outlineStraightSideSprite!!.width
                val textInset = borderWidth + 4
                boxW += textInset * 2
                boxH += textInset * 2
                if (boxW < minimalTextWidth) {
                    boxW = minimalTextWidth
                }
                if (boxH < MessageBox.minimalTextHeight) {
                    boxH = MessageBox.minimalTextHeight
                }
                val drawX = anchorX!!.calculateDrawX(boxW, Client.GAME_WIDTH) + paddingLeft
                val drawY = anchorY!!.calculateDrawY(boxH, Client.GAME_HEIGHT) + paddingTop
                toolkit.generateSprite(boxFilledBackground, false).drawScaledColored(drawX + outlineCornerSprite!!.width, drawY + outlineCornerSprite!!.height, boxW - outlineCornerSprite!!.width * 2, boxH - outlineCornerSprite!!.height * 2, 1, 0, 0)
                toolkit.generateSprite(outlineCornerSprite, true).draw(drawX, drawY)
                outlineCornerSprite!!.flipHorizontal()
                toolkit.generateSprite(outlineCornerSprite, true).draw(boxW + drawX - borderWidth, drawY)
                outlineCornerSprite!!.flipVertical()
                toolkit.generateSprite(outlineCornerSprite, true).draw(boxW + drawX - borderWidth, boxH + drawY - borderWidth)
                outlineCornerSprite!!.flipHorizontal()
                toolkit.generateSprite(outlineCornerSprite, true).draw(drawX, boxH + drawY - borderWidth)
                outlineCornerSprite!!.flipVertical()
                toolkit.generateSprite(outlineStraightSideSprite, true).drawTiled(drawX, drawY + outlineCornerSprite!!.height, borderWidth, boxH - outlineCornerSprite!!.height * 2)
                outlineStraightSideSprite!!.rotate90DegLeft()
                toolkit.generateSprite(outlineStraightSideSprite, true).drawTiled(drawX + outlineCornerSprite!!.width, drawY, boxW - outlineCornerSprite!!.width * 2, borderWidth)
                outlineStraightSideSprite!!.rotate90DegLeft()
                toolkit.generateSprite(outlineStraightSideSprite, true).drawTiled(drawX + boxW - borderWidth, drawY + outlineCornerSprite!!.height, borderWidth, boxH - outlineCornerSprite!!.height * 2)
                outlineStraightSideSprite!!.rotate90DegLeft()
                toolkit.generateSprite(outlineStraightSideSprite, true).drawTiled(drawX + outlineCornerSprite!!.width, boxH + drawY - borderWidth, boxW - outlineCornerSprite!!.width * 2, borderWidth)
                outlineStraightSideSprite!!.rotate90DegLeft()
                boxRenderer.drawText(text, drawX + textInset, drawY + textInset, boxW - textInset * 2, boxH - textInset * 2, textColor or 0xffffff.inv(), -1, 1, 1, 0, null, null, null, 0, 0)
                InterfaceComponentRenderer.inboundsComponentToTop(drawX, drawY, boxW, boxH)
            } else {
                val textW = boxMetrics.getTextWidthMultiline(text, 250, null)
                val textH = boxMetrics.getLineCount(text, 250, null) * 13
                val padding: Byte = 4
                val textX = padding + 6
                val textY = padding + 6
                toolkit.drawFilledRectangle(textX - padding, textY - padding, padding + textW + padding, textH + padding + padding, 0xffffff.inv(), 0)
                toolkit.drawRectangleOutline(textX - padding, textY - padding, padding + textW + padding, padding + textH + padding, -1, 0)
                boxRenderer.drawText(text, textX, textY, textW, textH, -1, -1, 1, 1, 0, null, null, null, 0, 0)
                InterfaceComponentRenderer.inboundsComponentToTop(textX - padding, textY - padding, padding + textW + padding, padding + textH + padding)
            }
            if (renderFrame) {
                try {
                    toolkit.renderFrame()
                } catch (exception: FrameRenderException) {
                    // NOOP
                }
            }
        }
    }
}
