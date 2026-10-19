package com.jagex.game.runetek5.config.struct

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class StructTypeList(
    @JvmField val structProvider: Js5ResourceProvider
) {

    @JvmField val totalStructCount: Int = structProvider.getArchiveFileCount(SharedConfigsType.STRUCTS.id)
    @JvmField val cache: Array<StructType> = Array(totalStructCount) { StructType() }

    init {
        for (id in 0 until this.totalStructCount) {
            val data: ByteArray? = synchronized(this.structProvider) {
                this.structProvider.getFile(SharedConfigsType.STRUCTS.id, id)
            }

            val structType = StructType()
            if (data != null) {
                structType.decode(Packet(data))
            }

            this.cache[id] = structType
        }
    }

    fun getStruct(id: Int): StructType {
        return if (id < 0) StructType() else this.cache[id]
    }
}
