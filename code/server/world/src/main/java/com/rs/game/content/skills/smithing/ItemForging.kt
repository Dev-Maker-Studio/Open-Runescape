package com.rs.game.content.skills.smithing

import com.rs.engine.dialogue.Dialogue
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.Constants
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

private const val SHARD_LEVEL = 80
private const val SHARD_XP = 100.0
private const val SHARD_REFUSAL = "You need 80 Smithing to work with that metal."
private const val SHARDS_JOINED = "You join the shards together."
private const val SIGIL_SMITHING = 85
private const val SIGIL_PRAYER = 90
private const val SIGIL_XP = 1800.0
private const val SIGIL_REFUSAL = "You need 85 Smithing and 90 Prayer to attach the sigil to the spirit shield."
private const val BLESS_PRAYER = 85
private const val BLESS_XP = 1500.0

private class Forgeable(val first: Int, val second: Int, val product: Int, val smithing: Int, val xp: Double, val refusal: String, val made: String, val prayer: Int = 0)

private fun shards(first: Int, second: Int, product: Int) = Forgeable(first, second, product, SHARD_LEVEL, SHARD_XP, SHARD_REFUSAL, SHARDS_JOINED)

private fun sigil(sigil: Int, product: Int, name: String) =
    Forgeable(sigil, Obj.sum2_spirit_shield_blessed, product, SIGIL_SMITHING, SIGIL_XP, SIGIL_REFUSAL, "You successfully attach the $name sigil to the blessed spirit shield.", SIGIL_PRAYER)

private val FORGEABLES = listOf(
    shards(Obj.godwars_godsword_blade1, Obj.godwars_godsword_blade2, Obj.godwars_godsword_blade1plus2),
    shards(Obj.godwars_godsword_blade2, Obj.godwars_godsword_blade3, Obj.godwars_godsword_blade2plus3),
    shards(Obj.godwars_godsword_blade1, Obj.godwars_godsword_blade3, Obj.godwars_godsword_blade1plus3),
    shards(Obj.godwars_godsword_blade1plus2, Obj.godwars_godsword_blade3, Obj.godwars_godsword_blade1plus2plus3),
    shards(Obj.godwars_godsword_blade2plus3, Obj.godwars_godsword_blade1, Obj.godwars_godsword_blade1plus2plus3),
    shards(Obj.godwars_godsword_blade1plus3, Obj.godwars_godsword_blade2, Obj.godwars_godsword_blade1plus2plus3),
    Forgeable(
        Obj.dragonfire_visage, Obj.antidragonbreathshield, Obj.dragonfire_shield_uncharged, 90, 2000.0,
        "You need 90 Smithing to fuse the visage to the shield.",
        "You set to work, trying to attach the ancient draconic visage to your anti-dragonbreath shield. It's not easy to work with the ancient artifact and it takes all of your skill as a master smith.",
    ),
    Forgeable(
        Obj.dragonshield_a, Obj.dragonshield_b, Obj.dragon_sq_shield, 60, 75.0,
        "You need 60 Smithing to work with the dragon metal.",
        "Even for an experienced armorer it is not an easy task, but eventually it is ready. You have restored the dragon square shield to its former glory.",
    ),
    sigil(Obj.sum2_sigil_spectral, Obj.sum2_spirit_shield_spectral, "spectral"),
    sigil(Obj.sum2_sigil_arcane, Obj.sum2_spirit_shield_arcane, "arcane"),
    sigil(Obj.sum2_sigil_elysian, Obj.sum2_spirit_shield_elysian, "elysian"),
    sigil(Obj.sum2_sigil_divine, Obj.sum2_spirit_shield_divine, "divine"),
)

private val ALTARS = arrayOf<Any>(Loc.loc_409, Loc.loc_2640, Loc.regicide_voyage_temple_altar, Loc.altarv2, Loc.loc_24343, Loc.loc_26287, Loc.loc_27661, Loc.opn_oratory_alter)

@ServerStartupEvent
fun mapItemForging() {
    onItemOnObject(arrayOf("Anvil"), FORGEABLES.flatMap { listOf(it.first, it.second) }.distinct().toTypedArray<Any>()) { (player) ->
        val creatable = FORGEABLES.filter { it.heldBy(player) }
        if (creatable.isEmpty()) {
            player.sendMessage("You don't have all the items required to fix this.")
            return@onItemOnObject
        }
        val makeX = Dialogue().addMakeX(creatable.map { it.product }.toIntArray())
        for (item in creatable) makeX.addNext(Runnable { item.forge(player) })
        player.startConversation(makeX)
    }

    onItemOnObject(ALTARS, arrayOf<Any>(Obj.sum2_holy_elixir, Obj.sum2_spirit_shield)) { (player) ->
        if (!player.inventory.containsItems(Obj.sum2_holy_elixir, Obj.sum2_spirit_shield)) {
            player.sendMessage("You need a spirit shield and a holy elixir to create a blessed spirit shield.")
            return@onItemOnObject
        }
        if (!player.isQuestComplete(Quest.SUMMERS_END, "to bless a spirit shield.")) return@onItemOnObject
        if (player.skills.getLevel(Constants.PRAYER) < BLESS_PRAYER) {
            player.sendMessage("You need $BLESS_PRAYER Prayer to bless a spirit shield.")
            return@onItemOnObject
        }
        player.sendMessage("You bless the spirit shield.")
        player.anim(Seq.human_pray)
        player.inventory.deleteItem(Obj.sum2_holy_elixir, 1)
        player.inventory.deleteItem(Obj.sum2_spirit_shield, 1)
        player.inventory.addItemDrop(Obj.sum2_spirit_shield_blessed, 1)
        player.skills.addXp(Constants.PRAYER, BLESS_XP)
    }
}

private fun Forgeable.heldBy(player: Player) = player.inventory.containsItem(first) && player.inventory.containsItem(second)

private fun Forgeable.forge(player: Player) {
    if (!heldBy(player)) return
    if (player.skills.getLevel(Constants.SMITHING) < smithing || player.skills.getLevel(Constants.PRAYER) < prayer) return player.sendMessage(refusal)
    if (!player.inventory.containsItem(Obj.hammer)) return player.sendMessage("You need a hammer to work the metal with.")
    player.anim(Seq.human_smithing)
    player.inventory.deleteItem(first, 1)
    player.inventory.deleteItem(second, 1)
    player.inventory.addItem(product, 1)
    player.itemDialogue(product, made)
    player.skills.addXp(Constants.SMITHING, xp)
}
