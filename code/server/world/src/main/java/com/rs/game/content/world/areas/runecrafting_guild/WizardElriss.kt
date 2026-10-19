package com.rs.game.content.world.areas.runecrafting_guild

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.CONFUSED
import com.rs.engine.dialogue.HeadE.HAPPY_TALKING
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private const val SHOWN_TALISMANS = "omniTalisman_shownTypes"

enum class OmniTalisman(val type: String, val talisman: Int, val tiara: Int, val xp: Double) {
    AIR("Air", Obj.air_talisman, Obj.tiara_air, 8.0),
    MIND("Mind", Obj.mind_talisman, Obj.tiara_mind, 9.0),
    WATER("Water", Obj.water_talisman, Obj.tiara_water, 11.0),
    EARTH("Earth", Obj.earth_talisman, Obj.tiara_earth, 17.0),
    FIRE("Fire", Obj.fire_talisman, Obj.tiara_fire, 28.0),
    BODY("Body", Obj.body_talisman, Obj.tiara_body, 49.0),
    COSMIC("Cosmic", Obj.cosmic_talisman, Obj.tiara_cosmic, 110.0),
    CHAOS("Chaos", Obj.chaos_talisman, Obj.tiara_chaos, 216.0),
    NATURE("Nature", Obj.nature_talisman, Obj.tiara_nature, 531.0),
    LAW("Law", Obj.law_talisman, Obj.tiara_law, 1428.0),
    DEATH("Death", Obj.death_talisman, Obj.tiara_death, 4241.0),
    BLOOD("Blood", Obj.blood_talisman, Obj.tiara_blood, 6958.0);
}

private val Player.shownTalismans: Set<String> get() = getO<List<String>>(SHOWN_TALISMANS)?.toSet().orEmpty()

private fun Player.carries(obj: Int) = inventory.containsItem(obj) || equipment.containsOneItem(obj)

fun Player.owns(obj: Int) = carries(obj) || bank.containsItem(obj)

@ServerStartupEvent
fun mapWizardElriss() {
    onNpcClick(Npc.rcguild_wizard_elriss) { (player, _, option) ->
        if (option == "Exchange") openGuildRewards(player) else talkToElriss(player)
    }
}

private fun talkToElriss(player: Player) = player.startConversation {
    val elriss = Npc.rcguild_wizard_elriss
    npc(elriss, HAPPY_TALKING, "Welcome to the Runecrafting Guild.")
    label("initialOps")
    options {
        op("What is this place?") {
            player(CONFUSED, "What is this place?")
            npc(elriss, HAPPY_TALKING, "This is the Runecrafting Guild. After the secret of Runecrafting was re-discovered, I set up the guild as a place for the most advanced runecrafters to work together.")
            goto("initialOps")
        }
        op("What can I do here?") {
            player(CONFUSED, "What can I do here?")
            npc(elriss, CALM_TALK, "Wizard Acantha and Wizard Vief are running the Great Orb Project. It requires large numbers of runecrafters, so you should speak with them if you want something to do.")
            npc(elriss, CALM_TALK, "Wizard Korvak has visited the Abyss and can repair abyssal pouches. I, myself, am working on a new kind of talisman: the omni-talisman.")
            options {
                op("Tell me about Acantha and Vief's project.") { greatOrbProject(this) }
                op("Tell me about the omni-talisman.") { omniTalisman(this, player) }
                op("Tell me about Wizard Korvak's pouch repairs.") {
                    player(CALM_TALK, "Tell me about Wizard Korvak's pouch repairs.")
                    npc(elriss, CALM_TALK, "Wizard Korvak is the only one of us to have visited the Abyss. He learned about rune pouches and how to repair them. None of us quite knows how he does it, and I'm not sure he does either, but it seems to work.")
                }
                op("Never mind.")
            }
        }
        opExec("I'd like to spend my tokens.") { openGuildRewards(player) }
        op("Who is that wizard outside the guild portal?") {
            npc(elriss, CALM_TALK, "Ah, you have met Wizard Finix, a promising young wizard, even if he does have some rather controversial theories.")
            npc(elriss, CALM_TALK, "He believes the altars are not endless pools of energy after all, and that we must learn to siphon runic energy from elsewhere. I have opened the Runespan to him so that runecrafters can train in the method.")
            player(CALM_TALK, "I had better get out there and start training, then!")
            npc(elriss, CALM_TALK, "Your efforts would be very much appreciated, ${player.displayName}.")
            goto("initialOps")
        }
        op("Never mind.")
    }
}

private fun omniTalisman(dialogue: DialogueBuilder, player: Player) = dialogue.apply {
    val elriss = Npc.rcguild_wizard_elriss
    player(CALM_TALK, "Tell me about the omni-talisman.")
    npc(elriss, CALM_TALK, "The omni-talisman combines the properties of every talisman. It will let you into any of the Runecrafting altars, and it can be bound to a tiara or a staff, just like an ordinary talisman.")
    npc(elriss, CALM_TALK, "Show me each type of talisman, or its tiara, and I will make you one. For each one you show me, I will also teach you a little about Runecrafting.")
    exec { showTalismans(player) }
}

private fun showTalismans(player: Player) {
    val shown = player.shownTalismans.toMutableSet()
    val newlyShown = OmniTalisman.entries.filter { it.type !in shown && (player.carries(it.talisman) || player.carries(it.tiara)) }
    for (talisman in newlyShown) {
        shown += talisman.type
        player.skills.addXp(Skills.RUNECRAFTING, talisman.xp)
    }
    player.set(SHOWN_TALISMANS, shown.toList())
    val remaining = OmniTalisman.entries.filter { it.type !in shown }
    val elriss = Npc.rcguild_wizard_elriss
    player.startConversation {
        newlyShown.forEach { item(if (player.carries(it.talisman)) it.talisman else it.tiara, "You show Elriss the ${it.type.lowercase()} ${if (player.carries(it.talisman)) "talisman" else "tiara"}.") }
        when {
            remaining.isNotEmpty() -> {
                npc(elriss, CALM_TALK, "You have yet to show me these talismans: ${remaining.joinToString(", ") { it.type }}.")
                npc(elriss, HAPPY_TALKING, "Once you've shown me them all, I can give you an omni-talisman.")
            }
            player.owns(Obj.omni_talisman) || player.owns(Obj.tiara_omni) || player.owns(Obj.rcguild_staff_omni) ->
                npc(elriss, CALM_TALK, "You have already shown me every talisman, and you still have your omni-talisman.")
            !player.inventory.hasFreeSlots() -> npc(elriss, CALM_TALK, "I have an omni-talisman for you, but you have no room to carry it.")
            else -> {
                npc(elriss, HAPPY_TALKING, "Excellent! You've shown me every talisman. Here is your omni-talisman.")
                item(Obj.omni_talisman, "Wizard Elriss gives you an omni-talisman.") { player.inventory.addItem(Obj.omni_talisman, 1) }
                npc(elriss, CALM_TALK, "Take good care of it. It is a powerful artefact, and Wizard Korvak can bind it to a tiara or a staff for you.")
            }
        }
    }
}

private fun greatOrbProject(dialogue: DialogueBuilder) = dialogue.apply {
    val elriss = Npc.rcguild_wizard_elriss
    player(CALM_TALK, "Tell me about Acantha and Vief's project.")
    npc(elriss, CALM_TALK, "The Great Orb Project? It's truly fascinating. Wizards Acantha and Vief have found that energy leaks out of some of the Runecrafting altars, and they are recruiting teams of experienced runecrafters to force it back in.")
    npc(elriss, CALM_TALK, "Join one of the teams by speaking to Wizard Acantha or Wizard Vief. When the wizards have enough helpers, I will open a portal to the air altar.")
    npc(elriss, CALM_TALK, "The energy appears as floating orbs, which you move with wands that attract or repel them. Wizard Acantha favours the green orbs, while Wizard Vief favours the yellow ones. A barrier generator lets you block the other team's orbs.")
    npc(elriss, CALM_TALK, "After two minutes the team that absorbed the most orbs wins that altar, and I open the portal to the next. After all eight altars you return here.")
    npc(elriss, CALM_TALK, "Every altar you help with earns you tokens: 100 for a win, 25 for a draw and 10 for a loss, and 200 more if your team wins the most altars. You can spend them with me.")
}
