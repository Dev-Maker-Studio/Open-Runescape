package com.rs.game.content.skills.thieving

import com.rs.game.content.items.degradeGlovesOfSilence
import com.rs.game.model.entity.ForceTalk
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.drop.DropTable
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Animation
import org.darkan.core.model.SpotAnim

private val LOOT_ANIMS = intArrayOf(Seq.human_pickpocket, Seq.agil09_pickpocket_2_items, Seq.agil09_pickpocket_3_items, Seq.agil09_pickpocket_4_items)
private val LOOT_SPOTANIMS = intArrayOf(-1, Spotanim.agil09_pickpocket_2_items_spot, Spotanim.agil09_pickpocket_3_items_spot, Spotanim.agil09_pickpocket_4_items_spot)
private val LOOT_MESSAGES = arrayOf("double", "triple", "quadruple")

@ServerStartupEvent
fun mapPickpocketing() {
    onNpcClick(options = arrayOf("Pickpocket", "Grab-feather")) { (player, npc) ->
        val pocket = PickPocketableNPC.get(npc.id) ?: return@onNpcClick
        npc.resetDirection()
        player.actionManager.setAction(PickPocketAction(npc, pocket))
    }
}

class PickPocketAction(@JvmField val npc: NPC, @JvmField val npcData: PickPocketableNPC) : PlayerAction() {
    private var index = 0
    private var success = false

    private val npcName get() = npc.definitions.name.lowercase()

    override fun start(player: Player): Boolean {
        if (!checkAll(player)) return false
        success = successful(player)
        player.faceEntityTile(npc)
        if (npcData == PickPocketableNPC.DESERT_PHOENIX) player.sendMessage("You attempt to grab the phoenix's tail-feather.")
        else player.sendMessage("You attempt to pick the $npcName's pocket...")
        WorldTasks.delay(0) {
            player.anim(LOOT_ANIMS[index])
            if (index > 0) player.spotAnim(LOOT_SPOTANIMS[index])
        }
        setActionDelay(player, 2)
        player.lock()
        return true
    }

    override fun process(player: Player): Boolean = checkAll(player)

    override fun processWithDelay(player: Player): Int {
        if (success) loot(player) else fail(player)
        stop(player)
        return -1
    }

    private fun fail(player: Player) {
        if (npcData == PickPocketableNPC.DESERT_PHOENIX) player.sendMessage("You fail to grab the feather.")
        else player.sendMessage("You fail to pick the $npcName's pocket.")
        npc.setNextAnimation(npcData.stunAnimation ?: Animation(Seq.seq_422))
        npc.faceEntityTile(player)
        player.anim(Seq.human_unarmedblock)
        player.setNextSpotAnim(SpotAnim(Spotanim.stunned, 5, 60))
        player.sendMessage("You've been stunned.")
        degradeGlovesOfSilence(player)
        player.applyHit(Hit(player, npcData.stunDamage.toInt(), HitLook.TRUE_DAMAGE))
        npc.setNextForceTalk(ForceTalk(when (npcData) {
            PickPocketableNPC.MASTER_FARMER, PickPocketableNPC.FARMER -> "Cor blimey mate, what are ye doing in me pockets?"
            PickPocketableNPC.DESERT_PHOENIX -> "Squawk!"
            else -> "What do you think you're doing?"
        }))
    }

    private fun loot(player: Player) {
        if (npcData == PickPocketableNPC.DWARF_TRADER) Thieving.checkGuards(player)
        player.sendMessage(lootMessage(), true)
        player.incrementCount("${npc.definitions.name} pickpocketed")
        player.skills.addXp(Constants.THIEVING, npcData.experience)
        repeat(index + 1) {
            DropTable.calculateDrops(player, npcData.loot).forEach { player.inventory.addItem(it.id, it.amount) }
        }
    }

    override fun stop(player: Player) {
        player.unlock()
        npc.setNextFaceEntity(null)
        player.setNextFaceEntity(null)
        setActionDelay(player, 1)
        if (!success) player.lock(npcData.stunTime.toInt())
    }

    private fun successful(player: Player): Boolean {
        if (!npcData.rollSuccess(player)) return false
        index = if (Utils.getRandomInclusive(50) < 5) highestLootTier(player) else 0
        return true
    }

    private fun highestLootTier(player: Player): Int {
        val thieving = player.skills.getLevel(Constants.THIEVING)
        val agility = player.skills.getLevel(Constants.AGILITY)
        return (0 until 4).lastOrNull { npcData.thievingLevels[it] <= thieving && npcData.agilityLevels[it] <= agility } ?: 0
    }

    private fun lootMessage(): String = when {
        npcData == PickPocketableNPC.DESERT_PHOENIX -> "You grab a tail-feather."
        index == 0 -> "You succesfully pick the $npcName's pocket."
        else -> "Your lighting-fast reactions allow you to steal ${LOOT_MESSAGES[index - 1]} loot."
    }

    private fun checkAll(player: Player): Boolean {
        if (player.isDead || player.hasFinished() || npc.isDead || npc.hasFinished() || player.hasPendingHits()) return false
        if (player.skills.getLevel(Constants.THIEVING) < npcData.thievingLevels[0]) {
            player.simpleDialogue("You need a thieving level of ${npcData.thievingLevels[0]} to steal from the ${npc.name.lowercase()}.")
            return false
        }
        if (player.inventory.freeSlots < 1) {
            player.sendMessage("You don't have enough space in your inventory.")
            return false
        }
        if (player.attackedBy != null && player.inCombat()) {
            player.sendMessage("You can't do that while you're in combat.")
            return false
        }
        if (npc.attackedBy != null && npc.inCombat()) {
            player.sendMessage("The ${npc.name.lowercase()} is under combat.")
            return false
        }
        return true
    }
}
