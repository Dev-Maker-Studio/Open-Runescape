package com.rs.game.content.quests.creatureoffenkenstrain

import com.rs.game.World
import com.rs.game.model.entity.actions.EntityFollow
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.ownedNpc
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

internal val HEAD_GRAVE = Tile.of(3608, 3491, 0)
private val GARDEN = Tile.of(3551, 3565, 0)
private val GARDEN_TILES = listOf(GARDEN, GARDEN.transform(-1, 0, 0), GARDEN.transform(0, -2, 0))
private val CONDUCTOR_TILE = Tile.of(3548, 3536, 2)

@ServerStartupEvent
fun mapFenkenstrainScene() {
    World.addGroundItemForever(Item(Obj.fenk_brain, 1), Tile.of(3495, 3474, 0), 10)
    onLogin { (player) -> player.queueFenkenstrainScene() }
    onChunkEnter { event ->
        val player = event.player ?: return@onChunkEnter
        player.queueFenkenstrainScene()
    }
}

private fun Player.queueFenkenstrainScene() {
    schedule("fenkenstrainScene") {
        repeat(10) {
            wait(2)
            if (hasFinished()) return@schedule
            refreshFenkenstrainScene(this@queueFenkenstrainScene)
        }
    }
}

internal fun refreshFenkenstrainScene(player: Player) {
    if (player.hasFinished() || player.isDead) return
    player.ensureFenkNpc("doctor", Npc.fenk_fenkenstrain_model, if (player.fenkStage == COMPLETE) Tile.of(3548, 3553, 2) else Tile.of(3548, 3556, 0))
    player.ensureFenkNpc("gardener", Npc.fenk_gardener, GARDEN)
    player.ensureFenkNpc("monster", if (player.fenkStage >= TRUTH) Npc.fenk_creature_model_2 else Npc.fenk_creature_model,
        Tile.of(3549, 3553, 2), player.fenkStage >= TOWER)
    if (player.plane == 2 && player.mapChunkIds.contains(CONDUCTOR_TILE.getChunkId())) {
        player.packets.sendAddObject(GameObject(if (player.fenkStage >= REVIVED) Loc.fenk_conductor_repaired else Loc.fenk_conductor_broken, ObjectShape.SCENERY_INTERACT, 2, CONDUCTOR_TILE))
    }
    if (player.fenkStage == UNSTARTED && player.tempAttribs.getB("fenkFollowing")) stopGardenerEscort(player)
}

private fun Player.ensureFenkNpc(key: String, id: Int, tile: Tile, visible: Boolean = true) {
    val name = "fenkNpc$key"
    val existing = tempAttribs.getO<OwnedNPC>(name)
    if (key == "gardener" && tempAttribs.getB("fenkFollowing")) {
        if (existing != null && !existing.hasFinished()) return
        tempAttribs.setB("fenkFollowing", false)
    }
    if (existing != null && !existing.hasFinished() && existing.id == id && existing.plane == tile.plane && visible && (key != "gardener" || existing.tile in GARDEN_TILES)) return
    existing?.finish()
    tempAttribs.removeO<OwnedNPC>(name)
    if (!visible || plane != tile.plane || this.tile.distanceTo(tile) > 14) return
    val destinations = if (key == "gardener") GARDEN_TILES else listOf(tile) + (1..3).flatMap { radius ->
        (-radius..radius).flatMap { x -> (-radius..radius).map { y -> tile.transform(x, y, 0) } }
    }
    val destination = destinations.firstOrNull { World.floorAndWallsFree(it, 1) } ?: return
    tempAttribs.setO<OwnedNPC>(name, ownedNpc(this, id, destination) { if (key != "doctor") setRandomWalk(false) })
}

internal fun Player.startGardenerEscort(npc: NPC) {
    if (tempAttribs.getB("fenkFollowing") || npc.hasFinished()) return
    endConversation()
    tempAttribs.setB("fenkFollowing", true)
    (npc as? OwnedNPC)?.isAutoDespawnAtDistance = false
    npc.follow(this)
    gardenerDirections(npc)
    npc.schedule {
        for (tick in 0 until 1000) {
            wait(1)
            if (!tempAttribs.getB("fenkFollowing") || hasFinished() || npc.hasFinished()) break
            if (isDead || moveTile != null || hasTeleported()) break
            if (fenkStage !in BODY_PARTS until CONDUCTOR || plane != 0 || x !in 3520..3635 || y !in 3450..3585) {
                if (!hasFinished()) {
                    npc.forceTalk("Oi'm afraid you've gone well off the beaten track..")
                    wait(2)
                    if (!npc.hasFinished()) npc.forceTalk("Fare thee well - oi must return to me' garden.")
                    wait(2)
                }
                break
            }
            if (tile.distanceTo(HEAD_GRAVE) <= 4) {
                npc.forceTalk("This is the place where I met me' maker.")
                wait(2)
                break
            }
            if (tile.distanceTo(npc.tile) > 12) npc.teleToEscortOwner(this@startGardenerEscort)
            if (npc.actionManager.action !is EntityFollow) npc.follow(this@startGardenerEscort)
        }
        if (tempAttribs.getO<OwnedNPC>("fenkNpcgardener") === npc) stopGardenerEscort(this@startGardenerEscort)
    }
}

private fun NPC.teleToEscortOwner(player: Player) {
    player.getNearestTeleTile(this)?.let { tele(it) }
}

internal fun stopGardenerEscort(player: Player) {
    player.tempAttribs.setB("fenkFollowing", false)
    val npc = player.tempAttribs.getO<OwnedNPC>("fenkNpcgardener") ?: return
    npc.finish()
    player.tempAttribs.removeO<OwnedNPC>("fenkNpcgardener")
    player.ensureFenkNpc("gardener", Npc.fenk_gardener, GARDEN)
}

internal fun Player.gardenerDirections(npc: NPC) {
    val text = when {
        tile.distanceTo(HEAD_GRAVE) <= 4 -> "This is the place where I met me' maker."
        y >= 3520 -> "You need to head off to them Haunted Woods, mate."
        else -> {
            val northSouth = when { y < HEAD_GRAVE.y - 2 -> "north"; y > HEAD_GRAVE.y + 2 -> "south"; else -> "" }
            val eastWest = when { x < HEAD_GRAVE.x - 2 -> "east"; x > HEAD_GRAVE.x + 2 -> "west"; else -> "" }
            "Go ${northSouth + eastWest}, mate."
        }
    }
    npc.forceTalk(text)
    if (tile.distanceTo(HEAD_GRAVE) <= 4) npc.schedule {
        wait(2)
        if (tempAttribs.getO<OwnedNPC>("fenkNpcgardener") === npc) stopGardenerEscort(this@gardenerDirections)
    }
}
