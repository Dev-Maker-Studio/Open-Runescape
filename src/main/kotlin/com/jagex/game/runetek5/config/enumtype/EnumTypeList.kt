package com.jagex.game.runetek5.config.enumtype

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class EnumTypeList(
    @JvmField val enumTypeResource: Js5ResourceProvider
) {

    @JvmField val cache: LRUCache = LRUCache(128)

    init {
        if (this.enumTypeResource != null) {
            val archiveCount = this.enumTypeResource.getArchiveCount() - 1
            SharedConfigsType.ENUMS.archiveFileCapacity()
            this.enumTypeResource.getArchiveFileCount(archiveCount)
        }
    }

    fun list(id: Int): EnumType {
        var enumType: EnumType?
        synchronized(this.cache) {
            enumType = this.cache.get(id.toLong()) as EnumType?
        }
        if (enumType != null) {
            return enumType!!
        }
        val data = this.enumTypeResource.getFile(SharedConfigsType.ENUMS.archiveId(id), SharedConfigsType.ENUMS.archiveFileId(id))
        enumType = EnumType()
        if (data != null) {
            enumType!!.decode(Packet(data))
        }
        synchronized(this.cache) {
            this.cache.put(enumType, id.toLong())
            return enumType!!
        }
    }
}
