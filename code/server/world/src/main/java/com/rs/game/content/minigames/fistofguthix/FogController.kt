package com.rs.game.content.minigames.fistofguthix

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.World
import com.rs.game.content.Effect
import com.rs.game.content.skills.cooking.Foods
import com.rs.game.content.Potion
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item

private const val DEATH_TICKS = 4
private const val REFUSAL_TICKS = 10
private val HUNTED_SPELLS = intArrayOf(Component.interface_430.component_430_32, Component.interface_430.component_430_37, Component.interface_430.component_430_46)
private val SPELLBOOKS = intArrayOf(Interface.interface_192, Interface.interface_193, Interface.interface_430, Interface.interface_950)

val Player.fogController get() = controllerManager.controller as? FogController
val Player.fogMatch get() = fogController?.match

class FogController(@Transient var match: FogMatch?) : Controller() {
    @Transient
    private var freezeTicks = 0L

    @Transient
    private var refusedAt = 0L

    override fun start() {
        player.setForceMultiArea(true)
        player.setCanPvp(true, false)
        player.interfaceManager.removeOverlay()
    }

    override fun sendInterfaces() {
        match?.takeIf { it.roundActive }?.sendHud(player)
    }

    override fun process() {
        val frozen = player.getEffectTicks(Effect.FREEZE)
        if (frozen > freezeTicks + 1) player.addEffect(Effect.FREEZE, frozen / 2)
        freezeTicks = player.getEffectTicks(Effect.FREEZE)
    }

    override fun canAttack(target: Entity): Boolean {
        val match = match ?: return false
        val refusal = when {
            target !is Player || target != match.opponentOf(player) -> "You can only fight your own opponent."
            !match.roundActive -> "The round hasn't started."
            match.inHouse(target) -> "Your quarry is hiding inside a house."
            match.inHouse(player) -> "You can't fight from inside a house."
            match.isHunted(player) && player.equipment.weaponId in STONES -> "You can't fight while holding the stone."
            match.isHunted(player) && player.combatDefinitions.spell != null -> "The hunted can't use combat magic."
            else -> return true
        }
        if (World.getServerTicks() - refusedAt >= REFUSAL_TICKS) {
            refusedAt = World.getServerTicks()
            player.sendMessage(refusal)
        }
        return false
    }

    override fun canHit(entity: Entity): Boolean {
        val match = match ?: return false
        return entity == match.opponentOf(player) && match.roundActive && !match.inHouse(entity as Player)
    }

    override fun processIncomingHit(hit: Hit) {
        val match = match ?: return
        if (hit.source is Player && (!match.roundActive || match.inHouse(player) || hit.source != match.opponentOf(player))) hit.setDamage(0)
    }

    override fun processButtonClick(interfaceId: Int, componentId: Int, slotId: Int, slotId2: Int, opNum: Int): Boolean {
        val match = match ?: return true
        if (interfaceId !in SPELLBOOKS || !match.isHunted(player)) return true
        if (interfaceId == Interface.interface_430 && (interfaceId shl 16 or componentId) in HUNTED_SPELLS) return true
        player.sendMessage("The hunted may only cast Vengeance, Cure Me and Dream.")
        return false
    }

    override fun canEat(food: Foods.Food): Boolean {
        player.sendMessage("You can't eat that in the arena; use your bandages.")
        return false
    }

    override fun canPot(pot: Potion): Boolean {
        player.sendMessage("You can't drink potions in the arena.")
        return false
    }

    override fun processObjectClick1(`object`: GameObject): Boolean {
        val match = match ?: return true
        when (`object`.id) {
            Loc.hvh_dispenser_with_stone -> match.takeStone(player, `object`)
            Loc.hvh_barrier_door -> match.passBarrier(player, `object`)
            Loc.hvh_safe_portal -> match.usePortal(player)
            Loc.hvh_cave_lower_entrancel, Loc.hvh_cave_lower_entrancer -> player.sendOptionsDialogue("Leave the arena? You will forfeit the match.") {
                opExec("Yes, forfeit the match.") { this@FogController.match?.forfeit(player, LeaveReason.FORFEIT) }
                op("No, keep fighting.")
            }
            else -> return true
        }
        return false
    }

    override fun canDropItem(item: Item): Boolean {
        if (item.id !in MATCH_ITEMS) return true
        player.sendMessage("You can't drop that here.")
        return false
    }

    override fun canTrade() = false

    override fun canDepositItem(item: Item) = false

    override fun processTeleport(tele: Teleport): Boolean {
        player.sendMessage("You can't teleport out of the arena; use a forfeit tunnel to leave.")
        return false
    }

    override fun onTeleported(type: TeleType) {
        val match = match
        this.match = null
        match?.forfeit(player, LeaveReason.TELEPORT)
    }

    override fun sendDeath(): Boolean {
        val match = match ?: return true
        player.lock()
        player.stopAll()
        player.schedule {
            player.anim(Seq.human_death)
            wait(DEATH_TICKS)
            match.died(player)
        }
        return false
    }

    override fun logout(): Boolean {
        val match = match
        this.match = null
        match?.forfeit(player, LeaveReason.LOGOUT) ?: FogMatch.cleanUp(player)
        player.tile = FOG_LOBBY_RETURN
        return true
    }

    override fun login(): Boolean {
        FogMatch.cleanUp(player)
        player.tile = FOG_LOBBY_RETURN
        return true
    }

    override fun forceClose() {
        val match = match
        this.match = null
        match?.forfeit(player, LeaveReason.CLOSED)
    }
}
