package com.rs.utils.record

import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.utils.record.impl.ClickHW
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.abs

private const val ACTION_LIMIT = 100
private const val CROSS_PLAYER_MILLIS = 300

class Recorder(private val player: Player) {
    private val actions = ArrayDeque<RecordedAction>()
    private var activeWatch: Any? = null

    fun record(action: RecordedAction) {
        player.refreshIdleTime()
        actions.addLast(action)
        if (actions.size > ACTION_LIMIT)
            actions.removeFirst()
    }

    val ticksSinceLastAction: Long get() = actions.lastOrNull()?.let { World.getServerTicks() - it.tick } ?: Long.MAX_VALUE

    fun showActions(displayNames: Array<String>) {
        val targets = onlineTargets(displayNames) ?: return
        val history = mergedActions(targets)
        if (history.isEmpty()) {
            player.packets.sendDevConsoleMessage("No recorded actions for ${displayNames.joinToString()}.")
            return
        }
        player.packets.sendDevConsoleMessage("Past actions for: ${displayNames.joinToString()}")
        val startTime = history.first().second.timeLogged
        history.forEachIndexed { i, action -> log(action, history.getOrNull(i - 1), startTime) }
    }

    fun watch(displayNames: Array<String>) {
        val targets = onlineTargets(displayNames) ?: return
        val watch = Any()
        activeWatch = watch
        val printed: MutableSet<RecordedAction> = Collections.newSetFromMap(IdentityHashMap())
        var startTime = -1L
        var prev: Pair<String, RecordedAction>? = null
        player.packets.sendDevConsoleMessage("Watching ${targets.keys.joinToString()}. Use ::stopwatch to stop.")
        WorldTasks.scheduleTimer { _ ->
            if (activeWatch !== watch) return@scheduleTimer false
            if (!player.isRunning || player.hasFinished()) {
                activeWatch = null
                return@scheduleTimer false
            }
            val loggedOut = targets.filterValues { !it.isRunning || it.hasFinished() }.keys
            if (loggedOut.isNotEmpty()) {
                player.packets.sendDevConsoleMessage("Stopped watching: ${loggedOut.joinToString()} logged out.")
                activeWatch = null
                return@scheduleTimer false
            }
            val current = mergedActions(targets)
            printed.retainAll(current.mapTo(Collections.newSetFromMap(IdentityHashMap())) { it.second })
            for (action in current) {
                if (!printed.add(action.second)) continue
                if (startTime == -1L) startTime = action.second.timeLogged
                log(action, prev, startTime)
                prev = action
            }
            true
        }
    }

    fun stopWatching(): Boolean {
        val watching = activeWatch != null
        activeWatch = null
        return watching
    }

    private fun onlineTargets(displayNames: Array<String>): Map<String, Player>? {
        if (displayNames.isEmpty()) {
            player.sendMessage("Name at least one player, with underscores for spaces.")
            return null
        }
        val targets = displayNames.associateWith { World.getPlayerByDisplay(it) }
        val offline = targets.filterValues { it == null }.keys
        if (offline.isNotEmpty()) {
            player.sendMessage("Not online: ${offline.joinToString()}.")
            return null
        }
        return targets.mapValues { it.value!! }
    }

    private fun mergedActions(targets: Map<String, Player>): List<Pair<String, RecordedAction>> =
        targets.flatMap { (name, target) -> target.recorder.actions.map { name to it } }.sortedBy { it.second }

    private fun log(action: Pair<String, RecordedAction>, prev: Pair<String, RecordedAction>?, startTime: Long) {
        val (name, recorded) = action
        val crossPlayer = prev != null && prev.first != name && abs(recorded.timeLogged - prev.second.timeLogged) < CROSS_PLAYER_MILLIS
        val synthetic = recorded is ClickHW && !recorded.isHardware
        val highlight = if (crossPlayer || synthetic) "<shad=000000><col=FF0000>" else ""
        player.packets.sendDevConsoleMessage("$highlight[${formatElapsed(recorded.timeLogged - startTime)}] $name: $recorded")
    }

    private fun formatElapsed(millis: Long): String = "%02d:%02d:%04d".format(millis / 60_000 % 60, millis / 1000 % 60, millis % 1000)
}
