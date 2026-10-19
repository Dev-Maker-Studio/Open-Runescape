package com.rs.game.content.skills.dungeoneering.npcs.bosses.blink

import com.rs.game.World
import com.rs.game.content.skills.dungeoneering.DungeonManager
import com.rs.game.content.skills.dungeoneering.DungeonUtils
import com.rs.game.content.skills.dungeoneering.RoomReference
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.ForceTalk
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.utils.WorldUtil
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile

private val RUSH_COORDINATES = arrayOf(
    intArrayOf(2, 3, 13, 3),
    intArrayOf(2, 6, 13, 6),
    intArrayOf(2, 9, 13, 9),
    intArrayOf(2, 12, 13, 12),
    intArrayOf(3, 2, 3, 13),
    intArrayOf(6, 2, 6, 13),
    intArrayOf(9, 2, 9, 13),
    intArrayOf(12, 2, 12, 13),
)

private val RUSH_MESSAGES = arrayOf(
    "Grrrr...",
    "More t...tea Alice?",
    "Where...who?",
    "H..here it comes!",
    "See you all next year!",
    "",
    "",
    "",
    "Coo-coo-ca-choo!",
    "Ah! Grrrr...",
    "Aha! Huh? Ahaha!",
    "",
    "",
    "A face! A huuuge face!",
    "Aaahaahaha!",
    "C...can't catch me!",
    "A whole new world!",
    "Over here!",
    "There's no place like home.",
    "The...spire...doors...everywhere...",
)

private const val RUSH_STOPPED = -1
private const val PILLAR_TICKS = 4

class Blink(tile: Tile, manager: DungeonManager, reference: RoomReference) :
    DungeonBoss(DungeonUtils.getClosestToCombatLevel(Utils.range(Npc.rand_blink_1, Npc.rand_blink_14), manager.bossLevel), tile, manager, reference) {

    private var rushCount = 0
    private var rushStage = 4
    private var selectedPath = RUSH_COORDINATES[0]
    private var inversedPath = false
    private var toPath: Tile? = null
    private var activePillar: Tile? = null
    var isSpecialRequired = false

    init {
        setForceFollowClose(true)
        hitpoints = maxHitpoints
        setRun(true)
    }

    override fun processHit(hit: Hit) {
        super.processHit(hit)
        if (hitpoints <= maxHitpoints * (rushStage * .2125)) {
            rushStage--
            rushCount = 0
        }
    }

    override fun setCombatTarget(entity: Entity?) {
        if (rushCount > RUSH_STOPPED) return
        super.setCombatTarget(if (entity is Familiar) entity.owner else entity)
    }

    override fun getMagePrayerMultiplier(): Double = 1.0

    override fun getRangePrayerMultiplier(): Double = 0.6

    override fun getMeleePrayerMultiplier(): Double = 0.8

    private fun stopRushAttack() {
        rushCount = RUSH_STOPPED
        setNextForceTalk(ForceTalk("Oof!"))
        anim(Seq.rand_blink_walk_fall)
        WorldTasks.schedule {
            isSpecialRequired = true
            setCantInteract(false)
        }
    }

    override fun canMove(dir: Direction): Boolean {
        val pillar = activePillar
        if (pillar == null || rushCount < 11) return true
        if (dir.dx + x == pillar.x && dir.dy + y == pillar.y) {
            stopRushAttack()
            return false
        }
        return true
    }

    override fun processNPC() {
        super.processNPC()
        if (rushCount <= RUSH_STOPPED || manager.isDestroyed || isDead) return
        rushCount++
        when (rushCount) {
            1 -> {
                resetWalkSteps()
                setNextFaceEntity(null)
                resetCombat()
                setCantInteract(true)
            }
            3 -> setNextForceTalk(ForceTalk("He saw me!"))
            4 -> {
                anim(Seq.rand_blink_portal_down)
                spotAnim(Spotanim.rand_blink_portal_down_spotanim)
            }
            5 -> transformIntoNPC(Npc.npc_1957)
            8 -> tele(getNextPath())
            9 -> {
                transformIntoNPC(Npc.rand_blink_1)
                val destination = manager.getTile(reference, selectedPath[if (inversedPath) 2 else 0], selectedPath[if (inversedPath) 3 else 1])
                toPath = destination
                addWalkSteps(destination.x, destination.y, 1, false)
            }
            10 -> {
                toPath?.let { addWalkSteps(it.x, it.y, -1, false) }
                setNextForceTalk(ForceTalk(RUSH_MESSAGES.random()))
            }
            11 -> blastNearbyPlayers()
            15 -> {
                rushCount = 5
                transformIntoNPC(Npc.npc_1957)
            }
        }
    }

    private fun blastNearbyPlayers() {
        spotAnim(Spotanim.rand_blink_aoe_spotanim)
        val room = manager.getCurrentRoomReference(tile)
        for (player in manager.party.team) {
            if (room != manager.getCurrentRoomReference(player.tile) || !WorldUtil.isInRange(player.x, player.y, 1, x, y, 1, 4)) continue
            var damage = Utils.random(200, 600)
            if (player.prayer.isProtectingMage || player.prayer.isProtectingRange) damage /= 2
            player.spotAnim(Spotanim.rand_blink_magic_attack_spotanim)
            player.applyHit(Hit(this, damage, HitLook.TRUE_DAMAGE, 35))
        }
    }

    fun getNextPath(): Tile {
        selectedPath = RUSH_COORDINATES.random()
        inversedPath = Utils.random(2) == 0
        return manager.getTile(reference, selectedPath[if (inversedPath) 0 else 2], selectedPath[if (inversedPath) 1 else 3])
    }

    fun raisePillar(selectedPillar: GameObject) {
        val newPillar = GameObject(selectedPillar)
        newPillar.id = Loc.rand_blink_obstacle_03
        activePillar = Tile.of(selectedPillar.tile)
        World.spawnObjectTemporary(newPillar, PILLAR_TICKS)
        WorldTasks.schedule(PILLAR_TICKS) { activePillar = null }
    }

    fun hasActivePillar(): Boolean = activePillar != null
}
