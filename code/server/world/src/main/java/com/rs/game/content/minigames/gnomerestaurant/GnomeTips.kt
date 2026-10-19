package com.rs.game.content.minigames.gnomerestaurant

import com.rs.game.content.minigames.gnomerestaurant.GnomeCustomer.BRAMBICKLE
import com.rs.game.content.minigames.gnomerestaurant.GnomeCustomer.CAPTAIN_DAERKIN
import com.rs.game.content.minigames.gnomerestaurant.GnomeCustomer.CAPTAIN_NINTO
import com.rs.game.content.minigames.gnomerestaurant.GnomeCustomer.PENWIE
import com.rs.game.content.minigames.gnomerestaurant.GnomeCustomer.PROFESSOR_IMBLEWYN
import com.rs.game.content.minigames.gnomerestaurant.GnomeCustomer.PROFESSOR_MANGLETHORP
import com.rs.game.content.minigames.gnomerestaurant.GnomeCustomer.PROFESSOR_ONGLEWIP
import com.rs.game.content.minigames.gnomerestaurant.GnomeCustomer.WINGSTONE
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.Item

private const val COMMON = 200
private const val UNCOMMON = 100
private const val RARE = 50
private const val NEST = 100
private const val KEY_HALF = 92
private const val CUSTOMER_SPECIAL = 115
private const val PILOT_UNIQUE = 153

private const val DEFAULT_LINE = "You must have had to travel a long way to get this to me. Here's a little something extra as a tip."
private const val SEED_POD_LINE = "Here, take these Grand Seed pods. Very useful in emergencies!"

const val EASY_TIP_MIN = 2
const val EASY_TIP_MAX = 500

class GnomeTip(val obj: Int, val min: Int, val max: Int, val weight: Int, val noted: Boolean = false, val only: Set<GnomeCustomer>? = null, val line: String = DEFAULT_LINE, val given: String? = null) {
    fun roll(): Item {
        val amount = Utils.random(min, max)
        val id = if (noted && amount > 1) Item(obj).definitions.noteId.takeIf { it > 0 } ?: obj else obj
        return Item(id, amount)
    }
}

private val PILOTS = setOf(CAPTAIN_NINTO, CAPTAIN_DAERKIN)
private val PROFESSORS = setOf(PROFESSOR_IMBLEWYN, PROFESSOR_ONGLEWIP)
private val EXPLORERS = setOf(WINGSTONE, PENWIE, BRAMBICKLE)

private val HARD_TIPS = listOf(
    GnomeTip(Obj.aluft_gnome_goggles, 1, 1, PILOT_UNIQUE, only = PILOTS, line = "I have some spare goggles, you might as well have them.", given = "You are given a pair of pilot's goggles as a tip."),
    GnomeTip(Obj.aluft_gnome_scarf, 1, 1, PILOT_UNIQUE, only = PILOTS, line = "Here you go, have a dashing pilot's scarf.", given = "You are given a gnome pilot's scarf as a tip."),
    GnomeTip(Obj.ball_gnomeball_game, 1, 1, COMMON),
    GnomeTip(Obj.aluft_seed_pod, 5, 5, CUSTOMER_SPECIAL, only = EXPLORERS + PROFESSOR_MANGLETHORP, line = SEED_POD_LINE),
    GnomeTip(Obj.aluft_gnome_mint_cake, 1, 1, CUSTOMER_SPECIAL, only = EXPLORERS),
    GnomeTip(Obj.toadflax, 3, 3, COMMON, noted = true),
    GnomeTip(Obj.snapdragon, 1, 1, COMMON),
    GnomeTip(Obj.eadgar_goutweed_herb, 1, 1, CUSTOMER_SPECIAL, only = setOf(BRAMBICKLE)),
    GnomeTip(Obj.uncut_sapphire, 6, 16, COMMON, noted = true),
    GnomeTip(Obj.uncut_emerald, 3, 5, COMMON, noted = true),
    GnomeTip(Obj.uncut_ruby, 2, 4, COMMON, noted = true),
    GnomeTip(Obj.uncut_diamond, 1, 1, COMMON),
    GnomeTip(Obj.uncut_jade, 2, 3, COMMON, noted = true),
    GnomeTip(Obj.uncut_opal, 3, 3, COMMON, noted = true),
    GnomeTip(Obj.uncut_red_topaz, 1, 1, COMMON),
    GnomeTip(Obj.lawrune, 7, 18, RARE, only = PROFESSORS),
    GnomeTip(Obj.naturerune, 9, 15, RARE, only = PROFESSORS),
    GnomeTip(Obj.chaosrune, 15, 27, RARE, only = PROFESSORS),
    GnomeTip(Obj.cosmicrune, 10, 19, RARE, only = PROFESSORS),
    GnomeTip(Obj.deathrune, 7, 13, RARE, only = PROFESSORS),
    GnomeTip(Obj.bloodrune, 6, 10, RARE, only = PROFESSORS),
    GnomeTip(Obj.soulrune, 5, 11, RARE, only = PROFESSORS),
    GnomeTip(Obj.blankrune_high, 51, 97, RARE, noted = true, only = PROFESSORS),
    GnomeTip(Obj.xbows_crossbow_bolts_mithril, 16, 35, COMMON),
    GnomeTip(Obj.xbows_crossbow_bolts_adamantite, 12, 26, UNCOMMON),
    GnomeTip(Obj.obj_9144, 7, 15, RARE),
    GnomeTip(Obj.obj_5073, 1, 1, NEST),
    GnomeTip(Obj.raw_oomlie, 1, 1, CUSTOMER_SPECIAL, only = setOf(PENWIE)),
    GnomeTip(Obj.snake_flute, 1, 1, CUSTOMER_SPECIAL, only = setOf(WINGSTONE)),
    GnomeTip(Obj.keyhalf2, 1, 1, KEY_HALF),
    GnomeTip(Obj.keyhalf1, 1, 1, KEY_HALF),
    GnomeTip(Obj.gold_ore, 6, 11, CUSTOMER_SPECIAL, noted = true, only = setOf(PROFESSOR_MANGLETHORP), line = "Here have this, I traded for it with some of the dwarves."),
    GnomeTip(Obj.coal, 12, 22, CUSTOMER_SPECIAL, noted = true, only = setOf(PROFESSOR_MANGLETHORP), line = "Here have this, I traded for it with some of the dwarves."),
)

fun hardTipFor(customer: GnomeCustomer): GnomeTip {
    val available = HARD_TIPS.filter { it.only == null || customer in it.only }
    var roll = Utils.random(available.sumOf { it.weight })
    return available.first { roll -= it.weight; roll < 0 }
}
