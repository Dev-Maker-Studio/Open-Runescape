package com.rs.game.content.quests.creatureoffenkenstrain

import com.rs.engine.dialogue.Dialogue
import com.rs.engine.dialogue.DialogueBuilder
import com.rs.game.model.entity.player.Player

internal fun DialogueBuilder.fenkExec(owner: Player, action: () -> Unit) {
    statement(Dialogue(Runnable {
        val conversation = owner.conversation
        action()
        if (conversation != null && owner.conversation === conversation) conversation.process(0)
    }))
}
