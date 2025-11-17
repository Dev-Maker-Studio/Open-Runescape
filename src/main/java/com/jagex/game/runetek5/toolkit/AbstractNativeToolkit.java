package com.jagex.game.runetek5.toolkit;

import com.jagex.game.runetek5.unidentified.*;
import jaclib.memory.DirectBufferHelper;
import jaclib.memory.Stream;
import jaclib.memory.heap.NativeHeap;
import jaclib.memory.heap.NativeHeapBuffer;
import sun.misc.Unsafe;

import java.awt.Canvas;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import com.jagex.game.runetek5.buffer.NativeMemoryNode;
import com.jagex.game.runetek5.buffer.NativeMemoryDX;
import com.jagex.game.runetek5.cache.node.LinkedList;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.WeakReferenceMappingCache;
import com.jagex.game.runetek5.client.GraphicsCache;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.material.NativeMaterialCache;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.font.NativeFont;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelDX;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.graphics.shader.Shader;
import com.jagex.game.runetek5.graphics.shader.ShaderLoader;
import com.jagex.game.runetek5.graphics.shader.program.AbstractBloomShader;
import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader;
import com.jagex.game.runetek5.graphics.shader.program.AbstractParticleShader;
import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader;
import com.jagex.game.runetek5.graphics.shader.program.AbstractWaterShader;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeFilterBloomShader;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeParticleShaderHd;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeEnvironmentShaderHd;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeEnvironmentShaderLd;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeSpriteShaderHd;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeEvtWaterMapper;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeParticleShaderLd;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeSpriteShaderLd;
import com.jagex.game.runetek5.graphics.shader.program.impl.NativeWaterfall;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteDX_Sub3;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.AbstractNativeCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.NativeCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.TextureStageState;
import com.jagex.game.runetek5.graphics.texture.TextureState;
import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.Texture3DSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler;
import com.jagex.game.runetek5.graphics.texture.water.NativeWaterTexture;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundDX;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.companion.Binding;
import com.jagex.game.runetek5.toolkit.companion.BlendFuncMode;
import com.jagex.game.runetek5.toolkit.companion.MatrixMode;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;
import com.jagex.game.runetek5.toolkit.directx.DirectXParticleRenderer;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent;
import com.jagex.game.runetek5.utils.ColorUtil;

public abstract class AbstractNativeToolkit extends AbstractToolkit {

    public static final Matrix44 aClass3848712 = new Matrix44();
    public int texturesSizeOccupied;
    public int textureBuffersSizeOccupied;
    public boolean GLEXTFramebufferObject;
    public int maxConcurrentTextures;
    public final Matrix44 particleMatrix;
    public final Matrix44 projectionMatrix;
    public final Matrix44 aClass3848729;
    public final float[] sunPosition;
    public float baseLightColorRed;
    public float baseLightColorGreen;
    public float baseLightColorBlue;
    public float sunColorIntensity;
    public int anInt8709;
    public Unsafe theUnsafe;
    public ByteBuffer directBuffer;
    public long directBufferAddress;
    public int animatingCycle;
    public NativeTextureSampler activeTextureSampler;
    public int renderBufferSizeOccupied;
    public int maxActiveLights;
    public float fogStart;
    public float fogEnd;
    public boolean SUPPORTS_TEXTURE3D;
    public boolean GLARBMultisample;
    public final Matrix44 aClass3848683;
    public float depthRangeNear;
    public float depthRangeFar;
    public float camNear;
    public float camFar;
    public int cTop;
    public int cBottom;
    public int cLeft;
    public int cRight;
    public int anInt8749;
    public int anInt8776;
    public int anInt8751;
    public int anInt8752;
    public int anInt8823;
    public int anInt8754;
    public boolean depthBufferingEnabled;
    public boolean aBool8756;
    public boolean enableDepthTest;
    public boolean aBool8684;
    public final boolean sunEnabled;
    public final boolean aBool8760;
    public final float[] lightPosition;
    public float lightIntensity;
    public float antiSunColorIntensity;
    public int currentTextureSlot;
    public final boolean fogSupported;
    public int fogColorRgb;
    public int fogDistance;
    public int anInt8680;
    public BlendFuncMode blendFuncMode;
    public boolean enableBlending;
    public boolean enableAlphaTest;
    public int anInt8844;
    public boolean enableLights;
    public boolean boundsCalculated;
    public int currentSceneLightCount;
    public int lastSceneLightCount;
    public int envColor;
    public boolean fogEnabled;
    public Matrix44[] textureMatrices;
    public TextureState[] textureStates;
    public TextureCombineMode[] textureRgbCombineModes;
    public TextureCombineMode[] textureAlphaCombineModes;
    public PointLight[] lightSources;
    public final WeakReferenceMappingCache weakReferences = new WeakReferenceMappingCache();
    public final LinkedList activeNativeMemory;
    public final Matrix43 aClass2948704;
    public final Matrix44 modelMatrix;
    public Matrix43 aClass2948706;
    public final Matrix43 aClass2948804;
    public final Matrix44 aClass3848814;
    public final Matrix43 aClass2948710;
    public final Matrix44 modelViewMatrix;
    public final Matrix43 unsure;
    public final Matrix44 aClass3848715;
    public final Vector3 pointLightVector;
    public final float[] aFloatArray8717;
    public boolean viewPositionCalibrated;
    public MatrixMode matrixMode;
    public boolean aBool8720;
    public final Matrix44 aClass3848774;
    public final Matrix44 worldViewProjectionMatrix;
    public boolean aBool8723;
    public final Matrix44 aClass3848724;
    public final Matrix44 aClass3848725;
    public Matrix44 aClass3848708;
    public final Matrix44 transformMatrix;
    public final float[][] aFloatArrayArray8730;
    public float aFloat8737;
    public float aFloat8738;
    public int viewportX;
    public int viewportY;
    public int viewportWidth;
    public int viewportHeight;
    public final float[] baseLightPosition;
    public final float[] baseLightInvertedPosition;
    public int baseLightColor;
    public int activeFlags;
    public int shadowSizeShift;
    public final int materialSize;
    public final AbstractWaterShader[] waterShadersByEffect;
    public final AbstractBloomShader[] bloomManager;
    public final boolean aBool8805;
    public int blendFuncType;
    public boolean aBool8681;
    public final ModelDX[] copyByTypeSrc;
    public final ModelDX[] copyByTypeDst;
    public final Matrix44 aClass3848841;
    public final Matrix43 aClass2948842;
    public Js5ResourceProvider shaderResource;
    public int endiannessType;
    public NativeMaterialCache materialCache;
    public int[] hslToRgb;
    public NativeWaterTexture aClass498694;
    public AbstractEnvironmentShader environmentShader;
    public AbstractSpriteShader spriteShader;
    public AbstractParticleShader abstractParticleShader;
    public NativeEvtWaterMapper nativeEvtWaterMapper;
    public NativeWaterfall nativeWaterfall;
    public NativeBloomRenderer aClass328714;
    public AbstractWaterShader aClass418753;
    public DirectXParticleRenderer particleRenderer;
    public int nativeFunctionMask;
    public int shadowOffsetX;
    public int shadowOffsetZ;
    public NativeHeap nativeHeap;
    public AbstractNativeCubeTextureGenerator aClass152Sub28731;
    public float viewportCenterX;
    public float viewportCenterY;
    public float aFloat8797;
    public float aFloat8741;
    public NativeStream anInterface48746;
    public VertexDeclaration aClass708786;
    public int renderedWaterColor;
    public WaterTile renderedWaterTile;
    public boolean missingWaterColor;
    public NativeStream vertexStream;
    public VertexDeclaration vertexDeclaration;
    public TextureSampler[] textureSamplers;
    public NativeTextureSampler anInterface68795;
    public VertexDeclaration aClass708832;
    public VertexDeclaration aClass708836;
    public VertexDeclaration aClass708721;
    public DirectXBuffer anInterface328834;
    public SpriteDX_Sub3 aNativeSpriteSub38790;
    public SpriteDX_Sub3 aNativeSpriteSub38791;
    public NativeStream anInterface48693;
	public static final Matrix44 WORLD_VIEW_PROJECTION_MATRIX = new Matrix44();

    AbstractNativeToolkit(MaterialProvider materials, Js5ResourceProvider shaders, int i_3, int i_4) {
        super(materials);
        this.activeNativeMemory = new LinkedList();
        this.aClass2948704 = new Matrix43();
        this.modelMatrix = new Matrix44();
        this.aClass2948706 = new Matrix43();
        this.aClass2948804 = new Matrix43();
        this.particleMatrix = new Matrix44();
        this.aClass3848814 = new Matrix44();
        this.aClass2948710 = new Matrix43();
        this.modelViewMatrix = new Matrix44();
        this.unsure = new Matrix43();
        this.aClass3848683 = new Matrix44();
        this.aClass3848715 = new Matrix44();
        this.pointLightVector = new Vector3();
        this.aFloatArray8717 = new float[4];
        this.viewPositionCalibrated = false;
        this.matrixMode = MatrixMode.aClass33380;
        this.aBool8720 = false;
        this.aClass3848774 = new Matrix44();
        this.worldViewProjectionMatrix = new Matrix44();
        this.aBool8723 = false;
        this.aClass3848724 = new Matrix44();
        this.aClass3848725 = new Matrix44();
        this.aClass3848708 = this.aClass3848725;
        this.projectionMatrix = new Matrix44();
        this.transformMatrix = new Matrix44();
        this.aClass3848729 = new Matrix44();
        this.aFloatArrayArray8730 = new float[6][4];
        this.depthRangeNear = 0.0F;
        this.depthRangeFar = 1.0F;
        this.aFloat8737 = 0.0F;
        this.aFloat8738 = 1.0F;
        this.camNear = 50.0F;
        this.camFar = 3584.0F;
        this.cTop = 0;
        this.cBottom = 0;
        this.cLeft = 0;
        this.cRight = 0;
        this.viewportX = 0;
        this.viewportY = 0;
        this.viewportWidth = 0;
        this.viewportHeight = 0;
        this.anInt8749 = 0;
        this.anInt8776 = 0;
        this.anInt8751 = 0;
        this.anInt8752 = 0;
        this.anInt8823 = 0;
        this.anInt8754 = 0;
        this.depthBufferingEnabled = true;
        this.aBool8756 = false;
        this.enableDepthTest = false;
        this.aBool8684 = false;
        this.sunEnabled = true;
        this.aBool8760 = false;
        this.sunPosition = new float[]{0.0F, 0.0F, 1.0F, 0.0F};
        this.lightPosition = new float[]{0.0F, 0.0F, -1.0f, 0.0F};
        this.baseLightPosition = new float[]{0.0F, 0.0F, 1.0F, 0.0F};
        this.baseLightInvertedPosition = new float[]{0.0F, 0.0F, 1.0F, 0.0F};
        this.baseLightColor = 16777215;
        this.baseLightColorRed = 1.0F;
        this.baseLightColorGreen = 1.0F;
        this.baseLightColorBlue = 1.0F;
        this.lightIntensity = 1.0F;
        this.sunColorIntensity = -1.0f;
        this.antiSunColorIntensity = -1.0f;
        this.activeFlags = 8;
        this.shadowSizeShift = 3;
        this.materialSize = 128;
        this.currentTextureSlot = 0;
        this.waterShadersByEffect = new AbstractWaterShader[16];
        this.bloomManager = new AbstractBloomShader[2];
        this.aBool8805 = false;
        this.fogSupported = true;
        this.fogColorRgb = -1;
        this.fogDistance = -1;
        this.anInt8680 = 0;
        this.blendFuncType = 1;
        this.blendFuncMode = BlendFuncMode.ALPHA_BLENDING;
        this.enableBlending = true;
        this.enableAlphaTest = true;
        this.aBool8681 = false;
        new Stream();
        this.copyByTypeSrc = new ModelDX[7];
        this.copyByTypeDst = new ModelDX[7];
        this.aClass3848841 = new Matrix44();
        this.aClass2948842 = new Matrix43();
        this.anInt8844 = -1;

        try {
            this.shaderResource = shaders;
            this.anInt8709 = i_3;
            this.endiannessType = i_4;
            ColorUtil.initializeTables(false, true);
            if (super.materialProvider != null) {
                this.materialCache = new NativeMaterialCache(this, super.materialProvider);
            } else {
                this.materialCache = null;
            }

            if (this.endiannessType == 0) {
                this.hslToRgb = ColorUtil.hslToRgb;
            } else {
                this.hslToRgb = ColorUtil.unsignedHslToRgb;
            }

            try {
                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                this.theUnsafe = (Unsafe) field.get(null);
            } catch (Exception exception) {
            	exception.printStackTrace();
            }

            this.directBuffer = ByteBuffer.allocateDirect(4194304);
            this.directBuffer.order(ByteOrder.nativeOrder());
            this.directBufferAddress = DirectBufferHelper.getDirectBufferAddress(this.directBuffer);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            cleanupAndShutdown();
            if (throwable instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) throwable;
            }

            throw new RuntimeException("");
        }

        setNativeHeap(getNativeMemory(131072));
    }
    
	public static AbstractToolkit createToolkit(Canvas canvas0, MaterialProvider interface221, Js5ResourceProvider index2, int i_3) {
	    try {
	        boolean jaclibIsLoaded = AbstractToolkit.isJaclibLoaded();
	        if (!jaclibIsLoaded) {
	            throw new RuntimeException("");
	        } else if (!AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jagdx")) {
	            throw new RuntimeException("");
	        } else {
	            AbstractToolkit toolkit = DirectXNativeToolkit.forCapabilities(canvas0, interface221, index2, Integer.valueOf(i_3));
	            return toolkit;
	        }
	    } catch (Throwable throwable) {
	        throw new RuntimeException("");
	    }
	}
    
    public Matrix44 getTextureMatrix() {
        return this.textureMatrices[this.currentTextureSlot];
    }

    public void clearReference(Binding interface71) {
        this.weakReferences.identify(interface71);
    }

    public void resetWeakReferences() {
        ArrayList refs = this.weakReferences.collect();
        for (Object ref : refs) {
            Binding mapping = (Binding) ref;
            mapping.deallocate();
        }
    }

    public abstract boolean supportsArbVertexShader();

    public abstract boolean supportsArbFragmentShader();

    public boolean areLightsEnabled() {
        return this.enableLights;
    }

    public abstract Shader loadShader(String shaderName);


    ShaderLoader createShader(byte[] shaderData) {
        if (shaderData == null) {
            return null;
        } 
        try {
            ShaderLoader shaderLoader = new ShaderLoader(shaderData);
            return shaderLoader;
        } catch (Exception exception) {
        	exception.printStackTrace();
            return null;
        }
    }

    public abstract void setMatrixTransform(Matrix44 var1, Matrix44 var2, Matrix44 projectionMatrix);

    abstract void blendDrawBuffer();

    public void method13895() {
        this.aClass498694 = new NativeWaterTexture(this);
        this.enableLights = false;

        try {
            this.environmentShader = new NativeEnvironmentShaderHd(this);
            this.spriteShader = new NativeSpriteShaderHd(this);
            this.abstractParticleShader = new NativeParticleShaderHd(this);
            this.nativeEvtWaterMapper = new NativeEvtWaterMapper(this, this.aClass498694);
            this.nativeWaterfall = new NativeWaterfall(this, this.aClass498694);
        } catch (Exception exception) {
            exception.printStackTrace();
            this.environmentShader = new NativeEnvironmentShaderLd(this);
            this.spriteShader = new NativeSpriteShaderLd(this);
            this.abstractParticleShader = new NativeParticleShaderLd(this);
            this.nativeEvtWaterMapper = null;
            this.nativeWaterfall = null;
            this.enableLights = true;
        }

        int displayWidth;
        int displayHeight;
        if (super.display != null) {
            displayWidth = super.display.getWidth();
            displayHeight = super.display.getHeight();
        } else {
            displayHeight = 0;
            displayWidth = 0;
        }

        this.aClass328714 = new NativeBloomRenderer(this, displayWidth, displayHeight);
        if (!this.enableLights) {
            this.bloomManager[1] = new NativeFilterBloomShader(this);
            this.aClass328714.initialize(this.bloomManager[1]);
        }

        if (this.aClass418753 != null) {
            this.aClass418753.method885();
            this.aClass418753 = null;
        }

        method13899();
    }

    public AbstractWaterShader getShaderByEffect(int effect) {
        return this.waterShadersByEffect[effect];
    }

    void setupGraphics() {
        colorMask(7);
        method13976();
    }



    public WaterTile getRenderedWaterTile() {
        return this.renderedWaterTile;
    }

    void method13899() {
        this.aClass328714.getFormat();

        for (int i_1 = 0; i_1 < this.bloomManager.length; i_1++) {
            if (this.bloomManager[i_1] != null && this.bloomManager[i_1].method1107()) {
                this.bloomManager[i_1].method1099();
            }
        }

        this.particleRenderer = new DirectXParticleRenderer(this);
        method13944();
        method14009();
        method14177();
        this.particleRenderer.method1089();
    }

    void deallocate() {
        this.aClass328714.destroy();
        this.anInterface48746.deallocate();
        this.vertexStream.deallocate();
        this.anInterface48693.deallocate();

        for (int i_1 = 0; i_1 < 7; i_1++) {
            this.copyByTypeDst[i_1].deallocate();
        }

        this.particleRenderer.deallocate();
        this.anInterface328834.deallocate();
    }

    @Override
    public NativeSprite createNativeSprite(int i_1, int i_2, int i_3, int i_4, boolean bool5) {
        SpriteDX_Sub3 class160Sub36 = new SpriteDX_Sub3(this, i_3, i_4, bool5, false);
        class160Sub36.method2750(0, 0, i_3, i_4, i_1, i_2);
        return class160Sub36;
    }

    @Override
    public boolean utilisesNativeHeap() {
        return true;
    }

    @Override
    public void shutdown() {
        if (!this.aBool8681) {
            deallocate();
            this.materialCache.reset();

            for (Node memory = this.activeNativeMemory.getTail(); memory != null; memory = this.activeNativeMemory.getPrevious()) {
                ((NativeMemoryDX) memory).deallocate();
            }

            ColorUtil.resetTables(false, true);

            for (int index = 0; index < this.waterShadersByEffect.length; index++) {
                if (this.waterShadersByEffect[index] != null) {
                    this.waterShadersByEffect[index].method878();
                    this.waterShadersByEffect[index] = null;
                }
            }

            for (int index = 0; index < this.bloomManager.length; index++) {
                if (this.bloomManager[index] != null) {
                    this.bloomManager[index].method1105();
                    this.bloomManager[index] = null;
                }
            }

            resetWeakReferences();
            animateTextures(0);
            this.aBool8681 = true;
        }

    }

    public void addWeakReference(Binding mapping) {
        this.weakReferences.put(mapping);
    }

    @Override
    public boolean forceFixedScreenmode() {
        return true;
    }

    @Override
    public boolean hasBloomSupport() {
        return this.bloomManager[1] != null;
    }

    public void method13903() {
        if (areLightsEnabled()) {
            configureLights();
        }

        this.currentSceneLightCount = this.lastSceneLightCount;
        this.lastSceneLightCount = 0;
    }

    public void toggleBlending(int blendFuncType) {
        if (this.blendFuncType != blendFuncType) {
            BlendFuncMode blendFunc;
            boolean alphaTest;
            boolean blend;
            if (blendFuncType == 1) {
                blendFunc = BlendFuncMode.ALPHA_BLENDING;
                alphaTest = true;
                blend = true;
            } else if (blendFuncType == 2) {
                blendFunc = BlendFuncMode.OPAQUE_BLENDING;
                alphaTest = false;
                blend = true;
            } else if (blendFuncType == 128) {
                blendFunc = BlendFuncMode.COLOR_BLENDING;
                alphaTest = true;
                blend = true;
            } else {
                blendFunc = BlendFuncMode.OPAQUE;
                alphaTest = false;
                blend = false;
            }

            if (alphaTest != this.enableAlphaTest) {
            	this.enableAlphaTest = alphaTest;
                toggleAlpha();
            }

            if (blend != this.enableBlending) {
            	this.enableBlending = blend;
                toggleBlending();
            }

            if (blendFunc != this.blendFuncMode) {
            	this.blendFuncMode = blendFunc;
                blendDrawBuffer();
            }

            this.blendFuncType = blendFuncType;
            this.nativeFunctionMask &= -13;
        }
    }

    @Override
    public boolean extendBuildArea() {
        return true;
    }

    @Override
    public boolean replaceCanvasOnDisplayUpdate() {
        return false;
    }

    @Override
    public boolean hasAntiAliasing() {
        return this.GLARBMultisample;
    }

    public abstract float method13905();

    @Override
    public void animateTextures(int cycle) {
        if (this.materialCache != null) {
            this.materialCache.processCycle();
        }
        this.animatingCycle = cycle & 0x7fffffff;
    }

    public void autoResize2() {
        autoResize();
    }

    public void autoResize() {
        method13936();
        method13938();
        method13934();
        calculateViewportCenter();
        resetClipping();
    }

    @Override
    public boolean bloomSupported() {
        if (this.bloomManager[1] != null && !this.bloomManager[1].method1107()) {
            boolean reset = this.aClass328714.initialize(this.bloomManager[1]);
            if (reset) {
                this.materialCache.reset();
            }
            return reset;
        } 
        return false;
    }

    @Override
    public void resetBloom() {
        if (this.bloomManager[1] != null && this.bloomManager[1].method1107()) {
            this.aClass328714.method822(this.bloomManager[1]);
            this.materialCache.reset();
        }
    }

    @Override
    public boolean bloomEnabled() {
        return this.bloomManager[1] != null && this.bloomManager[1].method1107();
    }

    @Override
    public void setBloomFilter(float r, float g, float b) {
        NativeFilterBloomShader.red = r;
        NativeFilterBloomShader.blue = g;
        NativeFilterBloomShader.green = b;
        NativeFilterBloomShader.baseBlur = (float) 1.0;
    }

    public boolean method13908(float f_1, float f_2, float f_4, float f_5) {
        this.directBuffer.clear();
        this.directBuffer.putFloat(f_1);
        this.directBuffer.putFloat(f_2);
        this.directBuffer.putFloat((float) 0.0);
        this.directBuffer.putFloat(f_4);
        this.directBuffer.putFloat(f_5);
        this.directBuffer.putFloat((float) 0.0);
        return this.vertexStream.upload(0, this.directBuffer.position(), this.directBufferAddress);
    }

    @Override
    public NativeMemoryNode getNativeMemory(int requiredCapacity) {
        NativeMemoryDX nativeMemory = new NativeMemoryDX(requiredCapacity);
        this.activeNativeMemory.insertTail(nativeMemory);
        return nativeMemory;
    }

    @Override
    public void setNativeHeap(NativeMemoryNode memoryNode) {
        this.nativeHeap = ((NativeMemoryDX) memoryNode).nativeHeap;
    }

    public NativeHeapBuffer generateNativeHeapBuffer(int initialCapacity, boolean allocateDirect) {
        return this.nativeHeap.createBuffer(initialCapacity, allocateDirect);
    }

    public DirectXBuffer method13911(int i_1) {
        if (this.anInterface328834.getCapacity() < i_1 * 2) {
            this.anInterface328834.create(i_1);
        }

        return this.anInterface328834;
    }

    @Override
    public int getOffheap() {
        return this.textureBuffersSizeOccupied + this.texturesSizeOccupied + this.renderBufferSizeOccupied;
    }

    @Override
    public NativeSprite generateSprite(SpriteType spriteType, boolean bool2) {
        NativeSprite nativeSprite;
        if (spriteType.width != 0 && spriteType.height != 0) {
            int[] spritePixels = new int[spriteType.width * spriteType.height];
            int index = 0;
            int pixelIndex = 0;
            if (spriteType.alpha != null) {
                for (int y = 0; y < spriteType.height; y++) {
                    for (int x = 0; x < spriteType.width; x++) {
                        spritePixels[pixelIndex++] = spriteType.alpha[index] << 24 | spriteType.pallete[spriteType.pixels[index] & 0xff];
                        index++;
                    }
                }
            } else {
                for (int y = 0; y < spriteType.height; y++) {
                    for (int x = 0; x < spriteType.width; x++) {
                        int color = spriteType.pallete[spriteType.pixels[index++] & 0xff];
                        spritePixels[pixelIndex++] = color != 0 ? ~0xffffff | color : 0;
                    }
                }
            }
            nativeSprite = createNativeSprite(spritePixels, spriteType.width, spriteType.width, spriteType.height);
        } else {
            nativeSprite = createNativeSprite(new int[1], 1, 1, 1);
        }
        nativeSprite.setSize(spriteType.minX, spriteType.minY, spriteType.borderX, spriteType.borderY);
        return nativeSprite;
    }

    @Override
    public NativeSprite createNativeSprite(int[] ints_1, int i_2, int i_3, int i_4, int i_5, boolean bool6) {
        return new SpriteDX_Sub3(this, i_4, i_5, ints_1, i_2, i_3);
    }

    public Matrix44 getProjectionMatrix() {
        return this.projectionMatrix;
    }

    @Override
    public ComponentSurface createComponentSurface(int i_1, int i_2, int[] ints_3, int[] ints_4) {
        return Class455_Sub1.method13769(this, i_1, i_2, ints_3, ints_4);
    }

    abstract void toggleBlending();

    @Override
    public Model createModel(Mesh rsmesh1, int i_2, int i_3, int i_4, int i_5) {
        return new ModelDX(this, rsmesh1, i_2, i_4, i_5, i_3);
    }

    @Override
    public int mergeFunctionMasks(int mask1, int mask2) {
        return mask1 | mask2;
    }

    @Override
    public Ground createGround(int i_1, int i_2, int[][] ints_3, int[][] ints_4, int i_6, int i_7) {
        return new GroundDX(this, i_6, i_7, i_1, i_2, ints_3, ints_4);
    }

    @Override
    public Matrix43 getRenderingMatrix() {
        return this.aClass2948842;
    }

    @Override
    public void setSunCubeTexture(AbstractCubeTextureGenerator class1521) {
        this.aClass152Sub28731 = (AbstractNativeCubeTextureGenerator) class1521;
    }

    @Override
    public void calculateViewportCenter() {
        this.viewportX = 0;
        this.viewportY = 0;
        this.viewportWidth = super.display.getWidth();
        this.viewportHeight = super.display.getHeight();
        calculateViewport();
    }

    @Override
    public void setDrawableArea(int x, int y, int width, int height) {
        this.viewportX = x;
        this.viewportY = y;
        this.viewportWidth = width;
        this.viewportHeight = height;
        calculateViewport();
    }

    public void calculateViewport() {
        if (this.matrixMode == MatrixMode.MODEL_VIEW) {
            this.anInt8749 = this.viewportX;
            this.anInt8776 = this.viewportY;
            this.anInt8751 = this.viewportWidth;
            this.anInt8752 = this.viewportHeight;
            this.depthRangeNear = this.aFloat8737;
            this.depthRangeFar = this.aFloat8738;
        } else {
            this.anInt8749 = 0;
            this.anInt8776 = 0;
            this.anInt8751 = super.display.getWidth();
            this.anInt8752 = super.display.getHeight();
            this.depthRangeNear = 0.0F;
            this.depthRangeFar = 1.0F;
        }

        setViewport();
        this.viewportCenterX = this.viewportWidth / 2.0F;
        this.viewportCenterY = this.viewportHeight / 2.0F;
        this.aFloat8797 = this.viewportX + this.viewportCenterX;
        this.aFloat8741 = this.viewportY + this.viewportCenterY;
    }



    @Override
    public void getClipping(int[] clipping) {
        clipping[0] = this.cLeft;
        clipping[1] = this.cTop;
        clipping[2] = this.cRight;
        clipping[3] = this.cBottom;
    }

    @Override
    public void expandClipping(int x, int y, int width, int height) {
        int displayHeight;
        int displayWidth;
        if (super.display != null) {
            displayWidth = super.display.getWidth();
            displayHeight = super.display.getHeight();
        } else {
            displayHeight = 0;
            displayWidth = 0;
        }

        x = Math.max(0, x);
        width = Math.min(width, displayWidth);
        y = Math.max(0, y);
        height = Math.min(height, displayHeight);
        boolean changed = false;
        if (this.cLeft < x) {
            this.cLeft = x;
            changed = true;
        }

        if (this.cRight > width) {
            this.cRight = width;
            changed = true;
        }

        if (this.cTop < y) {
            this.cTop = y;
            changed = true;
        }

        if (this.cBottom > height) {
            this.cBottom = height;
            changed = true;
        }

        if (changed) {
            if (!this.boundsCalculated) {
                this.boundsCalculated = true;
                toggleScissorTest();
            }

            setScissorRect();
        }

    }

    public void method13919() {
        this.anInt8823 = 0;
        this.anInt8754 = 0;
        setViewport();
        setScissorRect();
    }

    abstract void setViewport();

    abstract void setScissorRect();

    abstract void toggleScissorTest();

    public abstract void drawPrimitive(PrimitiveDrawMode var1, int var2, int var3);


    public void method13926() {
        this.transformMatrix.copyFrom(this.particleMatrix);
        this.transformMatrix.multiplyBy(this.aClass3848724);
        this.transformMatrix.method6539(this.aFloatArrayArray8730[0]);
        this.transformMatrix.method6540(this.aFloatArrayArray8730[1]);
        this.transformMatrix.method6535(this.aFloatArrayArray8730[2]);
        this.transformMatrix.method6536(this.aFloatArrayArray8730[3]);
        this.transformMatrix.method6613(this.aFloatArrayArray8730[4]);
        this.transformMatrix.method6538(this.aFloatArrayArray8730[5]);
        this.aClass3848729.copyFrom(this.transformMatrix);
        loadWorldViewProjectionMatrix(this.aClass3848729);
    }

    @Override
    public Matrix44 copyModelMatrix() {
        return this.aClass3848724;
    }

    public void requestViewPositionUpdate() {
        this.viewPositionCalibrated = false;
    }

    public Matrix44 getModelViewMatrix() {
        if (this.matrixMode == MatrixMode.MODEL_VIEW) {
            if (!this.viewPositionCalibrated) {
                calibrateViewPosition();
            }
            return this.modelViewMatrix;
        } 
        return this.modelMatrix;
    }

    public Matrix44 method13931() {
        return this.aClass3848729;
    }

    public void loadWorldViewProjectionMatrix() {
        if (!this.aBool8720) {
            int height;
            int width;
            if (super.display != null) {
                width = super.display.getWidth();
                height = super.display.getHeight();
            } else {
                height = 0;
                width = 0;
            }

            Matrix44 matrix443 = this.aClass3848774;
            if (width != 0 && height != 0) {
                matrix443.method6530(0.0F, width, 0.0F, height, -1.0f, 1.0F);
            } else {
                matrix443.identity();
            }

            this.worldViewProjectionMatrix.copyFrom(matrix443);
            loadWorldViewProjectionMatrix(this.worldViewProjectionMatrix);
            this.aBool8720 = true;
        }

    }

    public abstract void loadWorldViewProjectionMatrix(Matrix44 cacheInto);

    void method13934() {
        if (this.matrixMode != MatrixMode.aClass33380) {
            MatrixMode lastMode = this.matrixMode;
            this.matrixMode = MatrixMode.aClass33380;
            if (lastMode == MatrixMode.MODEL_VIEW) {
                requestViewPositionUpdate();
            }

            this.aClass3848708 = this.aClass3848725;
            calculateViewRange();
            calculateViewport();
            this.nativeFunctionMask &= -16;
        }

    }

    public void method13935() {
        if (this.matrixMode != MatrixMode.PROJECTION) {
            MatrixMode lastMode = this.matrixMode;
            this.matrixMode = MatrixMode.PROJECTION;
            if (lastMode == MatrixMode.MODEL_VIEW) {
                requestViewPositionUpdate();
            }

            loadWorldViewProjectionMatrix();
            this.aClass3848708 = this.aClass3848774;
            calculateViewRange();
            calculateViewport();
            this.nativeFunctionMask &= -9;
        }

    }

    public void method13936() {
        this.aBool8720 = false;
        if (this.matrixMode == MatrixMode.PROJECTION) {
            loadWorldViewProjectionMatrix();
            calculateViewRange();
        }

    }

    public void method13937() {
        if (this.matrixMode != MatrixMode.MODEL_VIEW) {
            this.matrixMode = MatrixMode.MODEL_VIEW;
            requestViewPositionUpdate();
            method13940();
            this.aClass3848708 = this.aClass3848724;
            calculateViewRange();
            calculateViewport();
            this.nativeFunctionMask &= -8;
        }

    }

    public void method13938() {
        this.aBool8723 = false;
        method13940();
        if (this.matrixMode == MatrixMode.MODEL_VIEW) {
            calculateViewRange();
        }
    }

    public void method13940() {
        if (!this.aBool8723) {
            this.aBool8723 = true;
        }

    }


    @Override
    public void setDepthBuffering(boolean enabled) {
        this.depthBufferingEnabled = enabled;
        setDepthRenderState();
    }

    void toggleDepthTest(boolean depthTest) {
        if (this.enableDepthTest != depthTest) {
            this.enableDepthTest = depthTest;
            toggleDepthTest();
            this.nativeFunctionMask &= -16;
        }
    }

    void method13942(boolean bool1) {
        if (this.aBool8756 != bool1) {
            this.aBool8756 = bool1;
            setDepthRenderState();
            this.nativeFunctionMask &= -16;
        }
    }

    public void method13944() {
        this.anInterface48746 = createStream(false);
        short s_1 = 160;
        this.anInterface48746.create(s_1, 32);
        this.directBuffer.clear();
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putInt(-1);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putInt(-1);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putInt(-1);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putInt(-1);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(1.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putInt(-1);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.anInterface48746.upload(0, this.directBuffer.position(), this.directBufferAddress);
        this.aClass708786 = method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(new VertexDeclarationAction[]{VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69690, VertexDeclarationAction.aClass69692, VertexDeclarationAction.aClass69692})});
    }

    @Override
    public void calculateProjection(float f_1, float f_2, float f_3, float[] floats_4) {
        float f_5 = this.transformMatrix.faceList[15] + this.transformMatrix.faceList[3] * f_1 + this.transformMatrix.faceList[7] * f_2 + this.transformMatrix.faceList[11] * f_3;
        float f_6 = this.transformMatrix.faceList[12] + this.transformMatrix.faceList[0] * f_1 + this.transformMatrix.faceList[4] * f_2 + this.transformMatrix.faceList[8] * f_3;
        float f_7 = this.transformMatrix.faceList[13] + this.transformMatrix.faceList[1] * f_1 + this.transformMatrix.faceList[5] * f_2 + this.transformMatrix.faceList[9] * f_3;
        float f_8 = this.particleMatrix.faceList[14] + this.particleMatrix.faceList[2] * f_1 + this.particleMatrix.faceList[6] * f_2 + this.particleMatrix.faceList[10] * f_3;
        floats_4[0] = this.aFloat8797 + this.viewportCenterX * f_6 / f_5;
        floats_4[1] = this.aFloat8741 + this.viewportCenterY * f_7 / f_5;
        floats_4[2] = f_8;
    }

    @Override
    public void computeWorldToScreen(float x, float y, float z, float[] screenCoords) {
        float wX = this.transformMatrix.faceList[14] + this.transformMatrix.faceList[2] * x + this.transformMatrix.faceList[6] * y + this.transformMatrix.faceList[10] * z;
        float wY = this.transformMatrix.faceList[15] + this.transformMatrix.faceList[3] * x + this.transformMatrix.faceList[7] * y + this.transformMatrix.faceList[11] * z;
        if (wX >= -wY && wX <= wY) {
            float wZ = this.transformMatrix.faceList[12] + this.transformMatrix.faceList[0] * x + this.transformMatrix.faceList[4] * y + this.transformMatrix.faceList[8] * z;
            if (wZ >= -wY && wZ <= wY) {
                float f_8 = this.transformMatrix.faceList[13] + this.transformMatrix.faceList[1] * x + this.transformMatrix.faceList[5] * y + this.transformMatrix.faceList[9] * z;
                if (f_8 >= -wY && f_8 <= wY) {
                    float screenZ = this.particleMatrix.faceList[14] + this.particleMatrix.faceList[2] * x + this.particleMatrix.faceList[6] * y + this.particleMatrix.faceList[10] * z;
                    screenCoords[0] = this.aFloat8797 + this.viewportCenterX * wZ / wY;
                    screenCoords[1] = this.aFloat8741 + this.viewportCenterY * f_8 / wY;
                    screenCoords[2] = screenZ;
                } else {
                    screenCoords[2] = Float.NaN;
                    screenCoords[1] = Float.NaN;
                    screenCoords[0] = Float.NaN;
                }
            } else {
                screenCoords[2] = Float.NaN;
                screenCoords[1] = Float.NaN;
                screenCoords[0] = Float.NaN;
            }
        } else {
            screenCoords[2] = Float.NaN;
            screenCoords[1] = Float.NaN;
            screenCoords[0] = Float.NaN;
        }

    }

    public void toggleLights(boolean bool1) {
        if (this.aBool8684 != bool1) {
            this.aBool8684 = bool1;
            toggleLights();
            this.nativeFunctionMask &= -8;
        }
    }

    @Override
    public void copyLights(int copyLength, PointLight[] lightNodesSrc) {
        if (this.lightSources == null || lightNodesSrc == null) return;
        if (copyLength >= 0) System.arraycopy(lightNodesSrc, 0, this.lightSources, 0, copyLength);
    	
    	this.lastSceneLightCount = copyLength;
        if (this.matrixMode == MatrixMode.MODEL_VIEW) {
            method13903();
        }
    }

    @Override
    public void updateSun(float intensity) {
        if (this.lightIntensity != intensity) {
            this.lightIntensity = intensity;
            setFogColorAndIntensity();
            enableLights();
        }
    }

    @Override
    public void applyLighting(int color, float intensity, float f_3, float positionX, float positionY, float positionZ) {
        boolean lightChanged = this.baseLightColor != color;
        if (lightChanged || this.sunColorIntensity != intensity || this.antiSunColorIntensity != f_3) {
            this.baseLightColor = color;
            this.sunColorIntensity = intensity;
            this.antiSunColorIntensity = f_3;
            if (lightChanged) {
                this.baseLightColorRed = (this.baseLightColor & 0xff0000) / 1.671168E7F;
                this.baseLightColorGreen = (this.baseLightColor & 0xff00) / 65280.0F;
                this.baseLightColorBlue = (this.baseLightColor & 0xff) / 255.0F;
                setFogColorAndIntensity();
            }
            setSunParameters();
        }
        if (this.baseLightPosition[0] != positionX || this.baseLightPosition[1] != positionY || this.baseLightPosition[2] != positionZ) {
            this.baseLightPosition[0] = positionX;
            this.baseLightPosition[1] = positionY;
            this.baseLightPosition[2] = positionZ;
            this.baseLightInvertedPosition[0] = -positionX;
            this.baseLightInvertedPosition[1] = -positionY;
            this.baseLightInvertedPosition[2] = -positionZ;
            float size = (float) (1.0D / Math.sqrt(positionX * positionX + positionY * positionY + positionZ * positionZ));
            this.sunPosition[0] = positionX * size;
            this.sunPosition[1] = positionY * size;
            this.sunPosition[2] = positionZ * size;
            this.lightPosition[0] = -this.sunPosition[0];
            this.lightPosition[1] = -this.sunPosition[1];
            this.lightPosition[2] = -this.sunPosition[2];
            setLightPosition();
            this.shadowOffsetX = (int) (positionX * 256.0F / positionY);
            this.shadowOffsetZ = (int) (positionZ * 256.0F / positionY);
        }
        enableLights();
    }

    @Override
    public int mismatchingFunctionMask(int maskA, int maskB) {
        return maskA & maskB ^ maskB;
    }

    public void setColourType(int i_1) {
        switch (i_1) {
            case 0:
                method13966(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE);
                break;
            case 1:
                method13966(TextureCombineMode.REPLACE, TextureCombineMode.REPLACE);
                break;
            case 2:
                method13966(TextureCombineMode.INTERPOLATE, TextureCombineMode.REPLACE);
                break;
            case 3:
                method13966(TextureCombineMode.ADD, TextureCombineMode.MODULATE);
                break;
            case 4:
                method13966(TextureCombineMode.SUBTRACT, TextureCombineMode.SUBTRACT);
        }

    }

    abstract void setFogColorAndIntensity();

    abstract void setSunParameters();

    abstract void setLightPosition();

    abstract void enableLights();

    @Override
    public int getMaxLights() {
        return this.maxActiveLights - 2;
    }

    abstract void toggleSun();

    abstract void configureLights();

    public abstract boolean method13954(GLFormat var1, GLActionFormat var2);

    public abstract boolean method13955(GLFormat var1, GLActionFormat var2);

    abstract void toggleDepthTest();

    public NativeTextureSampler createTextureSampler(GLFormat class1501, int i_2, int i_3, boolean bool4, byte[] bytes_5) {
        return createNativeTextureSampler(class1501, i_2, i_3, bool4, bytes_5);
    }

    public NativeTextureSampler createHdrTexture(GLFormat format, int width, int length, float[] pixels) {
        return createNativeTexture(format, width, length, pixels);
    }

    public void method13960() {
        if (this.textureStates[this.currentTextureSlot] != TextureState.INITIALIZE) {
            this.textureStates[this.currentTextureSlot] = TextureState.INITIALIZE;
            this.textureMatrices[this.currentTextureSlot].identity();
            method14133();
        }
    }

    abstract NativeTextureSampler createNativeTexture(GLFormat format, int width, int length, float[] pixels);

    @Override
    public Matrix43 copySunPosition() {
        return this.aClass2948706;
    }



    public abstract Texture3DSampler method13962(GLFormat var1, byte[] var6);

    @Override
    public void resetTextureState() {
    }

    public int getCurrentTextureSlot() {
        return this.currentTextureSlot;
    }

    @Override
    public void drawTransparentHorizontalLine(int x, int y, int width, int colorRgba, int blendMode) {
        drawTransparentLine(x, y, width + x, y, colorRgba, blendMode);
    }


    public abstract void setTextureOnCurrentSlot();

    public CubeTextureSampler getEnvironmentMapSampler() {
        return this.aClass152Sub28731 != null ? this.aClass152Sub28731.getNativeCubeTexture() : null;
    }

    public void method13966(TextureCombineMode class681, TextureCombineMode class682) {
        boolean bool3 = false;
        if (class681 != this.textureRgbCombineModes[this.currentTextureSlot]) {
            this.textureRgbCombineModes[this.currentTextureSlot] = class681;
            setTextureColorStage();
            bool3 = true;
        }

        if (class682 != this.textureAlphaCombineModes[this.currentTextureSlot]) {
            this.textureAlphaCombineModes[this.currentTextureSlot] = class682;
            setTextureAlphaStage();
            bool3 = true;
        }

        if (bool3) {
            this.nativeFunctionMask &= -14;
        }

    }

    public void method13967(int i_1, TextureStageState class672) {
        setTextureStageState(i_1, class672);
    }

    abstract void setTextureStageState(int var1, TextureStageState var2);

    public void method13969(int i_1, TextureStageState class672) {
        method13970(i_1, class672);
    }

    abstract void method13970(int var1, TextureStageState var2);

    public void setColourValue(int i_1) {
        if (this.envColor != i_1) {
            this.envColor = i_1;
            setTextureEnvironmentColor();
        }

    }

    public Matrix44 method13973() {
        return this.textureMatrices[this.currentTextureSlot];
    }

    public void setTextureState(TextureState textureState) {
        this.textureStates[this.currentTextureSlot] = textureState;
        method14133();
    }


    public void method13976() {
        setFogColorAndIntensity();
        setSunParameters();
        toggleLights();
        method13903();
        setLightPosition();
        enableLights();
        toggleSun();
        toggleDepthTest();
        setDepthRenderState();
        toggleFog();
        configureFog();
        toggleBlending();
        blendDrawBuffer();
        toggleAlpha();

        for (int slot = this.maxConcurrentTextures - 1; slot >= 0; --slot) {
            setTextureSlot(slot);
            setTextureColorStage();
            setTextureAlphaStage();
            method13960();
        }

        setTextureEnvironmentColor();
        setViewport();
    }

    abstract void activateCurrentTexture();

    abstract void setTextureColorStage();

    abstract void setTextureAlphaStage();

    @Override
    public void addWater(WaterTile waterTile) {
        this.renderedWaterColor = -1;
        this.renderedWaterTile = waterTile;
        this.missingWaterColor = true;
    }

    public void method13985() {
        if (this.anInt8844 != 0) {
            this.anInt8844 = 0;
            blendDrawBuffer();
        }

    }

    public abstract void colorMask(int var1);

    @Override
    public void resetClipping() {
        if (super.display != null) {
            this.cTop = 0;
            this.cLeft = 0;
            this.cRight = super.display.getWidth();
            this.cBottom = super.display.getHeight();
        } else {
            this.cBottom = 0;
            this.cRight = 0;
            this.cTop = 0;
            this.cLeft = 0;
        }

        if (this.boundsCalculated) {
            this.boundsCalculated = false;
            toggleScissorTest();
        }

    }

    abstract void setTextureEnvironmentColor();

    void toggleFog(boolean enabled) {
        if (this.fogEnabled != enabled) {
            this.fogEnabled = enabled;
            toggleFog();
            this.nativeFunctionMask &= -16;
        }
    }

    @Override
    public void calculateFog(int color, int distance, int i_3) {
        if (this.fogColorRgb != color || distance != this.fogDistance || i_3 != this.anInt8680) {
            this.fogColorRgb = color;
            this.fogDistance = distance;
            this.anInt8680 = i_3;
            method14062();
            toggleFog();
        }
    }

    @Override
    public void setSunPosition(Matrix43 matrix44var1) {
        this.aClass2948706 = matrix44var1;
        this.particleMatrix.fromMatrix(this.aClass2948706);
        this.aClass2948804.copyFrom(matrix44var1);
        this.aClass2948804.updateIdentitiyMatrix();
        this.aClass3848814.fromMatrix(this.aClass2948804);
        method13926();
        if (this.matrixMode == MatrixMode.MODEL_VIEW) {
            requestViewPositionUpdate();
        }

    }

    abstract void toggleFog();

    abstract void toggleMultisampling(boolean var1);

    public abstract DirectXBuffer method13993(boolean var1);

    public abstract NativeStream createStream(boolean var1);

    public abstract VertexDeclaration method13995(VertexDeclarationComponent[] var1);

    public abstract void setVertexDeclaration(VertexDeclaration var1);

    @Override
    public void clearRectangle(ComponentSurface class4552, int i_3, int i_4) {
        setDepthBuffering(false);
        this.aNativeSpriteSub38791.method2766(0.0F, 0.0F, getDisplay().getWidth(), 0.0F, 0.0F, getDisplay().getHeight(), 0, class4552, i_3, i_4);
        setDepthBuffering(true);
    }

    public abstract void method13997(DirectXBuffer var1);

    public Matrix44 getEyePositionOsMatrix() {
        return this.matrixMode == MatrixMode.MODEL_VIEW ? this.aClass3848814 : aClass3848712;
    }



    @Override
    public void drawRectangleOutline(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        drawTransparentLine(i_1, i_2, i_3 + i_1, i_2, i_5, i_6);
        drawTransparentLine(i_1, i_2 + i_4, i_3 + i_1, i_2 + i_4, i_5, i_6);
        drawTransparentLine(i_1, i_2, i_1, i_2 + i_4, i_5, i_6);
        drawTransparentLine(i_3 + i_1, i_2, i_3 + i_1, i_2 + i_4, i_5, i_6);
    }

    @Override
    public void renderWorldmapTile(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, byte[] bytes_7, int i_8, int i_9) {
    }

    @Override
    public void benchmark() {
    }

    @Override
    public void drawCircle(int i_1, int i_2, int i_3, int i_4) {
    }

    @Override
    public void drawTransparentLine(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        drawThickLine(i_1, i_2, i_3, i_4, i_5, 1, i_6);
    }

    @Override
    public void drawLine(int i_1, int i_2, int i_3, int i_4, int i_5, int i_7, int i_8, int i_9) {
        float f_10 = (float) i_3 - i_1;
        float f_11 = (float) i_4 - i_2;
        if (f_10 == 0.0F && f_11 == 0.0F) {
            f_10 = 1.0F;
        } else {
            float f_23 = (float) (1.0D / Math.sqrt(f_10 * f_10 + f_11 * f_11));
            f_10 *= f_23;
            f_11 *= f_23;
        }

        method13935();
        AbstractWaterShader class4112 = this.waterShadersByEffect[13];
        class4112.method873();
        class4112.method875();
        toggleBlending(1);
        class4112.method879();
        toggleMultisampling(false);
        i_9 %= i_8 + i_7;
        float f_13 = f_10 * i_7;
        float f_14 = f_11 * i_7;
        float f_15 = 0.0F;
        float f_16 = 0.0F;
        float f_17 = f_13;
        float f_18 = f_14;
        if (i_9 > i_7) {
            f_15 = f_10 * (i_8 + i_7 - i_9);
            f_16 = f_11 * (i_8 + i_7 - i_9);
        } else {
            f_17 = f_10 * (i_7 - i_9);
            f_18 = f_11 * (i_7 - i_9);
        }

        float f_19 = i_1 + f_15;
        float f_20 = i_2 + f_16;
        float f_21 = f_10 * i_8;
        float f_22 = f_11 * i_8;

        while (true) {
            if (i_3 > i_1) {
                if (f_19 > i_3) {
                    break;
                }

                if (f_19 + f_17 > i_3) {
                    f_17 = i_3 - f_19;
                }
            } else {
                if (f_19 < i_3) {
                    break;
                }

                if (f_19 + f_17 < i_3) {
                    f_17 = i_3 - f_19;
                }
            }

            if (i_4 > i_2) {
                if (f_20 > i_4) {
                    break;
                }

                if (f_20 + f_18 > i_4) {
                    f_18 = i_4 - f_20;
                }
            } else {
                if (f_20 < i_4) {
                    break;
                }

                if (f_20 + f_18 < i_4) {
                    f_18 = i_4 - f_20;
                }
            }

            if (!method13908(f_19, f_20, f_19 + f_17, f_20 + f_18)) {
                return;
            }

            drawPrimitiveLines();
            f_19 += f_21 + f_17;
            f_20 += f_22 + f_18;
            f_17 = f_13;
            f_18 = f_14;
        }

        toggleMultisampling(true);
        class4112.method885();
    }

    @Override
    public void drawTexturedLine(int i_1, int i_2, int i_3, int i_4, int i_5, ComponentSurface class4557, int i_8, int i_9, int i_10, int i_11, int i_12) {
    }

    @Override
    public void drawThickLine(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        i_1 = (int) (i_1 + 1.0F);
        i_2 = (int) (i_2 + 1.0F);
        i_3 = (int) (i_3 + 1.0F);
        i_4 = (int) (i_4 + 1.0F);
        float f_8 = (i_3 - i_1);
        float f_9 = (i_4 - i_2);
        float f_10 = 1.0F / (float) Math.sqrt(f_8 * f_8 + f_9 * f_9);
        f_8 *= f_10;
        f_9 *= f_10;
        i_1 = (int) (i_1 - f_8);
        i_2 = (int) (i_2 - f_9);
        float f_11 = -f_9;
        f_11 *= 0.5F * i_6;
        float f_12 = f_8 * 0.5F * i_6;
        this.aNativeSpriteSub38790.method2763(i_1 - f_11, i_2 - f_12, i_3 - f_11, i_4 - f_12, i_1 + f_11, i_2 + f_12, 0, i_5, i_7);
    }

    @Override
    public void renderParticles(ParticleWrapper particleWrapper) {
        this.particleRenderer.render(this, particleWrapper);
    }

    public void drawPrimitiveLines() {
        setStreamSource(0, this.vertexStream);
        setVertexDeclaration(this.vertexDeclaration);
        drawPrimitive(PrimitiveDrawMode.LINES, 0, 1);
    }

    public void method14001(PrimitiveDrawMode class3521) {
        setStreamSource(0, this.anInterface48746);
        setVertexDeclaration(this.aClass708786);
        drawPrimitive(class3521, 0, 2);
    }

 

    public abstract void drawIndexedPrimitive(PrimitiveDrawMode var1, int var2, int var3, int var4, int var5);

    public void method14003() {
        if (this.nativeFunctionMask != 2) {
            method13935();
            toggleFog(false);
            toggleLights(false);
            toggleDepthTest(false);
            method13942(false);
            this.nativeFunctionMask = 2;
        }

    }

    public void method14004() {
        if (this.nativeFunctionMask != 8) {
            method13937();
            toggleFog(true);
            toggleDepthTest(true);
            method13942(true);
            toggleBlending(1);
            this.nativeFunctionMask = 8;
        }

    }

    @Override
    public void setModelMatrix(Matrix44 matrix441) {
        this.aClass3848724.copyFrom(matrix441);
        method13926();
        if (this.matrixMode == MatrixMode.MODEL_VIEW) {
            calculateViewRange();
        }

    }


    public void method14009() {
        this.vertexStream = createStream(true);
        this.vertexStream.create(24, 12);
        this.vertexDeclaration = method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(VertexDeclarationAction.aClass69695)});
    }

    abstract void configureFog();

    @Override
    public boolean method8404() {
        return true;
    }

    @Override
    public AbstractCubeTextureGenerator createCubeTexture(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        return new NativeCubeTextureGenerator(this, i_1, i_2, i_3, i_4, i_5, i_6);
    }


    public NativeTextureSampler createTexture(int width, int length, boolean bool3, int[] pixels) {
        return createTextureSampler(width, length, bool3, pixels, 0, 0);
    }


    @Override
    public void resetDepthClippingMask() {
        this.aFloat8737 = (float) 0.0;
        this.aFloat8738 = (float) 1.0;
        calculateViewport();
    }


    @Override
    public void generateFlags() {
        int prime = 32;
        for (this.shadowSizeShift = 0; prime > 1; prime >>= 1) {
            this.shadowSizeShift++;
        }
        this.activeFlags = 1 << this.shadowSizeShift;
    }


    @Override
    public Matrix44 method8449() {
        return this.aClass3848841;
    }

    public void calculateViewRange() {
        this.projectionMatrix.copyFrom(this.aClass3848708);
        loadWorldViewProjectionMatrix(this.projectionMatrix);
        this.camFar = (this.projectionMatrix.faceList[14] - this.projectionMatrix.faceList[15]) / (this.projectionMatrix.faceList[11] - this.projectionMatrix.faceList[10]);
        this.camNear = -this.projectionMatrix.faceList[14] / this.projectionMatrix.faceList[10];
        method14062();
    }


    @Override
    public AbstractFontRenderer createFontRenderer(FontMetrics fontMetrics, SpriteType[] spriteTypes, boolean isPlain) {
        return new NativeFont(this, fontMetrics, spriteTypes, isPlain);
    }


    public abstract CubeTextureSampler generateNativeCubeTexture(int largestSize, boolean mipmappingEnabled, int[][] pixels);

    public void setTextureSampler(TextureSampler textureSampler) {
        if (textureSampler != this.textureSamplers[this.currentTextureSlot]) {
            this.textureSamplers[this.currentTextureSlot] = textureSampler;
            if (textureSampler != null) {
                textureSampler.bindTexture();
            } else {
                this.activeTextureSampler.bindTexture();
            }
            this.nativeFunctionMask &= -2;
        }
    }

    public Matrix44 getModelMatrix() {
        return this.modelMatrix;
    }



    @Override
    public void setClipping(int i_1, int i_2, int i_3, int i_4) {
        int displayHeight;
        int displayWidth;
        if (super.display != null) {
            displayWidth = super.display.getWidth();
            displayHeight = super.display.getHeight();
        } else {
            displayHeight = 0;
            displayWidth = 0;
        }

        if (i_1 <= 0 && i_3 >= displayWidth - 1 && i_2 <= 0 && i_4 >= displayHeight - 1) {
            resetClipping();
        } else {
            this.cLeft = Math.max(0, i_1);
            this.cRight = Math.min(i_3, displayWidth);
            this.cTop = Math.max(0, i_2);
            this.cBottom = Math.min(i_4, displayHeight);
            if (!this.boundsCalculated) {
                this.boundsCalculated = true;
                toggleScissorTest();
            }
            setScissorRect();
        }
    }


    @Override
    public boolean hasShadows() {
        return true;
    }


    abstract void setDepthRenderState();

    @Override
    public void method8525(int i_3, int i_4) {
        this.aClass328714.method830(i_3, i_4);
    }


    public void method14062() {
        if (this.aClass418753 != null) {
            this.aClass418753.method877();
        }

        configureFog();
    }

    public abstract RectangleTextureSampler method14064(GLFormat var1, GLActionFormat var2, int var3, int var4);

    public abstract NativeTextureSampler method14072(GLFormat var1, GLActionFormat var2, int var3, int var4);

    byte[] fetchShader(String archiveName, String fileName) {
        return this.shaderResource.getNamedFile(archiveName, fileName);
    }

    @Override
    public void addWaterTile(int color, WaterTile waterTile) {
        if (!this.missingWaterColor) {
            throw new RuntimeException("");
        } 
        this.renderedWaterColor = color;
        this.renderedWaterTile = waterTile;
        if (this.aBool8805) {
            this.waterShadersByEffect[3].method876();
            this.waterShadersByEffect[3].method877();
        }
    }


    public abstract NativeTextureSampler createTextureSampler(int var1, int var2, boolean var3, int[] var4, int var5, int var6);

    public abstract void setPolygonMode(int var1);

    @Override
    public void method8669(int i_1, int i_2, int i_3, int i_4, int i_5, ComponentSurface class4557, int i_8, int i_9) {
    }

    @Override
    public void onTilesDrawn() {
        this.missingWaterColor = false;
    }


    @Override
    public int isOnScreen(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        int i_7 = 0;
        float f_8 = this.transformMatrix.faceList[14] + this.transformMatrix.faceList[2] * i_1 + this.transformMatrix.faceList[6] * i_2 + this.transformMatrix.faceList[10] * i_3;
        float f_9 = this.transformMatrix.faceList[14] + this.transformMatrix.faceList[2] * i_4 + this.transformMatrix.faceList[6] * i_5 + this.transformMatrix.faceList[10] * i_6;
        float f_10 = this.transformMatrix.faceList[15] + this.transformMatrix.faceList[3] * i_1 + this.transformMatrix.faceList[7] * i_2 + this.transformMatrix.faceList[11] * i_3;
        float f_11 = this.transformMatrix.faceList[15] + this.transformMatrix.faceList[3] * i_4 + this.transformMatrix.faceList[7] * i_5 + this.transformMatrix.faceList[11] * i_6;
        if (f_8 < -f_10 && f_9 < -f_11) {
            i_7 |= 0x10;
        } else if (f_8 > f_10 && f_9 > f_11) {
            i_7 |= 0x20;
        }

        float f_12 = this.transformMatrix.faceList[12] + this.transformMatrix.faceList[0] * i_1 + this.transformMatrix.faceList[4] * i_2 + this.transformMatrix.faceList[8] * i_3;
        float f_13 = this.transformMatrix.faceList[12] + this.transformMatrix.faceList[0] * i_4 + this.transformMatrix.faceList[4] * i_5 + this.transformMatrix.faceList[8] * i_6;
        if (f_12 < -f_10 && f_13 < -f_11) {
            i_7 |= 0x1;
        }

        if (f_12 > f_10 && f_13 > f_11) {
            i_7 |= 0x2;
        }

        float f_14 = this.transformMatrix.faceList[13] + this.transformMatrix.faceList[1] * i_1 + this.transformMatrix.faceList[5] * i_2 + this.transformMatrix.faceList[9] * i_3;
        float f_15 = this.transformMatrix.faceList[13] + this.transformMatrix.faceList[1] * i_4 + this.transformMatrix.faceList[5] * i_5 + this.transformMatrix.faceList[9] * i_6;
        if (f_14 < -f_10 && f_15 < -f_11) {
            i_7 |= 0x4;
        }

        if (f_14 > f_10 && f_15 > f_11) {
            i_7 |= 0x8;
        }

        return i_7;
    }


    public Matrix44 getViewMatrix() {
        return this.matrixMode == MatrixMode.MODEL_VIEW ? this.particleMatrix : aClass3848712;
    }

    @Override
    public boolean hasTextures() {
        return true;
    }

    @Override
    public void initialize() {
    }

    public Matrix43 calibrateViewPosition() {
        if (this.matrixMode == MatrixMode.MODEL_VIEW) {
            if (!this.viewPositionCalibrated) {
                // Combines model matrix and view matrix into modelview matrix?
                this.aClass2948710.copyFrom(this.aClass2948704);
                this.aClass2948710.multiply(this.aClass2948706);
                this.modelViewMatrix.fromMatrix(this.aClass2948710);
                this.viewPositionCalibrated = true;
            }

            return this.aClass2948710;
        } else {
            return this.aClass2948704;
        }
    }

    public void method14133() {
        setTextureMatrix();
    }


    abstract NativeTextureSampler createNativeTextureSampler(GLFormat var1, int var2, int var3, boolean var4, byte[] var5);

    @Override
    public boolean supportsFullscreen() {
        return true;
    }

    @Override
    public NativeSprite generateEmptySprite(int i_1, int i_2, boolean bool3, boolean bool4) {
        return new SpriteDX_Sub3(this, i_1, i_2, bool3, bool4);
    }


    public void initializeTables() {
        this.textureSamplers = new TextureSampler[this.maxConcurrentTextures];
        this.textureMatrices = new Matrix44[this.maxConcurrentTextures];
        this.textureStates = new TextureState[this.maxConcurrentTextures];
        this.textureRgbCombineModes = new TextureCombineMode[this.maxConcurrentTextures];
        this.textureAlphaCombineModes = new TextureCombineMode[this.maxConcurrentTextures];

        for (int i_1 = 0; i_1 < this.maxConcurrentTextures; i_1++) {
            this.textureAlphaCombineModes[i_1] = TextureCombineMode.MODULATE;
            this.textureRgbCombineModes[i_1] = TextureCombineMode.MODULATE;
            this.textureStates[i_1] = TextureState.INITIALIZE;
            this.textureMatrices[i_1] = new Matrix44();
        }

        this.lightSources = new PointLight[this.maxActiveLights - 2];
        //System.out.println("Active lights: "+(light_node_count-2));     //Active lights: 6
        
        int[] ints_3 = {-1};
        this.activeTextureSampler = createTextureSampler(1, 1, false, ints_3, 0, 0);
        ints_3[0] = ~0xffffff;
        this.anInterface68795 = createTextureSampler(1, 1, false, ints_3, 0, 0);
        setNativeHeap(new NativeMemoryDX(262144));
        this.aClass708832 = method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(new VertexDeclarationAction[]{VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69692})});
        method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(new VertexDeclarationAction[]{VertexDeclarationAction.aClass69695, VertexDeclarationAction.aClass69690})});
        this.aClass708836 = method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(VertexDeclarationAction.aClass69695), new VertexDeclarationComponent(VertexDeclarationAction.aClass69690), new VertexDeclarationComponent(VertexDeclarationAction.aClass69692), new VertexDeclarationComponent(VertexDeclarationAction.NORMAL_POINTER_ACTION)});
        this.aClass708721 = method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(VertexDeclarationAction.aClass69695), new VertexDeclarationComponent(VertexDeclarationAction.aClass69690), new VertexDeclarationComponent(VertexDeclarationAction.aClass69692)});

        for (int type = 0; type < 7; type++) {
            this.copyByTypeSrc[type] = new ModelDX(this, 0, 0, false, false);
            this.copyByTypeDst[type] = new ModelDX(this, 0, 0, true, true);
        }

        method13895();
        this.anInterface328834 = method13993(true);
        setupGraphics();
        getImageProducer();
        resetClipping();
        renderLastFrame();
        this.aNativeSpriteSub38790 = new SpriteDX_Sub3(this, this.activeTextureSampler);
        this.aNativeSpriteSub38791 = new SpriteDX_Sub3(this, this.anInterface68795);
        applyClearFlags(3, 0);
    }

    public void method14150() {
        method14001(PrimitiveDrawMode.TRIANGLE_FAN);
    }

    public abstract void setStreamSource(int var1, NativeStream var2);

    public void setTextureSlot(int textureSlot) {
        if (this.currentTextureSlot != textureSlot) {
            this.currentTextureSlot = textureSlot;
            activateCurrentTexture();
        }
    }

    public void method14177() {
        this.anInterface48693 = createStream(false);
        this.anInterface48693.create(3096, 12);
        this.directBuffer.clear();
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);
        this.directBuffer.putFloat(0.0F);

        for (int i_1 = 0; i_1 <= 256; i_1++) {
            double d_2 = (i_1 * 2) * Math.PI / 256.0D;
            float f_4 = (float) Math.cos(d_2);
            float f_5 = (float) Math.sin(d_2);
            this.directBuffer.putFloat(f_5);
            this.directBuffer.putFloat(f_4);
            this.directBuffer.putFloat(0.0F);
        }

        this.anInterface48693.upload(0, this.directBuffer.position(), this.directBufferAddress);
    }


    @Override
    public void drawFilledRectangle(int x, int y, int width, int height, int color, int alpha) {
        if (this.depthBufferingEnabled) {
            setDepthBuffering(false);
            this.aNativeSpriteSub38790.drawCircle(x, y, width, height, 0, color, alpha);
            setDepthBuffering(true);
        } else {
            this.aNativeSpriteSub38790.drawCircle(x, y, width, height, 0, color, alpha);
        }
    }

    @Override
    public void drawTransparentVerticalLine(int i_1, int i_2, int i_3, int i_4, int i_5) {
        drawTransparentLine(i_1, i_2, i_1, i_3 + i_2, i_4, i_5);
    }

    @Override
    public void method8474() {
        this.aClass328714.method833();
    }

    abstract void toggleLights();

    abstract void setTextureMatrix();

    abstract void toggleAlpha();

	public static void drawLoadingPleaseWaitHardware() {
	    GraphicsCache.resetWorldmapCaches(false);
	    if (AbstractToolkit.activeRenderMode >= 0 && AbstractToolkit.activeRenderMode != 0) {
	        client.loadAssets(AbstractToolkit.activeRenderMode, false);
	        AbstractToolkit.activeRenderMode = -1;
	    }
	}

}
