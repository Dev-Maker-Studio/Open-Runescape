package com.rs.game.content.bosses.godwars

import com.rs.engine.dialogue.startConversation
import com.rs.game.World.sendProjectile
import com.rs.game.content.bosses.godwars.factions.GodFaction
import com.rs.game.content.bosses.godwars.factions.zaros.NexArena
import com.rs.game.content.bosses.godwars.factions.zaros.NexController
import com.rs.game.content.skills.magic.Magic.sendNormalTeleportNoType
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.Constants
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.game.model.item.ItemTooltips
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import java.util.concurrent.TimeUnit

@ServerStartupEvent
fun mapGWDEntranceAndExit() {
    onObjectClick(26342) { (player, obj, option) ->
        if (option == "Tie-rope") {
            if (player.inventory.containsItem(954)) {
                player.anim(832)
                player.inventory.deleteItem(954, 1)
                player.vars.saveVarBit(Varbit.godwars_lore_book_saradomin_amulet, 1)
                player.sendMessage("You carefully secure the rope and throw it into the dark hole.")
            } else
                player.sendMessage("A rope would help here.")
            return@onObjectClick
        }
        player.schedule {
            player.lock()
            player.faceObject(obj)
            wait(1)
            player.anim(828)
            wait(2)
            player.tele(Tile.of(2881, 5310, 2))
            player.controllerManager.startController(GodwarsController())
            wait(1)
            player.unlock()
        }
    }

    onObjectClick(26293) { (player) -> player.useStairs(828, Tile.of(2916, 3746, 0)) }
}

private const val ALTAR_RECHARGE_KEY = "gwdAltarRechargeAt"
private val ALTAR_RECHARGE_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(10)

@ServerStartupEvent
fun mapGWDAltars() {
    onObjectClick(Loc.loc_26286, Loc.loc_26287, Loc.loc_26288, Loc.loc_26289) { (player, _, option) ->
        if (option != "Pray-at")
            return@onObjectClick
        if (player.getL(ALTAR_RECHARGE_KEY, 0) >= System.currentTimeMillis()) {
            player.sendMessage("You must wait a total of 10 minutes before being able to recharge your prayer points.")
            return@onObjectClick
        }
        if (player.inCombat()) {
            player.sendMessage("You cannot recharge your prayer while engaged in combat.")
            return@onObjectClick
        }
        player.prayer.restorePrayer((player.skills.getLevelForXp(Constants.PRAYER) * 10).toDouble())
        player.anim(Seq.human_pray)
        player.sendMessage("Your prayer points feel rejuvenated.")
        player.set(ALTAR_RECHARGE_KEY, System.currentTimeMillis() + ALTAR_RECHARGE_COOLDOWN_MS)
    }
}

class GodwarsController : Controller() {
    private val killcount = IntArray(5)

    override fun start() = sendInterfaces()
    override fun logout() = false

    override fun login(): Boolean {
        sendInterfaces()
        return false // so doesnt remove script
    }

    override fun process() {
        if (!isAtGodwars(player.nextTile ?: player.tile)) {
            remove()
            removeController()
        }
        updateKillcount()
    }

    override fun processObjectClick1(obj: GameObject): Boolean {
        if (obj.id == 26444) {
            if (player.skills.getLevel(Constants.AGILITY) >= 70) player.useStairs(828, Tile.of(2914, 5300, 1), 1, 2)
            else player.sendMessage("You need an Agility level of 70 to maneuver this obstacle.")
            return false
        }

        if (obj.id == 26445) {
            if (player.skills.getLevel(Constants.AGILITY) >= 70) player.useStairs(828, Tile.of(2920, 5274, 0), 1, 2)
            else player.sendMessage("You need an Agility level of 70 to maneuver this obstacle.")
            return false
        }

        if (obj.id == 26427 && player.x >= 2908) {
            if (killcount[GodFaction.SARADOMIN.ordinal] >= 40) {
                player.tele(Tile.of(2907, 5265, 0))
                killcount[GodFaction.SARADOMIN.ordinal] -= 40
                updateKillcount()
            } else player.sendMessage("This door is locked by Saradomin and requires that you kill 40 of his minions before it is unlocked.")
            return false
        }

        if (obj.id == 26303) {
            if (player.skills.getLevel(Skills.RANGE) >= 70) {
                val withinArmadyl = player.y < 5276
                val tile: Tile = Tile.of(2871, if (withinArmadyl) 5279 else 5269, 2)
                player.lock()
                player.schedule {
                    wait(1)
                    player.setNextFaceTile(tile)
                    player.anim(385)
                    wait(2)
                    player.anim(16635)
                    wait(1)
                    player.appearance.isHidden = true
                    sendProjectile(player.tile, tile, 1185, 18 to 18, 30, 15, 30)
                    player.forceMove(tile, 0, 180, false) {
                        player.appearance.isHidden = false
                        player.anim(16672)
                        player.unlock()
                        player.resetReceivedHits()
                    }
                }
            } else player.sendMessage("You need a Ranged level of 70 to cross this obstacle.")
            return false
        }


        if (obj.id == 26426 && player.y <= 5295) {
            if (killcount[GodFaction.ARMADYL.ordinal] >= 40) {
                player.tele(Tile.of(2839, 5296, 2))
                killcount[GodFaction.ARMADYL.ordinal] -= 40
                updateKillcount()
            } else player.sendMessage("This door is locked by Armadyl and requires that you kill 40 of his minions before it is unlocked.")
            return false
        }

        if (obj.id == 26428 && player.y >= 5332) {
            if (killcount[GodFaction.ZAMORAK.ordinal] >= 40) {
                player.tele(Tile.of(2925, 5331, 2))
                killcount[GodFaction.ZAMORAK.ordinal] -= 40
                updateKillcount()
            } else player.sendMessage("This door is locked by Zamorak and requires that you kill 40 of his minions before it is unlocked.")
            return false
        }

        if (obj.id == 26425 && player.x <= 2863) {
            if (killcount[GodFaction.BANDOS.ordinal] >= 40) {
                player.tele(Tile.of(2864, 5354, 2))
                killcount[GodFaction.BANDOS.ordinal] -= 40
                updateKillcount()
            } else player.sendMessage("This door is locked by Bandos and requires that you kill 40 of his minions before it is unlocked.")
            return false
        }

        if (obj.id == 26384) {
            if (player.skills.getLevel(Constants.STRENGTH) >= 70) {
                if (player.inventory.containsItem(2347, 1)) {
                    if (player.x == 2851) {
                        player.sendMessage("You bang on the door with your hammer.")
                        player.useStairs(11033, Tile.of(2850, 5333, 2), 1, 2)
                    } else if (player.x == 2850) {
                        player.sendMessage("You bang on the door with your hammer.")
                        player.useStairs(11033, Tile.of(2851, 5333, 2), 1, 2)
                    }
                } else player.sendMessage("You need a hammer to be able to hit the gong to request entry.")
            } else player.sendMessage("You need a Strength level of 70 to enter this area.")
            return false
        }

        if (obj.id == 57211) {
            if (player.y == 5279) {
                val key = player.inventory.getItemById(20120)
                if (key != null && key.getMetaData("frozenKeyCharges") != null && key.getMetaDataI("frozenKeyCharges") > 1) {
                    player.useStairs(828, Tile.of(2885, 5275, 2), 1, 2)
                    key.addMetaData("frozenKeyCharges", key.getMetaDataI("frozenKeyCharges") - 1)
                    if (key.getMetaData("frozenKeyCharges") as Int == 1) player.sendMessage("Your frozen key breaks. You will have to repair it on a repair stand.")
                    else player.sendMessage("A part of your key chips off. It looks like it will still work.")
                } else player.sendMessage("You require a frozen key with enough charges to enter.")
            } else player.useStairs(828, Tile.of(2885, 5279, 2), 1, 2)
            return false
        }

        if (obj.id == 57260) {
            player.useStairs(828, Tile.of(2886, 5274, 2), 1, 2)
            return false
        }

        if (obj.id == 57254) {
            player.useStairs(828, Tile.of(2855, 5221, 0), 1, 2)
            return false
        }

        if (obj.id == 57234) {
            if (player.x == 2859) player.tele(player.transform(3, 0, 0))
            else if (player.x == 2862) player.tele(player.transform(-3, 0, 0))
            return false
        }

        if (obj.id == 57258) {
            if (killcount[GodFaction.ZAROS.ordinal] >= 40 || player.equipment.wearingFullCeremonial()) {
                if (player.equipment.wearingFullCeremonial()) player.sendMessage("The door somehow recognizes your relevance to the area and allows you to pass through.")
                player.tele(Tile.of(2900, 5204, 0))
            } else player.sendMessage("This door is locked by the power of Zaros. You will need to kill at least 40 of his followers before the door will open.")
            return false
        }

        if (obj.id == 57225) {
            val globalInstance: NexArena = NexArena.globalInstance
            player.startConversation {
                simple("The room beyond this point is a prison! There is no way out other than death or teleport. Only those who endure dangerous encounters should proceed.")
                options("There are currently " + globalInstance.playersCount + " people fighting.<br>Do you wish to join them?") {
                    opExec("Climb down.") {
                        player.tele(Tile.of(2911, 5204, 0))
                        player.controllerManager.startController(NexController(globalInstance))
                    }
                    op("Stay here.")
                }
            }
            return false
        }
        return true
    }

    override fun processObjectClick2(obj: GameObject): Boolean {
        if (obj.id == 26286) sendNormalTeleportNoType(player, Tile.of(2922, 5345, 2))
        else if (obj.id == 26287) sendNormalTeleportNoType(player, Tile.of(2912, 5268, 0))
        else if (obj.id == 26288) sendNormalTeleportNoType(player, Tile.of(2842, 5266, 2))
        else if (obj.id == 26289) sendNormalTeleportNoType(player, Tile.of(2837, 5355, 2))
        return true
    }

    override fun sendInterfaces() {
        player.interfaceManager.sendOverlay(601, true)
        player.packets.sendRunScriptReverse(1171)
    }

    override fun sendDeath(): Boolean {
        remove()
        removeController()
        return true
    }

    override fun onTeleported(type: TeleType) {
        remove()
        removeController()
    }

    override fun forceClose() = remove()

    fun updateKillcount() {
        GodFaction.entries.forEach {
            player.vars.setVarBit(it.kcVarbit, killcount[it.ordinal])
        }
    }

    fun sendKill(faction: GodFaction) {
        killcount[faction.ordinal]++
        updateKillcount()
    }

    fun remove() {
        player.interfaceManager.removeOverlay(true)
        player.sendMessage("The souls of those you have slain leave you as you exit the dungeon.")
    }

    companion object {
        val GWD_REGIONS = setOf(11345, 11346, 11347, 11601, 11602, 11603)
        @JvmStatic
        fun isAtGodwars(teleTile: Tile) = GWD_REGIONS.contains(teleTile.getRegionId())
    }
}

@ServerStartupEvent
fun mapFrozenKeyTooltip() {
    ItemTooltips.metadata("frozenKeyCharges") { _, item, _ -> ItemTooltips.count("Uses", (item.getMetaDataI("frozenKeyCharges") - 1).coerceAtLeast(0), 99) }
}
