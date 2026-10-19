package com.rs.game.content.skills.crafting

import com.rs.game.content.skills.util.CreateActionD
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item

private val SPINNING_WHEELS = arrayOf<Any>("Spinning wheel", Loc.loc_69558)
private val MATERIALS = arrayOf(
    arrayOf(Item(Obj.flax)), arrayOf(Item(Obj.yak_hair)), arrayOf(Item(Obj.wool)), arrayOf(Item(Obj.xbows_sinew)), arrayOf(Item(Obj.viking_golden_fleece))
)
private val PRODUCTS = arrayOf(
    arrayOf(Item(Obj.obj_1777)), arrayOf(Item(Obj.rope)), arrayOf(Item(Obj.ball_of_wool)), arrayOf(Item(Obj.xbows_crossbow_string)), arrayOf(Item(Obj.viking_golden_wool))
)
private val REQS = intArrayOf(1, 1, 1, 1, 1)
private val XP = doubleArrayOf(15.0, 15.0, 15.0, 1.0, 1.0)
private val ANIMS = IntArray(MATERIALS.size) { Seq.seq_883 }

@ServerStartupEvent
fun mapSpinningWheels() {
    onObjectClick(*SPINNING_WHEELS) { e ->
        if (e.opNum == 2) e.player.startConversation(CreateActionD(e.player, MATERIALS, PRODUCTS, XP, ANIMS, REQS, Constants.CRAFTING, 2))
    }
    onItemOnObject(SPINNING_WHEELS) { e ->
        val index = MATERIALS.indexOfFirst { it[0].id == e.item.id }
        if (index == -1) return@onItemOnObject
        e.player.startConversation(CreateActionD(e.player, arrayOf(arrayOf(MATERIALS[index][0])), arrayOf(arrayOf(PRODUCTS[index][0])), doubleArrayOf(XP[index]), intArrayOf(ANIMS[index]), Constants.CRAFTING, 2))
    }
}
