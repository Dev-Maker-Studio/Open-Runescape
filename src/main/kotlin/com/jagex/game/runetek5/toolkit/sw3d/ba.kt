package com.jagex.game.runetek5.toolkit.sw3d

import com.jagex.game.addon.NativeName
import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.toolkit.SoftwareToolkit

/**
 * Native name: ba
 * @author Brainpower
 *
 */
class ba(ja1: SoftwareToolkit, i_2: Int) : NativeMemoryNode(), INativeObject, AutoCloseable {

    @JvmField var nativeid: Long = 0L

    init {
        sa(ja1, i_2)
    }

    @NativeName("sa")
    external fun sa(var1: SoftwareToolkit, var2: Int)

    //TODO:
    override external fun destroy(var1: Boolean)

    @NativeName("ha")
    external fun ha()

    @NativeName("u")
    external fun u()

    @NativeName("v")
    internal external fun v()

    @NativeName("j")
    internal external fun j()

    @NativeName("n")
    internal external fun n(var1: SoftwareToolkit, var2: Int)

    @NativeName("g")
    internal external fun g(var1: SoftwareToolkit, var2: Int)

    override fun close() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this)
        }
    }
}
