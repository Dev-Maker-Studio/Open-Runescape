package com.rs.game.model.entity

import com.rs.game.content.Effect
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.AuraManager.Aura
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varp
import kotlin.jvm.Transient

class Poison {

    @Transient
    var entity: Entity? = null
    private var poisonDamage = 0
    private var poisonCount = 0

    val isPoisoned: Boolean
        get() = poisonDamage >= MIN_POISON_DAMAGE

    val remainingTicks: Int
        get() {
            if (!isPoisoned) return 0
            val hitsLeft = (poisonDamage - MIN_POISON_DAMAGE) / DAMAGE_PER_HIT + 1
            return poisonCount + (hitsLeft - 1) * HIT_INTERVAL_TICKS + 1
        }

    fun makePoisoned(startDamage: Int) {
        if (poisonDamage > startDamage) return
        val entity = this.entity
        if (entity is Player) {
            if (entity.hasEffect(Effect.ANTIPOISON)
                || entity.equipment.shieldId == Obj.rand_reward_anti_poison_totem
                || entity.inventory.containsItem(Obj.rand_reward_anti_poison_totem)
            ) return
            if (poisonDamage == 0) entity.sendMessage("You are poisoned.")
        }
        poisonDamage = startDamage
        refresh()
    }

    fun processPoison() {
        val entity = this.entity ?: return
        if (entity.isDead || !isPoisoned) return
        if (poisonCount > 0) {
            poisonCount--
            return
        }
        var heal = false
        if (entity is Player) {
            if (entity.interfaceManager.containsScreenInter()) return
            if (entity.auraManager.isActivated(Aura.POISON_PURGE, Aura.GREATER_POISON_PURGE, Aura.MASTER_POISON_PURGE, Aura.SUPREME_POISON_PURGE))
                heal = true
            if (!heal) entity.incrementCount("Poison damage taken", poisonDamage)
        }
        entity.applyHit(Hit(entity, poisonDamage, if (heal) HitLook.HEALED_DAMAGE else HitLook.POISON_DAMAGE))
        poisonDamage -= DAMAGE_PER_HIT
        if (isPoisoned) {
            poisonCount = HIT_INTERVAL_TICKS - 1
            return
        }
        reset()
    }

    fun reset() {
        poisonDamage = 0
        poisonCount = 0
        refresh()
    }

    fun refresh() {
        val player = entity as? Player ?: return
        player.vars.setVar(Varp.varp_102, if (isPoisoned) 1 else 0)
    }

    fun lowerPoisonDamage(reduction: Int) {
        poisonDamage = Utils.clampI(poisonDamage - reduction, 0, poisonDamage)
    }
}

private const val MIN_POISON_DAMAGE = 10
private const val DAMAGE_PER_HIT = 2
private const val HIT_INTERVAL_TICKS = 31
