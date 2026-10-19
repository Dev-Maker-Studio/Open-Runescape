package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.cutscenekt.cutscene
import com.rs.engine.dialogue.HeadE
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.game.model.entity.player.Player

internal fun hazeelRevivalCutscene(player: Player) {
    player.cutscene {
        fadeInAndWait()
        val hazeel = npcCreate(Npc.hazeel, 2607, 9669, 0)
        hazeel.faceEntityTile(player)
        fadeOut()
        dialogue {
            npc(Npc.hazeelcult_alomone, HeadE.CALM_TALK, "Lord Hazeel...Lord Zamorak...I call upon the powers of evil, the forces of chaos, the strength of hatred...")
            npc(Npc.hazeelcult_alomone, HeadE.CALM_TALK, "Sentente sillaberi junque dithmento! Ia! Ia! Dextrimon encanto! Termando...incurdo...solly enty rando...sentente!")
            npc(Npc.hazeelcult_alomone, HeadE.CALM_TALK, "Dintenta! Sententa! Retenta! Q'exjta! Ia! Dysmenta junque fammatio svelken! SOLOMENT!")
            npc(Npc.hazeel, HeadE.CALM_TALK, "My loyal followers. I have pride in you all. Never did I expect to return to this land.")
            npc(Npc.hazeel, HeadE.ANGRY, "Soon this world will cower once more at the name of Hazeel, and my fury will blaze across mine enemies.")
            npc(Npc.hazeel, HeadE.CALM_TALK, "Adventurer, your cunning and treachery mark you as a friend to the Mahjarrat. I offer you my thanks.")
            player(HeadE.CALM_TALK, "Can I have a reward now too?")
            npc(Npc.hazeel, HeadE.FRUSTRATED, "Your insolence shall be overlooked this time. I shall reward you with wealth such as you deserve.")
            simple("Hazeel offers you some coins.")
            npc(Npc.hazeel, HeadE.CALM_TALK, "Now I must join my fellow Mahjarrat in the north to regain my strength and powers. Much blood shall flow across these lands!")
            simple("Hazeel departs for the north.")
        }
        waitForDialogue()
        player.inventory.addItemDrop(Obj.coins, 2000)
        player.questManager.completeQuest(Quest.HAZEEL_CULT)
        stop()
    }
}
