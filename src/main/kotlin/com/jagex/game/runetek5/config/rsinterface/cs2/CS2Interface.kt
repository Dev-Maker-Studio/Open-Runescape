package com.jagex.game.runetek5.config.rsinterface.cs2

import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.Interface

class CS2Interface {

    @JvmField var inter: Interface? = null
    @JvmField var comp: Component? = null

    fun setChild(interfaceId: Int, slotId: Int): Boolean {
        val def = Component.listIcomponent(interfaceId, slotId)
        if (def != null) {
            this.inter = Interface.INTERFACES!![interfaceId shr 16]
            this.comp = def
            return true
        }
        reset()
        return false
    }

    fun load(rsinterface: Interface?, componentId: Int): Boolean {
        if (rsinterface != null) {
            val component = rsinterface.getComponent(componentId)
            if (component != null) {
                this.inter = rsinterface
                this.comp = component
                return true
            }
        }
        reset()
        return false
    }

    fun reset() {
        this.inter = null
        this.comp = null
    }

    fun method8772(): Component? {
        return this.inter!!.getComponent(this.comp!!.idHash)
    }
}
