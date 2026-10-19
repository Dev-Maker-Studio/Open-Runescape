package com.rs.game.content.quests.observatoryquest

import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Model
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.type.Tile

const val OBSERVATORY_CONSTELLATION_ATTR = "observatoryQuestConstellation"
const val OBSERVATORY_VIEWED_CONSTELLATION_ATTR = "observatoryQuestViewedConstellation"
const val OBSERVATORY_VIEWED_SCORPIO_ATTR = "observatoryQuestViewedScorpio"

const val CONSTELLATION_AQUARIUS = 0
const val CONSTELLATION_ARIES = 1
const val CONSTELLATION_CANCER = 2
const val CONSTELLATION_CAPRICORN = 3
const val CONSTELLATION_GEMINI = 4
const val CONSTELLATION_LEO = 5
const val CONSTELLATION_LIBRA = 6
const val CONSTELLATION_PISCES = 7
const val CONSTELLATION_SAGITTARIUS = 8
const val CONSTELLATION_SCORPIO = 9
const val CONSTELLATION_TAURUS = 10
const val CONSTELLATION_VIRGO = 11

private val CONSTELLATION_NAMES = arrayOf("Aquarius", "Aries", "Cancer", "Capricorn", "Gemini", "Leo", "Libra", "Pisces", "Sagittarius", "Scorpio", "Taurus", "Virgo")

private val CONSTELLATION_MODELS = mapOf(
    CONSTELLATION_AQUARIUS to Model.model_27064,
    CONSTELLATION_ARIES to Model.model_27066,
    CONSTELLATION_CANCER to Model.model_27067,
    CONSTELLATION_CAPRICORN to Model.model_27061,
    CONSTELLATION_GEMINI to Model.model_27068,
    CONSTELLATION_LEO to Model.model_27058,
    CONSTELLATION_LIBRA to Model.model_27057,
    CONSTELLATION_PISCES to Model.model_27062,
    CONSTELLATION_SAGITTARIUS to Model.model_27056,
    CONSTELLATION_SCORPIO to Model.model_27055,
    CONSTELLATION_TAURUS to Model.model_27059,
    CONSTELLATION_VIRGO to Model.model_27060
)

@ServerStartupEvent
fun mapObservatoryTelescope() {
    onObjectClick(Loc.loc_25591, Loc.qip_obs_tele_gear_upper, Loc.qip_obs_tele_gear_upper2, tiles = arrayOf(Tile.of(2440, 3159, 1))) { (player) ->
        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) < STAGE_OBSERVING_CONSTELLATION) {
            player.sendMessage("It seems the telescope is not operational at the moment.")
            return@onObjectClick
        }
        val completed = player.isQuestComplete(Quest.OBSERVATORY_QUEST)
        val constellation = if (completed) CONSTELLATION_SCORPIO else getOrAssignObservatoryConstellation(player)
        player.anim(Seq.look_through_scope)
        player.sendMessage("You look through the telescope.")
        if (completed) player.set(OBSERVATORY_VIEWED_SCORPIO_ATTR, true)
        else player.set(OBSERVATORY_VIEWED_CONSTELLATION_ATTR, true)
        player.vars.setVarBit(Varbit.observatory_scopelooked, 1)
        player.interfaceManager.sendInterface(Interface.interface_552)
        player.packets.setIFModel(Interface.interface_552, Utils.componentIdFromHash(Component.interface_552.component_552_7), observatoryConstellationModel(constellation))
    }

    onButtonClick(Interface.interface_552) { e ->
        if (e.component == Component.interface_552.component_552_8)
            e.player.closeInterfaces()
    }
}

fun getOrAssignObservatoryConstellation(player: Player): Int {
    val existing = player.getI(OBSERVATORY_CONSTELLATION_ATTR, -1)
    if (existing in CONSTELLATION_AQUARIUS..CONSTELLATION_VIRGO) return existing
    val constellation = Utils.random(CONSTELLATION_NAMES.size)
    player.set(OBSERVATORY_CONSTELLATION_ATTR, constellation)
    return constellation
}

fun getObservatoryConstellation(player: Player): Int = player.getI(OBSERVATORY_CONSTELLATION_ATTR, -1)

fun hasViewedObservatoryConstellation(player: Player): Boolean = player.getBool(OBSERVATORY_VIEWED_CONSTELLATION_ATTR)

internal fun observatoryConstellationModel(constellation: Int): Int = CONSTELLATION_MODELS[constellation] ?: CONSTELLATION_MODELS.getValue(CONSTELLATION_AQUARIUS)

fun getObservatoryConstellationName(constellation: Int): String = CONSTELLATION_NAMES.getOrElse(constellation) { "Unknown" }
