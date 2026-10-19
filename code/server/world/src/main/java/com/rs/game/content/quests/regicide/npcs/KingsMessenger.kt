package com.rs.game.content.quests.regicide.npcs

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.ownedNpc
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

internal const val KINGS_MESSAGE_DELIVERED = "kingsMessageDelivered"
private const val MESSENGER_DUE = "messengerDue"
private const val MESSENGER_TASK = "RegicideKingsMessenger"

@ServerStartupEvent
fun mapRegicideKingsMessenger() {
    onLogin { (player) -> player.scheduleRegicideMessenger() }
    onNpcClick(Npc.regicide_kings_messenger) { (player, npc) ->
        if (npc !is OwnedNPC || npc.owner != player) {
            player.startConversation { npc(npc, CALM_TALK, "I have no business with you.") }
        } else {
            player.receiveKingsMessage(npc)
        }
    }
}

fun Player.scheduleRegicideMessenger() {
    if (!isQuestComplete(Quest.UNDERGROUND_PASS) || isQuestStarted(Quest.REGICIDE))
        return
    val attributes = questManager.getAttribs(Quest.REGICIDE)
    if (attributes.getB(KINGS_MESSAGE_DELIVERED))
        return
    if (inventory.containsItem(Obj.regicide_quest_kings_summons) || bank.containsItem(Obj.regicide_quest_kings_summons)) {
        attributes.setB(KINGS_MESSAGE_DELIVERED, true)
        return
    }
    if (attributes.getL(MESSENGER_DUE) == 0L)
        attributes.setL(MESSENGER_DUE, System.currentTimeMillis() + Utils.random(240, 1800) * 1000L)
    schedule(MESSENGER_TASK) {
        var messenger: OwnedNPC? = null
        while (!hasFinished() && isQuestComplete(Quest.UNDERGROUND_PASS) && !isQuestStarted(Quest.REGICIDE)) {
            val currentAttributes = questManager.getAttribs(Quest.REGICIDE)
            if (currentAttributes.getB(KINGS_MESSAGE_DELIVERED))
                break
            if (currentAttributes.getL(MESSENGER_DUE) == 0L)
                currentAttributes.setL(MESSENGER_DUE, System.currentTimeMillis() + Utils.random(240, 1800) * 1000L)
            if (System.currentTimeMillis() >= currentAttributes.getL(MESSENGER_DUE) &&
                !isDead && !isLocked && !inCombat() && conversation == null &&
                !interfaceManager.containsScreenInter() && inventory.hasFreeSlots()) {
                val spawnTile = getNearestTeleTile(1)
                if ((messenger == null || messenger.hasFinished()) && spawnTile != null) {
                    messenger = ownedNpc(this@scheduleRegicideMessenger, Npc.regicide_kings_messenger, spawnTile) {
                        setRandomWalk(false)
                        setCantInteract(false)
                    }
                }
                messenger?.let { receiveKingsMessage(it) }
            }
            wait(50)
        }
        messenger?.finish()
    }
}

private fun Player.receiveKingsMessage(messenger: OwnedNPC) {
    if (!isQuestComplete(Quest.UNDERGROUND_PASS) || isQuestStarted(Quest.REGICIDE) ||
        questManager.getAttribs(Quest.REGICIDE).getB(KINGS_MESSAGE_DELIVERED)) {
        messenger.finish()
        return
    }
    if (!inventory.hasFreeSlots()) {
        sendMessage("You need a free inventory slot to receive the King's message.")
        return
    }
    startConversation {
        npc(messenger, CALM_TALK, "I take it you are ${displayName}? King Lathas sent this message for you. He's not a man you'd want to keep waiting.")
        exec {
            if (!isQuestStarted(Quest.REGICIDE) && !questManager.getAttribs(Quest.REGICIDE).getB(KINGS_MESSAGE_DELIVERED)) {
                if (inventory.containsItem(Obj.regicide_quest_kings_summons) || inventory.addItem(Obj.regicide_quest_kings_summons, 1)) {
                    questManager.getAttribs(Quest.REGICIDE).setB(KINGS_MESSAGE_DELIVERED, true)
                    sendMessage("The messenger gives you a scroll.")
                }
            }
            messenger.finish()
        }
    }
}
