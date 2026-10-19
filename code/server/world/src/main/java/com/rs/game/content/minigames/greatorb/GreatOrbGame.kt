package com.rs.game.content.minigames.greatorb

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.World
import com.rs.game.content.skills.runecrafting.Runecrafting
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.cache.EnumDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Enum
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.map.pathfinder.Direction
import org.darkan.core.model.Animation
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

const val ACTIVITY_MAX = 1000
const val GOP_XP_MULTIPLIER = 2.0
val GUILD_RETURN: Tile = Tile.of(1696, 5473, 2)

private const val GAMES_COMPLETED_COUNTER = "Great Orb Project games completed"
private const val GAMES_WON_COUNTER = "Great Orb Project games won"
private const val ALTARS_COUNTER = "Great Orb Project altars captured"
private const val ORBS_SCORED_COUNTER = "Great Orb Project orbs scored"
private const val ORBS_NUDGED_COUNTER = "Great Orb Project enemy orbs nudged"
private const val BARRIERS_RAISED_COUNTER = "Great Orb Project barriers raised"
private const val BARRIERS_DESTROYED_COUNTER = "Great Orb Project barriers destroyed"
private const val TOKENS_COUNTER = "Great Orb Project tokens earned"
private const val ROUND_TICKS = 200
private const val BREAK_TICKS = 25
private const val ENEMY_LOCKOUT_TICKS = 50
private const val ENEMY_COOLDOWN = 5
private const val ORBS_PER_TEAM = 3
private const val RESPAWN_DELAY = 3
private const val DESPAWN_DELAY = 2
private const val ACTIVITY_DRAIN = 8
private const val ATTRACT_REACH = 10
private const val REPEL_REACH = 8
private const val ROBE_PIECES_FOR_BONUS = 2
private const val MIN_SPAWN_DISTANCE = 5
private const val BARRIER_ALTAR_CLEARANCE = 4
private const val BARRIER_EXIT_CLEARANCE = 2
private const val WIN_TOKENS = 100
private const val DRAW_TOKENS = 25
private const val LOSS_TOKENS = 10
private const val OVERALL_BONUS = 200
private const val OPENING_TICKS = 5
private const val WAND_ANIM_INTERVAL = 4
private val ABANDON_EXCLUSION = Ticks.fromMinutes(4)
private val CONTRIBUTED = intArrayOf(
    Varbit.rcguild_contributed_1, Varbit.rcguild_contributed_2, Varbit.rcguild_contributed_3, Varbit.rcguild_contributed_4,
    Varbit.rcguild_contributed_5, Varbit.rcguild_contributed_6, Varbit.rcguild_contributed_7, Varbit.rcguild_contributed_8,
)
private val ALTAR_ICONS = intArrayOf(
    Component.interface_781.component_781_15, Component.interface_781.component_781_16, Component.interface_781.component_781_17, Component.interface_781.component_781_18,
    Component.interface_781.component_781_19, Component.interface_781.component_781_20, Component.interface_781.component_781_21, Component.interface_781.component_781_22,
)
private val ALTAR_RESULTS = intArrayOf(
    Component.interface_781.component_781_23, Component.interface_781.component_781_24, Component.interface_781.component_781_25, Component.interface_781.component_781_26,
    Component.interface_781.component_781_27, Component.interface_781.component_781_28, Component.interface_781.component_781_29, Component.interface_781.component_781_30,
)
private val CARDINALS = listOf(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)

class GreatOrbGame(roster: Map<OrbTeam, List<Player>>) {
    private enum class Phase { ACTIVE, BREAK, ENDED }

    private val players = LinkedHashMap<Player, OrbTeam>().apply { roster.forEach { (team, members) -> members.forEach { put(it, team) } } }
    private val orbs = ArrayList<Orb>()
    private val barriers = HashMap<Player, GameObject>()
    private val enemyCooldown = HashMap<Player, Int>()
    private val scores = IntArray(OrbTeam.entries.size)
    private val results = ArrayList<OrbTeam?>()
    private var phase = Phase.BREAK
    private var round = -1
    private var ticksLeft = OPENING_TICKS
    private var portal: NPC? = null
    private var spawnTiles = emptyList<Tile>()
    private var timerAnim = -1

    var clock = 0
        private set

    private val altar get() = OrbAltar.entries[round.coerceAtLeast(0)]
    private val lastRound get() = round == OrbAltar.entries.lastIndex

    init {
        players.keys.forEach { it.greatOrbController?.game = this }
        WorldTasks.scheduleTimer(1, 0) { tick() }
    }

    private fun tick(): Boolean {
        clock++
        when (phase) {
            Phase.ACTIVE -> {
                ticksLeft--
                orbs.toList().forEach(Orb::tick)
                drainActivity()
                if (phase == Phase.ACTIVE) {
                    refreshTimers()
                    if (ticksLeft <= 0) endRound()
                }
            }
            Phase.BREAK -> if (--ticksLeft <= 0) if (lastRound) finish(true) else beginRound()
            Phase.ENDED -> return false
        }
        return phase != Phase.ENDED
    }

    private fun beginRound() {
        round++
        portal?.finish()
        portal = null
        for (player in players.keys) {
            if (!player.withinDistance(altar.arrival, altar.spawnRadius)) player.tele(arrivalTile())
            player.vars.setVarBit(Varbit.rcguild_activity, ACTIVITY_MAX)
        }
        scores.fill(0)
        spawnTiles = findSpawnTiles()
        OrbTeam.entries.forEach { team -> repeat(ORBS_PER_TEAM) { spawnOrb(team) } }
        ticksLeft = ROUND_TICKS
        timerAnim = -1
        phase = Phase.ACTIVE
        sendAll("The orbs have appeared at the ${altar.name.lowercase()} altar!")
        players.keys.forEach(::refreshHud)
    }

    private fun arrivalTile(): Tile = altar.arrival.let { Tile.of(it.x + Utils.random(-1, 1), it.y + Utils.random(0, 1), it.plane) }

    private fun findSpawnTiles(): List<Tile> {
        val centre = altar.anchor.transform(1, 1)
        val seen = HashSet<Tile>()
        val queue = ArrayDeque(listOf(altar.arrival))
        seen += queue.first()
        while (queue.isNotEmpty()) {
            val tile = queue.removeFirst()
            for (direction in CARDINALS) {
                val next = tile.transform(direction.dx, direction.dy)
                if (next in seen || chebyshev(next, centre) > altar.spawnRadius || !World.checkWalkStep(tile, direction, 1)) continue
                seen += next
                queue += next
            }
        }
        return seen.filter { chebyshev(it, centre) >= MIN_SPAWN_DISTANCE }
    }

    private fun spawnOrb(team: OrbTeam) {
        val tile = spawnTiles.filter { tile -> orbs.none { it.tile == tile } && players.keys.none { it.tile == tile } && barriers.values.none { it.tile == tile } }.randomOrNull() ?: return
        orbs += Orb(this, team, tile)
    }

    fun inScoringArea(tile: Tile): Boolean {
        val anchor = altar.anchor
        return tile.plane == anchor.plane && tile.x in anchor.x - 1..anchor.x + 3 && tile.y in anchor.y - 1..anchor.y + 3
    }

    fun score(orb: Orb) {
        orbs.remove(orb)
        scores[orb.team.ordinal]++
        orb.lastMover?.takeIf { players[it] == orb.team }?.incrementCount(ORBS_SCORED_COUNTER)
        val scoredRound = round
        WorldTasks.schedule(DESPAWN_DELAY) { orb.finish() }
        WorldTasks.schedule(RESPAWN_DELAY) { if (phase == Phase.ACTIVE && round == scoredRound) spawnOrb(orb.team) }
        players.keys.forEach(::refreshScores)
    }

    fun instruct(player: Player, orb: Orb, changeWand: Boolean) {
        if (changeWand) swapWand(player)
        if (!mayInstruct(player, orb, quiet = false)) return
        val team = players.getValue(player)
        val attract = player.equipment.weaponId == team.attractor
        val bonus = if (orb.team.robes.count { player.equipment.containsOneItem(it) } >= ROBE_PIECES_FOR_BONUS) 1 else 0
        player.interactionManager.setInteraction(OrbControl(this, orb, attract, (if (attract) ATTRACT_REACH else REPEL_REACH) + bonus))
    }

    fun mayInstruct(player: Player, orb: Orb, quiet: Boolean): Boolean {
        val team = players[player] ?: return false
        val refusal = when {
            phase != Phase.ACTIVE || orb.scored -> "The orbs can only be moved while the altar is active."
            player.equipment.weaponId != team.attractor && player.equipment.weaponId != team.repeller -> "You need to wield one of your wands to move the orbs."
            orb.team != team && ticksLeft <= ENEMY_LOCKOUT_TICKS -> "You can't move the other team's orbs in the last 30 seconds."
            orb.team != team && clock < (enemyCooldown[player] ?: 0) -> "Your wand needs a moment before it can move another enemy orb."
            orb.team == team && orb.holder.let { it != null && it != player && it in players } -> "Someone on your team is already moving that orb."
            else -> return true
        }
        if (!quiet) player.sendMessage(refusal)
        return false
    }

    fun instructed(player: Player, orb: Orb, attract: Boolean, first: Boolean) {
        val team = players[player] ?: return
        if (orb.team == team) orb.hold(player, attract)
        else {
            orb.nudge(player, attract)
            player.incrementCount(ORBS_NUDGED_COUNTER)
            enemyCooldown[player] = clock + ENEMY_COOLDOWN
        }
        player.vars.setVarBit(CONTRIBUTED[round], 1)
        player.vars.setVarBit(Varbit.rcguild_activity, ACTIVITY_MAX)
        if (first || clock % WAND_ANIM_INTERVAL == 0) {
            player.anim(Seq.rcguild_player_wand_attract)
            player.spotAnim(if (attract) team.attractSpotanim else team.repelSpotanim)
        }
    }

    private fun swapWand(player: Player) {
        val team = players[player] ?: return
        val other = if (player.equipment.weaponId == team.attractor) team.repeller else team.attractor
        if (!player.inventory.containsOneItem(other)) return
        player.inventory.deleteItem(other, 1)
        player.equipment.setSlot(Equipment.WEAPON, Item(other))?.takeIf { it.id > 0 }?.let { player.inventory.addItem(it) }
        player.appearance.generateAppearanceData()
    }

    fun makeBarrier(player: Player) {
        val team = players[player] ?: return
        val tile = player.tile
        val refusal = when {
            phase != Phase.ACTIVE -> "You can only raise a barrier while the altar is active."
            chebyshev(tile, altar.anchor.transform(1, 1)) <= BARRIER_ALTAR_CLEARANCE -> "You can't raise a barrier this close to the altar."
            World.getAllObjectsInChunkRange(tile.getChunkId(), 1).any { it.id in altar.exits && chebyshev(it.tile, tile) <= BARRIER_EXIT_CLEARANCE } -> "You can't raise a barrier this close to the exit."
            orbs.any { it.tile == tile } -> "There is an orb in the way."
            barriers.values.any { it.tile == tile } -> "There is already a barrier here."
            else -> null
        }
        if (refusal != null) {
            player.sendMessage(refusal)
            return
        }
        barriers.remove(player)?.let(World::removeObject)
        val barrier = GameObject(team.barrier, ObjectShape.SCENERY_INTERACT, 0, tile)
        World.spawnObject(barrier)
        World.sendObjectAnimation(barrier, Animation(Seq.rcguild_magic_barrier_spawn))
        barriers[player] = barrier
        player.incrementCount(BARRIERS_RAISED_COUNTER)
        player.anim(Seq.rcguild_player_set_barrier)
        player.spotAnim(team.barrierSpotanim)
        player.vars.setVarBit(Varbit.rcguild_activity, ACTIVITY_MAX)
    }

    fun passBarrier(player: Player, barrier: GameObject) {
        if (player !in players) return
        val dx = barrier.x - player.x
        val dy = barrier.y - player.y
        val destination = barrier.tile.transform(dx, dy)
        if (abs(dx) > 1 || abs(dy) > 1 || (dx == 0 && dy == 0) || !World.floorAndWallsFree(destination, 1) || barriers.values.any { it.tile == destination }) {
            player.sendMessage("You can't pass through the barrier from here.")
            return
        }
        player.forceMove(destination, 0, 30)
    }

    fun destroyBarrier(player: Player, barrier: GameObject) {
        val team = players[player] ?: return
        if (barriers.values.none { it == barrier }) return
        player.anim(Seq.rcguild_player_remove_barrier)
        player.vars.setVarBit(Varbit.rcguild_activity, ACTIVITY_MAX)
        if (barrier.id != team.barrier && Utils.random(2) != 0) {
            player.sendMessage("The barrier resists your attempt to destroy it.")
            return
        }
        barriers.entries.removeIf { it.value == barrier }
        World.removeObject(barrier)
        player.incrementCount(BARRIERS_DESTROYED_COUNTER)
    }

    fun enterPortal(player: Player) {
        if (player !in players || phase != Phase.BREAK) return
        OrbAltar.entries.getOrNull(round + 1)?.let { player.tele(it.arrival) }
    }

    fun handleObject(player: Player, obj: GameObject): Boolean {
        if (player !in players) return true
        val crafted = OrbAltar.entries.firstOrNull { it.altar == obj.id }
        return when {
            crafted != null -> {
                Runecrafting.runecraft(player, crafted.rune, false, GOP_XP_MULTIPLIER)
                false
            }
            OrbAltar.entries.any { obj.id in it.exits } -> {
                player.sendOptionsDialogue("Leave the Great Orb Project? You won't be able to rejoin for a few minutes.") {
                    opExec("Yes, leave the game.") { player.controllerManager.forceStop() }
                    op("No, keep playing.")
                }
                false
            }
            else -> true
        }
    }

    private fun drainActivity() {
        for (player in players.keys.toList()) {
            if (player !in players) continue
            val activity = player.vars.getVarBit(Varbit.rcguild_activity) - ACTIVITY_DRAIN
            if (activity > 0) {
                player.vars.setVarBit(Varbit.rcguild_activity, activity)
                continue
            }
            player.sendMessage("You have been removed from the game for inactivity.")
            player.controllerManager.forceStop()
        }
    }

    private fun endRound() {
        val green = scores[OrbTeam.GREEN.ordinal]
        val yellow = scores[OrbTeam.YELLOW.ordinal]
        val winner = when {
            green > yellow -> OrbTeam.GREEN
            yellow > green -> OrbTeam.YELLOW
            else -> null
        }
        results += winner
        orbs.forEach(NPC::finish)
        orbs.clear()
        barriers.values.forEach(World::removeObject)
        barriers.clear()
        val outcome = winner?.let { "The ${it.displayName} team have captured the ${altar.name.lowercase()} altar, $green to $yellow." } ?: "The ${altar.name.lowercase()} altar is a draw, $green all."
        for ((player, team) in players) {
            player.sendMessage(outcome)
            if (player.vars.getVarBit(CONTRIBUTED[round]) == 1)
                player.inventory.addItemDrop(Obj.blankrune_high, max(1, scores[team.ordinal] - scores[team.opponent.ordinal] / 2 + 1))
            refreshResults(player)
        }
        phase = Phase.BREAK
        ticksLeft = BREAK_TICKS
        if (lastRound) sendAll("That was the last altar. Craft any essence you have before the project wraps up.")
        else portal = NPC(Npc.rcguild_portal, altar.arrival.transform(0, -1), true).apply { setRandomWalk(false) }
        players.keys.forEach(::refreshHud)
    }

    private fun finish(completed: Boolean) {
        if (phase == Phase.ENDED) return
        phase = Phase.ENDED
        val finalists = players.toList()
        players.clear()
        for ((player, team) in finalists) {
            val tokens = tokensEarned(player, team, completed)
            player.controllerManager.forceStop()
            WorldTasks.schedule(1) { settle(player, team, tokens, completed) }
        }
        orbs.forEach(NPC::finish)
        barriers.values.forEach(World::removeObject)
        portal?.finish()
        GreatOrbLobby.gameOver(this)
    }

    private fun tokensEarned(player: Player, team: OrbTeam, completed: Boolean): Int {
        val altarTokens = results.indices.filter { player.vars.getVarBit(CONTRIBUTED[it]) == 1 }.sumOf {
            when (results[it]) {
                team -> WIN_TOKENS
                null -> DRAW_TOKENS
                else -> LOSS_TOKENS
            }
        }
        val won = results.count { it == team }
        val lost = results.count { it == team.opponent }
        return altarTokens + when {
            !completed || altarTokens == 0 -> 0
            won > lost -> OVERALL_BONUS
            won == lost -> OVERALL_BONUS / 2
            else -> 0
        }
    }

    private fun settle(player: Player, team: OrbTeam, tokens: Int, completed: Boolean) {
        val won = results.count { it == team }
        val lost = results.count { it == team.opponent }
        if (tokens > 0) {
            player.inventory.addItemDrop(Obj.rcguild_reward_token, tokens)
            player.incrementCount(TOKENS_COUNTER, tokens)
        }
        if (won > 0) player.incrementCount(ALTARS_COUNTER, won)
        player.sendMessage("Your team captured $won altar${if (won == 1) "" else "s"} and lost $lost. You have been awarded ${Utils.formatNumber(tokens)} Runecrafting guild tokens.")
        if (!completed) return
        player.incrementCount(GAMES_COMPLETED_COUNTER)
        if (won > lost) player.incrementCount(GAMES_WON_COUNTER)
    }

    fun leave(player: Player) {
        players.remove(player) ?: return
        barriers.remove(player)?.let(World::removeObject)
        orbs.forEach { it.release(player) }
        enemyCooldown.remove(player)
        if (phase == Phase.ENDED) return
        player.tempAttribs.setL(GreatOrbLobby.EXCLUDED_UNTIL, World.getServerTicks() + ABANDON_EXCLUSION)
        if (OrbTeam.entries.any { team -> players.values.count { it == team } < GreatOrbLobby.MIN_TEAM }) {
            sendAll("Too many runecrafters have left; the Great Orb Project is over for now.")
            finish(false)
        }
    }

    private fun sendAll(message: String) = players.keys.forEach { it.sendMessage(message) }

    fun sendHud(player: Player) {
        player.interfaceManager.sendOverlay(Interface.interface_781)
        OrbAltar.entries.forEachIndexed { index, it -> player.setModel(ALTAR_ICONS[index], it.icon) }
        timerAnim = -1
        refreshHud(player)
    }

    private fun refreshHud(player: Player) {
        val team = players[player] ?: return
        OrbTeam.entries.forEach { player.hide(it.panel, it != team) }
        player.hide(Component.interface_781.component_781_1, phase != Phase.BREAK)
        player.hide(Component.interface_781.component_781_2, phase == Phase.ACTIVE)
        player.packets.setIFText(Component.interface_781.component_781_56, when (phase) {
            Phase.BREAK -> OrbAltar.entries.getOrNull(round + 1)?.let { "Next: the ${it.name.lowercase()} altar" } ?: ""
            else -> ""
        })
        refreshScores(player)
        refreshResults(player)
        refreshTimer(player, team)
    }

    private fun refreshScores(player: Player) {
        val team = players[player] ?: return
        player.packets.setIFText(team.ownScore, scores[team.ordinal].toString())
        player.packets.setIFText(team.enemyScore, scores[team.opponent.ordinal].toString())
    }

    private fun refreshResults(player: Player) = results.forEachIndexed { index, winner -> player.setModel(ALTAR_RESULTS[index], winner?.resultModel ?: -1) }

    private fun refreshTimers() {
        val anim = timerFrame()
        if (anim == timerAnim) return
        timerAnim = anim
        for ((player, team) in players) refreshTimer(player, team)
    }

    private fun refreshTimer(player: Player, team: OrbTeam) =
        player.packets.setIFAnimation(timerFrame(), Utils.interfaceIdFromHash(team.timer), Utils.componentIdFromHash(team.timer))

    private fun timerFrame() = EnumDefinitions.getEnum(Enum.rcguild_minigame_timer).getIntValue(if (phase == Phase.ACTIVE) ceil(ticksLeft * 0.6).toInt() else 0)
}

private fun chebyshev(a: Tile, b: Tile) = max(abs(a.x - b.x), abs(a.y - b.y))

fun Player.hide(component: Int, hidden: Boolean) =
    packets.setIFHidden(Utils.interfaceIdFromHash(component), Utils.componentIdFromHash(component), hidden)

private fun Player.setModel(component: Int, model: Int) =
    packets.setIFModel(Utils.interfaceIdFromHash(component), Utils.componentIdFromHash(component), model)
