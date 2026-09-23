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
import com.jagex.game.runetek5.toolkit.AbstractToolkit

object Renderers {
    /** -Drender.msaa=N overrides the anti-aliasing preference's sample count (debug/testing). */
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
    fun prepareForRendering(toolkitId: Int, topleftScreenText: String?, verifySettingsPreUpdate: Boolean) {
        // GUARD: Prevent re-initializing the same toolkit that's already active.
        // This destroys all GL state (textures, shaders, buffers) and recreates them,
        // but any previously loaded textures become invalid handles = black rendering.
        val currentToolkitId = PreferenceManager.preferences?.activeToolkit?.getValue() ?: -1
        if (currentToolkitId == toolkitId && activeToolkit != null) {
            println("[Renderers] Toolkit $toolkitId is already active — skipping re-initialization")
            return
        }
        if (toolkitId == 0) {
            activeToolkit = AbstractToolkit.create(0, GameShell.gameCanvas, TypeLists.MATERIALS, Resource.SHADER, multiSamplesFor(PreferenceManager.preferences!!.antiAliasingPreview!!.getValue() * 2))
            if (topleftScreenText != null) {
                activeToolkit!!.applyClearFlags(1, 0)
                val fontMetrics = FontMetrics.getFontMetrics(Resource.FONT_METRICS, FontManager.p12FullIndex)
                val fontRenderer = activeToolkit!!.createFontRenderer(fontMetrics, SpriteType.decodeGroup(Resource.SPRITES, FontManager.p12FullIndex), true)
                WindowMode.fillWindowRect()
                MessageBox.draw(topleftScreenText, true, activeToolkit!!, fontRenderer, fontMetrics)
            }
        } else {
            var generatedToolkit: AbstractToolkit? = null
            var fontMetrics: FontMetrics?
            var fontRenderer: AbstractFontRenderer?
            if (topleftScreenText != null) {
                generatedToolkit = AbstractToolkit.create(0, GameShell.gameCanvas, TypeLists.MATERIALS, Resource.SHADER, 0)
                generatedToolkit.applyClearFlags(1, 0)
                fontMetrics = FontMetrics.getFontMetrics(Resource.FONT_METRICS, FontManager.p12FullIndex)
                fontRenderer = generatedToolkit.createFontRenderer(fontMetrics, SpriteType.decodeGroup(Resource.SPRITES, FontManager.p12FullIndex), true)
                WindowMode.fillWindowRect()
                MessageBox.draw(topleftScreenText, true, generatedToolkit, fontRenderer, fontMetrics)
            }
            try {
                try {
                    activeToolkit = AbstractToolkit.create(toolkitId, GameShell.gameCanvas, TypeLists.MATERIALS, Resource.SHADER, multiSamplesFor(PreferenceManager.preferences!!.antiAliasingPreview!!.getValue() * 2))
                    if (topleftScreenText != null) {
                        generatedToolkit!!.applyClearFlags(1, 0)
                        fontMetrics = FontMetrics.getFontMetrics(Resource.FONT_METRICS, FontManager.p12FullIndex)
                        fontRenderer = generatedToolkit.createFontRenderer(fontMetrics, SpriteType.decodeGroup(Resource.SPRITES, FontManager.p12FullIndex), true)
                        WindowMode.fillWindowRect()
                        MessageBox.draw(topleftScreenText, true, generatedToolkit, fontRenderer, fontMetrics)
                    }
                    if (activeToolkit!!.utilisesNativeHeap()) {
                        var highMem = true
                        try {
                            highMem = SystemInfo.SYSTEM_INFO!!.ram > 256
                        } catch (throwable: Throwable) {
                            // NOOP
                        }
                        val nativeHeap: NativeMemoryNode
                        if (highMem) {
                            nativeHeap = activeToolkit!!.getNativeMemory(146800640)!!
                        } else {
                            nativeHeap = activeToolkit!!.getNativeMemory(104857600)!!
                        }
                        activeToolkit!!.setNativeHeap(nativeHeap)
                    }
                } catch (throwable: Throwable) {
                    val toolkitSetting = PreferenceManager.preferences!!.activeToolkit!!.getValue()
                    PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.activeToolkit!!, 0)
                    prepareForRendering(toolkitSetting, topleftScreenText, verifySettingsPreUpdate)
                    if (generatedToolkit != null) {
                        try {
                            generatedToolkit.cleanupAndShutdown()
                        } catch (subThrowable: Throwable) {
                            // NOOP
                        }
                    }
                    return
                }
                if (generatedToolkit != null) {
                    try {
                        generatedToolkit.cleanupAndShutdown()
                    } catch (throwable: Throwable) {
                        // NOOP
                    }
                }
            } catch (exception: Exception) {
                if (generatedToolkit != null) {
                    try {
                        generatedToolkit.cleanupAndShutdown()
                    } catch (throwable: Throwable) {
                        // NOOP
                    }
                }
            }
        }
        if (verifySettingsPreUpdate) {
            PreferenceManager.preferences!!.verifySettings(PreferenceManager.preferences!!.activeToolkit!!, false)
        }
        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.activeToolkit!!, toolkitId)
        if (!verifySettingsPreUpdate) {
            PreferenceManager.preferences!!.verifySettings(PreferenceManager.preferences!!.activeToolkit!!, true)
        }
        cleanup()
        activeToolkit!!.initialize()
        activeToolkit!!.generateFlags()
        Resource.mapRegion!!.calculateViewRange()
        activeToolkit!!.resetTextureState()
        if (activeToolkit!!.hasBloomSupport()) {
            // -Drender.bloom=true/false overrides the saved preference, so the bloom
            // post-process can be exercised without changing the user's settings.
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
