package com.jagex.game.runetek5.interaction

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.TextUtils

/**
 *
 * @author Steven - 18/09/2023
 *
 */
object FriendList {

    @JvmField val FRIENDS = arrayOfNulls<Friend>(200)
    @JvmField var FRIEND_COUNT = 0
    @JvmField var LOADED = 0

    @JvmStatic
    fun add(displayName: String?) {
        if (displayName != null) {
            if (FRIEND_COUNT < 200) {
                val formattedDisplayName = TextUtils.formatDisplayname(displayName)
                if (formattedDisplayName != null) {
                    for (index in 0 until FRIEND_COUNT) {
                        val friend = FRIENDS[index]!!
                        val friendFormattedDisplayName = TextUtils.formatDisplayname(friend.displayName)
                        if (friendFormattedDisplayName != null && friendFormattedDisplayName == formattedDisplayName) {
                            ChatBox.writeGameMessage(displayName + LocalizedText.ALREADY_ON_FRIENDS_LIST.translate(client.CURRENT_LANGUAGE)!!)
                            return
                        }
                        if (friend.previousDisplayName != null) {
                            val friendFormattedUsername = TextUtils.formatDisplayname(friend.previousDisplayName)
                            if (friendFormattedUsername != null && friendFormattedUsername == formattedDisplayName) {
                                ChatBox.writeGameMessage(displayName + LocalizedText.ALREADY_ON_FRIENDS_LIST.translate(client.CURRENT_LANGUAGE)!!)
                                return
                            }
                        }
                    }
                    for (index in 0 until IgnoreList.IGNORE_COUNT) {
                        val ignore = IgnoreList.IGNORED[index]!!
                        val ignoreFilteredDisplayName = TextUtils.formatDisplayname(ignore.unfilteredUsername!!)
                        if (ignoreFilteredDisplayName != null && ignoreFilteredDisplayName == formattedDisplayName) {
                            ChatBox.writeGameMessage(
                                LocalizedText.PLEASE_REMOVE_IGNORE.translate(client.CURRENT_LANGUAGE)!!
                                        + displayName + LocalizedText.FROM_IGNORE_FIRST.translate(client.CURRENT_LANGUAGE)!!
                            )
                            return
                        }
                        if (ignore.displayName != null) {
                            val ignoreFormattedDisplayName = TextUtils.formatDisplayname(ignore.displayName)
                            if (ignoreFormattedDisplayName != null && ignoreFormattedDisplayName == formattedDisplayName) {
                                ChatBox.writeGameMessage(
                                    LocalizedText.PLEASE_REMOVE_IGNORE.translate(client.CURRENT_LANGUAGE)!!
                                            + displayName + LocalizedText.FROM_IGNORE_FIRST.translate(client.CURRENT_LANGUAGE)!!
                                )
                                return
                            }
                        }
                    }
                    if (TextUtils.formatDisplayname(PlayerUpdate.MY_PLAYER!!.displayName!!) == formattedDisplayName) {
                        ChatBox.writeGameMessage(LocalizedText.CANT_ADD_SELF_TO_FRIEND.translate(client.CURRENT_LANGUAGE)!!)
                    } else {
                        val context = client.getConnectionContext()
                        val packet = PacketEncoder.createPacket(ClientProt.ADD_FRIEND, context.isaac)
                        packet.buffer.writeByte(TextUtils.getLength(displayName))
                        packet.buffer.writeString(displayName)
                        context.queuePacket(packet)
                    }
                }
            } else {
                ChatBox.writeGameMessage(LocalizedText.FRIEND_LIST_FULL.translate(client.CURRENT_LANGUAGE)!!)
            }
        }
    }

    @JvmStatic
    fun remove(displayName: String?) {
        if (displayName != null) {
            val deleted = TextUtils.formatDisplayname(displayName)
            if (deleted != null) {
                for (index in 0 until FRIEND_COUNT) {
                    val friend = FRIENDS[index]!!
                    val display = friend.displayName
                    val current = TextUtils.formatDisplayname(display)
                    if (TextUtils.matches(displayName, deleted, display, current, (-79).toByte())) {
                        if (--FRIEND_COUNT - index >= 0) {
                            System.arraycopy(FRIENDS, index + 1, FRIENDS, index, FRIEND_COUNT - index)
                        }
                        client.anInt7386 = client.anInt7347
                        val context = client.getConnectionContext()
                        val packet = PacketEncoder.createPacket(ClientProt.REMOVE_FRIEND, context.isaac)
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
    fun contains(displayName: String?): Boolean {
        if (displayName == null) {
            return false
        }
        for (index in 0 until FRIEND_COUNT) {
            if (displayName.equals(FRIENDS[index]!!.displayName, ignoreCase = true)) {
                return true
            }
        }
        return displayName.equals(PlayerUpdate.MY_PLAYER!!.displayName, ignoreCase = true)
    }

    @JvmStatic
    fun getIndex(displayName: String?): Int {
        if (displayName != null) {
            for (index in 0 until FRIEND_COUNT) {
                if (displayName.equals(FRIENDS[index]!!.displayName, ignoreCase = true)) {
                    return index
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun sort() {
        // Sort friends list by display name
        java.util.Arrays.sort(FRIENDS, 0, FRIEND_COUNT) { f1, f2 ->
            if (f1 == null || f1.displayName == null) return@sort 1
            if (f2 == null || f2.displayName == null) return@sort -1
            f1.displayName!!.compareTo(f2.displayName!!, ignoreCase = true)
        }
    }
}
