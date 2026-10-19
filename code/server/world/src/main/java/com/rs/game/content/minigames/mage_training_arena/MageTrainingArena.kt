package com.rs.game.content.minigames.mage_training_arena

import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.IFOnIFEvent
import com.rs.plugin.kts.interceptInterfaceOnInterface
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onDestroyItem
import com.rs.plugin.kts.onGroundItemClick
import com.rs.plugin.kts.onInterfaceOnGroundItem
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.model.Item
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.type.Tile

object MageTrainingArena {
    val lobby: Tile = Tile.of(3363, 3318, 0)
    val hats = intArrayOf(Obj.magictraining_proghat_dull, Obj.magictraining_proghat_energised, Obj.magictraining_proghat_full)
    val alchemyItems = intArrayOf(Obj.magictraining_leather_boots, Obj.magictraining_adamant_kiteshield, Obj.obj_6895, Obj.magictraining_emerald, Obj.magictraining_rune_longsword)
    val shapes = intArrayOf(Obj.magictraining_enchan_cube, Obj.magictraining_enchan_cylinder, Obj.magictraining_enchan_icosahendron, Obj.magictraining_enchan_pentamid)
    val bones = intArrayOf(Obj.magictraining_bones1, Obj.magictraining_bones2, Obj.magictraining_bones3, Obj.magictraining_bones4)
    val temporaryItems = alchemyItems + shapes + bones + intArrayOf(Obj.magictraining_coins, Obj.magictraining_enchan_shapeorb, Obj.magictraining_dragonstone, Obj.magictraining_guardianstatue)
    val enchantComponents = intArrayOf(Component.interface_192.component_192_29, Component.interface_192.component_192_41, Component.interface_192.component_192_53, Component.interface_192.component_192_61, Component.interface_192.component_192_76, Component.interface_192.component_192_88)

    fun points(player: Player, room: ArenaRoom) = player.vars.getVarBit(room.points)

    fun addPoints(player: Player, room: ArenaRoom, amount: Int) {
        player.vars.saveVarBit(room.points, (points(player, room) + amount).coerceIn(0, room.cap))
        (player.controllerManager.controller as? MageTrainingArenaController)?.refreshOverlay()
    }

    fun enter(player: Player, room: ArenaRoom) {
        if (player.controllerManager.controller != null || player.isLocked) return
        if (player.skills.getLevel(Constants.MAGIC) < room.level) {
            player.sendMessage("You need a Magic level of ${room.level} to enter this room.")
            return
        }
        if (player.familiar != null) {
            player.sendMessage("You cannot take a familiar into the Mage Training Arena.")
            return
        }
        if (hats.none { player.inventory.containsItem(it) || player.equipment.hatId == it }) {
            player.sendMessage("You need a progress hat. Speak to the Entrance Guardian.")
            return
        }
        if (room == ArenaRoom.ALCHEMY && player.inventory.containsItem(Obj.coins) || room == ArenaRoom.GRAVEYARD && player.inventory.containsOneItem(Obj.banana, Obj.peach)) {
            player.sendMessage("You cannot bring ${if (room == ArenaRoom.ALCHEMY) "coins" else "bananas or peaches"} into this room.")
            return
        }
        player.lock()
        player.controllerManager.startController(MageTrainingArenaController(room))
    }

    val alchemyComponents = intArrayOf(Component.interface_192.component_192_38, Component.interface_192.component_192_59)

    fun castOnInventory(e: IFOnIFEvent): Boolean {
        if (e.toInterfaceId != Interface.inventory) return false
        val player = e.player
        val item = player.inventory.getItem(e.toSlotId) ?: return false
        val controller = player.controllerManager.controller as? MageTrainingArenaController
        val alchemy = e.fromComponent in alchemyComponents
        if (controller?.room == ArenaRoom.ALCHEMY && alchemy && item.id !in alchemyItems) {
            player.sendMessage("You can only alchemise items from the cupboards in this room.")
            return true
        }
        if (item.id !in temporaryItems) return false
        if (controller == null) {
            player.sendMessage("That item can only be used inside its arena room.")
            return true
        }
        player.stopAll()
        if (alchemy) controller.alchemise(item, e.fromComponent == Component.interface_192.component_192_38)
        else controller.enchant(item, enchantComponents.indexOf(e.fromComponent))
        return true
    }
}

@ServerStartupEvent
fun mapMageTrainingArena() {
    onObjectClick(Loc.magictraining_temple_door) { (player, obj) ->
        World.sendObjectAnimation(obj, Seq.magictraining_door_rotate)
        player.addWalkSteps(obj.tile.transform(0, if (player.y < obj.y) 1 else -1, 0), 3, false)
    }
    val stairs = mapOf(
        Loc.magictraining_stairs_base_mirror to Tile.of(3357, 3307, 1),
        Loc.magictraining_stairs_top_mirror to Tile.of(3360, 3306, 0),
        Loc.magictraining_stairs_base to Tile.of(3369, 3307, 1),
        Loc.magictraining_stairs_top to Tile.of(3367, 3306, 0),
        Loc.rm_stairs_to_groundfloor to Tile.of(3358, 3305, 0),
        Loc.rm_stairs_to_basement to Tile.of(3619, 4814, 0)
    )
    stairs.forEach { (id, tile) -> onObjectClick(id) { it.player.useStairs(-1, tile, 0, 1) } }
    ArenaRoom.entries.forEach { room -> onObjectClick(room.portal) { MageTrainingArena.enter(it.player, room) } }
    onNpcClick(Npc.magictraining_guard_entrance) { (player) -> talkEntranceGuardian(player) }
    onNpcClick(Npc.magictraining_guard_rewards) { (player, _, option) ->
        if (option == "Trade-with") MageTrainingArenaRewards.open(player) else talkRewardsGuardian(player)
    }
    onNpcClick(Npc.magictraining_guard_tele, Npc.magictraining_guard_alchem, Npc.magictraining_guard_encha, Npc.magictraining_guard_grave) { (player, npc) ->
        talkArenaGuardian(player, npc.id)
    }
    onNpcClick(Npc.magictraining_guard_maze, checkDistance = false) { (player, npc) ->
        (player.controllerManager.controller as? MageTrainingArenaController)?.talkToMazeGuardian(npc)
    }
    onNpcClick(Npc.magictraining_ghost_shoes, Npc.magictraining_ghost_shoes2, Npc.magictraining_ghost_shoes3, Npc.magictraining_ghost_shoes4) { (player, npc) ->
        talkCharmedWarrior(player, npc.id)
    }
    onItemClick(*MageTrainingArena.hats.map { it as Any }.toTypedArray(), options = arrayOf("Talk-to", "Activate", "Destroy")) { e ->
        if (e.option == "Destroy") destroyProgressHat(e.player, e.slotId, e.item)
        else talkProgressHat(e.player)
    }
    onItemClick(Obj.magictraining_arenalorebook, options = arrayOf("Read")) { (player, _) ->
        player.simpleDialogue("The Mage Training Arena has four training rooms. Pull the guardian statue through mazes, alchemise cupboard items, enchant shapes, and turn bones into fruit. Earn Pizazz points in all four rooms to buy rewards from the guardian upstairs.")
    }
    onButtonClick(Interface.interface_197) { e ->
        if (e.component == Component.interface_197.component_197_16) MageTrainingArenaRewards.click(e.player, e.slotId, e.opNum)
    }
    onDestroyItem(*MageTrainingArena.hats.map { it as Any }.toTypedArray()) { (player, _) ->
        ArenaRoom.entries.forEach { player.vars.saveVarBit(it.points, 0) }
        player.vars.saveVarBit(Varbit.magictraining_tele_mazecount, 0)
        (player.controllerManager.controller as? MageTrainingArenaController)?.exit()
    }
    interceptInterfaceOnInterface(*MageTrainingArena.alchemyComponents, *MageTrainingArena.enchantComponents) { MageTrainingArena.castOnInventory(it) }
    onInterfaceOnGroundItem(fromComponents = intArrayOf(Component.interface_192.component_192_44), itemIds = intArrayOf(Obj.magictraining_guardianstatue)) { e ->
        (e.player.controllerManager.controller as? MageTrainingArenaController)?.pullStatue(e.item)
    }
    onGroundItemClick(Obj.magictraining_guardianstatue, options = arrayOf("Observe", "Reset"), checkDistance = false) { e ->
        (e.player.controllerManager.controller as? MageTrainingArenaController)?.statueOption(e.item, e.opNum)
    }
    onLogin { (player) ->
        if (player.controllerManager.controller !is MageTrainingArenaController) {
            MageTrainingArena.temporaryItems.forEach { player.inventory.deleteItem(it, player.inventory.getAmountOf(it)) }
            player.vars.setVarBit(Varbit.magictraining_zone, 0)
        }
    }
}
