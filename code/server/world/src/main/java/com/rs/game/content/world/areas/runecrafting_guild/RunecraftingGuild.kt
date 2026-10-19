package com.rs.game.content.world.areas.runecrafting_guild

import com.rs.game.World
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Utils
import org.darkan.core.cache.EnumDefinitions
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Enum
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.Animation
import world.gregs.voidps.type.Tile

private val WIZARDS_TOWER_PORTAL = Tile.of(3106, 3160, 1)
private val GUILD_PORTAL = Tile.of(1696, 5460, 2)
private val ELEMENTAL_TALISMANS = intArrayOf(Obj.air_talisman, Obj.water_talisman, Obj.earth_talisman, Obj.fire_talisman)
private val DEVICES = mapOf(
    Loc.rcguild_guild_container to Seq.rcguild_orb_container_one_receive_launch,
    Loc.rcguild_guild_accelerator to Seq.rcguild_orb_accelerator_one_receive_launch,
    Loc.rcguild_guild_gyroscope to Seq.rcguild_activated_gyro,
    Loc.rcguild_guild_glass_balls to Seq.rcguild_glass_ball_activated,
)

@ServerStartupEvent
fun mapRunecraftingGuild() {
    onObjectClick(Loc.rcguild_wiztower_portal, tiles = arrayOf(GUILD_PORTAL)) { (player) -> player.useStairs(-1, WIZARDS_TOWER_PORTAL, 0, 1) }

    onObjectClick(Loc.loc_38315) { (player) -> studyMap(player, null) }
    onItemOnObject(arrayOf(Loc.loc_38315), null) { (player, _, item) -> studyMap(player, item.id) }

    onObjectClick(*DEVICES.keys.toTypedArray()) { (player, obj) ->
        player.faceObject(obj)
        World.sendObjectAnimation(obj, Animation(DEVICES.getValue(obj.id)))
        player.sendMessage("You activate the ${obj.definitions.name.lowercase()}.")
    }

    onObjectClick(Loc.rcguild_guild_bookcase1, Loc.rcguild_guild_bookcase2, Loc.rcguild_guild_bookcase3) { (player) ->
        player.sendMessage("The bookcase is full of runecrafting theory, far too dense to read now.")
    }
}

private fun studyMap(player: Player, talisman: Int?) {
    val markers = EnumDefinitions.getEnum(Enum.enum_1681)
    val shown = when (talisman) {
        null -> emptyList()
        Obj.omni_talisman -> markers.map?.keys?.toList().orEmpty()
        Obj.elemental_talisman -> ELEMENTAL_TALISMANS.toList()
        else -> listOf(talisman).filter { markers.map?.containsKey(it) == true }
    }
    if (talisman != null && shown.isEmpty()) {
        player.sendMessage("You can only use an altar talisman on the map.")
        return
    }
    player.interfaceManager.sendInterface(Interface.interface_780)
    markers.map?.forEach { (item, component) ->
        val hash = component as? Int ?: return@forEach
        player.packets.setIFHidden(Utils.interfaceIdFromHash(hash), Utils.componentIdFromHash(hash), item !in shown)
    }
    val location = talisman?.let { EnumDefinitions.getEnum(Enum.enum_1682).getValue(it) as? String }
    player.packets.setIFText(Component.interface_780.component_780_79, location ?: "Use a talisman on the map to display its altar's location.")
}
