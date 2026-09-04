package com.jagex.game.runetek5.graphics.sprite

import java.awt.Image
import java.awt.MediaTracker
import java.awt.Toolkit
import java.awt.image.PixelGrabber

import com.jagex.game.runetek5.cache.compression.GzipDecompressor
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpIrregularBricks
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit

abstract class NativeSprite : Sprite {

    abstract fun draw(x: Int, y: Int, combineMode: Int, colorArgb: Int, blendMode: Int)

    abstract fun setSize(offsetX: Int, offsetY: Int, trimRight: Int, trimBottom: Int)

    abstract fun cacheSizeInto(sizeOut: IntArray)

    abstract fun getWidth(): Int

    abstract fun getSpriteHeight(): Int

    abstract fun captureScreenRegion(dstX: Int, dstY: Int, width: Int, height: Int, srcX: Int, srcY: Int)

    abstract fun pixelTransfer()

    open fun draw(x: Int, y: Int) {
        draw(x, y, 1, 0, 1)
    }

    open fun drawCircle(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        drawColoredCircle(x, y, width, height, combineMode, colorArgb, blendMode)
    }

    abstract fun drawColoredCircle(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int)

    open fun drawTiled(x: Int, y: Int, width: Int, height: Int) {
        drawScaledColored(x, y, width, height, 1, 0, 1)
    }

    open fun drawRotatedCenter(x: Float, y: Float, zoom: Int, rotation: Int) {
        drawRotatedWithPivot(x, y, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, zoom, rotation, 1, 0)
    }

    open fun drawRotatedWithPivot(x: Float, y: Float, pivotX: Float, pivotY: Float, zoom: Int, rotation: Int, combineMode: Int, colorArgb: Int) {
        if (zoom != 0) {
            var mutablePivotX = pivotX
            val rotationRadians = (rotation and 0xffff) * 9.587379924285257E-5
            var sinRotation = (StrictMath.sin(rotationRadians) * zoom).toFloat()
            var cosRotation = (StrictMath.cos(rotationRadians) * zoom).toFloat()
            var topLeftX = (-mutablePivotX * cosRotation + -pivotY * sinRotation) / 4096.0F + x
            var topLeftY = (--mutablePivotX * sinRotation + -pivotY * cosRotation) / 4096.0F + y
            val topRightX = ((getSpriteWidth() - mutablePivotX) * cosRotation + -pivotY * sinRotation) / 4096.0F + x
            val topRightY = (-(getSpriteWidth() - mutablePivotX) * sinRotation + -pivotY * cosRotation) / 4096.0F + y
            val bottomLeftX = (-mutablePivotX * cosRotation + (getSpriteHeight() - pivotY) * sinRotation) / 4096.0F + x
            val bottomLeftY = (--mutablePivotX * sinRotation + (getSpriteHeight() - pivotY) * cosRotation) / 4096.0F + y
            drawAffineTransformed(topLeftX, topLeftY, topRightX, topRightY, bottomLeftX, bottomLeftY, combineMode, colorArgb, 1)
        }
    }

    open fun drawRotatedSprite(x: Float, y: Float, rotation: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        rotate(x, y, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, 4141, rotation, surface, surfaceOffsetX, surfaceOffsetY)
    }

    open fun rotate(x: Float, y: Float, pivotX: Float, pivotY: Float, zoom: Int, rotation: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        if (zoom != 0) {
            var mutablePivotX = pivotX
            val rotationRadians = (rotation and 0xffff) * 9.587379924285257E-5
            val sinRotation = (StrictMath.sin(rotationRadians) * zoom).toFloat()
            val cosRotation = (StrictMath.cos(rotationRadians) * zoom).toFloat()
            val topLeftX = (-mutablePivotX * cosRotation + -pivotY * sinRotation) / 4096.0F + x
            val topLeftY = (--mutablePivotX * sinRotation + -pivotY * cosRotation) / 4096.0F + y
            val topRightX = ((getSpriteWidth() - mutablePivotX) * cosRotation + -pivotY * sinRotation) / 4096.0F + x
            val topRightY = (-(getSpriteWidth() - mutablePivotX) * sinRotation + -pivotY * cosRotation) / 4096.0F + y
            val bottomLeftX = (-mutablePivotX * cosRotation + (getSpriteHeight() - pivotY) * sinRotation) / 4096.0F + x
            val bottomLeftY = (--mutablePivotX * sinRotation + (getSpriteHeight() - pivotY) * cosRotation) / 4096.0F + y
            drawAffineTransformedMasked(topLeftX, topLeftY, topRightX, topRightY, bottomLeftX, bottomLeftY, surface, surfaceOffsetX, surfaceOffsetY)
        }
    }

    open fun drawAffineTransformed(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, combineMode: Int, colorArgb: Int, blendMode: Int) {
        drawAffine(topLeftX, topLeftY, topRightX, topRightY, bottomLeftX, bottomLeftY, combineMode, colorArgb, blendMode)
    }

    abstract fun drawAffine(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, combineMode: Int, colorArgb: Int, blendMode: Int)

    abstract fun drawAffineMasked(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, combineMode: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int)

    abstract fun drawScaledColored(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int)

    abstract fun drawWithSurface(x: Int, y: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int)

    /**
     * Draws a black opaque circle
     */
    open fun drawCircle(x: Int, y: Int, width: Int, height: Int) {
        drawColoredCircle(x, y, width, height, 1, 0, 1)
    }

    open fun drawRotatedCenterColored(x: Float, y: Float, zoom: Int, rotation: Int, colorArgb: Int) {
        drawRotatedWithPivot(x, y, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, zoom, rotation, 0, colorArgb)
    }

    abstract fun getHeight(): Int

    open fun drawAffineTransformedMasked(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        drawAffineMasked(topLeftX, topLeftY, topRightX, topRightY, bottomLeftX, bottomLeftY, 1, surface, surfaceOffsetX, surfaceOffsetY)
    }

    abstract override fun getSpriteWidth(): Int

    abstract fun getFramebuffer(): FrameBuffer?

    companion object {
        @JvmField var backgroundTransparency: Int = 0
        @JvmField var loadingBarTileSpriteId: Int = 0
        @JvmField var alphaJitter: Int = 0
        @JvmField val GZIPPED_SPRITE_DATA: ByteArray = byteArrayOf(31.toByte(), (-117).toByte(), 8.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), (-5).toByte(), 127.toByte(), (-29).toByte(), (-1).toByte(), 109.toByte(), 6.toByte(), 103.toByte(), 6.toByte(), 14.toByte(), 54.toByte(), 54.toByte(), 118.toByte(), 54.toByte(), 86.toByte(), 14.toByte(), 118.toByte(), 118.toByte(), 118.toByte(), 78.toByte(), 78.toByte(), 14.toByte(), 46.toByte(), 30.toByte(), 17.toByte(), 94.toByte(), 30.toByte(), 110.toByte(), 110.toByte(), 30.toByte(), 73.toByte(), 33.toByte(), 97.toByte(), 126.toByte(), 17.toByte(), 89.toByte(), 41.toByte(), 121.toByte(), 57.toByte(), 89.toByte(), 41.toByte(), 25.toByte(), 25.toByte(), 5.toByte(), 21.toByte(), 61.toByte(), 117.toByte(), 5.toByte(), 37.toByte(), 29.toByte(), 101.toByte(), 25.toByte(), 25.toByte(), 13.toByte(), 115.toByte(), 77.toByte(), 29.toByte(), 3.toByte(), 67.toByte(), 19.toByte(), 19.toByte(), 19.toByte(), 121.toByte(), 117.toByte(), 75.toByte(), 91.toByte(), 11.toByte(), 35.toByte(), 27.toByte(), 61.toByte(), 99.toByte(), 19.toByte(), 35.toByte(), (-112).toByte(), 33.toByte(), (-116).toByte(), (-100).toByte(), (-100).toByte(), (-100).toByte(), 60.toByte(), (-36).toByte(), 60.toByte(), 18.toByte(), (-68).toByte(), (-68).toByte(), 18.toByte(), 70.toByte(), (-118).toByte(), 50.toByte(), (-118).toByte(), 70.toByte(), 36.toByte(), (-125).toByte(), (-1).toByte(), 7.toByte(), 24.toByte(), 68.toByte(), 56.toByte(), 24.toByte(), 24.toByte(), 25.toByte(), 24.toByte(), 89.toByte(), 24.toByte(), (-107).toByte(), 24.toByte(), (-104).toByte(), 4.toByte(), 25.toByte(), (-103).toByte(), 5.toByte(), 25.toByte(), 89.toByte(), (-108).toByte(), 24.toByte(), (-2).toByte(), 31.toByte(), 97.toByte(), (-112).toByte(), 103.toByte(), 96.toByte(), 96.toByte(), 100.toByte(), 101.toByte(), 4.toByte(), 3.toByte(), 6.toByte(), 40.toByte(), 96.toByte(), 100.toByte(), 98.toByte(), 102.toByte(), 97.toByte(), 101.toByte(), 99.toByte(), (-25).toByte(), (-32).toByte(), (-28).toByte(), (-30).toByte(), 6.toByte(), 42.toByte(), (-40).toByte(), 42.toByte(), (-64).toByte(), (-64).toByte(), (-60).toByte(), (-56).toByte(), (-52).toByte(), (-52).toByte(), (-60).toByte(), (-62).toByte(), (-52).toByte(), (-54).toByte(), (-54).toByte(), (-62).toByte(), 2.toByte(), (-108).toByte(), (-83).toByte(), 5.toByte(), (-54).toByte(), 51.toByte(), (-80).toByte(), 8.toByte(), (-78).toByte(), 10.toByte(), 41.toByte(), 26.toByte(), 58.toByte(), (-78).toByte(), 9.toByte(), 7.toByte(), 38.toByte(), (-78).toByte(), 43.toByte(), 21.toByte(), (-118).toByte(), 24.toByte(), 53.toByte(), 78.toByte(), 92.toByte(), (-56).toByte(), (-95).toByte(), (-20).toByte(), (-76).toByte(), (-15).toByte(), (-96).toByte(), 104.toByte(), (-48).toByte(), (-59).toByte(), 15.toByte(), 42.toByte(), (-58).toByte(), 73.toByte(), 69.toByte(), 77.toByte(), (-100).toByte(), 92.toByte(), 98.toByte(), (-30).toByte(), 18.toByte(), (-110).toByte(), 82.toByte(), (-86).toByte(), 106.toByte(), (-22).toByte(), 26.toByte(), (-102).toByte(), 90.toByte(), 38.toByte(), (-90).toByte(), 102.toByte(), (-26).toByte(), 22.toByte(), (-106).toByte(), 86.toByte(), (-50).toByte(), 46.toByte(), (-82).toByte(), 110.toByte(), (-18).toByte(), 30.toByte(), (-98).toByte(), 94.toByte(), (-63).toByte(), 33.toByte(), (-95).toByte(), 97.toByte(), (-31).toByte(), 17.toByte(), (-111).toByte(), 81.toByte(), (-55).toByte(), 41.toByte(), (-87).toByte(), 105.toByte(), (-23).toByte(), 25.toByte(), (-103).toByte(), 89.toByte(), (-59).toByte(), 37.toByte(), (-91).toByte(), 101.toByte(), (-27).toByte(), 21.toByte(), (-107).toByte(), 85.toByte(), (-51).toByte(), 45.toByte(), (-83).toByte(), 109.toByte(), (-19).toByte(), 29.toByte(), (-99).toByte(), 93.toByte(), (-109).toByte(), 38.toByte(), 79.toByte(), (-103).toByte(), 58.toByte(), 109.toByte(), (-6).toByte(), (-116).toByte(), (-103).toByte(), (-77).toByte(), 22.toByte(), 45.toByte(), 94.toByte(), (-78).toByte(), 116.toByte(), (-39).toByte(), (-14).toByte(), 21.toByte(), 43.toByte(), 87.toByte(), 109.toByte(), (-38).toByte(), (-68).toByte(), 101.toByte(), (-21).toByte(), (-74).toByte(), (-19).toByte(), 59.toByte(), 118.toByte(), (-18).toByte(), 58.toByte(), 116.toByte(), (-56).toByte(), (-47).toByte(), 99.toByte(), (-57).toByte(), 79.toByte(), (-100).toByte(), 60.toByte(), 117.toByte(), (-23).toByte(), (-14).toByte(), (-107).toByte(), (-85).toByte(), (-41).toByte(), (-82).toByte(), (-33).toByte(), (-72).toByte(), 121.toByte(), (-21).toByte(), (-31).toByte(), (-93).toByte(), (-57).toByte(), 79.toByte(), (-98).toByte(), 62.toByte(), 123.toByte(), (-2).toByte(), (-30).toByte(), (-27).toByte(), (-85).toByte(), (-113).toByte(), (-97).toByte(), 62.toByte(), 127.toByte(), (-7).toByte(), (-6).toByte(), (-19).toByte(), (-5).toByte(), (-113).toByte(), (-97).toByte(), (-65).toByte(), 64.toByte(), (-2).toByte(), 98.toByte(), 100.toByte(), 96.toByte(), 102.toByte(), (-124).toByte(), 1.toByte(), (-84).toByte(), (-2).toByte(), 18.toByte(), 4.toByte(), (-6).toByte(), (-119).toByte(), (-119).toByte(), (-123).toByte(), (-123).toByte(), (-103).toByte(), (-123).toByte(), 29.toByte(), (-28).toByte(), 47.toByte(), 70.toByte(), (-90).toByte(), 114.toByte(), (-112).toByte(), 2.toByte(), 65.toByte(), 22.toByte(), 86.toByte(), 69.toByte(), 67.toByte(), 54.toByte(), 33.toByte(), (-57).toByte(), 64.toByte(), (-10).toByte(), (-60).toByte(), 66.toByte(), 97.toByte(), 37.toByte(), (-93).toByte(), 70.toByte(), 14.toByte(), 17.toByte(), (-89).toByte(), (-119).toByte(), 11.toByte(), 55.toByte(), 30.toByte(), (-28).toByte(), 84.toByte(), 54.toByte(), 14.toByte(), (-6).toByte(), 32.toByte(), (-102).toByte(), 84.toByte(), 116.toByte(), (-111).toByte(), 75.toByte(), 76.toByte(), (-59).toByte(), (-28).toByte(), (-95).toByte(), (-22).toByte(), 71.toByte(), (-112).toByte(), (-41).toByte(), (-64).toByte(), 62.toByte(), 35.toByte(), (-50).toByte(), 99.toByte(), 77.toByte(), 100.toByte(), (-7).toByte(), 12.toByte(), (-18).toByte(), 49.toByte(), (-124).toByte(), (-65).toByte(), 110.toByte(), 49.toByte(), (-16).toByte(), (-79).toByte(), 48.toByte(), 2.toByte(), (-29).toByte(), 15.toByte(), (-24).toByte(), 78.toByte(), 6.toByte(), 6.toByte(), 123.toByte(), (-122).toByte(), (-97).toByte(), (-1).toByte(), (-70).toByte(), 52.toByte(), 22.toByte(), (-16).toByte(), (-49).toByte(), 127.toByte(), (-79).toByte(), (-88).toByte(), (-21).toByte(), (-1).toByte(), 77.toByte(), 0.toByte(), 38.toByte(), 27.toByte(), (-28).toByte(), 10.toByte(), 110.toByte(), 2.toByte(), 0.toByte(), 0.toByte())

        @JvmField val SPRITE_LRU_CACHE: LRUCache = LRUCache(128, 4)

        @JvmStatic
        fun fromJs5(spriteResource: Js5ResourceProvider, fileId: Int): NativeSprite {
            var sprite = SPRITE_LRU_CACHE.get(fileId.toLong()) as NativeSprite?
            if (sprite == null) {
                sprite = if (Resource.USING_JAGEX_IMAGE_FORMAT) {
                    Renderers.activeToolkit!!.generateSprite(SpriteType.listSingle(spriteResource, fileId), true)
                } else {
                    fromPixelgrabber(spriteResource.getFile(fileId))
                }
                SPRITE_LRU_CACHE.put(sprite, fileId.toLong())
            }
            return sprite
        }

        @JvmStatic
        fun drawLoadingBarBackground(toolkit: AbstractToolkit, x: Int, y: Int, width: Int, totalHeight: Int, height: Int, fontRenderer: AbstractFontRenderer, fontMetrics: FontMetrics, text: String) {
            var alpha = 255 - backgroundTransparency - alphaJitter
            if (alpha < 0) {
                alpha = 0
            }

            if (StaticMedia.loadingBarBackgroundSprite == null || StaticMedia.loadingBarEdgeSprite == null) {
                if (Resource.SPRITES!!.isFileCached(StaticMethods.loadingBarBackgroundSpriteId) && Resource.SPRITES!!.isFileCached(loadingBarTileSpriteId)) {
                    StaticMedia.loadingBarBackgroundSprite = toolkit.generateSprite(SpriteType.listSingle(Resource.SPRITES!!, StaticMethods.loadingBarBackgroundSpriteId, 0), true)
                    val spriteType = SpriteType.listSingle(Resource.SPRITES!!, loadingBarTileSpriteId, 0)
                    StaticMedia.loadingBarEdgeSprite = toolkit.generateSprite(spriteType, true)
                    spriteType!!.flipHorizontal()
                    StaticMethods.loadingBarTileSprite = toolkit.generateSprite(spriteType, true)
                } else {
                    toolkit.drawFilledRectangle(x, y, width, height, alpha shl 24 or TextureOpIrregularBricks.backgroundColor, 1)
                }
            }

            if (StaticMedia.loadingBarBackgroundSprite != null && StaticMedia.loadingBarEdgeSprite != null) {
                val tileCount = (width - StaticMedia.loadingBarEdgeSprite!!.getWidth() * 2) / StaticMedia.loadingBarBackgroundSprite!!.getWidth()
                for (i in 0 until tileCount) {
                    StaticMedia.loadingBarBackgroundSprite!!.draw(x + StaticMedia.loadingBarEdgeSprite!!.getWidth() + i * StaticMedia.loadingBarBackgroundSprite!!.getWidth(), y)
                }
                StaticMedia.loadingBarEdgeSprite!!.draw(x, y)
                StaticMethods.loadingBarTileSprite!!.draw(width + x - StaticMethods.loadingBarTileSprite!!.getWidth(), y)
            }

            fontRenderer.renderPlain(text, x + 3, (20 - fontMetrics.topPadding) / 2 + y + fontMetrics.topPadding, MiniMenu.menuTextColor or 0xffffff.inv(), -1)
            toolkit.drawFilledRectangle(x, height + y, width, totalHeight - height, alpha shl 24 or TextureOpIrregularBricks.backgroundColor, 1)
        }

        @JvmStatic
        fun resetCache() {
            SPRITE_LRU_CACHE.reset()
        }

        @JvmStatic
        fun supportsGzippedSprites(): Boolean {
            return try {
                val gzip = GzipDecompressor()
                val spriteData = gzip.decompressCustomSize(GZIPPED_SPRITE_DATA)
                fromPixelgrabber(spriteData)
                true
            } catch (exception: Exception) {
                false
            }
        }

        @JvmStatic
        fun fromPixelgrabber(imageData: ByteArray?): NativeSprite {
            if (imageData == null) {
                throw RuntimeException("")
            }
            while (true) {
                try {
                    val image = Toolkit.getDefaultToolkit().createImage(imageData)
                    val mediaTracker = MediaTracker(GameShell.suppliedApplet)
                    mediaTracker.addImage(image, 0)
                    mediaTracker.waitForAll()
                    val width = image.getWidth(GameShell.suppliedApplet)
                    val height = image.getHeight(GameShell.suppliedApplet)
                    if (!mediaTracker.isErrorAny && width >= 0 && height >= 0) {
                        val pixels = IntArray(width * height)
                        val pixelGrabber = PixelGrabber(image, 0, 0, width, height, pixels, 0, width)
                        pixelGrabber.grabPixels()
                        val nativeSprite = Renderers.activeToolkit!!.createNativeSprite(pixels, width, width, height)
                        return nativeSprite
                    }
                    throw RuntimeException("")
                } catch (ignored: InterruptedException) {
                    // NOOP
                }
            }
        }
    }
}
