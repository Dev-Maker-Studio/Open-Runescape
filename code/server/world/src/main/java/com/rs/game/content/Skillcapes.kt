package com.rs.game.content

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.game.model.entity.player.Bank
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

enum class Skillcapes(
    val untrimmed: Int,
    val trimmed: Int,
    val hood: Int,
    val master: Int,
    val verb: String
) {
    Attack(Obj.skillcape_attack, Obj.skillcape_attack_trimmed, Obj.skillcape_attack_hood, Obj.obj_25324, "a master in the fine art of attacking"),
    Defence(Obj.skillcape_defence, Obj.skillcape_defence_trimmed, Obj.skillcape_defence_hood, Obj.obj_25326, "a master in the fine art of defence"),
    Strength(Obj.skillcape_strength, Obj.skillcape_strength_trimmed, Obj.skillcape_strength_hood, Obj.obj_25325, "as strong as is possible"),
    Constitution(Obj.skillcape_hitpoints, Obj.skillcape_hitpoints_trimmed, Obj.skillcape_hitpoints_hood, Obj.obj_25332, "as resilient as is possible"),
    Ranging(Obj.obj_9756, Obj.obj_9757, Obj.obj_9758, Obj.obj_25327, "a master in the fine art of ranging"),
    Prayer(Obj.skillcape_prayer, Obj.skillcape_prayer_trimmed, Obj.skillcape_prayer_hood, Obj.obj_25328, "as devoted to prayer as possible"),
    Magic(Obj.skillcape_magic, Obj.skillcape_magic_trimmed, Obj.skillcape_magic_hood, Obj.obj_25329, "as powerful a wizard as I"),
    Cooking(Obj.skillcape_cooking, Obj.skillcape_cooking_trimmed, Obj.skillcape_cooking_hood, Obj.obj_25344, "a master in the culinary arts"),
    Woodcutting(Obj.skillcape_woodcutting, Obj.obj_9808, Obj.skillcape_woodcutting_hood, Obj.obj_25346, "a master woodsman"),
    Fletching(Obj.skillcape_fletching, Obj.skillcape_fletching_trimmed, Obj.skillcape_fletching_hood, Obj.obj_25337, "a master in the fine art of fletching"),
    Fishing(Obj.skillcape_fishing, Obj.skillcape_fishing_trimmed, Obj.skillcape_fishing_hood, Obj.obj_25343, "a master fisherman"),
    Firemaking(Obj.skillcape_firemaking, Obj.skillcape_firemaking_trimmed, Obj.skillcape_firemaking_hood, Obj.obj_25345, "a master of fire"),
    Crafting(Obj.skillcape_crafting, Obj.skillcape_crafting_trimmed, Obj.skillcape_crafting_hood, Obj.obj_25336, "a master craftsman"),
    Smithing(Obj.skillcape_smithing, Obj.skillcape_smithing_trimmed, Obj.skillcape_smithing_hood, Obj.obj_25342, "a master blacksmith"),
    Mining(Obj.skillcape_mining, Obj.skillcape_mining_trimmed, Obj.skillcape_mining_hood, Obj.obj_25341, "a master miner"),
    Herblore(Obj.skillcape_herblore, Obj.skillcape_herblore_trimmed, Obj.skillcape_herblore_hood, Obj.obj_25334, "a master herbalist"),
    Agility(Obj.skillcape_agility, Obj.skillcape_agility_trimmed, Obj.skillcape_agility_hood, Obj.obj_25333, "as agile as possible"),
    Thieving(Obj.skillcape_thieving, Obj.skillcape_thieving_trimmed, Obj.skillcape_thieving_hood, Obj.obj_25335, "a master thief"),
    Slayer(Obj.skillcape_slayer, Obj.skillcape_slayer_trimmed, Obj.skillcape_slayer_hood, Obj.obj_25338, "an incredible student"),
    Farming(Obj.skillcape_farming, Obj.skillcape_farming_trimmed, Obj.skillcape_farming_hood, Obj.obj_25347, "a master farmer"),
    Runecrafting(Obj.obj_9765, Obj.obj_9766, Obj.skillcape_runecrafting_hood, Obj.obj_25330, "a master runecrafter"),
    Hunter(Obj.skillcape_hunting, Obj.skillcape_hunting_trimmed, Obj.skillcape_hunting_hood, Obj.obj_25339, "a master hunter"),
    Construction(Obj.obj_9789, Obj.obj_9790, Obj.obj_9791, Obj.obj_25331, "a master home builder"),
    Summoning(Obj.skillcape_summoning, Obj.skillcape_summoning_trimmed, Obj.skillcape_summoning_hood, Obj.obj_25348, "a master summoner"),
    Dungeoneering(Obj.skillcape_dungeoneering, Obj.skillcape_dungeoneering_trimmed, Obj.skillcape_dungeoneering_hood, Obj.skillcape_120_dungeoneering, "a master dungeon delver");

    private fun getGiveCapeDialogue(player: Player, npcId: Int, masterCape: Boolean, dialogue: DialogueBuilder) {
        if (!player.inventory.hasCoins(if (masterCape) 120000 else 99000)) {
                dialogue.player(SAD_MILD, "But, unfortunately, I was mistaken.")
                dialogue.npc(npcId, NO_EXPRESSION, "Well, come back and see me when you do.")
            return
        }
        player.startConversation {
            npc(npcId, CHEERFUL, if (ordinal == Constants.FIREMAKING) "I'm sure you'll look hot in that cape." else "Excellent! Wear that cape with pride my friend.") {
                player.inventory.removeCoins(if (masterCape) 120000 else 99000)
                if (!masterCape) player.inventory.addItem(hood, 1)
                player.inventory.addItem(if (masterCape) master else if (player.skills.checkMulti99s()) trimmed else untrimmed, 1)
            }
        }
    }

    fun getOffer99CapeDialogue(player: Player, npcId: Int) {
        player.startConversation {
            npc(npcId, CHEERFUL, "Ah, this is a Skillcape of ${name}. I have mastered the art of ${name.lowercase()} and wear it proudly to show others.")
            player(SKEPTICAL, "Hmm, interesting.")
            if (player.skills.getLevelForXp(ordinal) >= 99) {
                npc(npcId, NO_EXPRESSION, "Ah, but I see you are already $verb, perhaps you have come to me to purchase a Skillcape of $name, and thus join the elite few who have mastered this exacting skill?")
                options {
                    op("Yes, I'd like to buy one please.") {
                        npc(npcId, NO_EXPRESSION, "Most certainly; unfortunately being such a prestigious item, they are appropriately expensive. I'm afraid I must ask you for 99,000 gold.")
                        options {
                            op("99,000 coins? That's much too expensive.") {
                                player(HAPPY_TALKING, "99,000 coins? That's much too expensive.")
                                npc(npcId, CALM_TALK, "...")
                            }
                            op("I think I have the money right here, actually.") {
                                player(HAPPY_TALKING, "I think I have the money right here, actually.")
                                exec { getGiveCapeDialogue(player, npcId, false, this) }
                            }
                        }
                    }
                    if (player.skills.is120(this@Skillcapes.ordinal)) {
                        op("I've mastered this skill. Is there anything else?") {
                            npc(npcId, AMAZED, "I've been saving this master cape for someone truly $verb. Is that really you?")
                            player(CONFUSED, "I think so. I mean I really have mastered everything there is to know.")
                            npc(npcId, AMAZED, "I can see that! I would be glad to offer you this cape.")
                            options("Buy a master cape for 120,000 coins?") {
                                op("Yes, please.") {
                                    player(HAPPY_TALKING, "Yes, please. I think I have the money right here, actually.") { getGiveCapeDialogue(player, npcId, true, this) }
                                }
                                op("No, thanks.")
                            }
                        }
                    }
                    op("Nevermind.") {
                        npc(npcId, NO_EXPRESSION, "No problem; there are many other adventurers who would love the opportunity to purchase such a prestigious item! You can find me here if you change your mind.")
                    }
                }
            } else {
                options {
                    op("Please tell me more about skillcapes.") {
                        player(HAPPY_TALKING, "Please tell me more about skillcapes.")
                        npc(npcId, CALM_TALK, "Of course. Skillcapes are a symbol of achievement. Only people who have mastered a skill and reached level 99 can get their hands on them and gain the benefits they carry.")
                    }
                    op("Bye.") {
                        player(HAPPY_TALKING, "Bye.")
                        npc(npcId, CALM_TALK, "Bye.")
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun trimCapes(player: Player) {
            if (!player.skills.checkMulti99s()) return
            val trimmedOf = entries.associate { it.untrimmed to it.trimmed }
            trimmedOf[player.equipment.capeId]?.let {
                player.equipment.setSlot(Equipment.CAPE, Item(it, 1))
                player.appearance.generateAppearanceData()
            }
            val inventory = player.inventory
            for (slot in 0 until inventory.items.size) {
                val trimmed = trimmedOf[inventory.getItem(slot)?.id] ?: continue
                inventory.replaceItem(trimmed, 1, slot)
            }
            inventory.refresh()
            val bank = player.bank
            var refresh = false
            for (tab in bank.bankTabs) for (item in tab) {
                if (Bank.isPlaceholder(item)) continue
                item.id = trimmedOf[item.id] ?: continue
                refresh = true
            }
            if (refresh) bank.refreshItems()
        }
    }
}
