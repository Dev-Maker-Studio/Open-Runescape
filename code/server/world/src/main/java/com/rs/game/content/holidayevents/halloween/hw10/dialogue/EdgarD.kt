package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Spider
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.SpiderPosition
import com.rs.game.content.holidayevents.halloween.hw10.hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.inPartyHouse
import com.rs.game.content.holidayevents.halloween.hw10.position
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc

private const val EDGAR = Npc.hw10_edgar
private val EDGAR_ALONE = listOf("*sigh*", "...Nevermore.")

@ServerStartupEvent
fun mapEdgar() {
    onNpcClick(EDGAR, options = arrayOf("Talk-to")) { (player) ->
        player.startConversation {
            if (!player.inPartyHouse && player.hw10Stage < Hw10Stage.PARTY && player.position(Hw10Spider.TURQUOISE) == SpiderPosition.HIDING)
                talkToTurquoiseSpider(player)
            else
                npc(EDGAR, HeadE.SAD, EDGAR_ALONE.random())
        }
    }
}
