package com.rs.game.content.commands.development

import com.rs.engine.command.Commands
import com.rs.game.model.item.ItemTooltips
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.gameval.gen.Graphic
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item
import org.darkan.core.model.Rights

private const val PLAYER_MOD_ICON = 0
private const val JAGEX_MOD_ICON = 1
private const val PROTECT_FROM_MELEE = 0
private const val PROTECT_FROM_MISSILES = 1
private const val PROTECT_FROM_MAGIC = 2

private fun sprite(graphic: Int, frame: Int = 0) = "<sprite=$graphic,$frame>"

private fun obj(id: Int, amount: Int = 1) = "<obj=$id,$amount>"

private fun value(text: String) = "<col=2dc3ff>$text</col>"

private val showcaseTooltip = listOf(
    "<col=ffb000><shad=5a1a00>Legendary</shad></col> <col=ff981f>Tier 75</col>",
    obj(Obj.abyssal_whip),
    "Accuracy: ${value("1,924")}",
    "Type: ${value("Melee")}   Speed: <col=00ff00>Fast</col>",
    "Requires: ${sprite(Graphic.staticons_0)} ${value("70")}  ${sprite(Graphic.staticons_2)} ${value("1")}",
    "Item Level: ${value("10")}",
    "Item XP: ${value("49,164/57,095")}",
    sprite(Graphic.headicons_prayer, PROTECT_FROM_MELEE) + " " + sprite(Graphic.headicons_prayer, PROTECT_FROM_MISSILES) + " " + sprite(Graphic.headicons_prayer, PROTECT_FROM_MAGIC),
    "<col=c8c8c8>Precise 2</col>",
    "<col=c8c8c8>Eruptive 2</col>",
    "Drops from: ${obj(Obj.coins, 10_000)} ${obj(Obj.dragon_bones, 5)} ${obj(Obj.shark, 28)}",
    "${sprite(Graphic.login_tick)} <col=00ff00>Tradeable</col>   ${sprite(Graphic.cross)} <col=ff4040>Degrades</col>",
    "<col=ff3030>Red</col> <col=30ff30>green</col> <col=4090ff>blue</col> <shad=ff00ff>shadow</shad> <str>struck</str> <u=ffffff>underline</u>",
    "Crowns: <img=$PLAYER_MOD_ICON> <img=$JAGEX_MOD_ICON>   Escapes: <lt>tag<gt> <euro>5 <copy> <reg> 3 <times> 4",
).joinToString("<br>")

@ServerStartupEvent
fun loadTooltipCommands() {
    Commands.add(Rights.DEVELOPER, "tooltipwhip", "Gives an abyssal whip whose tooltip shows off every tooltip text feature.") { p, _ ->
        val whip = Item(Obj.abyssal_whip, 1).addMetaData(ItemTooltips.METADATA_KEY, showcaseTooltip)
        if (!p.inventory.addItem(whip)) {
            p.sendMessage("You need a free inventory slot.")
            return@add
        }
        p.sendMessage("Hover the whip to see its tooltip.")
    }
}
