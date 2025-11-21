package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.SoftwareToolkit
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject
import com.jagex.game.runetek5.toolkit.sw3d.ba

/**
 * Opaque font renderer for Software 3D toolkit with native rendering methods.
 */
class OpaqueFontRendererSw3d(
    ja1: SoftwareToolkit,
    ba2: ba,
    fontmetrics3: FontMetrics,
    sprites: Array<SpriteType>
) : AbstractFontRenderer(ja1, fontmetrics3), INativeObject, AutoCloseable {

    @JvmField var nativeid: Long = 0

    init {
        val pixelData = Array(sprites.size) { sprites[it].pixels }
        val widths = IntArray(sprites.size) { sprites[it].width }
        val heights = IntArray(sprites.size) { sprites[it].height }
        val minXOffsets = IntArray(sprites.size) { sprites[it].minX }
        val minYOffsets = IntArray(sprites.size) { sprites[it].minY }

        n(ja1, ba2, pixelData, widths, heights, minXOffsets, minYOffsets)
    }

    private external fun aq(var1: SoftwareToolkit, var2: ba, var3: Array<ByteArray>, var4: IntArray, var5: IntArray, var6: IntArray, var7: IntArray)

    override external fun destroy(var1: Boolean)

    override external fun drawChar(var1: Int, var2: Int, var3: Int, var4: Int, var5: Boolean)

    override fun drawChar(char: Int, x: Int, y: Int, color: Int, shadow: Boolean, surface: ComponentSurface, clipX: Int, clipY: Int) {
        MA(char, x, y, color, shadow, surface, clipX, clipY)
    }

    private external fun MA(var1: Int, var2: Int, var3: Int, var4: Int, var5: Boolean, var6: ComponentSurface, var7: Int, var8: Int)

    private external fun aa(var1: Char, var2: Int, var3: Int, var4: Int, var5: Boolean, var6: ComponentSurface, var7: Int, var8: Int)

    override fun close() {
        if (nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    private external fun n(var1: SoftwareToolkit, var2: ba, var3: Array<ByteArray>, var4: IntArray, var5: IntArray, var6: IntArray, var7: IntArray)

    private external fun ae(var1: SoftwareToolkit, var2: ba, var3: Array<ByteArray>, var4: IntArray, var5: IntArray, var6: IntArray, var7: IntArray)

    private external fun av(var1: Char, var2: Int, var3: Int, var4: Int, var5: Boolean, var6: ComponentSurface, var7: Int, var8: Int)
}
