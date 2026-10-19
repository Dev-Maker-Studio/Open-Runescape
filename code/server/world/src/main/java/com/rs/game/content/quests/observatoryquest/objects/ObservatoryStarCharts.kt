package com.rs.game.content.quests.observatoryquest

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc

private val STAR_CHART_BUTTONS = mapOf(
    Component.interface_104.component_104_19 to CONSTELLATION_AQUARIUS,
    Component.interface_104.component_104_20 to CONSTELLATION_ARIES,
    Component.interface_104.component_104_21 to CONSTELLATION_CANCER,
    Component.interface_104.component_104_22 to CONSTELLATION_CAPRICORN,
    Component.interface_104.component_104_23 to CONSTELLATION_GEMINI,
    Component.interface_104.component_104_24 to CONSTELLATION_LEO,
    Component.interface_104.component_104_25 to CONSTELLATION_LIBRA,
    Component.interface_104.component_104_26 to CONSTELLATION_PISCES,
    Component.interface_104.component_104_27 to CONSTELLATION_SAGITTARIUS,
    Component.interface_104.component_104_28 to CONSTELLATION_SCORPIO,
    Component.interface_104.component_104_29 to CONSTELLATION_TAURUS,
    Component.interface_104.component_104_30 to CONSTELLATION_VIRGO
)

@ServerStartupEvent
fun mapObservatoryStarCharts() {
    onObjectClick(Loc.qip_obs_star_chart1, Loc.qip_obs_star_chart2, Loc.qip_obs_star_chart3, Loc.qip_obs_star_chart4, Loc.qip_obs_star_chart5, Loc.qip_obs_star_chart6) { (player) ->
        player.interfaceManager.sendInterface(Interface.interface_104)
        player.showObservatoryStarChart(CONSTELLATION_AQUARIUS)
    }
    onButtonClick(Interface.interface_104) { e ->
        if (e.component == Component.interface_104.component_104_38) {
            e.player.closeInterfaces()
            return@onButtonClick
        }
        val constellation = STAR_CHART_BUTTONS[e.component] ?: return@onButtonClick
        e.player.showObservatoryStarChart(constellation)
    }
}

private fun Player.showObservatoryStarChart(constellation: Int) {
    packets.setIFModel(Interface.interface_104, Utils.componentIdFromHash(Component.interface_104.component_104_55), observatoryConstellationModel(constellation))
    packets.setIFText(Interface.interface_104, Utils.componentIdFromHash(Component.interface_104.component_104_57), getObservatoryConstellationName(constellation))
}
