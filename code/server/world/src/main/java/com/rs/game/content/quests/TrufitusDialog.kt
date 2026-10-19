package com.rs.game.content.quests

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.junglepotion.JunglePotion
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.COMPLETED
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.DEAL_WITH_RASHILIYIAS_CORPSE
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.DELIVER_BELT
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.INVESTIGATE_B_TOMB
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.RASHILIYIAS_TOMB
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.RETURN_TO_TRUFITUS
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.RETURN_TO_TRUFITUS_AGAIN
import com.rs.game.content.quests.shilo_village.ShiloVillage.Companion.VERIFY_TEMPLE_OF_AH_ZA_RHOON
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapTrufitus() {
    fun showedAhZaRhoonItems(player: Player): Boolean {
        val attribs = player.questManager.getAttribs(Quest.SHILO_VILLAGE)
        return attribs.getB("HAS_SHOWN_BONE_SHARD") && attribs.getB("HAS_SHOWN_TATTERED_SCROLL") && attribs.getB("HAS_SHOWN_CRUMPLED_SCROLL") && attribs.getB("HAS_BURIED_Z_CORPSE")
    }

    onItemOnNpc(Npc.trufitus) { (player, item, npc) ->
        when (item.id) {
            Obj.zqbevsword -> player.startConversation {
                if (player.getQuestStage(Quest.SHILO_VILLAGE) == INVESTIGATE_B_TOMB || player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS_AGAIN) {
                    simple("You show Trufitus the sword pommel.")
                    player(HeadE.CALM_TALK, "Could you have a look at this please?")
                    npc(npc, HeadE.CALM_TALK, "It is a very nice item Bwana. It may be just what you need to gain access to Rashiliyia's tomb. While you were away, I did some research. Rashiliyia would spare the lives of those who wore bronze necklaces.")
                    npc(npc, HeadE.CALM_TALK, "This pommel may have some significance to Bervirius. Perhaps you can craft something from it that can help? My guess is that you will need some protection from Rashiliyia if you intend to enter her tomb!")
                    options("Select an option") {
                        op("How do I make a bronze necklace?") {
                            player(HeadE.CONFUSED, "How do I make a bronze necklace?")
                            npc(npc, HeadE.CALM_TALK, "Well, Bwana, I would guess that you would need to get some bronze metal and work it into something that could be turned into a necklace?")
                            options("Select an option") {
                                op("What should I put on the necklace?") {
                                    player(HeadE.HAPPY_TALKING, "What should I put on the necklace?")
                                    npc(npc, HeadE.CALM_TALK, "Perhaps Zadimus' clue has the answer? Now, what was it that he said again? Something about kin and keys? That sword pommel belonged to Bervirius didn't it?")
                                }
                                op("Thanks!") {
                                    player(HeadE.HAPPY_TALKING, "Thanks!")
                                    npc(npc, HeadE.CALM_TALK, "You're more than welcome Bwana! Good luck for the rest of your quest.")
                                }
                            }
                        }
                        op("What should I put on the necklace?") {
                            player(HeadE.HAPPY_TALKING, "What should I put on the necklace?")
                            npc(npc, HeadE.CALM_TALK, "Perhaps Zadimus' clue has the answer? Now, what was it that he said again? Something about kin and keys? That sword pommel belonged to Bervirius didn't it?")
                            options {
                                op("How do I make a bronze necklace?") {
                                    player(HeadE.CONFUSED, "How do I make a bronze necklace?")
                                    npc(npc, HeadE.CALM_TALK, "Well, Bwana, I would guess that you would need to get some bronze metal and work it into something that could be turned into a necklace?")
                                }
                                op("Thanks!") {
                                    player(HeadE.HAPPY_TALKING, "Thanks!")
                                    npc(npc, HeadE.CALM_TALK, "You're more than welcome Bwana! Good luck for the rest of your quest.")
                                }
                            }
                        }
                    }
                } else {
                    npc(npc, HeadE.CALM_TALK, "I'm sorry Bwana but I just don't have a use for that!")
                }
            }

            Obj.zqcrystal -> player.startConversation {
                if (player.getQuestStage(Quest.SHILO_VILLAGE) == INVESTIGATE_B_TOMB || player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS_AGAIN) {
                    simple("You show Trufitus the Locating Crystal.")
                    npc(npc, HeadE.AMAZED, "This is incredible Bwana.")
                    player(HeadE.CALM_TALK, "It is?")
                    npc(npc, HeadE.AMAZED, "Absolutely! This will help you to locate the entrance to Rashiliyia's tomb. Simply activate it when you think you are near, and it should glow different colours to show how near you are.")
                } else {
                    npc(npc, HeadE.CALM_TALK, "I'm sorry Bwana but I just don't have a use for that!")
                }
            }

            Obj.zqdeadbeads -> player.startConversation {
                if (player.getQuestStage(Quest.SHILO_VILLAGE) == INVESTIGATE_B_TOMB || player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS_AGAIN || player.getQuestStage(Quest.SHILO_VILLAGE) == RASHILIYIAS_TOMB) {
                    simple("You show Trufitus the necklace.")
                    player(HeadE.CALM_TALK, "Take a look at this...")

                    npc(npc, HeadE.AMAZED, "This is very impressive Bwana, I'm quite surprised at your ingenuity. This should be a good protection against Rashiliyia if you ever find her Tomb.")
                } else {
                    npc(npc, HeadE.CALM_TALK, "I'm sorry Bwana but I just don't have a use for that!")
                }
            }

            Obj.zqberviriusscroll2 -> player.startConversation {
                if (player.getQuestStage(Quest.SHILO_VILLAGE) == INVESTIGATE_B_TOMB || player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS_AGAIN) {
                    simple("You hand the notes over to Trufitus.")
                    npc(npc, HeadE.CALM_TALK, "Hmm, these notes are quite extraordinary Bwana. They give location details of Rashiliyia's tomb, and some information on how to use the crystal. The information is quite specific, North of Ah Za Rhoon! That's a great place to start looking!")
                } else {
                    npc(npc, HeadE.CALM_TALK, "I'm sorry Bwana but I just don't have a use for that!")
                }
            }

            Obj.zqboneshard -> player.startConversation {
                simple("You show Trufitus the Bone Shard")
                player(HeadE.CALM_TALK, "Could you have a look at this please?")
                simple("Trufitus looks at the object for a moment.")
                npc(npc, HeadE.CALM_TALK, "It looks like a simple shard of bone. Why do you think it is significant?")
                options("SELECT AN OPTION") {
                    op("It appeared when I buried Zadimus' corpse.") {
                        player(HeadE.CALM_TALK, "It appeared when I buried Zadimus' corpse.")
                        npc(npc, HeadE.CALM_TALK, "Ah, interesting, so you think that Zadimus gave you the bone? What makes you say that?")
                        options("SELECT AN OPTION") {
                            op("He said something after he gave it to me.") {
                                player(HeadE.CALM_TALK, "He said something after he gave it to me.")
                                npc(npc, HeadE.CALM_TALK, "What did he say?")
                                options("SELECT AN OPTION") {
                                    op("The spirit said something about keys and kin?") {
                                        player(HeadE.CONFUSED, "The spirit said something about keys and kin?")
                                        npc(npc, HeadE.CALM_TALK, "Hmmm, maybe it's a clue of some kind?") {
                                            player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_SHOWN_BONE_SHARD", true)
                                            if (player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS && showedAhZaRhoonItems(player)) {
                                                player.setQuestStage(Quest.SHILO_VILLAGE, INVESTIGATE_B_TOMB)
                                            }
                                        }
                                        npc(npc, HeadE.CALM_TALK, "If you found anything at Ah Za Rhoon that you're not sure of, why not investigate it or show it to me and I'll see what I can make of it.")
                                    }
                                    op("The spirit rambled on about some nonsense.") {
                                        player(HeadE.CALM_TALK, "The spirit rambled on about some nonsense.")
                                        npc(npc, HeadE.CALM_TALK, "Oh, so it most likely was not very important then.")
                                    }
                                }
                            }
                            op("I'm not sure.") {
                                player(HeadE.CALM_TALK, "I'm not sure.")
                                npc(npc, HeadE.CALM_TALK, "Oh, right. Come back and talk with me if you get an idea.")
                            }
                        }
                    }
                    op("No reason really.") {
                        player(HeadE.CALM_TALK, "No reason really.")
                        npc(npc, HeadE.CALM_TALK, "Well why are you showing it to me then?")
                        options("SELECT AN OPTION") {
                            op("It appeared when I buried Zadimus' corpse.") {
                                player(HeadE.CALM_TALK, "It appeared when I buried Zadimus' corpse.")
                                npc(npc, HeadE.CALM_TALK, "Ah, interesting, so you think that Zadimus gave you the bone? What makes you say that?")
                                options("SELECT AN OPTION") {
                                    op("He said something after he gave it to me.") {
                                        player(HeadE.CALM_TALK, "He said something after he gave it to me.")
                                        npc(npc, HeadE.CALM_TALK, "What did he say?")
                                        options("SELECT AN OPTION") {
                                            op("The spirit said something about keys and kin?") {
                                                player(HeadE.CONFUSED, "The spirit said something about keys and kin?")
                                                npc(npc, HeadE.CALM_TALK, "Hmmm, maybe it's a clue of some kind?")
                                                npc(npc, HeadE.CALM_TALK, "If you found anything at Ah Za Rhoon that you're not sure of, why not investigate it or show it to me and I'll see what I can make of it.")
                                            }
                                            op("The spirit rambled on about some nonsense.") {
                                                player(HeadE.CALM_TALK, "The spirit rambled on about some nonsense.")
                                                npc(npc, HeadE.CALM_TALK, "Oh, so it most likely was not very important then.")
                                            }
                                        }
                                    }
                                    op("I'm not sure.") {
                                        player(HeadE.CALM_TALK, "I'm not sure.")
                                        npc(npc, HeadE.CALM_TALK, "Oh, right. Come back and talk with me if you get an idea.")
                                    }
                                }
                            }
                            op("I'm not sure.") {
                                player(HeadE.CALM_TALK, "I'm not sure.")
                                npc(npc, HeadE.CALM_TALK, "Oh, right. Come back and talk with me if you get an idea.")
                            }
                        }
                    }
                }
            }

            Obj.zqplaque -> player.startConversation {
                player(HeadE.CALM_TALK, "Can you decipher this please?")
                npc(npc, HeadE.CALM_TALK, "This is an ancient artefact!")
                simple("Trufitus looks at the item in awe..")
                npc(npc, HeadE.CALM_TALK, "I can certainly try! Hmm, incredible, it seems very ancient and mentions something about Zadimus and Ah Za Rhoon. It says, 'Here lies the traitor Zadimus, let his spirit be forever tormented'.")
                simple("Trufitus hands the Stone Plaque back.")
                npc(npc, HeadE.CALM_TALK, "If you have found anything else that you need help with, please just let me know.") {
                    exec {
                        player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_SHOWN_STONE_PLAQUE", true)
                        if (player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS && showedAhZaRhoonItems(player)) {
                            player.setQuestStage(Quest.SHILO_VILLAGE, INVESTIGATE_B_TOMB)
                        }
                    }
                }

            }

            Obj.zqberviriusscroll -> player.startConversation {
                simple("You hand the tattered scroll to Trufitus.")
                npc(npc, HeadE.AMAZED, "Truly amazing Bwana, this scroll must be ancient. I'm not sure if I get more meaning from it than you though. Perhaps Bervirius' tomb is still accessible?")
                simple("Trufitus hands the tattered scroll back to you.") {
                    exec {
                        player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_SHOWN_TATTERED_SCROLL", true)
                        if (player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS && showedAhZaRhoonItems(player)) {
                            player.setQuestStage(Quest.SHILO_VILLAGE, INVESTIGATE_B_TOMB)
                        }
                    }
                }
            }

            Obj.zqrashiliyiascroll -> player.startConversation {
                player(HeadE.CALM_TALK, "Have a look at this, tell me what you think.")
                npc(npc, HeadE.AMAZED, "I am speechless Bwana, this is truly ancient. Where did you find it?")
                player(HeadE.CALM_TALK, "In an underground building of some sort.")
                npc(npc, HeadE.AMAZED_MILD, "You must truly have found the temple of Ah Za Rhoon! The scroll gives some interesting details about Rashiliyia, some things I didn't know before.")
                simple("Trufitus gives back the scroll.") {
                    player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).setB("HAS_SHOWN_CRUMPLED_SCROLL", true)
                    if (player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS && showedAhZaRhoonItems(player)) {
                        player.setQuestStage(Quest.SHILO_VILLAGE, INVESTIGATE_B_TOMB)
                    }
                }
                options("Select an option") {
                    op("Anything that can help?") {
                        player(HeadE.CONFUSED, "Anything that can help?")
                        npc(npc, HeadE.CALM, "Hmmm, well just that part about the wards...")
                        simple("Trufitus seems to drift off in thought.")
                        npc(npc, HeadE.CALM, "It may be possible to make a ward like that... But what is the best thing to make it from? Perhaps something close to Bervirius, an item of some significance to him.")
                    }
                    op("Ok, thanks!") {
                        player(HeadE.CALM_TALK, "Ok, thanks!")
                        npc(npc, HeadE.CALM_TALK, "You're quite welcome Bwana.")
                    }
                }
            }

            Obj.zqzadimusbones -> player.startConversation {
                simple("You show Trufitus the corpse.")
                player(HeadE.CALM_TALK, "What do you make of this?")
                npc(npc, HeadE.AMAZED, "! GASP ! That's incredible, where did you find it?")
                player(HeadE.CALM_TALK, "I found the corpse in a decomposing gallows. I get a very strange feeling every time I try to bury the body.")
                npc(npc, HeadE.CONFUSED, "Hmmm, that sounds very strange. I sense a spirit in torment, you should try to bury the remains.")
                options("Select an option") {
                    op("Is there any sacred ground around here?") {
                        player(HeadE.CALM_TALK, "Is there any sacred ground around here?") {
                            val attribs = player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE)
                            attribs.setB("HAS_SHOWN_Z_CORPSE", true)
                            if (player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS && showedAhZaRhoonItems(player)) {
                                player.setQuestStage(Quest.SHILO_VILLAGE, INVESTIGATE_B_TOMB)
                            }
                        }
                        npc(npc, HeadE.CALM_TALK, "The ground in the centre of the village is very sacred to us. Maybe you could try there?")
                    }
                    op("Can you dispose of this for me?") {
                        player(HeadE.CALM_TALK, "Can you dispose of this for me?")
                        simple("Trufitus pulls away from you...")
                        npc(npc, HeadE.CALM_TALK, "I dare not touch it. I am a spiritual man and the spirit of this being may possess me and turn me into a minion of Rashiliyia.")
                    }
                }
            }

            Obj.zqcorpse -> player.startConversation {
                simple("You show Trufitus the remains...")
                player(HeadE.CALM_TALK, "Could you have a look at this..")
                npc(npc, HeadE.AMAZED, "This is truly incredible bwana... So these are the remains of the dread Queen Rashiliyia?")
                player(HeadE.CALM_TALK, "Yes, I think so.")
                options("Select an option") {
                    op("What should I do with them?") {
                        player(HeadE.CALM_TALK, "What should I do with them?")
                        npc(npc, HeadE.CALM_TALK, "Hmm, I'm not exactly sure... Perhaps there is a clue in one of the artefacts you have found?")
                        options("Select an option") {
                            op("Can you take them off my hands?") {
                                player(HeadE.CALM_TALK, "Can you take them off my hands?")
                                npc(npc, HeadE.SCARED, "I dare not take them, I may be taken over by the evil spirit of Rashiliyia!")
                            }
                            op("Thanks!") {
                                player(HeadE.CALM_TALK, "Thanks!")
                                npc(npc, HeadE.CALM_TALK, "You're more than welcome Bwana! Good luck for the rest of your quest.")
                            }
                        }
                    }
                    op("Can you take them off my hands?") {
                        options("Select an option") {
                            op("What should I do with them?") {
                                player(HeadE.CALM_TALK, "What should I do with them?")
                                npc(npc, HeadE.CALM_TALK, "Hmm, I'm not exactly sure... Perhaps there is a clue in one of the artefacts you have found?")
                                options("Select an option") {
                                    op("Can you take them off my hands?") {
                                        player(HeadE.CALM_TALK, "Can you take them off my hands?")
                                        npc(npc, HeadE.SCARED, "I dare not take them, I may be taken over by the evil spirit of Rashiliyia!")
                                    }
                                    op("Thanks!") {
                                        player(HeadE.CALM_TALK, "Thanks!")
                                        npc(npc, HeadE.CALM_TALK, "You're more than welcome Bwana! Good luck for the rest of your quest.")
                                    }
                                }
                            }
                        }
                    }
                }
                npc(npc, HeadE.CONFUSED, "Hmmm, that sounds very strange. I sense a spirit in torment, you should try to bury the remains.")
                options("Select an option") {
                    op("Is there any sacred ground around here?") {
                        player(HeadE.CALM_TALK, "Is there any sacred ground around here?")
                        exec {
                            val attribs = player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE)
                            attribs.setB("HAS_SHOWN_Z_CORPSE", true)
                            if (player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS && showedAhZaRhoonItems(player)) {
                                player.setQuestStage(Quest.SHILO_VILLAGE, INVESTIGATE_B_TOMB)
                            }
                        }
                        npc(npc, HeadE.CALM_TALK, "The ground in the centre of the village is very sacred to us. Maybe you could try there?")
                    }
                    op("Can you dispose of this for me?") {
                        player(HeadE.CALM_TALK, "Can you dispose of this for me?")
                        simple("Trufitus pulls away from you...")
                        npc(npc, HeadE.CALM_TALK, "I dare not touch it. I am a spiritual man and the spirit of this being may possess me and turn me into a minion of Rashiliyia.")
                    }
                }
            }

            Obj.mosol_wampum_belt -> if (player.getQuestStage(Quest.SHILO_VILLAGE) == DELIVER_BELT) {
                player.startConversation {
                    npc(npc, HeadE.MORTIFIED, "Hello Bwana, this message from Mosol Rei bears bad news... Yes, things do look very bad indeed.")
                    options("Select an option") {
                        op("What do you know about Rashiliyia?") {
                            player(HeadE.CALM_TALK, "What do you know about Rashiliyia?")
                            npc(npc, HeadE.CALM_TALK, "Hmmm, it's been a long time since I heard that name. Rashiliyia is the Queen of the Undead. And a more fearsome enemy you will be unlikely to find. I fear that you bring me news that she has returned to plague us once again? Alas I know of no weakness that she has.")
                            options("Select an option") {
                                op("So there is nothing we can do?") {
                                    player(HeadE.WORRIED, "So there is nothing we can do?")
                                    npc(npc, HeadE.CALM_TALK, "Not that I can think of.")
                                    options("Select an option") {
                                        op("Oh, ok!") {
                                            player(HeadE.CALM_TALK, "Oh, ok!")
                                            npc(npc, HeadE.SAD_MILD, "Yes, it's a bit sad really, I liked that village.") {
                                                player.simpleDialogue("Trufitus seems deeply touched...")
                                            }
                                            npc(npc, HeadE.CALM_TALK, "Well, I hope you will excuse me, but I need to get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                        }
                                        op("Should I start to evacuate the Island?") {
                                            player(HeadE.WORRIED, "Should I start to evacuate the Island?")
                                            npc(npc, HeadE.WORRIED, "Yes, that may be a good idea. Many people could die! If only there was a way to defeat her!")
                                            options("Select an option") {
                                                op("Mosol Rei said something about a legend?") {
                                                    label("legend")
                                                    player(HeadE.CONFUSED, "Mosol Rei said something about a legend?")
                                                    npc(npc, HeadE.CALM_TALK, "Ah, yes, there is a legend, but it is lost in the midst of antiquity... The last place to hold any details regarding this mystery was in the temple of Ah Za Rhoon....and that has long since vanished... it crumbled into dust...")
                                                    label("more")
                                                    options("Select an option") {
                                                        op("Why was it called Ah Za Rhoon?") {
                                                            player(HeadE.CONFUSED, "Why was it called Ah Za Rhoon?")
                                                            npc(npc, HeadE.CALM_TALK, "It is from an ancient language. The direct translation is... 'Magnificence floating on water'. But my research makes me believe that the temple was built on land. And most likely between large ")
                                                            npc(npc, HeadE.CALM_TALK, "bodies of water, for example large lakes. However, many people have searched for the temple, and have failed. I would hate to see you waste your time on a pointless search like that.")
                                                            options("Select an option") {
                                                                op("Thanks for the information!") {
                                                                    label("thanks")
                                                                    player(HeadE.CALM_TALK, "Thanks for the information!")
                                                                    npc(npc, HeadE.CONFUSED, "What information?")
                                                                    player(HeadE.CALM_TALK, "About Ah Za Rhoon and where it is.") {
                                                                        player.simpleDialogue("Trufitus looks at you blankly...")
                                                                    }
                                                                    npc(npc, HeadE.CALM_TALK, "Hmmm, well, you are welcome bwana.")
                                                                    options("Select an option") {
                                                                        op("Do you know anything more about the temple?") {
                                                                            player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                            npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                            options("Select an option") {
                                                                                op("Tell me more.") {
                                                                                    player(HeadE.CONFUSED, "Tell me more.")
                                                                                    npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                    goto("more")
                                                                                }
                                                                                op("Are there any traps there?") {
                                                                                    player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                    npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                                }
                                                                            }
                                                                        }
                                                                        op("I am going to search for Ah Za Rhoon!") {
                                                                            player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                            npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                            npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                            options("Select an option") {
                                                                                op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                    player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                    npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                    npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                        player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                        player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                    }
                                                                                }
                                                                                op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                    player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                                }
                                                                            }
                                                                        }
                                                                        op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                            player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                        }
                                                                    }
                                                                }
                                                                op("Do you know anything more about the temple?") {
                                                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                    options("Select an option") {
                                                                        op("Tell me more.") {
                                                                            player(HeadE.CONFUSED, "Tell me more.")
                                                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                            goto("more")
                                                                        }
                                                                        op("Are there any traps there?") {
                                                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                        }
                                                                    }
                                                                }
                                                                op("I am going to search for Ah Za Rhoon!") {
                                                                    player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                    npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                    npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                    options("Select an option") {
                                                                        op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                            player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                            npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                            npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                            }
                                                                        }
                                                                        op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                            player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                        }
                                                                    }
                                                                }
                                                                op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                    player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                }
                                                            }
                                                        }
                                                        op("Do you know anything more about the temple?") {
                                                            player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                            npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                            options("Select an option") {
                                                                op("Tell me more.") {
                                                                    player(HeadE.CONFUSED, "Tell me more.")
                                                                    npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                    goto("more")
                                                                }
                                                                op("Are there any traps there?") {
                                                                    player(HeadE.CONFUSED, "Are there any traps there?")
                                                                    npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                op("Will you pack your things now?") {
                                                    player(HeadE.CONFUSED, "Will you pack your things now?")
                                                    npc(npc, HeadE.CALM_TALK, "I will wait and see what will happen. Maybe Rashiliyia does not have the power to strike too far from her resting place? But there are many things that I need to do now.")
                                                    options("Select an option") {
                                                        op("Is her resting place important?") {
                                                            player(HeadE.CONFUSED, "Is her resting place important?")
                                                            npc(npc, HeadE.CALM_TALK, "I believe it is! It might be that her physical remains are the focal point of her supernatural powers. It is said that many years ago, a group of adventurers once infiltrated her tomb to try to rid the world of Rashiliyia.")
                                                            npc(npc, HeadE.CALM_TALK, "These adventurers reported seeing a wraith- like creature. Although the adventurers disturbed Rashiliyia's bones, they were not able to properly sanctify them. And this is the most likely reason why she still plagues us today.")
                                                            npc(npc, HeadE.CALM_TALK, "Of course, she only has to order one of her minions to move her bones and she can quite quickly and easily set up a new headquarters anywhere and continue to launch her plague of undead.")
                                                            label("selector")
                                                            options("Select an option") {
                                                                op("What are minions?") {
                                                                    player(HeadE.CONFUSED, "What are minions?")
                                                                    npc(npc, HeadE.CALM_TALK, "Minions are the fiendish undead creatures that she controls. She has very few living worshippers, but they need to be dealt with at some point.")
                                                                    npc(npc, HeadE.CALM_TALK, "Usually a strong creature of some sort will be guarding her remains. And of course, she is a very powerful spell caster herself. Not to be tackled lightly.")
                                                                    options("Select an option") {
                                                                        op("Thanks for the information!") {
                                                                            label("thanks")
                                                                            player(HeadE.CALM_TALK, "Thanks for the information!")
                                                                            npc(npc, HeadE.CONFUSED, "What information?")
                                                                            player(HeadE.CALM_TALK, "About Ah Za Rhoon and where it is.") {
                                                                                player.simpleDialogue("Trufitus looks at you blankly...")
                                                                            }
                                                                            npc(npc, HeadE.CALM_TALK, "Hmmm, well, you are welcome bwana.")
                                                                            options("Select an option") {
                                                                                op("Do you know anything more about the temple?") {
                                                                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                                    options("Select an option") {
                                                                                        op("Tell me more.") {
                                                                                            player(HeadE.CONFUSED, "Tell me more.")
                                                                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                            goto("more")
                                                                                        }
                                                                                        op("Are there any traps there?") {
                                                                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                op("I am going to search for Ah Za Rhoon!") {
                                                                                    player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                                    npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                                    npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                                    options("Select an option") {
                                                                                        op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                            player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                            npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                                player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                                player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                            }
                                                                                        }
                                                                                        op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                            player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                                    player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                                }
                                                                            }
                                                                        }
                                                                        op("Does she have any weaknesses?") {
                                                                            player(HeadE.CONFUSED, "Does she have any weaknesses?")
                                                                            npc(npc, HeadE.CALM_TALK, "I am not sure, but the legend about her certainly is long. It's a pity that the temple of Ah Za Rhoon has crumbled as there may be some clues that could help us to defeat her.")
                                                                            npc(npc, HeadE.CALM_TALK, "I think the largest problem will be in locating her resting place.")
                                                                            options("Select an option") {
                                                                                op("Why was it called Ah Za Rhoon?") {
                                                                                    player(HeadE.CONFUSED, "Why was it called Ah Za Rhoon?")
                                                                                    npc(npc, HeadE.CALM_TALK, "It is from an ancient language. The direct translation is... 'Magnificence floating on water'. But my research makes me believe that the temple was built on land. And most likely between large ")
                                                                                    npc(npc, HeadE.CALM_TALK, "bodies of water, for example large lakes. However, many people have searched for the temple, and have failed. I would hate to see you waste your time on a pointless search like that.")
                                                                                    options("Select an option") {
                                                                                        op("Thanks for the information!") {
                                                                                            label("thanks")
                                                                                            player(HeadE.CALM_TALK, "Thanks for the information!")
                                                                                            npc(npc, HeadE.CONFUSED, "What information?")
                                                                                            player(HeadE.CALM_TALK, "About Ah Za Rhoon and where it is.") {
                                                                                                player.simpleDialogue("Trufitus looks at you blankly...")
                                                                                            }
                                                                                            npc(npc, HeadE.CALM_TALK, "Hmmm, well, you are welcome bwana.")
                                                                                            options("Select an option") {
                                                                                                op("Do you know anything more about the temple?") {
                                                                                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                                                    options("Select an option") {
                                                                                                        op("Tell me more.") {
                                                                                                            player(HeadE.CONFUSED, "Tell me more.")
                                                                                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                                            goto("more")
                                                                                                        }
                                                                                                        op("Are there any traps there?") {
                                                                                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") {
                                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                                op("I am going to search for Ah Za Rhoon!") {
                                                                                                    player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                                                    npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                                                    npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                                                    options("Select an option") {
                                                                                                        op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                                            player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                                            npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                                            npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                                                player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                                                player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                                            }
                                                                                                        }
                                                                                                        op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                                            player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                                op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                                                    player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                        player.sendMessage("Trufitus goes back to his studies.")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        op("Do you know anything more about the temple?") {
                                                                                            player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                                            npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                                            options("Select an option") {
                                                                                                op("Tell me more.") {
                                                                                                    player(HeadE.CONFUSED, "Tell me more.")
                                                                                                    npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                                    goto("more")
                                                                                                }
                                                                                                op("Are there any traps there?") {
                                                                                                    player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                                    npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") {
                                                                                                        player.sendMessage("Trufitus goes back to his studies.")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        op("I am going to search for Ah Za Rhoon!") {
                                                                                            player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                                            npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                                            npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                                            options("Select an option") {
                                                                                                op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                                    player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                                        player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                                        player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                                    }
                                                                                                }
                                                                                                op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                                    player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                        player.sendMessage("Trufitus goes back to his studies.")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                                            player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                op("What are onions?") {
                                                                    player(HeadE.CONFUSED, "What are onions?")
                                                                    player.simpleDialogue("Trufitus looks at you blankly")
                                                                    npc(npc, HeadE.CONFUSED, "Surely you mean Minions?")
                                                                    player(HeadE.CALM_TALK, "Yes of course, I mean Minions, what made you think I said Onions?")
                                                                    player.simpleDialogue("Trufitus frowns at you but continues about...minions...")
                                                                    npc(npc, HeadE.CALM_TALK, "Minions are the fiendish undead creatures that Rashiliyia controls. She has very few living worshippers, but they need to be dealt with at some point.")
                                                                    npc(npc, HeadE.CALM_TALK, "Usually a strong creature of some sort will be guarding the bones and it is not to be tackled lightly.")
                                                                    options("Select an option") {
                                                                        op("Thanks for the information!") {
                                                                            label("thanks")
                                                                            player(HeadE.CALM_TALK, "Thanks for the information!")
                                                                            npc(npc, HeadE.CONFUSED, "What information?")
                                                                            player(HeadE.CALM_TALK, "About Ah Za Rhoon and where it is.") {
                                                                                player.simpleDialogue("Trufitus looks at you blankly...")
                                                                            }
                                                                            npc(npc, HeadE.CALM_TALK, "Hmmm, well, you are welcome bwana.")
                                                                            options("Select an option") {
                                                                                op("Do you know anything more about the temple?") {
                                                                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                                    options("Select an option") {
                                                                                        op("Tell me more.") {
                                                                                            player(HeadE.CONFUSED, "Tell me more.")
                                                                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                            goto("more")
                                                                                        }
                                                                                        op("Are there any traps there?") {
                                                                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                op("I am going to search for Ah Za Rhoon!") {
                                                                                    player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                                    npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                                    npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                                    options("Select an option") {
                                                                                        op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                            player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                            npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                                player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                                player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                            }
                                                                                        }
                                                                                        op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                            player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                                    player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                                }
                                                                            }
                                                                        }
                                                                        op("Does she have any weaknesses?") {
                                                                            player(HeadE.CONFUSED, "Does she have any weaknesses?")
                                                                            npc(npc, HeadE.CALM_TALK, "I am not sure, but the legend about her certainly is long. It's a pity that the temple of Ah Za Rhoon has crumbled as there may be some clues that could help us to defeat her.")
                                                                            npc(npc, HeadE.CALM_TALK, "I think the largest problem will be in locating her resting place.")
                                                                            options("Select an option") {
                                                                                op("Why was it called Ah Za Rhoon?") {
                                                                                    player(HeadE.CONFUSED, "Why was it called Ah Za Rhoon?")
                                                                                    npc(npc, HeadE.CALM_TALK, "It is from an ancient language. The direct translation is... 'Magnificence floating on water'. But my research makes me believe that the temple was built on land. And most likely between large ")
                                                                                    npc(npc, HeadE.CALM_TALK, "bodies of water, for example large lakes. However, many people have searched for the temple, and have failed. I would hate to see you waste your time on a pointless search like that.")
                                                                                    options("Select an option") {
                                                                                        op("Thanks for the information!") {
                                                                                            label("thanks")
                                                                                            player(HeadE.CALM_TALK, "Thanks for the information!")
                                                                                            npc(npc, HeadE.CONFUSED, "What information?")
                                                                                            player(HeadE.CALM_TALK, "About Ah Za Rhoon and where it is.") {
                                                                                                player.simpleDialogue("Trufitus looks at you blankly...")
                                                                                            }
                                                                                            npc(npc, HeadE.CALM_TALK, "Hmmm, well, you are welcome bwana.")
                                                                                            options("Select an option") {
                                                                                                op("Do you know anything more about the temple?") {
                                                                                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                                                    options("Select an option") {
                                                                                                        op("Tell me more.") {
                                                                                                            player(HeadE.CONFUSED, "Tell me more.")
                                                                                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                                            goto("more")
                                                                                                        }
                                                                                                        op("Are there any traps there?") {
                                                                                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") {
                                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                                op("I am going to search for Ah Za Rhoon!") {
                                                                                                    player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                                                    npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                                                    npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                                                    options("Select an option") {
                                                                                                        op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                                            player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                                            npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                                            npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                                                player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                                                player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                                            }
                                                                                                        }
                                                                                                        op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                                            player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                                op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                                                    player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                        player.sendMessage("Trufitus goes back to his studies.")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        op("Do you know anything more about the temple?") {
                                                                                            player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                                            npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                                            options("Select an option") {
                                                                                                op("Tell me more.") {
                                                                                                    player(HeadE.CONFUSED, "Tell me more.")
                                                                                                    npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                                    goto("more")
                                                                                                }
                                                                                                op("Are there any traps there?") {
                                                                                                    player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                                    npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") {
                                                                                                        player.sendMessage("Trufitus goes back to his studies.")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        op("I am going to search for Ah Za Rhoon!") {
                                                                                            player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                                            npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                                            npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                                            options("Select an option") {
                                                                                                op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                                    player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                                        player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                                        player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                                    }
                                                                                                }
                                                                                                op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                                    player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                        player.sendMessage("Trufitus goes back to his studies.")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                                            player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                op("Does she have any weaknesses?") {
                                                                    player(HeadE.CONFUSED, "Does she have any weaknesses?")
                                                                    npc(npc, HeadE.CALM_TALK, "I am not sure, but the legend about her certainly is long. It's a pity that the temple of Ah Za Rhoon has crumbled as there may be some clues that could help us to defeat her.")
                                                                    npc(npc, HeadE.CALM_TALK, "I think the largest problem will be in locating her resting place.")
                                                                    options("Select an option") {
                                                                        op("Why was it called Ah Za Rhoon?") {
                                                                            player(HeadE.CONFUSED, "Why was it called Ah Za Rhoon?")
                                                                            npc(npc, HeadE.CALM_TALK, "It is from an ancient language. The direct translation is... 'Magnificence floating on water'. But my research makes me believe that the temple was built on land. And most likely between large ")
                                                                            npc(npc, HeadE.CALM_TALK, "bodies of water, for example large lakes. However, many people have searched for the temple, and have failed. I would hate to see you waste your time on a pointless search like that.")
                                                                            options("Select an option") {
                                                                                op("Thanks for the information!") {
                                                                                    label("thanks")
                                                                                    player(HeadE.CALM_TALK, "Thanks for the information!")
                                                                                    npc(npc, HeadE.CONFUSED, "What information?")
                                                                                    player(HeadE.CALM_TALK, "About Ah Za Rhoon and where it is.") {
                                                                                        player.simpleDialogue("Trufitus looks at you blankly...")
                                                                                    }
                                                                                    npc(npc, HeadE.CALM_TALK, "Hmmm, well, you are welcome bwana.")
                                                                                    options("Select an option") {
                                                                                        op("Do you know anything more about the temple?") {
                                                                                            player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                                            npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                                            options("Select an option") {
                                                                                                op("Tell me more.") {
                                                                                                    player(HeadE.CONFUSED, "Tell me more.")
                                                                                                    npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                                    goto("more")
                                                                                                }
                                                                                                op("Are there any traps there?") {
                                                                                                    player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                                    npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") {
                                                                                                        player.sendMessage("Trufitus goes back to his studies.")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        op("I am going to search for Ah Za Rhoon!") {
                                                                                            player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                                            npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                                            npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                                            options("Select an option") {
                                                                                                op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                                    player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                                        player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                                        player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                                    }
                                                                                                }
                                                                                                op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                                    player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                        player.sendMessage("Trufitus goes back to his studies.")
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                                            player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                op("Do you know anything more about the temple?") {
                                                                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                                                    options("Select an option") {
                                                                                        op("Tell me more.") {
                                                                                            player(HeadE.CONFUSED, "Tell me more.")
                                                                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                                            goto("more")
                                                                                        }
                                                                                        op("Are there any traps there?") {
                                                                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                                                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                op("I am going to search for Ah Za Rhoon!") {
                                                                                    player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                                                    npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                                                    npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                                                    options("Select an option") {
                                                                                        op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                                            player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                                            npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                                                player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                                                player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                                            }
                                                                                        }
                                                                                        op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                                            player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") {
                                                                                                player.sendMessage("Trufitus goes back to his studies.")
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                                                    player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        op("Oh, ok!") {
                                                            player(HeadE.CALM_TALK, "Oh, ok!")
                                                            npc(npc, HeadE.SAD_MILD, "Yes, it's a bit sad really, I liked that village.") {
                                                                player.simpleDialogue("Trufitus seems deeply touched...")
                                                            }
                                                            npc(npc, HeadE.CALM_TALK, "Well, I hope you will excuse me, but I need to get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                op("Mosol Rei said something about a legend?") {
                                    label("legend")
                                    player(HeadE.CONFUSED, "Mosol Rei said something about a legend?")
                                    npc(npc, HeadE.CALM_TALK, "Ah, yes, there is a legend, but it is lost in the midst of antiquity... The last place to hold any details regarding this mystery was in the temple of Ah Za Rhoon....and that has long since vanished... it crumbled into dust...")
                                    label("more")
                                    options("Select an option") {
                                        op("Why was it called Ah Za Rhoon?") {
                                            player(HeadE.CONFUSED, "Why was it called Ah Za Rhoon?")
                                            npc(npc, HeadE.CALM_TALK, "It is from an ancient language. The direct translation is... 'Magnificence floating on water'. But my research makes me believe that the temple was built on land. And most likely between large ")
                                            npc(npc, HeadE.CALM_TALK, "bodies of water, for example large lakes. However, many people have searched for the temple, and have failed. I would hate to see you waste your time on a pointless search like that.")
                                            options("Select an option") {
                                                op("Thanks for the information!") {
                                                    label("thanks")
                                                    player(HeadE.CALM_TALK, "Thanks for the information!")
                                                    npc(npc, HeadE.CONFUSED, "What information?")
                                                    player(HeadE.CALM_TALK, "About Ah Za Rhoon and where it is.") {
                                                        player.simpleDialogue("Trufitus looks at you blankly...")
                                                    }
                                                    npc(npc, HeadE.CALM_TALK, "Hmmm, well, you are welcome bwana.")
                                                    options("Select an option") {
                                                        op("Do you know anything more about the temple?") {
                                                            player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                            npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                            options("Select an option") {
                                                                op("Tell me more.") {
                                                                    player(HeadE.CONFUSED, "Tell me more.")
                                                                    npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                                    goto("more")
                                                                }
                                                                op("Are there any traps there?") {
                                                                    player(HeadE.CONFUSED, "Are there any traps there?")
                                                                    npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                }
                                                            }
                                                        }
                                                        op("I am going to search for Ah Za Rhoon!") {
                                                            player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                            npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                            npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                            options("Select an option") {
                                                                op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                                    player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                                    npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                                    npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                        player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                        player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                                    }
                                                                }
                                                                op("Actually, now it comes to it, I'm having second thoughts.") {
                                                                    player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                                }
                                                            }
                                                        }
                                                        op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                            player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                        }
                                                    }
                                                }
                                                op("Do you know anything more about the temple?") {
                                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                    options("Select an option") {
                                                        op("Tell me more.") {
                                                            player(HeadE.CONFUSED, "Tell me more.")
                                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                            goto("more")
                                                        }
                                                        op("Are there any traps there?") {
                                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                        }
                                                    }
                                                }
                                                op("I am going to search for Ah Za Rhoon!") {
                                                    player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                    npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                    npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                    options("Select an option") {
                                                        op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                            player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                            npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                            npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                            }
                                                        }
                                                        op("Actually, now it comes to it, I'm having second thoughts.") {
                                                            player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                        }
                                                    }
                                                }
                                                op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                    player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                }
                                            }
                                        }
                                        op("Do you know anything more about the temple?") {
                                            player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                            npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                            options("Select an option") {
                                                op("Tell me more.") {
                                                    player(HeadE.CONFUSED, "Tell me more.")
                                                    npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                    goto("more")
                                                }
                                                op("Are there any traps there?") {
                                                    player(HeadE.CONFUSED, "Are there any traps there?")
                                                    npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        op("Mosol Rei said something about a legend?") {
                            label("legend")
                            player(HeadE.CONFUSED, "Mosol Rei said something about a legend?")
                            npc(npc, HeadE.CALM_TALK, "Ah, yes, there is a legend, but it is lost in the midst of antiquity... The last place to hold any details regarding this mystery was in the temple of Ah Za Rhoon....and that has long since vanished... it crumbled into dust...")
                            label("more")
                            options("Select an option") {
                                op("Why was it called Ah Za Rhoon?") {
                                    player(HeadE.CONFUSED, "Why was it called Ah Za Rhoon?")
                                    npc(npc, HeadE.CALM_TALK, "It is from an ancient language. The direct translation is... 'Magnificence floating on water'. But my research makes me believe that the temple was built on land. And most likely between large ")
                                    npc(npc, HeadE.CALM_TALK, "bodies of water, for example large lakes. However, many people have searched for the temple, and have failed. I would hate to see you waste your time on a pointless search like that.")
                                    options("Select an option") {
                                        op("Thanks for the information!") {
                                            label("thanks")
                                            player(HeadE.CALM_TALK, "Thanks for the information!")
                                            npc(npc, HeadE.CONFUSED, "What information?")
                                            player(HeadE.CALM_TALK, "About Ah Za Rhoon and where it is.") {
                                                player.simpleDialogue("Trufitus looks at you blankly...")
                                            }
                                            npc(npc, HeadE.CALM_TALK, "Hmmm, well, you are welcome bwana.")
                                            options("Select an option") {
                                                op("Do you know anything more about the temple?") {
                                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                                    options("Select an option") {
                                                        op("Tell me more.") {
                                                            player(HeadE.CONFUSED, "Tell me more.")
                                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                            goto("more")
                                                        }
                                                        op("Are there any traps there?") {
                                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                        }
                                                    }
                                                }
                                                op("I am going to search for Ah Za Rhoon!") {
                                                    player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                                    npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                                    npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                                    options("Select an option") {
                                                        op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                            player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                            npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                            npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                                player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                                player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                            }
                                                        }
                                                        op("Actually, now it comes to it, I'm having second thoughts.") {
                                                            player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                        }
                                                    }
                                                }
                                                op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                                    player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                }
                                            }
                                        }
                                        op("Do you know anything more about the temple?") {
                                            player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                            npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                            options("Select an option") {
                                                op("Tell me more.") {
                                                    player(HeadE.CONFUSED, "Tell me more.")
                                                    npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                                    goto("more")
                                                }
                                                op("Are there any traps there?") {
                                                    player(HeadE.CONFUSED, "Are there any traps there?")
                                                    npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                }
                                            }
                                        }
                                        op("I am going to search for Ah Za Rhoon!") {
                                            player(HeadE.CHEERFUL, "I am going to search for Ah Za Rhoon!")
                                            npc(npc, HeadE.CONFUSED, "What?! You must be crazy! That place has passed into myth and legend, it has been buried under rubble for years. It's most likely buried 20 men deep, and that's if you can actually find it.")
                                            npc(npc, HeadE.CONFUSED, "Are you sure you're going to go and look for it? I may be able to do some research into this if you agree. Only I don't want to waste my time if you're not serious about this!")
                                            options("Select an option") {
                                                op("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.") {
                                                    player(HeadE.CHEERFUL, "Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.")
                                                    npc(npc, HeadE.CALM_TALK, "Ok then Bwana, good luck with your quest, and remember to stock up well with adventuring supplies before setting off. You never know how useful some fairly ordinary things might be when you're adventuring.")
                                                    npc(npc, HeadE.CALM_TALK, "I'll hold on to this Wampum belt for you for the time being. I'll give it back to you when we have completed this quest.") {
                                                        player.inventory.deleteItem(Obj.mosol_wampum_belt, 1)
                                                        player.setQuestStage(Quest.SHILO_VILLAGE, 2)
                                                    }
                                                }
                                                op("Actually, now it comes to it, I'm having second thoughts.") {
                                                    player(HeadE.CONFUSED, "Actually, now it comes to it, I'm having second thoughts.")
                                                    npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                                }
                                            }
                                        }
                                        op("It's a pity that I can't search for Ah Za Rhoon now.") {
                                            player(HeadE.SAD_MILD, "It's a pity that I can't search for Ah Za Rhoon now.")
                                            npc(npc, HeadE.CALM_TALK, "Well, I understand. Perhaps you can search for it another time? Come back when you think you're ready. now won't you while I return to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                        }
                                    }
                                }
                                op("Do you know anything more about the temple?") {
                                    player(HeadE.CONFUSED, "Do you know anything more about the temple?")
                                    npc(npc, HeadE.CALM_TALK, "Not much... I would say that is about it... Even the great priest Zadimus who built the temple did not survive. Some say that Rashiliyia caused the temple to collapse. She was angry at Zadimus for not returning her affections. She was a great sorceress even before they met.")
                                    options("Select an option") {
                                        op("Tell me more.") {
                                            player(HeadE.CONFUSED, "Tell me more.")
                                            npc(npc, HeadE.SKEPTICAL, "I don't know anymore. You're very demanding aren't you!")
                                            goto("more")
                                        }
                                        op("Are there any traps there?") {
                                            player(HeadE.CONFUSED, "Are there any traps there?")
                                            npc(npc, HeadE.SKEPTICAL, "How am I supposed to know? A lot of what I know is most probably wrong but some of it seems right to me. Excuse me but I must get back to my studies.") { player.sendMessage("Trufitus goes back to his studies.") }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
                return@onItemOnNpc
            }

            else -> player.startConversation {
                npc(npc, HeadE.CALM_TALK, "I'm sorry Bwana but I just don't have a use for that!")
            }
        }
    }

    onNpcClick(Npc.trufitus, options = arrayOf("Talk-to")) { (player, npc) ->
        if (player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS || player.getQuestStage(Quest.SHILO_VILLAGE) == VERIFY_TEMPLE_OF_AH_ZA_RHOON) {
            player.startConversation {
                player(HeadE.CALM_TALK, "Greetings...")
                npc(npc, HeadE.CALM_TALK, "Greetings Bwana, you have been away! The situation with Rashiliyia is worsening! I pray that you have some good news for me.")
                player(HeadE.CALM_TALK, "I think I found the temple of Ah Za Rhoon.")
                npc(npc, HeadE.CALM_TALK, "Well that sounds great Bwana. Tell me, what did you find?")
                label("main")
                options("SELECT AN OPTION") {
                    op("I need help with Bervirius.") {
                        player(HeadE.CALM_TALK, "I need help with Bervirius.")
                        npc(npc, HeadE.CALM_TALK, "Bervirius is the son of Rashiliyia. His tomb may hold some clues as to how Rashiliyia may be defeated.")
                        goto("main")
                    }
                    op("I have some items that I need help with.") {
                        player(HeadE.CALM_TALK, "I have some items that I need help with.")
                        npc(npc, HeadE.CALM_TALK, "Well, just let me see the item and I'll help as much as I can.")
                        if (player.getQuestStage(Quest.SHILO_VILLAGE) == 3) {
                            npc(npc, HeadE.CALM_TALK, "We need to identify that the place you have found is indeed Ah Za Rhoon.")
                        }
                        if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_SHOWN_TATTERED_SCROLL")) {
                            npc(npc, HeadE.CALM_TALK, "Any scrolls or information about Rashiliyia's kin would be helpful.")
                        }
                        if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_SHOWN_CRUMPLED_SCROLL")) {
                            npc(npc, HeadE.CALM_TALK, "Have you got any items concerning Rashiliyia? If so, please show me them.")
                        }
                        if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_SHOWN_Z_CORPSE")) {
                            npc(npc, HeadE.CALM_TALK, "There must be something relating to Zadimus at the temple. Did you find anything? If so, let me see it.")
                        }
                        npc(npc, HeadE.CALM_TALK, "And best of luck!")
                        goto("main")
                    }
                    if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_BURIED_Z_CORPSE")) {
                        op("I need help with Zadimus.") {
                            player(HeadE.CALM_TALK, "I need help with Zadimus.")
                            npc(npc, HeadE.CALM_TALK, "Zadimus is a spirit yearning for freedom. Bury him in a sacred place to release his spirit.")
                            options("SELECT AN OPTION") {
                                op("Is there any sacred ground around here?") {
                                    player(HeadE.CALM_TALK, "Is there any sacred ground around here?")
                                    npc(npc, HeadE.CALM_TALK, "The ground in the centre of the village is very sacred to us. Maybe you could try there?")
                                    exec {
                                        val attribs = player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE)
                                        attribs.setB("HAS_SHOWN_Z_CORPSE", true)
                                        if (player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS && showedAhZaRhoonItems(player)) {
                                            player.setQuestStage(Quest.SHILO_VILLAGE, INVESTIGATE_B_TOMB)
                                        }
                                    }
                                }
                                op("Ok, thanks!") {
                                    player(HeadE.HAPPY_TALKING, "Ok, thanks!")
                                    npc(npc, HeadE.CALM_TALK, "You're quite welcome Bwana.")
                                }
                            }
                        }
                    }
                    op("I need help with Rashiliyia.") {
                        player(HeadE.CALM_TALK, "I need help with Rashiliyia.")
                        npc(npc, HeadE.CALM_TALK, "We need to find Rashiliyia's resting place and learn how to put her spirit to rest. You may find some clues to her resting place in Ah Za Rhoon or Bervirius' Tomb.")
                        goto("main")
                    }
                    if (player.getQuestStage(Quest.SHILO_VILLAGE) == 3) {
                        op("I need some help with the Temple of Ah Za Rhoon.") {
                            player(HeadE.CALM_TALK, "I need some help with the Temple of Ah Za Rhoon.")
                            npc(npc, HeadE.CALM_TALK, "If you have found the temple, you should search it thoroughly and see if there are any clues about Rashiliyia.")
                            if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_SHOWN_STONE_PLAQUE")) {
                                npc(npc, HeadE.CALM_TALK, "We need to identify that the place you have found is indeed Ah Za Rhoon.")
                            }
                            if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_SHOWN_TATTERED_SCROLL")) {
                                npc(npc, HeadE.CALM_TALK, "Any scrolls or information about Rashiliyia's kin would be helpful.")
                            }
                            if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_SHOWN_CRUMPLED_SCROLL")) {
                                npc(npc, HeadE.CALM_TALK, "Have you got any items concerning Rashiliyia? If so, please show me them.")
                            }
                            if (!player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_SHOWN_Z_CORPSE")) {
                                npc(npc, HeadE.CALM_TALK, "There must be something relating to Zadimus at the temple. Did you find anything? If so, let me see it.")
                            }
                            npc(npc, HeadE.CALM_TALK, "And best of luck!")
                            goto("main")
                        }
                    }
                }
            }
            return@onNpcClick
        }
        if (player.getQuestStage(Quest.SHILO_VILLAGE) == INVESTIGATE_B_TOMB || player.getQuestStage(Quest.SHILO_VILLAGE) == RETURN_TO_TRUFITUS_AGAIN) {
            player.startConversation {
                npc(npc, HeadE.CALM_TALK, "Greetings Bwana, did you find Bervirius’ Tomb?")
                options("Select an option:") {
                    if (player.getQuestManager().getAttribs(Quest.SHILO_VILLAGE).getB("HAS_FOUND_BERVIRIUS_TOMB")) {
                        op("I think I found Bervirius' Tomb.") {
                            player(HeadE.CALM_TALK, "I think I found Bervirius' Tomb.")
                            npc(npc, HeadE.CALM_TALK, "Congratulations Bwana, show me any items you have found though. I may be able to help.")
                            options {
                                op("I have some items that I need help with.") {
                                    player(HeadE.CALM_TALK, "I have some items that I need help with.")


                                    if (player.inventory.containsItem(Obj.zqberviriusscroll2)) {
                                        npc(npc, HeadE.CALM_TALK, "Hmm, these notes are quite extraordinary Bwana. They give location details of Rashiliyia's tomb, and some information on how to use the crystal.")
                                    }
                                    if (player.inventory.containsItem(Obj.zqcrystal)) {
                                        npc(npc, HeadE.CALM_TALK, "This is incredible Bwana. This will help you to locate the entrance to Rashiliyia's tomb. Simply activate it when you think you are near, and it should glow different colours to show how near you are.")

                                    }
                                    npc(npc, HeadE.CALM_TALK, "It is a very nice item Bwana. It may be just what you need to gain access to Rashiliyia's tomb.")
                                    if (player.inventory.containsItem(Obj.zqbevsword)) {
                                        npc(npc, HeadE.CALM_TALK, "My guess is that you will need some protection from Rashiliyia if you intend to enter her tomb!")
                                        npc(npc, HeadE.CALM_TALK, "Perhaps you can craft something from the pommel you found. While you were away, I did some research.")
                                        exec {
                                            player.setQuestStage(Quest.SHILO_VILLAGE, RASHILIYIAS_TOMB)
                                        }
                                    }
                                }
                                op("I didn't find anything in the tomb.") {
                                    player(HeadE.CALM_TALK, "I didn't find anything in the tomb.")
                                    npc(npc, HeadE.CALM_TALK, "Oh dear Bwana, you must have missed something. The dolmen there is usually fruitful.")
                                }
                            }
                        }
                    }
                    op("No, I didn't find a thing.") {
                        player(HeadE.CALM_TALK, "No, I didn't find a thing.")
                        npc(npc, HeadE.CALM_TALK, "Well, Bwana, you may need to search every part of the tomb carefully when you find it.")
                    }
                }
            }
            return@onNpcClick
        }
        if (player.getQuestStage(Quest.SHILO_VILLAGE) == RASHILIYIAS_TOMB || player.getQuestStage(Quest.SHILO_VILLAGE) == DEAL_WITH_RASHILIYIAS_CORPSE) {
            player.startConversation {
                player(HeadE.CALM_TALK, "Hello.")
                npc(npc, HeadE.CALM_TALK, "Greetings again Bwana. I hope that you have managed to locate Rashiliyia's Tomb. Again, if you found anything interesting, please show it to me.")
                options {
                    op("What should I do now?") {
                        player(HeadE.CALM_TALK, "What should I do now?.")
                        simple("Trufitus scratches his head.")
                        npc(npc, HeadE.CALM_TALK, "Well Bwana, if you have Rashiliyia's remains, you need to find a way to put her spirit to rest. Perhaps there was a clue with one of the artefacts that you have?")
                        npc(npc, HeadE.CALM_TALK, "Why not have a look through the artefacts that you have found and see if there is some clue that might help? If you do not have her remains, you will need to find them.")
                    }
                    op("Thanks!") {
                        player(HeadE.HAPPY_TALKING, "Thanks!")
                        npc(npc, HeadE.CALM_TALK, "You're more than welcome Bwana! Good luck for the rest of your quest.")
                    }
                }
            }
            return@onNpcClick
        }
        if (player.getQuestStage(Quest.SHILO_VILLAGE) == COMPLETED) {

            if (!player.questManager.getAttribs(Quest.SHILO_VILLAGE).getB("HAS_TALKED_TO_TRUFITUS_POST_QUEST")) {
                player.startConversation {
                    player(HeadE.CALM_TALK, "Hello.")
                    npc(npc, HeadE.CALM_TALK, "Hello Bwana. I conclude that you have been successful. Mosol sent word that the village is clearing of zombies. You have done us all a great deed!")
                    npc(npc, HeadE.CALM_TALK, "Why not go and visit him and have a look around Shilo village. You may find some interesting things there!")
                }
                player.questManager?.getAttribs(Quest.SHILO_VILLAGE)?.setB("HAS_TALKED_TO_TRUFITUS_POST_QUEST", true)
            } else {
                player.startConversation {
                    player(HeadE.CALM_TALK, "Hello.")
                    npc(npc, HeadE.CALM_TALK, "Hello again Bwana! Well Done again for helping to defeat Rashiliyia. Hopefully things will return to normal around here now.")
                }
            }
            return@onNpcClick
        }

        when (val questStage = player.getQuestStage(Quest.JUNGLE_POTION)) {
            JunglePotion.QUEST_COMPLETE -> player.startConversation {
                npc(npc, HeadE.HAPPY_TALKING, "My greatest respects, bwana. I  have communed with my gods and the future looks good for my people.")
                npc(npc, HeadE.NERVOUS, "We are happy now that the gods are not angry with us.")
                npc(npc, HeadE.CALM_TALK, "With some blessings, we will be safe here.")
                npc(npc, HeadE.CALM_TALK, "You should deliver the good news to bwana Timfraku, chief of Tai Bwo Wannai. He lives in a raised hut not far from here.")
            }
            JunglePotion.NOT_STARTED -> junglePotionIntroduction(player, npc)
            else -> JUNGLE_HERBS[questStage]?.let { junglePotionHerbDialogue(player, npc, questStage == JunglePotion.FIND_ROGUES_PURSE, it) }
        }
    }
}

private class JungleHerb(
    val grimy: Int,
    val clean: Int,
    val herbName: String,
    val greeting: String,
    val thanks: List<Pair<HeadE, String>>,
    val clue: List<Pair<HeadE, String>>
)

private const val ARDRIGAL_CLUE_1 = "The next herb is called 'ardrigal'. It is related to the palm, and grows in its brother's shady profusion."
private const val ARDRIGAL_CLUE_2 = "To the north-east you will find a small peninsula. It is just after the cliffs come down to meet the sands. That is where you should search for it."
private const val SITO_FOIL_CLUE = "Now I want you to find a herb called 'sito foil', and it grows best where the ground has been blackened by the living flame."
private const val VOLENCIA_MOSS_CLUE_1 = "The next herb is called 'volencia moss'. It clings to rocks for its existence, and is difficult to see, so you must search well for it."
private const val VOLENCIA_MOSS_CLUE_2 = "Volencia moss prefers rocks of high metal content and a frequently disturbed environment. There is some, I believe, to the south east of this village."
private const val ROGUES_PURSE_CLUE_1 = "This herb is the most difficult to find, as it inhabits the darkness of the underground."
private const val ROGUES_PURSE_CLUE_2 = "It is called 'rogue's purse', and is only to be found in pothole caverns in the northern part of this island."
private const val ROGUES_PURSE_CLUE_3 = "A secret entrance to the pothole caverns is set into the northern cliffs of this land. Take care, bwana, as it may be dangerous."
private const val SNAKE_WEED_CLUE = "The first herb that you need to gather is called 'snake weed'. It grows near vines in an area to the south-west, where the ground turns soft and the water kisses your feet."

private val JUNGLE_HERBS = mapOf(
    JunglePotion.FIND_SNAKE_WEED to JungleHerb(
        Obj.unidentified_snake_weed, Obj.snake_weed, "snake weed",
        "Greetings, bwana. Do you have the snake weed?",
        listOf(HeadE.HAPPY_TALKING to "Great! You have the snake weed. Many thanks.", HeadE.CALM_TALK to ARDRIGAL_CLUE_1, HeadE.CALM_TALK to ARDRIGAL_CLUE_2),
        listOf(HeadE.CALM_TALK to SNAKE_WEED_CLUE, HeadE.CALM_TALK to "I really need that snake weed if I am to make this potion.")
    ),
    JunglePotion.FIND_ARDRIGAL to JungleHerb(
        Obj.unidentified_ardrigal, Obj.ardrigal, "ardrigal",
        "Have you brought the ardrigal herb?",
        listOf(HeadE.HAPPY_TALKING to "Ardrigal! Wonderful. You are doing well, bwana.", HeadE.CALM_TALK to SITO_FOIL_CLUE),
        listOf(HeadE.CALM_TALK to ARDRIGAL_CLUE_1, HeadE.CALM_TALK to ARDRIGAL_CLUE_2)
    ),
    JunglePotion.FIND_SITO_FOIL to JungleHerb(
        Obj.unidentified_sito_foil, Obj.sito_foil, "sito foil",
        "Hello again, bwana. Have you been successful in getting the sito foil?",
        listOf(HeadE.HAPPY_TALKING to "Well done, bwana. Just two more herbs to collect.", HeadE.CALM_TALK to VOLENCIA_MOSS_CLUE_1, HeadE.CALM_TALK to VOLENCIA_MOSS_CLUE_2),
        listOf(HeadE.CALM_TALK to SITO_FOIL_CLUE)
    ),
    JunglePotion.FIND_VOLENCIA_MOSS to JungleHerb(
        Obj.unidentified_volencia_moss, Obj.volencia_moss, "volencia moss",
        "Greetings, bwana. Do you have the volencia moss?",
        listOf(
            HeadE.HAPPY_TALKING to "Ah, volencia moss. Beautiful. One final herb and the potion will be complete.",
            HeadE.AMAZED to ROGUES_PURSE_CLUE_1, HeadE.CALM_TALK to ROGUES_PURSE_CLUE_2, HeadE.CALM_TALK to ROGUES_PURSE_CLUE_3
        ),
        listOf(HeadE.CALM_TALK to VOLENCIA_MOSS_CLUE_1, HeadE.CALM_TALK to VOLENCIA_MOSS_CLUE_2)
    ),
    JunglePotion.FIND_ROGUES_PURSE to JungleHerb(
        Obj.unidentified_rogues_purse, Obj.rogues_purse, "rogue's purse",
        "Welcome back. Have you been successful in getting the rogue's purse, bwana?",
        listOf(
            HeadE.CALM_TALK to "Most excellent, bwana! You have returned all the herbs to me.",
            HeadE.HAPPY_TALKING to "I can finish the preparations for the potion, and at last divine with the gods. Many blessings upon you!",
            HeadE.CALM_TALK to "I must now prepare. Please excuse me while I make the arrangements."
        ),
        listOf(HeadE.AMAZED to ROGUES_PURSE_CLUE_1, HeadE.CALM_TALK to ROGUES_PURSE_CLUE_2, HeadE.CALM_TALK to ROGUES_PURSE_CLUE_3)
    )
)

private fun junglePotionHerbDialogue(player: Player, npc: NPC, finalHerb: Boolean, herb: JungleHerb) {
    player.startConversation {
        npc(npc, HeadE.CONFUSED, herb.greeting)
        options {
            op("Of course!") {
                exec { junglePotionHandOver(player, npc, finalHerb, herb) }
            }
            op("What's the clue again?") {
                herb.clue.forEach { (expression, text) -> npc(npc, expression, text) }
            }
            op("Farewell.")
        }
    }
}

private fun junglePotionHandOver(player: Player, npc: NPC, finalHerb: Boolean, herb: JungleHerb) {
    when {
        player.inventory.containsItem(herb.clean) -> player.startConversation {
            item(herb.clean, "You give the ${herb.herbName} to Trufitus.")
            exec {
                player.inventory.deleteItem(herb.clean, 1)
                if (!finalHerb) player.setQuestStage(Quest.JUNGLE_POTION, player.getQuestStage(Quest.JUNGLE_POTION) + 1)
            }
            herb.thanks.forEach { (expression, text) -> npc(npc, expression, text) }
            if (finalHerb) exec { player.questManager.completeQuest(Quest.JUNGLE_POTION) }
        }
        player.inventory.containsItem(herb.grimy) -> player.startConversation {
            npc(npc, HeadE.CONFUSED, "I don't recognise that grimy herb, bwana.")
        }
        else -> player.startConversation {
            npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "Please don't try to deceive me. I really need that ${herb.herbName} if I am to make this potion.")
        }
    }
}

private fun junglePotionQuestOffer(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.CALM_TALK, "I need to make a special brew! A potion that helps me to commune with the gods. For this potion, I need very special herbs, that are only found in the deep jungle.")
        npc(npc, HeadE.CALM_TALK, "I can only guide you so far, as the herbs are not easy to find. With some luck, you will find each herb in turn and bring it to me.")
        npc(npc, HeadE.CALM_TALK, "I will then give you details of where to find the next herb. In return for this great favour, I will give you training in Herblore.")
        questStart(Quest.JUNGLE_POTION)
        exec { player.questManager.setStage(Quest.JUNGLE_POTION, JunglePotion.FIND_SNAKE_WEED) }
        player(HeadE.HAPPY_TALKING, "It sounds like just the challenge for me. And it would make a nice break from killing things!")
        npc(npc, HeadE.CHEERFUL, "That is excellent, bwana!")
        npc(npc, HeadE.CALM_TALK, SNAKE_WEED_CLUE)
    }
}

private fun junglePotionIntroduction(player: Player, npc: NPC) {
    player.startConversation {
        npc(npc, HeadE.HAPPY_TALKING, "Greetings, bwana! I am Trufitus Shakaya of the Tai Bwo Wannai village.")
        npc(npc, HeadE.HAPPY_TALKING, "Welcome to our humble village.")
        options {
            op("What does 'bwana' mean?") {
                npc(npc, HeadE.HAPPY_TALKING, player.getPronoun("It means 'friend', gracious sir.", "Gracious lady, it means friend."))
                npc(npc, HeadE.CONFUSED, "And friends come in peace. I assume that you come in peace?")
                options {
                    op("Yes, of course I do.") {
                        npc(npc, HeadE.HAPPY_TALKING, "Well, that is good news, as I may have a proposition for you.")
                        options {
                            op("A proposition, eh? Sounds interesting.") {
                                npc(npc, HeadE.HAPPY_TALKING, "I hoped you would think so. My people are afraid to stay in the village. They have returned to the jungle and I need to commune with the gods to see what fate befalls us. You can help me by collecting some herbs that I need.")
                                exec { junglePotionQuestOffer(player, npc) }
                            }
                            op("I am sorry, but I am very busy.")
                        }
                    }
                    op("What does a warrior like me know about peace?") {
                        npc(npc, HeadE.CALM_TALK, "When you grow weary of violence and seek a more enlightened path, please pay me a visit as I may have a proposition for you. For now, I must attend to the plight of my people. Please excuse me.")
                    }
                }
            }
            op("Tai Bwo Wannai? What does that mean?") {
                npc(npc, HeadE.CALM_TALK, "It means 'small clearing in the jungle', but it is now the name of our village.")
                player(HeadE.CONFUSED, "It's a nice village, but where is everyone?")
                junglePotionHelpRequest(player, npc)
            }
            op("It's a nice village, but where is everyone?") {
                junglePotionHelpRequest(player, npc)
            }
            op("Goodbye.")
        }
    }
}

private fun DialogueBuilder.junglePotionHelpRequest(player: Player, npc: NPC) {
    npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "My people are afraid to stay in the village. They have returned to the jungle. I need to commune with the gods to see what fate befalls us.")
    npc(npc, HeadE.CONFUSED, "You may be able to help with this.")
    options {
        op("Me? How can I help?") {
            exec { junglePotionQuestOffer(player, npc) }
        }
        op("I am sorry, but I am very busy.") {
            npc(npc, HeadE.CALM_TALK, "Very well, then. May your journeys bring you much joy.")
            npc(npc, HeadE.CONFUSED, "Perhaps you will pass this way again, and then take up my proposal?")
            npc(npc, HeadE.HAPPY_TALKING, "But for now, fare thee well.")
        }
    }
}
