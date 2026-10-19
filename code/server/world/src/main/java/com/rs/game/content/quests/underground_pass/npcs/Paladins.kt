package com.rs.game.content.quests.underground_pass.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.model.entity.player.Player
import org.darkan.core.model.Item
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onNpcDeath

private const val RECEIVED_PALADIN_FOOD_ATTR = "receivedPaladinFood"

@ServerStartupEvent
fun mapUndergroundPassPaladins() {
    onNpcClick(Npc.upass_paladin1, Npc.upass_paladin2, Npc.upass_paladin3, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            val attributes = player.questManager.getAttribs(Quest.UNDERGROUND_PASS)
            if (player.isQuestStarted(Quest.UNDERGROUND_PASS) &&
                !player.isQuestComplete(Quest.UNDERGROUND_PASS) &&
                !attributes.getB(RECEIVED_PALADIN_FOOD_ATTR)
            ) {
                npc(npc, HAPPY_TALKING, "A traveller! We are knights of Ardougne, sent to investigate this cursed place.")
                player(CALM_TALK, "King Lathas sent me to find a route through.")
                npc(npc, CALM_TALK, "Then take some of our food. Keep your strength up.")
                exec {
                    player.inventory.addItemDrop(Obj.bread, 2)
                    player.inventory.addItemDrop(Obj.meat_pie, 2)
                    player.inventory.addItemDrop(Obj.stew, 1)
                    player.inventory.addItemDrop(Obj._2doseprayerrestore, 1)
                    player.inventory.addItemDrop(Obj._2dose1attack, 1)
                    attributes.setB(RECEIVED_PALADIN_FOOD_ATTR, true)
                }
            } else {
                npc(npc, SKEPTICAL, "The well ahead is sealed. Three crests seem to form its key.")
            }
        }
    }

    onNpcDeath(Npc.upass_paladin1, Npc.upass_paladin2, Npc.upass_paladin3) { event ->
        val player = event.killer as? Player ?: return@onNpcDeath
        if (!player.isQuestStarted(Quest.UNDERGROUND_PASS) || player.isQuestComplete(Quest.UNDERGROUND_PASS))
            return@onNpcDeath
        val (badge, varBit) = when (event.npc.id) {
            Npc.upass_paladin1 -> Obj.paladinbadge1 to Varbit.upass_paladin_badge_1
            Npc.upass_paladin2 -> Obj.paladinbadge2 to Varbit.upass_paladin_badge_2
            Npc.upass_paladin3 -> Obj.paladinbadge3 to Varbit.upass_paladin_badge_3
            else -> return@onNpcDeath
        }
        if (!player.inventory.containsItem(badge) && !player.bank.containsItem(badge))
            World.addGroundItem(Item(badge), event.npc.tile, player)
        player.vars.saveVarBit(varBit, 1)
    }
}
