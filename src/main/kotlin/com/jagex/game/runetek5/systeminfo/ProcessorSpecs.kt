package com.jagex.game.runetek5.systeminfo

class ProcessorSpecs(
    @JvmField val maxMemory: Int,
    @JvmField val processorCount: Int,
    @JvmField val isArm: Boolean
) {
    fun isARM(): Boolean {
        return isArm
    }

    fun getProcessorCount(): Int {
        return processorCount
    }

    fun getMaxMemory(): Int {
        return maxMemory
    }
}
