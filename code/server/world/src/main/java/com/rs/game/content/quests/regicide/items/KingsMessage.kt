package com.rs.game.content.quests.regicide.items

import com.rs.engine.book.Book
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapRegicideKingsMessage() {
    onItemClick(Obj.regicide_quest_kings_summons, options = arrayOf("Read")) { (player) ->
        player.openBook(object : Book("King's message", """
            Squire ${player.displayName},

            You are needed to serve the Kingdom of Kandarin. At last our magi have reopened the well of voyage. We must discuss your next commission.

            His Majesty King Lathas
        """.trimIndent()) {})
    }
}
