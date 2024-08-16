package com.jagex.game.runetek5.client.window;

import java.awt.Dimension;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.rendering.MessageBox;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.Utils;

public class DisplayModeDetails {

    public int width;
    public int bitDepth;
    public int height;
    public int refreshRate;
    
	public static DisplayModeDetails[] forWindowMode(WindowMode windowMode) {
	    int[] displayMode = windowMode.getDisplayModes();
	    DisplayModeDetails[] displayModeDetails = new DisplayModeDetails[displayMode.length >> 2];
	    for (int index = 0; index < displayModeDetails.length; index++) {
	        DisplayModeDetails instance = new DisplayModeDetails();
	        displayModeDetails[index] = instance;
	        instance.width = displayMode[index << 2];
	        instance.height = displayMode[(index << 2) + 1];
	        instance.bitDepth = displayMode[(index << 2) + 2];
	        instance.refreshRate = displayMode[(index << 2) + 3];
	    }
	    return displayModeDetails;
	}

	public static int getProfilingPerformance(int toolkitValue, int profilingTime) {
	    if (client.ENTITY_DEFAULTS.profilingMesh == -1) {
	        return 1;
	    } 
        if (toolkitValue != PreferenceManager.preferences.activeToolkit.getValue()) {
            client.loadAssets(toolkitValue, LocalizedText.PROFILING.translate(client.CURRENT_LANGUAGE), true);
            if (PreferenceManager.preferences.activeToolkit.getValue() != toolkitValue) {
                return -1;
            }
        }
        try {
            Dimension dimension = GameShell.gameCanvas.getSize();
            MessageBox.draw(LocalizedText.PROFILING.translate(client.CURRENT_LANGUAGE), true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
            Mesh profilingMesh = Mesh.fromResource(Resource.MODEL, client.ENTITY_DEFAULTS.profilingMesh);
            long startTime = Utils.time();
            Renderers.activeToolkit.resetClipping();
            client.SUN_MATRIX.setViewPosition(0.0F, 256.0F, 0.0F);
            Renderers.activeToolkit.setSunPosition(client.SUN_MATRIX);
            Matrix44 renderMatrix = Renderers.activeToolkit.method8449();
            renderMatrix.method6531((dimension.width / 2), (dimension.height / 2), 512.0F, 512.0F, Resource.mapRegion.getViewRangeStart(), Resource.mapRegion.getViewRangeEnd(), dimension.width, dimension.height);
            Renderers.activeToolkit.setModelMatrix(renderMatrix);
            Renderers.activeToolkit.updateSun(1.0F);
            Renderers.activeToolkit.applyLighting(16777215, 0.5F, 0.5F, 20.0F, -50.0f, 30.0F);
            Model profilingModel = Renderers.activeToolkit.createModel(profilingMesh, 2048, 64, 64, 768);
            int modelsDrawn = 0;
            Draw: for (int loops = 0; loops < 500; loops++) {
                Renderers.activeToolkit.applyClearFlags(3, 0);
                for (int i_12 = 15; i_12 >= 0; --i_12) {
                    for (int i_13 = 0; i_13 <= i_12; i_13++) {
                        client.INTERFACE_MODEL_MATRIX.setViewPosition(((int) (512.0F * (i_13 - i_12 / 2.0F))), 0.0F, ((i_12 + 1) * 512));
                        profilingModel.drawMatrixThreaded(client.INTERFACE_MODEL_MATRIX, null, ModelFunctionMaskGL.EMPTY);
                        modelsDrawn++;
                        if (Utils.time() - startTime >= profilingTime) {
                            break Draw;
                        }
                    }
                }
            }
            Renderers.activeToolkit.awaitRenderCompletion();
            long performanceIndicator = (modelsDrawn * 1000L) / (Utils.time() - startTime);
            Renderers.activeToolkit.applyClearFlags(3, 0);
            return (int) performanceIndicator;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return -1;
        }
	}
}
