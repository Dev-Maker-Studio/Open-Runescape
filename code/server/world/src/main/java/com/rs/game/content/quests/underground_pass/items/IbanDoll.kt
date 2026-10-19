package com.rs.game.content.quests.underground_pass.items

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.game.content.quests.underground_pass.updateIbanDollStage
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem

@ServerStartupEvent
fun mapIbanDollItems() {
    onItemClick(Obj.obj_1492, options = arrayOf("Search")) { (player) ->
        player.sendMessage("You carefully search the doll...", true)
        if (player.vars.getVarBit(Varbit.upass_iban_blood) == 1)
            player.sendMessage("Blood has been smeared onto the doll.", true)
        if (player.vars.getVarBit(Varbit.upass_iban_conscience) == 1)
            player.sendMessage("Crushed bones have been spread onto the doll.", true)
        if (player.vars.getVarBit(Varbit.upass_iban_flesh) == 1)
            player.sendMessage("Burnt ash has been smeared onto the doll.", true)
        if (player.vars.getVarBit(Varbit.upass_iban_shadow) == 1)
            player.sendMessage("A dark liquid has been poured over the doll.", true)
        player.sendMessage("The doll is made from old wood and cloth.", true)
    }

    val items = intArrayOf(Obj.obj_1492, Obj.ibansdove, Obj.ibansshadow, Obj.ibans_ashes)
    onItemOnItem(items, items) { event ->
        val doll = when (Obj.obj_1492) {
            event.item1.id -> event.item1
            event.item2.id -> event.item2
            else -> return@onItemOnItem
        }
        val part = if (doll === event.item1) event.item2 else event.item1
        val varbit = when (part.id) {
            Obj.ibansdove -> Varbit.upass_iban_conscience
            Obj.ibans_ashes -> Varbit.upass_iban_flesh
            Obj.ibansshadow -> Varbit.upass_iban_shadow
            else -> return@onItemOnItem
        }
        if (event.player.vars.getVarBit(varbit) == 1) {
            event.player.sendMessage("That part of Iban's spirit is already bound to the doll.")
            return@onItemOnItem
        }
        event.player.inventory.deleteItem(part)
        event.player.vars.saveVarBit(varbit, 1)
        event.player.sendMessage("The item dissolves and becomes part of the Doll of Iban.")
        event.player.updateIbanDollStage()
    }
}
