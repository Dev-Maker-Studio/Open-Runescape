package com.rs.game.content.skills.firemaking

import com.rs.game.World
import com.rs.game.content.skills.woodcutting.Hatchet
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.model.gameobject.OwnedObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onXpDrop
import com.rs.plugin.kts.ownedNpc
import com.rs.utils.DropSets
import com.rs.utils.drop.DropTable
import org.darkan.core.Constants
import org.darkan.core.Logger
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

private const val PYRE_LEVEL = 11
private const val LIGHT_XP = 40.0
private const val SPIRIT_LIFE = 12
private const val PYRE_LIFE = 50
private const val PRAYER_BONUS_BONES = 2
private const val PRAYER_BONUS_MAX = 6
private const val PRAYER_BONUS_MULTIPLIER = 3.0
private const val BONUS_KEY = "barbarianPyrePrayerBonus"
private const val FEROCIOUS_SPIRIT_KEY = "barbarianPyreFerociousSpirit"
private const val CREMATE_OPTION = "Cremate"

private enum class PyreStage(val loc: Int) {
    EMPTY(Loc.brut_pyre_boat_station_unused),
    STAGE1(Loc.brut_pyre_boat_station_stage1),
    STAGE2(Loc.brut_pyre_boat_station_stage2),
    STAGE3(Loc.brut_pyre_boat_station_stage3),
    BONES(Loc.brut_pyre_boat_station_stage3_bones),
    BURNING(Loc.brut_pyre_boat_station_on_fire),
    LAUNCHED(Loc.brut_pyre_boat_station_launched),
    READY(Loc.brut_pyre_boat_station_ready),
}

private enum class PyreCorpse(
    val boneId: Int,
    val remains: String,
    val spiritId: Int,
    val spiritAnim: Int,
    val prayerXp: Double,
    val rewardTable: String?,
    val aggressive: Boolean,
) {
    CHEWED(
        Obj.brut_barbarian_bones, "chewed bones",
        Npc.brut_ascending_spirit, Seq.brut_ascending_spirit, 50.0,
        "pyre_ship_chewed_reward", false,
    ),
    MANGLED(
        Obj.brut_barbarian_bones_bad, "mangled bones",
        Npc.brut_angry_spirit_bad, Seq.brut_angry_spirit_appear, 50.0,
        null, true,
    );

    companion object {
        fun forBone(id: Int) = entries.firstOrNull { it.boneId == id }
    }
}

private val boneIds = PyreCorpse.entries.map { it.boneId }

@ServerStartupEvent
fun mapPyreShips() {
    Logger.warn(World::class.java, "mapPyreShips", "barbarian pyre ships registered")
    val sites = arrayOf<Any>(Loc.brut_burned_ground)
    val logs = Firemaking.Fire.entries.map { it.logId as Any }.toTypedArray()
    val boatStages = arrayOf<Any>(
        PyreStage.STAGE1.loc, PyreStage.STAGE2.loc, PyreStage.STAGE3.loc, PyreStage.BONES.loc,
    )
    val bones = PyreCorpse.entries.map { it.boneId as Any }.toTypedArray()
    val lighters = arrayOf(Obj.tinderbox as Any, *Firemaking.bowIds.map { it as Any }.toTypedArray())
    onObjectClick(Loc.brut_burned_ground, checkDistance = true) { event ->
        if (event.option?.equals("Construct", ignoreCase = true) != true) return@onObjectClick
        val player = event.player
        val found = findStation(event.obj.tile)
        when {
            found is PyreBoat -> if (found.isLit) player.sendMessage("The pyre boat is already burning.") else advance(player, found)
            found != null && found.id != PyreStage.EMPTY.loc -> player.sendMessage("There is already a pyre boat here.")
            else -> carve(player, event.obj, null)
        }
    }
    onItemOnObject(sites, logs) { (player, obj, item) ->
        carve(player, obj, Firemaking.Fire.forId(item.id) ?: return@onItemOnObject)
    }
    onItemOnObject(boatStages, bones) { (player, obj, item) ->
        placeBones(player, obj as? PyreBoat ?: return@onItemOnObject, item)
    }
    onItemOnObject(boatStages, lighters) { (player, obj, item) ->
        (obj as? PyreBoat)?.light(player, if (item.id == Obj.tinderbox) -1 else Firemaking.bowLightAnimation(item.id))
    }
    onObjectClick(*boatStages, PyreStage.LAUNCHED.loc) { event ->
        if (event.option?.equals(CREMATE_OPTION, ignoreCase = true) != true) return@onObjectClick
        val boat = event.obj as? PyreBoat ?: return@onObjectClick
        advance(event.player, boat)
    }
    onXpDrop { event ->
        if (event.skillId != Constants.PRAYER) return@onXpDrop
        val left = event.player.tempAttribs.getI(BONUS_KEY)
        if (left > 0) {
            event.multiplier = PRAYER_BONUS_MULTIPLIER
            event.player.tempAttribs.setI(BONUS_KEY, left - 1)
        }
    }
}

private class PyreBoat(player: Player, station: GameObject) :
    OwnedObject(player, station, PyreStage.STAGE1.loc) {

    private var corpse: PyreCorpse? = null
    private var lit = false
    private var life = PYRE_LIFE

    val hasCorpse: Boolean get() = corpse != null
    val isLit: Boolean get() = lit

    override fun tick(owner: Player) {
        if (!lit && life-- <= 0) destroy()
    }

    fun placeBones(newCorpse: PyreCorpse): Boolean {
        if (lit || corpse != null) return false
        corpse = newCorpse
        life = PYRE_LIFE
        changeId(PyreStage.BONES.loc)
        return true
    }

    fun light(player: Player, bowAnim: Int): Boolean {
        val remains = corpse ?: return false
        if (lit) return false
        if (!ownedBy(player)) {
            player.sendMessage("That's not your pyre boat!")
            return false
        }
        if (bowAnim == -1 && !player.inventory.containsItem(Obj.tinderbox, 1)) {
            player.sendMessage("You need a tinderbox or a bow to light the pyre boat.")
            return false
        }
        lit = true
        life = PYRE_LIFE
        changeId(PyreStage.BURNING.loc)
        World.sendObjectAnimation(this, Seq.brut_pyre_boat_fire)
        player.anim(if (bowAnim != -1) bowAnim else Seq.bonfire_player_lighting_fire_2012_update)
        player.skills.addXp(Constants.FIREMAKING, LIGHT_XP)
        player.skills.addXp(Constants.PRAYER, remains.prayerXp)
        bankBonus(player)
        player.sendMessage("You light the pyre boat.")
        player.lock()
        player.schedule {
            wait(1)
            World.sendSpotAnim(coordFace, Spotanim.temple_pyre_fire)
            changeId(PyreStage.LAUNCHED.loc)
            World.sendObjectAnimation(this@PyreBoat, Seq.brut_pyre_boat_launch)
            if (remains.aggressive) {
                wait(2)
                summonFerocious(player, World.getFreeTile(player.tile, 1), remains)
                changeId(PyreStage.READY.loc)
                World.sendObjectAnimation(this@PyreBoat, Seq.brut_pyre_boat_ready)
                wait(1)
            } else {
                val spirit = ownedNpc(player, remains.spiritId, coordFace, false) {
                    anim(remains.spiritAnim)
                    setNextFaceTile(tile)
                }
                wait(SPIRIT_LIFE)
                spirit.finish()
                changeId(PyreStage.READY.loc)
                World.sendObjectAnimation(this@PyreBoat, Seq.brut_pyre_boat_ready)
                wait(1)
                giveReward(player, remains)
            }
            player.unlock()
            if (!isDestroyed) destroy()
        }
        return true
    }
}

private fun summonFerocious(player: Player, spot: Tile, corpse: PyreCorpse) {
    val spirit = ownedNpc(player, corpse.spiritId, spot, false) {
        anim(corpse.spiritAnim)
        setNextFaceTile(tile)
    }
    spirit.combat.target = player
    player.tempAttribs.setO<Any>(FEROCIOUS_SPIRIT_KEY, spirit)
    player.sendMessage("The barbarian spirit is furious at the disturbance! Defeat it to claim your reward.")
}

private fun activeFerociousSpirit(player: Player): NPC? {
    val spirit = player.tempAttribs.getO<NPC>(FEROCIOUS_SPIRIT_KEY) ?: return null
    return if (spirit.hasFinished() || spirit.isDead()) null else spirit
}

private fun giveReward(player: Player, corpse: PyreCorpse) {
    val table = corpse.rewardTable ?: return
    val drops = DropTable.calculateDrops(player, DropSets.getDropSet(table))
    for (item in drops) player.inventory.addItemDrop(item)
    if (drops.isNotEmpty())
        player.sendMessage("The spirit thanks you for putting his soul to rest.")
}

private fun bankBonus(player: Player) {
    val left = (player.tempAttribs.getI(BONUS_KEY) + PRAYER_BONUS_BONES).coerceAtMost(PRAYER_BONUS_MAX)
    player.tempAttribs.setI(BONUS_KEY, left)
}

private fun carve(player: Player, site: GameObject, log: Firemaking.Fire?) {
    if (player.vars.getVarBit(Varbit.brut_craft_ship) < 1) {
        player.sendMessage("You haven't learned how to make pyre ships.")
        return
    }
    if (activeFerociousSpirit(player) != null) {
        player.sendMessage("You must defeat the barbarian spirit first.")
        return
    }
    if (player.skills.getLevel(Constants.FIREMAKING) < PYRE_LEVEL || player.skills.getLevel(Constants.CRAFTING) < PYRE_LEVEL) {
        player.sendMessage("You need a Firemaking and Crafting level of $PYRE_LEVEL to make a pyre ship.")
        return
    }
    val hatchet = Hatchet.getBest(player)
    if (hatchet == null) {
        player.sendMessage("You need a hatchet to carve the pyre boat.")
        return
    }
    if (!player.inventory.containsItem(Obj.tinderbox, 1) && !player.inventory.containsItems(*Firemaking.bowIds)) {
        player.sendMessage("You need a tinderbox or a bow to make a pyre ship.")
        return
    }
    val station = findStation(site.tile)
    if (station != null && station.id != PyreStage.EMPTY.loc) {
        player.sendMessage("There is already a pyre boat here.")
        return
    }
    val fire = log ?: Firemaking.Fire.entries.firstOrNull { player.inventory.containsItem(it.logId, 1) }
    if (fire == null) {
        player.sendMessage("You need some logs to construct a pyre boat.")
        return
    }
    player.inventory.deleteItem(fire.logId, 1)
    player.anim(hatchet.animCanoe())
    val base = station ?: GameObject(PyreStage.EMPTY.loc, ObjectShape.SCENERY_INTERACT, 0, site.tile)
    PyreBoat(player, base).createReplace()
    player.skills.addXp(Constants.FIREMAKING, fire.xp * 0.5)
    player.skills.addXp(Constants.CRAFTING, fire.xp * 0.5)
    player.sendMessage("You carve the log into a pyre boat.")
}

private fun findStation(tile: Tile): GameObject? {
    var best: GameObject? = null
    var bestDist = Int.MAX_VALUE
    for (dx in -3..3) {
        for (dy in -3..3) {
            val obj = World.getObject(tile.transform(dx, dy), ObjectShape.SCENERY_INTERACT) ?: continue
            if (obj.id !in stationIds) continue
            val dist = dx * dx + dy * dy
            if (dist < bestDist) {
                best = obj
                bestDist = dist
            }
        }
    }
    return best
}

private val stationIds: Set<Int> = PyreStage.entries.map { it.loc }.toSet()

private fun placeBones(player: Player, boat: PyreBoat, item: Item) {
    val corpse = PyreCorpse.forBone(item.id) ?: return
    if (!boat.ownedBy(player)) {
        player.sendMessage("That's not your pyre boat!")
        return
    }
    if (!boat.placeBones(corpse)) {
        player.sendMessage("There are already remains on this pyre boat.")
        return
    }
    player.inventory.deleteItem(corpse.boneId, 1)
    player.anim(Seq.human_reachforladdertop)
    player.sendMessage("You place the ${corpse.remains} on the pyre boat.")
}

private fun advance(player: Player, boat: PyreBoat) {
    if (!boat.hasCorpse) {
        val bone = boneIds.firstNotNullOfOrNull { player.getItemFromInv(it) }
        if (bone == null) {
            player.sendMessage("There are no remains on this pyre boat.")
            return
        }
        placeBones(player, boat, bone)
        return
    }
    val bow = Firemaking.bowIds.firstOrNull { player.inventory.containsItem(it, 1) }
    when {
        player.inventory.containsItem(Obj.tinderbox, 1) -> boat.light(player, -1)
        bow != null -> boat.light(player, Firemaking.bowLightAnimation(bow))
        else -> player.sendMessage("You need a tinderbox or a bow to light the pyre boat.")
    }
}
