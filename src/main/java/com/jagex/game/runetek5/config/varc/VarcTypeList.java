package com.jagex.game.runetek5.config.varc;

import java.io.IOException;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.JFS32;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class VarcTypeList {

    public final int size;
    public final LRUCache varcTypeCache = new LRUCache(64);
    public final Js5ResourceProvider varcTypeResource;
	public static int[] VARC_INT;
	public static boolean[] VARC_BOOL;
	public static String[] VARC_STRING;

    public VarcTypeList(Js5ResourceProvider varctypeResource) {
        this.varcTypeResource = varctypeResource;
        this.size = this.varcTypeResource.getArchiveFileCount(SharedConfigsType.VARC.id);
    }

    public VarcType list(int id) {
        VarcType varcType;
        synchronized (this.varcTypeCache) {
            varcType = (VarcType) this.varcTypeCache.get(id);
        }

        if (varcType != null) {
            return varcType;
        } 
        byte[] varcData;
        synchronized (this.varcTypeResource) {
            varcData = this.varcTypeResource.getFile(SharedConfigsType.VARC.id, id);
        }

        varcType = new VarcType();
        if (varcData != null) {
            varcType.decode(new Packet(varcData));
        }
        synchronized (this.varcTypeCache) {
            this.varcTypeCache.put(varcType, id);
            return varcType;
        }
    }

	public static void readVarcIntPreferences() {
	    JFS32 jagexFilesystem = null;
	    try {
	        jagexFilesystem = PreferenceManager.loadPreferenceFile("2", client.CURRENT_GAME.name, false);
	        byte[] data = new byte[(int) jagexFilesystem.length()];
	        int seekOffset;
	        for (int position = 0; position < data.length; position += seekOffset) {
	            seekOffset = jagexFilesystem.getSeekOffset(data, position, data.length - position);
	            if (seekOffset == -1) {
	                throw new IOException();
	            }
	        }
	        decodeVarcInt(new Packet(data));
	    } catch (Exception exception) {
	    	// NOOP
	    }
	    try {
	        if (jagexFilesystem != null) {
	            jagexFilesystem.close();
	        }
	    } catch (Exception exception) {
	    	// NOOP
	    }
	}

	public static void decodeVarcInt(Packet buffer) {
	    if (buffer.buffer.length - buffer.currentPosition >= 1) {
	        int i_2 = buffer.readUByte();
	        if (i_2 >= 0 && i_2 <= 1 && buffer.buffer.length - buffer.currentPosition >= 2) {
	            int size = buffer.readUShort();
	            if (buffer.buffer.length - buffer.currentPosition >= size * 6) {
	                for (int index = 0; index < size; index++) {
	                    int varcId = buffer.readUShort();
	                    int varcState = buffer.readInt();
	                    if (varcId < VarcTypeList.VARC_INT.length && VarcTypeList.VARC_BOOL[varcId] && (TypeLists.VARC.list(varcId).aChar4984 != 49 || varcState >= -1 && varcState <= 1)) {
	                        VarcTypeList.VARC_INT[varcId] = varcState;
	                    }
	                }
	            }
	        }
	    }	
	}
}
