package com.jagex.game.runetek5.config.rsinterface

import com.jagex.game.runetek5.cache.node.Node

class IFEvents(
    @JvmField val settingsHash: Int,
    @JvmField val interfaceId: Int
) : Node<IFEvents>() {

    fun dragEnabled(): Boolean {
        return (this.settingsHash shr 21 and 0x1) != 0
    }

    fun clickOptionEnabled(option: Int): Boolean {
        return (this.settingsHash shr option + 1 and 0x1) != 0
    }

    fun getUseOptionFlags(): Int {
        return Component.getUseOptionFlags(this.settingsHash)
    }

    fun depthFlags(): Int {
        return this.settingsHash shr 18 and 0x7
    }

    fun isTargetableByUse(): Boolean {
        return (this.settingsHash shr 22 and 0x1) != 0
    }

    fun continueOptionEnabled(): Boolean {
        return (this.settingsHash and 0x1) != 0
    }

    fun ignoresDepthFlags(): Boolean {
        return (this.settingsHash shr 23 and 0x1) != 0
    }
}
