package com.rs.game.content.skills.cooking

import com.rs.game.content.Effect
import com.rs.game.content.ItemConstants
import com.rs.game.content.skills.dungeoneering.KinshipPerk
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.model.Animation
import org.darkan.core.model.Item
import kotlin.math.roundToInt

private const val MEDIC_BASE = 1.2
private const val MEDIC_PER_TIER = 0.03

object Foods {
    @JvmField
    val EAT_ANIM = Animation(Seq.human_eat)

    @JvmStatic
    fun eat(player: Player, item: Item, slot: Int, givenFrom: Player?): Boolean {
        val food = Food.forId(item.id) ?: return false
        if (!player.canEat() || !player.controllerManager.canEat(food)) return true
        if (food.heal < 0) {
            player.sendMessage("I'm not going to eat that!")
            return true
        }
        player.sendMessage("You eat the ${item.name.lowercase()}.", true)
        player.incrementCount("Food eaten")
        player.anim(EAT_ANIM)
        player.addFoodDelay(if (food.ids.size > 1) 2 else 3)
        player.actionManager.actionDelay += 3
        val replace = Item(item.id, item.amount)
        if (replace.definitions.isStackable()) replace.amount -= 1 else replace.id = food.getReplaceIdFor(item.id)
        player.inventory.items.set(slot, if (replace.id <= 0 || replace.amount <= 0) null else replace)
        player.inventory.refresh(slot)
        val hitpoints = player.hitpoints
        if (ItemConstants.isDungItem(item.id)) {
            val medic = givenFrom?.dungManager?.takeIf { it.activePerk == KinshipPerk.MEDIC }
            val healed = if (medic == null) food.heal else (food.heal * (MEDIC_BASE + medic.getKinshipTier(KinshipPerk.MEDIC) * MEDIC_PER_TIER)).toInt()
            player.applyHit(Hit(player, healed, HitLook.HEALED_DAMAGE))
        } else player.heal(food.heal)
        if (player.hitpoints > hitpoints) player.sendMessage("It heals some health.")
        player.inventory.refresh()
        food.effect?.invoke(player)
        player.soundEffect(Sound.sound_2393, false)
        return true
    }

    @JvmStatic
    fun isConsumable(item: Item) = Food.forId(item.id) != null

    enum class Food(val ids: IntArray, private val container: Int, val heal: Int, val effect: ((Player) -> Unit)? = null) {
        DELIVERED_TOAD_CRUNCHIES(Obj.aluft_toad_crunchies, 80),
        DELIVERED_SPICY_CRUNCHIES(Obj.aluft_spicy_crunchies, 70),
        DELIVERED_WORM_CRUNCHIES(Obj.aluft_worm_crunchies, 80),
        DELIVERED_CHOCCHIP_CRUNCHIES(Obj.obj_9544, 70),
        DELIVERED_FRUIT_BATTA(Obj.aluft_fruit_batta, 110),
        DELIVERED_TOAD_BATTA(Obj.aluft_toad_batta, 120),
        DELIVERED_WORM_BATTA(Obj.aluft_worm_batta, 120),
        DELIVERED_VEGETABLE_BATTA(Obj.aluft_vegetable_batta, 110),
        DELIVERED_CHEESE_AND_TOMATO_BATTA(Obj.aluft_cheeseplustom_batta, 110),
        DELIVERED_WORM_HOLE(Obj.aluft_worm_hole, 120),
        DELIVERED_VEG_BALL(Obj.aluft_veg_ball, 120),
        DELIVERED_TANGLED_TOAD_LEGS(Obj.aluft_tangled_toads_legs, 150),
        DELIVERED_CHOCOLATE_BOMB(Obj.aluft_chocolate_bomb, 150),
        ACAI(Obj.brsum_acai, 50),
        ADMIRAL_PIE(intArrayOf(Obj.admiral_pie, Obj.half_admiral_pie), Obj.piedish, 80, { it.skills.adjustStat(5, 0.0, Constants.FISHING) }),
        AMPHIBIOUS_FRUIT(Obj.jaro_fruit_amphibious, 150),
        ANCHOVY(Obj.anchovies, 10),
        ANCHOVY_PIZZA(intArrayOf(Obj.anchovie_pizza, Obj.obj_2299), 90),
        APPLE_PIE(intArrayOf(Obj.apple_pie, Obj.half_an_apple_pie), Obj.piedish, 70),
        AQUATIC_FRUIT(Obj.jaro_fruit_aquatic, 150),
        BAGUETTE(Obj.baguette, 60),
        BAKED_CAVE_POTATO(Obj.rand_potato, 20),
        BAKED_POTATO(Obj.potato_baked, 40),
        BANANA(Obj.banana, 20),
        BANANA_STEW(Obj.mm_banana_stew, 110),
        BARON_SHARK(Obj.golden_shark, 200, { it.addEffect(Effect.BARON_SHARK, Ticks.fromSeconds(12).toLong()) }),
        BASS(Obj.bass, 130),
        BAT_SHISH(Obj.dorgesh_bat_shish, 20),
        BISCUITS(Obj.obj_19467, 20),
        BLACK_MUSHROOM(Obj.golem_mushroom, 0, { it.sendMessage("Eugh! It tastes horrible, and stains your fingers black.") }),
        BLUE_CRAB(Obj.rand_fish_9, 220),
        BLUE_CRAB_POTATO(Obj.rand_potato_with_fish_9, 250),
        BLUE_CRAB_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_9_and_mushroom_b, 340),
        BLUE_CRAB_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_9_and_mushroom_a, 280),
        BLUE_SWEETS(Obj.obj_4558, 2),
        BOULDABASS(Obj.rand_fish_7, 170),
        BOULDABASS_POTATO(Obj.rand_potato_with_fish_7, 200),
        BOULDABASS_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_7_and_mushroom_b, 290),
        BOULDABASS_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_7_and_mushroom_a, 230),
        BREAD(Obj.bread, 50),
        BUTTON_MUSHROOM(Obj.sum1_button_mushroom, 10),
        CABBAGE(Obj.cabbage, 10, { it.sendMessage("You don't really like it much.", true) }),
        CAMOUFLAGED_FRUIT(Obj.jaro_fruit_camouflaged, 150),
        CANNIBAL_FRUIT(Obj.jaro_fruit_cannibal, 150),
        CARRION_FRUIT(Obj.jaro_fruit_carrion, 150),
        CAVEFISH(Obj.lrc_cavefish_cooked, 220),
        CAVE_EEL(Obj.cave_eel, 110),
        CAVE_MORAY(Obj.rand_fish_10, 250),
        CAVE_MORAY_POTATO(Obj.rand_potato_with_fish_10, 280),
        CAVE_NIGHTSHADE(Obj.nightshade, 0, {
            it.applyHit(Hit(it, 15, HitLook.POISON_DAMAGE))
            it.sendMessage("Ahhhh! What have I done")
        }),
        CAVIAR(Obj.brut_caviar, 50),
        CELEBRATION_CAKE_1(Obj.celebration_cake, 20),
        CELEBRATION_CAKE_2(Obj.celebration_cake_candles, 20),
        CELEBRATION_CAKE_3(Obj.celebration_cake_candles_lit, 20),
        CHEESE(Obj.cheese, 20),
        CHEESEPTOM_BATTA(Obj.cheeseplustom_batta, 110),
        CHEESE_WHEEL(Obj.cheese_wheel, 20),
        CHICKEN(Obj.cooked_chicken, 30),
        CHILLI_CON_CARNE(intArrayOf(Obj.bowl_chillipluscarne), Obj.bowl_empty, 50),
        CHILLI_POTATO(Obj.potato_chillipluscarne, 140),
        CHOCCHIP_CRUNCHIES(Obj.chocchip_crunchies, 70),
        CHOCOLATE_BAR(Obj.chocolate_bar, 30),
        CHOCOLATE_BOMB(Obj.chocolate_bomb, 150),
        CHOCOLATE_CAKE(intArrayOf(Obj.chocolate_cake, Obj.partial_chocolate_cake, Obj.chocolate_slice), 50),
        CAKE(intArrayOf(Obj.cake, Obj.partial_cake, Obj.cake_slice), 40),
        CHOCOLATE_DROP(Obj.obj_14083, 30),
        CHOCOLATE_EGG_1(Obj.obj_12646, 10),
        CHOCOLATE_EGG(Obj.obj_12648, 10),
        CHOCOLATE_KEBBIT(Obj.obj_11026, -1),
        CHOCOTREAT(Obj.obj_24148, 80),
        CHOC_ICE(Obj.obj_6794, 70),
        CHOMPY(Obj.cooked_chompy, 60),
        CHOPPED_GARLIC(intArrayOf(Obj.bowl_garlic), Obj.bowl_empty, 0),
        CHOPPED_ONION(intArrayOf(Obj.bowl_onion), Obj.bowl_empty, 10, ::onionTears),
        CHOPPED_TOMATO(intArrayOf(Obj.bowl_tomato), Obj.bowl_empty, 50),
        CHOPPED_TUNA(intArrayOf(Obj.bowl_tuna), Obj.bowl_empty, 50),
        COATED_FROGS_LEGS(Obj.dorgesh_crispy_froglegs, 20),
        COD(Obj.cod, 70),
        COMMON_FRUIT(Obj.jaro_fruit_common, 150),
        COOKED_CHICKEN(Obj.cooked_chicken, 30),
        COOKED_CHOMPY(Obj.cooked_chompy, 30),
        COOKED_CRAB_MEAT_1(Obj.hundred_pirate_giant_crab_meat_5, 10),
        COOKED_CRAB_MEAT_2(Obj.hundred_pirate_giant_crab_meat_4, 10),
        COOKED_CRAB_MEAT_3(Obj.hundred_pirate_giant_crab_meat_3, 10),
        COOKED_CRAB_MEAT_4(Obj.hundred_pirate_giant_crab_meat_2, 10),
        COOKED_CRAB_MEAT_5(Obj.hundred_pirate_giant_crab_meat_1, 10),
        COOKED_FISHCAKE(Obj.hundred_pirate_fishcake, 110),
        COOKED_JUBBLY(Obj._100_jubbly_meat_cooked, 150),
        COOKED_KARAMBWAN(Obj.tbwt_cooked_karambwan, 180),
        COOKED_MEAT(Obj.cooked_meat, 30),
        COOKED_OOMLIE_WRAP(Obj.cooked_oomlie, 140),
        COOKED_RABBIT(Obj.cooked_rabbit, 20),
        COOKED_SLIMY_EEL(Obj.mort_slimey_eel_cooked, 70),
        COOKED_SWEETCORN(Obj.sweetcorn_cooked, 0, { it.heal((it.maxHitpoints * 0.10).roundToInt()) }),
        COOKED_TURKEY(Obj.cooked_turkey, 30),
        COOKED_TURKEY_DRUMSTICK(Obj.cooked_turkey_drumstick, 20),
        CORONATION_CHICKEN_SANDWICH(Obj.obj_24398, 20),
        CRAB_MEAT(Obj.hundred_pirate_giant_crab_meat_5, -1),
        CRAYFISH(Obj.crayfish, 20),
        CREAM_TEA(Obj.obj_24396, 20),
        CRUMBLY_BITS(Obj.fremsaga_thok2_mining_ore, 0),
        CRUNCHY_RUNE_ROCKS(Obj.fremsaga_thok_runeessence, 0),
        CURRY(intArrayOf(Obj.curry), Obj.bowl_empty, 190),
        DEEP_BLUE_SWEETS(Obj.obj_4559, 20),
        DISEASED_FRUIT(Obj.jaro_fruit_diseased, 100),
        DOUGHNUT(Obj.donut, 20),
        DRACONIC_FRUIT(Obj.jaro_fruit_draconic, 100),
        DRUGGED_MEAT(Obj.tutorial3_meat, -1),
        DUSK_EEL(Obj.rand_fish_3, 70),
        DUSK_EEL_POTATO(Obj.rand_potato_with_fish_3, 100),
        DUSK_EEL_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_3_and_mushroom_b, 190),
        DUSK_EEL_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_3_and_mushroom_a, 130),
        DWELLBERRIES(Obj.dwellberries, 20),
        EASTER_EGG(Obj.easter_egg, 20),
        EASTER_EGG1(Obj.obj_7928, 20),
        EASTER_EGG2(Obj.obj_7929, 20),
        EASTER_EGG3(Obj.obj_7930, 20),
        EASTER_EGG4(Obj.obj_7931, 20),
        EASTER_EGG5(Obj.obj_7932, 20),
        EASTER_EGG6(Obj.obj_7933, 20),
        EASTER_EGG7(Obj.easter08_egg_default, 20),
        EASTER_EGG8(Obj.obj_12643, 20),
        EASTER_EGG9(Obj.easter_egg, 20),
        EASTER_EGG10(Obj.obj_12642, 20),
        EASTER_EGG11(Obj.obj_12641, 20),
        EASTER_EGG12(Obj.obj_12640, 20),
        EASTER_EGG13(Obj.obj_12639, 20),
        EASTER_EGG14(Obj.easter_egg, 20),
        EASTER_EGG15(Obj.easter_egg, 20),
        EASTER_EGG16(Obj.easter_egg, 20),
        EASTER_EGG17(Obj.easter_egg, 20),
        EASTER_EGG18(Obj.easter_egg, 20),
        EDIBLE_SEAWEED(Obj.edible_seaweed, 20),
        EDICAP_POTATO(Obj.rand_potato_with_mushroom_b, 120),
        EELSUSHI(Obj.dorgesh_cave_eel_sushi, 20),
        EGG_AND_TOMATO(intArrayOf(Obj.bowl_eggplustomato), Obj.bowl_empty, 80),
        EGG_POTATO(Obj.potato_eggplustomato, 160),
        EQUA_LEAVES(Obj.equa_leaves, 10),
        EVIL_DRUMSTICK(Obj.obj_24147, 10),
        EVIL_TURNIP(intArrayOf(Obj.lore_evil_turnip_3, Obj.lore_evil_turnip_2, Obj.lore_evil_turnip_1), 60),
        FAT_SNAIL_MEAT(Obj.snail_corpse_cooked3, 80, { it.heal(Utils.random(20)) }),
        FIELD_RATION(Obj.pest_field_ration, 50),
        FILLETS(Obj.dorgesh_cave_crawler_fillets, 25),
        FINGERS(Obj.dorgesh_wall_beast_fingers, 20),
        FISHCAKE(Obj.hundred_pirate_fishcake, 110),
        FISH_LIKE_THING(Obj.evil_bob_cooked_fish_correct, -1),
        FISH_N_CHIPS(Obj.obj_24400, 50),
        FISH_PIE(intArrayOf(Obj.fish_pie, Obj.half_fish_pie), Obj.piedish, 60, { it.skills.adjustStat(3, 0.0, Constants.FISHING) }),
        FLATFISH_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_4_and_mushroom_b, 220),
        FLATFISH_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_4_and_mushroom_a, 160),
        FOOD_CLASS_1(Obj.sc_food_1, 40),
        FOOD_CLASS_2(Obj.sc_food_2, 80),
        FOOD_CLASS_3(Obj.sc_food_3, 120),
        FOOD_CLASS_4(Obj.sc_food_4, 160),
        FOOD_CLASS_5(Obj.javelin, 200),
        FRESH_MONKFISH(Obj.swan_monkfish, 100),
        FRIED_MUSHROOMS(intArrayOf(Obj.bowl_mushroom_fried), Obj.bowl_empty, 50),
        FRIED_ONIONS(intArrayOf(Obj.bowl_onion_fried), Obj.bowl_empty, 50),
        FROGBURGER(Obj.dorgesh_frog_burger, 20),
        FROGSPAWN_GUMBO(Obj.dorgesh_frog_spawn_gumbo, 20),
        FROG_SPAWN(Obj.giant_frogspawn, 20),
        FRUIT_BATTA(Obj.fruit_batta, 110),
        FULL_BREAKFAST(Obj.obj_24404, 100),
        FURY_SHARK(Obj.jmod_boss_megashark, 280),
        GARDEN_PIE(intArrayOf(Obj.garden_pie, Obj.half_garden_pie), Obj.piedish, 60, { it.skills.adjustStat(3, 0.0, Constants.FARMING) }),
        GIANT_CARP(Obj.giant_carp, -1),
        GIANT_FLATFISH(Obj.rand_fish_4, 100),
        GIANT_FLATFISH_POTATO(Obj.rand_potato_with_fish_4, 130),
        GIANT_FROG_LEGS(Obj.giant_frog_legs, 60),
        GISSEL_POTATO(Obj.rand_potato_with_mushroom_a, 60),
        GOUT_TUBER(Obj.village_rare_tuber, 10, { it.restoreRunEnergy(100.0) }),
        GREEN_GLOOP_SOUP(Obj.dorgesh_green_gloop_soup, 20),
        GREEN_SWEETS(Obj.obj_4563, 20),
        GRUBS_A_LA_MODE(Obj.dorgesh_grubs_a_la_mode, 25),
        GUTHIX_FRUIT(Obj.jaro_fruit_guthix, 200),
        HALF_WINE_JUG(Obj.half_full_wine_jug, 87),
        HEIM_CRAB(Obj.rand_fish_1, 20),
        HEIM_CRAB_POTATO(Obj.rand_potato_with_fish_1, 50),
        HEIM_CRAB_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_1_and_mushroom_b, 140),
        HEIM_CRAB_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_1_and_mushroom_a, 80),
        HERRING(Obj.herring, 20),
        HUMBLE_PIE(Obj.humble_pie, -1),
        IGNEOUS_FRUIT(Obj.jaro_fruit_igneous, 150),
        JANGERBERRIES(Obj.jangerberries, 20),
        JUBBLY(Obj._100_jubbly_meat_cooked, 150),
        JUJU_GUMBO(Obj.juju_gumbo, 320, { it.addEffect(Effect.BARON_SHARK, Ticks.fromSeconds(12).toLong()) }),
        KARAMBWANJI(Obj.tbwt_cooked_karambwanji, 30),
        KEBAB(Obj.kebab, 0, ::kebab),
        KING_WORM(Obj.king_worm, 20),
        LAVA_EEL(Obj.lava_eel, 110),
        LEAN_SNAIL_MEAT(Obj.snail_corpse_cooked2, 80),
        LEMON(Obj.lemon, 20),
        LEMON_CHUNKS(Obj.lemon_chunks, 20),
        LEMON_SLICES(Obj.lemon_slices, 20),
        LIME(Obj.lime, 20),
        LIME_CHUNKS(Obj.lime_chunks, 20),
        LIME_SLICES(Obj.lime_slices, 20),
        LOACH(Obj.dorgesh_steamed_pond_loach, 30),
        LOBSTER(Obj.lobster, 120),
        LOCUST_MEAT(Obj.locust_meat, 20),
        MACKEREL(Obj.mackerel, 60),
        MAGIC_EGG(Obj.obj_11023, 20),
        MANTA(Obj.mantaray, 220),
        MEAT(Obj.cooked_meat, 30),
        MEAT_PIE(intArrayOf(Obj.meat_pie, Obj.half_a_meat_pie), Obj.piedish, 60),
        MEAT_PIZZA(intArrayOf(Obj.meat_pizza, Obj.obj_2295), 80),
        MINCED_MEAT(intArrayOf(Obj.bowl_carne), Obj.bowl_empty, 2),
        MINT_CAKE(Obj.aluft_gnome_mint_cake, 0, { it.restoreRunEnergy(100.0) }),
        MONKEY_BAR(Obj.mm_monkey_bar, 90),
        MONKEY_NUTS(Obj.mm_monkey_nuts, 20),
        MONKFISH(Obj.monkfish, 160),
        MORAY_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_10_and_mushroom_b, 370),
        MORAY_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_10_and_mushroom_a, 310),
        MUSHROOMS(Obj.dorgesh_sauteed_mushrooms, 25),
        MUSHROOM_AND_ONIONS(intArrayOf(Obj.bowl_mushroomplusonion), Obj.bowl_empty, 110),
        MUSHROOM_AND_ONION_POTATO(Obj.potato_mushroomplusonion, 200),
        NOT_MEAT(Obj.fremsaga_thok_potato, 20),
        ODD_CRUNCHIES(Obj.obj_2197, 10),
        OKTOBERFEST_PRETZEL(Obj.obj_19778, 20),
        ONION(Obj.onion, 10, ::onionTears),
        ONION_AND_TOMATO(intArrayOf(Obj.bowl_oniontomato), Obj.bowl_empty, 20),
        OOMILE(Obj.cooked_oomlie, 140),
        ORANGE(Obj.orange, 20),
        ORANGE_CHUNKS(Obj.orange_chunks, 20),
        ORANGE_SLICES(Obj.orange_slices, 20),
        PAPAYA(Obj.papaya, 80, { it.restoreRunEnergy(5.0) }),
        PEACH(Obj.peach, 80),
        PIKE(Obj.pike, 80),
        PINEAPPLE_CHUNKS(Obj.pineapple_chunks, 20),
        PINEAPPLE_PIZZA(intArrayOf(Obj.pineapple_pizza, Obj.obj_2303), 110),
        PINEAPPLE_RING(Obj.pineapple_ring, 20),
        PINK_SWEETS(Obj.obj_4564, 20),
        PLAIN_PIZZA_FULL(intArrayOf(Obj.plain_pizza, Obj.obj_2291), 70),
        PLANT_BITS(Obj.obj_2418, -1),
        PM_CHEESE_AND_TOMATO_BATTA(Obj.premade_cheeseplustom_batta, 110),
        PM_CHOCOCHIP_CRUNCHIES(Obj.obj_2239, 70),
        PM_CHOCOLATE_BOMB(Obj.premade_chocolate_bomb, 150),
        PM_FRUIT_BATTA(Obj.obj_2225, 110),
        PM_SPICY_CRUNCHIES(Obj.obj_2241, 70),
        PM_TANGLED_TOAD_LEGS(Obj.premade_tangled_toads_legs, 150),
        PM_TOAD_BATTA(Obj.obj_2221, 120),
        PM_TOAD_CRUNCHIES(Obj.obj_2243, 80),
        PM_VEGETABLE_BATTA(Obj.premade_vegetable_batta, 110),
        PM_WORM_BATTA(Obj.obj_2219, 120),
        PM_WORM_CRUNCHIES(Obj.obj_2237, 80),
        PM_WORM_HOLE(Obj.premade_worm_hole, 120),
        POISONED_CHEESE(Obj.ratcatchers_poisonedcheese, -1),
        POISON_KARAMBWAN(Obj.tbwt_poorly_cooked_karambwan, 0, { it.applyHit(Hit(it, 50, HitLook.POISON_DAMAGE)) }),
        POORLY_COOKED_BEAST_MEAT(Obj.beast_meat, 20),
        POORLY_COOKED_BIRD_MEAT(Obj.bird_meat, 20),
        POPCORN_BALL(Obj.obj_14082, 30),
        POTATO_WITH_BUTTER(Obj.potato_butter, 140),
        POTATO_WITH_CHEESE(Obj.potato_cheese, 160),
        POT_OF_CREAM(Obj.pot_of_cream, 20),
        PUMPKIN(Obj.pumpkin, 20),
        PUNCH(Obj.hw11_punch, 20),
        PURPLE_SWEETS(Obj.obj_4561, 0, ::purpleSweets),
        PURPLE_SWEETS2(Obj.trail_sweets, 0, ::purpleSweets),
        RABBIT_SANDWICH(Obj.omge_rabbit_sandwich, 42),
        RABIT(Obj.cooked_rabbit, 50),
        RAINBOW_FISH(Obj.hunting_fish_special, 110),
        REDBERRY_PIE(intArrayOf(Obj.redberry_pie, Obj.half_a_redberry_pie), Obj.piedish, 50),
        RED_BANANA(Obj.hundred_ilm_red_banana, 50),
        RED_EYE(Obj.rand_fish_2, 50),
        RED_EYE_POTATO(Obj.rand_potato_with_fish_2, 80),
        RED_EYE_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_2_and_mushroom_b, 170),
        RED_EYE_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_2_and_mushroom_a, 110),
        RED_SWEETS(Obj.obj_4562, 20),
        ROASTED_BEAST_MEAT(Obj.spit_roasted_beast_meat, 42),
        ROASTED_BIRD_MEAT(Obj.spit_roasted_bird_meat, 42),
        ROAST_BEAST_MEAT(Obj.spit_roasted_beast_meat, 42),
        ROAST_BIRD_MEAT(Obj.obj_998, 60),
        ROAST_FROG(Obj.dorgesh_whole_roasted_frog, 50),
        ROAST_POTATOES(Obj.obj_15429, 100),
        ROAST_RABBIT(Obj.spit_roasted_rabbit_meat, 70),
        ROCKTAIL(Obj.lrc_rocktail_cooked, 0, { it.heal(230, 100) }),
        BANDAGE(Obj.castlewars_bandages, 0, { it.heal(230, 100) }),
        ROE(Obj.brut_roe, 30),
        ROLL(Obj.roll, 20),
        ROTTEN_APPLE(Obj.rottenapples, -1),
        SALMON(Obj.salmon, 90),
        SALVE_EEL(Obj.rand_fish_8, 200),
        SALVE_EEL_POTATO(Obj.rand_potato_with_fish_8, 230),
        SALVE_EEL_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_8_and_mushroom_b, 320),
        SALVE_EEL_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_8_and_mushroom_a, 260),
        SARADOMIN_FRUIT(Obj.jaro_fruit_saradomin, 200),
        SARDINE(Obj.sardine, 40),
        SCORPION_MEAT(Obj.dom_scorpion_meat, 80),
        SCRAMBLED_EGG(intArrayOf(Obj.bowl_egg_scrambled), Obj.bowl_empty, 50),
        SEASONED_LEGS(Obj.obj_2158, 1),
        SEAWEED_SANDWICH(Obj.tbwt_seaweed_in_monkey_skin_sandwich, -1),
        SEA_MEAT_1(Obj.fremsaga_thok_raw_eel, 250),
        SEA_MEAT_2(Obj.fremsaga_thok2_sea_meat, 250),
        SEA_TURTLE(Obj.seaturtle, 210),
        SHADOW_FRUIT(Obj.jaro_fruit_shadow, 150),
        SHARK(Obj.shark, 200),
        SHORT_FINNED_EEL(Obj.rand_fish_5, 120),
        SHORT_FIN_EEL_POTATO(Obj.rand_potato_with_fish_5, 150),
        SHORT_FIN_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_5_and_mushroom_b, 240),
        SHORT_FIN_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_5_and_mushroom_a, 180),
        SHRIMP(Obj.shrimp, 30),
        SHRUNK_OGLEROOT(Obj.grim_turnip, 20),
        SKEWERED_KEBAB(Obj.pap_skewered_kebab, 90),
        SLICED_BANANA(Obj.tbwt_sliced_banana, 20),
        SLICED_MUSHROOMS(intArrayOf(Obj.bowl_mushroom_raw), Obj.bowl_empty, 0),
        SLICED_RED_BANANA(Obj.hundred_ilm_sliced_red_banana, 20),
        SLIMY_EEL(Obj.mort_slimey_eel_cooked, 70),
        SNIPPER_POTATO(Obj.rand_potato_with_fish_6, 180),
        SNIPPER_AND_EDICAP_POTATO(Obj.rand_potato_with_fish_6_and_mushroom_b, 270),
        SNIPPER_AND_GISSEL_POTATO(Obj.rand_potato_with_fish_6_and_mushroom_a, 210),
        SPICEY_SAUCE(intArrayOf(Obj.bowl_chilli), Obj.bowl_empty, 20),
        SPICY_CRUNCHIES(Obj.spicy_crunchies, 70),
        SPICY_MINCED_MEAT(Obj.bowl_spicymeat, 20),
        SPICY_SAUCE(Obj.bowl_chilli, 20),
        SPICY_TOADS_LEGS(Obj.obj_2156, 20),
        SPICY_TOMATO(Obj.bowl_spicytomato, 20),
        SPICY_WORM(Obj.cert_unidentified_cadantine, 20),
        SPIDER_ON_SHAFT(Obj.tbw_spider_on_shaft_cooked, 20),
        SPIDER_ON_STICK(Obj.tbw_spider_on_stick_cooked, 20),
        SPINACH_ROLL(Obj.spinach_roll, 2),
        SQUARE_SANDWICH(Obj.square_sandwich, 25),
        STEAK_AND_KIDNEY_PIE(Obj.obj_24402, 50),
        STEW(intArrayOf(Obj.stew), Obj.bowl_empty, 110),
        STRAWBERRY(Obj.strawberry, 10, { it.heal((it.maxHitpoints * 0.06).toInt()) }),
        STUFFED_SNAKE(Obj.hundred_ilm_cooked_stuffed_snake, 200),
        SUMMER_PIE_FULL(intArrayOf(Obj.summer_pie, Obj.half_summer_pie), Obj.piedish, 110, {
            it.skills.adjustStat(5, 0.0, Constants.AGILITY)
            it.restoreRunEnergy(10.0)
        }),
        SUMMER_SQIRKJUICE(Obj.osman_squirk_j_summer, 150, { it.skills.adjustStat(2, 0.1, true, Constants.THIEVING) }),
        SUPER_KEBAB(Obj.super_kebab, 0, ::kebab),
        SWORDFISH(Obj.swordfish, 140),
        TANGLED_TOAD_LEGS(Obj.tangled_toads_legs, 150),
        TCHIKI_MONKEY_NUTS(Obj.hundred_ilm_tchiki_monkey_nuts, 2),
        TCHIKI_NUT_PASTE(Obj.hundred_ilm_tchiki_monkey_nut_paste, 2),
        TEA_FLASK(Obj.tol_tea, 20),
        TENTH_ANNIVERSARY_CAKE(Obj.obj_20111, 20),
        THIN_SNAIL_MEAT(Obj.snail_corpse_cooked1, 30),
        THOK_RUNE(Obj.fremsaga_thokrune, 20),
        TIGER_SHARK(Obj.hlr4m_tiger_shark, 100),
        TOADS_LEGS(Obj.toads_legs, 20),
        TOAD_BATTA(Obj.toad_batta, 120),
        TOAD_CRUNCHIES(Obj.toad_crunchies, 80),
        TOMATO(Obj.tomato, 20),
        TRIANGLE_SANDWICH(Obj.triangle_sandwich, 20),
        TROUT(Obj.trout, 70),
        TRUFFLE(Obj.obj_12132, 20),
        TUNA(Obj.tuna, 100),
        TUNA_AND_CORN(intArrayOf(Obj.bowl_tunaplussweetcorn), Obj.bowl_empty, 130),
        TUNA_POTATO(Obj.potato_tunaplussweetcorn, 220),
        TURKEY_DRUMSTICK(Obj.obj_15428, 20),
        UGTHANKI_KEBAB_1(Obj.ugthanki_kebab_bad, 190, { it.forceTalk("Yum!") }),
        UGTHANKI_KEBAB_2(Obj.ugthanki_kebab, 190, { it.forceTalk("Yum!") }),
        UGTHANKI_MEAT(Obj.cooked_ugthanki_meat, 20),
        UNFINISHED_BATTA(Obj.obj_2261, 20),
        UNFINISHED_BATTA1(Obj.obj_2263, 20),
        UNFINISHED_BATTA2(Obj.obj_2265, 20),
        UNFINISHED_BATTA3(Obj.obj_2267, 20),
        UNFINISHED_BATTA4(Obj.obj_2269, 20),
        UNFINISHED_BATTA5(Obj.obj_2271, 20),
        UNFINISHED_BATTA6(Obj.obj_2273, 20),
        VEGETABLE_BATTA(Obj.vegetable_batta, 110),
        VEG_BALL(Obj.veg_ball, 120),
        WATERMELON_SLICE(Obj.watermelon_slice, 20),
        WEB_SNIPER(Obj.rand_fish_6, 150),
        WHITE_PEARL(Obj.mdaughter_white_pearl_fruit, 20),
        WHITE_SWEETS(Obj.scorpioncageempty, 20),
        WHITE_TREE_FRUIT(Obj.garden_white_tree_fruit, 20),
        WILD_PIE(intArrayOf(Obj.wild_pie, Obj.half_wild_pie), Obj.piedish, 110, {
            it.skills.adjustStat(4, 0.0, Constants.RANGE)
            it.skills.adjustStat(5, 0.0, Constants.SLAYER)
        }),
        WORM_BATTA(Obj.worm_batta, 120),
        WORM_CRUNCHIES(Obj.worm_crunchies, 80),
        WORM_HOLE(Obj.worm_hole, 120),
        WRAPPED_CANDY(Obj.obj_14084, 20),
        YULE_LOGS(Obj.obj_15430, 20),
        ZAMORAK_FRUIT(Obj.jaro_fruit_zamorak, 200);

        constructor(id: Int, heal: Int, effect: ((Player) -> Unit)? = null) : this(intArrayOf(id), -1, heal, effect)

        constructor(ids: IntArray, heal: Int, effect: ((Player) -> Unit)? = null) : this(ids, -1, heal, effect)

        val healAmount get() = heal

        fun getReplaceIdFor(id: Int): Int {
            if (ids.size <= 1) return -1
            val next = ids.indexOf(id).coerceAtLeast(0) + 1
            return if (next < ids.size) ids[next] else if (container <= 0) -1 else container
        }

        companion object {
            @JvmField
            val foods: Map<Int, Food> = entries.flatMap { food -> food.ids.map { it to food } }.toMap()

            @JvmStatic
            fun forId(itemId: Int) = foods[itemId]
        }
    }
}

@ServerStartupEvent
fun mapFoods() {
    onItemClick(*Foods.Food.foods.keys.toTypedArray(), options = arrayOf("Eat", "Heal")) { e -> Foods.eat(e.player, e.item, e.slotId, null) }
}

private fun onionTears(player: Player) = player.sendMessage("It hurts to see a grown ${if (player.appearance.isMale) "man" else "woman"} cry.")

private fun purpleSweets(player: Player) {
    player.heal(Utils.random(10, 30))
    player.restoreRunEnergy(10.0)
}

private fun kebab(player: Player) {
    val roll = Utils.random(100)
    val missing = player.maxHitpoints - player.hitpoints
    if (missing == 0) {
        if (roll < 10) {
            player.applyHit(Hit(player, Utils.random(10, 20), HitLook.TRUE_DAMAGE))
            player.sendMessage("The kebab didn't sit well with you.")
        }
        return
    }
    when {
        roll >= 96 -> {
            player.sendMessage("Wow, that was an amazing kebab! You feel really invigorated.")
            player.heal(minOf(300, missing))
            for (stat in intArrayOf(Constants.ATTACK, Constants.STRENGTH, Constants.DEFENSE)) player.skills.adjustStat(1 + Utils.random(3), 0.1, true, stat)
        }
        roll >= 75 -> {
            player.sendMessage("That was a good kebab. You feel a lot better.")
            player.heal(minOf(Utils.random(100, 200), missing))
        }
        roll >= 10 -> {
            player.sendMessage("It restores some life points.")
            player.heal(minOf((player.maxHitpoints * 0.10).toInt(), missing))
        }
        roll >= 1 -> player.sendMessage("That kebab didn't seem to do a lot.")
        Utils.random(0, 1) == 0 -> {
            val stat = Utils.random(0, Constants.SKILL_NAME.size - 1)
            player.sendMessage("That tasted a bit dodgy. You feel a bit ill.")
            player.sendMessage("Eating the kebab has damaged your ${Constants.SKILL_NAME[stat]} stat.")
            player.skills.adjustStat(-1, 0.1, true, stat)
        }
        else -> {
            player.sendMessage("That tasted very dodgy. You feel very ill.")
            player.sendMessage("Eating the kebab has done damage to some of your stats.")
            player.skills.adjustStat(-5, 0.05, true, Constants.ATTACK, Constants.STRENGTH, Constants.DEFENSE)
        }
    }
}
