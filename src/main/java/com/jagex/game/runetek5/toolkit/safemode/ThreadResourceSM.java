package com.jagex.game.runetek5.toolkit.safemode;

import com.jagex.game.runetek5.graphics.model.ModelSM;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;

/**
 * JagName = RenderContext?
 * Render dependencies for safe mode
 * @author Brainpower
 *
 */
public class ThreadResourceSM {
	public boolean aBool2304;
	public float offsetX;
	public float centerX;
	public float offsetY;
	public float centerY;
	public int clipWidth;
	public Runnable runnable;
	public RasterizerSM rasterizer;
	public float aFloat2305 = 0.85F;
	public float aFloat2303;
	public int fogColor;
	public int lastWaterColor;
	
	/**
	 * FIXME: incorrect
	 */
	public boolean hasWaterUpdate;
	
	public int waterAverageHeight;
	public int waterScale; // water depth?
	public boolean zbuffering;
	public final Matrix44 aClass3842346;
	public final Matrix43 aClass2942314;
	public final Matrix43 aClass2942316;
	public final Matrix44 particleMatrix;
	public final Matrix44 renderingMatrix;
	public final int[] waterRenderHeight;
	public final float[] projectedVerticesX;
	public final float[] projectedVerticesY;
	public final float[] projectedVerticesZ;
	public final float[] projectedVerticesZ2;
    public final int[] matrixX;
    public final int[] matrixY;
    public final int[] matrixZ;
    public final int[] renderCycleByVertex;
    public final int[] renderCycleByDepth;
    public float aFloat2311;
    public float aFloat2335;
    public float aFloat2308;
    public final float[] uvTypes;
    public final ModelSM[] copyDstModels;
    public final ModelSM[] copySrcModels;
    public final float[] perspectiveX;
    public final float[] perspectiveY;
    public final float[] perspectiveZNear;
    public final float[] perspectiveZFar;
    public final float[] aFloatArray2345;
    public final float[] particleOffsets;
    public final SafeModeToolkit toolkit;
    public final int[] billboardIdByFace;

    public ThreadResourceSM(SafeModeToolkit toolkit) {
        this.aFloat2303 = 1.0F - this.aFloat2305;
        this.fogColor = 0;
        this.lastWaterColor = 0;
        this.hasWaterUpdate = false;
        this.waterAverageHeight = 0;
        this.waterScale = 0;
        this.zbuffering = true;
        this.aClass3842346 = new Matrix44();
        this.aClass2942314 = new Matrix43();
        this.aClass2942316 = new Matrix43();
        this.particleMatrix = new Matrix44();
        this.renderingMatrix = new Matrix44();
        this.waterRenderHeight = new int[ModelSM.maxModelVertices];
        this.projectedVerticesX = new float[ModelSM.maxModelVertices];
        this.projectedVerticesY = new float[ModelSM.maxModelVertices];
        this.projectedVerticesZ = new float[ModelSM.maxModelVertices];
        this.projectedVerticesZ2 = new float[ModelSM.maxModelVertices];
        this.matrixX = new int[8];
        this.matrixY = new int[8];
        this.matrixZ = new int[8];
        this.renderCycleByVertex = new int[10000];
        this.renderCycleByDepth = new int[10000];
        this.aFloat2311 = 1.0F;
        this.aFloat2335 = 0.0F;
        this.aFloat2308 = 1.0F;
        this.uvTypes = new float[2];
        this.copyDstModels = new ModelSM[7];
        this.copySrcModels = new ModelSM[7];
        this.perspectiveX = new float[64];
        this.perspectiveY = new float[64];
        this.perspectiveZNear = new float[64];
        this.perspectiveZFar = new float[64];
        this.aFloatArray2345 = new float[64];
        this.particleOffsets = new float[3];
        this.toolkit = toolkit;
        this.rasterizer = new RasterizerSM(toolkit, this);
        for (int index = 0; index < 7; index++) {
        	this.copyDstModels[index] = new ModelSM(toolkit);
        	this.copySrcModels[index] = new ModelSM(toolkit);
        }
        this.billboardIdByFace = new int[ModelSM.maxModelTriangles];
        for (int index = 0; index < ModelSM.maxModelTriangles; index++) {
        	this.billboardIdByFace[index] = -1;
        }
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    public void initializeRasterizer() {
        this.rasterizer = new RasterizerSM(this.toolkit, this);
    }
}
