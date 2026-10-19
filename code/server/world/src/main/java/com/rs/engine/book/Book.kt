package com.rs.engine.book

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.Utils
import org.darkan.core.cache.IComponentDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Fontmetrics
import org.darkan.core.gameval.gen.Interface

private const val CURRENT_BOOK = "currBook"

private val LEFT_COMPONENTS = intArrayOf(
    Component.interface_960.component_960_49,
    Component.interface_960.component_960_56,
    Component.interface_960.component_960_61,
    Component.interface_960.component_960_62,
    Component.interface_960.component_960_54,
    Component.interface_960.component_960_63,
    Component.interface_960.component_960_55,
    Component.interface_960.component_960_51,
    Component.interface_960.component_960_60,
    Component.interface_960.component_960_58,
    Component.interface_960.component_960_53,
    Component.interface_960.component_960_50,
    Component.interface_960.component_960_57,
    Component.interface_960.component_960_59,
    Component.interface_960.component_960_52
)

private val RIGHT_COMPONENTS = intArrayOf(
    Component.interface_960.component_960_33,
    Component.interface_960.component_960_39,
    Component.interface_960.component_960_36,
    Component.interface_960.component_960_44,
    Component.interface_960.component_960_37,
    Component.interface_960.component_960_46,
    Component.interface_960.component_960_40,
    Component.interface_960.component_960_42,
    Component.interface_960.component_960_34,
    Component.interface_960.component_960_35,
    Component.interface_960.component_960_38,
    Component.interface_960.component_960_43,
    Component.interface_960.component_960_47,
    Component.interface_960.component_960_45,
    Component.interface_960.component_960_41
)

abstract class Book(private val title: String, private vararg val pages: BookPage) {
    private var player: Player? = null
    private var page = 0

    constructor(title: String, text: String) : this(title, *buildPages(text))

    fun open(player: Player) {
        player.endConversation()
        this.player = player
        player.tempAttribs.setO<Book>(CURRENT_BOOK, this)
        player.setCloseInterfacesEvent { player.tempAttribs.removeO<Book>(CURRENT_BOOK) }
        page = 0
        update()
    }

    fun update() {
        val player = player ?: return
        player.interfaceManager.sendInterface(Interface.interface_960)
        player.packets.setIFText(Component.interface_960.component_960_69, title)
        player.packets.setIFHidden(Interface.interface_960, Utils.componentIdFromHash(Component.interface_960.component_960_72), page == 0)
        player.packets.setIFText(Component.interface_960.component_960_70, if (page == 0) "" else "Prev")
        player.packets.setIFHidden(Interface.interface_960, Utils.componentIdFromHash(Component.interface_960.component_960_73), page >= pages.lastIndex)
        player.packets.setIFText(Component.interface_960.component_960_71, if (page >= pages.lastIndex) "" else "Next")
        for (line in LEFT_COMPONENTS.indices) {
            player.packets.setIFText(LEFT_COMPONENTS[line], pages[page].getLeftLine(line) ?: "")
            player.packets.setIFText(RIGHT_COMPONENTS[line], pages[page].getRightLine(line) ?: "")
        }
    }

    fun nextPage() {
        if (page >= pages.lastIndex) return
        page++
        update()
    }

    fun prevPage() {
        if (page <= 0) return
        page--
        update()
    }
}

private fun buildPages(text: String): Array<BookPage> {
    val formatted = text.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "<br>")
    val width = requireNotNull(IComponentDefinitions.getInterfaceComponent(Interface.interface_960, Utils.componentIdFromHash(RIGHT_COMPONENTS.first()))).baseWidth
    val lines = Utils.splitTextIntoLines(formatted, Fontmetrics.q8_full, width).ifEmpty { arrayOf("") }
    return lines.toList().chunked(LEFT_COMPONENTS.size + RIGHT_COMPONENTS.size).map {
        BookPage(it.take(LEFT_COMPONENTS.size).toTypedArray(), it.drop(LEFT_COMPONENTS.size).toTypedArray())
    }.toTypedArray()
}

@ServerStartupEvent
fun mapBookNavigation() {
    onButtonClick(Interface.interface_960) { e ->
        val book = e.player.tempAttribs.getO<Book>(CURRENT_BOOK) ?: return@onButtonClick
        when (e.component) {
            Component.interface_960.component_960_72 -> book.prevPage()
            Component.interface_960.component_960_73 -> book.nextPage()
        }
    }
}
