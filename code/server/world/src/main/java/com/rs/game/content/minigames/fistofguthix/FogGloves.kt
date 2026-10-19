package com.rs.game.content.minigames.fistofguthix

import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import com.rs.game.model.item.ItemTooltips
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onXpDrop
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.GroundItem
import org.darkan.core.model.Item

const val GLOVE_CHARGES_KEY = "fogGloveCharges"
private const val FISHING_BONUS_XP = 100.0
private const val HERB_FORCE_PERCENT = 50
private const val DRAGON_SLAYER_BONUS = 0.10
private const val DRAGON_TASK = "dragon"

enum class FogGlove(val obj: Int, val lifetime: Int, val target: Int) {
    AIR(Obj.hvh_air_gloves, 1000, Obj.airrune),
    WATER(Obj.hvh_water_gloves, 1000, Obj.waterrune),
    EARTH(Obj.hvh_earth_gloves, 1000, Obj.earthrune),
    SWORDFISH(Obj.hvh_swordfish_gloves, 1000, Obj.raw_swordfish),
    SHARK(Obj.hvh_shark_gloves, 1000, Obj.raw_shark),
    IRIT(Obj.hvh_irit_gloves, 100, Obj.unidentified_irit),
    AVANTOE(Obj.hvh_avantoe_gloves, 100, Obj.unidentified_avantoe),
    KWUARM(Obj.hvh_kwuarm_gloves, 100, Obj.unidentified_kwuarm),
    CADANTINE(Obj.hvh_cadantine_gloves, 100, Obj.unidentified_cadantine),
    DRAGON_SLAYER(Obj.hvh_dragon_slayer_gloves, 1000, -1);

    companion object {
        private val byObj = entries.associateBy { it.obj }
        fun worn(player: Player) = player.equipment.getItem(Equipment.HANDS)?.let { item -> byObj[item.id]?.let { it to item } }
    }
}

private val HERB_GLOVES = setOf(FogGlove.IRIT, FogGlove.AVANTOE, FogGlove.KWUARM, FogGlove.CADANTINE)

object FogGloves {
    fun runecraftingBonus(player: Player, rune: Int, xpPerEssence: Double, essence: Int): Double {
        val (glove, item) = FogGlove.worn(player)?.takeIf { it.first.target == rune } ?: return 0.0
        return xpPerEssence * use(player, glove, item, essence)
    }

    fun fishingBonus(player: Player, fish: Int): Double {
        val (glove, item) = FogGlove.worn(player)?.takeIf { it.first.target == fish } ?: return 0.0
        return FISHING_BONUS_XP * use(player, glove, item, 1)
    }

    internal fun use(player: Player, glove: FogGlove, item: Item, amount: Int): Int {
        val charges = charges(item, glove)
        val used = amount.coerceAtMost(charges)
        item.addMetaData(GLOVE_CHARGES_KEY, charges - used)
        item.markChanged()
        if (charges - used <= 0) {
            player.equipment.deleteSlot(Equipment.HANDS)
            player.appearance.generateAppearanceData()
            player.sendMessage("Your ${ItemDefinitions.getDefs(glove.obj).name.lowercase()} crumble to dust.")
        }
        return used
    }

    fun herbDrop(player: Player, drop: GroundItem) {
        val (glove, item) = FogGlove.worn(player)?.takeIf { it.first in HERB_GLOVES } ?: return
        if (drop.id != glove.target && Utils.random(100) < HERB_FORCE_PERCENT) drop.setId(glove.target)
        if (drop.id == glove.target) use(player, glove, item, 1)
    }

    private fun charges(item: Item, glove: FogGlove) = item.getMetaDataI(GLOVE_CHARGES_KEY, glove.lifetime)
}

@ServerStartupEvent
fun mapFogGloves() {
    onXpDrop { e ->
        if (e.skillId != Constants.SLAYER) return@onXpDrop
        val player = e.player
        val (glove, item) = FogGlove.worn(player)?.takeIf { it.first == FogGlove.DRAGON_SLAYER } ?: return@onXpDrop
        val task = player.slayer.task ?: return@onXpDrop
        if (DRAGON_TASK !in task.monster.getName().lowercase()) return@onXpDrop
        if (FogGloves.use(player, glove, item, 1) > 0) player.skills.addXpLamp(Constants.SLAYER, e.xp * DRAGON_SLAYER_BONUS)
    }

    ItemTooltips.metadata(GLOVE_CHARGES_KEY) { _, item, value ->
        val glove = FogGlove.entries.firstOrNull { it.obj == item.id } ?: return@metadata null
        ItemTooltips.count("Charges", (value as Number).toInt(), glove.lifetime)
    }
}
