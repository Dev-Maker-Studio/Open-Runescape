package com.jagex.game.runetek5.client;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.JFS32;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.preference.GamePreferences;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.MapSize;
import com.jagex.game.runetek5.systeminfo.ProfileCpu;
import com.jagex.game.runetek5.systeminfo.SystemInfo;

import java.io.File;
import java.io.IOException;

public class PreferenceManager {
	
	public static GamePreferences preferences;
	public static File preferenceFile;
	public static String preferencesParentFile;
	
    public static GamePreferences getGamePreferences() {
        JFS32 fileSystem = null;
        GamePreferences prefs = new GamePreferences(client.CURRENT_GAME);

        try {
            fileSystem = loadPreferenceFile("", client.CURRENT_GAME.name, false);
            byte[] fsBytes = new byte[(int) fileSystem.length()];

            int seekOffset;
            for (int offset = 0; offset < fsBytes.length; offset += seekOffset) {
                seekOffset = fileSystem.getSeekOffset(fsBytes, offset, fsBytes.length - offset);
                if (seekOffset == -1) {
                    throw new IOException();
                }
            }

            prefs = new GamePreferences(new Packet(fsBytes), client.CURRENT_GAME);
        } catch (Exception exception) {
        	// NOOP
        }

        try {
            if (fileSystem != null) {
                fileSystem.close();
            }
        } catch (Exception exception) {
        	// NOOP
        }

        return prefs;
    }
	
    public static JFS32 loadPreferenceFile(String preferenceUid, String preferencePrefix, boolean singleFile) {
        File prefsFile = new File(preferenceFile, "preferences" + preferenceUid + ".dat");
        if (prefsFile.exists()) {
            try {
                JFS32 preferenceFile = new JFS32(prefsFile, 10000L);
                return preferenceFile;
            } catch (IOException ignored) {
            	ignored.printStackTrace();
            }
        }
        String storePostfix = "";
        if (SystemInfo.storeId == 33) {
            storePostfix = "_rc";
        } else if (SystemInfo.storeId == 34) {
            storePostfix = "_wip";
        }
        File prefsParentFile = new File(preferencesParentFile + File.separatorChar + Loader.CACHE_DIR, "dk_" + preferencePrefix + "_preferences" + preferenceUid + storePostfix + ".dat");
        JFS32 preferenceFile;
        if (!singleFile && prefsParentFile.exists()) {
            try {
                preferenceFile = new JFS32(prefsParentFile, 10000L);
                return preferenceFile;
            } catch (IOException ioe) {
            	ioe.printStackTrace();
            }
        }
        try {
            preferenceFile = new JFS32(prefsFile, 10000L);
            return preferenceFile;
        } catch (IOException ioe) {
            throw new RuntimeException();
        }
    }
    
    public static void savePreferences() {
        JFS32 preferenceFile = null;

        try {
            preferenceFile = loadPreferenceFile("", client.CURRENT_GAME.name, true);
            Packet preferenceBuffer = preferences.encode();
            preferenceFile.write(preferenceBuffer.buffer, 0, preferenceBuffer.currentPosition);
        } catch (Exception exception) {
        	// NOOP
        }

        try {
            if (preferenceFile != null) {
                preferenceFile.close();
            }
        } catch (Exception exception) {
        	// NOOP
        }
    }
	
    
    public static int setSoftwaremodePerformance(int profilingPerformance) {
        byte mode;
        if (profilingPerformance > 12097) {
            loadHighDetailPreferences();
            mode = 4;
        } else if (profilingPerformance > 5098) {
            loadMidDetailPreferences();
            mode = 3;
        } else if (profilingPerformance > 2012) {
            loadLowDetailPreferences();
            mode = 2;
        } else {
            loadMinDetailPreferences();
            mode = 1;
        }
        if (preferences.activeToolkit.getValue() != 2) {
            preferences.setValue(preferences.toolkit, 2);
            client.loadAssets(2, false);
        } else {
            preferences.verifySettings(preferences.activeToolkit, true);
        }
        savePreferences();
        return mode;
    }

    public static int setHardwarePerformance(int renderMode, int profilingPerformance) {
        byte mode;
        if (profilingPerformance > 20077) {
            loadHighDetailPreferences();
            mode = 4;
        } else if (profilingPerformance > 10021) {
        	loadMidDetailPreferences();
            mode = 3;
        } else if (profilingPerformance > 5002) {
            loadLowDetailPreferences();
            mode = 2;
        } else {
            loadMinDetailPreferences();
            mode = 1;
        }

        if (preferences.activeToolkit.getValue() != renderMode) {
            preferences.setValue(preferences.toolkit, renderMode);
            client.loadAssets(renderMode, false);
        } else {
            preferences.verifySettings(preferences.activeToolkit, true);
        }

        savePreferences();
        return mode;
    }

    public static int calculateDisplayMode() {
        byte graphicsSettings;
        if (GameShell.maxMemory >= 96) {
            int benchmarkExecutionTime = ProfileCpu.getProfile();
            if (benchmarkExecutionTime <= 104) {
                loadHighDetailPreferences();
                graphicsSettings = 4;
            } else if (benchmarkExecutionTime <= 504) {
            	loadMidDetailPreferences();
                graphicsSettings = 3;
            } else if (benchmarkExecutionTime <= 1002) {
                loadLowDetailPreferences();
                graphicsSettings = 2;
            } else {
                loadMinDetailPreferences();
                graphicsSettings = 1;
            }
        } else {
            loadMinDetailPreferences();
            graphicsSettings = 1;
        }
        if (preferences.activeToolkit.getValue() != 0) {
            preferences.setValue(preferences.toolkit, 0);
            client.loadAssets(0, false);
        } else {
            preferences.verifySettings(preferences.activeToolkit, true);
        }
        savePreferences();
        return graphicsSettings;
    }

	public static void loadMinDetailPreferences() {
	    preferences.setValue(preferences.removeRoofs, 1);
	    preferences.setValue(preferences.removeRoofsOptionOverride, 1);
	    preferences.setValue(preferences.groundDecoration, 0);
	    preferences.setValue(preferences.fog, 0);
	    preferences.setValue(preferences.goundBlending, 0);
	    preferences.setValue(preferences.idleAnimations, 0);
	    preferences.setValue(preferences.flickeringEffects, 0);
	    preferences.setValue(preferences.characterShadows, 0);
	    preferences.setValue(preferences.sceneryShadows, 0);
	    preferences.setValue(preferences.textures, 0);
	    preferences.setValue(preferences.lightDetail, 0);
	    preferences.setValue(preferences.water, 0);
	    preferences.setValue(preferences.antiAliasing, 0);
	    preferences.setValue(preferences.antiAliasingPreview, 0);
	    preferences.setValue(preferences.particles, 0);
	    preferences.setValue(preferences.mapSize, MapSize.MAPSIZE_104_DEFAULT.id);
	    preferences.setValue(preferences.bloom, 0);
	    preferences.setValue(preferences.skyboxes, 0);
	    setCpuPreference();
	    preferences.setValue(preferences.maxScreenSize, 2);
	    preferences.setValue(preferences.graphics, 1);
	    GraphicsCache.resetGraphicCaches();
	    Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
	    client.needsResize = true;
	}

	
	public static void loadLowDetailPreferences() {
	    preferences.setValue(preferences.removeRoofs, 1);
	    preferences.setValue(preferences.removeRoofsOptionOverride, 1);
	    preferences.setValue(preferences.groundDecoration, 1);
	    preferences.setValue(preferences.goundBlending, 1);
	    preferences.setValue(preferences.idleAnimations, 0);
	    preferences.setValue(preferences.flickeringEffects, 0);
	    preferences.setValue(preferences.characterShadows, 0);
	    preferences.setValue(preferences.sceneryShadows, 0);
	    preferences.setValue(preferences.textures, 0);
	    preferences.setValue(preferences.lightDetail, 0);
	    preferences.setValue(preferences.water, 0);
	    preferences.setValue(preferences.fog, 0);
	    preferences.setValue(preferences.antiAliasing, 0);
	    preferences.setValue(preferences.antiAliasingPreview, 0);
	    preferences.setValue(preferences.particles, 0);
	    preferences.setValue(preferences.mapSize, MapSize.MAPSIZE_104_DEFAULT.id);
	    preferences.setValue(preferences.bloom, 0);
	    preferences.setValue(preferences.skyboxes, 0);
	    setCpuPreference();
	    preferences.setValue(preferences.maxScreenSize, 2);
	    preferences.setValue(preferences.graphics, 2);
	    GraphicsCache.resetGraphicCaches();
	    Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
	    client.needsResize = true;
	}
	
	public static void loadMidDetailPreferences() {
	    preferences.setValue(preferences.removeRoofs, 2);
	    preferences.setValue(preferences.removeRoofsOptionOverride, 2);
	    preferences.setValue(preferences.groundDecoration, 1);
	    preferences.setValue(preferences.goundBlending, 1);
	    preferences.setValue(preferences.idleAnimations, 1);
	    preferences.setValue(preferences.flickeringEffects, 1);
	    preferences.setValue(preferences.characterShadows, 1);
	    preferences.setValue(preferences.textures, 1);
	    preferences.setValue(preferences.sceneryShadows, 1);
	    preferences.setValue(preferences.lightDetail, 1);
	    preferences.setValue(preferences.water, 0);
	    preferences.setValue(preferences.fog, 1);
	    preferences.setValue(preferences.antiAliasing, 0);
	    preferences.setValue(preferences.antiAliasingPreview, 0);
	    preferences.setValue(preferences.particles, 1);
	    preferences.setValue(preferences.mapSize, MapSize.MAPSIZE_104_DEFAULT.id);
	    preferences.setValue(preferences.bloom, 0);
	    preferences.setValue(preferences.skyboxes, 1);
	    setCpuPreference();
	    preferences.setValue(preferences.maxScreenSize, 1);
	    preferences.setValue(preferences.graphics, 3);
	    GraphicsCache.resetGraphicCaches();
	    Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
	    client.needsResize = true;
	}

	public static void loadHighDetailPreferences() {
	    preferences.setValue(preferences.removeRoofs, 2);
	    preferences.setValue(preferences.removeRoofsOptionOverride, 2);
	    preferences.setValue(preferences.groundDecoration, 1);
	    preferences.setValue(preferences.goundBlending, 1);
	    preferences.setValue(preferences.idleAnimations, 1);
	    preferences.setValue(preferences.flickeringEffects, 1);
	    preferences.setValue(preferences.characterShadows, 1);
	    preferences.setValue(preferences.textures, 1);
	    preferences.setValue(preferences.sceneryShadows, 2);
	    preferences.setValue(preferences.lightDetail, 1);
	    preferences.setValue(preferences.water, 2);
	    preferences.setValue(preferences.fog, 1);
	    preferences.setValue(preferences.antiAliasing, 0);
	    preferences.setValue(preferences.antiAliasingPreview, 0);
	    preferences.setValue(preferences.particles, 2);
	    preferences.setValue(preferences.mapSize, MapSize.MAPSIZE_104_DEFAULT.id);
	    preferences.setValue(preferences.bloom, 0);
	    preferences.setValue(preferences.skyboxes, 1);
	    setCpuPreference();
	    preferences.setValue(preferences.maxScreenSize, 0);
	    preferences.setValue(preferences.graphics, 4);
	    GraphicsCache.resetGraphicCaches();
	    Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
	    client.needsResize = true;
	}

	public static void setCpuPreference() {
	    if (GameShell.availableProcessors > 1) {
	        preferences.setValue(preferences.cpu, 4);
	    } else {
	        preferences.setValue(preferences.cpu, 2);
	    }
	}
	
	public static boolean setBloom(boolean bloomEnabled) {
	    boolean isBloomEnabled = Renderers.activeToolkit.bloomEnabled();
	    if (bloomEnabled != isBloomEnabled) {
	        if (bloomEnabled) {
	            if (!Renderers.activeToolkit.bloomSupported()) {
	                bloomEnabled = false;
	            }
	        } else {
	            Renderers.activeToolkit.resetBloom();
	        }
	        if (bloomEnabled != isBloomEnabled) {
	            preferences.setValue(preferences.bloom, bloomEnabled ? 1 : 0);
	            savePreferences();
	            return true;
	        } 
	        return false;
	    } 
	    return true;
	}
}
