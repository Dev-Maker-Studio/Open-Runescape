package com.rs.game.content.quests.watchtower

import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.Constants

const val STAGE_UNSTARTED = 0
const val STAGE_SEARCH_WATCHTOWER = 1
const val STAGE_FIND_WAY_INTO_GUTANOTH = 2
const val STAGE_COLLECT_OGRE_RELIC = 3
const val STAGE_ENTER_GUTANOTH = 4
const val STAGE_GET_SKAVID_MAP = 5
const val STAGE_LEARN_SKAVID_LANGUAGE = 6
const val STAGE_SEARCH_SKAVID_CAVES = 7
const val STAGE_FIND_SHAMAN_ENCLAVE = 8
const val STAGE_DISCOVER_SHAMANS = 9
const val STAGE_MAKE_OGRE_POTION = 10
const val STAGE_DEFEAT_SHAMANS = 11
const val STAGE_RECOVER_CRYSTALS = 12
const val STAGE_RESTORE_WATCHTOWER = 13
const val STAGE_ACTIVATE_WATCHTOWER = 14
const val STAGE_COMPLETE = 15

const val REQUIRED_AGILITY_LEVEL = 25
const val REQUIRED_HERBLORE_LEVEL = 14
const val REQUIRED_MAGIC_LEVEL = 14
const val REQUIRED_MINING_LEVEL = 40
const val REQUIRED_THIEVING_LEVEL = 15

const val WATCHTOWER_SCROLL_READ = "watchtowerSpellScrollRead"
const val MAD_SKAVID_COMPLETE = "watchtowerMadSkavidComplete"

fun recoverWatchtowerSkavidProgress(player: Player) {
    if (player.getQuestStage(Quest.WATCHTOWER) == STAGE_SEARCH_SKAVID_CAVES &&
        player.questManager.getAttribs(Quest.WATCHTOWER).getB(MAD_SKAVID_COMPLETE)) {
        player.setQuestStage(Quest.WATCHTOWER, STAGE_FIND_SHAMAN_ENCLAVE)
    }
}

@ServerStartupEvent
fun mapWatchtowerItems() {
    onItemClick(
        Obj.shaman_robe,
        options = arrayOf("Search")
    ) { e ->
        val player = e.player

        if (player.getQuestStage(Quest.WATCHTOWER) < STAGE_RECOVER_CRYSTALS ||
            player.isQuestComplete(Quest.WATCHTOWER)
        ) {
            player.sendMessage("You search the robe but find nothing.")
            return@onItemClick
        }

        if (player.inventory.containsItem(Obj.powering_crystal3, 1) ||
            player.bank.containsItem(Obj.powering_crystal3, 1) ||
            player.vars.getVarBit(Varbit.watchtower_pillar_1) != 0
        ) {
            player.sendMessage("You search the robe but find nothing.")
            return@onItemClick
        }

        player.inventory.addItemDrop(Obj.powering_crystal3, 1)
        player.sendMessage("You search the robe and find a crystal.")
    }

    onItemClick(
        Obj.watchtowerspell,
        options = arrayOf("Read")
    ) { e ->
        val player = e.player

        if (!player.isQuestComplete(Quest.WATCHTOWER)) return@onItemClick
        player.sendMessage("You memorise what is written on the scroll.")

        player.inventory.deleteItem(
            Obj.watchtowerspell,
            1
        )

        player.set(
            WATCHTOWER_SCROLL_READ,
            true
        )

        player.sendMessage(
            "You can now cast the Watchtower teleport spell... ...Provided you have the required runes and magic level."
        )

        player.sendMessage(
            "The scroll crumbles to dust."
        )
    }
}

@QuestHandler(
    quest = Quest.WATCHTOWER,
    startText = "Speak to the Watchtower Wizard in the Watchtower, north-west of Yanille.",
    itemsText =
        "20 coins, dragon bones, ground bat bones, an unfinished guam potion, " +
        "2 ropes, armoured gloves, a gold bar, a death rune, a pickaxe and a light source. " +
        "Other items can be obtained during the quest.",
    combatText = "You will need to defeat Gorad and survive several aggressive ogres.",
    rewardsText =
        "15,250 Magic XP<br>" +
        "5,000 coins<br>" +
        "Watchtower Teleport spell<br>" +
        "Access to Gu'Tanoth and the Ogre Enclave",
    completedStage = STAGE_COMPLETE
)
class Watchtower : QuestOutline() {
    companion object {
        @JvmStatic
        fun meetsRequirements(player: Player): Boolean {
            return player.skills.getLevelForXp(Constants.AGILITY) >= REQUIRED_AGILITY_LEVEL &&
                    player.skills.getLevelForXp(Constants.HERBLORE) >= REQUIRED_HERBLORE_LEVEL &&
                    player.skills.getLevelForXp(Constants.MAGIC) >= REQUIRED_MAGIC_LEVEL &&
                    player.skills.getLevelForXp(Constants.MINING) >= REQUIRED_MINING_LEVEL &&
                    player.skills.getLevelForXp(Constants.THIEVING) >= REQUIRED_THIEVING_LEVEL
        }
    }

    override fun getJournalLines(
        player: Player,
        stage: Int
    ): List<String> = when (stage) {
        STAGE_UNSTARTED -> listOf(
            "I can start this quest by speaking to the",
            "Watchtower Wizard, north-west of Yanille.",
            "",
            "I need level 25 Agility, level 14 Herblore,",
            "level 14 Magic, level 40 Mining",
            "and level 15 Thieving."
        )

        STAGE_SEARCH_WATCHTOWER -> listOf(
            "The Watchtower's magical defences have failed.",
            "",
            "Four crystals which power the Watchtower's",
            "magical protection have been stolen.",
            "",
            "The Watchtower Wizard has asked me to search",
            "the surrounding area for evidence.",
            "",
            "He suggested that I search the bushes."
        )

        STAGE_FIND_WAY_INTO_GUTANOTH -> listOf(
            "I found some disgusting fingernails while",
            "searching around the Watchtower.",
            "",
            "The Watchtower Wizard identified them as",
            "belonging to a skavid.",
            "",
            "Skavids are servants of the ogres and live",
            "in caves around the Feldip Hills.",
            "",
            "I need to investigate the ogre settlements",
            "and find a way into Gu'Tanoth."
        )

        STAGE_COLLECT_OGRE_RELIC -> listOf(
            "The ogres around Gu'Tanoth may be able to",
            "help me gain entrance to their city.",
            "",
            "I should deal with Og, Grew and Toban.",
            "",
            "They each have their own problems which I",
            "may be able to solve.",
            "",
            "The strange pieces of an old ogre statue",
            "may prove useful."
        )

        STAGE_ENTER_GUTANOTH -> listOf(
            "The Watchtower Wizard has assembled the",
            "pieces into an ancient ogre relic.",
            "",
            "It represents an ogre warrior of old and",
            "should prove that I am a friend to the ogres.",
            "",
            "I should take the relic to the guards of",
            "Gu'Tanoth and try to enter the city."
        )

        STAGE_GET_SKAVID_MAP -> listOf(
            "I have gained access to Gu'Tanoth.",
            "",
            "The skavid caves are deep and complex.",
            "I will need a map before I can navigate them.",
            "",
            "I should search Gu'Tanoth for an ogre who",
            "can help me obtain a skavid map."
        )

        STAGE_LEARN_SKAVID_LANGUAGE -> listOf(
            "I have obtained a map of the skavid caves.",
            "",
            "I now need to investigate the caves and",
            "discover what the skavids know.",
            "",
            "The skavids speak their own language.",
            "Perhaps one of them can teach me enough",
            "to communicate with the others."
        )

        STAGE_SEARCH_SKAVID_CAVES -> listOf(
            "A scared skavid has begun teaching me",
            "the skavid language.",
            "",
            "The common words are:",
            "Ar, nod, gor, ig and cur.",
            "",
            "I should visit the other skavids and learn",
            "enough of their language to speak to the",
            "mad skavids inside Gu'Tanoth."
        )

        STAGE_FIND_SHAMAN_ENCLAVE -> if (player.questManager.getAttribs(Quest.WATCHTOWER).getB(MAD_SKAVID_COMPLETE)) listOf(
            "The mad skavid gave me a Watchtower crystal.",
            "",
            "The ogre shamans have the remaining crystals",
            "inside their enclave.",
            "",
            "I should use cave nightshade to distract",
            "an enclave guard and get inside."
        ) else listOf(
            "I have learned enough of the skavid language.",
            "",
            "The mad skavids know something about the",
            "stolen Watchtower crystals.",
            "",
            "I should find their cave inside Gu'Tanoth",
            "and continue my search.",
            "",
            "The ogre shamans may have the remaining",
            "crystals somewhere inside their enclave."
        )

        STAGE_DISCOVER_SHAMANS -> listOf(
            "I have discovered the ogre shamans' enclave.",
            "",
            "The shamans are protected by powerful magic.",
            "I cannot defeat them by ordinary means.",
            "",
            "I should return to the Watchtower Wizard",
            "and ask him how they can be destroyed."
        )

        STAGE_MAKE_OGRE_POTION -> listOf(
            "The Watchtower Wizard told me how to make",
            "a potion capable of defeating the shamans.",
            "",
            "I need to mix the ingredients in order:",
            "a guam leaf, then jangerberries, and finally",
            "some ground bat bones.",
            "",
            "I must return the completed potion to the",
            "Wizard so that he can empower it with magic."
        )

        STAGE_DEFEAT_SHAMANS -> listOf(
            "The Watchtower Wizard has infused my potion",
            "with magic.",
            "",
            "I should return to the Ogre Enclave and use",
            "the magic ogre potion on all of the shamans.",
            "",
            "The potion is the only way to overcome their",
            "magical protection."
        )

        STAGE_RECOVER_CRYSTALS -> listOf(
            "The ogre shamans have been destroyed.",
            "",
            "I still need to recover the Watchtower's",
            "remaining power crystals.",
            "",
            "One of the crystals was hidden inside the",
            "sacred Rock of Dalgroth.",
            "",
            "I must recover all four crystals before",
            "returning to the Watchtower."
        )

        STAGE_RESTORE_WATCHTOWER -> listOf(
            "I have recovered all four of the stolen",
            "Watchtower crystals.",
            "",
            "The Watchtower Wizard has told me to place",
            "the crystals on the pillars in the tower.",
            "",
            "Each crystal must be placed on the correct",
            "pillar before the shield can be restored."
        )

        STAGE_ACTIVATE_WATCHTOWER -> listOf(
            "All four crystals have been placed on their",
            "correct pillars.",
            "",
            "The Watchtower's magical defence system is",
            "ready to be activated.",
            "",
            "I should pull the lever and restore the",
            "magical shield protecting Yanille."
        )

        STAGE_COMPLETE -> listOf(
            "QUEST COMPLETE!"
        )

        else -> listOf(
            "Invalid quest stage. Report this to an administrator."
        )
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(
            Skills.MAGIC,
            15250.0
        )

        player.inventory.addCoins(5000)

        player.inventory.addItemDrop(
            Obj.watchtowerspell,
            1
        )

        sendQuestCompleteInterface(
            player,
            Obj.watchtowerspell
        )
    }
}

fun startWatchtower(player: Player) {
    if (player.getQuestStage(Quest.WATCHTOWER) != STAGE_UNSTARTED) return
    player.savingAttributes.keys.removeIf { it.startsWith("watchtower") && it != "watchtowerTeleport" }
    (WATCHTOWER_SHAMAN_VARBITS.values + listOf(
        Varbit.watchtower_pillar_1, Varbit.watchtower_pillar_2,
        Varbit.watchtower_pillar_3, Varbit.watchtower_pillar_4
    )).forEach { player.vars.saveVarBit(it, 0) }
    player.questManager.resetQuest(Quest.WATCHTOWER)
    player.setQuestStage(Quest.WATCHTOWER, STAGE_SEARCH_WATCHTOWER)
}
