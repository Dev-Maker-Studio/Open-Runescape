package com.jagex.game.runetek5.unidentified;

import com.jagex.game.runetek5.buffer.OpenGlRenderBuffer;
import com.jagex.game.runetek5.cache.node.LinkedList;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.graphics.texture.Node_Sub5;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture;
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public class Class170 {

    public OpenGlRenderBuffer renderBuffer;
    public OpenGl2DTexture activeTexture;
    public OpenGlRenderBuffer hdrRenderBuffer;
    public boolean aBool2103 = true;
    public boolean aBool2109 = true;
    public boolean aBool2105 = true;
    public boolean pixelFormatSet = true;
    public boolean aBool2100;
    public int s = 1;
    public int t = 1;
    public int vertexCoordinateX;
    public int vertexCoordinateY;
    public GLActionFormat format;
    public boolean loadTexture;
    public int texturedFaceCount;
    public final LinkedList aClass4732108 = new LinkedList();
    public OpenGl2DTexture[] textures2d = new OpenGl2DTexture[2];
    public final OpenGlToolkit activeToolkit;
    public IntegratedGraphicsDisplayGL aClass158Sub1Sub42099;
    public IntegratedGraphicsDisplayGL aClass158Sub1Sub42095;
    public IntegratedGraphicsDisplayGL aClass158Sub1Sub42101;

    public Class170(OpenGlToolkit class505Sub11) {
        this.format = GLActionFormat.PIXEL_FORMAT;
        this.activeToolkit = class505Sub11;
        if (this.activeToolkit.intelGpu && this.activeToolkit.arbRectangleTexturesSupported) {
            this.aClass158Sub1Sub42095 = this.aClass158Sub1Sub42099 = new IntegratedGraphicsDisplayGL(this.activeToolkit);
            if (this.activeToolkit.multiSamplesPreference > 1 && this.activeToolkit.framebufferBlit && this.activeToolkit.framebufferMultisample) {
                this.aClass158Sub1Sub42095 = this.aClass158Sub1Sub42101 = new IntegratedGraphicsDisplayGL(this.activeToolkit);
            }
        }

    }

    public boolean method2881() {
        if (this.aBool2103) {
            if (this.renderBuffer != null) {
                this.renderBuffer.deallocate();
                this.renderBuffer = null;
            }
            if (this.activeTexture != null) {
                this.activeTexture.delete();
                this.activeTexture = null;
            }
        }
        if (this.pixelFormatSet) {
            if (this.hdrRenderBuffer != null) {
                this.hdrRenderBuffer.deallocate();
                this.hdrRenderBuffer = null;
            }
            if (this.textures2d[0] != null) {
                this.textures2d[0].delete();
                this.textures2d[0] = null;
            }
            if (this.textures2d[1] != null) {
                this.textures2d[1].delete();
                this.textures2d[1] = null;
            }
        }
        if (this.aBool2103) {
            if (this.aClass158Sub1Sub42101 != null) {
                this.renderBuffer = new OpenGlRenderBuffer(this.activeToolkit, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, this.s, this.t, this.activeToolkit.multiSamplesPreference);
            }

            if (this.loadTexture) {
                this.activeTexture = new OpenGl2DTexture(this.activeToolkit, 34037, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, this.s, this.t);
            } else if (this.renderBuffer == null) {
                this.renderBuffer = new OpenGlRenderBuffer(this.activeToolkit, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, this.s, this.t);
            }

            this.aBool2103 = false;
            this.aBool2109 = true;
            this.aBool2105 = true;
        }

        if (this.pixelFormatSet) {
            if (this.aClass158Sub1Sub42101 != null) {
                this.hdrRenderBuffer = new OpenGlRenderBuffer(this.activeToolkit, GLFormat.HDR, this.format, this.s, this.t, this.activeToolkit.multiSamplesPreference);
            }

            this.textures2d[0] = new OpenGl2DTexture(this.activeToolkit, 34037, GLFormat.HDR, this.format, this.s, this.t);
            this.textures2d[1] = this.texturedFaceCount > 1 ? new OpenGl2DTexture(this.activeToolkit, 34037, GLFormat.HDR, this.format, this.s, this.t) : null;
            this.pixelFormatSet = false;
            this.aBool2109 = true;
            this.aBool2105 = true;
        }

        if (this.aBool2109) {
            if (this.aClass158Sub1Sub42101 != null) {
                this.activeToolkit.setDisplay(this.aClass158Sub1Sub42099);
                this.aClass158Sub1Sub42099.setRenderBuffer(null);
                this.aClass158Sub1Sub42099.setFramebuffer(0, null);
                this.aClass158Sub1Sub42099.setFramebuffer(1, null);
                this.aClass158Sub1Sub42099.setFramebuffer(0, this.textures2d[0].createFrameBuffer());
                this.aClass158Sub1Sub42099.setFramebuffer(1, this.texturedFaceCount > 1 ? this.textures2d[1].createFrameBuffer() : null);
                if (this.loadTexture) {
                    this.aClass158Sub1Sub42099.setRenderBuffer(this.activeTexture.createRenderBuffer());
                }

                this.activeToolkit.resizeDisplay(this.aClass158Sub1Sub42099);
                this.activeToolkit.setDisplay(this.aClass158Sub1Sub42101);
                this.aClass158Sub1Sub42101.setRenderBuffer(null);
                this.aClass158Sub1Sub42101.setFramebuffer(0, null);
                this.aClass158Sub1Sub42101.setFramebuffer(0, this.hdrRenderBuffer);
                this.aClass158Sub1Sub42101.setRenderBuffer(this.renderBuffer);
                this.activeToolkit.resizeDisplay(this.aClass158Sub1Sub42101);
            } else {
                this.activeToolkit.setDisplay(this.aClass158Sub1Sub42099);
                this.aClass158Sub1Sub42099.setRenderBuffer(null);
                this.aClass158Sub1Sub42099.setFramebuffer(0, null);
                this.aClass158Sub1Sub42099.setFramebuffer(1, null);
                this.aClass158Sub1Sub42099.setFramebuffer(0, this.textures2d[0].createFrameBuffer());
                this.aClass158Sub1Sub42099.setFramebuffer(1, this.texturedFaceCount > 1 ? this.textures2d[1].createFrameBuffer() : null);
                if (this.loadTexture) {
                    this.aClass158Sub1Sub42099.setRenderBuffer(this.activeTexture.createRenderBuffer());
                } else {
                    this.aClass158Sub1Sub42099.setRenderBuffer(this.renderBuffer);
                }

                this.activeToolkit.resizeDisplay(this.aClass158Sub1Sub42099);
            }

            this.aBool2109 = false;
            this.aBool2105 = true;
        }

        if (this.aBool2105) {
            this.activeToolkit.setDisplay(this.aClass158Sub1Sub42095);
            this.aBool2105 = !this.aClass158Sub1Sub42095.isFramebufferComplete();
            this.activeToolkit.resizeDisplay(this.aClass158Sub1Sub42095);
        }

        return !this.aBool2105;
    }

    @SuppressWarnings("unchecked")
    public void method2883() {
        if (this.aBool2100) {
            if (this.aClass158Sub1Sub42101 != null) {
                this.activeToolkit.resizeDisplay(this.aClass158Sub1Sub42101);
                this.activeToolkit.setDisplay(this.aClass158Sub1Sub42099);
                this.aClass158Sub1Sub42099.drawBuffer(0);
                this.aClass158Sub1Sub42101.bindFrameBuffer(this.s, this.t, this.loadTexture);
            }

            this.activeToolkit.setupSpriteRendering();
            this.activeToolkit.setBlendMode(0);
            this.activeToolkit.loadTextureCombineMode(1);
            this.activeToolkit.resetClipping();
            int textureIndex = 0;
            int colorAttachmentSlot = 1;

            Node_Sub5 prev;
            for (Node_Sub5 tail = (Node_Sub5) (Node) this.aClass4732108.getTail(); tail != null; tail = prev) {
                prev = (Node_Sub5) (Node) this.aClass4732108.getPrevious();
                int numTexturedFaces = tail.getTexturedFaces();

                for (int face = 0; face < numTexturedFaces; face++) {
                    tail.draw(this.textures2d[textureIndex]);
                    if (prev == null && face == numTexturedFaces - 1) {
                        this.activeToolkit.resizeDisplay(this.aClass158Sub1Sub42099);
                        this.activeToolkit.method13640(0, 0);
                        OpenGL.glBegin(7);
                        OpenGL.glTexCoord2f(0.0F, this.t);
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, 1.0F);
                        OpenGL.glVertex2i(this.vertexCoordinateX, this.vertexCoordinateY);
                        OpenGL.glTexCoord2f(0.0F, 0.0F);
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, 0.0F);
                        OpenGL.glVertex2i(this.vertexCoordinateX, this.vertexCoordinateY + this.t);
                        OpenGL.glTexCoord2f(this.s, 0.0F);
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 1.0F, 0.0F);
                        OpenGL.glVertex2i(this.vertexCoordinateX + this.s, this.vertexCoordinateY + this.t);
                        OpenGL.glTexCoord2f(this.s, this.t);
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 1.0F, 1.0F);
                        OpenGL.glVertex2i(this.vertexCoordinateX + this.s, this.vertexCoordinateY);
                    } else {
                        this.aClass158Sub1Sub42099.drawBuffer(colorAttachmentSlot);
                        OpenGL.glBegin(7);
                        OpenGL.glTexCoord2f(0.0F, this.t);
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, 1.0F);
                        OpenGL.glVertex2i(0, 0);
                        OpenGL.glTexCoord2f(0.0F, 0.0F);
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0F, 0.0F);
                        OpenGL.glVertex2i(0, this.t);
                        OpenGL.glTexCoord2f(this.s, 0.0F);
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 1.0F, 0.0F);
                        OpenGL.glVertex2i(this.s, this.t);
                        OpenGL.glTexCoord2f(this.s, this.t);
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 1.0F, 1.0F);
                        OpenGL.glVertex2i(this.s, 0);
                    }
                    OpenGL.glEnd();

                    tail.resetTexture();
                    colorAttachmentSlot = colorAttachmentSlot + 1 & 0x1;
                    textureIndex = textureIndex + 1 & 0x1;
                }
            }

            this.aBool2100 = false;
        }

    }

    public boolean method2884(int s, int t) {
        if (this.aClass158Sub1Sub42095 != null && !this.aClass4732108.isEmpty()) {
            if (s != this.s || t != this.t) {
                this.s = s;
                this.t = t;
                for (Node node5 = this.aClass4732108.getTail(); node5 != this.aClass4732108.head; node5 = node5.next) {
                    ((Node_Sub5) node5).calculateMipmaps(this.s, this.t);
                }
                this.pixelFormatSet = true;
                this.aBool2103 = true;
                this.aBool2109 = true;
            }
            if (method2881()) {
                this.vertexCoordinateX = 0;
                this.vertexCoordinateY = 0;
                this.aBool2100 = true;
                this.activeToolkit.method13640(-this.vertexCoordinateX, this.t + this.vertexCoordinateY - this.activeToolkit.getDisplay().getHeight());
                this.activeToolkit.setDisplay(this.aClass158Sub1Sub42095);
                this.activeToolkit.applyClearFlags(3, 0);
                this.aClass158Sub1Sub42095.drawBuffer(0);
                return true;
            } 
            return false;
        }
        return false;
    }

    public void method2885(Node_Sub5 class282Sub51) {
        class282Sub51.aBool7506 = false;
        class282Sub51.destroy();
        class282Sub51.unlink();
        method2886();
    }

    @SuppressWarnings("unchecked")
    public void method2886() {
        int texturedFaces = 0;
        boolean loadTexture = false;
        GLActionFormat pixelFormat = GLActionFormat.PIXEL_FORMAT;

        for (Node_Sub5 node = (Node_Sub5) (Node) this.aClass4732108.getTail(); node != null; node = (Node_Sub5) (Node) this.aClass4732108.getPrevious()) {
            GLActionFormat class767 = node.getFormat();
            if (class767.bytesPerEntry > pixelFormat.bytesPerEntry) {
                pixelFormat = class767;
            }
            loadTexture |= node.loadTexture();
            texturedFaces += node.getTexturedFaces();
        }
        if (pixelFormat != this.format) {
        	this.format = pixelFormat;
        	this.pixelFormatSet = true;
        }
        int currFaces = Math.min(2, this.texturedFaceCount);
        int newFaces = Math.min(2, texturedFaces);
        if (currFaces != newFaces) {
        	this.pixelFormatSet = true;
            this.aBool2109 = true;
        }
        if (loadTexture != this.loadTexture) {
        	this.loadTexture = loadTexture;
        	this.aBool2103 = true;
        }
        this.texturedFaceCount = texturedFaces;
    }

    public boolean method2896() {
        return this.aClass158Sub1Sub42095 != null;
    }

    public void shutdown() {
        this.aClass158Sub1Sub42099 = null;
        this.aClass158Sub1Sub42101 = null;
        this.aClass158Sub1Sub42095 = null;
        this.renderBuffer = null;
        this.activeTexture = null;
        this.textures2d = null;
        this.hdrRenderBuffer = null;
        if (!this.aClass4732108.isEmpty()) {
            for (Node node1 = this.aClass4732108.getTail(); node1 != this.aClass4732108.head; node1 = node1.next) {
                ((Node_Sub5) node1).destroy();
            }
        }

        this.t = 1;
        this.s = 1;
    }

    public boolean method2900(Node_Sub5 node) {
        if (this.aClass158Sub1Sub42095 != null) {
            if (node.displayPresent() || node.createShaders()) {
                this.aClass4732108.insertTail(node);
                method2886();
                if (method2881()) {
                    if (this.s != -1 && this.t != -1) {
                        node.calculateMipmaps(this.s, this.t);
                    }
                    node.aBool7506 = true;
                    return true;
                }
            }
            method2885(node);
        }
        return false;
    }

}
