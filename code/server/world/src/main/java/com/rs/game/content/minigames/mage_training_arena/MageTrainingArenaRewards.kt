package com.rs.game.content.minigames.mage_training_arena

import com.rs.game.model.entity.player.Player
import com.rs.engine.dialogue.HeadE.NONE
import com.rs.engine.dialogue.startConversation
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Inv
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item

object MageTrainingArenaRewards {
    private data class Reward(val item: Int, val tele: Int, val alchemy: Int, val enchantment: Int, val graveyard: Int, val previous: Int = -1) {
        val costs get() = intArrayOf(tele, alchemy, enchantment, graveyard)
    }

    private val rewards = listOf(
        Reward(Obj.magictraining_wand_beg, 30, 30, 300, 30),
        Reward(Obj.magictraining_wand_appr, 60, 60, 600, 60, Obj.magictraining_wand_beg),
        Reward(Obj.magictraining_wand_teach, 150, 200, 1500, 150, Obj.magictraining_wand_appr),
        Reward(Obj.magictraining_wand_master, 240, 240, 2400, 240, Obj.magictraining_wand_teach),
        Reward(Obj.magictraining_infinitytop, 400, 450, 4000, 400),
        Reward(Obj.magictraining_infinityhat, 350, 400, 3000, 350),
        Reward(Obj.magictraining_infinityboots, 120, 120, 1200, 120),
        Reward(Obj.magictraining_infinitygloves, 175, 225, 1500, 175),
        Reward(Obj.magictraining_infinitybottom, 450, 500, 5000, 450),
        Reward(Obj.magictraining_bookofmagic, 500, 550, 6000, 500),
        Reward(Obj.battlestaff, 1, 2, 20, 2),
        Reward(Obj.magictraining_peachspell, 200, 300, 2000, 200),
        Reward(Obj.mistrune, 1, 1, 15, 1),
        Reward(Obj.dustrune, 1, 1, 15, 1),
        Reward(Obj.mudrune, 1, 1, 15, 1),
        Reward(Obj.smokerune, 1, 1, 15, 1),
        Reward(Obj.steamrune, 1, 1, 15, 1),
        Reward(Obj.lavarune, 1, 1, 15, 1),
        Reward(Obj.cosmicrune, 0, 0, 5, 0),
        Reward(Obj.chaosrune, 0, 1, 5, 1),
        Reward(Obj.naturerune, 0, 1, 0, 1),
        Reward(Obj.deathrune, 2, 1, 20, 1),
        Reward(Obj.lawrune, 2, 0, 0, 0),
        Reward(Obj.soulrune, 2, 2, 25, 2),
        Reward(Obj.bloodrune, 2, 2, 25, 2)
    )

    fun open(player: Player) {
        if (!hasProgressHat(player)) {
            player.startConversation {
                npc(Npc.magictraining_guard_rewards, NONE, "Sorry, you must be carrying a Pizazz Progress Hat so I can check what points you have. If you haven't got one, talk to my fellow guardian downstairs.")
            }
            return
        }
        if (player.controllerManager.controller is MageTrainingArenaController) return
        player.interfaceManager.sendInterface(Interface.interface_197)
        player.packets.sendItems(Inv.magictraining_inventory, rewards.map { Item(it.item) }.toTypedArray())
        player.packets.sendRunScript(Cs2.magictraining_shop_points)
        player.packets.setIFRightClickOps(Interface.interface_197, Utils.componentIdFromHash(Component.interface_197.component_197_16), 0, rewards.lastIndex, 0, 1, 9)
    }

    fun click(player: Player, slot: Int, option: Int) {
        if (player.controllerManager.controller is MageTrainingArenaController) return
        val reward = rewards.getOrNull(slot) ?: return
        if (option == 1) {
            player.sendMessage("${Item(reward.item).definitions.name}: ${reward.tele} Telekinetic, ${reward.alchemy} Alchemist, ${reward.enchantment} Enchantment and ${reward.graveyard} Graveyard points.")
            return
        }
        if (option != 2) return
        if (reward.item == Obj.magictraining_peachspell && player.vars.getVarBit(Varbit.magictraining_bonespeaches) != 0) {
            player.sendMessage("You have already unlocked Bones to Peaches.")
            return
        }
        if (reward.previous != -1 && !player.inventory.containsItem(reward.previous)) {
            player.sendMessage("You need a ${Item(reward.previous).definitions.name.lowercase()} in your inventory to upgrade it.")
            return
        }
        if (ArenaRoom.entries.withIndex().any { (index, room) -> MageTrainingArena.points(player, room) < reward.costs[index] }) {
            player.sendMessage("You do not have enough Pizazz points for that reward.")
            return
        }
        val spell = reward.item == Obj.magictraining_peachspell
        if (!spell && reward.previous == -1 && !player.inventory.hasFreeSlots() && (!Item(reward.item).definitions.isStackable() || !player.inventory.containsItem(reward.item))) {
            player.sendMessage("You don't have enough inventory space.")
            return
        }
        if (reward.previous != -1) player.inventory.deleteItem(reward.previous, 1)
        if (spell) player.vars.saveVarBit(Varbit.magictraining_bonespeaches, 1)
        else player.inventory.addItem(reward.item, 1)
        ArenaRoom.entries.forEachIndexed { index, room -> MageTrainingArena.addPoints(player, room, -reward.costs[index]) }
        player.packets.sendRunScript(Cs2.magictraining_shop_points)
        player.sendMessage(if (spell) "You have unlocked the Bones to Peaches spell." else "You purchase ${Item(reward.item).definitions.name.lowercase()}.")
    }
}
