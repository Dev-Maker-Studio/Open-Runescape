package com.rs.game.content.minigames.gnomerestaurant

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.content.skills.cooking.gnome.GnomeCocktail
import com.rs.game.content.skills.cooking.gnome.GnomeDish
import com.rs.game.content.skills.cooking.gnome.GnomeRecipe
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.interceptNpcClick
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Item

const val GNOME_RESTAURANT_LEVEL = 29

private const val ORDER = "aluftOrder"
private const val CUSTOMER = "aluftCustomer"
private const val DEADLINE = "aluftDeadline"
private const val REFUSED_EASY = "aluftRefusedEasyUntil"
private const val REFUSED_HARD = "aluftRefusedHardUntil"
private const val CREDITS = "aluftCredits"
private const val PLAYED = "aluftPlayedBefore"
private const val TIMER = "aluftTimerDeadline"
private const val DELIVERY_PENDING = "aluftTokenDelivery"

private const val MINUTE_MILLIS = 60_000L
private const val MINUTE_TICKS = 100
private const val EASY_MINUTES = 6
private const val HARD_MINUTES = 11
private const val REFUSAL_MINUTES = 5
private const val EASY_CREDITS = 1
private const val HARD_CREDITS = 3
private const val CREDITS_PER_ITEM = 12
private const val MAX_TOKEN_ITEMS = 10
private const val MAX_CREDITS = CREDITS_PER_ITEM * MAX_TOKEN_ITEMS
private const val DELIVERY_ARRIVAL_TICKS = 8
private const val DELIVERY_HANDOVER_TICKS = 5
private const val DELIVERY_LEAVE_TICKS = 5
private const val DELIVERY_FOOD_WEIGHT = 3
private const val LATE = "Your delivery is too late! Go back to Aluft jnr. to get another order. You discard your delivery box as you don't need it any more."

enum class GnomeOrder(val recipe: GnomeRecipe, val premade: Int, val delivered: Int) {
    TOAD_CRUNCHIES(GnomeDish.TOAD_CRUNCHIES, Obj.obj_2243, Obj.aluft_toad_crunchies),
    SPICY_CRUNCHIES(GnomeDish.SPICY_CRUNCHIES, Obj.obj_2241, Obj.aluft_spicy_crunchies),
    WORM_CRUNCHIES(GnomeDish.WORM_CRUNCHIES, Obj.obj_2237, Obj.aluft_worm_crunchies),
    CHOCCHIP_CRUNCHIES(GnomeDish.CHOCCHIP_CRUNCHIES, Obj.obj_2239, Obj.obj_9544),
    FRUIT_BATTA(GnomeDish.FRUIT_BATTA, Obj.obj_2225, Obj.aluft_fruit_batta),
    TOAD_BATTA(GnomeDish.TOAD_BATTA, Obj.obj_2221, Obj.aluft_toad_batta),
    WORM_BATTA(GnomeDish.WORM_BATTA, Obj.obj_2219, Obj.aluft_worm_batta),
    VEGETABLE_BATTA(GnomeDish.VEGETABLE_BATTA, Obj.premade_vegetable_batta, Obj.aluft_vegetable_batta),
    CHEESE_AND_TOMATO_BATTA(GnomeDish.CHEESE_AND_TOMATO_BATTA, Obj.premade_cheeseplustom_batta, Obj.aluft_cheeseplustom_batta),
    WORM_HOLE(GnomeDish.WORM_HOLE, Obj.premade_worm_hole, Obj.aluft_worm_hole),
    VEG_BALL(GnomeDish.VEG_BALL, Obj.premade_veg_ball, Obj.aluft_veg_ball),
    TANGLED_TOADS_LEGS(GnomeDish.TANGLED_TOADS_LEGS, Obj.premade_tangled_toads_legs, Obj.aluft_tangled_toads_legs),
    CHOCOLATE_BOMB(GnomeDish.CHOCOLATE_BOMB, Obj.premade_chocolate_bomb, Obj.aluft_chocolate_bomb),
    FRUIT_BLAST(GnomeCocktail.FRUIT_BLAST, Obj.obj_2034, Obj.obj_9514),
    PINEAPPLE_PUNCH(GnomeCocktail.PINEAPPLE_PUNCH, Obj.obj_2036, Obj.obj_9512),
    WIZARD_BLIZZARD(GnomeCocktail.WIZARD_BLIZZARD, Obj.obj_2040, Obj.obj_9508),
    SHORT_GREEN_GUY(GnomeCocktail.SHORT_GREEN_GUY, Obj.obj_2038, Obj.obj_9510),
    DRUNK_DRAGON(GnomeCocktail.DRUNK_DRAGON, Obj.obj_2032, Obj.obj_9516),
    CHOC_SATURDAY(GnomeCocktail.CHOC_SATURDAY, Obj.obj_2030, Obj.obj_9518),
    BLURBERRY_SPECIAL(GnomeCocktail.BLURBERRY_SPECIAL, Obj.obj_2028, Obj.obj_9520);

    val product get() = recipe.product
    val easy get() = recipe.level <= GNOME_RESTAURANT_LEVEL
    val drink get() = recipe is GnomeCocktail
    val label get() = Utils.addArticle(ItemDefinitions.getDefs(product).name.lowercase())
}

private var Player.order: GnomeOrder?
    get() = (get(ORDER) as? String)?.let { name -> GnomeOrder.entries.firstOrNull { it.name == name } }
    set(value) = if (value == null) delete(ORDER) else set(ORDER, value.name)

private var Player.customer: GnomeCustomer?
    get() = (get(CUSTOMER) as? String)?.let { name -> GnomeCustomer.entries.firstOrNull { it.name == name } }
    set(value) = if (value == null) delete(CUSTOMER) else set(CUSTOMER, value.name)

private var Player.credits: Int
    get() = getI(CREDITS, 0)
    set(value) = set(CREDITS, value.coerceIn(0, MAX_CREDITS))

private val Player.tokenItems get() = credits / CREDITS_PER_ITEM

private val Player.minutesLeft get() = ((getL(DEADLINE, 0) - System.currentTimeMillis() + MINUTE_MILLIS - 1) / MINUTE_MILLIS).toInt()

private fun Player.waitText(key: String): String {
    val minutes = ((getL(key, 0) - System.currentTimeMillis()) / MINUTE_MILLIS).toInt()
    return when {
        minutes > 1 -> "$minutes minutes"
        minutes == 1 -> "a minute"
        else -> "less than a minute"
    }
}

private fun Player.refused(key: String) = getL(key, 0) > System.currentTimeMillis()

private fun Player.clearOrder() {
    order = null
    customer = null
    delete(DEADLINE)
    inventory.deleteItem(Obj.aluft_delivery_box, inventory.getAmountOf(Obj.aluft_delivery_box))
}

private fun Player.lateDelivery() {
    clearOrder()
    simpleDialogue(LATE)
}

private fun Player.watchDeadline() {
    val deadline = getL(DEADLINE, 0)
    if (order == null || tempAttribs.getL(TIMER) == deadline) return
    tempAttribs.setL(TIMER, deadline)
    WorldTasks.scheduleTimer(MINUTE_TICKS, MINUTE_TICKS) {
        if (hasFinished() || order == null || getL(DEADLINE, 0) != deadline) return@scheduleTimer false
        val minutes = minutesLeft
        when {
            minutes <= 0 -> lateDelivery()
            minutes == 1 -> sendMessage("You have less than a minute to deliver your current Aluft Aloft Delivery.")
            else -> sendMessage("You have less than $minutes minutes to deliver your current Aluft Aloft Delivery.")
        }
        minutes > 0
    }
}

private fun Player.acceptOrder(order: GnomeOrder, customer: GnomeCustomer) {
    this.order = order
    this.customer = customer
    set(DEADLINE, System.currentTimeMillis() + (if (customer.hard) HARD_MINUTES else EASY_MINUTES) * MINUTE_MILLIS)
    set(PLAYED, true)
    if (!inventory.containsItem(Obj.aluft_delivery_box, 1, false)) inventory.addItemDrop(Obj.aluft_delivery_box, 1)
    watchDeadline()
}

private fun Player.offerOrder(npc: NPC, hard: Boolean) {
    val order = (if (hard) GnomeOrder.entries else GnomeOrder.entries.filter { it.easy }).random()
    val customer = (if (hard) GnomeCustomer.hardOrders else GnomeCustomer.easy).random()
    val refusedKey = if (hard) REFUSED_HARD else REFUSED_EASY
    val accepted = if (hard) "You'd better get started, you have a long way to go..." else "You'd better get a move on then. The clock is already running!"
    startConversation {
        if (hard) {
            player(HeadE.CHEERFUL, "The edge of the world is no limit for Aluft Aloft Food Deliveries!")
            npc(npc, HeadE.CHEERFUL, "That's the spirit!")
        } else player(HeadE.CHEERFUL, "I think I'll warm up with an easy one.")
        npc(npc, HeadE.CALM_TALK, "${customer.displayName} wants ${order.label}.")
        npc(npc, HeadE.CALM_TALK, customer.hint)
        if (hard) npc(npc, HeadE.CALM_TALK, "Are you willing to take this order on? If you turn it down you'll have to wait five minutes for another hard delivery.")
        options("Take this order?") {
            op(if (hard) "Easy-peasy! I'm on the job." else "Easy-peasy! I'll get started.") {
                player(HeadE.CHEERFUL, if (hard) "Easy-peasy! I'm on the job." else "Easy-peasy! I'll get started.")
                npc(npc, HeadE.CALM_TALK, accepted)
                exec { acceptOrder(order, customer) }
            }
            op(if (hard) "I won't be able to do this order." else "I don't think I can complete this one.") {
                player(HeadE.SAD_MILD, if (hard) "I won't be able to do this order." else "I don't think I can complete this one.")
                npc(
                    npc, HeadE.CALM_TALK,
                    if (hard) "Are you sure you want to turn this order down, you'll have to wait 5 minutes for another hard order, although I may be able to give you an easy one."
                    else "Are you sure you want to turn this order down, you'll have to wait 5 minutes before I can give you another easy order. I may have a hard one available for you though...",
                )
                options("Do you want to refuse this order") {
                    op("I'll take this order then!") {
                        player(HeadE.CHEERFUL, if (hard) "Easy-peasy! I'm on the job." else "I'll take this order then!")
                        npc(npc, HeadE.CALM_TALK, accepted)
                        exec { acceptOrder(order, customer) }
                    }
                    op("Yes I'm sure.") {
                        player(HeadE.CALM_TALK, "Yes I'm sure.")
                        npc(
                            npc, HeadE.FRUSTRATED,
                            if (hard) "Fine, your loss. I may have an easier job for you, since you chickened out of that one, If you want another hard one come back in five minutes and maybe I'll be able to find you a something."
                            else "Fine, your loss. If you want another easy job one come back in five minutes and maybe I'll be able to find you one.",
                        )
                        exec { set(refusedKey, System.currentTimeMillis() + REFUSAL_MINUTES * MINUTE_MILLIS) }
                    }
                }
            }
        }
    }
}

private fun Player.explainGame(npc: NPC) {
    startConversation {
        npc(npc, HeadE.CALM_TALK, "Once you are working for us, I will give you a choice of either an easy or hard job. I will then ask you to make a piece of Gnome Cuisine, and tell you who ordered it, with some information about how to find him or her. Don't take too long, the order is only valid for a short period of time.")
        npc(npc, HeadE.CALM_TALK, "If you complete the delivery successfully, you will be given a tip by the customer and credit towards your own 'free delivery token'.")
        exec { gameQuestions(npc) }
    }
}

private fun Player.gameQuestions(npc: NPC) {
    startConversation {
        options {
            op("What's the difference between an easy and a hard job?") {
                player(HeadE.CONFUSED, "What's the difference between an easy and a hard job?")
                npc(npc, HeadE.CALM_TALK, "The foods that you need to cook for the easy jobs are more straight forward than the hard ones, and the customers will be within the Gnome Stronghold, or one of the Gnome Glider Pilots on duty. You will get one credit for completing a delivery, and the tips you get will be a small amount of cash.")
                npc(npc, HeadE.CALM_TALK, "The hard jobs could require you to cook any piece of gnome cuisine or cocktail. The customers could be anywhere in Gielinor, If you are going to complete the delivery in time, you'll probably have to use some form of transport. Gnome gliders are especially useful here.")
                npc(npc, HeadE.CALM_TALK, "For the hard jobs you will be given more time to complete the delivery though. You get three credits for completing it, and the tips you get tend to be useful objects, and even a few unusual items, that you won't be able to get hold of anywhere else!")
                exec { gameQuestions(npc) }
            }
            op("What are these free delivery tokens?") {
                player(HeadE.CONFUSED, "What are these free delivery tokens?")
                npc(npc, HeadE.CALM_TALK, "I'll give you a free delivery token when you earn 12 credits. You can then use the token to make a food order yourself! This is a great privilege, we normally only deliver to gnomes. You may only have one token at a time, and they are non-transferrable.")
                npc(npc, HeadE.CALM_TALK, "If you gain more credits then for every 12 extra credits you earn, you will receive an extra item of food or drink per delivery, up to a maximum of ten items. You can continue to make deliveries after this, but you will not get any more credits - just the normal tip and cooking training.")
                npc(npc, HeadE.CALM_TALK, "Once you call for a delivery your token will be used up, and you'll have to earn another one, if you want another delivery.")
                exec { gameQuestions(npc) }
            }
            op("Will this help me with my cooking?") {
                player(HeadE.CONFUSED, "Will this help me with my cooking?")
                npc(npc, HeadE.CALM_TALK, "You will be able to develop your cooking skills during deliveries - you'll get the normal training for making the items, but also a bonus for successfully delivering the goods.")
                exec { gameQuestions(npc) }
            }
            op("I'm ready to take a job on now.") { exec { requestJob(npc) } }
            op("I've changed my mind, there's something else I need to do now.")
        }
    }
}

private fun Player.chooseJob(npc: NPC) {
    startConversation {
        options("Do you want to take on an easy or a hard delivery?") {
            op("I think I'll warm up with an easy one.") { exec { offerOrder(npc, false) } }
            op("The edge of the world is no limit for Aluft Aloft Food Deliveries!") { exec { offerOrder(npc, true) } }
            op("I could do with a reminder of how this game works.") {
                player(HeadE.CONFUSED, "I could do with a reminder of how this game works.")
                exec { explainGame(npc) }
            }
        }
    }
}

private fun Player.requestJob(npc: NPC) {
    val refusedEasy = refused(REFUSED_EASY)
    val refusedHard = refused(REFUSED_HARD)
    startConversation {
        player(HeadE.CHEERFUL, "I'm ready to take an order now...")
        when {
            !hasGnomeRestaurantTraining() && skills.getLevelForXp(Constants.COOKING) < GNOME_RESTAURANT_LEVEL -> {
                npc(npc, HeadE.CALM_TALK, "I'm afraid you don't have the required cooking experience for this job. If you want to work for me you'll have to train up to level 29 cooking, talk to my father Aluft Gianne snr. for cooking training and Blurberry the barman for cocktail making training.")
                npc(npc, HeadE.CALM_TALK, "Once I get recommendations from both of them, you'll be able to start work for me.")
            }
            !hasGnomeRestaurantTraining() -> {
                npc(npc, HeadE.CALM_TALK, "I'm afraid you don't have the required training for this job. If you want to work for me you'll have to talk to my father Aluft Gianne snr. for cooking training and Blurberry the barman for cocktail making training.")
                npc(npc, HeadE.CALM_TALK, "Once I get recommendations from both of them, you'll be able to start work for me.")
                player(HeadE.CONFUSED, "So that's Aluft Gianne snr. for cooking training, and Blurberry for Cocktail training?")
                npc(npc, HeadE.CHEERFUL, "That's right - Good luck!")
            }
            refusedEasy && refusedHard ->
                npc(npc, HeadE.CALM_TALK, "Since you've refused both the last hard and easy job I've offered you, you'll have to wait for new orders to come through - come back in ${waitText(if (getL(REFUSED_EASY, 0) < getL(REFUSED_HARD, 0)) REFUSED_EASY else REFUSED_HARD)}.")
            refusedEasy -> {
                npc(npc, HeadE.CALM_TALK, "Since you've just refused an easy job, I only have a hard job available for you at the moment. If you want to wait for another easy one - come back in ${waitText(REFUSED_EASY)}.")
                options("Do you want to take the hard job?") {
                    op("I'll give it a go.") {
                        player(HeadE.CHEERFUL, "I'll give it a go.")
                        exec { offerOrder(npc, true) }
                    }
                    op("I could do with a reminder of how this game works.") { exec { explainGame(npc) } }
                    op("I think I'll wait for another easy one.")
                }
            }
            refusedHard -> {
                npc(npc, HeadE.CALM_TALK, "Since you've just refused a hard job, I only have a easy job available for you at the moment. If you want to wait for another hard one - come back in ${waitText(REFUSED_HARD)}.")
                options("Do you want to take the easy job?") {
                    op("I'll give it a go.") {
                        player(HeadE.CHEERFUL, "I'll give it a go.")
                        exec { offerOrder(npc, false) }
                    }
                    op("I could do with a reminder of how this game works.") { exec { explainGame(npc) } }
                    op("I think I'll wait for another hard job.")
                }
            }
            else -> exec { chooseJob(npc) }
        }
    }
}

private fun Player.awardToken(npc: NPC) {
    inventory.addItemDrop(Obj.aluft_free_delivery_token, 1)
    startConversation {
        npc(npc, HeadE.CHEERFUL, "Well done. You have worked hard for Aluft Aloft Delivery, and as a reward you are now eligible for a food delivery token that you can use to get food delivered to you!")
        options("Do you want me to explain how to use the token?") {
            op("Yes, please.") {
                player(HeadE.CHEERFUL, "Yes, please.")
                npc(npc, HeadE.CALM_TALK, "If you activate your token, a delivery rider will arrive and try to deliver some food to you. This uses up your token. If you want another delivery, you will have to earn more credits from Aluft jnr.")
                npc(npc, HeadE.CALM_TALK, "You may only have one token at a time. When you first earn a delivery token (having gained 12 delivery credits, you will be able to receive a single random gnome food or cocktail as your order. If you continue to earn credits, you will receive an extra random item per 12 credits.")
                npc(npc, HeadE.CALM_TALK, "If you have enough credits to get more than 10 you can still play the game, but you will not get any more delivery credits - you may pick up a nice tip though.")
                npc(npc, HeadE.CALM_TALK, "These tokens are not transferrable by the way - this is just a bonus for hard workers!")
            }
            op("No I already know.") { player(HeadE.CALM_TALK, "No I already know.") }
        }
    }
}

private fun Player.ownsToken() = inventory.containsItem(Obj.aluft_free_delivery_token, 1, false) || bank.containsItem(Obj.aluft_free_delivery_token, 1)

private fun Player.talkToGianne(npc: NPC) {
    when {
        tokenItems > 0 && !ownsToken() && !getBool(DELIVERY_PENDING) -> awardToken(npc)
        !getBool(PLAYED) && !hasGnomeRestaurantTraining() -> startConversation {
            npc(npc, HeadE.CHEERFUL, "Hello, welcome to Aluft Aloft, the gnome food delivery service. We deliver gnome foods to customers all over Gielinor. Do you want a job? We are always looking for new staff.")
            options {
                op("Yes, tell me more.") {
                    player(HeadE.CHEERFUL, "Yes, tell me more.")
                    npc(npc, HeadE.CALM_TALK, "First of all you will need some basic Gnome Cooking and Cocktail making training. Go and talk to my Father, Aluft Gianne snr. and Blurberry, if you haven't already, and they'll tell you what you need to do.")
                    npc(npc, HeadE.CALM_TALK, "Once you have got recommendations from both of them I'd be happy to take you on at Aluft Aloft!")
                    exec { explainGame(npc) }
                }
                op("Not right now, thanks.")
            }
        }
        credits >= MAX_CREDITS -> startConversation {
            npc(npc, HeadE.CHEERFUL, "Clearly you are a very hard worker - I think you need a holiday. You have collected all the tokens I can give you. Of course, if you use the tokens up, you can come back and do some more deliveries. I can still give you a job if you're really keen, but you'll only be able to collect tips.")
            options {
                op("I'm ready to take an order now...") { exec { requestJob(npc) } }
                op("I'll pass in that case.")
            }
        }
        order != null && minutesLeft > 0 -> startConversation {
            simple("Your current customer is: ${customer?.displayName}<br>Your current order is: ${order?.label}<br>You have about $minutesLeft minutes to make your delivery.")
        }
        else -> requestJob(npc)
    }
}

private fun Player.deliver(npc: NPC, order: GnomeOrder, customer: GnomeCustomer) {
    val food = ItemDefinitions.getDefs(order.product).name.lowercase()
    val amount = if (order.drink) "a" else "some"
    when {
        minutesLeft <= 0 -> return lateDelivery()
        !inventory.containsItem(order.product, 1, false) -> return simpleDialogue(
            if (inventory.containsItem(order.premade, 1, false)) "Premade dishes will not do I'm afraid. Only hand made Gnome Cuisine is good enough for customers of Aluft Aloft."
            else "Although you have found your customer you don't appear to have brought the correct food with you! You need $amount $food!",
        )
    }
    val tip = if (customer.hard) hardTipFor(customer) else null
    val reward = tip?.roll() ?: Item(Obj.coins, Utils.random(EASY_TIP_MIN, EASY_TIP_MAX))
    val earned = if (customer.hard) HARD_CREDITS else EASY_CREDITS
    inventory.deleteItem(order.product, 1)
    clearOrder()
    credits += earned
    inventory.addItemDrop(reward)
    incrementCount("Gnome Restaurant ${if (customer.hard) "hard" else "easy"} deliveries")
    anim(Seq.aluft_player_handing_over)
    val rewardName = ItemDefinitions.getDefs(tip?.obj ?: Obj.coins).name.lowercase()
    startConversation {
        simple("You hand over your delivery of $amount $food to ${customer.displayName}.")
        npc(npc, HeadE.CHEERFUL, customer.thanks)
        if (tip != null) npc(npc, HeadE.CHEERFUL, tip.line)
        item(reward.id, tip?.given ?: "You are given ${if (reward.amount > 1) "some" else Utils.addArticle(rewardName).substringBefore(' ')} $rewardName as a tip.")
        simple(
            "You have ${credits % CREDITS_PER_ITEM} credits towards your free delivery token. " +
                if (tokenItems > 0) "You may get $tokenItems pieces of gnome food delivered to you." else "You are not eligible to get gnome food delivered to you yet.",
        )
        if (tokenItems > 0 && !ownsToken()) simple("Don't forget to pick up your free delivery token from Aluft Gianne jnr.")
    }
}

private fun Player.deliverGnomeOrder(npc: NPC): Boolean {
    val order = order ?: return false
    val customer = customer ?: return false
    if (customer.npc != npc.id || !inventory.containsItem(Obj.aluft_delivery_box, 1, false)) return false
    deliver(npc, order, customer)
    return true
}

private fun Player.callDelivery() {
    val items = tokenItems
    if (items <= 0 || getBool(DELIVERY_PENDING)) return
    inventory.deleteItem(Obj.aluft_free_delivery_token, 1)
    set(DELIVERY_PENDING, true)
    sendMessage("You have made your order, please be patient while the gnome finds you.")
    WorldTasks.schedule(DELIVERY_ARRIVAL_TICKS) {
        if (hasFinished()) return@schedule
        val rider = World.spawnNPC(Npc.aluft_delivery_mounted_terrorchic, World.findAdjacentFreeTile(tile) ?: tile, true, false, null)
        rider.spotAnim(Spotanim.terrorchick_food_delivery)
        rider.faceEntityTile(this)
        rider.forceTalk("Delivery for $displayName!!!")
        WorldTasks.schedule(DELIVERY_HANDOVER_TICKS) {
            rider.forceTalk("Here you go $displayName, your food delivery - still warm!")
            rider.anim(Seq.terrorchick_food_delivery)
            if (!hasFinished()) {
                val menu = GnomeOrder.entries.flatMap { order -> List(if (order.drink) 1 else DELIVERY_FOOD_WEIGHT) { order } }
                repeat(items) { inventory.addItemDrop(menu.random().delivered, 1) }
                credits = 0
                delete(DELIVERY_PENDING)
            }
            WorldTasks.schedule(DELIVERY_LEAVE_TICKS) { rider.finish() }
        }
    }
}

@ServerStartupEvent
fun mapGnomeRestaurant() {
    onNpcClick(Npc.npc_4572, options = arrayOf("Talk-to")) { (player, npc) -> player.talkToGianne(npc) }

    interceptNpcClick("Talk-to") { player, npc -> player.deliverGnomeOrder(npc) }

    onItemClick(Obj.aluft_delivery_box, options = arrayOf("Check")) { (player) ->
        val order = player.order
        val customer = player.customer
        if (order == null || customer == null) {
            player.inventory.deleteItem(Obj.aluft_delivery_box, 1)
            player.sendMessage("You are not currently playing the game, so you don't need this box!")
            return@onItemClick
        }
        if (player.minutesLeft <= 0) return@onItemClick player.lateDelivery()
        player.startConversation {
            simple("Your current customer is: ${customer.displayName}<br>Your current order is: ${order.label}<br>You have about ${player.minutesLeft} minutes to make your delivery.")
            options("Do you want details of where to find your customer?") {
                op("Yes.") { simple(customer.hint) }
                op("No.")
            }
        }
    }

    onItemClick(Obj.aluft_free_delivery_token, options = arrayOf("Check", "Activate")) { (player, _, option) ->
        if (option == "Check") player.simpleDialogue("You can use this token to order gnome food to deliver. You can currently order ${player.tokenItems} items.")
        else player.callDelivery()
    }

    onLogin { (player) ->
        player.delete(DELIVERY_PENDING)
        player.watchDeadline()
    }
}
