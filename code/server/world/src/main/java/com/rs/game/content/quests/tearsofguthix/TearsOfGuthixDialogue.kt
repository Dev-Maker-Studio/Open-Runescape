package com.rs.game.content.quests.tearsofguthix

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.CONFUSED
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private val INTRO = listOf(
    "I have been waiting here three thousand years, guarding the Tears of Guthix. I serve my master faithfully, but I am bored.",
    "An adventurer such as yourself must have many tales to tell. If you can entertain me, I will let you into the cave for a time.",
    "The more I enjoy your story, the more time I will give you in the cave.",
    "Then you can drink of the power of balance, which will make you stronger in whatever area you are weakest."
)

private val TEARS_STORY = listOf(
    "The Third Age of the world was a time of great conflict, of destruction never seen before or since, when all the gods save Guthix warred for control.",
    "The colossal wyrms, of whom today's dragons are a pale reflection, turned all the sky to fire, while on the ground armies of foot soldiers - goblins and trolls and humans - filled the valleys and plains with blood.",
    "In time the noise of the conflict woke Guthix from His deep slumber, and He rose and stood in the centre of the battlefield so that the splendour of His wrath filled the world, and He called for the conflict to cease!",
    "Silence fell, for the gods knew that none could challenge the power of the mighty Guthix; for His power is that of nature itself, to which all other things are subject, in the end.",
    "Guthix reclaimed that which had been stolen from Him, and went back underground to return to His sleep and continue to draw the world's power into Himself.",
    "But on His way into the depths of the earth He sat and rested in this cave; and, thinking of the battle-scarred desert that now stretched from one side of His world to the other, He wept.",
    "And so great was His sorrow, and so great was His life- giving power, that the rocks themselves began to weep with Him.",
    "Later, Guthix noticed that the rocks continued to weep, and that their tears were infused with a small part of His power.",
    "So He set me, His servant, to guard the cave, and He entrusted to me the task of judging who was and was not worthy to access the tears."
)

private val LIGHT_STORY = listOf(
    "I will tell you the story of the light creatures.",
    "Myriad and beautiful were the creatures and civilizations of the early ages of the world. Gielinor was a work of art, shaped lovingly over the millennia by the creative mind of Guthix.",
    "Only the sturdiest races survived the God Wars, and even then only by abandoning their high culture and gearing their societies towards war. Of the more delicate races there is now no trace, and almost no memory.",
    "One such race had bodies as fragile as snowflakes, yet they built crystal cities that stood for a thousand years.",
    "The wind would whisper through the spires and fill them with sweet harmonies, and the rising sun would shine through the precious gems that studded the towers and create inter plays of light as if rainbows were dancing.",
    "Indeed, so marvellous was this light-show at its height that the patterns of light themselves became alive, and great flocks of luminous creatures rode along the gem- cast beams, each drawn to its own colour.",
    "The creatures you see floating in this chasm are the last sorry remnants of that age. I do not know how they made their way here and survived to this time, but I am grateful for their company."
)

@ServerStartupEvent
fun mapTearsOfGuthixDialogue() {
    onObjectClick(Loc.loc_6657, Loc.loc_31302, Loc.loc_31303) { (player) -> player.talkJuna() }
    onItemOnObject(arrayOf<Any>(Loc.loc_6657, Loc.loc_31302, Loc.loc_31303), arrayOf<Any>(Obj.tog_bowl)) { (player) -> player.talkJuna() }
    onNpcClick(Npc.tog_juna_dummy) { (player) -> player.talkJuna() }
}

private fun DialogueBuilder.juna(text: String) = npc(Npc.tog_juna_dummy, CALM_TALK, text)

private fun Player.talkJuna() {
    val owner = this
    if (tearsStage in MAKE_BOWL..MADE_BOWL) {
        startConversation {
            juna("Before you can collect the Tears of Guthix you must make a bowl out of the stone in the cave on the south of the chasm.")
            if (inventory.containsItem(Obj.tog_bowl, 1)) {
                player(CALM_TALK, "I have a bowl.")
                juna("I will keep your bowl for you, so that you may collect the tears many times in the future.")
                juna("I suggest you keep the lantern you used to attract the light creatures. I foresee a time when you will need it again.")
                juna("Now, tell me another story, and I will let you collect the tears for the first time.")
                exec {
                    if (owner.tearsStage in MAKE_BOWL..MADE_BOWL && owner.inventory.containsItem(Obj.tog_bowl, 1)) {
                        owner.inventory.deleteItem(Obj.tog_bowl, 1)
                        owner.questManager.completeQuest(TOG)
                    }
                }
            } else options {
                op("But I don't know how to reach the cave!") {
                    player(CALM_TALK, "But I don't know how to reach the cave!")
                    LIGHT_STORY.forEach { juna(it) }
                }
                op("What are the Tears of Guthix?") {
                    player(CALM_TALK, "What are the Tears of Guthix?")
                    TEARS_STORY.forEach { juna(it) }
                }
                op("Okay.") { player(CALM_TALK, "Okay.") }
            }
        }
        return
    }
    startConversation {
        juna("Tell me... a story...")
        if (owner.tearsStage == UNSTARTED && !owner.tears.getB("introduced")) {
            player(CONFUSED, "A story?")
            INTRO.forEachIndexed { index, text ->
                npc(Npc.tog_juna_dummy, CALM_TALK, text, if (index == INTRO.lastIndex) Runnable { owner.tears.setB("introduced", true) } else null)
            }
        }
        options {
            op("Okay...") { player(CALM_TALK, "Okay..."); exec { owner.offerJunaStory() } }
            op("A story?") { player(CONFUSED, "A story?"); INTRO.forEach { juna(it) } }
            op("Not now.") { player(CALM_TALK, "Not now.") }
            op("You tell me a story.") {
                player(CALM_TALK, "You tell me a story.")
                options {
                    op("Tell me about the Tears of Guthix.") { player(CALM_TALK, "Tell me about the Tears of Guthix."); TEARS_STORY.forEach { juna(it) } }
                    op("Tell me a new story.") { player(CALM_TALK, "Tell me a new story."); juna("I have already told you my story, and you have not told me yours!") }
                }
            }
        }
    }
}

private fun Player.offerJunaStory() {
    if (!handsFreeForTears()) return
    if (tearsStage == UNSTARTED && !canStartTears()) {
        simpleDialogue("You need 44 quest points, 20 Crafting, 20 Mining and 49 Firemaking to start Tears of Guthix.")
        return
    }
    if (tearsStage == COMPLETE) tearsAvailability()?.let { simpleDialogue(it); return }
    val owner = this
    startConversation {
        simple("You tell Juna some stories of your adventures.")
        juna("Your stories have entertained me. I will let you into the cave for a short time.")
        if (owner.tearsStage == UNSTARTED) questStart(TOG, accepted = {
            juna("But first you will need to make a bowl in which to collect the tears.")
            juna("There is a cave on the south side of the chasm that is similarly infused with the power of Guthix. The stone in that cave is the only substance that can catch the Tears of Guthix.")
            juna("Mine some stone from that cave, make it into a bowl, and bring it to me, and then I will let you catch the tears.")
            exec { if (owner.tearsStage == UNSTARTED && owner.canStartTears() && owner.handsFreeForTears()) owner.setQuestStage(TOG, MAKE_BOWL) }
        }, declined = {})
        else exec { owner.enterTears() }
    }
}
