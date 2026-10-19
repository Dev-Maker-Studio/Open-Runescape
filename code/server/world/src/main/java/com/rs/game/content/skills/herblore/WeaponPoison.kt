package com.rs.game.content.skills.herblore

import com.rs.game.content.skills.dungeoneering.DungeonUtils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnItem
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

enum class PoisonableItem(val id: Int, vararg poisonIds: Int) {
    BRONZE_DART(Obj.bronze_dart, Obj.bronze_dart_p, Obj.bronze_dart_pplus, Obj.bronze_dart_pplusplus),
    IRON_DART(Obj.iron_dart, Obj.iron_dart_p, Obj.iron_dart_pplus, Obj.iron_dart_pplusplus),
    STEEL_DART(Obj.steel_dart, Obj.steel_dart_p, Obj.steel_dart_pplus, Obj.steel_dart_pplusplus),
    MITHRIL_DART(Obj.mithril_dart, Obj.mithril_dart_p, Obj.mithril_dart_pplus, Obj.mithril_dart_pplusplus),
    ADAMANT_DART(Obj.adamant_dart, Obj.adamant_dart_p, Obj.adamant_dart_pplus, Obj.adamant_dart_pplusplus),
    RUNE_DART(Obj.rune_dart, Obj.rune_dart_p, Obj.rune_dart_pplus, Obj.rune_dart_pplusplus),
    IRON_JAVELIN(Obj.iron_javelin, Obj.iron_javelin_p, Obj.iron_javelin_pplus, Obj.iron_javelin_pplusplus),
    STEEL_JAVELIN(Obj.steel_javelin, Obj.steel_javelin_p, Obj.steel_javelin_pplus, Obj.steel_javelin_pplusplus),
    MITHRIL_JAVELIN(Obj.mithril_javelin, Obj.mithril_javelin_p, Obj.mithril_javelin_pplus, Obj.mithril_javelin_pplusplus),
    ADAMANT_JAVELIN(Obj.adamant_javelin, Obj.adamant_javelin_p, Obj.adamant_javelin_pplus, Obj.adamant_javelin_pplusplus),
    RUNE_JAVELIN(Obj.rune_javelin, Obj.rune_javelin_p, Obj.rune_javelin_pplus, Obj.rune_javelin_pplusplus),
    IRON_KNIFE(Obj.iron_knife, Obj.iron_knife_p, Obj.iron_knife_pplus, Obj.iron_knife_pplusplus),
    BRONZE_KNIFE(Obj.bronze_knife, Obj.bronze_knife_p, Obj.bronze_knife_pplus, Obj.bronze_knife_pplusplus),
    STEEL_KNIFE(Obj.steel_knife, Obj.steel_knife_p, Obj.steel_knife_pplus, Obj.steel_knife_pplusplus),
    MITHRIL_KNIFE(Obj.mithril_knife, Obj.mithril_knife_p, Obj.mithril_knife_pplus, Obj.mithril_knife_pplusplus),
    ADAMANT_KNIFE(Obj.adamant_knife, Obj.adamant_knife_p, Obj.adamant_knife_pplus, Obj.adamant_knife_pplusplus),
    RUNE_KNIFE(Obj.rune_knife, Obj.rune_knife_p, Obj.rune_knife_pplus, Obj.rune_knife_pplusplus),
    BLACK_KNIFE(Obj.black_knife, Obj.black_knife_p, Obj.black_knife_pplus, Obj.black_knife_pplusplus),
    BRONZE_BOLTS(Obj.bolt, Obj.poison_bolt, Obj.poison_boltplus, Obj.poison_boltplusplus),
    BRONZE_ARROW(Obj.bronze_arrow, Obj.bronze_arrow_p, Obj.bronze_arrow_pplus, Obj.bronze_arrow_pplusplus),
    IRON_ARROW(Obj.iron_arrow, Obj.iron_arrow_p, Obj.iron_arrow_pplus, Obj.iron_arrow_pplusplus),
    STEEL_ARROW(Obj.steel_arrow, Obj.steel_arrow_p, Obj.steel_arrow_pplus, Obj.steel_arrow_pplusplus),
    MITHRIL_ARROW(Obj.mithril_arrow, Obj.mithril_arrow_p, Obj.mithril_arrow_pplus, Obj.mithril_arrow_pplusplus),
    ADAMANT_ARROW(Obj.adamant_arrow, Obj.adamant_arrow_p, Obj.adamant_arrow_pplus, Obj.adamant_arrow_pplusplus),
    RUNE_ARROW(Obj.rune_arrow, Obj.rune_arrow_p, Obj.rune_arrow_pplus, Obj.rune_arrow_pplusplus),
    IRON_DAGGER(Obj.iron_dagger, Obj.iron_dagger_p, Obj.iron_dagger_pplus, Obj.iron_dagger_pplusplus),
    BRONZE_DAGGER(Obj.bronze_dagger, Obj.bronze_dagger_p, Obj.bronze_dagger_pplus, Obj.bronze_dagger_pplusplus),
    STEEL_DAGGER(Obj.steel_dagger, Obj.steel_dagger_p, Obj.steel_dagger_pplus, Obj.steel_dagger_pplusplus),
    MITHRIL_DAGGER(Obj.mithril_dagger, Obj.mithril_dagger_p, Obj.mithril_dagger_pplus, Obj.mithril_dagger_pplusplus),
    ADAMANT_DAGGER(Obj.adamant_dagger, Obj.adamant_dagger_p, Obj.adamant_dagger_pplus, Obj.adamant_dagger_pplusplus),
    RUNE_DAGGER(Obj.rune_dagger, Obj.rune_dagger_p, Obj.rune_dagger_pplus, Obj.rune_dagger_pplusplus),
    DRAGON_DAGGER(Obj.dragon_dagger, Obj.dragon_dagger_p, Obj.dragon_dagger_pplus, Obj.dragon_dagger_pplusplus),
    BLACK_DAGGER(Obj.black_dagger, Obj.black_dagger_p, Obj.black_dagger_pplus, Obj.black_dagger_pplusplus),
    BRONZE_SPEAR(Obj.bronze_spear, Obj.bronze_spear_p, Obj.bronze_spear_pplus, Obj.bronze_spear_pplusplus, Obj.tbwt_bronze_spear_kp),
    IRON_SPEAR(Obj.iron_spear, Obj.iron_spear_p, Obj.iron_spear_pplus, Obj.iron_spear_pplusplus, Obj.tbwt_iron_spear_kp),
    STEEL_SPEAR(Obj.steel_spear, Obj.steel_spear_p, Obj.steel_spear_pplus, Obj.steel_spear_pplusplus, Obj.tbwt_steel_spear_kp),
    MITHRIL_SPEAR(Obj.mithril_spear, Obj.mithril_spear_p, Obj.mithril_spear_pplus, Obj.mithril_spear_pplusplus, Obj.tbwt_mithril_spear_kp),
    ADAMANT_SPEAR(Obj.adamant_spear, Obj.adamant_spear_p, Obj.adamant_spear_pplus, Obj.adamant_spear_pplusplus, Obj.tbwt_adamant_spear_kp),
    RUNE_SPEAR(Obj.rune_spear, Obj.rune_spear_p, Obj.rune_spear_pplus, Obj.rune_spear_pplusplus, Obj.tbwt_rune_spear_kp),
    DRAGON_SPEAR(Obj.dragon_spear, Obj.dragon_spear_p, Obj.dragon_spear_pplus, Obj.dragon_spear_pplusplus, Obj.tbwt_dragon_spear_kp),
    BLACK_DART(Obj.black_dart, Obj.black_dart_p, Obj.black_dart_pplus, Obj.black_dart_pplusplus),
    BLACK_SPEAR(Obj.black_spear, Obj.black_spear_p, Obj.black_spear_pplus, Obj.black_spear_pplusplus, Obj.tbwt_black_spear_kp),
    WHITE_DAGGER(Obj.white_dagger, Obj.white_dagger_p, Obj.white_dagger_pplus, Obj.white_dagger_pplusplus),
    BONE_DAGGER(Obj.dttd_bone_dagger, Obj.dttd_bone_dagger_p, Obj.dttd_bone_dagger_pplus, Obj.dttd_bone_dagger_pplusplus),
    BLURITE_BOLTS(Obj.xbows_crossbow_bolts_blurite, Obj.xbows_crossbow_bolts_blurite_poisoned, Obj.xbows_crossbow_bolts_blurite_poisonedplus, Obj.xbows_crossbow_bolts_blurite_poisonedplusplus),
    IRON_BOLTS(Obj.xbows_crossbow_bolts_iron, Obj.xbows_crossbow_bolts_iron_poisoned, Obj.xbows_crossbow_bolts_iron_poisonedplus, Obj.xbows_crossbow_bolts_iron_poisonedplusplus),
    STEEL_BOLTS(Obj.xbows_crossbow_bolts_steel, Obj.xbows_crossbow_bolts_steel_poisoned, Obj.xbows_crossbow_bolts_steel_poisonedplus, Obj.xbows_crossbow_bolts_steel_poisonedplusplus),
    MITHRIL_BOLTS(Obj.xbows_crossbow_bolts_mithril, Obj.xbows_crossbow_bolts_mithril_poisoned, Obj.xbows_crossbow_bolts_mithril_poisonedplus, Obj.xbows_crossbow_bolts_mithril_poisonedplusplus),
    ADAMANT_BOLTS(Obj.xbows_crossbow_bolts_adamantite, Obj.xbows_crossbow_bolts_adamantite_poisoned, Obj.xbows_crossbow_bolts_adamantite_poisonedplus, Obj.xbows_crossbow_bolts_adamantite_poisonedplusplus),
    RUNITE_BOLTS(Obj.obj_9144, Obj.obj_9291, Obj.obj_9298, Obj.obj_9305),
    SILVER_BOLTS(Obj.xbows_crossbow_bolts_silver, Obj.xbows_crossbow_bolts_silver_poisoned, Obj.xbows_crossbow_bolts_silver_poisonedplus, Obj.xbows_crossbow_bolts_silver_poisonedplusplus),
    KERIS(Obj.contact_keris, Obj.contact_keris_p, Obj.contact_keris_pplus, Obj.contact_keris_pplusplus),
    DRAGON_ARROW(Obj.dragon_arrow, Obj.dragon_arrow_p, Obj.dragon_arrow_pplus, Obj.dragon_arrow_pplusplus),
    DRAGON_DART(Obj.dragon_dart, Obj.dragon_dart_p, Obj.dragon_dart_pplus, Obj.dragon_dart_pplusplus),
    BRONZE_HASTA(Obj.brut_bronze_spear, Obj.brut_bronze_spear_p, Obj.brut_bronze_spear_pplus, Obj.brut_bronze_spear_pplusplus, Obj.brut_bronze_spear_kp),
    IRON_HASTA(Obj.brut_iron_spear, Obj.brut_iron_spear_p, Obj.brut_iron_spear_pplus, Obj.brut_iron_spear_pplusplus, Obj.brut_iron_spear_kp),
    STEEL_HASTA(Obj.brut_steel_spear, Obj.brut_steel_spear_p, Obj.brut_steel_spear_pplus, Obj.brut_steel_spear_pplusplus, Obj.brut_steel_spear_kp),
    MITHRIL_HASTA(Obj.brut_mithril_spear, Obj.brut_mithril_spear_p, Obj.brut_mithril_spear_pplus, Obj.brut_mithril_spear_pplusplus, Obj.brut_mithril_spear_kp),
    ADAMANT_HASTA(Obj.brut_adamant_spear, Obj.brut_adamant_spear_p, Obj.brut_adamant_spear_pplus, Obj.brut_adamant_spear_pplusplus, Obj.brut_adamant_spear_kp),
    RUNE_HASTA(Obj.brut_rune_spear, Obj.brut_rune_spear_p, Obj.brut_rune_spear_pplus, Obj.brut_rune_spear_pplusplus, Obj.brut_rune_spear_kp),
    BLACK_BOLTS(Obj.trail_black_crossbow_bolts, Obj.trail_black_crossbow_bolts_poison, Obj.trail_black_crossbow_bolts_poisonplus, Obj.trail_black_crossbow_bolts_poisonplusplus),
    MORRIGANS_JAVELIN(Obj.pvpw_javelin, Obj.pvpw_javelin_p, Obj.pvpw_javelin_pplus, Obj.pvpw_javelin_pplusplus),
    NOVITE_ARROWS(Obj.rand_arrow_1, -1, -1, -1, -1, Obj.obj_16482, Obj.obj_16537, Obj.obj_16592),
    BATHUS_ARROWS(Obj.rand_arrow_2, -1, -1, -1, -1, Obj.obj_16487, Obj.obj_16542, Obj.obj_16597),
    MARMAROS_ARROWS(Obj.rand_arrow_3, -1, -1, -1, -1, Obj.obj_16492, Obj.obj_16547, Obj.obj_16602),
    KRATONITE_ARROWS(Obj.rand_arrow_4, -1, -1, -1, -1, Obj.obj_16497, Obj.obj_16552, Obj.obj_16607),
    FRACTITE_ARROWS(Obj.rand_arrow_5, -1, -1, -1, -1, Obj.obj_16502, Obj.obj_16557, Obj.obj_16612),
    ZEPHYRIUM_ARROWS(Obj.rand_arrow_6, -1, -1, -1, -1, Obj.obj_16507, Obj.obj_16562, Obj.obj_16617),
    ARGONITE_ARROWS(Obj.rand_arrow_7, -1, -1, -1, -1, Obj.obj_16512, Obj.obj_16567, Obj.obj_16622),
    KATAGON_ARROWS(Obj.rand_arrow_8, -1, -1, -1, -1, Obj.obj_16517, Obj.obj_16572, Obj.obj_16627),
    GORGONITE_ARROWS(Obj.rand_arrow_9, -1, -1, -1, -1, Obj.obj_16522, Obj.obj_16577, Obj.obj_16632),
    PROMETHIUM_ARROWS(Obj.rand_arrow_10, -1, -1, -1, -1, Obj.obj_16527, Obj.obj_16582, Obj.obj_16637),
    SAGITTARIAN_ARROWS(Obj.rand_arrow_11, -1, -1, -1, -1, Obj.obj_16532, Obj.obj_16587, Obj.obj_16642),
    NOVITE_DAGGER(Obj.rand_dagger_1, -1, -1, -1, -1, Obj.rand_dagger_1_p, Obj.rand_dagger_1_pp, Obj.rand_dagger_1_ppp),
    BATHUS_DAGGER(Obj.rand_dagger_2, -1, -1, -1, -1, Obj.rand_dagger_2_p, Obj.rand_dagger_2_pp, Obj.rand_dagger_2_ppp),
    MARMAROS_DAGGER(Obj.rand_dagger_3, -1, -1, -1, -1, Obj.rand_dagger_3_p, Obj.rand_dagger_3_pp, Obj.rand_dagger_3_ppp),
    KRATONITE_DAGGER(Obj.rand_dagger_4, -1, -1, -1, -1, Obj.rand_dagger_4_p, Obj.rand_dagger_4_pp, Obj.rand_dagger_4_ppp),
    FRACTITE_DAGGER(Obj.rand_dagger_5, -1, -1, -1, -1, Obj.rand_dagger_5_p, Obj.rand_dagger_5_pp, Obj.rand_dagger_5_ppp),
    ZEPHYRIUM_DAGGER(Obj.rand_dagger_6, -1, -1, -1, -1, Obj.rand_dagger_6_p, Obj.rand_dagger_6_pp, Obj.rand_dagger_6_ppp),
    ARGONITE_DAGGER(Obj.rand_dagger_7, -1, -1, -1, -1, Obj.rand_dagger_7_p, Obj.rand_dagger_7_pp, Obj.rand_dagger_7_ppp),
    KATAGON_DAGGER(Obj.rand_dagger_8, -1, -1, -1, -1, Obj.rand_dagger_8_p, Obj.rand_dagger_8_pp, Obj.rand_dagger_8_ppp),
    GORGONITE_DAGGER(Obj.rand_dagger_9, -1, -1, -1, -1, Obj.rand_dagger_9_p, Obj.rand_dagger_9_pp, Obj.rand_dagger_9_ppp),
    PROMETHIUM_DAGGER(Obj.rand_dagger_10, -1, -1, -1, -1, Obj.rand_dagger_10_p, Obj.rand_dagger_10_pp, Obj.rand_dagger_10_ppp),
    PRIMAL_DAGGER(Obj.rand_dagger_11, -1, -1, -1, -1, Obj.rand_dagger_11_p, Obj.rand_dagger_11_pp, Obj.rand_dagger_11_ppp),
    NOVITE_SPEAR(Obj.rand_spear_1, -1, -1, -1, -1, Obj.rand_spear_1_p, Obj.rand_spear_1_pp, Obj.rand_spear_1_ppp),
    BATHUS_SPEAR(Obj.rand_spear_2, -1, -1, -1, -1, Obj.rand_spear_2_p, Obj.rand_spear_2_pp, Obj.rand_spear_2_ppp),
    MARMAROS_SPEAR(Obj.rand_spear_3, -1, -1, -1, -1, Obj.rand_spear_3_p, Obj.rand_spear_3_pp, Obj.rand_spear_3_ppp),
    KRATONITE_SPEAR(Obj.rand_spear_4, -1, -1, -1, -1, Obj.rand_spear_4_p, Obj.rand_spear_4_pp, Obj.rand_spear_4_ppp),
    FRACTITE_SPEAR(Obj.rand_spear_5, -1, -1, -1, -1, Obj.rand_spear_5_p, Obj.rand_spear_5_pp, Obj.rand_spear_5_ppp),
    ZEPHYRIUM_SPEAR(Obj.rand_spear_6, -1, -1, -1, -1, Obj.rand_spear_6_p, Obj.rand_spear_6_pp, Obj.rand_spear_6_ppp),
    ARGONITE_SPEAR(Obj.rand_spear_7, -1, -1, -1, -1, Obj.rand_spear_7_p, Obj.rand_spear_7_pp, Obj.rand_spear_7_ppp),
    KATAGON_SPEAR(Obj.rand_spear_8, -1, -1, -1, -1, Obj.rand_spear_8_p, Obj.rand_spear_8_pp, Obj.rand_spear_8_ppp),
    GORGONITE_SPEAR(Obj.rand_spear_9, -1, -1, -1, -1, Obj.rand_spear_9_p, Obj.rand_spear_9_pp, Obj.rand_spear_9_ppp),
    PROMETHIUM_SPEAR(Obj.rand_spear_10, -1, -1, -1, -1, Obj.rand_spear_10_p, Obj.rand_spear_10_pp, Obj.rand_spear_10_ppp),
    PRIMAL_SPEAR(Obj.rand_spear_11, -1, -1, -1, -1, Obj.rand_spear_11_p, Obj.rand_spear_11_pp, Obj.rand_spear_11_ppp);

    val poisonIds: IntArray = poisonIds

    companion object {
        private val byId = entries.associateBy { it.id }
        private val baseByPoisoned = entries.flatMap { weapon -> weapon.poisonIds.filter { it != -1 }.map { it to weapon.id } }.toMap()

        private val boundWeapons: Map<Int, IntArray> by lazy {
            entries.filter { it.poisonIds.size == POISONS.size && it.poisonIds[FIRST_DUNGEONEERING_POISON] != -1 }.mapNotNull { weapon ->
                val bound = DungeonUtils.getBindedId(Item(weapon.id, 1))
                if (bound == -1) return@mapNotNull null
                val poisoned = IntArray(POISONS.size) { -1 }
                for (tier in FIRST_DUNGEONEERING_POISON until POISONS.size)
                    poisoned[tier] = DungeonUtils.getBindedId(Item(weapon.poisonIds[tier], 1))
                bound to poisoned
            }.toMap()
        }

        private val boundBaseByPoisoned: Map<Int, Int> by lazy {
            boundWeapons.flatMap { (base, poisoned) -> poisoned.filter { it != -1 }.map { it to base } }.toMap()
        }

        val boundIds: IntArray get() = boundWeapons.keys.toIntArray()
        val boundPoisonedIds: IntArray get() = boundBaseByPoisoned.keys.toIntArray()

        fun poisonedIds(itemId: Int): IntArray? = byId[itemId]?.poisonIds ?: boundWeapons[itemId]

        fun baseOfPoisoned(itemId: Int): Int? = baseByPoisoned[itemId] ?: boundBaseByPoisoned[itemId]
    }
}

private val POISONS = intArrayOf(Obj.obj_187, Obj.obj_5937, Obj.obj_5940, Obj.tbwt_poisonous_karambwan_paste, Obj.rand_poison_1, Obj.rand_poison_2, Obj.rand_poison_3)
private const val FIRST_DUNGEONEERING_POISON = 4
private const val KARAMBWAN_PASTE = 3
private const val WEAPON_POISON_CLEANER = Obj.tbwt_cleaning_cloth
private const val MAX_POISONED_AT_ONCE = 15

@ServerStartupEvent
fun mapWeaponPoison() {
    val weapons = PoisonableItem.entries.map { it.id }.toIntArray() + PoisonableItem.boundIds
    onItemOnItem(weapons, POISONS) { event ->
        val player = event.player
        val target = event.getUsedWith(*POISONS) ?: return@onItemOnItem
        val poison = event.getUsedWith(target.id) ?: return@onItemOnItem
        val poisonedIds = PoisonableItem.poisonedIds(target.id) ?: return@onItemOnItem
        val poisonType = POISONS.indexOf(poison.id)
        if (poisonType < 0 || poisonType >= poisonedIds.size || poisonedIds[poisonType] == -1) return@onItemOnItem
        val amount = minOf(target.amount, MAX_POISONED_AT_ONCE)
        if (amount > 1 && !player.inventory.hasFreeSlots()) {
            player.sendMessage("Not enough space in your inventory.")
            return@onItemOnItem
        }
        if (poisonType == KARAMBWAN_PASTE) {
            player.inventory.deleteItem(poison)
        } else {
            poison.id = if (poisonType >= FIRST_DUNGEONEERING_POISON) Obj.rand_vial_empty else Obj.vial_empty
            player.inventory.refresh(poison.slot)
        }
        if (amount < target.amount) {
            player.inventory.deleteItem(target.id, amount)
            player.inventory.addItem(poisonedIds[poisonType], amount)
        } else {
            target.id = poisonedIds[poisonType]
            player.inventory.refresh(target.slot)
        }
        player.sendMessage("You dip the tip of the ${target.name.lowercase()} in the poison.")
    }
    val poisoned = PoisonableItem.entries.flatMap { weapon -> weapon.poisonIds.filter { it != -1 } }.toIntArray() + PoisonableItem.boundPoisonedIds
    onItemOnItem(poisoned, intArrayOf(WEAPON_POISON_CLEANER)) { event ->
        val target = event.getUsedWith(WEAPON_POISON_CLEANER) ?: return@onItemOnItem
        val base = PoisonableItem.baseOfPoisoned(target.id) ?: return@onItemOnItem
        target.id = base
        event.player.inventory.refresh(target.slot)
        event.player.sendMessage("You clean the poison off the ${target.name.lowercase()}.")
    }
}
