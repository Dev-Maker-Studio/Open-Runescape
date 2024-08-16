package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL;

import jaggl.OpenGL;

public class OpenGlCubeTextureGenerator extends AbstractOpenGlCubeTextureGenerator {

    public static final int[] CLIPPINGS = new int[4];
    public final OpenGlToolkit openglToolkit;
    public final OpenGlArbCubeMapTexture cubeTexture;

    public OpenGlCubeTextureGenerator(OpenGlToolkit class505Sub11, int i_2) {
        this.openglToolkit = class505Sub11;
        this.cubeTexture = new OpenGlArbCubeMapTexture(class505Sub11, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, i_2);
    }

    @Override
    public OpenGlArbCubeMapTexture getCubeTexture() {
        return this.cubeTexture;
    }

    public boolean drawQuad(OpenGlArbCubeMapTexture class137Sub21, OpenGlArbCubeMapTexture class137Sub22, float alpha) {
        boolean framebuffersReady = true;
        IntegratedGraphicsDisplayGL display = this.openglToolkit.integratedGraphicsImageProducer;
        this.openglToolkit.getClipping(CLIPPINGS);
        this.openglToolkit.resetClipping();
        this.openglToolkit.method13586();
        OpenGL.glMatrixMode(GL_PROJECTION);
        OpenGL.glLoadIdentity();
        OpenGL.glOrtho(0.0D, 1.0D, 0.0D, 1.0D, -1.0, 1.0D);
        OpenGL.glMatrixMode(GL_MODELVIEW);
        OpenGL.glLoadIdentity();
        OpenGL.glPushAttrib(GL_VIEWPORT_BIT);
        OpenGL.glViewport(0, 0, this.cubeTexture.largestMaterialSize, this.cubeTexture.largestMaterialSize);
        this.openglToolkit.toggleFog(false);
        this.openglToolkit.setLightingEnabled(false);
        this.openglToolkit.toggleZbuffer(false);
        this.openglToolkit.setDepthMask(false);
        this.openglToolkit.renderTexture(-2);
        this.openglToolkit.activateTexture(1);
        this.openglToolkit.updatePointSizeRange(0.0F, 0.0F, 0.0F, alpha);
        this.openglToolkit.loadTextureCombineMode(GL_INTERPOLATE, GL_INTERPOLATE);
        this.openglToolkit.setActiveTexture(class137Sub22);
        this.openglToolkit.activateTexture(0);
        this.openglToolkit.loadTextureCombineMode(1);
        this.openglToolkit.setActiveTexture(class137Sub21);
        this.openglToolkit.setDisplay(display);

        for (int index = 0; index < 6; index++) {
            int side = index + GL_TEXTURE_CUBE_MAP_POSITIVE_X;
            display.setFramebuffer(0, this.cubeTexture.getAttachableTexture(side));
            display.drawBuffer(0);
            if (!display.isFramebufferComplete()) {
                framebuffersReady = false;
                break;
            }

            OpenGL.glBegin(GL_QUADS);
            switch (side) {
                case GL_TEXTURE_CUBE_MAP_POSITIVE_X:
                    OpenGL.glTexCoord3i(65535, 65534, 65534);
                    OpenGL.glMultiTexCoord3i(33985, 65535, 65534, 65534);
                    OpenGL.glVertex2f(0.0F, 0.0F);
                    OpenGL.glTexCoord3i(65535, 65534, -65534);
                    OpenGL.glMultiTexCoord3i(33985, 65535, 65534, -65534);
                    OpenGL.glVertex2f(1.0F, 0.0F);
                    OpenGL.glTexCoord3i(65535, -65534, -65534);
                    OpenGL.glMultiTexCoord3i(33985, 65535, -65534, -65534);
                    OpenGL.glVertex2f(1.0F, 1.0F);
                    OpenGL.glTexCoord3i(65535, -65534, 65534);
                    OpenGL.glMultiTexCoord3i(33985, 65535, -65534, 65534);
                    OpenGL.glVertex2f(0.0F, 1.0F);
                    break;
                case GL_TEXTURE_CUBE_MAP_NEGATIVE_X:
                    OpenGL.glTexCoord3i(-65535, 65534, -65534);
                    OpenGL.glMultiTexCoord3i(33985, -65535, 65534, -65534);
                    OpenGL.glVertex2f(0.0F, 0.0F);
                    OpenGL.glTexCoord3i(-65535, 65534, 65534);
                    OpenGL.glMultiTexCoord3i(33985, -65535, 65534, 65534);
                    OpenGL.glVertex2f(1.0F, 0.0F);
                    OpenGL.glTexCoord3i(-65535, -65534, 65534);
                    OpenGL.glMultiTexCoord3i(33985, -65535, -65534, 65534);
                    OpenGL.glVertex2f(1.0F, 1.0F);
                    OpenGL.glTexCoord3i(-65535, -65534, -65534);
                    OpenGL.glMultiTexCoord3i(33985, -65535, -65534, -65534);
                    OpenGL.glVertex2f(0.0F, 1.0F);
                    break;
                case GL_TEXTURE_CUBE_MAP_POSITIVE_Y:
                    OpenGL.glTexCoord3i(-65534, 65535, -65534);
                    OpenGL.glMultiTexCoord3i(33985, -65534, 65535, -65534);
                    OpenGL.glVertex2f(0.0F, 0.0F);
                    OpenGL.glTexCoord3i(65534, 65535, -65534);
                    OpenGL.glMultiTexCoord3i(33985, 65534, 65535, -65534);
                    OpenGL.glVertex2f(1.0F, 0.0F);
                    OpenGL.glTexCoord3i(65534, 65535, 65534);
                    OpenGL.glMultiTexCoord3i(33985, 65534, 65535, 65534);
                    OpenGL.glVertex2f(1.0F, 1.0F);
                    OpenGL.glTexCoord3i(-65534, 65535, 65534);
                    OpenGL.glMultiTexCoord3i(33985, -65534, 65535, 65534);
                    OpenGL.glVertex2f(0.0F, 1.0F);
                    break;
                case GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:
                    OpenGL.glTexCoord3i(-65534, -65535, 65534);
                    OpenGL.glMultiTexCoord3i(33985, -65534, -65535, 65534);
                    OpenGL.glVertex2f(0.0F, 0.0F);
                    OpenGL.glTexCoord3i(65534, -65535, 65534);
                    OpenGL.glMultiTexCoord3i(33985, 65534, -65535, 65534);
                    OpenGL.glVertex2f(1.0F, 0.0F);
                    OpenGL.glTexCoord3i(65534, -65535, -65534);
                    OpenGL.glMultiTexCoord3i(33985, 65534, -65535, -65534);
                    OpenGL.glVertex2f(1.0F, 1.0F);
                    OpenGL.glTexCoord3i(-65534, -65535, -65534);
                    OpenGL.glMultiTexCoord3i(33985, -65534, -65535, -65534);
                    OpenGL.glVertex2f(0.0F, 1.0F);
                    break;
                case GL_TEXTURE_CUBE_MAP_POSITIVE_Z:
                    OpenGL.glTexCoord3i(-65534, 65534, 65535);
                    OpenGL.glMultiTexCoord3i(33985, -65534, 65534, 65535);
                    OpenGL.glVertex2f(0.0F, 0.0F);
                    OpenGL.glTexCoord3i(65534, 65534, 65535);
                    OpenGL.glMultiTexCoord3i(33985, 65534, 65534, 65535);
                    OpenGL.glVertex2f(1.0F, 0.0F);
                    OpenGL.glTexCoord3i(65534, -65534, 65535);
                    OpenGL.glMultiTexCoord3i(33985, 65534, -65534, 65535);
                    OpenGL.glVertex2f(1.0F, 1.0F);
                    OpenGL.glTexCoord3i(-65534, -65534, 65535);
                    OpenGL.glMultiTexCoord3i(33985, -65534, -65534, 65535);
                    OpenGL.glVertex2f(0.0F, 1.0F);
                    break;
                case GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:
                    OpenGL.glTexCoord3i(65534, 65534, -65535);
                    OpenGL.glMultiTexCoord3i(33985, 65534, 65534, -65535);
                    OpenGL.glVertex2f(0.0F, 0.0F);
                    OpenGL.glTexCoord3i(-65534, 65534, -65535);
                    OpenGL.glMultiTexCoord3i(33985, -65534, 65534, -65535);
                    OpenGL.glVertex2f(1.0F, 0.0F);
                    OpenGL.glTexCoord3i(-65534, -65534, -65535);
                    OpenGL.glMultiTexCoord3i(33985, -65534, -65534, -65535);
                    OpenGL.glVertex2f(1.0F, 1.0F);
                    OpenGL.glTexCoord3i(65534, -65534, -65535);
                    OpenGL.glMultiTexCoord3i(33985, 65534, -65534, -65535);
                    OpenGL.glVertex2f(0.0F, 1.0F);
            }

            OpenGL.glEnd();
        }

        display.setFramebuffer(0, null);
        this.openglToolkit.resizeDisplay(display);
        this.openglToolkit.activateTexture(1);
        this.openglToolkit.setActiveTexture(null);
        this.openglToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        this.openglToolkit.activateTexture(0);
        this.openglToolkit.setActiveTexture(null);
        OpenGL.glPopAttrib();
        this.openglToolkit.setClipping(CLIPPINGS[0], CLIPPINGS[1], CLIPPINGS[2], CLIPPINGS[3]);
        if (framebuffersReady && !this.openglToolkit.radeonGpu) {
            this.cubeTexture.generateMipmaps();
        }

        return framebuffersReady;
    }

    public int getLargestMaterialSize() {
        return this.cubeTexture.largestMaterialSize;
    }

}
