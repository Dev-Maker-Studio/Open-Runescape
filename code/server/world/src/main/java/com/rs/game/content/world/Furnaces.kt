package com.rs.game.content.world

import com.rs.game.content.quests.regicide.items.makeRegicideQuicklime
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Loc
import com.rs.game.content.quests.familycrest.PERFECT_BAR
import com.rs.game.content.quests.familycrest.PERFECT_ORE
import com.rs.game.content.quests.familycrest.familyCrestItemOnFurnace
import com.rs.game.content.skills.crafting.GOLD_BAR
import com.rs.game.content.skills.crafting.Silver
import com.rs.game.content.skills.crafting.Silver.SILVER_BAR
import com.rs.game.content.skills.crafting.openInterface
import com.rs.game.content.skills.smithing.MoltenGlassMaking
import com.rs.game.content.skills.smithing.SmeltingD
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.model.Item
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapFurnaces() {
    onItemOnObject(arrayOf("Furnace", "Small furnace", "Clay forge", "Lava Furnace", Loc.loc_11666), arrayOf(Obj.limestone)) { (player, obj) -> player.makeRegicideQuicklime(obj) }
    onObjectClick("Furnace", "Small furnace", "Clay forge", "Lava Furnace", Loc.loc_11666) { e -> use(e.player, e.obj) }
    onItemOnObject(arrayOf("Furnace", "Small furnace", "Clay forge", "Lava Furnace", Loc.loc_11666), arrayOf(GOLD_BAR, SILVER_BAR)) { e -> useItem(e.player, e.`object`, e.item) }
    onItemOnObject(arrayOf("Furnace", "Small furnace", "Clay forge", "Lava Furnace", Loc.loc_11666), arrayOf(PERFECT_ORE, PERFECT_BAR)) { e -> familyCrestItemOnFurnace(e.player, e.item) }
}

private fun use(player: Player, furnace: GameObject?) {
    if (player.inventory.containsItem(Obj.limestone)) {
        player.sendOptionDialogue("Would you like to place limestone in the furnace?") { ops ->
            ops.add("Yes") { player.makeRegicideQuicklime(furnace) }
            ops.add("No") { useWithoutLimestone(player, furnace) }
        }
        return
    }
    useWithoutLimestone(player, furnace)
}

private fun useWithoutLimestone(player: Player, furnace: GameObject?) {
    if (player.inventory.containsItems(Item(MoltenGlassMaking.SODA_ASH), Item(MoltenGlassMaking.BUCKET_OF_SAND))) {
        MoltenGlassMaking.openDialogue(player)
        return
    }
    player.startConversation(SmeltingD(player, furnace))
}

private fun useItem(player: Player, furnace: GameObject, item: Item) {
    if (item.id == GOLD_BAR) {
        openInterface(player, false)
        player.tempAttribs.setO<Any>("jewelryObject", furnace)
        return
    }

    if (item.id == SILVER_BAR) {
        Silver.openSilverInterface(player)
        player.tempAttribs.setO<Any>("silverObject", furnace)
        return
    }
}
