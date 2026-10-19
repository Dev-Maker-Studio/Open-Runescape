package com.rs.game.content.minigames.barbarianassault

import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.game.World
import com.rs.game.content.combat.CombatSpell
import com.rs.game.content.combat.XPType
import com.rs.game.content.minigames.barbarianassault.penance.Penance
import com.rs.game.content.minigames.barbarianassault.penance.PenanceHealer
import com.rs.game.content.minigames.barbarianassault.penance.PenanceQueen
import com.rs.game.content.minigames.barbarianassault.penance.PenanceRunner
import com.rs.game.content.minigames.barbarianassault.penance.QueenSpawn
import com.rs.game.map.instance.Instance
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.map.pathfinder.Direction
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.model.Animation
import org.darkan.core.model.GroundItem
import org.darkan.core.model.GroundItemType
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import org.darkan.core.model.SpotAnim
import com.rs.game.map.ChunkManager
import world.gregs.voidps.type.Tile
import java.util.EnumMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

private const val SPAWN_INTERVAL = 10
private const val WAVES_COUNTER = "Barbarian Assault waves completed"
private const val QUEEN_COUNTER = "Penance Queen kills"
private const val HONOUR_COUNTER = "Barbarian Assault Honour Points earned"
private const val TRAPS_COUNTER = "Barbarian Assault traps repaired"
private const val EGGS_COLLECTED_COUNTER = "Barbarian Assault eggs collected"
private const val EGGS_CONVERTED_COUNTER = "Barbarian Assault eggs converted"
private const val EGGS_LOADED_COUNTER = "Barbarian Assault cannon eggs loaded"
private const val DEATHS_COUNTER = "Barbarian Assault deaths"
internal const val HEALERS_POISONED_COUNTER = "Penance Healers poisoned"
private const val CALL_INTERVAL = 50
private const val HUD_INTERVAL = 2
private const val STALE_CALL_TICKS = 1
private const val FAILED_ATTACK_BACKLASH = 20
private const val ATTACK_BONUS_PER_LEVEL = 10
private const val WRONG_EGG_DAMAGE = 50
private const val SUPPLY_AMOUNT = 50
private const val STOCK_EACH = 7
private const val TAKE_AMOUNT = 10
private const val EGG_SPEED = 4
private const val EGG_ANGLE = 16
private val EGG_HEIGHTS = 80 to 30
private const val YELLOW_EGG_MIN = 4
private const val YELLOW_EGG_SPREAD = 3
private const val ARENA_EXIT_DELAY = 3
private const val FINISH_DELAY = 4
private const val TRAP_CHARGES = 2
private const val REPAIR_TICKS = 5
private const val NEXT_WAVE_COUNTDOWN = 50
private const val RED_DAMAGE = 30
private const val RED_SPLASH_DAMAGE = 10
private const val RED_SPLASH_RADIUS = 3
private const val GREEN_DAMAGE = 20
private const val GREEN_PULSE = 10
private const val GREEN_PULSES = 3
private const val GREEN_PULSE_INTERVAL = 5
private const val BLUE_DAMAGE = 10
private const val BLUE_STUN = 9
private const val OMEGA_MIN = 300
private const val OMEGA_MAX = 400
private const val QUEEN_POINTS = 80
private const val QUEEN_OTHER_POINTS = 5
private const val EGG_CAP = 60
private val HEAL_AMOUNTS = intArrayOf(100, 150, 200, 250, 350)
private val BAG_CAPACITY = intArrayOf(2, 4, 6, 7, 8)
private val CONVERT_CHANCE = intArrayOf(60, 70, 80, 90, 100)
private val EGG_PATTERNS = listOf(
    listOf(EggColour.BLUE to (0 to 1), EggColour.RED to (-1 to 0), EggColour.BLUE to (0 to 0), EggColour.GREEN to (1 to 0), EggColour.RED to (-1 to -1), EggColour.GREEN to (0 to -1)),
    listOf(EggColour.GREEN to (0 to 1), EggColour.RED to (1 to 1), EggColour.BLUE to (-1 to 0), EggColour.GREEN to (0 to 0), EggColour.RED to (0 to -1), EggColour.BLUE to (1 to -1)),
    listOf(EggColour.BLUE to (-1 to 1), EggColour.RED to (1 to 1), EggColour.GREEN to (-1 to 0), EggColour.RED to (0 to 0), EggColour.GREEN to (0 to -1), EggColour.BLUE to (1 to -1)),
)
private val FAMILY_ORDER = listOf(PenanceFamily.RUNNER, PenanceFamily.HEALER, PenanceFamily.FIGHTER, PenanceFamily.RANGER)
private val HEALER_TEAM_NAMES = intArrayOf(Component.interface_488.component_488_3, Component.interface_488.component_488_8, Component.interface_488.component_488_13, Component.interface_488.component_488_18)
private val HEALER_TEAM_HP = intArrayOf(Component.interface_488.component_488_4, Component.interface_488.component_488_9, Component.interface_488.component_488_14, Component.interface_488.component_488_19)
private val HEALER_TEAM_MEDIC = intArrayOf(Component.interface_488.component_488_5, Component.interface_488.component_488_10, Component.interface_488.component_488_15, Component.interface_488.component_488_20)
private val TRAP_STATES = intArrayOf(Loc.barbassault_trap_broken, Loc.barbassault_trap_1use, Loc.barbassault_trap)
private val ARROWS = mapOf(Call.CONTROLLED to Obj.bronze_arrow, Call.ACCURATE to Obj.iron_arrow, Call.AGGRESSIVE to Obj.steel_arrow, Call.DEFENSIVE to Obj.mithril_arrow)
private val SPELL_ELEMENTS = mapOf(Call.CONTROLLED to "WIND", Call.ACCURATE to "WATER", Call.AGGRESSIVE to "EARTH", Call.DEFENSIVE to "FIRE")
private val XP_TYPES = mapOf(Call.CONTROLLED to XPType.CONTROLLED, Call.ACCURATE to XPType.ACCURATE, Call.AGGRESSIVE to XPType.AGGRESSIVE, Call.DEFENSIVE to XPType.DEFENSIVE)
private val EGG_PROJECTILES = mapOf(EggColour.GREEN to Spotanim.barbassault_egg_projectile, EggColour.RED to Spotanim.barbassault_egg_projectile_2, EggColour.BLUE to Spotanim.barbassault_egg_projectile_3)
private val RESULT_TEAM = intArrayOf(
    Component.interface_497.component_497_16, Component.interface_497.component_497_17, Component.interface_497.component_497_18, Component.interface_497.component_497_19, Component.interface_497.component_497_20,
    Component.interface_497.component_497_21, Component.interface_497.component_497_22, Component.interface_497.component_497_23, Component.interface_497.component_497_24,
)
private val RESULT_POINTS = intArrayOf(
    Component.interface_497.component_497_26, Component.interface_497.component_497_27, Component.interface_497.component_497_28, Component.interface_497.component_497_29, Component.interface_497.component_497_30,
    Component.interface_497.component_497_31, Component.interface_497.component_497_32, Component.interface_497.component_497_33, Component.interface_497.component_497_34,
)
private val RESULT_OWN = intArrayOf(
    Component.interface_497.component_497_40, Component.interface_497.component_497_44, Component.interface_497.component_497_43, Component.interface_497.component_497_41, Component.interface_497.component_497_42,
    Component.interface_497.component_497_45, Component.interface_497.component_497_47, Component.interface_497.component_497_46, Component.interface_497.component_497_48,
)

class WaveStats {
    var failedAttacks = 0
    var goodEggs = 0
    var badEggs = 0
    var wrongPoison = 0
    var healed = 0
}

class BarbarianAssaultGame(val team: BarbarianAssaultTeam, val wave: Wave) {
    private enum class State { LOADING, RUNNING, ENDED }

    val players: List<Player> = team.members.toList()
    private val roles: Map<Player, Role> = players.associateWith { team.roleOf(it) ?: Role.ATTACKER }
    val layout = if (wave.queen) QUEEN_ARENA else NORMAL_ARENA
    private val instance = Instance.of(WaitingRoom.of(wave).inside, ARENA_CHUNKS, ARENA_CHUNKS)
    private var state = State.LOADING
    private val active = LinkedHashSet<Player>()
    private val required = EnumMap<Role, Call>(Role::class.java)
    private val announced = EnumMap<Role, Call>(Role::class.java)
    private val previous = EnumMap<Role, Call>(Role::class.java)
    private var callsChangedTick = 0
    private val medic = HashSet<Player>()
    private val penance = ArrayList<Penance>()
    private val kills = EnumMap<PenanceFamily, Int>(PenanceFamily::class.java)
    private var escapes = 0
    private var creations = 0
    private val baits = ArrayList<Bait>()
    private var baitOrdinal = 0
    private val traps = ArrayList<GameObject>()
    private val trapCharges = IntArray(layout.traps.size) { TRAP_CHARGES }
    private val repairing = HashSet<Player>()
    private val hoppers = EnumMap<Cannon, IntArray>(Cannon::class.java).apply { Cannon.entries.forEach { put(it, IntArray(EggColour.entries.size)) } }
    private var omega = 0
    private val eggs = HashSet<GroundItem>()
    private val resources = arrayOfNulls<GroundItem>(layout.logs.size + 1)
    private val stats = HashMap<Player, WaveStats>()
    private val supplies = HashMap<Player, IntArray>()
    private val bags = HashMap<Player, EnumMap<EggColour, Int>>()
    var queen: PenanceQueen? = null
        private set
    private val queenSpawns = ArrayList<QueenSpawn>()
    var tick = 0
        private set

    init {
        team.game = this
        val chunkX = if (wave.queen) QUEEN_CHUNK_X else NORMAL_CHUNK_X
        val chunkY = if (wave.queen) QUEEN_CHUNK_Y else NORMAL_CHUNK_Y
        instance.copyMapAllPlanes(chunkX, chunkY).thenAccept { WorldTasks.schedule(0) { begin() } }
    }

    fun local(x: Int, y: Int, plane: Int = 0): Tile = instance.getLocalTile(x, y, plane)

    fun local(tile: Tile): Tile = local(tile.x, tile.y, tile.plane)

    fun toLocal(tile: Tile): Tile = Tile.of(tile.x - instance.baseX, tile.y - instance.baseY, tile.plane)

    fun roleOf(player: Player) = roles[player]

    fun isPlaying(player: Player) = player in active && !player.isDead && !player.hasFinished()

    fun runners() = penance.filterIsInstance<PenanceRunner>()

    private fun begin() {
        val present = players.filter { !it.hasFinished() && BarbarianAssaultTeams.of(it) == team && it.baWaitingController != null }
        if (state != State.LOADING || present.size < players.size || team.members.size < TEAM_SIZE) {
            team.game = null
            team.sendTeam("Someone left before the wave began.")
            instance.destroy()
            state = State.ENDED
            return
        }
        for (trap in layout.traps) {
            val tile = local(trap)
            traps += World.getObjectWithId(tile, Loc.barbassault_trap) ?: GameObject(Loc.barbassault_trap, ObjectShape.GROUND_DECORATION, 0, tile).also(World::spawnObject)
        }
        Role.entries.forEach { required[it] = Call.forRole(it).random() }
        refreshResources()
        state = State.RUNNING
        players.forEachIndexed { slot, player ->
            player.controllerManager.removeControllerWithoutCheck()
            player.interfaceManager.removeOverlay()
            player.closeInterfaces()
            player.stopAll()
            player.tele(local(layout.starts[slot]))
            active += player
            stats[player] = WaveStats()
            supplies[player] = IntArray(TRACKED_SUPPLIES.size) { player.inventory.getAmountOf(TRACKED_SUPPLIES[it]) }
            player.controllerManager.startController(BarbarianAssaultController(this))
            issueTools(player)
            player.vars.setVarBit(Varbit.barbassault_team_curwave, wave.number)
        }
        sendTeamMessage("Wave ${wave.number} begins!")
        WorldTasks.scheduleTimer(1, 0) { process() }
    }

    private fun issueTools(player: Player) {
        val role = roles.getValue(player)
        player.inventory.addItemDrop(role.horn(player), 1)
        if (role == Role.COLLECTOR) player.inventory.addItemDrop(COLLECTION_BAGS[role.level(player) - 1], 1)
    }

    private fun process(): Boolean {
        if (state != State.RUNNING) return false
        tick++
        if (tick % CALL_INTERVAL == 0) changeCalls()
        if (tick % SPAWN_INTERVAL == 0) {
            spawnCheck()
            refreshResources()
        }
        if (tick % HUD_INTERVAL == 0) active.forEach(::refreshTeamHealth)
        checkCompletion()
        return state == State.RUNNING
    }

    private fun changeCalls() {
        previous.putAll(required)
        callsChangedTick = tick
        for (role in Role.entries) required[role] = Call.forRole(role).filter { it != required[role] }.random()
        announced.clear()
        active.forEach(::refreshCalls)
    }

    private fun alive(family: PenanceFamily) = penance.count { it.family == family && !it.dying }

    private fun spawnCheck() {
        if (wave.queen && queen != null) return
        for (family in FAMILY_ORDER) {
            val spec = wave.spec(family)
            val alive = alive(family)
            if (alive < spec.cap && kills.getOrDefault(family, 0) + alive < spec.quota) spawn(family, spec.npc)
        }
    }

    private fun spawn(family: PenanceFamily, npc: Int) {
        val tile = local(layout.spawns.getValue(family))
        val ordinal = ++creations
        penance += when (family) {
            PenanceFamily.RUNNER -> PenanceRunner(npc, tile, this, ordinal)
            PenanceFamily.HEALER -> PenanceHealer(npc, tile, this, ordinal)
            else -> Penance(npc, tile, this, family, ordinal).apply {
                setForceAgressive(true)
                setForceAggroDistance(CANNON_RANGE)
            }
        }
    }

    fun targetsFor(npc: Penance): List<Entity> = active.filter { isPlaying(it) && npc.withinDistance(it.tile, CANNON_RANGE) }

    fun canPenanceStep(npc: Penance, direction: Direction): Boolean {
        if (!World.checkWalkStep(npc.tile, direction, npc.size)) return false
        val next = npc.tile.transform(direction.dx, direction.dy)
        return active.none { it.tile == next }
    }

    fun penanceDied(npc: Penance) {
        kills.merge(npc.family, 1, Int::plus)
    }

    fun penanceRemoved(npc: Penance) {
        penance.remove(npc)
    }

    fun dropEggs(npc: Penance) {
        if (npc.family != PenanceFamily.FIGHTER && npc.family != PenanceFamily.RANGER || state != State.RUNNING) return
        val origin = npc.tile
        for ((colour, offset) in EGG_PATTERNS.random()) {
            var tile = origin.transform(offset.first, offset.second)
            if (!World.floorAndWallsFree(tile, 1)) tile = origin
            val egg = GroundItem(Item(colour.obj), tile, null, GroundItemType.NORMAL)
            World.addGroundItem(egg, null, -1, -1)
            eggs += egg
        }
    }

    fun runnerEscaped(runner: PenanceRunner) {
        escapes++
        sendTeamMessage("A Penance Runner has escaped!")
        runner.finish()
    }

    fun baitExists(bait: Bait) = bait in baits

    fun findBait(runner: PenanceRunner): Bait? {
        val range = lureRange()
        val here = runner.tile
        val zoneX = here.x shr 3
        val zoneY = here.y shr 3
        val order = listOf(1 to 1, 1 to 0, 1 to -1, 0 to 1, 0 to 0, 0 to -1, -1 to 1, -1 to 0, -1 to -1)
        var remembered: Bait? = null
        var gated = false
        for ((ox, oy) in order) {
            val inZone = baits.filter { (it.tile.x shr 3) == zoneX + ox && (it.tile.y shr 3) == zoneY + oy }.sortedByDescending { it.ordinal }
            for (bait in inZone) {
                if (!World.hasLineOfSight(bait.tile, 1, here, 1)) continue
                if (remembered == null) remembered = bait
                if (chebyshev(bait.tile, here) <= range) gated = true
            }
        }
        return if (gated) remembered else null
    }

    private fun lureRange(): Int {
        val defender = roles.entries.firstOrNull { it.value == Role.DEFENDER && it.key in active }?.key ?: return 4
        return if (Role.DEFENDER.level(defender) >= 2) 5 else 4
    }

    fun consumeBait(bait: Bait): Boolean {
        if (!baits.remove(bait)) return false
        World.removeGroundItem(bait.ground)
        return true
    }

    fun springTrap(runner: PenanceRunner): Boolean {
        val index = layout.traps.indices.firstOrNull { trapCharges[it] > 0 && chebyshev(runner.tile, local(layout.traps[it])) <= TRAP_RADIUS } ?: return false
        trapCharges[index]--
        setTrapState(index)
        World.sendObjectAnimation(traps[index], Animation(Seq.barbassault_trap_attack))
        runner.forceTalk("Aaargh!")
        runner.applyHit(Hit(roles.entries.firstOrNull { it.value == Role.DEFENDER }?.key ?: runner, runner.hitpoints, HitLook.TRUE_DAMAGE))
        return true
    }

    private fun setTrapState(index: Int) {
        val old = traps[index]
        val replacement = GameObject(TRAP_STATES[trapCharges[index]], old.type, old.rotation, old.tile)
        World.spawnObject(replacement)
        traps[index] = replacement
    }

    fun fixTrap(player: Player, obj: GameObject) {
        val index = traps.indexOfFirst { it.tile == obj.tile }
        val refusal = when {
            index < 0 -> return
            roles[player] != Role.DEFENDER -> "Only the Defender knows how to fix the traps."
            trapCharges[index] >= TRAP_CHARGES -> "The trap doesn't need fixing."
            !player.inventory.containsItem(Obj.hammer) -> "You need a hammer to fix the trap."
            !player.inventory.containsItem(Obj.logs, 1, false) -> "You need some logs to fix the trap."
            player in repairing -> return
            else -> null
        }
        if (refusal != null) {
            player.sendMessage(refusal)
            return
        }
        repairing += player
        player.lock(REPAIR_TICKS)
        player.anim(Seq.barbassault_trap_door_build)
        WorldTasks.schedule(REPAIR_TICKS) {
            repairing -= player
            if (state != State.RUNNING || !isPlaying(player) || trapCharges[index] >= TRAP_CHARGES || !player.inventory.containsItem(Obj.logs, 1, false)) return@schedule
            player.inventory.deleteItem(Obj.logs, 1)
            trapCharges[index] = TRAP_CHARGES
            setTrapState(index)
            player.incrementCount(TRAPS_COUNTER)
            player.sendMessage("You fix the trap.")
        }
    }

    fun dropBait(player: Player, item: Item) {
        val slot = slotOf(player, item) ?: return
        if (roles[player] != Role.DEFENDER) {
            player.sendMessage("Only the Defender can use food to lure the runners.")
            return
        }
        player.inventory.deleteItem(slot, item)
        val bait = Bait(item.id, player.tile, item.id == required.getValue(Role.DEFENDER).bait, ++baitOrdinal)
        val ground = GroundItem(Item(item.id), player.tile, null, GroundItemType.NORMAL)
        bait.ground = ground
        World.addGroundItem(ground, null, -1, -1)
        baits += bait
    }

    fun takeGroundItem(player: Player, item: GroundItem): Boolean {
        if (item in eggs) {
            takeEgg(player, item)
            return false
        }
        baits.firstOrNull { it.ground == item }?.let { bait ->
            if (roles[player] != Role.DEFENDER) {
                player.sendMessage("That food belongs to the Defender.")
                return false
            }
            baits.remove(bait)
            return true
        }
        val resource = resources.indexOfFirst { it == item }
        if (resource >= 0) resources[resource] = null
        return true
    }

    private fun takeEgg(player: Player, item: GroundItem) {
        val colour = EggColour.forObj(item.id)
        val stats = stats[player] ?: return
        when {
            roles[player] != Role.COLLECTOR -> player.sendMessage("Only the Collector can pick up the eggs.")
            item.id == Obj.barbassault_egg_04 -> takeYellowEgg(player, item)
            colour == null -> return
            colour != required.getValue(Role.COLLECTOR).eggColour -> {
                if (!World.removeGroundItem(player, item, false)) return
                eggs -= item
                stats.badEggs++
                World.sendSpotAnim(item.tile, SpotAnim(Spotanim.barbassault_atomic_blast))
                player.applyHit(Hit(player, WRONG_EGG_DAMAGE, HitLook.TRUE_DAMAGE))
                player.sendMessage("The egg explodes in your hands! That's the wrong colour.")
            }
            else -> {
                val bag = bags.getOrPut(player) { EnumMap(EggColour::class.java) }
                val inBag = bag.values.sum()
                val toBag = inBag < BAG_CAPACITY[Role.COLLECTOR.level(player) - 1]
                if (!toBag && !player.inventory.hasFreeSlots()) {
                    player.sendMessage("You have no room for that egg.")
                    return
                }
                if (!World.removeGroundItem(player, item, false)) return
                eggs -= item
                stats.goodEggs++
                player.incrementCount(EGGS_COLLECTED_COUNTER)
                if (toBag) bag.merge(colour, 1, Int::plus) else player.inventory.addItem(colour.obj, 1)
            }
        }
    }

    fun lookInBag(player: Player) {
        val bag = bags[player]?.filterValues { it > 0 }.orEmpty()
        player.sendMessage(if (bag.isEmpty()) "Your collection bag is empty." else "Your bag holds ${bag.entries.joinToString { "${it.value} ${it.key.displayName}" }} egg(s).")
    }

    fun emptyBag(player: Player) {
        val bag = bags[player] ?: return
        for (colour in EggColour.entries) {
            while ((bag[colour] ?: 0) > 0 && player.inventory.hasFreeSlots()) {
                bag.merge(colour, -1, Int::plus)
                player.inventory.addItem(colour.obj, 1)
            }
        }
    }

    fun convertEggs(player: Player) {
        if (roles[player] != Role.COLLECTOR) {
            player.sendMessage("Only the Collector can use the converter.")
            return
        }
        val chance = CONVERT_CHANCE[Role.COLLECTOR.level(player) - 1]
        var converted = 0
        for (slot in 0 until 28) {
            val item = player.inventory.getItem(slot) ?: continue
            val next = when (item.id) {
                Obj.barbassault_egg_03 -> Obj.barbassault_egg_01
                Obj.barbassault_egg_01 -> Obj.barbassault_egg_02
                Obj.barbassault_egg_02 -> Obj.barbassault_egg_01
                else -> continue
            }
            if (Utils.random(100) < chance) {
                item.id = next
                converted++
            } else player.inventory.deleteItem(slot, item)
        }
        player.inventory.refresh()
        player.anim(Seq.human_pickuptable)
        if (converted > 0) player.incrementCount(EGGS_CONVERTED_COUNTER, converted)
        player.sendMessage(if (converted == 0) "The converter has nothing to convert." else "The converter changes $converted egg(s).")
    }

    fun loadHopper(player: Player, cannon: Cannon) {
        if (roles[player] != Role.COLLECTOR) {
            player.sendMessage("Only the Collector can load the egg hopper.")
            return
        }
        val stock = hoppers.getValue(cannon)
        var loaded = 0
        emptyBag(player)
        for (colour in EggColour.entries) {
            val count = player.inventory.getAmountOf(colour.obj)
            if (count == 0) continue
            player.inventory.deleteItem(colour.obj, count)
            stock[colour.ordinal] += count
            loaded += count
        }
        val omegas = player.inventory.getAmountOf(Obj.barbassault_egg_heated_poison_spikes)
        if (omegas > 0) {
            player.inventory.deleteItem(Obj.barbassault_egg_heated_poison_spikes, omegas)
            omega += omegas
            loaded += omegas
        }
        bags[player]?.clear()
        if (loaded == 0) {
            player.sendMessage("You have no eggs to load.")
            return
        }
        player.anim(Seq.barb_assault_egg_place)
        player.incrementCount(EGGS_LOADED_COUNTER, loaded)
        player.sendMessage("You load $loaded egg(s) into the ${cannon.displayName} hopper.")
        active.forEach { refreshHopper(it, cannon) }
    }

    fun lookInHopper(player: Player, cannon: Cannon) {
        val stock = hoppers.getValue(cannon)
        player.sendMessage("The ${cannon.displayName} hopper holds ${EggColour.entries.joinToString { "${stock[it.ordinal]} ${it.displayName}" }}${if (wave.queen) " and $omega omega" else ""} egg(s).")
    }

    private fun refreshHopper(player: Player, cannon: Cannon) {
        val stock = hoppers.getValue(cannon)
        player.vars.setVarBit(Varbit.barbassault_eggcount_poison, stock[EggColour.GREEN.ordinal].coerceAtMost(31))
        player.vars.setVarBit(Varbit.barbassault_eggcount_explosive, stock[EggColour.RED.ordinal].coerceAtMost(31))
        player.vars.setVarBit(Varbit.barbassault_eggcount_stun, stock[EggColour.BLUE.ordinal].coerceAtMost(31))
        player.vars.setVarBit(Varbit.barbassault_eggcount_omega, omega.coerceAtMost(31))
    }

    fun shoot(player: Player, cannon: Cannon) {
        val stock = hoppers.getValue(cannon)
        player.sendOptionsDialogue("Which eggs will you fire?") {
            for (colour in EggColour.entries) if (stock[colour.ordinal] > 0) opExec("${colour.displayName.replaceFirstChar(Char::uppercase)} egg (${stock[colour.ordinal]})") { fire(player, cannon, colour) }
            if (omega > 0 && queen != null) opExec("Omega egg ($omega)") { fireOmega(player, cannon) }
            op("Nothing.")
        }
    }

    fun fire(player: Player, cannon: Cannon, colour: EggColour) {
        val stock = hoppers.getValue(cannon)
        val launcher = local(cannon.launcher)
        val target = penance.filter { !it.dying && !it.isDead && it.stunTicks == 0 && chebyshev(it.tile, launcher) <= CANNON_RANGE }
            .minWithOrNull(compareBy<Penance> { hypot((it.x - launcher.x).toDouble(), (it.y - launcher.y).toDouble()) }.thenBy { it.ordinal })
        when {
            state != State.RUNNING -> return
            stock[colour.ordinal] <= 0 -> player.sendMessage("There are no ${colour.displayName} eggs left in the hopper.")
            target == null -> player.sendMessage("There is nothing in range of the cannon.")
            else -> {
                stock[colour.ordinal]--
                player.anim(Seq.barbassault_egg_cannon_human_fire)
                World.sendProjectile(launcher, target, EGG_PROJECTILES.getValue(colour), EGG_HEIGHTS, 0, EGG_SPEED, EGG_ANGLE) { eggImpact(player, target, colour) }
                active.forEach { refreshHopper(it, cannon) }
            }
        }
    }

    private fun eggImpact(shooter: Player, target: Penance, colour: EggColour) {
        if (state != State.RUNNING || target.dying || target.isDead || target.hasFinished()) return
        when (colour) {
            EggColour.RED -> {
                target.applyHit(Hit(shooter, RED_DAMAGE.coerceAtMost(target.hitpoints), HitLook.TRUE_DAMAGE))
                target.spotAnim(Spotanim.barbassault_atomic_blast)
                for (other in penance.filter { it != target && it.stunTicks == 0 && chebyshev(it.tile, target.tile) <= RED_SPLASH_RADIUS }
                    .sortedWith(compareByDescending<Penance> { it.y shr 3 }.thenByDescending { it.x shr 3 }.thenByDescending { it.ordinal })) {
                    if (Utils.random(2) != 0) break
                    other.applyHit(Hit(shooter, RED_SPLASH_DAMAGE.coerceAtMost(other.hitpoints), HitLook.TRUE_DAMAGE))
                }
            }
            EggColour.GREEN -> {
                target.applyHit(Hit(shooter, GREEN_DAMAGE.coerceAtMost(target.hitpoints), HitLook.POISON_DAMAGE))
                target.spotAnim(Spotanim.barbassault_egg_poision)
                for (pulse in 1..GREEN_PULSES) WorldTasks.schedule(pulse * GREEN_PULSE_INTERVAL) {
                    if (state == State.RUNNING && !target.dying && !target.isDead) target.applyHit(Hit(shooter, GREEN_PULSE.coerceAtMost(target.hitpoints), HitLook.POISON_DAMAGE))
                }
            }
            EggColour.BLUE -> {
                target.applyHit(Hit(shooter, BLUE_DAMAGE.coerceAtMost(target.hitpoints), HitLook.TRUE_DAMAGE))
                target.spotAnim(Spotanim.barbassault_egg_stun)
                target.eggStun(BLUE_STUN)
            }
        }
    }

    private fun fireOmega(player: Player, cannon: Cannon) {
        val queen = queen ?: return
        if (omega <= 0 || state != State.RUNNING || queen.dying) return
        omega--
        val launcher = local(cannon.launcher)
        player.anim(Seq.barbassault_egg_cannon_human_fire)
        World.sendProjectile(launcher, queen, Spotanim.barbassault_egg_projectile, EGG_HEIGHTS, 0, EGG_SPEED, EGG_ANGLE) {
            if (state == State.RUNNING && !queen.dying && !queen.isDead) {
                queen.spotAnim(Spotanim.barbassault_atomic_spike)
                queen.omegaHit(player, Utils.random(OMEGA_MIN, OMEGA_MAX))
                sendTeamMessage("An omega egg slams into the Penance Queen!")
            }
        }
        active.forEach { refreshHopper(it, cannon) }
    }

    fun dispense(player: Player, obj: GameObject, option: String) {
        val role = roles[player] ?: return
        when (obj.id) {
            Loc.loc_20241 -> {
                if (role != Role.ATTACKER) return player.sendMessage("Only the Attacker can take supplies from this machine.")
                when (option) {
                    "Take-arrows" -> player.sendOptionsDialogue("Which arrows would you like?") {
                        for (arrow in ARROWS.values) opExec(ItemDefinitions.getDefs(arrow).name) { give(player, arrow, SUPPLY_AMOUNT) }
                    }
                    "Take-runes" -> {
                        listOf(Obj.airrune, Obj.waterrune, Obj.earthrune, Obj.firerune, Obj.mindrune).forEach { give(player, it, SUPPLY_AMOUNT) }
                    }
                }
            }
            Loc.loc_20242 -> {
                if (role != Role.DEFENDER) return player.sendMessage("Only the Defender can take supplies from this machine.")
                when (option) {
                    "Stock-up" -> stockUp(player, BAIT_OBJS)
                    "Take-crackers" -> fill(player, Obj.barbassault_runner_food)
                    "Take-tofu" -> fill(player, Obj.barbassault_runner_food2)
                    "Take-worms" -> fill(player, Obj.barbassault_runner_food3)
                }
            }
            Loc.loc_20243 -> {
                if (role != Role.HEALER) return player.sendMessage("Only the Healer can take supplies from this machine.")
                when (option) {
                    "Stock-up" -> {
                        if (VIALS.none { player.inventory.containsItem(it) }) player.inventory.addItem(Obj.barbassault_vial_04_full, 1)
                        stockUp(player, POISON_OBJS)
                    }
                    "Take-vial" -> if (VIALS.any { player.inventory.containsItem(it) }) player.sendMessage("You already have a healing vial.") else player.inventory.addItem(Obj.barbassault_vial_04_full, 1)
                    "Take-tofu" -> fill(player, Obj.barbassault_poision_01)
                    "Take-worms" -> fill(player, Obj.barbassault_poision_02)
                    "Take-meat" -> fill(player, Obj.barbassault_poision_03)
                }
            }
        }
        player.anim(Seq.human_pickuptable)
        World.sendObjectAnimation(obj, Animation(Seq.barbassault_loc_vending_arm_pull))
    }

    private fun give(player: Player, obj: Int, amount: Int) {
        val have = player.inventory.getAmountOf(obj)
        if (have < amount) player.inventory.addItem(obj, amount - have)
    }

    private fun fill(player: Player, obj: Int) {
        if (!player.inventory.hasFreeSlots()) return player.sendMessage("You don't have any room.")
        player.inventory.addItem(obj, TAKE_AMOUNT.coerceAtMost(player.inventory.freeSlots))
    }

    private fun stockUp(player: Player, objs: IntArray) {
        for (obj in objs) {
            val missing = (STOCK_EACH - player.inventory.getAmountOf(obj)).coerceAtMost(player.inventory.freeSlots)
            if (missing > 0) player.inventory.addItem(obj, missing)
        }
    }

    fun horn(player: Player, option: String) {
        val role = roles[player] ?: return
        if (state != State.RUNNING) return
        if (option.equals("Medic", ignoreCase = true)) {
            medic += player
            player.anim(Seq.barbassault_human_use_horn)
            active.filter { roles[it] == Role.HEALER }.forEach {
                it.sendMessage("${player.displayName} calls for a medic!")
                refreshTeamHealth(it)
            }
            return
        }
        val call = Call.forHorn(role, option) ?: return
        if (tick - callsChangedTick <= STALE_CALL_TICKS && call == previous[role.partner]) {
            player.sendMessage("The calls have just changed; call your ${role.partner.displayName} again.")
            return
        }
        announced[role.partner] = call
        player.anim(Seq.barbassault_human_use_horn)
        player.sendMessage("You call '${call.shout}' to your ${role.partner.displayName}.")
        for (partner in active.filter { roles[it] == role.partner }) {
            partner.sendMessage("Your ${role.displayName} calls '${call.shout}'.")
            refreshCalls(partner)
        }
    }

    fun hornOfGlory(player: Player) {
        val role = roles[player] ?: return
        player.sendOptionsDialogue("Which call would you like to make?") {
            for (call in Call.forRole(role.partner)) opExec(call.shout) { horn(player, call.hornOption) }
        }
    }

    fun validateHit(player: Player, hit: Hit, target: Entity) {
        val npc = target as? Penance ?: return
        if (npc.family != PenanceFamily.FIGHTER && npc.family != PenanceFamily.RANGER) return
        if (roles[player] != Role.ATTACKER) return
        val call = required.getValue(Role.ATTACKER)
        val correct = when (hit.look) {
            HitLook.MELEE_DAMAGE -> player.combatDefinitions.getAttackStyle().xpType == XP_TYPES[call]
            HitLook.RANGE_DAMAGE -> player.equipment.ammoId == ARROWS[call]
            HitLook.MAGIC_DAMAGE -> (hit.getData("combatSpell") as? CombatSpell)?.name?.startsWith(SPELL_ELEMENTS.getValue(call)) == true
            else -> return
        }
        if (!correct) {
            hit.setDamage(0)
            stats[player]?.failedAttacks = (stats[player]?.failedAttacks ?: 0) + 1
            player.applyHit(Hit(player, FAILED_ATTACK_BACKLASH, HitLook.TRUE_DAMAGE))
            player.sendMessage("You feel a jolt of pain: that's not what your Collector called!")
            return
        }
        if (hit.damage > 0) hit.setDamage(hit.damage + ATTACK_BONUS_PER_LEVEL * Role.ATTACKER.level(player))
    }

    fun poisonHealer(player: Player, item: Item, npc: PenanceHealer) {
        val slot = slotOf(player, item) ?: return
        if (roles[player] != Role.HEALER) {
            player.sendMessage("Only the Healer knows how to poison the Penance Healers.")
            return
        }
        if (npc.dying || npc.isDead) return
        player.inventory.deleteItem(slot, item)
        player.anim(Seq.human_throw)
        player.faceEntity(npc)
        val correct = item.id == required.getValue(Role.HEALER).poison
        if (!correct) {
            stats[player]?.wrongPoison = (stats[player]?.wrongPoison ?: 0) + 1
            player.sendMessage("The healer spits out the ${ItemDefinitions.getDefs(item.id).name.lowercase()}; that's the wrong poison.")
        }
        npc.feed(player, correct)
    }

    fun healWithVial(player: Player, item: Item, target: Player) {
        val slot = slotOf(player, item) ?: return
        if (roles[player] != Role.HEALER) return player.sendMessage("Only the Healer can use the healing vial.")
        if (target !in active || !isPlaying(target)) return player.sendMessage("You can only heal your own team.")
        val dose = VIALS.indexOf(item.id)
        if (dose <= 0) return player.sendMessage("Your vial is empty; refill it at the spring.")
        val missing = target.maxHitpoints - target.hitpoints
        val poisoned = target.poison.isPoisoned
        if (missing <= 0 && !poisoned && target !in medic) return player.sendMessage("${target.displayName} doesn't need healing.")
        val restored = missing.coerceAtMost(HEAL_AMOUNTS[Role.HEALER.level(player) - 1])
        target.heal(restored)
        target.poison.reset()
        target.restoreRunEnergy(20.0)
        medic -= target
        player.inventory.replaceItem(VIALS[dose - 1], 1, slot)
        player.anim(Seq.barbassault_vial_scoop)
        stats[player]?.healed = (stats[player]?.healed ?: 0) + restored
        target.sendMessage("${player.displayName} heals you.")
        refreshTeamHealth(player)
    }

    fun spring(player: Player, option: String) {
        when (option) {
            "Drink-from" -> {
                player.anim(Seq.barbassault_drink_from_spring)
                if (roles[player] == Role.HEALER) {
                    player.heal(player.maxHitpoints)
                    player.poison.reset()
                    player.restoreRunEnergy(100.0)
                } else player.sendMessage("The water is only meant for the Healer.")
            }
            "Take-from" -> {
                val slot = player.inventory.items.array().indexOfFirst { it != null && it.id in VIALS }
                if (slot < 0) return player.sendMessage("You need a healing vial to fill.")
                player.anim(Seq.barbassault_vial_scoop)
                player.inventory.replaceItem(Obj.barbassault_vial_04_full, 1, slot)
            }
        }
    }

    fun canAttack(player: Player, target: Entity): Boolean {
        if (target is PenanceQueen) {
            player.sendMessage("Weapons can't hurt the Queen. Fire omega eggs at her from the cannon.")
            return false
        }
        val npc = target as? Penance ?: return true
        return when {
            npc.family == PenanceFamily.FIGHTER || npc.family == PenanceFamily.RANGER -> (roles[player] == Role.ATTACKER).also { if (!it) player.sendMessage("Only the Attacker can fight the Penance Fighters and Rangers.") }
            else -> false
        }
    }

    private fun refreshResources() {
        layout.logs.forEachIndexed { index, tile -> if (resources[index]?.let(::itemPresent) != true) resources[index] = placeResource(Obj.logs, tile) }
        val hammerIndex = layout.logs.size
        if (resources[hammerIndex]?.let(::itemPresent) != true) resources[hammerIndex] = placeResource(Obj.hammer, layout.hammer)
    }

    private fun itemPresent(item: GroundItem) = ChunkManager.getChunk(item.tile.getChunkId()).itemExists(item)

    private fun placeResource(obj: Int, tile: Tile): GroundItem {
        val item = GroundItem(Item(obj), local(tile), null, GroundItemType.NORMAL)
        World.addGroundItem(item, null, -1, -1)
        return item
    }

    private fun checkCompletion() {
        if (penance.any { !it.dying }) return
        val ordinaryDone = PenanceFamily.entries.all { kills.getOrDefault(it, 0) >= wave.spec(it).quota }
        if (!ordinaryDone) return
        if (wave.queen) {
            val queen = queen
            if (queen == null) {
                summonQueen()
                return
            }
            if (!queen.dying) return
        }
        complete()
    }

    private fun summonQueen() {
        sendTeamMessage("The ground shakes as the Penance Queen emerges!")
        queen = PenanceQueen(local(QUEEN_EMERGE), this)
    }

    fun queenSpawned(spawn: QueenSpawn) {
        queenSpawns.removeIf { it.hasFinished() }
        queenSpawns += spawn
    }

    fun dropYellowEgg(queen: PenanceQueen) {
        val centre = queen.middleTile
        val reach = queen.size / 2 + YELLOW_EGG_MIN
        val target = (0 until 20).asSequence()
            .map { centre.transform(Utils.random(-reach - YELLOW_EGG_SPREAD, reach + YELLOW_EGG_SPREAD), Utils.random(-reach - YELLOW_EGG_SPREAD, reach + YELLOW_EGG_SPREAD)) }
            .firstOrNull { chebyshev(it, centre) >= reach && World.floorAndWallsFree(it, 1) } ?: return
        val egg = GroundItem(Item(Obj.barbassault_egg_04), target, null, GroundItemType.NORMAL)
        World.addGroundItem(egg, null, -1, -1)
        eggs += egg
    }

    private fun takeYellowEgg(player: Player, item: GroundItem) {
        if (!player.inventory.hasFreeSlots()) return player.sendMessage("You have no room for that egg.")
        if (!World.removeGroundItem(player, item, false)) return
        eggs -= item
        player.inventory.addItem(Obj.barbassault_egg_04, 1)
        stats[player]?.goodEggs = (stats[player]?.goodEggs ?: 0) + 1
    }

    fun passEgg(player: Player, item: Item, target: Player) {
        val slot = slotOf(player, item) ?: return
        val needed = when (item.id) {
            Obj.barbassault_egg_04 -> Role.HEALER
            Obj.barbassault_egg_poison -> Role.ATTACKER
            Obj.obj_10536 -> Role.DEFENDER
            Obj.barbassault_egg_heated_poison_spikes -> Role.COLLECTOR
            else -> return
        }
        when {
            target !in active -> player.sendMessage("You can only pass eggs to your own team.")
            roles[target] != needed -> player.sendMessage("That egg needs to go to your ${needed.displayName}.")
            !target.inventory.hasFreeSlots() -> player.sendMessage("${target.displayName} has no room for the egg.")
            else -> {
                player.inventory.deleteItem(slot, item)
                target.inventory.addItem(item.id, 1)
                player.sendMessage("You pass the egg to ${target.displayName}.")
                target.sendMessage("${player.displayName} passes you an egg.")
            }
        }
    }

    fun processQueenEgg(player: Player, item: Item, obj: GameObject?) {
        val slot = slotOf(player, item) ?: return
        val (role, result) = when (item.id) {
            Obj.barbassault_egg_04 -> Role.HEALER to Obj.barbassault_egg_poison
            Obj.barbassault_egg_poison -> Role.ATTACKER to Obj.obj_10536
            Obj.obj_10536 -> Role.DEFENDER to Obj.barbassault_egg_heated_poison_spikes
            else -> return
        }
        val valid = when (item.id) {
            Obj.barbassault_egg_04 -> obj?.id == Loc.barbassault_crater_poision || obj?.id == Loc.barbassault_crater_poision_big
            Obj.barbassault_egg_poison -> obj == null && player.inventory.containsItem(Obj.barbassault_spikes)
            else -> obj?.id == Loc.barbassault_crater_lava || obj?.id == Loc.barbassault_crater_lava_big
        }
        when {
            roles[player] != role -> player.sendMessage("Your ${role.displayName} needs to do that.")
            !valid -> player.sendMessage(when (item.id) {
                Obj.barbassault_egg_04 -> "Dunk the egg in a poison crater."
                Obj.barbassault_egg_poison -> "You need spikes from a petrified mushroom to spike the egg."
                else -> "Dunk the egg in a lava crater."
            })
            else -> {
                if (item.id == Obj.barbassault_egg_poison) player.inventory.deleteItem(Obj.barbassault_spikes, 1)
                player.inventory.replaceItem(result, 1, slot)
                player.anim(Seq.human_pickuptable)
                obj?.let { World.sendObjectAnimation(it, Animation(Seq.barbassault_crater_bubbles)) }
            }
        }
    }

    fun takeSpikes(player: Player) {
        if (!player.inventory.hasFreeSlots()) return player.sendMessage("You have no room for spikes.")
        player.inventory.addItem(Obj.barbassault_spikes, 1)
        player.sendMessage("You break some spikes off the mushroom.")
    }

    private fun complete() {
        if (state != State.RUNNING) return
        state = State.ENDED
        val finalists = active.toList()
        val scores = score()
        for (player in finalists) {
            val role = roles.getValue(player)
            val individual = scores.team + (scores.components[role] ?: 0).coerceAtLeast(0)
            val points = if (wave.queen) QUEEN_POINTS else individual.coerceAtLeast(0)
            val awarded = role.addPoints(player, points)
            val otherAwarded = if (wave.queen) Role.entries.filter { it != role }.sumOf { it.addPoints(player, QUEEN_OTHER_POINTS) } else 0
            if (awarded + otherAwarded > 0) player.incrementCount(HONOUR_COUNTER, awarded + otherAwarded)
            player.set(HIGHEST_WAVE, max(player.highestBarbarianAssaultWave, wave.number))
            player.incrementCount(WAVES_COUNTER)
            player.incrementCount("$WAVES_COUNTER as ${role.displayName}")
            if (wave.queen) {
                player.incrementCount(QUEEN_COUNTER)
                player.vars.saveVarBit(Varbit.barbassault_queendead, 1)
                player.vars.saveVarBit(Varbit.barbassault_armour_get_granite, 1)
            }
            player.sendMessage("Wave ${wave.number} complete! You have been awarded $awarded ${role.displayName} Honour Points.")
            if (awarded < points) player.sendMessage("You can't hold more than ${role.pointsCap} ${role.displayName} Honour Points; spend some with Commander Connad.")
        }
        val next = wave.next
        team.room = next?.let(WaitingRoom::of) ?: WaitingRoom.ONE
        team.nextWave = next
        team.game = null
        leaveArena(finalists, team.room) { showResults(it, scores) }
        if (next != null) WorldTasks.schedule(ARENA_EXIT_DELAY) { BarbarianAssaultWaiting.startCountdown(team, next, NEXT_WAVE_COUNTDOWN) }
        cleanUp()
    }

    private class Scores(val team: Int, val components: Map<Role, Int>, val rows: IntArray)

    private fun score(): Scores {
        val fighters = kills.getOrDefault(PenanceFamily.FIGHTER, 0)
        val rangers = kills.getOrDefault(PenanceFamily.RANGER, 0)
        val runners = kills.getOrDefault(PenanceFamily.RUNNER, 0)
        val healers = kills.getOrDefault(PenanceFamily.HEALER, 0)
        val failed = stats.values.sumOf { it.failedAttacks }
        val eggsCollected = (stats.values.sumOf { it.goodEggs } - stats.values.sumOf { it.badEggs }).coerceIn(0, EGG_CAP)
        val healed = stats.values.sumOf { it.healed }
        val wrongPoison = stats.values.sumOf { it.wrongPoison }
        val components = mapOf(
            Role.ATTACKER to (fighters - 1) + (rangers - 1) - (failed / 1.2).toInt(),
            Role.DEFENDER to (1.8 * runners).toInt() - (3 * escapes).coerceAtMost(10),
            Role.COLLECTOR to (eggsCollected / 4.35).toInt(),
            Role.HEALER to healed / 180 + (0.7 * healers).toInt() - wrongPoison / 4,
        )
        val total = components.values.sumOf { it.coerceAtLeast(0) }
        return Scores(total, components, intArrayOf(escapes, rangers, fighters, healers, runners, healed, wrongPoison, eggsCollected, failed))
    }

    private fun showResults(player: Player, scores: Scores) {
        val role = roles.getValue(player)
        player.interfaceManager.sendInterface(Interface.interface_497)
        player.packets.setIFText(Component.interface_497.component_497_3, "Wave ${wave.number} Complete!")
        scores.rows.forEachIndexed { index, value -> player.packets.setIFText(RESULT_TEAM[index], value.toString()) }
        val points = listOf(
            -(3 * scores.rows[0]).coerceAtMost(10), scores.rows[1] - 1, scores.rows[2] - 1, (0.7 * scores.rows[3]).toInt(),
            (1.8 * scores.rows[4]).toInt(), scores.rows[5] / 180, -(scores.rows[6] / 4), (scores.rows[7] / 4.35).toInt(), -(scores.rows[8] / 1.2).toInt(),
        )
        points.forEachIndexed { index, value -> player.packets.setIFText(RESULT_POINTS[index], value.toString()) }
        val own = stats[player] ?: WaveStats()
        val ownRows = intArrayOf(0, 0, 0, 0, 0, own.healed, own.wrongPoison, own.goodEggs - own.badEggs, own.failedAttacks)
        ownRows.forEachIndexed { index, value -> player.packets.setIFText(RESULT_OWN[index], value.toString()) }
        player.packets.setIFText(Component.interface_497.component_497_35, scores.team.toString())
        player.packets.setIFText(Component.interface_497.component_497_36, scores.team.toString())
        player.packets.setIFText(Component.interface_497.component_497_39, role.displayName)
        player.packets.setIFText(Component.interface_497.component_497_49, (scores.components[role] ?: 0).coerceAtLeast(0).toString())
        player.packets.setIFText(Component.interface_497.component_497_51, (scores.team + (scores.components[role] ?: 0).coerceAtLeast(0)).toString())
        player.packets.setIFText(Component.interface_497.component_497_59, role.points(player).toString())
        player.setHidden(Component.interface_497.component_497_61, wave.queen)
        player.setHidden(Component.interface_497.component_497_62, !wave.queen)
        player.setHidden(Component.interface_497.component_497_71, true)
        player.setHidden(Component.interface_497.component_497_72, true)
        if (wave.queen) player.packets.setIFText(Component.interface_497.component_497_65, "$QUEEN_POINTS ${role.displayName} Honour Points<br>$QUEEN_OTHER_POINTS points in each other role")
    }

    fun playerDied(player: Player) {
        player.incrementCount(DEATHS_COUNTER)
        if (state != State.RUNNING) return
        sendTeamMessage("${player.displayName} has fallen! The wave is lost.")
        fail()
    }

    private fun fail() {
        if (state != State.RUNNING) return
        state = State.ENDED
        val finalists = active.toList()
        team.game = null
        team.nextWave = null
        team.room = WaitingRoom.of(wave)
        leaveArena(finalists, team.room) { it.sendMessage("Your team failed wave ${wave.number}. The leader can climb down to try again.") }
        cleanUp()
    }

    private fun stopEnemies() {
        val enemies: List<NPC> = penance.toList() + queenSpawns + listOfNotNull(queen)
        for (npc in enemies) {
            npc.combat.removeTarget()
            npc.clearPendingTasks()
            npc.finish()
        }
    }

    private fun leaveArena(finalists: List<Player>, room: WaitingRoom, after: (Player) -> Unit) {
        active.clear()
        stopEnemies()
        finalists.forEachIndexed { slot, player ->
            val controller = player.controllerManager.controller as? BarbarianAssaultController
            controller?.game = null
            player.controllerManager.removeControllerWithoutCheck()
            cleanInventory(player)
            player.interfaceManager.removeOverlay()
            player.reset()
            player.unlock()
            player.anim(-1)
            player.tele(room.arrivalTile(slot))
            player.controllerManager.startController(BarbarianAssaultWaitingController(room))
            WorldTasks.schedule(1) { after(player) }
        }
    }

    private fun cleanInventory(player: Player) {
        for (obj in ARENA_ONLY_OBJS) {
            player.inventory.deleteItem(obj, Int.MAX_VALUE)
            player.equipment.deleteItem(obj, Int.MAX_VALUE)
        }
        val before = supplies.remove(player) ?: return
        TRACKED_SUPPLIES.forEachIndexed { index, obj ->
            val extra = player.inventory.getAmountOf(obj) - before[index]
            if (extra > 0) player.inventory.deleteItem(obj, extra)
        }
        bags.remove(player)
        player.appearance.generateAppearanceData()
    }

    fun leave(player: Player) {
        if (!active.remove(player)) return
        cleanInventory(player)
        player.reset()
        medic -= player
        sendTeamMessage("${player.displayName} has left the arena.")
        if (state == State.RUNNING && active.isEmpty()) {
            state = State.ENDED
            team.game = null
            cleanUp()
        }
    }

    private fun cleanUp() {
        WorldTasks.schedule(FINISH_DELAY) {
            stopEnemies()
            eggs.forEach(World::removeGroundItem)
            baits.forEach { World.removeGroundItem(it.ground) }
            resources.filterNotNull().forEach(World::removeGroundItem)
            instance.destroy()
        }
    }

    fun sendTeamMessage(message: String) = active.forEach { it.sendMessage(message) }

    fun sendHud(player: Player) {
        val role = roles[player] ?: return
        player.interfaceManager.sendOverlay(role.hud)
        player.packets.setIFText(role.waveLine, "Wave ${wave.number}")
        player.packets.setIFText(role.partnerLine, role.partner.displayName)
        refreshCalls(player)
        refreshTeamHealth(player)
        Cannon.entries.firstOrNull()?.let { refreshHopper(player, it) }
    }

    private fun refreshCalls(player: Player) {
        val role = roles[player] ?: return
        val received = announced[role]?.received ?: listOf("---")
        role.receivedLines.forEachIndexed { index, line -> player.packets.setIFText(line, received.getOrElse(index) { "" }) }
        player.packets.setIFText(role.callLine, required[role.partner]?.toCall ?: "")
    }

    private fun refreshTeamHealth(player: Player) {
        if (roles[player] != Role.HEALER) return
        val mates = players.filter { it != player }
        for (slot in HEALER_TEAM_NAMES.indices) {
            val mate = mates.getOrNull(slot)
            player.packets.setIFText(HEALER_TEAM_NAMES[slot], mate?.displayName ?: "------")
            player.packets.setIFText(HEALER_TEAM_HP[slot], mate?.let { "HP ${it.hitpoints}/${it.maxHitpoints}" } ?: "")
            player.setHidden(HEALER_TEAM_MEDIC[slot], mate == null || mate !in medic)
        }
    }

    fun cannonAt(obj: GameObject): Cannon? {
        val local = toLocal(obj.tile)
        return layout.cannons.firstOrNull { (it.hopper.x == local.x || it.launcher.x == local.x) && it.hopper.y == local.y }
    }

    private fun slotOf(player: Player, item: Item): Int? {
        val items = player.inventory.items.array()
        return items.indexOfFirst { it === item }.takeIf { it >= 0 } ?: items.indexOfFirst { it?.id == item.id }.takeIf { it >= 0 }
    }

    private fun chebyshev(a: Tile, b: Tile) = max(abs(a.x - b.x), abs(a.y - b.y))
}
