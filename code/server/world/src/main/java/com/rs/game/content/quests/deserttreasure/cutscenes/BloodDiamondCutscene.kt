package com.rs.game.content.quests.deserttreasure

import com.rs.engine.cutscenekt.Cutscene
import com.rs.engine.cutscenekt.cutscene
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onPlayerStep
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

private const val BLOOD_INTRO_CUTSCENE_SEEN = "dtBloodIntroCutsceneSeen"

private val HAIR_OF_THE_DOG_ENTRY = arrayOf(Tile.of(3494, 3479, 0))

private val ROAVAR_TILE = Tile.of(5, 7, 0)
private val MALAK_TILE = Tile.of(5, 9, 0)
private val PLAYER_TILE = Tile.of(6, 14, 0)
private val ROAVAR_CAMERA_TILE = Tile.of(10, 12, 0)
private val MALAK_CAMERA_TILE = Tile.of(10, 13, 0)
private val PLAYER_CAMERA_TILE = Tile.of(10, 11, 0)

@ServerStartupEvent
fun mapBloodDiamondIntroCutscene() {
    onPlayerStep(HAIR_OF_THE_DOG_ENTRY) { (player) ->
        if (player.getQuestStage(Quest.DESERT_TREASURE) != STAGE_DIAMONDS_OF_AZZANADRA)
            return@onPlayerStep
        if (player.isLocked || player.getBool(BLOOD_INTRO_CUTSCENE_SEEN))
            return@onPlayerStep

        playBloodDiamondIntroCutscene(player)
    }
}

private fun playBloodDiamondIntroCutscene(player: Player) {
    player.cutscene {
        endTile = player.tile
        try {
            fadeInAndWait()
            hideMinimap()
            dynamicRegion(endTile, 436, 433, 4, 4, copyNpcs = false)
            entityTeleTo(player, PLAYER_TILE.x, PLAYER_TILE.y)
            val roavar = npcCreate(Npc.werewolfinnkeeper, ROAVAR_TILE.x, ROAVAR_TILE.y, ROAVAR_TILE.plane)
            val malak = npcCreate(Npc.fourdiamonds_vampire_lord, MALAK_TILE.x, MALAK_TILE.y, MALAK_TILE.plane)
            wait(2)
            roavar.faceEntityTile(malak)
            malak.faceEntityTile(roavar)
            player.faceEntityTile(malak)
            panTo(ROAVAR_TILE, ROAVAR_CAMERA_TILE)
            fadeOutAndWait()

            dialogue {
                npc(Npc.werewolfinnkeeper, NERVOUS, "M-my lord Malak, what brings you here?")
            }
            waitForDialogue()

            panTo(MALAK_TILE, MALAK_CAMERA_TILE)
            dialogue {
                npc(Npc.fourdiamonds_vampire_lord, CALM_TALK, "Hmmm? You know very well that your tithe is due, Roavar.")
                npc(Npc.fourdiamonds_vampire_lord, CALM_TALK, "I am here to collect it this time.")
            }
            waitForDialogue()

            panTo(ROAVAR_TILE, ROAVAR_CAMERA_TILE)
            dialogue {
                npc(Npc.werewolfinnkeeper, NERVOUS, "Y-yes of course, sire, but surely your graciousness is too important to perform such menial duties such as this himself?")
                npc(Npc.werewolfinnkeeper, NERVOUS, "Is this not Gadderanks job, your most high beneficence?")
            }
            waitForDialogue()

            panTo(MALAK_TILE, MALAK_CAMERA_TILE)
            dialogue {
                npc(Npc.fourdiamonds_vampire_lord, CALM_TALK, "Indeed it is Roavar, but I cannot find that incompetent fool. And assuming you are not the one who wishes to tell Lord Drakan that his tithe has not been collected, I saw fit to take on the duty myself this time.")
            }
            waitForDialogue()

            panTo(ROAVAR_TILE, ROAVAR_CAMERA_TILE)
            dialogue {
                npc(Npc.werewolfinnkeeper, NERVOUS, "Of course my lord, I meant no disrespect.")
            }
            waitForDialogue()

            panTo(MALAK_TILE, MALAK_CAMERA_TILE)
            dialogue {
                npc(Npc.fourdiamonds_vampire_lord, CONFUSED, "So you have the tithe ready for me?")
            }
            waitForDialogue()

            panTo(ROAVAR_TILE, ROAVAR_CAMERA_TILE)
            dialogue {
                npc(Npc.werewolfinnkeeper, NERVOUS, "Of course my lord, I trust you will find it all there and in good order.")
            }
            waitForDialogue()

            panTo(MALAK_TILE, MALAK_CAMERA_TILE)
            dialogue {
                npc(Npc.fourdiamonds_vampire_lord, CALM_TALK, "Hmmm. Well, for your sake you had better hope that indeed it is.")
                npc(Npc.fourdiamonds_vampire_lord, ANGRY, "The mood I am in for having to come to this wolfen cesspit means any shortfall is coming from you directly.")
                npc(Npc.fourdiamonds_vampire_lord, CALM_TALK, "Then again... perhaps this was not a total waste of my time after all.")
                npc(Npc.fourdiamonds_vampire_lord, ANGRY, "You there!")
            }
            waitForDialogue()

            panTo(PLAYER_TILE, PLAYER_CAMERA_TILE)
            dialogue {
                player(NERVOUS, "M-me?")
                npc(Npc.fourdiamonds_vampire_lord, CALM_TALK, "Yes, you. Your bearing suggest you are not one of the usual peasants, and your stench suggests you are not one of these filthy wolfen folk.")
                npc(Npc.fourdiamonds_vampire_lord, CALM_TALK, "Come here, I would like to know what you are doing in these lands.")
            }
            waitForDialogue()

            player.set(BLOOD_INTRO_CUTSCENE_SEEN, true)
        } finally {
            unhideMinimap()
            stop()
        }
    }
}

private suspend fun Cutscene.panTo(target: Tile, camera: Tile) {
    camPos(camera.x, camera.y, 1400)
    camLook(target.x, target.y, 800)
    wait(1)
}
