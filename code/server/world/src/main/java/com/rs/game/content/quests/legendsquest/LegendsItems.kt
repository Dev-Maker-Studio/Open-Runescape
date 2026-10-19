package com.rs.game.content.quests.legendsquest

import com.rs.engine.dialogue.startConversation
import com.rs.game.content.skills.smithing.sendSmithingInterface
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onItemOnItem
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPickupItem
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

private val SACRED_POOLS = arrayOf<Any>(Loc.sacred_water, Loc.sacred_water_polluted, Loc.lgwaterpool)
private val BOWLS = intArrayOf(Obj.goldbowl_empty, Obj.goldbowlbless_empty, Obj.goldbowl_water, Obj.goldbowl_pure, Obj.goldbowlbless_water, Obj.goldbowlbless_pure)

@ServerStartupEvent
fun mapLegendsItems() {
    onObjectClick(Loc.kharazi_bamboo_tree_base_leafy) { (player, _, option) ->
        if (option != "Shake" || !player.inKharazi()) return@onObjectClick
        if (!player.inventory.hasFreeSlots()) { player.sendMessage("You need a free inventory slot for a palm leaf."); return@onObjectClick }
        if (player.inventory.addItem(Obj.palm_leaf, 1)) player.sendMessage("You shake a palm leaf loose from the tree.")
    }
    onItemOnItem(intArrayOf(Obj.raw_oomlie), intArrayOf(Obj.palm_leaf)) { event ->
        val player = event.player
        if (!player.legendsLevel(Skills.COOKING, 50) || !player.inventory.containsItems(Obj.raw_oomlie, Obj.palm_leaf)) return@onItemOnItem
        if (player.replaceLegendsItem(Obj.raw_oomlie, Obj.wrapped_oomlie)) {
            player.inventory.deleteItem(Obj.palm_leaf, 1)
            player.skills.addXp(Skills.COOKING, 10.0)
            player.sendMessage("You wrap the oomlie meat in the palm leaf.")
        }
    }
    onPickupItem(Obj.viyeldihat, tiles = arrayOf(Tile.of(2378, 4714, 0))) { event ->
        event.cancelPickup()
        val player = event.player
        if (player.legendsStage < SOURCE || player.legends.getB("viyeldiDead") || !player.withinDistance(event.item.tile, 2)) {
            player.sendMessage("Your hand passes through the hat as though it wasn't there at all.")
            return@onPickupItem
        }
        ensureLegendsNpc(player, Npc.viyeldi, Tile.of(2378, 4714, 0))
        viyeldiDialogue(player)
    }
    onItemClick(Obj.thkaramjamap, Obj.thkaramjamapcomp) { (player, _, option) ->
        if (option == "Complete") mapJungle(player)
        else if (option == "Read") player.startConversation {
            options {
                op("Read Mission Briefing.") { simple("Legends' Quest: Map the Kharazi Jungle, at the Southern tip of Karamja. There are three key areas left to map. Try to contact the Kharazi tribe and establish friendly relations. Bring back a token of the Kharazi's esteem, as proof that you have completed your task.", "Note: There are foresters nearby who have some basic knowledge of the jungle. It would be wise to speak to them before venturing in.") }
                op("Check mapping progress.") {
                    simple("Eastern Kharazi Jungle - ${if (player.legends.getI("map") and 4 != 0) "Complete" else "Incomplete"}",
                        "Middle Kharazi Jungle - ${if (player.legends.getI("map") and 2 != 0) "Complete" else "Incomplete"}",
                        "Western Kharazi Jungle - ${if (player.legends.getI("map") and 1 != 0) "Complete" else "Incomplete"}")
                }
                if (player.legends.getI("map") < 7 && (!player.inKharazi() || player.legends.getI("map") and player.jungleSection() == 0))
                    opExec("Start Mapping Kharazi Jungle.") { mapJungle(player) }
                else op("Close.") { simple("You put the notes away.") }
            }
        }
    }
    onItemClick(Obj.bullroarer, options = arrayOf("Swing")) { (player) ->
        if (player.isLocked) return@onItemClick
        if (player.inCombat()) { player.sendMessage("You can't swing the bullroarer while in combat."); return@onItemClick }
        player.anim(Seq.human_bullroarer)
        player.spotAnim(Spotanim.bullroarer)
        player.sendMessage("You swing the bullroarer above your head. You feel a bit silly at first, but soon it makes an interesting sound.")
        if (!player.inKharazi() || player.legendsStage < CONTACT || !player.legends.getB("bullroarer")) { player.sendMessage("Nothing much seems to happen, though."); return@onItemClick }
        val tile = player.legendsDestination(player.transform(1, 0, 0)) ?: return@onItemClick
        ensureLegendsNpc(player, Npc.gujuo, tile)
        player.sendMessage("You see some movement in the trees... and a tall, dark, charismatic-looking man approaches you.")
        gujuoDialogue(player)
    }
    onItemClick(Obj.goldbowlpic, options = arrayOf("Look")) { (player) -> player.sendMessage("It's a hastily-scribbled charcoal drawing, depicting a bowl. This is what Gujuo was referring to when he said that you needed to make a vessel made from the metal of the sun.") }
    onItemOnObject(arrayOf("Anvil"), arrayOf(Obj.goldbowlpic, "Gold bar")) { (player, obj, item) ->
        if (player.legendsStage >= RESCUE && player.legends.getB("bowlRecipe")) offerGoldenBowl(player, obj)
        else if (item.id == Obj.gold_bar) sendSmithingInterface(player, obj, item.id)
    }
    onObjectClick(Loc.loc_2944) { (player, _, option) ->
        if (option == "Cut-Reed") cutReed(player)
        else player.sendMessage("These hollow reeds might help siphon water from the pool.")
    }
    onItemOnObject(arrayOf(Loc.loc_2944), arrayOf(Obj.machette, Obj.machette_opal, Obj.machette_jade, Obj.machette_redtopaz)) { (player) -> cutReed(player) }
    onObjectClick(*SACRED_POOLS) { (player, obj, option) ->
        if (!legendsPool(obj)) return@onObjectClick
        player.sendMessage(when {
            obj.y > 4600 && !sourcePoolOpen(player, obj) -> "The boulders block the sacred spring."
            obj.y < 4000 && (player.legendsStage in DRY_POOL until WATER_RESTORED || player.legendsStage >= WATER_RESTORED && player.legends.getI("boulders") == 0) -> "The water pool has dried up."
            obj.y > 4600 -> "You see some sparkling, effervescent water, just like the kind you saw above ground."
            option == "Investigate" -> "The water's hard to reach with all these rocks around. There's a narrow gap, but you'll struggle to fit a container through it."
            else -> "A pretty babbling spring. The water bubbles with a strange effervescence."
        })
    }
    onItemOnObject(SACRED_POOLS, arrayOf(Obj.reed_hollow, *BOWLS.toTypedArray())) { (player, obj, item) -> fillSacredBowl(player, obj, item.id) }
    onItemOnObject(SACRED_POOLS, arrayOf(Obj.vial_enchanted)) { (player, obj) -> fillHolyVial(player, obj) }
    onItemOnItem(intArrayOf(Obj.reed_hollow), intArrayOf(Obj.goldbowlbless_empty, Obj.vial_enchanted)) { e ->
        if (!e.player.inKharazi() || e.player.tile.distanceTo(Tile.of(2837, 2915, 0)) > 4) { e.player.sendMessage("You need to be beside the sacred pool."); return@onItemOnItem }
        val pool = GameObject(Loc.sacred_water, ObjectShape.SCENERY_INTERACT, 0, Tile.of(2837, 2915, 0))
        if (e.getUsedWith(Obj.reed_hollow)?.id == Obj.vial_enchanted) fillHolyVial(e.player, pool) else fillSacredBowl(e.player, pool, Obj.reed_hollow)
    }
    onItemClick(*BOWLS.toTypedArray(), options = arrayOf("Empty")) { (player, item) ->
        player.replaceLegendsItem(item.id, if (item.id in intArrayOf(Obj.goldbowlbless_water, Obj.goldbowlbless_pure)) Obj.goldbowlbless_empty else Obj.goldbowl_empty)
        player.sendMessage("You empty the golden bowl.")
    }
    onItemOnItem(intArrayOf(Obj.goldbowlbless_pure), intArrayOf(Obj.vial_enchanted)) { e ->
        val player = e.player
        if (!player.inventory.containsItems(Obj.goldbowlbless_pure, Obj.vial_enchanted)) return@onItemOnItem
        player.inventory.deleteItem(Obj.vial_enchanted, 1)
        player.inventory.addItem(Obj.holy_water, 1)
        val remaining = player.legends.getI("bowlVials", 10) - 1
        player.legends.setI("bowlVials", remaining)
        if (remaining <= 0) player.replaceLegendsItem(Obj.goldbowlbless_pure, Obj.goldbowlbless_empty)
        player.sendMessage("You fill the enchanted vial with sacred water.")
    }
    onItemOnItem(intArrayOf(Obj.goldbowlbless_pure), intArrayOf(Obj.yommiseeds)) { e ->
        val player = e.player
        if (player.legendsStage < SEEDS || !player.legends.getB("seedsGiven")) return@onItemOnItem
        if (!player.inventory.containsItem(Obj.goldbowlbless_pure)) return@onItemOnItem
        val seeds = player.inventory.items.array().firstOrNull { it?.id == Obj.yommiseeds } ?: return@onItemOnItem
        seeds.id = Obj.yommiseeds_germ
        player.inventory.refresh()
        player.inventory.items.array().filterNotNull().filter { it.id == Obj.goldbowlbless_pure }.forEach { it.id = Obj.goldbowlbless_empty }
        player.inventory.refresh()
        player.advanceLegends(DRY_POOL)
        player.sendMessage("You soak the seeds in the sacred water. The seeds germinate. You notice that the sacred pool has dried up.")
    }
    onItemClick(Obj.yommiseeds, Obj.yommiseeds_germ, options = arrayOf("Look")) { (player, item) -> player.sendMessage(if (item.id == Obj.yommiseeds) "These seeds look magical. They need to be germinated in pure sacred water." else "These germinated seeds are ready to be planted in fertile soil.") }
    onItemOnItem(intArrayOf(Obj.vial_water), intArrayOf(Obj.snake_weed, Obj.ardrigal)) { e ->
        val herb = e.getUsedWith(Obj.vial_water) ?: return@onItemOnItem
        val player = e.player
        if (player.legendsStage < DRY_POOL || !player.legends.getB("braveryRecipe") || !player.legendsLevel(Skills.HERBLORE, 45)) return@onItemOnItem
        if (player.replaceLegendsItem(Obj.vial_water, if (herb.id == Obj.snake_weed) Obj.snakeweed_sol else Obj.ardrigal_sol)) {
            player.inventory.deleteItem(herb.id, 1)
            player.sendMessage("You mix the herb into the vial of water.")
        }
    }
    onItemOnItem(intArrayOf(Obj.snakeweed_sol), intArrayOf(Obj.ardrigal)) { e -> finishBraveryPotion(e.player, Obj.snakeweed_sol, Obj.ardrigal) }
    onItemOnItem(intArrayOf(Obj.ardrigal_sol), intArrayOf(Obj.snake_weed)) { e -> finishBraveryPotion(e.player, Obj.ardrigal_sol, Obj.snake_weed) }
    onItemClick(Obj.bravery_pot, options = arrayOf("Drink")) { (player) ->
        if (player.legendsStage < DRY_POOL) return@onItemClick
        player.startConversation {
            simple("The Bravery Potion bubbles with a strange effervescence, but it smells very strange, like oily burnt oranges. Are you sure you want to drink it?")
            options {
                opExec("Yes, I'll bravely drink the Bravery Potion.") {
                    if (player.replaceLegendsItem(Obj.bravery_pot, Obj.vial_empty)) {
                        player.legends.setB("brave", true)
                        player.sendMessage("You bravely swig down the entire contents of the vial. After a few seconds, you realise that you actually feel alright.")
                    }
                }
                op("Actually, let me think it over a while longer; it smells a bit odd.")
            }
        }
    }
    onItemClick(Obj.book_of_binding) { (player, _, option) ->
        if (option == "Enchant-Vials") enchantVial(player)
        else player.openBook(LegendsBook("Book of Binding", "The spirits may be bound by this ancient book. Bring its words before the one who is possessed. An empty vial may be enchanted to retain the sacred water's power."))
    }
    onItemOnItem(intArrayOf(Obj.book_of_binding), intArrayOf(Obj.vial_empty)) { e -> enchantVial(e.player) }
    onItemClick(Obj.holyforce, options = arrayOf("Cast Spell")) { (player) ->
        if (player.x in 2385..2402 && player.y in 4673..4688) revealEchned(player, true)
        else player.sendMessage("You need to use this spell on the spirit at the source of the spring.")
    }
    onItemClick(Obj.heartcrystal, Obj.heartcrystal_glow, Obj.thtotempolegift) { (player, item) ->
        player.sendMessage(when (item.id) {
            Obj.heartcrystal -> "As the crystal touches your hands a voice inside of your head says... Bring life to the dragon's eye."
            Obj.heartcrystal_glow -> "This crystal seems to glow and has a mystical aura about it. It seems to be vaguely heart shaped."
            else -> "This totem pole is truly awe inspiring. It depicts powerful Karamjan animals. It is very well carved and brings a sense of power and spiritual fulfilment to anyone who looks at it."
        })
    }
}

private fun Player.jungleSection() = when { x < 2810 -> 1; x < 2870 -> 2; else -> 4 }

private fun mapJungle(player: Player) {
    if (player.legendsStage !in MAPPING until TRAINING || !player.inventory.containsItem(Obj.thkaramjamap)) return
    if (!player.inKharazi()) { player.sendMessage("You're not even in the Kharazi Jungle yet. You need to get to the Southern end of Karamja before you can start mapping."); return }
    val part = player.jungleSection()
    if (player.legends.getI("map") and part != 0) { player.sendMessage("You have already completed this part of the map."); return }
    if (!player.inventory.containsItems(Obj.papyrus, Obj.charcoal)) { player.sendMessage("You need papyrus and charcoal to map the jungle."); return }
    if (!player.legendsLevel(Skills.CRAFTING, 50)) return
    if (!player.legendsTrial(Skills.CRAFTING, 50)) {
        when (Utils.random(4)) {
            0 -> { player.inventory.deleteItem(Obj.charcoal, 1); player.sendMessage("Your charcoal breaks while you draw the map.") }
            1 -> { player.inventory.deleteItem(Obj.papyrus, 1); player.sendMessage("You tear the papyrus while drawing the map.") }
            2 -> { player.inventory.deleteItem(Obj.charcoal, 1); player.inventory.deleteItem(Obj.papyrus, 1); player.sendMessage("Your charcoal breaks and the papyrus is ruined.") }
            else -> player.sendMessage("You make a mistake, but manage to salvage the papyrus. Try again.")
        }
        return
    }
    player.inventory.deleteItem(Obj.papyrus, 1)
    player.legends.setI("map", player.legends.getI("map") or part)
    player.sendMessage("You use your Crafting skill to neatly add a new section to your map.")
    if (player.legends.getI("map") == 7) {
        player.replaceLegendsItem(Obj.thkaramjamap, Obj.thkaramjamapcomp)
        player.advanceLegends(CONTACT)
        player.sendMessage("Well done! You have finished mapping the Kharazi Jungle. Radimus Erkle will be pleased.")
    }
    player.sendMessage("Eastern Kharazi Jungle - ${if (player.legends.getI("map") and 4 != 0) "Complete" else "Incomplete"}")
    player.sendMessage("Middle Kharazi Jungle - ${if (player.legends.getI("map") and 2 != 0) "Complete" else "Incomplete"}")
    player.sendMessage("Western Kharazi Jungle - ${if (player.legends.getI("map") and 1 != 0) "Complete" else "Incomplete"}")
}

internal fun offerGoldenBowl(player: Player, anvil: GameObject) {
    if (player.legendsStage < RESCUE || !player.legends.getB("bowlRecipe")) return
    player.startConversation {
        options("Would you like to make a golden bowl?") {
            opExec("Yes.") {
                if (!player.legendsLevel(Skills.SMITHING, 50)) return@opExec
                if (!player.inventory.containsItem(Obj.gold_bar, 2) || !player.inventory.containsItem(Obj.hammer)) { player.sendMessage("You need two Gold Bars and a hammer to make a bowl."); return@opExec }
                player.faceObject(anvil)
                player.inventory.deleteItem(Obj.gold_bar, 2)
                player.anim(Seq.human_smithing)
                if (!player.legendsTrial(Skills.SMITHING, 50)) { player.sendMessage("You fail to shape the gold into a bowl. The gold is ruined."); return@opExec }
                player.inventory.addItem(Obj.goldbowl_empty, 1)
                player.sendMessage("You forge a beautiful bowl from solid gold.")
            }
            op("No.")
        }
    }
}

private fun cutReed(player: Player) {
    if (player.legendsStage < RESCUE || !player.inKharazi()) return
    val machetes = intArrayOf(Obj.machette, Obj.machette_opal, Obj.machette_jade, Obj.machette_redtopaz)
    if (!player.inventory.containsOneItem(*machetes) && player.equipment.weaponId !in machetes) { player.sendMessage("You need a machete to cut a hollow reed."); return }
    if (player.giveLegendsItem(Obj.reed_hollow)) player.sendMessage("You cut a hollow reed.")
}

private fun canFillSacredWater(player: Player, obj: GameObject): Boolean {
    if (player.legendsStage < RESCUE || !legendsPool(obj)) return false
    if (obj.y > 4600) {
        if (player.legendsStage < WATER_RESTORED || !sourcePoolOpen(player, obj)) { player.sendMessage("The boulders block the sacred spring."); return false }
    } else if (player.legendsStage in DRY_POOL until WATER_RESTORED || (player.legendsStage >= WATER_RESTORED && player.legends.getI("boulders") == 0)) {
        player.sendMessage("The water pool has dried up.")
        return false
    }
    return true
}

private fun legendsPool(obj: GameObject) = obj.plane == 0 && (obj.tile == Tile.of(2837, 2915, 0) || obj.tile in SOURCE_POOLS)

private fun sourcePoolOpen(player: Player, obj: GameObject): Boolean {
    val index = SOURCE_POOLS.indexOf(obj.tile)
    return index >= 0 && player.legends.getI("boulders") and (1 shl index) != 0
}

private fun fillSacredBowl(player: Player, obj: GameObject, used: Int) {
    if (!canFillSacredWater(player, obj)) return
    if (obj.y < 4000 && !player.inventory.containsItem(Obj.reed_hollow)) { player.sendMessage("You need a hollow reed to siphon water from this pool."); return }
    val bowl = when {
        used in BOWLS && player.inventory.containsItem(used) && used in intArrayOf(Obj.goldbowlbless_empty, Obj.goldbowl_empty) -> used
        used in BOWLS -> { player.sendMessage("You need to empty that bowl first."); return }
        player.inventory.containsItem(Obj.goldbowlbless_empty) -> Obj.goldbowlbless_empty
        player.inventory.containsItem(Obj.goldbowl_empty) -> Obj.goldbowl_empty
        else -> { if (used == Obj.reed_hollow && player.inventory.containsItem(Obj.vial_enchanted)) fillHolyVial(player, obj) else player.sendMessage("You need an empty golden bowl."); return }
    }
    player.replaceLegendsItem(bowl, if (bowl == Obj.goldbowlbless_empty) Obj.goldbowlbless_pure else Obj.goldbowl_pure)
    player.legends.setI("bowlVials", 10)
    player.sendMessage("You siphon some water from the pool into your golden bowl.")
}

private fun fillHolyVial(player: Player, obj: GameObject) {
    if (!canFillSacredWater(player, obj) || !player.inventory.containsItem(Obj.vial_enchanted)) return
    if (obj.y < 4000 && !player.inventory.containsItem(Obj.reed_hollow)) { player.sendMessage("You need a hollow reed to siphon water from this pool."); return }
    if (player.replaceLegendsItem(Obj.vial_enchanted, Obj.holy_water)) player.sendMessage(if (obj.y < 4000) "You use the hollow reed to siphon some water from the pool into your vial." else "You scoop some sacred water into your enchanted vial.")
}

private fun enchantVial(player: Player) {
    if (!player.inventory.containsItems(Obj.book_of_binding, Obj.vial_empty)) { player.sendMessage("You need the binding book and an empty vial."); return }
    if (player.skills.getLevel(Skills.MAGIC) < 10 || player.prayer.points < 100) { player.sendMessage("You need at least 10 Magic and 10 Prayer points to enchant a vial."); return }
    if (player.replaceLegendsItem(Obj.vial_empty, Obj.vial_enchanted)) {
        player.prayer.drainPrayer(50.0)
        player.skills.adjustStat(-5, 0.0, Skills.MAGIC)
        player.anim(Seq.seq_726)
        player.sendMessage("You enchant the empty vial using the Book of Binding.")
    }
}

private fun finishBraveryPotion(player: Player, mixture: Int, herb: Int) {
    if (!player.legends.getB("braveryRecipe") || !player.legendsLevel(Skills.HERBLORE, 45) || !player.inventory.containsItems(mixture, herb)) return
    if (player.replaceLegendsItem(mixture, Obj.bravery_pot)) {
        player.inventory.deleteItem(herb, 1)
        player.sendMessage("You mix the two ingredients together. It makes a heady brew. This must be what Gujuo was talking about. You certainly would need to be brave to drink this!")
    }
}
