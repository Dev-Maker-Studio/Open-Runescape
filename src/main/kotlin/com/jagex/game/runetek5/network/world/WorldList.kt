package com.jagex.game.runetek5.network.world

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.utils.Utils

object WorldList {
    fun decode(refresh: Boolean, data: ByteArray) {
        if (WorldMeta.WORLD_LIST_BUFFER == null) WorldMeta.WORLD_LIST_BUFFER = Packet(20000)
        WorldMeta.WORLD_LIST_BUFFER!!.writeBytes(data, 0, data.size)
        if (refresh) {
            WorldDescriptor.decodeWorldList(WorldMeta.WORLD_LIST_BUFFER!!.buffer)
            WorldConnection.WORLD_DESCRIPTORS_BYID = arrayOfNulls(WorldMeta.WORLD_LIST_SIZE)
            var worldNumber = 0
            for (worldIndex in WorldDescriptor.WORLD_LIST_START..WorldMeta.WORLD_LIST_SIZEPLUS1) {
                val world: WorldDescriptor? = WorldDescriptor.getWorld(worldIndex)
                if (world != null) WorldConnection.WORLD_DESCRIPTORS_BYID[worldNumber++] = world
            }
            WorldConnection.requestingWorldList = false
            WorldConnection.worldListReceivedTime = Utils.time()
            WorldMeta.WORLD_LIST_BUFFER = null
        }
    }
}