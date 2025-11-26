package com.jagex.game.runetek5.cache.node.impl

import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.sequence.Projectile
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.network.packet.UpdateZonePacket
import com.jagex.game.runetek5.scene.MapRegion

class ProjectileSpawnRequest(
    @JvmField val projectile: Projectile
) : CacheableNode() {

    companion object {
        @JvmStatic
        fun create(
            spotAnimId: Int, source: Int, lockOn: Int, basOffIdx: Int, startHeight: Int, endHeight: Int,
            localX: Int, localY: Int, absX: Int, absY: Int, startTime: Int, endTime: Int, angle: Int, slope: Int, useFloorHeight: Boolean
        ) {
            var adjustedStartHeight = startHeight
            if (source != 0 && basOffIdx != -1) {
                var entity: Any? = null
                if (source < 0) {
                    val playerId = -source - 1
                    entity = if (playerId == PlayerUpdate.myPlayerIndex) {
                        PlayerUpdate.MY_PLAYER
                    } else {
                        PlayerUpdate.players[playerId]
                    }
                } else {
                    val npcIndex = source - 1
                    val npcObjectNode = client.NPC_MAP.get(npcIndex.toLong())
                    if (npcObjectNode != null) {
                        entity = npcObjectNode.value
                    }
                }
                if (entity != null) {
                    val basType = (entity as PathingEntity).getBastype()
                    if (basType.objVerticeTransformations != null && basType.objVerticeTransformations!![basOffIdx] != null) {
                        adjustedStartHeight -= basType.objVerticeTransformations!![basOffIdx]!![1]
                    }
                    if (basType.projectionOffset != null && basType.projectionOffset!![basOffIdx] != null) {
                        adjustedStartHeight -= basType.projectionOffset!![basOffIdx]!![1]
                    }
                }
            }
            val projectile = Projectile(
                Resource.mapRegion!!.getScene(), spotAnimId, UpdateZonePacket.UPDATE_ZONE_PLANE, UpdateZonePacket.UPDATE_ZONE_PLANE,
                localX, localY, adjustedStartHeight, startTime + client.cycles, endTime + client.cycles, angle, slope, source, lockOn, endHeight, useFloorHeight, basOffIdx
            )
            projectile.start(absX, absY, MapRegion.tileheightForAbsolutePosition(absX, absY, UpdateZonePacket.UPDATE_ZONE_PLANE) - endHeight, startTime + client.cycles)
            client.PROJECTILES.addFirst(ProjectileSpawnRequest(projectile))
        }
    }
}
