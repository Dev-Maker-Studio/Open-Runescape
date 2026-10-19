package com.rs.game.content.quests.legendsquest

import com.rs.engine.dialogue.Dialogue
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.game.model.entity.player.Player

internal fun DialogueBuilder.legendsExec(player: Player, action: () -> Unit) {
    statement(Dialogue(Runnable {
        val conversation = player.conversation
        action()
        if (conversation != null && player.conversation === conversation) conversation.process(0)
    }))
}
