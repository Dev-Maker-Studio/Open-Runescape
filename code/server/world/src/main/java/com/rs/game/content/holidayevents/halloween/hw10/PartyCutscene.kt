package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.engine.cutscenekt.cutscene
import com.rs.engine.dialogue.HeadE
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Npc

fun playPartyCutscene(player: Player) {
    player.hw10Stage = Hw10Stage.PARTY
    player.cutscene {
        fadeInAndWait()
        endTile = PARTY_DANCE_FLOOR
        player.tele(PARTY_DANCE_FLOOR)
        wait(2)
        player.musicsManager.forcePlayMusic(Midi.midi_908)
        camPos(4319, 5326, 2900)
        camLook(4320, 5338, 300)
        fadeOutAndWait()
        camPos(4319, 5330, 2200, 2, 12)

        dialogue { npc(Npc.hw10_black_zabeth_band, HeadE.DRUNK, "ONE!") }
        waitForDialogue()

        camPos(4320, 5336, 1450)
        camLook(4320, 5341, 400)
        dialogue { npc(Npc.hw10_black_zabeth_band, HeadE.DRUNK, "TWO!") }
        waitForDialogue()
        dialogue { npc(Npc.hw10_black_zabeth_band, HeadE.DRUNK, "ONE TWO THREE FOUR!") }
        waitForDialogue()

        camPos(4319, 5332, 1500)
        camLook(4319, 5338, 300)
        camPos(4319, 5333, 1300, 2, 12)
        wait(5)

        camPos(4319, 5326, 2800, 2, 10)
        camLook(4319, 5337, 200, 4, 4)
        wait(4)
        completeHalloween2010(player)
    }
}
