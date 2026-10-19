package com.rs.game.content.skills.cooking.gnome

import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

const val GNOME_COCKTAIL_MIX_XP = 30.0
const val GNOME_FIRST_BAKE_XP = 30.0
const val GNOME_BURN_STOP_LEVEL = 38

class Ingredient(val obj: Int, val amount: Int, val text: Int, val icon: Int)

interface GnomeRecipe {
    val level: Int
    val make: Int
    val product: Int
    val ingredients: List<Ingredient>
}

enum class GnomeMould(val mould: Int, val raw: Int, val halfBaked: Int, val burnt: Int, val menu: Int, val prepareXp: Double) {
    CRUNCHY(Obj.crunchy_tray, Obj.raw_crunchies, Obj.half_baked_crunchy, Obj.burnt_crunchies, Interface.interface_437, 40.0),
    BATTA(Obj.batta_tin, Obj.raw_batta, Obj.half_baked_batta, Obj.burnt_batta, Interface.interface_434, 40.0),
    BOWL(Obj.gnomebowl_mould, Obj.raw_gnomebowl, Obj.half_baked_bowl, Obj.burnt_gnomebowl, Interface.interface_435, 50.0);

    val dishes by lazy { GnomeDish.entries.filter { it.mould == this } }

    companion object {
        val byMould = entries.associateBy { it.mould }
        val byRaw = entries.associateBy { it.raw }
        val byHalfBaked = entries.associateBy { it.halfBaked }
        val byBurnt = entries.associateBy { it.burnt }
        val byMenu = entries.associateBy { it.menu }
    }
}

enum class GnomeDish(
    val mould: GnomeMould,
    override val level: Int,
    override val make: Int,
    override val product: Int,
    val halfMade: Int,
    val unfinished: Int?,
    val finishXp: Double,
    override val ingredients: List<Ingredient>,
    val garnish: List<Item>,
) : GnomeRecipe {
    TOAD_CRUNCHIES(
        GnomeMould.CRUNCHY, 10, Component.interface_437.component_437_3, Obj.toad_crunchies, Obj.obj_9581, Obj.obj_9582, 30.0,
        listOf(Ingredient(Obj.toads_legs, 2, Component.interface_437.component_437_6, Component.interface_437.component_437_4), Ingredient(Obj.gnome_spice, 1, Component.interface_437.component_437_7, Component.interface_437.component_437_5)),
        listOf(Item(Obj.equa_leaves, 1)),
    ),
    SPICY_CRUNCHIES(
        GnomeMould.CRUNCHY, 12, Component.interface_437.component_437_10, Obj.spicy_crunchies, Obj.obj_9579, Obj.obj_9580, 32.0,
        listOf(Ingredient(Obj.gnome_spice, 1, Component.interface_437.component_437_13, Component.interface_437.component_437_11), Ingredient(Obj.equa_leaves, 2, Component.interface_437.component_437_14, Component.interface_437.component_437_12)),
        listOf(Item(Obj.gnome_spice, 1)),
    ),
    WORM_CRUNCHIES(
        GnomeMould.CRUNCHY, 14, Component.interface_437.component_437_17, Obj.worm_crunchies, Obj.obj_9583, Obj.obj_9584, 34.0,
        listOf(Ingredient(Obj.gnome_spice, 1, Component.interface_437.component_437_21, Component.interface_437.component_437_18), Ingredient(Obj.king_worm, 2, Component.interface_437.component_437_22, Component.interface_437.component_437_19), Ingredient(Obj.equa_leaves, 1, Component.interface_437.component_437_23, Component.interface_437.component_437_20)),
        listOf(Item(Obj.gnome_spice, 1)),
    ),
    CHOCCHIP_CRUNCHIES(
        GnomeMould.CRUNCHY, 16, Component.interface_437.component_437_26, Obj.chocchip_crunchies, Obj.obj_9577, Obj.obj_9578, 36.0,
        listOf(Ingredient(Obj.chocolate_bar, 2, Component.interface_437.component_437_29, Component.interface_437.component_437_27), Ingredient(Obj.gnome_spice, 1, Component.interface_437.component_437_30, Component.interface_437.component_437_28)),
        listOf(Item(Obj.chocolate_dust, 1)),
    ),
    FRUIT_BATTA(
        GnomeMould.BATTA, 25, Component.interface_434.component_434_3, Obj.fruit_batta, Obj.obj_9480, Obj.obj_9481, 80.0,
        listOf(Ingredient(Obj.equa_leaves, 4, Component.interface_434.component_434_8, Component.interface_434.component_434_4), Ingredient(Obj.lime_chunks, 1, Component.interface_434.component_434_9, Component.interface_434.component_434_5), Ingredient(Obj.orange_chunks, 1, Component.interface_434.component_434_10, Component.interface_434.component_434_6), Ingredient(Obj.pineapple_chunks, 1, Component.interface_434.component_434_11, Component.interface_434.component_434_7)),
        listOf(Item(Obj.gnome_spice, 1)),
    ),
    TOAD_BATTA(
        GnomeMould.BATTA, 26, Component.interface_434.component_434_14, Obj.toad_batta, Obj.obj_9482, null, 82.0,
        listOf(Ingredient(Obj.equa_leaves, 1, Component.interface_434.component_434_19, Component.interface_434.component_434_15), Ingredient(Obj.toads_legs, 1, Component.interface_434.component_434_20, Component.interface_434.component_434_16), Ingredient(Obj.gnome_spice, 1, Component.interface_434.component_434_21, Component.interface_434.component_434_17), Ingredient(Obj.cheese, 1, Component.interface_434.component_434_22, Component.interface_434.component_434_18)),
        emptyList(),
    ),
    WORM_BATTA(
        GnomeMould.BATTA, 27, Component.interface_434.component_434_25, Obj.worm_batta, Obj.obj_9485, Obj.obj_9486, 84.0,
        listOf(Ingredient(Obj.gnome_spice, 1, Component.interface_434.component_434_29, Component.interface_434.component_434_26), Ingredient(Obj.king_worm, 1, Component.interface_434.component_434_30, Component.interface_434.component_434_27), Ingredient(Obj.cheese, 1, Component.interface_434.component_434_31, Component.interface_434.component_434_28)),
        listOf(Item(Obj.equa_leaves, 1)),
    ),
    VEGETABLE_BATTA(
        GnomeMould.BATTA, 28, Component.interface_434.component_434_34, Obj.vegetable_batta, Obj.obj_9483, Obj.obj_9484, 86.0,
        listOf(Ingredient(Obj.tomato, 2, Component.interface_434.component_434_40, Component.interface_434.component_434_35), Ingredient(Obj.onion, 1, Component.interface_434.component_434_41, Component.interface_434.component_434_36), Ingredient(Obj.cabbage, 1, Component.interface_434.component_434_42, Component.interface_434.component_434_37), Ingredient(Obj.dwellberries, 1, Component.interface_434.component_434_43, Component.interface_434.component_434_38), Ingredient(Obj.cheese, 1, Component.interface_434.component_434_44, Component.interface_434.component_434_39)),
        listOf(Item(Obj.equa_leaves, 1)),
    ),
    CHEESE_AND_TOMATO_BATTA(
        GnomeMould.BATTA, 29, Component.interface_434.component_434_47, Obj.cheeseplustom_batta, Obj.obj_9478, Obj.obj_9479, 88.0,
        listOf(Ingredient(Obj.tomato, 1, Component.interface_434.component_434_50, Component.interface_434.component_434_48), Ingredient(Obj.cheese, 1, Component.interface_434.component_434_51, Component.interface_434.component_434_49)),
        listOf(Item(Obj.equa_leaves, 1)),
    ),
    WORM_HOLE(
        GnomeMould.BOWL, 30, Component.interface_435.component_435_3, Obj.worm_hole, Obj.obj_9563, Obj.obj_9564, 90.0,
        listOf(Ingredient(Obj.king_worm, 4, Component.interface_435.component_435_7, Component.interface_435.component_435_4), Ingredient(Obj.onion, 2, Component.interface_435.component_435_8, Component.interface_435.component_435_5), Ingredient(Obj.gnome_spice, 1, Component.interface_435.component_435_9, Component.interface_435.component_435_6)),
        listOf(Item(Obj.equa_leaves, 1)),
    ),
    VEG_BALL(
        GnomeMould.BOWL, 35, Component.interface_435.component_435_12, Obj.veg_ball, Obj.obj_9561, Obj.obj_9562, 95.0,
        listOf(Ingredient(Obj.onion, 2, Component.interface_435.component_435_16, Component.interface_435.component_435_13), Ingredient(Obj.potato, 2, Component.interface_435.component_435_17, Component.interface_435.component_435_14), Ingredient(Obj.gnome_spice, 1, Component.interface_435.component_435_18, Component.interface_435.component_435_15)),
        listOf(Item(Obj.equa_leaves, 1)),
    ),
    TANGLED_TOADS_LEGS(
        GnomeMould.BOWL, 40, Component.interface_435.component_435_21, Obj.tangled_toads_legs, Obj.obj_9558, null, 105.0,
        listOf(Ingredient(Obj.toads_legs, 4, Component.interface_435.component_435_27, Component.interface_435.component_435_22), Ingredient(Obj.cheese, 2, Component.interface_435.component_435_28, Component.interface_435.component_435_23), Ingredient(Obj.equa_leaves, 2, Component.interface_435.component_435_29, Component.interface_435.component_435_24), Ingredient(Obj.gnome_spice, 1, Component.interface_435.component_435_30, Component.interface_435.component_435_25), Ingredient(Obj.dwellberries, 1, Component.interface_435.component_435_31, Component.interface_435.component_435_26)),
        emptyList(),
    ),
    CHOCOLATE_BOMB(
        GnomeMould.BOWL, 42, Component.interface_435.component_435_34, Obj.chocolate_bomb, Obj.obj_9559, Obj.obj_9560, 110.0,
        listOf(Ingredient(Obj.chocolate_bar, 4, Component.interface_435.component_435_37, Component.interface_435.component_435_35), Ingredient(Obj.equa_leaves, 1, Component.interface_435.component_435_38, Component.interface_435.component_435_36)),
        listOf(Item(Obj.chocolate_dust, 1), Item(Obj.pot_of_cream, 2)),
    );
    companion object {
        val byHalfMade = entries.associateBy { it.halfMade }
        val byUnfinished = entries.filter { it.unfinished != null }.associateBy { it.unfinished!! }
    }
}

enum class CocktailStyle { POURED, HEATED_THEN_GARNISHED, GARNISHED_THEN_HEATED }

enum class GnomeCocktail(
    override val level: Int,
    override val make: Int,
    val mixed: Int,
    override val product: Int,
    val finishXp: Double,
    val style: CocktailStyle,
    override val ingredients: List<Ingredient>,
    val garnish: List<Item>,
) : GnomeRecipe {
    FRUIT_BLAST(
        6, Component.interface_436.component_436_23, Obj.aluft_shaker_fruit_blast, Obj.obj_2084, 20.0, CocktailStyle.POURED,
        listOf(Ingredient(Obj.pineapple, 1, Component.interface_436.component_436_27, Component.interface_436.component_436_24), Ingredient(Obj.lemon, 1, Component.interface_436.component_436_28, Component.interface_436.component_436_25), Ingredient(Obj.orange, 1, Component.interface_436.component_436_29, Component.interface_436.component_436_26)),
        listOf(Item(Obj.lemon_slices, 1)),
    ),
    PINEAPPLE_PUNCH(
        8, Component.interface_436.component_436_32, Obj.aluft_shaker_pineapple_punch, Obj.obj_2048, 40.0, CocktailStyle.POURED,
        listOf(Ingredient(Obj.pineapple, 2, Component.interface_436.component_436_36, Component.interface_436.component_436_33), Ingredient(Obj.orange, 1, Component.interface_436.component_436_37, Component.interface_436.component_436_35), Ingredient(Obj.lemon, 1, Component.interface_436.component_436_38, Component.interface_436.component_436_34)),
        listOf(Item(Obj.lime_chunks, 1), Item(Obj.pineapple_chunks, 1), Item(Obj.orange_slices, 1)),
    ),
    WIZARD_BLIZZARD(
        18, Component.interface_436.component_436_3, Obj.aluft_shaker_wizzard_blizzard, Obj.obj_2054, 80.0, CocktailStyle.POURED,
        listOf(Ingredient(Obj.vodka, 2, Component.interface_436.component_436_9, Component.interface_436.component_436_4), Ingredient(Obj.gin, 1, Component.interface_436.component_436_10, Component.interface_436.component_436_5), Ingredient(Obj.orange, 1, Component.interface_436.component_436_11, Component.interface_436.component_436_6), Ingredient(Obj.lime, 1, Component.interface_436.component_436_12, Component.interface_436.component_436_7), Ingredient(Obj.lemon, 1, Component.interface_436.component_436_13, Component.interface_436.component_436_8)),
        listOf(Item(Obj.pineapple_chunks, 1), Item(Obj.lime_slices, 1)),
    ),
    SHORT_GREEN_GUY(
        20, Component.interface_436.component_436_16, Obj.aluft_shaker_sgg, Obj.obj_2080, 90.0, CocktailStyle.POURED,
        listOf(Ingredient(Obj.vodka, 1, Component.interface_436.component_436_19, Component.interface_436.component_436_17), Ingredient(Obj.lime, 3, Component.interface_436.component_436_20, Component.interface_436.component_436_18)),
        listOf(Item(Obj.lime_slices, 1), Item(Obj.equa_leaves, 1)),
    ),
    DRUNK_DRAGON(
        32, Component.interface_436.component_436_41, Obj.aluft_shaker_drunk_dragon, Obj.obj_2092, 130.0, CocktailStyle.GARNISHED_THEN_HEATED,
        listOf(Ingredient(Obj.vodka, 1, Component.interface_436.component_436_45, Component.interface_436.component_436_42), Ingredient(Obj.gin, 1, Component.interface_436.component_436_46, Component.interface_436.component_436_43), Ingredient(Obj.dwellberries, 1, Component.interface_436.component_436_47, Component.interface_436.component_436_44)),
        listOf(Item(Obj.pineapple_chunks, 1), Item(Obj.pot_of_cream, 1)),
    ),
    CHOC_SATURDAY(
        33, Component.interface_436.component_436_50, Obj.aluft_shaker_choc_saturday, Obj.obj_2074, 140.0, CocktailStyle.HEATED_THEN_GARNISHED,
        listOf(Ingredient(Obj.whisky, 1, Component.interface_436.component_436_55, Component.interface_436.component_436_51), Ingredient(Obj.equa_leaves, 1, Component.interface_436.component_436_56, Component.interface_436.component_436_52), Ingredient(Obj.chocolate_bar, 1, Component.interface_436.component_436_57, Component.interface_436.component_436_53), Ingredient(Obj.bucket_milk, 1, Component.interface_436.component_436_58, Component.interface_436.component_436_54)),
        listOf(Item(Obj.chocolate_dust, 1), Item(Obj.pot_of_cream, 1)),
    ),
    BLURBERRY_SPECIAL(
        37, Component.interface_436.component_436_61, Obj.aluft_shaker_blurberry_special, Obj.obj_2064, 150.0, CocktailStyle.POURED,
        listOf(Ingredient(Obj.vodka, 1, Component.interface_436.component_436_67, Component.interface_436.component_436_62), Ingredient(Obj.brandy, 1, Component.interface_436.component_436_68, Component.interface_436.component_436_63), Ingredient(Obj.gin, 1, Component.interface_436.component_436_69, Component.interface_436.component_436_64), Ingredient(Obj.orange, 1, Component.interface_436.component_436_70, Component.interface_436.component_436_65), Ingredient(Obj.lemon, 2, Component.interface_436.component_436_71, Component.interface_436.component_436_66)),
        listOf(Item(Obj.lemon_chunks, 1), Item(Obj.orange_chunks, 1), Item(Obj.equa_leaves, 1), Item(Obj.lime_slices, 1)),
    );
    companion object {
        val byMixed = entries.associateBy { it.mixed }
    }
}
