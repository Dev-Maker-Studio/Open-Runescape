package com.rs.game.content.quests.observatoryquest

import com.rs.engine.book.Book
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile

class AstronomyBook : Book(
    "The Tale of Scorpius",
    """
        A History of Astronomy in Gielinor.

        At the start of the Fourth Age, a learned man by the name of Scorpius, known well for his powers of vision and magic, sought communion with the gods of the world. So many unanswered questions had he that he devoted his entire life to this cause.

        After many years of study, seeking knowledge from the wise of that time, he developed a machine infused with magical power, infused with the ability to pierce into the very heavens - a huge eye that gave the user incredible sight, like never before.

        As time passed, Scorpius grew adept at using his specialised skills, and followed the movements of the stars in Gielinor, which he mapped and named, and are still used to this very day.

        Before long, Scorpius used his knowledge for predicting the future, and, in turn, he called upon the dark knowledge of Zamorakian worshippers to further his cause. Living below ground, the followers of the dark god remained until the local tribes and settlements grew in strength and control.

        The chieftains of that time worked to banish the Zamorakian followers in the area, hiding all reference to Scorpius's invention, due to its 'evil nature'.

        Years after, when the minds of the kings lent more towards the research of the unknown, the plans of Scorpius were uncovered and the heavenly eye constructed again.

        Since then, many have studied the ways of the astronomer, Scorpius, and in his memory a grave was constructed near the Observatory.

        Some claim his ghost still wanders nearby, in torment as he seeks the secrets of the heavens that can never be solved. Tales tell that he will grant those adept in the arts of the astronomer a blessing of unusual power.

        Here ends the tale of how astronomy entered the known world.
    """.trimIndent()
)

@ServerStartupEvent
fun mapObservatoryAstronomyBook() {
    onObjectClick(Loc.canafis_table_small, tiles = arrayOf(Tile.of(2438, 3187, 0))) { (player) ->
        if (player.inventory.containsItem(Obj.book_of_astrology, 1)) {
            player.sendMessage("You already have a copy of the Astronomy book.")
            return@onObjectClick
        }
        if (!player.inventory.hasFreeSlots()) {
            player.sendMessage("You don't have enough inventory space.")
            return@onObjectClick
        }
        player.inventory.addItem(Obj.book_of_astrology, 1)
        player.sendMessage("You take the Astronomy book from the table.")
    }

    onItemClick(Obj.book_of_astrology, options = arrayOf("Read")) { (player) ->
        player.openBook(AstronomyBook())
    }
}
