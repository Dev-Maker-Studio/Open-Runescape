package com.jagex.game.runetek5.imageproducer

import java.awt.*

import com.jagex.game.runetek5.toolkit.SoftwareToolkit
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject
import com.jagex.game.runetek5.utils.Utils

class ImageProducerSw3d(ja1: SoftwareToolkit, canvas2: Canvas, i_3: Int, i_4: Int) : DelegatingDisplay(), INativeObject, AutoCloseable {

    @JvmField var anInt10288: Int
    @JvmField var anInt10287: Int
    @JvmField var nativeid: Long = 0L
    @JvmField var aCanvas10290: Canvas?
    @JvmField val aja10289: SoftwareToolkit

    init {
        aja10289 = ja1
        aCanvas10290 = canvas2
        anInt10288 = i_3
        anInt10287 = i_4
        n(ja1, aCanvas10290, i_3, i_4)
    }

    override fun close() {
        if (nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    override fun reload(): Boolean {
        aja10289.PA(this)
        return true
    }

    external fun n(var1: SoftwareToolkit, var2: Canvas?, var3: Int, var4: Int)

    external fun H(var1: Int, var2: Int, var3: Int, var4: Int)

    external fun wa(var1: Canvas?, var2: Int, var3: Int)

    override fun draw(): Int {
        return draw(0, 0)
    }

    override fun resize(i_1: Int, i_2: Int) {
        anInt10288 = i_1
        anInt10287 = i_2
        wa(aCanvas10290, i_1, i_2)
        if (aja10289.getDisplay() === this) {
            reload()
        }
    }

    override fun getWidth(): Int {
        return anInt10288
    }

    override fun draw(i_1: Int, i_2: Int): Int {
        return try {
            synchronized(aCanvas10290!!.treeLock) {
                val dimension4 = aCanvas10290!!.size
                H(i_1, i_2, dimension4.width, dimension4.height)
                aBool10291 = false
                0
            }
        } catch (exception6: Exception) {
            method15706(exception6)
            -1
        }
    }

    override fun cleanup() {
        if (aja10289.getDisplay() === this) {
            setDepthStencilSurface()
        }

        destroy(true)
        nativeid = 0L
        aCanvas10290 = null
    }

    external fun `as`(var1: Int, var2: Int, var3: Int, var4: Int)

    external fun ai(var1: Canvas?, var2: Int, var3: Int)

    external fun ah(var1: Canvas?, var2: Int, var3: Int)

    external override fun destroy(var1: Boolean)

    external fun ax(var1: Int, var2: Int, var3: Int, var4: Int)

    fun method15706(exception1: Exception) {
        if (!aBool10291) {
            aLong10286 = Utils.time()
            aBool10291 = true
        } else {
            if (Utils.time() - aLong10286 >= 30000L) {
                throw RuntimeException(exception1.message)
            }

            aCanvas10290!!.repaint()
        }
    }

    override fun getHeight(): Int {
        return anInt10287
    }

    override fun setDepthStencilSurface(): Boolean {
        return true
    }

    external fun az(var1: SoftwareToolkit, var2: Canvas?, var3: Int, var4: Int)

    external fun ap(var1: Int, var2: Int, var3: Int, var4: Int)

    external fun al(var1: Int, var2: Int, var3: Int, var4: Int)

    companion object {
        @JvmField var aBool10291 = false
        @JvmField var aLong10286: Long = 0L
    }
}
