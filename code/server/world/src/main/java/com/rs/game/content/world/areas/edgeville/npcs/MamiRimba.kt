package com.rs.game.content.world.areas.edgeville.npcs

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.shop.ShopsHandler
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Varbit

const val MAMI_RIMBA = Npc.sniper_mami_rimba
const val MAMI_RIMBA_SHOP = "mami_rimbas_shop"

@ServerStartupEvent
fun handleMamiRimba() {
    onNpcClick(MAMI_RIMBA, options = arrayOf("Talk-to")) { e ->
        val player = e.player
        player.startConversation {
            npc(MAMI_RIMBA, HeadE.HAPPY_TALKING, "Eh heh heh... Ello there, my little friend. You be wanting to get your hands on a bakriminel bolt tip, no? These bolts that cripple mighty mages?")
            if (player.vars.getVarBit(Varbit.sniper_spoken_to_mami_rimba) == 0) {
                player.vars.saveVarBit(Varbit.sniper_spoken_to_mami_rimba, 1)
                npc(MAMI_RIMBA, HeadE.CALM_TALK, "Ah, but Mami Rimba has not seen you before, so perhaps she should tell you about these vicious little things.")
                npc(MAMI_RIMBA, HeadE.CALM_TALK, "Find the bloodwood trees in the dark and dangerous places, cut em down and fletch them into bolts.")
                npc(MAMI_RIMBA, HeadE.CALM_TALK, "Use these bolts to strike at other adventurers with a more magical leaning and watch as these bolts strip their power from them.")
                npc(MAMI_RIMBA, HeadE.CALM_TALK, "Target their head to weaken their magic, target their torso to cause them pain and target their legs to stop them running.")
                npc(MAMI_RIMBA, HeadE.HAPPY_TALKING, "Heh heh heh... Vicious little blighters be these bakriminel bolts.")
            }
            label("mami_rimba_options")
            options {
                op("I'd like to trade.") {
                    npc(MAMI_RIMBA, HeadE.HAPPY_TALKING, "Eh heh heh... Excellent choice, my friend! Let Mami Rimba show you what she has.")
                    exec { ShopsHandler.openShop(player, MAMI_RIMBA_SHOP) }
                }
                op("Who are you?") {
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "So, you not be knowing old Mami Rimba then.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Heh heh... You be sure you ain't heard the stories?")
                    player(HeadE.CONFUSED, "What stories?")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Mami Rimba, she be friend of the outcast Papa Mambo. Mami Rimba, she be practicing the forbidden arts.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Mami Rimba, she kill all her twenty children because them were making a noise at night.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Mami Rimba, she be a widow of many husbands and each of them died a horrible death.")
                    player(HeadE.SKEPTICAL_THINKING, "None of those are true, of course.")
                    npc(MAMI_RIMBA, HeadE.HAPPY_TALKING, "Eh heh heh heh heh.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "You being so sure of that?")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "But stories and rumours do not matter to Mami Rimba; she does not care for such things.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Mami Rimba, she only care about these trees and them strange magic.")
                    goto("mami_rimba_options")
                }
                op("What is that tree?") {
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Ah, this tree... This is a very special tree, my friend. This is a bloodwood tree; a tree born of violence and death, that feeds on the dark residue of departed souls.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "This tree, it is of darker magic than most wizards dare to know, and its wood is filled with that same sorcery.")
                    goto("mami_rimba_options")
                }
                op("How do I make the bolts?") {
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Ah, these bakriminel bolts yes. Cut a bloodwood tree, but do not stray far away from her, for the wood it die if you are not close by. Fletch the wood, if you be of great skill.")
                    if (player.skills.getLevel(Constants.FLETCHING) < 93) {
                        npc(MAMI_RIMBA, HeadE.CALM_TALK, "Of which you, I see, are not.")
                        simple("You need a Fletching skill of 93 before you can fletch bakriminel bolts.")
                    }
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Ah but here is the tricky part. You must be fletching these bolts at the tree herself, else the wood you cut from her will crumble in your hands.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "Only with enchanted bolt tips firmly attached to the bolt shafts, can you be hoping to take the bolts away from the tree.")
                    player(HeadE.CONFUSED, "How do I make them?")
                    npc(MAMI_RIMBA, HeadE.HAPPY_TALKING, "Eh heh heh. You do not, my friend. That secret be Mami Rimba's and hers alone.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "But do not be so sad - Mami Rimba will sell you the enchanted bolt tips she has made. Use these on the bolt shafts and their magic will be complete.")
                    player(HeadE.SKEPTICAL_THINKING, "So I have to fletch the logs next to the tree and then use your bolt tips on them to complete them? And I need to be by the tree the whole time?")
                    npc(MAMI_RIMBA, HeadE.HAPPY_TALKING, "Eh heh heh. Yes, my friend. That is what you must do.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "But remember - these trees grow in places of violence and bloodshed, and they grow back after much time has passed. Be ready to defend yourself.")
                    goto("mami_rimba_options")
                }
                op("What do these bolts do?") {
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "If you cut the bloodwood into shafts you can make cruel weapons that will do more than just injure your foe.")
                    npc(MAMI_RIMBA, HeadE.CALM_TALK, "These bolts will tear into their flesh - their very souls - and will steal all chance of victory from them... if them be mages, anyway. These trees - them really hate mages.")
                    npc(MAMI_RIMBA, HeadE.HAPPY_TALKING, "So, if they be wearing a mage's armour, they be in trouble. Heh heh heh.")
                    goto("mami_rimba_options")
                }
            }
        }
    }
}
