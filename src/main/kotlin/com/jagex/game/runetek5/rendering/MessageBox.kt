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
                var textW = boxMetrics.getTextWidthMultiline(text, 250, null)
                var textH = boxMetrics.getTextHeightMultiline(text, 250, boxMetrics.verticalSpacing, null)
                val i_15 = outlineStraightSideSprite!!.width
                val i_10 = i_15 + 4
                textW += i_10 * 2
                textH += i_10 * 2
                if (textW < minimalTextWidth) {
                    textW = minimalTextWidth
                }
                if (textH < MessageBox.minimalTextHeight) {
                    textH = MessageBox.minimalTextHeight
                }
                val drawX = anchorX!!.calculateDrawX(textW, Client.GAME_WIDTH) + paddingLeft
                val drawY = anchorY!!.calculateDrawY(textH, Client.GAME_HEIGHT) + paddingTop
                toolkit.generateSprite(boxFilledBackground, false).drawScaledColored(drawX + outlineCornerSprite!!.width, drawY + outlineCornerSprite!!.height, textW - outlineCornerSprite!!.width * 2, textH - outlineCornerSprite!!.height * 2, 1, 0, 0)
                toolkit.generateSprite(outlineCornerSprite, true).draw(drawX, drawY)
                outlineCornerSprite!!.flipHorizontal()
                toolkit.generateSprite(outlineCornerSprite, true).draw(textW + drawX - i_15, drawY)
                outlineCornerSprite!!.flipVertical()
                toolkit.generateSprite(outlineCornerSprite, true).draw(textW + drawX - i_15, textH + drawY - i_15)
                outlineCornerSprite!!.flipHorizontal()
                toolkit.generateSprite(outlineCornerSprite, true).draw(drawX, textH + drawY - i_15)
                outlineCornerSprite!!.flipVertical()
                toolkit.generateSprite(outlineStraightSideSprite, true).drawTiled(drawX, drawY + outlineCornerSprite!!.height, i_15, textH - outlineCornerSprite!!.height * 2)
                outlineStraightSideSprite!!.rotate90DegLeft()
                toolkit.generateSprite(outlineStraightSideSprite, true).drawTiled(drawX + outlineCornerSprite!!.width, drawY, textW - outlineCornerSprite!!.width * 2, i_15)
                outlineStraightSideSprite!!.rotate90DegLeft()
                toolkit.generateSprite(outlineStraightSideSprite, true).drawTiled(drawX + textW - i_15, drawY + outlineCornerSprite!!.height, i_15, textH - outlineCornerSprite!!.height * 2)
                outlineStraightSideSprite!!.rotate90DegLeft()
                toolkit.generateSprite(outlineStraightSideSprite, true).drawTiled(drawX + outlineCornerSprite!!.width, textH + drawY - i_15, textW - outlineCornerSprite!!.width * 2, i_15)
                outlineStraightSideSprite!!.rotate90DegLeft()
                boxRenderer.drawText(text, drawX + i_10, drawY + i_10, textW - i_10 * 2, textH - i_10 * 2, textColor or 0xffffff.inv(), -1, 1, 1, 0, null, null, null, 0, 0)
                InterfaceComponentRenderer.inboundsComponentToTop(drawX, drawY, textW, textH)
            } else {
                val i_7 = boxMetrics.getTextWidthMultiline(text, 250, null)
                val i_8 = boxMetrics.getLineCount(text, 250, null) * 13
                val b_9: Byte = 4
                val i_10 = b_9 + 6
                val i_11 = b_9 + 6
                toolkit.drawFilledRectangle(i_10 - b_9, i_11 - b_9, b_9 + i_7 + b_9, i_8 + b_9 + b_9, 0xffffff.inv(), 0)
                toolkit.drawRectangleOutline(i_10 - b_9, i_11 - b_9, b_9 + i_7 + b_9, b_9 + i_8 + b_9, -1, 0)
                boxRenderer.drawText(text, i_10, i_11, i_7, i_8, -1, -1, 1, 1, 0, null, null, null, 0, 0)
                InterfaceComponentRenderer.inboundsComponentToTop(i_10 - b_9, i_11 - b_9, b_9 + i_7 + b_9, b_9 + i_8 + b_9)
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
