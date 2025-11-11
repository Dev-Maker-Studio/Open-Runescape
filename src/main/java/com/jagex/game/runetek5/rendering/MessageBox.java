package com.jagex.game.runetek5.rendering;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX;
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY;
import com.jagex.game.runetek5.scene.FrameRenderException;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

/**
 * MessageBox || TopLeftBlackBox
 * @author Brainpower
 *
 */
public class MessageBox {

	public static int paddingTop;
	public static int paddingLeft;
	public static boolean assetsLoaded;
	public static int minimalTextWidth;
	public static int boxId;
	public static AnchorX anchorX;
	public static AnchorY anchorY;
	public static SpriteType outlineCornerSprite; // diagonal corner outline sprite, used 4x once for each corner `,
	public static SpriteType outlineStraightSideSprite; // straight sides (left/right/bottom/top) outline
	public static SpriteType[] boxTextCharacterSprites;
	public static int filledBackgroundSpriteId;
	public static FontMetrics boxFontMetrics;
	public static int textColor;
	public static int outlineCornerSpriteId;
	public static int outlineStraightSideSpriteId;
	public static SpriteType boxFilledBackground; // box background sprite without outlines
	public static int minimalTextHeight;


	public static void initialize(AnchorX horizontal, AnchorY vertical, int paddingLeft, int paddingTop, int minimalTextWidth, int minimalTextHeight, int outlineCorner, int outlineStraight, int outlineFill, int textColor, int boxId) {
		MessageBox.anchorX = horizontal;
		MessageBox.anchorY = vertical;
		MessageBox.paddingLeft = paddingLeft;
		MessageBox.paddingTop = paddingTop;
		MessageBox.minimalTextWidth = minimalTextWidth;
		MessageBox.minimalTextHeight = minimalTextHeight;
		MessageBox.outlineCornerSpriteId = outlineCorner;
		MessageBox.outlineStraightSideSpriteId = outlineStraight;
		MessageBox.filledBackgroundSpriteId = outlineFill;
		MessageBox.outlineCornerSprite = null;
		MessageBox.outlineStraightSideSprite = null;
		MessageBox.boxFilledBackground = null;
		MessageBox.textColor = textColor;
		MessageBox.boxId = boxId;
		MessageBox.prepareAssets();
		MessageBox.assetsLoaded = true;
    }

	public static boolean prepareAssets() {
	    boolean fullyCached = true;
	    if (outlineCornerSprite == null) {
	        if (Resource.SPRITES.isFileCached(outlineCornerSpriteId)) {
	            outlineCornerSprite = SpriteType.listSingle(Resource.SPRITES, outlineCornerSpriteId);
	        } else {
	            fullyCached = false;
	        }
	    }
	    if (outlineStraightSideSprite == null) {
	        if (Resource.SPRITES.isFileCached(outlineStraightSideSpriteId)) {
	            outlineStraightSideSprite = SpriteType.listSingle(Resource.SPRITES, outlineStraightSideSpriteId);
	        } else {
	            fullyCached = false;
	        }
	    }
	    if (boxFilledBackground == null) {
	        if (Resource.SPRITES.isFileCached(filledBackgroundSpriteId)) {
	            boxFilledBackground = SpriteType.listSingle(Resource.SPRITES, filledBackgroundSpriteId);
	        } else {
	            fullyCached = false;
	        }
	    }
	    if (boxFontMetrics == null) {
	        if (Resource.FONT_METRICS.isFileCached(boxId)) {
	            boxFontMetrics = FontMetrics.list(Resource.FONT_METRICS, boxId);
	        } else {
	            fullyCached = false;
	        }
	    }
	    if (boxTextCharacterSprites == null) {
	        if (Resource.SPRITES.isFileCached(boxId)) {
	            boxTextCharacterSprites = SpriteType.listGroup(Resource.SPRITES, boxId);
	        } else {
	            fullyCached = false;
	        }
	    }
	    return fullyCached;
	}

	// draw_top_left_box
	public static void draw(String text, boolean renderFrame, AbstractToolkit toolkit, AbstractFontRenderer fontRenderer, FontMetrics fontMetrics) {
	    FontMetrics boxMetrics = fontMetrics;
	    AbstractFontRenderer boxRenderer = fontRenderer;
	    boolean ready = !assetsLoaded || prepareAssets();
	    if (ready) {
	        if (assetsLoaded) {
	            boxMetrics = boxFontMetrics;
	            boxRenderer = toolkit.createFontRenderer(boxMetrics, boxTextCharacterSprites, true);
	            int textW = boxMetrics.getTextWidthMultiline(text, 250, null);
	            int textH = boxMetrics.getTextHeightMultiline(text, 250, boxMetrics.verticalSpacing, null);
	            int i_15 = outlineStraightSideSprite.width;
	            int i_10 = i_15 + 4;
	            textW += i_10 * 2;
	            textH += i_10 * 2;
	            if (textW < minimalTextWidth) {
	                textW = minimalTextWidth;
	            }
	            if (textH < MessageBox.minimalTextHeight) {
	                textH = MessageBox.minimalTextHeight;
	            }
	            int drawX = anchorX.calculateDrawX(textW, client.GAME_WIDTH) + paddingLeft;
	            int drawY = anchorY.calculateDrawY(textH, client.GAME_HEIGHT) + paddingTop;
	            toolkit.generateSprite(boxFilledBackground, false).method2772(drawX + outlineCornerSprite.width, drawY + outlineCornerSprite.height, textW - outlineCornerSprite.width * 2, textH - outlineCornerSprite.height * 2, 1, 0, 0);
	            toolkit.generateSprite(outlineCornerSprite, true).draw(drawX, drawY);
	            outlineCornerSprite.flipHorizontal();
	            toolkit.generateSprite(outlineCornerSprite, true).draw(textW + drawX - i_15, drawY);
	            outlineCornerSprite.flipVertical();
	            toolkit.generateSprite(outlineCornerSprite, true).draw(textW + drawX - i_15, textH + drawY - i_15);
	            outlineCornerSprite.flipHorizontal();
	            toolkit.generateSprite(outlineCornerSprite, true).draw(drawX, textH + drawY - i_15);
	            outlineCornerSprite.flipVertical();
	            toolkit.generateSprite(outlineStraightSideSprite, true).method2756(drawX, drawY + outlineCornerSprite.height, i_15, textH - outlineCornerSprite.height * 2);
	            outlineStraightSideSprite.rotate90DegLeft();
	            toolkit.generateSprite(outlineStraightSideSprite, true).method2756(drawX + outlineCornerSprite.width, drawY, textW - outlineCornerSprite.width * 2, i_15);
	            outlineStraightSideSprite.rotate90DegLeft();
	            toolkit.generateSprite(outlineStraightSideSprite, true).method2756(drawX + textW - i_15, drawY + outlineCornerSprite.height, i_15, textH - outlineCornerSprite.height * 2);
	            outlineStraightSideSprite.rotate90DegLeft();
	            toolkit.generateSprite(outlineStraightSideSprite, true).method2756(drawX + outlineCornerSprite.width, textH + drawY - i_15, textW - outlineCornerSprite.width * 2, i_15);
	            outlineStraightSideSprite.rotate90DegLeft();
	            boxRenderer.drawText(text, drawX + i_10, drawY + i_10, textW - i_10 * 2, textH - i_10 * 2, textColor | ~0xffffff, -1, 1, 1, 0, null, null, null, 0, 0);
	            InterfaceComponentRenderer.inboundsComponentToTop(drawX, drawY, textW, textH);
	        } else {
	            int i_7 = boxMetrics.getTextWidthMultiline(text, 250, null);
	            int i_8 = boxMetrics.method6949(text, 250, null) * 13;
	            byte b_9 = 4;
	            int i_10 = b_9 + 6;
	            int i_11 = b_9 + 6;
	            toolkit.drawFilledRectangle(i_10 - b_9, i_11 - b_9, b_9 + i_7 + b_9, i_8 + b_9 + b_9, ~0xffffff, 0);
	            toolkit.drawRectangleOutline(i_10 - b_9, i_11 - b_9, b_9 + i_7 + b_9, b_9 + i_8 + b_9, -1, 0);
	            boxRenderer.drawText(text, i_10, i_11, i_7, i_8, -1, -1, 1, 1, 0, null, null, null, 0, 0);
	            InterfaceComponentRenderer.inboundsComponentToTop(i_10 - b_9, i_11 - b_9, b_9 + i_7 + b_9, b_9 + i_8 + b_9);
	        }
	        if (renderFrame) {
	            try {
	                toolkit.renderFrame();
	            } catch (FrameRenderException exception) {
	            	// NOOP
	            }
	        }
	    }
	}
}
