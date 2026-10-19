package com.rs.game.content.skills.firemaking

import com.rs.game.World
import com.rs.game.content.minigames.duel.DuelArenaController
import com.rs.game.content.minigames.duel.DuelController
import com.rs.game.content.skills.dungeoneering.DungeonConstants
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.actions.Action
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.Task
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Constants
import org.darkan.core.model.GroundItem
import org.darkan.core.model.Item
import org.darkan.core.model.ObjectShape
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import com.rs.net.decoders.handlers.InventoryOptionsHandler
import world.gregs.voidps.type.Tile

class Firemaking @JvmOverloads constructor(
    private val fire: Fire,
    private val groundItem: GroundItem? = null,
    private val bowAnim: Int = -1,
) : Action() {

    enum class Fire(val logId: Int, val level: Int, val life: Int, val fireId: Int, val xp: Double, val time: Int) {
        NORMAL(1511, 1, 300, 70755, 40.0, 20),
        ACHEY(2862, 1, 300, 70756, 40.0, 1),
        OAK(1521, 15, 450, 70757, 60.0, 1),
        WILLOW(1519, 30, 450, 70758, 90.0, 1),
        TEAK(6333, 35, 450, 70759, 105.0, 1),
        ARCTIC_PINE(10810, 42, 500, 70760, 125.0, 1),
        MAPLE(1517, 45, 500, 70761, 135.0, 1),
        MAHOGANY(6332, 50, 700, 70762, 157.5, 1),
        EUCALYPTUS(12581, 58, 700, 70763, 193.5, 1),
        YEW(1515, 60, 800, 70764, 202.5, 1),
        MAGIC(1513, 75, 900, 70765, 303.8, 1),
        CURSED_MAGIC(13567, 82, 1000, 70766, 303.8, 1),

        TANGLE_GUM_BRANCHES(17682, 1, 300, 49940, 25.0, 1),
        SEEPING_ELM_BRANCHES(17684, 10, 375, 49941, 44.5, 1),
        BLOOD_SPINDLE_BRANCHES(17686, 20, 410, 49942, 65.6, 1),
        UTUKU_BRANCHES(17688, 30, 450, 49943, 88.3, 1),
        SPINEBEAM_BRANCHES(17690, 40, 500, 49944, 112.6, 1),
        BOVISTRANGLER_BRANCHES(17692, 50, 700, 49945, 138.5, 1),
        THIGAT_BRANCHES(17694, 60, 700, 49946, 166.0, 1),
        CORPSETHRON_BRANCHES(17696, 70, 850, 49947, 195.1, 1),
        ENTGALLOW_BRANCHES(17698, 80, 925, 49948, 225.8, 1),
        GRAVE_CREEPER_BRANCHES(17700, 90, 1000, 49949, 258.1, 1);

        companion object {
            @JvmStatic
            fun forId(logId: Int) = entries.firstOrNull { it.logId == logId }
        }
    }
    override fun start(entity: Entity): Boolean {
        val player = playerFor(entity)
        if (!checkAll(entity)) return false
        entity.resetWalkSteps()
        player?.sendMessage(if (bowAnim != -1) "You attempt to light the logs with the bow." else "You attempt to light the logs.", true)
        if (player != null && groundItem == null) {
            player.inventory.deleteItem(fire.logId, 1)
            World.addGroundItem(Item(fire.logId, 1), Tile.of(entity.tile), player, true, 180)
        }
        val quickFire = entity.tempAttribs.removeL("Fire") > System.currentTimeMillis()
        setActionDelay(entity, if (quickFire) 1 else 2)
        if (!quickFire) entity.anim(if (bowAnim != -1) bowAnim else if (entity is Familiar) 8085 else 16700)
        return true
    }

    private fun checkAll(entity: Entity): Boolean {
        val player = playerFor(entity)
        if (entity is Player && bowAnim == -1 && !player!!.inventory.containsItem(Obj.tinderbox, 1)
            && (fire.ordinal >= Fire.TANGLE_GUM_BRANCHES.ordinal && !player.inventory.containsItem(DungeonConstants.TINDERBOX))) {
            player.sendMessage("You do not have the required items to light this.")
            return false
        }
        if (player != null && player.skills.getLevel(Constants.FIREMAKING) < fire.level) {
            player.sendMessage("You do not have the required level to light this.")
            return false
        }
        if (canLightFire(entity, player)) {
            player?.sendMessage("You can't light a fire here.")
            return false
        }
        return true
    }

    override fun process(entity: Entity) = checkAll(entity)

    override fun processWithDelay(entity: Entity): Int {
        val player = playerFor(entity)
        val tile = Tile.of(entity.tile)
        if (!entity.addWalkSteps(entity.x - 1, entity.y, 1))
            if (!entity.addWalkSteps(entity.x + 1, entity.y, 1))
                if (!entity.addWalkSteps(entity.x, entity.y + 1, 1))
                    entity.addWalkSteps(entity.x, entity.y - 1, 1)
        player?.sendMessage("The fire catches and the logs begin to burn.", true)
        WorldTasks.schedule(object : Task() {
            override fun run() {
                if (player != null) {
                    val item = groundItem ?: ChunkManager.getChunk(tile.getChunkId()).getGroundItem(fire.logId, tile, player)
                    if (item == null || !World.removeGroundItem(player, item, false)) return
                }
                World.spawnTempGroundObject(GameObject(fire.fireId, ObjectShape.SCENERY_INTERACT, 0, tile.x, tile.y, tile.plane), 592, fire.life)
                player?.skills?.addXp(Constants.FIREMAKING, increasedExperience(player, fire.xp, false))
                entity.setNextFaceTile(tile)
            }
        }, 1)
        entity.tempAttribs.setL("Fire", System.currentTimeMillis() + 1800)
        return -1
    }

    override fun stop(entity: Entity) {
        setActionDelay(entity, 3)
    }

    companion object {
        private val BOW_ANIMATIONS = mapOf(
            Obj.longbow to Seq.brut_player_firemaking_shortbow,
            Obj.shortbow to Seq.brut_player_firemaking_shortbow,
            Obj.oak_longbow to Seq.brut_player_firemaking_oak_shortbow,
            Obj.oak_shortbow to Seq.brut_player_firemaking_oak_shortbow,
            Obj.willow_longbow to Seq.brut_player_firemaking_willow_shortbow,
            Obj.willow_shortbow to Seq.brut_player_firemaking_willow_shortbow,
            Obj.maple_longbow to Seq.brut_player_firemaking_maple_shortbow,
            Obj.maple_shortbow to Seq.brut_player_firemaking_maple_shortbow,
            Obj.yew_longbow to Seq.brut_player_firemaking_yew_shortbow,
            Obj.yew_shortbow to Seq.brut_player_firemaking_yew_shortbow,
            Obj.magic_longbow to Seq.brut_player_firemaking_magic_shortbow,
            Obj.magic_shortbow to Seq.brut_player_firemaking_magic_shortbow,
            Obj.ogre_bow to Seq.brut_player_firemaking_daganoth_bow,
            Obj.daganoth_cave_magic_shortbow to Seq.brut_player_firemaking_daganoth_bow,
        )

        fun playerFor(entity: Entity): Player? = when (entity) {
            is Player -> entity
            is Familiar -> entity.owner
            else -> null
        }

        @JvmStatic
        fun isFiremaking(player: Player, logId: Int): Boolean {
            val fire = Fire.forId(logId) ?: return false
            player.actionManager.setAction(Firemaking(fire))
            return true
        }

        @JvmStatic
        fun isFiremaking(player: Player, item1: Item, item2: Item): Boolean {
            val log = InventoryOptionsHandler.contains(Obj.tinderbox, item1, item2) ?: return false
            return isFiremaking(player, log.id)
        }

        @JvmStatic
        fun bowLightAnimation(bowId: Int) = BOW_ANIMATIONS[bowId] ?: -1

        val bowIds: IntArray get() = BOW_ANIMATIONS.keys.toIntArray()

        fun hasLearnedBowLighting(player: Player) = player.vars.getVarBit(Varbit.brut_fire) >= 1

        fun bowLight(player: Player, log: Fire, bowId: Int): Boolean {
            if (!hasLearnedBowLighting(player)) return false
            val anim = BOW_ANIMATIONS[bowId] ?: return false
            val required = bowLevel(log) ?: log.level
            if (player.skills.getLevel(Constants.FIREMAKING) < required) {
                player.sendMessage("You need a Firemaking level of $required to light this log with a bow.")
                return false
            }
            if (player.vars.getVarBit(Varbit.brut_fire) == 1) player.vars.saveVarBit(Varbit.brut_fire, 2)
            player.actionManager.setAction(Firemaking(log, null, anim))
            return true
        }

        private fun bowLevel(fire: Fire): Int? = when (fire) {
            Fire.NORMAL, Fire.ACHEY -> 21
            Fire.OAK -> 35
            Fire.WILLOW -> 50
            Fire.TEAK -> 55
            Fire.ARCTIC_PINE -> 62
            Fire.MAPLE -> 65
            Fire.MAHOGANY -> 70
            Fire.EUCALYPTUS -> 78
            Fire.YEW -> 80
            Fire.MAGIC -> 95
            else -> null
        }

        @JvmStatic
        fun increasedExperience(player: Player, totalXp: Double, usingBonfire: Boolean): Double {
            var xp = totalXp
            if (!usingBonfire && player.hasFamiliar() && player.familiar.id == 7377) xp += 10.0
            if (player.equipment.glovesId == 13660) xp *= 1.025
            if (player.equipment.ringId == 13659) xp *= 1.025
            return xp
        }

        @JvmStatic
        fun canLightFire(entity: Entity, player: Player?): Boolean =
            !World.canLightFire(entity.plane, entity.x, entity.y)
                || ChunkManager.getChunk(entity.chunkId).getSpawnedObject(entity.tile) != null
                || (player != null && (player.controllerManager.controller is DuelArenaController || player.controllerManager.controller is DuelController))
    }
}
