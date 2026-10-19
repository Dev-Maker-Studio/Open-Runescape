package com.rs.game.content.quests.murdermystery

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc

@ServerStartupEvent
fun mapMurderMysteryGossip() {
    onNpcClick(Npc.gossipy_man) { (player, _) ->
        val stage = player.getQuestStage(Quest.MURDER_MYSTERY)
        if (stage == STAGE_UNSTARTED) {
            player.startConversation {
                npc(Npc.gossipy_man, CALM_TALK, "There's some kind of commotion up at the Sinclair place I hear. Not surprising all things considered.")
            }
            return@onNpcClick
        }
        if (stage == STAGE_COMPLETE)
            return@onNpcClick
        player.startConversation {
            player(CALM_TALK, "I'm investigating the murder up at the Sinclair place.")
            npc(Npc.gossipy_man, CALM_TALK, "Murder is it? Well, I'm not really surprised...")
            options {
                op("What can you tell me about the Sinclairs?") {
                    player(CALM_TALK, "What can you tell me about the Sinclairs?")
                    npc(Npc.gossipy_man, CALM_TALK, "Well, what do you want to know?")
                    options {
                        op("Tell me about Lord Sinclair.") {
                            player(CALM_TALK, "Tell me about Lord Sinclair.")
                            npc(Npc.gossipy_man, CALM_TALK, "Old Lord Sinclair was a great man with a lot of respect in these parts. More than his worthless children have anyway.")
                            player(CALM_TALK, "His children? They have something to gain by his death?")
                            npc(Npc.gossipy_man, CALM_TALK, "Yes. You could say that. Not that I'm one to gossip.")
                        }
                        op("Why do the Sinclairs live so far from town?") {
                            player(CALM_TALK, "Why do the Sinclairs live so far from town?")
                            npc(Npc.gossipy_man, CALM_TALK, "Well, they used to live in the big castle, but old Lord Sinclair gave it up so that those strange knights could live there instead. So the king built him a new house to the North.")
                            npc(Npc.gossipy_man, CALM_TALK, "It's more cramped than his old place, but he seemed to like it. His children were furious at him for doing it though!")
                        }
                        op("What can you tell me about his sons?") {
                            player(CALM_TALK, "What can you tell me about his sons?")
                            npc(Npc.gossipy_man, CALM_TALK, "His sons eh? They all have their own skeletons in their cupboards. You'll have to be more specific. Who are you interested in exactly?")
                            options {
                                op("Tell me about Bob.") {
                                    player(CALM_TALK, "Tell me about Bob.")
                                    npc(Npc.gossipy_man, CALM_TALK, "Bob is an odd character indeed... I'm not one to gossip, but I heard Bob is addicted to Tea. He can't make")
                                    npc(Npc.gossipy_man, CALM_TALK, "it through the day without having at least 20 cups!")
                                    npc(Npc.gossipy_man, CALM_TALK, "You might not think that's such a big thing, but he has spent thousands of gold to feed his habit!")
                                    npc(Npc.gossipy_man, CALM_TALK, "At one point he stole a lot of silverware from the kitchen and pawned it just so he could afford to buy his daily tea allowance.")
                                    npc(Npc.gossipy_man, CALM_TALK, "If his father ever found out, he would be in so much trouble... he might even get disowned!")
                                }
                                op("Tell me about David.") {
                                    player(CALM_TALK, "Tell me about David.")
                                    npc(Npc.gossipy_man, CALM_TALK, "David... oh David... not many people know this, but David really has an anger problem. He's always screaming and shouting")
                                    npc(Npc.gossipy_man, CALM_TALK, "at the household servants when he's angry, and they live in a state of fear, always walking on eggshells around him, but none of them have the courage")
                                    npc(Npc.gossipy_man, CALM_TALK, "to talk to his father about his behaviour. If they did, Lord Sinclair would almost certainly")
                                    npc(Npc.gossipy_man, CALM_TALK, "kick him out of the house, as some of the servants have been there longer than he has, and he definitely has no right to treat them like he does... but I'm not one to gossip about people.")
                                }
                                op("Tell me about Frank.") {
                                    player(CALM_TALK, "Tell me about Frank.")
                                    npc(Npc.gossipy_man, CALM_TALK, "I'm not one to talk ill of people behind their back, but Frank is a real piece of work. He is an absolutely terrible gambler... he can't pass 2 dogs in the street without putting a bet on which one will bark first!")
                                    npc(Npc.gossipy_man, CALM_TALK, "He has already squandered all of his allowance, and I heard he had stolen a number of paintings of his fathers to sell to try and cover his debts, but he still owes a lot of")
                                    npc(Npc.gossipy_man, CALM_TALK, "people a lot of money. If his father ever found out, he would stop his income, and then he would be in serious trouble!")
                                }
                            }
                        }
                        op("What can you tell me about his daughters?") {
                            player(CALM_TALK, "What can you tell me about his daughters?")
                            npc(Npc.gossipy_man, CALM_TALK, "His daughters eh? They're all nasty pieces of work. which of them specifically did you want to know about?")
                            options {
                                op("Tell me about Anna.") {
                                    player(CALM_TALK, "Tell me about Anna.")
                                    npc(Npc.gossipy_man, CALM_TALK, "Anna... ah yes... Anna has 2 great loves:")
                                    npc(Npc.gossipy_man, CALM_TALK, "Sewing and gardening. But one thing she has kept secret is that she once had an affair with Stanford the gardener, and tried to get him fired when they broke up,")
                                    npc(Npc.gossipy_man, CALM_TALK, "by killing all of the flowers in the garden. If her father ever found out she had done that he would be so furious he would probably disown her.")
                                }
                                op("Tell me about Carol.") {
                                    player(CALM_TALK, "Tell me about Carol.")
                                    npc(Npc.gossipy_man, CALM_TALK, "Oh Carol... she is such a fool. You didn't hear this from me, but I heard a while ago she was conned out of a lot of money by a travelling salesman who sold her a box full")
                                    npc(Npc.gossipy_man, CALM_TALK, "of beans by telling her they were magic. But they weren't. She sold some rare books from the library to cover her debts, but")
                                    npc(Npc.gossipy_man, CALM_TALK, "her father would be incredibly annoyed if he ever found out - he might even throw her out of the house!")
                                }
                                op("Tell me about Elizabeth.") {
                                    player(CALM_TALK, "Tell me about Elizabeth.")
                                    npc(Npc.gossipy_man, CALM_TALK, "Elizabeth? Elizabeth has a strange problem... She cannot help herself, but is always stealing small objects - it's pretty sad that she is rich enough to afford to buy things, but would rather steal them instead.")
                                    npc(Npc.gossipy_man, CALM_TALK, "Now, I don't want to spread stories, but I heard she even stole a silver needle from her father that had great sentimental value for him.")
                                    npc(Npc.gossipy_man, CALM_TALK, "He was devastated when it was lost, and cried for a week thinking he had lost it!")
                                    npc(Npc.gossipy_man, CALM_TALK, "If he ever found out that it was her who had stolen it he would go absolutely mental, maybe even disowning her!")
                                }
                            }
                        }
                    }
                }
                op("Who do you think was responsible?") {
                    player(CALM_TALK, "Who do you think was responsible?")
                    npc(Npc.gossipy_man, CALM_TALK, "Well, I guess it could have been an intruder, but with that big guard dog of theirs I seriously doubt it. I suspect it was someone closer to home...")
                    npc(Npc.gossipy_man, CALM_TALK, "Especially as I heard that the poison salesman in the Seers' village made a big sale to one of the family the other day.")
                }
                op("I think the butler did it.") {
                    player(CALM_TALK, "I think the butler did it.")
                    npc(Npc.gossipy_man, CALM_TALK, "And I think you've been reading too many cheap detective novels. Hobbes is kind of uptight, but his loyalty to old Lord Sinclair is beyond question.")
                }
                op("I am so confused about who did it.") {
                    player(CALM_TALK, "I am so confused about who did it...")
                    player(CALM_TALK, "Think you could give me any hints?")
                    when ((1..5).random()) {
                        1 -> {
                            npc(Npc.gossipy_man, CALM_TALK, "I don't know how much help this is to you but my dad was in the guard once, and he told me that the marks on your hands are totally unique. He called them 'finger prints'.")
                            npc(Npc.gossipy_man, CALM_TALK, "He said you can find them on any shiny metallic surface by using a fine powder to mark out where the marks are, and then using some sticky paper to lift the print from the object.")
                            npc(Npc.gossipy_man, CALM_TALK, "I bet if you could find a way to get everyone's 'finger prints' you could solve the crime pretty easily!")
                        }
                        2 -> {
                            npc(Npc.gossipy_man, CALM_TALK, "My father used to be in the guard. He always wrote himself notes on a piece of paper so he could keep track of information easily.")
                            npc(Npc.gossipy_man, CALM_TALK, "Maybe you should try that? Don't forget to thank me if I help you solve the case!")
                        }
                        3 -> npc(Npc.gossipy_man, CALM_TALK, "Well, I don't know how much help this is, but I heard that their guard dog will bark loudly at anyone it doesn't recognise. Maybe you should find out if anyone heard anything suspicious?")
                        4 -> {
                            npc(Npc.gossipy_man, CALM_TALK, "Well, I don't know if it's related, but I heard from that Poison Salesman in town that he sold some poison to one of the Sinclair family")
                            npc(Npc.gossipy_man, CALM_TALK, "the other day. I don't think he has any stock left now though...")
                        }
                        5 -> {
                            npc(Npc.gossipy_man, CALM_TALK, "Well, this might be of some help to you. My father was in the guards when he was younger,")
                            npc(Npc.gossipy_man, CALM_TALK, "and he always said that there isn't a crime that can't be solved through careful examination of the crime scene and all surrounding areas.")
                        }
                    }
                }
            }
        }
    }
}
