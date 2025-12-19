package com.jagex.game.runetek5.config

import com.jagex.game.runetek5.cache.Decompressor
import com.jagex.game.runetek5.cache.FileOnDisk
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.graphics.font.HelveticaFont
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.js5.library.NativeLibraryLoader
import com.jagex.game.runetek5.js5.request.Js5NetQueue
import com.jagex.game.runetek5.js5.request.local.LocalRequester
import com.jagex.game.runetek5.js5.request.remote.RemoteFileWorker
import com.jagex.game.runetek5.scene.AsyncMapLoader
import com.jagex.game.runetek5.scene.MapRegion

object Resource {

    @JvmField
    var SPRITES: Js5ResourceProvider? = null

    @JvmField
    var FRAME_SET: Js5ResourceProvider? = null

    @JvmField
    var FRAME_BASE: Js5ResourceProvider? = null

    @JvmField
    var CONFIG: Js5ResourceProvider? = null

    @JvmField
    var INTERFACE: Js5ResourceProvider? = null

    @JvmField
    var SOUND_EFFECT: Js5ResourceProvider? = null

    @JvmField
    var MAPS: Js5ResourceProvider? = null

    @JvmField
    var MUSIC: Js5ResourceProvider? = null

    @JvmField
    var MODEL: Js5ResourceProvider? = null

    @JvmField
    var MESH: Js5ResourceProvider? = null

    @JvmField
    var TEXTURE: Js5ResourceProvider? = null

    @JvmField
    var HUFFMAN: Js5ResourceProvider? = null

    @JvmField
    var JINGLE: Js5ResourceProvider? = null

    @JvmField
    var CS2: Js5ResourceProvider? = null

    @JvmField
    var MIDI_INSTRUMENT: Js5ResourceProvider? = null

    @JvmField
    var SOUND_MIDI: Js5ResourceProvider? = null

    @JvmField
    var LOC: Js5ResourceProvider? = null

    @JvmField
    var ENUM: Js5ResourceProvider? = null

    @JvmField
    var NPC: Js5ResourceProvider? = null

    @JvmField
    var OBJ: Js5ResourceProvider? = null

    @JvmField
    var SEQ: Js5ResourceProvider? = null

    @JvmField
    var GFX: Js5ResourceProvider? = null

    @JvmField
    var VARBIT: Js5ResourceProvider? = null

    @JvmField
    var WORLD_MAP: Js5ResourceProvider? = null

    @JvmField
    var QC_MESSAGES: Js5ResourceProvider? = null

    @JvmField
    var QC_MENU: Js5ResourceProvider? = null

    @JvmField
    var MATERIAL: Js5ResourceProvider? = null

    @JvmField
    var PARTICLE: Js5ResourceProvider? = null

    @JvmField
    var DEFAULTS: Js5ResourceProvider? = null

    @JvmField
    var BILLBOARD: Js5ResourceProvider? = null

    @JvmField
    var CUTSCENE: Js5ResourceProvider? = null

    @JvmField
    var NATIVE_LIBRARY: Js5ResourceProvider? = null

    @JvmField
    var SHADER: Js5ResourceProvider? = null

    @JvmField
    var VORBIS: Js5ResourceProvider? = null

    @JvmField
    var FONT: Js5ResourceProvider? = null

    @JvmField
    var GAME_TIPS: Js5ResourceProvider? = null

    @JvmField
    var FONT_METRICS: Js5ResourceProvider? = null

    @JvmField
    var PARTICLE_PRODUCER: Js5ResourceProvider? = null

    @JvmField
    var helvetica11Font: HelveticaFont? = null

    @JvmField
    var helvetica12Font: HelveticaFont? = null

    @JvmField
    var helvetica14Font: HelveticaFont? = null

    @JvmField
    var helvetica17Font: HelveticaFont? = null

    @JvmField
    var helvetica19Font: HelveticaFont? = null

    @JvmField
    var helvetica22Font: HelveticaFont? = null

    @JvmField
    var helvetica26Font: HelveticaFont? = null

    @JvmField
    var helvetica30Font: HelveticaFont? = null

    @JvmField
    var gameTipsCrc: Int = 0

    @JvmField
    var fontCrc: Int = 0

    @JvmField
    var mapRegionLoaderTask: AsyncMapLoader? = null

    @JvmField
    var NATIVE_LIBRARY_LOADER: NativeLibraryLoader? = null

    @JvmField
    var USING_JAGEX_IMAGE_FORMAT: Boolean = false

    @JvmField
    var referenceTableDecompressor: Decompressor? = null

    @JvmField
    var mapRegion: MapRegion? = null

    @JvmField
    var remoteFileWorkersByIndex: Array<RemoteFileWorker?>? = null

    @JvmField
    var remoteFileRequesterJs5: Js5NetQueue? = null

    @JvmField
    var localRequesterJs5: LocalRequester? = null

    @JvmStatic
    fun createProvider(reference: IndexReference, directMemoryMode: Boolean, loadType: Int): Js5ResourceProvider {
        if (remoteFileWorkersByIndex == null) {
            remoteFileWorkersByIndex = arrayOfNulls(IndexReference.getMaxIndexReference())
        }
        var cacheFile: Decompressor? = null
        val indexId = reference.getIndex()
        if (GameShell.cacheFileData != null) {
            cacheFile = Decompressor(indexId, GameShell.cacheFileData!!, FileOnDisk.cacheFileIndices!![indexId]!!)
        }
        remoteFileWorkersByIndex!![indexId] = client.requestManager.getWorker(indexId, cacheFile, referenceTableDecompressor)
        remoteFileWorkersByIndex!![indexId]!!.reset()
        return Js5ResourceProvider(remoteFileWorkersByIndex!![indexId]!!, directMemoryMode, loadType)
    }

    @JvmStatic
    fun getTipsCrc(): Int {
        return gameTipsCrc
    }

    @JvmStatic
    fun getFontCrc(): Int {
        return fontCrc
    }
}
