package com.rs.game.content.quests.underground_pass.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import org.darkan.core.map.pathfinder.Direction
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.quests.underground_pass.*
import com.rs.game.content.quests.underground_pass.objects.isDisguisedAsIbanDisciple
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onNpcDeath
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

private const val IBAN_ATTACK_INTERVAL = 4L
private const val IBAN_BEAM_INTERVAL = 3L
private const val IBAN_THREAT_INTERVAL = 20L
private const val IBAN_ATTACK_MIN_DAMAGE = 100
private const val IBAN_ATTACK_MAX_DAMAGE = 150
private const val IBAN_TEMPLE_RADIUS = 9
private const val KALRAG_LAIR_MIN_X = 2344
private const val KALRAG_LAIR_MIN_Y = 9888
private const val KALRAG_LAIR_MAX_X = 2367
private const val KALRAG_LAIR_MAX_Y = 9919
private const val MAX_AVENGING_SPIDERS = 5
private val IBAN_TEMPLE_ENTRANCE = Tile.of(2142, 4648, 1)
private val IBAN_THREATS = arrayOf(
    "Begone from my temple!",
    "Fool!",
    "I'll swallow your soul!",
    "You belong in the slave pits!",
    "You will die, frail mortal.",
    "You dare to defy me?!?",
    "Who dares desecrate my temple!",
    "I am the great Iban, I cannot die!"
)

@ServerStartupEvent
fun mapIbanFollowers() {
    instantiateNpc(Npc.iban) { id, tile -> LordIbanNpc(id, tile) }
    instantiateNpc(Npc.ibanmonk) { id, tile -> DiscipleOfIbanNpc(id, tile) }
    instantiateNpc(Npc.kalrag) { id, tile -> KalragNpc(id, tile) }
    instantiateNpc(Npc.blessed_spider) { id, tile -> BlessedSpiderNpc(id, tile) }

    onNpcClick(Npc.iban) { (player, npc) ->
        if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_PREPARED_DOLL)
            player.npcDialogue(npc, SCARED, "You are mine, mortal. Turn back, or surrender your mind to Iban!")
        else
            player.npcDialogue(npc, SCARED, "No! You cannot possess the four parts of my spirit!")
    }

    onNpcClick(Npc.ibanmonk, options = arrayOf("Talk-to")) { (player, npc, option) ->
        player.startConversation {
            player(CALM_TALK, "Hi.")
            if (!player.isDisguisedAsIbanDisciple()) {
                npc(npc, ANGRY, "An impostor... Die, scum!")
                exec { npc.combatTarget = player }
                return@startConversation
            }
            when (Random.nextInt(5)) {
                0 -> npc(npc, CALM_TALK, "Iban is our father, our guide... Soon he will rule all life.")
                1 -> {
                    npc(npc, CALM_TALK, "Som Molica Aniul Demonte.")
                    player(CONFUSED, "Pardon?")
                }
                2 -> {
                    npc(npc, CALM_TALK, "Hail the great one, my lord Iban. I die for you again and again.")
                    player(CONFUSED, "Is that possible?")
                    npc(npc, CALM_TALK, "Under Iban anything is possible. Death is only the beginning.")
                }
                3 -> npc(npc, CALM_TALK, "Succumb to the might of Iban, the ruler of this world.")
            }
        }
    }

    onNpcDeath(Npc.othainian, Npc.doomion, Npc.holthion, Npc.kalrag) { event ->
        val player = event.killer as? Player ?: return@onNpcDeath
        if (event.npc.id == Npc.kalrag) {
            player.sendMessage("Kalrag slumps to the floor...")
            (event.npc as? KalragNpc)?.let { kalrag ->
                val spiders = World.getNPCsInChunkRange(kalrag.chunkId, 2)
                    .filterIsInstance<BlessedSpiderNpc>()
                    .filter { it.respawnTile.isInKalragLair() }
                spiders.forEach(BlessedSpiderNpc::stopAvengingKalrag)
                spiders
                    .sortedBy { max(abs(it.x - player.x), abs(it.y - player.y)) }
                    .take(MAX_AVENGING_SPIDERS)
                    .forEach { it.avengeKalrag(player) }
            }
        }
        if (!player.isQuestStarted(Quest.UNDERGROUND_PASS) || player.isQuestComplete(Quest.UNDERGROUND_PASS))
            return@onNpcDeath
        when (event.npc.id) {
            Npc.othainian -> player.giveUniqueQuestItem(Obj.obj_1497)
            Npc.doomion -> player.giveUniqueQuestItem(Obj.obj_1498)
            Npc.holthion -> player.giveUniqueQuestItem(Obj.obj_1499)
            Npc.kalrag -> if (player.inventory.containsItem(Obj.obj_1492)) {
                player.vars.saveVarBit(Varbit.upass_iban_blood, 1)
                player.sendMessage("Kalrag's blood soaks into the doll.")
                player.updateIbanDollStage()
            }
        }
    }
}

private class LordIbanNpc(id: Int, tile: Tile) : NPC(id, tile) {
    init {
        setRandomWalk(false)
        faceDir(Direction.EAST)
    }

    override fun resetDirection() {
        stopFaceEntity()
        faceDir(Direction.EAST)
    }

    override fun onRespawn() = resetDirection()

    override fun withinDistance(player: Player, distance: Int): Boolean =
        player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_RETURN_TO_LATHAS &&
            super.withinDistance(player, distance)

    override fun processNPC() {
        super.processNPC()
        val ticks = World.getServerTicks()
        val activePlayers = World.getPlayersInChunkRange(chunkId, 1).filter {
            it.getQuestStage(Quest.UNDERGROUND_PASS) in STAGE_FOUND_DWARVES until STAGE_RETURN_TO_LATHAS &&
                it.withinDistance(tile, 14)
        }
        if (activePlayers.isEmpty())
            return

        if (ticks % IBAN_BEAM_INTERVAL == 0L)
            IBAN_TEMPLE_EFFECT_TILES.toList().shuffled().take(Random.nextInt(2, 5)).forEach {
                World.sendSpotAnim(it, Spotanim.spotanim_87)
            }
        if (ticks % IBAN_THREAT_INTERVAL == 0L)
            forceTalk(IBAN_THREATS.random())
        if (ticks % IBAN_ATTACK_INTERVAL != 0L)
            return

        activePlayers
            .filter { it.isVulnerableToIban() }
            .forEach { player ->
                val distance = max(abs(player.x - x), abs(player.y - y))
                val attackChance = when {
                    distance <= 3 -> 4
                    distance <= 5 -> 2
                    else -> 1
                }
                if (Random.nextInt(attackChance) == 0)
                    castSpikes(player, distance)
            }
    }

    private fun Player.isVulnerableToIban(): Boolean =
        !isDead && !hasFinished() &&
            plane == this@LordIbanNpc.plane &&
            getQuestStage(Quest.UNDERGROUND_PASS) == STAGE_PREPARED_DOLL &&
            inventory.containsItem(Obj.obj_1492) &&
            max(abs(x - this@LordIbanNpc.x), abs(y - this@LordIbanNpc.y)) <= IBAN_TEMPLE_RADIUS

    private fun castSpikes(player: Player, distance: Int) {
        val impactTile = Tile.of(player.tile)
        val spikeTiles = mutableSetOf(impactTile)
        repeat(if (distance > 5) 3 else 1) {
            spikeTiles += impactTile.transform(Random.nextInt(-1, 2), Random.nextInt(-1, 2))
        }
        spikeTiles.forEach { World.sendSpotAnim(it, Spotanim.spotanim_87) }

        WorldTasks.schedule(1) {
            if (!player.isVulnerableToIban() || player.tile != impactTile)
                return@schedule
            player.applyHit(
                Hit(
                    this,
                    Random.nextInt(IBAN_ATTACK_MIN_DAMAGE, IBAN_ATTACK_MAX_DAMAGE + 1),
                    Hit.HitLook.TRUE_DAMAGE
                )
            )
            player.sendMessage("Iban's magic hurls you back to the temple entrance.")
            player.tele(IBAN_TEMPLE_ENTRANCE)
        }
    }
}

private class DiscipleOfIbanNpc(id: Int, tile: Tile) : NPC(id, tile)

private class KalragNpc(id: Int, tile: Tile) : NPC(id, tile) {
    override fun onRespawn() {
        World.getNPCsInChunkRange(chunkId, 2)
            .filterIsInstance<BlessedSpiderNpc>()
            .filter { it.respawnTile.isInKalragLair() }
            .forEach(BlessedSpiderNpc::stopAvengingKalrag)
    }
}

private class BlessedSpiderNpc(id: Int, tile: Tile) : NPC(id, tile) {
    private var avengingKalrag = false

    fun avengeKalrag(player: Player) {
        avengingKalrag = true
        isForceAgressive = true
        isNoDistanceCheck = true
        combatTarget = player
    }

    fun stopAvengingKalrag() {
        if (avengingKalrag)
            combat.reset()
        avengingKalrag = false
        isForceAgressive = false
        isNoDistanceCheck = false
    }

    override fun onRespawn() {
        val kalragIsAlive = World.getNPCsInChunkRange(chunkId, 2).any {
            it.id == Npc.kalrag && it.respawnTile.isInKalragLair() && !it.isDead && !it.hasFinished()
        }
        if (kalragIsAlive)
            stopAvengingKalrag()
    }
}

private fun Tile.isInKalragLair(): Boolean =
    plane == 0 && x in KALRAG_LAIR_MIN_X..KALRAG_LAIR_MAX_X && y in KALRAG_LAIR_MIN_Y..KALRAG_LAIR_MAX_Y
