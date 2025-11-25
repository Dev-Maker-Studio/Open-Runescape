package com.jagex.game.runetek5.network.world

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.network.PingRequest
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.staticmethods.StaticMethods

class WorldMeta {

    @JvmField var activity: String? = null
    @JvmField var countryId: Int = 0

    companion object {
        @JvmField var WORLD_LIST_BUFFER: Packet? = null
        @JvmField var WORLD_LIST_SIZE: Int = 0
        @JvmField var WORLDS_LOADED: Boolean = false
        @JvmField var WORLD_LIST_DESCRIPTORS: Array<WorldDescriptor?>? = null
        @JvmField var WORLD_LIST_SIZEPLUS1: Int = 0

        @JvmStatic
        fun decodeWorldList(buffer: Packet) {
            val size = buffer.readUSmart()
            LoginManager.WORLDS = Array(size) { WorldMeta() }
            for (i in 0 until size) {
                LoginManager.WORLDS!![i].countryId = buffer.readUSmart()
                LoginManager.WORLDS!![i].activity = buffer.readJagString()
                if (Loader.DEBUG)
                    println("Worldlist set: " + LoginManager.WORLDS!![i])
            }
            WorldDescriptor.WORLD_LIST_START = buffer.readUSmart()
            WORLD_LIST_SIZEPLUS1 = buffer.readUSmart()
            WORLD_LIST_SIZE = buffer.readUSmart()
            WORLD_LIST_DESCRIPTORS = arrayOfNulls(WORLD_LIST_SIZEPLUS1 - WorldDescriptor.WORLD_LIST_START + 1)
            for (i in 0 until WORLD_LIST_SIZE) {
                val id = buffer.readUSmart()
                val world = WorldDescriptor()
                WORLD_LIST_DESCRIPTORS!![id] = world
                world.worldIndex = buffer.readUByte()
                world.flags = buffer.readInt()
                world.worldNumber = id + WorldDescriptor.WORLD_LIST_START
                world.unknown = buffer.readJagString()
                world.ipAddress = buffer.readJagString()
                if (world.hasPort())
                    world.port = buffer.readInt()
                if (Loader.DEBUG)
                    println("Worldlist set: $world")
            }
            StaticMethods.WORLD_LIST_VERSION = buffer.readInt()
            WORLDS_LOADED = true
        }

        @JvmStatic
        fun pingWorlds() {
            if (!WorldConnection.PING_WORLDS)
                return

            val iterator = WorldConnection.WORLD_PING_REQUESTS.entries.iterator()

            while (iterator.hasNext()) {
                val entry = iterator.next()
                val worldIdx = entry.key
                val request = entry.value
                val world = WorldDescriptor.getWorld(worldIdx)

                if (world == null) {
                    iterator.remove()
                    continue
                }

                val elapsedTime = System.currentTimeMillis() - request.startTime
                val timedOut = elapsedTime > 5000

                if (request.ping != -1 || timedOut) {
                    if (timedOut && request.ping == -1) {
                        //System.out.println("Ping timeout for world " + world.ipAddress);
                        world.ping = 1000
                    } else {
                        //System.out.println("Ping: " + world.getWorld().activity + " (" + world.ipAddress + ") -> " + request.ping);
                        world.ping = request.ping
                    }

                    iterator.remove()
                } else if (elapsedTime > 1000 && (elapsedTime % 1000) < 50) {
                    //System.out.println("Waiting on " + worldIdx + " - " + world.ipAddress + " (" + elapsedTime/1000 + "s elapsed)");
                }
            }

            var availableSlots = 10 - WorldConnection.WORLD_PING_REQUESTS.size
            if (availableSlots > 0) {
                var worldIdx = WorldDescriptor.WORLD_LIST_START
                while (worldIdx < WORLD_LIST_SIZEPLUS1 && availableSlots > 0) {

                    if (!WorldConnection.WORLD_PING_REQUESTS.containsKey(worldIdx)) {
                        val world = WorldDescriptor.getWorld(worldIdx)
                        if (world != null && world.ping == -1 && !WorldConnection.WORLD_PING_REQUESTS.containsKey(worldIdx)) {
                            val request = client.PING_REQUESTER.createPingRequest(world.ipAddress)
                            request.startTime = System.currentTimeMillis()

                            WorldConnection.WORLD_PING_REQUESTS[worldIdx] = request
                            //System.out.println("Started ping request for " + world.getWorld().activity + " (" + world.ipAddress + ")");
                            availableSlots--
                        }
                    }
                    worldIdx++
                }
            }
        }
    }
}
