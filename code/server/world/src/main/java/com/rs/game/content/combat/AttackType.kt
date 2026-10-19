package com.rs.game.content.combat

import org.darkan.core.model.Bonus
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player

enum class AttackType(val attBonus: Bonus, val defBonus: Bonus) {
    STAB(Bonus.STAB_ATT, Bonus.STAB_DEF),
    SLASH(Bonus.SLASH_ATT, Bonus.SLASH_DEF),
    CRUSH(Bonus.CRUSH_ATT, Bonus.CRUSH_DEF),

    ACCURATE(Bonus.RANGE_ATT, Bonus.RANGE_DEF),
    RAPID(Bonus.RANGE_ATT, Bonus.RANGE_DEF),
    LONG_RANGE(Bonus.RANGE_ATT, Bonus.RANGE_DEF),

    POLYPORE_ACCURATE(Bonus.MAGIC_ATT, Bonus.MAGIC_DEF),
    POLYPORE_LONGRANGE(Bonus.MAGIC_ATT, Bonus.MAGIC_DEF),

    MAGIC(Bonus.MAGIC_ATT, Bonus.MAGIC_DEF);
}
