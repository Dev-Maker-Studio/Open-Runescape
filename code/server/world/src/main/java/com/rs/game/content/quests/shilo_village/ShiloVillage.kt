package com.rs.game.content.quests.shilo_village

import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Loc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import org.darkan.core.map.pathfinder.Direction
import com.rs.engine.quest.Quest
import com.rs.engine.quest.QuestHandler
import com.rs.engine.quest.QuestOutline
import com.rs.game.World
import com.rs.game.World.addGroundItem
import com.rs.game.World.removeGroundItem
import com.rs.game.World.spawnNPC
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.DEAL_WITH_RASHILIYIAS_CORPSE
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.FIND_TEMPLE_OF_AH_ZA_RHOON
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.INVESTIGATE_B_TOMB
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.RETURN_TO_TRUFITUS_AGAIN
import com.rs.game.content.world.doors.Doors
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Constants
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import com.rs.plugin.PluginManager
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.events.PickupItemEvent
import com.rs.plugin.kts.*

@QuestHandler(
    quest = Quest.SHILO_VILLAGE,
    startText = "Talk to Mosol Rei, who is outside of Shilo Village in the south part of Karamja.",
    itemsText = "A spool of bronze wire, 3 regular bones, a rope, a chisel, and a torch or other light source.",
    combatText = "Nazastarool (Level 53) (First appears as a zombie, then as a skeleton, and finally as a ghost)",
    rewardsText = "2 Quest Points<br>3,750 Crafting XP<br>Access to the Shilo Village, gem rocks and cart<br>travel between Shilo Village and Brimhaven<br>2,000 coins from Yanni Salika in Shilo Village if you sell him the quest's items",
    completedStage = 9
)

class ShiloVillage : QuestOutline() {
    companion object {
        const val NOT_STARTED = 0
        const val DELIVER_BELT = 1
        const val FIND_TEMPLE_OF_AH_ZA_RHOON = 2
        const val VERIFY_TEMPLE_OF_AH_ZA_RHOON = 3
        const val RETURN_TO_TRUFITUS = 4
        const val INVESTIGATE_B_TOMB = 5
        const val RETURN_TO_TRUFITUS_AGAIN = 6
        const val RASHILIYIAS_TOMB = 7
        const val DEAL_WITH_RASHILIYIAS_CORPSE = 8
        const val COMPLETED = 9
    }

    override fun getJournalLines(player: Player, stage: Int) = when (stage) {
        NOT_STARTED -> listOf("Talk to Mosol Rei, who is outside of Shilo Village in the south part of Karamja.")
        DELIVER_BELT -> listOf("Take the Wampum belt to Trufitus in Tai Bwo Wannai Village.")
        FIND_TEMPLE_OF_AH_ZA_RHOON -> listOf("Find the Temple of Ah Za Rhoon.")
        VERIFY_TEMPLE_OF_AH_ZA_RHOON -> listOf("The cavern system I found might be Ah Za Rhoon, if I could identify the cavern it would help a lot.")
        RETURN_TO_TRUFITUS -> listOf("Take what I found at the Temple of Ah Za Rhoon to Trufitus.")
        INVESTIGATE_B_TOMB -> listOf("Search Bervirius' tomb for clues.")
        RETURN_TO_TRUFITUS_AGAIN -> listOf("Talk to Trufitus about what I found at Berivius' Tomb.")
        RASHILIYIAS_TOMB -> listOf("Search Rashiliyia's tomb for clues.")
        DEAL_WITH_RASHILIYIAS_CORPSE -> listOf("Deal with Rashiliyia's corpse.")
        COMPLETED -> listOf("QUEST COMPLETE!")
        else -> listOf("Invalid quest stage. Report this to an administrator.")
    }

    override fun complete(player: Player) {
        player.skills.addXpQuest(Constants.CRAFTING, 3875.0)
        sendQuestCompleteInterface(player, Obj.cake)
    }

}

@ServerStartupEvent
fun mapShiloVillage() {
    var lit = false
    var isBossUp = false


    fun spawnBoss(player: Player) {
        player.packets.sendCameraShake(3, 50, 1, 20, 1)
        WorldTasks.schedule(2) {
            val nazastarool = spawnNPC(Npc.zq_mainzombie1, Tile.of(player.tile, 1), true, true)
            nazastarool.isForceAgressive = true
            nazastarool.setForceAggroDistance(10)
            nazastarool.combatTarget = player
            WorldTasks.schedule(2) {
                player.packets.sendCameraShake(3, 0, 0, 0, 0)
                player.sendMessage("Nazastarool: Who dares disturb Rashiliyia's rest?")
                nazastarool.forceTalk("Who dares disturb Rashiliyia's rest?")
                WorldTasks.schedule(2) {
                    player.sendMessage("Nazastarool: I am Nazastarool! Prepare to die!")
                    nazastarool.forceTalk("I am Nazastarool! Prepare to die!")
                }
            }
        }
    }

    onNpcDeath(Npc.zq_mainzombie1){ e ->
        val location = e.npc.tile
        WorldTasks.schedule(7) {
            val nazastarool = spawnNPC(Npc.zq_mainzombie2, location, true, true)
            nazastarool.faceDir(e.npc.direction)
            nazastarool.isForceAgressive = true
            nazastarool.setForceAggroDistance(10)
            nazastarool.combatTarget = e.killer
            val player = e.killer as Player?
            WorldTasks.schedule(2) {
                player?.sendMessage("Nazastarool: Quake in fear, for I am reborn!")
                WorldTasks.schedule(2) {
                    player?.sendMessage("Nazastarool: Your death will be swift.")
                }
            }
        }
    }

    onNpcDeath(Npc.zq_mainzombie2){ e ->
        WorldTasks.schedule(7) {
            val location = e.npc.tile
            val nazastarool = spawnNPC(Npc.zq_mainzombie3, location, true, true)
            nazastarool.faceDir(e.npc.direction)
            nazastarool.isForceAgressive = true
            nazastarool.setForceAggroDistance(10)
            nazastarool.combatTarget = e.killer
            val player = e.killer as Player?
            WorldTasks.schedule(2) {
                player?.sendMessage("Nazastarool: Nazastarool returns with vengeance!")
                nazastarool.forceTalk("Nazastarool returns with vengeance!")
                WorldTasks.schedule(2) {
                    player?.sendMessage("Nazastarool: Soon you will serve Rashiliyia.")
                    nazastarool.forceTalk("Soon you will serve Rashiliyia.")
                }
            }
        }
    }

    onNpcDeath(Npc.zq_mainzombie3){ e ->
        e.killer.soundEffect(Sound.sound_438, true)
        isBossUp = false
        val player = e.killer as Player?
        player?.sendMessage("You see something appear on the dolmen.")
        player?.sendMessage("Nazastarool: May you perish in the fires of Zamorak's furnace! Rashiliyia's curse is upon you!")
        player?.startConversation {
            simple("You hear a disembodied voice fading away into the distance:<br>May you perish in the fires of Zamorak's furnace!<br>Rashiliyia's curse is upon you!")
            exec{
                addGroundItem(Item(Obj.zqcorpse), Tile.of(2892, 9487, 0), player)
                player.questManager?.getAttribs(Quest.SHILO_VILLAGE)?.setB("HAS_KILLED_BOSS", true)
            }
        }
    }

    onPickupItem(Obj.zqcorpse){ e ->
        e.player.startConversation {
            item(e.item.id, "You take the mummified remains of a human female. You feel certain that these are Rashiliyia's remains. You carefully place the remains in your inventory.")
        }
        e.player.setQuestStage(Quest.SHILO_VILLAGE, DEAL_WITH_RASHILIYIAS_CORPSE)
    }

    onItemOnItem(intArrayOf(Obj.chisel), intArrayOf(Obj.zqboneshard)) { e ->
        when (e.player.getQuestStage(Quest.SHILO_VILLAGE)) {
            7 -> {
                if(e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_TRIED_LOCK")) {
                    if (e.player.getLevel(Skills.CRAFTING) >= 20) {
                        e.player.startConversation {
                            simple("Remembering Zadimus' words and the strange bone lock, you start to craft the bone. You successfully make a key out of the bone shard.")
                            exec {
                                e.player.anim(Seq.human_diamondcutting)
                                WorldTasks.schedule(2) {
                                    e.player.inventory.removeItems(Item(Obj.zqboneshard, 1))
                                    e.player.inventory.addItem(Item(Obj.zqbonekey, 1))
                                }
                            }
                        }
                    } else {
                        e.player.startConversation {
                            simple("You need to have a Crafting level of at least 20 to work this material.")
                        }
                    }
                }else{
                    e.player.startConversation {
                        simple("You're not quite sure what to make with this. Perhaps it will come to you as you discover more about Rashiliyia?")
                    }
                }
            }
            else -> {
                e.player.startConversation {
                    simple("You're not quite sure what to make with this. Perhaps it will come to you as you discover more about Rashiliyia?")
                }
            }
        }
    }

    onItemOnItem(intArrayOf(Obj.chisel), intArrayOf(Obj.zqbevsword)) { e ->
        when (e.player.getQuestStage(Quest.SHILO_VILLAGE)) {
            7 -> {
                e.player.startConversation {
                    item(Obj.zqbevsword, "You prepare the ivory pommel and the chisel to start crafting...")
                    item(Obj.zqbonebeads, "You successfully craft some of the ivory into beads. They may look good as part of a necklace.")
                    exec {
                        e.player.anim(Seq.human_diamondcutting)
                        WorldTasks.schedule(2) {
                            e.player.inventory.removeItems(Item(Obj.zqbevsword, 1))
                            e.player.inventory.addItem(Item(Obj.zqbonebeads, 1))
                        }
                    }
                }
            }
            else -> {
                e.player.startConversation {
                    simple("You're not quite sure what to make with this. Perhaps it will come to you as you discover more about Rashiliyia?")
                }}
        }
    }

    onItemOnItem(intArrayOf(Obj.rd_wire, Obj.bronzecraftwire), intArrayOf(Obj.zqbonebeads)) { e ->
        when (e.player.getQuestStage(Quest.SHILO_VILLAGE)) {
            7 -> {
                e.player.startConversation {
                    item(Obj.zqdeadbeads, "You successfully craft the beads and bronze wire into a necklace which you name, 'Beads of the Dead'")
                    exec {
                        e.player.anim(Seq.human_diamondcutting)
                        WorldTasks.schedule(1) {
                            e.player.inventory.removeItems(Item(e.item1.id, 1))
                            e.player.inventory.removeItems(Item(e.item2.id, 1))
                            e.player.inventory.addItem(Item(Obj.zqdeadbeads, 1))
                        }
                    }
                }
            }
            else -> {
                e.player.startConversation {
                    simple("You're not quite sure what to make with this. Perhaps it will come to you as you discover more about Rashiliyia?")
                }}
        }
    }

    onItemClick(Obj.zqboneshard, options = arrayOf("Inspect")){ (player) ->
        player.startConversation {
            simple("You look at the shard of bone. The words of Zadimus come back to you. 'I am the key, but only kin may approach her.'")
        }
    }

    onItemClick(Obj.zqzadimusbones, options = arrayOf("Bury")){ (player) ->
        val statuetile = Tile.of(2795, 3090, 0)
        if(player.tile.withinDistance(statuetile, 4) && player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_SHOWN_Z_CORPSE")) {
            player.sendMessage("You feel an unearthly compunction to bury this corpse!")
            player.sendMessage("You dig a hole for the remains.")
            player.lock(9)
            WorldTasks.schedule(1) {
                player.anim(Seq.seq_830)
            }
            WorldTasks.schedule(2) {
                player.anim(Seq.seq_827)
                player.sendMessage("You lay the remains of Zadimus to rest")
                player.inventory.removeItems(Item(Obj.zqzadimusbones, 1))
            }
            WorldTasks.schedule(4) {
                player.faceDir(Direction.SOUTH)
            }
            WorldTasks.schedule(6) {
                player.lock()
                val ghost = spawnNPC(Npc.zadimus_ghost, player.tile, true, true)
                ghost.setRandomWalk(false)
                ghost.faceDir(Direction.SOUTH)
                ghost.addWalkSteps(Tile.of(2975, 3084, 0), 2, false)
                ghost.faceDir(Direction.NORTH)
                player.startConversation {
                    simple("You hear an unearthly moaning sound as you see an apparition materialise right in front of you.")
                    npc(Npc.zadimus_ghost,HeadE.CALM_TALK,"You have released me from my torture, and now I shall aid you. You seek to dispell the one who tortured and killed me. Remember this... 'I am the key, but only kin may approach her.'") {
                        exec {
                            player.unlock()
                            ghost.finish()
                            player.inventory.addItem(Obj.zqboneshard, 1)
                            player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_BURIED_Z_CORPSE", true)
                        }
                    }
                    item(Obj.zqboneshard,"The apparition disappears into the ground where you buried the corpse. You see the ground in front of you shake as a shard of bone forces its way to the surface. You take the bone shard and place it in your")
                    item(Obj.zqboneshard, "inventory.")
                }
            }
        }else{
            player.sendMessage("You feel an unearthly compunction to bury this corpse!")
            WorldTasks.schedule(2) {
                player.startConversation {
                    simple("You hear a ghostly wailing sound coming from the corpse and a whispering voice says,'Let me rest in a sacred place and assist you I will'.")
                }
            }
        }
    }

    onItemClick(Obj.zqberviriusscroll, options = arrayOf("Read")) { e ->
        e.player.startConversation {
            simple("This looks like part of a scroll about someone called Bervirius<br>Would you like to read it?")
            options("SELECT AN OPTION") {
                op("Yes please.") {
                    exec{
                        e.player.sendShiloScroll(
                            "",
                            "",
                            "Bervirius, son of King Danthalas, was killed in ",
                            "battle. His bravery became legend... devout mother",
                            "Rashiliya was so heartbroken that she swore fealty",
                            "to Zamorak if he would return her son to her.",
                            "Bervirius returned as an undead creature...",
                            "Many guards died fighting the undead Bervirius.",
                            "Eventually the undead Bervirius was set on fire...",
                            "His remains were taken far to the South, and then",
                            "towards the setting sun to a tomb that is",
                            "surrounded by and level with the sea. This is the",
                            "only way to contain the spirits of witches and the",
                            "undead."
                        )
                        if(!e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_TATTERED_SCROLL")) {
                            if(e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_CRUMPLED_SCROLL")) {
                                if (e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_COLLECTED_Z_CORPSE")) {
                                    e.player.setQuestStage(Quest.SHILO_VILLAGE, 4)
                                }
                            }
                        }
                        e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_TATTERED_SCROLL", true)
                    }
                }
                op("No thanks.") {
                    simple("You decide not to open the scroll. But instead place it carefully back in your inventory.")
                }
            }
        }
    }

    onItemClick(Obj.zqberviriusscroll2, options = arrayOf("Read")) { e ->
        e.player.startConversation {
            item(Obj.zqberviriusscroll2,"This scroll is a collection of writings. Some of them are just scraps of papyrus with what looks like random scribblings. Which would you like to read?")
            options("Select an Option") {
                op("Tattered Yellow papyrus.") {
                    simple("...and rest like your mother who is silent in the peace of her tomb far to the North of Ah Za Rhoon. Near the sea, and under the hills deep in the underground to watch all nature from the darkness of her final resting place.")
                    exec{
                        if(!e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_YELLOW_PAPYRUS")) {
                            if(e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_ORANGE_PAPYRUS")) {
                                if(e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_WHITE_PAPYRUS")) {
                                    e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_BERVIRIUS_NOTES", true)
                                }
                            }
                        }
                        e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_YELLOW_PAPYRUS", true)
                    }
                }
                op("Decayed White papyrus.") {
                    simple("...Rashiliyia did so love objects of beauty. Her tomb was adorned with crystals that glowed brightly when near to each other.")
                    exec{
                        if(!e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_WHITE_PAPYRUS")) {
                            if(e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_ORANGE_PAPYRUS")) {
                                if(e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_YELLOW_PAPYRUS")) {
                                    e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_BERVIRIUS_NOTES", true)
                                }
                            }
                        }
                        e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_WHITE_PAPYRUS", true)
                    }
                }
                op("Crusty Orange papyrus.") {
                    simple("...Rashiliyia did so love objects of beauty. Her tomb was adorned with crystals that glowed brightly when near to each other.")
                    exec{
                        if(!e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_ORANGE_PAPYRUS")) {
                            if(e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_WHITE_PAPYRUS")) {
                                if(e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_YELLOW_PAPYRUS")) {
                                    e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_BERVIRIUS_NOTES", true)
                                }
                            }
                        }
                        e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_ORANGE_PAPYRUS", true)
                    }
                }
            }
        }
    }

    onItemClick(Obj.zqrashiliyiascroll, options = arrayOf("Read")) { e ->
        e.player.startConversation {
            simple("This looks like part of a scroll about Rashiliya. Would you like to read it?")
            options("SELECT AN OPTION") {
                op("Yes please.") {
                    exec{
                        e.player.sendShiloScroll(
                            "",
                            "",
                            "Rashiliya's rage went unchecked. She killed",
                            "without mercy for revenge of her son's life. Like a",
                            "spectre through the night she entered houses and",
                            "one by one quietly strangled life from the.",
                            "occupants. It is said that only a handful survived,",
                            "protected by necklace wards to keep the Witch ",
                            "Queen at bay."
                        )
                        if(!e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_CRUMPLED_SCROLL")) {
                            if (e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_TATTERED_SCROLL")) {
                                if (e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_COLLECTED_Z_CORPSE")) {
                                    e.player.setQuestStage(Quest.SHILO_VILLAGE, 4)
                                }
                            }
                        }
                        e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_CRUMPLED_SCROLL", true)
                    }
                }
                op("No thanks.") {
                    exec{
                        e.player.sendMessage("You decide to leave the scroll well alone.")
                    }
                }
            }
        }
    }

    onItemClick(Obj.zqplaque, options = arrayOf("Read")) { e ->
        e.player.startConversation {
            simple("The markings are very intricate. It's a very strange language. The meaning of it evades you though. Perhaps Trufitus can decipher the markings?")
            {
                exec {
                    if (!e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_STONE_PLAQUE")) {
                        if (e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_CRUMPLED_SCROLL")) {
                            if (e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_TATTERED_SCROLL")) {
                                if (e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_COLLECTED_Z_CORPSE")) {
                                    e.player.setQuestStage(Quest.SHILO_VILLAGE, 4)
                                }
                            }
                        }
                    }
                    e.player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_READ_STONE_PLAQUE", true)
                }

            }
        }
    }

    onItemOnObject(objectNamesOrIds = arrayOf(Loc.ahzarhoon_entrance_fissure), itemNamesOrIds = arrayOf("Lit candle", "Lit torch", "Lit black candle")){ (player, obj, item) ->
        if((player.getQuestStage(Quest.SHILO_VILLAGE) >= 2)) {
            player.anim(Seq.seq_827)
            WorldTasks.schedule(2) {
                player.inventory.removeItems(Item(item.id, 1))
                lit = true
                player.startConversation {
                    item(item.id,"You drop the " + item.name + " into the fissure and see that there is quite a large drop after you get through the hole. There is a good anchor point nearby onto which you could tie a rope.")
                    simple("Some rope might help here.")
                }
            }
            WorldTasks.schedule(30) {
                lit = false
                player.startConversation {
                    item(item.id, "The ${item.name} burns out.")
                }
            }
        }
    }

    onItemOnObject(objectNamesOrIds = arrayOf(Loc.zqdolmen), itemNamesOrIds = arrayOf(Obj.zqcorpse)){ (player, obj, item) ->

        player.anim(Seq.seq_827)
        WorldTasks.schedule(1) {
            player.inventory.removeItems(Item(item.id, 1))
            WorldTasks.schedule(2) {
                val rashiliyia = spawnNPC(Npc.zqzombiequeen, Tile.of(player.tile, 1), true, true)
                rashiliyia.faceEntity(player)
                player.faceEntity(rashiliyia)
                rashiliyia.setRandomWalk(false)
                player.startConversation {
                    item(
                        item.id,
                        "You carefully place Rashiliyias remains on the dolmen. You feel a strange vibration in the air."
                    )
                    exec {
                        player.sendMessage("Without warning the spirit of Rashiliyia disappears")
                    }
                    npc(
                        Npc.zqzombiequeen,
                        HeadE.CALM_TALK,
                        "You have my gratitude for releasing my spirit. I have suffered a vengeful and evil existence. I was tricked by Zamorak. He returned my son to me as an undead creature."
                    )
                    npc(
                        Npc.zqzombiequeen,
                        HeadE.CALM_TALK,
                        "My hatred and bitterness corrupted me. I tried to destroy all life... now I am released. And am grateful to contemplate eternal rest..."
                    )
                    exec {
                        player.questManager.completeQuest(Quest.SHILO_VILLAGE)
                        player.questManager.setStage(Quest.SHILO_VILLAGE, 9)
                        rashiliyia.finish()
                    }
                }
            }
        }
    }

    onItemOnObject(objectNamesOrIds = arrayOf(Loc.thzq_tombrooml1, Loc.thzq_tombroomr1, Loc.thzq_tombrooml2, Loc.thzq_tombrooml3, Loc.thzq_tombroomr2), itemNamesOrIds = arrayOf("Bones")){ (player, obj, item) ->
        if(player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_USED_ONE_BONE")) {
            if(player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_USED_TWO_BONES")) {
                player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_USED_THREE_BONES", true)
                player.sendMessage("You carefully place another bone in the door...")
                player.anim(Seq.seq_827)
                WorldTasks.schedule(2) {
                    player.inventory.removeItems(Item(item.id, 1))
                    World.getObject(Tile.of(2893,9480,0))?.setIdTemporary(Loc.thzq_tombroomr2, 270)
                }
                player.inventory.removeItems(Item(item.id, 1))
                player.startConversation{
                    item(item.id, "You place the bone into the last recess on the skeletal door and it fits. All the recesses are filled...")
                    simple("The door seems to change slightly. Two depictions of skeletal warriors turn their heads towards you. They are alive! The skeletons wrench themselves free of the door. Stepping out of the door, with grinning teeth they push the huge doors open.")
                }
            }else{
                player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_USED_TWO_BONES", true)
                player.sendMessage("You carefully place another bone in the door...")
                player.anim(Seq.seq_827)
                WorldTasks.schedule(2) {
                    player.inventory.removeItems(Item(item.id, 1))
                    World.getObject(Tile.of(2892,9480,0))?.setIdTemporary(Loc.thzq_tombrooml3, 270)
                    player.startConversation{
                        item(item.id, "You place the bone into the skeletal door and it fits. There is just one recess left now.")
                    }
                }
            }
        }else{
            player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_USED_ONE_BONE", true)
            player.sendMessage("You carefully place a bone in the door...")
            player.anim(Seq.seq_827)
            WorldTasks.schedule(2) {
                player.inventory.removeItems(Item(item.id, 1))
                World.getObject(Tile.of(2892,9480,0))?.setIdTemporary(Loc.thzq_tombrooml2, 270)
                player.startConversation{
                    item(item.id, "You place the bone into the skeletal door and it fits. There are two recesses left now.")
                }
            }
            player.sendMessage("You fit the bone into the recess.")
        }
        if(player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_USED_THREE_BONES")) {
            Doors.handleDoubleDoor(player, World.getObject(Tile.of(2893,9480,0)))
        }
    }

    onItemOnObject(arrayOf("Ancient Door"), itemNamesOrIds = arrayOf(Obj.zqbonekey)){ (player, obj, item) ->
        player.sendMessage("You try the key with the lock")
        player.anim(Seq.seq_827)
        WorldTasks.schedule(1) {
            player.sendMessage("A shimmering light dances over the doors, before you can blink, the doors creak open.")
        }
        WorldTasks.schedule(2) {
            player.vars.setVarBit(Varbit.varbit_4882, 2)
        }
        WorldTasks.schedule(3) {
            player.startConversation{
                simple("The doors close behind you with the sound of crunching bone. Before you stretches a winding tunnel blocked by an ancient gate.")
                player.tele(2928, 9521, 0)
            }
            WorldTasks.schedule(1) {
                if (player.equipment.neckId != Obj.zqdeadbeads) {
                    player.sendMessage("Rashiliyia appears!")
                    WorldTasks.schedule(5) {
                        player.sendMessage("You feel invisible hands starting to choke you...")
                    }
                }
            }
        }
    }

    onItemOnObject(arrayOf(Loc.hillsideexitclosedr), itemNamesOrIds = arrayOf(Obj.zqbonekey)){ (player, obj, item) ->
        player.sendMessage("You unlock the door with the key.")
        player.anim(Seq.seq_827)
        WorldTasks.schedule(1) {
            player.sendMessage("The doors creak open revealing bright daylight.")
        }
        WorldTasks.schedule(2) {
            player.sendMessage("You walk outside into the warmth of the jungle heat.")
            player.tele(2916, 3092, 0)
        }
        WorldTasks.schedule(3) {
            player.sendMessage("You get a sense that something seems incomplete.")
        }
    }

    onItemOnObject(objectNamesOrIds = arrayOf(Loc.ahzarhoon_entrance_fissure), itemNamesOrIds = arrayOf("Rope")){ (player, obj, item) ->
        if(!lit) {
            player.startConversation {
                player(HeadE.CONFUSED, "There is not enough light to see a safe place place to anchor it.")
            }
        }
        if(lit) {
            player.startConversation {
                item(item.id,"You see where to attach the rope very clearly. You secure the rope well.")
            }
            player.anim(Seq.seq_827)
            WorldTasks.schedule(2) {
                obj.setIdTemporary(Loc.ahzarhoon_entrance_fissure_withrope, 360)
                player.inventory.removeItems(Item(item.id, 1))
            }



        }
    }

    onItemOnObject(objectNamesOrIds = arrayOf(Loc.zqsecretstone), itemNamesOrIds = arrayOf("Chisel")){ (player) ->
        if(!player.inventory.containsItem(Obj.zqplaque) && !player.bank.containsItem(Obj.zqplaque)) {
            player.startConversation {
                item(
                    Obj.zqplaque,
                    "You cleanly cut the plaque of letters away from the rock. You place it carefully into your inventory."
                )
                player.anim(Seq.seq_827)
                if (player.inventory.hasFreeSlots()) {
                    player.sendMessage("You add the stone plaque to your inventory.")
                    player.inventory.addItem(Obj.zqplaque, 1, false)
                    player.questManager.getAttribs(Quest.SHILO_VILLAGE).setB("HAS_COLLECTED_PLAQUE", true)
                } else {
                    player.sendMessage("You do not have enough room in your inventory.")
                }
            }
        }
    }

    onObjectClick(Loc.ahzarhoon_entrance_fissure) { (player, obj, options) ->
        if (options == "Search") {
            player.startConversation {
                simple("You see a small fisure in the granite that you might just be able to crawl through. Beyond the fissure is a long fall. Do you want to try to crawl through the fissure?")
                options("CLIMB INTO THE FISURE?") {
                    op("Yes, I'll give it a go!") {
                        player(HeadE.CALM_TALK, "I had better use a rope.")

                    }
                    op("No thanks, I'm having second thoughts.") {
                        simple("You think better of attempting to squeeze into the fissure.")
                        exec {player.forceTalk("It looks very dangerous, and dark. Scary!")}
                    }
                }
            }
        }
        if (options == "Look-at") {
            player.startConversation {
                simple("It looks as if something is buried here.")
            }
        }
    }

    onObjectClick("Foliage") { (player, obj, options) ->
        if (options == "Search") {
            player.startConversation {
                simple("You search the folliage...")
                exec{
                    player.sendMessage("transforming varbit")
                    player.anim(Seq.seq_827)
                    WorldTasks.schedule(2) {
                        player.vars.setVarBit(Varbit.varbit_4882, 1)
                    }
                }
                simple("...and reveal an ancient doorway set into the side of the hill!")
            }
        }
    }

    onObjectClick(Loc.zombiequeengateclosedl, Loc.zombiequeengateclosedr) { (player, obj, options) ->
        if (options == "Search") {
            player.startConversation {
                simple("There is an ancient symbol on the gate. It looks like a human figure with something around its neck. It looks pretty scary.")
            }
        }else{
            if (player.equipment.neckId == Obj.zqdeadbeads) {
                player.sendMessage("The Beads of the Dead start to glow...")
                Doors.handleDoubleDoor(player, obj)
            }
        }
    }

    onObjectClick("Ancient Door") { (player, obj, options) ->
        if (options == "Enter") {
            player.startConversation {
                player.vars.setVarBit(Varbit.varbit_4882, 0)
            }
        }
        if (options == "Open") {
            player.startConversation {
                simple("There seems to be some sort of recepticle on the door. Perhaps it needs a key?")
            }
        }
        if (options == "Search") {
            player.startConversation {
                simple("Examining the door, you see that it has a very strange lock. You're shocked to find that it seems to be made out of bone!")
                exec{
                    player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_TRIED_LOCK", true)
                }
            }
        }
        if (options == "Examine") {
            player.startConversation {
                simple("There seems to be some sort of recepticle on the door. Perhaps it needs a key?")
            }
        }
    }

    onObjectClick(Loc.ahzarhoon_entrance_fissure_withrope) { (player, obj, options) ->
        if (options == "Look-at") {
            player.startConversation {
                simple("You see a small fissure in the granite that you might just be able to crawl through. You can see that a rope is attached nearby.")
            }
        }
        if (options == "Search") {
            player.startConversation {
                simple("You see a small fisure in the granite that you might just be able to crawl through. You can see that a rope is attached nearby. would you like to climb down?.")
                options("CLIMB INTO THE FISURE?") {
                    op("Yes, I'll give it a go!") {
                        exec {
                            player.anim(Seq.seq_827)
                            WorldTasks.schedule(1) {
                                player.tele(2898, 9401, 0)
                                player.faceDir(Direction.SOUTH)
                            }
                            if(!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_FOUND_AH_ZA_RHOON")) {
                                player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_FOUND_AH_ZA_RHOON", true)
                                player.setQuestStage(Quest.SHILO_VILLAGE, 3)
                            }
                        }
                    }
                    op("No thanks, I'm having second thoughts.") {
                        simple("You think better of attempting to squeeze into the fissure."){
                            exec {player.forceTalk("It looks very dangerous, and dark. Scary!")}
                        }
                    }
                }
            }
        }
    }

    onObjectClick(Loc.zqsecretstone) { (player, obj, options) ->
        if (options == "Look-at") {
            player.startConversation {
                simple("This stone seems to have strange markings on it.")
            }
        }
        if (options == "Investigate") {
            player.startConversation {
                simple("This stone seems to have strange markings on it. Maybe Trufitus can decipher them? THe stone is too heavy to carry but the letters stand proud on a plaque. Maybe you could seperate the plaque from the rock?")
            }
        }
    }

    onObjectClick(Loc.zqwaterfallrocks) { (player, obj,options) ->
        if (options == "Look-at") {
            player.startConversation {
                simple("There is a huge cascading waterfall in front of you, the roar of the water is quite loud. To the east you can see what looks like a trecherous pathway, it might lead outside.")
            }
        }
        if (options == "Search") {
            player.startConversation {
                simple("You see a huge waterfall blocking your path. The path leads on through the cascading waterfall, but it looks quite dangerous. Would you like to try and follow the path?")
                options("SELECT AN OPTION"){
                    op("Yes, I'll follow the path."){
                        exec {
                            player.sendMessage("You start carefully edging along the slippery path...")
                            player.lock(6)
                            WorldTasks.schedule(2) {
                                player.anim(Seq.seq_827)
                            }
                            WorldTasks.schedule(4) {
                                player.tele(2923, 2997, 0)
                            }
                        }
                    }
                    op("No, I'll look for another exit."){
                        simple("You decide to have another look around. And see if you can find a better way to get out.")
                    }
                }
            }
        }
    }

    onObjectClick(Loc.secretrubblebook) { (player) ->
        if(player.inventory.containsItem(Obj.zqberviriusscroll) || player.bank.containsItem(Obj.zqberviriusscroll)){
            player.sendMessage("You have already searched the rocks and retrieved a tattered scroll.")
            return@onObjectClick
        }
        player.startConversation {
            simple("You see that there is something hidden behind some rocks. Do you want to have a look? it looks a bit dangerous as the ceiling doesn't look at all safe")
            options("SELECT AN OPTION") {
                op("Yes, I'll carefully move the rocks to see what's behind them."){
                    simple("You start to slowly move the rocks to one side")
                    item(Obj.zqberviriusscroll,"You carefully manage to remove enough rocks to see a book shelf. You gingerly remove a delicate scroll from the shelf and place it in your inventory"){
                        exec {
                            player.anim(Seq.seq_827)
                            if (player.inventory.hasFreeSlots()) {
                                player.sendMessage("You add the tattered scroll to your inventory.")
                                player.inventory.addItem(Obj.zqberviriusscroll,1,false)
                            } else {
                                player.sendMessage("You do not have enough room in your inventory.")
                            }
                        }
                    }
                }
                op("No, I'll leave them alone. I don't like the look of that ceiling.") {
                    simple("You decide to leave the rocks well alone. The ceiling does look a little unsafe.")
                }
            }
        }
    }

    onObjectClick(Loc.hillsideexitclosedr) { (player, obj, options) ->

        if (options == "Open") {
            player.sendMessage("The door seems to be locked!")
            WorldTasks.schedule(1) {
                player.forceTalk("Oh no, I'm going to be stuck in here forever!")
            }
            WorldTasks.schedule(3) {
                player.forceTalk("How will I ever get out!")
            }
            WorldTasks.schedule(5) {
                player.forceTalk("I'm too young to die!")
            }
        }
        if (options == "Search") {
            player.startConversation {
                simple("You can see a small recepticle, not unlike the one on the opposite side of the door!")
            }
        }

    }

    onObjectClick(Loc.zqsacks) { (player) ->
        if(player.inventory.containsItem(Obj.zqrashiliyiascroll) || player.bank.containsItem(Obj.zqrashiliyiascroll)){
            player.sendMessage("You have already searched the sacks and retrieved a crumpled scroll.")
            return@onObjectClick
        }
        player.itemDialogue(Obj.zqrashiliyiascroll,"You find a tattered but very ornate scroll, which you place carefully in your inventory.")
        player.anim(Seq.seq_827)
        if (player.inventory.hasFreeSlots()) {
            player.sendMessage("You add the crumpled scroll to your inventory.")
            player.inventory.addItem(Obj.zqrashiliyiascroll, 1, false)
        } else {
            player.sendMessage("You do not have enough room in your inventory.")
        }
    }

    onObjectClick(Loc.zqgallows) { ( player, obj, options) ->
        if(player.inventory.containsItem(Obj.zqzadimusbones) || player.bank.containsItem(Obj.zqzadimusbones)){
            player.sendMessage("You have already searched the gallows and retrieved the corpse.")
            return@onObjectClick
        }
        if (options == "Look-at") {
            player.startConversation {
                simple("You take a look at th gallows. It's pretty eerie looking.")
                simple("A grisly sight meets your eyes. A human corpse hangs from the noose. His hands have been tied behind his back.")
            }
        }
        if (options == "Search") {
            player.startConversation {
                simple("You find a human corpse hanging in the noose. It looks as if the corpse can be removed easily. Would you like to remove the corpse from the noose?")
                options("SELECT AN OPTION") {
                    op("I don't think so, it might animate and attack me!") {
                        simple("You move away from the corpse quietly and slowly... ...you have an eerie feeling about this!"){
                            exec{
                                player.forceTalk("**GULP!**")
                            }
                        }
                    }
                    op("Yes, I may find something else on the corpse.") {

                        item(Obj.zqzadimusbones, "You gently support the frame of the skeleton and lift the skull through the noose."){
                            exec{
                                player.sendMessage("You search the gallows...")
                            }
                        }
                        item(
                            Obj.zqzadimusbones,
                            "You find an old sack and place the skeleton in it. Maybe Trufitus can give you some tips on what to do with it."
                        ) {
                            exec {
                                player.anim(Seq.seq_827)
                                if (player.inventory.hasFreeSlots()) {
                                    player.sendMessage("You add the corpse to your inventory.")
                                    player.inventory.addItem(Obj.zqzadimusbones, 1, false)
                                    if(!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_COLLECTED_Z_CORPSE")) {
                                        if (player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_CRUMPLED_SCROLL")) {
                                            if (player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_READ_TATTERED_SCROLL")) {
                                                player.setQuestStage(Quest.SHILO_VILLAGE, 4)
                                            }
                                        }
                                    }
                                    player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_COLLECTED_Z_CORPSE", true)
                                } else {
                                    player.sendMessage("You do not have enough room in your inventory.")
                                }
                            }
                        }
                        simple("You sense that there is a spirit that needs to be put to rest.")
                    }
                }
            }
        }
    }

    onObjectClick(Loc.secretrubble) { (player) ->
        player.startConversation {
            simple("You see that there is a narrow gap through into the darkness. You could try to wriggle through and see where it takes you")
            options("WRIGGLE THROUGH THE RUBBLE?") {
                op("Yes, I'll wriggle through.") {
                    exec {
                        player.anim(Seq.seq_827)
                        WorldTasks.schedule(1) {
                            val dungtile = Tile.of(2889, 9285, 0)
                            if(player.tile.withinDistance(dungtile, 3)) {
                                player.tele(2888, 9373, 0)
                                player.faceDir(Direction.NORTH)
                            }else{
                                player.tele(2889, 9285, 0)
                                player.faceDir(Direction.NORTH)
                            }
                        }
                    }
                }
                op("No, I'll stay here.") {}
            }
        }
    }

    onObjectClick(Loc.ahzarhoon_entrance) { (player, obj, options) ->
        if(player.getQuestStage(Quest.SHILO_VILLAGE) >= FIND_TEMPLE_OF_AH_ZA_RHOON) {
            if (options == "Search") {
                player.startConversation {
                    simple("It looks as if something is buried here. You may need some tools to excavate further.")
                    options("WHAT DO YOU WANT TO DO WITH THE MOUND?") {
                        op("Try and excavate the mound.") {
                            exec {
                                if (player.getInventory().containsItem(Obj.spade, 1)) {
                                    player.lock(15)
                                    WorldTasks.schedule(1) {
                                        player.anim(Seq.seq_830)
                                    }
                                    WorldTasks.schedule(3) {
                                        player.anim(Seq.seq_830)
                                    }
                                    WorldTasks.schedule(5) {
                                        player.anim(Seq.seq_830)
                                    }
                                    WorldTasks.schedule(7) {
                                        player.anim(Seq.seq_830)
                                    }
                                    WorldTasks.schedule(9) {
                                        player.anim(Seq.seq_830)
                                    }
                                    WorldTasks.schedule(11) {
                                        player.anim(Seq.seq_830)
                                    }
                                    WorldTasks.schedule(13) {
                                        player.anim(Seq.seq_830)
                                    }
                                    WorldTasks.schedule(14) {
                                        obj.setIdTemporary(Loc.ahzarhoon_entrance_fissure, 3600)
                                    }
                                }else{
                                    player.sendMessage("You need to have a spade with you to excavate this.")
                                }
                            }
                        }
                        op("Leave the mound alone.") {}
                    }
                }
            }
            if (options == "Look-at") {
                player.startConversation {
                    simple("It looks as if something is buried here.")
                }
            }
        }
    }

    onObjectClick(Loc.zqrocks) { (player, obj, options) ->
        if(player.getQuestStage(Quest.SHILO_VILLAGE) >= INVESTIGATE_B_TOMB) {
            if (options == "Search") {
                player.startConversation {
                    simple("You investigate the rocks and find a dank, narrow crawl-way. Do you want to crawl into this dank, dark, narrow, possibly dangerous hole?<br>Crawl into hole?")
                    options("CRAWL INTO HOLE?") {
                        op("Yes please, I can think of nothing nicer!") {
                            exec {
                                player.lock(3)
                                WorldTasks.schedule(1) {
                                    player.anim(Seq.seq_827)
                                }
                                WorldTasks.schedule(2) {
                                    player.tele(2763, 9375, 0)
                                }
                                player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_FOUND_BERVIRIUS_TOMB", true)
                            }
                        }
                        op("No way could you get me to go in there!") {}
                    }
                }
            }
            if (options == "Investigate") {
                player.startConversation {
                    simple("These rocks look like they have been stacked uniformly.")
                }
            }
        }
    }

    onObjectClick(Loc.zq_rashrocks, checkDistance = true) { (player, obj, options) ->
        player.lock(3)
        player.anim(Seq.seq_827)
        WorldTasks.schedule(1) {
            val dungtile2 = Tile.of(2929, 9516, 0)
            if (player.tile.withinDistance(dungtile2, 3)) {
                player.tele(2928, 9511, 0)
                player.faceDir(Direction.SOUTH)
            } else {
                player.tele(2929, 9516, 0)
                player.faceDir(
                    Direction.NORTH
                )
            }
        }
    }

    onObjectClick(Loc.zqhandholds) { (player, obj, options) ->
        if (options == "Climb") {
            player.startConversation {
                simple("You attempt to climb the granite rock.")
                simple("You manage to climb back out again!") {
                    exec {
                        player.lock(3)
                        WorldTasks.schedule(1) {
                            player.anim(Seq.seq_827)
                        }
                        WorldTasks.schedule(2) {
                            player.tele(2762, 2989, 0)
                        }
                    }
                }
            }
        }
    }

    onObjectClick(Loc.thzq_tombrooml1, Loc.thzq_tombroomr1, Loc.thzq_tombrooml2, Loc.thzq_tombrooml3, Loc.thzq_tombroomr2) { (player, obj, options) ->
        if (options == "Search") {
            player.startConversation {
                simple("The door is ornately carved with depictions of skeletal warriors. You notice that some of the skeletal warriors depictions are not complete. Instead, there are recesses where some of the bones should be. There are three recesses.")
            }
        }
        if (options == "Open") {
            player.startConversation {
                if(!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_USED_THREE_BONES")) {
                    simple("The door won't budge.")
                }else{
                    Doors.handleDoubleDoor(player, World.getObject(Tile.of(2893,9480,0)))
                }
            }
        }
    }

    onObjectClick(Loc.zqrashdolmen) { (player, obj, options) ->

        if (!isBossUp) {
            if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_KILLED_BOSS")) {
                player.startConversation {
                    simple("You touch the dolmen, and the ground starts to shake. You hear an unearthly voice booming and you step away from the dolmen in anticipation.")
                    exec {
                        spawnBoss(player)
                    }
                    return@startConversation
                }
            } else {
                if (!player.inventory.containsItem(Obj.zqcorpse) && !player.bank.containsItem(Obj.zqcorpse)) {
                    val chunk = ChunkManager.getChunk(player.getChunkId())
                    val groundItemsMap = chunk.getGroundItems()
                    var foundCorpse = false
                    for ((_, tileMap) in groundItemsMap) {
                        for ((_, groundItemList) in tileMap) {
                            val iterator = groundItemList.iterator()
                            while (iterator.hasNext()) {
                                val groundItem = iterator.next()
                                if (groundItem.id == Obj.zqcorpse && groundItem.tile == Tile.of(2892,9487,0) && groundItem.ownerUsername == player.username) {
                                    val e1 = PickupItemEvent(player, groundItem, false)
                                    PluginManager.handle(e1)
                                    foundCorpse = true;
                                    if (!e1.isCancelPickup) {
                                        player.startConversation {
                                            if (options == "Search") {
                                                simple("You search the dolmen... And find the mumified remains of a human female.")
                                            }
                                            exec{
                                                player.soundEffect(Sound.sound_2582, false)
                                                removeGroundItem(player, groundItem, true)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if(!foundCorpse){
                        player.sendMessage("You see something appear on the dolmen.")
                        addGroundItem(Item(Obj.zqcorpse), Tile.of(2892, 9487, 0), player)
                    }
                }
            }
        }else {
            player.sendMessage("Tha dolmen remains silent.")
        }

    }

    onObjectClick(Loc.zqdolmen) { (player, obj, options) ->

        val hasPommel = player.inventory.containsItem(Obj.zqbevsword) || player.bank.containsItem(Obj.zqbevsword)
        val hasSphere =
            player.inventory.containsItem(Obj.zqcrystal) || player.bank.containsItem(Obj.zqcrystal)
        val hasNotes = player.inventory.containsItem(Obj.zqberviriusscroll2) || player.bank.containsItem(Obj.zqberviriusscroll2)
        val missingItems = mutableListOf<Pair<Int, Int>>()

        if (!hasPommel) missingItems.add(Obj.zqbevsword to 1)
        if (!hasSphere) missingItems.add(Obj.zqcrystal to 1)
        if (!hasNotes) missingItems.add(Obj.zqberviriusscroll2 to 1)

        val itemCount = missingItems.size

        if (options == "Inspect") {
            when (player.getQuestStage(Quest.SHILO_VILLAGE)) {
                DEAL_WITH_RASHILIYIAS_CORPSE -> {
                    player.sendMessage("There is nothing new about this dolmen.")
                }
                INVESTIGATE_B_TOMB -> {
                    player.startConversation {
                        if (itemCount == 0) {
                            simple("You already have all of the items from the dolmen.")
                            return@startConversation
                        }
                        simple("The dolmen is intricately decorated with the symbol of two crossed palm trees. It might be the family crest? You can see that there ${if (itemCount > 1) "are some items" else "is an item"} on the dolmen.")
                    }
                }
            }
        }

        if (options == "Search") {
            when (player.getQuestStage(Quest.SHILO_VILLAGE)) {
                DEAL_WITH_RASHILIYIAS_CORPSE -> {
                    player.sendMessage("There is nothing new about this dolmen.")
                }
                INVESTIGATE_B_TOMB -> {
                    player.startConversation {
                        if (itemCount == 0) {
                            simple("You already have all of the items from the dolmen.")
                            return@startConversation
                        }
                        simple("The dolmen is intricately decorated with the symbol of two crossed palm trees. It might be the family crest? You can see that there ${if (itemCount > 1) "are some items" else "is an item"} on the dolmen.")
                        if (player.inventory.freeSlots >= itemCount) {
                            for ((id, amount) in missingItems) {
                                if (id == Obj.zqbevsword) {
                                    item(
                                        id,
                                        "You find a rusty sword with an ivory pommel. You take the Ivory pommel and place it in your inventory."
                                    )
                                }
                                if (id == Obj.zqcrystal) {
                                    item(
                                        id,
                                        "You find a Crystal Sphere. You take the crystal and place it carefully in your inventory."
                                    )
                                }
                                if (id == Obj.zqberviriusscroll2) {
                                    item(
                                        id,
                                        "You find some writing on the dolmen, you grab some nearby scraps of delicate paper together and copy the text as best you can and collect them together as a scroll."
                                    )
                                }
                                player.inventory.addItem(id, amount)
                            }
                            player.setQuestStage(Quest.SHILO_VILLAGE, RETURN_TO_TRUFITUS_AGAIN)
                        } else {
                            player.sendMessage("You need $itemCount free inventory slot${if (itemCount > 1) "s" else ""} to collect the item${if (itemCount > 1) "s" else ""} from the dolmen.")
                        }
                    }
                }
            }
        }
    }

    onNpcClick(Npc.shilocartdriver){ (player, npc) ->
        if(player.questManager.isComplete(Quest.SHILO_VILLAGE)){
            player.startConversation {
                npc(npc,HeadE.CALM_TALK,"I am offering a cart ride to Brimhaven if you're interested. It will cost 10 gold coins...")
                options("Select an option") {
                    op("Yes please. I'd like to go to Brimhaven."){
                        if (player.getInventory().hasCoins(10)) {
                            player.getInventory().removeCoins(10)
                            simple("You hop into the cart and the driver urges The horses on. You take a taxing journey through the jungle to Brimhaven.")
                            exec{player.fadeScreen(Runnable {player.tele(Tile.of(2779, 3213, 0)) })}
                            simple("You feel tired from the journey, but at least you didn't have to walk all that distance.")
                        } else player.sendMessage("You don't have enough coins for the ride.")
                    }
                    op("No, thanks."){
                        player(HeadE.CALM_TALK,"No, thanks.")
                    }
                }
            }
        }else{
            player.sendMessage("He seems like he doesn't like strangers.")
        }
    }

    onNpcClick(Npc.brimhavencartdriver){ (player, npc) ->
        if(player.questManager.isComplete(Quest.SHILO_VILLAGE)){
            player.startConversation {
                npc(npc,HeadE.CALM_TALK,"I am offering a cart ride to Shilo Village if you're interested. It will cost 10 gold coins...")
                options("Select an option") {
                    op("Yes please. I'd like to go to Shilo Village."){
                        if (player.getInventory().hasCoins(10)) {
                            player.getInventory().removeCoins(10)
                            simple("You hop into the cart and the driver urges The horses on. You take a taxing journey through the jungle to Shilo Village.")
                            exec{player.fadeScreen(Runnable {player.tele(Tile.of(2831, 2955, 0)) })}
                            simple("You feel tired from the journey, but at least you didn't have to walk all that distance.")
                        } else player.sendMessage("You don't have enough coins for the ride.")
                    }
                    op("No, thanks."){
                        player(HeadE.CALM_TALK,"No, thanks.")
                    }
                }
            }
        }else{
            player.sendMessage("He seems like he doesn't like strangers.")
        }
    }

    onObjectClick(Loc.shilocart){(player, obj, options) ->
        if(player.questManager.isComplete(Quest.SHILO_VILLAGE)) {
            if (player.getInventory().hasCoins(10)) {
                player.getInventory().removeCoins(10)
                player.startConversation {
                    simple("You hop into the cart and the driver urges The horses on. You take a taxing journey through the jungle to Brimhaven.")
                    exec { player.fadeScreen(Runnable { player.tele(Tile.of(2779, 3213, 0)) }) }
                    simple("You feel tired from the journey, but at least you didn't have to walk all that distance.")
                }
            } else player.sendMessage("You don't have enough coins for the ride.")
        }else player.sendMessage("The driver doesn't like strangers.")
        return@onObjectClick
    }

    onObjectClick(Loc.brimhavencart) { (player, obj, options) ->
        if (player.questManager.isComplete(Quest.SHILO_VILLAGE)) {
            if (player.getInventory().hasCoins(10)) {
                player.getInventory().removeCoins(10)
                player.startConversation {
                    simple("You hop into the cart and the driver urges The horses on. You take a taxing journey through the jungle to Shilo Village.")
                    exec { player.fadeScreen(Runnable { player.tele(Tile.of(2831, 2955, 0)) }) }
                    simple("You feel tired from the journey, but at least you didn't have to walk all that distance.")
                }
            } else player.sendMessage("You don't have enough coins for the ride.")
        } else player.sendMessage("The driver doesn't like strangers.")
        return@onObjectClick
    }

    onObjectClick(Loc.shilo_brokencart) { e ->
        if (e.player.questManager.isComplete(Quest.SHILO_VILLAGE)) {
            e.player.startConversation {
                simple("You approach the cart and see undead creatures gathering by the village gates. There is a note attached to the cart. The note says, :: Danger :: Deadly green mist DO NOT ENTER IF YOU VALUE YOUR LIFE !")
                options(""){
                    op("Yes, I am very nimble and agile!"){
                        exec{
                            e.player.sendMessage("You nimbly jump from one side of the cart...")
                            e.player.ladder(
                                if (e.player.x > e.obj.x) e.player.transform(-4, 0, 0)
                                else e.player.transform(4, 0, 0)
                            )
                            WorldTasks.schedule(2) {
                                e.player.sendMessage("...to the other and climb down again.")
                            }
                        }
                    }
                    op("No, I am happy where I am thanks!"){
                        exec{
                            e.player.sendMessage("You think better of clambering over the cart, you might get dirty.")
                            WorldTasks.schedule(2) {
                                e.player.forceTalk("I'd probably have just scraped my knees up as well.")
                            }
                        }
                    }
                }
            }

        }else{
            e.player.startConversation {
                simple("The village looks like it isn't safe to enter.")
            }
        }
        return@onObjectClick
    }
}

private val SCROLL_LINES = intArrayOf(
    Component.interface_174.component_174_1, Component.interface_174.component_174_2, Component.interface_174.component_174_3,
    Component.interface_174.component_174_4, Component.interface_174.component_174_5, Component.interface_174.component_174_6,
    Component.interface_174.component_174_7, Component.interface_174.component_174_8, Component.interface_174.component_174_9,
    Component.interface_174.component_174_10, Component.interface_174.component_174_11, Component.interface_174.component_174_12,
    Component.interface_174.component_174_13, Component.interface_174.component_174_14, Component.interface_174.component_174_15,
    Component.interface_174.component_174_16
)

private fun Player.sendShiloScroll(vararg text: String) {
    interfaceManager.sendInterface(Interface.interface_174)
    SCROLL_LINES.forEachIndexed { index, component -> packets.setIFText(component, text.getOrElse(index) { "" }) }
}
