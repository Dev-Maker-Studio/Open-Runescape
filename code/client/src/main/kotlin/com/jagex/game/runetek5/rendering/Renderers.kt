package com.jagex.game.runetek5.rendering

import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.scene.AsyncMapLoader
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.preference.impl.ToolkitPreference
import com.jagex.game.runetek5.toolkit.AbstractToolkit

object Renderers {
    @JvmStatic
    fun multiSamplesFor(preference: Int): Int = System.getProperty("render.msaa")?.toIntOrNull() ?: preference

    @JvmField var activeToolkit: AbstractToolkit? = null
    @JvmField var FONT_RENDERER: AbstractFontRenderer? = null
    @JvmField var GPU_ITEM_RENDERER: AbstractToolkit? = null
    @JvmField var GPU_ITEM_FONT_RENDERER: AbstractFontRenderer? = null

    @JvmStatic
    fun cleanup() {
        if (GPU_ITEM_RENDERER != null) {
            GPU_ITEM_RENDERER!!.cleanupAndShutdown()
            GPU_ITEM_RENDERER = null
            GPU_ITEM_FONT_RENDERER = null
        }
    }

    @JvmStatic
    fun prepareForRendering(bootstrap: Boolean, verifySettingsPreUpdate: Boolean) {
        val toolkitId = if (bootstrap) AbstractToolkit.BOOTSTRAP_MODE else ToolkitPreference.MODERN_OPENGL
        activeToolkit = AbstractToolkit.create(toolkitId, GameShell.gameCanvas, TypeLists.MATERIALS, Resource.SHADER, multiSamplesFor(PreferenceManager.preferences!!.antiAliasingPreview!!.getValue() * 2))
        if (!bootstrap && activeToolkit!!.utilisesNativeHeap()) {
            val highMem = runCatching { SystemInfo.SYSTEM_INFO!!.ram > 256 }.getOrDefault(true)
            activeToolkit!!.setNativeHeap(activeToolkit!!.getNativeMemory(if (highMem) 146800640 else 104857600)!!)
        }
        if (verifySettingsPreUpdate) {
            PreferenceManager.preferences!!.verifySettings(PreferenceManager.preferences!!.activeToolkit!!, false)
        }
        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.activeToolkit!!, ToolkitPreference.MODERN_OPENGL)
        if (!verifySettingsPreUpdate) {
            PreferenceManager.preferences!!.verifySettings(PreferenceManager.preferences!!.activeToolkit!!, true)
        }
        cleanup()
        activeToolkit!!.initialize()
        activeToolkit!!.generateFlags()
        Resource.mapRegion!!.calculateViewRange()
        activeToolkit!!.resetTextureState()
        if (activeToolkit!!.hasBloomSupport()) {
            val bloomOverride = System.getProperty("render.bloom")
            val wantBloom = when {
                bloomOverride.equals("true", true) -> true
                bloomOverride.equals("false", true) -> false
                else -> PreferenceManager.preferences!!.bloom!!.getValue() == 1
            }
            PreferenceManager.setBloom(wantBloom)
        }
        Resource.mapRegion!!.resetAtmosphere()
        Resource.mapRegion!!.getEnvironmentManager().requestDirectEnvironmentTransition()
        val mapRegionLoader = Resource.mapRegionLoaderTask
        if (mapRegionLoader != null) {
            synchronized(mapRegionLoader.stateLock) {
                val loaderRegion = mapRegionLoader.getMapRegion()
                if (mapRegionLoader.state == AsyncMapLoader.State.IDLE && loaderRegion.environmentManager != null) {
                    loaderRegion.resetAtmosphere()
                }
            }
        }
        Client.preferencesSavedOnServer = false
        Client.needsResize = true
        WindowMode.activeDisplayModeDetails = null
        activeToolkit!!.resetDepthClippingMask()
    }
}
