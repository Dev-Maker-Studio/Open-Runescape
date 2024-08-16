package com.jagex.game.runetek5.toolkit;

import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode;
import com.jagex.game.runetek5.toolkit.directx.DXDeviceContextException;
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer;
import com.jagex.game.runetek5.toolkit.directx.DirectXIndexBuffer;
import com.jagex.game.runetek5.toolkit.directx.DirectXStreamSource;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationDX;

import jagdx.*;

import java.awt.*;

import com.jagex.game.runetek5.buffer.DirectXNativeRenderBuffer;
import com.jagex.game.runetek5.cache.node.LinkedList;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.shader.ShaderMappingDX;
import com.jagex.game.runetek5.graphics.shader.Shader;
import com.jagex.game.runetek5.graphics.shader.ShaderDX;
import com.jagex.game.runetek5.graphics.shader.ShaderLoader;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.Direct3DRectangleTexture;
import com.jagex.game.runetek5.graphics.texture.TextureStageState;
import com.jagex.game.runetek5.graphics.texture.TextureState;
import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.Direct3DCubeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.Direct3DTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.Direct3DVolumeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.SampleMode;
import com.jagex.game.runetek5.graphics.texture.sampler.Texture3DSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.TextureDirect3D;
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.imageproducer.DirectXDelegatingDisplay;
import com.jagex.game.runetek5.imageproducer.DirectXDisplay;
import com.jagex.game.runetek5.imageproducer.DirectXDisplayEntry;
import com.jagex.game.runetek5.imageproducer.ImageProducerDX1_Sub1;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.rendering.RenderInfo;
import com.jagex.game.runetek5.scene.FrameRenderException;
import com.jagex.game.runetek5.utils.Utils;

public class DirectXNativeToolkit extends AbstractNativeToolkit {

    public static final int[] autoDepthStencilFormat = {77, 80};
    public static final int[] backBufferFormat = {22, 23};
    public static final int anInt345 = StaticMethods.method755('1');
    public static final int anInt346 = StaticMethods.method755('5');
    public ShaderMappingDX activeShaderMapping;
    public long aLong10246;
    public long renderTarget;
    public final LinkedList displayEntries = new LinkedList();
    public int sceneRenderLoops;
    public long sun;
    public long antiSun;
    public long lightC;
    public boolean lightsEnabled;
    public final float[] activeTextureMatrix = new float[16];
    public int handleCleanupCacheSize = 128;
    public int handlesAwaitingReleaseCount;
    public long[] handlesAwaitingRelease;
    public int anInt10260;
    public int anInt10244;
    public D3DPRESENT_PARAMETERS presentParameters;
    public D3DCAPS capabilities;
    public boolean IS_NO_MIPMAP;
    public boolean IS_NON_POW2;
    public boolean SUPPORTS_CUBE_MAP;
    public boolean[] aBoolArray10255;
    public boolean[] aBoolArray10256;
    public boolean[] availableTextureSlots;
    public SampleMode[] mipmapModes;
    public boolean[] aBoolArray10258;
    public int[] anIntArray10266;
    public Matrix44 aClass38410261;
    public long vertexShaderHandle;
	public DirectXNativeToolkit(int i_1, int i_2, long long_3, long long_5, D3DPRESENT_PARAMETERS d3dpresentParameters7, D3DCAPS d3dcaps8, MaterialProvider interface229, Js5ResourceProvider index10, int i_11) {
        super(interface229, index10, i_11, 0);
        this.handlesAwaitingRelease = new long[this.handleCleanupCacheSize];

        try {
            this.anInt10260 = i_1;
            this.anInt10244 = i_2;
            this.aLong10246 = long_3;
            this.renderTarget = long_5;
            this.presentParameters = d3dpresentParameters7;
            this.capabilities = d3dcaps8;
            this.sun = D3DLIGHT.Create();
            this.antiSun = D3DLIGHT.Create();
            this.lightC = D3DLIGHT.Create();
            super.maxConcurrentTextures = this.capabilities.MaxSimultaneousTextures;
            super.maxActiveLights = this.capabilities.MaxActiveLights > 0 ? this.capabilities.MaxActiveLights : 8;
            this.IS_NO_MIPMAP = (this.capabilities.TextureCaps & 0x2) == 0;
            super.SUPPORTS_TEXTURE3D = (this.capabilities.TextureCaps & 0x2000) != 0;
            this.IS_NON_POW2 = (this.capabilities.TextureCaps & 0x10000) != 0;
            this.SUPPORTS_CUBE_MAP = (this.capabilities.TextureCaps & 0x4000) != 0;
            super.GLEXTFramebufferObject = true;
            super.GLARBMultisample = super.anInt8709 > 0 || IDirect3D.CheckDeviceMultiSampleType(this.aLong10246, this.anInt10260, this.anInt10244, this.presentParameters.BackBufferFormat, true, 2) == 0;
            this.aBoolArray10255 = new boolean[super.maxConcurrentTextures];
            this.aBoolArray10256 = new boolean[super.maxConcurrentTextures];
            this.availableTextureSlots = new boolean[super.maxConcurrentTextures];
            this.mipmapModes = new SampleMode[super.maxConcurrentTextures];
            this.aBoolArray10258 = new boolean[super.maxConcurrentTextures];
            this.anIntArray10266 = new int[super.maxConcurrentTextures];
            Matrix43 matrix44var12 = new Matrix43();
            matrix44var12.setScale(1.0F, -1.0f, 0.5F);
            matrix44var12.translate(0.0F, 0.0F, 0.5F);
            this.aClass38410261 = new Matrix44();
            this.aClass38410261.fromMatrix(matrix44var12);
            IDirect3DDevice.BeginScene(this.renderTarget);
        } catch (Throwable throwable13) {
            throwable13.printStackTrace();
            cleanupAndShutdown();
            throw new RuntimeException("");
        }
    }

    public static AbstractToolkit forCapabilities(Canvas canvas, MaterialProvider materialProvider, Js5ResourceProvider index2, Integer integer3) {
        DirectXNativeToolkit generatedToolkit = null;
        byte b_6 = 0;
        byte b_7 = 1;
        long long_8 = IDirect3D.Direct3DCreate();
        D3DCAPS direct3dcaps = new D3DCAPS();
        IDirect3D.GetDeviceCaps(long_8, b_6, b_7, direct3dcaps);
        if ((direct3dcaps.RasterCaps & 0x1000000) == 0) {
            throw new RuntimeException("");
        } else if (direct3dcaps.MaxSimultaneousTextures < 2) {
            throw new RuntimeException("");
        } else if ((direct3dcaps.TextureOpCaps & 0x2) == 0) {
            throw new RuntimeException("");
        } else if ((direct3dcaps.TextureOpCaps & 0x8) == 0) {
            throw new RuntimeException("");
        } else if ((direct3dcaps.TextureOpCaps & 0x40) == 0) {
            throw new RuntimeException("");
        } else if ((direct3dcaps.TextureOpCaps & 0x200) == 0) {
            throw new RuntimeException("");
        } else if ((direct3dcaps.TextureOpCaps & 0x2000000) == 0) {
            throw new RuntimeException("");
        } else if ((direct3dcaps.SrcBlendCaps & direct3dcaps.DestBlendCaps & 0x10) == 0) {
            throw new RuntimeException("");
        } else if ((direct3dcaps.SrcBlendCaps & direct3dcaps.DestBlendCaps & 0x20) == 0) {
            throw new RuntimeException("");
        } else if ((direct3dcaps.SrcBlendCaps & direct3dcaps.DestBlendCaps & 0x2) == 0) {
            throw new RuntimeException("");
        } else if (direct3dcaps.MaxActiveLights > 0 && direct3dcaps.MaxActiveLights < 2) {
            throw new RuntimeException("");
        } else if (direct3dcaps.MaxStreams < 5) {
            throw new RuntimeException("");
        } 
        D3DPRESENT_PARAMETERS params = new D3DPRESENT_PARAMETERS(canvas);
        params.Windowed = true;
        params.EnableAutoDepthStencil = true;
        params.BackBufferWidth = canvas.getWidth();
        params.BackBufferHeight = canvas.getHeight();
        params.BackBufferCount = 1;
        params.PresentationInterval = Integer.MIN_VALUE;
        if (!supports(b_6, b_7, long_8, integer3.intValue(), params)) {
            throw new RuntimeException("");
        }
        int deviceContextFlags = 0;
        if ((direct3dcaps.DevCaps & 0x100000) != 0) {
            deviceContextFlags |= 0x10;
        }
        long contextUid = 0L;
        try {
            contextUid = IDirect3D.CreateDeviceContext(long_8, b_6, b_7, canvas, deviceContextFlags | 0x40, params);
        } catch (DXDeviceContextException runtimeexceptionSub117) {
            contextUid = IDirect3D.CreateDeviceContext(long_8, b_6, b_7, canvas, deviceContextFlags & -1048577 | 0x20, params);
        }
        generatedToolkit = new DirectXNativeToolkit(b_6, b_7, long_8, contextUid, params, direct3dcaps, materialProvider, index2, integer3.intValue());
        if (!generatedToolkit.canvasToProducerMap.containsKey(canvas)) {
            AbstractToolkit.setIgnoreRepaint(canvas);
            generatedToolkit.addImageProducer(canvas, new DirectXDelegatingDisplay(generatedToolkit, canvas, canvas.getWidth(), canvas.getHeight(), true));
        }
        generatedToolkit.setActiveCanvas(canvas);
        generatedToolkit.initializeTables();
        return generatedToolkit;
    }

    public static int getTextureStageStateFlags(TextureStageState textureStageState) {
        switch (textureStageState.state) {
            case 0:
                return 3;
            case 1:
                return 0;
            case 2:
                return 2;
            case 3:
                return 1;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static int method15652(GLFormat class1500, GLActionFormat class761) {
        switch (class761.anInt757) {
            case 3:
                if (GLFormat.HDR == class1500) {
                    return 116;
                }
                break;
            case 4:
                switch (class1500.formatType) {
                    case 0:
                        return 50;
                    case 1:
                        return 28;
                    case 2:
                    case 3:
                    default:
                        break;
                    case 4:
                        return 51;
                    case 5:
                        return 22;
                    case 6:
                        return 21;
                    case 7:
                        return anInt345;
                    case 8:
                        return 77;
                    case 9:
                        return anInt346;
                }
            case 5:
            case 6:
            case 7:
            default:
                break;
            case 8:
                if (GLFormat.HDR == class1500) {
                    return 113;
                }
        }

        throw new IllegalArgumentException("");
    }

    public static int method15654(PrimitiveDrawMode class3520) {
        switch (class3520.mode) {
            case 0:
                return 3;
            case 1:
                return 2;
            case 2:
                return 5;
            case 3:
                return 6;
            case 4:
                return 4;
            case 5:
                return 1;
            default:
                throw new IllegalArgumentException("");
        }
    }

    public static boolean supports(int i_0, int i_1, long long_2, int i_4, D3DPRESENT_PARAMETERS parameter) {
        int i_6 = 0;
        int i_7 = 0;
        int i_8 = 0;

        try {
            D3DDISPLAYMODE displayMode = new D3DDISPLAYMODE();
            if (Utils.belowZero(IDirect3D.GetAdapterDisplayMode(long_2, i_0, displayMode))) {
                return false;
            } else {
                label74:
                for (; i_4 >= 0; --i_4) {
                    if (i_4 != 1) {
                        i_8 = i_4;

                        for (int i_11 = 0; i_11 < backBufferFormat.length; i_11++) {
                            if (IDirect3D.CheckDeviceType(long_2, i_0, i_1, displayMode.Format, backBufferFormat[i_11], true) == 0 && IDirect3D.CheckDeviceFormat(long_2, i_0, i_1, displayMode.Format, 1, 1, backBufferFormat[i_11]) == 0 && (i_4 == 0 || IDirect3D.CheckDeviceMultiSampleType(long_2, i_0, i_1, backBufferFormat[i_11], true, i_8) == 0)) {
                                for (int i_12 = 0; i_12 < autoDepthStencilFormat.length; i_12++) {
                                    if (IDirect3D.CheckDeviceFormat(long_2, i_0, i_1, displayMode.Format, 2, 1, autoDepthStencilFormat[i_12]) == 0 && IDirect3D.CheckDepthStencilMatch(long_2, i_0, i_1, displayMode.Format, backBufferFormat[i_11], autoDepthStencilFormat[i_12]) == 0 && (i_4 == 0 || IDirect3D.CheckDeviceMultiSampleType(long_2, i_0, i_1, autoDepthStencilFormat[i_11], true, i_8) == 0)) {
                                        i_7 = backBufferFormat[i_11];
                                        i_6 = autoDepthStencilFormat[i_12];
                                        break label74;
                                    }
                                }
                            }
                        }
                    }
                }

                if (i_4 >= 0 && i_7 != 0 && i_6 != 0) {
                    parameter.BackBufferFormat = i_7;
                    parameter.AutoDepthStencilFormat = i_6;
                    parameter.MultiSampleType = i_8;
                    parameter.MultiSampleQuality = 0;
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Throwable throwable) {
            return false;
        }
    }

    public static int method15661(TextureCombineMode class680) {
        switch (class680.mode) {
            case 0:
                return 7;
            case 1:
                return 10;
            case 2:
                return 2;
            case 3:
                return 26;
            case 4:
                return 4;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static int method15667(TextureState class370) {
        switch (class370.type) {
            case 1:
                return 2;
            case 2:
                return 1;
            case 3:
                return 3;
            case 4:
                return 256;
            case 5:
                return 4;
            default:
                return 0;
        }
    }

    @Override
    public boolean supportsArbFragmentShader() {
        return (this.capabilities.PixelShaderVersion & 0xffff) >= 257;
    }

    @Override
    void deallocate() {
        for (Node node = this.displayEntries.getTail(); node != null; node = this.displayEntries.getPrevious()) {
            DirectXDisplayEntry displayEntry = (DirectXDisplayEntry) node;
            DirectXDisplay display = displayEntry.directxDisplay;
            display.cleanup();
        }

        super.deallocate();
    }

    public boolean bindRenderTarget() {
        int level = IDirect3DDevice.TestCooperativeLevel(this.renderTarget);
        if (level == 0 || level == -2005530519) {
            IDirect3DDevice.SetDepthStencilSurface(this.renderTarget, 0L);

            for (int renderTargetIndex = 0; renderTargetIndex < 4; renderTargetIndex++) {
                IDirect3DDevice.SetRenderTarget(this.renderTarget, renderTargetIndex, 0L);
            }

            for (int streamNumber = 0; streamNumber < 4; streamNumber++) {
                IDirect3DDevice.SetStreamSource(this.renderTarget, streamNumber, 0L, 0, 0);
            }

            for (int slot = 0; slot < 4; slot++) {
                IDirect3DDevice.SetTexture(this.renderTarget, slot, 0L);
            }

            IDirect3DDevice.SetIndices(this.renderTarget, 0L);
            deallocate();
            this.presentParameters.BackBufferWidth = 0;
            this.presentParameters.BackBufferHeight = 0;
            if (supports(this.anInt10260, this.anInt10244, this.aLong10246, super.anInt8709, this.presentParameters)) {
            	int result = IDirect3DDevice.Reset(this.renderTarget, this.presentParameters);
                if (Utils.equalOrAboveZero(result)) {
                    method13899();
                    setupGraphics();
                    return true;
                }
                System.out.println("[ToolkitDX_Sub2] Exit");
                System.exit(0);
            }
        }

        return false;
    }

    @Override
    void setupGraphics() {
        for (int textureIndex = 0; textureIndex < super.maxConcurrentTextures; textureIndex++) {
            IDirect3DDevice.SetSamplerState(this.renderTarget, textureIndex, 7, 2);
            IDirect3DDevice.SetSamplerState(this.renderTarget, textureIndex, 6, 2);
            IDirect3DDevice.SetSamplerState(this.renderTarget, textureIndex, 5, 2);
            IDirect3DDevice.SetSamplerState(this.renderTarget, textureIndex, 1, 1);
            IDirect3DDevice.SetSamplerState(this.renderTarget, textureIndex, 2, 1);
            this.mipmapModes[textureIndex] = SampleMode.LINEAR;
            boolean[] bools_2 = this.aBoolArray10255;
            this.aBoolArray10256[textureIndex] = true;
            bools_2[textureIndex] = true;
            this.aBoolArray10258[textureIndex] = false;
            this.anIntArray10266[textureIndex] = 0;
        }

        IDirect3DDevice.SetTextureStageState(this.renderTarget, 0, 6, 1);
        IDirect3DDevice.SetRenderState(this.renderTarget, 9, 2);
        IDirect3DDevice.SetRenderState(this.renderTarget, 23, 4);
        IDirect3DDevice.SetRenderState(this.renderTarget, 25, 5);
        IDirect3DDevice.SetRenderState(this.renderTarget, 24, 0);
        IDirect3DDevice.SetRenderState(this.renderTarget, 22, 2);
        IDirect3DDevice.SetRenderState(this.renderTarget, 147, 1);
        IDirect3DDevice.SetRenderState(this.renderTarget, 145, 1);
        IDirect3DDevice.setRenderStateF(this.renderTarget, 38, 0.95F);
        IDirect3DDevice.SetRenderState(this.renderTarget, 35, 3);
        IDirect3DDevice.setRenderStateF(this.renderTarget, 154, 1.0F);
        D3DLIGHT.SetType(this.sun, 3);
        D3DLIGHT.SetType(this.antiSun, 3);
        D3DLIGHT.SetType(this.lightC, 1);
        IDirect3DDevice.SetRenderState(this.renderTarget, 206, 1);
        this.lightsEnabled = false;
        super.setupGraphics();
    }

    @Override
    public RenderInfo getRenderInfo() {
        D3DADAPTER_IDENTIFIER adapter = new D3DADAPTER_IDENTIFIER();
        IDirect3D.GetAdapterIdentifier(this.aLong10246, this.anInt10260, 0, adapter);
        return new RenderInfo(adapter.VendorID, "Direct3D", 9, adapter.Description, adapter.DriverVersion);
    }

    @Override
    public void render() throws FrameRenderException {
        IDirect3DDevice.EndScene(this.renderTarget);
        int i_3 = super.delegatingDisplay.draw();
        if (Utils.belowZero(i_3)) {
            if (i_3 != -2005530520) {
                if (++this.sceneRenderLoops > 50) {
                    throw new FrameRenderException();
                }
            } else {
                super.delegatingDisplay.cleanup();
                bindRenderTarget();
                ((DirectXDelegatingDisplay) super.delegatingDisplay).setWindowed();
            }
        } else {
            this.sceneRenderLoops = 0;
        }

        IDirect3DDevice.BeginScene(this.renderTarget);
        if (super.materialProvider != null) {
            super.materialProvider.cycle();
        }
    }

    @Override
    public void awaitRenderCompletion() {
        long long_1 = IDirect3DDevice.CreateEventQuery(this.renderTarget);
        if (Utils.equalOrAboveZero(IDirect3DEventQuery.Issue(long_1))) {
            while (true) {
                int i_3 = IDirect3DEventQuery.IsSignaled(long_1);
                if (i_3 != 1) {
                    break;
                }
                Thread.yield();
            }
        }
        IUnknown.Release(long_1);
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public DirectDisplay getImageProducer() {
        return new ImageProducerDX1_Sub1(this);
    }

    @Override
    public DelegatingDisplay createDelegatingDisplay(Canvas canvas1, int i_2, int i_3) {
        return new DirectXDelegatingDisplay(this, canvas1, i_2, i_3, false);
    }

    @Override
    public RenderBuffer getRenderBuffer(int width, int height) {
        return new DirectXNativeRenderBuffer(this, GLActionFormat.DEPTH_COMPONENT_24, width, height);
    }

    public DirectXDisplayEntry method15641(DirectXDisplay interface331) {
        for (Node node2 = this.displayEntries.getTail(); node2 != null; node2 = this.displayEntries.getPrevious()) {
            DirectXDisplayEntry class282Sub93 = (DirectXDisplayEntry) node2;
            if (interface331 == class282Sub93.directxDisplay) {
                return class282Sub93;
            }
        }

        return null;
    }

    public void method15642(DirectXDisplay interface331) {
        if (method15641(interface331) == null) {
            this.displayEntries.insertTail(new DirectXDisplayEntry(interface331));
        }

    }

    public void release(DirectXDisplay interface331) {
        DirectXDisplayEntry class282Sub92 = method15641(interface331);
        if (class282Sub92 != null) {
            class282Sub92.unlink();
        }

    }

    @Override
    void enableLights() {
        if (super.enableLights && !this.lightsEnabled) {
            IDirect3DDevice.LightEnable(this.renderTarget, 0, false);
            IDirect3DDevice.LightEnable(this.renderTarget, 1, false);
            IDirect3DDevice.SetLight(this.renderTarget, 0, this.sun);
            IDirect3DDevice.SetLight(this.renderTarget, 1, this.antiSun);
            IDirect3DDevice.LightEnable(this.renderTarget, 0, true);
            IDirect3DDevice.LightEnable(this.renderTarget, 1, true);
            this.lightsEnabled = true;
        }
    }

    @Override
    public void setVertexDeclaration(VertexDeclaration vertexDeclaration) {
        VertexDeclarationDX dxVertexDeclaration = (VertexDeclarationDX) vertexDeclaration;
        IDirect3DDevice.SetVertexDeclaration(this.renderTarget, dxVertexDeclaration.pointer);
    }

    @Override
    public void applyClearFlags(int i_1, int i_2) {
        IDirect3DDevice.Clear(this.renderTarget, i_1, i_2, 1.0F, 0);
    }

    @Override
    void setViewport() {
        if (super.display != null) {
            IDirect3DDevice.SetViewport(this.renderTarget, super.anInt8823 + super.anInt8749, super.anInt8754 + super.anInt8776, super.anInt8751, super.anInt8752, super.depthRangeNear, super.depthRangeFar);
        }
    }

    @Override
    void setScissorRect() {
        IDirect3DDevice.SetScissorRect(this.renderTarget, super.anInt8823 + super.cLeft, super.anInt8754 + super.cTop, super.cRight, super.cBottom);
    }

    @Override
    void setTextureStageState(int i_1, TextureStageState stageState) {
        if (super.enableLights) {
            byte mask = 0;
            byte b_6;
            switch (i_1) {
                case 1:
                    b_6 = 3;
                    break;
                case 2:
                    b_6 = 26;
                    break;
                default:
                    b_6 = 2;
            }

            IDirect3DDevice.SetTextureStageState(this.renderTarget, super.currentTextureSlot, b_6, getTextureStageStateFlags(stageState) | mask);
        }

    }

    public void setPixelShader(long long_1) {
        IDirect3DDevice.SetPixelShader(this.renderTarget, long_1);
    }

    @Override
    public void setMatrixTransform(Matrix44 matrix441, Matrix44 matrix442, Matrix44 matrix443) {
        IDirect3DDevice.SetTransform(this.renderTarget, 256, matrix441.faceList);
        IDirect3DDevice.SetTransform(this.renderTarget, 2, matrix442.faceList);
        IDirect3DDevice.SetTransform(this.renderTarget, 3, matrix443.faceList);
    }

    @Override
    public void loadWorldViewProjectionMatrix(Matrix44 matrix441) {
        matrix441.multiplyBy(this.aClass38410261);
    }

    @Override
    void toggleDepthTest() {
        IDirect3DDevice.setRenderStateB(this.renderTarget, 7, super.enableDepthTest);
    }

    @Override
    void setDepthRenderState() {
        IDirect3DDevice.setRenderStateB(this.renderTarget, 14, super.depthBufferingEnabled && super.aBool8756);
    }

    @Override
    public DirectXBuffer method13993(boolean bool1) {
        return new DirectXIndexBuffer(this, GLActionFormat.TEXTURE_FORMAT, bool1);
    }

    @Override
    void setSunParameters() {
        if (super.enableLights) {
            float sunIntensity = super.sunEnabled ? super.sunColorIntensity : 0.0F;
            float antiSunIntensity = super.sunEnabled ? -super.antiSunColorIntensity : 0.0F;
            D3DLIGHT.SetDiffuse(this.sun, sunIntensity * super.baseLightColorRed, sunIntensity * super.baseLightColorGreen, sunIntensity * super.baseLightColorBlue, 0.0F);
            D3DLIGHT.SetDiffuse(this.antiSun, antiSunIntensity * super.baseLightColorRed, antiSunIntensity * super.baseLightColorGreen, antiSunIntensity * super.baseLightColorBlue, 0.0F);
            this.lightsEnabled = false;
        }
    }

    @Override
    void setLightPosition() {
        if (super.enableLights) {
            D3DLIGHT.SetDirection(this.sun, -super.sunPosition[0], -super.sunPosition[1], -super.sunPosition[2]);
            D3DLIGHT.SetDirection(this.antiSun, -super.lightPosition[0], -super.lightPosition[1], -super.lightPosition[2]);
            this.lightsEnabled = false;
        }

    }

    @Override
    void toggleSun() {
        setSunParameters();
        enableLights();
    }

    @Override
    public void renderLastFrame() {
    }

    @Override
    void configureLights() {
        int lightIndex;
        for (lightIndex = 0; lightIndex < super.lastSceneLightCount; lightIndex++) {
            PointLight lightnode = super.lightSources[lightIndex];
            int lightHandle = lightIndex + 2;
            int lightColor = lightnode.getColor();
            float lightTransparency = lightnode.getAlpha() / 255.0F;
            D3DLIGHT.SetPosition(this.lightC, lightnode.getX(), lightnode.getY(), lightnode.getZ());
            D3DLIGHT.SetDiffuse(this.lightC, (lightColor >> 16 & 0xff) * lightTransparency, (lightColor >> 8 & 0xff) * lightTransparency, (lightColor & 0xff) * lightTransparency, 0.0F);
            D3DLIGHT.SetAttenuation(this.lightC, 0.0F, 0.0F, 1.0F / (lightnode.getSize2d() * lightnode.getSize2d()));
            D3DLIGHT.SetRange(this.lightC, lightnode.getSize2d());
            IDirect3DDevice.SetLight(this.renderTarget, lightHandle, this.lightC);
            IDirect3DDevice.LightEnable(this.renderTarget, lightHandle, true);
        }
        while (lightIndex < super.currentSceneLightCount) {
            IDirect3DDevice.LightEnable(this.renderTarget, lightIndex + 2, false);
            ++lightIndex;
        }
    }

    @Override
	public
    boolean method13955(GLFormat class1501, GLActionFormat class762) {
        D3DDISPLAYMODE d3ddisplaymode3 = new D3DDISPLAYMODE();
        return Utils.equalOrAboveZero(IDirect3D.GetAdapterDisplayMode(this.aLong10246, this.anInt10260, d3ddisplaymode3)) && Utils.equalOrAboveZero(IDirect3D.CheckDeviceFormat(this.aLong10246, this.anInt10260, this.anInt10244, d3ddisplaymode3.Format, 0, 4, method15652(class1501, class762)));
    }

    @Override
	public
    NativeTextureSampler createTextureSampler(int i_1, int i_2, boolean bool3, int[] ints_4, int i_5, int i_6) {
        return new TextureDirect3D(this, i_1, i_2, bool3, ints_4, i_5, i_6);
    }

    @Override
    NativeTextureSampler createNativeTexture(GLFormat class1501, int i_2, int i_3, float[] floats_5) {
        return new TextureDirect3D(this, class1501, i_2, i_3, floats_5);
    }

    @Override
	public
    CubeTextureSampler generateNativeCubeTexture(int i_1, boolean bool2, int[][] ints_3) {
        return new Direct3DCubeTextureSampler(this, i_1, bool2, ints_3);
    }

    @Override
    public Texture3DSampler method13962(GLFormat class1501, byte[] bytes_6) {
        return new Direct3DVolumeTextureSampler(this, class1501, bytes_6);
    }

    public void setTextureSamplerState(TextureDirect3D class360Sub21) {
        method15662(class360Sub21);
        if (this.aBoolArray10255[super.currentTextureSlot] != class360Sub21.aBool9156) {
            IDirect3DDevice.SetSamplerState(this.renderTarget, super.currentTextureSlot, 1, class360Sub21.aBool9156 ? 1 : 3);
            this.aBoolArray10255[super.currentTextureSlot] = class360Sub21.aBool9156;
        }

        if (this.aBoolArray10256[super.currentTextureSlot] != class360Sub21.aBool9155) {
            IDirect3DDevice.SetSamplerState(this.renderTarget, super.currentTextureSlot, 2, class360Sub21.aBool9155 ? 1 : 3);
            this.aBoolArray10256[super.currentTextureSlot] = class360Sub21.aBool9155;
        }

    }

    @Override
    void setTextureMatrix() {
        if (this.vertexShaderHandle == 0L && super.textureStates[super.currentTextureSlot] != TextureState.INITIALIZE) {
            if (super.textureStates[super.currentTextureSlot] == TextureState.PARTIAL_COPY_LAST) {
                IDirect3DDevice.SetTransform(this.renderTarget, super.currentTextureSlot + 16, super.textureMatrices[super.currentTextureSlot].partialCopyInto(this.activeTextureMatrix));
            } else {
                IDirect3DDevice.SetTransform(this.renderTarget, super.currentTextureSlot + 16, super.textureMatrices[super.currentTextureSlot].copyInto(this.activeTextureMatrix));
            }

            int i_1 = method15667(super.textureStates[super.currentTextureSlot]);
            if (i_1 != this.anIntArray10266[super.currentTextureSlot]) {
                IDirect3DDevice.SetTextureStageState(this.renderTarget, super.currentTextureSlot, 24, i_1);
                this.anIntArray10266[super.currentTextureSlot] = i_1;
            }
        } else {
            IDirect3DDevice.SetTextureStageState(this.renderTarget, super.currentTextureSlot, 24, 0);
            this.anIntArray10266[super.currentTextureSlot] = 0;
        }

    }

    @Override
    void activateCurrentTexture() {
    }

    @Override
    void setTextureColorStage() {
        if (super.enableLights) {
            int i_1 = this.availableTextureSlots[super.currentTextureSlot] ? method15661(super.textureRgbCombineModes[super.currentTextureSlot]) : 1;
            IDirect3DDevice.SetTextureStageState(this.renderTarget, super.currentTextureSlot, 1, i_1);
        }
    }

    @Override
    void setTextureAlphaStage() {
        if (super.enableLights) {
            int i_1 = this.availableTextureSlots[super.currentTextureSlot] ? method15661(super.textureAlphaCombineModes[super.currentTextureSlot]) : 1;
            IDirect3DDevice.SetTextureStageState(this.renderTarget, super.currentTextureSlot, 4, i_1);
        }
    }

    @Override
    void method13970(int i_1, TextureStageState class672) {
        if (super.enableLights) {
            byte b_4 = 0;
            byte b_5;
            switch (i_1) {
                case 1:
                    b_5 = 6;
                    break;
                case 2:
                    b_5 = 27;
                    break;
                default:
                    b_5 = 5;
            }

            IDirect3DDevice.SetTextureStageState(this.renderTarget, super.currentTextureSlot, b_5, getTextureStageStateFlags(class672) | b_4);
        }

    }

    @Override
    void setTextureEnvironmentColor() {
        if (super.enableLights) {
            IDirect3DDevice.SetRenderState(this.renderTarget, 60, super.envColor);
        }
    }

    @Override
	public
    void colorMask(int i_1) {
        IDirect3DDevice.SetRenderState(this.renderTarget, 168, i_1);
    }

    @Override
    void toggleAlpha() {
        IDirect3DDevice.setRenderStateB(this.renderTarget, 15, super.enableAlphaTest);
    }

    @Override
    void blendDrawBuffer() {
        switch (super.blendFuncMode.mode) {
            case 0:
                IDirect3DDevice.SetRenderState(this.renderTarget, 19, 2);
                IDirect3DDevice.SetRenderState(this.renderTarget, 20, 2);
                break;
            case 1:
                IDirect3DDevice.SetRenderState(this.renderTarget, 19, 5);
                IDirect3DDevice.SetRenderState(this.renderTarget, 20, 6);
                break;
            case 2:
                IDirect3DDevice.SetRenderState(this.renderTarget, 19, 2);
                IDirect3DDevice.SetRenderState(this.renderTarget, 20, 1);
                break;
            case 3:
                IDirect3DDevice.SetRenderState(this.renderTarget, 19, 9);
                IDirect3DDevice.SetRenderState(this.renderTarget, 20, 2);
        }

        switch (super.anInt8844) {
            case 0:
                IDirect3DDevice.SetRenderState(this.renderTarget, 207, 1);
                IDirect3DDevice.SetRenderState(this.renderTarget, 208, 1);
                break;
            case 1:
                IDirect3DDevice.SetRenderState(this.renderTarget, 207, 2);
                IDirect3DDevice.SetRenderState(this.renderTarget, 208, 1);
                break;
            case 2:
                IDirect3DDevice.SetRenderState(this.renderTarget, 207, 2);
                IDirect3DDevice.SetRenderState(this.renderTarget, 208, 2);
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
            IDirect3DDevice.setRenderStateF(this.renderTarget, 36, super.fogStart);
            IDirect3DDevice.setRenderStateF(this.renderTarget, 37, super.fogEnd);
            IDirect3DDevice.SetRenderState(this.renderTarget, 34, super.fogColorRgb);
        }

    }

    public synchronized void requestHandleCleanup(long handle) {
        if (this.handlesAwaitingReleaseCount == this.handleCleanupCacheSize) {
            this.handleCleanupCacheSize *= 2;
            long[] longs_3 = new long[this.handleCleanupCacheSize];
            System.arraycopy(this.handlesAwaitingRelease, 0, longs_3, 0, this.handlesAwaitingReleaseCount);
            this.handlesAwaitingRelease = longs_3;
        }

        this.handlesAwaitingRelease[this.handlesAwaitingReleaseCount++] = handle;
    }

    @Override
	public
    boolean method13954(GLFormat class1501, GLActionFormat class762) {
        D3DDISPLAYMODE d3ddisplaymode3 = new D3DDISPLAYMODE();
        return Utils.equalOrAboveZero(IDirect3D.GetAdapterDisplayMode(this.aLong10246, this.anInt10260, d3ddisplaymode3)) && Utils.equalOrAboveZero(IDirect3D.CheckDeviceFormat(this.aLong10246, this.anInt10260, this.anInt10244, d3ddisplaymode3.Format, 0, 3, method15652(class1501, class762)));
    }

    @Override
    void toggleLights() {
        if (super.enableLights) {
            IDirect3DDevice.setRenderStateB(this.renderTarget, 137, super.aBool8684 && !super.aBool8760);
        }

    }

    @Override
	public VertexDeclaration method13995(VertexDeclarationComponent[] arr_1) {
        return new VertexDeclarationDX(this, arr_1);
    }

    @Override
    public float method13905() {
        return -0.5f;
    }

    @Override
    public void setStreamSource(int i_1, NativeStream interface42) {
        DirectXStreamSource class3003 = (DirectXStreamSource) interface42;
        IDirect3DDevice.SetStreamSource(this.renderTarget, i_1, class3003.handle, 0, class3003.method5317());
    }

    @Override
    public void method13997(DirectXBuffer interface321) {
        IDirect3DDevice.SetIndices(this.renderTarget, ((DirectXIndexBuffer) interface321).handle);
    }

    @Override
    public void drawPrimitive(PrimitiveDrawMode class3521, int i_2, int i_3) {
        if (this.activeShaderMapping != null) {
            this.activeShaderMapping.method14591();
        }

        IDirect3DDevice.DrawPrimitive(this.renderTarget, method15654(class3521), i_2, i_3);
    }

    @Override
    public void drawIndexedPrimitive(PrimitiveDrawMode class3521, int i_2, int i_3, int i_4, int i_5) {
        if (this.activeShaderMapping != null) {
            this.activeShaderMapping.method14591();
        }

        IDirect3DDevice.DrawIndexedPrimitive(this.renderTarget, method15654(class3521), 0, i_2, i_3, i_4, i_5);
    }

    public byte[] getShaderData(String fileName) {
        return fetchShader("dx", fileName);
    }

    @Override
    public Shader loadShader(String name) {
        byte[] shaderData = getShaderData(name);
        if (shaderData == null) {
            return null;
        } 
        ShaderLoader shaderLoader = createShader(shaderData);
        return new ShaderDX(this, shaderLoader);
    }

    @Override
    public synchronized void animateTextures(int i_1) {
        for (int i_2 = 0; i_2 < this.handlesAwaitingReleaseCount; i_2++) {
            IUnknown.Release(this.handlesAwaitingRelease[i_2]);
        }

        this.handlesAwaitingReleaseCount = 0;
        super.animateTextures(i_1);
    }

    public void method15657(Direct3DVolumeTextureSampler class360Sub11) {
        method15662(class360Sub11);
        if (!this.aBoolArray10255[super.currentTextureSlot]) {
            IDirect3DDevice.SetSamplerState(this.renderTarget, super.currentTextureSlot, 1, 1);
            this.aBoolArray10255[super.currentTextureSlot] = true;
        }

        if (!this.aBoolArray10256[super.currentTextureSlot]) {
            IDirect3DDevice.SetSamplerState(this.renderTarget, super.currentTextureSlot, 2, 1);
            this.aBoolArray10256[super.currentTextureSlot] = true;
        }

    }

    @Override
    public void setPolygonMode(int i_1) {
        int i_2 = (i_1 & 0x2) != 0 ? 2 : 3;
        IDirect3DDevice.SetRenderState(this.renderTarget, 8, i_2);
    }

    @Override
    void toggleMultisampling(boolean bool1) {
        IDirect3DDevice.setRenderStateB(this.renderTarget, 161, bool1);
    }

    @Override
    void setFogColorAndIntensity() {
        if (super.enableLights) {
            D3DLIGHT.SetAmbient(this.sun, super.baseLightColorRed * super.lightIntensity, super.baseLightColorGreen * super.lightIntensity, super.baseLightColorBlue * super.lightIntensity, 0.0F);
            this.lightsEnabled = false;
        }
    }


    @Override
    NativeTextureSampler createNativeTextureSampler(GLFormat class1501, int i_2, int i_3, boolean bool4, byte[] bytes_5) {
        return new TextureDirect3D(this, class1501, i_2, i_3, bool4, bytes_5);
    }



    public void method15662(Direct3DTextureSampler class3601) {
        IDirect3DDevice.SetTexture(this.renderTarget, super.currentTextureSlot, class3601.getHandle());
        if (super.enableLights && !this.availableTextureSlots[super.currentTextureSlot]) {
            this.availableTextureSlots[super.currentTextureSlot] = true;
            setTextureAlphaStage();
            setTextureColorStage();
        }

    }


    @Override
    public AbstractCubeTextureGenerator drawQuad(AbstractCubeTextureGenerator class1521, AbstractCubeTextureGenerator class1522, float f_3, AbstractCubeTextureGenerator class1524) {
        return f_3 < 0.5F ? class1521 : class1522;
    }


    @Override
    public void debugModeChanged() {
    }

    public void setVertexShader(long handle) {
        this.vertexShaderHandle = handle;
        IDirect3DDevice.SetVertexShader(this.renderTarget, handle);
    }

    @Override
    void method13899() {
        for (Node node1 = this.displayEntries.getTail(); node1 != null; node1 = this.displayEntries.getPrevious()) {
            DirectXDisplayEntry class282Sub92 = (DirectXDisplayEntry) node1;
            DirectXDisplay interface333 = class282Sub92.directxDisplay;
            interface333.setWindowed();
            if (interface333 == super.display) {
                interface333.reload();
            }
        }

        super.method13899();
    }

    @Override
    void toggleScissorTest() {
        IDirect3DDevice.setRenderStateB(this.renderTarget, 174, super.boundsCalculated);
    }

    @Override
    void toggleFog() {
        if (super.enableLights) {
            IDirect3DDevice.setRenderStateB(this.renderTarget, 28, super.fogSupported && super.fogEnabled && super.fogDistance >= 0);
        }

    }

    @Override
    void toggleBlending() {
        IDirect3DDevice.setRenderStateB(this.renderTarget, 27, super.enableBlending);
    }

    @Override
	public
    NativeTextureSampler method14072(GLFormat class1501, GLActionFormat class762, int i_3, int i_4) {
        return new TextureDirect3D(this, class1501, class762, i_3, i_4);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (this.sun != 0L) {
            D3DLIGHT.Destroy(this.sun);
            this.sun = 0L;
        }

        if (this.antiSun != 0L) {
            D3DLIGHT.Destroy(this.antiSun);
            this.antiSun = 0L;
        }

        if (this.lightC != 0L) {
            D3DLIGHT.Destroy(this.lightC);
            this.lightC = 0L;
        }

        if (this.renderTarget != 0L) {
            IDirect3DDevice.Destroy(this.renderTarget);
            this.renderTarget = 0L;
        }

        if (this.aLong10246 != 0L) {
            IUnknown.Release(this.aLong10246);
            this.aLong10246 = 0L;
        }

    }

    @Override
    public void setTextureOnCurrentSlot() {
        if (this.availableTextureSlots[super.currentTextureSlot]) {
            this.availableTextureSlots[super.currentTextureSlot] = false;
            IDirect3DDevice.SetTexture(this.renderTarget, super.currentTextureSlot, 0L);
            setTextureAlphaStage();
            setTextureColorStage();
        }
    }

    @Override
    public int[] copyPixelRectangle(int i_1, int i_2, int i_3, int i_4) {
        int[] ints_5 = null;
        long long_6 = IDirect3DDevice.GetRenderTarget(this.renderTarget, 0);
        long long_8 = IDirect3DDevice.CreateRenderTarget(this.renderTarget, i_3, i_4, 21, 0, 0, true);
        if (Utils.equalOrAboveZero(IDirect3DDevice.StretchRect(this.renderTarget, long_6, i_1, i_2, i_3, i_4, long_8, 0, 0, i_3, i_4, 1))) {
            ints_5 = new int[i_3 * i_4];
            IDirect3DSurface.Download(long_8, 0, 0, i_3, i_4, i_3 * 4, 16, super.directBufferAddress);
            super.directBuffer.clear();
            super.directBuffer.asIntBuffer().get(ints_5);
        }

        IUnknown.Release(long_6);
        IUnknown.Release(long_8);
        return ints_5;
    }

    @Override
	public
    RectangleTextureSampler method14064(GLFormat class1501, GLActionFormat class762, int i_3, int i_4) {
        return new Direct3DRectangleTexture(this, class1501, class762, i_3, i_4);
    }

    @Override
    public boolean supportsArbVertexShader() {
        return (this.capabilities.VertexShaderVersion & 0xffff) >= 257;
    }

    @Override
    public NativeStream createStream(boolean bool1) {
        return new DirectXStreamSource(this, bool1);
    }


}
