package com.rs.game.content.skills.cooking.gnome

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item

private const val BAKE_TICKS = 4
private const val MIN_BAKE_SUCCESS = 0.7
private const val ENOUGH_COLOUR = "<col=00ff00>"

private val COOKERS = arrayOf<Any>("Gnome cooker", "Cooker", "Range", "Cooking range", "Fancy range", "Stove", "Oven", "Clay oven", "Clay Oven")

private val GREY_ICONS = mapOf(
    Obj.vodka to Obj.obj_9491,
    Obj.lemon to Obj.obj_9492,
    Obj.orange to Obj.obj_9493,
    Obj.equa_leaves to Obj.obj_9494,
    Obj.bucket_milk to Obj.obj_9495,
    Obj.dwellberries to Obj.obj_9496,
    Obj.pineapple to Obj.obj_9497,
    Obj.lime_chunks to Obj.obj_9498,
    Obj.orange_chunks to Obj.obj_9498,
    Obj.pineapple_chunks to Obj.obj_9498,
    Obj.toads_legs to Obj.obj_9499,
    Obj.gnome_spice to Obj.obj_9500,
    Obj.king_worm to Obj.obj_9501,
    Obj.cheese to Obj.obj_9502,
    Obj.tomato to Obj.obj_9503,
    Obj.onion to Obj.obj_9504,
    Obj.cabbage to Obj.obj_9505,
    Obj.potato to Obj.obj_9506,
    Obj.chocolate_bar to Obj.obj_9507,
)

private val EMPTIES = mapOf(Obj.bucket_milk to Obj.bucket_empty, Obj.pot_of_cream to Obj.pot_empty)

private val CLOSE_BUTTONS = mapOf(
    Interface.interface_434 to Component.interface_434.component_434_71,
    Interface.interface_435 to Component.interface_435.component_435_56,
    Interface.interface_436 to Component.interface_436.component_436_95,
    Interface.interface_437 to Component.interface_437.component_437_48,
)

private sealed interface Bake {
    val input: Int
}

private class FirstBake(val mould: GnomeMould) : Bake {
    override val input get() = mould.raw
}

private class SecondBake(val dish: GnomeDish) : Bake {
    override val input get() = dish.halfMade
}

private class Heat(val cocktail: GnomeCocktail, override val input: Int, val output: Int) : Bake

private val BAKES: Map<Int, Bake> = buildMap {
    GnomeMould.entries.forEach { put(it.raw, FirstBake(it)) }
    GnomeDish.entries.forEach { put(it.halfMade, SecondBake(it)) }
    put(Obj.aluft_shaker_choc_saturday_preheat, Heat(GnomeCocktail.CHOC_SATURDAY, Obj.aluft_shaker_choc_saturday_preheat, Obj.aluft_shaker_choc_saturday_heated))
    put(Obj.aluft_shaker_drunk_dragon_preheat, Heat(GnomeCocktail.DRUNK_DRAGON, Obj.aluft_shaker_drunk_dragon_preheat, GnomeCocktail.DRUNK_DRAGON.product))
}

@ServerStartupEvent
fun mapGnomeCooking() {
    onItemOnItem(intArrayOf(Obj.gianne_dough), GnomeMould.byMould.keys.toIntArray()) { e ->
        val mould = GnomeMould.byMould[e.item1.id] ?: GnomeMould.byMould.getValue(e.item2.id)
        e.player.inventory.deleteItem(Obj.gianne_dough, 1)
        e.player.inventory.deleteItem(mould.mould, 1)
        e.player.inventory.addItem(mould.raw, 1)
        e.player.sendMessage("You put the dough into the ${name(mould.mould)}.", true)
    }

    onItemClick(*GnomeMould.byHalfBaked.keys.toTypedArray(), options = arrayOf("Prepare")) { (player, item) ->
        val mould = GnomeMould.byHalfBaked.getValue(item.id)
        player.openGnomeMenu(mould.menu, mould.dishes)
    }

    onItemClick(Obj.cocktail_shaker, options = arrayOf("Mix-cocktail")) { (player) ->
        player.openGnomeMenu(Interface.interface_436, GnomeCocktail.entries)
    }

    for ((menu, close) in CLOSE_BUTTONS)
        onButtonClick(menu) { e ->
            val player = e.player
            if (e.componentId == Utils.componentIdFromHash(close)) {
                player.closeInterfaces()
                return@onButtonClick
            }
            val recipes: List<GnomeRecipe> = GnomeMould.byMenu[menu]?.dishes ?: GnomeCocktail.entries
            val recipe = recipes.firstOrNull { Utils.componentIdFromHash(it.make) == e.componentId } ?: return@onButtonClick
            player.closeInterfaces()
            when (recipe) {
                is GnomeDish -> player.prepare(recipe)
                is GnomeCocktail -> player.mix(recipe)
            }
        }

    onItemOnObject(COOKERS, BAKES.keys.toTypedArray<Any>()) { (player, obj, item) ->
        player.actionManager.setAction(GnomeBaking(obj, BAKES.getValue(item.id), player.inventory.items.getNumberOf(item.id)))
    }

    for (dish in GnomeDish.entries) {
        val unfinished = dish.unfinished ?: continue
        onItemOnItem(intArrayOf(unfinished), dish.garnish.map { it.id }.toIntArray()) { e -> e.player.garnish(dish) }
    }

    onItemClick(*GnomeMould.byBurnt.keys.toTypedArray(), options = arrayOf("Empty Tray", "Empty Tin", "Empty Bowl")) { (player, item) ->
        val mould = GnomeMould.byBurnt.getValue(item.id)
        item.id = mould.mould
        player.inventory.refresh()
        player.sendMessage("You empty the ${name(mould.mould)}.", true)
    }

    onItemClick(*GnomeCocktail.byMixed.keys.toTypedArray(), options = arrayOf("Pour")) { (player, item) ->
        player.pour(GnomeCocktail.byMixed.getValue(item.id))
    }

    onItemClick(Obj.aluft_shaker_choc_saturday_heated, Obj.aluft_shaker_drunk_dragon_prefinish, options = arrayOf("Add-ingreds")) { (player, item) ->
        val cocktail = if (item.id == Obj.aluft_shaker_choc_saturday_heated) GnomeCocktail.CHOC_SATURDAY else GnomeCocktail.DRUNK_DRAGON
        if (!player.hasAll(cocktail.garnish, "finish")) return@onItemClick
        player.consume(cocktail.garnish)
        if (cocktail.style == CocktailStyle.HEATED_THEN_GARNISHED) {
            item.id = cocktail.product
            player.skills.addXp(Constants.COOKING, cocktail.finishXp)
            player.sendMessage("You finish the ${name(cocktail.product)}.", true)
        } else {
            item.id = Obj.aluft_shaker_drunk_dragon_preheat
            player.sendMessage("You add the ingredients. The cocktail now needs heating.", true)
        }
        player.inventory.refresh()
    }
}

private fun name(obj: Int) = ItemDefinitions.getDefs(obj).name.lowercase()

private fun Player.held(obj: Int) = inventory.items.getNumberOf(obj)

private fun Player.openGnomeMenu(menu: Int, recipes: List<GnomeRecipe>) {
    interfaceManager.sendInterface(menu)
    for (recipe in recipes) {
        packets.setIFItem(menu, Utils.componentIdFromHash(recipe.make), if (recipe is GnomeCocktail && !canMake(recipe)) Obj.aluft_coctail_glass_empty_dummy else recipe.product, 1)
        for (ingredient in recipe.ingredients) {
            val held = held(ingredient.obj)
            val enough = held >= ingredient.amount
            packets.setIFItem(menu, Utils.componentIdFromHash(ingredient.icon), if (enough) ingredient.obj else GREY_ICONS[ingredient.obj] ?: ingredient.obj, 1)
            packets.setIFText(ingredient.text, "${if (enough) ENOUGH_COLOUR else ""}${held.coerceAtMost(ingredient.amount)}/${ingredient.amount}")
        }
    }
}

private fun Player.canMake(recipe: GnomeRecipe) = skills.getLevel(Constants.COOKING) >= recipe.level && recipe.ingredients.all { held(it.obj) >= it.amount }

private fun Player.meetsLevel(recipe: GnomeRecipe): Boolean {
    if (skills.getLevel(Constants.COOKING) >= recipe.level) return true
    sendMessage("You need a Cooking level of ${recipe.level} to make ${Utils.addArticle(name(recipe.product))}.")
    return false
}

private fun Player.hasIngredients(recipe: GnomeRecipe): Boolean {
    val missing = recipe.ingredients.firstOrNull { held(it.obj) < it.amount } ?: return true
    sendMessage("You need ${missing.amount} x ${name(missing.obj)} to make ${Utils.addArticle(name(recipe.product))}.")
    return false
}

private fun Player.hasAll(items: List<Item>, purpose: String): Boolean {
    val missing = items.firstOrNull { held(it.id) < it.amount } ?: return true
    sendMessage("You need ${missing.amount} x ${name(missing.id)} to $purpose this.")
    return false
}

private fun Player.consume(items: List<Item>) {
    for (item in items) {
        inventory.deleteItem(item.id, item.amount)
        EMPTIES[item.id]?.let { inventory.addItemDrop(it, item.amount) }
    }
}

private fun Player.prepare(dish: GnomeDish) {
    if (!inventory.containsItem(dish.mould.halfBaked, 1, false)) return
    if (!meetsLevel(dish) || !hasIngredients(dish)) return
    consume(dish.ingredients.map { Item(it.obj, it.amount) })
    inventory.deleteItem(dish.mould.halfBaked, 1)
    inventory.addItem(dish.halfMade, 1)
    skills.addXp(Constants.COOKING, dish.mould.prepareXp)
    sendMessage("You prepare the ${name(dish.product)}. It now needs baking.", true)
}

private fun Player.garnish(dish: GnomeDish) {
    val unfinished = dish.unfinished ?: return
    if (!inventory.containsItem(unfinished, 1, false) || !hasAll(dish.garnish, "finish")) return
    consume(dish.garnish)
    inventory.deleteItem(unfinished, 1)
    inventory.addItem(dish.product, 1)
    skills.addXp(Constants.COOKING, dish.finishXp)
    sendMessage("You finish the ${name(dish.product)}.", true)
}

private fun Player.mix(cocktail: GnomeCocktail) {
    if (!inventory.containsItem(Obj.cocktail_shaker, 1, false)) return
    if (!meetsLevel(cocktail) || !hasIngredients(cocktail)) return
    consume(cocktail.ingredients.map { Item(it.obj, it.amount) })
    inventory.deleteItem(Obj.cocktail_shaker, 1)
    inventory.addItem(cocktail.mixed, 1)
    skills.addXp(Constants.COOKING, GNOME_COCKTAIL_MIX_XP)
    sendMessage("You mix the ingredients in the shaker.", true)
}

private fun Player.pour(cocktail: GnomeCocktail) {
    if (!inventory.containsItem(Obj.cocktail_glass_empty, 1, false)) {
        sendMessage("You need a cocktail glass to pour this into.")
        return
    }
    val poured = when (cocktail.style) {
        CocktailStyle.POURED -> {
            if (!hasAll(cocktail.garnish, "garnish")) return
            consume(cocktail.garnish)
            skills.addXp(Constants.COOKING, cocktail.finishXp)
            cocktail.product
        }
        CocktailStyle.HEATED_THEN_GARNISHED -> Obj.aluft_shaker_choc_saturday_preheat
        CocktailStyle.GARNISHED_THEN_HEATED -> Obj.aluft_shaker_drunk_dragon_prefinish
    }
    inventory.deleteItem(cocktail.mixed, 1)
    inventory.deleteItem(Obj.cocktail_glass_empty, 1)
    inventory.addItem(Obj.cocktail_shaker, 1)
    inventory.addItem(poured, 1)
    sendMessage("You pour the cocktail into the glass.", true)
}

private class GnomeBaking(private val cooker: GameObject, private val bake: Bake, private var quantity: Int) : PlayerAction() {
    override fun start(player: Player): Boolean {
        player.faceObject(cooker)
        return true
    }

    override fun process(player: Player) = quantity > 0 && player.inventory.containsItem(bake.input, 1, false)

    override fun processWithDelay(player: Player): Int {
        quantity--
        player.anim(Seq.human_cooking)
        val cooked = cooked(player)
        player.inventory.deleteItem(bake.input, 1)
        when (bake) {
            is FirstBake -> if (cooked) {
                player.inventory.addItem(bake.mould.halfBaked, 1)
                player.skills.addXp(Constants.COOKING, GNOME_FIRST_BAKE_XP)
            } else player.inventory.addItem(bake.mould.burnt, 1)
            is SecondBake -> if (cooked) {
                val dish = bake.dish
                player.inventory.addItem(dish.unfinished ?: dish.product, 1)
                player.inventory.addItemDrop(dish.mould.mould, 1)
                if (dish.unfinished == null) player.skills.addXp(Constants.COOKING, dish.finishXp)
            } else player.inventory.addItem(bake.dish.mould.burnt, 1)
            is Heat -> {
                player.inventory.addItem(bake.output, 1)
                if (cooked && bake.cocktail.style == CocktailStyle.GARNISHED_THEN_HEATED) player.skills.addXp(Constants.COOKING, bake.cocktail.finishXp)
            }
        }
        player.sendMessage(
            when {
                bake is Heat -> "You heat the cocktail."
                cooked -> "You bake the ${name(bake.input)}."
                else -> "You accidentally burn the ${name(bake.input)}."
            },
            true,
        )
        return if (quantity > 0) BAKE_TICKS else -1
    }

    override fun stop(player: Player) = setActionDelay(player, BAKE_TICKS)

    private fun cooked(player: Player): Boolean {
        val level = player.skills.getLevel(Constants.COOKING)
        return level >= GNOME_BURN_STOP_LEVEL || Math.random() < (level.toDouble() / GNOME_BURN_STOP_LEVEL).coerceAtLeast(MIN_BAKE_SUCCESS)
    }
}
