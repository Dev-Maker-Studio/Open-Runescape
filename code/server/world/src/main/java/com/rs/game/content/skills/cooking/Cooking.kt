package com.rs.game.content.skills.cooking

import com.rs.game.content.Effect
import com.rs.game.content.quests.bigchompybirdhunting.objects.reservesQuestChompy
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.actions.PlayerAction
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item
import org.darkan.core.model.Sound
import org.darkan.core.model.SoundType
import org.darkan.core.gameval.gen.Sound as SoundId

private const val COOK_TICKS = 3
private const val BURN_LEVEL_REDUCTION = 6
private const val MIN_SUCCESS_CHANCE = 0.7
private const val BONFIRE_XP_BONUS = 0.10
private const val SINEW_XP = 3.0

private val COOKING_LOCS = arrayOf<Any>("Fire", "Range", "Campfire", "Oven", "Cooking range", "Fancy range", "Gnome cooker", "Cooker", "Sulphur pit", "Stove", "Clay oven", "Clay Oven", "Fireplace", "Ogre spit-roast", "Iron spit")

class Cooking(private val option: Int, private val gameObject: GameObject, private val cookable: Cookables, private var quantity: Int) : PlayerAction() {
    enum class Cookables(
        val rawItem: Item,
        val burntItem: Item,
        val productItem: IntArray,
        val level: Int,
        val burningLvl: Int,
        val xp: Double,
        val isSpitRoastRequired: Boolean,
        val isFireOnly: Boolean,
        val isRangeOnly: Boolean,
        val returnItemId: Int = -1,
    ) {
        SODA_ASH_1(Obj.seaweed, Obj.soda_ash, intArrayOf(Obj.soda_ash), 1, 0, 1.0),
        SODA_ASH_2(Obj.hundred_pirate_kelp, Obj.soda_ash, intArrayOf(Obj.soda_ash), 1, 0, 1.0),
        SODA_ASH_3(Obj.dorgesh_swamp_weed, Obj.soda_ash, intArrayOf(Obj.soda_ash), 1, 0, 1.0),
        NETTLE_TEA(Obj.bowl_nettlewater, Obj.bowl_nettletea, intArrayOf(Obj.bowl_nettletea), 1, 0, 61.0),
        KARAMBWAN(Obj.tbwt_raw_karambwan, Obj.tbwt_burnt_karambwan, intArrayOf(Obj.tbwt_cooked_karambwan), 1, 20, 80.0),
        POISON_KARAMBWAN(Obj.tbwt_cooked_karambwan, Obj.tbwt_poorly_cooked_karambwan, intArrayOf(Obj.tbwt_poorly_cooked_karambwan), 1, 20, 1.0),
        BURNT_MEAT(Obj.cooked_meat, Obj.burnt_meat, intArrayOf(Obj.burnt_meat), 1, 0, 0.0),
        RAW_MEAT(Obj.raw_beef, Obj.burnt_meat, intArrayOf(Obj.cooked_meat, Obj.xbows_sinew), 1, 34, 30.0),
        RAW_RAT(Obj.raw_rat_meat, Obj.burnt_meat, intArrayOf(Obj.cooked_meat), 1, 34, 30.0),
        RAW_BEAR(Obj.raw_bear_meat, Obj.burnt_meat, intArrayOf(Obj.cooked_meat, Obj.xbows_sinew), 1, 34, 30.0),
        RAW_WOLF(Obj.raw_wolf_meat, Obj.burnt_meat, intArrayOf(Obj.cooked_meat, Obj.xbows_sinew), 1, 34, 30.0),
        RAW_RABBIT(Obj.raw_rabbit, Obj.spit_burned_rabbit_meat, intArrayOf(Obj.cooked_rabbit), 1, 34, 30.0, fireOnly = true),
        RAW_CHICKEN(Obj.raw_chicken, Obj.burnt_chicken, intArrayOf(Obj.cooked_chicken), 1, 34, 30.0),
        RAW_SHRIMP(Obj.raw_shrimp, Obj.burnt_shrimp, intArrayOf(Obj.shrimp), 1, 34, 30.0),
        RAW_ANCHOVIES(Obj.raw_anchovies, Obj.burntfish1, intArrayOf(Obj.anchovies), 1, 34, 30.0),
        RAW_CRAYFISH(Obj.raw_crayfish, Obj.burnt_crayfish, intArrayOf(Obj.crayfish), 1, 34, 30.0),
        RAW_MEAT_GHOST(Obj.obj_4287, Obj.burnt_meat, intArrayOf(Obj.cooked_meat), 1, 34, 30.0),
        RAW_CHICKEN_GHOST(Obj.obj_4289, Obj.burnt_meat, intArrayOf(Obj.cooked_meat), 1, 34, 30.0),
        BREAD(Obj.bread_dough, Obj.burnt_bread, intArrayOf(Obj.bread), 1, 37, 40.0),
        RAW_SARDINE(Obj.raw_sardine, Obj.burntfish5, intArrayOf(Obj.sardine), 1, 37, 40.0),
        RAW_UGTHANKI_MEAT(Obj.raw_ugthanki_meat, Obj.burnt_meat, intArrayOf(Obj.cooked_ugthanki_meat), 1, 100, 40.0),
        RAW_KARAMBWANJI(Obj.tbwt_raw_karambwanji, Obj.ashes, intArrayOf(Obj.tbwt_cooked_karambwanji), 1, 28, 10.0),
        RAW_HERRING(Obj.raw_herring, Obj.burntfish3, intArrayOf(Obj.herring), 5, 41, 50.0),
        RAW_POTATO(Obj.potato, Obj.potato_burnt, intArrayOf(Obj.potato_baked), 7, 41, 15.0, rangeOnly = true),
        RAW_MACKEREL(Obj.raw_mackerel, Obj.burntfish3, intArrayOf(Obj.mackerel), 10, 45, 60.0),
        RAW_REDBERRY_PIE(Obj.uncooked_redberry_pie, Obj.burnt_pie, intArrayOf(Obj.redberry_pie), 10, 45, 78.0, rangeOnly = true),
        RAW_BIRD_MEAT(Obj.spit_raw_bird_meat, Obj.spit_burned_bird_meat, intArrayOf(Obj.bird_meat), 11, 99, 62.0),
        RAW_BIRD_MEAT2(Obj.spit_skewered_bird_meat, Obj.spit_burned_bird_meat, intArrayOf(Obj.spit_roasted_bird_meat), 11, 99, 62.0, fireOnly = true),
        RAW_TROUT(Obj.raw_trout, Obj.burntfish2, intArrayOf(Obj.trout), 15, 49, 70.0),
        RAW_COD(Obj.raw_cod, Obj.burntfish2, intArrayOf(Obj.cod), 18, 51, 75.0),
        RAW_PIKE(Obj.raw_pike, Obj.burntfish2, intArrayOf(Obj.pike), 20, 53, 75.0),
        RAW_MEAT_PIE(Obj.uncooked_meat_pie, Obj.burnt_pie, intArrayOf(Obj.meat_pie), 20, 53, 110.0, rangeOnly = true),
        RAW_CRAB_MEAT(Obj.hundred_pirate_giant_crab_meat, Obj.hundred_pirate_burned_giant_crab_meat, intArrayOf(Obj.hundred_pirate_giant_crab_meat_5), 21, 62, 100.0),
        RAW_BEAST_MEAT(Obj.spit_raw_beast_meat, Obj.spit_burned_beast_meat, intArrayOf(Obj.spit_roasted_beast_meat), 21, 99, 82.5, spitRoast = true),
        RAW_SALMON(Obj.raw_salmon, Obj.burntfish2, intArrayOf(Obj.salmon), 25, 58, 90.0),
        RAW_SLIMY_EEL(Obj.mort_slimey_eel, Obj.burnt_eel, intArrayOf(Obj.mort_slimey_eel_cooked), 28, 60, 95.0),
        SWEETCORN(Obj.sweetcorn, Obj.sweetcorn_burnt, intArrayOf(Obj.sweetcorn_cooked), 28, 53, 104.0),
        RAW_MUD_PIE(Obj.uncooked_mud_pie, Obj.burnt_pie, intArrayOf(Obj.mud_pie), 29, 63, 128.0, rangeOnly = true),
        RAW_CHOMPY(Obj.raw_chompy, Obj.ruined_chompy, intArrayOf(Obj.cooked_chompy), 30, 99, 140.0, spitRoast = true, fireOnly = true),
        RAW_TUNA(Obj.raw_tuna, Obj.burntfish4, intArrayOf(Obj.tuna), 30, 63, 100.0),
        RAW_APPLE_PIE(Obj.uncooked_apple_pie, Obj.burnt_pie, intArrayOf(Obj.apple_pie), 30, 63, 130.0, rangeOnly = true),
        RAW_FISHCAKE(Obj.hundred_pirate_raw_fishcake, Obj.hundred_pirate_burned_fishcake, intArrayOf(Obj.hundred_pirate_fishcake), 31, 62, 100.0),
        RAW_GARDEN_PIE(Obj.uncooked_garden_pie, Obj.burnt_pie, intArrayOf(Obj.garden_pie), 34, 68, 138.0, rangeOnly = true),
        RAW_RAINBOW_FISH(Obj.hunting_raw_fish_special, Obj.hunting_raw_fish_special_burnt, intArrayOf(Obj.hunting_fish_special), 35, 64, 110.0),
        RAW_CAVE_EEL(Obj.raw_cave_eel, Obj.obj_5006, intArrayOf(Obj.cave_eel), 38, 74, 115.0),
        RAW_LOBSTER(Obj.raw_lobster, Obj.burnt_lobster, intArrayOf(Obj.lobster), 40, 74, 120.0),
        UNCOOKED_CAKE(Obj.uncooked_cake, Obj.burnt_cake, intArrayOf(Obj.cake), 40, 70, 180.0, rangeOnly = true, returnItemId = Obj.cake_tin),
        RAW_JUBBLY(Obj._100_jubbly_meat_raw, Obj._100_jubbly_meat_burned, intArrayOf(Obj._100_jubbly_meat_cooked), 41, 99, 160.0, spitRoast = true),
        RAW_BASS(Obj.raw_bass, Obj.burntfish4, intArrayOf(Obj.bass), 43, 79, 130.0),
        RAW_SWORDFISH(Obj.raw_swordfish, Obj.burnt_swordfish, intArrayOf(Obj.swordfish), 45, 86, 140.0),
        RAW_FISH_PIE(Obj.uncooked_fish_pie, Obj.burnt_pie, intArrayOf(Obj.fish_pie), 47, 74, 164.0, rangeOnly = true),
        WRAPPED_OOMLIE(Obj.wrapped_oomlie, Obj.burnt_oomlie, intArrayOf(Obj.cooked_oomlie), 50, 44, 30.0),
        RAW_LAVA_EEL(Obj.raw_lava_eel, -1, intArrayOf(Obj.lava_eel), 53, 0, 30.0),
        RAW_MONKFISH(Obj.raw_monkfish, Obj.burnt_monkfish, intArrayOf(Obj.monkfish), 62, 92, 150.0),
        RAW_ADMIRAL_PIE(Obj.uncooked_admiral_pie, Obj.burnt_pie, intArrayOf(Obj.admiral_pie), 70, 94, 210.0, rangeOnly = true),
        RAW_BARON_SHARK(Obj.raw_golden_shark, Obj.burnt_shark, intArrayOf(Obj.golden_shark), 80, 95, 210.0),
        RAW_SHARK(Obj.raw_shark, Obj.burnt_shark, intArrayOf(Obj.shark), 80, 100, 210.0),
        RAW_SEA_TURTLE(Obj.raw_seaturtle, Obj.burnt_seaturtle, intArrayOf(Obj.seaturtle), 82, 100, 211.3),
        RAW_WILD_PIE(Obj.uncooked_wild_pie, Obj.burnt_pie, intArrayOf(Obj.wild_pie), 85, 100, 240.0, rangeOnly = true),
        RAW_CAVEFISH(Obj.lrc_cavefish_raw, Obj.lrc_cavefish_burnt, intArrayOf(Obj.lrc_cavefish_cooked), 88, 100, 214.0),
        RAW_MANTA_RAY(Obj.raw_mantaray, Obj.burnt_mantaray, intArrayOf(Obj.mantaray), 91, 100, 216.0),
        RAW_ROCKTAIL(Obj.lrc_rocktail_raw, Obj.lrc_rocktail_burnt, intArrayOf(Obj.lrc_rocktail_cooked), 93, 100, 225.0),
        RAW_SUMMER_PIE(Obj.uncooked_summer_pie, Obj.burnt_pie, intArrayOf(Obj.summer_pie), 95, 100, 260.0, rangeOnly = true),
        UNCOOKED_PIZZA(Obj.uncooked_pizza, Obj.burnt_pizza, intArrayOf(Obj.plain_pizza), 35, 68, 143.0),
        UNCOOKED_STEW(Obj.uncooked_stew, Obj.burnt_stew, intArrayOf(Obj.stew), 25, 58, 117.0),
        UNCOOKED_CURRY(Obj.uncooked_curry, Obj.burnt_curry, intArrayOf(Obj.curry), 60, 74, 280.0),
        UNCOOKED_EGG(Obj.bowl_egg_raw, Obj.bowl_egg_burnt, intArrayOf(Obj.bowl_egg_scrambled), 13, 48, 50.0),
        CHOPPED_ONION(Obj.bowl_onion, Obj.bowl_onion_burnt, intArrayOf(Obj.bowl_onion_fried), 42, 77, 60.0),
        SLICED_MUSHROOMS(Obj.bowl_mushroom_raw, Obj.bowl_mushroom_burnt, intArrayOf(Obj.bowl_mushroom_fried), 46, 90, 60.0),
        THIN_SNAIL(Obj.snail_corpse1, Obj.burnt_snail, intArrayOf(Obj.snail_corpse_cooked1), 12, 47, 70.0),
        FAT_SNAIL(Obj.snail_corpse3, Obj.burnt_snail, intArrayOf(Obj.snail_corpse_cooked3), 22, 56, 95.0),
        LEAN_SNAIL(Obj.snail_corpse2, Obj.burnt_snail, intArrayOf(Obj.snail_corpse_cooked2), 17, 50, 80.0),
        CAVE_POTATO(Obj.rand_raw_potato, -1, intArrayOf(Obj.rand_potato), 1, 1, 9.0),
        HEIM_CRAB(Obj.rand_raw_fish_1, Obj.rand_burnt_fish_1, intArrayOf(Obj.rand_fish_1), 1, 20, 22.0),
        RED_EYE(Obj.rand_raw_fish_2, Obj.rand_burnt_fish_2, intArrayOf(Obj.rand_fish_2), 10, 30, 41.0),
        DUSK_EEL(Obj.rand_raw_fish_3, Obj.rand_burnt_fish_3, intArrayOf(Obj.rand_fish_3), 20, 40, 61.0),
        GIANT_FLATFISH(Obj.rand_raw_fish_4, Obj.rand_burnt_fish_4, intArrayOf(Obj.rand_fish_4), 30, 50, 82.0),
        SHORTFINNED_EEL(Obj.rand_raw_fish_5, Obj.rand_burnt_fish_5, intArrayOf(Obj.rand_fish_5), 40, 60, 103.0),
        WEB_SNIPPER(Obj.rand_raw_fish_6, Obj.rand_burnt_fish_6, intArrayOf(Obj.rand_fish_6), 50, 70, 124.0),
        BOULDABASS(Obj.rand_raw_fish_7, Obj.rand_burnt_fish_7, intArrayOf(Obj.rand_fish_7), 60, 70, 146.0),
        SALVE_EEL(Obj.rand_raw_fish_8, Obj.rand_burnt_fish_8, intArrayOf(Obj.rand_fish_8), 70, 70, 168.0),
        BLUE_CRAB(Obj.rand_raw_fish_9, Obj.rand_burnt_fish_9, intArrayOf(Obj.rand_fish_9), 80, 70, 191.0),
        CAVE_MORAY(Obj.rand_raw_fish_10, Obj.rand_burnt_fish_10, intArrayOf(Obj.rand_fish_10), 90, 70, 215.0);

        constructor(raw: Int, burnt: Int, product: IntArray, level: Int, burningLvl: Int, xp: Double, spitRoast: Boolean = false, fireOnly: Boolean = false, rangeOnly: Boolean = false, returnItemId: Int = -1) :
            this(Item(raw), Item(burnt), product, level, burningLvl, xp, spitRoast, fireOnly, rangeOnly, returnItemId)

        companion object {
            private val byRaw = entries.associateBy { it.rawItem.id }

            @JvmStatic
            fun forId(itemId: Int) = byRaw[itemId]
        }
    }

    private val product = cookable.productItem.getOrNull(option)

    override fun start(player: Player): Boolean {
        if (product == null) return false
        player.faceObject(gameObject)
        return true
    }

    override fun process(player: Player): Boolean {
        if (!ChunkManager.getChunk(gameObject.tile.getChunkId()).objectExists(gameObject)) return false
        if (!player.inventory.hasFreeSlots() && (cookable.rawItem.definitions.isStackable() || cookable.returnItemId != -1)) {
            if (cookable.returnItemId != -1) player.sendMessage("You need a free inventory slot for the cake tin.")
            return false
        }
        return player.inventory.containsItem(cookable.rawItem.id, 1)
    }

    override fun processWithDelay(player: Player): Int {
        val product = product ?: return -1
        val name = Item(product).definitions.name.lowercase()
        quantity--
        player.anim(if (gameObject.getDefinitions(player).name == "Fire") Seq.human_firecooking else Seq.human_cooking)
        player.inventory.deleteItem(cookable.rawItem.id, 1)
        if (burns(player, product)) {
            player.inventory.addItem(cookable.burntItem.id, cookable.burntItem.amount)
            player.sendMessage("You accidentally burn the $name.", true)
        } else {
            player.inventory.addItem(product, 1)
            val bonfire = gameObject.attribs.getI("bonfire_participants") > 0
            player.skills.addXp(Constants.COOKING, if (product == Obj.xbows_sinew) SINEW_XP else if (bonfire) cookable.xp * (1 + BONFIRE_XP_BONUS) else cookable.xp)
            player.sendMessage("You successfully cook ${if (cookable == Cookables.RAW_SHRIMP || cookable == Cookables.RAW_ANCHOVIES) "some" else "a"} $name.", true)
        }
        if (cookable.returnItemId != -1) player.inventory.addItem(cookable.returnItemId, 1)
        player.packets.sendSound(Sound(SoundId.sound_2577, 0, SoundType.EFFECT))
        return if (quantity > 0) COOK_TICKS else -1
    }

    override fun stop(player: Player) = setActionDelay(player, COOK_TICKS)

    private fun burns(player: Player, product: Int): Boolean {
        if (product == Obj.xbows_sinew) return false
        var burnLevel = cookable.burningLvl
        if (player.equipment.glovesId == Obj.gauntlets_of_cooking) burnLevel -= BURN_LEVEL_REDUCTION
        if (player.hasEffect(Effect.LIVID_FISH_COOKING_BOOST)) burnLevel -= BURN_LEVEL_REDUCTION
        val level = player.skills.getLevel(Constants.COOKING)
        if (level >= burnLevel) return false
        return (level.toDouble() / burnLevel).coerceAtLeast(MIN_SUCCESS_CHANCE) < Math.random()
    }
}

@ServerStartupEvent
fun mapCooking() {
    onItemOnObject(COOKING_LOCS, Cooking.Cookables.entries.map { it.rawItem.id }.toTypedArray<Any>()) { (player, obj, item) ->
        if (item.id == Obj.raw_chompy && reservesQuestChompy(player, obj.getDefinitions(player).name)) return@onItemOnObject
        player.startConversation(CookingD(player, Cooking.Cookables.forId(item.id), obj))
    }
}
