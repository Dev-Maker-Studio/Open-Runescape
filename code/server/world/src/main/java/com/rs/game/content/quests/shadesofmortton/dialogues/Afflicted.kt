package com.rs.game.content.quests.shadesofmortton

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.DIZZY
import com.rs.engine.dialogue.startConversation
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.Utils

private val afflictedWords = listOf(
    "pirsl", "zurgle", "wot's", "nots", "says", "seysyl", "ughugh",
    "gurgle", "gurghl", "mee's", "seysyi", "sfriess", "knows'is"
)

@ServerStartupEvent
fun mapAfflictedDialogue() {
    onNpcClick(*afflictedLocals.keys.map { it as Any }.toTypedArray(), options = arrayOf("Talk-to")) { (player, npc) ->
        val gibberish = List(Utils.randomInclusive(2, 6)) {
            afflictedWords[Utils.random(afflictedWords.size)]
        }.joinToString(" ")
        player.startConversation {
            npc(npc, DIZZY, gibberish)
            simple("~ This person doesn't make any sense at all. ~")
        }
    }
    onNpcClick(*afflictedLocals.values.map { it as Any }.toTypedArray(), options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            npc(npc, CALM_TALK, "Oh, I feel much better for that super serum. I really hate it when I'm afflicted!")
        }
    }
}
