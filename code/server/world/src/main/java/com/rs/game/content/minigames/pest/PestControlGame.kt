package com.rs.game.content.minigames.pest

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.World
import com.rs.game.content.minigames.pest.npcs.Pest
import com.rs.game.content.minigames.pest.npcs.PestFamily
import com.rs.game.content.minigames.pest.npcs.PestPortal
import com.rs.game.content.minigames.pest.npcs.PortalPosition
import com.rs.game.content.minigames.pest.npcs.Ravager
import com.rs.game.content.minigames.pest.npcs.Shifter
import com.rs.game.content.minigames.pest.npcs.Spinner
import com.rs.game.content.minigames.pest.npcs.Splatter
import com.rs.game.content.minigames.pest.npcs.VoidKnight
import com.rs.game.map.instance.Instance
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import com.rs.utils.WorldUtil
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile
import kotlin.math.ceil

const val SEAL_HIT = "pestControlSeal"
const val REQUIRED_CONTRIBUTION = 500
const val REPAIR_CONTRIBUTION = 50

private const val MAP_CHUNKS = 8
private const val TEMPLATE_CHUNK_X = 328
private const val TEMPLATE_CHUNK_Y = 320
private const val KNIGHT_X = 32
private const val KNIGHT_Y = 32
private const val SQUIRE_X = 35
private const val SQUIRE_Y = 54
private const val SUPPLY_TABLE_X = 32
private const val SUPPLY_TABLE_Y = 54
private const val DECK_MIN_X = 32
private const val DECK_MAX_X = 35
private const val DECK_MIN_Y = 50
private const val DECK_MAX_Y = 53
private const val PORTAL_HEAL = 500
private const val HUD_REFRESH = 5
private const val SPAWN_RADIUS = 4
private const val SPAWN_ATTEMPTS = 10
private const val MIN_SPAWN_INTERVAL = 8
private const val BASE_SPAWN_INTERVAL = 20
private const val MIN_PORTAL_POPULATION = 4
private const val MAX_PORTAL_POPULATION = 15
private const val WIN_COINS_PER_COMBAT_LEVEL = 10
private const val HYBRID_BOOTS_CHANCE = 550
private const val HYBRID_OFFER = "pestControlHybridBootsOffer"
private const val WINS_COUNTER = "Pest Control games won"
private const val PLAYED_COUNTER = "Pest Control games played"
private const val COMMENDATIONS_COUNTER = "Pest Control commendations earned"
private const val PORTALS_COUNTER = "Pest Control portals destroyed"
private const val REPAIRS_COUNTER = "Pest Control fortifications repaired"
private val DURATION = Ticks.fromMinutes(20)
private val SHIELD_DROPS = intArrayOf(Ticks.fromSeconds(15), Ticks.fromSeconds(45), Ticks.fromSeconds(75), Ticks.fromSeconds(105))
private val SHIELD_ORDERS = listOf(
    listOf(PortalPosition.WEST, PortalPosition.EAST, PortalPosition.SOUTH_EAST, PortalPosition.SOUTH_WEST),
    listOf(PortalPosition.WEST, PortalPosition.SOUTH_EAST, PortalPosition.EAST, PortalPosition.SOUTH_WEST),
    listOf(PortalPosition.EAST, PortalPosition.WEST, PortalPosition.SOUTH_WEST, PortalPosition.SOUTH_EAST),
    listOf(PortalPosition.EAST, PortalPosition.SOUTH_WEST, PortalPosition.SOUTH_EAST, PortalPosition.WEST),
    listOf(PortalPosition.SOUTH_EAST, PortalPosition.WEST, PortalPosition.SOUTH_WEST, PortalPosition.EAST),
    listOf(PortalPosition.SOUTH_EAST, PortalPosition.SOUTH_WEST, PortalPosition.WEST, PortalPosition.EAST),
)
private val KNIGHTS = intArrayOf(Npc.pest_voidknight_control1, Npc.pest_voidknight_control3, Npc.pest_voidknight_control4)
private val PORTAL_HP_TEXT = mapOf(
    PortalPosition.WEST to Component.interface_408.component_408_13,
    PortalPosition.EAST to Component.interface_408.component_408_14,
    PortalPosition.SOUTH_EAST to Component.interface_408.component_408_15,
    PortalPosition.SOUTH_WEST to Component.interface_408.component_408_16,
)
private val PORTAL_ICON = mapOf(
    PortalPosition.WEST to Component.interface_408.component_408_2,
    PortalPosition.EAST to Component.interface_408.component_408_3,
    PortalPosition.SOUTH_EAST to Component.interface_408.component_408_4,
    PortalPosition.SOUTH_WEST to Component.interface_408.component_408_5,
)
private val PORTAL_SHIELD_ICON = mapOf(
    PortalPosition.WEST to Component.interface_408.component_408_18,
    PortalPosition.EAST to Component.interface_408.component_408_20,
    PortalPosition.SOUTH_EAST to Component.interface_408.component_408_22,
    PortalPosition.SOUTH_WEST to Component.interface_408.component_408_24,
)
private val HYBRID_BOOTS = listOf(
    "Battle-mage boots" to Obj.hlr4m_battlemage_boots_stats,
    "Trickster boots" to Obj.hlr4m_trickster_boots_stats,
    "Vanguard boots" to Obj.hlr4m_vanguard_boots_stats,
)

class PestControlGame private constructor(val tier: LanderTier, private val roster: List<Player>) {
    private enum class State { LOADING, RUNNING, ENDED }

    private var state = State.LOADING
    private val instance = Instance.of(tier.exitTile, MAP_CHUNKS, MAP_CHUNKS)
    private val contribution = HashMap<Player, Int>()
    private val pests = HashSet<Pest>()
    private val shieldOrder = SHIELD_ORDERS.random()
    private var elapsed = 0
    private lateinit var portals: List<PestPortal>
    private lateinit var squire: NPC

    val players = ArrayList<Player>()
    lateinit var knight: VoidKnight
        private set
    lateinit var fortifications: Fortifications
        private set

    private val ticksLeft get() = (DURATION - elapsed).coerceAtLeast(0)

    init {
        instance.copyMapAllPlanes(TEMPLATE_CHUNK_X, TEMPLATE_CHUNK_Y).thenAccept { WorldTasks.schedule(0) { begin() } }
    }

    private fun local(x: Int, y: Int, plane: Int = 0): Tile = instance.getLocalTile(x, y, plane)

    private fun begin() {
        val boarded = roster.filter { it.hasStarted() && !it.hasFinished() && it.controllerManager.controller is PestControlLobbyController }
        if (boarded.isEmpty()) {
            instance.destroy()
            return
        }
        fortifications = Fortifications.scan(World.getAllObjectsInChunkRange(local(KNIGHT_X, KNIGHT_Y).getChunkId(), MAP_CHUNKS / 2))
        fortifications.rebuildAll()
        knight = VoidKnight(KNIGHTS.random(), local(KNIGHT_X, KNIGHT_Y), this)
        squire = NPC(Npc.pest_squire_instance, local(SQUIRE_X, SQUIRE_Y), true).apply { setRandomWalk(false) }
        World.spawnObject(GameObject(Loc.snp_counter_bandages_blue, ObjectShape.SCENERY_INTERACT, 0, local(SUPPLY_TABLE_X, SUPPLY_TABLE_Y)))
        portals = PortalPosition.entries.map {
            PestPortal(this, it, local(it.localX, it.localY), tier.activePortals[it.ordinal], tier.shieldedPortals[it.ordinal], tier.portalHitpoints)
        }
        state = State.RUNNING
        for (player in boarded) {
            player.controllerManager.removeControllerWithoutCheck()
            player.interfaceManager.removeOverlay()
            players += player
            player.useStairs(-1, deckTile(), 1, 2)
            player.controllerManager.startController(PestControlGameController(this))
        }
        WorldTasks.scheduleTimer(1, 0) { tick() }
    }

    fun deckTile(): Tile = local(Utils.random(DECK_MIN_X, DECK_MAX_X), Utils.random(DECK_MIN_Y, DECK_MAX_Y))

    private fun tick(): Boolean {
        if (state != State.RUNNING) return false
        elapsed++
        SHIELD_DROPS.indexOf(elapsed).takeIf { it >= 0 }?.let { portal(shieldOrder[it]).dropShield() }
        if (elapsed % spawnInterval() == 0) portals.filter { !it.destroyed }.forEach(::spawnPest)
        if (elapsed % HUD_REFRESH == 0) players.forEach(::refreshHud)
        if (ticksLeft == 0) end(true)
        return state == State.RUNNING
    }

    private fun portal(position: PortalPosition) = portals[position.ordinal]

    private fun spawnInterval() = (BASE_SPAWN_INTERVAL - players.size / 2).coerceAtLeast(MIN_SPAWN_INTERVAL)

    private fun portalPopulation() = (MIN_PORTAL_POPULATION + players.size / 2).coerceAtMost(MAX_PORTAL_POPULATION)

    private fun spawnPest(portal: PestPortal) {
        if (pests.count { it.portal == portal } >= portalPopulation()) return
        val family = rollFamily()
        val id = tier.pests.getValue(family).random()
        val tile = (0 until SPAWN_ATTEMPTS).asSequence()
            .map { Tile.of(portal.middleTile, SPAWN_RADIUS) }
            .firstOrNull { !WorldUtil.collides(it, portal.tile, 1, portal.size) && World.floorAndWallsFree(it, if (family == PestFamily.BRAWLER) 2 else 1) } ?: return
        pests += when (family) {
            PestFamily.SPLATTER -> Splatter(id, tile, this, portal)
            PestFamily.SHIFTER -> Shifter(id, tile, this, portal)
            PestFamily.RAVAGER -> Ravager(id, tile, this, portal)
            PestFamily.SPINNER -> Spinner(id, tile, this, portal)
            else -> Pest(id, tile, this, portal, family)
        }
    }

    private fun rollFamily(): PestFamily {
        var roll = Utils.random(PestFamily.entries.sumOf { it.spawnWeight })
        for (family in PestFamily.entries) {
            roll -= family.spawnWeight
            if (roll < 0) return family
        }
        return PestFamily.entries.last()
    }

    fun removePest(pest: Pest) {
        pests.remove(pest)
    }

    fun blastVictims(source: Entity, radius: Int): List<Entity> =
        (players + pests + portals).filter { it != source && !it.isDead && !it.hasFinished() && WorldUtil.isInRange(source, it, radius) }

    fun pestsNear(entity: Entity, radius: Int) = pests.filter { !it.isDead && !it.hasFinished() && WorldUtil.isInRange(entity, it, radius) }

    fun blocksMovement(tile: Tile) =
        pests.any { it.family == PestFamily.BRAWLER && WorldUtil.collides(it.tile, tile, it.size, 1) } ||
            portals.any { !it.destroyed && WorldUtil.collides(it.tile, tile, it.size, 1) }

    fun credit(hit: Hit) {
        val player = hit.source as? Player ?: return
        if (state != State.RUNNING || player !in players || hit.damage <= 0 || hit.getData(SEAL_HIT) != null) return
        addContribution(player, hit.damage)
    }

    fun creditRepair(player: Player) {
        if (state != State.RUNNING || player !in players) return
        addContribution(player, REPAIR_CONTRIBUTION)
        player.incrementCount(REPAIRS_COUNTER)
    }

    private fun addContribution(player: Player, amount: Int) {
        contribution.merge(player, amount, Int::plus)
        refreshContribution(player)
    }

    fun portalDestroyed(portal: PestPortal) {
        if (state != State.RUNNING) return
        knight.heal(PORTAL_HEAL)
        portal.recievedDamageEntities.filterIsInstance<Player>().filter { it in players }.forEach { it.incrementCount(PORTALS_COUNTER) }
        sendTeamMessage("The ${portal.position.description} portal has been destroyed!")
        pests.filter { it.portal == portal }.filterIsInstance<Spinner>().forEach(Spinner::detonate)
        players.forEach(::refreshHud)
        if (portals.all { it.destroyed }) end(true)
    }

    fun knightKilled() = end(false)

    fun leave(player: Player) {
        if (!players.remove(player)) return
        contribution.remove(player)
        if (state == State.RUNNING && players.isEmpty()) abandon()
    }

    fun sendTeamMessage(message: String) = players.forEach { it.sendMessage(message) }

    fun sendHud(player: Player) {
        player.interfaceManager.sendOverlay(Interface.interface_408)
        refreshHud(player)
    }

    private fun refreshHud(player: Player) {
        player.packets.setIFText(Component.interface_408.component_408_0, "${ceil(ticksLeft / Ticks.fromMinutes(1).toDouble()).toInt()} min")
        player.packets.setIFText(Component.interface_408.component_408_1, knight.hitpoints.toString())
        for (portal in portals) {
            player.packets.setIFText(PORTAL_HP_TEXT.getValue(portal.position), if (portal.destroyed) "0" else portal.hitpoints.toString())
            player.hideComponent(PORTAL_ICON.getValue(portal.position), portal.destroyed)
            player.hideComponent(PORTAL_SHIELD_ICON.getValue(portal.position), !portal.shielded || portal.destroyed)
        }
        refreshContribution(player)
    }

    private fun refreshContribution(player: Player) {
        val score = contribution[player] ?: 0
        player.packets.setIFText(Component.interface_408.component_408_11, if (score >= REQUIRED_CONTRIBUTION) "<col=75AE49>$score</col>" else score.toString())
    }

    private fun end(won: Boolean) {
        if (state != State.RUNNING) return
        state = State.ENDED
        val finalists = players.toList()
        players.clear()
        for (player in finalists) {
            val score = contribution[player] ?: 0
            player.controllerManager.forceStop()
            settle(player, won, score)
        }
        cleanUp()
    }

    private fun abandon() {
        state = State.ENDED
        cleanUp()
    }

    private fun cleanUp() {
        pests.toList().forEach(NPC::finish)
        portals.forEach(NPC::finish)
        knight.finish()
        squire.finish()
        instance.destroy()
    }

    private fun settle(player: Player, won: Boolean, score: Int) {
        player.incrementCount(PLAYED_COUNTER)
        val result = when {
            !won -> "You failed to protect the void knight and have not been awarded any points."
            score < REQUIRED_CONTRIBUTION -> "The knights notice your lack of zeal in that battle and have not presented you with any points."
            else -> {
                val credited = player.awardCommendations(tier.commendations)
                val coins = player.skills.combatLevelWithSummoning * WIN_COINS_PER_COMBAT_LEVEL
                player.inventory.addCoins(coins)
                player.incrementCount(WINS_COUNTER)
                player.incrementCount("Pest Control ${tier.displayName} games won")
                if (credited > 0) player.incrementCount(COMMENDATIONS_COUNTER, credited)
                if (Utils.random(HYBRID_BOOTS_CHANCE) == 0) player.set(HYBRID_OFFER, true)
                val capped = if (credited < tier.commendations) " You have reached the maximum of $COMMENDATION_CAP commendations." else ""
                "Congratulations! You managed to protect the void knight and have been awarded $credited commendation points and ${Utils.formatNumber(coins)} coins.$capped"
            }
        }
        WorldTasks.schedule(2) {
            player.sendMessage(result)
            if (player.getBool(HYBRID_OFFER)) offerHybridBoots(player) else player.simpleDialogue(result)
        }
    }

    companion object {
        fun launch(tier: LanderTier, roster: List<Player>) {
            PestControlGame(tier, roster)
        }

        fun offerHybridBoots(player: Player) {
            if (!player.getBool(HYBRID_OFFER)) return
            player.sendOptionsDialogue("The Void Knights offer you a pair of boots for your service.") {
                for ((name, obj) in HYBRID_BOOTS)
                    opExec(name) {
                        if (!player.getBool(HYBRID_OFFER)) return@opExec
                        player.delete(HYBRID_OFFER)
                        player.inventory.addItemDrop(obj, 1)
                    }
            }
        }
    }
}

private fun Player.hideComponent(component: Int, hidden: Boolean) =
    packets.setIFHidden(Utils.interfaceIdFromHash(component), Utils.componentIdFromHash(component), hidden)
