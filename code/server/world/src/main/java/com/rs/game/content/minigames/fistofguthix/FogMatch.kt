package com.rs.game.content.minigames.fistofguthix

import com.rs.game.World
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varcstr
import org.darkan.core.model.Animation
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import kotlin.math.max

const val ROUND_TICKS = 1000
const val CHARGE_CAP = 5000
const val PORTAL_COST = 20
const val ROLE_HUNTER = 2
const val ROLE_HUNTED = 3

private const val PULSE_TICKS = 5
private const val DAMAGE_EVENTS = 6
private const val LOW_RATE = 15
private const val LOW_STONE_DAMAGE = 10
private const val HIGH_STONE_DAMAGE = 20
private const val REENTRY_TICKS = 34
private const val HOUSE_DAMAGE_TICKS = 8
private const val HOUSE_DAMAGE = 30
private const val HOUSE_ENTRY_PERCENT = 10
private const val PORTAL_SUCCESS_PERCENT = 50
private const val STONE_STAGE_CHARGES = 1000
private const val STAT_FLOOR_PERCENT = 50
private const val CENTRE_RATE = 28
private const val CENTRE_RUN_RESTORE = 1.0
private const val COUNTDOWN_TICKS = 5
private const val ROUND_BREAK_TICKS = 5
private const val FINISH_TICKS = 5
private const val HINT_ARROW = 1
private const val CELEBRATE_TICKS = 2
private const val TELEPORT_TICKS = 3
private const val RESPAWN_MIN_DISTANCE = 20
private const val UNSEEN_COMPENSATION = 500
private const val MIDTURN_COMPENSATION = 150

val STONES = intArrayOf(Obj.hvh_stone_1, Obj.hvh_stone_2, Obj.hvh_stone_3, Obj.hvh_stone_4, Obj.hvh_stone_5_6)
val SUPPLIES = listOf(Item(Obj.hvh_food, 5), Item(Obj.hvh_catalytic_rune, 300), Item(Obj.hvh_elemental_rune, 1000), Item(Obj.hvh_teleport_tablet, 1))
val MATCH_ITEMS: Set<Int> = STONES.toSet() + SUPPLIES.map { it.id }
val SUPPLY_SLOTS = SUPPLIES.sumOf { if (it.definitions.isStackable()) 1 else it.amount }
private val DRAINED_STATS = intArrayOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENSE, Skills.RANGE, Skills.MAGIC)
private val HUD_VARBITS = intArrayOf(
    Varbit.hvh_game_role, Varbit.hvh_time, Varbit.hvh_charge, Varbit.hvh_opponents_charge,
    Varbit.hvh_played_hunted, Varbit.hvh_player_a, Varbit.hvh_opponents_combat,
)

enum class LeaveReason { FORFEIT, LOGOUT, TELEPORT, CLOSED }

class FogCombatant(val player: Player, val slot: Int) {
    var score = 0
    var hadHuntedTurn = false
    var house: House? = null
    var houseExitTick = -REENTRY_TICKS
    var houseClock = 0
    var scoringEvents = 0
    var lastIncrement = 0
    var shownStone = true
    var teleporting = false
    var hintIndex = -1
}

class FogMatch(first: Player, second: Player) {
    private enum class Phase { COUNTDOWN, ROUND, BREAK, ENDED }

    private val sides = listOf(FogCombatant(first, 0), FogCombatant(second, 1))
    private var phase = Phase.COUNTDOWN
    private var huntedSlot = Utils.random(2)
    private var round = 0
    private var ticks = COUNTDOWN_TICKS
    private var elapsed = 0
    private var clock = 0

    val players get() = sides.map { it.player }
    val roundActive get() = phase == Phase.ROUND
    private val hunted get() = sides[huntedSlot]
    private val hunter get() = sides[1 - huntedSlot]

    init {
        for (side in sides) {
            side.player.stopAll()
            side.player.controllerManager.startController(FogController(this))
            SUPPLIES.forEach { side.player.inventory.addItem(Item(it.id, it.amount)) }
            side.player.sendMessage("You have been paired against ${opponentOf(side.player).displayName}. Your supplies must last both rounds.")
            side.player.lock()
        }
        placeForRound()
        WorldTasks.scheduleTimer(1, 0) { tick() }
    }

    private val present = sides.map { it.player }.toMutableSet()

    fun state(player: Player) = sides.firstOrNull { it.player == player && player in present }

    fun opponentOf(player: Player) = sides.first { it.player != player }.player

    fun isHunted(player: Player) = phase == Phase.ROUND && hunted.player == player

    fun isHunter(player: Player) = phase == Phase.ROUND && hunter.player == player

    fun role(player: Player) = if (hunted.player == player) ROLE_HUNTED else ROLE_HUNTER

    private fun tick(): Boolean {
        clock++
        when (phase) {
            Phase.COUNTDOWN -> countdown()
            Phase.ROUND -> playRound()
            Phase.BREAK -> if (--ticks <= 0) beginRound()
            Phase.ENDED -> return false
        }
        return phase != Phase.ENDED
    }

    private fun countdown() {
        if (--ticks > 0) {
            if (ticks <= 3) sides.forEach { it.player.forceTalk("$ticks") }
            return
        }
        beginRound()
    }

    private fun placeForRound() {
        val huntedStart = FogArena.dispensers.random()
        val hunterStart = FogArena.dispensers.maxBy { distance(it, huntedStart) }
        teleport(hunted.player, standTile(huntedStart))
        teleport(hunter.player, standTile(hunterStart))
    }

    private fun beginRound() {
        phase = Phase.ROUND
        elapsed = 0
        hunted.hadHuntedTurn = true
        hunted.scoringEvents = 0
        hunted.lastIncrement = 0
        for (side in sides) {
            side.player.unlock()
            side.player.forceTalk("Go!")
            sendHud(side.player)
        }
        hunted.player.sendMessage("<col=00ff00>You are the hunted. Take a stone from a dispenser and wield it to gather charge.")
        hunter.player.sendMessage("<col=ff0000>You are the hunter. Kill ${hunted.player.displayName} before they gather $CHARGE_CAP charges.")
        refreshHint()
    }

    private fun playRound() {
        elapsed++
        if (elapsed % PULSE_TICKS == 0) pulse()
        if (phase != Phase.ROUND) return
        hunted.house?.let { if (++hunted.houseClock % HOUSE_DAMAGE_TICKS == 0) damage(hunted.player, HOUSE_DAMAGE) }
        if (hunted.lastIncrement == CENTRE_RATE && hunted.house == null) hunted.player.restoreRunEnergy(CENTRE_RUN_RESTORE)
        refreshTimer()
        refreshStoneReminder()
        if (elapsed >= ROUND_TICKS) endRound("Time is up")
    }

    private fun pulse() {
        val side = hunted
        val player = side.player
        if (player.isDead || player.hasFinished() || side.house != null || side.teleporting || !holdsStone(player)) return
        val increment = FogArena.chargeRate(player.tile)
        side.score = (side.score + increment).coerceAtMost(CHARGE_CAP)
        side.lastIncrement = increment
        player.spotAnim(Spotanim.hvh_charge_spot)
        updateStone(player, side.score)
        if (++side.scoringEvents % DAMAGE_EVENTS == 0) weaken(side)
        refreshScores()
        if (side.score >= CHARGE_CAP) endRound("${player.displayName} has gathered the full $CHARGE_CAP charges")
    }

    private fun weaken(side: FogCombatant) {
        val player = side.player
        val drain = when {
            side.lastIncrement >= CENTRE_RATE -> 3
            side.lastIncrement > LOW_RATE -> 2
            else -> 1
        }
        for (stat in DRAINED_STATS) {
            val floor = player.skills.getLevelForXp(stat) * STAT_FLOOR_PERCENT / 100
            val level = player.skills.getLevel(stat)
            if (level > floor) player.skills.set(stat, max(floor, level - drain))
        }
        player.spotAnim(Spotanim.hvh_player_damage_spot)
        damage(player, if (side.lastIncrement > LOW_RATE) HIGH_STONE_DAMAGE else LOW_STONE_DAMAGE)
    }

    private fun damage(player: Player, amount: Int) {
        if (player.isDead || player.hasFinished()) return
        player.applyHit(Hit(amount, HitLook.TRUE_DAMAGE))
    }

    private fun holdsStone(player: Player) = player.equipment.weaponId in STONES

    private fun hasStone(player: Player) = holdsStone(player) || STONES.any { player.inventory.containsItem(it, 1) }

    private fun updateStone(player: Player, score: Int) {
        val stone = STONES[(score / STONE_STAGE_CHARGES).coerceAtMost(STONES.lastIndex)]
        val worn = player.equipment.getItem(Equipment.WEAPON) ?: return
        if (worn.id == stone) return
        worn.id = stone
        player.equipment.refresh(Equipment.WEAPON)
        player.appearance.generateAppearanceData()
    }

    fun takeStone(player: Player, dispenser: GameObject) {
        when {
            !isHunted(player) -> player.sendMessage("Only the hunted may take a stone from the dispenser.")
            hasStone(player) -> player.sendMessage("You already have a stone.")
            !player.inventory.hasFreeSlots() -> player.sendMessage("You need a free inventory space to take a stone.")
            else -> {
                player.faceObject(dispenser)
                player.anim(Seq.human_pickuptable)
                player.inventory.addItem(STONES[(hunted.score / STONE_STAGE_CHARGES).coerceAtMost(STONES.lastIndex)], 1)
                player.incrementCount(STONES_COUNTER)
                player.sendMessage("You take a stone from the dispenser. Wield it to begin gathering charge.")
            }
        }
    }

    fun passBarrier(player: Player, barrier: GameObject) {
        val house = FogArena.houseOfBarrier(barrier.tile) ?: return
        val side = state(player) ?: return
        val inner = house.innerSide(barrier.tile)
        if (side.house == house) {
            moveThrough(player, barrier, inner, barrier.tile) {
                side.house = null
                side.houseExitTick = clock
                refreshHint()
            }
            return
        }
        val refusal = when {
            !isHunted(player) -> "Only the hunted may enter the houses."
            clock - side.houseExitTick < REENTRY_TICKS -> "You must wait before entering another house."
            Utils.getDistanceI(player.tile, barrier.tile) > 1 || house.inside(player.tile) -> "You need to stand next to the barrier to pass through it."
            else -> null
        }
        if (refusal != null) return player.sendMessage(refusal)
        moveThrough(player, barrier, barrier.tile, inner) { enterHouse(side, house) }
    }

    private fun moveThrough(player: Player, barrier: GameObject, from: Tile, destination: Tile, after: () -> Unit) {
        World.sendObjectAnimation(barrier, Animation(Seq.hvh_barrier_door_thru))
        player.forceMove(from, destination, -1, 0, 30, true) { after() }
    }

    private fun enterHouse(side: FogCombatant, house: House) {
        side.house = house
        side.houseClock = 0
        val player = side.player
        player.resetCombat()
        if (side == hunted) {
            hunter.player.interactionManager.forceStop()
        }
        damage(player, player.hitpoints * HOUSE_ENTRY_PERCENT / 100)
        refreshHint()
    }

    fun usePortal(player: Player) {
        val side = state(player) ?: return
        val house = side.house ?: return
        if (!isHunted(player) || side.teleporting) return
        if (side.score < PORTAL_COST) return player.sendMessage("You need at least $PORTAL_COST charges to activate the portal.")
        side.score -= PORTAL_COST
        player.incrementCount(PORTALS_COUNTER)
        refreshScores()
        val succeeded = Utils.random(100) < PORTAL_SUCCESS_PERCENT
        val destination = if (succeeded) FogArena.houses.filter { it != house }.random() else null
        val landing = destination?.interior()?.random() ?: openGround.random()
        side.teleporting = true
        player.lock()
        player.anim(Seq.hvh_player_teleport)
        player.spotAnim(Spotanim.hvh_safehouse_portal_spot)
        WorldTasks.schedule(TELEPORT_TICKS) {
            side.teleporting = false
            if (state(player) == null || phase == Phase.ENDED) return@schedule
            player.unlock()
            player.tele(landing)
            player.anim(Seq.hvh_player_teleport_appear)
            player.spotAnim(Spotanim.hvh_portal_reverse_spot)
            if (destination != null) {
                side.house = destination
                side.houseClock = 0
                player.sendMessage("The portal carries you to another house.")
            } else {
                side.house = null
                side.houseExitTick = clock
                player.sendMessage("The portal's energy fizzles and throws you out into the open!")
            }
            refreshHint()
        }
    }

    fun useOrb(player: Player, slot: Int) {
        val side = state(player) ?: return
        val refusal = when {
            phase != Phase.ROUND || elapsed >= ROUND_TICKS - TELEPORT_TICKS -> "You can only use the tele-orb while a round is under way."
            side.house != null -> "You can't use the tele-orb inside a house."
            side.teleporting || player.isLocked -> "You can't do that right now."
            else -> null
        }
        if (refusal != null) return player.sendMessage(refusal)
        if (player.inventory.getItem(slot)?.id != Obj.hvh_teleport_tablet) return
        player.inventory.deleteItem(slot, Item(Obj.hvh_teleport_tablet, 1))
        player.incrementCount(ORBS_COUNTER)
        side.teleporting = true
        player.lock()
        player.anim(Seq.hvh_orb_teleport)
        player.spotAnim(Spotanim.hvh_orb_teleport_spot)
        WorldTasks.schedule(TELEPORT_TICKS) {
            side.teleporting = false
            if (state(player) == null || phase == Phase.ENDED) return@schedule
            player.unlock()
            player.tele(FogArena.centreTiles.random())
            player.anim(Seq.hvh_orb_teleport_arrive)
            player.spotAnim(Spotanim.hvh_orb_teleport_arrive_spot)
        }
    }

    fun inHouse(player: Player) = state(player)?.house != null

    fun died(player: Player) {
        val side = state(player) ?: return
        clearHint(side)
        player.reset()
        player.unlock()
        player.anim(-1)
        side.house = null
        if (phase == Phase.ROUND && side == hunted) {
            player.sendMessage("You have been killed. Your charge for this round stands at ${side.score}.")
            hunter.player.sendMessage("You have killed ${player.displayName}!")
            hunter.player.incrementCount(KILLS_COUNTER)
            stripStones(player)
            endRound("${player.displayName} has been killed")
            return
        }
        teleport(player, respawnTile())
        if (phase == Phase.ROUND) {
            player.sendMessage("You have been killed. The hunt goes on.")
            refreshHint()
        }
    }

    private fun respawnTile(): Tile {
        val target = hunted.player.tile
        val far = FogArena.dispensers.filter { distance(it, target) >= RESPAWN_MIN_DISTANCE }.ifEmpty { FogArena.dispensers }
        return standTile(far.random())
    }

    private fun endRound(reason: String) {
        if (phase != Phase.ROUND) return
        phase = Phase.BREAK
        ticks = ROUND_BREAK_TICKS
        refreshHint()
        val finished = hunted
        for (side in sides) {
            side.player.sendMessage("<col=ffff00>$reason. ${finished.player.displayName} finished the round with ${finished.score} charges.")
            side.player.vars.setVarBit(Varbit.hvh_played_hunted, if (side.hadHuntedTurn) 1 else 0)
        }
        if (round == 1) {
            finish()
            return
        }
        round++
        huntedSlot = 1 - huntedSlot
        for (side in sides) {
            restore(side)
            side.player.lock()
        }
        placeForRound()
    }

    private fun restore(side: FogCombatant) {
        val player = side.player
        stripStones(player)
        player.stopAll()
        player.reset()
        player.anim(-1)
        side.house = null
        side.houseExitTick = -REENTRY_TICKS
        side.teleporting = false
    }

    private fun finish() {
        phase = Phase.ENDED
        val (a, b) = sides
        val winner = when {
            a.score > b.score -> a
            b.score > a.score -> b
            else -> null
        }
        val results = sides.associateWith { FogResults.normal(it, sides.first { other -> other != it }, winner == it) }
        FogQueue.record(a.player, a.score, b.player, b.score, winner?.player)
        WorldTasks.schedule(FINISH_TICKS) {
            for ((side, result) in results) {
                if (state(side.player) == null) continue
                leave(side.player, null)
                FogResults.apply(side.player, result)
                if (side == winner) celebrate(side.player)
            }
        }
    }

    fun forfeit(player: Player, reason: LeaveReason) {
        val quitter = state(player) ?: return
        if (phase == Phase.ENDED) {
            leave(player, reason)
            return
        }
        val remaining = sides.first { it != quitter }
        remaining.score = when {
            !remaining.hadHuntedTurn -> max(remaining.score, UNSEEN_COMPENSATION)
            phase == Phase.ROUND && remaining == hunted -> remaining.score + MIDTURN_COMPENSATION
            else -> remaining.score
        }.coerceAtMost(CHARGE_CAP)
        phase = Phase.ENDED
        FogQueue.record(quitter.player, quitter.score, remaining.player, remaining.score, remaining.player)
        FogResults.apply(player, FogResults.forfeited(quitter))
        leave(player, reason)
        val result = FogResults.normal(remaining, quitter, true)
        remaining.player.sendMessage("<col=ffff00>${player.displayName} has left the arena. You win by forfeit with ${remaining.score} charges.")
        WorldTasks.schedule(FINISH_TICKS) {
            if (state(remaining.player) == null) return@schedule
            leave(remaining.player, null)
            FogResults.apply(remaining.player, result)
            celebrate(remaining.player)
        }
    }

    private fun leave(player: Player, reason: LeaveReason?) {
        val side = state(player) ?: return
        sides.forEach(::clearHint)
        side.house = null
        present -= player
        player.fogController?.match = null
        if (player.fogController != null) player.controllerManager.removeControllerWithoutCheck()
        cleanUp(player)
        when (reason) {
            LeaveReason.LOGOUT -> player.tile = FOG_LOBBY_RETURN
            LeaveReason.TELEPORT -> {}
            else -> player.tele(FOG_LOBBY_RETURN)
        }
        FogQueue.matchLeft(this, player, reason == null || reason == LeaveReason.FORFEIT)
    }

    private fun celebrate(player: Player) = WorldTasks.schedule(CELEBRATE_TICKS) {
        if (player.hasFinished()) return@schedule
        player.stopAll()
        player.anim(Seq.hvh_player_winner_pose)
        player.spotAnim(Spotanim.hvh_winners_spot)
    }

    fun sendHud(player: Player) {
        val side = state(player) ?: return
        val opponent = opponentOf(player)
        player.interfaceManager.sendOverlay(Interface.interface_730)
        player.vars.setVarBit(Varbit.hvh_player_a, side.slot)
        player.vars.setVarBit(Varbit.hvh_opponents_combat, opponent.skills.combatLevelWithSummoning)
        player.packets.sendVarcString(Varcstr.varcstr_217, opponent.displayName)
        side.shownStone = true
        refreshRole(player)
        refreshScores()
        refreshTimer()
        refreshStoneReminder()
    }

    private fun refreshRole(player: Player) {
        val hunting = role(player) == ROLE_HUNTER
        player.vars.setVarBit(Varbit.hvh_game_role, role(player))
        player.hide(Component.interface_730.component_730_19, !hunting)
        player.hide(Component.interface_730.component_730_20, hunting)
    }

    private fun refreshScores() {
        for (side in sides) {
            val other = sides.first { it != side }
            side.player.vars.setVarBit(Varbit.hvh_charge, side.score)
            side.player.vars.setVarBit(Varbit.hvh_opponents_charge, other.score)
            side.player.packets.setIFText(Component.interface_730.component_730_17, "Charge: ${hunted.score}")
        }
    }

    private fun refreshTimer() = sides.forEach { it.player.vars.setVarBit(Varbit.hvh_time, elapsed.coerceAtMost(ROUND_TICKS)) }

    private fun refreshStoneReminder() {
        for (side in sides) {
            val show = phase == Phase.ROUND && side == hunted && !hasStone(side.player)
            if (show == side.shownStone) continue
            side.shownStone = show
            side.player.hide(Component.interface_730.component_730_24, !show)
        }
    }

    private fun refreshHint() {
        for (side in sides) {
            clearHint(side)
            if (phase != Phase.ROUND || side.player !in present) continue
            val target = sides.first { it != side }
            if (target.player !in present || (side == hunter && target.house != null)) continue
            side.hintIndex = side.player.hintIconsManager.addHintIcon(target.player, HINT_ARROW, -1, true)
        }
    }

    private fun clearHint(side: FogCombatant) {
        if (side.hintIndex >= 0) side.player.hintIconsManager.removeHintIcon(side.hintIndex)
        side.hintIndex = -1
    }

    private fun teleport(player: Player, tile: Tile) {
        player.stopAll()
        player.tele(tile)
    }

    private fun standTile(dispenser: Tile): Tile =
        listOf(dispenser.transform(1, 0, 0), dispenser.transform(-1, 0, 0), dispenser.transform(0, 1, 0), dispenser.transform(0, -1, 0))
            .filter { World.floorAndWallsFree(it, 1) }
            .minByOrNull { FogArena.distanceFromCentre(it) } ?: dispenser.transform(0, -1, 0)

    private fun distance(a: Tile, b: Tile) = Utils.getDistanceI(a, b)

    companion object {
        private val openGround: List<Tile> by lazy {
            FogArena.tilesWithin(9.0, 20.0).filter { tile -> FogArena.houseAt(tile) == null && FogArena.houseOfBarrier(tile) == null && FogArena.houses.none { it.portal == tile } && World.floorAndWallsFree(tile, 1) }
        }

        fun stripStones(player: Player) {
            for (stone in STONES) {
                player.inventory.deleteItem(stone, Int.MAX_VALUE)
                player.equipment.deleteItem(stone, Int.MAX_VALUE)
            }
            player.appearance.generateAppearanceData()
        }

        fun cleanUp(player: Player) {
            for (id in MATCH_ITEMS) {
                player.inventory.deleteItem(id, Int.MAX_VALUE)
                player.equipment.deleteItem(id, Int.MAX_VALUE)
            }
            player.stopAll()
            player.reset()
            player.unlock()
            player.anim(-1)
            player.hintIconsManager.removeAll()
            player.setForceMultiArea(false)
            player.setCanPvp(false, false)
            HUD_VARBITS.forEach { player.vars.setVarBit(it, 0) }
            player.interfaceManager.removeOverlay()
            player.appearance.generateAppearanceData()
        }
    }
}

fun Player.hide(component: Int, hidden: Boolean) =
    packets.setIFHidden(Utils.interfaceIdFromHash(component), Utils.componentIdFromHash(component), hidden)
