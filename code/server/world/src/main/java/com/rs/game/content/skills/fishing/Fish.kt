package com.rs.game.content.skills.fishing

import com.rs.game.content.Effect
import com.rs.game.content.minigames.fistofguthix.FogGloves
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private const val GOLDEN_SHARK_PERCENT = 30
private const val FISHING_SUIT_BONUS = 1.025

enum class Fish(
    val fishId: Int,
    val level: Int,
    val xp: Double,
    private val rate1: Int,
    private val rate99: Int,
    private val extraRequirements: ((Player) -> Boolean)? = null,
    private val extraRewards: ((Player) -> Unit)? = null,
) {
    CRAYFISH(Obj.raw_crayfish, 1, 10.0, 60, 180),
    KARAMBWANJI(Obj.tbwt_raw_karambwanji, 5, 5.0, 100, 250),
    KARAMBWAN(Obj.tbwt_raw_karambwan, 65, 50.0, 1, 160),
    SHRIMP(Obj.raw_shrimp, 1, 10.0, 60, 180),
    ANCHOVIES(Obj.raw_anchovies, 15, 40.0, 30, 90),
    SARDINES(Obj.raw_sardine, 5, 20.0, 32, 180),
    GIANT_CARP(Obj.raw_giant_carp, 10, 50.0, 32, 192),
    HERRING(Obj.raw_herring, 10, 30.0, 16, 96),
    TROUT(Obj.raw_trout, 20, 50.0, 32, 192),
    SALMON(Obj.raw_salmon, 30, 70.0, 16, 96),
    PIKE(Obj.raw_pike, 25, 60.0, 16, 110),
    FROGSPAWN(Obj.giant_frogspawn, 33, 75.0, 16, 96),
    TUNA(Obj.raw_tuna, 35, 80.0, 16, 70, extraRewards = { quickReflexes(it, Obj.raw_tuna, 35) }),
    SWORDFISH(Obj.raw_swordfish, 50, 100.0, 8, 50, extraRewards = { quickReflexes(it, Obj.raw_swordfish, 50) }),
    LOBSTER(Obj.raw_lobster, 40, 90.0, 1, 120),
    MACKEREL(Obj.raw_mackerel, 16, 20.0, 10, 50),
    COD(Obj.raw_cod, 23, 45.0, 6, 30),
    BASS(Obj.raw_bass, 46, 100.0, 6, 20),
    LEATHER_BOOTS(Obj.leather_boots, 16, 1.0, 8, 8),
    SEAWEED(Obj.seaweed, 16, 1.0, 8, 8),
    LEATHER_GLOVES(Obj.leather_gloves, 16, 1.0, 4, 4),
    OYSTER(Obj.oystershell, 16, 10.0, 4, 4),
    CASKET(Obj.casket, 16, 10.0, 2, 3),
    LEAPING_TROUT(Obj.brut_spawning_trout, 48, 50.0, 32, 192, { strongAndAgile(it, 15) }, { barbarianXp(it, 5.0) }),
    LAVA_EEL(Obj.raw_lava_eel, 53, 60.0, 25, 80),
    LEAPING_SALMON(Obj.brut_spawning_salmon, 58, 70.0, 16, 96, { strongAndAgile(it, 30) }, { barbarianXp(it, 6.0) }),
    LEAPING_STURGEON(Obj.brut_sturgeon, 70, 80.0, 8, 64, { strongAndAgile(it, 45) }, { barbarianXp(it, 7.0) }),
    MONKFISH(Obj.raw_monkfish, 62, 120.0, 25, 80),
    SHARK(Obj.raw_shark, 76, 110.0, 1, 37, extraRewards = { quickReflexes(it, Obj.raw_shark, 76) }),
    CAVEFISH(Obj.lrc_cavefish_raw, 85, 300.0, -40, 40),
    ROCKTAIL(Obj.lrc_rocktail_raw, 90, 385.0, -35, 35);

    fun checkRequirements(player: Player) = player.skills.getLevel(Constants.FISHING) >= level && extraRequirements?.invoke(player) != false

    fun rollSuccess(player: Player, level: Int) = Utils.skillSuccess(level, player.auraManager.fishingMul, rate1, rate99)

    fun giveFish(player: Player, spot: FishingSpot) {
        val fish = Item(fishId)
        deleteBait(player, spot)
        var totalXp = xp
        if (Fishing.hasFishingSuit(player)) totalXp *= FISHING_SUIT_BONUS
        if (fish.id == Obj.raw_shark && player.hasEffect(Effect.JUJU_FISHING) && Utils.random(100) < GOLDEN_SHARK_PERCENT) fish.id = Obj.raw_golden_shark
        player.sendMessage(Fishing.getMessage(this), true)
        if (fish.id != -1) player.inventory.addItem(fish)
        player.skills.addXp(Constants.FISHING, totalXp + FogGloves.fishingBonus(player, fishId))
        player.incrementCount(fish.definitions.name + " caught fishing")
        extraRewards?.invoke(player)
    }

    fun deleteBait(player: Player, spot: FishingSpot) {
        val bait = spot.bait?.firstOrNull { player.inventory.containsItem(it) } ?: return
        player.inventory.deleteItem(bait, 1)
    }
}

private fun quickReflexes(player: Player, fish: Int, agility: Int) {
    if (Utils.random(10) != 0 || player.skills.getLevel(Constants.AGILITY) < agility) return
    player.inventory.addItemDrop(fish, 1)
    player.sendMessage("Your quick reflexes allow you to catch an extra fish!", true)
}

private fun strongAndAgile(player: Player, level: Int) = player.skills.getLevel(Constants.AGILITY) >= level && player.skills.getLevel(Constants.STRENGTH) >= level

private fun barbarianXp(player: Player, xp: Double) {
    player.skills.addXp(Constants.AGILITY, xp)
    player.skills.addXp(Constants.STRENGTH, xp)
}
