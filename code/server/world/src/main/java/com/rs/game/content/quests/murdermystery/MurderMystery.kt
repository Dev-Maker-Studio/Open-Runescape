package com.rs.game.content.quests.murdermystery

import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

const val STAGE_UNSTARTED = 0
const val STAGE_SOMETHINGS_AMISS = 1
const val STAGE_QUESTIONING_INHABITANTS = 2
const val STAGE_GATHERING_EVIDENCE = 3
const val STAGE_VISITING_POISON_SALESMAN = 4
const val STAGE_INVESTIGATING_POISONED_AREAS = 5
const val STAGE_PRESENTING_EVIDENCE = 6
const val STAGE_COMPLETE = 7

const val MM_INTERVIEWS_KEY = "murderMysteryInterviews"
const val MM_POISON_CLAIMS_KEY = "murderMysteryPoisonClaims"

private val MM_INTERVIEWEES = linkedMapOf(
    Npc.anna_sinclair to "Anna",
    Npc.bob_sinclair to "Bob",
    Npc.carol_sinclair to "Carol",
    Npc.david_sinclair to "David",
    Npc.elizabeth_sinclair to "Elizabeth",
    Npc.frank_sinclair to "Frank",
    Npc.donovan_the_family_handyman to "Donovan",
    Npc.hobbes_the_butler to "Hobbes",
    Npc.louisa_the_cook to "Louisa",
    Npc.mary_the_maid to "Mary",
    Npc.pierre_the_family_dog_handler to "Pierre",
    Npc.stanford_the_gardener to "Stanford"
)

fun missingMurderMysteryInterviews(player: Player): List<String> =
    MM_INTERVIEWEES.values.filterIndexed { index, _ -> (player.getI(MM_INTERVIEWS_KEY, 0) and (1 shl index)) == 0 }

fun recordMurderMysteryInterview(player: Player, npcId: Int) {
    if (!murderMysteryStarted(player)) return
    val index = MM_INTERVIEWEES.keys.indexOf(npcId)
    if (index < 0) return
    player.set(MM_INTERVIEWS_KEY, player.getI(MM_INTERVIEWS_KEY, 0) or (1 shl index))
    refreshMurderMysteryProgress(player)
}

fun recordMurderMysteryPoisonClaim(player: Player, suspect: Int) {
    if (!murderMysteryStarted(player) || !player.getBool(MM_POISON_BUYERS_KNOWN)) return
    player.set(MM_POISON_CLAIMS_KEY, player.getI(MM_POISON_CLAIMS_KEY, 0) or (1 shl suspect))
}

fun knowsMurderMysteryPoisonClaim(player: Player, suspect: Int): Boolean =
    (player.getI(MM_POISON_CLAIMS_KEY, 0) and (1 shl suspect)) != 0

fun hasMurderMysteryEvidence(player: Player): Boolean =
    murderMysteryStarted(player) &&
        missingMurderMysteryInterviews(player).isEmpty() &&
        player.getBool(MM_POISON_BUYERS_KNOWN) &&
        knowsMurderMysteryPoisonClaim(player, ensureMurderer(player)) &&
        player.getBool(MM_POISON_EVIDENCE_FOUND) &&
        player.inventory.containsItem(criminalThreadFor(player), 1) &&
        player.inventory.containsItem(Obj.murderfingerprint, 1)

fun refreshMurderMysteryProgress(player: Player) {
    if (hasMurderMysteryEvidence(player))
        player.setQuestStage(Quest.MURDER_MYSTERY, STAGE_PRESENTING_EVIDENCE)
}

@QuestHandler(
    quest = Quest.MURDER_MYSTERY,
    startText = "Speak to a guard at Sinclair Mansion, north of Camelot and north-east of Seers' Village.",
    itemsText = "A pot. Additional items required during the investigation can be obtained during the quest.",
    combatText = "None.",
    rewardsText = "3 Quest Points<br>2,000 coins<br>1,406 Crafting XP",
    completedStage = STAGE_COMPLETE
)
class MurderMystery : QuestOutline() {
    override fun getJournalLines(player: Player, stage: Int): List<String> {
        val lines = when (stage) {
            STAGE_UNSTARTED -> listOf("I can start this quest by speaking to one of the guards", "at Sinclair Mansion, north of Camelot.")
            STAGE_SOMETHINGS_AMISS -> listOf("Something is amiss at Sinclair Mansion.", "Lord Sinclair has been murdered and the guards have asked", "me to help investigate the crime.")
            STAGE_QUESTIONING_INHABITANTS -> listOf("I should question the inhabitants of Sinclair Mansion.", "Someone here may know something useful about the murder.")
            STAGE_GATHERING_EVIDENCE -> listOf("I need to gather evidence from around Sinclair Mansion.", "The crime scene and the belongings of the suspects may", "help me identify Lord Sinclair's murderer.")
            STAGE_VISITING_POISON_SALESMAN -> listOf("My investigation has raised questions about poison.", "I should visit the Poison Salesman in Seers' Village", "and find out who recently bought poison from him.")
            STAGE_INVESTIGATING_POISONED_AREAS -> listOf("I should investigate the places where the Sinclair family", "claim to have used the poison they purchased.", "One of their stories may not add up.")
            STAGE_PRESENTING_EVIDENCE -> listOf("I have gathered enough evidence to identify the murderer.", "I should present my evidence to one of the guards", "at Sinclair Mansion.")
            STAGE_COMPLETE -> listOf("QUEST COMPLETE!")
            else -> listOf("Invalid quest stage. Report this to an administrator.")
        }

        if (stage !in STAGE_SOMETHINGS_AMISS until STAGE_COMPLETE) return lines
        val missing = missingMurderMysteryInterviews(player)
        return lines + if (missing.isEmpty()) listOf("I have questioned all twelve suspects.") else
            listOf("I still need to question:") + missing.map { "  $it" }
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Skills.CRAFTING, 1406.0)
        player.inventory.addCoins(2000)
        sendQuestCompleteInterface(player, Obj.coins)
    }
}
