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

    abstract fun setSize(var1: Int, var2: Int, var3: Int, var4: Int)

    abstract fun cacheSizeInto(var1: IntArray)

    abstract fun getWidth(): Int

    abstract fun getSpriteHeight(): Int

    abstract fun method2750(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int)

    abstract fun pixelTransfer()

    open fun draw(x: Int, y: Int) {
        draw(x, y, 1, 0, 1)
    }

    open fun drawCircle(x: Int, y: Int, width: Int, length: Int, i_5: Int, color: Int, alpha: Int) {
        drawColoredCircle(x, y, width, length, i_5, color, alpha)
    }

    abstract fun drawColoredCircle(x: Int, y: Int, width: Int, length: Int, var5: Int, color: Int, blendMode: Int)

    open fun method2756(x: Int, y: Int, width: Int, height: Int) {
        method2772(x, y, width, height, 1, 0, 1)
    }

    open fun method2758(f_1: Float, f_2: Float, i_3: Int, rotation: Int) {
        method2760(f_1, f_2, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, i_3, rotation, 1, 0)
    }

    open fun method2760(x: Float, y: Float, scaleX: Float, scaleY: Float, offset: Int, rotation: Int, i_7: Int, i_8: Int) {
        if (offset != 0) {
            var mutableScaleX = scaleX
            val rotationRadians = (rotation and 0xffff) * 9.587379924285257E-5
            var sinRotation = (StrictMath.sin(rotationRadians) * offset).toFloat()
            var cosRotation = (StrictMath.cos(rotationRadians) * offset).toFloat()
            var transformedX1 = (-mutableScaleX * cosRotation + -scaleY * sinRotation) / 4096.0F + x
            var transformedY1 = (--mutableScaleX * sinRotation + -scaleY * cosRotation) / 4096.0F + y
            val transformedX2 = ((getSpriteWidth() - mutableScaleX) * cosRotation + -scaleY * sinRotation) / 4096.0F + x
            val transformedY2 = (-(getSpriteWidth() - mutableScaleX) * sinRotation + -scaleY * cosRotation) / 4096.0F + y
            val transformedX3 = (-mutableScaleX * cosRotation + (getSpriteHeight() - scaleY) * sinRotation) / 4096.0F + x
            val transformedY3 = (--mutableScaleX * sinRotation + (getSpriteHeight() - scaleY) * cosRotation) / 4096.0F + y
            method2763(transformedX1, transformedY1, transformedX2, transformedY2, transformedX3, transformedY3, i_7, i_8, 1)
        }
    }

    open fun drawRotatedSprite(f_1: Float, f_2: Float, i_4: Int, class4555: ComponentSurface, i_6: Int, i_7: Int) {
        rotate(f_1, f_2, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, 4141, i_4, class4555, i_6, i_7)
    }

    open fun rotate(f_1: Float, f_2: Float, f_3: Float, f_4: Float, i_5: Int, i_6: Int, class4557: ComponentSurface, i_8: Int, i_9: Int) {
        if (i_5 != 0) {
            var mutableF_3 = f_3
            val d_10 = (i_6 and 0xffff) * 9.587379924285257E-5
            val f_12 = (StrictMath.sin(d_10) * i_5).toFloat()
            val f_13 = (StrictMath.cos(d_10) * i_5).toFloat()
            val f_14 = (-mutableF_3 * f_13 + -f_4 * f_12) / 4096.0F + f_1
            val f_15 = (--mutableF_3 * f_12 + -f_4 * f_13) / 4096.0F + f_2
            val f_16 = ((getSpriteWidth() - mutableF_3) * f_13 + -f_4 * f_12) / 4096.0F + f_1
            val f_17 = (-(getSpriteWidth() - mutableF_3) * f_12 + -f_4 * f_13) / 4096.0F + f_2
            val f_18 = (-mutableF_3 * f_13 + (getSpriteHeight() - f_4) * f_12) / 4096.0F + f_1
            val f_19 = (--mutableF_3 * f_12 + (getSpriteHeight() - f_4) * f_13) / 4096.0F + f_2
            method2797(f_14, f_15, f_16, f_17, f_18, f_19, class4557, i_8, i_9)
        }
    }

    open fun method2763(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, i_8: Int, i_9: Int) {
        method2764(f_1, f_2, f_3, f_4, f_5, f_6, i_7, i_8, i_9)
    }

    abstract fun method2764(var1: Float, var2: Float, var3: Float, var4: Float, var5: Float, var6: Float, var7: Int, var8: Int, var9: Int)

    abstract fun method2766(var1: Float, var2: Float, var3: Float, var4: Float, var5: Float, var6: Float, var7: Int, var8: ComponentSurface, var9: Int, var10: Int)

    abstract fun method2772(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, var6: Int, var7: Int)

    abstract fun method2773(var1: Int, var2: Int, var3: ComponentSurface, var4: Int, var5: Int)

    /**
     * Draws a black opaque circle
     */
    open fun drawCircle(x: Int, y: Int, width: Int, length: Int) {
        drawColoredCircle(x, y, width, length, 1, 0, 1)
    }

    open fun method2790(f_1: Float, f_2: Float, i_3: Int, i_4: Int, i_6: Int) {
        method2760(f_1, f_2, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, i_3, i_4, 0, i_6)
    }

    abstract fun getHeight(): Int

    open fun method2797(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, class4557: ComponentSurface, i_8: Int, i_9: Int) {
        method2766(f_1, f_2, f_3, f_4, f_5, f_6, 1, class4557, i_8, i_9)
    }

    abstract override fun getSpriteWidth(): Int

    abstract fun getFramebuffer(): FrameBuffer?

    companion object {
        @JvmField var anInt3556: Int = 0
        @JvmField var anInt4350: Int = 0
        @JvmField var anInt156: Int = 0
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
        fun method12680(toolkit: AbstractToolkit, x: Int, y: Int, width: Int, i_4: Int, height: Int, fontrenderer: AbstractFontRenderer, fontmetrics: FontMetrics, string_8: String) {
            var i_10 = 255 - anInt3556 - anInt156
            if (i_10 < 0) {
                i_10 = 0
            }

            if (StaticMedia.aNativeSprite5576 == null || StaticMedia.aNativeSprite647 == null) {
                if (Resource.SPRITES!!.isFileCached(StaticMethods.anInt5438) && Resource.SPRITES!!.isFileCached(anInt4350)) {
                    StaticMedia.aNativeSprite5576 = toolkit.generateSprite(SpriteType.listSingle(Resource.SPRITES!!, StaticMethods.anInt5438, 0), true)
                    val spriteType = SpriteType.listSingle(Resource.SPRITES!!, anInt4350, 0)
                    StaticMedia.aNativeSprite647 = toolkit.generateSprite(spriteType, true)
                    spriteType!!.flipHorizontal()
                    StaticMethods.aNativeSprite612 = toolkit.generateSprite(spriteType, true)
                } else {
                    toolkit.drawFilledRectangle(x, y, width, height, i_10 shl 24 or TextureOpIrregularBricks.anInt9792, 1)
                }
            }

            if (StaticMedia.aNativeSprite5576 != null && StaticMedia.aNativeSprite647 != null) {
                val i_11 = (width - StaticMedia.aNativeSprite647!!.getWidth() * 2) / StaticMedia.aNativeSprite5576!!.getWidth()
                for (i_12 in 0 until i_11) {
                    StaticMedia.aNativeSprite5576!!.draw(x + StaticMedia.aNativeSprite647!!.getWidth() + i_12 * StaticMedia.aNativeSprite5576!!.getWidth(), y)
                }
                StaticMedia.aNativeSprite647!!.draw(x, y)
                StaticMethods.aNativeSprite612!!.draw(width + x - StaticMethods.aNativeSprite612!!.getWidth(), y)
            }

            fontrenderer.renderPlain(string_8, x + 3, (20 - fontmetrics.topPadding) / 2 + y + fontmetrics.topPadding, MiniMenu.anInt4722 or 0xffffff.inv(), -1)
            toolkit.drawFilledRectangle(x, height + y, width, i_4 - height, i_10 shl 24 or TextureOpIrregularBricks.anInt9792, 1)
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
