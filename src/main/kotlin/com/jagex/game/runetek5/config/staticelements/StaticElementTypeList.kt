package com.jagex.game.runetek5.config.staticelements

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.js5.Js5ResourceProvider

object StaticElementTypeList {
    @JvmStatic
    fun list(elementsResource: Js5ResourceProvider, fileName: String, positioned: Boolean): StaticElements {
        val archiveId = elementsResource.getArchiveId(fileName)
        if (archiveId == -1) {
            return StaticElements(0)
        }
        val fileIds = elementsResource.getValidFileIds(archiveId)
        val elements = StaticElements(fileIds!!.size)
        var elementIndex = 0
        var fileIndex = 0
        while (elementIndex < elements.elementCount) {
            val stream = Packet(elementsResource.getFile(archiveId, fileIds[fileIndex++])!!)
            val bitpackedPlacement = stream.readInt()
            val categoryId = stream.readUShort()
            val allowSkip = stream.readUByte()
            if (!positioned && allowSkip == 1) {
                elements.elementCount--
            } else {
                elements.bitpackedPlacement[elementIndex] = bitpackedPlacement
                elements.mapCategoryIds[elementIndex] = categoryId
                elementIndex++
            }
        }
        return elements
    }
}
