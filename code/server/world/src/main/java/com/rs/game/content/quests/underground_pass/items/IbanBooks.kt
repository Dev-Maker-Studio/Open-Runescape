package com.rs.game.content.quests.underground_pass.items

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.book.Book
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick

@ServerStartupEvent
fun mapIbanBooks() {
    onItemClick(Obj.upass_journal, Obj.obj_1494, options = arrayOf("Read")) { (player, item) ->
        when (item.id) {
            Obj.upass_journal -> {
                player.vars.setVarBit(Varbit.upass_read_journal, 1)
                player.openBook(JournalOfRandas())
            }
            Obj.obj_1494 -> {
                player.vars.setVarBit(Varbit.upass_read_book, 1)
                player.openBook(HistoryOfIban())
            }
        }
    }
}

private class JournalOfRandas : Book(
    "The Journal of Randas",
    """
        I came to cleanse these mountain passes of the dark forces that dwell here. I knew my journey would be treacherous, so I deposited Spheres of Light in some of the tunnels. These spheres are a beacon of safety for all who come. The spheres were created by Saradominist mages. When held they boost our faith and courage. I still feel... Iban relentlessly tugging... at my weak soul......

        bringing out any innate goodness to one's heart, illuminating the dark caverns with the light of Saradomin, bringing fear and pain to all who embrace the dark side. My men are still repelled by 'Iban's will' - it seems as if their pure hearts bar them from entering Iban's realm. My turn has come. I dare not admit it to my loyal men, but I fear for the welfare of my soul.
    """.trimIndent()
)

private class HistoryOfIban : Book(
    "History of Iban",
    """
        <u>Introduction</u>

        Gather round, all ye followers of the dark arts. Read carefully the words that I hereby inscribe, as I detail the heady brew that is responsible for my greatest creation in all my time on this world.

        I am Kardia, the most wretched witch in the land; scorned by beauty, the world and its inhabitants, see what I have created: The most fearsome and powerful force of darkness the like of which has never before been seen in this world, in human form...

        <u>The History of Iban</u>

        Iban was a Black Knight who had learned to fight under the great Daquarius, Lord of the Black Knights. Together they had taken on the might of the White and the blood of a hundred soldiers had been wiped from the sword of Iban.

        Iban was not so different from those who tasted his blade: noble and educated, with a taste for the finer of things available in life. But there was something that made him different: Ambition. This hunger for more went far past the realm of mere mortals, into the shadowy places of darkness and evil.

        Iban's ambition was almost godlike in its insatiability, but therein lay the essence of his darkness: as its most base Iban's fundamental desire was to control the hearts and minds of his fellow men, to take them beyond the pale of mere allegiance, and corrupt them into a force of evil.

        A whole legion of these Soul-less beings, their minds demented from the sheer power of darkness that chanelled through them... Zombies, void of emotions, without feelings or cares, servants to their wicked master even unto death... But dreams were all they ever were. As a mere mortal, heroic though he was, this ambition Iban was unable to achieve.

        Meeting his demise in the White Knight's now famous Dawn Ascent, Iban died with the bitter taste of failure in his mouth. Little did he know that death was only just the beginning...

        <u>Iban's Resurrection</u>

        I knew of Iban's life, though of course I had not met him. Using the power of my dark practices, I vowed to resurrect this greatest of warriors. I would raise him again to fulfil the promise of his human life. To be a master... of the undead...

        <u>The Four Elements</u>

        <u>Flesh</u>

        Taking a small doll with the likeness of Iban I smeared my effigy with the four elements that together bring existence into being. Essence of his darkness.

        At the battlefield where Iban lay, I had been able to steal a piece of Iban's cold flesh. Clasping some in my hand, I smeared it over the figure of Iban, and chanted his name with mighty incantation.

        <u>Blood</u>

        I also needed blood, the giver of life force. By now Iban's body was a hardened vessel, the blood drained empty. But those caverns are home to the giant spider, a venomous creature known to feed on the warm blood of humans.

        I found and killed one of these foul beasts, and wiped the blood from its vile body onto the effigy of Iban that I had fashioned.

        <u>Shadow</u>

        Then came the hardest part, recreating the parts of a man that cannot be seen or touched: Those intangible things, the essence of life itself. Using mystical forces and under terrible strain, I performed the ancient ritual of Incantia, an undertaking so dark and so powerful, that the life was nearly stolen from my body.

        When I recovered, I saw three Demons summoned, standing in a triangle, their energy focused on the doll of Iban. These Demons were the keepers of Iban's shadow, forever bound to him...

        <u>Conscience</u>

        Finally, I had to make the most unique thing, the one element that separates man from all other beasts - his Conscience. A zombie has no mind: a creature borne of bloodlust, destruction. But for all Iban's life he chose to take the path of darkness, the road of evil. Driven by this unholy ambition, his potential grew and now I could harness the residue of his existence, that remained trapped in the dark places, to the fullest.

        Locked inside an old wooden cage sat a beautiful white dove. A symbol of peace, freedom, and hope, but also the oblivious to the darkness of the world, like a newborn child. Taking the dove with me, I cradled the thing in my arms, stroking its soft downy feathers.

        I looked into the eyes of the bird, and gently placing a kiss upon its fragile head, I then strangled the bird, taking its life between my callous fingers. Truly this bird would be the conscience of Iban: innocence corrupted by evil...

        Taking crushed bones from the dove's body, I cast my mind's eye onto the body of Iban.

        My ritual was complete, soon he would come again renewed with life. I, Kardia, had done the unimaginable: Iban was resurrected, the most powerful evil to take human form.

        I alone knew that the same process I had used to resurrect the soul of Iban could be used to destroy that very same evil. But now I was tired, as I closed my eyes, I was contented by the thought of the evil to be unleashed...
    """.trimIndent()
)
