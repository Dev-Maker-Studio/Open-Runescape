package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.game.content.holidayevents.halloween.hw09.SpiderStatement
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Task
import com.rs.game.content.holidayevents.halloween.hw10.finishesHw10Tasks
import com.rs.game.model.entity.player.Player

fun DialogueBuilder.eek(text: String) = statement(SpiderStatement(text))

fun DialogueBuilder.checkInWithPete(player: Player, task: Hw10Task) {
    if (player.finishesHw10Tasks(task))
        player(HeadE.CALM_TALK, "I think I've done everything I can do. I should go and check in with Pumpkin Pete.")
}
