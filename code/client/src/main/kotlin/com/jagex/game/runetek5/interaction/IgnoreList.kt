package com.jagex.game.runetek5.interaction

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.TextUtils

object IgnoreList {

    @JvmField val IGNORED = arrayOfNulls<Ignore>(100)
    @JvmField var IGNORE_COUNT = 0

    @JvmStatic
    fun add(displayName: String?, flag: Boolean) {
        if (displayName != null) {
            if (IGNORE_COUNT >= 100) {
                ChatBox.writeGameMessage(LocalizedText.IGNORE_LIST_FULL.translate(Client.CURRENT_LANGUAGE!!)!!)
            } else {
                val formattedDisplayName = TextUtils.formatDisplayname(displayName)
                if (formattedDisplayName != null) {
                    for (index in 0 until IGNORE_COUNT) {
                        val ignore = IGNORED[index]!!
                        val formattedIgnoreUnfilteredUsername = TextUtils.formatDisplayname(ignore.unfilteredUsername!!)
                        if (formattedIgnoreUnfilteredUsername != null && formattedIgnoreUnfilteredUsername == formattedDisplayName) {
                            ChatBox.writeGameMessage(displayName + LocalizedText.ALREADY_IGNORED.translate(Client.CURRENT_LANGUAGE!!))
                            return
                        }
                        if (ignore.displayName != null) {
                            val formattedIgnoreDisplayName = TextUtils.formatDisplayname(ignore.displayName)
                            if (formattedIgnoreDisplayName != null && formattedIgnoreDisplayName == formattedDisplayName) {
                                ChatBox.writeGameMessage(displayName + LocalizedText.ALREADY_IGNORED.translate(Client.CURRENT_LANGUAGE!!))
                                return
                            }
                        }
                    }
                    for (index in 0 until FriendList.FRIEND_COUNT) {
                        val friend = FriendList.FRIENDS[index]!!
                        val formattedFriendDisplayname = TextUtils.formatDisplayname(friend.displayName)
                        if (formattedFriendDisplayname != null && formattedFriendDisplayname == formattedDisplayName) {
                            ChatBox.writeGameMessage(
                                LocalizedText.PLEASE_REMOVE.translate(Client.CURRENT_LANGUAGE!!) + displayName
                                        + LocalizedText.FROM_FRIENDS_FIRST.translate(Client.CURRENT_LANGUAGE!!)
                            )
                            return
                        }
                        if (friend.previousDisplayName != null) {
                            val formattedFriendUsername = TextUtils.formatDisplayname(friend.previousDisplayName)
                            if (formattedFriendUsername != null && formattedFriendUsername == formattedDisplayName) {
                                ChatBox.writeGameMessage(
                                    LocalizedText.PLEASE_REMOVE.translate(Client.CURRENT_LANGUAGE!!) + displayName
                                            + LocalizedText.FROM_FRIENDS_FIRST.translate(Client.CURRENT_LANGUAGE!!)
                                )
                                return
                            }
                        }
                    }
                    if (TextUtils.formatDisplayname(PlayerUpdate.MY_PLAYER!!.displayName!!) == formattedDisplayName) {
                        ChatBox.writeGameMessage(LocalizedText.CANT_ADD_SELF_TO_IGNORE.translate(Client.CURRENT_LANGUAGE!!)!!)
                    } else {
                        val context = Client.connectionContext
                        val packet = PacketEncoder.createPacket(ClientProt.ADD_IGNORE, context.isaac)
                        packet.buffer.writeByte(TextUtils.getLength(displayName) + 1)
                        packet.buffer.writeString(displayName)
                        packet.buffer.writeByte(if (flag) 1 else 0)
                        context.queuePacket(packet)
                    }
                }
            }
        }
    }

    @JvmStatic
    fun remove(displayName: String?) {
        if (displayName != null) {
            val formattedDisplayName = TextUtils.formatDisplayname(displayName)
            if (formattedDisplayName != null) {
                for (index in 0 until IGNORE_COUNT) {
                    val ignore = IGNORED[index]!!
                    val unfilteredUsername = ignore.unfilteredUsername
                    val formattedIgnoredDisplayName = TextUtils.formatDisplayname(unfilteredUsername)
                    if (TextUtils.matches(displayName, formattedDisplayName, unfilteredUsername, formattedIgnoredDisplayName, (-110).toByte())) {
                        if (--IGNORE_COUNT - index >= 0)
                            System.arraycopy(IGNORED, index + 1, IGNORED, index, IGNORE_COUNT - index)
                        Client.lastFriendIgnoreUpdateTick = Client.serverTick
                        val context = Client.connectionContext
                        val packet = PacketEncoder.createPacket(ClientProt.REMOVE_IGNORE, context.isaac)
                        packet.buffer.writeByte(TextUtils.getLength(displayName))
                        packet.buffer.writeString(displayName)
                        context.queuePacket(packet)
                        break
                    }
                }
            }
        }
    }

    @JvmStatic
    fun contains(username: String?): Boolean {
        if (username != null) {
            for (index in 0 until IGNORE_COUNT) {
                val ignore = IGNORED[index]!!
                if (username.equals(ignore.unfilteredUsername, ignoreCase = true)) {
                    return true
                }
                if (username.equals(ignore.displayName, ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }
}
