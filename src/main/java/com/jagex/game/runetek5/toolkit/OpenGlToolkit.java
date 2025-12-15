package com.jagex.game.runetek5.toolkit;

import com.jagex.game.runetek5.unidentified.*;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.MathUtils;

import jaclib.memory.Buffer;
import jaclib.memory.Stream;
import jaclib.memory.heap.NativeHeap;
import jaggl.OpenGL;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import java.awt.*;

import com.jagex.game.runetek5.buffer.NativeMemoryNode;
import com.jagex.game.runetek5.buffer.OpenGlRenderBuffer;
import com.jagex.game.runetek5.buffer.ArrayBufferGL;
import com.jagex.game.runetek5.buffer.ElementsBufferGL;
import com.jagex.game.runetek5.buffer.NativeMemoryGL;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.LinkedList;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.font.FontRendererGL;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelGL;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.graphics.shader.Texture3DShaderGL;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteGL;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.texture.OpenGLArbTextureCubeMapGenerator;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.AbstractOpenGlCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture_Sub1;
import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture;
import com.jagex.game.runetek5.graphics.texture.OpenGlCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.graphics.texture.RectangularTextureGL;
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.imageproducer.ImageProducerGL2;
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.rendering.RenderInfo;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundGL;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.systeminfo.OperatingSystemInfo;
import com.jagex.game.runetek5.toolkit.opengl.BlendType;
import com.jagex.game.runetek5.toolkit.opengl.EffectManagerGL;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.OpenGLTextureCache;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.toolkit.opengl.GlParticleRenderer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlArrayBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboArrayBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboElementsArrayBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer;
import com.jagex.game.runetek5.tools.TimeTools;
import com.jagex.game.runetek5.utils.Utils;

public class OpenGlToolkit extends AbstractToolkit {

    public static final int[] HANDLE_BUFFER = new int[1000];
    public static final int initialLightCount = 4;
    public static final float[] BUFFER_4F = new float[4];
    public static final float[] LIGHT_MATRIX = new float[4];
    public OpenGlTexture[] openglTextures;
    public OpenGl2DTexture texture2d;
    public IntegratedGraphicsDisplayGL integratedGraphicsImageProducer;
    public int activeBlendAlpha;
    public int activeBlendType;
    
    /**
     * Allow selective rendering based on fragment alpha values, 
     * determining whether to display or discard pixels depending on the reference alpha value set
     */
    public boolean alphaTestEnabled;
    
    public boolean colormaskActive;
    public int fogColor;
    public int sunColorRgb = -1;
    public boolean colortableReset;
    public final int currentTextureSize = 128;
    public final GlParticleRenderer particleRenderer = new GlParticleRenderer();
    public final Matrix44 aClass3848360 = new Matrix44();
    public final Matrix44 aClass3848361 = new Matrix44();
    public final Matrix43 aClass2948476 = new Matrix43();
    public int openglFlags = 8;
    public int shadowScale = 3;
    public final LinkedList heapManager = new LinkedList();
    public final LinkedList listsAwaitingDeallocation = new LinkedList();
    public final LinkedList vboBuffersAwaitingDeallocation = new LinkedList();
    public final LinkedList texturesAwaitingDeallocation = new LinkedList();
    public final LinkedList frameBuffersAwaitingDeallocation = new LinkedList();
    public final LinkedList renderBuffersAwaitingRemoval = new LinkedList();
    public final LinkedList arbProgramsAwaitingDeallocation = new LinkedList();
    public final LinkedList shaderProgramsAwaitingDeallocation = new LinkedList();
    public final Matrix43 aClass2948389 = new Matrix43();
    public final Matrix44 aClass3848348 = new Matrix44();
    public final Matrix44 aClass3848391 = new Matrix44();
    public final Matrix44 aClass3848442 = new Matrix44();
    public final Matrix44 aClass3848385 = new Matrix44();
    public final Matrix44 aClass3848394 = new Matrix44();
    public final float[][] aFloatArrayArray8395 = new float[6][4];
    public final float[] aFloatArray8396 = new float[4];
    public float depthClippingNear;
    public float depthClippingFar = 1.0F;
    public float fogMinimumStartPosition;
    public float aFloat8404 = -1.0f;
    public final Matrix43 aClass2948405 = new Matrix43();
    public final Matrix44 aClass3848380 = new Matrix44();
    public final Matrix44 aClass3848407 = new Matrix44();
    public final float[] textureMatrix = new float[16];
    public boolean depthBufferingEnabled = true;
    public int clipTop;
    public int clipBottom;
    public int clipLeft;
    public int clipRight;
    public int anInt8453;
    public int anInt8416;
    public final float[] sunPreviousPosition = new float[4];
    public final float[] antiSunPreviousPosition = new float[4];
    
    /**
     * For naming see: {@link com.jagex.game.runetek5.toolkit.opengl.effect.MaterialEffect9GL#toggle(boolean)}
     */
    public final float[] sunDirection = new float[4];
    
    public final float[] lightingDirection = new float[4];
    public float sunColorRed = 1.0F;
    public float sunColorGreen = 1.0F;
    public float sunColorBlue = 1.0F;
    public float aFloat8433 = -1.0f;
    public float aFloat8434 = -1.0f;
    public final PointLight[] lightNodes;
    public int fogDensity;
    public int anInt8378;
    public float densityFactor;
    public float fogMin;
    public boolean renderStillWater;
    public int textureCombineRgb;
    public int textureCombineAlpha;
    public float outlineCirclePointSize;
    public float fillCirclePointSize;
    public final ModelGL[] copySrc;
    public final ModelGL[] copyDst;
    public GlBitBuffer textureBuffer;
    public int[] projectedVerticesX;
    public int[] projectedVerticesY;
    public int[] anIntArray8502;
    public final byte[] shadowTextureData;
    public int multiSamplesPreference;
    public OpenGL activeOpenglInstance;
    public int pixelType;
    public boolean intelGpu;
    public boolean s3Graphics;
    public boolean arbTextureFloat;
    public OpenGLTextureCache textureCache;
    public Texture3DShaderGL aClass1648363;
    public Class170 aClass1708357;
    public boolean arbRectangleTexturesSupported;
    public boolean shouldUseArbVbo;
    public boolean radeonGpu;
    public RectangularTextureGL rectangularTexture;
    public EffectManagerGL effectManager;
    public boolean vertexbufferSupported;
    public boolean texture3dSupported;
    public NativeHeap nativeHeap;
    public ArrayBufferGL anInterface148496;
    public OpenGlArrayDrawBuffer aClass1438494;
    public OpenGlArrayDrawBuffer aClass1438495;
    public int waterDepth;
    public WaterTile waterTile;
    public boolean stillWaterToggled;
    public int anInt8417;
    public int anInt8418;
    public int imageProducerWidth;
    public int imageProducerHeight;
    public float imageProducerCenterX;
    public float imageProducerCenterY;
    public float aFloat8315;
    public float aFloat8399;
    public float sunBrightness;
    public int vboBuffersSize;
    public int textureBuffersSize;
    public int renderBuffersSize;
    public int textureAnimatingProgression;
    public long lastGcCycle;
    public int depthClippingMode;
    public int functionMask;
    public int anInt8438;
    public int anInt8439;
    public float fogEndPosition;
    public float fogStartPosition;
    public ArrayBufferGL currentArrayBuffer;
    public boolean textureUpdate;
    public int lastMaterialId;
    public int glTexturesCursorPos;
    public boolean lightingEnabled;
    public boolean hideLights;
    public boolean enableDepthMask;
    public int activeLightCount;
    public boolean fogEnabled;
    public OpenGl2DTexture_Sub1 aClass137Sub1Sub18462;
    public String gpuCompanyString;
    public String gpuNameString;
    public int openglVersion;
    public int maxTextureUnits;
    public int maxTextureCoordinates;
    public int maxTextureImageUnits;
    public boolean bigEndianess;
    public boolean arbMultisample;
    public boolean arbVertexProgram;
    public boolean arbVertexShader;
    public boolean arbFragmentShader;
    public boolean arbTextureCubeMap;
    public boolean aBool8498;
    public boolean framebufferBlit;
    public boolean framebufferMultisample;
    public boolean advancedFramebuffers;
    public boolean appleOperatingSystem;
    public int lastSceneLightCount;
    public boolean depthBuffering;
    public AbstractOpenGlCubeTextureGenerator aClass152Sub18317;
    public ElementsBufferGL currentElementsBuffer;

	public static final float[] SINE = new float[16384];
    public static final float[] COSINE = new float[16384];

    static {
        double angle = 3.834951969714103E-4D;
        for (int index = 0; index < 16384; index++) {
            SINE[index] = (float) Math.sin(index * angle);
            COSINE[index] = (float) Math.cos(index * angle);
        }
    }
    
    public OpenGlToolkit(Canvas canvas1, MaterialProvider materials, int i_3) {
        super(materials);
        this.lightNodes = new PointLight[initialLightCount];
        this.fogColor = -1;
        this.fogDensity = -1;
        this.anInt8378 = 0;
        this.densityFactor = 1.0F;
        this.fogMin = 0.0F;
        this.renderStillWater = false;
        this.textureCombineRgb = 8448;
        this.textureCombineAlpha = 8448;
        this.outlineCirclePointSize = -1.0f;
        this.fillCirclePointSize = -1.0f;
        this.copySrc = new ModelGL[7];
        this.copyDst = new ModelGL[7];
        this.textureBuffer = new GlBitBuffer(8192);
        this.projectedVerticesX = new int[1];
        this.projectedVerticesY = new int[1];
        this.anIntArray8502 = new int[1];
        this.shadowTextureData = new byte[16384];

        try {
            this.multiSamplesPreference = i_3;
            if (!AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jaclib")) {
            	System.out.println("Missing jaclib");
                throw new RuntimeException("");
            }
            if (!AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jaggl")) {
            	System.out.println("Missing jaggl");
                throw new RuntimeException("");
            }
            this.activeOpenglInstance = new OpenGL();
            long generatedUid = this.activeOpenglInstance.init(canvas1, 8, 8, 8, 24, 0, this.multiSamplesPreference);
            if (generatedUid == 0L) {
                throw new RuntimeException("");
            }
            attach();
            int i_6 = accumulateCapabilities();
            if (i_6 != 0) {
                throw new RuntimeException("");
            }
            this.pixelType = this.bigEndianess ? GL_UNSIGNED_INT_8_8_8_8_REV : GL_UNSIGNED_BYTE;
            if (this.gpuNameString.indexOf("radeon") != -1) {
                int gpuVersion = 0;
                boolean hasHdPrefix = false;
                boolean bool9 = false;
                String[] namePieces = Utils.split(this.gpuNameString.replace('/', ' '), ' ');
                for (int i_11 = 0; i_11 < namePieces.length; i_11++) {
                    String name = namePieces[i_11];
                    try {
                        if (!name.isEmpty()) {
                            if (name.charAt(0) == 120 && name.length() >= 3 && Utils.isWrongJava(name.substring(1, 3))) {
                                name = name.substring(1);
                                bool9 = true;
                            }
                            if ("hd".equals(name)) {
                                hasHdPrefix = true;
                            } else {
                                if (name.startsWith("hd")) {
                                    name = name.substring(2);
                                    hasHdPrefix = true;
                                }
                                if (name.length() >= 4 && Utils.isWrongJava(name.substring(0, 4))) {
                                    gpuVersion = Utils.parseInt(name.substring(0, 4));
                                    break;
                                }
                            }
                        }
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
                if (!bool9 && !hasHdPrefix) {
                    if (gpuVersion >= 7000 && gpuVersion <= 7999) {
                        this.vertexbufferSupported = false;
                    }
                    if (gpuVersion >= 7000 && gpuVersion <= 9250) {
                        this.texture3dSupported = false;
                    }
                }
                if (!hasHdPrefix || gpuVersion < 4000) {
                    this.arbTextureFloat = false;
                }
                this.arbRectangleTexturesSupported &= this.activeOpenglInstance.supportsExtension("GL_ARB_half_float_pixel");
                this.shouldUseArbVbo = this.vertexbufferSupported;
                this.radeonGpu = true;
            }
            if (this.gpuCompanyString.indexOf("intel") != -1) {
                this.intelGpu = false;
            }
            this.s3Graphics = !"s3 graphics".equals(this.gpuCompanyString);
            if (this.vertexbufferSupported) {
                try {
                    int[] handle = new int[1];
                    OpenGL.glGenBuffersARB(1, handle, 0);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    throw new RuntimeException("");
                }
            }
            ColorUtil.initializeTables(false, true);
            this.colortableReset = true;
            if (super.materialProvider == null) {
                throw new IllegalStateException("OpenGlToolkit requires non-null materialProvider");
            }
            this.textureCache = new OpenGLTextureCache(this, super.materialProvider);
            buildTables();
            this.aClass1648363 = new Texture3DShaderGL(this);
            this.aClass1708357 = new Class170(this);
            if (this.aClass1708357.method2896()) {
                this.rectangularTexture = new RectangularTextureGL(this);
                if (!this.rectangularTexture.rectangularTextureSupport()) {
                    this.rectangularTexture.destroy();
                    this.rectangularTexture = null;
                }
            }
            addImageProducer(canvas1, new ImageProducerGL2(this, canvas1, generatedUid));
            setActiveCanvas(canvas1);
            this.effectManager = new EffectManagerGL(this);
            prepareForRendering();
            renderLastFrame();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            cleanupAndShutdown();
            if (throwable instanceof OutOfMemoryError) {
                throw (OutOfMemoryError) throwable;
            } else {
                throw new RuntimeException("");
            }
        }
    }
    
    public static AbstractToolkit createToolkitHd(Canvas gameCanvas, MaterialProvider interface221, Js5ResourceProvider index2, int i_3) {
        try {
            boolean jaclibLoaded = AbstractToolkit.isJaclibLoaded();
            if (!jaclibLoaded) {
                throw new RuntimeException("");
            }
            if (!AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jaggl")) {
                throw new RuntimeException("");
            }
            AbstractToolkit.setIgnoreRepaint(gameCanvas);
            OpenGL opengl = new OpenGL();
            long createdUid = opengl.init(gameCanvas, 8, 8, 8, 24, 0, i_3);
            if (createdUid == 0L) {
                throw new RuntimeException("");
            }
            OpenGLNativeToolkit toolkit = new OpenGLNativeToolkit(opengl, gameCanvas, createdUid, interface221, index2, i_3);
            toolkit.initializeTables();
            return toolkit;
        } catch (Throwable throwable) {
        	throwable.printStackTrace();
            throw new RuntimeException("");
        }
    }
    
    public static AbstractToolkit createToolkitLd(Canvas canvas0, MaterialProvider interface221, int i_2) {
        return new OpenGlToolkit(canvas0, interface221, i_2);
    }

    public static int format(GLFormat pixelFormat) {
        switch (pixelFormat.formatType) {
            case 0:
                return GL_LUMINANCE;
            case 1:
                return GL_ALPHA;
            case 2:
            case 3:
            case 7:
            default:
                throw new IllegalStateException();
            case 4:
                return GL_LUMINANCE_ALPHA;
            case 5:
                return GL_RGB;
            case 6:
                return GL_RGBA;
            case 8:
                return GL_DEPTH_COMPONENT;
        }
    }

    public static int getInternalTextureFormat(GLFormat internalFormat, GLActionFormat actionFormat) {
        if (actionFormat == GLActionFormat.PIXEL_FORMAT) {
            switch (internalFormat.formatType) {
                case 0:
                    return GL_LUMINANCE;
                case 1:
                    return GL_ALPHA;
                case 2:
                case 3:
                default:
                    throw new IllegalArgumentException();
                case 4:
                    return GL_LUMINANCE_ALPHA;
                case 5:
                    return GL_RGB;
                case 6:
                    return GL_RGBA;
            }
        } else if (actionFormat == GLActionFormat.TEXTURE_FORMAT) {
            switch (internalFormat.formatType) {
                case 0:
                    return GL_LUMINANCE16;
                case 1:
                    return GL_ALPHA16;
                case 2:
                case 3:
                case 7:
                default:
                    throw new IllegalArgumentException();
                case 4:
                    return GL_LUMINANCE_ALPHA16UI_EXT;
                case 5:
                    return GL_RGB16;
                case 6:
                    return GL_RGBA16;
                case 8:
                    return GL_DEPTH_COMPONENT16;
            }
        } else if (actionFormat == GLActionFormat.DEPTH_COMPONENT_24) {
            switch (internalFormat.formatType) {
                case 8:
                    return GL_DEPTH_COMPONENT24;
                default:
                    throw new IllegalArgumentException();
            }
        } else if (actionFormat == GLActionFormat.FLOAT_16_ARB) {
            switch (internalFormat.formatType) {
                case 0:
                    return GL_LUMINANCE16F_ARB;
                case 1:
                    return GL_ALPHA16F_ARB;
                case 2:
                case 3:
                default:
                    throw new IllegalArgumentException();
                case 4:
                    return GL_LUMINANCE_ALPHA16F_ARB;
                case 5:
                    return GL_RGB16F_ARB;
                case 6:
                    return GL_RGBA16F_ARB;
            }
        } else if (actionFormat == GLActionFormat.FLOAT_32_ARB) {
            switch (internalFormat.formatType) {
                case 0:
                    return GL_LUMINANCE32F_ARB;
                case 1:
                    return GL_ALPHA32F_ARB;
                case 2:
                case 3:
                default:
                    throw new IllegalArgumentException();
                case 4:
                    return GL_LUMINANCE_ALPHA32F_ARB;
                case 5:
                    return GL_RGB32F_ARB;
                case 6:
                    return GL_RGBA32F_ARB;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void defineScissorBox() {
        if (super.display != null && this.clipLeft < this.clipRight && this.clipTop < this.clipBottom) {
            OpenGL.glScissor(this.anInt8453 + this.clipLeft, this.anInt8416 + super.display.getHeight() - this.clipBottom, this.clipRight - this.clipLeft, this.clipBottom - this.clipTop);
        } else {
            OpenGL.glScissor(0, 0, 0, 0);
        }

    }

    public void attach() {
        int attachAttempt = 0;
        while (!this.activeOpenglInstance.attach()) {
            if (attachAttempt++ > 5) {
                throw new RuntimeException("");
            }
            TimeTools.sleep(1000L);
        }
    }

    public void buildTables() {
        this.openglTextures = new OpenGlTexture[this.maxTextureUnits];
        this.texture2d = new OpenGl2DTexture(this, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, 1, 1);
        new OpenGl2DTexture(this, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, 1, 1);
        new OpenGl2DTexture(this, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, 1, 1);
        for (int index = 0; index < 7; index++) {
            this.copySrc[index] = new ModelGL(this);
            this.copyDst[index] = new ModelGL(this);
        }
        if (this.intelGpu) {
            this.integratedGraphicsImageProducer = new IntegratedGraphicsDisplayGL(this);
            new IntegratedGraphicsDisplayGL(this);
        }
    }

    public void prepareForRendering() {
        renderTexture(-2);
        for (int textureUnit = this.maxTextureUnits - 1; textureUnit >= 0; --textureUnit) {
            activateTexture(textureUnit);
            setActiveTexture(null);
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
        }
        loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        setTexture(2, GL_PREVIOUS, GL_SRC_ALPHA);
        renderStationaryTexture();
        this.activeBlendAlpha = 1;
        OpenGL.glEnable(GL_BLEND);
        OpenGL.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        this.activeBlendType = 1;
        OpenGL.glEnable(GL_ALPHA_TEST);
        OpenGL.glAlphaFunc(GL_GREATER, 0.0F);
        this.alphaTestEnabled = true;
        OpenGL.glColorMask(true, true, true, true);
        this.colormaskActive = true;
        toggleFog(true);
        setLightingEnabled(true);
        toggleZbuffer(true);
        setDepthMask(true);
        resetDepthClippingMask();
        method13586();
        this.activeOpenglInstance.setSwapInterval(0);
        OpenGL.glShadeModel(GL_SMOOTH);
        OpenGL.glClearDepth(1.0F);
        OpenGL.glDepthFunc(515);
        OpenGL.glPolygonMode(GL_FRONT, GL_FILL);
        OpenGL.glEnable(GL_CULL_FACE);
        OpenGL.glCullFace(GL_BACK);
        OpenGL.glMatrixMode(GL_MODELVIEW);
        OpenGL.glLoadIdentity();
        OpenGL.glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        OpenGL.glEnable(GL_COLOR_MATERIAL);
        float[] defaultAmbient = {0.0F, 0.0F, 0.0F, 1.0F};
        for (int lightHandleOffset = 0; lightHandleOffset < 8; lightHandleOffset++) {
            int lightHandle = lightHandleOffset + GL_LIGHT0;
            OpenGL.glLightfv(lightHandle, GL_AMBIENT, defaultAmbient, 0);
            OpenGL.glLightf(lightHandle, GL_CONSTANT_ATTENUATION, 0.0F);
            OpenGL.glLightf(lightHandle, GL_LINEAR_ATTENUATION, 0.0F);
        }
        OpenGL.glEnable(GL_LIGHT0);
        OpenGL.glEnable(GL_LIGHT1);
        OpenGL.glFogf(GL_FOG_DENSITY, 0.95F);
        OpenGL.glFogi(GL_FOG_MODE, GL_LINEAR);
        OpenGL.glHint(GL_FOG_HINT, GL_FASTEST);
        this.fogColor = -1;
        this.sunColorRgb = -1;
        calculateViewportCenter();
        resetClipping();
    }

    @Override
    public void render() {
        try {
            super.delegatingDisplay.draw();
        } catch (Exception exception) {
        	exception.printStackTrace();
        }
        if (super.materialProvider != null) {
            super.materialProvider.cycle();
        }
    }

    @Override
    public void awaitRenderCompletion() {
        OpenGL.glFinish();
    }

    @Override
    public void shutdown() {
        for (Node node1 = this.heapManager.getTail(); node1 != null; node1 = this.heapManager.getPrevious()) {
            ((NativeMemoryGL) node1).deallocate();
        }
        if (this.aClass1708357 != null) {
            this.aClass1708357.shutdown();
        }
        if (this.colortableReset) {
            ColorUtil.resetTables(false, true);
            this.colortableReset = false;
        }
    }

    @Override
    public boolean supportsFullscreen() {
        return true;
    }

    @Override
    public boolean forceFixedScreenmode() {
        return true;
    }

    @Override
    public boolean hasBloomSupport() {
        return this.rectangularTexture != null && (this.multiSamplesPreference <= 1 || this.advancedFramebuffers);
    }

    @Override
    public boolean method8404() {
        return true;
    }

    public void pushMatrix() {
        OpenGL.glPushMatrix();
    }



    @Override
    public boolean extendBuildArea() {
        return true;
    }

    public void method13578() {
        method13639();
    }

    @Override
    public int[] copyPixelRectangle(int x, int y, int width, int height) {
        if (super.display == null) {
            return null;
        } 
        int[] pixels = new int[width * height];
        int producerHeight = super.display.getHeight();
        for (int h = 0; h < height; h++) {
            OpenGL.glReadPixelsi(x, producerHeight - y - h - 1, width, 1, 32993, this.pixelType, pixels, width * h);
        }
        return pixels;
    }

    @Override
    public void renderLastFrame() {
        if (this.s3Graphics && super.display != null) {
            int left = this.clipLeft;
            int right = this.clipRight;
            int top = this.clipTop;
            int bottom = this.clipBottom;
            resetClipping();
            int i_5 = this.anInt8417;
            int i_6 = this.anInt8418;
            int i_7 = this.imageProducerWidth;
            int i_8 = this.imageProducerHeight;
            calculateViewportCenter();
            OpenGL.glReadBuffer(GL_FRONT);
            OpenGL.glDrawBuffer(GL_BACK);
            method13586();
            toggleFog(false);
            setLightingEnabled(false);
            toggleZbuffer(false);
            setDepthMask(false);
            setActiveTexture(null);
            renderTexture(-2);
            loadTextureCombineMode(1);
            setBlendMode(0);
            OpenGL.glMatrixMode(GL_PROJECTION);
            OpenGL.glLoadIdentity();
            OpenGL.glOrtho(0.0D, 1.0D, 0.0D, 1.0D, -1.0, 1.0D);
            OpenGL.glMatrixMode(GL_MODELVIEW);
            OpenGL.glLoadIdentity();
            OpenGL.glRasterPos2i(0, 0);
            OpenGL.glCopyPixels(0, 0, super.display.getWidth(), super.display.getHeight(), GL_COLOR);
            OpenGL.glFlush();
            OpenGL.glReadBuffer(GL_BACK);
            OpenGL.glDrawBuffer(GL_BACK);
            setClipping(left, top, right, bottom);
            setDrawableArea(i_5, i_6, i_7, i_8);
        }
    }

    @Override
    public void applyClearFlags(int functionMask, int colorArgb) {
        int bufferClearMask = 0;
        if ((functionMask & 0x1) != 0) {
            OpenGL.glClearColor((colorArgb & 0xff0000) / 1.671168E7F, (colorArgb & 0xff00) / 65280.0F, (colorArgb & 0xff) / 255.0F, (colorArgb >>> 24) / 255.0F);
            bufferClearMask = 16384;
        }

        if ((functionMask & 0x2) != 0) {
            setDepthMask(true);
            bufferClearMask |= 0x100;
        }

        if ((functionMask & 0x4) != 0) {
            bufferClearMask |= 0x400;
        }

        OpenGL.glClear(bufferClearMask);
    }

    /**
     * Example: https://youtu.be/E0hXye2B08s?t=216
     */
    @Override
    public void drawRectangleOutline(int x, int y, int width, int height, int colorArgb, int blendMode) {
        float leftX = x + 0.35F;
        float topY = y + 0.35F;
        float rightX = leftX + width - 1.0F;
        float bottomY = topY + height - 1.0F;
        setupShapeRendering();
        setBlendMode(blendMode);
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        if (this.arbMultisample) {
            OpenGL.glDisable(GL_MULTISAMPLE);
        }
        OpenGL.glBegin(GL_LINE_LOOP);
        OpenGL.glVertex2f(leftX, topY);
        OpenGL.glVertex2f(leftX, bottomY);
        OpenGL.glVertex2f(rightX, bottomY);
        OpenGL.glVertex2f(rightX, topY);
        OpenGL.glEnd();
        if (this.arbMultisample) {
            OpenGL.glEnable(GL_MULTISAMPLE);
        }

    }

    @Override
    public void drawFilledRectangle(int x, int y, int width, int height, int colorArgb, int blendMode) {
        float leftX = x + 0.35F;
        float topY = y + 0.35F;
        float rightX = leftX + width;
        float bottomY = topY + height;
        setupShapeRendering();
        setBlendMode(blendMode);
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        if (this.arbMultisample) {
            OpenGL.glDisable(GL_MULTISAMPLE);
        }

        OpenGL.glBegin(GL_QUADS);
        OpenGL.glVertex2f(leftX, topY);
        OpenGL.glVertex2f(leftX, bottomY);
        OpenGL.glVertex2f(rightX, bottomY);
        OpenGL.glVertex2f(rightX, topY);
        OpenGL.glEnd();
        if (this.arbMultisample) {
            OpenGL.glEnable(GL_MULTISAMPLE);
        }

    }

    public void setDepthMask() {
        OpenGL.glDepthMask(this.enableDepthMask && this.depthBufferingEnabled);
    }

    @Override
    public void benchmark() {
        setupShapeRendering();
        setBlendMode(1);
        OpenGL.glBegin(4);
        OpenGL.glColor4ub((byte) (-65536 >> 16), (byte) (-65536 >> 8), (byte) -65536, (byte) (-65536 >> 24));
        OpenGL.glVertex3f(5.35f, 10.35f, (float) 100.0);
        OpenGL.glColor4ub((byte) (-65536 >> 16), (byte) (-65536 >> 8), (byte) -65536, (byte) (-65536 >> 24));
        OpenGL.glVertex3f(75.35f, 50.35f, (float) 100.0);
        OpenGL.glColor4ub((byte) (-65536 >> 16), (byte) (-65536 >> 8), (byte) -65536, (byte) (-65536 >> 24));
        OpenGL.glVertex3f(15.35f, 90.35f, (float) 100.0);
        OpenGL.glEnd();
    }

    @Override
    public void setDepthBuffering(boolean zbuffer) {
        this.depthBufferingEnabled = zbuffer;
        setDepthMask();
    }

    public void renderTexture(int materialId) {
        renderTexture(materialId, true);
    }

    @Override
    public void drawLine(int startX, int startY, int endX, int endY, int colorArgb, int lineSpacing, int lineLength, int offset) {
        if (endX != startX || startY != endY) {
            setupShapeRendering();
            setBlendMode(1);
            float deltaX = (float) endX - startX;
            float deltaY = (float) endY - startY;
            float f_12 = (float) (1.0D / Math.sqrt(deltaX * deltaX + deltaY * deltaY));
            deltaX *= f_12;
            deltaY *= f_12;
            OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
            offset %= lineLength + lineSpacing;
            float dx1 = deltaX * lineSpacing;
            float dy1 = deltaY * lineSpacing;
            float dx2 = 0.0F;
            float dy2 = 0.0F;
            float dx3 = dx1;
            float dy3 = dy1;
            if (offset > lineSpacing) {
                dx2 = deltaX * (lineLength + lineSpacing - offset);
                dy2 = deltaY * (lineLength + lineSpacing - offset);
            } else {
                dx3 = deltaX * (lineSpacing - offset);
                dy3 = deltaY * (lineSpacing - offset);
            }

            float x = startX + 0.35F + dx2;
            float y = startY + 0.35F + dy2;
            float dx4 = deltaX * lineLength;
            float dy4 = deltaY * lineLength;

            while (true) {
                if (endX > startX) {
                    if (x > endX + 0.35F) {
                        break;
                    }

                    if (x + dx3 > endX) {
                        dx3 = endX - x;
                    }
                } else {
                    if (x < endX + 0.35F) {
                        break;
                    }

                    if (x + dx3 < endX) {
                        dx3 = endX - x;
                    }
                }

                if (endY > startY) {
                    if (y > endY + 0.35F) {
                        break;
                    }

                    if (y + dy3 > endY) {
                        dy3 = endY - y;
                    }
                } else {
                    if (y < endY + 0.35F) {
                        break;
                    }

                    if (y + dy3 < endY) {
                        dy3 = endY - y;
                    }
                }

                OpenGL.glBegin(GL_LINES);
                OpenGL.glVertex2f(x, y);
                OpenGL.glVertex2f(x + dx3, y + dy3);
                OpenGL.glEnd();
                x += dx4 + dx3;
                y += dy4 + dy3;
                dx3 = dx1;
                dy3 = dy1;
            }
        }

    }

    @Override
    public void method8669(int leftX, int topY, int rightX, int bottomY, int colorArgb, ComponentSurface dimension, int clipLeft, int clipTop) {
        SpriteSurfaceGL class455Sub210 = (SpriteSurfaceGL) dimension;
        OpenGl2DTexture_Sub1 class137Sub1Sub111 = class455Sub210.surface;
        setupSpriteRendering();
        setActiveTexture(class455Sub210.surface);
        setBlendMode(1);
        loadTextureCombineMode(GL_REPLACE, GL_MODULATE);
        setTexture(0, GL_PRIMARY_COLOR, GL_SRC_COLOR);
        float f_12 = class137Sub1Sub111.s / class137Sub1Sub111.singleWidth;
        float f_13 = class137Sub1Sub111.t / class137Sub1Sub111.singleLength;
        float f_14 = (float) rightX - leftX;
        float f_15 = (float) bottomY - topY;
        float f_16 = (float) (1.0D / Math.sqrt(f_14 * f_14 + f_15 * f_15));
        f_14 *= f_16;
        f_15 *= f_16;
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        OpenGL.glBegin(GL_LINES);
        OpenGL.glTexCoord2f(f_12 * (leftX - clipLeft), f_13 * (topY - clipTop));
        OpenGL.glVertex2f(leftX + 0.35F, topY + 0.35F);
        OpenGL.glTexCoord2f(f_12 * (rightX - clipLeft), f_13 * (bottomY - clipTop));
        OpenGL.glVertex2f(rightX + f_14 + 0.35F, bottomY + f_15 + 0.35F);
        OpenGL.glEnd();
        setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
    }

    @Override
    public void drawTexturedLine(int i_1, int i_2, int i_3, int i_4, int colorArgb, ComponentSurface class4557, int clipLeft, int clipTop, int i_10, int i_11, int i_12) {
    	if (i_3 != i_1 || i_2 != i_4) {
            SpriteSurfaceGL class455Sub213 = (SpriteSurfaceGL) class4557;
            OpenGl2DTexture_Sub1 class137Sub1Sub114 = class455Sub213.surface;
            setupSpriteRendering();
            setActiveTexture(class455Sub213.surface);
            setBlendMode(1);
            loadTextureCombineMode(7681, 8448);
            setTexture(0, 34167, 768);
            float f_15 = class137Sub1Sub114.s / class137Sub1Sub114.singleWidth;
            float f_16 = class137Sub1Sub114.t / class137Sub1Sub114.singleLength;
            float f_17 = (float) i_3 - i_1;
            float f_18 = (float) i_4 - i_2;
            float f_19 = (float) (1.0D / Math.sqrt(f_17 * f_17 + f_18 * f_18));
            f_17 *= f_19;
            f_18 *= f_19;
            OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
            i_12 %= i_11 + i_10;
            float f_20 = f_17 * i_10;
            float f_21 = f_18 * i_10;
            float f_22 = 0.0F;
            float f_23 = 0.0F;
            float f_24 = f_20;
            float f_25 = f_21;
            if (i_12 > i_10) {
                f_22 = f_17 * (i_11 + i_10 - i_12);
                f_23 = f_18 * (i_11 + i_10 - i_12);
            } else {
                f_24 = f_17 * (i_10 - i_12);
                f_25 = f_18 * (i_10 - i_12);
            }

            float f_26 = i_1 + 0.35F + f_22;
            float f_27 = i_2 + 0.35F + f_23;
            float f_28 = f_17 * i_11;
            float f_29 = f_18 * i_11;

            while (true) {
                if (i_3 > i_1) {
                    if (f_26 > i_3 + 0.35F) {
                        break;
                    }

                    if (f_26 + f_24 > i_3) {
                        f_24 = i_3 - f_26;
                    }
                } else {
                    if (f_26 < i_3 + 0.35F) {
                        break;
                    }

                    if (f_26 + f_24 < i_3) {
                        f_24 = i_3 - f_26;
                    }
                }

                if (i_4 > i_2) {
                    if (f_27 > i_4 + 0.35F) {
                        break;
                    }

                    if (f_27 + f_25 > i_4) {
                        f_25 = i_4 - f_27;
                    }
                } else {
                    if (f_27 < i_4 + 0.35F) {
                        break;
                    }

                    if (f_27 + f_25 < i_4) {
                        f_25 = i_4 - f_27;
                    }
                }

                OpenGL.glBegin(GL_LINES);
                OpenGL.glTexCoord2f(f_15 * (f_26 - clipLeft), f_16 * (f_27 - clipTop));
                OpenGL.glVertex2f(f_26, f_27);
                OpenGL.glTexCoord2f(f_15 * (f_26 + f_24 - clipLeft), f_16 * (f_27 + f_25 - clipTop));
                OpenGL.glVertex2f(f_26 + f_24, f_27 + f_25);
                OpenGL.glEnd();
                f_26 += f_28 + f_24;
                f_27 += f_29 + f_25;
                f_24 = f_20;
                f_25 = f_21;
            }

            setTexture(0, 5890, 768);
        }
    }

    @Override
    public void drawThickLine(int leftX, int topY, int rightX, int bottomY, int colorArgb, int lineWidth, int blendMode) {
        OpenGL.glLineWidth(lineWidth);
        drawTransparentLine(leftX, topY, rightX, bottomY, colorArgb, blendMode);
        OpenGL.glLineWidth(1.0F);
    }

    @Override
    public void calculateProjection(float f_1, float f_2, float f_3, float[] floats_4) {
        float f_5 = this.aClass3848394.getFaceList()[15] + this.aClass3848394.getFaceList()[3] * f_1 + this.aClass3848394.getFaceList()[7] * f_2 + this.aClass3848394.getFaceList()[11] * f_3;
        float f_6 = this.aClass3848394.getFaceList()[12] + this.aClass3848394.getFaceList()[0] * f_1 + this.aClass3848394.getFaceList()[4] * f_2 + this.aClass3848394.getFaceList()[8] * f_3;
        float f_7 = this.aClass3848394.getFaceList()[13] + this.aClass3848394.getFaceList()[1] * f_1 + this.aClass3848394.getFaceList()[5] * f_2 + this.aClass3848394.getFaceList()[9] * f_3;
        float f_8 = this.aClass3848348.getFaceList()[14] + this.aClass3848348.getFaceList()[2] * f_1 + this.aClass3848348.getFaceList()[6] * f_2 + this.aClass3848348.getFaceList()[10] * f_3;
        floats_4[0] = this.aFloat8315 + this.imageProducerCenterX * f_6 / f_5;
        floats_4[1] = this.aFloat8399 + this.imageProducerCenterY * f_7 / f_5;
        floats_4[2] = f_8;
    }

    @Override
    public void computeWorldToScreen(float f_1, float f_2, float f_3, float[] floats_4) {
        float f_5 = this.aClass3848394.getFaceList()[14] + this.aClass3848394.getFaceList()[2] * f_1 + this.aClass3848394.getFaceList()[6] * f_2 + this.aClass3848394.getFaceList()[10] * f_3;
        float f_6 = this.aClass3848394.getFaceList()[15] + this.aClass3848394.getFaceList()[3] * f_1 + this.aClass3848394.getFaceList()[7] * f_2 + this.aClass3848394.getFaceList()[11] * f_3;
        if (f_5 >= -f_6 && f_5 <= f_6) {
            float f_7 = this.aClass3848394.getFaceList()[12] + this.aClass3848394.getFaceList()[0] * f_1 + this.aClass3848394.getFaceList()[4] * f_2 + this.aClass3848394.getFaceList()[8] * f_3;
            if (f_7 >= -f_6 && f_7 <= f_6) {
                float f_8 = this.aClass3848394.getFaceList()[13] + this.aClass3848394.getFaceList()[1] * f_1 + this.aClass3848394.getFaceList()[5] * f_2 + this.aClass3848394.getFaceList()[9] * f_3;
                if (f_8 >= -f_6 && f_8 <= f_6) {
                    float f_9 = this.aClass3848348.getFaceList()[14] + this.aClass3848348.getFaceList()[2] * f_1 + this.aClass3848348.getFaceList()[6] * f_2 + this.aClass3848348.getFaceList()[10] * f_3;
                    floats_4[0] = this.aFloat8315 + this.imageProducerCenterX * f_7 / f_6;
                    floats_4[1] = this.aFloat8399 + this.imageProducerCenterY * f_8 / f_6;
                    floats_4[2] = f_9;
                } else {
                    floats_4[2] = Float.NaN;
                    floats_4[1] = Float.NaN;
                    floats_4[0] = Float.NaN;
                }
            } else {
                floats_4[2] = Float.NaN;
                floats_4[1] = Float.NaN;
                floats_4[0] = Float.NaN;
            }
        } else {
            floats_4[2] = Float.NaN;
            floats_4[1] = Float.NaN;
            floats_4[0] = Float.NaN;
        }

    }

    @Override
    public void drawTransparentLine(int leftX, int topY, int rightX, int bottomY, int colorArgb, int blendMode) {
        setupShapeRendering();
        setBlendMode(blendMode);
        float deltaX = (float) rightX - leftX;
        float deltaY = (float) bottomY - topY;
        if (deltaX == 0.0F && deltaY == 0.0F) {
            deltaX = 1.0F;
        } else {
            float length = (float) (1.0D / Math.sqrt(deltaX * deltaX + deltaY * deltaY));
            deltaX *= length;
            deltaY *= length;
        }

        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        OpenGL.glBegin(GL_LINES);
        OpenGL.glVertex2f(leftX + 0.35F, topY + 0.35F);
        OpenGL.glVertex2f(rightX + deltaX + 0.35F, bottomY + deltaY + 0.35F);
        OpenGL.glEnd();
    }

    @Override
    public NativeMemoryNode getNativeMemory(int size) {
        NativeMemoryGL nativeMemory = new NativeMemoryGL(size);
        this.heapManager.insertTail(nativeMemory);
        return nativeMemory;
    }

    @Override
    public void setNativeHeap(NativeMemoryNode class282Sub11) {
        this.nativeHeap = ((NativeMemoryGL) class282Sub11).nativeHeap;
        if (this.anInterface148496 == null) {
            GlBitBuffer textureBuffer = new GlBitBuffer(80);
            if (this.bigEndianess) {
                textureBuffer.writeFloatBigEndian(-1.0f);
                textureBuffer.writeFloatBigEndian(-1.0f);
                textureBuffer.writeFloatBigEndian(0.0F);
                textureBuffer.writeFloatBigEndian(0.0F);
                textureBuffer.writeFloatBigEndian(0.0F);
                textureBuffer.writeFloatBigEndian(-1.0f);
                textureBuffer.writeFloatBigEndian(1.0F);
                textureBuffer.writeFloatBigEndian(0.0F);
                textureBuffer.writeFloatBigEndian(0.0F);
                textureBuffer.writeFloatBigEndian(1.0F);
                textureBuffer.writeFloatBigEndian(1.0F);
                textureBuffer.writeFloatBigEndian(1.0F);
                textureBuffer.writeFloatBigEndian(0.0F);
                textureBuffer.writeFloatBigEndian(1.0F);
                textureBuffer.writeFloatBigEndian(1.0F);
                textureBuffer.writeFloatBigEndian(1.0F);
                textureBuffer.writeFloatBigEndian(-1.0f);
                textureBuffer.writeFloatBigEndian(0.0F);
                textureBuffer.writeFloatBigEndian(1.0F);
                textureBuffer.writeFloatBigEndian(0.0F);
            } else {
                textureBuffer.writeFloatLittleEndian(-1.0f);
                textureBuffer.writeFloatLittleEndian(-1.0f);
                textureBuffer.writeFloatLittleEndian(0.0F);
                textureBuffer.writeFloatLittleEndian(0.0F);
                textureBuffer.writeFloatLittleEndian(0.0F);
                textureBuffer.writeFloatLittleEndian(-1.0f);
                textureBuffer.writeFloatLittleEndian(1.0F);
                textureBuffer.writeFloatLittleEndian(0.0F);
                textureBuffer.writeFloatLittleEndian(0.0F);
                textureBuffer.writeFloatLittleEndian(1.0F);
                textureBuffer.writeFloatLittleEndian(1.0F);
                textureBuffer.writeFloatLittleEndian(1.0F);
                textureBuffer.writeFloatLittleEndian(0.0F);
                textureBuffer.writeFloatLittleEndian(1.0F);
                textureBuffer.writeFloatLittleEndian(1.0F);
                textureBuffer.writeFloatLittleEndian(1.0F);
                textureBuffer.writeFloatLittleEndian(-1.0f);
                textureBuffer.writeFloatLittleEndian(0.0F);
                textureBuffer.writeFloatLittleEndian(1.0F);
                textureBuffer.writeFloatLittleEndian(0.0F);
            }

            this.anInterface148496 = createArrayBuffer(20, textureBuffer.buffer, textureBuffer.currentPosition, false);
            this.aClass1438494 = new OpenGlArrayDrawBuffer(this.anInterface148496, GL_FLOAT, 3, 0);
            this.aClass1438495 = new OpenGlArrayDrawBuffer(this.anInterface148496, GL_FLOAT, 2, 12);
            this.particleRenderer.initialize(this);
        }
    }

    @Override
    public NativeSprite generateEmptySprite(int i_1, int i_2, boolean bool3, boolean bool4) {
        return new SpriteGL(this, i_1, i_2, bool3);
    }

    @Override
    public NativeSprite generateSprite(SpriteType spriteType, boolean bool2) {
        int[] pixels = new int[spriteType.width * spriteType.height];
        int src = 0;
        int dst = 0;
        if (spriteType.alpha != null) {
            for (int x = 0; x < spriteType.height; x++) {
                for (int y = 0; y < spriteType.width; y++) {
                    pixels[dst++] = spriteType.alpha[src] << 24 | spriteType.pallete[spriteType.pixels[src++] & 0xff];
                }
            }
        } else {
            for (int x = 0; x < spriteType.height; x++) {
                for (int y = 0; y < spriteType.width; y++) {
                    int pixel = spriteType.pallete[spriteType.pixels[src++] & 0xff];
                    pixels[dst++] = pixel != 0 ? ~0xffffff | pixel : 0;
                }
            }
        }
        NativeSprite nativeSprite = createNativeSprite(pixels, spriteType.width, spriteType.width, spriteType.height);
        nativeSprite.setSize(spriteType.minX, spriteType.minY, spriteType.borderX, spriteType.borderY);
        return nativeSprite;
    }

    @Override
    public NativeSprite createNativeSprite(int[] ints_1, int i_2, int i_3, int i_4, int i_5, boolean bool6) {
        return new SpriteGL(this, i_4, i_5, ints_1, i_2, i_3);
    }

    @Override
    public NativeSprite createNativeSprite(int i_1, int i_2, int i_3, int i_4, boolean bool5) {
        return new SpriteGL(this, i_1, i_2, i_3, i_4);
    }

    @Override
    public ComponentSurface createComponentSurface(int sizeX, int sizeY, int[] rowStartX, int[] rowWidth) {
        return SpriteSurfaceGL.create(this, sizeX, sizeY, rowStartX, rowWidth);
    }

    @Override
    public void clearRectangle(ComponentSurface class4552, int i_3, int i_4) {
        SpriteSurfaceGL class455Sub25 = (SpriteSurfaceGL) class4552;
        OpenGl2DTexture_Sub1 class137Sub1Sub16 = class455Sub25.surface;
        setupSpriteRendering();
        setActiveTexture(class455Sub25.surface);
        setBlendMode(1);
        loadTextureCombineMode(GL_REPLACE, GL_MODULATE);
        setTexture(0, 34167, 768);
        float f_7 = class137Sub1Sub16.s / class137Sub1Sub16.singleWidth;
        float f_8 = class137Sub1Sub16.t / class137Sub1Sub16.singleLength;
        OpenGL.glColor4ub((byte) (~0xffffff >> 16), (byte) (~0xffffff >> 8), (byte) ~0xffffff, (byte) (~0xffffff >> 24));
        OpenGL.glBegin(GL_QUADS);
        OpenGL.glTexCoord2f(f_7 * (this.clipLeft - i_3), f_8 * (this.clipTop - i_4));
        OpenGL.glVertex2i(this.clipLeft, this.clipTop);
        OpenGL.glTexCoord2f(f_7 * (this.clipLeft - i_3), f_8 * (this.clipBottom - i_4));
        OpenGL.glVertex2i(this.clipLeft, this.clipBottom);
        OpenGL.glTexCoord2f(f_7 * (this.clipRight - i_3), f_8 * (this.clipBottom - i_4));
        OpenGL.glVertex2i(this.clipRight, this.clipBottom);
        OpenGL.glTexCoord2f(f_7 * (this.clipRight - i_3), f_8 * (this.clipTop - i_4));
        OpenGL.glVertex2i(this.clipRight, this.clipTop);
        OpenGL.glEnd();
        setTexture(0, 5890, 768);
    }

    @Override
    public void initialize() {
    }

    @Override
    public Model createModel(Mesh rsmesh1, int i_2, int i_3, int i_4, int i_5) {
        return new ModelGL(this, rsmesh1, i_2, i_4, i_5, i_3);
    }

    @Override
    public int mismatchingFunctionMask(int i_1, int i_2) {
        return i_1 & i_2 ^ i_2;
    }

    @Override
    public void calculateFog(int color, int density, int i_3) {
        if (this.fogColor != color || density != this.fogDensity || i_3 != this.anInt8378) {
            this.fogColor = color;
            this.fogDensity = density;
            this.anInt8378 = i_3;
            setFogRange();
            toggleFog();
        }
    }

    @Override
    public Matrix44 method8449() {
        return this.aClass3848360;
    }

    @Override
    public Matrix43 getRenderingMatrix() {
        return this.aClass2948476;
    }

    @Override
    public void renderParticles(ParticleWrapper particleWrapper) {
        this.particleRenderer.drawParticles(this, particleWrapper);
    }

    @Override
    public void addWater(WaterTile waterTile) {
    	this.waterDepth = -1;
    	this.waterTile = waterTile;
    	this.stillWaterToggled = true;
    }

    @Override
    public void addWaterTile(int waterDepth, WaterTile waterTile) {
        if (!this.stillWaterToggled) {
            throw new RuntimeException("");
        } 
        this.waterDepth = waterDepth;
        this.waterTile = waterTile;
        if (this.renderStillWater) {
        	this.effectManager.waterEffect.enable();
        	this.effectManager.waterEffect.activate();
        }
    }

    @Override
    public RenderBuffer getRenderBuffer(int i_1, int i_2) {
        return new OpenGlRenderBuffer(this, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, i_1, i_2);
    }

    @Override
    public void calculateViewportCenter() {
        if (super.display == null) {
            return;
        }
        this.anInt8417 = 0;
        this.anInt8418 = 0;
        this.imageProducerWidth = super.display.getWidth();
        this.imageProducerHeight = super.display.getHeight();
        method13584();
    }

    @Override
    public void setDrawableArea(int i_1, int i_2, int i_3, int i_4) {
        this.anInt8417 = i_1;
        this.anInt8418 = i_2;
        this.imageProducerWidth = i_3;
        this.imageProducerHeight = i_4;
        method13584();
    }

    @Override
    public void getClipping(int[] clipping) {
        clipping[0] = this.clipLeft;
        clipping[1] = this.clipTop;
        clipping[2] = this.clipRight;
        clipping[3] = this.clipBottom;
    }

    @Override
    public void resetClipping() {
        if (super.display != null) {
            this.clipLeft = 0;
            this.clipTop = 0;
            this.clipRight = super.display.getWidth();
            this.clipBottom = super.display.getHeight();
            OpenGL.glDisable(GL_SCISSOR_TEST);
        }
    }

    @Override
    public void setClipping(int left, int top, int right, int bottom) {
        if (super.display != null) {
            if (left < 0) {
                left = 0;
            }
            if (right > super.display.getWidth()) {
                right = super.display.getWidth();
            }
            if (top < 0) {
                top = 0;
            }
            if (bottom > super.display.getHeight()) {
                bottom = super.display.getHeight();
            }
            this.clipLeft = left;
            this.clipTop = top;
            this.clipRight = right;
            this.clipBottom = bottom;
            OpenGL.glEnable(GL_SCISSOR_TEST);
            defineScissorBox();
        }
    }

    @Override
    public void expandClipping(int left, int top, int right, int bottom) {
        if (this.clipLeft < left) {
            this.clipLeft = left;
        }
        if (this.clipRight > right) {
            this.clipRight = right;
        }
        if (this.clipTop < top) {
            this.clipTop = top;
        }
        if (this.clipBottom > bottom) {
            this.clipBottom = bottom;
        }
        OpenGL.glEnable(GL_SCISSOR_TEST);
        defineScissorBox();
    }

    @Override
    public void onTilesDrawn() {
        this.stillWaterToggled = false;
    }

    public void method13584() {
        if (super.display != null) {
            int i_1;
            int i_2;
            int i_3;
            int i_4;
            if (this.depthClippingMode == 2) {
                i_1 = this.anInt8417;
                i_2 = this.anInt8418;
                i_3 = this.imageProducerWidth;
                i_4 = this.imageProducerHeight;
            } else {
                i_1 = 0;
                i_2 = 0;
                i_3 = super.display.getWidth();
                i_4 = super.display.getHeight();
            }

            if (i_3 < 1) {
                i_3 = 1;
            }

            if (i_4 < 1) {
                i_4 = 1;
            }

            OpenGL.glViewport(i_1 + this.anInt8453, this.anInt8416 + super.display.getHeight() - i_2 - i_4, i_3, i_4);
            this.imageProducerCenterX = this.imageProducerWidth / 2.0F;
            this.imageProducerCenterY = this.imageProducerHeight / 2.0F;
            this.aFloat8315 = this.anInt8417 + this.imageProducerCenterX;
            this.aFloat8399 = this.anInt8418 + this.imageProducerCenterY;
        }

    }


    @Override
    public synchronized void animateTextures(int i_1) {
        int handleCount = 0;
        i_1 &= 0x7fffffff;
        while (!this.vboBuffersAwaitingDeallocation.isEmpty()) {
        	IntNode handle = (IntNode) this.vboBuffersAwaitingDeallocation.popTail();
            HANDLE_BUFFER[handleCount++] = (int) handle.pointer;
            this.vboBuffersSize -= handle.value;
            if (handleCount == 1000) {
                OpenGL.glDeleteBuffersARB(1000, HANDLE_BUFFER, 0);
                handleCount = 0;
            }
        }
        if (handleCount > 0) {
            OpenGL.glDeleteBuffersARB(handleCount, HANDLE_BUFFER, 0);
            handleCount = 0;
        }
        while (!this.texturesAwaitingDeallocation.isEmpty()) {
        	IntNode handle = (IntNode) this.texturesAwaitingDeallocation.popTail();
            HANDLE_BUFFER[handleCount++] = (int) handle.pointer;
            this.textureBuffersSize -= handle.value;
            if (handleCount == 1000) {
                OpenGL.glDeleteTextures(1000, HANDLE_BUFFER, 0);
                handleCount = 0;
            }
        }
        if (handleCount > 0) {
            OpenGL.glDeleteTextures(handleCount, HANDLE_BUFFER, 0);
            handleCount = 0;
        }
        while (!this.frameBuffersAwaitingDeallocation.isEmpty()) {
        	IntNode handle = (IntNode) this.frameBuffersAwaitingDeallocation.popTail();
            HANDLE_BUFFER[handleCount++] = handle.value;
            if (handleCount == 1000) {
                OpenGL.glDeleteFramebuffersEXT(1000, HANDLE_BUFFER, 0);
                handleCount = 0;
            }
        }
        if (handleCount > 0) {
            OpenGL.glDeleteFramebuffersEXT(handleCount, HANDLE_BUFFER, 0);
            handleCount = 0;
        }
        while (!this.renderBuffersAwaitingRemoval.isEmpty()) {
        	IntNode class282Sub384 = (IntNode) this.renderBuffersAwaitingRemoval.popTail();
            HANDLE_BUFFER[handleCount++] = (int) class282Sub384.pointer;
            this.renderBuffersSize -= class282Sub384.value;
            if (handleCount == 1000) {
                OpenGL.glDeleteRenderbuffersEXT(1000, HANDLE_BUFFER, 0);
                handleCount = 0;
            }
        }
        if (handleCount > 0) {
            OpenGL.glDeleteRenderbuffersEXT(handleCount, HANDLE_BUFFER, 0);
        }
        while (!this.listsAwaitingDeallocation.isEmpty()) {
        	IntNode handle = (IntNode) this.listsAwaitingDeallocation.popTail();
            OpenGL.glDeleteLists((int) handle.pointer, handle.value);
        }
        while (!this.arbProgramsAwaitingDeallocation.isEmpty()) {
        	Node<?> handle = this.arbProgramsAwaitingDeallocation.popTail();
            OpenGL.glDeleteProgramARB((int) handle.pointer);
        }
        while (!this.shaderProgramsAwaitingDeallocation.isEmpty()) {
        	Node<?> handle = this.shaderProgramsAwaitingDeallocation.popTail();
            OpenGL.glDeleteShader((int) handle.pointer);
        }
        while (!this.listsAwaitingDeallocation.isEmpty()) {
        	IntNode handle = (IntNode) this.listsAwaitingDeallocation.popTail();
            OpenGL.glDeleteLists((int) handle.pointer, handle.value);
        }
        this.textureCache.cycleBloomCache();
        if (getOffheap() > 100663296 && Utils.time() > this.lastGcCycle + 60000L) {
            System.gc();
            this.lastGcCycle = Utils.time();
        }
        this.textureAnimatingProgression = i_1;
    }

    @Override
    public void drawCircle(int x, int y, int radius, int colorArgb) {
        if (radius < 0) {
            radius = -radius;
        }

        if (radius + x >= this.clipLeft && x - radius <= this.clipRight && radius + y >= this.clipTop && y - radius <= this.clipBottom) {
            setupShapeRendering();
            setBlendMode(1);
            OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
            float startX = x + 0.35F;
            float startY = y + 0.35F;
            int pointSize = radius << 1;
            if (pointSize < this.fillCirclePointSize) {
                // Filled circle
                OpenGL.glBegin(GL_QUADS);
                OpenGL.glVertex2f(startX + 1.0F, startY + 1.0F);
                OpenGL.glVertex2f(startX + 1.0F, startY - 1.0F);
                OpenGL.glVertex2f(startX - 1.0F, startY - 1.0F);
                OpenGL.glVertex2f(startX - 1.0F, startY + 1.0F);
                OpenGL.glEnd();
            } else if (pointSize <= this.outlineCirclePointSize) {
            	// Circle outline
                OpenGL.glEnable(GL_POINT_SMOOTH);
                OpenGL.glPointSize(pointSize);
                OpenGL.glBegin(GL_POINTS);
                OpenGL.glVertex2f(startX, startY);
                OpenGL.glEnd();
                OpenGL.glDisable(GL_POINT_SMOOTH);
            } else {
            	// Filled circle with complex outline
                OpenGL.glBegin(GL_TRIANGLE_FAN);
                OpenGL.glVertex2f(startX, startY);
                int segments = 262144 / (radius * 6);
                if (segments <= 64) {
                    segments = 64;
                } else if (segments > 512) {
                    segments = 512;
                }

                segments = MathUtils.furtestBitValue(segments);
                OpenGL.glVertex2f(startX + radius, startY);

                for (int i = 16384 - segments; i > 0; i -= segments) {
                    OpenGL.glVertex2f(startX + COSINE[i] * radius, startY + SINE[i] * radius);
                }

                OpenGL.glVertex2f(startX + radius, startY);
                OpenGL.glEnd();
            }
        }

    }

    public void loadMatrix(Matrix44 matrix4x4) {
        OpenGL.glLoadMatrixf(matrix4x4.getFaceList(), 0);
    }

    @Override
    public void setSunPosition(Matrix43 matrix44var1) {
        this.aClass2948389.copyFrom(matrix44var1);
        this.aClass3848348.fromMatrix(this.aClass2948389);
        this.aClass2948405.copyFrom(matrix44var1);
        this.aClass2948405.updateIdentitiyMatrix();
        this.aClass3848391.fromMatrix(this.aClass2948405);
        method13597();
        if (this.depthClippingMode != 1) {
            method13748();
        }

    }

    @Override
    public Matrix43 copySunPosition() {
        return new Matrix43(this.aClass2948389);
    }

    @Override
    public Matrix44 copyModelMatrix() {
        return new Matrix44(this.aClass3848442);
    }

    public void method13586() {
        if (this.depthClippingMode != 0) {
            this.depthClippingMode = 0;
            method13584();
            updateDepthRange();
            this.functionMask &= -16;
        }

    }

    public void method13587() {
        if (this.depthClippingMode != 1) {
            this.depthClippingMode = 1;
            method13588();
            method13584();
            updateDepthRange();
            OpenGL.glMatrixMode(GL_MODELVIEW);
            OpenGL.glLoadIdentity();
            this.functionMask &= -9;
        }
    }

    public void method13588() {
        this.aFloat8404 = this.aClass3848442.getDepthPerspectiveCD();
        this.fogMinimumStartPosition = this.aClass3848442.method6533();
        setFogRange();
        if (this.depthClippingMode == 2) {
            invertMatrix(this.aClass3848442.getFaceList());
        } else if (this.depthClippingMode == 1) {
            invertMatrix(this.aClass3848385.getFaceList());
        }
    }

    @Override
    public boolean utilisesNativeHeap() {
        return true;
    }

    @Override
    public void method8525(int i_3, int i_4) {
        this.aClass1708357.method2884(i_3, i_4);
    }

    @Override
    public void updateSun(float sunBrightness) {
        if (this.sunBrightness != sunBrightness) {
        	this.sunBrightness = sunBrightness;
            updateSunColor();
        }
    }

    @Override
    public void applyLighting(int sunColor, float f_2, float f_3, float x, float y, float z) {
        boolean calculateSunColor = this.sunColorRgb != sunColor;
        if (calculateSunColor || this.aFloat8433 != f_2 || this.aFloat8434 != f_3) {
            this.sunColorRgb = sunColor;
            this.aFloat8433 = f_2;
            this.aFloat8434 = f_3;
            if (calculateSunColor) {
                this.sunColorRed = (this.sunColorRgb & 0xff0000) / 1.671168E7F;
                this.sunColorGreen = (this.sunColorRgb & 0xff00) / 65280.0F;
                this.sunColorBlue = (this.sunColorRgb & 0xff) / 255.0F;
                updateSunColor();
            }

            method13592();
        }

        if (this.sunPreviousPosition[0] != x || this.sunPreviousPosition[1] != y || this.sunPreviousPosition[2] != z) {
            this.sunPreviousPosition[0] = x;
            this.sunPreviousPosition[1] = y;
            this.sunPreviousPosition[2] = z;
            this.antiSunPreviousPosition[0] = -x;
            this.antiSunPreviousPosition[1] = -y;
            this.antiSunPreviousPosition[2] = -z;
            float length = (float) (1.0D / Math.sqrt(x * x + y * y + z * z));
            this.sunDirection[0] = x * length;
            this.sunDirection[1] = y * length;
            this.sunDirection[2] = z * length;
            this.lightingDirection[0] = -this.sunDirection[0];
            this.lightingDirection[1] = -this.sunDirection[1];
            this.lightingDirection[2] = -this.sunDirection[2];
            setLightingSourceParams();
            this.anInt8438 = (int) (x * 256.0F / y);
            this.anInt8439 = (int) (z * 256.0F / y);
        }
    }

    @Override
    public int isOnScreen(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        int i_7 = 0;
        float f_8 = this.aClass3848394.getFaceList()[14] + this.aClass3848394.getFaceList()[2] * i_1 + this.aClass3848394.getFaceList()[6] * i_2 + this.aClass3848394.getFaceList()[10] * i_3;
        float f_9 = this.aClass3848394.getFaceList()[14] + this.aClass3848394.getFaceList()[2] * i_4 + this.aClass3848394.getFaceList()[6] * i_5 + this.aClass3848394.getFaceList()[10] * i_6;
        float f_10 = this.aClass3848394.getFaceList()[15] + this.aClass3848394.getFaceList()[3] * i_1 + this.aClass3848394.getFaceList()[7] * i_2 + this.aClass3848394.getFaceList()[11] * i_3;
        float f_11 = this.aClass3848394.getFaceList()[15] + this.aClass3848394.getFaceList()[3] * i_4 + this.aClass3848394.getFaceList()[7] * i_5 + this.aClass3848394.getFaceList()[11] * i_6;
        if (f_8 < -f_10 && f_9 < -f_11) {
            i_7 |= 0x10;
        } else if (f_8 > f_10 && f_9 > f_11) {
            i_7 |= 0x20;
        }

        float f_12 = this.aClass3848394.getFaceList()[12] + this.aClass3848394.getFaceList()[0] * i_1 + this.aClass3848394.getFaceList()[4] * i_2 + this.aClass3848394.getFaceList()[8] * i_3;
        float f_13 = this.aClass3848394.getFaceList()[12] + this.aClass3848394.getFaceList()[0] * i_4 + this.aClass3848394.getFaceList()[4] * i_5 + this.aClass3848394.getFaceList()[8] * i_6;
        if (f_12 < -f_10 && f_13 < -f_11) {
            i_7 |= 0x1;
        }

        if (f_12 > f_10 && f_13 > f_11) {
            i_7 |= 0x2;
        }

        float f_14 = this.aClass3848394.getFaceList()[13] + this.aClass3848394.getFaceList()[1] * i_1 + this.aClass3848394.getFaceList()[5] * i_2 + this.aClass3848394.getFaceList()[9] * i_3;
        float f_15 = this.aClass3848394.getFaceList()[13] + this.aClass3848394.getFaceList()[1] * i_4 + this.aClass3848394.getFaceList()[5] * i_5 + this.aClass3848394.getFaceList()[9] * i_6;
        if (f_14 < -f_10 && f_15 < -f_11) {
            i_7 |= 0x4;
        }

        if (f_14 > f_10 && f_15 > f_11) {
            i_7 |= 0x8;
        }

        return i_7;
    }

    @Override
    public AbstractFontRenderer createFontRenderer(FontMetrics fontmetrics1, SpriteType[] arr_2, boolean bool3) {
        return new FontRendererGL(this, fontmetrics1, arr_2, bool3);
    }

    public void configureFog(float densityFactor, float f_2) {
        this.densityFactor = densityFactor; // 0.5
        this.fogMin = f_2; // fog_denstity
        setFogRange();
    }

  

    public void method13592() {
        BUFFER_4F[0] = this.aFloat8433 * this.sunColorRed;
        BUFFER_4F[1] = this.aFloat8433 * this.sunColorGreen;
        BUFFER_4F[2] = this.aFloat8433 * this.sunColorBlue;
        BUFFER_4F[3] = 1.0F;
        OpenGL.glLightfv(GL_COLOR_BUFFER_BIT, GL_DIFFUSE, BUFFER_4F, 0);
        BUFFER_4F[0] = -this.aFloat8434 * this.sunColorRed;
        BUFFER_4F[1] = -this.aFloat8434 * this.sunColorGreen;
        BUFFER_4F[2] = -this.aFloat8434 * this.sunColorBlue;
        BUFFER_4F[3] = 1.0F;
        OpenGL.glLightfv(GL_LIGHT1, GL_DIFFUSE, BUFFER_4F, 0);
    }

    public void setLightingSourceParams() {
        OpenGL.glLightfv(GL_COLOR_BUFFER_BIT, GL_POSITION, this.sunDirection, 0);
        OpenGL.glLightfv(GL_LIGHT1, GL_POSITION, this.lightingDirection, 0);
    }

    @Override
    public boolean hasShadows() {
        return true;
    }

    public void setFogRange() {
        this.fogEndPosition = this.aFloat8404 - this.anInt8378 - this.fogMin;
        this.fogStartPosition = this.fogEndPosition - this.fogDensity * this.densityFactor;
        if (this.fogStartPosition < this.fogMinimumStartPosition) {
            this.fogStartPosition = this.fogMinimumStartPosition;
        }

        OpenGL.glFogf(GL_FOG_START, this.fogStartPosition);
        OpenGL.glFogf(GL_FOG_END, this.fogEndPosition);
        BUFFER_4F[0] = (this.fogColor & 0xff0000) / 1.671168E7F;
        BUFFER_4F[1] = (this.fogColor & 0xff00) / 65280.0F;
        BUFFER_4F[2] = (this.fogColor & 0xff) / 255.0F;
        OpenGL.glFogfv(GL_FOG_COLOR, BUFFER_4F, 0);
    }

    public void setTexture(int activeUnit, int i_2, int i_3) {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, activeUnit + GL_SOURCE0_RGB, i_2);
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, activeUnit + GL_OPERAND0_RGB, i_3);
    }

    @Override
    public AbstractCubeTextureGenerator createCubeTexture(int materialAtPositiveX, int materialAtNegativeX, int materialAtPositiveY, int materialAtNegativeY, int materialAtPositiveZ, int materialAtNegativeZ) {
        return this.arbTextureCubeMap ? new OpenGLArbTextureCubeMapGenerator(this, materialAtPositiveX, materialAtNegativeX, materialAtPositiveY, materialAtNegativeY, materialAtPositiveZ, materialAtNegativeZ) : null;
    }

    @Override
    public AbstractCubeTextureGenerator drawQuad(AbstractCubeTextureGenerator class1521, AbstractCubeTextureGenerator class1522, float alpha, AbstractCubeTextureGenerator class1524) {
        if (class1521 != null && class1522 != null && this.arbTextureCubeMap && this.intelGpu) {
            OpenGlCubeTextureGenerator cubeTextureGenerator = null;
            AbstractOpenGlCubeTextureGenerator class152Sub16 = (AbstractOpenGlCubeTextureGenerator) class1521;
            AbstractOpenGlCubeTextureGenerator class152Sub17 = (AbstractOpenGlCubeTextureGenerator) class1522;
            OpenGlArbCubeMapTexture class137Sub28 = class152Sub16.getCubeTexture();
            OpenGlArbCubeMapTexture class137Sub29 = class152Sub17.getCubeTexture();
            if (class137Sub28 != null && class137Sub29 != null) {
                int largestMaterialSize = Math.max(class137Sub28.largestMaterialSize, class137Sub29.largestMaterialSize);
                if (class1521 != class1524 && class1522 != class1524 && class1524 instanceof OpenGlCubeTextureGenerator class152Sub1Sub211) {
                    if (class152Sub1Sub211.getLargestMaterialSize() == largestMaterialSize) {
                        cubeTextureGenerator = class152Sub1Sub211;
                    }
                }

                if (cubeTextureGenerator == null) {
                    cubeTextureGenerator = new OpenGlCubeTextureGenerator(this, largestMaterialSize);
                }

                if (cubeTextureGenerator.drawQuad(class137Sub28, class137Sub29, alpha)) {
                    return cubeTextureGenerator;
                }
            }
        }

        return alpha < 0.5F ? class1521 : class1522;
    }


    public OpenGlArbCubeMapTexture getCubeMap() {
        return this.aClass152Sub18317 != null ? this.aClass152Sub18317.getCubeTexture() : null;
    }

    @Override
    public boolean bloomSupported() {
        if (this.rectangularTexture != null) {
            if (!this.rectangularTexture.method12129()) {
                if (!this.aClass1708357.method2900(this.rectangularTexture)) {
                    return false;
                }

                this.textureCache.resetBloomCache();
            }

            return true;
        }
        return false;
    }

    @Override
    public void resetBloom() {
        if (this.rectangularTexture != null && this.rectangularTexture.method12129()) {
            this.aClass1708357.method2885(this.rectangularTexture);
            this.textureCache.resetBloomCache();
        }
    }

    @Override
    public boolean bloomEnabled() {
        return this.rectangularTexture != null && this.rectangularTexture.method12129();
    }

    @Override
    public void setBloomFilter(float whitePoint, float bloom, float brightpass) {
        RectangularTextureGL.whitePoint = whitePoint;
        RectangularTextureGL.bloom = bloom;
        RectangularTextureGL.brightpass = brightpass;
    }

    @Override
    public void method8474() {
        this.aClass1708357.method2883();
    }

    @Override
    public DirectDisplay getImageProducer() {
        return new IntegratedGraphicsDisplayGL(this);
    }

    public void method13597() {
        this.aClass3848394.copyFrom(this.aClass3848348);
        this.aClass3848394.multiplyBy(this.aClass3848442);
        this.aClass3848394.method6539(this.aFloatArrayArray8395[0]);
        this.aClass3848394.method6540(this.aFloatArrayArray8395[1]);
        this.aClass3848394.method6535(this.aFloatArrayArray8395[2]);
        this.aClass3848394.method6536(this.aFloatArrayArray8395[3]);
        this.aClass3848394.method6613(this.aFloatArrayArray8395[4]);
        this.aClass3848394.method6538(this.aFloatArrayArray8395[5]);
    }

    public ElementsBufferGL createElementsArrayBuffer(byte[] bytes_2, int i_3, boolean bool4) {
        return !this.vertexbufferSupported || bool4 && !this.shouldUseArbVbo ? new GlElementsArrayBuffer(this, bytes_2, i_3) : new GlVboElementsArrayBuffer(this, bytes_2, i_3, bool4);
    }

    public ArrayBufferGL createArrayBuffer(int size, byte[] buffer, int position, boolean bool4) {
        return !this.vertexbufferSupported || bool4 && !this.shouldUseArbVbo ? new GlArrayBuffer(this, size, buffer, position) : new GlVboArrayBuffer(this, size, buffer, position, bool4);
    }

    public ArrayBufferGL createDataArrayBuffer(int i_1, Buffer buffer2, int i_3) {
        return !this.vertexbufferSupported ? new GlArrayBuffer(this, i_1, buffer2) : new GlVboArrayBuffer(this, i_1, buffer2, i_3);
    }

    public void setArrayBuffer(ArrayBufferGL arrayBuffer) {
        if (arrayBuffer != this.currentArrayBuffer) {
            if (this.vertexbufferSupported) {
                OpenGL.glBindBufferARB(GL_ARRAY_BUFFER, arrayBuffer.getId());
            }
            this.currentArrayBuffer = arrayBuffer;
        }
    }

    @Override
    public void resetTextureState() {
    }

    public void renderAnimatedTexture(float x, float y) {
        OpenGL.glMatrixMode(GL_TEXTURE);
        if (this.textureUpdate) {
            OpenGL.glLoadIdentity();
        }

        OpenGL.glTranslatef(x, y, (float) 0.0);
        OpenGL.glMatrixMode(GL_MODELVIEW);
        this.textureUpdate = true;
    }

    public void drawArrays(int count) {
        OpenGL.glDrawArrays(7, 0, count);
    }
    
    public void setupShapeRendering() {
        if (this.functionMask != 1) {
            method13587();
            toggleFog(false);
            setLightingEnabled(false);
            toggleZbuffer(false);
            setDepthMask(false);
            setActiveTexture(null);
            renderTexture(-2);
            loadTextureCombineMode(1);
            this.functionMask = 1;
        }
    }
    
    public void setupSpriteRendering() {
        if (this.functionMask != 2) {
            method13587();
            toggleFog(false);
            setLightingEnabled(false);
            toggleZbuffer(false);
            setDepthMask(false);
            renderTexture(-2);
            this.functionMask = 2;
        }
    }

    public void setupTextRendering() {
        if (this.functionMask != 4) {
            method13587();
            toggleFog(false);
            setLightingEnabled(false);
            toggleZbuffer(false);
            setDepthMask(false);
            renderTexture(-2);
            setBlendMode(1);
            this.functionMask = 4;
        }
    }

    public void setupMatrixRendering() {
        if (this.functionMask != 8) {
            method13645();
            toggleFog(true);
            toggleZbuffer(true);
            setDepthMask(true);
            setBlendMode(1);
            this.functionMask = 8;
        }
    }

    public void renderTexture(int materialId, boolean hasLight) {
        renderTexture(materialId, hasLight, true);
    }

    public void renderTexture(int materialId, boolean hasLight, boolean has0x8) {
        if (this.lastMaterialId != materialId || this.renderStillWater != this.stillWaterToggled) {
            OpenGl2DTexture texture = null;
            int combineMode = 0;
            byte param1 = 0;
            int param2 = 0;
            int effectId = this.stillWaterToggled ? 3 : 0;
            if (materialId < 0) {
                renderStationaryTexture();
            } else {
                texture = this.textureCache.get(materialId);
                MaterialType material = super.materialProvider.getMaterial(materialId);
                if (material.textureSpeedU == 0 && material.textureSpeedV == 0) {
                    renderStationaryTexture();
                } else {
                    renderAnimatedTexture((this.textureAnimatingProgression % 128000) / 1000.0F * material.textureSpeedU / 64.0F % 1.0F, (this.textureAnimatingProgression % 128000) / 1000.0F * material.textureSpeedV / 64.0F % 1.0F);
                }
                if (!this.stillWaterToggled) {
                    param1 = material.effectParam1;
                    param2 = material.effectParam2;
                    effectId = material.effectId;
                }
                combineMode = material.combineMode;
            }
            this.effectManager.loadEffect(effectId, param1, param2, hasLight, has0x8);
            if (!this.effectManager.loadCombiner(texture, combineMode)) {
                setActiveTexture(texture);
                loadTextureCombineMode(combineMode);
            }
            this.renderStillWater = this.stillWaterToggled;
            this.lastMaterialId = materialId;
        }
        this.functionMask &= -8;
    }

    public void activateTexture(int textureUnit) {
        if (this.glTexturesCursorPos != textureUnit) {
            OpenGL.glActiveTexture(textureUnit + GL_TEXTURE0);
            this.glTexturesCursorPos = textureUnit;
        }
    }

    public void drawElements(ElementsBufferGL elementsBuffer, int i_3, int i_4) {
        setElementsBuffer(elementsBuffer);
        OpenGL.glDrawElements(GL_TRIANGLES, i_4, GL_UNSIGNED_SHORT, elementsBuffer.getBufferAddress() + (i_3 * 2L));
    }

    public void loadTextureCombineMode(int mode) {
        if (mode == 1) {
            loadTextureCombineMode(GL_REPLACE, GL_REPLACE);
        } else if (mode == 0) {
            loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        } else if (mode == 2) {
            loadTextureCombineMode(GL_INTERPOLATE, GL_REPLACE);
        } else if (mode == 3) {
            loadTextureCombineMode(GL_ADD, GL_MODULATE);
        } else if (mode == 4) {
            loadTextureCombineMode(GL_SUBTRACT, GL_SUBTRACT);
        }
    }

    public int getCombineModeEffectType(int mode) {
        if (mode == 1) {
            return GL_REPLACE;
        } else if (mode == 0) {
            return GL_MODULATE;
        } else if (mode == 2) {
            return GL_INTERPOLATE;
        } else if (mode == 3) {
            return GL_ADD;
        } else if (mode == 4) {
            return GL_SUBTRACT;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void setTextureTransparency(int textureUnit, /*FIXME: name*/int alpha) {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, textureUnit + GL_SOURCE0_ALPHA, alpha);
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, textureUnit + GL_OPERAND0_ALPHA, GL_SRC_ALPHA);
    }

    public void setTextureEnvColor(int textureColorArgb) {
        BUFFER_4F[0] = (textureColorArgb & 0xff0000) / 1.671168E7F;
        BUFFER_4F[1] = (textureColorArgb & 0xff00) / 65280.0F;
        BUFFER_4F[2] = (textureColorArgb & 0xff) / 255.0F;
        BUFFER_4F[3] = (textureColorArgb >>> 24) / 255.0F;
        OpenGL.glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, BUFFER_4F, 0);
    }


    public void renderStationaryTexture() {
        if (this.textureUpdate) {
            OpenGL.glMatrixMode(GL_TEXTURE);
            OpenGL.glLoadIdentity();
            OpenGL.glMatrixMode(GL_MODELVIEW);
            this.textureUpdate = false;
        }
    }

    public void setLightingEnabled(boolean enabled) {
        if (this.lightingEnabled != enabled) {
            this.lightingEnabled = enabled;
            toggleLighting();
            this.functionMask &= -8;
        }
    }

    public void setHideLights(boolean enabled) {
        if (this.hideLights != enabled) {
            this.hideLights = enabled;
            toggleLighting();
        }
    }

    public void toggleLighting() {
        if (this.lightingEnabled && !this.hideLights) {
            OpenGL.glEnable(GL_LIGHTING);
        } else {
            OpenGL.glDisable(GL_LIGHTING);
        }
    }



    public void setDepthMask(boolean enableMask) {
        if (this.enableDepthMask != enableMask) {
            this.enableDepthMask = enableMask;
            setDepthMask();
            this.functionMask &= -16;
        }
    }

    public void setBlendMode(int blendAlpha) {
        if (this.activeBlendAlpha != blendAlpha) {
            byte blendType;
            boolean testAlpha;
            if (blendAlpha == 1) {
                blendType = BlendType.ALPHA;
                testAlpha = true;
            } else if (blendAlpha == 2) {
                blendType = BlendType.ADDITIVE;
                testAlpha = false;
            } else if (blendAlpha == 128) {
                blendType = BlendType.MULTIPLY;
                testAlpha = true;
            } else {
                blendType = BlendType.DISABLED;
                testAlpha = false;
            }
            if (!this.colormaskActive) {
                OpenGL.glColorMask(true, true, true, true);
                this.colormaskActive = true;
            }
            if (testAlpha != this.alphaTestEnabled) {
                if (testAlpha) {
                    OpenGL.glEnable(GL_ALPHA_TEST);
                } else {
                    OpenGL.glDisable(GL_ALPHA_TEST);
                }
                this.alphaTestEnabled = testAlpha;
            }
            if (blendType != this.activeBlendType) {
                if (blendType == BlendType.ALPHA) {
                    OpenGL.glEnable(GL_BLEND);
                    OpenGL.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                } else if (blendType == BlendType.ADDITIVE) {
                    OpenGL.glEnable(GL_BLEND);
                    OpenGL.glBlendFunc(GL_ONE, GL_ONE);
                } else if (blendType == BlendType.MULTIPLY) {
                    OpenGL.glEnable(GL_BLEND);
                    OpenGL.glBlendFunc(GL_DST_COLOR, GL_ONE);
                } else {
                    OpenGL.glDisable(GL_BLEND);
                }
                this.activeBlendType = blendType;
            }
            this.activeBlendAlpha = blendAlpha;
            this.functionMask &= -13;
        }
    }

    @Override
    public int getOffheap() {
        return this.vboBuffersSize + this.textureBuffersSize + this.renderBuffersSize;
    }

    @Override
    public boolean replaceCanvasOnDisplayUpdate() {
        return false;
    }


    public synchronized void requestTextureCleanup(int handle, int sizeRequirement) {
        IntNode handleAwaitingRemoval = new IntNode(sizeRequirement);
        handleAwaitingRemoval.pointer = handle;
        this.texturesAwaitingDeallocation.insertTail(handleAwaitingRemoval);
    }

    public synchronized void requestShaderProgramCleanup(long programHandle) {
        Node<?> handleAwaitingRemoval = new Node();
        handleAwaitingRemoval.pointer = programHandle;
        this.shaderProgramsAwaitingDeallocation.insertTail(handleAwaitingRemoval);
    }

    public synchronized void requestArbProgramCleanup(int programHandle) {
        Node<?> handleAwaitingRemoval = new Node();
        handleAwaitingRemoval.pointer = programHandle;
        this.arbProgramsAwaitingDeallocation.insertTail(handleAwaitingRemoval);
    }
    
    public synchronized void requestArbBufferCleanup(int bufferHandle, int bufferSize) {
        IntNode handleAwaitingRemoval = new IntNode(bufferSize);
        handleAwaitingRemoval.pointer = bufferHandle;
        this.vboBuffersAwaitingDeallocation.insertTail(handleAwaitingRemoval);
    }
    
    public synchronized void requestFrameBufferCleanup(int bufferSize) {
        IntNode handleAwaitingRemoval = new IntNode(bufferSize);
        this.frameBuffersAwaitingDeallocation.insertTail(handleAwaitingRemoval);
    }


    public void multiMatrix(Matrix44 matrix44) {
        OpenGL.glPushMatrix();
        OpenGL.glMultMatrixf(matrix44.getFaceList(), 0);
    }

    public void updatePointSizeRange(float red, float green, float blue, float alpha) {
        BUFFER_4F[0] = red;
        BUFFER_4F[1] = green;
        BUFFER_4F[2] = blue;
        BUFFER_4F[3] = alpha;
        OpenGL.glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, BUFFER_4F, 0);
    }

    public void renderLights() {
        int lightCount;
        for (lightCount = 0; lightCount < this.lastSceneLightCount; lightCount++) {
            PointLight activeLight = this.lightNodes[lightCount];
            int lightHandle = lightCount + 16386;
            LIGHT_MATRIX[0] = activeLight.getX();
            LIGHT_MATRIX[1] = activeLight.getY();
            LIGHT_MATRIX[2] = activeLight.getZ();
            LIGHT_MATRIX[3] = 1.0F;
            OpenGL.glLightfv(lightHandle, GL_POSITION, LIGHT_MATRIX, 0);
            int color = activeLight.getColor();
            float alphaMultiplier = activeLight.getAlpha() / 255.0F;
            LIGHT_MATRIX[0] = (color >> 16 & 0xff) * alphaMultiplier;
            LIGHT_MATRIX[1] = (color >> 8 & 0xff) * alphaMultiplier;
            LIGHT_MATRIX[2] = (color & 0xff) * alphaMultiplier;
            OpenGL.glLightfv(lightHandle, GL_DIFFUSE, LIGHT_MATRIX, 0);
            OpenGL.glLightf(lightHandle, GL_QUADRATIC_ATTENUATION, 1.0F / (activeLight.getSize2d() * activeLight.getSize2d()));
            OpenGL.glEnable(lightHandle);
        }
        while (lightCount < this.activeLightCount) {
            OpenGL.glDisable(lightCount + GL_LIGHT2);
            lightCount++;
        }
        this.activeLightCount = this.lastSceneLightCount;
    }

    public void method13639() {
        if (super.display == null) {
            return;
        }
        int displayWidth = super.display.getWidth();
        int displayHeight = super.display.getHeight();
        this.aClass3848385.method6530(0.0F, displayWidth, 0.0F, displayHeight, -1.0f, 1.0F);
        calculateViewportCenter();
        method13586();
        resetClipping();
    }

    public void method13640(int i_1, int i_2) {
        this.anInt8453 = i_1;
        this.anInt8416 = i_2;
        method13584();
        defineScissorBox();
    }


    public void popMatrix() {
        OpenGL.glPopMatrix();
    }

    public void toggleFog(boolean enableFog) {
        if (this.fogEnabled != enableFog) {
            this.fogEnabled = enableFog;
            toggleFog();
            this.functionMask &= -16;
        }
    }

    @Override
    public void generateFlags() {
        int meme = 32;
        for (this.shadowScale = 0; meme > 1; meme >>= 1) {
            this.shadowScale++;
        }
        this.openglFlags = 1 << this.shadowScale;
    }

    public void method13645() {
        if (this.depthClippingMode != 2) {
            this.depthClippingMode = 2;
            invertMatrix(this.aClass3848442.getFaceList());
            method13748();
            method13584();
            updateDepthRange();
            this.functionMask &= -8;
        }
    }

    @Override
    public void debugModeChanged() {
    }


    public void toggleDrawingClientstates(OpenGlArrayDrawBuffer vertices, OpenGlArrayDrawBuffer normals, OpenGlArrayDrawBuffer colors, OpenGlArrayDrawBuffer textureCoordinates) {
        if (vertices != null) {
            setArrayBuffer(vertices.buffer);
            OpenGL.glVertexPointer(vertices.size, vertices.type, this.currentArrayBuffer.getStride(), this.currentArrayBuffer.getBufferAddress() + vertices.offset);
            OpenGL.glEnableClientState(GL_VERTEX_ARRAY);
        } else {
            OpenGL.glDisableClientState(GL_VERTEX_ARRAY);
        }
        
        if (normals != null) {
            setArrayBuffer(normals.buffer);
            OpenGL.glNormalPointer(normals.type, this.currentArrayBuffer.getStride(), this.currentArrayBuffer.getBufferAddress() + normals.offset);
            OpenGL.glEnableClientState(GL_NORMAL_ARRAY);
        } else {
            OpenGL.glDisableClientState(GL_NORMAL_ARRAY);
        }

        if (colors != null) {
            setArrayBuffer(colors.buffer);
            OpenGL.glColorPointer(colors.size, colors.type, this.currentArrayBuffer.getStride(), this.currentArrayBuffer.getBufferAddress() + colors.offset);
            OpenGL.glEnableClientState(GL_COLOR_ARRAY);
        } else {
            OpenGL.glDisableClientState(GL_COLOR_ARRAY);
        }

        if (textureCoordinates != null) {
            setArrayBuffer(textureCoordinates.buffer);
            OpenGL.glTexCoordPointer(textureCoordinates.size, textureCoordinates.type, this.currentArrayBuffer.getStride(), this.currentArrayBuffer.getBufferAddress() + textureCoordinates.offset);
            OpenGL.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        } else {
            OpenGL.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        }

    }

    @Override
    public void drawTransparentHorizontalLine(int x, int y, int width, int colorArgb, int alpha) {
        setupShapeRendering();
        setBlendMode(alpha);
        float lineX = x + 0.35F;
        float lineY = y + 0.35F;
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        OpenGL.glBegin(1);
        OpenGL.glVertex2f(lineX, lineY);
        OpenGL.glVertex2f(lineX + width, lineY);
        OpenGL.glEnd();
    }


    @Override
    public void resetDepthClippingMask() {
        this.depthClippingNear = (float) 0.0;
        this.depthClippingFar = (float) 1.0;
        updateDepthRange();
    }

    @Override
    public void drawTransparentVerticalLine(int x, int y, int height, int colorArgb, int blendMode) {
        setupShapeRendering();
        setBlendMode(blendMode);
        float lineX = x + 0.35F;
        float lineY = y + 0.35F;
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        OpenGL.glBegin(1);
        OpenGL.glVertex2f(lineX, lineY);
        OpenGL.glVertex2f(lineX, lineY + height);
        OpenGL.glEnd();
    }


    public int accumulateCapabilities() {
        int attributes = 0;
        this.gpuCompanyString = OpenGL.glGetString(GL_VENDOR).toLowerCase();
        this.gpuNameString = OpenGL.glGetString(GL_RENDERER).toLowerCase();
        if (this.gpuCompanyString.indexOf("microsoft") != -1) {
            attributes |= 0x1;
        }
        if (this.gpuCompanyString.indexOf("brian paul") != -1 || this.gpuCompanyString.indexOf("mesa") != -1) {
            attributes |= 0x1;
        }
        String version = OpenGL.glGetString(GL_VERSION);
        String[] versionStrings = Utils.split(version.replace('.', ' '), ' ');
        if (versionStrings.length >= 2) {
            try {
                int majorVersion = Utils.parseInt(versionStrings[0]);
                int minorVersion = Utils.parseInt(versionStrings[1]);
                this.openglVersion = majorVersion * 10 + minorVersion;
            } catch (NumberFormatException numberformatexception) {
                attributes |= 0x4;
            }
        } else {
            attributes |= 0x4;
        }
        if (this.openglVersion < 12) {
            attributes |= 0x2;
        }
        if (!this.activeOpenglInstance.supportsExtension("GL_ARB_multitexture")) {
            attributes |= 0x8;
        }
        if (!this.activeOpenglInstance.supportsExtension("GL_ARB_texture_env_combine")) {
            attributes |= 0x20;
        }
        int[] textureInfo = new int[1];
        OpenGL.glGetIntegerv(GL_MAX_TEXTURE_UNITS, textureInfo, 0);
        this.maxTextureUnits = textureInfo[0];
        OpenGL.glGetIntegerv(GL_MAX_TEXTURE_COORDS, textureInfo, 0);
        this.maxTextureCoordinates = textureInfo[0];
        OpenGL.glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, textureInfo, 0);
        this.maxTextureImageUnits = textureInfo[0];
        if (this.maxTextureUnits < 2 || this.maxTextureCoordinates < 2 || this.maxTextureImageUnits < 2) {
            attributes |= 0x10;
        }
        this.bigEndianess = Stream.bigEndianessEnabled();
        this.vertexbufferSupported = this.activeOpenglInstance.supportsExtension("GL_ARB_vertex_buffer_object");
        this.arbMultisample = this.activeOpenglInstance.supportsExtension("GL_ARB_multisample");
        this.arbVertexProgram = this.activeOpenglInstance.supportsExtension("GL_ARB_vertex_program");
        this.activeOpenglInstance.supportsExtension("GL_ARB_fragment_program");
        this.arbVertexShader = this.activeOpenglInstance.supportsExtension("GL_ARB_vertex_shader");
        this.arbFragmentShader = this.activeOpenglInstance.supportsExtension("GL_ARB_fragment_shader");
        this.texture3dSupported = this.activeOpenglInstance.supportsExtension("GL_EXT_texture3D");
        this.arbRectangleTexturesSupported = this.activeOpenglInstance.supportsExtension("GL_ARB_texture_rectangle");
        this.arbTextureCubeMap = this.activeOpenglInstance.supportsExtension("GL_ARB_texture_cube_map");
        this.arbTextureFloat = this.activeOpenglInstance.supportsExtension("GL_ARB_texture_float");
        this.aBool8498 = false;
        this.intelGpu = this.activeOpenglInstance.supportsExtension("GL_EXT_framebuffer_object");
        this.framebufferBlit = this.activeOpenglInstance.supportsExtension("GL_EXT_framebuffer_blit");
        this.framebufferMultisample = this.activeOpenglInstance.supportsExtension("GL_EXT_framebuffer_multisample");
        this.advancedFramebuffers = this.framebufferBlit & this.framebufferMultisample;
        this.appleOperatingSystem = OperatingSystemInfo.OS_NAME.startsWith("mac");
        OpenGL.glGetFloatv(GL_POINT_SIZE_RANGE, BUFFER_4F, 0);
        this.fillCirclePointSize = BUFFER_4F[0];
        this.outlineCirclePointSize = BUFFER_4F[1];
        return attributes;
    }

    public void setActiveTexture(OpenGlTexture replacingCombiner) {
        OpenGlTexture currentCombiner = this.openglTextures[this.glTexturesCursorPos];
        if (currentCombiner != replacingCombiner) {
            if (replacingCombiner != null) {
                if (currentCombiner != null) {
                    if (currentCombiner.textureTarget != replacingCombiner.textureTarget) {
                        OpenGL.glDisable(currentCombiner.textureTarget);
                        OpenGL.glEnable(replacingCombiner.textureTarget);
                    }
                } else {
                    OpenGL.glEnable(replacingCombiner.textureTarget);
                }
                OpenGL.glBindTexture(replacingCombiner.textureTarget, replacingCombiner.activeHandle);
            } else {
                OpenGL.glDisable(currentCombiner.textureTarget);
            }
            this.openglTextures[this.glTexturesCursorPos] = replacingCombiner;
        }
        this.functionMask &= -2;
    }


    public void toggleZbuffer(boolean zbuffer) {
        if (this.depthBuffering != zbuffer) {
            if (zbuffer) {
                OpenGL.glEnable(GL_DEPTH_TEST);
            } else {
                OpenGL.glDisable(GL_DEPTH_TEST);
            }
            this.depthBuffering = zbuffer;
            this.functionMask &= -16;
        }
    }

    public void setElementsBuffer(ElementsBufferGL elementsBuffer) {
        if (elementsBuffer != this.currentElementsBuffer) {
            if (this.vertexbufferSupported) {
                OpenGL.glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER, elementsBuffer.getId());
            }
            this.currentElementsBuffer = elementsBuffer;
        }
    }


    @Override
    public Ground createGround(int regionSizeX, int regionSizeY, int[][] heightsCurrentLvl, int[][] heightsNextLvl, int floorFunctionMask, int waterFunctionMask) {
        return new GroundGL(this, floorFunctionMask, waterFunctionMask, regionSizeX, regionSizeY, heightsCurrentLvl, heightsNextLvl);
    }


    @Override
    public DelegatingDisplay createDelegatingDisplay(Canvas canvas1, int i_2, int i_3) {
        return new ImageProducerGL2(this, canvas1);
    }


    public void updateDepthRange() {
        if (this.depthClippingMode == 2) {
            OpenGL.glDepthRange(this.depthClippingNear, this.depthClippingFar);
        } else {
            OpenGL.glDepthRange(0.0F, 1.0F);
        }
    }

    @Override
    public RenderInfo getRenderInfo() {
        int gpuVerdorIdentifier = -1;
        if (this.gpuCompanyString.indexOf("nvidia") != -1) {
            gpuVerdorIdentifier = 4318;
        } else if (this.gpuCompanyString.indexOf("intel") != -1) {
            gpuVerdorIdentifier = 32902;
        } else if (this.gpuCompanyString.indexOf("ati") != -1) {
            gpuVerdorIdentifier = 4098;
        }
        return new RenderInfo(gpuVerdorIdentifier, "OpenGL", this.openglVersion, this.gpuNameString, 0L);
    }

    public void updateSunColor() {
        BUFFER_4F[0] = this.sunBrightness * this.sunColorRed;
        BUFFER_4F[1] = this.sunBrightness * this.sunColorGreen;
        BUFFER_4F[2] = this.sunBrightness * this.sunColorBlue;
        BUFFER_4F[3] = 1.0F;
        OpenGL.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, BUFFER_4F, 0);
    }


    @Override
    public void renderWorldmapTile(int startX, int startY, int width, int height, int colorArgb, int textureEnvColor, byte[] bytes_7, int i_8, int textureBlendColor) {
        float f_10;
        float f_11;
        if (this.aClass137Sub1Sub18462 != null && this.aClass137Sub1Sub18462.width >= width && this.aClass137Sub1Sub18462.length >= height) {
            this.aClass137Sub1Sub18462.uploadTextureData(0, width, height, bytes_7, GLFormat.RGBA, false);
            f_10 = this.aClass137Sub1Sub18462.t * height / this.aClass137Sub1Sub18462.length;
            f_11 = this.aClass137Sub1Sub18462.s * width / this.aClass137Sub1Sub18462.width;
        } else {
            this.aClass137Sub1Sub18462 = OpenGl2DTexture_Sub1.createSurface(this, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, width, height, bytes_7, GLFormat.RGBA);
            this.aClass137Sub1Sub18462.setRepeat(false, false);
            f_10 = this.aClass137Sub1Sub18462.t;
            f_11 = this.aClass137Sub1Sub18462.s;
        }
        setupSpriteRendering();
        setActiveTexture(this.aClass137Sub1Sub18462);
        setBlendMode(textureBlendColor);
        OpenGL.glColor4ub((byte) (colorArgb >> 16), (byte) (colorArgb >> 8), (byte) colorArgb, (byte) (colorArgb >> 24));
        setTextureEnvColor(textureEnvColor);
        loadTextureCombineMode(GL_INTERPOLATE, GL_INTERPOLATE);
        setTexture(0, GL_CONSTANT, GL_SRC_COLOR);
        setTexture(2, GL_TEXTURE, GL_SRC_ALPHA);
        setTextureTransparency(0, GL_CONSTANT);
        setTextureTransparency(2, GL_TEXTURE);
        float xA = startX;
        float yA = startY;
        float xB = xA + width;
        float yB = yA + height;
        OpenGL.glBegin(7);
        OpenGL.glTexCoord2f(0.0F, 0.0F);
        OpenGL.glVertex2f(xA, yA);
        OpenGL.glTexCoord2f(0.0F, f_11);
        OpenGL.glVertex2f(xA, yB);
        OpenGL.glTexCoord2f(f_10, f_11);
        OpenGL.glVertex2f(xB, yB);
        OpenGL.glTexCoord2f(f_10, 0.0F);
        OpenGL.glVertex2f(xB, yA);
        OpenGL.glEnd();
        setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        setTexture(2, GL_CONSTANT, GL_SRC_ALPHA);
        setTextureTransparency(0, GL_TEXTURE);
        setTextureTransparency(2, GL_CONSTANT);
    }

    @Override
    public void copyLights(int lightCount, PointLight[] lights) {
        if (lightCount > 0) System.arraycopy(lights, 0, this.lightNodes, 0, lightCount);
        this.lastSceneLightCount = lightCount;
        if (this.depthClippingMode != 1) {
            renderLights();
        }
    }

    public boolean stillwaterEffectLoaded() {
        return this.effectManager.stillwaterEffectLoaded();
    }



    public synchronized void requestRenderBufferCleanup(int handle, int i_2) {
        IntNode handleAwaitingRemoval = new IntNode(i_2);
        handleAwaitingRemoval.pointer = handle;
        this.renderBuffersAwaitingRemoval.insertTail(handleAwaitingRemoval);
    }

    @Override
    public void setSunCubeTexture(AbstractCubeTextureGenerator class1521) {
        this.aClass152Sub18317 = (AbstractOpenGlCubeTextureGenerator) class1521;
    }

    public void loadTextureCombineMode(int combineRgb, int combineAlpha) {
        if (this.glTexturesCursorPos == 0) {
            boolean hasCombining = false;
            if (this.textureCombineRgb != combineRgb) {
                OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, combineRgb);
                this.textureCombineRgb = combineRgb;
                hasCombining = true;
            }
            if (combineAlpha != this.textureCombineAlpha) {
                OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, combineAlpha);
                this.textureCombineAlpha = combineAlpha;
                hasCombining = true;
            }
            if (hasCombining) {
                this.functionMask &= -14;
            }
        } else {
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, combineRgb);
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, combineAlpha);
        }
    }

    public void invertMatrix(float[] matrix) {
        float[] invertedMatrix = new float[16];
        System.arraycopy(matrix, 0, invertedMatrix, 0, 16);
        invertedMatrix[1] = -invertedMatrix[1];
        invertedMatrix[5] = -invertedMatrix[5];
        invertedMatrix[9] = -invertedMatrix[9];
        invertedMatrix[13] = -invertedMatrix[13];
        OpenGL.glMatrixMode(GL_PROJECTION);
        OpenGL.glLoadMatrixf(invertedMatrix, 0);
        OpenGL.glMatrixMode(GL_MODELVIEW);
    }

    @Override
    public boolean hasTextures() {
        return true;
    }

    @Override
    public int getMaxLights() {
        return 4;
    }

    @Override
    public void setModelMatrix(Matrix44 matrix441) {
        this.aClass3848442.copyFrom(matrix441);
        method13597();
        method13588();
    }

    @Override
    public boolean hasAntiAliasing() {
        return this.arbMultisample && (!bloomEnabled() || this.advancedFramebuffers);
    }

    public void toggleFog() {
        if (this.fogEnabled && this.fogDensity >= 0) {
            OpenGL.glEnable(GL_FOG);
        } else {
            OpenGL.glDisable(GL_FOG);
        }
    }

    public void method13748() {
        OpenGL.glLoadIdentity();
        OpenGL.glMultMatrixf(this.aClass3848348.getFaceList(), 0);
        if (this.renderStillWater) {
            this.effectManager.waterEffect.enable();
        }
        setLightingSourceParams();
        renderLights();
    }

    @Override
    public int mergeFunctionMasks(int base, int mask) {
        return base | mask;
    }


}
