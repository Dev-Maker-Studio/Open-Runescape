package com.rs.game.content.quests.insearchofthemyreque

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Npc

internal fun DialogueBuilder.vanstromIntro(owner: Player) {
    npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Hello there, how goes it stranger?")
    player(CALM_TALK, "Quite well, thanks for asking. How about you?")
    npc(Npc.route_vanstrom_klause_sitting, CALM_TALK, "Hmm, well, I am a little concerned about some friends of mine. They're in dire need of some assistance, but I'm at a loss as to how I can help them.")
}

internal fun DialogueBuilder.beforeVeliaf(owner: Player) {
    player(CALM_TALK, "Hello there...")
    npc(Npc.route_veliaf_hurtz, CALM_TALK, "Hello there... how did you get in here? Who are you?")
    player(CALM_TALK, "I was asked to bring you some weapons. Apparently you need them? Don't worry, I'm a friend!")
    npc(Npc.route_veliaf_hurtz, CALM_TALK, "Hmm, well, it's true we could do with some supplies. It's very good of you to bring them to us.")
    npc(Npc.route_veliaf_hurtz, CALM_TALK, "I'm sorry but I'm a bit busy at the moment; please introduce yourself to the others, then we can talk about those weapons.")
}

internal fun DialogueBuilder.beforeIvan(owner: Player) {
    npc(Npc.route_ivan_strom, CALM_TALK, "Oh--er... hello... greetings that is... sorry... you need to speak to Veliaf first. I'm not sure if I'm supposed to be talking to you.")
}

internal fun DialogueBuilder.beforePolmafi(owner: Player) {
    npc(Npc.npc_1573, CALM_TALK, "Good day. You should quickly introduce yourself to Veliaf - after that I'll be happy to have a chat with you.")
}

internal fun DialogueBuilder.beforeRadigad(owner: Player) {
    npc(Npc.npc_1572, CALM_TALK, "Greetings, friend! Once you've introduced yourself to Veliaf please feel free to chat with me...")
}

internal fun DialogueBuilder.beforeSani(owner: Player) {
    npc(Npc.route_sani_piliu, CALM_TALK, "Hi there... you're new here, aren't you? You'd best go and talk with Veliaf. But afterwards, I'd love to introduce myself properly.")
}

internal fun DialogueBuilder.beforeHarold(owner: Player) {
    npc(Npc.route_harold_evans, CALM_TALK, "Hail and well met, friend! You'd better go and introduce yourself to Veliaf. But after come and have a chat if you've a mind to.")
}

internal fun DialogueBuilder.introduceIvan(owner: Player) {
    player(CALM_TALK, "Greetings! My name is ${owner.displayName}. Veliaf said that I should introduce myself to all members of the Myreque.")
    npc(Npc.route_ivan_strom, CALM_TALK, "Veliaf says it's okay for me to talk to you. My name is Ivan, nice to meet you.")
    player(CALM_TALK, "Would you mind if I ask you a few questions?")
    npc(Npc.route_ivan_strom, CALM_TALK, "Yeah, I suppose so, but I don't know how much I can help.")
}

internal fun DialogueBuilder.questionsIvan(owner: Player) {
    label("questions")
    options {
        op("What are you doing here?") {
            player(CALM_TALK, "What are you doing here?")
            npc(Npc.route_ivan_strom, CALM_TALK, "I'm not sure really. I should really be at the temple to Saradomin, but Veliaf says that the trip's too dangerous.")
            npc(Npc.route_ivan_strom, CALM_TALK, "I am a trainee cleric of Saradomin. I hope one day to serve him as faithfully as all his priests.")
            goto("questions")
        }
        op("What can you tell me about the Myreque?") {
            player(CALM_TALK, "What can you tell me about the Myreque?")
            npc(Npc.route_ivan_strom, CALM_TALK, "Well, they're certainly brave and they're prepared to put up a fight against the evil of the Drakan clan.")
            npc(Npc.route_ivan_strom, CALM_TALK, "I'm proud to be associated with them, but with my poor skills I worry that I will let them down.")
            goto("questions")
        }
        op("What do you know about Morytania?") {
            player(CALM_TALK, "What do you know about Morytania?")
            npc(Npc.route_ivan_strom, CALM_TALK, "It's a lost land ruled by the evil Drakans. People cling to each other in fear every week when the blood tithes are to be paid.")
            npc(Npc.route_ivan_strom, CALM_TALK, "With Saradomin's help we'll bring light back to this dark land.")
            goto("questions")
        }
        op("Have you heard any rumours?") {
            player(CALM_TALK, "Have you heard any rumours?")
            npc(Npc.route_ivan_strom, CALM_TALK, "I'm sorry my friend, but I spend most of my free time in prayer to Saradomin.")
            npc(Npc.route_ivan_strom, CALM_TALK, "I prefer to cleanse my soul in case he should wish that I serve him closer.")
            goto("questions")
        }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}

internal fun DialogueBuilder.introducePolmafi(owner: Player) {
    npc(Npc.npc_1573, CALM_TALK, "Good! Now that you've introduced yourself to Veliaf and observed proper protocol,")
    npc(Npc.npc_1573, CALM_TALK, "I'll be happy to chat with you. Now, please tell me... what can I do for you?")
    player(CALM_TALK, "Do you mind if I ask a few questions?")
    npc(Npc.npc_1573, CALM_TALK, "Not at all, I'm sure I'll be able to help; and if I can't, I'll tell you so.")
}

internal fun DialogueBuilder.questionsPolmafi(owner: Player) {
    label("questions")
    options {
        op("What are you doing here?") {
            player(CALM_TALK, "What are you doing here?")
            npc(Npc.npc_1573, CALM_TALK, "I am the key advisor to Veliaf. He seeks my council on many issues before we engage the enemy.")
            npc(Npc.npc_1573, CALM_TALK, "Not that we would be doing that any time soon... the situation is somewhat difficult and terrifying at the moment.")
            npc(Npc.npc_1573, CALM_TALK, "However, we must always remember, 'All that is required for evil to survive is for good people to do nothing.'")
            goto("questions")
        }
        op("What can you tell me about the Myreque?") {
            player(CALM_TALK, "What can you tell me about the Myreque?")
            npc(Npc.npc_1573, CALM_TALK, "Well, we have a hierarchical chain of command. Veliaf reports to Calsidiu who is considered the head of the armed resistance against the Drakans.")
            npc(Npc.npc_1573, CALM_TALK, "I've also heard people talking about a spiritual man who inspires many people to join the resistance.")
            npc(Npc.npc_1573, CALM_TALK, "He's said to be very influential with supposed contacts within the Drakan clan.")
            goto("questions")
        }
        op("What do you know about Morytania?") {
            player(CALM_TALK, "What do you know about Morytania?")
            npc(Npc.npc_1573, CALM_TALK, "It has a rugged landscape, situated to the east of the River Salve. Primary occupants seem to be human in appearance,")
            npc(Npc.npc_1573, CALM_TALK, "but are in fact large vampyric predators. They maintain a stock of human villages in the same way that humans keep cattle.")
            npc(Npc.npc_1573, CALM_TALK, "Weekly blood tithes are paid to the Vampyric overlord Drakan in return for moderate safety.")
            goto("questions")
        }
        op("Have you heard any rumours?") {
            player(CALM_TALK, "Have you heard any rumours?")
            npc(Npc.npc_1573, CALM_TALK, "Yes, I hear them all the time, but they're usually neither correct nor even all that interesting.")
            npc(Npc.npc_1573, CALM_TALK, "I suspect that we will be looking to make a strike against the human sympathisers some time soon.")
            goto("questions")
        }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}

internal fun DialogueBuilder.introduceRadigad(owner: Player) {
    npc(Npc.npc_1572, CALM_TALK, "Ah, it's ${owner.displayName}. I see you've introduced yourself to Veliaf, good! Now, how can I be of service to you?")
    player(CALM_TALK, "Do you mind if I ask a few questions?")
    npc(Npc.npc_1572, CALM_TALK, "Sure, go ahead. I'll answer as best I can.")
}

internal fun DialogueBuilder.questionsRadigad(owner: Player) {
    label("questions")
    options {
        op("What's your job here?") {
            player(CALM_TALK, "What's your job here?")
            npc(Npc.npc_1572, CALM_TALK, "I'm a bit of an all-rounder and help Veliaf where I can. I've relied on his sword arm many a time, and I like to think that he can rely on mine.")
            goto("questions")
        }
        op("What can you tell me about the Myreque?") {
            player(CALM_TALK, "What can you tell me about the Myreque?")
            npc(Npc.npc_1572, CALM_TALK, "We're just developing the resistance against the Drakans and the rest of the Vampyric entourage.")
            npc(Npc.npc_1572, CALM_TALK, "We're seriously outnumbered. Veliaf doesn't like to hear us saying 'hopeless' but that's our situation at the minute.")
            goto("questions")
        }
        op("What're your plans for the future?") {
            player(CALM_TALK, "What are your plans for the future?")
            npc(Npc.npc_1572, CALM_TALK, "I'm not really sure. I guess Veliaf might know that, however I hope that more people will join the ranks and")
            npc(Npc.npc_1572, CALM_TALK, "take the fight to the dead hearts of our vampyric foe so that we might one day reclaim our right to exist.")
            goto("questions")
        }
        op("Have you heard any rumours?") {
            player(CALM_TALK, "Have you heard any rumours?")
            npc(Npc.npc_1572, CALM_TALK, "I have. Something you might find interesting is that our enigmatic leader 'Safalaan' has connections inside the Drakan Clan.")
            npc(Npc.npc_1572, CALM_TALK, "I'm not sure how true that is but it would be great!")
            goto("questions")
        }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}

internal fun DialogueBuilder.introduceSani(owner: Player) {
    npc(Npc.route_sani_piliu, CALM_TALK, "Hello... very nice to meet you! My name is Sani Piliu, what's yours?")
    player(CALM_TALK, "I am known as ${owner.displayName}; nice to meet you, Sani.")
    npc(Npc.route_sani_piliu, CALM_TALK, "The pleasure's all mine! Excuse me for being forward, but you somehow seem different to other people,")
    npc(Npc.route_sani_piliu, CALM_TALK, "somehow more determined. I sense a powerful aura... sorry, that must seem really strange?")
    player(CALM_TALK, "Well, perhaps a little; but it's nice of you to say so in any case. I'd like to ask a few questions if that's alright.")
    npc(Npc.route_sani_piliu, CALM_TALK, "Sure, go ahead, I'll answer as best I can.")
}

internal fun DialogueBuilder.questionsSani(owner: Player) {
    label("questions")
    options {
        op("Tell me a bit about yourself.") {
            player(CALM_TALK, "Tell me a bit about yourself.")
            npc(Npc.route_sani_piliu, CALM_TALK, "There's not much to tell, really. I was about nine years old when my family was ripped to pieces by a vampyre attack.")
            npc(Npc.route_sani_piliu, CALM_TALK, "One day a happy member of a loving family, the next day an orphan living a hand to mouth existence.")
            npc(Npc.route_sani_piliu, CALM_TALK, "Somehow I managed to get by, eking out a living wherever I could. People took pity on me, I earned food by helping people out.")
            npc(Npc.route_sani_piliu, CALM_TALK, "Before too long I was quite adept at helping myself, other people appreciated my skills.")
            npc(Npc.route_sani_piliu, CALM_TALK, "I bumped into Veliaf in Canifis. He saw that I would be an asset to the resistance so I joined.")
            npc(Npc.route_sani_piliu, CALM_TALK, "I want to fight back against those killers who took my family. One day I may even escape to the west across the Salve! Who knows?")
            goto("questions")
        }
        op("What can you tell me about the Myreque?") {
            player(CALM_TALK, "What can you tell me about the Myreque?")
            npc(Npc.route_sani_piliu, CALM_TALK, "As far as I know, Veliaf is one of two Lieutenants who report to another soldier called Calsidiu. While we don't advertise the fact,")
            npc(Npc.route_sani_piliu, CALM_TALK, "there are quite a lot of people in Morytania who want to put up a fight. Our current operations are merely trying to find a weakness")
            npc(Npc.route_sani_piliu, CALM_TALK, "in the Drakans' hierarchy and perhaps get a spy in there. We need to find out how to kill them before we then take the war to them.")
            player(CALM_TALK, "You make it sound easy!")
            npc(Npc.route_sani_piliu, CALM_TALK, "With determination you can achieve great things. However, Veliaf always tells us not to underestimate the enemy.")
            npc(Npc.route_sani_piliu, CALM_TALK, "After those juvenile vampyres discovered us in the forest, it's a mistake I won't be making again.")
            goto("questions")
        }
        op("What's going on in Morytania?") {
            player(CALM_TALK, "What's going on in Morytania?")
            npc(Npc.route_sani_piliu, CALM_TALK, "Lowerniel Drakan and his siblings collect blood tithes from the locals as a way of controlling the population and keeping their brood happy.")
            npc(Npc.route_sani_piliu, CALM_TALK, "Sanguinesti and Castle Drakan hold sway over all the inhabitants of Morytania. We're nothing more than an ill-organised rabble, but we're determined")
            npc(Npc.route_sani_piliu, CALM_TALK, "to fight against these evil monsters. We won't just offer our family members to the Drakans for a weekly snack, we're going to fight back!")
            goto("questions")
        }
        op("Is this your main camp?") {
            player(CALM_TALK, "Is this your main camp?")
            npc(Npc.route_sani_piliu, CALM_TALK, "It's our only camp! At least, I don't know of any others.")
            goto("questions")
        }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}

internal fun DialogueBuilder.introduceHarold(owner: Player) {
    npc(Npc.route_harold_evans, CALM_TALK, "Well met, friend! My name's Harold... nice to meet a fellow soldier. Just joined us have you?")
    player(CALM_TALK, "Not exactly! I am ${owner.displayName}. I'd like to ask a few questions if that's okay.")
    npc(Npc.route_harold_evans, CALM_TALK, "Sure, go ahead. I'll answer as best I can.")
}

internal fun DialogueBuilder.questionsHarold(owner: Player) {
    label("questions")
    options {
        op("What's your job here?") {
            player(CALM_TALK, "What's your job here?")
            npc(Npc.route_harold_evans, CALM_TALK, "I generally do what Veliaf orders. Taking a bridge, defending a tactical retreat... you name it,")
            npc(Npc.route_harold_evans, CALM_TALK, "I'm usually in the thick of it. It's funny, I seem to cheat death most of the time.")
            goto("questions")
        }
        op("What can you tell me about your squad?") {
            player(CALM_TALK, "What can you tell me about your squad?")
            npc(Npc.route_harold_evans, CALM_TALK, "They're all good soldiers, though they're pretty much a bunch of irregulars. I consider them a good squad, ready to leap into the fray if needed,")
            npc(Npc.route_harold_evans, CALM_TALK, "but we're seriously outnumbered by the Drakans and their brood. Most fights we have to improvise but this bunch are very good at that.")
            npc(Npc.route_harold_evans, CALM_TALK, "That young lad over there, Ivan Strom, he saved our bacon in the last encounter by his faith in Saradomin... he weakened the Juves and we ran for it!")
            player(CALM_TALK, "Juves? What are they?")
            npc(Npc.route_harold_evans, CALM_TALK, "It's short for 'juvenile', that's the stage that those vampyres were at in the forest. They've only recently become vampyric and")
            npc(Npc.route_harold_evans, CALM_TALK, "they're pretty inexperienced. However, they're stronger and faster than many well trained militia men, and twice as bloodthirsty.")
            goto("questions")
        }
        op("What's your grasp of the situation, soldier?") {
            player(CALM_TALK, "What's your grasp of the situation, soldier?")
            npc(Npc.route_harold_evans, CALM_TALK, "The situation is grave, it would be dangerous to think differently. We're really up against it here,")
            npc(Npc.route_harold_evans, CALM_TALK, "but we have no choice: this is a battle for our right to exist and we need to employ any tactics to achieve our goal.")
            goto("questions")
        }
        op("Have you heard any rumours?") {
            player(CALM_TALK, "Have you heard any rumours?")
            npc(Npc.route_harold_evans, CALM_TALK, "Yes, I hear them all the time. Thankfully, I don't pay any attention to them.")
            goto("questions")
        }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}

internal fun DialogueBuilder.repeatIvan(owner: Player) {
    npc(Npc.route_ivan_strom, CALM_TALK, "Oh, hello again. How are you?")
    player(CALM_TALK, "Fine, thanks. I'd like to ask a few questions if that's alright?")
}

internal fun DialogueBuilder.repeatPolmafi(owner: Player) {
    npc(Npc.npc_1573, CALM_TALK, "Warmest salutations of the day, my friend! How are you?")
    player(CALM_TALK, "Fine, thanks. I'd like to ask a few questions, if that's okay.")
    npc(Npc.npc_1573, CALM_TALK, "Not at all, I'm sure I'll be able to help; and if I can't, I'll tell you so.")
}

internal fun DialogueBuilder.repeatRadigad(owner: Player) {
    npc(Npc.npc_1572, CALM_TALK, "Hello again! Radigad at your service. Are you planning to join the fray against the Morytanian monsters and the rest of the Drakans?")
    player(CALM_TALK, "Maybe! I'd like to ask a few questions, if that's OK?")
}

internal fun DialogueBuilder.repeatSani(owner: Player) {
    npc(Npc.route_sani_piliu, CALM_TALK, "Oh hello ${owner.displayName}, nice to see you again! How's things?")
    player(CALM_TALK, "Fine thanks, do you mind if I ask a few questions?")
}

internal fun DialogueBuilder.repeatHarold(owner: Player) {
    npc(Npc.route_harold_evans, CALM_TALK, "Well met, ${owner.displayName}! Here's to your strong sword arm!")
    player(CALM_TALK, "Do you mind if I ask a few questions?")
}

internal fun DialogueBuilder.questionsVeliaf(owner: Player) {
    label("veliafQuestions")
    options {
        op("So are you the leader?") {
            player(CALM_TALK, "So are you the leader?")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "Well, as much as anyone is, it's not official or anything. I guess the others just look up to me or something.")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "I feel a certain responsibility for them so I guess that means I'm a leader of sorts.")
            goto("veliafQuestions")
        }
        op("Can you tell me about your organisation?") {
            player(CALM_TALK, "Can you tell me about your organisation?")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "Huh, organisation? That's rich! We're really not that organised, however,")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "the Myreque are dedicated to mounting a resistance against the Drakans and the evil that they've brought to Morytania.")
            goto("veliafQuestions")
        }
        op("What can you tell me about Morytania?") {
            player(CALM_TALK, "What can you tell me about Morytania?")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "I can tell you that it used to be a beautiful country before Lowerniel and his quarelling siblings arrived.")
            player(CALM_TALK, "Lowerniel? Who's that?")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "Lowerniel Vergidiyad Drakan - he's the head of the Drakan family and along with his brother Ranis and his")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "sister Vanescula they play out their petty backstabbing games using the peoples of Morytania as their pawns.")
            player(CALM_TALK, "You sound very bitter.")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "So would you, if you lost everyone dear to you at the hands of such monsters.")
            goto("veliafQuestions")
        }
        op("What are your plans for the future?") {
            player(CALM_TALK, "What are your plans for the future?")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "At the moment we're maintaining a low profile after we were spotted scouting out the Drakans' castle.")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "The forest where we camped is infested with vampyres, and even though they're only juveniles in terms of experience, we only just managed to survive.")
            npc(Npc.route_veliaf_hurtz, CALM_TALK, "After we've regrouped, we will try to recruit more people to our cause. At the minute we're hopelessly outnumbered.")
            goto("veliafQuestions")
        }
        op("Ok, thanks.") { player(CALM_TALK, "Ok, thanks.") }
    }
}

internal fun DialogueBuilder.mourningRadigad(owner: Player) {
    player(CALM_TALK, "Are you alright?")
    npc(Npc.npc_1572, CALM_TALK, "Yeah, I'm fine, thanks. You certainly proved yourself against that unearthly monster that Vanstrom conjured. I'm sorry I can't be more upbeat,")
    npc(Npc.npc_1572, CALM_TALK, "but I'm very sad about Harold and Sani. It's terrible to see your comrades cut down in combat, especially those two. They were still so very young.")
}

internal fun DialogueBuilder.mourningPolmafi(owner: Player) {
    npc(Npc.npc_1573, CALM_TALK, "I'm so sad since Sani and Harold were killed. The Myreque will be lonelier without them. Still, at least they didn't suffer.")
    player(CALM_TALK, "My commiserations for your loss. I wish that there was something I could do to help.")
    npc(Npc.npc_1573, CALM_TALK, "Don't worry. No one blames you. Anyone could have been tricked by Vanstrom.")
    npc(Npc.npc_1573, CALM_TALK, "Whilst I mourn the loss of my comrades, it serves no useful purpose to blame anyone for it.")
}

internal fun DialogueBuilder.mourningIvan(owner: Player) {
    npc(Npc.route_ivan_strom, CALM_TALK, "Oh, thank Saradomin you managed to kill that awful beast! It was so horrible seeing Sani and Harold killed right in front of my own eyes!")
    player(CALM_TALK, "I'm so sorry for your loss. If there's anything I can do, please let me know.")
    npc(Npc.route_ivan_strom, CALM_TALK, "I think you've done enough already, bringing that monster Vanstrom here.")
}
