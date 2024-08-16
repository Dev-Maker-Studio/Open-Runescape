package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.buffer.OpenGlRenderBuffer;
import com.jagex.game.runetek5.graphics.texture.OpenGl3DTexture;
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

/**
 * 
 * @author Brainpower - 22/04/2022
 *
 */
public class Texture3DShaderGL {

    public OpenGLShaderManager shader;
    public final OpenGlToolkit openglToolkit;

    public Texture3DShaderGL(OpenGlToolkit openglToolkit) {
        this.openglToolkit = openglToolkit;
    }

    public boolean createShader() {
        if (this.openglToolkit.intelGpu && this.openglToolkit.arbFragmentShader && this.shader == null) {
            OpenGlShader shaderProgram = OpenGlShader.createShader(this.openglToolkit, GL_FRAGMENT_SHADER,
            		"uniform float rcpRelief;\n"
            		+ "uniform vec2 sampleSize;\n"
            		+ "uniform sampler3D heightMap;\n"
            		+ "void main() {\n"
            		+ "float dx = texture3D(heightMap, vec3(-sampleSize.x, 0.0, 0.0)+gl_TexCoord[0].xyz).r - texture3D(heightMap, vec3(sampleSize.x, 0.0, 0.0)+gl_TexCoord[0].xyz).r;\n"
            		+ "float dy = texture3D(heightMap, vec3(0.0, -sampleSize.y, 0.0)+gl_TexCoord[0].xyz).r - texture3D(heightMap, vec3(0.0, sampleSize.y, 0.0)+gl_TexCoord[0].xyz).r;\n"
            		+ "gl_FragColor = vec4(0.5+normalize(vec3(dx, dy, rcpRelief))*0.5, texture3D(heightMap, gl_TexCoord[0].xyz).r);\n}\n");
            if (shaderProgram != null) {
                this.shader = OpenGLShaderManager.addShaders(this.openglToolkit, new OpenGlShader[]{shaderProgram});
            }
        }
        return this.shader != null;
    }

    public boolean activate(OpenGl3DTexture hdrTexture3d, OpenGl3DTexture texture3d) {
        if (!createShader()) {
            return false;
        } 
        IntegratedGraphicsDisplayGL openglDisplay = this.openglToolkit.integratedGraphicsImageProducer;
        OpenGlRenderBuffer openglRenderBuffer = new OpenGlRenderBuffer(this.openglToolkit, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, hdrTexture3d.width, hdrTexture3d.height);
        boolean ready = false;
        this.openglToolkit.setDisplay(openglDisplay);
        openglDisplay.setFramebuffer(0, openglRenderBuffer);
        if (openglDisplay.isFramebufferComplete()) {
            OpenGL.glPushMatrix();
            OpenGL.glLoadIdentity();
            OpenGL.glMatrixMode(GL_PROJECTION);
            OpenGL.glPushMatrix();
            OpenGL.glLoadIdentity();
            OpenGL.glOrtho(0.0D, 1.0D, 0.0D, 1.0D, -1.0, 1.0D);
            OpenGL.glPushAttrib(GL_VIEWPORT_BIT);
            OpenGL.glViewport(0, 0, hdrTexture3d.width, hdrTexture3d.height);
            OpenGL.glUseProgram(this.shader.programHandle);
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(this.shader.programHandle, "heightMap"), 0);
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(this.shader.programHandle, "rcpRelief"), 0.5f);
            OpenGL.glUniform2f(OpenGL.glGetUniformLocation(this.shader.programHandle, "sampleSize"), 1.0F / texture3d.width, 1.0F / texture3d.height);

            for (int depth = 0; depth < hdrTexture3d.depth; depth++) {
                float r = (float) depth / hdrTexture3d.depth;
                this.openglToolkit.setActiveTexture(texture3d);
                OpenGL.glBegin(7);
                OpenGL.glTexCoord3f(0.0F, 0.0F, r);
                OpenGL.glVertex2f(0.0F, 0.0F);
                OpenGL.glTexCoord3f(1.0F, 0.0F, r);
                OpenGL.glVertex2f(1.0F, 0.0F);
                OpenGL.glTexCoord3f(1.0F, 1.0F, r);
                OpenGL.glVertex2f(1.0F, 1.0F);
                OpenGL.glTexCoord3f(0.0F, 1.0F, r);
                OpenGL.glVertex2f(0.0F, 1.0F);
                OpenGL.glEnd();
                hdrTexture3d.copy(depth, hdrTexture3d.width, hdrTexture3d.height);
            }

            OpenGL.glUseProgram(0);
            OpenGL.glPopAttrib();
            OpenGL.glPopMatrix();
            OpenGL.glMatrixMode(GL_MODELVIEW);
            OpenGL.glPopMatrix();
            ready = true;
        }

        openglDisplay.setFramebuffer(0, null);
        this.openglToolkit.resizeDisplay(openglDisplay);
        return ready;        
    }
}
