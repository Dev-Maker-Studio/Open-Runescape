package com.rs.game.content.quests.tearsofguthix

import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onItemEquip
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Bas
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile
import kotlin.math.ceil
import kotlin.math.floor

private const val WEEKLY_ENTRY = "tearsOfGuthix"
private const val ENTRY_XP = "tearsEntryXp"
private const val ENTRY_QP = "tearsEntryQp"
internal val TEARS_EXIT = Tile.of(3251, 9517, 2)
private val TEARS_ENTRY = Tile.of(3256, 9517, 2)
private val WALLS = listOf(
    TearWall(Tile.of(3257, 9514, 2), 1, Loc.tog_weeping_wall_bad_r),
    TearWall(Tile.of(3258, 9514, 2), 1, Loc.tog_weeping_wall_bad_l),
    TearWall(Tile.of(3259, 9514, 2), 1, Loc.tog_weeping_wall_bad_r),
    TearWall(Tile.of(3257, 9520, 2), 3, Loc.tog_weeping_wall_good_l),
    TearWall(Tile.of(3258, 9520, 2), 3, Loc.tog_weeping_wall_good_r),
    TearWall(Tile.of(3259, 9520, 2), 3, Loc.tog_weeping_wall_good_l),
    TearWall(Tile.of(3261, 9516, 2), 0, Loc.tog_weeping_wall_off_r),
    TearWall(Tile.of(3261, 9517, 2), 0, Loc.tog_weeping_wall_off_l),
    TearWall(Tile.of(3261, 9518, 2), 0, Loc.tog_weeping_wall_off_r)
)
private data class TearWall(val tile: Tile, val rotation: Int, val original: Int) {
    val left get() = original in setOf(Loc.tog_weeping_wall_good_l, Loc.tog_weeping_wall_bad_l, Loc.tog_weeping_wall_off_l)
    fun objectFor(colour: Int) = GameObject(when (colour) {
        1 -> if (left) Loc.tog_weeping_wall_good_l else Loc.tog_weeping_wall_good_r
        -1 -> if (left) Loc.tog_weeping_wall_bad_l else Loc.tog_weeping_wall_bad_r
        else -> if (left) Loc.tog_weeping_wall_off_l else Loc.tog_weeping_wall_off_r
    }, ObjectShape.STRAIGHT_INSIDE_WALL_DEC, rotation, tile)
}

@ServerStartupEvent
fun mapTearsOfGuthixActivity() {
    onItemEquip(Obj.tog_bowl) { event ->
        if (event.dequip() && event.player.controllerManager.controller is TearsOfGuthixController) event.cancel()
    }
    onLogin { (player) -> player.recoverTearsCave() }
    onChunkEnter { event -> event.player?.recoverTearsCave() }
}

private fun Player.recoverTearsCave() {
    if (inTearsRoom() && controllerManager.controller !is TearsOfGuthixController) {
        equipment.itemsCopy.indices.filter { equipment.getId(it) == Obj.tog_bowl }.forEach { equipment.setSlot(it, null) }
        appearance.setBAS(-1)
        vars.setVarBit(Varbit.tog_minigame_collecting, 0)
        interfaceManager.removeOverlay()
        tele(TEARS_EXIT)
    }
}

internal fun Player.inTearsRoom() = plane == 2 && x in 3253..3261 && y in 9515..9519

internal fun Player.handsFreeForTears(): Boolean {
    if (equipment.weaponId != -1 || equipment.shieldId != -1) {
        simpleDialogue("If you want to collect the Tears of Guthix, you should have both hands free to hold your bowl.")
        return false
    }
    if (familiar != null || pet != null) {
        simpleDialogue("A follower will distract you too much from collecting Tears of Guthix. You should dismiss it first.")
        return false
    }
    return true
}

internal fun Player.tearsAvailability(): String? {
    if (getWeeklyI(WEEKLY_ENTRY) != 0) return "You may collect the Tears of Guthix again after the weekly reset."
    val entryXp = getL(ENTRY_XP)
    if (entryXp == -1L) return null
    if (questManager.questPoints <= getI(ENTRY_QP, 0) && skills.totalXp - entryXp < 100000L)
        return "You must have gained a Quest Point or 100,000 total experience to enter Juna's cavern."
    return null
}

internal fun Player.enterTears() {
    if (tearsStage != COMPLETE || isLocked || !inTearsCavern() || controllerManager.controller != null || !handsFreeForTears()) return
    tearsAvailability()?.let { simpleDialogue(it); return }
    stopAll()
    controllerManager.startController(TearsOfGuthixController())
}

class TearsOfGuthixController : Controller() {
    private var remaining = 0
    private var duration = 0
    private var tears = 0
    private var elapsed = 0
    private var collecting = -1
    private var collectionTile: Tile? = null
    private var collectionDelay = 0
    private var running = false
    private var previousRun = false
    private var previousBas = -1
    private var positions = IntArray(6)
    private var nextChanges = IntArray(6)

    override fun start() {
        if (player.tearsStage != COMPLETE || !player.inTearsCavern() || !player.handsFreeForTears()) { removeController(); player.tele(TEARS_EXIT); return }
        running = true
        player.tele(TEARS_ENTRY)
        previousRun = player.run
        previousBas = player.bas
        player.run = false
        duration = player.questManager.questPoints.coerceIn(1, 300)
        remaining = duration
        player.setWeeklyI(WEEKLY_ENTRY, 1)
        player.set(ENTRY_XP, player.skills.totalXp)
        player.set(ENTRY_QP, player.questManager.questPoints)
        player.equipment.setSlot(Equipment.WEAPON, Item(Obj.tog_bowl, 1))
        player.appearance.setBAS(Bas.tog_bowl)
        player.vars.setVarBit(Varbit.tog_minigame_collecting, 1)
        player.interfaceManager.sendOverlay(Interface.tog_water_bowl)
        positions = WALLS.indices.shuffled().take(6).toIntArray()
        nextChanges = IntArray(6) { 9 + it }
        WALLS.indices.forEach(::refreshWall)
        refreshInterface()
        player.musicsManager.unlockMusic(Midi.midi_398)
        player.musicsManager.playSpecificAmbientSong(Midi.midi_398, true)
        player.sendMessage("Collect the blue tears. Green tears will reduce the amount in your bowl.")
    }

    override fun process() {
        if (!running) return
        if (!player.inTearsRoom()) {
            if (elapsed == 0 && player.moveTile == TEARS_ENTRY) return
            finish(false)
            return
        }
        player.run = false
        for (stream in positions.indices) if (elapsed >= nextChanges[stream]) {
            val old = positions[stream]
            val empty = WALLS.indices.filter { it !in positions }.random()
            positions[stream] = empty
            nextChanges[stream] = elapsed + 9 + (0..7).random()
            refreshWall(old)
            refreshWall(empty)
        }
        if (collecting >= 0 && player.tile == collectionTile && !player.hasWalkSteps()) {
            if (collectionDelay > 0) collectionDelay--
            else {
                val stream = positions.indexOf(collecting)
                tears = (tears + when { stream < 0 -> 0; stream < 3 -> 1; else -> -1 }).coerceIn(0, 511)
            }
        } else collecting = -1
        elapsed++
        remaining--
        refreshInterface()
        if (remaining <= 0) finish(true)
    }

    override fun processObjectClick1(obj: GameObject): Boolean {
        if (obj.id != Loc.tog_weepingwall) {
            if (obj.id in setOf(Loc.loc_6657, Loc.loc_31302, Loc.loc_31303)) finish(true)
            return false
        }
        val wall = WALLS.indexOfFirst { it.tile == obj.tile }
        if (wall >= 0) {
            player.stopAll()
            collecting = wall
            collectionTile = player.tile
            collectionDelay = 2
            player.setNextFaceTile(obj.tile)
            player.anim(Seq.seq_2043)
        }
        return false
    }

    override fun canMove(dir: Direction): Boolean { collecting = -1; player.anim(-1); return true }
    override fun canEquip(slotId: Int, itemId: Int) = false
    override fun canDropItem(item: Item) = false
    override fun canSummonFamiliar() = false
    override fun canAttack(target: Entity) = false
    override fun canTrade() = false
    override fun processItemOnObject(obj: GameObject, item: Item) = false
    override fun processItemOnNPC(npc: NPC, item: Item) = false
    override fun logout(): Boolean { finish(false); return true }
    override fun login(): Boolean { finish(false); return true }
    override fun onTeleported(type: TeleType) { if (!player.inTearsRoom()) finish(false) }
    override fun sendDeath(): Boolean { finish(false); return true }
    override fun forceClose() { finish(false) }

    private fun refreshWall(index: Int) {
        val stream = positions.indexOf(index)
        player.packets.sendAddObject(WALLS[index].objectFor(when { stream < 0 -> 0; stream < 3 -> 1; else -> -1 }))
    }

    private fun refreshInterface() {
        player.vars.setVarBit(Varbit.tog_tears_collected, tears)
        player.vars.setVarBit(Varbit.tog_time_left_tenths, ceil(remaining * 10.0 / duration.coerceAtLeast(1)).toInt().coerceIn(0, 10))
        player.packets.sendRunScript(Cs2.tog_update_interface)
    }

    private fun finish(reward: Boolean) {
        if (!running) { removeController(); if (player.inTearsRoom() || player.moveTile == TEARS_ENTRY) player.tele(TEARS_EXIT); return }
        running = false
        removeController()
        player.stopAll()
        WALLS.forEach { wall -> player.packets.sendAddObject(GameObject(wall.original, ObjectShape.STRAIGHT_INSIDE_WALL_DEC, wall.rotation, wall.tile)) }
        player.interfaceManager.removeOverlay()
        player.vars.setVarBit(Varbit.tog_minigame_collecting, 0)
        player.vars.setVarBit(Varbit.tog_tears_collected, 0)
        player.vars.setVarBit(Varbit.tog_time_left_tenths, 0)
        if (player.equipment.weaponId == Obj.tog_bowl) player.equipment.setSlot(Equipment.WEAPON, null)
        player.appearance.setBAS(previousBas)
        player.run = previousRun
        player.anim(-1)
        if (player.inTearsRoom() || player.moveTile == TEARS_ENTRY) player.tele(TEARS_EXIT)
        if (reward && tears > 0) {
            val skill = Constants.SKILL_NAME.indices.minBy { player.skills.getXp(it) }
            val xpPerTear = minOf((100 + floor(player.skills.getXp(skill) / 27)) / 10, 60.0)
            player.anim(Seq.seq_2045)
            val xp = player.skills.addXpLamp(skill, tears * xpPerTear)
            player.sendMessage("You drink the Tears of Guthix and gain ${xp.toInt()} ${Constants.SKILL_NAME[skill]} experience.")
        }
        player.set(ENTRY_XP, player.skills.totalXp)
        player.set(ENTRY_QP, player.questManager.questPoints)
        player.musicsManager.playSpecificAmbientSong(Midi.midi_397, true)
    }
}
