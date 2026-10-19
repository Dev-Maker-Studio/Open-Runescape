package com.rs.game.content.minigames.crucible

import com.rs.game.content.Effect
import com.rs.game.content.Potion
import com.rs.game.content.minigames.checkAndDeleteFoodAndPotions
import com.rs.game.content.minigames.giveFoodAndPotions
import com.rs.game.content.minigames.isMinigameSupply
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Animation
import org.darkan.core.model.Item
import org.darkan.core.util.Area
import org.darkan.core.util.MapUtils
import world.gregs.voidps.type.Tile

private val FISSURES = Loc.cruc_crevasse_1_invis..Loc.cruc_crevasse_13_invis
private val BANK_FISSURES = Loc.cruc_crevasse_5_invis..Loc.cruc_crevasse_13_invis

class CrucibleController(@JvmField val dangerous: Boolean) : Controller() {
    @JvmField var rank = if (dangerous) -1 else 0
    @JvmField var points = 0
    @Transient private var wasInArea = false

    override fun start() {
        if (dangerous) applyDangerousEffects()
        sendInterfaces()
        Crucible.add(player, dangerous)
    }

    override fun sendInterfaces() {
        player.interfaceManager.sendOverlay(Interface.interface_1296)
        Crucible.updateInterface(player, this)
    }

    override fun sendDeath(): Boolean {
        player.lock(8)
        player.stopAll()
        WorldTasks.scheduleTimer { loop ->
            when (loop) {
                0 -> player.setNextAnimation(Animation(Seq.human_death))
                1 -> player.sendMessage("Oh dear, you have died.")
                4 -> {
                    val killer = player.mostDamageReceivedSourcePlayer
                    killer?.removeDamage(player)
                    killer?.increaseKillCount(player)
                    if (dangerous) {
                        player.sendPVPItemsOnDeath(killer)
                        player.equipment.init()
                        player.inventory.init()
                    }
                    player.reset()
                    player.tele(getRespawnTile())
                    player.stopAnim()
                    if (dangerous) applyDangerousEffects()
                }
                5 -> {
                    player.jingle(90)
                    return@scheduleTimer false
                }
            }
            true
        }
        return false
    }

    override fun canDepositItem(item: Item): Boolean {
        if (isMinigameSupply(item.id)) {
            player.inventory.deleteItem(item)
            return false
        }
        return true
    }

    override fun processTeleport(tele: Teleport?): Boolean {
        player.sendMessage("A mysterious force prevents you from teleporting.")
        return false
    }

    override fun forceClose() {
        checkAndDeleteFoodAndPotions(player)
        remove(false)
    }

    private fun remove(needRemove: Boolean) {
        checkAndDeleteFoodAndPotions(player)
        if (needRemove) removeController()
        if (wasInArea) player.setCanPvp(false)
        player.interfaceManager.removeOverlay()
        player.removeSkull()
        player.removeEffect(Effect.OVERLOAD_PVP_REDUCTION)
        Crucible.remove(player, dangerous)
    }

    override fun processObjectClick1(obj: GameObject): Boolean {
        when (obj.id) {
            in FISSURES -> Crucible.useFissure(player, obj, true, false)
            Loc.cruc_bank -> player.sendOptionDialogue("What would you like to do?") { ops ->
                ops.add("I'd like to open my bank please.") { player.bank.open() }
                if (!dangerous) ops.add("Kit me out with some food and potions please.") { giveFoodAndPotions(player) }
                ops.add("Nevermind.")
            }
            Loc.cruc_crevase_2x2_tunnel_exit_invis -> {
                remove(true)
                player.useStairs(-1, Tile.of(3355, 6119, 0), 0, 1)
            }
            else -> return true
        }
        return false
    }

    override fun processObjectClick2(obj: GameObject): Boolean {
        if (obj.id !in FISSURES) return true
        Crucible.useFissure(player, obj, false, false)
        return false
    }

    override fun processObjectClick3(obj: GameObject): Boolean {
        if (obj.id !in BANK_FISSURES) return true
        Crucible.useFissure(player, obj, true, true)
        return false
    }

    override fun moved() {
        val inArea = !inSafeArea(player)
        if (inArea && !wasInArea) {
            player.setCanPvp(true)
            wasInArea = true
        } else if (!inArea && wasInArea) {
            player.setCanPvp(false)
            wasInArea = false
        }
    }

    override fun canAttack(target: Entity): Boolean = canHit(target)

    override fun canHit(entity: Entity): Boolean {
        val controller = (entity as? Player)?.controllerManager?.controller as? CrucibleController ?: return true
        if (dangerous != controller.dangerous) {
            player.sendMessage("That player isn't in ${if (dangerous) "dangerous" else "safe"} mode with you.")
            return false
        }
        return true
    }

    override fun logout(): Boolean = false

    override fun login(): Boolean {
        start()
        moved()
        return false
    }

    private fun applyDangerousEffects() {
        Potion.checkPVPPotionBoosts(player)
        player.addEffect(Effect.OVERLOAD_PVP_REDUCTION, Int.MAX_VALUE.toLong())
        player.setSkullInfiniteDelay(7)
    }

    private fun inSafeArea(player: Player): Boolean = SAFE_ZONES.any { it.within(player.tile) }

    companion object {
        private val SAFE_ZONES: Array<Area> = arrayOf(
            MapUtils.getArea(3314, 6136, 3322, 6149),
            MapUtils.getArea(3258, 6196, 3269, 6203),
            MapUtils.getArea(3204, 6139, 3212, 6149),
            MapUtils.getArea(3255, 6084, 3266, 6093)
        )

        @JvmStatic
        fun getRespawnTile(): Tile {
            val zone = SAFE_ZONES[Utils.random(SAFE_ZONES.size)]
            return Tile.of(
                zone.x + (zone.width - 1) / 2 + Utils.random(-1, 2),
                zone.y + (zone.height - 1) / 2 + Utils.random(-1, 2),
                0
            )
        }
    }
}
