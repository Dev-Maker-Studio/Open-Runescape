package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.SoftwareToolkit
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject
import com.jagex.game.runetek5.toolkit.sw3d.ba

/**
 * Transparent font renderer for enhanced software mode with alpha support.
 */
class TransparentFontRendererSw3d(
    toolkit: SoftwareToolkit,
    ba2: ba,
    fontmetrics: FontMetrics,
    sprites: Array<SpriteType>
) : AbstractFontRenderer(toolkit, fontmetrics), INativeObject, AutoCloseable {

    @JvmField var nativeid: Long = 0

    init {
        val alphaData = Array(sprites.size) { index ->
            val sprite = sprites[index]
            if (sprite.alpha != null) {
                sprite.alpha
            } else {
                val pixels = sprite.pixels
                val alpha = ByteArray(pixels.size)
                for (i in pixels.indices) {
                    alpha[i] = if (pixels[i].toInt() == 0) 0 else -1
                }
                alpha
            }
        }
        val widths = IntArray(sprites.size) { sprites[it].width }
        val heights = IntArray(sprites.size) { sprites[it].height }
        val minXOffsets = IntArray(sprites.size) { sprites[it].minX }
        val minYOffsets = IntArray(sprites.size) { sprites[it].minY }

        n(toolkit, ba2, alphaData, widths, heights, minXOffsets, minYOffsets)
    }

    override external fun drawChar(var1: Int, var2: Int, var3: Int, var4: Int, var5: Boolean)

    private external fun n(
        var1: SoftwareToolkit,
        var2: ba,
        var3: Array<ByteArray>,
        var4: IntArray,
        var5: IntArray,
        var6: IntArray,
        var7: IntArray
    )

    private external fun aq(
        var1: Char,
        var2: Int,
        var3: Int,
        var4: Int,
        var5: Boolean,
        var6: ComponentSurface,
        var7: Int,
        var8: Int
    )

    override fun close() {
        if (nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    private external fun P(
        var1: Int,
        var2: Int,
        var3: Int,
        var4: Int,
        var5: Boolean,
        var6: ComponentSurface,
        var7: Int,
        var8: Int
    )

    override external fun destroy(var1: Boolean)

    override fun drawChar(
        var_1: Int,
        i_2: Int,
        i_3: Int,
        i_4: Int,
        bool5: Boolean,
        class4556: ComponentSurface,
        i_7: Int,
        i_8: Int
    ) {
        P(var_1, i_2, i_3, i_4, bool5, class4556, i_7, i_8)
    }

    private external fun ae(
        var1: Char,
        var2: Int,
        var3: Int,
        var4: Int,
        var5: Boolean,
        var6: ComponentSurface,
        var7: Int,
        var8: Int
    )
}
