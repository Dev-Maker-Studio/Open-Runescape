package com.rs.game.content.quests.shadesofmortton

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.model.Item

internal data class SerumDose(val remaining: Int?, val permanent: Boolean)

internal val serumDoses = buildMap {
    val temporaryVials = listOf(Obj.mort_serum4, Obj.mort_serum3, Obj.mort_serum2, Obj.mort_serum1, Obj.vial_empty)
    val permanentVials = listOf(Obj.mort_serum_perm4, Obj.mort_serum_perm3, Obj.mort_serum_perm2, Obj.mort_serum_perm1, Obj.vial_empty)
    val temporaryFlasks = listOf(Obj.bigpot_6dosemort_serum, Obj.bigpot_5dosemort_serum, Obj.bigpot_4dosemort_serum, Obj.bigpot_3dosemort_serum, Obj.bigpot_2dosemort_serum, Obj.bigpot_1dosemort_serum)
    val permanentFlasks = listOf(Obj.bigpot_6dosemort_serum_perm, Obj.bigpot_5dosemort_serum_perm, Obj.bigpot_4dosemort_serum_perm, Obj.bigpot_3dosemort_serum_perm, Obj.bigpot_2dosemort_serum_perm, Obj.bigpot_1dosemort_serum_perm)
    temporaryVials.zipWithNext().forEach { (id, remaining) -> put(id, SerumDose(remaining, false)) }
    permanentVials.zipWithNext().forEach { (id, remaining) -> put(id, SerumDose(remaining, true)) }
    temporaryFlasks.forEachIndexed { index, id -> put(id, SerumDose(temporaryFlasks.getOrNull(index + 1), false)) }
    permanentFlasks.forEachIndexed { index, id -> put(id, SerumDose(permanentFlasks.getOrNull(index + 1), true)) }
}

@ServerStartupEvent
fun mapMortton() {
    val razmire = arrayOf<Any>(Npc.npc_7899, Npc.razmire_keelgan_afflicted, Npc.razmire_keelgan)
    val ulsquire = arrayOf<Any>(Npc.npc_7898, Npc.ulsquire_shauncy_afflicted, Npc.ulsquire_shauncy)
    onNpcClick(*razmire) { (player, _, option) -> player.talkMorttonRazmire(option = option) }
    onNpcClick(*ulsquire) { (player) -> player.talkMorttonUlsquire() }
    onItemOnNpc(*razmire) { (player, item) -> player.cureMorttonResident(item, false) }
    onItemOnNpc(*ulsquire) { (player, item) -> player.cureMorttonResident(item, true) }
    onLogin { (player) ->
        player.vars.setVarBit(Varbit.razmire_visible, player.vars.getVarBit(Varbit.razmire_perm_serum_used))
        player.vars.setVarBit(Varbit.ulsquire_visible, player.vars.getVarBit(Varbit.ulsquire_perm_serum_used))
    }
}

private fun Player.cureMorttonResident(item: Item, priest: Boolean) {
    val dose = serumDoses[item.id] ?: return
    val visible = if (priest) Varbit.ulsquire_visible else Varbit.razmire_visible
    val permanent = if (priest) Varbit.ulsquire_perm_serum_used else Varbit.razmire_perm_serum_used
    if (vars.getVarBit(permanent) == 1 || (!dose.permanent && vars.getVarBit(visible) == 1)) {
        sendMessage("He's already cured!")
        return
    }
    consumeMorttonSerum(item, dose)
    vars.setVarBit(visible, 1)
    if (dose.permanent) {
        vars.saveVarBit(permanent, 1)
        vars.saveVarBit(visible, 1)
        val coins = if (priest) Utils.random(200, 215) else Utils.random(186, 211)
        startConversation {
            npc(if (priest) Npc.ulsquire_shauncy else Npc.razmire_keelgan, CALM_TALK,
                if (priest) "Well done... I feel this serum is really working on me. I think you've cracked it my friend! I'd like to give you something for your trouble!"
                else "Well done... I feel this serum is really working on me. I think you've cracked it, my friend! I'd like to give you something for your trouble!") {
                inventory.addCoins(coins)
            }
            simple("${if (priest) "Ulsquire" else "Razmire"} gives you $coins coins.")
            exec { if (priest) talkMorttonUlsquire() else talkMorttonRazmire() }
        }
    } else {
        val key = if (priest) "morttonUlsquireFirstCure" else "morttonRazmireFirstCure"
        val first = !getBool(key)
        set(key, true)
        val timerKey = "${key}Timer"
        val generation = tempAttribs.getI(timerKey) + 1
        tempAttribs.setI(timerKey, generation)
        WorldTasks.schedule(50) {
            if (!hasFinished() && tempAttribs.getI(timerKey) == generation && vars.getVarBit(permanent) == 0) vars.setVarBit(visible, 0)
        }
        if (priest) talkMorttonUlsquire(first) else talkMorttonRazmire(first)
    }
}

internal fun Player.consumeMorttonSerum(item: Item, dose: SerumDose) {
    if (dose.remaining == null) {
        inventory.deleteItem(item)
        sendMessage("The flask shatters as you use the last dose.")
    } else {
        item.id = dose.remaining
        inventory.refresh()
    }
}
