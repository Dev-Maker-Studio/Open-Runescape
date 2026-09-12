package com.jagex.game.runetek5.client

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.JFS32
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.preference.GamePreferences
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.MapSize
import com.jagex.game.runetek5.systeminfo.ProfileCpu
import com.jagex.game.runetek5.systeminfo.SystemInfo
import java.io.File
import java.io.IOException

object PreferenceManager {
    @JvmField
    var preferences: GamePreferences? = null

    @JvmField
    var preferenceFile: File? = null

    @JvmField
    var preferencesParentFile: String? = null

    @JvmStatic
    fun getGamePreferences(): GamePreferences {
        var fileSystem: JFS32? = null
        var prefs = GamePreferences(Client.CURRENT_GAME!!)

        try {
            fileSystem = loadPreferenceFile("", Client.CURRENT_GAME!!.name, false)
            val fsBytes = ByteArray(fileSystem.length().toInt())

            var offset = 0
            while (offset < fsBytes.size) {
                val seekOffset = fileSystem.getSeekOffset(fsBytes, offset, fsBytes.size - offset)
                if (seekOffset == -1) {
                    throw IOException()
                }
                offset += seekOffset
            }

            prefs = GamePreferences(Packet(fsBytes), Client.CURRENT_GAME!!)
        } catch (exception: Exception) {
            println("[PreferenceManager] stored preferences could not be read, using defaults: $exception")
        }

        try {
            fileSystem?.close()
        } catch (exception: Exception) {
            // NOOP
        }

        return prefs
    }

    @JvmStatic
    fun loadPreferenceFile(preferenceUid: String, preferencePrefix: String, singleFile: Boolean): JFS32 {
        val prefsFile = File(preferenceFile, "preferences$preferenceUid.dat")
        if (prefsFile.exists()) {
            try {
                return JFS32(prefsFile, 10000L)
            } catch (ignored: IOException) {
                ignored.printStackTrace()
            }
        }
        val storePostfix = when (SystemInfo.storeId) {
            33 -> "_rc"
            34 -> "_wip"
            else -> ""
        }
        val prefsParentFile = File(preferencesParentFile + File.separatorChar + Loader.CACHE_DIR, "dk_${preferencePrefix}_preferences$preferenceUid$storePostfix.dat")
        if (!singleFile && prefsParentFile.exists()) {
            try {
                return JFS32(prefsParentFile, 10000L)
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
        }
        try {
            return JFS32(prefsFile, 10000L)
        } catch (ioe: IOException) {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun savePreferences() {
        var preferenceFile: JFS32? = null

        try {
            val preferenceBuffer = preferences!!.encode()
            preferenceFile = loadPreferenceFile("", Client.CURRENT_GAME!!.name, true)
            preferenceFile.write(preferenceBuffer.buffer, 0, preferenceBuffer.currentPosition)
        } catch (exception: Exception) {
            println("[PreferenceManager] preferences could not be saved: $exception")
        }

        try {
            preferenceFile?.close()
        } catch (exception: Exception) {
            // NOOP
        }
    }

    @JvmStatic
    fun setHardwarePerformance(renderMode: Int, profilingPerformance: Int): Int {
        val mode = when {
            profilingPerformance > 20077 -> {
                loadHighDetailPreferences()
                4
            }
            profilingPerformance > 10021 -> {
                loadMidDetailPreferences()
                3
            }
            profilingPerformance > 5002 -> {
                loadLowDetailPreferences()
                2
            }
            else -> {
                loadMinDetailPreferences()
                1
            }
        }

        if (preferences!!.activeToolkit!!.getValue() != renderMode) {
            preferences!!.setValue(preferences!!.toolkit!!, renderMode)
            Client.loadAssets(renderMode, false)
        } else {
            preferences!!.verifySettings(preferences!!.activeToolkit!!, true)
        }

        savePreferences()
        return mode
    }

    @JvmStatic
    fun calculateDisplayMode(): Int {
        val graphicsSettings = if (GameShell.maxMemory >= 96) {
            val benchmarkExecutionTime = ProfileCpu.getProfile()
            when {
                benchmarkExecutionTime <= 104 -> {
                    loadHighDetailPreferences()
                    4
                }
                benchmarkExecutionTime <= 504 -> {
                    loadMidDetailPreferences()
                    3
                }
                benchmarkExecutionTime <= 1002 -> {
                    loadLowDetailPreferences()
                    2
                }
                else -> {
                    loadMinDetailPreferences()
                    1
                }
            }
        } else {
            loadMinDetailPreferences()
            1
        }
        if (preferences!!.activeToolkit!!.getValue() != 0) {
            preferences!!.setValue(preferences!!.toolkit!!, 0)
            Client.loadAssets(0, false)
        } else {
            preferences!!.verifySettings(preferences!!.activeToolkit!!, true)
        }
        savePreferences()
        return graphicsSettings
    }

    @JvmStatic
    fun loadMinDetailPreferences() {
        preferences!!.setValue(preferences!!.removeRoofs!!, 1)
        preferences!!.setValue(preferences!!.removeRoofsOptionOverride!!, 1)
        preferences!!.setValue(preferences!!.groundDecoration!!, 0)
        preferences!!.setValue(preferences!!.fog!!, 0)
        preferences!!.setValue(preferences!!.goundBlending!!, 0)
        preferences!!.setValue(preferences!!.idleAnimations!!, 0)
        preferences!!.setValue(preferences!!.flickeringEffects!!, 0)
        preferences!!.setValue(preferences!!.characterShadows!!, 0)
        preferences!!.setValue(preferences!!.sceneryShadows!!, 0)
        preferences!!.setValue(preferences!!.textures!!, 0)
        preferences!!.setValue(preferences!!.lightDetail!!, 0)
        preferences!!.setValue(preferences!!.water!!, 0)
        preferences!!.setValue(preferences!!.antiAliasing!!, 0)
        preferences!!.setValue(preferences!!.antiAliasingPreview!!, 0)
        preferences!!.setValue(preferences!!.particles!!, 0)
        preferences!!.setValue(preferences!!.mapSize!!, MapSize.MAPSIZE_104_DEFAULT.id)
        preferences!!.setValue(preferences!!.bloom!!, 0)
        preferences!!.setValue(preferences!!.skyboxes!!, 0)
        setCpuPreference()
        preferences!!.setValue(preferences!!.maxScreenSize!!, 2)
        preferences!!.setValue(preferences!!.graphics!!, 1)
        GraphicsCache.resetGraphicCaches()
        Resource.mapRegion!!.getEnvironmentManager().requestDirectEnvironmentTransition()
        Client.needsResize = true
    }

    @JvmStatic
    fun loadLowDetailPreferences() {
        preferences!!.setValue(preferences!!.removeRoofs!!, 1)
        preferences!!.setValue(preferences!!.removeRoofsOptionOverride!!, 1)
        preferences!!.setValue(preferences!!.groundDecoration!!, 1)
        preferences!!.setValue(preferences!!.goundBlending!!, 1)
        preferences!!.setValue(preferences!!.idleAnimations!!, 0)
        preferences!!.setValue(preferences!!.flickeringEffects!!, 0)
        preferences!!.setValue(preferences!!.characterShadows!!, 0)
        preferences!!.setValue(preferences!!.sceneryShadows!!, 0)
        preferences!!.setValue(preferences!!.textures!!, 0)
        preferences!!.setValue(preferences!!.lightDetail!!, 0)
        preferences!!.setValue(preferences!!.water!!, 0)
        preferences!!.setValue(preferences!!.fog!!, 0)
        preferences!!.setValue(preferences!!.antiAliasing!!, 0)
        preferences!!.setValue(preferences!!.antiAliasingPreview!!, 0)
        preferences!!.setValue(preferences!!.particles!!, 0)
        preferences!!.setValue(preferences!!.mapSize!!, MapSize.MAPSIZE_104_DEFAULT.id)
        preferences!!.setValue(preferences!!.bloom!!, 0)
        preferences!!.setValue(preferences!!.skyboxes!!, 0)
        setCpuPreference()
        preferences!!.setValue(preferences!!.maxScreenSize!!, 2)
        preferences!!.setValue(preferences!!.graphics!!, 2)
        GraphicsCache.resetGraphicCaches()
        Resource.mapRegion!!.getEnvironmentManager().requestDirectEnvironmentTransition()
        Client.needsResize = true
    }

    @JvmStatic
    fun loadMidDetailPreferences() {
        preferences!!.setValue(preferences!!.removeRoofs!!, 2)
        preferences!!.setValue(preferences!!.removeRoofsOptionOverride!!, 2)
        preferences!!.setValue(preferences!!.groundDecoration!!, 1)
        preferences!!.setValue(preferences!!.goundBlending!!, 1)
        preferences!!.setValue(preferences!!.idleAnimations!!, 1)
        preferences!!.setValue(preferences!!.flickeringEffects!!, 1)
        preferences!!.setValue(preferences!!.characterShadows!!, 1)
        preferences!!.setValue(preferences!!.textures!!, 1)
        preferences!!.setValue(preferences!!.sceneryShadows!!, 1)
        preferences!!.setValue(preferences!!.lightDetail!!, 1)
        preferences!!.setValue(preferences!!.water!!, 0)
        preferences!!.setValue(preferences!!.fog!!, 1)
        preferences!!.setValue(preferences!!.antiAliasing!!, 0)
        preferences!!.setValue(preferences!!.antiAliasingPreview!!, 0)
        preferences!!.setValue(preferences!!.particles!!, 1)
        preferences!!.setValue(preferences!!.mapSize!!, MapSize.MAPSIZE_104_DEFAULT.id)
        preferences!!.setValue(preferences!!.bloom!!, 0)
        preferences!!.setValue(preferences!!.skyboxes!!, 1)
        setCpuPreference()
        preferences!!.setValue(preferences!!.maxScreenSize!!, 1)
        preferences!!.setValue(preferences!!.graphics!!, 3)
        GraphicsCache.resetGraphicCaches()
        Resource.mapRegion!!.getEnvironmentManager().requestDirectEnvironmentTransition()
        Client.needsResize = true
    }

    @JvmStatic
    fun loadHighDetailPreferences() {
        preferences!!.setValue(preferences!!.removeRoofs!!, 2)
        preferences!!.setValue(preferences!!.removeRoofsOptionOverride!!, 2)
        preferences!!.setValue(preferences!!.groundDecoration!!, 1)
        preferences!!.setValue(preferences!!.goundBlending!!, 1)
        preferences!!.setValue(preferences!!.idleAnimations!!, 1)
        preferences!!.setValue(preferences!!.flickeringEffects!!, 1)
        preferences!!.setValue(preferences!!.characterShadows!!, 1)
        preferences!!.setValue(preferences!!.textures!!, 1)
        preferences!!.setValue(preferences!!.sceneryShadows!!, 2)
        preferences!!.setValue(preferences!!.lightDetail!!, 1)
        preferences!!.setValue(preferences!!.water!!, 2)
        preferences!!.setValue(preferences!!.fog!!, 1)
        preferences!!.setValue(preferences!!.antiAliasing!!, 0)
        preferences!!.setValue(preferences!!.antiAliasingPreview!!, 0)
        preferences!!.setValue(preferences!!.particles!!, 2)
        preferences!!.setValue(preferences!!.mapSize!!, MapSize.MAPSIZE_104_DEFAULT.id)
        preferences!!.setValue(preferences!!.bloom!!, 0)
        preferences!!.setValue(preferences!!.skyboxes!!, 1)
        setCpuPreference()
        preferences!!.setValue(preferences!!.maxScreenSize!!, 0)
        preferences!!.setValue(preferences!!.graphics!!, 4)
        GraphicsCache.resetGraphicCaches()
        Resource.mapRegion!!.getEnvironmentManager().requestDirectEnvironmentTransition()
        Client.needsResize = true
    }

    @JvmStatic
    fun setCpuPreference() {
        if (GameShell.availableProcessors > 1) {
            preferences!!.setValue(preferences!!.cpu!!, 4)
        } else {
            preferences!!.setValue(preferences!!.cpu!!, 2)
        }
    }

    @JvmStatic
    fun setBloom(bloomEnabled: Boolean): Boolean {
        var enabled = bloomEnabled
        val isBloomEnabled = Renderers.activeToolkit!!.bloomEnabled()
        if (enabled != isBloomEnabled) {
            if (enabled) {
                if (!Renderers.activeToolkit!!.bloomSupported()) {
                    enabled = false
                }
            } else {
                Renderers.activeToolkit!!.resetBloom()
            }
            if (enabled != isBloomEnabled) {
                preferences!!.setValue(preferences!!.bloom!!, if (enabled) 1 else 0)
                savePreferences()
                return true
            }
            return false
        }
        return true
    }
}
