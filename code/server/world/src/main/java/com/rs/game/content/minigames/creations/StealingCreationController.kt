package com.rs.game.content.minigames.creations

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.sendOptionsDialogue
import com.rs.engine.dialogue.startConversation
import com.rs.engine.pathfinder.RouteEvent
import com.rs.game.World
import com.rs.game.content.combat.RangedWeapon
import com.rs.game.content.combat.getMeleeCombatDelay
import com.rs.game.content.combat.getWeaponAttackEmote
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.Teleport
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Controller
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.map.pathfinder.Direction
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

private const val WALL_CLIMB_AGILITY = 60
private const val RIFT_JUMP_AGILITY = 40
private const val PICKPOCKET_KEY = "sc_pickpocket_tick"
private const val CLAY_COUNTER = "Stealing Creation sacred clay gathered"
private const val KILN_COUNTER = "Stealing Creation items made at the kiln"
private const val BUILT_COUNTER = "Stealing Creation barriers built"
private const val REPAIRED_COUNTER = "Stealing Creation barriers repaired"
private const val PICKPOCKETS_COUNTER = "Stealing Creation pickpockets"
private const val KILLS_COUNTER = "Stealing Creation players killed"
private const val DEATHS_COUNTER = "Stealing Creation deaths"
private const val PICKPOCKET_DELAY_TICKS = 3
private const val PICKPOCKET_LEVEL_GAP = 20
private const val GIVE_DISTANCE = 3
private const val BARRIER_CLEARANCE = 6
private const val BARRIER_REPAIR_COST = 1
private const val FIRST_CLAY_GATHER_DELAY = 2
private val CLIMBABLE_WALLS = setOf(Loc.sc_agility_wall, Loc.sc_agility_wall_end, Loc.sc_agility_wall_02)
private val CRATE_TABLES = (Loc.sc_table_clay..Loc.sc_table_summoning).toSet()
private val WIELD_SKILLS = mapOf(
    KilnProduct.HATCHET to Constants.WOODCUTTING, KilnProduct.PICKAXE to Constants.MINING,
    KilnProduct.HARPOON to Constants.FISHING, KilnProduct.BUTTERFLY_NET to Constants.HUNTER,
    KilnProduct.DAGGER to Constants.ATTACK, KilnProduct.SCIMITAR to Constants.ATTACK, KilnProduct.WARHAMMER to Constants.ATTACK,
    KilnProduct.HELM to Constants.DEFENSE, KilnProduct.PLATEBODY to Constants.DEFENSE, KilnProduct.PLATELEGS to Constants.DEFENSE,
    KilnProduct.COIF to Constants.RANGE, KilnProduct.LEATHER_BODY to Constants.RANGE, KilnProduct.CHAPS to Constants.RANGE,
    KilnProduct.BOW to Constants.RANGE, KilnProduct.ARROWS to Constants.RANGE,
    KilnProduct.HAT to Constants.MAGIC, KilnProduct.ROBE_TOP to Constants.MAGIC, KilnProduct.ROBE_BOTTOM to Constants.MAGIC, KilnProduct.STAFF to Constants.MAGIC,
)
private val WIELD_REQUIREMENTS: Map<Int, Pair<Int, Int>> = buildMap {
    for ((product, skill) in WIELD_SKILLS) for (clayClass in 1..5) put(product.product(clayClass).id, skill to StealingCreation.levelFor(clayClass))
}
private val ENEMY_DAMAGE_LOOKS = setOf(HitLook.MELEE_DAMAGE, HitLook.RANGE_DAMAGE, HitLook.MAGIC_DAMAGE)
private val CAPE_SLOT_COMPONENT = Utils.componentIdFromHash(Component.interface_387.component_387_9)

fun depositFamiliarItems(owner: Player, familiar: Familiar): Boolean {
    val controller = owner.controllerManager.controller as? StealingCreationController
    if (controller == null) {
        familiar.dismiss()
        return false
    }
    familiar.inventory.array().filterNotNull().filter { StealingCreation.crateFor(it.id) != null }.forEach {
        familiar.removeItem(it)
        controller.game.deposit(owner, it, controller.getTeam())
    }
    return true
}

class StealingCreationController(@Transient val game: StealingCreationGameController, private val team: Boolean) : Controller() {

    fun getTeam() = team

    override fun allowsBotItemTransfer() = true

    override fun start() {
        sendInterfaces()
    }

    override fun logout(): Boolean {
        game.leave(player)
        player.tile = StealingCreation.EXIT
        StealingCreation.reset(player)
        return true
    }

    override fun login(): Boolean {
        StealingCreation.reset(player)
        player.tele(StealingCreation.EXIT)
        return true
    }

    override fun sendInterfaces() {
        player.interfaceManager.sendOverlay(Interface.interface_809, false)
        sendTimer()
        player.vars.setVarBit(Varbit.sc_team, teamNumber(team))
        game.getScore(player)?.let { sendScore(it) }
    }

    fun sendTimer() = player.packets.sendVarc(Varc.sc_server_cycles_left_on_launch, game.ticksLeft)

    fun sendScore(score: Score) {
        player.vars.setVar(Varp.sc_score_clay_gathered, score.gathering)
        player.vars.setVar(Varp.sc_score_clay_deposited, score.depositing)
        player.vars.setVar(Varp.sc_score_clay_processed, score.processing)
        player.vars.setVar(Varp.sc_score_clay_taken, score.withdrawing)
        player.vars.setVar(Varp.sc_score_damage_inflicted, score.damaging)
    }

    override fun moved() {
        val area = game.area
        val x = area.plotX(player.tile)
        val y = area.plotY(player.tile)
        if (area.inBounds(x, y) && area.getType(x, y) == PlotType.FOG && area.inFog(player, x, y)) {
            if (player.appearance.isNPC) return
            player.appearance.transformIntoNPC(Npc.npc_1957)
            player.appearance.isHidden = true
            player.setRunHidden(false)
            player.nextHits.clear()
        } else if (player.appearance.isNPC) {
            player.appearance.transformIntoNPC(-1)
            player.appearance.isHidden = false
            if (!player.run) WorldTasks.schedule(0) { player.setRunHidden(true) }
        }
    }

    override fun process() {
        if (game.area.contains(player.tile)) return
        player.sendMessage("An error has occured, please submit bug report.")
        player.controllerManager.forceStop()
    }

    override fun canEquip(slotId: Int, itemId: Int): Boolean {
        if (slotId == Equipment.CAPE) {
            player.sendMessage("You can't remove your team's colours.")
            return false
        }
        val (skill, level) = WIELD_REQUIREMENTS[itemId] ?: return true
        if (player.skills.getLevel(skill) >= level) return true
        player.sendMessage("You need a ${Constants.SKILL_NAME[skill]} level of $level to wield that.")
        return false
    }

    private fun sameTeam(other: Player) = other.equipment.capeId == player.equipment.capeId

    override fun canHit(entity: Entity): Boolean = when (entity) {
        is Player -> !sameTeam(entity) && !game.area.inSafeArea(entity, !team)
        is Familiar -> !sameTeam(entity.owner)
        else -> true
    }

    override fun canAttack(target: Entity): Boolean {
        val refusal = when {
            target is Player && sameTeam(target) -> "You cannot attack player's on the same team!"
            target is Player && Kiln.isProtected(target) -> "The power of the creation kiln is protecting the player."
            target is Familiar && sameTeam(target.owner) -> "You cannot attack a familiar on the same side as you!"
            else -> return true
        }
        player.sendMessage(refusal)
        return false
    }

    override fun keepCombating(target: Entity): Boolean {
        if (target is Player) {
            if (target.appearance.isNPC) {
                player.sendMessage("Your target is nowhere to be found.")
                return false
            }
            if (game.area.inSafeArea(target, !team) || game.area.inSafeArea(player, team)) return false
        }
        if (player.appearance.isNPC && (player.combatDefinitions.spell != null || RangedWeapon.forId(player.equipment.weaponId) != null)) {
            player.sendMessage("The fog is too thick to fire arrows or cast spells through.")
            return false
        }
        return true
    }

    override fun canSummonFamiliar(): Boolean {
        if (!player.appearance.isNPC) return true
        player.sendMessage("You cannot summon a familiar while inside the fog.")
        return false
    }

    override fun processOutgoingHit(hit: Hit, target: Entity) {
        if (target !is Player || sameTeam(target) || hit.look !in ENEMY_DAMAGE_LOOKS) return
        val damage = hit.damage.coerceIn(0, target.hitpoints)
        if (damage > 0) game.updateScore(player) { dealDamage(damage) }
    }

    override fun processItemOnPlayer(p2: Player, item: Item, slot: Int): Boolean {
        if (!player.withinDistance(p2.tile, GIVE_DISTANCE) || p2.isDead || player.isDead) return false
        if (!sameTeam(p2)) {
            player.sendMessage("You cannot give an item to a player on the opposite team!")
            return false
        }
        if (p2.inventory.addItem(item)) {
            player.inventory.deleteItem(item)
            p2.sendMessage("${Utils.formatPlayerNameForDisplay(player.displayName)} has given you an item.")
        } else
            player.sendMessage("${Utils.formatPlayerNameForDisplay(p2.displayName)} has insufficient room in their inventory.")
        return false
    }

    override fun canPlayerOption2(target: Player) = false

    override fun canPlayerOption4(target: Player): Boolean {
        if (sameTeam(target)) return true
        player.sendMessage("You cannot give an item to a player on the opposite team!")
        return false
    }

    override fun canPlayerOption3(target: Player): Boolean {
        val now = World.getServerTicks()
        if (player.tempAttribs.getL(PICKPOCKET_KEY) + PICKPOCKET_DELAY_TICKS > now || game.area.inSafeArea(target, !team) || game.area.inSafeArea(player, team)) return false
        val level = player.skills.getLevel(Constants.THIEVING)
        val targetLevel = target.skills.getLevel(Constants.THIEVING)
        val targetName = Utils.formatPlayerNameForDisplay(target.displayName)
        val refusal = when {
            player.attackedBy != null && player.inCombat() -> "You can't do this while you're under combat."
            sameTeam(target) -> "You cannot pickpocket player that is on the same team!"
            Kiln.isProtected(target) -> "The power of the creation kiln is protecting the player."
            targetLevel - level >= PICKPOCKET_LEVEL_GAP -> "You need a thieving level of at least ${targetLevel - PICKPOCKET_LEVEL_GAP} to pickpocket $targetName."
            target.inventory.freeSlots == target.inventory.itemsContainerSize -> "$targetName appears to have nothing in their pockets."
            player.inventory.freeSlots == 0 -> "You don't have enough space in your inventory to steal from your target."
            target.isDead -> "Too late."
            else -> null
        }
        if (refusal != null) {
            player.sendMessage(refusal)
            return false
        }
        player.setRouteEvent(RouteEvent(target) {
            player.setNextFaceEntity(target)
            player.anim(Seq.human_pickpocket)
            player.sendMessage("You attempt to pickpocket from $targetName's pockets.")
            player.tempAttribs.setL(PICKPOCKET_KEY, World.getServerTicks())
            val ratio = Utils.getRandomInclusive(level).toDouble() / (Utils.random(targetLevel) + 6)
            if (Math.round(ratio * level).toInt() <= targetLevel) {
                player.sendMessage("You fail to pickpocket $targetName.")
                return@RouteEvent
            }
            val stolen = target.inventory.items.array().filterNotNull().randomOrNull() ?: return@RouteEvent
            if (!player.inventory.addItem(stolen)) return@RouteEvent
            target.inventory.deleteItem(stolen)
            player.incrementCount(PICKPOCKETS_COUNTER)
            player.sendMessage("You sucessfully pickpocket an item from $targetName's pockets!")
        })
        return false
    }

    override fun processTeleport(tele: Teleport): Boolean {
        if (tele.type() == TeleType.OBJECT) return true
        player.simpleDialogue("You can't leave just like that!")
        return false
    }

    override fun onTeleported(type: TeleType) {
        player.controllerManager.forceStop()
    }

    override fun forceClose() {
        game.remove(player)
    }

    override fun processButtonClick(interfaceId: Int, componentId: Int, slotId: Int, slotId2: Int, opNum: Int): Boolean {
        when (interfaceId) {
            Interface.interface_813 -> {
                Kiln.handleButton(player, componentId, opNum) { clayClass, clayUsed ->
                    game.updateScore(player) { process(clayClass, clayUsed) }
                    player.incrementCount(KILN_COUNTER, clayUsed)
                }
                return true
            }
            Interface.interface_387 -> if (opNum == 1 && componentId == CAPE_SLOT_COMPONENT) {
                player.sendMessage("You can't remove your team's colours.")
                return false
            }
            Interface.inventory -> if (opNum == 1) {
                val item = player.inventory.getItem(slotId) ?: return true
                return !player.consumeScItem(item, slotId)
            }
        }
        return true
    }

    override fun processNPCClick1(npc: NPC): Boolean {
        if (npc.id !in MANAGER_NPCS) return true
        npc.setNextFaceEntity(player)
        talkToMystic(npc)
        return false
    }

    override fun processNPCClick2(npc: NPC): Boolean {
        if (npc.id !in MANAGER_NPCS) return true
        npc.setNextFaceEntity(player)
        StealingCreation.showClayStatus(game.area, player)
        return false
    }

    private fun talkToMystic(npc: NPC) {
        player.heal(player.maxHitpoints)
        player.poison.reset()
        player.startConversation {
            npc(npc.id, HeadE.ANGRY, if (Utils.random(3) == 0) "It's close. It could be anyone's game." else "Get a move on! Gather some clay before the other team takes it all!")
            options {
                opExec("Show me the remaining clay.") { StealingCreation.showClayStatus(game.area, player) }
                op("I want to quit!") {
                    npc(npc.id, HeadE.CONFUSED, "Are you sure you want to quit? You will not score any points if you leave.")
                    options("Are you sure you want to quit?") {
                        opExec("Yes, I want to quit.") { player.controllerManager.forceStop() }
                        op("No, I want to stay.")
                    }
                }
                op("Nevermind.")
            }
        }
    }

    override fun processItemOnObject(`object`: GameObject, item: Item): Boolean {
        if (`object`.id != Loc.sc_table_deposit_all) return true
        player.inventory.items.array().filterNotNull().filter { it.id == item.id }.forEach { depositItem(it) }
        return false
    }

    private fun depositItem(item: Item) {
        if (StealingCreation.crateFor(item.id) == null) return
        player.inventory.deleteItem(item)
        game.deposit(player, item, team)
    }

    private fun depositAll() {
        val kept = ClaySource.entries.mapNotNull { source ->
            source.tools.withIndex().lastOrNull { (tier, tool) -> player.inventory.containsItem(tool, 1) && player.skills.getLevel(source.skill) >= StealingCreation.levelFor(tier + 1) }?.value
        }.toMutableSet()
        for (item in player.inventory.items.array().filterNotNull()) {
            if (kept.remove(item.id)) continue
            depositItem(item)
        }
    }

    private fun takeFromCrate(crate: Int) {
        val stock = game.crateStock(team, crate)
        if (stock.isEmpty()) {
            player.sendMessage("There is nothing on this table.")
            return
        }
        player.sendOptionsDialogue("What would you like to take?") {
            for (item in stock) opExec("${item.name} (${Utils.formatNumber(item.amount)})") {
                if (item.definitions.isStackable())
                    player.sendInputInteger("How many would you like to take?") { game.withdraw(player, team, item.id, it) }
                else
                    game.withdraw(player, team, item.id, 1)
            }
        }
    }

    override fun processObjectClick1(`object`: GameObject): Boolean {
        val id = `object`.id
        when {
            id == Loc.sc_processing_point -> Kiln.open(player)
            id == Loc.sc_table_deposit_all -> depositAll()
            id in CRATE_TABLES -> takeFromCrate(id)
            id in (if (team) RED_DOOR_IDS else BLUE_DOOR_IDS) -> player.passBarrier(`object`, team)
            BarrierTier.isBarrier(id, !team) -> attackBarrier(`object`)
            id == Loc.sc_altar -> restoreAtAltar()
            id == Loc.sc_jump_loc || id in CLIMBABLE_WALLS -> crossObstacle(`object`)
            else -> if (!gatherClay(`object`)) return true
        }
        return false
    }

    override fun processObjectClick2(`object`: GameObject): Boolean {
        val id = `object`.id
        when {
            id in BarrierTier.EMPTY_HOTSPOTS -> buildBarrier(`object`)
            BarrierTier.isGate(id, team) -> player.passBarrier(`object`, team)
            else -> return true
        }
        return false
    }

    override fun processObjectClick3(`object`: GameObject): Boolean {
        if (!BarrierTier.isBarrier(`object`.id, team)) return true
        repairBarrier(`object`)
        return false
    }

    private fun restoreAtAltar() {
        player.anim(Seq.human_pray)
        player.prayer.restorePrayer(player.skills.getLevelForXp(Constants.PRAYER) * 10.0)
        player.skills.set(Constants.SUMMONING, maxOf(player.skills.getLevel(Constants.SUMMONING), player.skills.getLevelForXp(Constants.SUMMONING)))
        player.restoreRunEnergy(100.0)
        player.sendMessage("The altar restores your prayer, summoning and energy.")
    }

    private fun attackBarrier(barrier: GameObject) {
        if (player.actionManager.action != null || player.actionManager.actionDelay > 0) return
        val x = game.area.plotX(barrier.tile)
        val y = game.area.plotY(barrier.tile)
        val weaponId = player.equipment.weaponId
        player.actionManager.addActionDelay(getMeleeCombatDelay(weaponId))
        player.anim(getWeaponAttackEmote(weaponId, player.combatDefinitions.getAttackStyle()))
        WorldTasks.schedule(0) { game.damageBarrier(x, y) }
    }

    private fun buildBarrier(hotspot: GameObject) {
        val held = BarrierTier.entries.filter { player.inventory.containsItem(it.item, BarrierTier.BUILD_COST) }
        if (held.isEmpty()) {
            player.sendMessage("You need ${BarrierTier.BUILD_COST} barriers of the same class to build.")
            return
        }
        val tier = held.lastOrNull { player.skills.getLevel(Constants.CONSTRUCTION) >= StealingCreation.levelFor(it.tier) }
        if (tier == null) {
            player.sendMessage("You need a Construction level of ${StealingCreation.levelFor(held.first().tier)} to build those barriers.")
            return
        }
        val x = game.area.plotX(hotspot.tile)
        val y = game.area.plotY(hotspot.tile)
        val enemies = (if (team) game.blueTeam else game.redTeam).filter { it.withinDistance(hotspot.tile, BARRIER_CLEARANCE) }
        if (enemies.any { game.area.nearPlotPoint(it, x, y, 2, 2) }) {
            player.sendMessage("You cannot build a barrier while players from the other team are near the pallet.")
            return
        }
        enemies.forEach {
            it.resetWalkSteps()
            it.lock(3)
        }
        player.lock(2)
        player.schedule {
            player.anim(Seq.sc_player_building_walls)
            wait(1)
            if (player.inventory.containsItem(tier.item, BarrierTier.BUILD_COST) && game.buildBarrier(team, tier, x, y)) {
                player.inventory.deleteItem(tier.item, BarrierTier.BUILD_COST)
                player.incrementCount(BUILT_COUNTER)
            }
            player.unlock()
        }
    }

    private fun repairBarrier(barrier: GameObject) {
        val area = game.area
        val x = area.plotX(barrier.tile)
        val y = area.plotY(barrier.tile)
        if (area.getWallTeam(x, y) != teamNumber(team)) return
        val tier = BarrierTier.forTier(area.getWallTier(x, y)) ?: return
        val health = area.getWallStatus(x, y)
        if (health <= 0 || health >= tier.maxHealth) {
            player.sendMessage("This barrier doesn't need any repairing.")
            return
        }
        val repairWith = BarrierTier.entries.drop(tier.ordinal).firstOrNull { player.inventory.containsItem(it.item, BARRIER_REPAIR_COST) }
        if (repairWith == null) {
            player.sendMessage("You need a barrier of class ${tier.tier} or higher to repair this barrier.")
            return
        }
        player.lock(2)
        player.schedule {
            player.anim(Seq.sc_player_building_walls)
            wait(1)
            if (player.inventory.containsItem(repairWith.item, BARRIER_REPAIR_COST) && game.repairBarrier(x, y)) {
                player.inventory.deleteItem(repairWith.item, BARRIER_REPAIR_COST)
                player.incrementCount(REPAIRED_COUNTER)
            }
            player.unlock()
        }
    }

    private fun crossObstacle(obstacle: GameObject) {
        val isWall = obstacle.id in CLIMBABLE_WALLS
        val required = if (isWall) WALL_CLIMB_AGILITY else RIFT_JUMP_AGILITY
        if (player.skills.getLevel(Constants.AGILITY) < required) {
            player.sendMessage("You need an Agility level of $required to ${if (isWall) "climb over the wall" else "jump across the rift"}.")
            return
        }
        val distance = if (isWall) 2 else 3
        val acrossX = if (isWall) obstacle.rotation % 2 == 0 else obstacle.rotation % 2 == 1
        val direction = when {
            acrossX && obstacle.x >= player.x -> Direction.EAST
            acrossX -> Direction.WEST
            obstacle.y >= player.y -> Direction.NORTH
            else -> Direction.SOUTH
        }
        val destination = Tile.of(player.x + direction.dx * distance, player.y + direction.dy * distance, player.plane)
        player.forceMove(destination, if (isWall) Seq.sc_climb_over_wall else Seq.wild_ditch_jump, 5, 60) { player.setFaceAngle(direction.angle) }
    }

    private fun gatherClay(spot: GameObject): Boolean {
        val (source, clayClass) = ClaySource.forSpot(spot.id) ?: return false
        val x = game.area.plotX(spot.tile)
        val y = game.area.plotY(spot.tile)
        val requiredLevel = StealingCreation.levelFor(clayClass)
        player.actionManager.setAction(object : PlayerAction() {
            var toolTier: Int? = null

            override fun start(player: Player): Boolean {
                if (player.skills.getLevel(source.skill) < requiredLevel) {
                    player.sendMessage("You need a ${Constants.SKILL_NAME[source.skill]} level of $requiredLevel to collect this level of clay.")
                    return false
                }
                toolTier = source.bestTool(player)
                setActionDelay(player, gatherDelay())
                return true
            }

            override fun process(player: Player): Boolean {
                if (game.isEmpty(x, y) || player.inventory.freeSlots == 0) return false
                player.anim(toolTier?.let { source.firstAnimation + it } ?: Seq.sc_collect_clay_pickup)
                player.setNextFaceTile(spot.tile)
                return true
            }

            override fun processWithDelay(player: Player): Int {
                if (Utils.getRandomInclusive(clayClass) == 0) game.useSkillPlot(x, y)
                player.inventory.addItem(Item(StealingCreation.SACRED_CLAY[clayClass - 1], 1))
                player.incrementCount(CLAY_COUNTER)
                game.updateScore(player) { gather(clayClass) }
                return gatherDelay()
            }

            private fun gatherDelay(): Int {
                if (clayClass == 1) return FIRST_CLAY_GATHER_DELAY
                val toolBonus = toolTier?.let { GATHER_TOOL_BONUS[it] } ?: 1
                return (GATHER_SPOT_DIFFICULTY[clayClass - 1] - player.skills.getLevel(source.skill) - toolBonus).coerceAtLeast(FIRST_CLAY_GATHER_DELAY)
            }

            override fun stop(player: Player) {
                setActionDelay(player, 3)
            }
        })
        return true
    }

    override fun sendDeath(): Boolean {
        player.schedule {
            player.anim(Seq.human_death)
            wait(1)
            player.familiar?.sendDeath(player)
            wait(2)
            val killer = player.mostDamageReceivedSourcePlayer
            if (killer != null) {
                game.updateScore(killer) { kill() }
                killer.incrementCount(KILLS_COUNTER)
                killer.removeDamage(player)
                game.getScore(killer)?.let { killer.sendMessage("You have killed ${player.displayName}, you now have ${it.killed} kills.") }
                player.sendMessage("You have been killed by ${killer.displayName}")
            }
            player.incrementCount(DEATHS_COUNTER)
            player.equipment.deleteSlot(Equipment.CAPE)
            dropItems(killer)
            player.tele(game.area.respawnPoint(player, team))
            player.stopAll()
            game.updateScore(player) { die() }
            player.anim(-1)
            WorldTasks.schedule(1) {
                StealingCreation.giveCape(player, team)
                player.jingle(90)
                player.resetWalkSteps()
            }
            player.reset()
        }
        return false
    }

    private fun dropItems(killer: Player?) {
        val items = (player.inventory.items.itemsCopy + player.equipment.itemsCopy).filterNotNull()
        player.inventory.reset()
        player.equipment.reset()
        player.appearance.generateAppearanceData()
        val tile = Tile.of(player.tile)
        items.forEach { World.addGroundItem(it, tile, killer ?: player) }
    }

    private companion object {
        val GATHER_TOOL_BONUS = intArrayOf(20, 51, 65, 101, 151)
        val GATHER_SPOT_DIFFICULTY = intArrayOf(10, 90, 125, 175, 241)
    }
}
