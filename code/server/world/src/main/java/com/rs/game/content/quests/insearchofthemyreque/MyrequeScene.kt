package com.rs.game.content.quests.insearchofthemyreque

import com.rs.game.World
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.ownedNpc
import org.darkan.core.Logger
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import kotlin.random.Random
import world.gregs.voidps.type.Tile

internal val MYREQUE_ROOM = Tile.of(3507, 9838, 0)
private val MYREQUE_CHARACTERS = linkedMapOf(
    Npc.route_veliaf_hurtz to Tile.of(3504, 9840, 0),
    Npc.route_sani_piliu to Tile.of(3503, 9836, 0),
    Npc.route_harold_evans to Tile.of(3505, 9836, 0),
    Npc.npc_1572 to Tile.of(3509, 9843, 0),
    Npc.npc_1573 to Tile.of(3512, 9842, 0),
    Npc.route_ivan_strom to Tile.of(3512, 9835, 0)
)
internal val BRIDGE_TILES = listOf(Tile.of(3502, 3428, 0), Tile.of(3502, 3429, 0), Tile.of(3502, 3430, 0))

@ServerStartupEvent
fun mapMyrequeScene() {
    instantiateNpc(Npc.npc_2020) { id, tile -> NPC(id, tile).apply { setRandomWalk(false) } }
    onLogin { (player) -> player.scheduleMyrequeScene() }
    onChunkEnter { event -> event.player?.scheduleMyrequeScene() }
}

internal fun Player.inMyrequeRoom() = plane == 0 && x in 3502..3515 && y in 9831..9845

internal fun refreshMyrequeScene(player: Player) {
    if (player.hasFinished() || player.isDead) return
    if (player.plane == 1 && player.x == 3502 && player.y in 3427..3430 && player.myrequeStage >= BOAT) player.tele(Tile.of(player.x, player.y, 0))
    if (player.inMyrequeRoom() && player.myrequeStage >= HIDEOUT && player.musicsManager.playingMusicId != Midi.stillness) player.musicsManager.playSongAndUnlock(Midi.stillness)
    player.tempAttribs.removeO<OwnedNPC>("myrequeNpctavern")?.finish()
    player.ensureMyrequeCharacter("guard", Npc.npc_1568, Tile.of(3506, 3448, 0))
    MYREQUE_CHARACTERS.forEach { (id, tile) ->
        val dead = id in setOf(Npc.route_sani_piliu, Npc.route_harold_evans) && player.myrequeStage >= HELLHOUND && (!player.tempAttribs.getB("myrequeAmbush") || player.tempAttribs.getB("myrequeKilled$id"))
        player.ensureMyrequeCharacter("member$id", id, tile, player.myrequeStage >= HIDEOUT && !dead)
    }
    if (!player.tempAttribs.getB("myrequeAmbush") && player.myrequeStage == HELLHOUND && player.inMyrequeRoom()) spawnMyrequeHellhound(player)
}

private fun Player.scheduleMyrequeScene() {
    schedule("myrequeScene") {
        wait(2)
        while (!hasFinished() && !isDead) {
            refreshMyrequeScene(this@scheduleMyrequeScene)
            if (!(plane in 0..1 && x in 3490..3515 && y in 3415..3460 || inMyrequeRoom())) break
            wait(5)
        }
    }
}

private fun Player.ensureMyrequeCharacter(key: String, id: Int, tile: Tile, visible: Boolean = true): OwnedNPC? {
    val name = "myrequeNpc$key"
    val existing = tempAttribs.getO<OwnedNPC>(name)
    if (existing != null && !existing.hasFinished() && existing.id == id && visible && plane == tile.plane && this.tile.distanceTo(tile) <= 15) return existing
    existing?.finish()
    tempAttribs.removeO<OwnedNPC>(name)
    if (!visible || plane != tile.plane || this.tile.distanceTo(tile) > 14) return null
    val destination = myrequeWalkable(tile, 1) ?: return null
    return (if (id in MYREQUE_CHARACTERS) MyrequeMember(this, id, destination) else ownedNpc(this, id, destination) { setRandomWalk(false) })
        .also { tempAttribs.setO<OwnedNPC>(name, it) }
}

private class MyrequeMember(owner: Player, id: Int, tile: Tile) : OwnedNPC(owner, id, tile, true) {
    init { faceTile(MYREQUE_ROOM) }

    override fun processNPC() {
        if (owner.tempAttribs.getB("myrequeAmbush") || owner.myrequeStage == HELLHOUND) resetWalkSteps()
        super.processNPC()
    }

    override fun faceInteracting(player: Player) {
        resetWalkSteps()
        super.faceInteracting(player)
    }

    override fun stopFacingInteracting(player: Player) {
        super.stopFacingInteracting(player)
        if (lastFaceEntity < 0 && !owner.tempAttribs.getB("myrequeAmbush")) faceTile(MYREQUE_ROOM)
    }

    override fun randomWalk() {
        if (!shouldRandomWalk() || hasWalkSteps() || lastFaceEntity >= 0 || owner.tempAttribs.getB("myrequeAmbush") || owner.myrequeStage == HELLHOUND || Random.nextInt(10) != 0) return
        val destination = tile.transform(Random.nextInt(-1, 2), Random.nextInt(-1, 2), 0)
        if (destination.x in 3502..3515 && destination.y in 9831..9845 && destination.distanceTo(respawnTile) <= 2)
            addWalkSteps(destination.x, destination.y, 1, true)
    }
}

internal fun myrequeWalkable(tile: Tile, size: Int): Tile? =
    (listOf(tile) + (1..3).flatMap { radius -> (-radius..radius).flatMap { x -> (-radius..radius).map { y -> tile.transform(x, y, 0) } } })
        .firstOrNull { World.floorAndWallsFree(it, size) }

internal fun startMyrequeAmbush(player: Player) {
    if (player.myrequeStage != INTRODUCTIONS || !player.metAllMyreque() || !player.hasMyrequeWeapons() || !player.inMyrequeRoom() || player.tempAttribs.getB("myrequeAmbush")) return
    player.tempAttribs.setB("myrequeAmbush", true)
    WEAPON_LIST.forEach { (id, amount) -> player.inventory.deleteItem(id, amount) }
    player.advanceMyreque(HELLHOUND)
    player.stopAll()
    player.lock(50)
    var vanstrom: OwnedNPC? = null
    WorldTasks.scheduleTimer { tick ->
        if (player.hasFinished() || player.isDead || player.myrequeStage != HELLHOUND || !player.inMyrequeRoom()) {
            finishMyrequeAmbush(player, vanstrom)
            return@scheduleTimer false
        }
        try {
            when (tick) {
                0 -> {
                    player.packets.sendCameraPos(MYREQUE_ROOM.transform(5, -5, 0), 900)
                    player.packets.sendCameraLook(MYREQUE_ROOM, 300)
                    player.tempAttribs.getO<OwnedNPC>("myrequeNpcmember${Npc.route_veliaf_hurtz}")?.forceTalk("Hey, what's that mist coming in through the door? WE'VE A VAMPYRE IN THE ROOM!")
                    player.sendMessage("You see a shifting mist enter the room!")
                    val spawn = myrequeWalkable(MYREQUE_ROOM.transform(0, -2, 0), 3)
                    if (spawn == null) { finishMyrequeAmbush(player, vanstrom); return@scheduleTimer false }
                    vanstrom = ownedNpc(player, Npc.route_vanstrom_vampire_misty, spawn) { setRandomWalk(false) }
                }
                3 -> {
                    vanstrom?.transformIntoNPC(Npc.route_vanstrom_klause_normal)
                    vanstrom?.forceTalk("Ha, ha, ha... you took me straight to them!")
                    player.sendMessage("Vanstrom: Ha, ha, ha... you took me straight to them!")
                }
                7 -> vanstrom?.forceTalk("The little dears are going to wonder which side you're on!")
                11 -> player.tempAttribs.getO<OwnedNPC>("myrequeNpcmember${Npc.route_sani_piliu}")?.forceTalk("It's Vanstrom! We're dead!")
                14 -> {
                    vanstrom?.forceTalk("Quite right...")
                    vanstrom?.anim(Seq.human_caststrike)
                    player.tempAttribs.getO<OwnedNPC>("myrequeNpcmember${Npc.route_sani_piliu}")?.apply { spotAnim(Spotanim.spell_blood_burst_impact); anim(Seq.human_death) }
                }
                17 -> {
                    player.tempAttribs.setB("myrequeKilled${Npc.route_sani_piliu}", true)
                    player.tempAttribs.getO<OwnedNPC>("myrequeNpcmember${Npc.route_sani_piliu}")?.finish()
                    vanstrom?.forceTalk("Sorry, Harold, you too!")
                    vanstrom?.anim(Seq.human_caststrike)
                    player.tempAttribs.getO<OwnedNPC>("myrequeNpcmember${Npc.route_harold_evans}")?.apply { spotAnim(Spotanim.spell_blood_burst_impact); anim(Seq.human_death) }
                }
                20 -> {
                    player.tempAttribs.setB("myrequeKilled${Npc.route_harold_evans}", true)
                    player.tempAttribs.getO<OwnedNPC>("myrequeNpcmember${Npc.route_harold_evans}")?.finish()
                    vanstrom?.transformIntoNPC(Npc.route_vanstrom_klause_vamp_wingsgrow)
                    vanstrom?.anim(Seq.vanstrom_klause_wings_unfold)
                }
                23 -> { vanstrom?.transformIntoNPC(Npc.route_vanstrom_klause_vamp); vanstrom?.forceTalk("And now I'm going to finish the rest of them off.") }
                27 -> { vanstrom?.forceTalk("With my little pet!"); vanstrom?.anim(Seq.vanstrom_klause_wings_summon) }
                30 -> { vanstrom?.anim(Seq.vanstrom_klause_wings_launch); vanstrom?.forceTalk("Ha ha ha ha haaaaaa!") }
                33 -> finishMyrequeAmbush(player, vanstrom)
            }
            tick < 33
        } catch (error: Exception) {
            finishMyrequeAmbush(player, vanstrom)
            Logger.handle(InSearchOfTheMyreque::class.java, "myrequeAmbush", error)
            false
        }
    }
}

private fun finishMyrequeAmbush(player: Player, vanstrom: OwnedNPC?) {
    vanstrom?.finish()
    player.tempAttribs.setB("myrequeAmbush", false)
    listOf(Npc.route_sani_piliu, Npc.route_harold_evans).forEach { player.tempAttribs.setB("myrequeKilled$it", false) }
    if (player.hasFinished()) return
    player.packets.sendResetCamera()
    if (!player.isDead) player.unlock()
    refreshMyrequeScene(player)
}
