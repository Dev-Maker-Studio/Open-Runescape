package com.jagex.game.runetek5.rendering;

import com.jagex.game.runetek5.buffer.NativeMemoryNode;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class Renderers {

    public static AbstractToolkit activeToolkit;
    public static AbstractFontRenderer FONT_RENDERER;
	public static AbstractToolkit GPU_ITEM_RENDERER;
	public static AbstractFontRenderer GPU_ITEM_FONT_RENDERER;
	
	public static void cleanup() {
	    if (GPU_ITEM_RENDERER != null) {
	        GPU_ITEM_RENDERER.cleanupAndShutdown();
	        GPU_ITEM_RENDERER = null;
	        GPU_ITEM_FONT_RENDERER = null;
	    }
	}

	public static void prepareForRendering(int toolkitId, String topleftScreenText, boolean verifySettingsPreUpdate) {
	    if (toolkitId == 0) {
	        activeToolkit = AbstractToolkit.create(0, GameShell.gameCanvas, TypeLists.MATERIALS, Resource.SHADER, PreferenceManager.preferences.antiAliasingPreview.getValue() * 2);
	        if (topleftScreenText != null) {
	            activeToolkit.applyClearFlags(1, 0);
	            FontMetrics fontMetrics = FontMetrics.getFontMetrics(Resource.FONT_METRICS, FontManager.p12FullIndex);
	            AbstractFontRenderer fontRenderer = activeToolkit.createFontRenderer(fontMetrics, SpriteType.decodeGroup(Resource.SPRITES, FontManager.p12FullIndex), true);
	            WindowMode.fillWindowRect();
	            MessageBox.draw(topleftScreenText, true, activeToolkit, fontRenderer, fontMetrics);
	        }
	    } else {
	        AbstractToolkit generatedToolkit = null;
	        FontMetrics fontMetrics;
	        AbstractFontRenderer fontRenderer;
	        if (topleftScreenText != null) {
	            generatedToolkit = AbstractToolkit.create(0, GameShell.gameCanvas, TypeLists.MATERIALS, Resource.SHADER, 0);
	            generatedToolkit.applyClearFlags(1, 0);
	            fontMetrics = FontMetrics.getFontMetrics(Resource.FONT_METRICS, FontManager.p12FullIndex);
	            fontRenderer = generatedToolkit.createFontRenderer(fontMetrics, SpriteType.decodeGroup(Resource.SPRITES, FontManager.p12FullIndex), true);
	            WindowMode.fillWindowRect();
	            MessageBox.draw(topleftScreenText, true, generatedToolkit, fontRenderer, fontMetrics);
	        }
	        try {
	            try {
	                activeToolkit = AbstractToolkit.create(toolkitId, GameShell.gameCanvas, TypeLists.MATERIALS, Resource.SHADER, PreferenceManager.preferences.antiAliasingPreview.getValue() * 2);
	                if (topleftScreenText != null) {
	                    generatedToolkit.applyClearFlags(1, 0);
	                    fontMetrics = FontMetrics.getFontMetrics(Resource.FONT_METRICS, FontManager.p12FullIndex);
	                    fontRenderer = generatedToolkit.createFontRenderer(fontMetrics, SpriteType.decodeGroup(Resource.SPRITES, FontManager.p12FullIndex), true);
	                    WindowMode.fillWindowRect();
	                    MessageBox.draw(topleftScreenText, true, generatedToolkit, fontRenderer, fontMetrics);
	                }
	                if (activeToolkit.utilisesNativeHeap()) {
	                    boolean highMem = true;
	                    try {
	                        highMem = SystemInfo.SYSTEM_INFO.ram > 256;
	                    } catch (Throwable throwable) {
	                    	// NOOP
	                    }
	                    NativeMemoryNode nativeHeap;
	                    if (highMem) {
	                        nativeHeap = activeToolkit.getNativeMemory(146800640);
	                    } else {
	                        nativeHeap = activeToolkit.getNativeMemory(104857600);
	                    }
	                    activeToolkit.setNativeHeap(nativeHeap);
	                }
	            } catch (Throwable throwable) {
	                int toolkitSetting = PreferenceManager.preferences.activeToolkit.getValue();
	                if (toolkitSetting == 2) {
	                    client.renderWithCpu = true;
	                }
	                PreferenceManager.preferences.setValue(PreferenceManager.preferences.activeToolkit, 0);
	                prepareForRendering(toolkitSetting, topleftScreenText, verifySettingsPreUpdate);
	                if (generatedToolkit != null) {
	                    try {
	                        generatedToolkit.cleanupAndShutdown();
	                    } catch (Throwable subThrowable) {
	                    	// NOOP
	                    }
	                }
	                return;
	            }
	            if (generatedToolkit != null) {
	                try {
	                    generatedToolkit.cleanupAndShutdown();
	                } catch (Throwable throwable) {
	                	// NOOP
	                }
	            }
	        } catch (Exception exception) {
	            if (generatedToolkit != null) {
	                try {
	                    generatedToolkit.cleanupAndShutdown();
	                } catch (Throwable throwable) {
	                	// NOOP
	                }
	            }
	        }
	    }
	    if (verifySettingsPreUpdate) {
	        PreferenceManager.preferences.verifySettings(PreferenceManager.preferences.activeToolkit, false);
	    }
	    PreferenceManager.preferences.setValue(PreferenceManager.preferences.activeToolkit, toolkitId);
	    if (!verifySettingsPreUpdate) {
	        PreferenceManager.preferences.verifySettings(PreferenceManager.preferences.activeToolkit, true);
	    }
	    cleanup();
	    activeToolkit.initialize();
	    activeToolkit.generateFlags();
	    Resource.mapRegion.calculateViewRange();
	    activeToolkit.resetTextureState();
	    if (activeToolkit.hasBloomSupport()) {
	        PreferenceManager.setBloom(PreferenceManager.preferences.bloom.getValue() == 1);
	    }
	    Resource.mapRegion.resetAtmosphere();
	    Resource.mapRegion.getEnvironmentManager().requestDirectEnvironmentTransition();
	    client.preferencesSavedOnServer = false;
	    client.needsResize = true;
	    WindowMode.activeDisplayModeDetails = null;
	    activeToolkit.resetDepthClippingMask();
	}
}
