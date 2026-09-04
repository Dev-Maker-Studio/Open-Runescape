package com.jagex.game.runetek5.buffer

interface NativeBuffer {
    fun unlock()
    fun getCapacity(): Int
    fun getDataStorePointer(offset: Int, unused: Int): Long
    fun upload(offset: Int, size: Int, dataPointer: Long): Boolean
}
