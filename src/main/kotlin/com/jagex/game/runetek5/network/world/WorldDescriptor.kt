package com.jagex.game.runetek5.network.world

import com.darkan.bot.Bot
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.utils.TextUtils

open class WorldDescriptor : WorldDescriptorFlags() {

    @JvmField var worldNumber: Int = 0
    @JvmField var port: Int = -1
    @JvmField var ipAddress: String? = null
    @JvmField var unknown: String? = null
    @JvmField var ping = -1

    fun getWorld(): WorldMeta {
        return LoginManager.WORLDS!![worldIndex]
    }

    companion object {
        @JvmField var WORLD_LIST_START: Int = 0

        @JvmStatic
        fun getWorld(id: Int): WorldDescriptor? {
            return if (WorldMeta.WORLDS_LOADED && id >= WORLD_LIST_START && id <= WorldMeta.WORLD_LIST_SIZEPLUS1)
                WorldMeta.WORLD_LIST_DESCRIPTORS!![id - WORLD_LIST_START]
            else
                null
        }

        /**
         *
         * @param world1
         * @param world2
         * @param type
         * @param bool3
         * @return
         */
        @JvmStatic
        fun worldTypeChanged(world1: WorldDescriptor, world2: WorldDescriptor, type: Int, bool3: Boolean): Int {
            if (type == 1) {
                var playersWorld1 = world1.playerCount
                var playersWorld2 = world2.playerCount
                if (!bool3) {
                    if (playersWorld1 == -1) {
                        playersWorld1 = 2001
                    }

                    if (playersWorld2 == -1) {
                        playersWorld2 = 2001
                    }
                }

                return playersWorld1 - playersWorld2
            } else if (type == 2) {
                return TextUtils.method7557(world1.getWorld().activity!!, world2.getWorld().activity!!, client.CURRENT_LANGUAGE)
            } else if (type == 3) {
                return if ("-" == world1.unknown) (if ("-" == world2.unknown) 0 else (if (bool3) -1 else 1)) else (if ("-" == world2.unknown) (if (bool3) 1 else -1) else TextUtils.method7557(
                    world1.unknown!!,
                    world2.unknown!!,
                    client.CURRENT_LANGUAGE
                ))
            } else if (type == 4) {
                return if (world1.isLootshare()) (if (world2.isLootshare()) 0 else 1) else (if (world2.isLootshare()) -1 else 0)
            } else if (type == 5) {
                return if (world1.isQuickChat()) (if (world2.isQuickChat()) 0 else 1) else (if (world2.isQuickChat()) -1 else 0)
            } else if (type == 6) {
                return if (world1.isPvp()) (if (world2.isPvp()) 0 else 1) else (if (world2.isPvp()) -1 else 0)
            } else if (type == 7) {
                return if (world1.isMembers()) (if (world2.isMembers()) 0 else 1) else (if (world2.isMembers()) -1 else 0)
            } else if (type == 8) {
                var pingWorld1 = world1.ping
                var pingWorld2 = world2.ping
                if (bool3) {
                    if (pingWorld1 == 1000) {
                        pingWorld1 = -1
                    }

                    if (pingWorld2 == 1000) {
                        pingWorld2 = -1
                    }
                } else {
                    if (pingWorld1 == -1) {
                        pingWorld1 = 1000
                    }

                    if (pingWorld2 == -1) {
                        pingWorld2 = 1000
                    }
                }

                return pingWorld1 - pingWorld2
            }
            return world1.worldNumber - world2.worldNumber
        }

        @JvmStatic
        fun decodeWorldList(data: ByteArray): Boolean {
            val buffer = Packet(data)
            val decodeMode = buffer.readUByte()
            if (decodeMode != 2)
                return false
            val decodeWorlds = buffer.readUByte() == 1
            if (decodeWorlds)
                WorldMeta.decodeWorldList(buffer)
            PacketEncoder.decodePlayerCounts(buffer)
            return true
        }

        @JvmStatic
        fun method4325(world1: WorldDescriptor, world2: WorldDescriptor, type: Int, bool3: Boolean, i_4: Int, bool5: Boolean): Int {
            val i_7 = worldTypeChanged(world1, world2, type, bool3)
            if (i_7 != 0) {
                return if (bool3) -i_7 else i_7
            }
            if (i_4 == -1) {
                return 0
            }
            val i_8 = worldTypeChanged(world1, world2, i_4, bool5)
            return if (bool5) -i_8 else i_8
        }

        @JvmStatic
        fun get_world(): WorldDescriptor? {
            return getWorld(LoginManager.GAME_CONNECTION_INFO.worldId)
        }

        @JvmStatic
        fun getFirstWorldDescriptor(): WorldDescriptor? {
            WorldConnection.CURRENT_WORLD = 0
            return WorldConnection.getNextWorldDescriptor()
        }
    }
}
