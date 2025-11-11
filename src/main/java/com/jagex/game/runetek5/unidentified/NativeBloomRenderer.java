package com.jagex.game.runetek5.unidentified;

import java.nio.ByteBuffer;
import java.util.Vector;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.graphics.shader.program.AbstractBloomShader;
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent;

public class NativeBloomRenderer {

    public NativeStream anInterface4376;
    public VertexDeclaration aClass70377;
    public GLActionFormat format;
    public DirectDisplay directDisplay;
    public RectangleTextureSampler anInterface29363;
    public FrameBuffer anInterface9364;
    public RectangleTextureSampler anInterface29372;
    public FrameBuffer anInterface9368;
    public RectangleTextureSampler anInterface29366;
    public FrameBuffer anInterface9369;
    public RenderBuffer anInterface8362;
    public int formatType;
    public final Vector<AbstractBloomShader> bloomVector = new Vector<AbstractBloomShader>();
    public final AbstractNativeToolkit toolkit;
    public int width;
    public int height;
    public boolean aBool370;

    public NativeBloomRenderer(AbstractNativeToolkit toolkit, int width, int height) {
        this.toolkit = toolkit;
        this.format = GLActionFormat.PIXEL_FORMAT;
        this.width = width;
        this.height = height;
    }

    public boolean initialize(AbstractBloomShader bloomShader) {
        return initialize(this.bloomVector.size(), bloomShader);
    }

    public void method817() {
        if (this.anInterface4376 == null) {
            this.anInterface4376 = this.toolkit.createStream(false);
            this.anInterface4376.create(12, 4);
            ByteBuffer buffer = this.toolkit.directBuffer;
            buffer.clear();
            buffer.putFloat(0.0F);
            buffer.putFloat(1.0F);
            buffer.putFloat(2.0F);
            this.anInterface4376.upload(0, buffer.position(), this.toolkit.directBufferAddress);
            this.aClass70377 = this.toolkit.method13995(new VertexDeclarationComponent[]{new VertexDeclarationComponent(VertexDeclarationAction.aClass69691)});
        }

    }

    public void destroy() {
        this.directDisplay.cleanup();
        this.anInterface9364.deallocate();
        this.anInterface9368.deallocate();
        this.anInterface9369.deallocate();
        this.anInterface29363.deallocate();
        this.anInterface29372.deallocate();
        this.anInterface29366.deallocate();
        this.anInterface8362.deallocate();
        int elements = this.bloomVector.size();
        for (int index = 0; index < elements; index++) {
            AbstractBloomShader bloom = this.bloomVector.elementAt(index);
            bloom.destroy();
        }
    }

    public void getFormat() {
        method817();
        switch (this.formatType) {
            case 0:
                this.format = GLActionFormat.PIXEL_FORMAT;
                break;
            case 1:
                this.format = GLActionFormat.FLOAT_16_ARB;
                break;
            case 2:
                this.format = GLActionFormat.FLOAT_32_ARB;
                break;
            default:
                throw new RuntimeException();
        }

        this.directDisplay = this.toolkit.getImageProducer();
        this.anInterface29363 = this.toolkit.method14064(GLFormat.HDR, this.format, this.width, this.height);
        this.anInterface9364 = this.anInterface29363.getFramebuffer();
        this.anInterface29372 = this.toolkit.method14064(GLFormat.HDR, this.format, this.width, this.height);
        this.anInterface9368 = this.anInterface29372.getFramebuffer();
        this.anInterface29366 = this.toolkit.method14064(GLFormat.HDR, this.format, this.width, this.height);
        this.anInterface9369 = this.anInterface29366.getFramebuffer();
        this.anInterface8362 = this.toolkit.getRenderBuffer(this.width, this.height);
        int i_1 = this.bloomVector.size();

        for (int i_2 = 0; i_2 < i_1; i_2++) {
            AbstractBloomShader class553 = this.bloomVector.elementAt(i_2);
            class553.method1102();
        }

    }

    public void drawTriangles() {
        this.toolkit.setStreamSource(0, this.anInterface4376);
        this.toolkit.setVertexDeclaration(this.aClass70377);
        this.toolkit.drawPrimitive(PrimitiveDrawMode.TRIANGLES, 0, 1);
    }

    public boolean initialize(int insertAtIndex, AbstractBloomShader bloom) {
        if (!bloom.isLoaded() && !bloom.method1097()) {
            method822(bloom);
            return true;
        } 
        this.bloomVector.insertElementAt(bloom, insertAtIndex);
        bloom.method1102();
        int format = bloom.getFormatType();
        if (format > this.formatType) {
            this.formatType = format;
        }
        bloom.hasLinkedVector = true;
        return true;
    }

    public void method822(AbstractBloomShader bloom) {
        bloom.destroy();
        bloom.hasLinkedVector = false;
        this.bloomVector.removeElement(bloom);
    }

    public void draw() {
        this.toolkit.method14003();
        this.toolkit.toggleBlending(0);
        this.toolkit.setColourType(1);
        this.toolkit.resetClipping();
        this.directDisplay.setRenderBuffer(null);
        this.toolkit.method13919();
        int count = this.bloomVector.size();
        this.anInterface29366.enableMipmapping();

        for (int index = 0; index < count; index++) {
            AbstractBloomShader bloomShader = this.bloomVector.elementAt(index);
            int iterations = bloomShader.getIterations();
            boolean resize = index == count - 1;

            for (int type = 0; type < iterations; type++) {
                if (resize && type == iterations - 1) {
                    this.toolkit.resizeDisplay(this.directDisplay);
                } else {
                    this.directDisplay.setFramebuffer(0, this.anInterface9368);
                }

                RectangleTextureSampler compositeSampler = this.anInterface29363;
                if (type == 0) {
                    compositeSampler = this.anInterface29366;
                }

                bloomShader.initialize(type, this.directDisplay, compositeSampler, this.anInterface29366);
                drawTriangles();
                bloomShader.reset();
                RectangleTextureSampler interface298 = this.anInterface29363;
                this.anInterface29363 = this.anInterface29372;
                this.anInterface29372 = interface298;
                FrameBuffer interface99 = this.anInterface9364;
                this.anInterface9364 = this.anInterface9368;
                this.anInterface9368 = interface99;
            }
        }

        this.toolkit.toggleBlending(1);
        this.toolkit.setColourType(0);
    }

    public boolean method830(int w, int h) {
        if (this.bloomVector.isEmpty()) {
            return false;
        } 
        if (w != this.width || h != this.height) {
            this.width = w;
            this.height = h;
            destroy();
            getFormat();
        }

        this.directDisplay.setFramebuffer(0, this.anInterface9369);
        if (this.anInterface8362 != null) {
            this.directDisplay.setRenderBuffer(this.anInterface8362);
        }

        this.toolkit.setDisplay(this.directDisplay);
        this.toolkit.applyClearFlags(3, ~0xffffff);
        this.toolkit.colorMask(15);
        this.toolkit.method13985();
        if (!this.directDisplay.isFramebufferComplete()) {
            throw new RuntimeException("");
        } 
        this.aBool370 = true;
        return true;       
    }

    public void method833() {
        if (this.aBool370) {
            draw();
            this.aBool370 = false;
        }
    }
}