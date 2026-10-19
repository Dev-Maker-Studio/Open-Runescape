package com.rs.game.content.minigames.shadesofmortton

import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.shadesofmortton.canRepairMorttonTemple
import com.rs.game.content.quests.shadesofmortton.morttonState
import com.rs.game.content.quests.shadesofmortton.onMorttonOilSanctified
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.Areas
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim as SpotAnimId
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile
import java.util.concurrent.ConcurrentHashMap

private val altarTile = Tile.of(3506, 3316, 0)
private val wallIds = arrayOf<Any>(Loc.templewall_base, Loc.templewall_1, Loc.templewall_2, Loc.templewall_3, Loc.templewall_4, Loc.templewall_5, Loc.templewall_6, Loc.templewall_7, Loc.templewall_8, Loc.templewall_9, Loc.templewall_10, Loc.templewallcorner_base, Loc.templewallcorner_1, Loc.templewallcorner_2, Loc.templewallcorner_3, Loc.templewallcorner_4, Loc.templewallcorner_5, Loc.templewallcorner_6, Loc.templewallcorner_7, Loc.templewallcorner_8, Loc.templewallcorner_9, Loc.templewallcorner_10)
private val oilCosts = mapOf(Obj.oliveoil4 to 3.6, Obj.oliveoil3 to 2.7, Obj.oliveoil2 to 1.8, Obj.oliveoil1 to 0.9)
private val oils = mapOf(Obj.oliveoil4 to Obj.sacred_oil4, Obj.oliveoil3 to Obj.sacred_oil3, Obj.oliveoil2 to Obj.sacred_oil2, Obj.oliveoil1 to Obj.sacred_oil1)
private val serums = mapOf(Obj.mort_serum4 to Obj.mort_serum_perm4, Obj.mort_serum3 to Obj.mort_serum_perm3, Obj.mort_serum2 to Obj.mort_serum_perm2, Obj.mort_serum1 to Obj.mort_serum_perm1)
private val kits = mapOf(Obj.luc2_dagonhai_robe_top to Obj.obj_21477, Obj.luc2_dagonhai_hat to Obj.obj_21478, Obj.luc2_dagonhai_robe_bottom to Obj.obj_21479)
private val skulls = mapOf(Obj.staff_of_air to Obj.obj_21490, Obj.staff_of_water to Obj.obj_21491, Obj.staff_of_earth to Obj.obj_21492, Obj.staff_of_fire to Obj.obj_21493, Obj.fire_battlestaff to Obj.obj_21494, Obj.water_battlestaff to Obj.obj_21495, Obj.air_battlestaff to Obj.obj_21496, Obj.earth_battlestaff to Obj.obj_21497, Obj.mystic_fire_staff to Obj.obj_21498, Obj.mystic_water_staff to Obj.obj_21499, Obj.mystic_air_staff to Obj.obj_21500, Obj.mystic_earth_staff to Obj.obj_21501, Obj.lava_battlestaff to Obj.obj_21502, Obj.mystic_lava_staff to Obj.obj_21503, Obj.mud_battlestaff to Obj.obj_21504, Obj.mystic_mud_staff to Obj.obj_21505, Obj.godwars_steam_battlestaff to Obj.obj_21506, Obj.godwars_mystic_steam_battlestaff to Obj.obj_21507)

object ShadesOfMortton {
    private val walls = ConcurrentHashMap<Int, TempleWall>()
    private var repairState = 0

    @JvmStatic fun getRepairState(): Int = repairState
    @JvmStatic fun getWall(obj: GameObject): TempleWall = walls[obj.tile.id] ?: TempleWall(obj)
    @JvmStatic fun getRandomWall(): TempleWall? = walls.values.toList().let { if (it.isEmpty()) null else it[Utils.random(it.size)] }
    @JvmStatic fun addWall(wall: TempleWall) { walls[wall.tile.id] = wall }
    @JvmStatic fun deleteWall(wall: TempleWall) { walls.remove(wall.tile.id) }
    @JvmStatic fun getWalls(): List<TempleWall> = walls.values.toList()

    fun updateRepairState() {
        repairState = (walls.values.sumOf { it.repairPerc } / 1500.0 * 100).toInt().coerceIn(0, 100)
    }

    fun updateVars(player: Player) {
        player.vars.setVar(Varp.varp_343, repairState)
        player.vars.setVar(Varp.varp_344, player.getI("shadeResources", 0))
        player.vars.setVar(Varp.varp_345, kotlin.math.ceil(player.tempAttribs.getD("shadeSanctity", 0.0)).toInt())
    }

    @JvmStatic fun addSanctity(player: Player, amount: Double) {
        player.tempAttribs.setD("shadeSanctity", (player.tempAttribs.getD("shadeSanctity", 0.0) + amount).coerceIn(0.0, 100.0))
        updateVars(player)
    }
    @JvmStatic fun removeSanctity(player: Player, amount: Double) { addSanctity(player, -amount) }
    @JvmStatic fun addResources(player: Player, amount: Int) {
        player.set("shadeResources", (player.getI("shadeResources", 0) + amount).coerceIn(0, 100))
        updateVars(player)
    }
    @JvmStatic fun removeResources(player: Player, amount: Int) { addResources(player, -amount) }
}

@ServerStartupEvent
fun mapShadesOfMortton() {
    WorldTasks.scheduleTimer(Ticks.fromSeconds(30), Ticks.fromSeconds(30)) {
        ShadesOfMortton.updateRepairState()
        val players = World.getPlayersInChunkRange(Tile.of(3497, 3298, 0).zone.id, 4).filter { it.hasStarted() && !it.hasFinished() }
        players.forEach { ShadesOfMortton.removeSanctity(it, 1.0) }
        val altar = World.getObject(altarTile)
        if (altar != null) {
            val replacement = when {
                ShadesOfMortton.getRepairState() < 99 -> Loc.templefire_altar_nofire_broken
                altar.id == Loc.templefire_altar_nofire_broken -> Loc.templefire_altar_nofire
                altar.id == Loc.templefire_altar && Utils.random(2) == 0 -> Loc.templefire_altar_nofire
                else -> altar.id
            }
            if (replacement != altar.id) {
                val sanctified = altar.id == Loc.templefire_altar_nofire_broken && replacement == Loc.templefire_altar_nofire
                altar.changeId(replacement)
                World.spawnObject(GameObject(altar))
                World.sendSpotAnim(altar.tile, SpotAnim(SpotAnimId.my4q_stone_stand_smoke_puff))
                if (sanctified) players.forEach { it.sendMessage("The holy altar appears sanctified, it can now be lit!") }
            }
        }
        true
    }
    onChunkEnter { e ->
        val player = e.entity as? Player ?: return@onChunkEnter
        if (!player.hasStarted()) return@onChunkEnter
        val inside = Areas.withinArea("shades_temple", e.chunkId)
        if (inside != player.tempAttribs.getB("inShadeTemple")) {
            if (inside) player.interfaceManager.sendOverlay(Interface.interface_328) else player.interfaceManager.removeOverlay()
            player.tempAttribs.setB("inShadeTemple", inside)
            ShadesOfMortton.updateVars(player)
        }
    }
    onItemOnObject(arrayOf(Loc.templefire_altar)) { (player, _, item) ->
        val oil = oils[item.id]
        val serum = serums[item.id]
        val product = oil ?: serum ?: return@onItemOnObject
        if (!player.canRepairMorttonTemple()) {
            player.sendMessage("You're not sure how you would go about fixing this.")
            return@onItemOnObject
        }
        val required = if (serum != null) 20 else 10
        if (player.tempAttribs.getD("shadeSanctity", 0.0) < required) {
            player.sendMessage(if (serum != null) "You need to increase your sanctity to 20% before you can use the sacred flame for this purpose. Using the sacred flame reduces your sanctity."
                else "You need to increase your sanctity to 10% before you can use the sacred flame. Using the sacred flame reduces your sanctity.")
            return@onItemOnObject
        }
        val cost = if (serum != null) 20.0 else oilCosts.getValue(item.id)
        item.id = product
        ShadesOfMortton.removeSanctity(player, cost)
        player.inventory.refresh()
        if (serum != null) {
            player.vars.saveVarBit(Varbit.player_made_perm_serum, 1)
            player.sendMessage("You carefully sanctify the serum in the sacred flame.")
            player.sendMessage("You sanctify serum 207 in the sacred flame and rename it to serum 208.")
        } else {
            player.sendMessage("You carefully sanctify the oil in the sacred flame.")
            player.onMorttonOilSanctified()
        }
    }
    onObjectClick(Loc.templefire_altar_nofire) { (player, obj) ->
        if (!player.canRepairMorttonTemple()) return@onObjectClick
        if (player.tempAttribs.getD("shadeSanctity", 0.0) < 10) {
            player.sendMessage("You haven't sanctified enough of the temple to allow you to light the fire. Repair or reinforce the wall to increase your sanctity. You need at least 10% Sanctity to attempt to light the fire")
            return@onObjectClick
        }
        if (!player.inventory.containsItem(Obj.tinderbox, 1)) {
            player.sendMessage("You need a tinderbox to do that.")
            return@onObjectClick
        }
        obj.changeId(Loc.templefire_altar)
        World.spawnObject(GameObject(obj))
        player.anim(Seq.human_light_torch)
        player.skills.addXp(Constants.FIREMAKING, 100.0)
    }
    onObjectClick(*wallIds) { (player, obj) ->
        if (!player.canRepairMorttonTemple()) {
            player.sendMessage("You're not sure how you would go about fixing this.")
            return@onObjectClick
        }
        player.actionManager.setAction(object : PlayerAction() {
            override fun start(player: Player): Boolean {
                player.faceTile(obj.tile)
                stockResources(player)
                if (player.getI("shadeResources", 0) <= 0) {
                    player.sendMessage("You don't have enough resources!")
                    return false
                }
                if (!player.inventory.containsOneItem(Obj.flamtaer_hammer, Obj.hammer)) {
                    player.sendMessage("You need a hammer to do that.")
                    return false
                }
                if (!player.isQuestComplete(Quest.SHADES_OF_MORTTON)) player.morttonState().setB("repairStarted", true)
                player.actionManager.setActionDelay(4)
                return true
            }
            override fun process(player: Player): Boolean {
                if (!player.canRepairMorttonTemple()) return false
                val inside = player.x in 3505..3507 && player.y in 3315..3317
                val high = ShadesOfMortton.getWall(obj).repairPerc > 50
                val hammer = player.inventory.containsItem(Obj.flamtaer_hammer, 1)
                if (!hammer && !player.inventory.containsItem(Obj.hammer, 1)) return false
                player.anim(when {
                    hammer && high -> Seq.player_repair_temple_high_sc_volatile_hammer
                    hammer && inside -> Seq.player_repair_temple_low_insidewall_sc_volatile_hammer
                    hammer -> Seq.player_repair_temple_low_outsidewall_sc_volatile_hammer
                    high -> Seq.player_repair_temple_high_hammer
                    inside -> Seq.player_repair_temple_low_insidewall_hammer
                    else -> Seq.player_repair_temple_low_outsidewall_hammer
                })
                return true
            }
            override fun processWithDelay(player: Player): Int {
                player.faceTile(obj.tile)
                stockResources(player)
                if (player.getI("shadeResources", 0) <= 0) {
                    player.sendMessage("You have run out of resources!")
                    return -1
                }
                if (Utils.random(if (player.inventory.containsItem(Obj.flamtaer_hammer, 1)) 2 else 6) != 0) {
                    player.skills.addXp(Constants.CRAFTING, Utils.random(5, 9).toDouble())
                    return 4
                }
                ShadesOfMortton.getWall(obj).increaseProgress()
                ShadesOfMortton.removeResources(player, 1)
                ShadesOfMortton.addSanctity(player, 5.0)
                player.skills.addXp(Constants.CRAFTING, Utils.random(20, 35).toDouble())
                return 4
            }
            override fun stop(player: Player) { player.anim(-1) }
        })
    }
    registerShadeEquipment(kits, Obj.obj_21489, "Remove-kit", true)
    registerShadeEquipment(skulls, Obj.hlr4m_shade_skull, "Remove-skull", false)
}

private fun stockResources(player: Player) {
    if (player.getI("shadeResources", 0) <= 95 && player.inventory.containsOneItem(Obj.timberbeam, Obj.woodplank) &&
        player.inventory.containsItem(Obj.limestonebrick, 1) && player.inventory.containsItem(Obj.swamppaste, 5)) {
        player.inventory.deleteItem(if (player.inventory.containsItem(Obj.timberbeam, 1)) Obj.timberbeam else Obj.woodplank, 1)
        player.inventory.deleteItem(Obj.limestonebrick, 1)
        player.inventory.deleteItem(Obj.swamppaste, 5)
        ShadesOfMortton.addResources(player, 5)
    }
}

private fun registerShadeEquipment(products: Map<Int, Int>, attachment: Int, removeOption: String, cloth: Boolean) {
    onItemOnItem(intArrayOf(attachment), products.keys.toIntArray()) { e ->
        val player = e.player
        val item = e.getUsedWith(attachment)
        val product = products[item.id] ?: return@onItemOnItem
        if (player.skills.getLevel(Constants.CRAFTING) < 85) {
            player.sendMessage(if (cloth) "You need a Crafting level of 85 to attach the necromancer kit." else "You need a Crafting level of 85 to attach the skull.")
            return@onItemOnItem
        }
        if (cloth && !player.inventory.containsItem(Obj.fine_cloth, 10)) {
            player.sendMessage("You need 10 fine cloth to attach the necromancer kit.")
            return@onItemOnItem
        }
        item.id = product
        player.inventory.deleteItem(attachment, 1)
        if (cloth) {
            player.inventory.deleteItem(Obj.fine_cloth, 10)
            player.skills.addXp(Constants.CRAFTING, 150.0)
        }
        player.inventory.refresh()
    }
    val bases = products.entries.associate { (base, upgraded) -> upgraded to base }
    onItemClick(*bases.keys.map { it as Any }.toTypedArray(), options = arrayOf(removeOption)) { (player, item) ->
        if (!player.inventory.hasFreeSlots()) {
            player.sendMessage("You don't have enough inventory space!")
            return@onItemClick
        }
        player.startConversation {
            options(if (cloth) "Would you like to remove the kit? You will not recover the fine cloth." else "Would you like to remove the skull?") {
                opExec("Yes") {
                    if (player.inventory.hasFreeSlots() && item.id in bases) {
                        item.id = bases.getValue(item.id)
                        player.inventory.addItemDrop(attachment, 1)
                        player.inventory.refresh()
                    }
                }
                op("Nevermind")
            }
        }
    }
}
