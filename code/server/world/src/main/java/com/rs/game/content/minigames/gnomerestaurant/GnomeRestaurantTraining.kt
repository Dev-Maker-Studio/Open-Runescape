package com.rs.game.content.minigames.gnomerestaurant

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.miniquests.bar_crawl.BarCrawl.BarCrawlBars.Bars.BLURBERRYS_BAR
import com.rs.game.content.miniquests.bar_crawl.BarCrawl.BarCrawlBars.isBarVisited
import com.rs.game.content.miniquests.bar_crawl.BarCrawl.BarCrawlBars.onBarCrawl
import com.rs.game.content.miniquests.bar_crawl.BarCrawl.Companion.hasCard
import com.rs.game.content.skills.cooking.gnome.GnomeCocktail
import com.rs.game.content.skills.cooking.gnome.GnomeMould
import com.rs.game.content.utils.BartenderUtils.buyBarcrawlDrink
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private const val COOKING_STAGE = "aluftCookingTraining"
private const val COCKTAIL_STAGE = "aluftCocktailTraining"
private const val TRAINED = 6
private const val FREE_DOUGH = 100

private class Lesson(val order: GnomeOrder, val tool: Int, val supplies: List<Item>, val ask: String, val handOver: String, val taste: String, val praise: String, val next: String?)

private fun items(vararg supplies: Pair<Int, Int>) = supplies.map { (obj, amount) -> Item(obj, amount) }

private val COOKING_LESSONS = listOf(
    Lesson(
        GnomeOrder.TOAD_CRUNCHIES, Obj.crunchy_tray,
        items(Obj.toads_legs to 2, Obj.gnome_spice to 1, Obj.equa_leaves to 1, Obj.gianne_dough to 1, Obj.crunchy_tray to 1),
        "So how did you get on with the toad crunchies?", "No problem, it was easy.", "You give Aluft the toad crunchies. He takes a bite...", "Not bad, not bad at all.",
        "Ok, now for something you might prefer. Try making some chocolate chip crunchies for me. They're my favourite. Here's everything you need. You can re-use the spice from the last recipe - one tin goes a long way.",
    ),
    Lesson(
        GnomeOrder.CHOCCHIP_CRUNCHIES, Obj.crunchy_tray,
        items(Obj.chocolate_bar to 2, Obj.chocolate_dust to 1, Obj.gianne_dough to 1),
        "Hello there, how did you get on.", "Here you go.", "You give Aluft the chocolate crunchies. He takes a bite...", "Yes, yes, yes, that's superb. I'm really impressed.",
        "Ok then, can you make me a toad batta? Here's what you need.",
    ),
    Lesson(
        GnomeOrder.TOAD_BATTA, Obj.batta_tin,
        items(Obj.cheese to 1, Obj.gianne_dough to 1, Obj.equa_leaves to 1, Obj.toads_legs to 1, Obj.gnome_spice to 1, Obj.batta_tin to 1),
        "So where's my toad batta?", "Here you go, that was no problem.", "You give Aluft the toad batta. He takes a bite...", "Ooh, that's some good toad. Very nice.",
        "Let's see if you can make a cheese and tomato batta next.",
    ),
    Lesson(
        GnomeOrder.CHEESE_AND_TOMATO_BATTA, Obj.batta_tin,
        items(Obj.gianne_dough to 1, Obj.gnome_spice to 1, Obj.cheese to 1, Obj.tomato to 1, Obj.equa_leaves to 1),
        "Have you made my Cheese and tomato Batta?", "Here, see what you think.", "You give Aluft the batta. He sniffs it then takes a bite...", "Hmm, that's actually really good.",
        null,
    ),
)

private val COCKTAIL_LESSONS = listOf(
    Lesson(
        GnomeOrder.FRUIT_BLAST, Obj.cocktail_shaker,
        items(Obj.lemon to 2, Obj.orange to 1, Obj.pineapple to 1, Obj.cocktail_shaker to 1, Obj.cocktail_glass_empty to 1, Obj.knife to 1),
        "So where's my Fruit Blast?", "Here you go.", "You give Blurberry the Fruit Blast. He takes a sip...", "Hmmm... not bad, not bad at all.",
        "Now can you make me a Pineapple Punch? I can give you what you need, but again I won't be able to give you any more if you make a mistake.",
    ),
    Lesson(
        GnomeOrder.PINEAPPLE_PUNCH, Obj.cocktail_shaker,
        items(Obj.pineapple to 3, Obj.orange to 2, Obj.lemon to 1, Obj.lime to 1, Obj.cocktail_glass_empty to 1),
        "How are you faring with the Pineapple Punch?", "Here you go.", "You give Blurberry the Pineapple Punch. He takes a sip...", "Not bad, not bad at all.... I can certainly sell drinks of that quality.",
        "Okay then let's try something with some real booze in it! Make me a Wizard Blizzard.",
    ),
    Lesson(
        GnomeOrder.WIZARD_BLIZZARD, Obj.cocktail_shaker,
        items(Obj.vodka to 2, Obj.gin to 1, Obj.lemon to 1, Obj.pineapple to 1, Obj.lime to 2, Obj.orange to 1, Obj.cocktail_glass_empty to 1),
        "Hello, how did it go with the Wizard Blizzard.", "Here, try some.", "You give Blurberry the Wizard Blizzard. He takes a sip...", "Well that is both Wizardy and Blizzardy. You're quite a bartender.",
        "Okay, let's test you once more. Make me a SGG, then we'll see if you have what it takes. A Short Green Guy, and don't bring me a gnome. Here's all you need.",
    ),
    Lesson(
        GnomeOrder.SHORT_GREEN_GUY, Obj.cocktail_shaker,
        items(Obj.vodka to 1, Obj.lime to 4, Obj.equa_leaves to 1, Obj.cocktail_glass_empty to 1),
        "So, have you got my Short Green Guy?", "Here you go.", "You give Blurberry the SGG. He takes a sip...", "Ooh, that's a frisky little number. Very tart, how it should be. Excellent. Well I'm happy to recommend you to the Giannes. You truly are a gnomish bartender now..",
        null,
    ),
)

fun Player.hasGnomeRestaurantTraining() = getI(COOKING_STAGE, 0) >= TRAINED && getI(COCKTAIL_STAGE, 0) >= TRAINED

private fun Player.give(supplies: List<Item>) = supplies.forEach { inventory.addItemDrop(it) }

private fun Player.holdsTool(lesson: Lesson): Boolean {
    val mould = GnomeMould.byMould[lesson.tool]
    val inUse = if (mould == null) GnomeCocktail.byMixed.keys else mould.dishes.flatMap { listOfNotNull(it.halfMade, it.unfinished) } + mould.raw + mould.halfBaked + mould.burnt
    return (inUse + lesson.tool).any { inventory.containsItem(it, 1, false) }
}

private fun Player.lesson(npc: NPC, lessons: List<Lesson>, stageKey: String, premadeRefusal: (String) -> String, graduate: () -> Unit) {
    val index = getI(stageKey, 0) - 2
    val lesson = lessons[index]
    val food = ItemDefinitions.getDefs(lesson.order.product).name
    val tool = ItemDefinitions.getDefs(lesson.tool).name.lowercase()
    startConversation {
        npc(npc, HeadE.CALM_TALK, lesson.ask)
        when {
            inventory.containsItem(lesson.order.product, 1, false) -> {
                player(HeadE.CHEERFUL, lesson.handOver)
                simple(lesson.taste)
                npc(npc, HeadE.CHEERFUL, lesson.praise)
                exec {
                    inventory.deleteItem(lesson.order.product, 1)
                    set(stageKey, getI(stageKey, 0) + 1)
                    val following = lessons.getOrNull(index + 1)
                    if (following == null) graduate()
                    else startConversation {
                        npc(npc, HeadE.CALM_TALK, lesson.next ?: "")
                        exec { give(following.supplies) }
                    }
                }
            }
            inventory.containsItem(lesson.order.premade, 1, false) -> {
                player(HeadE.CALM_TALK, "I think I've got one.")
                npc(npc, HeadE.SKEPTICAL, premadeRefusal(food))
            }
            !holdsTool(lesson) -> options {
                op("I'm afraid I've lost the $tool you gave to me.") {
                    player(HeadE.SAD_MILD, "I'm afraid I've lost the $tool you gave to me.")
                    npc(npc, HeadE.CALM_TALK, "Ok, I'll give you another one. Obviously I can't expect you to work without the proper utensils.")
                    exec { inventory.addItemDrop(lesson.tool, 1) }
                }
                op("I've not finished yet.") {
                    player(HeadE.CALM_TALK, "I've not finished yet.")
                    npc(npc, HeadE.CALM_TALK, "Ok, well let me know when you're done.")
                }
            }
            else -> {
                player(HeadE.CALM_TALK, "I've not finished yet.")
                npc(npc, HeadE.CALM_TALK, "Ok, well let me know when you're done.")
            }
        }
    }
}

private fun Player.talkToAluft(npc: NPC) {
    when (getI(COOKING_STAGE, 0)) {
        0 -> startConversation {
            player(HeadE.CHEERFUL, "Hello.")
            npc(npc, HeadE.CHEERFUL, "Well hello there. Are you hungry? If so you've come to the right place. Eat green, eat gnome cuisine. My waiter will be glad to take your order.")
            player(HeadE.CHEERFUL, "Thanks.")
            npc(npc, HeadE.CALM_TALK, "On the other hand, if you're looking for some gnome cooking training, this is also the place to be. My son Lufty is looking for staff at the moment.")
            options {
                op("No thanks, I'm no cook.") {
                    player(HeadE.CALM_TALK, "No thanks, I'm no cook.")
                    npc(npc, HeadE.CHEERFUL, "In that case, please eat and enjoy.")
                }
                op("Ok, I'll give it a go.") {
                    player(HeadE.CHEERFUL, "Ok, I'll give it a go.")
                    npc(npc, HeadE.CHEERFUL, "Well that's great. I'm Aluft Gianne, owner of this restaurant, and you are...?")
                    player(HeadE.CALM_TALK, "Most people call me $displayName.")
                    npc(npc, HeadE.CONFUSED, "Really? Sorry, how rude of me. It just takes a little while to get used to the funny names you humans have.")
                    npc(npc, HeadE.CALM_TALK, "Of course if you're going to work for me I'll have to see what you're like first. Here, take a look at our menu.")
                    item(Obj.giannes_cook_book, "Aluft hands you a cook book.")
                    npc(npc, HeadE.CALM_TALK, "When you've had a look at the book, come back here and I'll let you prepare a few dishes.")
                    player(HeadE.CALM_TALK, "Ok then.")
                    exec {
                        inventory.addItemDrop(Obj.giannes_cook_book, 1)
                        set(COOKING_STAGE, 1)
                    }
                }
            }
        }
        1 -> startConversation {
            player(HeadE.CHEERFUL, "Hi Mr. Gianne.")
            npc(npc, HeadE.CHEERFUL, "Hello $displayName. So what did you think of the book?")
            player(HeadE.SKEPTICAL, "I'm not too sure about toad's legs.")
            npc(npc, HeadE.CHEERFUL, "They're a gnome delicacy, very tasty. Chicken tastes like toad's legs you know. You'll love them. If I give you what you need, can you make me some Toad Crunchies?")
            player(HeadE.CALM_TALK, "I'll give it a go.")
            simple("Aluft gives you two pairs of toad's legs, some gnome spice, some equa leaves, some dough and a crunchy tray.")
            npc(npc, HeadE.CALM_TALK, "The dough is made according to a secret family recipe. If you need any more, I suggest you speak to Hudo the grocer. He's very reliable.")
            npc(npc, HeadE.CALM_TALK, "Let me know how you get on.")
            exec {
                give(COOKING_LESSONS.first().supplies)
                set(COOKING_STAGE, 2)
            }
        }
        in 2 until TRAINED -> lesson(npc, COOKING_LESSONS, COOKING_STAGE, { "That looks identical to the ${it.lowercase()} I cooked not five minutes ago. You'll have to prove to me that you can make one yourself if you want my recommendation." }) {
            startConversation {
                npc(npc, HeadE.CHEERFUL, "How about this? Hmmm. I think you may well be ready for real cooking. Well I'm happy to give you my recommendation to Jnr, if that's what you want.")
                player(HeadE.CHEERFUL, "Great thanks.")
                npc(npc, HeadE.CALM_TALK, "Oh, one last thing. My special dough, can be occasionally tricky to get hold of. I wouldn't want to stop you from cooking gnome delicacies for want of dough!")
                item(Obj.gianne_dough, "Aluft Gianne gives you a voucher for 100 balls of Gianne Dough.")
                npc(npc, HeadE.CHEERFUL, "That should last you for a small while eh? That's all the free dough I'm going to give you so don't waste it!")
                exec {
                    inventory.addItemDrop(Obj.cert_gianne_dough, FREE_DOUGH)
                    finishedTraining(npc)
                }
            }
        }
        else -> finishedTraining(npc)
    }
}

private fun Player.finishedTraining(npc: NPC) {
    val aluft = npc.id == Npc.aluft_gianne
    val otherDone = getI(if (aluft) COCKTAIL_STAGE else COOKING_STAGE, 0) >= TRAINED
    startConversation {
        if (otherDone) {
            npc(npc, HeadE.CHEERFUL, "${if (aluft) "Blurberry" else "Aluft Gianne Snr."} tells me that you have finished training with him, so I think you're ready to do some work for ${if (aluft) "my son " else ""}Aluft Gianne Jnr. He is in charge of ${if (aluft) "our new family venture: Gnome Food Delivery" else "the new Gnome Food Delivery service"}. There's good tips to be made if you make and deliver gnome foods - though you may have to travel all over Gielinor to fulfil some of the other orders...")
            player(HeadE.CHEERFUL, "Sounds interesting....")
            npc(npc, HeadE.CALM_TALK, "Go and have a chat with ${if (aluft) "my son" else "young"} Lufty Jnr. and he'll fill you in with details.")
        } else {
            npc(npc, HeadE.CHEERFUL, "Well your training ${if (aluft) "with me " else ""}is over $displayName. Are you interested in some work?")
            player(HeadE.CALM_TALK, "I may be...")
            npc(npc, HeadE.CALM_TALK, "I think you're almost ready to do some work for ${if (aluft) "my son " else ""}Aluft Gianne Jnr. He is in charge of ${if (aluft) "our" else "a"} new family venture: Gnome Food Delivery. There's good tips to be made if you make and deliver gnome foods - though you may have to travel all over Gielinor to fulfil some of the other orders...")
            npc(
                npc, HeadE.CALM_TALK,
                if (aluft) "However, some of the orders Aluft aloft gets are for gnome cocktails, so if you want to work for Lufty, you'll need to undergo some basic cocktail making training. Go and have a chat with Blurberry. He's not that scary..."
                else "However, you'll need some basic gnome cooking training if you want to work for Lufty. Go and have a chat with his Dad, Aluft Gianne Snr. He'll tell you what you need to do.",
            )
            player(HeadE.CHEERFUL, "Thanks for your help!")
        }
    }
}

private fun Player.talkToBlurberry(npc: NPC) {
    if (!isBarVisited(this, BLURBERRYS_BAR) && hasCard(this) && onBarCrawl(this)) {
        startConversation {
            player(HeadE.HAPPY_TALKING, "I'm doing Alfred Grimhand's Barcrawl.")
            npc(npc, HeadE.CHEERFUL, "Ah, you've come to the best stop on your list! I'll give you my famous ${BLURBERRYS_BAR.drinkName}! It'll cost you ${BLURBERRYS_BAR.price} coins.")
            exec { buyBarcrawlDrink(this@talkToBlurberry, BLURBERRYS_BAR) }
        }
        return
    }
    when (getI(COCKTAIL_STAGE, 0)) {
        0 -> startConversation {
            player(HeadE.CHEERFUL, "Hello.")
            npc(npc, HeadE.CHEERFUL, "Well hello there traveller. If you're looking for a cocktail the barman will happily make you one.")
            npc(npc, HeadE.CALM_TALK, "Or if you're looking for some training in cocktail making, then I'm your gnome! Aluft Gianne jnr. is looking for gnome cooks, barmen and barwomen to help in his new venture, so it's a useful skill to have.")
            options {
                op("No thanks, I prefer to stay this side of the bar.") { player(HeadE.CALM_TALK, "No thanks, I prefer to stay this side of the bar.") }
                op("Ok then I'll give it a go.") {
                    player(HeadE.CHEERFUL, "Ok then I'll give it a go.")
                    npc(npc, HeadE.CHEERFUL, "Excellent. It's not an easy job, I'll have to try you out first. I'm sure you'll be great though.")
                    npc(npc, HeadE.CALM_TALK, "Here, take this cocktail guide. The book tells you how to make all the cocktails we serve. I'll tell you what I need and you can make them.")
                    player(HeadE.CHEERFUL, "Sounds easy enough.")
                    npc(npc, HeadE.CALM_TALK, "Take a look at the book then come and talk to me.")
                    exec {
                        inventory.addItemDrop(Obj.cocktail_guide, 1)
                        set(COCKTAIL_STAGE, 1)
                    }
                }
            }
        }
        1 -> startConversation {
            player(HeadE.CHEERFUL, "Hello Blurberry.")
            npc(npc, HeadE.CHEERFUL, "Hi, are you ready to make your first cocktail?")
            player(HeadE.CHEERFUL, "Absolutely.")
            npc(npc, HeadE.CALM_TALK, "Ok then, to start with make a Fruit Blast. Here, you'll need these ingredients, but I'm afraid I can't give you any more if you mess up.")
            simple("Blurberry gives you two lemons, one orange, one pineapple, a cocktail shaker, a glass and a knife.")
            exec {
                give(COCKTAIL_LESSONS.first().supplies)
                set(COCKTAIL_STAGE, 2)
            }
        }
        in 2 until TRAINED -> lesson(npc, COCKTAIL_LESSONS, COCKTAIL_STAGE, { "No, I need you to make the $it yourself. I'm afraid it doesn't really work if you try to sell the bar its own drinks." }) { finishedTraining(npc) }
        else -> finishedTraining(npc)
    }
}

@ServerStartupEvent
fun mapGnomeRestaurantTraining() {
    onNpcClick(Npc.aluft_gianne, options = arrayOf("Talk-to")) { (player, npc) -> player.talkToAluft(npc) }
    onNpcClick(Npc.blurberry, options = arrayOf("Talk-to")) { (player, npc) -> player.talkToBlurberry(npc) }
}
