package com.jagex.game.runetek5.systeminfo

class RawCpuInfo(
    @JvmField val cpuIdLeafNum: Int,
    @JvmField val resultEAX: Int,
    @JvmField val resultEBX: Int,
    @JvmField val resultECX: Int,
    @JvmField val resultEDX: Int
) {
    override fun hashCode(): Int {
        return cpuIdLeafNum
    }
}
