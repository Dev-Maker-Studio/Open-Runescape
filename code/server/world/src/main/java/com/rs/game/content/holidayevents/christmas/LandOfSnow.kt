package com.rs.game.content.holidayevents.christmas

import org.darkan.core.cache.ItemDefinitions
import com.rs.game.World.spawnNPC
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onXpDrop
import com.rs.utils.Areas
import com.rs.utils.spawns.NPCSpawn
import com.rs.utils.spawns.NPCSpawns
import com.rs.utils.spawns.ObjectSpawn
import com.rs.utils.spawns.ObjectSpawns
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import java.util.*

private const val ACTIVE = true
private val TRADEABLE_REWARDS = setOf(Obj.santa_hat, Obj.obj_1053, Obj.obj_1055, Obj.obj_1057, Obj.pumpkin)
private val VARROCK_CUPBOARD: Tile = Tile.of(3212, 3434, 0)
private val VARROCK_CUPBOARD_EXIT: Tile = Tile.of(3212, 3433, 0)

private val randomFood: Int
    get() = Obj.obj_15428 + Utils.random(4)

@ServerStartupEvent
fun initObjects() {
    if (!ACTIVE) return
    val center = Tile.of(3212, 3428, 3)
    for (x in -10..9) for (y in -10..9) ObjectSpawns.add(ObjectSpawn(Loc.loc_3701, 10, 1, center.transform(x * 5, y * 5, 0)))

    ObjectSpawns.add(ObjectSpawn(Loc.xmas10_portal, 10, 0, VARROCK_CUPBOARD, "Cupboard to Christmas event."))

    ObjectSpawns.add(ObjectSpawn(Loc.castlewars_bankchest, 10, 0, Tile.of(2648, 5667, 0), "Bank chest"))
    ObjectSpawns.add(ObjectSpawn(Loc.loc_70761, 10, 0, Tile.of(2650, 5666, 0), "Bonfire"))
    ObjectSpawns.add(ObjectSpawn(Loc.castlewars_bankchest, 10, 0, Tile.of(2662, 5667, 0), "Bank chest"))
    ObjectSpawns.add(ObjectSpawn(Loc.loc_70761, 10, 0, Tile.of(2660, 5666, 0), "Bonfire"))
    ObjectSpawns.add(ObjectSpawn(Loc.artisan_cannon_furnace, 10, 0, Tile.of(2665, 5661, 0), "Furnace"))

    ObjectSpawns.add(ObjectSpawn(Loc.loc_21273, 10, 0, Tile.of(2645, 5671, 0), "Arctic pine bottom"))
    ObjectSpawns.add(ObjectSpawn(Loc.loc_47759, 10, 0, Tile.of(2644, 5671, 0), "Arctic pine presents"))
    ObjectSpawns.add(ObjectSpawn(Loc.loc_47761, 10, 0, Tile.of(2647, 5671, 0), "Arctic pine presents"))
    ObjectSpawns.add(ObjectSpawn(Loc.loc_21273, 10, 0, Tile.of(2640, 5665, 0), "Arctic pine bottom"))
    ObjectSpawns.add(ObjectSpawn(Loc.loc_47759, 10, 0, Tile.of(2641, 5664, 0), "Arctic pine presents"))
    ObjectSpawns.add(ObjectSpawn(Loc.loc_47761, 10, 0, Tile.of(2640, 5667, 0), "Arctic pine presents"))

    ObjectSpawns.add(ObjectSpawn(Loc.loc_46485, 10, 0, Tile.of(2667, 5670, 0), "Ice fishing"))
    NPCSpawns.add(NPCSpawn(Npc._0_45_49_rarefish, Tile.of(2667, 5670, 0), "Fishing spot"))
    NPCSpawns.add(NPCSpawn(Npc._0_48_50_saltfish, Tile.of(2667, 5670, 0), "Fishing spot"))

    ObjectSpawns.add(ObjectSpawn(Loc.loc_46485, 10, 0, Tile.of(2665, 5670, 0), "Ice fishing"))
    NPCSpawns.add(NPCSpawn(Npc.npc_334, Tile.of(2665, 5670, 0), "Fishing spot"))
    NPCSpawns.add(NPCSpawn(Npc._0_48_53_freshfish, Tile.of(2665, 5670, 0), "Fishing spot"))

    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2666, 5645, 0), "Carnivorous chinchompa"))
    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2667, 5644, 0), "Carnivorous chinchompa"))
    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2664, 5645, 0), "Carnivorous chinchompa"))
    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2665, 5647, 0), "Carnivorous chinchompa"))
    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2670, 5646, 0), "Carnivorous chinchompa"))
    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2672, 5647, 0), "Carnivorous chinchompa"))
    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2673, 5649, 0), "Carnivorous chinchompa"))
    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2671, 5650, 0), "Carnivorous chinchompa"))
    NPCSpawns.add(NPCSpawn(Npc.hunting_chinchompa_big, Tile.of(2671, 5645, 0), "Carnivorous chinchompa"))

    ObjectSpawns.add(ObjectSpawn(Loc.omge_skill_summoning_stone, 10, 0, Tile.of(2672, 5679, 0), "Summoning obelisk"))

    val n = spawnNPC(Npc.mah5_wave_lucien, Tile.of(2658, 5671, 0), permaDeath = false, withFunction = true)
    n.setLoadsUpdateZones()
    n.setPermName("Yule-cien")
    n.hitpoints = Int.MAX_VALUE / 2
    n.combatDefinitions.hitpoints = Int.MAX_VALUE / 2
    n.capDamage = 750
    n.setForceMultiArea(true)
    n.isForceMultiAttacked = true
}

@ServerStartupEvent
fun mapLandOfSnowPlugins() {
    onXpDrop { e ->
        if (!ACTIVE) return@onXpDrop
        if (Areas.withinArea("christmasevent", e.player.getChunkId())) {
            if (e.player.bonusXpRate != 0.10) {
                e.player.sendMessage("<shad=000000><col=ff0000>You've been granted a 10% experience boost for skilling within the Land of Snow!")
                e.player.bonusXpRate = 0.10
            }
            val chance = e.xp / 75000.0
            if (Math.random() < chance) {
                val reward = TRADEABLE_REWARDS.random()
                e.player.sendMessage("<shad=000000><col=ff0000>You found a " + ItemDefinitions.getDefs(reward).name + " while skilling!")
                if (e.player.inventory.hasFreeSlots()) e.player.inventory.addItemDrop(reward, 1)
                else {
                    e.player.sendMessage("<shad=000000><col=ff0000>as you did not have room in your inventory, it has been added to your bank.")
                    e.player.bank.addItem(Item(reward, 1), true)
                }
            }
        } else e.player.bonusXpRate = 0.0
    }

    onObjectClick(Loc.xmas10_portal, Loc.xmas09_landofsnow_portal) { (player, obj) ->
        player.fadeScreen { player.tele(if (obj.id == Loc.xmas10_portal) Tile.of(2646, 5659, 0) else VARROCK_CUPBOARD_EXIT) }
    }

    onObjectClick(Loc.xmas07_snow_mound) { (player) ->
        if (!ACTIVE) return@onObjectClick
        if (player.inventory.addItem(Obj.xmas07_snowball, player.inventory.freeSlots))
            player.anim(Seq.seq_2282)
    }

    onObjectClick(Loc.loc_47777, Loc.loc_47778) { (player) ->
        if (!ACTIVE) return@onObjectClick
        val food: Int = randomFood
        if (player.inventory.addItem(food, 1))
            player.sendMessage("You take " + Utils.addArticle(ItemDefinitions.getDefs(food).name.lowercase(Locale.getDefault())) + ".")
    }
}
