package com.jagex.game.runetek5.toolkit;

import jaclib.memory.Stream;
import jaggl.MapBuffer;
import jaggl.OpenGL;

import java.awt.*;

import com.jagex.game.runetek5.buffer.OpenGlNativeRenderBuffer;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.LinkedList;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.shader.Shader;
import com.jagex.game.runetek5.graphics.shader.ShaderGL;
import com.jagex.game.runetek5.graphics.shader.ShaderLoader;
import com.jagex.game.runetek5.graphics.shader.ShaderMappingGL;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.graphics.texture.TextureStageState;
import com.jagex.game.runetek5.graphics.texture.TextureState;
import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapRectangleTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler2D;
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler3D;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.OpenGLCubeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.Texture3DSampler;
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.imageproducer.ImageProducerDX1_Sub2;
import com.jagex.game.runetek5.imageproducer.ImageProducerDX3;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.rendering.RenderInfo;
import com.jagex.game.runetek5.toolkit.companion.BlendFuncMode;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArbArrayBuffer;
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArbElementArrayBuffer;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationGL;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.utils.Utils;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public class OpenGLNativeToolkit extends AbstractNativeToolkit {

    public ShaderMappingGL activeShaderMapping;
    public final LinkedList displayListsToDelete = new LinkedList();
    public final LinkedList textureBuffersToDelete = new LinkedList();
    public final LinkedList texturesToDelete = new LinkedList();
    public final LinkedList frameBuffersToDelete = new LinkedList();
    public final LinkedList renderBuffersToDelete = new LinkedList();
    public final LinkedList programsToDelete = new LinkedList();
    public final LinkedList shadersToDelete = new LinkedList();
    public final int[] textureIdsToDelete = new int[1000];
    public final OpenGlArbArrayBuffer[] aClass326Sub1Array10186 = new OpenGlArbArrayBuffer[16];
    /**
     * Used to store either the fog position / color
     */
    public final float[] parameters;
    public final float[] currentTextureMatrix;
    public OpenGL opengl;
    public String gpuVendor;
    public String gpuManufactorer;
    public int combinedOpenglVersion;
    public boolean GLARBVertexBufferObject;
    public boolean GLARBTextureRectangle;
    public boolean GLARBTextureNonPowerOfTwo;
    public boolean GLARBVertexShader;
    public boolean GLARBFragmentShader;
    public int[] textureSlots;
    public int pixelType;
    public OpenGlArbElementArrayBuffer aClass326Sub210187;
    public long lastGcTime;
    public boolean enableVertexArray;
    public boolean enableVertexNormal;
    public boolean enableVertexColor;
    public int textureAllocationPosition;

    public OpenGLNativeToolkit(OpenGL opengl1, Canvas canvas2, long long_3, MaterialProvider interface225, Js5ResourceProvider index6, int i_7) {
        super(interface225, index6, i_7, 1);
        new MapBuffer();
        new MapBuffer();
        this.parameters = new float[4];
        this.currentTextureMatrix = new float[16];
        int[] ints_8 = new int[1];

        try {
            this.opengl = opengl1;
            this.opengl.attach();
            this.gpuVendor = OpenGL.glGetString(GL_VENDOR).toLowerCase();
            this.gpuManufactorer = OpenGL.glGetString(GL_RENDERER).toLowerCase();
            if (this.gpuVendor.indexOf("microsoft") == -1 && this.gpuVendor.indexOf("brian paul") == -1 && this.gpuVendor.indexOf("mesa") == -1) {
                String openglVersion = OpenGL.glGetString(GL_VERSION);
                String[] openglVersionParts = Utils.split(openglVersion.replace('.', ' '), ' ');
                if (openglVersionParts.length < 2) {
                	throw new RuntimeException("");
                }

                try {
                    int majorVersion = Utils.parseInt(openglVersionParts[0]);
                    int minorVersion = Utils.parseInt(openglVersionParts[1]);
                    this.combinedOpenglVersion = majorVersion * 10 + minorVersion;
                } catch (NumberFormatException exception) {
                    throw new RuntimeException("");
                }

                if (this.combinedOpenglVersion < 12) {
                    throw new RuntimeException("");
                } 
                OpenGL.glGetIntegerv(GL_MAX_TEXTURE_UNITS, ints_8, 0);
                super.maxConcurrentTextures = ints_8[0];
                if (super.maxConcurrentTextures < 2) {
                    throw new RuntimeException("");
                } 
                super.maxActiveLights = 8;
                this.GLARBVertexBufferObject = this.opengl.supportsExtension("GL_ARB_vertex_buffer_object");
                super.GLARBMultisample = this.opengl.supportsExtension("GL_ARB_multisample");
                this.GLARBTextureRectangle = this.opengl.supportsExtension("GL_ARB_texture_rectangle");
                this.opengl.supportsExtension("GL_ARB_texture_cube_map");
                this.GLARBTextureNonPowerOfTwo = this.opengl.supportsExtension("GL_ARB_texture_non_power_of_two");
                super.SUPPORTS_TEXTURE3D = this.opengl.supportsExtension("GL_EXT_texture3D");
                this.GLARBVertexShader = this.opengl.supportsExtension("GL_ARB_vertex_shader");
                this.opengl.supportsExtension("GL_ARB_vertex_program");
                this.GLARBFragmentShader = this.opengl.supportsExtension("GL_ARB_fragment_shader");
                this.opengl.supportsExtension("GL_ARB_fragment_program");
                this.opengl.supportsExtension("GL_EXT_framebuffer_object");
                this.textureSlots = new int[super.maxConcurrentTextures];
                super.GLEXTFramebufferObject = this.opengl.supportsExtension("GL_EXT_framebuffer_object");
                this.opengl.supportsExtension("GL_EXT_framebuffer_blit");
                this.opengl.supportsExtension("GL_EXT_framebuffer_multisample");
                if (!supportsArbVertexShader() || !supportsArbFragmentShader()) {
                    if (!this.opengl.supportsExtension("GL_ARB_multitexture")) {
                        throw new RuntimeException("");
                    }

                    if (!this.opengl.supportsExtension("GL_ARB_texture_env_combine")) {
                        throw new RuntimeException("");
                    }
                }

                this.pixelType = Stream.bigEndianessEnabled() ? GL_UNSIGNED_INT_8_8_8_8_REV : GL_UNSIGNED_BYTE;
                if (this.gpuManufactorer.indexOf("radeon") != -1) {
                    int radeonCapabilities = 0;
                    boolean isRadeonHd = false;
                    boolean wrongJava = false;
                    String[] gpuManufactorerParts = Utils.split(this.gpuManufactorer.replace('/', ' '), ' ');

                    for (int i_15 = 0; i_15 < gpuManufactorerParts.length; i_15++) {
                        String manufactorerPart = gpuManufactorerParts[i_15];

                        try {
                            if (!manufactorerPart.isEmpty()) {
                                if (manufactorerPart.charAt(0) == 120 && manufactorerPart.length() >= 3 && Utils.isWrongJava(manufactorerPart.substring(1, 3))) {
                                    manufactorerPart = manufactorerPart.substring(1);
                                    wrongJava = true;
                                }

                                if ("hd".equals(manufactorerPart)) {
                                    isRadeonHd = true;
                                } else {
                                    if (manufactorerPart.startsWith("hd")) {
                                        manufactorerPart = manufactorerPart.substring(2);
                                        isRadeonHd = true;
                                    }

                                    if (manufactorerPart.length() >= 4 && Utils.isWrongJava(manufactorerPart.substring(0, 4))) {
                                        radeonCapabilities = Utils.parseInt(manufactorerPart.substring(0, 4));
                                        break;
                                    }
                                }
                            }
                        } catch (Exception exception) {
                        	// NOOP
                        }
                    }

                    if (!wrongJava && !isRadeonHd) {
                        if (radeonCapabilities >= 7000 && radeonCapabilities <= 7999) {
                            this.GLARBVertexBufferObject = false;
                        }

                        if (radeonCapabilities >= 7000 && radeonCapabilities <= 9250) {
                            super.SUPPORTS_TEXTURE3D = false;
                        }
                    }

                    this.GLARBTextureRectangle &= this.opengl.supportsExtension("GL_ARB_half_float_pixel");
                }

                this.gpuVendor.indexOf("intel");
                if (this.GLARBVertexBufferObject) {
                    try {
                        int[] buffers = new int[1];
                        OpenGL.glGenBuffersARB(1, buffers, 0);
                    } catch (Throwable throwable) {
                        throw new RuntimeException("");
                    }
                }

                addImageProducer(canvas2, new ImageProducerDX3(this, canvas2, long_3));
                setActiveCanvas(canvas2);
                generateNativeHeapBuffer(GL_QUERY_BUFFER_BARRIER_BIT, false);
                generateNativeHeapBuffer(GL_QUERY_BUFFER_BARRIER_BIT, false);
                    

            } else {
                throw new RuntimeException("");
            }
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

    public static int actionFormatDataType(GLActionFormat actionFormat) {
        switch (actionFormat.anInt757) {
            case 0:
                return GL_SHORT;
            case 1:
            case 4:
            default:
                return GL_UNSIGNED_BYTE;
            case 2:
                return GL_UNSIGNED_INT;
            case 3:
                return GL_FLOAT;
            case 5:
                return GL_UNSIGNED_SHORT;
            case 6:
                return GL_BYTE;
            case 7:
                return GL_INT;
            case 8:
                return GL_HALF_FLOAT;
        }
    }

    public static int method15567(TextureStageState class670) {
        if (TextureStageState.aClass67680 == class670) {
            return GL_TEXTURE;
        } else if (TextureStageState.aClass67678 == class670) {
            return GL_PRIMARY_COLOR;
        } else if (TextureStageState.aClass67679 == class670) {
            return GL_PREVIOUS;
        } else if (TextureStageState.aClass67677 == class670) {
            return GL_CONSTANT;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static int texture2dInternalFormat(GLFormat actionFormat, GLActionFormat format) {
        if (format == GLActionFormat.PIXEL_FORMAT) {
            switch (actionFormat.formatType) {
                case 0:
                    return GL_LUMINANCE;
                case 1:
                    return GL_ALPHA;
                case 2:
                case 3:
                case 8:
                default:
                    throw new IllegalArgumentException();
                case 4:
                    return GL_LUMINANCE_ALPHA;
                case 5:
                    return GL_RGB;
                case 6:
                    return GL_RGBA;
                case 7:
                    return GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
                case 9:
                    return GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
            }
        } else if (format == GLActionFormat.TEXTURE_FORMAT) {
            switch (actionFormat.formatType) {
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
        } else if (format == GLActionFormat.DEPTH_COMPONENT_24) {
            switch (actionFormat.formatType) {
                case 8:
                    return GL_DEPTH_COMPONENT24;
                default:
                    throw new IllegalArgumentException();
            }
        } else if (format == GLActionFormat.FLOAT_16_ARB) {
            switch (actionFormat.formatType) {
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
        } else if (format == GLActionFormat.FLOAT_32_ARB) {
            switch (actionFormat.formatType) {
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

    public static int texture2dFormat(GLFormat class1500) {
        switch (class1500.formatType) {
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

    public static int getTextureCombineMode(TextureCombineMode textureCombineMode) {
        if (TextureCombineMode.REPLACE == textureCombineMode) {
            return GL_REPLACE;
        } else if (TextureCombineMode.MODULATE == textureCombineMode) {
            return GL_MODULATE;
        } else if (TextureCombineMode.INTERPOLATE == textureCombineMode) {
            return GL_INTERPOLATE;
        } else if (TextureCombineMode.ADD == textureCombineMode) {
            return GL_ADD;
        } else if (TextureCombineMode.SUBTRACT == textureCombineMode) {
            return GL_SUBTRACT;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Shader loadShader(String string_1) {
        byte[] shaderData = getGLShaderData(string_1);
        if (shaderData == null) {
            return null;
        } 
        ShaderLoader class1143 = createShader(shaderData);
        return new ShaderGL(this, class1143);
    }

    @Override
    public boolean supportsArbVertexShader() {
        return this.GLARBVertexShader;
    }

    @Override
    public boolean supportsArbFragmentShader() {
        return this.GLARBFragmentShader;
    }

    @Override
    public void awaitRenderCompletion() {
        OpenGL.glFinish();
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (this.opengl != null) {
            this.opengl.detach();
            this.opengl.release();
            this.opengl = null;
        }
    }

    @Override
    public float method13905() {
        return 0.0F;
    }

    @Override
    public DelegatingDisplay createDelegatingDisplay(Canvas canvas1, int i_2, int i_3) {
        return new ImageProducerDX3(this, canvas1);
    }

    @Override
    public DirectDisplay getImageProducer() {
        return new ImageProducerDX1_Sub2(this);
    }

    @Override
    public int[] copyPixelRectangle(int i_1, int i_2, int i_3, int i_4) {
        int[] ints_5 = new int[i_3 * i_4];
        int i_6 = super.display.getHeight();

        for (int i_7 = 0; i_7 < i_4; i_7++) {
            OpenGL.glReadPixelsi(i_1, i_6 - i_2 - i_7 - 1, i_3, 1, 32993, this.pixelType, ints_5, i_3 * i_7);
        }

        return ints_5;
    }

    @Override
    public AbstractCubeTextureGenerator drawQuad(AbstractCubeTextureGenerator class1521, AbstractCubeTextureGenerator class1522, float f_3, AbstractCubeTextureGenerator class1524) {
        return f_3 < 0.5F ? class1521 : class1522;
    }

    @Override
    public void applyClearFlags(int flags, int color) {
        int mask = 0;
        if ((flags & 0x1) != 0) { 
            OpenGL.glClearColor((color & 0xff0000) / 1.671168E7F, (color & 0xff00) / 65280.0F, (color & 0xff) / 255.0F, (color >>> 24) / 255.0F);
            mask = 16384; // GL_COLOR_BUFFER_BIT
        }

        if ((flags & 0x2) != 0) { 
            method13942(true);
            mask |= 0x100; // GL_DEPTH_BUFFER_BIT
        }

        if ((flags & 0x4) != 0) { 
            mask |= 0x400; // GL_STENCIL_BUFFER_BIT
        }

        OpenGL.glClear(mask);
    }

    @Override
    void setViewport() {
        if (super.display != null) {
            OpenGL.glViewport(super.anInt8823 + super.anInt8749, super.anInt8754 + super.display.getHeight() - super.anInt8776 - super.anInt8752, super.anInt8751, super.anInt8752);
        }

        OpenGL.glDepthRange(super.depthRangeNear, super.depthRangeFar);
    }

    @Override
    void toggleDepthTest() {
        if (super.enableDepthTest) {
            OpenGL.glEnable(GL_DEPTH_TEST);
        } else {
            OpenGL.glDisable(GL_DEPTH_TEST);
        }
    }

    @Override
    void setDepthRenderState() {
        OpenGL.glDepthMask(super.depthBufferingEnabled && super.aBool8756);
    }

    @Override
    void toggleLights() {
        if (super.aBool8684 && !super.aBool8760) {
            OpenGL.glEnable(GL_LIGHTING);
        } else {
            OpenGL.glDisable(GL_LIGHTING);
        }
    }

    @Override
    void setSunParameters() {
        this.parameters[0] = super.sunColorIntensity * super.baseLightColorRed;
        this.parameters[1] = super.sunColorIntensity * super.baseLightColorGreen;
        this.parameters[2] = super.sunColorIntensity * super.baseLightColorBlue;
        this.parameters[3] = 1.0F;
        OpenGL.glLightfv(GL_LIGHT0, GL_DIFFUSE, this.parameters, 0);
        this.parameters[0] = -super.antiSunColorIntensity * super.baseLightColorRed;
        this.parameters[1] = -super.antiSunColorIntensity * super.baseLightColorGreen;
        this.parameters[2] = -super.antiSunColorIntensity * super.baseLightColorBlue;
        this.parameters[3] = 1.0F;
        OpenGL.glLightfv(GL_LIGHT1, GL_DIFFUSE, this.parameters, 0);
    }

    @Override
    public void loadWorldViewProjectionMatrix(Matrix44 matrix441) {
        float[] floats_2 = matrix441.faceList;
        floats_2[1] = -floats_2[1];
        floats_2[5] = -floats_2[5];
        floats_2[9] = -floats_2[9];
        floats_2[13] = -floats_2[13];
    }

    @Override
    void toggleSun() {
        if (super.sunEnabled) {
            OpenGL.glEnable(GL_LIGHT0);
            OpenGL.glEnable(GL_LIGHT1);
        } else {
            OpenGL.glDisable(GL_LIGHT0);
            OpenGL.glDisable(GL_LIGHT1);
        }
    }

    @Override
    void enableLights() {
    }

    @Override
    void configureLights() {
        OpenGL.glMatrixMode(GL_MODELVIEW);
        OpenGL.glLoadMatrixf(super.particleMatrix.faceList, 0);

        int index;
        for (index = 0; index < super.lastSceneLightCount; index++) {
            PointLight lightSource = super.lightSources[index];
            int color = lightSource.getColor();
            int lightUnit = index + GL_LIGHT2;
            float alpha = lightSource.getAlpha() / 255.0F;
            this.parameters[0] = lightSource.getX();
            this.parameters[1] = lightSource.getY();
            this.parameters[2] = lightSource.getZ();
            this.parameters[3] = 1.0F;
            OpenGL.glLightfv(lightUnit, GL_POSITION, this.parameters, 0);
            this.parameters[0] = (color >> 16 & 0xff) * alpha;
            this.parameters[1] = (color >> 8 & 0xff) * alpha;
            this.parameters[2] = (color & 0xff) * alpha;
            this.parameters[3] = 1.0F;
            OpenGL.glLightfv(lightUnit, GL_DIFFUSE, this.parameters, 0);
            OpenGL.glLightf(lightUnit, GL_QUADRATIC_ATTENUATION, 1.0F / (lightSource.getSize2d() * lightSource.getSize2d()));
            OpenGL.glEnable(lightUnit);
        }

        while (index < super.currentSceneLightCount) {
            OpenGL.glDisable(GL_LIGHT2 + index++);
        }
    }

    @Override
    public boolean method13955(GLFormat class1501, GLActionFormat class762) {
        return super.SUPPORTS_TEXTURE3D;
    }

    @Override
    public NativeTextureSampler method14072(GLFormat class1501, GLActionFormat class762, int i_3, int i_4) {
        return method14064(class1501, class762, i_3, i_4);
    }

    @Override
    NativeTextureSampler createNativeTexture(GLFormat class1501, int i_2, int i_3, float[] floats_5) {
        if (!this.GLARBTextureNonPowerOfTwo && (!OpenGlTexture.isPowerOfTwo(i_2) || !OpenGlTexture.isPowerOfTwo(i_3))) {
            if (this.GLARBTextureRectangle) {
                return new MipmapRectangleTextureSampler(this, class1501, i_2, i_3, floats_5);
            }
            MipmapTextureSampler2D sampler2d = new MipmapTextureSampler2D(this, class1501, GLActionFormat.FLOAT_32_ARB, MathUtils.nextPowerOfTwo(i_2), MathUtils.nextPowerOfTwo(i_3));
            sampler2d.method14494(i_2, i_3, floats_5, class1501);
            return sampler2d;
        }
        return new MipmapTextureSampler2D(this, class1501, i_2, i_3, floats_5);
    }

    @Override
    public RectangleTextureSampler method14064(GLFormat class1501, GLActionFormat class762, int i_3, int i_4) {
        return !this.GLARBTextureNonPowerOfTwo && (!OpenGlTexture.isPowerOfTwo(i_3) || !OpenGlTexture.isPowerOfTwo(i_4)) ? (this.GLARBTextureRectangle ? new MipmapRectangleTextureSampler(this, class1501, class762, i_3, i_4) : new MipmapTextureSampler2D(this, class1501, class762, MathUtils.nextPowerOfTwo(i_3), MathUtils.nextPowerOfTwo(i_4))) : new MipmapTextureSampler2D(this, class1501, class762, i_3, i_4);
    }

    @Override
    public CubeTextureSampler generateNativeCubeTexture(int sizeXy, boolean mipmapsGenerated, int[][] ints_3) {
        return new OpenGLCubeTextureSampler(this, sizeXy, mipmapsGenerated, ints_3);
    }

    @Override
    public Texture3DSampler method13962(GLFormat class1501, byte[] bytes_6) {
        return new MipmapTextureSampler3D(this, class1501, bytes_6);
    }

    @Override
    void activateCurrentTexture() {
        OpenGL.glActiveTexture(super.currentTextureSlot + GL_TEXTURE0);
    }

    @Override
    void setTextureColorStage() {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, getTextureCombineMode(super.textureRgbCombineModes[super.currentTextureSlot]));
    }

    @Override
    void setTextureAlphaStage() {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, getTextureCombineMode(super.textureAlphaCombineModes[super.currentTextureSlot]));
    }

    @Override
    void method13970(int i_1, TextureStageState class672) {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, i_1 + 34184, method15567(class672));
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, i_1 + 34200, 770);
    }

    @Override
    void setTextureEnvironmentColor() {
        this.parameters[0] = (super.envColor & 0xff0000) / 1.671168E7F;
        this.parameters[1] = (super.envColor & 0xff00) / 65280.0F;
        this.parameters[2] = (super.envColor & 0xff) / 255.0F;
        this.parameters[3] = (super.envColor >>> 24) / 255.0F;
        OpenGL.glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, this.parameters, 0);
    }

    @Override
    public void colorMask(int flags) {
        OpenGL.glColorMask((flags & 0x1) != 0, (flags & 0x2) != 0, (flags & 0x4) != 0, (flags & 0x8) != 0);
    }

    @Override
    void blendDrawBuffer() {
        byte alphaSrcFactor = 0;
        byte alphaDstFactor = 0;
        if (super.anInt8844 == 0) {
            alphaDstFactor = 0;
        } else if (super.anInt8844 == 1) {
            alphaSrcFactor = 1;
            alphaDstFactor = 0;
        } else if (super.anInt8844 == 2) {
            alphaSrcFactor = 1;
            alphaDstFactor = 1;
        }

        if (super.blendFuncMode == BlendFuncMode.ALPHA_BLENDING) {
            OpenGL.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, alphaSrcFactor, alphaDstFactor);
        } else if (super.blendFuncMode == BlendFuncMode.OPAQUE_BLENDING) {
            OpenGL.glBlendFuncSeparate(GL_ONE, GL_ONE, alphaSrcFactor, alphaDstFactor);
        } else if (super.blendFuncMode == BlendFuncMode.COLOR_BLENDING) {
            OpenGL.glBlendFuncSeparate(GL_DST_COLOR, GL_ONE, alphaSrcFactor, alphaDstFactor);
        }
    }

    @Override
    void toggleBlending() {
        if (super.enableBlending) {
            OpenGL.glEnable(GL_BLEND);
        } else {
            OpenGL.glDisable(GL_BLEND);
        }
    }

    @Override
    void toggleFog() {
        if (super.fogSupported && super.fogEnabled && super.fogDistance >= 0) {
            OpenGL.glEnable(GL_FOG);
        } else {
            OpenGL.glDisable(GL_FOG);
        }
    }

    @Override
    void configureFog() {
        super.fogEnd = super.camFar - super.anInt8680;
        super.fogStart = super.fogEnd - super.fogDistance;
        if (super.fogStart < super.camNear) {
            super.fogStart = super.camNear;
        }

        if (super.enableLights) {
            OpenGL.glFogf(GL_FOG_START, super.fogStart);
            OpenGL.glFogf(GL_FOG_END, super.fogEnd);
            this.parameters[0] = (super.fogColorRgb & 0xff0000) / 1.671168E7F;
            this.parameters[1] = (super.fogColorRgb & 0xff00) / 65280.0F;
            this.parameters[2] = (super.fogColorRgb & 0xff) / 255.0F;
            OpenGL.glFogfv(GL_FOG_COLOR, this.parameters, 0);
        }
    }

    @Override
    void toggleMultisampling(boolean enabled) {
        if (enabled) {
            OpenGL.glEnable(GL_MULTISAMPLE);
        } else {
            OpenGL.glDisable(GL_MULTISAMPLE);
        }

    }

    @Override
	public
    NativeStream createStream(boolean bool1) {
        return new OpenGlArbArrayBuffer(this, bool1);
    }

    @Override
    public void setStreamSource(int i_1, NativeStream interface42) {
        this.aClass326Sub1Array10186[i_1] = (OpenGlArbArrayBuffer) interface42;
    }

    @Override
    public void drawPrimitive(PrimitiveDrawMode drawMode, int first, int amt) {
        byte mode;
        int count;
        if (drawMode == PrimitiveDrawMode.LINES) {
            mode = GL_LINES;
            count = amt * 2;
        } else if (drawMode == PrimitiveDrawMode.LINE_STRIP) {
            mode = GL_LINE_STRIP;
            count = amt + 1;
        } else if (drawMode == PrimitiveDrawMode.TRIANGLES) {
            mode = GL_TRIANGLES;
            count = amt * 3;
        } else if (drawMode == PrimitiveDrawMode.TRIANGLE_FAN) {
            mode = GL_TRIANGLE_STRIP;
            count = amt + 2;
        } else if (drawMode == PrimitiveDrawMode.TRIANGLE_STRIP) {
            mode = GL_TRIANGLE_STRIP;
            count = amt + 2;
        } else {
            mode = GL_POINTS;
            count = amt;
        }
        OpenGL.glDrawArrays(mode, first, count);
    }

    @Override
    public void drawIndexedPrimitive(PrimitiveDrawMode drawMode, int i_2, int i_3, int i_4, int amt) {
        byte mode;
        int count;
        if (drawMode == PrimitiveDrawMode.LINES) {
            mode = GL_LINES;
            count = amt * 2;
        } else if (drawMode == PrimitiveDrawMode.LINE_STRIP) {
            mode = GL_LINE_STRIP;
            count = amt + 1;
        } else if (drawMode == PrimitiveDrawMode.TRIANGLES) {
            mode = GL_TRIANGLES;
            count = amt * 3;
        } else if (drawMode == PrimitiveDrawMode.TRIANGLE_FAN) {
            mode = GL_TRIANGLE_FAN;
            count = amt + 2;
        } else if (drawMode == PrimitiveDrawMode.TRIANGLE_STRIP) {
            mode = GL_TRIANGLE_STRIP;
            count = amt + 2;
        } else {
            mode = GL_POINTS;
            count = amt;
        }
        GLActionFormat actionFormat = this.aClass326Sub210187.getActionFormat();
        OpenGL.glDrawElements(mode, count, actionFormatDataType(actionFormat), this.aClass326Sub210187.getBufferAddress() + ((long) i_4 * actionFormat.bytesPerEntry));
    }

    @Override
    public synchronized void animateTextures(int textureId) {
        int index = 0;
        textureId &= 0x7fffffff;

        while (!this.textureBuffersToDelete.isEmpty()) {
        	IntNode node = (IntNode) this.textureBuffersToDelete.popTail();
            this.textureIdsToDelete[index++] = (int) node.pointer;
            super.textureBuffersSizeOccupied -= node.value;
            if (index == 1000) {
                OpenGL.glDeleteBuffersARB(1000, this.textureIdsToDelete, 0);
                index = 0;
            }
        }

        if (index > 0) {
            OpenGL.glDeleteBuffersARB(index, this.textureIdsToDelete, 0);
            index = 0;
        }

        while (!this.texturesToDelete.isEmpty()) {
        	IntNode node = (IntNode) this.texturesToDelete.popTail();
            this.textureIdsToDelete[index++] = (int) node.pointer;
            super.texturesSizeOccupied -= node.value;
            if (index == 1000) {
                OpenGL.glDeleteTextures(1000, this.textureIdsToDelete, 0);
                index = 0;
            }
        }

        if (index > 0) {
            OpenGL.glDeleteTextures(index, this.textureIdsToDelete, 0);
            index = 0;
        }

        while (!this.frameBuffersToDelete.isEmpty()) {
        	IntNode node = (IntNode) this.frameBuffersToDelete.popTail();
            this.textureIdsToDelete[index++] = node.value;
            if (index == 1000) {
                OpenGL.glDeleteFramebuffersEXT(1000, this.textureIdsToDelete, 0);
                index = 0;
            }
        }

        if (index > 0) {
            OpenGL.glDeleteFramebuffersEXT(index, this.textureIdsToDelete, 0);
            index = 0;
        }

        while (!this.renderBuffersToDelete.isEmpty()) {
        	IntNode node = (IntNode) this.renderBuffersToDelete.popTail();
            this.textureIdsToDelete[index++] = (int) node.pointer;
            super.renderBufferSizeOccupied -= node.value;
            if (index == 1000) {
                OpenGL.glDeleteRenderbuffersEXT(1000, this.textureIdsToDelete, 0);
                index = 0;
            }
        }

        if (index > 0) {
            OpenGL.glDeleteRenderbuffersEXT(index, this.textureIdsToDelete, 0);
        }

        while (!this.displayListsToDelete.isEmpty()) {
        	IntNode node = (IntNode) this.displayListsToDelete.popTail();
            OpenGL.glDeleteLists((int) node.pointer, node.value);
        }

        while (!this.programsToDelete.isEmpty()) {
        	Node node = this.programsToDelete.popTail();
            OpenGL.glDeleteProgram((int) node.pointer);
        }

        while (!this.shadersToDelete.isEmpty()) {
        	Node node = this.shadersToDelete.popTail();
            OpenGL.glDeleteShader((int) node.pointer);
        }

        while (!this.displayListsToDelete.isEmpty()) {
        	IntNode node = (IntNode) this.displayListsToDelete.popTail();
            OpenGL.glDeleteLists((int) node.pointer, node.value);
        }

        if (getOffheap() > 100663296 && Utils.time() > this.lastGcTime + 60000L) {
            System.gc();
            this.lastGcTime = Utils.time();
        }

        super.animateTextures(textureId);
    }

    public synchronized void requestTextureBufferDeletion(int handle, int size) {
        IntNode node = new IntNode(size);
        node.pointer = handle;
        this.textureBuffersToDelete.insertTail(node);
    }

    public synchronized void requestTextureDeletion(int textureId, int size) {
        IntNode node = new IntNode(size);
        node.pointer = textureId;
        this.texturesToDelete.insertTail(node);
    }

    public synchronized void requestFrameBufferDeletion(int size) {
        IntNode node = new IntNode(size);
        this.frameBuffersToDelete.insertTail(node);
    }

    public synchronized void requestRenderBufferDeletion(int textureId, int size) {
        IntNode node = new IntNode(size);
        node.pointer = textureId;
        this.renderBuffersToDelete.insertTail(node);
    }

    public synchronized void destroyProgram(int programHandle) {
        Node node = new Node();
        node.pointer = programHandle;
        this.programsToDelete.insertTail(node);
    }

    @Override
    void setTextureMatrix() {
        OpenGL.glMatrixMode(GL_TEXTURE);
        if (super.textureStates[super.currentTextureSlot] != TextureState.INITIALIZE) {
            OpenGL.glLoadMatrixf(super.textureMatrices[super.currentTextureSlot].copyInto(this.currentTextureMatrix), 0);
        } else {
            OpenGL.glLoadIdentity();
        }

        OpenGL.glMatrixMode(GL_MODELVIEW);
    }

    @Override
    public void debugModeChanged() {
    }

    @Override
    public void setPolygonMode(int mode) {
        OpenGL.glPolygonMode(GL_FRONT, (mode & 0x2) != 0 ? GL_LINE : GL_FILL);
    }


    @Override
    public void method13997(DirectXBuffer interface321) {
        this.aClass326Sub210187 = (OpenGlArbElementArrayBuffer) interface321;
        this.aClass326Sub210187.bind();
    }


    @Override
	public
    VertexDeclaration method13995(VertexDeclarationComponent[] arr_1) {
        return new VertexDeclarationGL(arr_1);
    }


    public byte[] getGLShaderData(String string_1) {
        return fetchShader("gl", string_1);
    }

    @Override
    void toggleScissorTest() {
        if (super.boundsCalculated) {
            OpenGL.glEnable(GL_SCISSOR_TEST);
        } else {
            OpenGL.glDisable(GL_SCISSOR_TEST);
        }
    }
    
    @Override
    void setupGraphics() {
        // Set depth function
        OpenGL.glDepthFunc(GL_LEQUAL);

        // Enable backface culling
        OpenGL.glEnable(GL_CULL_FACE);
        OpenGL.glCullFace(GL_BACK);

        // Set clear depth and alpha function
        OpenGL.glClearDepth(1.0F);
        OpenGL.glAlphaFunc(GL_GREATER, 0.0F);

        // Set polygon mode
        OpenGL.glPolygonMode(GL_FRONT, GL_FILL);

        // Set texture environment
        for (int textureUnit = super.maxConcurrentTextures - 1; textureUnit >= 0; textureUnit--) {
            OpenGL.glActiveTexture(GL_TEXTURE0 + textureUnit);
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_MODULATE);
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_SRC2_RGB, GL_CONSTANT);
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_MODULATE);
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_SRC2_ALPHA, GL_CONSTANT);
        }
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_SRC2_ALPHA, GL_PREVIOUS);
        
        // Set shade model and matrix mode
        OpenGL.glShadeModel(GL_SMOOTH);
        OpenGL.glMatrixMode(GL_MODELVIEW);
        OpenGL.glLoadIdentity();

        // Enable color material
        OpenGL.glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
        OpenGL.glEnable(GL_COLOR_MATERIAL);

        // Set default lighting
        float[] ambient = {0.0F, 0.0F, 0.0F, 1.0F};
        for (int lightUnit = 0; lightUnit < 8; lightUnit++) {
            int light = GL_LIGHT0 + lightUnit;
            OpenGL.glLightfv(light, GL_AMBIENT, ambient, 0);
            OpenGL.glLightf(light, GL_CONSTANT_ATTENUATION, 0.0F);
            OpenGL.glLightf(light, GL_LINEAR_ATTENUATION, 0.0F);
        }
        
        // Set default fog
        OpenGL.glFogf(GL_FOG_DENSITY, 0.95F);
        OpenGL.glFogi(GL_FOG_MODE, GL_LINEAR);
        
        // Set default hints and swap interval
        OpenGL.glHint(GL_FOG_HINT, GL_FASTEST);
        this.opengl.setSwapInterval(0);
        
        super.setupGraphics();
    }

    public synchronized void requestShaderDeletion(long pointer) {
        Node node3 = new Node();
        node3.pointer = pointer;
        this.shadersToDelete.insertTail(node3);
    }

    @Override
    public void render() {
        super.delegatingDisplay.draw();
        if (super.materialProvider != null) {
            super.materialProvider.cycle();
        }
    }


    @Override
    public void setVertexDeclaration(VertexDeclaration vertexDeclaration) {
        VertexDeclarationComponent[] components = vertexDeclaration.vertexDeclarationComponents;
        int numTextures = 0;
        boolean enableVertexColor = false;
        boolean enableVertexNormal = false;
        boolean enableVertexArray = false;

        for (int i = 0; i < components.length; i++) {
            VertexDeclarationComponent component = components[i];
            OpenGlArbArrayBuffer arrayBuffer = this.aClass326Sub1Array10186[i];
            int pointerOffset = 0;
            int stride = arrayBuffer.getStride();
            long pointer = arrayBuffer.getBufferAddress();
            arrayBuffer.bind();

            for (int j = 0; j < component.getSize(); j++) {
                VertexDeclarationAction action = component.getAction(j);
                switch (action.openglAction) {
                    case 0:
                        OpenGL.glNormalPointer(GL_FLOAT, stride, pointer + pointerOffset);
                        enableVertexNormal = true;
                    case 1:
                    case 3:
                    case 5:
                    case 6:
                    default:
                        break;
                    case 2:
                        OpenGL.glClientActiveTexture(GL_TEXTURE0 + numTextures++);
                        OpenGL.glTexCoordPointer(4, GL_FLOAT, stride, pointer + pointerOffset);
                        break;
                    case 4:
                        OpenGL.glClientActiveTexture(GL_TEXTURE0 + numTextures++);
                        OpenGL.glTexCoordPointer(2, GL_FLOAT, stride, pointer + pointerOffset);
                        break;
                    case 7:
                        OpenGL.glVertexPointer(3, GL_FLOAT, stride, pointer + pointerOffset);
                        enableVertexArray = true;
                        break;
                    case 8:
                        OpenGL.glClientActiveTexture(GL_TEXTURE0 + numTextures++);
                        OpenGL.glTexCoordPointer(1, GL_FLOAT, stride, pointer + pointerOffset);
                        break;
                    case 9:
                        OpenGL.glClientActiveTexture(GL_TEXTURE0 + numTextures++);
                        OpenGL.glTexCoordPointer(3, GL_FLOAT, stride, pointer + pointerOffset);
                        break;
                    case 10:
                        OpenGL.glColorPointer(4, GL_UNSIGNED_BYTE, stride, pointer + pointerOffset);
                        enableVertexColor = true;
                }

                pointerOffset += action.pointerOffsetStep;
            }
        }

        if (enableVertexArray != this.enableVertexArray) {
            if (enableVertexArray) {
                OpenGL.glEnableClientState(GL_VERTEX_ARRAY);
            } else {
                OpenGL.glDisableClientState(GL_VERTEX_ARRAY);
            }

            this.enableVertexArray = enableVertexArray;
        }

        if (enableVertexNormal != this.enableVertexNormal) {
            if (enableVertexNormal) {
                OpenGL.glEnableClientState(GL_NORMAL_ARRAY);
            } else {
                OpenGL.glDisableClientState(GL_NORMAL_ARRAY);
            }

            this.enableVertexNormal = enableVertexNormal;
        }

        if (enableVertexColor != this.enableVertexColor) {
            if (enableVertexColor) {
                OpenGL.glEnableClientState(GL_COLOR_ARRAY);
            } else {
                OpenGL.glDisableClientState(GL_COLOR_ARRAY);
            }

            this.enableVertexColor = enableVertexColor;
        }

        if (this.textureAllocationPosition < numTextures) {
            for (int textureUnit = this.textureAllocationPosition; textureUnit < numTextures; textureUnit++) {
                OpenGL.glClientActiveTexture(textureUnit + GL_TEXTURE0);
                OpenGL.glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            }

            this.textureAllocationPosition = numTextures;
        } else if (this.textureAllocationPosition > numTextures) {
            for (int textureUnit = numTextures; textureUnit < this.textureAllocationPosition; textureUnit++) {
                OpenGL.glClientActiveTexture(textureUnit + GL_TEXTURE0);
                OpenGL.glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            }

            this.textureAllocationPosition = numTextures;
        }

    }

    @Override
    public DirectXBuffer method13993(boolean bool1) {
        return new OpenGlArbElementArrayBuffer(this, GLActionFormat.TEXTURE_FORMAT, bool1);
    }

    @Override
    public void setTextureOnCurrentSlot() {
        int target = this.textureSlots[super.currentTextureSlot];
        if (target != 0) {
            this.textureSlots[super.currentTextureSlot] = 0;
            OpenGL.glBindTexture(target, 0);
            OpenGL.glDisable(target);
        }
    }

    @Override
    public RenderInfo getRenderInfo() {
        int vendor = -1;
        if (this.gpuVendor.indexOf("nvidia") != -1) {
            vendor = 4318;
        } else if (this.gpuVendor.indexOf("intel") != -1) {
            vendor = 32902;
        } else if (this.gpuVendor.indexOf("ati") != -1) {
            vendor = 4098;
        }
        return new RenderInfo(vendor, "OpenGL", this.combinedOpenglVersion, this.gpuManufactorer, 0L);
    }

    @Override
    NativeTextureSampler createNativeTextureSampler(GLFormat class1501, int width, int height, boolean bool4, byte[] pixels) {
        if (!this.GLARBTextureNonPowerOfTwo && (!OpenGlTexture.isPowerOfTwo(width) || !OpenGlTexture.isPowerOfTwo(height))) {
            if (this.GLARBTextureRectangle) {
                return new MipmapRectangleTextureSampler(this, class1501, width, height, pixels);
            } 
            MipmapTextureSampler2D sampler2d = new MipmapTextureSampler2D(this, class1501, GLActionFormat.PIXEL_FORMAT, MathUtils.nextPowerOfTwo(width), MathUtils.nextPowerOfTwo(height));
            sampler2d.method52(0, 0, width, height, pixels, class1501, 0, 0);
            return sampler2d;
        }
        return new MipmapTextureSampler2D(this, class1501, width, height, bool4, pixels);
    }

    @Override
	public
    boolean method13954(GLFormat class1501, GLActionFormat class762) {
        return true;
    }

    @Override
    public void setMatrixTransform(Matrix44 matrix441, Matrix44 matrix442, Matrix44 matrix443) {
        OpenGL.glMatrixMode(GL_MODELVIEW);
        super.aClass3848683.calculatePerspective(matrix441, matrix442);
        OpenGL.glLoadMatrixf(super.aClass3848683.faceList, 0);
        OpenGL.glMatrixMode(5889);
        OpenGL.glLoadMatrixf(matrix443.faceList, 0);
    }


    @Override
    void setLightPosition() {
        OpenGL.glMatrixMode(GL_MODELVIEW);
        OpenGL.glLoadMatrixf(super.particleMatrix.faceList, 0);
        OpenGL.glLightfv(GL_LIGHT0, GL_POSITION, super.sunPosition, 0);
        OpenGL.glLightfv(GL_LIGHT1, GL_POSITION, super.lightPosition, 0);
    }

    @Override
    void setScissorRect() {
        if (super.display != null) {
            int x = super.anInt8823 + super.cLeft;
            int y = super.anInt8754 + super.display.getHeight() - super.cBottom;
            int width = super.cRight - super.cLeft;
            int height = super.cBottom - super.cTop;
            if (width < 0) {
                width = 0;
            }

            if (height < 0) {
                height = 0;
            }

            OpenGL.glScissor(x, y, width, height);
        }
    }

    @Override
    void toggleAlpha() {
        if (super.enableAlphaTest) {
            OpenGL.glEnable(GL_ALPHA_TEST);
        } else {
            OpenGL.glDisable(GL_ALPHA_TEST);
        }
    }

    @Override
    public RenderBuffer getRenderBuffer(int width, int height) {
        return new OpenGlNativeRenderBuffer(this, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, width, height);
    }



    @Override
    void setTextureStageState(int src, TextureStageState state) {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, src + GL_SOURCE0_RGB, method15567(state));
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, src + GL_OPERAND0_RGB, 768);
    }

    @Override
	public
    NativeTextureSampler createTextureSampler(int i_1, int i_2, boolean bool3, int[] ints_4, int i_5, int i_6) {
        if (!this.GLARBTextureNonPowerOfTwo && (!OpenGlTexture.isPowerOfTwo(i_1) || !OpenGlTexture.isPowerOfTwo(i_2))) {
            if (this.GLARBTextureRectangle) {
                return new MipmapRectangleTextureSampler(this, i_1, i_2, ints_4, i_5, i_6);
            } else {
                MipmapTextureSampler2D class319Sub27 = new MipmapTextureSampler2D(this, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, MathUtils.nextPowerOfTwo(i_1), MathUtils.nextPowerOfTwo(i_2));
                class319Sub27.method48(0, 0, i_1, i_2, ints_4, i_5, i_6);
                return class319Sub27;
            }
        } else {
            return new MipmapTextureSampler2D(this, i_1, i_2, bool3, ints_4, i_5, i_6);
        }
    }

    @Override
    public void renderLastFrame() {
        if (super.display != null) {
            int displayWidth = super.display.getWidth();
            int displayHeight = super.display.getHeight();
            if (displayWidth > 0 || displayHeight > 0) {
                int i_3 = super.anInt8749;
                int i_4 = super.anInt8776;
                int i_5 = super.anInt8751;
                int i_6 = super.anInt8752;
                calculateViewportCenter();
                int i_7 = super.cLeft;
                int i_8 = super.cRight;
                int i_9 = super.cTop;
                int i_10 = super.cBottom;
                resetClipping();
                OpenGL.glReadBuffer(GL_FRONT);
                OpenGL.glDrawBuffer(GL_BACK);
                method13934();
                toggleFog(false);
                toggleLights(false);
                toggleDepthTest(false);
                method13942(false);
                setTextureSampler(null);
                setColourType(1);
                toggleBlending(0);
                OpenGL.glMatrixMode(GL_PROJECTION);
                OpenGL.glLoadIdentity();
                OpenGL.glOrtho(0.0D, 1.0D, 0.0D, 1.0D, -1.0, 1.0D);
                OpenGL.glMatrixMode(GL_MODELVIEW);
                OpenGL.glLoadIdentity();
                OpenGL.glRasterPos2i(0, 0);
                OpenGL.glCopyPixels(0, 0, displayWidth, displayHeight, GL_COLOR);
                OpenGL.glFlush();
                OpenGL.glReadBuffer(GL_BACK);
                OpenGL.glDrawBuffer(GL_BACK);
                setClipping(i_7, i_9, i_8, i_10);
                setDrawableArea(i_3, i_4, i_5, i_6);
            }
        }

    }

    @Override
    void setFogColorAndIntensity() {
        this.parameters[0] = super.lightIntensity * super.baseLightColorRed;
        this.parameters[1] = super.lightIntensity * super.baseLightColorGreen;
        this.parameters[2] = super.lightIntensity * super.baseLightColorBlue;
        this.parameters[3] = 1.0F;
        OpenGL.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, this.parameters, 0);
    }

}
