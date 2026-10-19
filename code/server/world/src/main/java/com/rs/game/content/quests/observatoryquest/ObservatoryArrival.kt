package com.rs.game.content.quests.observatoryquest

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private const val CONSTELLATION_XP = 875.0

private val RECEPTION_PROFESSOR_TILE = Tile.of(2439, 3186, 0)
private val OBSERVATORY_PROFESSOR_TILE = Tile.of(2438, 3160, 0)
private val UPSTAIRS_PROFESSOR_TILE = Tile.of(2437, 3160, 1)

private val CONSTELLATION_PAGES = listOf(
    listOf(CONSTELLATION_AQUARIUS, CONSTELLATION_CAPRICORN, CONSTELLATION_SCORPIO),
    listOf(CONSTELLATION_LIBRA, CONSTELLATION_VIRGO, CONSTELLATION_LEO),
    listOf(CONSTELLATION_CANCER, CONSTELLATION_GEMINI, CONSTELLATION_TAURUS),
    listOf(CONSTELLATION_ARIES, CONSTELLATION_PISCES, CONSTELLATION_SAGITTARIUS)
)

@ServerStartupEvent
fun mapObservatoryArrival() {
    onLogin { (player) -> syncObservatoryProfessor(player) }
}

fun talkToObservatoryDomeProfessor(player: Player, npc: NPC): Boolean {
    if (npc !is OwnedNPC || npc.owner != player || !(npc.tile.within(OBSERVATORY_PROFESSOR_TILE, 8) || npc.tile.within(UPSTAIRS_PROFESSOR_TILE, 8))) return false
    when (player.getQuestStage(Quest.OBSERVATORY_QUEST)) {
        STAGE_TRAVELLING_TO_OBSERVATORY -> repairTelescope(player, npc)
        STAGE_OBSERVING_CONSTELLATION -> observingConstellationDialogue(player)
        STAGE_COMPLETE -> player.startConversation {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Hello again.")
            if (player.getBool(OBSERVATORY_VIEWED_SCORPIO_ATTR))
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Scorpio is the constellation of the astronomer Scorpius. My assistant can tell you more.")
        }
    }
    return true
}

fun syncObservatoryProfessor(player: Player) {
    val tile = when (player.getQuestStage(Quest.OBSERVATORY_QUEST)) {
        in STAGE_UNSTARTED until STAGE_TRAVELLING_TO_OBSERVATORY -> RECEPTION_PROFESSOR_TILE
        STAGE_TRAVELLING_TO_OBSERVATORY -> OBSERVATORY_PROFESSOR_TILE
        else -> UPSTAIRS_PROFESSOR_TILE
    }
    val professors = World.NPCs.filterIsInstance<OwnedNPC>()
        .filter { it.id == Npc.observatory_professor && it.owner == player }
    professors.drop(1).forEach { it.finish() }
    val professor = professors.firstOrNull() ?: OwnedNPC(player, Npc.observatory_professor, tile, true)
    professor.setAutoDespawnAtDistance(false)
    professor.setRandomWalk(false)
    if (!professor.tile.matches(tile)) professor.tele(tile)
}

private fun repairTelescope(player: Player, professor: OwnedNPC) {
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hi, professor!") { professor.anim(Seq.emote_wave) }
        npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Oh, hi there!")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "I'm just adding the finishing touches.")
        player(HeadE.CALM_TALK, "Okay, don't let me interrupt!")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Thank you.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Right, let's get this finished.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Hmmmm...") { professor.anim(Seq.professor_fixing_scope_scratch_head) }
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Bit of a tap here...") { professor.anim(Seq.professor_fixing_scope_hammer_time) }
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "${player.displayName}, I'm just going upstairs to finish off.")
        player(HeadE.CALM_TALK, "Right-oh.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "In goes the lens.") { professor.anim(Seq.professor_fitting_lens) }
        npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Looking good.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "And one final adjustment.") { professor.anim(Seq.professor_fixing_scope_screw_driver) }
        npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "And all our work pays off.") {
            if (player.getQuestStage(Quest.OBSERVATORY_QUEST) == STAGE_TRAVELLING_TO_OBSERVATORY) {
                professor.anim(Seq.emote_cheer)
                player.setQuestStage(Quest.OBSERVATORY_QUEST, STAGE_OBSERVING_CONSTELLATION)
                syncObservatoryProfessor(player)
            }
        }
    }
}

private fun observingConstellationDialogue(player: Player) {
    player.startConversation {
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "What would you like to talk about?")
        options {
            op("Talk about the Observatory Quest.") {
                npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Hello, friend.")
                if (!hasViewedObservatoryConstellation(player)) {
                    player(HeadE.HAPPY_TALKING, "Hi, this really is impressive.")
                    npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Certainly is. Please, take a look through the telescope and tell me what you see.")
                    return@op
                }
                player(HeadE.HAPPY_TALKING, "I've had a look through the telescope.")
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "What did you see? If you're not sure, you can find out by looking at the star charts dotted around the walls downstairs.")
                player(HeadE.CALM_TALK, "It was...")
                constellationPage(player, 0)
            }
            op("Talk about Treasure Trails.") {
                exec { player.sendMessage("You decide to talk about that another time.") }
            }
        }
    }
}

private fun DialogueBuilder.constellationPage(player: Player, page: Int) {
    label("constellationPage$page")
    options {
        CONSTELLATION_PAGES[page].forEach { constellation ->
            opExec(getObservatoryConstellationName(constellation)) { checkConstellationAnswer(player, constellation) }
        }
        op("More...") {
            if (page + 1 < CONSTELLATION_PAGES.size) constellationPage(player, page + 1) else goto("constellationPage0")
        }
    }
}

private fun checkConstellationAnswer(player: Player, answer: Int) {
    if (player.getQuestStage(Quest.OBSERVATORY_QUEST) != STAGE_OBSERVING_CONSTELLATION || !hasViewedObservatoryConstellation(player)) return
    val actual = getObservatoryConstellation(player)
    if (actual !in CONSTELLATION_AQUARIUS..CONSTELLATION_VIRGO) return
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "${getObservatoryConstellationName(answer)}!")
        if (answer != actual) {
            npc(Npc.observatory_professor, HeadE.CALM_TALK, "I'm afraid not. Have another look. Remember, you can check the star charts on the walls for reference.")
            return@startConversation
        }
        npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's exactly it!")
        player(HeadE.HAPPY_TALKING, "Yes! Woo hoo!") { player.anim(Seq.emote_cheer) }
        observatoryConstellationSuccess(player, actual)
    }
}

private fun DialogueBuilder.observatoryConstellationSuccess(player: Player, constellation: Int) {
    when (constellation) {
        CONSTELLATION_AQUARIUS -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Aquarius, the water-bearer.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "It seems suitable, then, to award you with water runes!")
        }
        CONSTELLATION_ARIES -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Aries, the ram.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "A fierce fighter. I'm sure he'll look down on you and improve your Attack for such insight.")
        }
        CONSTELLATION_CANCER -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Cancer, the crab.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "An armoured creature - I think I shall reward you with an amulet of protection.")
        }
        CONSTELLATION_CAPRICORN -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Capricorn, the goat.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Capricorn will surely reward your insight with an increase to your Strength.")
        }
        CONSTELLATION_GEMINI -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Gemini, the twins.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "With the double nature of Gemini, I can't offer you anything more suitable than a two-handed weapon.")
        }
        CONSTELLATION_LEO -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Leo, the lion.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "I think the majestic power of the lion will improve your Constitution.")
        }
        CONSTELLATION_LIBRA -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Libra, the scales.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Hmmm, balance, law, order - I shall award you with law runes!")
        }
        CONSTELLATION_PISCES -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Pisces, the fish.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "What's more suitable as a reward than some tuna?")
        }
        CONSTELLATION_SAGITTARIUS -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Sagittarius, the centaur.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "As you've spotted the archer, I shall reward you with a maple longbow.")
        }
        CONSTELLATION_SCORPIO -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Scorpio, the scorpion.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "I think weapon poison would make a suitable reward.")
        }
        CONSTELLATION_TAURUS -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Taurus, the bull.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "This Strength potion should be a suitable reward.")
        }
        CONSTELLATION_VIRGO -> {
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "That's Virgo, the virtuous.")
            npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Virgo will surely provide you with an increase to Defence.")
        }
    }
    npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "By Saradomin's earlobes! You must be a friend of the gods indeed.")
    npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Look in your backpack for your reward, in payment for your work.")
    npc(Npc.observatory_professor, HeadE.CALM_TALK, "I'll unlock the doors to make it easier for you to get back.")
    player(HeadE.CALM_TALK, "And I suppose I'll just jump down?")
    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Oh right! I'll just have my assistant tie a rope off or something...")
    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Perhaps in the future you can help me fix the bridge, but there's too much preparation I'll have to do before then.") {
        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) == STAGE_OBSERVING_CONSTELLATION && hasViewedObservatoryConstellation(player) && getObservatoryConstellation(player) == constellation) {
            player.questManager.completeQuest(Quest.OBSERVATORY_QUEST)
            syncObservatoryProfessor(player)
            syncObservatoryRope(player)
        }
    }
}

internal fun giveObservatoryConstellationReward(player: Player, constellation: Int) {
    player.inventory.addItemDrop(Obj.uncut_sapphire, 1)
    when (constellation) {
        CONSTELLATION_AQUARIUS -> player.inventory.addItemDrop(Obj.waterrune, 25)
        CONSTELLATION_ARIES -> player.skills.addXpQuest(Skills.ATTACK, CONSTELLATION_XP)
        CONSTELLATION_CANCER -> player.inventory.addItemDrop(Obj.amulet_of_defence, 1)
        CONSTELLATION_CAPRICORN -> player.skills.addXpQuest(Skills.STRENGTH, CONSTELLATION_XP)
        CONSTELLATION_GEMINI -> player.inventory.addItemDrop(Obj.black_2h_sword, 1)
        CONSTELLATION_LEO -> player.skills.addXpQuest(Skills.HITPOINTS, CONSTELLATION_XP)
        CONSTELLATION_LIBRA -> player.inventory.addItemDrop(Obj.lawrune, 3)
        CONSTELLATION_PISCES -> player.inventory.addItemDrop(Obj.tuna, 3)
        CONSTELLATION_SAGITTARIUS -> player.inventory.addItemDrop(Obj.maple_longbow, 1)
        CONSTELLATION_SCORPIO -> player.inventory.addItemDrop(Obj.obj_187, 1)
        CONSTELLATION_TAURUS -> player.inventory.addItemDrop(Obj._1dose2strength, 1)
        CONSTELLATION_VIRGO -> player.skills.addXpQuest(Skills.DEFENSE, CONSTELLATION_XP)
    }
}
