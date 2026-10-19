package com.rs.game.content.minigames.creations

import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varp
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile

enum class ClaySource(val plot: PlotType?, val skill: Int, val tools: IntArray, val firstAnimation: Int, val spots: List<IntArray>) {
    FRAGMENTS(null, Constants.HUNTER, intArrayOf(), Seq.sc_collect_clay_pickup, emptyList()),
    ROCK(PlotType.ROCK, Constants.MINING, intArrayOf(Obj.sc_pickaxe_1, Obj.sc_pickaxe_2, Obj.sc_pickaxe_3, Obj.sc_pickaxe_4, Obj.sc_pickaxe_5), Seq.sc_collect_clay_pickaxe_1, listOf(
        intArrayOf(Loc.sc_mining_rock_tier2_full, Loc.sc_mining_rock_tier2_half, Loc.sc_mining_rock_tier2_empty),
        intArrayOf(Loc.sc_mining_rock_tier3_full, Loc.sc_mining_rock_tier3_half, Loc.sc_mining_rock_tier3_empty),
        intArrayOf(Loc.sc_mining_rock_tier4_full, Loc.sc_mining_rock_tier4_half, Loc.sc_mining_rock_tier4_empty),
        intArrayOf(Loc.sc_mining_rock_tier5_full, Loc.sc_mining_rock_tier5_half, Loc.sc_mining_rock_tier5_empty),
    )),
    TREE(PlotType.TREE, Constants.WOODCUTTING, intArrayOf(Obj.sc_hatchet_1, Obj.sc_hatchet_2, Obj.sc_hatchet_3, Obj.sc_hatchet_4, Obj.sc_hatchet_5), Seq.sc_collect_clay_woodcut_1, listOf(
        intArrayOf(Loc.sc_clay_tree_tier_2_full, Loc.sc_clay_tree_tier_2_half, Loc.sc_clay_tree_tier_2_empty),
        intArrayOf(Loc.sc_clay_tree_tier_3_full, Loc.sc_clay_tree_tier_3_half, Loc.sc_clay_tree_tier_3_empty),
        intArrayOf(Loc.sc_clay_tree_tier_4_full, Loc.sc_clay_tree_tier_4_half, Loc.sc_clay_tree_tier_4_empty),
        intArrayOf(Loc.sc_clay_tree_tier_5_full, Loc.sc_clay_tree_tier_5_half, Loc.sc_clay_tree_tier_5_empty),
    )),
    POOL(PlotType.POOL, Constants.FISHING, intArrayOf(Obj.sc_harpoon_1, Obj.sc_harpoon_2, Obj.sc_harpoon_3, Obj.sc_harpoon_4, Obj.sc_harpoon_5), Seq.sc_collect_clay_harpoon_1, listOf(
        intArrayOf(Loc.sc_clay_pool_tier2_full, Loc.sc_clay_pool_tier2_half, Loc.sc_clay_pool_tier2_empty),
        intArrayOf(Loc.sc_clay_pool_tier3_full, Loc.sc_clay_pool_tier3_half, Loc.sc_clay_pool_tier3_empty),
        intArrayOf(Loc.sc_clay_pool_tier4_full, Loc.sc_clay_pool_tier4_half, Loc.sc_clay_pool_tier4_empty),
        intArrayOf(Loc.sc_clay_pool_tier5_full, Loc.sc_clay_pool_tier5_half, Loc.sc_clay_pool_tier5_empty),
    )),
    SWARM(PlotType.SWARM, Constants.HUNTER, intArrayOf(Obj.sc_butterflynet_1, Obj.sc_butterflynet_2, Obj.sc_butterflynet_3, Obj.sc_butterflynet_4, Obj.sc_butterflynet_5), Seq.sc_collect_clay_butterfly_net_1, listOf(
        intArrayOf(Loc.sc_clay_insects_tier_2_full, Loc.sc_clay_insects_tier_2_half, Loc.sc_clay_insects_tier_2_empty),
        intArrayOf(Loc.sc_clay_insects_tier_3_full, Loc.sc_clay_insects_tier_3_half, Loc.sc_clay_insects_tier_3_empty),
        intArrayOf(Loc.sc_clay_insects_tier_4_full, Loc.sc_clay_insects_tier_4_half, Loc.sc_clay_insects_tier_4_empty),
        intArrayOf(Loc.sc_clay_insects_tier_5_full, Loc.sc_clay_insects_tier_5_half, Loc.sc_clay_insects_tier_5_empty),
    ));

    fun spotsForTier(tier: Int): IntArray = if (tier <= 1) SHARDS else spots[tier - 2]

    fun bestTool(player: Player): Int? = tools.indices.lastOrNull { tier ->
        val tool = tools[tier]
        (player.equipment.weaponId == tool || player.inventory.containsItem(tool, 1)) && player.skills.getLevel(skill) >= StealingCreation.levelFor(tier + 1)
    }

    companion object {
        private val SHARDS = intArrayOf(Loc.sc_clay_shards, Loc.sc_clay_shards, Loc.sc_clay_shards_empty)
        private val gatherSpots: Map<Int, Pair<ClaySource, Int>> = buildMap {
            put(Loc.sc_clay_shards, FRAGMENTS to 1)
            for (source in ClaySource.entries) source.spots.forEachIndexed { index, looks -> put(looks[0], source to index + 2); put(looks[1], source to index + 2) }
        }
        private val byPlot = entries.filter { it.plot != null }.associateBy { it.plot }

        fun forSpot(locId: Int) = gatherSpots[locId]

        fun forPlot(plot: PlotType) = byPlot[plot]
    }
}

object StealingCreation {
    @JvmField val EXIT: Tile = Tile.of(2968, 9710, 0)
    @JvmField val SACRED_CLAY = intArrayOf(Obj.sc_clay_1, Obj.sc_clay_2, Obj.sc_clay_3, Obj.sc_clay_4, Obj.sc_clay_5)
    val GAME_DURATION = Ticks.fromMinutes(20)
    val DESERTER_PENALTY = Ticks.fromMinutes(5)
    const val DESERTER_KEY = "sc_deserter_until"
    const val MUSIC = 578
    private const val LEVELS_PER_CLASS = 20

    private val SCORE_VARPS = intArrayOf(Varp.sc_score_clay_gathered, Varp.sc_score_clay_deposited, Varp.sc_score_clay_processed, Varp.sc_score_clay_taken, Varp.sc_score_damage_inflicted)

    private val CRATES: Map<Int, Int> = buildMap {
        fun crate(loc: Int, vararg ids: Int) = ids.forEach { put(it, loc) }
        crate(Loc.sc_table_clay, *SACRED_CLAY)
        crate(Loc.sc_table_planks, *BarrierTier.entries.map { it.item }.toIntArray())
        crate(Loc.sc_table_tools, *ClaySource.entries.flatMap { it.tools.asIterable() }.toIntArray())
        crate(Loc.sc_table_melee_weapon, *products(KilnProduct.DAGGER, KilnProduct.SCIMITAR, KilnProduct.WARHAMMER))
        crate(Loc.sc_table_melee_armour, *products(KilnProduct.HELM, KilnProduct.PLATEBODY, KilnProduct.PLATELEGS))
        crate(Loc.sc_table_ranger_weapon, *products(KilnProduct.BOW, KilnProduct.ARROWS))
        crate(Loc.sc_table_ranger_armour, *products(KilnProduct.COIF, KilnProduct.LEATHER_BODY, KilnProduct.CHAPS))
        crate(Loc.sc_table_mage_weapon, *products(KilnProduct.STAFF, KilnProduct.ELEMENTAL_RUNES, KilnProduct.CATALYTIC_RUNES))
        crate(Loc.sc_table_mage_armour, *products(KilnProduct.HAT, KilnProduct.ROBE_TOP, KilnProduct.ROBE_BOTTOM))
        crate(Loc.sc_table_food, *products(KilnProduct.FOOD))
        crate(Loc.sc_table_summoning, *products(KilnProduct.POUCH, KilnProduct.SCROLL))
        crate(Loc.sc_table_potions, *ScPotion.entries.flatMap { it.doses.asIterable() }.toIntArray())
    }

    private fun products(vararg products: KilnProduct) = products.flatMap { product -> (1..5).map { product.product(it).id } }.toIntArray()

    fun crateFor(itemId: Int) = CRATES[itemId]

    fun levelFor(clayClass: Int) = (clayClass - 1) * LEVELS_PER_CLASS

    fun cape(red: Boolean) = if (red) Obj.sc_cape_team2 else Obj.sc_cape_team1

    fun spawn(area: GameArea, player: Player, red: Boolean) {
        player.reset()
        player.setCanPvp(true)
        player.setPlayerOption("Follow", 2)
        player.setPlayerOption("Give to", 4)
        player.setPlayerOption("Pickpocket", 5)
        player.setForceMultiArea(true)
        player.useStairs(-1, area.baseEntrance(player.tile, red), 0, 1)
        player.musicsManager.playSongAndUnlock(MUSIC)
    }

    fun giveCape(player: Player, red: Boolean) {
        player.equipment.setSlot(Equipment.CAPE, Item(cape(red)))
        player.appearance.generateAppearanceData()
    }

    @JvmStatic
    fun reset(player: Player) {
        player.stopAll()
        player.equipment.reset()
        player.inventory.reset()
        player.familiar?.sendDeath(player)
        player.familiar = null
        player.appearance.transformIntoNPC(-1)
        player.appearance.isHidden = false
        player.appearance.generateAppearanceData()
        player.reset()
        SCORE_VARPS.forEach { player.vars.setVar(it, 0) }
        player.vars.setVarBit(Varbit.sc_team, 0)
    }

    @JvmStatic
    fun sendHome(player: Player) {
        reset(player)
        player.useStairs(-1, EXIT, 0, 1)
        player.setCanPvp(false)
        player.setForceMultiArea(false)
        player.sendDefaultPlayersOptions()
        player.interfaceManager.removeOverlay(false)
        player.controllerManager.removeControllerWithoutCheck()
    }

    fun showClayStatus(area: GameArea, player: Player) {
        player.interfaceManager.sendInterface(Interface.interface_806)
        val remaining = HashMap<Pair<ClaySource?, Int>, Int>()
        for (x in 0 until area.size) for (y in 0 until area.size) {
            val source = ClaySource.forPlot(area.getType(x, y)) ?: continue
            val tier = area.getTier(x, y)
            if (area.getDegradation(x, y) <= 0 || tier !in 1..5) continue
            val key = (if (tier == 1) null else source) to tier
            remaining[key] = (remaining[key] ?: 0) + 1
        }
        player.packets.sendVarc(Varc.varc_564, remaining[null to 1] ?: 0)
        for ((source, varcs) in CLAY_STATUS_VARCS) varcs.forEachIndexed { index, varc -> player.packets.sendVarc(varc, remaining[source to index + 2] ?: 0) }
    }

    private val CLAY_STATUS_VARCS = mapOf(
        ClaySource.POOL to intArrayOf(Varc.varc_565, Varc.varc_566, Varc.varc_567, Varc.varc_568),
        ClaySource.ROCK to intArrayOf(Varc.varc_569, Varc.varc_570, Varc.varc_571, Varc.varc_572),
        ClaySource.TREE to intArrayOf(Varc.varc_573, Varc.varc_574, Varc.varc_575, Varc.varc_576),
        ClaySource.SWARM to intArrayOf(Varc.varc_577, Varc.varc_578, Varc.varc_579, Varc.varc_580),
    )
}
