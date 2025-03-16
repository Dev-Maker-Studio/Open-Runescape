package com.jagex.game.runetek5.network.world;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.network.PingRequest;
import com.jagex.game.runetek5.network.WorldConnection;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

import java.util.Iterator;
import java.util.Map;

public class WorldMeta {

    public static Packet WORLD_LIST_BUFFER;
    public String activity;
    public int countryId;
	public static int WORLD_LIST_SIZE;
	public static boolean WORLDS_LOADED;
	public static WorldDescriptor[] WORLD_LIST_DESCRIPTORS;
	public static int WORLD_LIST_SIZEPLUS1;

    public static void decodeWorldList(Packet buffer) {
        int size = buffer.readUSmart();
        LoginManager.WORLDS = new WorldMeta[size];
        for (int i = 0; i < size; i++) {
            LoginManager.WORLDS[i] = new WorldMeta();
            LoginManager.WORLDS[i].countryId = buffer.readUSmart();
            LoginManager.WORLDS[i].activity = buffer.readJagString();
            if (Loader.DEBUG)
                System.out.println("Worldlist set: " + LoginManager.WORLDS[i]);
        }
        WorldDescriptor.WORLD_LIST_START = buffer.readUSmart();
        WorldMeta.WORLD_LIST_SIZEPLUS1 = buffer.readUSmart();
        WorldMeta.WORLD_LIST_SIZE = buffer.readUSmart();
        WorldMeta.WORLD_LIST_DESCRIPTORS = new WorldDescriptor[WorldMeta.WORLD_LIST_SIZEPLUS1 - WorldDescriptor.WORLD_LIST_START + 1];
        for (int i = 0; i < WorldMeta.WORLD_LIST_SIZE; i++) {
            int id = buffer.readUSmart();
            WorldDescriptor world = WorldMeta.WORLD_LIST_DESCRIPTORS[id] = new WorldDescriptor();
            world.worldIndex = buffer.readUByte();
            world.flags = buffer.readInt();
            world.worldNumber = id + WorldDescriptor.WORLD_LIST_START;
            world.unknown = buffer.readJagString();
            world.ipAddress = buffer.readJagString();
            if (world.hasPort())
                world.port = buffer.readInt();
            if (Loader.DEBUG)
                System.out.println("Worldlist set: " + world);
        }
        StaticMethods.WORLD_LIST_VERSION = buffer.readInt();
        WorldMeta.WORLDS_LOADED = true;
    }

    public static void pingWorlds() {
        if (!WorldConnection.PING_WORLDS)
            return;

        Iterator<Map.Entry<Integer, PingRequest>> iterator = WorldConnection.WORLD_PING_REQUESTS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, PingRequest> entry = iterator.next();
            Integer worldIdx = entry.getKey();
            PingRequest request = entry.getValue();
            WorldDescriptor world = WorldDescriptor.getWorld(worldIdx);

            if (world == null) {
                iterator.remove();
                continue;
            }

            long elapsedTime = System.currentTimeMillis() - request.startTime;
            boolean timedOut = elapsedTime > 5000;

            if (request.ping != -1 || timedOut) {
                if (timedOut && request.ping == -1) {
                    //System.out.println("Ping timeout for world " + world.ipAddress);
                    world.ping = 1000;
                } else {
                    //System.out.println("Ping: " + world.getWorld().activity + " (" + world.ipAddress + ") -> " + request.ping);
                    world.ping = request.ping;
                }

                iterator.remove();
            } else if (elapsedTime > 1000 && (elapsedTime % 1000) < 50) {
                //System.out.println("Waiting on " + worldIdx + " - " + world.ipAddress + " (" + elapsedTime/1000 + "s elapsed)");
            }
        }

        int availableSlots = 10 - WorldConnection.WORLD_PING_REQUESTS.size();
        if (availableSlots > 0) {
            for (int worldIdx = WorldDescriptor.WORLD_LIST_START;
                 worldIdx < WorldMeta.WORLD_LIST_SIZEPLUS1 && availableSlots > 0;
                 worldIdx++) {

                if (WorldConnection.WORLD_PING_REQUESTS.containsKey(worldIdx))
                    continue;

                WorldDescriptor world = WorldDescriptor.getWorld(worldIdx);
                if (world != null && world.ping == -1 && !WorldConnection.WORLD_PING_REQUESTS.containsKey(worldIdx)) {
                    PingRequest request = client.PING_REQUESTER.createPingRequest(world.ipAddress);
                    request.startTime = System.currentTimeMillis();

                    WorldConnection.WORLD_PING_REQUESTS.put(worldIdx, request);
                    //System.out.println("Started ping request for " + world.getWorld().activity + " (" + world.ipAddress + ")");
                    availableSlots--;
                }
            }
        }
    }
}
