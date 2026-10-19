package com.rs.game.content.quests.regicide.items

import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import com.rs.engine.book.Book
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.gameval.gen.Obj

private class BigBookOfBangs : Book("The Big Book of Bangs", """
In this book I will try to cover all the explosive reactions I witnessed on my travels. Let us start with one of the more simple reactions, one I'm sure you have seen yourself.

When mining, certain gasses are released. Once these gasses are mixed with the air it only takes a small spark to ignite them. Unfortunately the result is uncontrollable.

In the far east I found a recipe for a mix they called fire oil. This fire oil has an explosive capability but also it sticks to anything it touches and burns.

Ingredients.

Quick Lime.
This is a highly caustic compound and is created by the endothermic reaction of limestone.

Brimstone.
Known as yellow crystals. odourless, insoluble and brittle as a solid. But as gas quite toxic, preventing respiration.

Naphtha.
A colourless, flammable solvent. One of the more volatile fractions of the fractional distillation of coal tar.

Preparation.

Great care must be taken when mixing this. The brimstone, naphtha and quicklime may be mixed at any time, but only light it at the last second. The tricky thing is a delivery method. A naked flame is required to ignite the concoction, the problem being that the resulting explosion will cover the area in hot sticky fire.

The last bang in my list comes from a secret recipe digging compound based on nitroglycerine. I have seen the results but could not discover the ingredients. I'm told that the secret of this dig compound can be found out by becoming an archaeologist.
""".trimIndent())

@ServerStartupEvent
fun mapRegicideBooks() {
    onItemClick(Obj.obj_3230, options = arrayOf("Read")) { (player) -> player.openBook(BigBookOfBangs()) }
    onItemClick(Obj.regicide_iorwerth_message, options = arrayOf("Read")) { (player) ->
        player.sendMessage("You try to open the message...")
        player.sendMessage("The seal on the scroll is unbreakable.")
    }
}

private val MESSAGE_LINES = intArrayOf(
    Component.interface_174.component_174_1, Component.interface_174.component_174_2,
    Component.interface_174.component_174_3, Component.interface_174.component_174_4,
    Component.interface_174.component_174_5, Component.interface_174.component_174_6,
    Component.interface_174.component_174_7, Component.interface_174.component_174_8,
    Component.interface_174.component_174_9, Component.interface_174.component_174_10,
    Component.interface_174.component_174_11, Component.interface_174.component_174_12,
    Component.interface_174.component_174_13, Component.interface_174.component_174_14,
    Component.interface_174.component_174_15, Component.interface_174.component_174_16
)

internal fun Player.showIorwerthMessage() {
    val text = arrayOf(
        "Your Majesty King Lathas",
        "",
        "Your agent did well. The path is now open for the Dark",
        "Lord to enter this realm. You will yet live to see Camelot",
        "crushed under foot.",
        "",
        "Warmaster Iorwerth"
    )
    interfaceManager.sendInterface(Interface.interface_174)
    MESSAGE_LINES.forEachIndexed { index, component -> packets.setIFText(component, text.getOrElse(index) { "" }) }
}
