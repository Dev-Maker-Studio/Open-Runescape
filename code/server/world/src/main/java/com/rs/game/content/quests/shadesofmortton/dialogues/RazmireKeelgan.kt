package com.rs.game.content.quests.shadesofmortton

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.DIZZY
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.utils.shop.ShopsHandler
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

fun Player.talkMorttonRazmire(firstCure: Boolean = false, option: String = "Talk-to") {
    if (vars.getVarBit(Varbit.razmire_visible) == 0) {
        startConversation {
            npc(Npc.razmire_keelgan_afflicted, DIZZY, "mee's zurgle mee's wot's nots")
            simple("~ This person doesn't make any sense at all. ~")
        }
        return
    }
    val stage = getQuestStage(Quest.SHADES_OF_MORTTON)
    if (option != "Talk-to" && stage >= STAGE_VISIT_ULSQUIRE) {
        ShopsHandler.openShop(this, if (option == "Trade-General-Store") "razmire_general" else "razmire_builders")
        return
    }
    startConversation {
        if (firstCure) npc(Npc.razmire_keelgan, CALM_TALK, "Ah, excellent, you've made your own serum and it seems to work well! Many thanks for using the Serum on me! But beware, I may start to change back soon!")
        when {
            stage == STAGE_KILL_SHADES -> {
                if (option == "Talk-to") npc(Npc.razmire_keelgan, CALM_TALK, "Ah, it's you, how's it going?")
                npc(Npc.razmire_keelgan, CALM_TALK, if (option == "Talk-to") "Hey, have you killed the five shades yet?" else "Have you killed those Shades yet?")
                if (morttonState().getI("shadeKills") < 5) {
                    player(CALM_TALK, if (option == "Talk-to") "No not yet!" else "No, sorry, not yet.")
                    npc(Npc.razmire_keelgan, CALM_TALK, if (option == "Talk-to") "Come back then when you have. And remember to bring me the remains, Ulsquire seems to think we might be able to send these poor souls to a better place." else "Well, come back when you have.")
                } else if (!inventory.containsItem(Obj.shade_bones1, 5)) {
                    player(CALM_TALK, "Yes I have actually!")
                    npc(Npc.razmire_keelgan, CALM_TALK, "Hmm, yes, but you don't have the five shade remains I asked of you! Come back when you have them.")
                } else {
                    player(CALM_TALK, "Yes I have actually!") {
                        if (getQuestStage(Quest.SHADES_OF_MORTTON) == STAGE_KILL_SHADES && inventory.containsItem(Obj.shade_bones1, 5)) {
                            inventory.deleteItem(Obj.shade_bones1, 2)
                            setQuestStage(Quest.SHADES_OF_MORTTON, STAGE_VISIT_ULSQUIRE)
                        }
                    }
                    simple("Razmire takes two Shade remains.")
                    npc(Npc.razmire_keelgan, CALM_TALK, "Great, I'll experiment on these! I think that Ulsquire might be keen to see some shade remains, if you take some over to him, he can possibly give you some extra information about them.")
                    npc(Npc.razmire_keelgan, CALM_TALK, "Now that you've slayed some of those nasty shades, I feel motivated to help you out, I can trade with you now if you like.")
                    razmireStores(this@talkMorttonRazmire)
                }
            }
            stage >= STAGE_RETURN_TO_ULSQUIRE -> {
                npc(Npc.razmire_keelgan, CALM_TALK, "Ah, it's you, how's it going?")
                if (!getBool("morttonLairExplained")) {
                    player(CALM_TALK, "Hey, I've laid a Shade to rest, can you tell me where the Shade lair is now?")
                    npc(Npc.razmire_keelgan, CALM_TALK, if (stage == STAGE_COMPLETE) "Yeah, great job on putting that Shade to rest. The entrance to the Shade Lair is due North of my shop. Good hunting in the lair my friend!" else "Yeah, great job putting that Shade to rest. The entrance to the Shade Lair is due North of my shop. Good hunting in the lair my friend!") {
                        set("morttonLairExplained", true)
                    }
                }
                player(CALM_TALK, "Can you open a store for me?")
                npc(Npc.razmire_keelgan, CALM_TALK, "Sure, which store do you wanna see?")
                razmireStores(this@talkMorttonRazmire)
            }
            stage >= STAGE_REBUILD_TEMPLE && morttonState().getB("repairStarted") -> {
                npc(Npc.razmire_keelgan, CALM_TALK, "Ah, it's you, how's it going?")
                player(CALM_TALK, "Hello there, I've started repairing the temple.")
                npc(Npc.razmire_keelgan, CALM_TALK, "That's great, carry on the good work.")
                razmireRepair(this@talkMorttonRazmire)
            }
            stage >= STAGE_VISIT_ULSQUIRE -> {
                npc(Npc.razmire_keelgan, CALM_TALK, "Ah, it's you, how's it going?")
                npc(Npc.razmire_keelgan, CALM_TALK, "Hello again...what can I do for you now?")
                razmireStores(this@talkMorttonRazmire)
            }
            else -> {
                if (!firstCure) npc(Npc.razmire_keelgan, CALM_TALK, "Ah, it's you, how's it going?")
                razmireInitial(this@talkMorttonRazmire)
            }
        }
    }
}

private fun DialogueBuilder.razmireInitial(owner: Player) {
    label("razmireInitial")
    options {
        op("Who are you?") {
            player(CALM_TALK, "Who are you?")
            npc(Npc.razmire_keelgan, CALM_TALK, "My name is Razmire Keelgan, I run a couple of shops in Mort'ton but don't get many customers these days.")
            player(CALM_TALK, "Which stores do you run?")
            npc(Npc.razmire_keelgan, CALM_TALK, "I run a general store with a few useful items, and I have a building supplies store, I'm hoping that when things get better down here, people will want to buy items to repair their buildings.")
            options {
                op("Can I see your general store please") {
                    npc(Npc.razmire_keelgan, CALM_TALK, "Well to be honest, all of my stock is packed away right now seeing as the town is all overrun with these Shades. I tell you what. You put an end to five Shades for me and I'll open the store for you! I have details on that dilapidated temple to the North. These shades have a purpose - they jealously guard their burial treasure! If you can do me some favours, I could help you to locate it!")
                    shadeOffer(owner)
                }
                op("Can I see your build supplies store please") {
                    npc(Npc.razmire_keelgan, CALM_TALK, "Well to be honest, all of my stock is packed away right now seeing as the town is all overrun with these Shades. I tell you what. You put an end to five Shades for me and I'll open the store for you!")
                    npc(Npc.razmire_keelgan, CALM_TALK, "I have details on that dilapidated temple to the North. These shades have a purpose - they jealously guard their burial treasure! If you can do me some favours, I could help you to locate it!")
                    shadeOffer(owner)
                }
                op("I have other questions for you.") {
                    goto("razmireInitial")
                }
            }
        }
        op("What is this place?") {
            player(CALM_TALK, "What is this place?")
            npc(Npc.razmire_keelgan, CALM_TALK, "This place is called Mort'ton, it used to be a quiet town with a specialism in death. That is to say that many burial tombs were created here in the naturally formed caves under this very village.")
            goto("razmireInitial")
        }
        op("What's going on around here?") {
            player(CALM_TALK, "What's going on around here?")
            npc(Npc.razmire_keelgan, CALM_TALK, "Well, the people of the village, my usual customers... are all affected by the emanations from some place to the North East. Ulsquire can give you the name.")
            npc(Npc.razmire_keelgan, CALM_TALK, "Needless to say Herbi was working on a serum to help us and you stumbled across the recipe! I for one am grateful to you, but there's a lot left to do before Mort'ton is returned to its former glory.")
            goto("razmireInitial")
        }
        op("What are all these shadowy creatures?") {
            player(CALM_TALK, "What are all these shadowy creatures?")
            npc(Npc.razmire_keelgan, CALM_TALK, "Those disgusting entities are the filth that have ruined my life. They're the restless spirits of the dead who jealously guard their burial treasure yet feed on the the living.")
            npc(Npc.razmire_keelgan, CALM_TALK, "I've heard that they jealously guard their tomb treasure and that it's worth plundering. I need you to kill five shades and bring me their remains so that I can conduct some experiments. Will you do it?")
            shadeOffer(owner)
        }
        op("Is there anything worth doing around here?") {
            player(CALM_TALK, "Is there anything worth doing around here?")
            npc(Npc.razmire_keelgan, CALM_TALK, "Life is a daily struggle with those Shades around here. They attack when you least expect it...it's terrifying! I tell you what, if you kill five of those Shades I'll see if I can help you out! Is it a deal?")
            shadeOffer(owner)
        }
    }
}

private fun DialogueBuilder.shadeOffer(owner: Player) {
    options("Kill five shades?") {
        op("Yes, I'll dispatch those dark and evil creatures.") {
            player(CALM_TALK, "Yes, I'll dispatch those dark and evil creatures.")
            npc(Npc.razmire_keelgan, CALM_TALK, "Great, that's what I wanted to hear. When you've done it, bring back all the remains, I want to inspect them.") {
                if (owner.getQuestStage(Quest.SHADES_OF_MORTTON) == STAGE_READ_DIARY) {
                    owner.morttonState().setI("shadeKills", 0)
                    owner.setQuestStage(Quest.SHADES_OF_MORTTON, STAGE_KILL_SHADES)
                } else if (!owner.isQuestStarted(Quest.SHADES_OF_MORTTON)) {
                    owner.sendMessage("You should read Herbi Flax's diary first.")
                }
            }
        }
        op("Sorry, not right now, I need to do something else first.") {
            player(CALM_TALK, "Sorry, not right now, I need to do something else first.")
            npc(Npc.razmire_keelgan, CALM_TALK, "Ok, fair enough...")
        }
        op("I have another question.") {
            goto("razmireInitial")
        }
    }
}

private fun DialogueBuilder.razmireStores(owner: Player) {
    label("razmireStores")
    options {
        opExec("Can I see the general store please?") {
            ShopsHandler.openShop(owner, "razmire_general")
        }
        opExec("Can I see the building store please?") {
            ShopsHandler.openShop(owner, "razmire_builders")
        }
        op("I have another question.") {
            razmireQuestions(owner)
        }
        op("Ok, thanks.") {
            player(CALM_TALK, "Ok, thanks")
        }
    }
}

private fun DialogueBuilder.razmireQuestions(owner: Player) {
    label("razmireQuestions")
    options {
        op("What do you know out about the shades?") {
            player(CALM_TALK, "What do you know about the shades?")
            npc(Npc.razmire_keelgan, CALM_TALK, "Ulsqire says they're the incarnation of the dead who were interred in the tombs under Mort'ton. If you can find a way to put these spirits to rest, I'll show you a way into their lair! Mind you...there must be thousands of them buried under there! Just imagine all the treasure they buried under there!")
            goto("razmireQuestions")
        }
        op("What can you tell me about that temple?") {
            player(CALM_TALK, "What can you tell me about that temple?")
            npc(Npc.razmire_keelgan, CALM_TALK, "It's ancient and has been desecrated since before I was born. I've always felt that it was quite a nice place to sit and have a picnic, quite a nice view out of the swamps of Mort Myre.")
            npc(Npc.razmire_keelgan, CALM_TALK, "I told Ulsquire that if he can afford to go halves with me on the building materials, I'd help him to try and fix it up one day. I've always wondered what it looked like back in pagan times.") {
                owner.learnMorttonTemple("razmireTemple")
            }
            goto("razmireQuestions")
        }
        op("Do you know how I could make this serum permanent?") {
            player(CALM_TALK, "Do you know how I could make this serum permanent?")
            npc(Npc.razmire_keelgan, CALM_TALK, "I'd say not my friend. That clever Flax chap was always experimentatin', I guess he was a scholarly type. He always dismissed Ulsquires learned speculations about the temple, I'd say that was a bit short sighted myself.")
            player(CALM_TALK, "What's that supposed to mean?")
            npc(Npc.razmire_keelgan, CALM_TALK, "Well, you made the serum yourself so I guess you'll experimentate with it. I'd say that the temple might be able to change the serum in some way. Well, yes, I guess it sounds superstitious, but what's to be lost?")
            goto("razmireQuestions")
        }
        op("Can you open a store for me?") {
            player(CALM_TALK, "Can you open a store for me?")
            npc(Npc.razmire_keelgan, CALM_TALK, "Sure, which store do you wanna see?")
            goto("razmireStores")
        }
        op("Ok, thanks") {
            player(CALM_TALK, "Ok, thanks")
        }
    }
}

private fun DialogueBuilder.razmireRepair(owner: Player) {
    label("razmireRepair")
    options {
        op("What should I do when the temple is built?") {
            player(CALM_TALK, "What should I do when the temple is built?")
            npc(Npc.razmire_keelgan, CALM_TALK, "Well, you could always have a nice picnic... oh... but then there's the problem with the Shades... hmmm, not nice... not nice at all! Well, I'll be honest, I don't really know! But I bet Ulsquire will know.")
        }
        op("The Shades keep knocking the temple down!") {
            player(CALM_TALK, "The Shades keep knocking the temple down!")
            npc(Npc.razmire_keelgan, CALM_TALK, "Interesting... perhaps they have something against the temple? Perhaps it represents something bad to them? Hmmm, interesting stuff, but you'd best speak to Ulsquire about that sort of thing... do you need any supplies?")
            goto("razmireRepair")
        }
        op("I keep running out of building materials!") {
            player(CALM_TALK, "I keep running out of building materials!")
            npc(Npc.razmire_keelgan, CALM_TALK, "Oh really... that's interesting... I have just the thing for you!")
            exec {
                ShopsHandler.openShop(owner, "razmire_builders")
            }
        }
        op("Can you open a store for me?") {
            player(CALM_TALK, "Can you open a store for me?")
            npc(Npc.razmire_keelgan, CALM_TALK, "Sure, which store do you wanna see?")
            razmireStores(owner)
        }
        op("Ok, thanks.") {
            player(CALM_TALK, "Ok, thanks")
        }
    }
}
