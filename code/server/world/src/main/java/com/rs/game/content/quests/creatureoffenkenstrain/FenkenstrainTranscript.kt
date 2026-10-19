package com.rs.game.content.quests.creatureoffenkenstrain

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.game.model.entity.npc.OwnedNPC
import world.gregs.voidps.type.Tile

internal fun DialogueBuilder.hired(owner: Player) {
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Looks like you're just the ${if (owner.appearance.isMale) "man" else "woman"} for the job! Welcome aboard!")
    player(CALM_TALK, "Is there anything you'd like me to do for you, sir?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Yes, there is. You're highly skilled at grave-digging, yes?")
    player(CALM_TALK, "Err...yes, that's what I said.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Excellent. Now listen carefully.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I need you to find some...stuff...for me.")
    player(CALM_TALK, "Stuff?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "That's what I said...stuff.")
    player(CALM_TALK, "What kind of stuff?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Well...dead stuff.")
    player(CALM_TALK, "Go on...")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I need you to get me enough dead body parts for me to stitch together a complete body, which I plan to bring to life.")
    player(CALM_TALK, "Right...okay...if you insist.")
}

internal fun DialogueBuilder.partsHint() {
    player(CALM_TALK, "Do you know where I could find body parts?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "The soil of Morytania is unique in its ability to preserve the bodies of the dead, which is one reason why I have chosen to carry out my experiments here.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I recommend digging up some graves in the local area. To the south-east you will find the Haunted Woods; I believe there are many graves there.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "There is also a mausoleum on an island west of this castle. I expect the bodies that are buried there to be extremely well preserved, as they were wealthy in life.")
}

internal fun DialogueBuilder.purpose() {
    player(CALM_TALK, "Why do you want to make a creature?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I came to the land of Morytania many years ago, to find a safe sanctuary for my experiments. This abandoned castle suited my purposes exactly.")
    player(CALM_TALK, "What were you experimenting in?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Oh, perfectly innocent experiments - for the good of mankind.")
    player(CALM_TALK, "Then why did you need to come to Morytania?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Enough questions, now. Get back to your work.")
}

internal fun DialogueBuilder.job() {
    player(CALM_TALK, "Will this creature put me out of a job?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "No, my friend. I have a very special purpose in mind for this creature.")
}

internal fun DialogueBuilder.gardenerHistory() {
    npc(Npc.fenk_gardener, CALM_TALK, "Oi could tell you a few things about old Fenky, yeah.")
    player(CALM_TALK, "Go on.")
    npc(Npc.fenk_gardener, CALM_TALK, "Once, this castle were full o' good folk - my friends. Fenky was just the castle doctor you know, to the Lord and the castle folk.")
    npc(Npc.fenk_gardener, CALM_TALK, "I don't know what happened to them all, but one by one they all disappeared. When they were gone a while, I went an dug graves for 'em, in the forest.")
    npc(Npc.fenk_gardener, CALM_TALK, "After a while there weren't no-one left, but the Lord, Fenkenstrain, and meself.")
    npc(Npc.fenk_gardener, CALM_TALK, "Old Fenky sent me into the Forest to dig 'im a pit. Never said what for. Then would you believe it, someone chops me head off.")
    player(CALM_TALK, "Did you see who did it, before ...")
    npc(Npc.fenk_gardener, CALM_TALK, "Before oi kicked the bucket, you mean?")
    player(CALM_TALK, "Umm...")
    npc(Npc.fenk_gardener, CALM_TALK, "Don't worry yourself, I'm not worried about bein' dead. Worse things could happen, I suppose.")
    npc(Npc.fenk_gardener, CALM_TALK, "One thing I do know is, there ain't no Lord of the castle anymore, 'cept for old Fenky. Makes you think a bit, don't it?")
}

internal fun DialogueBuilder.gardenerHead() {
    player(CALM_TALK, "What happened to your head?")
    npc(Npc.fenk_gardener, CALM_TALK, "Oi was in the old 'aunted Forest to the south, diggin' a pit for moi old maaster, old Fenkenstrain, when would you believe it, someone chops me head off. Awful bad luck, weren't it?")
    player(CALM_TALK, "Oh yes, dreadful bad luck.")
    npc(Npc.fenk_gardener, CALM_TALK, "So oi thinks to meself, I don't needs any 'ead to be gettin on with me gardenin', long as I got me hands and me spade.")
    player(CALM_TALK, "Would you show me where the place was?")
}

internal fun DialogueBuilder.gardenerName() {
    npc(Npc.fenk_gardener, CALM_TALK, "Me name? It's been a moivellous long while, mate, since I had any use for such a thing as a name.")
    player(CALM_TALK, "Don't worry, I was just trying to make conversation.")
    npc(Npc.fenk_gardener, CALM_TALK, "No, no, I can't be havin' that - I'll remember in a minute-")
    player(CALM_TALK, "Please, don't worry.")
    npc(Npc.fenk_gardener, CALM_TALK, "Lestwit, that's it! Ed Lestwit!")
}

internal fun DialogueBuilder.lightning() {
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Perfect. But I need one more thing from you - flesh and bones by themselves do not make life.")
    player(CALM_TALK, "Really?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I have honed to perfection an ancient ritual that will give life to this creature, but for this I must harness the very power of Nature.")
    player(CALM_TALK, "And what power is this?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "The power of lightning.")
    player(CALM_TALK, "Sorry, can't make lightning, you've got the wrong man-")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Silence your insolent tongue! The storm that brews overhead will create the lightning. What I need you to do is to repair the lightning conductor on the balcony above.")
    player(CALM_TALK, "Repair the lightning conductor, right. Can I have a break, soon? By law I'm entitled to 15 minutes every-")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Repair the conductor and BEGONE!!!")
}

internal fun DialogueBuilder.conductorHint(owner: Player) {
    player(CALM_TALK, "How do I repair the lightning conductor?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Oh, it would be easier to do it myself! If you find a conductor mould you should be able to cast a new one.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Remember this, ${owner.displayName}: my experiment will only work with a conductor made from silver.")
}

internal fun DialogueBuilder.mouldHint() {
    player(CALM_TALK, "Do you know where I can find a lightning conductor mould?")
    npc(Npc.fenk_gardener, CALM_TALK, "A conductive mould, you say? Let me see-")
    npc(Npc.fenk_gardener, CALM_TALK, "There used to be a bloke 'ere, sort of an 'andyman e was. Did everything round the place - fixed what was broke, swept the chimneys and the like. He would 'ave had a mould, I imagine.")
    player(CALM_TALK, "Where is he now?")
    npc(Npc.fenk_gardener, CALM_TALK, "E's dead, just like everyone else round 'ere ... 'cept for me.")
}

internal fun DialogueBuilder.revived(owner: Player) {
    player(CALM_TALK, "So did it work, then?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Yes, I'm afraid it did, ${owner.displayName} - all too well.")
    player(CALM_TALK, "I can't see it anywhere.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I tricked it into going up to the Tower, and there it remains, imprisoned.")
    player(CALM_TALK, "So the creature wasn't all you'd hoped, then?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "...oh, what have I done...")
    player(CALM_TALK, "Oh, I see, we're developing a sense of right and wrong now are we?")
    player(CALM_TALK, "Bit late for that, I'd say.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I have no control over it! It's coming to get me!")
    player(CALM_TALK, "What do you want me to do about it?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Destroy it!!! Take the key to the Tower and take back the life I never should have granted!!!")
}

internal fun DialogueBuilder.monster(owner: Player) {
    player(CALM_TALK, "I am commanded to destroy you, creature!")
    npc(Npc.fenk_creature_model, CALM_TALK, "Oh that's *hic* not very *hic* nice ...")
    player(CALM_TALK, "Are you feeling okay?")
    npc(Npc.fenk_creature_model, CALM_TALK, "Abso *hic* lutely. Never *buuurrp* better.")
    player(CALM_TALK, "You don't look very dangerous.")
    npc(Npc.fenk_creature_model, CALM_TALK, "How *hic* do I look?")
    player(CALM_TALK, "You really don't know, do you? Have a look for yourself.")
    fenkExec(owner) {
        owner.tempAttribs.getO<OwnedNPC>("fenkNpcmonster")?.let {
            it.addWalkSteps(3550, 3554, 3, false)
            it.faceTile(Tile.of(3551, 3554, 2))
        }
    }
    simple("The creature stumbles over towards the mirror, focuses upon his reflection and...")
    npc(Npc.fenk_creature_model, SCARED, "AAAAARRGGGGHHHH!")
    simple("The creature becomes instantly sober, horror all too evident in his undead eyes.")
    player(CALM_TALK, "I'm sorry. I suppose I'm partly to blame for this.")
    npc(Npc.fenk_creature_model, CALM_TALK, "No - it was him I wager - Fenkenstrain - wasn't it? He's brought me back to life!")
    player(CALM_TALK, "Who are - were - you?")
    npc(Npc.fenk_creature_model, CALM_TALK, "I was Rologarth, Lord of the North Coast - this castle was once mine. Fenkenstrain was the castle doctor.")
    player(CALM_TALK, "So the castle wasn't really abandoned when he found it?")
    npc(Npc.fenk_creature_model, CALM_TALK, "Is that what he told you? No, no, this castle was once full of people and life. Fenkenstrain advised me to sell them to the vampyres, which - I am sad to say - I did.")
    player(CALM_TALK, "I found your brain in a jar in Canifis, so he must have sold you too.")
    npc(Npc.fenk_creature_model, CALM_TALK, "Of that I will not speak. There lie memories that should rest with the dead, the living unable to bear them.")
    player(CALM_TALK, "That's it - I'm leaving this dreadful place, whether I get paid or not. Is there anything I can do for you before I leave?")
    npc(Npc.fenk_creature_model, CALM_TALK, "Only one - please stop Fenkenstrain from carrying on his experiments, once and for all, so that no other poor soul has to endure suffering such as that of my people and I.")
}

internal fun DialogueBuilder.ringHint() {
    player(CALM_TALK, "Do you know how I can stop Fenkenstrain's experiments?")
    npc(Npc.fenk_creature_model, CALM_TALK, "Take the Ring of Charos from him.")
    player(CALM_TALK, "What is this ring?")
    npc(Npc.fenk_creature_model, CALM_TALK, "It was my birthright, passed down to me through the ages, its origin forgotten.")
    npc(Npc.fenk_creature_model, CALM_TALK, "The Ring of Charos has many powers, but Fenkenstrain has bent them to his own evil purposes. Without the power of the ring, he will not be able to raise the dead from their sleep.")
    npc(Npc.fenk_creature_model, CALM_TALK, "It has one other, extremely important use - it confuses the werewolves' senses, making them believe that they smell one of their own kind. Without the ring, Fenkenstrain will be at their mercy.")
}

internal fun DialogueBuilder.defiance() {
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "So have you destroyed it?!!?")
    player(CALM_TALK, "Never, now that he has told me the truth!")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Oh my, oh my, this is exactly what I feared!")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Why did you have to pick Rologarth's brain of all brains?!?")
    player(CALM_TALK, "I'm through working for you.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "No!! I refuse to release you!! You must help me build another creature to destroy this dreadful mistake!!")
}

internal fun DialogueBuilder.rologarthPost() {
    npc(Npc.fenk_creature_model_2, CALM_TALK, "How goes it, friend?")
    player(CALM_TALK, "I stole the Ring of Charos from Fenkenstrain.")
    npc(Npc.fenk_creature_model_2, CALM_TALK, "I saw him climb up into the Tower to hide it. It doesn't matter - soon the werewolves will come for him, and his experiments will be forever ceased.")
    player(CALM_TALK, "Do you want the ring back? It is yours after all.")
    npc(Npc.fenk_creature_model_2, CALM_TALK, "No, you keep it, my friend. Werewolves hunger for the scent of live flesh - I have no need for the ring. I have my castle back, if not my soul.")
}

internal fun DialogueBuilder.doctorPost() {
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "theyrecomingtogetme theyrecomingtogetme...")
    player(CALM_TALK, "It is all you deserve. Lord Rologarth is master of this castle once more. Let him protect you - if he wants to.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "theyrecomingtogetme theyrecomingtogetme...")
}

internal fun DialogueBuilder.zombieHead() {
    player(CALM_TALK, "I have a winner for you here, Doctor! Look! It's still moving!")
    item(Obj.trick_or_treat_head, "Nobody knows the troubles I've seen...nobody knows my sorrow...")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "I can't use that head!")
    player(CALM_TALK, "Why not?")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "If that thing keeps wriggling around, I'll never get the stitches straight.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "...And who knows what problems will come up if we have an undead head on a truly dead body.")
    npc(Npc.fenk_fenkenstrain_model, CALM_TALK, "Bring me another head, one that isn't still moving")
}
