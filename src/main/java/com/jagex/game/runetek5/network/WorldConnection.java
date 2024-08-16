package com.jagex.game.runetek5.network;

import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.network.world.WorldDescriptor;

public class WorldConnection {

    public static ConnectionInfo GAME_CONNECTION;
    public static ConnectionInfo CONNECTION_INFO;
    public static ConnectionInfo LOBBY_CONNECTION_INFO;
    public static long aLong5425;
    public static boolean reconnectingWorld;
    public static ConnectionInfo connectionInfo;
    public static boolean aBool5422;
    public static boolean PING_WORLDS;
    public static WorldDescriptor[] WORLD_DESCRIPTORS_BYID = new WorldDescriptor[0];
    public static int CURRENT_WORLD = 1000336;
    public static int currentWorldPingIdx;
    public static PingRequest CURRENT_WORLD_PING_REQUEST;
	public static Connection gameConnection;

    WorldConnection() {
        throw new Error();
    }

	public static void setLobbyConnection(String host, int port) {
	    if (LOBBY_CONNECTION_INFO == null) {
	        LOBBY_CONNECTION_INFO = new ConnectionInfo();
	    }
	    LOBBY_CONNECTION_INFO.host = host;
	    LOBBY_CONNECTION_INFO.worldId = port + 1099;
	    LOBBY_CONNECTION_INFO.devWorld1 = LOBBY_CONNECTION_INFO.worldId;
	    LOBBY_CONNECTION_INFO.devWorld2 = LOBBY_CONNECTION_INFO.worldId;
	}

	public static void dropClient() {
	    if (!GameState.inLobby(client.gameState) && !GameState.isLoggedOut(client.gameState)) {
	        WorldConnection.gameConnection = client.GAME_CONNECTION_CONTEXT.getConnection();
	        client.GAME_CONNECTION_CONTEXT.reset();
	        client.setGameState(10);
	    } else {
	        client.logout(false);
	    }
	}

	public static void quicksort(int srcPos, int size, int i_2, boolean bool3, int i_4, boolean bool5) {
	    if (srcPos < size) {
	        int center = (srcPos + size) / 2;
	        int currentPosition = srcPos;
	        WorldDescriptor class217Sub19 = WORLD_DESCRIPTORS_BYID[center];
	        WORLD_DESCRIPTORS_BYID[center] = WORLD_DESCRIPTORS_BYID[size];
	        WORLD_DESCRIPTORS_BYID[size] = class217Sub19;
	        for (int i = srcPos; i < size; i++) {
	            if (WorldDescriptor.method4325(WORLD_DESCRIPTORS_BYID[i], class217Sub19, i_2, bool3, i_4, bool5) <= 0) {
	                WorldDescriptor class217Sub111 = WORLD_DESCRIPTORS_BYID[i];
	                WORLD_DESCRIPTORS_BYID[i] = WORLD_DESCRIPTORS_BYID[currentPosition];
	                WORLD_DESCRIPTORS_BYID[currentPosition++] = class217Sub111;
	            }
	        }
	        WORLD_DESCRIPTORS_BYID[size] = WORLD_DESCRIPTORS_BYID[currentPosition];
	        WORLD_DESCRIPTORS_BYID[currentPosition] = class217Sub19;
	        quicksort(srcPos, currentPosition - 1, i_2, bool3, i_4, bool5);
	        quicksort(currentPosition + 1, size, i_2, bool3, i_4, bool5);
	    }
	}

	public static void method6299(int i_0, boolean bool1, int i_2, boolean bool3) {
	    quicksort(0, WORLD_DESCRIPTORS_BYID.length - 1, i_0, bool1, i_2, bool3);
	    currentWorldPingIdx = 0;
	    CURRENT_WORLD_PING_REQUEST = null;
	}

	public static WorldDescriptor getNextWorldDescriptor() {
	    return CURRENT_WORLD < WORLD_DESCRIPTORS_BYID.length ? WORLD_DESCRIPTORS_BYID[++CURRENT_WORLD - 1] : null;
	}

}
