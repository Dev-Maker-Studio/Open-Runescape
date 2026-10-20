package com.darkan.bot

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.interaction.chat.ChatBox

object Describe {
    private const val LIMIT = 12

    private fun tile(entity: PathingEntity) = (entity.movementBaseX[0] + Bot.getBaseX()) to (entity.movementBaseY[0] + Bot.getBaseY())

    private fun section(title: String, rows: List<Triple<Int, String, Pair<Int, Int>>>, px: Int, py: Int): String {
        if (rows.isEmpty()) return "$title: none"
        val sorted = rows.sortedBy { maxOf(kotlin.math.abs(it.third.first - px), kotlin.math.abs(it.third.second - py)) }
        val shown = sorted.take(LIMIT).joinToString("; ") { "${it.second} @${it.third.first},${it.third.second} d${maxOf(kotlin.math.abs(it.third.first - px), kotlin.math.abs(it.third.second - py))}" }
        return "$title (${rows.size}): $shown" + if (rows.size > LIMIT) "; ..." else ""
    }

    fun render(): String {
        val out = StringBuilder("tick ${Footprints.gameTick} state ${Client.gameState}")
        if (Bot.isLoggedOut()) return out.toString()
        val me = Bot.getMyPlayer()
        val (px, py, pz) = Bot.getMyPlayerPosition().let { Triple(it.x, it.y, it.z) }
        out.append("\npos $px,$py,$pz region ${(px shr 6 shl 8) or (py shr 6)} name ${me.displayName ?: me.username}")
        out.append("\nhp ${Bot.getHealth().toInt()} prayer ${Bot.getPrayer().toInt()} run ${Client.RUN_ENERGY} anim ${Bot.getCurrentAnimation()} moving ${me.movementStepsLeft > 0} combat ${Bot.myPlayerInCombat()}")
        out.append("\ninterfaces: ").append(Footprints.openInterfaces().sorted().joinToString(" ").ifEmpty { "none" })
        out.append('\n').append(section("npcs", Bot.getNPCs().map { Triple(it.playerIndex, "${it.definitions!!.name}#${it.definitions!!.type}", tile(it)) }, px, py))
        val players = (0 until PlayerUpdate.NUM_OUT_PLAYER_INDICES).mapNotNull { PlayerUpdate.players[PlayerUpdate.OUT_PLAYER_INDICES[it]] }.filter { it !== me }
        out.append('\n').append(section("players", players.map { Triple(0, it.displayName ?: it.username ?: "?", tile(it)) }, px, py))
        out.append('\n').append(section("objects", Bot.getNearbyObjects().distinctBy { Triple(it.id, it.x, it.y) }.map { Triple(it.id, "${it.getDefinitions().name}#${it.id}", it.x to it.y) }.filter { !it.second.startsWith("null") }, px, py))
        out.append('\n').append(section("ground", Bot.getGroundItems().map { Triple(it.id, "${it.getName()}#${it.id}x${it.amount}", it.position.x to it.position.y) }, px, py))
        out.append("\nchat:")
        ChatBox.CHAT_LINES.take(5).filterNotNull().reversed().forEach { out.append("\n  [${it.type}] ${it.simpleName ?: ""}: ${it.message}") }
        return out.toString()
    }
}
