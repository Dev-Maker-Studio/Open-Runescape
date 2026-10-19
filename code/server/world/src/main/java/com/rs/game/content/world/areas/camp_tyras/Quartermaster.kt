package com.rs.game.content.world.areas.camp_tyras

import com.rs.engine.Shop
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.shop.ShopsHandler
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private const val BEFORE_REGICIDE_SHOP = "quartermasters_stores_before_regicide"

@ServerStartupEvent
fun mapTyrasQuartermaster() {
    val stock = ShopsHandler.getShop("quartermasters_stores").mainStock.filter { it.item.id != Obj.dragon_halberd }.toTypedArray()
    ShopsHandler.addShop(BEFORE_REGICIDE_SHOP, Shop("Quartermaster's Stores", Obj.coins, stock, false, false))
    onNpcClick(Npc.regicidegeneralshopkeeper, options = arrayOf("Talk-to", "Trade")) { (player, npc, option) ->
        if (option == "Trade") {
            player.openTyrasShop()
            return@onNpcClick
        }
        player.startConversation {
            if (player.isQuestComplete(Quest.REGICIDE))
                npc(npc, CALM_TALK, "Good day ${player.getPronoun("Sir", "Miss")}. I'm the quartermaster for King Tyras's camp. We have a little we could trade here. We have a new stock of dragon halberds. Would you like a look at what we have now?")
            else
                npc(npc, CALM_TALK, "Good day ${player.getPronoun("Sir", "Miss")}. I'm the quartermaster for King Tyras's camp. We have a little we could trade here. We should be getting a new stock soon. Would you like a look at what we do have now?")
            options {
                op("Yes please. What are you selling?") { exec { player.openTyrasShop() } }
                op("No thanks.") { player(CALM_TALK, "No thanks.") }
            }
        }
    }
}

private fun Player.openTyrasShop() {
    ShopsHandler.openShop(this, if (isQuestComplete(Quest.REGICIDE)) "quartermasters_stores" else BEFORE_REGICIDE_SHOP)
}
