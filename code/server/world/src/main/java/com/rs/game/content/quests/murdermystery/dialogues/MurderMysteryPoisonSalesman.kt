package com.rs.game.content.quests.murdermystery

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

const val MM_POISON_BUYERS_KNOWN = "murderMysteryPoisonBuyersKnown"

@ServerStartupEvent
fun mapMurderMysteryPoisonSalesman() {
    onNpcClick(Npc.poison_salesman) { (player) ->
        if (player.getQuestStage(Quest.MURDER_MYSTERY) == STAGE_UNSTARTED ||
            player.getQuestStage(Quest.MURDER_MYSTERY) == STAGE_COMPLETE
        ) {
            return@onNpcClick
        }
        player.startConversation {
            poisonSalesmanDialogue(player)
        }
    }
}

private fun DialogueBuilder.poisonSalesmanDialogue(player: Player) {
    player(CALM_TALK, "I'm investigating the murder at the Sinclair house.")
    npc(Npc.poison_salesman, AMAZED, "There was a murder at the Sinclair House??? That's terrible! And I was only there the other day too! They bought the last of my Patented Multi Purpose Poison!"
    )
    label("poisonOptions")
    options {
        op("Patented Multi Purpose Poison?") {
            player(CALM_TALK, "Patented Multi Purpose Poison?")
            npc(Npc.poison_salesman, HAPPY_TALKING, "Aaaaah... a miracle of modern apothecaries!")
            npc(Npc.poison_salesman, HAPPY_TALKING, "This exclusive concoction has been tested on all known forms of life and been proven to kill them all in varying dilutions from cockroaches to king dragons!"
            )
            npc(Npc.poison_salesman, HAPPY_TALKING, "So incredibly versatile, it can be used as pest control, a cleansing agent, drain cleaner, metal polish and washes whiter than white,"
            )
            npc(Npc.poison_salesman, HAPPY_TALKING, "all with our uniquely fragrant concoction that is immediately recognisable across the land as Peter Potter's Patented Poison potion!!!"
            )
            simple("The salesman stops for breath.")
            npc(Npc.poison_salesman, HAPPY_TALKING, "I'd love to sell you some but I've sold out recently. That's just how good it is! Three hundred and twenty eight people in this area alone cannot be wrong!"
            )
            npc(Npc.poison_salesman, HAPPY_TALKING, "Nine out of Ten poisoners prefer it in controlled tests!")
            npc(Npc.poison_salesman, CALM_TALK, "Can I help you with anything else?")
            npc(Npc.poison_salesman, CALM_TALK, "Perhaps I can take your name and add it to our mailing list of poison users? We will only send you information related to the use of poison and other Peter Potter Products!"
            )
            player(CALM_TALK, "Uh... no, it's ok. Really.")
        }
        op("Who did you sell Poison to at the house?") {
            player(CALM_TALK, "Who did you sell Poison to at the house?")
            npc(Npc.poison_salesman, CALM_TALK, "Well, Peter Potter's Patented Multi Purpose Poison is a product of such obvious quality that I am glad to say I managed to sell a bottle to each of the Sinclairs!"
            )
            npc(Npc.poison_salesman, CALM_TALK, "Anna, Bob, Carol, David, Elizabeth and Frank all bought a bottle! In fact they bought the last of my supplies!"
            )
            npc(Npc.poison_salesman, CALM_TALK, "Maybe I can take your name and address and I will personally come and visit you when stocks return?"
            )
            player(CALM_TALK, "Uh... no, it's ok.") {
                player.set(MM_POISON_BUYERS_KNOWN, true)
                if (player.getQuestStage(Quest.MURDER_MYSTERY) < STAGE_VISITING_POISON_SALESMAN)
                    player.setQuestStage(Quest.MURDER_MYSTERY, STAGE_VISITING_POISON_SALESMAN)
            }
        }
        op("Can I buy some Poison?") {
            player(CALM_TALK, "Can I buy some Poison?")
            npc(Npc.poison_salesman, CALM_TALK, "I'm afraid I am totally out of stock at the moment after my successful trip to the Sinclairs' House the other day."
            )
            npc(Npc.poison_salesman, HAPPY_TALKING, "But don't worry! Our factories are working overtime to produce Peter Potter's Patented Multi Purpose Poison!"
            )
            npc(Npc.poison_salesman, HAPPY_TALKING, "Possibly the finest multi purpose poison and cleaner yet available to the general market."
            )
            npc(Npc.poison_salesman, HAPPY_TALKING, "And its unique fragrance makes it the number one choice for cleaners and exterminators the whole country over!"
            )
        }
        if (player.inventory.containsItem(Obj.murderpot2, 1)) {
            op("I have this pot I found at the murder scene...") {
                player(CALM_TALK, "I have this pot I found at the murder scene...")
                simple("You show the poison salesman the pot you found at the murder scene with the unusual smell.")
                npc(Npc.poison_salesman, CALM_TALK, "Hmmm... yes, that smells exactly like my Patented Multi Purpose Poison, but I don't see how it could be. It quite clearly says on the label of all bottles"
                )
                npc(Npc.poison_salesman, CALM_TALK, "'Not to be taken internally - EXTREMELY POISONOUS'.")
                player(CALM_TALK, "Perhaps someone else put it in his wine?")
                npc(Npc.poison_salesman, CALM_TALK, "Yes... I suppose that could have happened...")
            }
        }
    }
}
