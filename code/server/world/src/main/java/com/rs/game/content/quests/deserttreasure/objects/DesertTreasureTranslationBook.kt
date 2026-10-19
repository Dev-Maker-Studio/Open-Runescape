package com.rs.game.content.quests.deserttreasure

import com.rs.engine.book.Book
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.gameval.gen.Obj

class DesertTreasureTranslationBook : Book(
    "Translation",
    "This is a rough translation of the stone tablet brought to me by courier earlier today. " +
        "The cuneiforms of this particular tablet are far different to anything I have previously seen in my career as an archaeological expert. " +
        "Where possible I have given as accurate a translation as possible, but some of the words I have attempted to translate hold different meanings depending upon their intonation and context. " +
        "Due to my unfamiliarity with this language, I have given possible translations for these words wherever I have encountered them. " +
        "Wherever I have a word in brackets, it is a word which has many meanings depending upon the context, although the general meaning should be clear to even a casual study. " +
        "Hopefully this translation will help you in your excavations Asgarnia, and as usual I look forward to seeing what relics you bring back to the Museum of Varrock this time! " +
        "Your friend, as always,<br>Terry Balando<br><br>Translation follows:<br>" +
        "...the permanent (exile/journey) of the people ended. " +
        "And so it came to pass, that deep in the (fiery/uncomfortable) desert, the gods (argued/decided) amongst themselves that the (fortress/home) would be the (selected/chosen) place that would (imprison/conceal) the (wealth/power). " +
        "Thus (guarded/protected) by the (unusually archaic word here, I believe it means either the sick or the dead depending on the context) and (defended/trapped) by the (diamonds/crystals) of (this word is untranslatable). " +
        "So it was that the gods left the four (diamonds/crystals) as the (key/secret). " +
        "(Guarded/Protected) by the (again, this word has no modern equivalent) and held by the (worthy/strong) so that the (wealth/power) might forever be (imprisoned/concealed). " +
        "There seems to be some further missing information continued onto a further tablet, but from this preliminary translation I think you may be onto something very big indeed!"
)

@ServerStartupEvent
fun mapDesertTreasureTranslationBook() {
    onItemClick(Obj.four_diamonds_translation_primer, options = arrayOf("Read")) { (player) ->
        player.openBook(DesertTreasureTranslationBook())
    }
}
