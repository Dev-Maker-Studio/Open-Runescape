package com.rs.game.content.world.areas.wilderness

import com.rs.engine.dialogue.Dialogue
import com.rs.engine.dialogue.statements.Statement
import com.rs.game.World
import com.rs.game.content.bosses.instance.isInstanceOption
import com.rs.game.content.skills.agility.Agility
import com.rs.game.content.skills.magic.Magic
import com.rs.game.content.skills.thieving.Thieving
import com.rs.game.content.transportation.WildernessObelisk
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.DropSets
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Item
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile

private val WILDERNESS_DITCHES = arrayOf<Any>(
    Loc.wilderness_wall_var1, Loc.wilderness_wall_var2, Loc.wilderness_wall_var3, Loc.wilderness_wall_var4, Loc.wilderness_wall_var5,
    Loc.wild_imp_boundry_wall_low_3x2_var1, Loc.wild_imp_boundry_wall_low_3x2_var1_m,
    Loc.wild_imp_boundry_wall_low_3x2_var2, Loc.wild_imp_boundry_wall_low_3x2_var2_m,
    Loc.wild_imp_boundry_wall_low_3x2_var3, Loc.wild_imp_boundry_wall_low_3x2_var3_m,
    Loc.wild_imp_boundry_wall_low_3x2_var4, Loc.wild_imp_boundry_wall_low_3x2_var4_m,
    Loc.wild_imp_boundry_wall_low_3x2_var5, Loc.wild_imp_boundry_wall_low_3x2_var5_m,
    Loc.wild_imp_boundry_wall_low_3x2_var6, Loc.wild_imp_boundry_wall_low_3x2_var6_m,
)

private val WILDERNESS_LEVERS = mapOf(
    Loc.magearena_lever_to_cellar to Tile.of(2539, 4712, 0),
    Loc.magearena_lever_from_cellar to Tile.of(3089, 3957, 0),
    Loc.wildinlever to Tile.of(3155, 3923, 0),
    Loc.wildoutlever to Tile.of(2561, 3311, 0),
)

private val FORINTHRY_DUNGEON = mapOf(
    Loc.loc_18341 to Tile.of(3039, 3765, 0),
    Loc.revenant_dungeon_entrance_n to Tile.of(3037, 10171, 0),
    Loc.loc_18342 to Tile.of(3071, 3649, 0),
    Loc.revenant_dungeon_entrance_s to Tile.of(3077, 10058, 0),
)

private val GOD_STATUE_CAPES = mapOf(
    Loc.magearena_statue_saradomin to Obj.saradomin_cape,
    Loc.magearena_statue_zamorak to Obj.zamorak_cape,
    Loc.magearena_statue_guthix to Obj.guthix_cape,
)

@ServerStartupEvent
fun mapWilderness() {
    onObjectClick(Loc.loc_2566, tiles = arrayOf(Tile.of(3188, 3962, 0), Tile.of(3189, 3962, 0), Tile.of(3193, 3962, 0))) { e ->
        when (e.opNum) {
            1 -> {
                e.player.sendMessage("You attempt to open the chest without disarming the traps.")
                e.player.applyHit(Hit(e.player.skills.getLevel(Skills.HITPOINTS) + 20, Hit.HitLook.TRUE_DAMAGE))
            }
            2 -> Thieving.checkTrapsChest(e.player, e.obj, Loc.loc_2574, 32, 14, 7.5, DropSets.getDropSet("magic_axe_hut_chest"))
        }
    }

    onObjectClick(Loc.wildymirrorladdertop1, tiles = arrayOf(Tile.of(3017, 3849, 0))) { (player) ->
        player.useStairs(Seq.human_reachforladder, Tile.of(3069, 10255, 0), 1, 2)
    }

    onObjectClick(Loc.di2_ladder_base, tiles = arrayOf(Tile.of(3069, 10256, 0))) { (player) ->
        player.useStairs(Seq.human_reachforladder, Tile.of(3017, 3848, 0), 1, 2)
    }

    onObjectClick(Loc.loc_1816) { (player, _, option) ->
        if (isInstanceOption(option)) return@onObjectClick
        player.stopAll()
        Magic.pushLeverTeleport(player, Tile.of(2273, 4681, 0))
        player.controllerManager.forceStop()
    }

    onObjectClick(Loc.loc_1817, tiles = arrayOf(Tile.of(2273, 4680, 0))) { (player) ->
        Magic.pushLeverTeleport(player, Tile.of(3067, 10254, 0))
        player.controllerManager.startController(WildernessController())
    }

    onObjectClick(Loc.di2_stairs_from_cellar, tiles = arrayOf(Tile.of(3043, 10328, 0))) { (player) ->
        player.tele(player.transform(3, -6400, 0))
        player.controllerManager.startController(WildernessController())
    }

    onObjectClick(Loc.red_dragon_sc) { (player) ->
        if (!Agility.hasLevel(player, 54)) return@onObjectClick player.sendMessage("You need level 54 agility to use this shortcut.")
        val north = player.y > 3800
        player.forceMove(player.transform(if (north) 1 else -1, if (north) -2 else 2), Seq.seq_4721, 25, 30)
    }

    onObjectClick(Loc.godwars_climbing_rocks_up, Loc.godwars_climbing_rocks_down, Loc.godwars_climbing_rocks_plain_down, Loc.godwars_climbing_rocks_plain_up) { (player, obj) ->
        if (!Agility.hasLevel(player, 60)) return@onObjectClick player.sendMessage("You need level 60 agility to use this shortcut.")
        when (obj.id) {
            Loc.godwars_climbing_rocks_plain_up -> player.forceMove(Tile.of(2943, 3767, 0), Seq.tog_climb_slope_up, 25, 60)
            Loc.godwars_climbing_rocks_plain_down -> {
                player.tele(Tile.of(2943, 3767, 0))
                player.forceMove(Tile.of(2950, 3767, 0), Seq.tog_climb_slope_down, 25, 60)
            }
            Loc.godwars_climbing_rocks_down -> player.forceMove(Tile.of(2928, 3757, 0), Seq.tog_climb_slope_up, 25, 60)
            Loc.godwars_climbing_rocks_up -> player.forceMove(Tile.of(2927, 3761, 0), Seq.tog_climb_slope_down, 25, 60)
        }
    }

    onObjectClick(
        Loc.wild_imp_wilderness_portal_stone_0, Loc.wild_imp_wilderness_portal_stones_1, Loc.wild_imp_wilderness_portal_stones_2, Loc.wild_imp_wilderness_portal_stones_3,
        Loc.wild_imp_wilderness_portal_stones_4, Loc.wild_imp_wilderness_portal_stones_5, Loc.wild_imp_wilderness_portal_stones_var1,
    ) { (player, obj) -> WildernessObelisk.activateObelisk(obj.id, player) }

    onObjectClick(Loc.wild_imp_chaosaltar) { (player) -> player.prayer.worshipAltar() }

    onObjectClick(Loc.wild_imp_chaos_temple_trapdoor_closed) { (player) -> player.tele(Tile.of(3241, 9991, 0)) }

    onObjectClick(Loc.zemo_base_ladder) { (player) ->
        player.useStairs(Seq.human_reachforladder, Tile.of(3240, 3607, 0), 1, 1)
        player.controllerManager.startController(WildernessController())
    }

    onObjectClick(*FORINTHRY_DUNGEON.keys.toTypedArray<Any>()) { (player, obj) ->
        player.useStairs(-1, FORINTHRY_DUNGEON.getValue(obj.id), 0, 1)
    }

    onObjectClick(Loc.magearena_waterportal1, Loc.magearena_waterportal2) { (player, obj) ->
        val fromCellar = obj.id == Loc.magearena_waterportal2
        player.simpleDialogue("You step into the pool of sparkling water. You feel the energy rush through your veins.")
        val destination = if (fromCellar) Tile.of(2509, 4687, 0) else Tile.of(2542, 4720, 0)
        val teleportTarget = if (fromCellar) Tile.of(2542, 4718, 0) else Tile.of(2509, 4689, 0)
        player.forceMove(destination, Seq.magearena_jump_in_teleport_pool, 0, 60) {
            player.anim(-1)
            player.tele(teleportTarget)
        }
    }

    onObjectClick(*GOD_STATUE_CAPES.keys.toTypedArray<Any>()) { (player, obj) ->
        player.sendMessage("You kneel and begin to chant to ${obj.definitions.name.replace("Statue of ", "")}...")
        player.anim(Seq.human_pray)
        WorldTasks.schedule(3) {
            player.simpleDialogue("You feel a rush of energy charge through your veins. Suddenly a cape appears before you.")
            val location = Tile.of(obj.x, obj.y - 1, 0)
            World.sendSpotAnim(location, SpotAnim(Spotanim.my4q_stone_stand_smoke_puff))
            World.addGroundItem(Item(GOD_STATUE_CAPES.getValue(obj.id)), location)
        }
    }

    onObjectClick(Loc.sum2_corp_beast_cave_exit_double, Loc.sum1_cave_exit, Loc.sum1_cave_exit_double, Loc.loc_38815) { (player, obj, option) ->
        if (isInstanceOption(option)) return@onObjectClick
        when (obj.id) {
            Loc.sum2_corp_beast_cave_exit_double -> player.interfaceManager.sendInterface(Interface.interface_650)
            Loc.sum1_cave_exit_double -> {
                player.stopAll()
                player.tele(Tile.of(if (player.x == 2921) 2917 else 2921, player.y, player.plane))
            }
            Loc.sum1_cave_exit -> {
                player.stopAll()
                player.tele(Tile.of(3214, 3782, 0))
                player.controllerManager.startController(WildernessController())
            }
            Loc.loc_38815 -> {
                val skills = player.skills
                if (skills.getLevelForXp(Constants.WOODCUTTING) < 37 || skills.getLevelForXp(Constants.MINING) < 45 || skills.getLevelForXp(Constants.SUMMONING) < 23 ||
                    skills.getLevelForXp(Constants.FIREMAKING) < 47 || skills.getLevelForXp(Constants.PRAYER) < 55) {
                    return@onObjectClick player.sendMessage("You need 23 Summoning, 37 Woodcutting, 45 Mining, 47 Firemaking and 55 Prayer to enter this dungeon.")
                }
                player.stopAll()
                player.tele(Tile.of(2885, 4372, 2))
                player.controllerManager.forceStop()
            }
        }
    }

    onObjectClick(Loc.sum1_cave_exit_double, tiles = arrayOf(Tile.of(2918, 4382, 0))) { (player) ->
        player.stopAll()
        player.tele(Tile.of(if (player.x == 2921) 2917 else 2921, player.y, player.plane))
    }

    onObjectClick(*WILDERNESS_LEVERS.keys.toTypedArray<Any>()) { (player, obj) ->
        Magic.pushLeverTeleport(player, WILDERNESS_LEVERS.getValue(obj.id))
    }

    onObjectClick(Loc.wild_imp_stairs_cellar, Loc.di2_stairs_from_cellar) { (player, obj) ->
        player.useStairs(-1, if (obj.id == Loc.wild_imp_stairs_cellar) Tile.of(3044, 10325, 0) else Tile.of(3045, 3927, 0), 0, 1)
    }

    onObjectClick(*WILDERNESS_DITCHES) { (player, obj) ->
        player.startConversation(Dialogue()
            .addNext(object : Statement {
                override fun send(player: Player) {
                    player.interfaceManager.sendInterface(Interface.interface_382)
                }

                override fun getOptionId(componentId: Int) = if (componentId == Utils.componentIdFromHash(Component.interface_382.component_382_19)) 0 else 1

                override fun close(player: Player) {}
            })
            .addNext {
                player.stopAll()
                val acrossX = obj.rotation == 3 || obj.rotation == 1
                val destination = Tile.of(if (acrossX) obj.x - 1 else player.x, if (acrossX) player.y else obj.y + 2, obj.plane)
                player.forceMove(destination, Seq.wild_ditch_jump, 25, 60) {
                    player.faceObject(obj)
                    player.controllerManager.startController(WildernessController())
                    player.resetReceivedDamage()
                }
            })
    }
}
