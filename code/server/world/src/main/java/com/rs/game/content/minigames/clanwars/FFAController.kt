package com.rs.game.content.minigames.clanwars

import com.rs.game.content.Effect
import com.rs.game.content.Potion
import com.rs.game.content.minigames.checkAndDeleteFoodAndPotions
import com.rs.game.content.minigames.giveFoodAndPotions
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Animation
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

private val SAFE_ARENA = Tile.of(2815, 5511, 0)
private val DANGEROUS_ARENA = Tile.of(3007, 5511, 0)
private val LOBBY = Tile.of(2993, 9679, 0)
private const val PVP_AREA_Y = 5512
private const val ARENA_RADIUS = 128

@ServerStartupEvent
fun mapFreeForAll() {
    onObjectClick(Loc.clanwars_ffa_safe_entrance, Loc.clanwars_ffa_dangerous_entrance) { (player, obj) ->
        WorldTasks.delay(0) {
            val dangerous = obj.id == Loc.clanwars_ffa_dangerous_entrance
            player.vars.setVarBit(Varbit.clanwars_ffatype, if (dangerous) 1 else 0)
            if (player.vars.getVarBit(warningVarbit(dangerous)) == 1) enter(player, dangerous)
            else player.interfaceManager.sendInterface(Interface.interface_793)
        }
    }
    onButtonClick(Interface.interface_793) { e ->
        val player = e.player
        val dangerous = player.vars.getVarBit(Varbit.clanwars_ffatype) == 1
        val warning = warningVarbit(dangerous)
        when (e.component) {
            Component.interface_793.component_793_9 -> player.vars.setVarBit(warning, if (player.vars.getVarBit(warning) == 1) 0 else 1)
            Component.interface_793.component_793_14 -> player.closeInterfaces()
            Component.interface_793.component_793_15 -> {
                if (player.vars.getVarBit(warning) == 1) player.vars.saveVarBit(warning, 1)
                player.closeInterfaces()
                enter(player, dangerous)
            }
        }
    }
}

private fun warningVarbit(dangerous: Boolean) = if (dangerous) Varbit.cws_warning_31 else Varbit.cws_warning_22

private fun enter(player: Player, dangerous: Boolean) {
    player.tele(if (dangerous) DANGEROUS_ARENA else SAFE_ARENA)
    player.controllerManager.startController(FFAController(dangerous))
}

class FFAController(@JvmField val dangerous: Boolean) : Controller() {
    @Transient private var wasInArea = false

    override fun start() {
        if (dangerous) {
            Potion.checkPVPPotionBoosts(player)
            player.addEffect(Effect.OVERLOAD_PVP_REDUCTION, Int.MAX_VALUE.toLong())
        }
        sendInterfaces()
    }

    override fun sendInterfaces() {
        player.interfaceManager.sendOverlay(Interface.interface_789)
    }

    override fun sendDeath(): Boolean {
        player.lock(8)
        player.stopAll()
        WorldTasks.scheduleTimer { loop ->
            when (loop) {
                0 -> player.setNextAnimation(Animation(Seq.human_death))
                1 -> player.sendMessage("Oh dear, you have died.")
                3 -> {
                    val killer = player.mostDamageReceivedSourcePlayer
                    killer?.removeDamage(player)
                    killer?.increaseKillCount(player)
                    if (dangerous) {
                        player.sendPVPItemsOnDeath(killer)
                        player.equipment.init()
                        player.inventory.init()
                    }
                    player.reset()
                    player.tele(LOBBY)
                    remove(true)
                    player.stopAnim()
                }
                4 -> {
                    player.jingle(90)
                    return@scheduleTimer false
                }
            }
            true
        }
        return false
    }

    override fun onTeleported(type: TeleType) = remove(true)

    override fun forceClose() = remove(false)

    private fun remove(needRemove: Boolean) {
        checkAndDeleteFoodAndPotions(player)
        player.reset()
        if (needRemove) removeController()
        if (wasInArea) player.setCanPvp(false)
        player.interfaceManager.removeOverlay()
        player.removeEffect(Effect.OVERLOAD_PVP_REDUCTION)
    }

    override fun processObjectClick1(obj: GameObject): Boolean {
        when (obj.id) {
            Loc.clanwars_ffa_exit -> {
                remove(true)
                player.useStairs(-1, LOBBY, 0, 1)
                return false
            }
            Loc.snp_counter_bandages_blue -> giveFoodAndPotions(player)
        }
        return true
    }

    override fun moved() {
        val inArea = player.y >= PVP_AREA_Y
        if (inArea && !wasInArea) {
            player.setCanPvp(true)
            wasInArea = true
        } else if (!inArea && wasInArea) {
            player.setCanPvp(false)
            wasInArea = false
        }
    }

    private fun inArena() = player.tile.withinDistance(if (dangerous) DANGEROUS_ARENA else SAFE_ARENA, ARENA_RADIUS)

    override fun canDepositItem(item: Item): Boolean {
        player.sendMessage("You can't bank items here.")
        return false
    }

    override fun logout(): Boolean = false

    override fun login(): Boolean {
        if (!inArena()) return true
        start()
        moved()
        return false
    }
}
