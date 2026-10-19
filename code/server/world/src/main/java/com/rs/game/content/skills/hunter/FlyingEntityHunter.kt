package com.rs.game.content.skills.hunter

import com.rs.engine.dialogue.Dialogue
import com.rs.engine.pathfinder.DumbRouteFinder
import com.rs.game.content.Effect
import com.rs.game.content.minigames.creations.SacredClayTool
import com.rs.game.content.minigames.creations.markSacredClayToolUsed
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.getInteractionDistance
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.DropSets
import com.rs.utils.drop.DropTable
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Item

enum class FlyingEntities(val npcId: Int, val reward: Int, val experience: Double, val level: Int, val rate1: Int, val rate99: Int) {
    BABY_IMPLING(Npc.ii_impling_type_1, Obj.ii_captured_impling_1, 25.0, 17, 79, 400),
    YOUNG_IMPLING(Npc.ii_impling_type_2, Obj.ii_captured_impling_2, 65.0, 22, 69, 349),
    GOURMET_IMPLING(Npc.ii_impling_type_3, Obj.obj_11242, 113.0, 28, 61, 324),
    EARTH_IMPLING(Npc.ii_impling_type_4, Obj.ii_captured_impling_4, 177.0, 36, 51, 301),
    ESSENCE_IMPLING(Npc.ii_impling_type_5, Obj.obj_11246, 225.0, 42, 41, 275),
    ECLECTIC_IMPLING(Npc.ii_impling_type_6, Obj.ii_captured_impling_6, 289.0, 50, 31, 250),
    SPIRIT_IMPLING(Npc.ii_impling_type_6a, Obj.ii_captured_impling_6a, 321.0, 54, 24, 225),
    NATURE_IMPLING(Npc.ii_impling_type_7, Obj.ii_captured_impling_7, 250.0, 58, 20, 199),
    MAGPIE_IMPLING(Npc.ii_impling_type_8, Obj.ii_captured_impling_8, 289.0, 65, 15, 176),
    NINJA_IMPLING(Npc.ii_impling_type_9, Obj.ii_captured_impling_9, 339.0, 74, 10, 150),
    PIRATE_IMPLING(Npc.ii_impling_type_11, Obj.ii_captured_impling_11, 350.0, 76, 8, 132),
    DRAGON_IMPLING(Npc.ii_impling_type_10, Obj.ii_captured_impling_10, 390.0, 83, 5, 125),
    ZOMBIE_IMPLING(Npc.ii_impling_type_12, Obj.ii_captured_impling_12, 412.0, 87, 8, 140),
    KINGLY_IMPLING(Npc.ii_impling_type_13, Obj.ii_captured_impling_13, 434.0, 91, 3, 99),

    BABY_IMPLING_PP(Npc.ii_impling_type_1_maze, Obj.ii_captured_impling_1, 25.0, 17, 79, 400),
    YOUNG_IMPLING_PP(Npc.ii_impling_type_2_maze, Obj.ii_captured_impling_2, 65.0, 22, 69, 349),
    GOURMET_IMPLING_PP(Npc.ii_impling_type_3_maze, Obj.obj_11242, 113.0, 28, 61, 324),
    EARTH_IMPLING_PP(Npc.ii_impling_type_4_maze, Obj.ii_captured_impling_4, 177.0, 36, 51, 301),
    ESSENCE_IMPLING_PP(Npc.ii_impling_type_5_maze, Obj.obj_11246, 225.0, 42, 41, 275),
    ECLECTIC_IMPLING_PP(Npc.ii_impling_type_6_maze, Obj.ii_captured_impling_6, 289.0, 50, 31, 250),
    SPIRIT_IMPLING_PP(Npc.ii_impling_type_6a_maze, Obj.ii_captured_impling_6a, 321.0, 54, 24, 225),
    NATURE_IMPLING_PP(Npc.ii_impling_type_7_maze, Obj.ii_captured_impling_7, 353.0, 58, 20, 199),
    MAGPIE_IMPLING_PP(Npc.ii_impling_type_8_maze, Obj.ii_captured_impling_8, 409.0, 65, 15, 176),
    NINJA_IMPLING_PP(Npc.ii_impling_type_9_maze, Obj.ii_captured_impling_9, 481.0, 74, 10, 150),
    PIRATE_IMPLING_PP(Npc.ii_impling_type_11_maze, Obj.ii_captured_impling_11, 497.0, 76, 8, 132),
    DRAGON_IMPLING_PP(Npc.ii_impling_type_10_maze, Obj.ii_captured_impling_10, 553.0, 83, 5, 125),
    ZOMBIE_IMPLING_PP(Npc.ii_impling_type_12_maze, Obj.ii_captured_impling_12, 585.0, 87, 8, 140),
    KINGLY_IMPLING_PP(Npc.ii_impling_type_13_maze, Obj.ii_captured_impling_13, 617.0, 91, 3, 99);

    companion object {
        val byNpc = entries.associateBy { it.npcId }
        val byReward = entries.distinctBy { it.reward }.associateBy { it.reward }

        @JvmStatic
        fun forItem(reward: Int) = byReward[reward]
    }
}

private val CHARMS = intArrayOf(Obj.lore_creature_charm, Obj.lore_insect_charm, Obj.lore_leech_charm, Obj.lore_outsider_charm)
private val STANDARD_NETS = SacredClayTool.BUTTERFLY_NET.ids + Obj.hunting_butterfly_net

@ServerStartupEvent
fun mapFlyingEntityHunter() {
    val npcIds = FlyingEntities.byNpc.keys.toTypedArray()

    getInteractionDistance(*npcIds) { _, _ -> 0 }

    onNpcClick(*npcIds) { e ->
        val (player, npc, option) = e
        if (option != "Catch" || !e.isAtNPC) return@onNpcClick
        val entity = FlyingEntities.byNpc.getValue(npc.id)
        val isImpling = npc is Impling
        val hasNet = player.hasButterflyNetEquipped()
        val jar = if (isImpling) Obj.ii_impling_jar else Obj.butterfly_jar
        val name = npc.name.lowercase()

        if (npc is Impling && !hasNet && npc.puroPuro) {
            player.sendMessage("You need to have a butterfly net equipped in order to capture an impling.")
            return@onNpcClick
        }
        if (hasNet) {
            if (player.skills.getLevel(Constants.HUNTER) < entity.level) {
                player.sendMessage("You need a hunter level of ${entity.level} to capture a $name.")
                return@onNpcClick
            }
            if (!player.inventory.containsItem(jar, 1)) {
                player.sendMessage("You don't have an empty ${if (isImpling) "impling jar" else "butterfly jar"} in which to keep ${if (isImpling) "an impling" else "a butterfly"}.")
                return@onNpcClick
            }
        } else if (player.skills.getLevel(Constants.HUNTER) < entity.level + 10) {
            player.sendMessage("You need a hunter level of ${entity.level + 10} in order to capture a $name barehanded.")
            return@onNpcClick
        }

        player.lock(2)
        player.sendMessage("You swing your net...")
        player.anim(Seq.ii_human_net_throw_oldnet)
        val weaponId = player.equipment.weaponId
        if (rollSuccess(player, entity, weaponId in STANDARD_NETS)) {
            npc.anim(Seq.seq_6615)
            player.incrementCount("${npc.name} trapped")
            player.inventory.deleteItem(jar, 1)
            player.inventory.addItem(entity.reward, 1)
            if (weaponId in SacredClayTool.BUTTERFLY_NET.ids) player.markSacredClayToolUsed(weaponId)
            player.skills.addXp(Constants.HUNTER, entity.experience)
            player.sendMessage("You manage to catch the $name and squeeze it into a jar.")
            npc.sendDeath(player)
            return@onNpcClick
        }

        player.sendMessage("...you stumble and miss the $name")
        if (!npc.hasEffect(Effect.FREEZE)) {
            npc.resetWalkSteps()
            DumbRouteFinder.addDumbPathfinderSteps(npc, npc.respawnTile, 10, npc.collisionStrategy)
        }
        if (isImpling && Utils.random(10) == 0) npc.forceTalk("Tee hee, you missed me!")
    }

    onItemClick(*FlyingEntities.byReward.keys.toTypedArray(), options = arrayOf("Open", "Loot", "Empty", "Release")) { e ->
        val (player, item, option) = e
        val entity = FlyingEntities.byReward.getValue(item.id)
        when (option) {
            "Empty", "Release" -> item.id = Obj.ii_impling_jar
            "Open", "Loot" -> {
                val loot = DropTable.calculateDrops(player, DropSets.getDropSet(entity.npcId))
                item.id = Obj.ii_impling_jar
                loot.filterNotNull().forEach { player.inventory.addItemDrop(it.id, it.amount) }
                entity.releaseCharm(player)
                if (Utils.random(10) == 0) {
                    player.inventory.deleteItem(Item(Obj.ii_impling_jar))
                    player.sendMessage("You press too hard on the jar and the glass shatters in your hands.")
                    player.applyHit(Hit(player, 10, HitLook.TRUE_DAMAGE))
                }
                player.inventory.refresh(e.slotId)
            }
        }
    }
}

private fun Player.hasButterflyNetEquipped() = equipment.weaponId.let { it in STANDARD_NETS || it == Obj.ii_magic_butterfly_net }

private fun rollSuccess(player: Player, entity: FlyingEntities, standardNet: Boolean) =
    Utils.skillSuccess(player.skills.getLevel(Constants.HUNTER), (if (standardNet) 0.0 else 0.1) + player.auraManager.huntingMul, entity.rate1, entity.rate99)

private fun FlyingEntities.releaseCharm(player: Player) {
    if (reward != Obj.ii_captured_impling_6a || Utils.random(2) != 0) return
    val charm = Item(CHARMS[Utils.random(CHARMS.size)], 1)
    player.startConversation(Dialogue().addItem(charm.id, "The impling was carrying a ${charm.name.lowercase()}."))
    player.inventory.addItemDrop(charm)
}
