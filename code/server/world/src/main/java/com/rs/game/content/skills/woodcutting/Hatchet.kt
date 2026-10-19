package com.rs.game.content.skills.woodcutting

import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

enum class Hatchet(val itemId: Int, val useLevel: Int, val toolMod: Double, private vararg val animations: Int) {
    BRONZE(Obj.bronze_axe, 1, 0.5, Seq.wc_update09_player_woodcutting_hollow_tree_bronze_axe, Seq.brut_human_canoeing_carve_bronze_axe, Seq.jaro_player_woodcutting_bronze_axe, Seq.mtx_player_woodcutting_bronze_axe, Seq.human_woodcutting_bronze_axe_abyssal_priorities, Seq.seq_879, Seq.clan_field_playerdestroybarrier_hatchet_bronze, Seq.wc_update09_player_splitting_logs_bronze_axe, Seq.mah1_player_make_canoe_bronze_axe, Seq.wc_update09_player_cutting_ivy_bronze_axe, Seq.sum1_player_magictree_fail_bronze_axe),
    IRON(Obj.iron_axe, 1, 0.6, Seq.wc_update09_player_woodcutting_hollow_tree_iron_axe, Seq.brut_human_canoeing_carve_iron_axe, Seq.jaro_player_woodcutting_iron_axe, Seq.mtx_player_woodcutting_iron_axe, Seq.human_woodcutting_iron_axe_abyssal_priorities, Seq.seq_877, Seq.clan_field_playerdestroybarrier_hatchet_iron, Seq.wc_update09_player_splitting_logs_iron_axe, Seq.mah1_player_make_canoe_iron_axe, Seq.wc_update09_player_cutting_ivy_iron_axe, Seq.sum1_player_magictree_fail_iron_axe),
    STEEL(Obj.steel_axe, 6, 0.7, Seq.wc_update09_player_woodcutting_hollow_tree_steel_axe, Seq.brut_human_canoeing_carve_steel_axe, Seq.jaro_player_woodcutting_steel_axe, Seq.mtx_player_woodcutting_steel_axe, Seq.human_woodcutting_steel_axe_abyssal_priorities, Seq.seq_875, Seq.clan_field_playerdestroybarrier_hatchet_steel, Seq.wc_update09_player_splitting_logs_steel_axe, Seq.mah1_player_make_canoe_steel_axe, Seq.wc_update09_player_cutting_ivy_steel_axe, Seq.sum1_player_magictree_fail_steel_axe),
    BLACK(Obj.black_axe, 6, 0.7, Seq.wc_update09_player_woodcutting_hollow_tree_black_axe, Seq.brut_human_canoeing_carve_black_axe, Seq.jaro_player_woodcutting_black_axe, Seq.mtx_player_woodcutting_black_axe, Seq.human_woodcutting_black_axe_abyssal_priorities, Seq.seq_873, -1, -1, Seq.mah1_player_make_canoe_black_axe, Seq.wc_update09_player_cutting_ivy_black_axe, Seq.sum1_player_magictree_fail_black_axe),
    MITHRIL(Obj.mithril_axe, 21, 0.8, Seq.wc_update09_player_woodcutting_hollow_tree_mithril_axe, Seq.brut_human_canoeing_carve_mithril_axe, Seq.jaro_player_woodcutting_mithril_axe, Seq.mtx_player_woodcutting_mithril_axe, Seq.human_woodcutting_mithril_axe_abyssal_priorities, Seq.seq_871, Seq.clan_field_playerdestroybarrier_hatchet_mithril, Seq.wc_update09_player_splitting_logs_mithril_axe, Seq.mah1_player_make_canoe_mithril_axe, Seq.wc_update09_player_cutting_ivy_mithril_axe, Seq.sum1_player_magictree_fail_mithril_axe),
    ADAMANT(Obj.adamant_axe, 31, 1.0, Seq.wc_update09_player_woodcutting_hollow_tree_adamant_axe, Seq.brut_human_canoeing_carve_adamant_axe, Seq.jaro_player_woodcutting_adamant_axe, Seq.mtx_player_woodcutting_adamant_axe, Seq.human_woodcutting_adamant_axe_abyssal_priorities, Seq.seq_869, Seq.clan_field_playerdestroybarrier_hatchet_adamant, Seq.wc_update09_player_splitting_logs_adamant_axe, Seq.mah1_player_make_canoe_adamant_axe, Seq.wc_update09_player_cutting_ivy_adamant_axe, Seq.sum1_player_magictree_fail_adamant_axe),
    RUNE(Obj.rune_axe, 41, 1.2, Seq.wc_update09_player_woodcutting_hollow_tree_rune_axe, Seq.brut_human_canoeing_carve_rune_axe, Seq.jaro_player_woodcutting_rune_axe, Seq.mtx_player_woodcutting_rune_axe, Seq.human_woodcutting_rune_axe_abyssal_priorities, Seq.seq_867, Seq.clan_field_playerdestroybarrier_hatchet_rune, Seq.wc_update09_player_splitting_logs_rune_axe, Seq.mah1_player_make_canoe_rune_axe, Seq.wc_update09_player_cutting_ivy_rune_axe, Seq.sum1_player_magictree_fail_rune_axe),
    DRAGON(Obj.dragon_axe, 61, 1.3, Seq.wc_update09_player_woodcutting_hollow_tree_dragon_axe, Seq.brut_human_canoeing_carve_dragon_axe, Seq.jaro_player_woodcutting_dragon_axe, Seq.mtx_player_woodcutting_dragon_axe, Seq.human_woodcutting_dragon_axe_abyssal_priorities, Seq.seq_2846, Seq.clan_field_playerdestroybarrier_hatchet_dragon, Seq.wc_update09_player_splitting_logs_dragon_axe, Seq.mah1_player_make_canoe_dragon_axe, Seq.seq_870, Seq.sum1_player_magictree_fail_dragon_axe),
    INFERNO(Obj.firedup_adze, 61, 1.3, Seq.wc_update09_player_woodcutting_hollow_tree_firedup_adze, Seq.brut_human_canoeing_carve_firedup_adze, Seq.jaro_player_woodcutting_firedup_adze, Seq.mtx_player_woodcutting_firedup_adze, Seq.human_woodcutting_firedup_adze_abyssal_priorities, Seq.firedup_adze_woodcut_success, Seq.clan_field_playerdestroybarrier_adze, Seq.wc_update09_player_splitting_logs_firedup_adze, Seq.mah1_player_make_canoe_firedup_adze, Seq.wc_update09_player_cutting_ivy_firedup_adze, Seq.sum1_player_magictree_fail_firedup_adze),
    SACRED_CLAY(Obj.sc_reward_axe, 40, 1.2, Seq.wc_update09_player_woodcutting_hollow_tree_sc_reward_axe, Seq.brut_human_canoeing_carve_sc_reward_axe, Seq.jaro_player_woodcutting_sc_reward_axe, Seq.mtx_player_woodcutting_sc_reward_axe, Seq.human_woodcutting_sc_reward_axe_abyssal_priorities, Seq.seq_1905, Seq.clan_field_playerdestroybarrier_hatchet_sc, Seq.wc_update09_player_splitting_logs_sc_reward_axe, Seq.mah1_player_make_canoe_sc_reward_axe, Seq.wc_update09_player_cutting_ivy_sc_reward_axe, Seq.sum1_player_magictree_fail_sc_reward_axe),
    VOLATILE_CLAY(Obj.sc_reward_volatile_axe, 40, 1.2, Seq.wc_update09_player_woodcutting_hollow_tree_sc_reward_volatile_axe, Seq.brut_human_canoeing_carve_sc_reward_volatile_axe, Seq.jaro_player_woodcutting_sc_reward_volatile_axe, Seq.mtx_player_woodcutting_sc_reward_volatile_axe, Seq.human_woodcutting_sc_reward_volatile_axe_abyssal_priorities, Seq.seq_1907, Seq.clan_field_playerdestroybarrier_hatchet_sc_volatile, Seq.wc_update09_player_splitting_logs_sc_reward_volatile_axe, Seq.mah1_player_make_canoe_sc_reward_volatile_axe, Seq.wc_update09_player_cutting_ivy_sc_reward_volatile_axe, Seq.sum1_player_magictree_fail_sc_reward_volatile_axe);

    fun animNormal() = animations[5]

    fun animIvy() = animations[9]

    fun animCanoe() = animations[1]

    fun getAnim(type: TreeType) = if (type == TreeType.IVY) animIvy() else animNormal()

    companion object {
        @JvmStatic
        fun getBest(player: Player): Hatchet? = entries.lastOrNull {
            (player.inventory.containsItem(it.itemId) || player.equipment.weaponId == it.itemId) && player.skills.getLevel(Constants.WOODCUTTING) >= it.useLevel
        }
    }
}
