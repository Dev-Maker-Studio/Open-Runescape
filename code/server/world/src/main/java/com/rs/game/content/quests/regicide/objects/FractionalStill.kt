package com.rs.game.content.quests.regicide.objects

import com.rs.engine.quest.Quest
import com.rs.game.content.quests.regicide.STAGE_PREPARE_EXPLOSIVE
import com.rs.game.content.quests.regicide.STAGE_DESTROY_TENT
import com.rs.game.content.quests.regicide.items.replaceRegicideIngredients
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varp
import world.gregs.voidps.type.Tile

private const val STILL_LOADED = "stillLoaded"
private const val STILL_SESSION = "regicideStillSession"
private data class StillSession(val tile: Tile, var heat: Int = 0, var pressure: Int = 0, var heatTicks: Int = 0, var pressureValve: Int = 0, var tarValve: Int = 0)

@ServerStartupEvent
fun mapRegicideFractionalStill() {
    onObjectClick(Loc.regicide_fractionalizing_still, Loc.rimm_imp_fractionalizing_still) { (player, obj) ->
        player.musicsManager.playSpecificAmbientSong(Midi.midi_12, true)
        player.openRegicideStill(obj.tile)
    }
    onItemOnObject(arrayOf(Loc.regicide_fractionalizing_still, Loc.rimm_imp_fractionalizing_still)) { e ->
        val player = e.player
        if (!player.canUseRegicideStill())
            return@onItemOnObject
        val attributes = player.questManager.getAttribs(Quest.REGICIDE)
        if (e.item.id == Obj.regicide_barrel_tar) {
            if (attributes.getB(STILL_LOADED)) {
                player.sendMessage("The still is already full.")
                return@onItemOnObject
            }
            if (player.replaceRegicideIngredients(Obj.regicide_barrel_tar, null, Obj.regicide_barrel_empty)) {
                attributes.setB(STILL_LOADED, true)
                player.vars.saveVar(Varp.regicide_still_total, 0)
            }
        } else if (e.item.id == Obj.regicide_barrel_empty && attributes.getB(STILL_LOADED) && player.vars.getVar(Varp.regicide_still_total) >= 25) {
            player.collectRegicideNaphtha()
        } else {
            player.sendMessage("I don't want to damage the still.")
        }
    }
    onButtonClick(Interface.interface_286) { e ->
        val player = e.player
        val session = player.tempAttribs.getO<StillSession>(STILL_SESSION) ?: return@onButtonClick
        if (!player.withinDistance(session.tile, 3) || !player.questManager.getAttribs(Quest.REGICIDE).getB(STILL_LOADED))
            return@onButtonClick
        when (e.component) {
            Component.interface_286.component_286_8 -> player.closeInterfaces()
            Component.interface_286.component_286_22 -> {
                if (player.inventory.containsItem(Obj.coal)) {
                    player.inventory.deleteItem(Obj.coal, 1)
                    session.heatTicks = 6
                    session.heat = (session.heat + 3).coerceAtMost(12)
                } else {
                    player.sendMessage("You need coal.")
                }
            }
            Component.interface_286.component_286_27 -> session.pressureValve = (session.pressureValve - 1).coerceAtLeast(0)
            Component.interface_286.component_286_28 -> session.pressureValve = (session.pressureValve + 1).coerceAtMost(2)
            Component.interface_286.component_286_29 -> session.tarValve = (session.tarValve - 1).coerceAtLeast(0)
            Component.interface_286.component_286_30 -> session.tarValve = (session.tarValve + 1).coerceAtMost(2)
        }
        player.updateStillGauges(session)
    }
}

private fun Player.openRegicideStill(tile: Tile) {
    if (!canUseRegicideStill())
        return
    val attributes = questManager.getAttribs(Quest.REGICIDE)
    if (!attributes.getB(STILL_LOADED)) {
        sendMessage("You need to fill the still before using it.")
        return
    }
    if (vars.getVar(Varp.regicide_still_total) >= 25) {
        collectRegicideNaphtha()
        return
    }
    val session = StillSession(tile)
    tempAttribs.setO<StillSession>(STILL_SESSION, session)
    interfaceManager.sendInterface(Interface.interface_286)
    updateStillGauges(session)
    schedule("RegicideFractionalStill") {
        while (!hasFinished() && interfaceManager.topOpen(Interface.interface_286) && withinDistance(tile, 3) && tempAttribs.getO<StillSession>(STILL_SESSION) === session && attributes.getB(STILL_LOADED)) {
            wait(2)
            if (!interfaceManager.topOpen(Interface.interface_286) || !withinDistance(tile, 3))
                break
            if (session.heatTicks > 0)
                session.heatTicks--
            else {
                session.heat = (session.heat - 1).coerceAtLeast(0)
                session.heatTicks = 6
            }
            session.pressure = (session.pressure + session.tarValve - session.pressureValve * 2).coerceAtLeast(0)
            if (session.heat >= 12 || session.pressure >= 12) {
                attributes.setB(STILL_LOADED, false)
                vars.saveVar(Varp.regicide_still_total, 0)
                sendMessage("The still has overheated or exceeded its pressure limit. You must start again.")
                closeInterfaces()
                break
            }
            if (session.heat in 6..10 && session.pressure in 4..8 && session.tarValve > 0)
                vars.saveVar(Varp.regicide_still_total, vars.getVar(Varp.regicide_still_total) + 1)
            updateStillGauges(session)
            if (vars.getVar(Varp.regicide_still_total) >= 25) {
                closeInterfaces()
                collectRegicideNaphtha()
                break
            }
        }
        if (tempAttribs.getO<StillSession>(STILL_SESSION) === session)
            tempAttribs.removeO<StillSession>(STILL_SESSION)
    }
}

private fun Player.updateStillGauges(session: StillSession) {
    val settings = (1 shl session.pressure.coerceIn(0, 12)) or
        (1 shl (13 + session.heat.coerceIn(0, 12))) or
        (1 shl (26 + session.pressureValve)) or (1 shl (29 + session.tarValve))
    vars.setVar(Varp.regicide_still_settings, settings)
    vars.setVar(Varp.regicide_still_total, vars.getVar(Varp.regicide_still_total))
}

private fun Player.collectRegicideNaphtha() {
    val attributes = questManager.getAttribs(Quest.REGICIDE)
    if (!attributes.getB(STILL_LOADED) || vars.getVar(Varp.regicide_still_total) < 25)
        return
    if (!replaceRegicideIngredients(Obj.regicide_barrel_empty, null, Obj.regicide_barrel_naphtha)) {
        sendMessage("You need an empty barrel to collect the distillate.")
        return
    }
    attributes.setB(STILL_LOADED, false)
    vars.saveVar(Varp.regicide_still_total, 0)
}

private fun Player.canUseRegicideStill(): Boolean {
    if (getQuestStage(Quest.REGICIDE) >= STAGE_DESTROY_TENT)
        return true
    sendMessage(if (getQuestStage(Quest.REGICIDE) == STAGE_PREPARE_EXPLOSIVE)
        "You should show the Big Book of Bangs to the Chemist before using the still."
    else
        "You do not yet know how to use the still.")
    return false
}
