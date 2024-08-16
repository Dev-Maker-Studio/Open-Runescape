package com.jagex.game.runetek5.network.world;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.network.WorldConnection;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.utils.TextUtils;

public class WorldDescriptor extends WorldDescriptorFlags {

    public static int WORLD_LIST_START;
    public int worldNumber;
    public int port;
    public String ipAddress;
    public String unknown;
    public int ping = -1;

    public static WorldDescriptor getWorld(int id) {
        return WorldMeta.WORLDS_LOADED && id >= WORLD_LIST_START && id <= WorldMeta.WORLD_LIST_SIZEPLUS1 ? WorldMeta.WORLD_LIST_DESCRIPTORS[id - WORLD_LIST_START] : null;
    }

    /**
     * 
     * @param world1
     * @param world2
     * @param type
     * @param bool3
     * @return
     */
    public static int worldTypeChanged(WorldDescriptor world1, WorldDescriptor world2, int type, boolean bool3) {
        if (type == 1) {
            int playersWorld1 = world1.playerCount;
            int playersWorld2 = world2.playerCount;
            if (!bool3) {
                if (playersWorld1 == -1) {
                    playersWorld1 = 2001;
                }

                if (playersWorld2 == -1) {
                    playersWorld2 = 2001;
                }
            }

            return playersWorld1 - playersWorld2;
        } else if (type == 2) {
            return TextUtils.method7557(world1.getWorld().activity, world2.getWorld().activity, client.CURRENT_LANGUAGE);
        } else if (type == 3) {
            return "-".equals(world1.unknown) ? ("-".equals(world2.unknown) ? 0 : (bool3 ? -1 : 1)) : ("-".equals(world2.unknown) ? (bool3 ? 1 : -1) : TextUtils.method7557(world1.unknown, world2.unknown, client.CURRENT_LANGUAGE));
        } else if (type == 4) {
            return world1.isLootshare() ? (world2.isLootshare() ? 0 : 1) : (world2.isLootshare() ? -1 : 0);
        } else if (type == 5) {
            return world1.isQuickChat() ? (world2.isQuickChat() ? 0 : 1) : (world2.isQuickChat() ? -1 : 0);
        } else if (type == 6) {
            return world1.isPvp() ? (world2.isPvp() ? 0 : 1) : (world2.isPvp() ? -1 : 0);
        } else if (type == 7) {
            return world1.isMembers() ? (world2.isMembers() ? 0 : 1) : (world2.isMembers() ? -1 : 0);
        } else if (type == 8) {
            int pingWorld1 = world1.ping;
            int pingWorld2 = world2.ping;
            if (bool3) {
                if (pingWorld1 == 1000) {
                    pingWorld1 = -1;
                }

                if (pingWorld2 == 1000) {
                    pingWorld2 = -1;
                }
            } else {
                if (pingWorld1 == -1) {
                    pingWorld1 = 1000;
                }

                if (pingWorld2 == -1) {
                    pingWorld2 = 1000;
                }
            }

            return pingWorld1 - pingWorld2;
        } 
        return world1.worldNumber - world2.worldNumber;
    }

    public static boolean decodeWorldList(byte[] data) {
        Packet buffer = new Packet(data);
        int two = buffer.readUnsignedByte();
        if (two != 2) {
            return false;
        }
        boolean create = buffer.readUnsignedByte() == 1;
        if (create) {
            WorldMeta.decodeWorldList(buffer);
        }

        PacketEncoder.decodePlayerCounts(buffer);
        return true;
    }

    public static int method4325(WorldDescriptor world1, WorldDescriptor world2, int type, boolean bool3, int i_4, boolean bool5) {
        int i_7 = worldTypeChanged(world1, world2, type, bool3);
        if (i_7 != 0) {
            return bool3 ? -i_7 : i_7;
        } 
        if (i_4 == -1) {
            return 0;
        } 
        int i_8 = worldTypeChanged(world1, world2, i_4, bool5);
        return bool5 ? -i_8 : i_8;
    }

    public WorldMeta getWorld() {
        return LoginManager.WORLDS[super.worldIndex];
    }

	public static WorldDescriptor get_world() {
	    return getWorld(LoginManager.GAME_CONNECTION_INFO.worldId);
	}

	public static WorldDescriptor getFirstWorldDescriptor() {
	    WorldConnection.CURRENT_WORLD = 0;
	    return WorldConnection.getNextWorldDescriptor();
	}
}
