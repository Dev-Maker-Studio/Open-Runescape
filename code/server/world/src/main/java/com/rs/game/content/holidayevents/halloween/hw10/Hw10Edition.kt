package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.EmotesManager.Emote
import org.darkan.core.Utils
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.startConversation

sealed interface Hw10Reward {
    val description: String
    fun grant(player: Player)
}

class ItemReward(val obj: Int, val amount: Int = 1, val reclaimable: Boolean = true) : Hw10Reward {
    override val description: String
        get() = ItemDefinitions.getDefs(obj).name.let { if (amount > 1) "$amount x $it" else Utils.addArticle(it) }

    override fun grant(player: Player) {
        player.inventory.addItemDrop(obj, amount)
        if (reclaimable)
            player.addDiangoReclaimItem(obj)
    }

    fun revoke(player: Player) {
        player.inventory.deleteItem(obj, Int.MAX_VALUE)
        player.equipment.deleteItem(obj, Int.MAX_VALUE)
        player.bank.removeItem(obj)
        player.diangoReclaim.remove(obj)
    }
}

class EmoteReward(val emote: Emote, val name: String) : Hw10Reward {
    override val description get() = "The $name emote"

    override fun grant(player: Player) {
        if (player.emotesManager.unlockEmote(emote))
            player.startConversation { emoteLearned(name) }
    }
}

class Hw10Edition(val key: String, val rewards: List<Hw10Reward>) {
    val rewardsText get() = rewards.joinToString("<br>") { it.description }
    val scrollObj get() = rewards.filterIsInstance<ItemReward>().firstOrNull()?.obj ?: -1
}

val HW10_EDITION = Hw10Edition(
    key = "hw2026",
    rewards = listOf(
        ItemReward(Obj.hw10_bone_broach),
        EmoteReward(Emote.PUPPET_MASTER, "Puppet Master"),
    ),
)

private const val EDITION_KEY = "hw10Edition"
private const val REWARDED_EDITION_KEY = "hw10RewardedEdition"

fun Player.syncHw10Edition() {
    if (get(EDITION_KEY) == HW10_EDITION.key)
        return
    if (hw10Stage != Hw10Stage.NOT_STARTED)
        resetHw10()
    set(EDITION_KEY, HW10_EDITION.key)
}

private val HW10_EMOTES = listOf(Emote.TRICK, Emote.SCARED, Emote.PUPPET_MASTER)

fun Player.hardResetHw10(): String {
    val emotes = HW10_EMOTES.filter { emotesManager.lockEmote(it) }
    val items = HW10_EDITION.rewards.filterIsInstance<ItemReward>().onEach { it.revoke(this) }
    delete(REWARDED_EDITION_KEY)
    set(EDITION_KEY, HW10_EDITION.key)
    resetHw10()
    return "Hard reset: progress, hw10_var and event items cleared; emotes locked: ${emotes.joinToString { it.name }.ifEmpty { "none" }}; " +
        "rewards removed from inventory, equipment, bank and Diango: ${items.joinToString { ItemDefinitions.getDefs(it.obj).name }}; reward record cleared."
}

fun grantHalloween2010Rewards(player: Player) {
    if (player.get(REWARDED_EDITION_KEY) == HW10_EDITION.key)
        return
    HW10_EDITION.rewards.forEach { it.grant(player) }
    player.set(REWARDED_EDITION_KEY, HW10_EDITION.key)
}
