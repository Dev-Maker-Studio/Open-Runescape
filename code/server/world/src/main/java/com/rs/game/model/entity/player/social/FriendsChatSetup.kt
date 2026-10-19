package com.rs.game.model.entity.player.social

import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.model.FriendsChatRank
import org.darkan.core.social.SocialAction
import org.darkan.world.server.WorldServer

private val SETUP_HIDDEN = intArrayOf(
    Component.interface_1108.component_1108_49,
    Component.interface_1108.component_1108_63,
    Component.interface_1108.component_1108_77,
    Component.interface_1108.component_1108_91,
)

private val RANK_BY_OP = mapOf(
    1 to FriendsChatRank.UNRANKED,
    2 to FriendsChatRank.FRIEND,
    3 to FriendsChatRank.RECRUIT,
    4 to FriendsChatRank.CORPORAL,
    5 to FriendsChatRank.SERGEANT,
    6 to FriendsChatRank.LIEUTENANT,
    7 to FriendsChatRank.CAPTAIN,
    8 to FriendsChatRank.GENERAL,
    9 to FriendsChatRank.OWNER,
)

@ServerStartupEvent
fun mapFriendsChatSetup() {
    onButtonClick(Interface.interface_1108) { e ->
        val player = e.player
        val settings = player.social.friendsChat
        when (e.component) {
            Component.interface_1108.component_1108_1 -> when (e.opNum) {
                1 -> player.sendInputName("Enter chat prefix:") { name ->
                    settings.name = name
                    publishSettings(player)
                }
                2 -> settings.name = null
                else -> player.sendMessage("Unexpected FC interface packet...")
            }
            Component.interface_1108.component_1108_2 -> settings.rankToEnter = rankOf(e.opNum)
            Component.interface_1108.component_1108_3 -> settings.rankToSpeak = rankOf(e.opNum)
            Component.interface_1108.component_1108_4 -> settings.rankToKick = rankOf(e.opNum)
            Component.interface_1108.component_1108_5 -> settings.rankToLS = rankOf(e.opNum)
        }
        publishSettings(player)
    }

    onButtonClick(Interface.interface_1109) { e ->
        val player = e.player
        when (e.component) {
            Component.interface_1109.component_1109_26 -> {
                if (!player.social.currentFriendsChat.isNullOrEmpty()) {
                    WorldServer.act(player.username, SocialAction.LeaveFriendsChat)
                } else {
                    player.sendInputName("Whose chat would you like to join?") { name ->
                        if (!name.isNullOrEmpty()) WorldServer.act(player.username, SocialAction.JoinFriendsChat(name))
                    }
                }
            }
            Component.interface_1109.component_1109_31 -> {
                if (player.interfaceManager.containsScreenInter()) {
                    player.sendMessage("Please close the interface you have opened before using Friends Chat setup.")
                    return@onButtonClick
                }
                player.stopAll()
                openFriendsChatSetup(player)
            }
            Component.interface_1109.component_1109_19 -> player.toggleLootShare()
        }
    }
}

private fun rankOf(opNum: Int): FriendsChatRank = RANK_BY_OP[opNum] ?: FriendsChatRank.UNRANKED

private fun publishSettings(player: Player) {
    val settings = player.social.friendsChat
    WorldServer.act(
        player.username,
        SocialAction.UpdateFriendsChatSettings(
            name = settings.name,
            rankToEnter = settings.rankToEnter,
            rankToSpeak = settings.rankToSpeak,
            rankToKick = settings.rankToKick,
            rankToLootShare = settings.rankToLS,
        ),
    )
    refreshSetup(player)
}

private fun openFriendsChatSetup(player: Player) {
    player.interfaceManager.sendInterface(Interface.interface_1108)
    refreshSetup(player)
    for (component in SETUP_HIDDEN) player.packets.setIFHidden(Interface.interface_1108, Utils.componentIdFromHash(component), true)
}

private fun refreshSetup(player: Player) {
    val settings = player.social.friendsChat
    player.packets.setIFText(Component.interface_1108.component_1108_1, settings.name ?: "Chat disabled")
    player.packets.setIFText(Component.interface_1108.component_1108_2, requirementText(settings.rankToEnter, "Only me"))
    player.packets.setIFText(Component.interface_1108.component_1108_3, requirementText(settings.rankToSpeak, "Only me"))
    player.packets.setIFText(Component.interface_1108.component_1108_4, requirementText(settings.rankToKick, "Only me"))
    player.packets.setIFText(Component.interface_1108.component_1108_5, requirementText(settings.rankToLS, "No-one"))
}

private fun requirementText(rank: FriendsChatRank, ownerText: String): String = when (rank) {
    FriendsChatRank.UNRANKED -> "Anyone"
    FriendsChatRank.FRIEND -> "Any friends"
    FriendsChatRank.RECRUIT -> "Recruit+"
    FriendsChatRank.CORPORAL -> "Corporal+"
    FriendsChatRank.SERGEANT -> "Sergeant+"
    FriendsChatRank.LIEUTENANT -> "Lieutenant+"
    FriendsChatRank.CAPTAIN -> "Captain+"
    FriendsChatRank.GENERAL -> "General+"
    FriendsChatRank.OWNER, FriendsChatRank.JMOD -> ownerText
}
