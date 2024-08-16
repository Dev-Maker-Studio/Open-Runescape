package com.jagex.game.runetek5.config;

import com.jagex.game.runetek5.cache.Decompressor;
import com.jagex.game.runetek5.cache.FileOnDisk;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.graphics.font.HelveticaFont;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.js5.library.NativeLibraryLoader;
import com.jagex.game.runetek5.js5.request.Js5NetQueue;
import com.jagex.game.runetek5.js5.request.local.LocalRequester;
import com.jagex.game.runetek5.js5.request.remote.RemoteFileWorker;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.AsyncMapLoader;

public class Resource {

    public static Js5ResourceProvider SPRITES;
    public static Js5ResourceProvider FRAME_SET;
    public static Js5ResourceProvider FRAME_BASE;
    public static Js5ResourceProvider CONFIG;
    public static Js5ResourceProvider INTERFACE;
    public static Js5ResourceProvider SOUND_EFFECT;
    public static Js5ResourceProvider MAPS;
    public static Js5ResourceProvider MUSIC;
    public static Js5ResourceProvider MODEL;
	public static Js5ResourceProvider MESH;
    public static Js5ResourceProvider TEXTURE;
    public static Js5ResourceProvider HUFFMAN;
    public static Js5ResourceProvider JINGLE;
    public static Js5ResourceProvider CS2;
    public static Js5ResourceProvider MIDI_INSTRUMENT;
    public static Js5ResourceProvider SOUND_MIDI; 
    public static Js5ResourceProvider LOC;
    public static Js5ResourceProvider ENUM;
    public static Js5ResourceProvider NPC;
    public static Js5ResourceProvider OBJ;
    public static Js5ResourceProvider SEQ;
    public static Js5ResourceProvider GFX;
    public static Js5ResourceProvider VARBIT;
    public static Js5ResourceProvider WORLD_MAP;
    public static Js5ResourceProvider QC_MESSAGES;
    public static Js5ResourceProvider QC_MENU;
    public static Js5ResourceProvider MATERIAL;
    public static Js5ResourceProvider PARTICLE;
    public static Js5ResourceProvider DEFAULTS;
    public static Js5ResourceProvider BILLBOARD;
    public static Js5ResourceProvider CUTSCENE;
    public static Js5ResourceProvider NATIVE_LIBRARY;
    public static Js5ResourceProvider SHADER;
    public static Js5ResourceProvider VORBIS;
    public static Js5ResourceProvider FONT;
    public static Js5ResourceProvider GAME_TIPS;
    public static Js5ResourceProvider FONT_METRICS;
	public static Js5ResourceProvider PARTICLE_PRODUCER;
    
	public static HelveticaFont helvetica11Font;
	public static HelveticaFont helvetica12Font;
	public static HelveticaFont helvetica14Font;
	public static HelveticaFont helvetica17Font;
	public static HelveticaFont helvetica19Font;
	public static HelveticaFont helvetica22Font;
	public static HelveticaFont helvetica26Font;
	public static HelveticaFont helvetica30Font;

    public static int gameTipsCrc;
    public static int fontCrc;
        
    public static AsyncMapLoader mapRegionLoaderTask;
    public static NativeLibraryLoader NATIVE_LIBRARY_LOADER;
    public static boolean USING_JAGEX_IMAGE_FORMAT;
    public static Decompressor referenceTableDecompressor;
	public static MapRegion mapRegion;
	public static RemoteFileWorker[] remoteFileWorkersByIndex;
	public static Js5NetQueue remoteFileRequesterJs5;
	public static LocalRequester localRequesterJs5;

    public static Js5ResourceProvider createProvider(IndexReference reference, boolean directMemoryMode, int loadType) {
        if (Resource.remoteFileWorkersByIndex == null) {
            Resource.remoteFileWorkersByIndex = new RemoteFileWorker[IndexReference.getMaxIndexReference()];
        }
        Decompressor cacheFile = null;
        int indexId = reference.getIndex();
        if (client.cacheFileData != null) {
            cacheFile = new Decompressor(indexId, client.cacheFileData, FileOnDisk.cacheFileIndices[indexId]);
        }
        Resource.remoteFileWorkersByIndex[indexId] = client.requestManager.getWorker(indexId, cacheFile, referenceTableDecompressor);
        Resource.remoteFileWorkersByIndex[indexId].reset();
        return new Js5ResourceProvider(Resource.remoteFileWorkersByIndex[indexId], directMemoryMode, loadType);
    }

    public static int getTipsCrc() {
        return gameTipsCrc;
    }
	

	public static int getFontCrc() {
	    return fontCrc;
	}
}
