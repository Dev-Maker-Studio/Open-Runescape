package com.jagex.game.runetek5.cache.node.impl;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.sequence.Projectile;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.network.packet.UpdateZonePacket;
import com.jagex.game.runetek5.scene.MapRegion;

public class ProjectileSpawnRequest extends CacheableNode {

    public final Projectile projectile;

    public ProjectileSpawnRequest(Projectile projectile) {
        this.projectile = projectile;
    }

    public static void create(int spotAnimId, int source, int lockOn, int basOffIdx, int startHeight, int endHeight,
                              int localX, int localY, int absX, int absY, int startTime, int endTime, int angle, int slope, boolean useFloorHeight) {
        if (source != 0 && basOffIdx != -1) {
            Object entity = null;
            if (source < 0) {
                int playerId = -source - 1;
                if (playerId == PlayerUpdate.myPlayerIndex) {
                    entity = PlayerUpdate.MY_PLAYER;
                } else {
                    entity = PlayerUpdate.players[playerId];
                }
            } else {
                int npcIndex = source - 1;
                ObjectNode npcObjectNode = client.NPC_MAP.get(npcIndex);
                if (npcObjectNode != null) {
                    entity = npcObjectNode.value;
                }
            }
            if (entity != null) {
                BasType basType = ((PathingEntity) entity).getBastype();
                if (basType.objVerticeTransformations != null && basType.objVerticeTransformations[basOffIdx] != null) {
                    startHeight -= basType.objVerticeTransformations[basOffIdx][1];
                }
                if (basType.projectionOffset != null && basType.projectionOffset[basOffIdx] != null) {
                    startHeight -= basType.projectionOffset[basOffIdx][1];
                }
            }
        }
        Projectile projectile = new Projectile(Resource.mapRegion.getScene(), spotAnimId, UpdateZonePacket.UPDATE_ZONE_PLANE, UpdateZonePacket.UPDATE_ZONE_PLANE, localX, localY, startHeight, startTime + client.cycles, endTime + client.cycles, angle, slope, source, lockOn, endHeight, useFloorHeight, basOffIdx);
        projectile.start(absX, absY, MapRegion.tileheightForAbsolutePosition(absX, absY, UpdateZonePacket.UPDATE_ZONE_PLANE) - endHeight, startTime + client.cycles);
        client.PROJECTILES.addFirst(new ProjectileSpawnRequest(projectile));
    }
}
