package com.rs.game.content.minigames.mage_training_arena

import com.rs.game.World
import com.rs.game.content.skills.magic.BoneSpell
import com.rs.game.content.skills.magic.Magic
import com.rs.game.content.skills.magic.Rune
import com.rs.game.content.skills.magic.RuneSet
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.map.instance.Instance
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.InstancedController
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.GroundItem
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

class MageTrainingArenaController(val room: ArenaRoom) : InstancedController(Instance.of(MageTrainingArena.lobby, 8, 8)) {
    @Transient private var ready = false
    @Transient private var cleaned = false
    @Transient private var ticks = 0
    @Transient private var guardian: NPC? = null
    @Transient private var statue: GroundItem? = null
    @Transient private var movingStatue = false
    @Transient private var mazeSolved = false
    @Transient private var observing = false
    @Transient private var mazeIndex = 0
    @Transient private var mazeCount = 0
    @Transient private var enchantedCount = 0
    @Transient private var orbDeposits = 0
    @Transient private var fruitDeposits = 0
    @Transient private var boneCounts = mutableMapOf<Tile, Int>()
    @Transient private var coinReward = 0
    @Transient private var bonusShape = 0
    @Transient private var freeAlchemy = 0
    @Transient private var values = intArrayOf(1, 5, 8, 15, 30)
    @Transient private var cupboards = listOf(0, 1, 2, 3, 4, -1, -1, -1)
    @Transient private var dragonstones = mutableMapOf<Tile, Int>()

    override fun onBuildInstance() {
        instance.copyMapAllPlanes(416, if (room == ArenaRoom.TELEKINETIC) 1208 else 1200).thenAccept {
            if (player.controllerManager.controller !== this) return@thenAccept
            mazeCount = player.vars.getVarBit(Varbit.magictraining_tele_mazecount)
            ready = true
            if (room == ArenaRoom.TELEKINETIC) nextMaze()
            else {
                instance.teleportLocal(player, room.entranceX, room.entranceY, room.plane)
                val id = when (room) {
                    ArenaRoom.ALCHEMY -> Npc.magictraining_guard_alchem
                    ArenaRoom.ENCHANTMENT -> Npc.magictraining_guard_encha
                    else -> Npc.magictraining_guard_grave
                }
                guardian = NPC(id, instance.getLocalTile(37, if (room == ArenaRoom.ALCHEMY) 26 else 39, room.plane), true).apply { setRandomWalk(false) }
                if (room == ArenaRoom.ALCHEMY) rotateAlchemy()
                if (room == ArenaRoom.ENCHANTMENT) {
                    bonusShape = Utils.random(MageTrainingArena.shapes.size)
                    listOf(29 to 36, 30 to 43, 34 to 45, 40 to 43, 41 to 36, 35 to 34).forEach { (x, y) ->
                        val tile = instance.getLocalTile(x, y, 0)
                        World.addGroundItemNoExpire(Item(Obj.magictraining_dragonstone), tile)
                        dragonstones[tile] = 0
                    }
                }
            }
            player.vars.setVarBit(Varbit.magictraining_zone, room.ordinal + 1)
            player.unlock()
            sendInterfaces()
            explainRoom()
        }.exceptionally {
            player.tele(MageTrainingArena.lobby)
            player.controllerManager.forceStop()
            null
        }
    }

    override fun sendInterfaces() {
        if (!ready) return
        player.interfaceManager.sendOverlay(room.overlay)
        refreshOverlay()
    }

    fun refreshOverlay() {
        if (!ready) return
        player.packets.setIFText(room.pointText, MageTrainingArena.points(player, room).toString())
        when (room) {
            ArenaRoom.TELEKINETIC -> player.packets.setIFText(Component.interface_198.component_198_7, (mazeCount % 5).toString())
            ArenaRoom.ALCHEMY -> {
                val texts = intArrayOf(Component.interface_194.component_194_9, Component.interface_194.component_194_10, Component.interface_194.component_194_11, Component.interface_194.component_194_12, Component.interface_194.component_194_13)
                texts.forEachIndexed { i, component -> player.packets.setIFText(component, values[i].toString()) }
                val components = intArrayOf(Component.interface_194.component_194_14, Component.interface_194.component_194_15, Component.interface_194.component_194_16, Component.interface_194.component_194_17, Component.interface_194.component_194_18)
                components.forEachIndexed { i, component -> player.packets.setIFHidden(room.overlay, Utils.componentIdFromHash(component), i != freeAlchemy) }
            }
            ArenaRoom.ENCHANTMENT -> player.packets.setIFItem(Component.interface_195.component_195_6, MageTrainingArena.shapes[bonusShape], 1)
            ArenaRoom.GRAVEYARD -> Unit
        }
    }

    fun explainRoom() {
        val guardianId = when (room) {
            ArenaRoom.TELEKINETIC -> Npc.magictraining_guard_tele
            ArenaRoom.ALCHEMY -> Npc.magictraining_guard_alchem
            ArenaRoom.ENCHANTMENT -> Npc.magictraining_guard_encha
            ArenaRoom.GRAVEYARD -> Npc.magictraining_guard_grave
        }
        talkArenaGuardian(player, guardianId)
    }

    override fun process() {
        if (!ready || cleaned) return
        ticks++
        when (room) {
            ArenaRoom.ALCHEMY -> if (ticks % 100 == 0) rotateAlchemy()
            ArenaRoom.ENCHANTMENT -> {
                if (ticks % 42 == 0) {
                    bonusShape = Utils.random(MageTrainingArena.shapes.size)
                    guardian?.forceTalk("The bonus shape is now ${Item(MageTrainingArena.shapes[bonusShape]).definitions.name.lowercase()}.")
                    refreshOverlay()
                }
                dragonstones.entries.forEach { entry ->
                    if (entry.value > 0) {
                        entry.setValue(entry.value - 1)
                        if (entry.value == 0) World.addGroundItemNoExpire(Item(Obj.magictraining_dragonstone), entry.key)
                    }
                }
            }
            ArenaRoom.GRAVEYARD -> if (ticks % 10 == 0) {
                if (player.hitpoints <= 20) {
                    MageTrainingArena.addPoints(player, room, -10)
                    player.hitpoints = player.maxHitpoints
                    exit()
                } else player.applyHit(Hit(20, Hit.HitLook.TRUE_DAMAGE))
            }
            ArenaRoom.TELEKINETIC -> Unit
        }
    }

    private fun rotateAlchemy() {
        values = listOf(1, 5, 8, 15, 30).shuffled().toIntArray()
        val contents = listOf(0, 1, 2, 3, 4, -1, -1, -1)
        val offset = Utils.random(contents.size)
        cupboards = contents.indices.map { contents[(it + offset) % contents.size] }
        freeAlchemy = Utils.random(5)
        refreshOverlay()
    }

    override fun processObjectClick1(obj: GameObject): Boolean {
        if (!ready) return false
        when {
            obj.id == Loc.magictraining_returndoor -> exit()
            room == ArenaRoom.ALCHEMY && (obj.id in CUPBOARDS || obj.id in OPEN_CUPBOARDS) -> {
                val index = CUPBOARDS.indexOf(obj.id).takeIf { it >= 0 } ?: OPEN_CUPBOARDS.indexOf(obj.id)
                if (obj.id in CUPBOARDS) World.spawnObjectTemporary(GameObject(obj, OPEN_CUPBOARDS[index]), 3)
                val item = cupboards[index]
                if (item < 0) player.sendMessage("The cupboard is empty.")
                else player.inventory.addItem(MageTrainingArena.alchemyItems[item], 1)
            }
            room == ArenaRoom.ALCHEMY && obj.id == Loc.magictraining_coin_collector -> depositCoins()
            room == ArenaRoom.ENCHANTMENT && obj.id in SHAPE_PILES -> player.inventory.addItem(MageTrainingArena.shapes[SHAPE_PILES.indexOf(obj.id)], 1)
            room == ArenaRoom.ENCHANTMENT && obj.id == Loc.magictraining_encha_hole -> depositOrbs()
            room == ArenaRoom.GRAVEYARD && obj.id in BONE_PILES -> {
                if (player.inventory.hasFreeSlots()) {
                    val count = boneCounts.getOrDefault(obj.tile, BONE_PILES.indexOf(obj.id) * 4)
                    player.inventory.addItem(MageTrainingArena.bones[count / 4], 1)
                    val next = (count + 1) % 16
                    boneCounts[obj.tile] = next
                    if (next / 4 != count / 4) World.spawnObject(GameObject(obj, BONE_PILES[next / 4]))
                } else player.sendMessage("You don't have enough inventory space.")
            }
            room == ArenaRoom.GRAVEYARD && obj.id == Loc.magictraining_grave_foodshute -> depositFruit()
        }
        return false
    }

    override fun processObjectClick2(obj: GameObject) = processObjectClick1(obj)

    fun alchemise(item: Item, low: Boolean) {
        val index = MageTrainingArena.alchemyItems.indexOf(item.id)
        if (!ready || room != ArenaRoom.ALCHEMY || index < 0 || !player.canCastSpell()) return
        val runes = if (index == freeAlchemy) null else RuneSet(Rune.NATURE, 1, Rune.FIRE, if (low) 3 else 5)
        if (!Magic.checkMagicAndRunes(player, if (low) 21 else 55, true, runes)) return
        player.inventory.deleteItem(item.id, 1)
        player.inventory.addItem(Obj.magictraining_coins, values[index])
        player.anim(if (low) Seq.human_castlowlvlalchemy else Seq.human_casthighlvlalchemy)
        player.spotAnim(if (low) Spotanim.lowlvlalchemy else Spotanim.highlvlalchemy)
        player.skills.addXp(Constants.MAGIC, if (low) 31.0 else 65.0)
        player.addSpellDelay(if (low) 2 else 4)
    }

    private fun depositCoins() {
        val coins = player.inventory.getAmountOf(Obj.magictraining_coins)
        if (coins == 0) { player.sendMessage("You have no arena coins to deposit."); return }
        player.inventory.deleteItem(Obj.magictraining_coins, coins)
        if (coins > 12000) {
            player.sendMessage("You deposited too many coins. The collector cannot reward more than 12,000 coins at once.")
            return
        }
        val points = coins / 100
        MageTrainingArena.addPoints(player, room, points)
        coinReward = (coinReward + points * 10).coerceAtMost(1000)
        player.skills.addXp(Constants.MAGIC, coins * 2.0)
        player.sendMessage("You deposit $coins coins and receive $points Alchemist Pizazz points.")
    }

    fun enchant(item: Item, spell: Int) {
        if (!ready || room != ArenaRoom.ENCHANTMENT || !player.canCastSpell()) return
        val shape = MageTrainingArena.shapes.indexOf(item.id)
        if (shape < 0 && item.id != Obj.magictraining_dragonstone) return
        val levels = intArrayOf(7, 27, 49, 57, 68, 87)
        val xp = doubleArrayOf(17.5, 37.0, 59.0, 67.0, 78.0, 97.0)
        val runes = arrayOf(RuneSet(Rune.COSMIC, 1, Rune.WATER, 1), RuneSet(Rune.COSMIC, 1, Rune.AIR, 3), RuneSet(Rune.COSMIC, 1, Rune.FIRE, 5), RuneSet(Rune.COSMIC, 1, Rune.EARTH, 10), RuneSet(Rune.COSMIC, 1, Rune.EARTH, 15, Rune.WATER, 15), RuneSet(Rune.COSMIC, 1, Rune.EARTH, 20, Rune.FIRE, 20))
        if (!Magic.checkMagicAndRunes(player, levels[spell], true, runes[spell])) return
        player.inventory.deleteItem(item.id, 1)
        player.inventory.addItem(Obj.magictraining_enchan_shapeorb, 1)
        enchantedCount++
        val spellTier = spell + 1
        val shapePoints = if (item.id == Obj.magictraining_dragonstone) spellTier * 2 else if (shape == bonusShape) 1 else 0
        val batchPoints = if (enchantedCount == 10) spellTier else 0
        if (batchPoints > 0) {
            enchantedCount = 0
            player.sendMessage("You have enchanted ten shapes and earned $batchPoints Enchantment Pizazz ${if (batchPoints == 1) "Point" else "Points"}.")
        }
        MageTrainingArena.addPoints(player, room, shapePoints + batchPoints)
        player.vars.setVarBit(Varbit.magictraining_ench_convcount, enchantedCount)
        player.anim(Seq.human_enchantamuletlvl1)
        player.spotAnim(Spotanim.enchant_amulet_lvl1)
        player.skills.addXp(Constants.MAGIC, xp[spell] * 0.75)
        player.addSpellDelay(2)
    }

    private fun depositOrbs() {
        val amount = player.inventory.getAmountOf(Obj.magictraining_enchan_shapeorb)
        if (amount == 0) { player.sendMessage("You have no orbs to deposit."); return }
        player.inventory.deleteItem(Obj.magictraining_enchan_shapeorb, amount)
        orbDeposits += amount
        while (orbDeposits >= 20) {
            orbDeposits -= 20
            player.inventory.addItemDrop(intArrayOf(Obj.deathrune, Obj.cosmicrune, Obj.bloodrune).random(), 3)
        }
        player.vars.setVarBit(Varbit.magictraining_enchan_orbcount, orbDeposits)
    }

    override fun processButtonClick(interfaceId: Int, componentId: Int, slotId: Int, slotId2: Int, opNum: Int): Boolean {
        val spell = BoneSpell.forComponent(interfaceId shl 16 or componentId)
        if (spell == null || room != ArenaRoom.GRAVEYARD) return true
        convertBones(spell)
        return false
    }

    private fun convertBones(spell: BoneSpell) {
        if (!ready || !spell.isUnlocked(player)) return
        val counts = MageTrainingArena.bones.map { player.inventory.getAmountOf(it) }
        if (counts.sum() == 0) { player.sendMessage("You aren't holding any bones!"); return }
        if (!spell.payRunes(player)) return
        val fruit = counts.mapIndexed { i, n -> (i + 1) * n }.sum()
        MageTrainingArena.bones.forEachIndexed { i, id -> player.inventory.deleteItem(id, counts[i]) }
        player.inventory.addItem(spell.fruit, minOf(fruit, player.inventory.freeSlots))
        spell.finish(player)
    }

    private fun depositFruit() {
        val amount = player.inventory.getAmountOf(Obj.banana) + player.inventory.getAmountOf(Obj.peach)
        if (amount == 0) { player.sendMessage("You have no fruit to deposit."); return }
        player.inventory.deleteItem(Obj.banana, player.inventory.getAmountOf(Obj.banana))
        player.inventory.deleteItem(Obj.peach, player.inventory.getAmountOf(Obj.peach))
        fruitDeposits += amount
        val points = fruitDeposits / 16
        fruitDeposits %= 16
        MageTrainingArena.addPoints(player, room, points)
        player.skills.addXp(Constants.MAGIC, points * 25.0)
        repeat(points) { player.inventory.addItemDrop(intArrayOf(Obj.naturerune, Obj.deathrune, Obj.bloodrune, Obj.earthrune, Obj.waterrune).random(), 1) }
        player.vars.setVarBit(Varbit.magictraining_grave_fruitcount, fruitDeposits)
    }

    private fun nextMaze() {
        statue?.let { World.removeGroundItem(it) }
        mazeIndex = Utils.random(MageTrainingArenaMazes.layouts.size)
        val maze = MageTrainingArenaMazes.layouts[mazeIndex]
        instance.teleportLocal(player, maze.entranceX, maze.entranceY, maze.plane)
        guardian?.finish()
        guardian = null
        statue = World.addGroundItemNoExpire(Item(Obj.magictraining_guardianstatue), instance.getLocalTile(maze.startX, maze.startY, maze.plane))
        mazeSolved = false
        observing = false
        player.vars.setVarBit(Varbit.magictraining_tele_camstat, 0)
        player.vars.setVarBit(Varbit.magictraining_tele_maze, mazeIndex)
        player.vars.setVarBit(Varbit.magictraining_mazesolved, 0)
        player.packets.sendResetCamera()
        refreshOverlay()
    }

    fun statueOption(item: GroundItem, option: Int): Boolean {
        if (item.id != Obj.magictraining_guardianstatue || room != ArenaRoom.TELEKINETIC) return false
        if (option != 3 && option != 4) return false
        if (item !== statue || !ready || movingStatue) return true
        if (option == 3) {
            observing = !observing
            player.vars.setVarBit(Varbit.magictraining_tele_camstat, if (observing) 1 else 0)
            if (!observing) {
                player.packets.sendResetCamera()
                return true
            }
            val maze = MageTrainingArenaMazes.layouts[mazeIndex]
            player.packets.sendCameraLook(instance.getLocalTile(maze.minX + 4, maze.minY + 4, maze.plane), 0)
            player.packets.sendCameraPos(instance.getLocalTile(maze.minX + 4, maze.minY - 3, maze.plane), 1800)
        } else if (option == 4 && !mazeSolved) {
            val maze = MageTrainingArenaMazes.layouts[mazeIndex]
            observing = false
            player.vars.setVarBit(Varbit.magictraining_tele_camstat, 0)
            player.packets.sendResetCamera()
            World.removeGroundItem(item)
            statue = World.addGroundItemNoExpire(Item(Obj.magictraining_guardianstatue), instance.getLocalTile(maze.startX, maze.startY, maze.plane))
        }
        return true
    }

    fun pullStatue(item: GroundItem): Boolean {
        if (item.id != Obj.magictraining_guardianstatue || room != ArenaRoom.TELEKINETIC) return false
        if (!ready || mazeSolved || player.isLocked || player.isDead || movingStatue || item !== statue || !player.canCastSpell()) return true
        val maze = MageTrainingArenaMazes.layouts[mazeIndex]
        val x = player.x - instance.baseX
        val y = player.y - instance.baseY
        val direction = when {
            x < maze.minX && y in maze.minY until maze.minY + 10 -> 0
            y >= maze.minY + 10 && x in maze.minX until maze.minX + 10 -> 1
            x >= maze.minX + 10 && y in maze.minY until maze.minY + 10 -> 2
            y < maze.minY && x in maze.minX until maze.minX + 10 -> 3
            else -> { player.sendMessage("Stand along one side of the maze to pull the statue."); return true }
        }
        if (!Magic.checkMagicAndRunes(player, 33, true, RuneSet(Rune.AIR, 1, Rune.LAW, 1))) return true
        player.resetWalkSteps()
        player.faceTile(item.tile)
        player.anim(Seq.human_caststrike)
        player.spotAnim(Spotanim.telegrab_casting)
        player.skills.addXp(Constants.MAGIC, 43.0)
        player.addSpellDelay(3)
        var statueX = item.tile.x - instance.baseX
        var statueY = item.tile.y - instance.baseY
        val dx = intArrayOf(-1, 0, 1, 0)[direction]
        val dy = intArrayOf(0, 1, 0, -1)[direction]
        movingStatue = true
        WorldTasks.scheduleTimer(1, 0) {
            if (cleaned || player.controllerManager.controller !== this) return@scheduleTimer false
            if (!maze.canStep(statueX, statueY, direction)) {
                movingStatue = false
                if (statueX == maze.goalX && statueY == maze.goalY) completeMaze()
                return@scheduleTimer false
            }
            statue?.let { World.removeGroundItem(it) }
            statueX += dx
            statueY += dy
            statue = World.addGroundItemNoExpire(Item(Obj.magictraining_guardianstatue), instance.getLocalTile(statueX, statueY, maze.plane))
            true
        }
        return true
    }

    private fun completeMaze() {
        if (mazeSolved) return
        mazeSolved = true
        player.vars.setVarBit(Varbit.magictraining_mazesolved, 1)
        observing = false
        player.vars.setVarBit(Varbit.magictraining_tele_camstat, 0)
        player.packets.sendResetCamera()
        val maze = MageTrainingArenaMazes.layouts[mazeIndex]
        statue?.let { World.removeGroundItem(it) }
        statue = null
        guardian = NPC(Npc.magictraining_guard_maze, instance.getLocalTile(maze.goalX, maze.goalY, maze.plane), true).apply { setRandomWalk(false) }
        mazeCount++
        MageTrainingArena.addPoints(player, room, 2 + if (mazeCount % 5 == 0) 8 else 0)
        if (mazeCount % 5 == 0) {
            player.inventory.addItemDrop(Obj.lawrune, 10)
            player.skills.addXp(Constants.MAGIC, 1000.0)
            player.sendMessage("You have solved five mazes! You receive eight bonus points and ten law runes.")
        }
        player.vars.saveVarBit(Varbit.magictraining_tele_mazecount, mazeCount % 5)
        refreshOverlay()
        player.sendMessage("You have solved the maze. Speak to the Maze Guardian to move to the next maze.")
    }

    fun talkToMazeGuardian(npc: NPC) {
        if (ready && mazeSolved && npc === guardian && room == ArenaRoom.TELEKINETIC) talkMazeGuardian(player) { claimMaze() }
    }

    private fun claimMaze() {
        if (!mazeSolved || cleaned) return
        nextMaze()
    }

    override fun canTakeItem(item: GroundItem): Boolean {
        if (item.id == Obj.magictraining_guardianstatue) return false
        if (item.id == Obj.magictraining_dragonstone && dragonstones.containsKey(item.tile)) {
            if (!player.inventory.hasFreeSlots()) return false
            dragonstones[item.tile] = 200
        }
        return true
    }

    override fun canDropItem(item: Item) = item.id !in MageTrainingArena.temporaryItems
    override fun canDepositItem(item: Item) = item.id !in MageTrainingArena.temporaryItems && (room != ArenaRoom.GRAVEYARD || item.id != Obj.banana && item.id != Obj.peach)
    override fun canTrade() = false
    override fun canSummonFamiliar() = false
    override fun canAttack(target: Entity) = false
    
    override fun onTeleported(type: TeleType) {
        player.controllerManager.forceStop()
    }

    override fun sendDeath(): Boolean {
        if (room == ArenaRoom.GRAVEYARD) MageTrainingArena.addPoints(player, room, -10)
        player.hitpoints = player.maxHitpoints
        exit()
        return false
    }

    fun exit() {
        player.tele(MageTrainingArena.lobby)
        player.controllerManager.forceStop()
    }

    override fun onDestroyInstance() {
        if (cleaned) return
        cleaned = true
        ready = false
        movingStatue = false
        guardian?.finish()
        guardian = null
        statue?.let { World.removeGroundItem(it) }
        statue = null
        dragonstones.clear()
        boneCounts.clear()
        MageTrainingArena.temporaryItems.forEach { player.inventory.deleteItem(it, player.inventory.getAmountOf(it)) }
        if (room == ArenaRoom.GRAVEYARD) {
            player.inventory.deleteItem(Obj.banana, player.inventory.getAmountOf(Obj.banana))
            player.inventory.deleteItem(Obj.peach, player.inventory.getAmountOf(Obj.peach))
        }
        if (coinReward > 0) { player.bank.addItem(Item(Obj.coins, coinReward), true); coinReward = 0 }
        player.vars.setVarBit(Varbit.magictraining_zone, 0)
        intArrayOf(Varbit.magictraining_tele_maze, Varbit.magictraining_ench_convcount, Varbit.magictraining_enchan_orbcount, Varbit.magictraining_grave_fruitcount, Varbit.magictraining_tele_camstat, Varbit.magictraining_mazesolved).forEach { player.vars.setVarBit(it, 0) }
        player.interfaceManager.removeOverlay()
        player.packets.sendResetCamera()
        player.unlock()
    }

    companion object {
        private val CUPBOARDS = intArrayOf(Loc.magictraining_alchem_cupboard0, Loc.magictraining_alchem_cupboard1, Loc.magictraining_alchem_cupboard2, Loc.magictraining_alchem_cupboard3, Loc.magictraining_alchem_cupboard4, Loc.magictraining_alchem_cupboard5, Loc.magictraining_alchem_cupboard6, Loc.magictraining_alchem_cupboard7)
        private val OPEN_CUPBOARDS = intArrayOf(Loc.magictraining_alchem_cupboard0_open, Loc.magictraining_alchem_cupboard1_open, Loc.magictraining_alchem_cupboard2_open, Loc.magictraining_alchem_cupboard3_open, Loc.magictraining_alchem_cupboard4_open, Loc.magictraining_alchem_cupboard5_open, Loc.magictraining_alchem_cupboard6_open, Loc.magictraining_alchem_cupboard7_open)
        private val SHAPE_PILES = intArrayOf(Loc.magictraining_enchan_shapepile1, Loc.magictraining_enchan_shapepile2, Loc.magictraining_enchan_shapepile3, Loc.magictraining_enchan_shapepile4)
        private val BONE_PILES = intArrayOf(Loc.magictraining_bones_pile1, Loc.magictraining_bones_pile2, Loc.magictraining_bones_pile3, Loc.magictraining_bones_pile4)
    }
}
