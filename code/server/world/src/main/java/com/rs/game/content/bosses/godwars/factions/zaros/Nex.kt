package com.rs.game.content.bosses.godwars.factions.zaros

import com.rs.game.World
import com.rs.game.content.bosses.MinionHost
import com.rs.game.content.bosses.MinionPolicy
import com.rs.game.content.bosses.MinionSpawn
import com.rs.game.content.bosses.godwars.factions.zaros.attack.NexAttack
import com.rs.game.content.combat.getWeaponAttackEmote
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.npc.NPC
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.WorldUtil
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Animation
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile

enum class NexPhase { SMOKE, SHADOW, BLOOD, ICE, ZAROS }

private val MINIONS = listOf(
    MinionSpawn(Npc.godwars2_smokemage, -12, 14),
    MinionSpawn(Npc.godwars2_shadowmage, 13, 14),
    MinionSpawn(Npc.godwars2_bloodmage, 13, -12),
    MinionSpawn(Npc.godwars2_icemage, -12, -12),
)

private class MinionStage(val npcId: Int, val phase: NexPhase, val hitpoints: Int, val shout: String, val voice: Int)

private val STAGES = listOf(
    MinionStage(Npc.godwars2_smokemage, NexPhase.SMOKE, 24000, "Fumus, don't fail me!", Sound.sound_3321),
    MinionStage(Npc.godwars2_shadowmage, NexPhase.SHADOW, 18000, "Umbra, don't fail me!", Sound.sound_3307),
    MinionStage(Npc.godwars2_bloodmage, NexPhase.BLOOD, 12000, "Cruor, don't fail me!", Sound.sound_3298),
    MinionStage(Npc.godwars2_icemage, NexPhase.ICE, 6000, "Glacies, don't fail me!", Sound.sound_3327),
)

private class Summon(val npcId: Int, val shout: String, val voice: Int, val faceX: Int, val faceY: Int)

private val SUMMONS = mapOf(
    3 to Summon(Npc.godwars2_smokemage, "Fumus!", Sound.sound_3325, 1, -1),
    5 to Summon(Npc.godwars2_shadowmage, "Umbra!", Sound.sound_3313, -1, -1),
    7 to Summon(Npc.godwars2_bloodmage, "Cruor!", Sound.sound_3299, -1, 1),
    9 to Summon(Npc.godwars2_icemage, "Glacies!", Sound.sound_3304, 1, 1),
)

private class PhaseStep(val from: NexPhase, val next: NexPhase, val drawsFrom: Int?, val shout: String, val voice: Int)

private val PHASE_STEPS = listOf(
    PhaseStep(NexPhase.SMOKE, NexPhase.SHADOW, Npc.godwars2_shadowmage, "Darken my shadow!", Sound.sound_3302),
    PhaseStep(NexPhase.SHADOW, NexPhase.BLOOD, Npc.godwars2_bloodmage, "Flood my lungs with blood!", Sound.sound_3306),
    PhaseStep(NexPhase.BLOOD, NexPhase.ICE, Npc.godwars2_icemage, "Infuse me with the power of ice!", Sound.sound_3303),
    PhaseStep(NexPhase.ICE, NexPhase.ZAROS, null, "NOW, THE POWER OF ZAROS!", Sound.sound_3312),
)

class Nex(id: Int, tile: Tile, spawned: Boolean) : MinionHost(id, tile, spawned, MINIONS, MinionPolicy.BOUND) {
    private val baseId = id
    var arena: NexArena? = null
    var isFollowTarget = false
    var phase = NexPhase.SMOKE
        private set
    private var minionStage = 0
    var attackCount = 1
        private set
    private var ticksLastAttack = 0L
    val bloodReavers = arrayOfNulls<NPC>(2)

    init {
        setCantInteract(true)
        capDamage = 500
        lureDelay = 3000
        isIntelligentRouteFinder = true
        setRun(true)
        isIgnoreDocile = true
        refreshTicksAttacked()
        summon()
    }

    private fun nexMinion(npcId: Int) = minion(npcId) as? NexMinion

    override fun processNPC() {
        val stage = STAGES.getOrNull(minionStage)
        if (stage != null && phase == stage.phase && hitpoints <= stage.hitpoints) {
            capDamage = 0
            forceTalk(stage.shout)
            combat.addCombatDelay(1)
            nexMinion(stage.npcId)?.breakBarrier()
            voiceEffect(stage.voice, true)
            minionStage++
        }
        if (isDead || isCantInteract) return
        if (combat.process()) return
        checkAggressivity()
        val target = combatTarget ?: return
        val maxDistance = if (isFollowTarget) 0 else 9
        if (!lineOfSightTo(target, isFollowTarget) || !WorldUtil.isInRange(x, y, size, target.x, target.y, target.size, maxDistance)) {
            resetWalkSteps()
            if (!WorldUtil.isInRange(x, y, size, target.x, target.y, target.size, 5)) {
                val tile = target.getNearestTeleTile(this) ?: Tile.of(target.tile)
                if (World.floorAndWallsFree(tile, size)) forceMove(tile, Seq.godwars2_zaros_spin_out, 0, 30)
            } else calcFollow(target, 2, true)
            return
        }
        resetWalkSteps()
    }

    override fun onRespawn() {
        super.onRespawn()
        restoreForm()
        phase = NexPhase.SMOKE
        minionStage = 0
        attackCount = 1
        capDamage = 500
        setCantInteract(true)
        summon()
    }

    /** Her prayer switches and her death throes are all transformations; a fresh Nex wears none. */
    private fun restoreForm() {
        if (id != baseId) transformIntoNPC(baseId)
    }

    /** Nex takes her minions' power before she will fight: nothing can touch her until she has. */
    private fun summon() {
        WorldTasks.scheduleTimer { tick ->
            if (hasFinished() || isDead) return@scheduleTimer false
            when (tick) {
                1 -> {
                    forceTalk("AT LAST!")
                    anim(Seq.godwars2_zaros_blast_away)
                    spotAnim(Spotanim.zaros_blast_away_dust_spotanim)
                    voiceEffect(Sound.sound_3295, true)
                }
                in SUMMONS.keys -> drawFrom(SUMMONS.getValue(tick))
                11 -> {
                    forceTalk("Fill my soul with smoke!")
                    minion(Npc.godwars2_smokemage)?.let { World.sendProjectile(it, this, Spotanim.mah4_reward_spot_leech_proj_4, 18 to 18, 60, 10, 0) }
                    voiceEffect(Sound.sound_3310, true)
                }
                13 -> {
                    setCantInteract(false)
                    return@scheduleTimer false
                }
            }
            true
        }
    }

    private fun drawFrom(summon: Summon) {
        val minion = minion(summon.npcId) ?: return
        minion.faceAngle = Utils.getAngleTo(summon.faceX, summon.faceY)
        setNextFaceTile(Tile.of(minion.getCoordFaceX(minion.size), minion.getCoordFaceY(minion.size), plane))
        forceTalk(summon.shout)
        anim(Seq.godwars2_zaros_cast_attack)
        World.sendProjectile(minion, this, Spotanim.mah4_reward_spot_leech_proj_4, 18 to 18, 60, 10, 0)
        voiceEffect(summon.voice, true)
    }

    override fun sendDeath(source: Entity?) {
        transformIntoNPC(Npc.godwars2_zaros_avatar_dying)
        val defs = combatDefinitions
        WorldTasks.scheduleTimer { tick ->
            if (tick == 0) anim(defs.deathEmote)
            else if (tick >= defs.deathDelay) {
                drop()
                restoreForm()
                reset()
                val war = arena
                if (war != null) {
                    finish()
                    war.endWar()
                } else setRespawnTask()
                return@scheduleTimer false
            }
            true
        }
        forceTalk("Taste my wrath!")
        voiceEffect(Sound.sound_3323, true)
        sendWrath()
    }

    fun sendWrath() {
        spotAnim(Spotanim.mah4_reward_mushroom_cloud_spotanim)
        WRATH_OFFSETS.forEach { (dx, dy) -> sendWrathProj(this, Tile.of(x + dx, y + dy, plane), 15) }
        WorldTasks.schedule(1) { killBloodReavers() }
    }

    fun calculatePossibleTargets(current: Tile, position: Tile, northSouth: Boolean): List<Entity> =
        possibleTargets.filter {
            it.inArea(current.x, current.y, position.x + (if (northSouth) 2 else 0), position.y + (if (!northSouth) 2 else 0)) ||
                it.inArea(position.x, position.y, current.x + (if (northSouth) 2 else 0), current.y + (if (!northSouth) 2 else 0))
        }

    fun nextPhase() {
        val step = PHASE_STEPS.firstOrNull { it.from == phase } ?: return
        if (minionStage != PHASE_STEPS.indexOf(step) + 1) return
        capDamage = 500
        forceTalk(step.shout)
        step.drawsFrom?.let { id -> nexMinion(id)?.let { World.sendProjectile(it, this, Spotanim.mah4_reward_spot_leech_proj_4, 18 to 18, 60, 5, 0) } }
        if (step.drawsFrom == null) {
            anim(Seq.godwars2_zaros_turmoil)
            spotAnim(Spotanim.zaros_turmoil_spotanim)
            heal(6000)
        }
        combat.addCombatDelay(1)
        voiceEffect(step.voice, true)
    }

    fun checkPhase() {
        val step = PHASE_STEPS.firstOrNull { it.from == phase } ?: return
        val stage = STAGES.getOrNull(PHASE_STEPS.indexOf(step)) ?: return
        if (minionStage != PHASE_STEPS.indexOf(step) + 1) return
        val minion = nexMinion(stage.npcId)
        if (minion == null || minion.isDead || minion.hasFinished()) setPhase(step.next)
    }

    fun switchPrayers() {
        if (isDead) return
        transformIntoNPC(if (id == Npc.godwars2_zaros_deflect) Npc.godwars2_zaros_avatar else id + 1)
    }

    override fun handlePreHit(hit: Hit) {
        checkPhase()
        if (tempAttribs.getB("siphoning")) hit.setHealHit()
        if (id == Npc.godwars2_zaros_deflect && hit.look == HitLook.MELEE_DAMAGE) {
            hit.source?.let { source ->
                val deflected = (hit.damage * 0.1).toInt()
                hit.damage = (hit.damage * source.meleePrayerMultiplier).toInt()
                if (deflected > 0) source.applyHit(Hit(this, deflected, HitLook.REFLECTED_DAMAGE))
            }
        }
        super.handlePreHit(hit)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun setNextAnimation(nextAnimation: Animation?) {
        if (tempAttribs.getB("siphoning")) return
        super.setNextAnimation(nextAnimation)
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun setNextSpotAnim(nextGraphic: SpotAnim?) {
        if (tempAttribs.getB("siphoning")) return
        super.setNextSpotAnim(nextGraphic)
    }

    override fun getMagePrayerMultiplier() = 0.6
    override fun getRangePrayerMultiplier() = 0.6
    override fun getMeleePrayerMultiplier() = 0.6

    override fun getPossibleTargets(): MutableList<Entity> =
        arena?.possibleTargets ?: super.getPossibleTargets()

    override fun handlePreHitOut(target: Entity, hit: Hit) {
        if (id == Npc.godwars2_zaros_avatar_soulsplit && hit.damage > 0 && hit.look in DAMAGING_LOOKS) target.sendSoulSplit(hit, this)
        if (phase == NexPhase.BLOOD && hit.damage > 15 && (hit.look == HitLook.MELEE_DAMAGE || hit.look == HitLook.MAGIC_DAMAGE))
            healHit((hit.damage * 0.15).toInt())
    }

    fun setPhase(phase: NexPhase) {
        this.phase = phase
        attackCount = 0
    }

    fun killBloodReavers() {
        for (index in bloodReavers.indices) {
            val npc = bloodReavers[index] ?: continue
            bloodReavers[index] = null
            if (npc.isDead || npc.hasFinished()) continue
            healHit(npc.hitpoints)
            npc.sendDeath(this)
        }
    }

    fun incrementAttack() {
        refreshTicksAttacked()
        attackCount++
    }

    fun performAttack(target: Entity, attack: NexAttack): Int {
        incrementAttack()
        return attack.attack(this, target)
    }

    fun shouldStopMeleeing() = World.getServerTicks() - ticksLastAttack > 20

    fun refreshTicksAttacked() {
        ticksLastAttack = World.getServerTicks()
    }

    private companion object {
        private val DAMAGING_LOOKS = setOf(HitLook.MELEE_DAMAGE, HitLook.MAGIC_DAMAGE, HitLook.RANGE_DAMAGE)
        private val WRATH_OFFSETS = listOf(
            3 to 3, 3 to 0, 3 to -3, -3 to 3, -3 to 0, -3 to -3, 0 to 3, 0 to -3,
            2 to -2, -2 to 2, 2 to 2, -2 to -2,
        )

        private fun sendWrathProj(nex: Nex, tile: Tile, projStepDelay: Int) {
            World.sendProjectile(nex, tile, Spotanim.mah4_reward_mushroom_cloud_projanim, 24 to 0, 0, projStepDelay, 30, 0) {
                World.sendSpotAnim(tile, SpotAnim(Spotanim.mah4_reward_mushroom_cloud_spotanim_small))
            }
        }
    }
}

@ServerStartupEvent
fun mapNex() {
    instantiateNpc(Npc.godwars2_zaros_avatar) { npcId, tile -> Nex(npcId, tile, false) }

    onObjectClick(Loc.godwars2_avatar_icicle2) { (player, obj) ->
        if (player.tempAttribs.getB("inIcePrison")) {
            player.sendMessage("You can't move!")
            return@onObjectClick
        }
        player.anim(getWeaponAttackEmote(player.equipment.weaponId, player.combatDefinitions.getAttackStyle()))
        player.lock(2)
        World.removeObject(obj)
    }
}
