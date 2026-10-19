package com.rs.game.content.world.areas.ardougne

import com.rs.engine.quest.Quest
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.shop.ShopsHandler
import org.darkan.core.gameval.gen.Npc
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

private val COMBAT_BRACELETS = setOf(Obj.jewl_bracelet_of_combat, Obj.jewl_bracelet_of_combat_1, Obj.jewl_bracelet_of_combat_2, Obj.jewl_bracelet_of_combat_3, Obj.jewl_bracelet_of_combat_4)
private val SKILLS_NECKLACES = setOf(Obj.jewl_necklace_of_skills, Obj.jewl_necklace_of_skills_1, Obj.jewl_necklace_of_skills_2, Obj.jewl_necklace_of_skills_3, Obj.jewl_necklace_of_skills_4)

@ServerStartupEvent
fun mapLegendsGuild() {
    onNpcClick(Npc.fionella, Npc.siegfried_erkle, options = arrayOf("Talk-to", "Trade")) { (player, npc, option) ->
        if (!player.isQuestComplete(Quest.LEGENDS_QUEST, "to use the Legends' Guild shops.")) return@onNpcClick
        val shop = if (npc.id == Npc.fionella) "legends_guild_general_store" else "legends_guild_shop_of_useful_items"
        if (option == "Trade") ShopsHandler.openShop(player, shop)
        else player.startConversation {
            npc(npc, CALM_TALK, "Welcome to the Legends' Guild. Would you like to see what I have for sale?")
            options { opExec("Yes, please.") { ShopsHandler.openShop(player, shop) }; op("No, thank you.") }
        }
    }
    onItemOnObject(arrayOf(Loc.lg_totem_pole_legends), (COMBAT_BRACELETS + SKILLS_NECKLACES).toTypedArray<Any>()) { (player) ->
        if (!player.isQuestComplete(Quest.LEGENDS_QUEST, "to recharge jewellery at the Legends' Guild.")) return@onItemOnObject
        var changed = false
        for (item in player.inventory.items.array()) {
            if (item == null) continue
            val charged = when (item.id) { in COMBAT_BRACELETS -> Obj.jewl_bracelet_of_combat_4; in SKILLS_NECKLACES -> Obj.jewl_necklace_of_skills_4; else -> continue }
            if (item.id != charged) { item.id = charged; changed = true }
        }
        if (changed) {
            player.inventory.refresh()
            player.anim(Seq.human_furnace)
            player.sendMessage("You recharge some jewellery at the totem.")
        }
    }
}
