package com.jagex.game.runetek5.network.world;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.network.WorldConnection;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

public class WorldMeta {

    public static Packet WORLD_LIST_BUFFER;
    public String activity;
    public int countryId;
	public static int WORLD_LIST_SIZE;
	public static boolean WORLDS_LOADED;
	public static WorldDescriptor[] WORLD_LIST_DESCRIPTORS;
	public static int WORLD_LIST_SIZEPLUS1;

    public static void decodeWorldList(Packet buffer) {
        int size = buffer.readUnsignedSmart();
        LoginManager.WORLDS = new WorldMeta[size];
        for (int i = 0; i < size; i++) {
            LoginManager.WORLDS[i] = new WorldMeta();
            LoginManager.WORLDS[i].countryId = buffer.readUnsignedSmart();
            LoginManager.WORLDS[i].activity = buffer.readGJString();
            if (Loader.DEBUG)
                System.out.println("Worldlist set: " + LoginManager.WORLDS[i]);
        }
        WorldDescriptor.WORLD_LIST_START = buffer.readUnsignedSmart();
        WorldMeta.WORLD_LIST_SIZEPLUS1 = buffer.readUnsignedSmart();
        WorldMeta.WORLD_LIST_SIZE = buffer.readUnsignedSmart();
        WorldMeta.WORLD_LIST_DESCRIPTORS = new WorldDescriptor[WorldMeta.WORLD_LIST_SIZEPLUS1 - WorldDescriptor.WORLD_LIST_START + 1];
        for (int i = 0; i < WorldMeta.WORLD_LIST_SIZE; i++) {
            int id = buffer.readUnsignedSmart();
            WorldDescriptor world = WorldMeta.WORLD_LIST_DESCRIPTORS[id] = new WorldDescriptor();
            world.worldIndex = buffer.readUnsignedByte();
            world.flags = buffer.readInt();
            world.worldNumber = id + WorldDescriptor.WORLD_LIST_START;
            world.unknown = buffer.readGJString();
            world.ipAddress = buffer.readGJString();
            if (world.hasPort())
                world.port = buffer.readInt();
            if (Loader.DEBUG)
                System.out.println("Worldlist set: " + world);
        }
        StaticMethods.WORLD_LIST_IDK = buffer.readInt();
        WorldMeta.WORLDS_LOADED = true;
    }

    public static void pingWorlds() {
        if (WorldConnection.PING_WORLDS) {
            if (WorldConnection.currentWorldPingIdx < WorldDescriptor.WORLD_LIST_START) {
                WorldConnection.currentWorldPingIdx = WorldDescriptor.WORLD_LIST_START;
            }

            while (true) {
                while (WorldConnection.currentWorldPingIdx < WorldMeta.WORLD_LIST_SIZEPLUS1) {
                    WorldDescriptor world = WorldDescriptor.getWorld(WorldConnection.currentWorldPingIdx);
                    if (world != null && world.ping == -1) {
                        if (WorldConnection.CURRENT_WORLD_PING_REQUEST == null) {
                            WorldConnection.CURRENT_WORLD_PING_REQUEST = client.PING_REQUESTER.createPingRequest(world.ipAddress);
                        }

                        int ping = WorldConnection.CURRENT_WORLD_PING_REQUEST.ping;

                        //System.out.println("Ping: " + world.ipAddress + "->" + ping);

                        if (ping == -1) {
                            return;
                        }

                        world.ping = ping;
                        ++WorldConnection.currentWorldPingIdx;
                        WorldConnection.CURRENT_WORLD_PING_REQUEST = null;
                    } else {
                        ++WorldConnection.currentWorldPingIdx;
                    }
                }

                return;
            }
        }
    }
}
