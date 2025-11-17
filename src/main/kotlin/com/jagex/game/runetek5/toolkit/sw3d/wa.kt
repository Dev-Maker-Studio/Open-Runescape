package com.jagex.game.runetek5.toolkit.sw3d

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.toolkit.SoftwareToolkit

class wa(
    ja1: SoftwareToolkit,
    ba2: ba,
    i_3: Int,
    i_4: Int,
    ints_5: IntArray?,
    ints_6: IntArray?
) : ComponentSurface(), INativeObject, AutoCloseable {

    @JvmField var nativeid: Long = 0

    init {
        l(ja1, ba2, i_3, i_4, ints_5, ints_6)
    }

    external fun g(var1: SoftwareToolkit, var2: ba, var3: Int, var4: Int, var5: IntArray?, var6: IntArray?)

    external fun l(var1: SoftwareToolkit, var2: ba, var3: Int, var4: Int, var5: IntArray?, var6: IntArray?)

    override fun close() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }

    override external fun destroy(var1: Boolean)

    external fun n(var1: SoftwareToolkit, var2: ba, var3: Int, var4: Int, var5: IntArray?, var6: IntArray?)
}
