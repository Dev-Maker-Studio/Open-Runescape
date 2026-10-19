package com.rs.game.content.skills.cooking

import com.rs.game.content.skills.util.CreateActionD
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private const val CURRY_LEAVES = 3

private class CookingCombo(val level: Int, val xp: Double, val first: Item, val second: Item, val products: List<Item>, val knife: Boolean = false) {
    fun uses(a: Int, b: Int) = (first.id == a && second.id == b) || (first.id == b && second.id == a)
}

private fun combo(level: Int, xp: Double, first: Int, second: Int, vararg products: Int, knife: Boolean = false) =
    CookingCombo(level, xp, Item(first), Item(second), products.map { Item(it) }, knife)

private fun chop(ingredient: Int, product: Int) = combo(1, 0.0, Obj.bowl_empty, ingredient, product, knife = true)

private val COMBOS = listOf(
    combo(1, 0.0, Obj.pastry_dough, Obj.piedish, Obj.pie_shell),
    combo(10, 0.0, Obj.pie_shell, Obj.redberries, Obj.uncooked_redberry_pie),
    combo(20, 0.0, Obj.pie_shell, Obj.cooked_meat, Obj.uncooked_meat_pie),
    combo(20, 0.0, Obj.pie_shell, Obj.cooked_chicken, Obj.uncooked_meat_pie),
    combo(29, 0.0, Obj.pie_shell, Obj.bucket_compost, Obj.unfinished_mud_pie_1, Obj.bucket_empty),
    combo(29, 0.0, Obj.unfinished_mud_pie_1, Obj.bucket_water, Obj.unfinished_mud_pie_2, Obj.bucket_empty),
    combo(29, 0.0, Obj.unfinished_mud_pie_2, Obj.clay, Obj.uncooked_mud_pie),
    combo(30, 0.0, Obj.pie_shell, Obj.cooking_apple, Obj.uncooked_apple_pie),
    combo(34, 0.0, Obj.pie_shell, Obj.tomato, Obj.unfinished_garden_pie_1),
    combo(34, 0.0, Obj.unfinished_garden_pie_1, Obj.onion, Obj.unfinished_garden_pie_2),
    combo(34, 0.0, Obj.unfinished_garden_pie_2, Obj.cabbage, Obj.uncooked_garden_pie),
    combo(47, 0.0, Obj.pie_shell, Obj.trout, Obj.unfinished_fish_pie_1),
    combo(47, 0.0, Obj.unfinished_fish_pie_1, Obj.cod, Obj.unfinished_fish_pie_2),
    combo(47, 0.0, Obj.unfinished_fish_pie_2, Obj.potato, Obj.uncooked_fish_pie),
    combo(70, 0.0, Obj.pie_shell, Obj.salmon, Obj.unfinished_admiral_pie_1),
    combo(70, 0.0, Obj.unfinished_admiral_pie_1, Obj.tuna, Obj.unfinished_admiral_pie_2),
    combo(70, 0.0, Obj.unfinished_admiral_pie_2, Obj.potato, Obj.uncooked_admiral_pie),
    combo(85, 0.0, Obj.pie_shell, Obj.raw_bear_meat, Obj.unfinished_wild_pie_1),
    combo(85, 0.0, Obj.unfinished_wild_pie_1, Obj.raw_chompy, Obj.unfinished_wild_pie_2),
    combo(85, 0.0, Obj.unfinished_wild_pie_2, Obj.raw_rabbit, Obj.uncooked_wild_pie),
    combo(95, 0.0, Obj.pie_shell, Obj.strawberry, Obj.unfinished_summer_pie_1),
    combo(95, 0.0, Obj.unfinished_summer_pie_1, Obj.watermelon, Obj.unfinished_summer_pie_2),
    combo(95, 0.0, Obj.unfinished_summer_pie_2, Obj.cooking_apple, Obj.uncooked_summer_pie),

    combo(35, 0.0, Obj.tomato, Obj.pizza_base, Obj.incomplete_pizza),
    combo(35, 0.0, Obj.incomplete_pizza, Obj.cheese, Obj.uncooked_pizza),
    combo(45, 26.0, Obj.plain_pizza, Obj.cooked_meat, Obj.meat_pizza),
    combo(45, 26.0, Obj.plain_pizza, Obj.cooked_chicken, Obj.meat_pizza),
    combo(55, 39.0, Obj.plain_pizza, Obj.anchovies, Obj.anchovie_pizza),
    combo(65, 45.0, Obj.plain_pizza, Obj.pineapple_ring, Obj.pineapple_pizza),
    combo(65, 45.0, Obj.plain_pizza, Obj.pineapple_chunks, Obj.pineapple_pizza),

    combo(25, 0.0, Obj.bowl_water, Obj.potato, Obj.stew1),
    combo(25, 0.0, Obj.stew1, Obj.cooked_meat, Obj.uncooked_stew),
    combo(25, 0.0, Obj.stew1, Obj.cooked_chicken, Obj.uncooked_stew),
    CookingCombo(60, 0.0, Item(Obj.uncooked_stew), Item(Obj.curry_leaf, CURRY_LEAVES), listOf(Item(Obj.uncooked_curry))),
    combo(60, 0.0, Obj.uncooked_stew, Obj.spicespot, Obj.uncooked_curry),

    chop(Obj.garlic, Obj.bowl_garlic),
    chop(Obj.onion, Obj.bowl_onion),
    chop(Obj.bittercap_mushroom, Obj.bowl_mushroom_raw),
    chop(Obj.tuna, Obj.bowl_tuna),
    chop(Obj.cooked_meat, Obj.bowl_carne),
    combo(13, 0.0, Obj.bowl_empty, Obj.egg, Obj.bowl_egg_raw),
    combo(1, 0.0, Obj.bowl_empty, Obj.sweetcorn_cooked, Obj.obj_7088),
    combo(9, 25.0, Obj.bowl_garlic, Obj.gnome_spice, Obj.bowl_chilli),
    combo(11, 0.0, Obj.bowl_chilli, Obj.cooked_meat, Obj.bowl_chillipluscarne, knife = true),
    combo(11, 0.0, Obj.bowl_chilli, Obj.bowl_carne, Obj.bowl_chillipluscarne, Obj.bowl_empty),
    combo(23, 0.0, Obj.bowl_egg_scrambled, Obj.tomato, Obj.bowl_eggplustomato),
    combo(57, 0.0, Obj.bowl_mushroom_fried, Obj.bowl_onion_fried, Obj.bowl_mushroomplusonion, Obj.bowl_empty),
    combo(67, 0.0, Obj.bowl_tuna, Obj.sweetcorn_cooked, Obj.bowl_tunaplussweetcorn),
    combo(67, 0.0, Obj.bowl_tuna, Obj.obj_7088, Obj.bowl_tunaplussweetcorn, Obj.bowl_empty),
    combo(67, 0.0, Obj.obj_7088, Obj.tuna, Obj.bowl_tunaplussweetcorn, knife = true),

    combo(39, 40.0, Obj.potato_baked, Obj.pot_of_butter, Obj.potato_butter),
    combo(41, 15.0, Obj.potato_butter, Obj.bowl_chillipluscarne, Obj.potato_chillipluscarne, Obj.bowl_empty),
    combo(47, 40.0, Obj.potato_butter, Obj.cheese, Obj.potato_cheese),
    combo(51, 45.0, Obj.potato_butter, Obj.bowl_eggplustomato, Obj.potato_eggplustomato),
    combo(64, 55.0, Obj.potato_butter, Obj.bowl_mushroomplusonion, Obj.potato_mushroomplusonion),
    combo(68, 10.0, Obj.potato_butter, Obj.bowl_tunaplussweetcorn, Obj.potato_tunaplussweetcorn, Obj.bowl_empty),

    combo(1, 0.0, Obj.rand_potato, Obj.rand_mushroom_a, Obj.rand_potato_with_mushroom_a),
    combo(1, 0.0, Obj.rand_potato, Obj.rand_mushroom_b, Obj.rand_potato_with_mushroom_b),
    combo(3, 0.0, Obj.rand_potato, Obj.rand_fish_1, Obj.rand_potato_with_fish_1),
    combo(6, 0.0, Obj.rand_potato_with_fish_1, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_1_and_mushroom_a),
    combo(9, 0.0, Obj.rand_potato_with_fish_1, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_1_and_mushroom_b),
    combo(13, 0.0, Obj.rand_potato, Obj.rand_fish_2, Obj.rand_potato_with_fish_2),
    combo(16, 0.0, Obj.rand_potato_with_fish_2, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_2_and_mushroom_a),
    combo(19, 0.0, Obj.rand_potato_with_fish_2, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_2_and_mushroom_b),
    combo(23, 0.0, Obj.rand_potato, Obj.rand_fish_3, Obj.rand_potato_with_fish_3),
    combo(26, 0.0, Obj.rand_potato_with_fish_3, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_3_and_mushroom_a),
    combo(29, 0.0, Obj.rand_potato_with_fish_3, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_3_and_mushroom_b),
    combo(33, 0.0, Obj.rand_potato, Obj.rand_fish_4, Obj.rand_potato_with_fish_4),
    combo(36, 0.0, Obj.rand_potato_with_fish_4, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_4_and_mushroom_a),
    combo(39, 0.0, Obj.rand_potato_with_fish_4, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_4_and_mushroom_b),
    combo(43, 0.0, Obj.rand_potato, Obj.rand_fish_5, Obj.rand_potato_with_fish_5),
    combo(46, 0.0, Obj.rand_potato_with_fish_5, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_5_and_mushroom_a),
    combo(49, 0.0, Obj.rand_potato_with_fish_5, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_5_and_mushroom_b),
    combo(53, 0.0, Obj.rand_potato, Obj.rand_fish_6, Obj.rand_potato_with_fish_6),
    combo(56, 0.0, Obj.rand_potato_with_fish_6, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_6_and_mushroom_a),
    combo(59, 0.0, Obj.rand_potato_with_fish_6, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_6_and_mushroom_b),
    combo(63, 0.0, Obj.rand_potato, Obj.rand_fish_7, Obj.rand_potato_with_fish_7),
    combo(66, 0.0, Obj.rand_potato_with_fish_7, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_7_and_mushroom_a),
    combo(69, 0.0, Obj.rand_potato_with_fish_7, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_7_and_mushroom_b),
    combo(73, 0.0, Obj.rand_potato, Obj.rand_fish_8, Obj.rand_potato_with_fish_8),
    combo(76, 0.0, Obj.rand_potato_with_fish_8, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_8_and_mushroom_a),
    combo(79, 0.0, Obj.rand_potato_with_fish_8, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_8_and_mushroom_b),
    combo(83, 0.0, Obj.rand_potato, Obj.rand_fish_9, Obj.rand_potato_with_fish_9),
    combo(86, 0.0, Obj.rand_potato_with_fish_9, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_9_and_mushroom_a),
    combo(89, 0.0, Obj.rand_potato_with_fish_9, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_9_and_mushroom_b),
    combo(93, 0.0, Obj.rand_potato, Obj.rand_fish_10, Obj.rand_potato_with_fish_10),
    combo(96, 0.0, Obj.rand_potato_with_fish_10, Obj.rand_mushroom_a, Obj.rand_potato_with_fish_10_and_mushroom_a),
    combo(99, 0.0, Obj.rand_potato_with_fish_10, Obj.rand_mushroom_b, Obj.rand_potato_with_fish_10_and_mushroom_b),
)

object CookingCombos {
    @JvmStatic
    fun handleCombos(player: Player, used: Item, usedWith: Item): Boolean {
        val combo = COMBOS.firstOrNull { it.uses(used.id, usedWith.id) } ?: return false
        val product = ItemDefinitions.getDefs(combo.products.first().id).name.lowercase()
        when {
            player.skills.getLevel(Constants.COOKING) < combo.level -> player.sendMessage("You need a Cooking level of ${combo.level} to make that.")
            combo.knife && !player.inventory.containsItem(Obj.knife) -> player.sendMessage("You need a knife to make $product.")
            player.inventory.containsItems(combo.first, combo.second) ->
                player.startConversation(CreateActionD(player, arrayOf(arrayOf(combo.first, combo.second)), arrayOf(combo.products.toTypedArray()), doubleArrayOf(combo.xp), intArrayOf(-1), intArrayOf(combo.level), Constants.COOKING, 0))
        }
        return true
    }
}
