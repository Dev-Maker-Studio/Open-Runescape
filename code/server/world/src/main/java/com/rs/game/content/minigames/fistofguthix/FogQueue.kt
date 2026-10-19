package com.rs.game.content.minigames.fistofguthix

import com.rs.game.content.skills.cooking.Foods
import com.rs.game.content.Potion
import com.rs.game.content.skills.magic.Rune
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import kotlin.math.ceil

val FOG_LOBBY_ARRIVAL: Tile = Tile.of(1677, 5598, 0)
val FOG_LOBBY_RETURN: Tile = Tile.of(1710, 5599, 0)
val GROTTO_RETURN: Tile = Tile.of(2969, 9672, 0)
private val LOBBY_PASSAGE_SIDE: Tile = Tile.of(1718, 5598, 0)
private val WAITING_PASSAGE_SIDE: Tile = Tile.of(1652, 5600, 0)
private val LOBBY_AREA = 1674..1721 to 5593..5607
private val WAITING_AREA = 1605..1656 to 5583..5621

const val LOBBY_MINIMUM = 8
const val LOBBY_FULL = 16
private const val ACTIVITY_CAP = 250
private const val FULL_COUNTDOWN = 25
private val MINIMUM_COUNTDOWN = Ticks.fromMinutes(1)
private const val SCOREBOARD_SIZE = 50
private const val PASSAGE_TICKS = 2

fun inFogLobby(tile: Tile) = tile.plane == 0 && tile.x in LOBBY_AREA.first && tile.y in LOBBY_AREA.second
fun inFogWaitingRoom(tile: Tile) = tile.plane == 0 && tile.x in WAITING_AREA.first && tile.y in WAITING_AREA.second

object FogQueue {
    private val waiting = LinkedHashSet<Player>()
    private val matches = LinkedHashSet<FogMatch>()
    private val scoreboard = ArrayDeque<String>()
    private var countdown = 0
    private var running = false

    val results: List<String> get() = scoreboard.toList()

    private fun participants() = waiting.size + matches.sumOf { it.players.size }

    fun enterWaitingRoom(player: Player) {
        val refusal = when {
            participants() >= ACTIVITY_CAP -> "The arena is full at the moment. Try again shortly."
            player.inventory.freeSlots < SUPPLY_SLOTS -> "You need $SUPPLY_SLOTS free inventory spaces for your match supplies."
            else -> null
        }
        if (refusal != null) return player.simpleDialogue(refusal)
        forbiddenItem(player)?.let {
            player.fogLobbyController?.showForbidden(it)
            return player.simpleDialogue("You can't take ${ItemDefinitions.getDefs(it.id).name.lowercase()} into the arena.")
        }
        player.fogLobbyController?.showForbidden(null)
        player.useStairs(-1, WAITING_PASSAGE_SIDE, 1, PASSAGE_TICKS)
        WorldTasks.schedule(PASSAGE_TICKS) {
            if (player.hasFinished() || !inFogWaitingRoom(player.tile) || player.fogLobbyController == null) return@schedule
            waiting += player
            player.sendMessage("You enter the waiting room. Matches begin a minute after $LOBBY_MINIMUM players are waiting, or at once with $LOBBY_FULL.")
            ensureTimer()
            refreshAll()
        }
    }

    fun leaveWaitingRoom(player: Player) {
        leave(player)
        player.useStairs(-1, LOBBY_PASSAGE_SIDE, 1, PASSAGE_TICKS)
    }

    fun leave(player: Player) {
        if (waiting.remove(player)) refreshAll()
    }

    fun isWaiting(player: Player) = player in waiting

    private fun ensureTimer() {
        if (running) return
        running = true
        countdown = 0
        WorldTasks.scheduleTimer(1, 0) {
            waiting.removeIf { it.hasFinished() || !inFogWaitingRoom(it.tile) }
            if (waiting.isEmpty() && matches.isEmpty()) {
                running = false
                return@scheduleTimer false
            }
            val before = countdown
            countdown = when {
                waiting.size >= LOBBY_FULL -> if (countdown in 1..FULL_COUNTDOWN) countdown else FULL_COUNTDOWN
                waiting.size >= LOBBY_MINIMUM -> if (countdown > 0) countdown else MINIMUM_COUNTDOWN
                else -> 0
            }
            if (countdown > 0 && --countdown == 0) pair()
            if ((countdown > 0 && countdown % 5 == 0) || (before == 0) != (countdown == 0)) refreshAll()
            true
        }
    }

    private fun pair() {
        val entrants = waiting.toList().let { if (it.size % 2 == 1) it.dropLast(1) else it }
        waiting.removeAll(entrants.toSet())
        entrants.sortedBy { FogResults.rating(it) }.chunked(2).forEach { (first, second) -> matches += FogMatch(first, second) }
        refreshAll()
    }

    fun matchLeft(match: FogMatch, player: Player, returnToLobby: Boolean) {
        if (match.players.all { match.state(it) == null }) matches -= match
        if (returnToLobby) WorldTasks.schedule(PASSAGE_TICKS) {
            if (!player.hasFinished() && player.controllerManager.controller == null && inFogLobby(player.tile)) player.controllerManager.startController(FogLobbyController())
        }
    }

    fun record(first: Player, firstScore: Int, second: Player, secondScore: Int, winner: Player?) {
        val outcome = winner?.let { "${it.displayName} won" } ?: "Tie: both lost"
        scoreboard.addFirst("${first.displayName} $firstScore - $secondScore ${second.displayName} ($outcome)")
        while (scoreboard.size > SCOREBOARD_SIZE) scoreboard.removeLast()
    }

    private fun forbiddenItem(player: Player): Item? = (player.inventory.items.array().toList() + player.equipment.itemsCopy.toList())
        .filterNotNull()
        .firstOrNull { isForbidden(it) }

    private fun isForbidden(item: Item): Boolean {
        val defs = ItemDefinitions.getDefs(item.id)
        return Foods.isConsumable(item) || Potion.POTS.containsKey(item.id) || Rune.forId(item.id) != null || defs.isNoted() || item.id in MATCH_ITEMS
    }

    private fun refreshAll() = waiting.forEach { it.fogLobbyController?.refresh() }

    fun status(player: Player): String = when {
        player !in waiting -> "Walk through the passageway to join the waiting room."
        countdown == 0 -> "Waiting for players: ${waiting.size}/$LOBBY_MINIMUM"
        else -> "Pairing in ${ceil(countdown * 0.6).toInt()} seconds (${waiting.size} waiting)"
    }
}

val Player.fogLobbyController get() = controllerManager.controller as? FogLobbyController

class FogLobbyController : Controller() {
    override fun start() = sendInterfaces()

    override fun sendInterfaces() {
        player.interfaceManager.sendOverlay(Interface.interface_731)
        showForbidden(null)
        refresh()
    }

    fun refresh() {
        player.packets.setIFText(Component.interface_731.component_731_7, "Rating: ${FogResults.rating(player)}")
        player.packets.setIFText(Component.interface_731.component_731_16, FogQueue.status(player))
    }

    fun showForbidden(item: Item?) {
        player.hide(Component.interface_731.component_731_26, item == null)
        if (item != null) player.packets.setIFText(Component.interface_731.component_731_25, "The following item is not allowed in the arena:<br>${ItemDefinitions.getDefs(item.id).name}")
    }

    override fun process() {
        if (!inFogLobby(player.tile) && !inFogWaitingRoom(player.tile) && !player.isLocked) player.controllerManager.forceStop()
    }

    override fun onTeleported(type: TeleType) = player.controllerManager.forceStop()

    override fun logout(): Boolean {
        FogQueue.leave(player)
        if (inFogWaitingRoom(player.tile)) player.tile = FOG_LOBBY_RETURN
        return false
    }

    override fun login(): Boolean {
        if (inFogWaitingRoom(player.tile)) player.tile = FOG_LOBBY_RETURN
        sendInterfaces()
        return false
    }

    override fun forceClose() {
        FogQueue.leave(player)
        player.interfaceManager.removeOverlay()
    }
}
