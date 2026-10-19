package com.rs.game.content.skills.mining

import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

enum class Pickaxe(val itemId: Int, val level: Int, val animId: Int, val ticks: Int) {
    BRONZE(Obj.bronze_pickaxe, 1, Seq.seq_625, 8),
    BRONZE_G(Obj.lfm_gilded_bronze_pickaxe, 1, Seq.seq_234, 8),
    IRON(Obj.iron_pickaxe, 1, Seq.seq_626, 7),
    IRON_G(Obj.lfm_gilded_iron_pickaxe, 1, Seq.seq_235, 7),
    STEEL(Obj.steel_pickaxe, 6, Seq.seq_627, 6),
    STEEL_G(Obj.lfm_gilded_steel_pickaxe, 6, Seq.seq_236, 6),
    MITHRIL(Obj.mithril_pickaxe, 21, Seq.seq_629, 5),
    MITHRIL_G(Obj.lfm_gilded_mithril_pickaxe, 21, Seq.seq_238, 5),
    ADAMANT(Obj.adamant_pickaxe, 31, Seq.seq_628, 4),
    ADAMANT_G(Obj.lfm_gilded_adamant_pickaxe, 31, Seq.seq_237, 4),
    RUNE(Obj.rune_pickaxe, 41, Seq.seq_624, 3),
    RUNE_G(Obj.lfm_gilded_rune_pickaxe, 41, Seq.seq_249, 3),
    DRAGON(Obj.dragon_pickaxe, 61, Seq.seq_12189, 3),
    DRAGON_G(Obj.lfm_gilded_dragon_pickaxe, 61, Seq.seq_250, 3),
    INFERNO_ADZE(Obj.firedup_adze, 61, Seq.human_mining_firedup_adze, 3),
    SACRED_CLAY(Obj.sc_reward_pickaxe, 40, Seq.seq_11019, 3),
    VOLATILE_CLAY(Obj.sc_reward_volatile_pickaxe, 40, Seq.seq_11019, 3);

    companion object {
        @JvmStatic
        fun getBest(player: Player): Pickaxe? = entries.lastOrNull {
            (player.inventory.containsItem(it.itemId) || player.equipment.weaponId == it.itemId) && player.skills.getLevel(Constants.MINING) >= it.level
        }
    }
}
