package com.rs.game.content.quests.creatureoffenkenstrain

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onNpcDeath
import com.rs.plugin.kts.onItemOnNpc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

@ServerStartupEvent
fun mapFenkenstrainNpcs() {
    onNpcClick(Npc.fenk_fenkenstrain_model) { (player, npc, option) ->
        if (!player.ownsFenkenstrainNpc(npc)) return@onNpcClick
        when (option) {
            "Talk-to" -> player.talkFenkenstrain()
            "Pickpocket" -> player.stealCharos()
        }
    }
    onNpcClick(Npc.fenk_gardener) { (player, npc) ->
        if (player.ownsFenkenstrainNpc(npc)) player.talkGardener(npc)
    }
    onNpcClick(Npc.fenk_creature_model, Npc.fenk_creature_model_2) { (player, npc) ->
        if (!player.ownsFenkenstrainNpc(npc) || player.fenkStage < TOWER) return@onNpcClick
        player.startConversation {
            when (player.fenkStage) {
                TOWER -> {
                    monster(player)
                    fenkExec(player) { if (player.fenkStage == TOWER) player.setQuestStage(FENKENSTRAIN, TRUTH) }
                }
                TRUTH -> ringHint()
                COMPLETE -> rologarthPost()
            }
        }
    }
    onNpcDeath(Npc.fenk_experiment_1) { (killer, _) ->
        val player = killer as? Player ?: return@onNpcDeath
        if (player.fenkStage >= BODY_PARTS && !player.containsItem(Obj.fenk_mausoleum_key))
            player.inventory.addItemDrop(Obj.fenk_mausoleum_key, 1)
    }
    onItemOnNpc(Npc.fenk_fenkenstrain_model) { (player, item, npc) ->
        if (!player.ownsFenkenstrainNpc(npc)) return@onItemOnNpc
        when (item.id) {
            in PARTS, Obj.needle, Obj.thread -> player.talkFenkenstrain()
            Obj.fenk_head_empty -> player.startConversation { npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I can't use this head, ${player.displayName}. It's missing the most important part - the old grey matter.") }
            Obj.trick_or_treat_head -> player.startConversation { zombieHead() }
        }
    }
}

internal fun Player.ownsFenkenstrainNpc(npc: NPC) = npc !is OwnedNPC || npc.owner === this

internal fun DialogueBuilder.fenkenstrainInterview(owner: Player) {
    player(CALM_TALK, "Yes, if it pays well.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I'll have to ask you some questions first.")
    player(CALM_TALK, "Okay...")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "How would you describe yourself in one word?")
    options {
        ops("Stunning.", "Awe-inspiring.", "Breathtaking.") { rejected() }
        op("Braindead.") {
            npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Mmmm, I see.")
            npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Just one more question. What would you say is your greatest skill?")
            options {
                listOf("Combat.", "Magic.", "Cooking.").forEach { skill ->
                    op(skill) { player(CALM_TALK, skill); rejected() }
                }
                op("Grave-digging.") {
                    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Mmmm, I see.")
                    hired(owner)
                    fenkExec(owner) { if (owner.fenkStage <= ADVERT) owner.setQuestStage(FENKENSTRAIN, BODY_PARTS) }
                }
            }
        }
    }
}

private fun DialogueBuilder.rejected() {
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Mmmm, I see.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I'm sorry, but I don't think you have the aptitude for this position.")
}

private fun Player.talkFenkenstrain() {
    when (fenkStage) {
        UNSTARTED, ADVERT -> startConversation {
            npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Have you come to apply for the job?")
            if (fenkStage == UNSTARTED) questStart(FENKENSTRAIN, accepted = {
                if (canStartFenkenstrain()) {
                    fenkExec(this@talkFenkenstrain) { if (fenkStage == UNSTARTED) setQuestStage(FENKENSTRAIN, ADVERT) }
                    fenkenstrainInterview(this@talkFenkenstrain)
                } else simple("You need Priest in Peril, The Restless Ghost started, 20 Crafting and 25 Thieving.")
            }, declined = {
                player(CALM_TALK, "No thanks, I wouldn't work here if you paid me.")
                npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Charming.")
            }) else options {
                op("Yes.") {
                    if (canStartFenkenstrain()) fenkenstrainInterview(this@talkFenkenstrain)
                    else simple("You need Priest in Peril, The Restless Ghost started, 20 Crafting and 25 Thieving.")
                }
                op("No.") {
                    player(CALM_TALK, "No thanks, I wouldn't work here if you paid me.")
                    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Charming.")
                }
            }
        }
        BODY_PARTS -> startConversation {
            options {
                op("I have some body parts for you.") { fenkExec(this@talkFenkenstrain) { handInParts() } }
                op("Do you know where I could find body parts?") { partsHint() }
                op("Remind me what you want me to do.") {
                    player(CALM_TALK, "What do you want me to do, again?")
                    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I need you to get me enough dead body parts for me to stitch together a complete body, which I plan to bring to life.")
                    player(CALM_TALK, "Right...okay...if you insist.")
                }
                op("Why are you trying to make this creature?") { purpose() }
                op("Will this creature put me out of a job?") { job() }
                op("I must get back to work, sir.") { player(CALM_TALK, "I must get back to work, sir.") }
            }
        }
        SEWING -> handInSewing()
        CONDUCTOR -> startConversation {
            if (inventory.containsItem(Obj.fenk_conductor)) npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Very good, you have made a new conductor - now get up on the roof and repair it before this storm passes!")
            else conductorHint(this@talkFenkenstrain)
        }
        REVIVED -> startConversation {
            revived(this@talkFenkenstrain)
            fenkExec(this@talkFenkenstrain) {
                if (fenkStage == REVIVED) {
                    inventory.addItemDrop(Obj.fenk_tower_key, 1)
                    setQuestStage(FENKENSTRAIN, TOWER)
                }
            }
        }
        TOWER -> startConversation {
            npc(Npc.fenk_fenkenstrain_model, SCARED, "So have you destroyed it?!!?")
            player(CALM_TALK, "Not yet.")
            npc(Npc.fenk_fenkenstrain_model, SCARED, "Please, hurry - save me!!!!")
            fenkExec(this@talkFenkenstrain) { if (!fenk.getB("towerUnlocked") && !containsItem(Obj.fenk_tower_key)) inventory.addItemDrop(Obj.fenk_tower_key, 1) }
        }
        TRUTH -> startConversation { defiance() }
        COMPLETE -> startConversation { doctorPost() }
    }
}

private fun Player.handInParts() {
    if (fenkStage != BODY_PARTS) return
    val handed = PARTS.filter { !fenk.getB("part$it") && inventory.containsItem(it, 1, false) }
    handed.forEach {
        inventory.deleteItem(it, 1)
        fenk.setB("part$it", true)
    }
    questManager.sendQuestStage(FENKENSTRAIN)
    startConversation {
        player(CALM_TALK, "I have some body parts for you.")
        handed.forEach { part ->
            npc(Npc.fenk_fenkenstrain_model, HAPPY_TALKING, when (part) {
                Obj.fenk_head_full -> "Fantastic, you've brought me a head."
                Obj.fenk_torso -> "Splendid, you've brought me a torso."
                Obj.fenk_arms -> "Great, you've brought me some arms."
                else -> "Excellent, you've brought me some legs."
            })
        }
        if (PARTS.all { fenk.getB("part$it") }) {
            npc(Npc.fenk_fenkenstrain_model, HAPPY_TALKING, "Superb!! Those are all the parts I need. Now to sew them together ...")
            npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Go and get me a needle, and I'll need 5 lots of thread.") {
                if (fenkStage == BODY_PARTS) setQuestStage(FENKENSTRAIN, SEWING)
            }
        } else if (handed.isEmpty()) npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I need you to get me enough dead body parts for me to stitch together a complete body, which I plan to bring to life.")
    }
}

private fun Player.handInSewing() {
    val neededNeedle = !fenk.getB("needle")
    val neededThread = 5 - fenk.getI("thread")
    val needle = neededNeedle && inventory.containsItem(Obj.needle, 1, false)
    val thread = minOf(neededThread, inventory.getNumberOf(Obj.thread, false))
    if (needle) { inventory.deleteItem(Obj.needle, 1); fenk.setB("needle", true) }
    if (thread > 0) { inventory.deleteItem(Obj.thread, thread); fenk.setI("thread", fenk.getI("thread") + thread) }
    questManager.sendQuestStage(FENKENSTRAIN)
    startConversation {
        if (neededNeedle || neededThread > 0) npc(Npc.fenk_fenkenstrain_model, CALM_TALK, when {
            neededNeedle && neededThread > 0 -> "Where are my needle and thread, $displayName?"
            neededNeedle -> "Where's my needle, $displayName?"
            else -> "Where's my thread, $displayName?"
        })
        if (needle) npc(Npc.fenk_fenkenstrain_model, HAPPY_TALKING, "Ah, a needle. Wonderful.")
        if (thread > 0) npc(Npc.fenk_fenkenstrain_model, HAPPY_TALKING, "Some thread, excellent.")
        if (fenk.getB("needle") && fenk.getI("thread") == 5) {
            simple("Fenkenstrain uses the needle and thread to sew the body parts together. Soon, a hideous creature lies inanimate on the ritual table.")
            lightning()
            fenkExec(this@handInSewing) { if (fenkStage == SEWING) setQuestStage(FENKENSTRAIN, CONDUCTOR) }
        }
    }
}

private fun Player.stealCharos() {
    when {
        fenkStage == COMPLETE -> {
            if (containsAnyItems(Obj.ring_of_charos, Obj.ring_of_charos_unlocked)) simpleDialogue("You already have the Ring of Charos.")
            else fenkGive(Obj.ring_of_charos, "You steal the Ring of Charos from Fenkenstrain.")
        }
        fenkStage != TRUTH -> startConversation { npc(Npc.fenk_fenkenstrain_model, ANGRY, "What do you think you're doing???") }
        skills.getLevel(Skills.THIEVING) < 25 -> simpleDialogue("You need a Thieving level of 25 to pickpocket Fenkenstrain.")
        !inventory.hasFreeSlots() -> simpleDialogue("You need a free inventory space.")
        else -> {
            anim(Seq.human_pickpocket)
            sendMessage("You steal the Ring of Charos from Fenkenstrain.")
            questManager.completeQuest(FENKENSTRAIN)
        }
    }
}

private fun Player.talkGardener(npc: NPC) {
    if (equipment.neckId !in setOf(Obj.amulet_of_ghostspeak, Obj.amulet_of_ghostspeak_enchanted)) {
        simpleDialogue("The Headless Gardener's neck twitches at you, but the lack of any head prevents him from speaking.")
        return
    }
    if (tempAttribs.getB("fenkFollowing")) {
        gardenerDirections(npc)
        return
    }
    if (fenkStage >= REVIVED) {
        startConversation {
            player(CALM_TALK, "How are you?")
            npc(Npc.fenk_gardener, CALM_TALK, "Same as ever, mate, just getting' on with it regardless.")
            player(CALM_TALK, "Good for you...err...mate.")
        }
        return
    }
    startConversation {
        simple("You feel power emanate from the Amulet of Ghostspeak, and the air around you vibrates with the ghostly voice of the Headless Gardener...")
        options {
            op("Tell me about Fenkenstrain.") {
                player(CALM_TALK, "Can you tell me anything about Fenkenstrain?")
                gardenerHistory()
            }
            if (fenkStage == CONDUCTOR) {
                op("Do you know where the key to the shed is?") {
                    player(CALM_TALK, "Do you know where the key to the shed is?")
                    npc(Npc.fenk_gardener, CALM_TALK, "Got it right 'ere in my pocket. Here you go.")
                    fenkExec(this@talkGardener) { if (!containsItem(Obj.fenk_shed_key)) inventory.addItemDrop(Obj.fenk_shed_key, 1) }
                    simple("The headless gardener hands you a rusty key.")
                }
                op("Do you know where a conductor mould is?") { mouldHint() }
            }
            op("What happened to your head?") {
                gardenerHead()
                if (containsAnyItems(Obj.fenk_head_empty, Obj.fenk_head_full) || fenk.getB("part${Obj.fenk_head_full}"))
                    npc(Npc.fenk_gardener, CALM_TALK, "No mate, you've found my head already, oi think.")
                else {
                    npc(Npc.fenk_gardener, CALM_TALK, "Well, oi s'pose oi've got 10 minutes to spare.")
                    fenkExec(this@talkGardener) { if (fenkStage in BODY_PARTS until CONDUCTOR) startGardenerEscort(npc) }
                }
            }
            op("What's your name?") { player(CALM_TALK, "What's your name?"); gardenerName() }
        }
    }
}
