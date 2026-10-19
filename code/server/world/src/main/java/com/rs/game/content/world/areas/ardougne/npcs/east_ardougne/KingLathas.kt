package com.rs.game.content.world.areas.ardougne.npcs.east_ardougne

import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.biohazard.dialogue.npcs.east_ardougne.KingLathasD
import com.rs.game.content.quests.biohazard.utils.*
import com.rs.game.content.quests.regicide.npcs.regicideKingLathasDialogue
import com.rs.game.content.quests.underground_pass.npcs.undergroundPassKingLathasDialogue
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc

class KingLathas(player: Player, npc: NPC) {
    init {
        val biohazardStage = player.questManager.getStage(Quest.BIOHAZARD)
        val biohazardComplete = player.isQuestComplete(Quest.BIOHAZARD)
        if (biohazardStage != STAGE_SPEAK_TO_KING && !biohazardComplete) {
            player.sendMessage("<col=A31818>The king is too busy to talk.</col>")
        } else {
            player.startConversation {
                options {
                    if (biohazardStage == STAGE_SPEAK_TO_KING)
                        op("Talk about Biohazard") { exec { KingLathasD(player, npc) } }
                    if (biohazardComplete)
                        op("Talk about Underground Pass") {
                            exec { undergroundPassKingLathasDialogue(player, npc) }
                        }
                    if (player.isQuestComplete(Quest.UNDERGROUND_PASS))
                        op("Talk about Regicide") {
                            exec { regicideKingLathasDialogue(player, npc) }
                        }
                    op("Never mind.")
                }
            }
        }
    }
}

@ServerStartupEvent
fun mapKingLathas() {
    onNpcClick(Npc.npc_364) { (player, npc) -> KingLathas(player, npc) }
}
