package com.rs.game.content.quests.shadesofmortton

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.HeadE.DIZZY
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.skills.prayer.cremation.Pyre
import com.rs.game.content.skills.prayer.cremation.PyreLog
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.OwnedObject
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit

fun Player.talkMorttonUlsquire(firstCure: Boolean = false) {
    if (vars.getVarBit(Varbit.ulsquire_visible) == 0) {
        startConversation {
            npc(Npc.ulsquire_shauncy_afflicted, DIZZY, "seysyi gurghl ughugh")
            simple("~ This person doesn't make any sense at all. ~")
        }
        return
    }
    val stage = getQuestStage(Quest.SHADES_OF_MORTTON)
    startConversation {
        if (firstCure) npc(Npc.ulsquire_shauncy, CALM_TALK, "Ah, excellent, you've made your own serum and it seems to work well! Many thanks for using the Serum on me! But beware, I may start to change back soon!") else npc(Npc.ulsquire_shauncy, CALM_TALK, "Ah, hello again, what can I do for you now?")
        when (stage) {
            STAGE_COMPLETE -> {
                player(CALM_TALK, "Hello there.")
                npc(Npc.ulsquire_shauncy, CALM_TALK, "Hello again! Many thanks for helping us to solve the riddle with the Shades, we can start to work now putting all those Shade spirits to rest and get a little bit of a reward to go along with it!")
            }
            STAGE_RETURN_TO_ULSQUIRE -> {
                player(CALM_TALK, "I've put the Shade's spirit to rest!")
                npc(Npc.ulsquire_shauncy, CALM_TALK, "Great! Well done my friend!") {
                    questManager.completeQuest(Quest.SHADES_OF_MORTTON)
                }
            }
            STAGE_VISIT_ULSQUIRE -> {
                player(CALM_TALK, "Razmire said to come and talk to you.")
                npc(Npc.ulsquire_shauncy, CALM_TALK, "Oh yes, well, that's very nice.")
                if (!inventory.containsItem(Obj.shade_bones1, 1)) {
                    player(CALM_TALK, "I just slayed five shades for Razmire...he said that you might be interested in seeing the shade remains. Unfortunately I don't have any with me at the moment...")
                    npc(Npc.ulsquire_shauncy, CALM_TALK, "That is a shame, I would have liked to have seen the remains. Razmire was right. I'm keen to study them and perhaps see if I can work out a way to put their spirits to rest.")
                } else {
                    player(CALM_TALK, "I just slayed 5 shades for Razmire, he said you might be interested in seeing some shade remains?")
                    npc(Npc.ulsquire_shauncy, CALM_TALK, "Oh yes, that would be interesting!") {
                        if (getQuestStage(Quest.SHADES_OF_MORTTON) == STAGE_VISIT_ULSQUIRE && inventory.containsItem(Obj.shade_bones1, 1)) {
                            inventory.deleteItem(Obj.shade_bones1, 1)
                            setQuestStage(Quest.SHADES_OF_MORTTON, STAGE_LEARN_ABOUT_TEMPLE)
                        }
                    }
                    simple("You show the shade remains to the priest. He wanders off to study the bones.")
                }
            }
            STAGE_CREMATE_SHADE -> {
                when {
                    OwnedObject.getOwnedBy(this@talkMorttonUlsquire).filterIsInstance<Pyre>().any {
                        it.isShadePyre && !it.isDestroyed
                    } -> {
                        player(CALM_TALK, "I've placed the logs on the pyre. What do I do now?")
                        npc(Npc.ulsquire_shauncy, CALM_TALK, "You need to place the remains of the shade on top of the logs and then set alight to it.")
                    }
                    morttonState().getB("pyreLogsMade") || PyreLog.values().any {
                        inventory.containsItem(it.itemId, 1)
                    } -> {
                        player(CALM_TALK, "Hello there, I used the sacred oil on the logs and I made pyre logs! What should I do now?")
                        npc(Npc.ulsquire_shauncy, CALM_TALK, "Now you need to place the sacred logs on the pyre, then add the remains of a shade, then set fire to the pyre. This should release the Shade from it's eternal damnation and help it pass on to the other side.")
                    }
                    else -> {
                        player(CALM_TALK, "Hello there, I used the olive oil on the flame and it gave me sacred oil! What should I do now?")
                        npc(Npc.ulsquire_shauncy, CALM_TALK, "From what I've learned, the ancients used to use the sacred oil on logs in order to make pyre logs for cremation. You could try that!")
                    }
                }
            }
            STAGE_KILL_SHADES -> {
                player(CALM_TALK, "How's it going? I talked to Razmire and he's asked me to slay five shades.")
                npc(Npc.ulsquire_shauncy, CALM_TALK, "I can't say that I'm surprised, Razmire hates them, but it would be interesting to see the remains of one and investigate it somehow. I might find something useful!")
                ulsquireInitial(early = true)
            }
            STAGE_REBUILD_TEMPLE -> {
                if (morttonState().getB("repairStarted")) {
                    player(CALM_TALK, "Hello there, I've started repairing the temple.")
                    npc(Npc.ulsquire_shauncy, CALM_TALK, "That's great, carry on the good work. I'm sure that the temple is the key to unlocking a better life for all of us in Mort'ton.")
                    ulsquireRepair(this@talkMorttonUlsquire)
                } else ulsquireResearch(this@talkMorttonUlsquire)
            }
            STAGE_LEARN_ABOUT_TEMPLE -> ulsquireResearch(this@talkMorttonUlsquire)
            else -> ulsquireInitial()
        }
    }
}

private fun DialogueBuilder.ulsquireInitial(early: Boolean = false) {
    if (early) DialogueBuilder(stages).apply {
        ulsquireInitial()
    }
    label(if (early) "ulsquireInitialEarly" else "ulsquireInitial")
    options {
        op("Who are you?") {
            player(CALM_TALK, "Who are you?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "My name is Ulsquire Shauncy. I'm the local teacher and priest around here. You've really come at a time when Mort'ton is in a terrible state. The emanations from the rest of Morytania affect the people here terribly turning them into mindless creatures. And, as if that wasn't enough, we get picked off by the evil Shades that exist in this area.")
            player(CALM_TALK, "Wow, you guys seem to be really at the bottom of the food chain around here.")
            goto("ulsquireInitial")
        }
        op("What is this place?") {
            player(CALM_TALK, "What is this place?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "This sad and afflicted little town is called 'Mort'ton'. In it's heyday it was a busy and bustling market town with a specialty for interring the dead. The vile emanations from the Sanguinesti area started to spread and the town hasn't been the same since. The serum you made was the one that Herbi Flax was working on. Unfortunately the serum isn't permanent.")
            player(CALM_TALK, "Do you know how I could make the serum permanent?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "I remember Herbi Flax talking about the serum. He said it worked on what he called the 'biological' effects of the affliction but that some other secondary effect was at work which he didn't quite understand. He was interested in 'Flaemtaer', the ruined temple to the North. I found this odd. A man of science studying a structure dedicated to faith and the struggle of good verus evil, another concept he didn't understand.")
            goto("ulsquireInitial")
        }
        op("What's going on around here?") {
            player(CALM_TALK, "What's going on around here?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "The people are affected by the emanations from the rest of Morytania and the dead from the tombs have manifested into shadow creatures.")
            goto("ulsquireInitial")
        }
        op("What are all these shadowy creatures?") {
            player(CALM_TALK, "What are all these shadowy creatures?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "They are the disturbed spirits of those interred in the earth beneath Mort'ton. Their eternal spirits have been poisoned by the close vicinity to the evil nature of the inhabitants of the Sanguinesti area.")
            goto("ulsquireInitial")
        }
        if (!early) op("Is there anyone else worth talking to around here?") {
            player(CALM_TALK, "Is there anyone else worth talking to around here?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "Well, there's my good friend Razmire Keelgan. He's a prominent shop owner in Mort'ton, at least he was before he was affected by the area. I know he tried to gather the militia together to fight the shades. He might know something about the temple. Razmire lives in the building to the west just over there, go and talk to him.")
        }
        op("Ok, thanks.") {
            player(CALM_TALK, "Ok, thanks.")
        }
    }
}

private fun DialogueBuilder.ulsquireResearch(owner: Player) {
    label("ulsquireResearch")
    options {
        op("What did you find out about the remains?") {
            player(CALM_TALK, "What did you find out about the remains.")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "The shade remains are made of a strange substance which I've not come across before. I believe we should put these tormented spirits to rest.")
            player(CALM_TALK, "How can we do that? You can't even bury the remains!")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "Quite right... I was thinking that a holy cremation would do the trick, but the funeral pyres need specially prepared 'pyre logs' - normal logs that have been treated with 'sacred oil' to give it a nice holy touch.")
            player(CALM_TALK, "How would I make some sacred oil?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "You won't! It's a pagan secret lost a long time ago. Normally I would try to bless some commonly found vegetable oil, but I'm so weak from this illness that I know I just won't be able to do it.")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "Huh! I used to joke with Herbi Flax that if 'Flaemtaer temple' was still standing, we could practice some ancient pagan rituals and incite the sacred flame! Huh, I'm not surprised that he never took me seriously!") {
                owner.learnMorttonTemple("cremation")
            }
            goto("ulsquireResearch")
        }
        op("What can you tell me about that temple?") {
            player(CALM_TALK, "What can you tell me about that temple?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "Hmm, interesting question. It is an ancient pagan temple referred to as Flaemtaer. I've studied it quite a lot, one day I hope to be able to rebuild it, who knows what ancient power it could unleash?")
            player(CALM_TALK, "How can you rebuild a totally destroyed temple?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "I couldn't do it alone, I'd need a few expert artisans to help with the construction. I'd also need a good supply of limestone bricks, swamp paste and timber beams. Thankfully Razmire stocks all these items. (Sigh)") {
                owner.learnMorttonTemple("ulsquireTemple")
            }
            goto("ulsquireResearch")
        }
        op("Do you know how I could make this serum permanent?") {
            player(CALM_TALK, "Do you know how I could make this serum permanent?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "The serum is a product of science. Herbi was a clever man to have made it, but he didn't integrate anything from the spiritual world into his work - and for all of his hard science there is still something missing.")
            player(CALM_TALK, "So, you don't know then!")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "I'm a man of faith...that's all I know. Sometimes I feel that we should experiment with the sacred and the holy, not just the crude matter we find around us everyday.")
            goto("ulsquireResearch")
        }
        op("What should I do now?") {
            player(CALM_TALK, "What should I do now?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "That's up to you my friend! I once heard someone say that making your own decisions is the only real freedom you have. Exercise your freedom my friend, while you still can.")
            goto("ulsquireResearch")
        }
        op("Ok, thanks") {
            player(CALM_TALK, "Ok, thanks")
        }
    }
}

private fun DialogueBuilder.ulsquireRepair(owner: Player) {
    label("ulsquireRepair")
    options {
        opExec("What should I do when the temple is built?") {
            owner.explainMorttonSacredFlame()
        }
        op("The Shades keep knocking the temple down!") {
            player(CALM_TALK, "The Shades keep knocking the temple down!")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "I'm not surprised. The temple represents holiness. They may feel that it has power over them in some way. My advice is to carry on and finish the temple if you can and see what happens. You many need help though!")
            player(CALM_TALK, "How do you mean?")
            npc(Npc.ulsquire_shauncy, CALM_TALK, "You may need help building the temple or defending yourself against the Shades. I'm sure if you ask around nicely you would be able to get some help.")
            label("ulsquireShadeQuestions")
            options {
                op("Tell me more about the Shades.") {
                    player(CALM_TALK, "Tell me more about the Shades.")
                    npc(Npc.ulsquire_shauncy, CALM_TALK, "All undead hate mortal spirituality. It's a symbol of mortal hope in the face of death and the unknown. The temple represents the finality of true death where all ties to the mortal world are lost.")
                    npc(Npc.ulsquire_shauncy, CALM_TALK, "The Shades wish to forever possess the value of their tomb treasures and jealously guard them in death as they did in life. They are afraid of true death and so try to destroy anything which reminds them of it.")
                    goto("ulsquireRepair")
                }
                opExec("What should I do when the temple is built?") {
                    owner.explainMorttonSacredFlame()
                }
                op("I keep running out of building materials!") {
                    ulsquireMaterials()
                    goto("ulsquireShadeQuestions")
                }
                op("Ok, thanks.") {
                    player(CALM_TALK, "Ok, thanks.")
                }
            }
        }
        op("I keep running out of building materials!") {
            ulsquireMaterials()
            goto("ulsquireRepair")
        }
        op("Ok, thanks.") {
            player(CALM_TALK, "Ok, thanks.")
        }
    }
}

private fun DialogueBuilder.ulsquireMaterials() {
    player(CALM_TALK, "I keep running out of building materials!")
    npc(Npc.ulsquire_shauncy, CALM_TALK, "I'd go and see Razmire, perhaps he has some items in stock?")
}

private fun Player.explainMorttonSacredFlame() {
    startConversation {
        player(CALM_TALK, "What should I do when the temple is built?")
        npc(Npc.ulsquire_shauncy, CALM_TALK, "I've researched this much, it seems that the temple was dedicated to the worship of elemental flame or fire. The sacred flame was said to be very holy and was used somehow to sanctify oil for use in cremations.")
        if (vars.getVarBit(Varbit.shades_ulsquire_oil_given) == 0) {
            npc(Npc.ulsquire_shauncy, CALM_TALK, "The pagans believed that sacred oil sanctified the cremation ceremony and thus helped the deceased pass onto the next life, and it helped the logs to burn! In case there's any truth in the story, have this!") {
                if (vars.getVarBit(Varbit.shades_ulsquire_oil_given) == 0 && inventory.hasFreeSlots()) {
                    inventory.addItem(Obj.oliveoil3, 1)
                    vars.saveVarBit(Varbit.shades_ulsquire_oil_given, 1)
                    sendMessage("Ulsquire gives you some olive oil.")
                } else if (vars.getVarBit(Varbit.shades_ulsquire_oil_given) == 0) {
                    sendMessage("You need a free inventory space to take the olive oil.")
                }
            }
            player(CALM_TALK, "Thanks!")
        } else {
            npc(Npc.ulsquire_shauncy, CALM_TALK, "The pagans believed that sacred oil sanctified the cremation ceremony and thus helped the deceased pass onto the next life, and it helped the logs to burn! You can buy olive oil from Razmire, it's quite cheap.")
        }
        ulsquireRepair(this@explainMorttonSacredFlame)
    }
}
