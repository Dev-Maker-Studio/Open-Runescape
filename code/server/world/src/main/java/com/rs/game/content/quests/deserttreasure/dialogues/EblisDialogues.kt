package com.rs.game.content.quests.deserttreasure

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private const val INGREDIENTS_AGREED = "dtEblisIngredientsAgreed"

private enum class Ingredient(val itemId: Int, val notedId: Int?, val required: Int, val key: String, val one: String, val many: String) {
    LOGS(Obj.magic_logs, Obj.cert_magic_logs, 12, "dtEblisMagicLogs", "a magic log", "magic logs"),
    STEEL(Obj.steel_bar, Obj.cert_steel_bar, 6, "dtEblisSteelBars", "a steel bar", "steel bars"),
    GLASS(Obj.molten_glass, Obj.cert_molten_glass, 6, "dtEblisMoltenGlass", "some molten glass", "pieces of molten glass"),
    BONE(Obj.bones, Obj.cert_bones, 1, "dtEblisBones", "a set of standard bones", "sets of standard bones"),
    ASH(Obj.ashes, Obj.cert_ashes, 1, "dtEblisAshes", "some ashes", "ashes"),
    COAL(Obj.charcoal, Obj.cert_charcoal, 1, "dtEblisCharcoal", "some charcoal", "charcoal"),
    RUNE(Obj.bloodrune, null, 1, "dtEblisBloodRune", "a blood rune", "blood runes");

    fun delivered(player: Player) = player.getI(key, 0)
    val itemIds: List<Int> get() = listOfNotNull(itemId, notedId)
    fun available(player: Player) = itemIds.sumOf { player.inventory.getAmountOf(it) }
    fun remaining(player: Player) = (required - delivered(player)).coerceAtLeast(0)
    fun describe(amount: Int) = if (amount == 1) one else "$amount $many"
}

fun resetEblisIngredients(player: Player) {
    player.set(INGREDIENTS_AGREED, false)
    Ingredient.entries.forEach { player.set(it.key, 0) }
}

@ServerStartupEvent
fun mapEblisDesertTreasure() {
    onNpcClick(Npc.npc_1922, Npc.npc_1924) { (player, npc) -> eblisDialogue(player, npc.id) }
    onItemOnNpc(Npc.npc_1922) { (player, item) -> giveIngredient(player, item.id) }
}

private fun eblisDialogue(player: Player, eblisId: Int) {
    player.startConversation {
        when (player.getQuestStage(Quest.DESERT_TREASURE)) {
            STAGE_TALK_TO_EBLIS ->
                if (player.getBool(INGREDIENTS_AGREED)) collectIngredients(player, eblisId)
                else firstMeeting(player, eblisId)
            STAGE_RETURN_TO_MIRRORS ->
                if (eblisId == Npc.npc_1924) mirrorsBriefing(player, eblisId) else pointToMirrors(eblisId)
            STAGE_DIAMONDS_OF_AZZANADRA ->
                if (eblisId == Npc.npc_1924) diamondProgress(player, eblisId) else pointToMirrors(eblisId)
            STAGE_IFTAH_YA_SIMSIM -> hurryToPillars(eblisId)
            STAGE_RETURN_TO_JALDRAOCHT -> pillarsReport(eblisId)
            STAGE_TOMB_RAIDER, STAGE_AZZANADRA, STAGE_COMPLETE ->
                npc(eblisId, CALM_TALK, "Azzanadra walks free. Our people will remember what you did.")
            else -> firstMeeting(player, eblisId)
        }
    }
}

private fun DialogueBuilder.firstMeeting(player: Player, eblisId: Int) {
    player(CALM_TALK, "Hello, I represent the Museum of Varrock, and I have reason to believe there may be some kinds of artefacts of historical significance in the nearby area...")
    npc(eblisId, ANGRY, "Ah yes. The only time people care about our existence is when they think they have something to gain from us.")
    npc(eblisId, ANGRY, "I have nothing to say to you. You and your kind are not welcome here.")
    player(CALM_TALK, "Please, if I can just have a few minutes of your time to ask some questions...?")
    npc(eblisId, FRUSTRATED, "(sigh) I suppose I can spare you that. What do you wish to know about?")
    topics(player, eblisId)
}

private fun DialogueBuilder.topics(player: Player, eblisId: Int) {
    options {
        op("Why is this village so hostile?") { villageHostility(eblisId) }
        op("Do you know anything about treasure near here?") {
            player(CALM_TALK, "I was wondering if you knew anything about some treasure somewhere around here?")
            player(CALM_TALK, "I have some evidence that there might be some kind of treasure hidden very close to this village...")
            npc(eblisId, FRUSTRATED, "If I knew of any treasure I would not choose to spend my life in this gods-forsaken desert.")
        }
        op("Do you know anything about a fortress near here?") {
            player(CALM_TALK, "Do you know anything about some kind of fortress nearby? I have reason to believe there is, or at least used to be, some kind of fortress very close to here...")
            npc(eblisId, FRUSTRATED, "Nobody would build anything in this wasteland unless they were forced to, to survive.")
            npc(eblisId, FRUSTRATED, "I know of no fortress, I know of no reason why anyone would ever bother doing anything out here in the desert.")
        }
        if (player.getQuestStage(Quest.DESERT_TREASURE) >= STAGE_TALK_TO_EBLIS)
            op("Tell me of The four diamonds of Azzanadra.") { diamondsHistory(player, eblisId) }
        op("Nothing, thanks.") {
            player(CALM_TALK, "Actually, there was nothing I really wanted to ask you about.")
            npc(eblisId, ANGRY, "Yes, it is exactly like your sort to waste my time in such a way.")
        }
    }
}

private fun DialogueBuilder.villageHostility(eblisId: Int) {
    player(CONFUSED, "Why is everyone here so unfriendly? I only came to ask a few questions.")
    npc(eblisId, FRUSTRATED, "It is a long story, and your sort never cares to hear it. You take what you want and leave us to the sand.")
    options {
        op("I'd like to hear the story.") {
            player(CALM_TALK, "Actually, I would like to hear what has made everyone here so bitter.")
            npc(eblisId, CALM_TALK, "Long ago my people ruled great lands to the north, from the eastern sea to the river Lum. Our god watched over us and we prospered.")
            npc(eblisId, ANGRY, "Then he was cast out. Saradomin and Zamorak fought over the world, and both turned on us. We were driven south, step by step, into this desert.")
            npc(eblisId, SAD, "Our greatest hero, Azzanadra, was sealed inside a stone structure south of here. We have waited for his return ever since.")
            player(CONFUSED, "That was hundreds of years ago. Perhaps it is time to let it go?")
            npc(eblisId, ANGRY, "The wrongs done to us will never be forgotten. A wind will come to uncover the past, and we will be ready.")
        }
        op("I don't care about your story.") {
            player(ANGRY, "Frankly, I don't care. I have done nothing to you, yet everyone here treats me like a criminal.")
            npc(eblisId, ANGRY, "From where we stand, you have. You live a life that was taken from us.")
            player(FRUSTRATED, "Whatever. No wonder you all live out here alone.")
        }
    }
}

private fun DialogueBuilder.diamondsHistory(player: Player, eblisId: Int) {
    player(CALM_TALK, "So tell me... Did you ever hear of something called the Diamonds of Azzanadra?")
    npc(eblisId, AMAZED, "This is the treasure which you seek???")
    npc(eblisId, HAPPY_TALKING, "Please accept my apologies noble ${if (player.appearance.isMale) "sir" else "madam"}! I thought you were but some opportunistic thief, looking to steal what heritage we have left! Now I see that you are in fact a brave adventurer, looking to restore our glories back upon us!")
    player(CONFUSED, "Uh... yeah... So anyway, you have heard of them?")
    npc(eblisId, AMAZED, "Heard of them? Of course I have heard of them! They are the legacy of our greatest ever hero!")
    player(CONFUSED, "So... do you have any idea where they might be? I have a feeling they will be very valuable.")
    player(CALM_TALK, "Uh, valuable as historical artefacts I mean, obviously.")
    npc(eblisId, CALM_TALK, "They were stolen by warriors of the false god Zamorak generations ago. When you find the warriors, you will find the diamonds.")
    npc(eblisId, CALM_TALK, "I suspect they will not willingly part with such objects of power however.")
    npc(eblisId, WORRIED, "Beware too, for these warriors are very powerful; they have taken the powers of the diamonds into themselves!")
    player(CONFUSED, "How do you mean?")
    npc(eblisId, CALM_TALK, "Each diamond has an elemental quality... There is the Diamond of Blood, the Diamond of Ice, the Diamond of Smoke and the Diamond of Shadow.")
    npc(eblisId, CALM_TALK, "You should expect the warriors to have taken some aspect of these diamonds as their own...")
    player(CALM_TALK, " Do you have any idea how I could track down these warriors somehow, then?")
    npc(eblisId, CALM_TALK, "There is an ancient spell I know of that may spy upon such power... But it will require a few ingredients for it to work.")
    npc(eblisId, CALM_TALK, "Should you be willing to get these ingredients for me, I will be able to locate the rough area where each of these warriors has taken refuge. The spell is imprecise, but it should help you get on the right track in your search.")
    npc(eblisId, CALM_TALK, "Is your desire for our freedom strong enough? Will you gather the ingredients for this spell for me?")
    options {
        op("Yes") { ingredientBriefing(player, eblisId) }
        op("No") {
            player(CALM_TALK, "Actually I don't feel like going on a shopping trip for you right now.")
            npc(eblisId, SAD, "As you wish. I should have known not to get my hopes up that our long cursed life may soon be at an end...")
        }
    }
}

private fun DialogueBuilder.ingredientBriefing(player: Player, eblisId: Int) {
    player(CALM_TALK, "Sure, what do you need?")
    npc(eblisId, CALM_TALK, "For this spell, I will need to make some scrying glasses. I will need enough so that we can view the realm in its entirety.")
    npc(eblisId, CALM_TALK, "When enchanted, the scrying glass will be able to let us view any area that has been influenced by the presence of the Diamonds of Azzanadra.")
    player(CONFUSED, "Okay, but what exactly do you need for this spell?")
    npc(eblisId, CALM_TALK, "Well, six scrying glasses should be sufficient. For each scrying glass, I will need two magic logs, a steel bar and some molten glass. This makes a total of 12 magic logs, 6 pieces of molten glass, and 6 steel bars.")
    npc(eblisId, CALM_TALK, "In addition, for the actual spell to enchant the glasses, I will require one set of normal bones, some ash, some charcoal and a single blood rune.")
    npc(eblisId, CALM_TALK, "Do you understand me, adventurer?")
    label("eblisIngredientChoice")
    options {
        op("Yes, I'll go and get those.") {
            player(CALM_TALK, "It's a slightly odd collection of ingredients, but I shouldn't have too much trouble getting those for you.") {
                player.set(INGREDIENTS_AGREED, true)
            }
        }
        op("No, please repeat those ingredients.") {
            player(CALM_TALK, "No, please repeat those ingredients.")
            npc(eblisId, CALM_TALK, "12 magic logs, 6 steel bars, 6 pieces of molten glass, a set of ordinary bones, some ashes, some charcoal and a blood rune.")
            goto("eblisIngredientChoice")
        }
        op("Quick question; what kind of bones do you need?") {
            player(CONFUSED, "Quick question; what kind of bones do you need?")
            npc(eblisId, CALM_TALK, "Standard bones. Other types of bones are of no use to me in this spell.")
            goto("eblisIngredientChoice")
        }
    }
}

private fun ingredientPlan(player: Player): Map<Ingredient, Int> =
    Ingredient.entries.associateWith { minOf(it.remaining(player), it.available(player)) }

private fun outstandingText(player: Player, plan: Map<Ingredient, Int>): String =
    Ingredient.entries.mapNotNull { ingredient ->
        (ingredient.remaining(player) - plan.getValue(ingredient)).takeIf { it > 0 }?.let { ingredient.describe(it) }
    }.joinToString(", ")

private fun handOver(player: Player, ingredient: Ingredient, amount: Int): Int {
    if (player.getQuestStage(Quest.DESERT_TREASURE) != STAGE_TALK_TO_EBLIS || !player.getBool(INGREDIENTS_AGREED)) return 0
    var needed = minOf(amount, ingredient.remaining(player)).coerceAtLeast(0)
    var received = 0
    for (itemId in ingredient.itemIds) {
        val before = player.inventory.getAmountOf(itemId)
        val take = minOf(needed, before)
        if (take <= 0) continue
        player.inventory.deleteItem(itemId, take)
        val removed = before - player.inventory.getAmountOf(itemId)
        received += removed
        needed -= removed
    }
    player.set(ingredient.key, ingredient.delivered(player) + received)
    return received
}

private fun DialogueBuilder.collectIngredients(player: Player, eblisId: Int) {
    val plan = ingredientPlan(player)
    val outstanding = outstandingText(player, plan)

    if (Ingredient.entries.all { it.remaining(player) <= 0 }) {
        npc(eblisId, CHEERFUL, "Excellent! Those are all the ingredients I need to create the scrying glasses.")
        npc(eblisId, CALM_TALK, "I will find a suitable spot in the desert to the East of here, and set them up. When you are ready to begin your search, please come and find me there, I will show you how to utilise the mirrors to find the diamonds.") {
            if (Ingredient.entries.all { it.remaining(player) == 0 })
                player.setQuestStage(Quest.DESERT_TREASURE, STAGE_RETURN_TO_MIRRORS)
        }
        return
    }

    if (plan.values.all { it == 0 }) {
        player(CALM_TALK, "I'm still gathering what you asked for.")
        npc(eblisId, CALM_TALK, "Before I can complete the spell I will still need the following items; $outstanding.")
        return
    }

    player(CALM_TALK, "I have some of the stuff you are looking for with me...")
    npc(eblisId, CALM_TALK, "Thank you, pass over the ingredients so that I may begin working on the spell.") {
        if (player.getQuestStage(Quest.DESERT_TREASURE) == STAGE_TALK_TO_EBLIS && player.getBool(INGREDIENTS_AGREED)) {
            plan.forEach { (ingredient, amount) -> handOver(player, ingredient, amount) }
            player.startConversation { collectIngredients(player, eblisId) }
        }
    }
}

private fun giveIngredient(player: Player, itemId: Int) {
    val eligible = player.getQuestStage(Quest.DESERT_TREASURE) == STAGE_TALK_TO_EBLIS && player.getBool(INGREDIENTS_AGREED)
    val ingredient = Ingredient.entries.firstOrNull { itemId in it.itemIds }
    when {
        !eligible -> player.sendMessage("Nothing interesting happens.")
        ingredient == null -> player.startConversation {
            npc(Npc.npc_1922, CALM_TALK, "Thank you, but I don't have any need for that.")
        }
        ingredient.remaining(player) <= 0 -> player.startConversation {
            npc(Npc.npc_1922, CALM_TALK, "I already have as much of that as the spell requires.")
        }
        else -> {
            val amount = minOf(ingredient.remaining(player), player.inventory.getAmountOf(itemId))
            val received = handOver(player, ingredient, amount)
            if (received > 0) player.sendMessage("You hand over ${ingredient.describe(received)}.")
        }
    }
}

private fun DialogueBuilder.pointToMirrors(eblisId: Int) {
    npc(eblisId, CALM_TALK, "Meet me again in the desert east of here. I will have the mirrors ready for you there.")
}

private fun DialogueBuilder.mirrorsBriefing(player: Player, eblisId: Int) {
    npc(eblisId, CHEERFUL, "Ah, so you got here at last.")
    npc(eblisId, CHEERFUL, " As you may have noticed, I have made the mirrors for the spell, and cast the enchantment upon them.")
    npc(eblisId, CALM_TALK, " By simply looking into each mirror, you will be able to see the area where the trace magics from the Diamonds of Azzanadra are emanating from.")
    npc(eblisId, CALM_TALK, "Unfortunately, I cannot narrow the search closer with this kind of spell, but if you search the areas shown to you, you may be able to find some clues leading you to the evil warriors of Zamorak who stole the diamonds in the first place.")
    player(CONFUSED, " So you can't be anymore specific about where to look for these warriors and their diamonds?")
    npc(eblisId, CALM_TALK, "I'm afraid not, other than the direction that the mirror is facing will be approximately the direction you will need to head in.")
    npc(eblisId, CALM_TALK, " Make sure to come and speak to me when you have retrieved all four diamonds.") {
        player.setQuestStage(Quest.DESERT_TREASURE, STAGE_DIAMONDS_OF_AZZANADRA)
    }
}

private fun DialogueBuilder.diamondProgress(player: Player, eblisId: Int) {
    if (DesertTreasureDiamonds.hasAllDiamonds(player)) {
        allFourDiamonds(player, eblisId)
        return
    }

    val count = DesertTreasureDiamonds.diamondCount(player)
    options {
        op("So can you give me any help on where to find these warriors and their diamonds?") {
            player(CONFUSED, "So can you give me any help on where to find these warriors and their diamonds?")
            npc(eblisId, CALM_TALK, "No, the magic used in this spell is powerful, but inaccurate. The direction the scrying glass faces is roughly the direction you will find the warrior, but I'm afraid I can't be any more help than that.")
        }
        op("Why are there six mirrors for four diamonds?") {
            player(CONFUSED, "I don't understand why there are six mirrors when there are only four diamonds...")
            npc(eblisId, CALM_TALK, "As I say, the enchantment is very inaccurate.")
            npc(eblisId, CALM_TALK, "I can only focus upon the aura the diamonds have left behind them, so any place where the Diamonds were present for a significant period of time will still be shown - such as the Bandit Camp where I make my home.")
            npc(eblisId, SAD, "My apologies, but magic is an inaccurate art in many respects.")
        }
        op("Leave") {
            npc(eblisId, CALM_TALK, "Don't forget to come back here when you have collected all four diamonds.")
        }
        if (count > 0)
            op("I've been collecting the diamonds.") {
                player(CHEERFUL, "I have collected $count of the diamonds so far.")
                npc(eblisId, CALM_TALK, "That is a good start... but Azzanadra is still imprisoned!")
            }
    }
}

private fun DialogueBuilder.allFourDiamonds(player: Player, eblisId: Int) {
    player(CHEERFUL, "So I have all four of these Diamonds of Azzanadra, now what?")
    npc(eblisId, CALM_TALK, "Azzanadra was our greatest ever hero. He was unkillable, and the cowardly traitors who stole our lands did not know what to do with him, for his hatred for them was as strong as his magics.")
    npc(eblisId, CALM_TALK, "In the end, they cast a spell upon him, to trap him in the stone structure to the South of here.")
    npc(eblisId, CALM_TALK, "They stole his very life force, the essence of his power, and trapped it within four crystals - the very same Four Diamonds which you have now recovered from the brigands who stole from us.")
    npc(eblisId, CALM_TALK, "The four pillars surrounding the structure are keeping the containment spell intact. By placing a diamond into each, you will breach the magical defenses and begin to restore Azzanadra's power, and be able to enter the structure.")
    npc(eblisId, HAPPY_TALKING, "Go, place the diamonds, and free my lord Azzanadra!")
    npc(eblisId, CALM_TALK, "The path will be hard, for his prison is full of traps and danger to prevent his rescue, but he will reward you beyond your wildest dreams when freed!") {
        player.setQuestStage(Quest.DESERT_TREASURE, STAGE_IFTAH_YA_SIMSIM)
    }
}

private fun DialogueBuilder.hurryToPillars(eblisId: Int) {
    npc(eblisId, CHEERFUL, "Quickly... After all these centuries, Lord Azzanadra is nearly free! You must spare no time, place the Diamonds upon the pillars and enter the pyramid so that you may free him!")
}

private fun DialogueBuilder.pillarsReport(eblisId: Int) {
    player(CALM_TALK, "I used the Four Diamonds on the pillars surrounding the pyramid...")
    npc(eblisId, CONFUSED, "Yes? And what happened?")
    player(CONFUSED, "Er... they changed colour.")
    npc(eblisId, CALM_TALK, "Ah, that was the power being re-absorbed back into them. That should have been sufficient to break the magic seal preventing anyone exiting or entering the structure. Quickly, you must let Lord Azzanadra know that he is free again after these many years!")
    npc(eblisId, CALM_TALK, "Please, when you have freed Azzanadra, let me know what words he has for his faithful followers...")
}
