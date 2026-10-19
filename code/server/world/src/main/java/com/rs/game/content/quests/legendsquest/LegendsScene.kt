package com.rs.game.content.quests.legendsquest

import com.rs.game.World
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

internal val LEGENDS_TOTEMS = listOf(Tile.of(2778, 2917, 0), Tile.of(2814, 2896, 0), Tile.of(2909, 2917, 0))
internal val SOURCE_BOULDERS = listOf(Tile.of(2371, 4693, 0), Tile.of(2385, 4690, 0), Tile.of(2391, 4679, 0))
internal val SOURCE_POOLS = listOf(Tile.of(2371, 4692, 0), Tile.of(2385, 4689, 0), Tile.of(2391, 4678, 0))
internal val ECHNED_SPAWN = Tile.of(2387, 4690, 0)
internal val GUARDIAN_SPAWNS = mapOf(Npc.npc_936 to Tile.of(2404, 4730, 0), Npc.npc_937 to Tile.of(2407, 4721, 0), Npc.npc_938 to Tile.of(2415, 4732, 0))
private val LEGENDS_NPCS = setOf(Npc.radimus_erkle, Npc.gujuo, Npc.ungadulu_good, Npc.viyeldi, Npc.npc_936, Npc.npc_937, Npc.npc_938, Npc.echned_zekin, Npc.boulder_legends, Npc.nezikchened)
private val GUILD_TOTEM = GameObject(Loc.lg_totem_pole_legends, ObjectShape.SCENERY_INTERACT, 0, Tile.of(2729, 3378, 0))

@ServerStartupEvent
fun mapLegendsScene() {
    LEGENDS_TOTEMS.forEach { World.spawnObject(GameObject(Loc.lg_totem_pole_evil, ObjectShape.SCENERY_INTERACT, 0, it)) }
    SOURCE_POOLS.forEach { World.spawnObject(GameObject(Loc.lgwaterpool, ObjectShape.SCENERY_INTERACT, 0, it), false) }
    World.addGroundItemForever(Item(Obj.viyeldihat, 1), Tile.of(2378, 4714, 0), 1)
    onLogin { (player) -> player.schedule { wait(2); refreshLegendsScene(player) } }
    onChunkEnter { event ->
        val player = event.player ?: return@onChunkEnter
        player.schedule { wait(2); refreshLegendsScene(player) }
    }
}

internal fun refreshLegendsScene(player: Player) {
    if (player.hasFinished() || player.isDead) return
    if (player.plane == 0 && player.mapChunkIds.contains(GUILD_TOTEM.tile.getChunkId())) {
        if (player.legendsStage >= TRAINING) player.packets.sendAddObject(GUILD_TOTEM)
        else player.packets.sendRemoveObject(GUILD_TOTEM)
    }
    SOURCE_POOLS.forEachIndexed { index, tile ->
        if (player.plane == tile.plane && player.mapChunkIds.contains(tile.getChunkId())) {
            val pool = GameObject(Loc.lgwaterpool, ObjectShape.SCENERY_INTERACT, 0, tile)
            if (player.legends.getI("boulders") and (1 shl index) != 0) player.packets.sendAddObject(pool)
            else player.packets.sendRemoveObject(pool)
        }
    }
    when {
        player.plane == 0 && player.x in 2710..2745 && player.y in 3340..3399 -> {
            val mainHall = player.legendsStage >= TRAINING
            ensureLegendsNpc(player, Npc.radimus_erkle, if (mainHall) Tile.of(2729, 3381, 0) else Tile.of(2724, 3367, 0))
        }
        player.plane == 0 && player.x in 2748..2815 && player.y in 9290..9348 && player.legendsStage >= RESCUE -> {
            ensureLegendsNpc(player, Npc.ungadulu_good, Tile.of(2793, 9329, 0))
            if (player.legends.getB("winchRope")) World.spawnObject(GameObject(Loc.lg_winchdown_rope, ObjectShape.SCENERY_INTERACT, 3, Tile.of(2759, 9329, 0)))
        }
        player.plane == 0 && player.x in 2368..2440 && player.y in 4670..4742 && player.legendsStage >= SOURCE -> {
            GUARDIAN_SPAWNS.forEach { (id, tile) -> ensureLegendsNpc(player, id, tile, true) }
            SOURCE_BOULDERS.forEachIndexed { index, tile ->
                if (player.legends.getI("boulders") and (1 shl index) == 0) ensureLegendsNpc(player, Npc.boulder_legends, tile)
            }
        }
    }
    restoreYommiTree(player)
}

internal fun ensureLegendsNpc(player: Player, id: Int, tile: Tile, guardian: Boolean = false): NPC? {
    if (player.plane != tile.plane || !player.mapChunkIds.contains(tile.getChunkId())) return null
    val nearby = World.getNPCsInChunkRange(tile.getChunkId(), 3)
    val existing = nearby.firstOrNull { it.id == id && !it.hasFinished() && !it.isDead && it is OwnedNPC && it.owner === player && (id == Npc.gujuo || it.tempAttribs.getI("legendsSpawnX") == tile.x && it.tempAttribs.getI("legendsSpawnY") == tile.y) }
    if (existing != null) {
        if (id == Npc.gujuo && !existing.withinDistance(player.tile, 14)) {
            existing.tele(player.legendsDestination(tile) ?: return null)
        }
        return existing
    }
    val destination = if (id == Npc.boulder_legends) {
        if (!World.floorFree(tile, 2)) return null
        tile
    } else player.legendsDestination(tile) ?: return null
    return (if (guardian) LegendsGuardian(player, id, destination, false) else LegendsCharacter(player, id, destination)).apply {
        if (id == Npc.echned_zekin || id == Npc.viyeldi) faceTile(player.tile)
        tempAttribs.setI("legendsSpawnX", tile.x)
        tempAttribs.setI("legendsSpawnY", tile.y)
    }
}

internal fun clearLegendsScene(player: Player) {
    World.NPCs.filter { it.id in LEGENDS_NPCS && it is OwnedNPC && it.owner === player }.forEach(NPC::finish)
    player.tempAttribs.removeO<NPC>("legendsFight")?.finish()
    clearYommiTree(player)
}
