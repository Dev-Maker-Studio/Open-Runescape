package com.rs.game.content.transportation

import com.rs.engine.dialogue.Dialogue
import com.rs.engine.dialogue.HeadE
import com.rs.engine.quest.Quest
import com.rs.game.content.dnds.eviltree.handleEvilTreeOps
import com.rs.game.content.skills.magic.Magic
import com.rs.game.content.skills.magic.TeleType
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapSpiritTrees() {
    onObjectClick("Spirit Tree", "Spirit tree", Loc.loc_26723) { (player, obj, option) ->
        val stronghold = obj.id == Loc.gnome_area_imp_stronghold_ent
        if (!player.isQuestComplete(Quest.TREE_GNOME_VILLAGE) || stronghold && !player.isQuestComplete(Quest.GRAND_TREE)) {
            player.simpleDialogue("The tree doesn't feel like talking.")
            return@onObjectClick
        }
        when {
            option.contains("talk", ignoreCase = true) -> {
                val head = if (obj.id in intArrayOf(Loc.gnome_area_imp_stronghold_ent, Loc.gnome_area_imp_ent))
                    Npc.treevillage_spirittree else Npc.treevillage_small_spirittree
                player.startConversation(Dialogue()
                    .addNPC(head, HeadE.CALM_TALK, "If you are a friend of the gnome people, you are a friend of mine. Do you wish to travel?")
                    .addOptions { ops ->
                        ops.add("Yes, please.") { SpiritTree.openInterface(player) }
                        handleEvilTreeOps(player, ops, head)
                        ops.add("No, thanks.")
                    })
            }
            option.contains("travel", ignoreCase = true) || option.contains("teleport", ignoreCase = true) -> SpiritTree.openInterface(player)
        }
    }
    onButtonClick(Interface.interface_864) { event ->
        if (event.component == Component.interface_864.component_864_6 && playerCanTravel(event.player))
            SpiritTree.handleSpiritTree(event.player, event.slotId)
    }
}

private fun playerCanTravel(player: Player) = player.isQuestComplete(Quest.TREE_GNOME_VILLAGE) &&
        (player.regionId != 9781 || player.isQuestComplete(Quest.GRAND_TREE))

object SpiritTree {
    private val TELEPORTS = arrayOf(
        Tile.of(2554, 3255, 0), Tile.of(3187, 3507, 0), Tile.of(2416, 2852, 0),
        Tile.of(2339, 3108, 0), Tile.of(2541, 3170, 0), Tile.of(2462, 3445, 0)
    )

    fun openInterface(player: Player) {
        if (!playerCanTravel(player)) return
        player.vars.setVarBit(Varbit.pog_spirit, 3)
        player.interfaceManager.sendInterface(Interface.interface_864)
        player.packets.setIFRightClickOps(Interface.interface_864,
            Utils.componentIdFromHash(Component.interface_864.component_864_6), 0, 7, 0)
        when (player.regionId) {
            10033, 12102 -> player.vars.setVar(Varp.varp_1469, 0x27b8c61)
            9781 -> player.vars.setVar(Varp.varp_1469, 0x2678d74)
            else -> sendTeleport(player, TELEPORTS[4])
        }
    }

    fun sendTeleport(player: Player, tile: Tile) {
        player.sendMessage("You place your hands on the dry tough bark of the spirit tree, and feel a surge of energy run through your veins.")
        Magic.sendTeleportSpell(player, Seq.pog_player_organic_teleport, Seq.pog_player_organic_teleport_reversed,
            Spotanim.pog_organic_teleport_spotanim_reversed, Spotanim.pog_organic_teleport_spotanim_reversed,
            1, 0.0, tile, 4, true, TeleType.OBJECT, null)
    }

    fun handleSpiritTree(player: Player, slot: Int) {
        val destination = if (slot == 0) {
            if (player.regionId == 10033) 6 else 5
        } else slot
        TELEPORTS.getOrNull(destination - 1)?.let { sendTeleport(player, it) }
    }
}
