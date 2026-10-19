package com.rs.game.content.minigames.gnomerestaurant

import org.darkan.core.cache.NPCDefinitions
import org.darkan.core.gameval.gen.Npc

enum class GnomeCustomer(val npc: Int, val hard: Boolean, val hint: String, val thanks: String, private val title: String? = null) {
    BURKOR(Npc.aluft_gnomeknight, false, "Burkor keeps guard in the tower just south east of the Gnomeball field.", "Thank you. Can't talk now I'm on duty."),
    BRIMSTAIL(Npc.gnome_brimstail, false, "Brimstail has a cave in the Western side of the Stronghold.", "You have my gratitude, young delivery person!"),
    CAPTAIN_ERRDO(Npc.npc_3811, false, "Captain Errdo is the gnome pilot stationed upstairs.", "An excellent delivery. Thank you."),
    COACH(Npc.gnome_ball_coach, false, "You'll find Coach by the Gnomeball pitch, unsurprisingly.", "Ah, sustenance! Thanks.", "Coach"),
    DALILA(Npc.aluft_gnome_customer_two, false, "Dalila is sitting in the gnome restaurant with her long- suffering husband.", "Ah my order. At last...."),
    DAMWIN(Npc.aluft_gnome_cheerleader, false, "Damwin likes to have a drink with the Gnomeball fans after the match.", "Ooh, thanks a lot dearie."),
    EEBEL(Npc.aluft_customer_male, false, "Eebel lives in a hut in the southern end of the stronghold, near to Brimstail's cave.", "I didn't expect a human to deliver my order! Thank you very much."),
    ERMIN(Npc.aluft_customer_spinner, false, "Ermin is one of the flax spinners in the South East of the Stronghold.", "Ahhh. Thank you, I'll take few minutes from spinning to enjoy this."),
    FEMI(Npc.grandtree_femi, false, "Femi delivers goods to the Stronghold, she can usually be found just outside the Stronghold gates.", "Thanks. Yummy."),
    FROONO(Npc.aluft_gnome_child, false, "Froono likes to look down on the Magic trees in the South of the Stronghold.", "Hee Hee Hee. It worked! Thanks mister."),
    GUARD_VEMMELDO(Npc.gnome_bank_guard, false, "Guard Vemmeldo is in charge of bank security at the bank in the main part of the Stronghold.", "Thank you. Be vigilant."),
    GULLUCK(Npc.gnome_gulluck, false, "Gulluck is the local armourer, he's upstairs in the Grand Tree.", "Thanks. Hmph."),
    HECKEL_FUNCH(Npc.gnome_heckelfunch, false, "Heckel Funch runs the cocktail ingredient shop on the other side of the Grand Tree,", "Hey! Thanks a lot mate. You're a diamond."),
    KING_NARNODE(Npc.grandtree_narnode, false, "The King has a throne in the base of the Grand tree.", "Thank you my son.", "His Royal Highness King Narnode"),
    MEEGLE(Npc.aluft_gnome_terrorbird_trainer, false, "Meegle breeds Terrorbirds, in the big Terrorbird pen to the West of the Village.", "Ah, just in time for a break. Thank you."),
    PERRDUR(Npc.aluft_gnome_customer_one, false, "Perrdur is sitting in the gnome restaurant with his domineering wife Dalila.", "Thank you dear, This looks as if it might be edible for a change!"),
    ROMETTI(Npc.gnome_rometti, false, "Rometti is the gnome Master Tailor. His shop of beautiful pastel robes can be found in the North East Corner of the Grand Tree.", "Ooh, Thank you darling."),
    SARBLE(Npc.aluft_toad_handler, false, "Sarble keeps an eye on the toads and the worms near the pond.", "Thank you kindly, you might want to grab some toads and worms while you are here."),
    TRAINER_NACKLEPEN(Npc.gnome_tortoise_trainer, false, "Nacklepen is the giant tortoise trainer, he hangs around the tortoise training pen north west of the Grand Tree.", "Huzzah!"),
    WURBEL(Npc.aluft_gnomeball_fan, false, "Wurbel likes to watch the Gnomeball. He always tries to get a good view.", "Come on guys, I've got a blurberry special riding on this one. Oh my order? Thanks."),

    AMBASSADOR_FERRNOOK(Npc.aluft_gnome_diplomat_varrock, true, "Ambassador Ferrnook is attempting to get an audience with the King in Varrock.", "Excellent. A taste of home."),
    AMBASSADOR_GIMBLEWAP(Npc.aluft_gnome_diplomat_ardougne, true, "Ambassador Gimblewap is conducting negotiations in Ardougne Castle.", "Many thanks. This will help wile away the time whilst I'm waiting for an audience."),
    AMBASSADOR_SPANFIPPLE(Npc.aluft_gnome_diplomat_falador, true, "Ambassador Spanfipple is conducting negotiations in the White Knight Castle in Falador.", "Thank you kindly. I haven't got used to this human food and drink yet."),
    BRAMBICKLE(Npc.aluft_gnome_explorer_mountain, true, "Brave Brambickle is off exploring the Northern peaks of Trollweiss Mountain.", "Brrrr. This will warm me up, I'm bored of snow and Troll food."),
    CAPTAIN_BLEEMADGE(Npc.pilot_white_wolf, true, "Captain Bleemadge has the misfortune to be stationed on the top of White Wolf Mountain.", "Aahh, the smell of home, there's nothing like it. Thanks."),
    CAPTAIN_DAERKIN(Npc.aluft_pilot_daerkin, true, "Captain Daerkin is a gnome pilot, though currently off duty. You should find him somewhere in Al Kharid, he likes to watch a good fight.", "Excellent. Just what I need whilst watching the duels."),
    CAPTAIN_DALBUR(Npc.pilot_al_kharid, true, "Captain Dalbur is stationed in Al Kharid.", "Good show old chap, nothing like a reminder of home."),
    CAPTAIN_KLEMFOODLE(Npc.pilot_karamja, true, "Captain Klemfoodle is stationed on the eastern coast of Karamja.", "Very good, very good. Its good to taste real gnomish flavours."),
    CAPTAIN_NINTO(Npc.aluft_pilot_ninto, true, "Captain Ninto is an off duty gnome pilot usually stationed on White Wolf Mountain, he should be found somewhere nearby - probably the closest place that sells beer.", "Aaa shomefing ter go wiv my beer. Thish dwarf beerz quite shtrong you know..."),
    GLO_CARANOCK(Npc.mm_caranock, true, "G.L.O Caranock? I'm not sure where he is I'm afraid.", "Just hand over the order human."),
    GARKOR(Npc.mm_garkor, true, "Garkor. How interesting. I thought he was missing after a gnome glider incident. I'm sure I heard some monkeys in the background when he called.", "You found me. Excellent. I wasn't sure the order would get here."),
    GNORMADIUM_AVLAFRIM(Npc.gnormadium_avlafrim, true, "Gnormadium is all the way south in the Feldips.", "Oh my! I've had nothing to eat or drink but chompy birds and smelly water for days. Thank you."),
    HAZELMERE(Npc.grandtree_hazelmere, true, "I believe Hazelmere has a hut on an Island East of Yanille.", "*Hazelmere nods his thanks*"),
    KING_BOLREN(Npc.king_bolren, true, "King Bolren rules the Gnome Tree Village, inside the maze.", "Thank you. I don't have any gnome cooks here. I should really do something about that."),
    LIEUTENANT_SCHEPBUR(Npc.gnome_tortoise_commander, true, "Lieutenant Schepbur commands the Battle Tortoises on the Khazard Battlefield.", "Soon Gnome Cuisine will take over the world! Thanks."),
    PENWIE(Npc.aluft_gnome_explorer_jungle, true, "Penwie the explorer is investigating the Island of Karamja.", "At last a taste of home!"),
    PROFESSOR_IMBLEWYN(Npc.aluft_gnome_mage_2, true, "Professor Imblewyn is studying human magic in the Mages Guild in Yanille.", "Hmmm, I wonder... Oh. My order. Thank you - mind you don't get any food or drink on the books."),
    PROFESSOR_MANGLETHORP(Npc.aluft_gnome_inventor, true, "Professor Manglethorp is visiting some engineers in Keldagrim.", "Just what I need. Dwarven cuisine is a bit ... solid for my tastes."),
    PROFESSOR_ONGLEWIP(Npc.aluft_gnome_mage, true, "Professor Onglewip is visiting the Wizards in the Wizards Tower.", "Ah, now that's a familiar smell. Thank you."),
    WINGSTONE(Npc.aluft_gnome_explorer_safari, true, "Wingstone the explorer is surveying the Southern half of the Kharidian Desert.", "This will hit the spot. Oh yes. Thanks.");

    val displayName: String get() = title ?: NPCDefinitions.getDefs(npc).name

    companion object {
        val byNpc = entries.associateBy { it.npc }
        val easy = entries.filter { !it.hard }
        val hardOrders = entries.filter { it.hard }
    }
}
