package com.jagex.game.runetek5.network.world

abstract class WorldDescriptorFlags {
    @JvmField
    var flags: Int = 0

    @JvmField
    var playerCount: Int = 0

    @JvmField
    var worldIndex: Int = 0

    fun isMembers(): Boolean {
        return (flags and 0x1) != 0
    }

    fun isQuickChat(): Boolean {
        return (flags and 0x2) != 0
    }

    fun isPvp(): Boolean {
        return (flags and 0x4) != 0
    }

    fun isLootshare(): Boolean {
        return (flags and 0x8) != 0
    }

    fun hasPort(): Boolean {
        return (flags and 0x40000000) != 0
    }

    fun hasCustomCache(): Boolean {
        return (flags and CUSTOM_CACHE) != 0
    }

    companion object {
        const val CUSTOM_CACHE = 0x20
    }
}
