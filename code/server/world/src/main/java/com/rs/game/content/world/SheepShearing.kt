package com.rs.game.content.world

import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound

private val SHEEP = arrayOf<Any>(
    Npc.npc_5157, Npc.npc_1765, Npc.npc_43, Npc.npc_5160, Npc.npc_5161, Npc.npc_5156,
    Npc.npc_14908, Npc.npc_14909, Npc.npc_14910, Npc.npc_5158, Npc.npc_3310, Npc.npc_5164, Npc.sheep_black
)
private val RAMS = arrayOf<Any>(Npc.npc_3673, Npc.npc_5168)
private const val RAM_TOO_VIOLENT = "That sheep looks a little too violent to shear..."

@ServerStartupEvent
fun mapSheepShearing() {
    onNpcClick(*SHEEP, options = arrayOf("Shear")) { shear(it.player, it.npc) }
    onItemOnNpc(*SHEEP) { shear(it.player, it.npc) }
    onNpcClick(*RAMS, options = arrayOf("Shear")) { it.player.sendMessage(RAM_TOO_VIOLENT) }
    onItemOnNpc(*RAMS) { if (it.item.id == Obj.shears) it.player.sendMessage(RAM_TOO_VIOLENT) }
}

private fun shear(player: Player, npc: NPC) {
    val npcId = npc.id
    if (Utils.getRandomInclusive(2) == 0) {
        npc.forceTalk("Baa!")
        npc.soundEffect(player, Sound.sound_756, true)
        npc.addWalkSteps(npcId, npcId, 4, true)
        npc.run = true
        player.sendMessage("The sheep runs away from you.")
        return
    }
    if (!player.inventory.containsItem(Obj.shears, 1)) {
        player.sendMessage("You need a pair of shears to shear the sheep.")
        return
    }
    val black = npcId == Npc.sheep_black
    player.soundEffect(Sound.sound_761, true)
    player.inventory.addItem(if (black) Obj.black_wool else Obj.wool, 1)
    player.sendMessage("You shear the sheep of it's fleece.")
    player.anim(Seq.human_shearing)
    npc.transformIntoNPC(if (black) Npc.npc_8877 else Npc.npc_5149)
    WorldTasks.delay(Ticks.fromSeconds(10)) { npc.transformIntoNPC(npcId) }
}
