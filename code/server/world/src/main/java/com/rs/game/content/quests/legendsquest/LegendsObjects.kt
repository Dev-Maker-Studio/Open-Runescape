package com.rs.game.content.quests.legendsquest

import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.content.combat.getWeaponAttackEmote
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.async.schedule
import com.rs.game.tasks.WorldTasks
import com.rs.game.content.skills.mining.Pickaxe
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile

private val GEM_ROCKS = listOf(
    Tile.of(2757, 9297, 0) to Obj.emerald,
    Tile.of(2764, 9309, 0) to Obj.opal,
    Tile.of(2767, 9289, 0) to Obj.ruby,
    Tile.of(2771, 9303, 0) to Obj.jade,
    Tile.of(2772, 9295, 0) to Obj.red_topaz,
    Tile.of(2774, 9287, 0) to Obj.diamond,
    Tile.of(2781, 9291, 0) to Obj.sapphire
)
private val WALL_RUNES = intArrayOf(Obj.soulrune, Obj.mindrune, Obj.earthrune, Obj.lawrune, Obj.lawrune)
private val NOTES = mapOf(Loc.shaman_small_table to Obj.scrawled_note1, Loc.shaman_poor_bed to Obj.scrawled_note2, Loc.shaman_crate to Obj.scrawled_note3, Loc.shaman_desk to Obj.shamans_tome)
private val ROCKY_PATH = mapOf(
    Loc.rocky_ledge to (Tile.of(2378, 4716, 0) to Tile.of(2376, 4718, 0)),
    Loc.rocky_ledge1 to (Tile.of(2377, 4727, 0) to Tile.of(2378, 4729, 0)),
    Loc.rocky_ledge2 to (Tile.of(2382, 4730, 0) to Tile.of(2383, 4728, 0)),
    Loc.viycaves_climbrock1 to (Tile.of(2386, 4727, 0) to Tile.of(2388, 4728, 0)),
    Loc.viycaves_climbrock2 to (Tile.of(2390, 4725, 0) to Tile.of(2390, 4723, 0)),
    Loc.viycaves_climbrock3 to (Tile.of(2390, 4719, 0) to Tile.of(2390, 4717, 0))
)

@ServerStartupEvent
fun mapLegendsObjects() {
    onObjectClick(Loc.legendsguildgatel, Loc.legendsguildgater, tiles = arrayOf(Tile.of(2728, 3349, 0), Tile.of(2729, 3349, 0))) { (player) ->
        if (player.y > 3349 || player.legendsStage > 0 || player.legends.getB("admitted")) enterLegendsGate(player)
        else legendsGuard(player)
    }
    onObjectClick(Loc.loc_2896, Loc.loc_2897, Loc.inac_legendsguilddoorl, Loc.inac_legendsguilddoorr, tiles = arrayOf(Tile.of(2728, 3373, 0), Tile.of(2729, 3373, 0))) { (player) ->
        if (player.y > 3373 || player.legendsStage >= TRAINING) openLegendsDoors(player, Tile.of(2728, 3373, 0), Tile.of(2729, 3373, 0), Loc.loc_2896, Loc.loc_2897)
        else player.sendMessage("You need to complete the Legends' quest before you can enter the Legends' Guild.")
    }
    onObjectClick(Loc.loc_2885, Loc.loc_2886) { (player, obj, option) ->
        if (option == "Open" || option == "Shut") {
            World.spawnObjectTemporary(GameObject(if (option == "Open") Loc.loc_2886 else Loc.loc_2885, obj.type, obj.rotation, obj.tile), 20)
        } else if (player.legendsStage > 0 && !player.hasLegendsItem(Obj.machette)) {
            if (player.giveLegendsItem(Obj.machette)) player.sendMessage("You find a machete in the cupboard.")
        } else player.sendMessage("You don't think Radimus Erkle would be too pleased if he caught you rifling through his cupboards without permission.")
    }
    onObjectClick(Loc.lgshamancaverock1, Loc.lgshamancaverock2, Loc.lgshamancaverock3) { (player, _, option) ->
        if (!player.inKharazi() || option != "Search") return@onObjectClick
        if (player.legendsStage < RESCUE) { player.sendMessage("You search the rocks but see nothing significant."); return@onObjectClick }
        player.startConversation {
            simple("You see that there is a small crevice that you may be able to crawl through.")
            options("Crawl through hole?") {
                opExec("Yes, I'll crawl through. I'm very athletic.") {
                    if (player.legendsLevel(Skills.AGILITY, 50)) {
                        player.enterLegendsShamanCave()
                    }
                }
                op("No, I'm a bit claustrophobic.") { simple("You decide against forcing yourself into the tiny crevice.") }
            }
        }
    }
    onObjectClick(Loc.shaman_bookcase, Loc.shaman_cave_crevice, Loc.shaman_cave_crevice_in) { (player, obj, option) ->
        if (option == "Look" || option == "Look-at") {
            player.sendMessage(if (obj.id == Loc.shaman_bookcase) "This bamboo book shelf doesn't house many books, it doesn't look as if the Shaman has much opportunity to read at the moment in any case." else "It looks like a crevice, you could possibly squeeze through.")
            return@onObjectClick
        }
        if (option != "Search" || player.legendsStage < RESCUE) return@onObjectClick
        player.startConversation {
            simple(if (obj.id == Loc.shaman_bookcase) "After perusing the empty bookcase for a while, you decide to check behind it. There's a crevice in the wall!" else "You search the crevice. It looks as if you might be able to squeeze through, would you like to try?")
            options("Squeeze through incredibly tight crevice?") {
                opExec("Yes please!") {
                    if (!player.legendsLevel(Skills.AGILITY, 50)) return@opExec
                    if (!player.legendsTrial(Skills.AGILITY, 50)) { player.skills.drainLevel(Skills.AGILITY, 1); player.sendMessage("You fail to squeeze through the crevice. The effort drains your Agility."); return@opExec }
                    val target = if (obj.id == Loc.shaman_cave_crevice) Tile.of(2795, 9338, 0) else Tile.of(2800, 9340, 0)
                    player.crawlLegendsPassage(listOf(target))
                }
                op("No thanks.")
            }
        }
    }
    onObjectClick(Loc.di2_cave_wall_exit_light_brown, tiles = arrayOf(Tile.of(2771, 9342, 0))) { (player) ->
        player.crawlLegendsPassage(listOf(Tile.of(2782, 2934, 0)))
    }
    onObjectClick(*NOTES.keys.toTypedArray<Any>()) { (player, obj, option) ->
        if (option == "Look" || option == "Look-at") {
            player.sendMessage(when (obj.id) {
                Loc.shaman_small_table -> "A crudely constructed makeshift table made from various pieces of wood. You see a piece of screwed up paper on the table top."
                Loc.shaman_crate -> "It looks like a rickety old crate, perhaps placed in this recess to hide it from prying eyes."
                else -> "It's a very old rickety desk made of bamboo."
            })
            return@onObjectClick
        }
        if (option != "Search" || player.legendsStage < RESCUE) return@onObjectClick
        if (player.giveLegendsItem(NOTES.getValue(obj.id))) player.sendMessage("You find a scrap of paper with strange writing on it.")
    }
    onObjectClick(Loc.lqfirewall_straight, Loc.lqfirewall_diagonal) { (player, _, option) ->
        if (player.legendsStage < RESCUE) return@onObjectClick
        if (player.legendsStage >= SEEDS) crossFireWall(player)
        else if (player.inOctagram()) player.startConversation {
            options {
                opExec("Leap out of the flaming octagram.") { crossFireWall(player); player.applyHit(Hit.flat(null, 30)) }
                opExec("Attract the shaman's attention.") { fireWallDialogue(player) }
            }
        } else if (option == "Investigate") fireWallDialogue(player)
        else { player.sendMessage("You get too close and the intense heat burns you!"); player.applyHit(Hit.flat(null, 50)) }
    }
    onItemOnObject(arrayOf(Loc.lqfirewall_straight, Loc.lqfirewall_diagonal), arrayOf(Obj.goldbowlbless_pure, Obj.goldbowl_pure)) { (player, _, item) ->
        if (player.legendsStage < RESCUE) return@onItemOnObject
        if (item.id != Obj.goldbowlbless_pure) { player.sendMessage("The water loses its sacred properties in an unblessed bowl."); return@onItemOnObject }
        player.sendMessage("You splash some sacred water on the flames.")
        player.replaceLegendsItem(Obj.goldbowlbless_pure, Obj.goldbowlbless_empty)
        player.advanceLegends(BINDING)
        crossFireWall(player)
    }
    onObjectClick(Loc.lglockpickgatebottoml, Loc.lglockpickgatebottomr, tiles = arrayOf(Tile.of(2809, 9332, 0), Tile.of(2810, 9332, 0))) { (player, _, option) ->
        if (player.legendsStage < RESCUE) return@onObjectClick
        if (option == "Search") { player.sendMessage("You find a complicated lock. A lockpick might help."); return@onObjectClick }
        if (option != "Open") return@onObjectClick
        if (!player.legends.getB("lockGate")) player.sendMessage("The gate is locked.")
        else openLegendsDoors(player, Tile.of(2809, 9332, 0), Tile.of(2810, 9332, 0), Loc.lglockpickgatebottomr, Loc.lglockpickgatebottoml)
    }
    onItemOnObject(arrayOf(Loc.lglockpickgatebottoml, Loc.lglockpickgatebottomr), arrayOf(Obj.lockpick)) { (player) ->
        if (player.legendsStage < RESCUE || !player.legendsLevel(Skills.THIEVING, 50)) return@onItemOnObject
        if (!player.legendsTrial(Skills.THIEVING, 50)) {
            player.skills.drainLevel(Skills.THIEVING, 1)
            player.startConversation {
                simple("You fail to pick the lock. Your lockpick is about to break!")
                legendsExec(player) { player.inventory.deleteItem(Obj.lockpick, 1) }
                simple("Your lockpick breaks.")
            }
            return@onItemOnObject
        }
        player.sendMessage("You carefully insert your lockpick into the lock. CLICK! You tumble the lock mechanism and the door opens easily.")
        player.legends.setB("lockGate", true)
        openLegendsDoors(player, Tile.of(2809, 9332, 0), Tile.of(2810, 9332, 0), Loc.lglockpickgatebottomr, Loc.lglockpickgatebottoml)
    }
    onObjectClick(Loc.mine_test_boulder1, Loc.mine_test_boulder2, Loc.mine_test_boulder3) { (player, obj) ->
        if (player.legendsStage < RESCUE || !player.legendsLevel(Skills.MINING, 52)) return@onObjectClick
        val pick = Pickaxe.getBest(player)
        if (pick == null) { player.sendMessage("You need a pickaxe to smash this boulder."); return@onObjectClick }
        val destination = Tile.of(2809, if (player.y > obj.y) obj.y - 2 else obj.y + 2, 0)
        player.anim(pick.animId)
        if (!player.legendsTrial(Skills.MINING, 52)) { player.skills.drainLevel(Skills.MINING, 1); player.sendMessage("You fail to break the boulder. The effort drains your Mining skill."); return@onObjectClick }
        player.sendMessage("You manage to smash the rock to bits. Another boulder drops down behind you.")
        player.useStairs(pick.animId, destination, 2, 3)
    }
    onObjectClick(Loc.lgstrengthtrialgatel, Loc.lgstrengthtrialgater, tiles = arrayOf(Tile.of(2809, 9314, 0), Tile.of(2810, 9314, 0))) { (player, _, option) ->
        if (player.legendsStage < RESCUE) return@onObjectClick
        if (option == "Search") { player.sendMessage("The ancient gates are stiff and heavy. You might be able to force them open."); return@onObjectClick }
        if (option != "Open") return@onObjectClick
        player.startConversation {
            simple("You push the doors. They're quite stiff... they won't budge with a normal push. Do you want to try to force them open with brute strength?")
            options("Use brute strength on the gates?") {
                opExec("Yes, I'm very strong, I'll force them open.") {
                    if (!player.legendsLevel(Skills.STRENGTH, 50)) return@opExec
                    if (!player.legendsTrial(Skills.STRENGTH, 50)) { player.skills.drainLevel(Skills.STRENGTH, 1); player.sendMessage("The gates resist your efforts. Your Strength is drained."); return@opExec }
                    openLegendsDoors(player, Tile.of(2809, 9314, 0), Tile.of(2810, 9314, 0), Loc.lgstrengthtrialgater, Loc.lgstrengthtrialgatel)
                }
                op("No, I'm having second thoughts.")
            }
        }
    }
    onObjectClick(Loc.crumbled_wall_low, Loc.crumbled_wall_mirror, tiles = arrayOf(Tile.of(2791, 9294, 0), Tile.of(2790, 9294, 0)), checkDistance = false) { (player, _, option) ->
        if (option == "Look") { player.sendMessage("It looks like this room was once sealed, but the top wall fell down. If you're feeling particularly agile, you could try jumping over it."); return@onObjectClick }
        if (option != "Jump-over" || player.legendsStage < RESCUE || player.isLocked || !player.legendsLevel(Skills.AGILITY, 50)) return@onObjectClick
        val northSide = player.y >= 9295
        val approach = Tile.of(2790, if (northSide) 9296 else 9293, 0)
        val destination = Tile.of(2790, if (northSide) 9293 else 9296, 0)
        player.walkToAndExecute(approach) {
            if (player.isLocked || player.tile != approach || player.legendsStage < RESCUE || !player.legendsLevel(Skills.AGILITY, 50)) return@walkToAndExecute
            jumpJaggedWall(player, destination)
        }
    }
    onObjectClick(Loc.lgancientwalldoor) { (player, obj, option) ->
        if (player.legendsStage < RESCUE) return@onObjectClick
        if (player.legends.getI("runes") == WALL_RUNES.size) markedWallPassage(player, obj)
        else if (option == "Search") player.startConversation {
            simple("You find five slightly round depressions and some strange markings. You clear away the dirt and mould.")
            options("Read message on wall?") {
                op("Yes, I'll read it.") { simple("Place five stones of magical power - but place them wrong, and your fate will sour. First is the spirit of man or beast, second the place where fools tread least. Third is the soil where plants do grow - and four and five are the rules you must know. Solve this riddle, and overcome your ignorance; arranged correct, it should make sense.") }
                op("No, I won't read it.")
            }
        } else player.sendMessage("You don't see any way to use that. Perhaps you should search the door?")
    }
    onItemOnObject(arrayOf(Loc.lgancientwalldoor), null) { (player, obj, item) ->
        if (player.legendsStage < RESCUE) return@onItemOnObject
        val progress = player.legends.getI("runes")
        if (progress == WALL_RUNES.size) { markedWallPassage(player, obj); return@onItemOnObject }
        if (item.id != WALL_RUNES[progress]) {
            player.legends.setI("runes", 0)
            player.sendMessage("The runes fade from the wall. You must start again.")
            return@onItemOnObject
        }
        player.inventory.deleteItem(item.id, 1)
        player.legends.incI("runes")
        player.sendMessage("The rune merges with the wall. The letter '${"SMELL"[progress]}' appears.")
        if (progress == WALL_RUNES.lastIndex) markedWallPassage(player, obj)
    }
    onObjectClick(Loc.lg_gemplacerock) { (player, _, option) ->
        player.sendMessage(if (option == "Search") "You see a faded inscription on the rock. 'Once there were crystals to make the pool shine. Ordered in stature to retrieve what's mine.'" else "It's meant to look like a stalagmite, but there's something unnatural about it. Looking carefully, you notice a strange carving on the rocky surface.")
    }
    onItemOnObject(arrayOf(Loc.lg_gemplacerock), GEM_ROCKS.map { it.second }.toTypedArray<Any>()) { (player, obj, item) ->
        if (player.legendsStage < RESCUE || player.legends.getI("runes") != 5 || !player.legendsLevel(Skills.CRAFTING, 50)) return@onItemOnObject
        val index = GEM_ROCKS.indexOfFirst { it.first == obj.tile }
        if (index < 0 || GEM_ROCKS[index].second != item.id) { player.sendMessage("You carefully move the gem closer to the rock. But nothing happens."); return@onItemOnObject }
        val bit = 1 shl index
        if (player.legends.getI("gems") and bit != 0) { player.sendMessage("A gem has already been placed here."); return@onItemOnObject }
        player.inventory.deleteItem(item.id, 1)
        player.legends.setI("gems", player.legends.getI("gems") or bit)
        player.sendMessage("The gem glows and starts spinning as it hovers over the rock. The gem soon begins to fade.")
        if (player.legends.getI("gems") == 127) {
            player.legends.setB("bindingBook", true)
            player.startConversation {
                simple("You feel a powerful force picking you up...", "All the gems suddenly appear again...", "Magic flies from the gems into the centre of the room.", "Suddenly the room is lit up with a brilliant light...", "...Strangely a book appears near the middle of the room.")
                legendsExec(player) { if (player.legendsStage >= RESCUE && player.legends.getB("bindingBook") && !player.hasLegendsItem(Obj.book_of_binding)) player.inventory.addItemDrop(Obj.book_of_binding, 1) }
            }
        }
    }
    onObjectClick(Loc.lg_dwarfremains) { (player) ->
        if (player.legends.getB("bindingBook") && !player.hasLegendsItem(Obj.book_of_binding)) player.giveLegendsItem(Obj.book_of_binding)
        else player.sendMessage("It looks as though some poor unfortunate soul died here. There's nothing interesting about these remains.")
    }
    onObjectClick(Loc.di2_cave_door_lgmagictrialgatel, Loc.di2_cave_door_lgmagictrialgater, tiles = arrayOf(Tile.of(2762, 9316, 0), Tile.of(2763, 9316, 0))) { (player, _, option) ->
        if (option == "Search") {
            player.sendMessage("To all those with learning and supernatural powers, who with book and rune spend the long dark hours. If further challenges you would endure, give me a taste of your power so pure.")
            return@onObjectClick
        }
        if (option != "Open") return@onObjectClick
        if (player.legendsStage >= DRY_POOL && player.legends.getB("orbGate")) passOrbGate(player)
        else player.sendMessage("This strange-looking gate is covered in mysterious symbols. Weirdly, the handle is a glass orb.")
    }
    onObjectClick(Loc.skullcavebarrel) { (player, obj, option) ->
        if (option != "Smash" || player.isLocked || player.legendsStage < RESCUE || obj.plane != 0 || obj.x !in 2752..2779 || obj.y !in 9319..9337) return@onObjectClick
        if (World.getObjectWithId(obj.tile, obj.id) == null) return@onObjectClick
        if (!player.inventory.hasFreeSlots()) { player.sendMessage("You need some free inventory space to collect anything from the barrel."); return@onObjectClick }
        player.endConversation()
        player.anim(getWeaponAttackEmote(player.equipment.weaponId, player.combatDefinitions.getAttackStyle()))
        player.lock(3)
        WorldTasks.schedule(1) {
            if (player.hasFinished() || player.isDead || !player.withinDistance(obj.tile, 2) || World.getObjectWithId(obj.tile, obj.id) == null) return@schedule
            if (Utils.random(100) >= (player.skills.getLevel(Skills.ATTACK) + 20).coerceAtMost(95)) {
                player.sendMessage("You fail to smash the barrel.")
                return@schedule
            }
            World.removeObjectTemporary(obj, 30)
            when (Utils.random(6)) {
                0, 1 -> {
                    if (player.inventory.addItem(Obj.rope, 1)) player.sendMessage("You smash the barrel and find a rope.")
                    else player.sendMessage("You find a rope, but have no room to collect it. Try another barrel after freeing a slot.")
                }
                2 -> { player.sendMessage("The barrel explodes!"); player.applyHit(Hit.flat(null, Utils.random(60, 120))) }
                3 -> { player.sendMessage("You hit the barrel at an awkward angle, weakening your attack."); player.skills.drainLevel(Skills.ATTACK, Utils.random(1, 3)) }
                else -> player.sendMessage("You smash the barrel, but find nothing inside.")
            }
        }
    }
    onItemOnObject(arrayOf(Loc.lg_winchdown_norope, Loc.lg_winchdown_rope), arrayOf(Obj.rope)) { (player, obj) ->
        if (player.legendsStage < DRY_POOL || !player.legends.getB("orbGate")) return@onItemOnObject
        if (!player.legends.getB("winchRope")) {
            player.inventory.deleteItem(Obj.rope, 1)
            player.legends.setB("winchRope", true)
        }
        World.spawnObject(GameObject(Loc.lg_winchdown_rope, obj.type, obj.rotation, obj.tile))
        player.sendMessage("You throw a rope around the winch.")
    }
    onObjectClick(Loc.lg_winchdown_norope, Loc.lg_winchdown_rope) { (player, _, option) ->
        if (!player.legends.getB("winchRope")) { player.sendMessage("Perhaps with a rope, it might be a bit more functional."); return@onObjectClick }
        if (option == "Search") { player.sendMessage("You search the wooden beams and find the rope you attached."); return@onObjectClick }
        player.startConversation {
            simple("It's very dark down there. Are you sure you want to go down?")
            options {
                opExec("I'm not scared. Let's go!") {
                    if (player.legendsStage < DRY_POOL || !player.legends.getB("winchRope") || !player.legends.getB("orbGate") || !player.legends.getB("brave")) { player.sendMessage("But a terrible fear grips you... You can go no further."); return@opExec }
                    player.advanceLegends(SOURCE)
                    if (!player.legendsTrial(Skills.AGILITY, 50)) {
                        player.useStairs(Seq.emote_climbing_rope, Tile.of(2393, 4717, 0), 1, 3)
                        player.applyHit(Hit.flat(null, 150))
                        player.sendMessage("You lose your grip and fall into the pit!")
                        return@opExec
                    }
                    player.useStairs(Seq.emote_climbing_rope, Tile.of(2377, 4712, 0), 1, 3)
                    player.sendMessage("You shimmy down the rope and into the darkness. You land on a tall ledge... It's a long way down!")
                }
                op("Actually, maybe this is a bad idea...")
            }
        }
    }
    onObjectClick(Loc.lgclimbrope_viyeldicaves) { (player) -> player.useStairs(Seq.emote_climbing_rope, Tile.of(2759, 9328, 0), 1, 3) }
    onObjectClick(*ROCKY_PATH.keys.toTypedArray<Any>(), checkDistance = false) { (player, obj, option) ->
        if (option != "Climb-over" || player.isLocked || player.legendsStage < SOURCE) return@onObjectClick
        val (first, second) = ROCKY_PATH.getValue(obj.id)
        val goingDown = player.tile.distanceTo(first) <= player.tile.distanceTo(second)
        val approach = if (goingDown) first else second
        val destination = if (goingDown) second else first
        player.walkToAndExecute(approach) {
            fun climb() {
                if (player.isLocked || player.tile != approach || player.legendsStage < SOURCE || !player.legendsLevel(Skills.AGILITY, 50)) return
                if (!World.floorFree(destination, 1)) { player.sendMessage("The other side of the rocks is blocked."); return }
                if (goingDown && !player.legendsTrial(Skills.AGILITY, 50)) {
                    player.useStairs(Seq.human_walk_style, Tile.of(2393, 4717, 0), 1, 2)
                    player.applyHit(Hit.flat(null, 400))
                    player.sendMessage("You lose your footing and fall down the rocky path!")
                    return
                }
                player.sendMessage("You climb confidently over the rocks and hold your balance well.")
                player.forceMove(destination, Seq.human_walk_style, 0, 60)
            }
            if (!goingDown) climb()
            else player.startConversation {
                simple("This looks like quite a dangerous walk way, are you sure you want to take this path?")
                options("Walk along dangerous path?") {
                    opExec("Yes, I can think of nothing more exciting!") { climb() }
                    op("No, I'm having second thoughts.")
                }
            }
        }
    }
    onObjectClick(Loc.furnace_legendsquest) { (player, _, option) ->
        if (option == "Look") { player.sendMessage("It looks designed to fuse things together at extremely high temperatures."); return@onObjectClick }
        if (option != "Search") return@onObjectClick
        if (player.legendsStage >= SOURCE && player.legends.getI("crystals") == 7 && !player.hasLegendsItem(Obj.heartcrystal_glow) && !player.legends.getB("barrier")) player.giveLegendsItem(Obj.heartcrystal)
        else player.sendMessage("The compartment in the furnace looks like it should hold something. It's divided into three separate sections.")
    }
    onItemOnObject(arrayOf(Loc.furnace_legendsquest), arrayOf(Obj.heartcrystal_sectiona, Obj.heartcrystal_sectionb, Obj.heartcrystal_sectionc)) { (player, _, item) ->
        if (player.legendsStage < SOURCE || !player.legendsLevel(Skills.CRAFTING, 50)) return@onItemOnObject
        val bit = when (item.id) { Obj.heartcrystal_sectiona -> 1; Obj.heartcrystal_sectionb -> 2; else -> 4 }
        if (player.legends.getI("crystals") and bit != 0) { player.sendMessage("That compartment is already filled."); return@onItemOnObject }
        player.inventory.deleteItem(item.id, 1)
        player.legends.setI("crystals", player.legends.getI("crystals") or bit)
        player.sendMessage("You place the piece of crystal into a specially-shaped compartment of the furnace.")
        if (player.legends.getI("crystals") == 7) {
            player.inventory.addItemDrop(Obj.heartcrystal, 1)
            player.anim(Seq.human_furnace)
            player.sendMessage("The heat in the furnace slowly rises and soon fuses the parts together. As the crystal touches your hands a voice inside of your head says... Bring life to the dragon's eye.")
        }
    }
    onObjectClick(Loc.dragons_eye_rock) { (player) ->
        if (player.inventory.containsItem(Obj.heartcrystal)) chargeHeartCrystal(player)
        else player.sendMessage("You see nothing special about this mossy rock.")
    }
    onItemOnObject(arrayOf(Loc.dragons_eye_rock), arrayOf(Obj.heartcrystal)) { (player) ->
        chargeHeartCrystal(player)
    }
    onItemOnObject(arrayOf(Loc.heart_recess_empty, Loc.heart_recess_full), arrayOf(Obj.heartcrystal_glow)) { (player) ->
        if (player.legendsStage < SOURCE || player.legends.getB("barrier")) return@onItemOnObject
        player.inventory.deleteItem(Obj.heartcrystal_glow, 1)
        player.legends.setB("barrier", true)
        player.sendMessage("You carefully place the glowing, heart-shaped crystal into the depression. It slots in perfectly and glows even brighter. You hear a snapping sound coming from in front of the cave.")
    }
    onObjectClick(Loc.heart_recess_empty, Loc.heart_recess_full) { (player) -> player.sendMessage(if (player.legends.getB("barrier")) "You've placed the dragon heart crystal in the wall, and now have access to the cavern." else "A heart-shaped depression is carved into the wall.") }
    onObjectClick(Loc.legendsquest_force_barrier) { (player, _, option) ->
        if (option == "Look") { player.sendMessage("A magical barrier stretches across the entrance to the cavern."); return@onObjectClick }
        if (option != "Walk-through" || player.isLocked) return@onObjectClick
        if (!player.legends.getB("barrier")) { player.sendMessage("The magical barrier prevents you from passing."); return@onObjectClick }
        val north = player.y >= 4691
        val start = Tile.of(2421, if (north) 4691 else 4689, 0)
        val destination = Tile.of(2421, if (north) 4689 else 4691, 0)
        if (!World.floorFree(start, 1) || !World.floorFree(destination, 1)) return@onObjectClick
        player.walkToAndExecute(start) {
            if (player.hasFinished() || player.isDead || player.isLocked || player.tile != start || !player.legends.getB("barrier") || !World.floorFree(destination, 1)) return@walkToAndExecute
            player.sendMessage("You walk carefully through the magical barrier and into the darkness of the cavern.")
            player.forceMove(destination, Seq.human_walk_f, 0, 60)
        }
    }
}

private fun Player.enterLegendsShamanCave() {
    crawlLegendsPassage(listOf(Tile.of(2772, 9341, 0), Tile.of(2771, 9341, 0), Tile.of(2773, 9341, 0)))
}

private fun Player.crawlLegendsPassage(destinations: List<Tile>) {
    if (isLocked) return
    val origin = tile
    destinations.map { it.getChunkId() }.distinct().forEach { ChunkManager.getChunk(it) }
    endConversation()
    schedule("legendsCavePassage") {
        repeat(20) {
            wait(1)
            if (hasFinished() || isDead || tile != origin || legendsStage < RESCUE) return@schedule
            val destination = destinations.firstOrNull { World.floorAndWallsFree(it, 1) }
            if (destination != null) {
                sendMessage("You contort your body to fit the crevice. You adroitly squeeze, serpent-like, into the crevice.")
                useStairs(Seq.human_crawling, destination, 1, 3)
                return@schedule
            }
        }
        sendMessage("The way is blocked. Please try again in a moment.")
    }
}

private fun chargeHeartCrystal(player: Player) {
    if (player.legendsStage >= SOURCE && player.replaceLegendsItem(Obj.heartcrystal, Obj.heartcrystal_glow)) player.sendMessage("You carefully place the dragon crystal on the rock. The rocks seem to vibrate and hum and the crystal starts to glow. The vibration in the area diminishes, but the crystal continues to glow.")
}

internal fun enterLegendsGate(player: Player) {
    if (player.isLocked || player.plane != 0) return
    if (player.y <= 3349 && player.legendsStage == 0 && (!player.legends.getB("admitted") || !legendsEligible(player))) return
    val entering = player.y <= 3349
    val lane = player.x.coerceIn(2728, 2729)
    val start = Tile.of(lane, if (entering) 3348 else 3351, 0)
    val destination = Tile.of(lane, if (entering) 3351 else 3348, 0)
    player.walkToAndExecute(start) {
        if (player.isLocked || player.tile != start) return@walkToAndExecute
        if (entering && player.legendsStage == 0 && (!player.legends.getB("admitted") || !legendsEligible(player))) return@walkToAndExecute
        if (!World.floorAndWallsFree(destination, 1)) {
            player.sendMessage("The other side of the gate is blocked.")
            return@walkToAndExecute
        }
        val left = World.getObjectWithId(Tile.of(2728, 3349, 0), Loc.legendsguildgatel) ?: return@walkToAndExecute
        val right = World.getObjectWithId(Tile.of(2729, 3349, 0), Loc.legendsguildgater) ?: return@walkToAndExecute
        val openedLeft = GameObject(Loc.legendsguildgatel_open, left.type, 0, left.tile.transform(0, 1, 0))
        val openedRight = GameObject(Loc.legendsguildgater_open, right.type, 2, right.tile.transform(0, 1, 0))
        player.resetWalkSteps()
        player.lock(4)
        player.packets.sendRemoveObject(left)
        player.packets.sendRemoveObject(right)
        player.packets.sendAddObject(openedLeft)
        player.packets.sendAddObject(openedRight)
        player.addWalkSteps(destination, 4, false)
        WorldTasks.schedule(4) {
            if (!player.hasFinished() && player.mapChunkIds.contains(left.tile.getChunkId())) {
                player.packets.sendRemoveObject(openedLeft)
                player.packets.sendRemoveObject(openedRight)
                player.packets.sendAddObject(left)
                player.packets.sendAddObject(right)
            }
        }
    }
}

private fun openLegendsDoors(player: Player, leftTile: Tile, rightTile: Tile, leftId: Int, rightId: Int) {
    if (player.isLocked) return
    val left = World.getObjectWithId(leftTile, leftId) ?: return
    val right = World.getObjectWithId(rightTile, rightId) ?: return
    val northSide = if (left.rotation == 3) player.y >= leftTile.y else player.y > leftTile.y
    val lane = player.x.coerceIn(leftTile.x, rightTile.x)
    val destination = Tile.of(lane, leftTile.y + if (northSide) -1 else 1, leftTile.plane)
    if (!World.floorFree(destination, 1)) { player.sendMessage("The other side of the gate is blocked."); return }
    val offset = if (left.rotation == 3) -1 else 1
    val openedIds = when (left.id) {
        Loc.lglockpickgatebottomr -> Loc.inac_lglockpickgatebottomr to Loc.inac_lglockpickgatebottoml
        Loc.lgstrengthtrialgater -> Loc.lgstrengthtrialgate_inact_r to Loc.lgstrengthtrialgate_inact_l
        Loc.di2_cave_door_lgmagictrialgater -> Loc.di2_cave_door_lgmagictrialgater_open_inactive to Loc.di2_cave_door_lgmagictrialgatel_open_inactive
        else -> Loc.inac_legendsguilddoorl to Loc.inac_legendsguilddoorr
    }
    val turn = if (left.id == Loc.loc_2896) -1 else 1
    val openedLeft = GameObject(openedIds.first, left.type, (left.rotation + turn) and 3, left.tile.transform(0, offset, 0))
    val openedRight = GameObject(openedIds.second, right.type, (right.rotation - turn) and 3, right.tile.transform(0, offset, 0))
    player.endConversation()
    player.packets.sendRemoveObject(left)
    player.packets.sendRemoveObject(right)
    player.packets.sendAddObject(openedLeft)
    player.packets.sendAddObject(openedRight)
    player.resetWalkSteps()
    player.lock(4)
    player.addWalkSteps(destination, 4, false)
    WorldTasks.schedule(4) {
        if (!player.hasFinished() && player.mapChunkIds.contains(leftTile.getChunkId())) {
            player.packets.sendRemoveObject(openedLeft)
            player.packets.sendRemoveObject(openedRight)
            player.packets.sendAddObject(left)
            player.packets.sendAddObject(right)
        }
    }
}

private fun jumpJaggedWall(player: Player, destination: Tile) {
    if (!World.floorFree(destination, 1)) { player.sendMessage("The other side of the wall is blocked."); return }
    if (!player.legendsTrial(Skills.AGILITY, 50)) { player.skills.drainLevel(Skills.AGILITY, 1); player.sendMessage("You fail to clear the jagged wall. The effort drains your Agility."); return }
    player.sendMessage("You take a good run up and sail majestically over the wall. You land perfectly!")
    player.forceMove(destination, Seq.human_longjump, 0, 60)
}

private fun markedWallPassage(player: Player, obj: GameObject) {
    player.startConversation {
        simple("You place the last rune and the illusory wall fades away, revealing a door. Would you like to go through?")
        options {
            opExec("Yes, I'll go through!") {
                if (player.legendsStage < RESCUE || player.legends.getI("runes") != WALL_RUNES.size || !player.withinDistance(obj.tile, 2)) return@opExec
                val destination = if (obj.tile == Tile.of(2779, 9305, 0)) Tile.of(2775, 9303, 0) else Tile.of(2780, 9306, 0)
                if (!World.floorAndWallsFree(destination, 1)) { player.sendMessage("The other side of the door is blocked."); return@opExec }
                player.useStairs(-1, destination, 1, 2)
            }
            op("No, I'll stay here.")
        }
    }
}

private fun crossFireWall(player: Player) {
    val destination = if (player.inOctagram()) Tile.of(2793, 9336, 0) else Tile.of(2793, 9331, 0)
    player.forceMove(destination, Seq.human_longjump, 0, 60)
}

internal fun passOrbGate(player: Player) {
    if (player.legendsStage < DRY_POOL || !player.legends.getB("orbGate") || player.plane != 0 || !player.withinDistance(Tile.of(2762, 9316, 0), 3)) return
    openLegendsDoors(player, Tile.of(2762, 9316, 0), Tile.of(2763, 9316, 0), Loc.di2_cave_door_lgmagictrialgater, Loc.di2_cave_door_lgmagictrialgatel)
}
