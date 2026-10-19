package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj

private enum class MoonshineStep(val vial: Int, val ingredient: Int, val product: Int, val message: String) {
    STRAIN(Obj.obj_19833, Obj.obj_19840, Obj.obj_19834, "You use the evil leaves as a strainer for the drink."),
    BLOOD(Obj.obj_19834, Obj.obj_19838, Obj.obj_19835, "You strain the blood through the leaves. It pools at the bottom of the vial in an unpleasant manner."),
    FEAR(Obj.obj_19835, Obj.obj_19841, Obj.obj_19836, "You strain the fear through the leaves. It mixes with the blood and gives the drink an orange colour."),
    BONES(Obj.obj_19836, Obj.obj_19837, Obj.obj_19842, "You sprinkle the bones on top of the mixture. It has an instant reaction and takes on a silvery sheen.");

    companion object {
        val byVial = entries.associateBy { it.vial }
        val vials = entries.map { it.vial }.toIntArray()
        val ingredients = entries.map { it.ingredient }.toIntArray()
    }
}

private val BUFFET_TABLES = arrayOf<Any>(
    Loc.hw10_table_01, Loc.hw10_table_02, Loc.hw10_table_03, Loc.hw10_table_04, Loc.hw10_table_05,
    Loc.hw10_table_06, Loc.hw10_table_07, Loc.hw10_table_08, Loc.hw10_table_09
)

private const val EMPTY_VIAL = Obj.obj_19833
private const val FAILED_MOONSHINE = Obj.obj_19839
private const val INGREDIENTS_ICON = Obj.obj_19843
private const val MOONSHINE = Obj.obj_19842

@ServerStartupEvent
fun mapMoonshine() {
    onObjectClick(*BUFFET_TABLES) { (player) -> searchBuffet(player) }

    onItemOnItem(MoonshineStep.vials, MoonshineStep.ingredients) { e ->
        val (vial, ingredient) = if (e.item1.id in MoonshineStep.vials) e.item1 to e.item2 else e.item2 to e.item1
        val step = MoonshineStep.byVial.getValue(vial.id)
        if (ingredient.id == MoonshineStep.STRAIN.ingredient && step != MoonshineStep.STRAIN) {
            e.player.sendMessage("The vial is already strained with leaves; adding more won't make a difference.")
            return@onItemOnItem
        }
        e.player.inventory.deleteItem(ingredient.id, 1)
        if (ingredient.id != step.ingredient) {
            e.player.inventory.replace(vial.id, FAILED_MOONSHINE)
            e.player.startConversation {
                player(HeadE.SKEPTICAL_HEAD_SHAKE, "Going by the smell, that really didn't work.")
                emptyFailedMoonshine(e.player)
            }
            return@onItemOnItem
        }
        e.player.inventory.replace(vial.id, step.product)
        if (step == MoonshineStep.BONES)
            e.player.startConversation { item(step.product, step.message) }
        else
            e.player.sendMessage(step.message)
    }

    onItemClick(FAILED_MOONSHINE, options = arrayOf("Empty")) { (player) ->
        player.startConversation { emptyFailedMoonshine(player) }
    }
}

private fun searchBuffet(player: Player) {
    when {
        player.inPartyHouse || player.werewolfState == WerewolfState.MOONSHINE_DELIVERED -> player.startConversation {
            player(HeadE.CALM_TALK, "There's no point getting ingredients now.")
        }
        player.werewolfState == WerewolfState.NOT_ASKED -> player.startConversation {
            player(HeadE.SKEPTICAL, "A collection of food that only the most sinister of creatures would appreciate.")
        }
        player.inventory.containsItem(MOONSHINE) -> player.startConversation {
            player(HeadE.CALM_TALK, "I've already made the moonshine. I should take it to the werewolves.")
        }
        else -> {
            val missing = missingIngredients(player)
            if (missing.isEmpty()) {
                player.startConversation { simple("You already have the ingredients. There's no need to collect any more.") }
                return
            }
            if (player.inventory.freeSlots < missing.size) {
                player.sendMessage("You don't have enough inventory space to carry the ingredients.")
                return
            }
            missing.forEach { player.inventory.addItem(it) }
            player.moonshineIngredientsTaken = true
            player.startConversation {
                item(INGREDIENTS_ICON, "You collect an empty vial, a big bottle of fear, a big bottle of blood, a collection of leaves from an evil tree and some ground bones.")
            }
        }
    }
}

val Player.hasMoonshineIngredients get() = inventory.containsOneItem(*MoonshineStep.vials)

private fun DialogueBuilder.emptyFailedMoonshine(player: Player) = item(EMPTY_VIAL, "You empty the vile liquid out of the vial.", Runnable {
    player.inventory.replace(FAILED_MOONSHINE, EMPTY_VIAL)
})

private fun missingIngredients(player: Player): List<Int> {
    val step = MoonshineStep.entries.firstOrNull { player.inventory.containsItem(it.vial) }
    val needed = when {
        step != null -> MoonshineStep.entries.drop(step.ordinal).map { it.ingredient }
        player.inventory.containsItem(FAILED_MOONSHINE) -> MoonshineStep.ingredients.toList()
        else -> listOf(EMPTY_VIAL) + MoonshineStep.ingredients.toList()
    }
    return needed.filter { !player.inventory.containsItem(it) }
}
