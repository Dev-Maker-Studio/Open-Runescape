package com.jagex.game.runetek5.interaction.chat.clanchat.channel

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.utils.ArrayUtils

class ClanChannel(buffer: Packet) : Node<ClanChannel>() {

    @JvmField var numPlayers: Int = 0
    @JvmField var clanName: String? = null
    @JvmField var players: Array<ClanChannelPlayer?>? = null
    @JvmField var minRankToKick: Byte = 0
    @JvmField var guestsTalk: Byte = 0
    @JvmField var namesAsString: Boolean = true
    @JvmField var sortedNameIndices: IntArray? = null
    @JvmField var namesAsLong: Boolean = false
    @JvmField var nextUpdateNumber: Long = 0

    init {
        decode(buffer)
    }

    fun decode(buffer: Packet) {
        val nameTypeFlag = buffer.readUByte()
        if ((nameTypeFlag and 0x1) != 0) {
            namesAsLong = true
        }
        if ((nameTypeFlag and 0x2) != 0) {
            namesAsString = true
        }
        pointer = buffer.readLong()
        nextUpdateNumber = buffer.readLong()
        clanName = buffer.readString()
        buffer.readUByte()
        minRankToKick = buffer.readByte()
        guestsTalk = buffer.readByte()
        numPlayers = buffer.readUShort()
        if (numPlayers > 0) {
            players = arrayOfNulls(numPlayers)
            for (index in 0 until numPlayers) {
                val player = ClanChannelPlayer()
                if (namesAsLong) {
                    buffer.readLong()
                }
                if (namesAsString) {
                    player.name = buffer.readString()
                }
                player.rank = buffer.readByte()
                player.world = buffer.readUShort()
                players!![index] = player
            }
        }
    }

    fun getPlayersSortedByName(): IntArray {
        if (sortedNameIndices == null) {
            val playerNames = Array(numPlayers) { "" }
            sortedNameIndices = IntArray(numPlayers)
            var i = 0
            while (i < numPlayers) {
                playerNames[i] = players!![i]!!.name!!
                sortedNameIndices!![i] = i++
            }
            @Suppress("UNCHECKED_CAST")
            ArrayUtils.quicksortt(playerNames as Array<String?>, sortedNameIndices!!)
        }
        return sortedNameIndices!!
    }

    fun removePlayer(playerIndex: Int) {
        if (--numPlayers == 0) {
            players = null
        } else {
            ArrayUtils.arrayCopy(players!!, playerIndex + 1, players!!, playerIndex, numPlayers - playerIndex)
        }
        sortedNameIndices = null
    }

    fun getUserSlot(playerName: String): Int {
        for (index in 0 until numPlayers) {
            if (players!![index]!!.name.equals(playerName, ignoreCase = true)) {
                return index
            }
        }
        return -1
    }

    fun addPlayer(player: ClanChannelPlayer) {
        if (players == null || numPlayers >= players!!.size) {
            expand(numPlayers + 5)
        }
        players!![numPlayers++] = player
        sortedNameIndices = null
    }

    fun expand(newSize: Int) {
        if (players != null) {
            val newPlayers = arrayOfNulls<ClanChannelPlayer>(newSize)
            ArrayUtils.arrayCopy(players!!, 0, newPlayers, 0, numPlayers)
            players = newPlayers
        } else {
            players = arrayOfNulls(newSize)
        }
    }
}
