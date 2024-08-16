package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.graphics.shader.OpenGLShaderManager;
import com.jagex.game.runetek5.graphics.shader.OpenGlShader;
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.utils.MathUtils;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaggl.OpenGL;

/**
 * 
 * @author Brainpower - 22/04/2022
 *
 */
public class RectangularTextureGL extends Node_Sub5 {

	public static float brightpass = 0.25F;
    public static float bloom = 1.0F;
    public static float whitePoint = 1.0F;
    public IntegratedGraphicsDisplayGL display;
    public OpenGl2DTexture aClass137Sub110038;
    public OpenGl2DTexture aClass137Sub110034;
    public OpenGLShaderManager scenetexShader;
    public OpenGLShaderManager mipmappedScenetexShader;
    public OpenGLShaderManager bloomScenetexShader;
    public OpenGLShaderManager basetexShader;
    public int s;
    public int t;
    public IntegratedGraphicsDisplayGL textureDisplay;
    public OpenGl2DTexture[] mipmaps;
    public int width;
    public int height;

    public RectangularTextureGL(OpenGlToolkit toolkit) {
        super(toolkit);
    }

    @Override
    public void draw(OpenGl2DTexture texture) {
        OpenGL.glPushAttrib(GL_VIEWPORT_BIT);
        OpenGL.glMatrixMode(GL_PROJECTION);
        OpenGL.glPushMatrix();
        OpenGL.glLoadIdentity();
        OpenGL.glOrtho(0.0D, 1.0D, 0.0D, 1.0D, -1.0, 1.0D);
        if (this.mipmaps != null) {
        	this.activeToolkit.setDisplay(this.textureDisplay);
            int width = MathUtils.furtestBitValue(this.s);
            int height = MathUtils.furtestBitValue(this.t);

            int mipmapLevel;
            for (mipmapLevel = 0; width > 256 || height > 256; mipmapLevel++) {
                OpenGL.glViewport(0, 0, width, height);
                this.textureDisplay.setFramebuffer(0, this.mipmaps[mipmapLevel].createFrameBuffer());
                if (mipmapLevel == 0) {
                	this.activeToolkit.setActiveTexture(texture);
                    OpenGL.glBegin(GL_QUADS);
                    OpenGL.glTexCoord2f(0.0F, 0.0F);
                    OpenGL.glVertex2i(0, 0);
                    OpenGL.glTexCoord2f(this.s, 0.0F);
                    OpenGL.glVertex2i(1, 0);
                    OpenGL.glTexCoord2f(this.s, this.t);
                    OpenGL.glVertex2i(1, 1);
                    OpenGL.glTexCoord2f(0.0F, this.t);
                } else {
                	this.activeToolkit.setActiveTexture(this.mipmaps[mipmapLevel - 1]);
                    OpenGL.glBegin(GL_QUADS);
                    OpenGL.glTexCoord2f(0.0F, 0.0F);
                    OpenGL.glVertex2i(0, 0);
                    OpenGL.glTexCoord2f(1.0F, 0.0F);
                    OpenGL.glVertex2i(1, 0);
                    OpenGL.glTexCoord2f(1.0F, 1.0F);
                    OpenGL.glVertex2i(1, 1);
                    OpenGL.glTexCoord2f(0.0F, 1.0F);
                }
                OpenGL.glVertex2i(0, 1);
                OpenGL.glEnd();

                if (width > 256) {
                    width >>= 1;
                }

                if (height > 256) {
                    height >>= 1;
                }
            }

            this.activeToolkit.resizeDisplay(this.textureDisplay);
            this.activeToolkit.setActiveTexture(this.mipmaps[mipmapLevel - 1]);
            this.activeToolkit.setDisplay(this.display);
            this.display.drawBuffer(0);
            OpenGL.glViewport(0, 0, 256, 256);
            int handle = this.mipmappedScenetexShader.programHandle;
            OpenGL.glUseProgram(handle);
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "sceneTex"), 0);
            OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "params"), whitePoint, 0.0F, 0.0F);
            OpenGL.glBegin(GL_QUADS);
            OpenGL.glTexCoord2f(0.0F, 0.0F);
            OpenGL.glVertex2i(0, 0);
            OpenGL.glTexCoord2f(1.0F, 0.0F);
            OpenGL.glVertex2i(1, 0);
            OpenGL.glTexCoord2f(1.0F, 1.0F);
            OpenGL.glVertex2i(1, 1);
            OpenGL.glTexCoord2f(0.0F, 1.0F);
        } else {
        	this.activeToolkit.setActiveTexture(texture);
        	this.activeToolkit.setDisplay(this.display);
        	this.display.drawBuffer(0);
            OpenGL.glViewport(0, 0, 256, 256);
            int handle = this.scenetexShader.programHandle;
            OpenGL.glUseProgram(handle);
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "sceneTex"), 0);
            OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "params"), whitePoint, 0.0F, 0.0F);
            OpenGL.glBegin(GL_QUADS);
            OpenGL.glTexCoord2f(0.0F, 0.0F);
            OpenGL.glVertex2i(0, 0);
            OpenGL.glTexCoord2f(this.s, 0.0F);
            OpenGL.glVertex2i(1, 0);
            OpenGL.glTexCoord2f(this.s, this.t);
            OpenGL.glVertex2i(1, 1);
            OpenGL.glTexCoord2f(0.0F, this.t);
        }
        OpenGL.glVertex2i(0, 1);
        OpenGL.glEnd();

        this.display.drawBuffer(1);
        this.activeToolkit.setActiveTexture(this.aClass137Sub110038);
        int handle = this.basetexShader.programHandle;
        OpenGL.glUseProgram(handle);
        OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "baseTex"), 0);
        OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "step"), 0.00390625F, 0.0F, 0.0F);
        OpenGL.glBegin(GL_QUADS);
        OpenGL.glTexCoord2f(0.0F, 0.0F);
        OpenGL.glVertex2i(0, 0);
        OpenGL.glTexCoord2f(1.0F, 0.0F);
        OpenGL.glVertex2i(1, 0);
        OpenGL.glTexCoord2f(1.0F, 1.0F);
        OpenGL.glVertex2i(1, 1);
        OpenGL.glTexCoord2f(0.0F, 1.0F);
        OpenGL.glVertex2i(0, 1);
        OpenGL.glEnd();
        this.display.drawBuffer(0);
        this.activeToolkit.setActiveTexture(this.aClass137Sub110034);
        OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "step"), 0.0F, 0.00390625F, 0.0F);
        OpenGL.glBegin(GL_QUADS);
        OpenGL.glTexCoord2f(0.0F, 0.0F);
        OpenGL.glVertex2i(0, 0);
        OpenGL.glTexCoord2f(1.0F, 0.0F);
        OpenGL.glVertex2i(1, 0);
        OpenGL.glTexCoord2f(1.0F, 1.0F);
        OpenGL.glVertex2i(1, 1);
        OpenGL.glTexCoord2f(0.0F, 1.0F);
        OpenGL.glVertex2i(0, 1);
        OpenGL.glEnd();
        OpenGL.glPopAttrib();
        OpenGL.glPopMatrix();
        OpenGL.glMatrixMode(GL_MODELVIEW);
        this.activeToolkit.resizeDisplay(this.display);
        int handle2 = this.bloomScenetexShader.programHandle;
        OpenGL.glUseProgram(handle2);
        OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle2, "sceneTex"), 0);
        OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle2, "bloomTex"), 1);
        OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle2, "params"), brightpass, bloom, 0.0F);
        this.activeToolkit.activateTexture(1);
        this.activeToolkit.setActiveTexture(this.aClass137Sub110038);
        this.activeToolkit.activateTexture(0);
        this.activeToolkit.setActiveTexture(texture);
    }

    public boolean rectangularTextureSupport() {
        return this.activeToolkit.intelGpu && this.activeToolkit.arbFragmentShader && this.activeToolkit.arbTextureFloat;
    }

    @Override
    public boolean displayPresent() {
        return this.display != null;
    }

    @Override
    public boolean createShaders() {
        if (this.activeToolkit.intelGpu && this.activeToolkit.arbFragmentShader && this.activeToolkit.arbTextureFloat) {
        	this.display = new IntegratedGraphicsDisplayGL(super.activeToolkit);
        	this.aClass137Sub110038 = new OpenGl2DTexture(this.activeToolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.FLOAT_16_ARB, 256, 256);
        	this.aClass137Sub110038.setRepeat(false, false);
        	this.aClass137Sub110034 = new OpenGl2DTexture(this.activeToolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.FLOAT_16_ARB, 256, 256);
        	this.aClass137Sub110034.setRepeat(false, false);
        	this.activeToolkit.setDisplay(this.display);
        	this.display.setFramebuffer(0, this.aClass137Sub110038.createFrameBuffer());
        	this.display.setFramebuffer(1, this.aClass137Sub110034.createFrameBuffer());
        	this.display.drawBuffer(0);
            if (!this.display.isFramebufferComplete()) {
            	this.activeToolkit.resizeDisplay(this.display);
                return false;
            } 
            this.activeToolkit.resizeDisplay(this.display);
            this.scenetexShader = OpenGLShaderManager.addShaders(this.activeToolkit, new OpenGlShader[]{
            		OpenGlShader.createShader(super.activeToolkit, GL_FRAGMENT_SHADER,
            				"#extension GL_ARB_texture_rectangle : enable\n"
            				+ "uniform vec3 params;\n"
            				+ "uniform sampler2DRect sceneTex;\n"
            				+ "const vec3 lumCoef = vec3(0.2126, 0.7152, 0.0722);\n"
            				+ "void main() {\n    "
            				+ "vec4 col = texture2DRect(sceneTex, gl_TexCoord[0].xy);\n    "
            				+ "gl_FragColor = col*step(params.x, dot(lumCoef, col.rgb));\n}\n")});
            this.mipmappedScenetexShader = OpenGLShaderManager.addShaders(this.activeToolkit, new OpenGlShader[]{
            		OpenGlShader.createShader(super.activeToolkit, GL_FRAGMENT_SHADER,
            				"uniform vec3 params;\n"
            				+ "uniform sampler2D sceneTex;\n"
            				+ "const vec3 lumCoef = vec3(0.2126, 0.7152, 0.0722);\n"
            				+ "void main() {\n    "
            				+ "vec4 col = texture2D(sceneTex, gl_TexCoord[0].xy);\n    "
            				+ "gl_FragColor = col*step(params.x, dot(lumCoef, col.rgb));\n}\n")});
            this.bloomScenetexShader = OpenGLShaderManager.addShaders(this.activeToolkit, new OpenGlShader[]{
            		OpenGlShader.createShader(super.activeToolkit, GL_FRAGMENT_SHADER,
            				"#extension GL_ARB_texture_rectangle : enable\n"
            				+ "uniform vec3 params;\n"
            				+ "uniform vec3 dimScale;\n"
            				+ "uniform sampler2D bloomTex;\n"
            				+ "uniform sampler2DRect sceneTex;\n"
            				+ "const vec3 lumCoef = vec3(0.2126, 0.7152, 0.0722);\n"
            				+ "void main() {\n\t "
            				+ "vec4 bloomCol = texture2D(bloomTex, gl_TexCoord[1].xy);\n\t "
            				+ "vec4 sceneCol = texture2DRect(sceneTex, gl_TexCoord[0].xy);\n\t "
            				+ "float preLum = 0.99*dot(lumCoef, sceneCol.rgb)+0.01;\n    "
            				+ "float postLum = preLum*(1.0+(preLum/params.y))/(preLum+1.0);\n\t "
            				+ "gl_FragColor = sceneCol*(postLum/preLum)+bloomCol*params.x;\n}\n")});
            this.basetexShader = OpenGLShaderManager.addShaders(this.activeToolkit, new OpenGlShader[]{
            		OpenGlShader.createShader(super.activeToolkit, GL_FRAGMENT_SHADER,
            				"uniform vec3 step;\n"
            				+ "uniform sampler2D baseTex;\n"
            				+ "void main() {\n\t"
            				+ "vec4 fragCol = texture2D(baseTex, gl_TexCoord[0].xy)*0.091396265;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-1.0*step.xy))*0.088584304;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 1.0*step.xy))*0.088584304;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-2.0*step.xy))*0.08065692;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 2.0*step.xy))*0.08065692;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-3.0*step.xy))*0.068989515;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 3.0*step.xy))*0.068989515;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-4.0*step.xy))*0.055434637;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 4.0*step.xy))*0.055434637;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-5.0*step.xy))*0.04184426;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 5.0*step.xy))*0.04184426;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-6.0*step.xy))*0.029672023;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 6.0*step.xy))*0.029672023;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-7.0*step.xy))*0.019765828;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 7.0*step.xy))*0.019765828;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-8.0*step.xy))*0.012369139;\n\t"
            				+ "fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 8.0*step.xy))*0.012369139;\n\t"
            				+ "gl_FragColor = fragCol;\n}\n")});
            return this.mipmappedScenetexShader != null && this.scenetexShader != null && this.bloomScenetexShader != null && this.basetexShader != null;
        } 
        return false;        
    }

    @Override
    public void calculateMipmaps(int s, int t) {
        this.s = s;
        this.t = t;
        int width = MathUtils.furtestBitValue(this.s);
        int height = MathUtils.furtestBitValue(this.t);
        if (width != this.width || height != this.height) {
            if (this.mipmaps != null) {
                for (int level = 0; level < this.mipmaps.length; level++) {
                	this.mipmaps[level].delete();
                }

                this.mipmaps = null;
            }

            if (width <= 256 && height <= 256) {
            	this.textureDisplay = null;
            } else {
                int w = width;
                int h = height;
                int level = 0;

                GenerateMipmaps:
                while (true) {
                    if (w <= 256 && h <= 256) {
                        if (this.textureDisplay == null) {
                        	this.textureDisplay = new IntegratedGraphicsDisplayGL(this.activeToolkit);
                        }

                        this.mipmaps = new OpenGl2DTexture[level];
                        w = width;
                        h = height;
                        level = 0;

                        while (true) {
                            if (w <= 256 && h <= 256) {
                                break GenerateMipmaps;
                            }

                            this.mipmaps[level++] = new OpenGl2DTexture(this.activeToolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.FLOAT_16_ARB, w, h);
                            if (w > 256) {
                                w >>= 1;
                            }

                            if (h > 256) {
                                h >>= 1;
                            }
                        }
                    }

                    if (w > 256) {
                        w >>= 1;
                    }

                    if (h > 256) {
                        h >>= 1;
                    }

                    level++;
                }
            }

            this.width = width;
            this.height = height;
        }

    }

    @Override
    public GLActionFormat getFormat() {
        return GLActionFormat.FLOAT_16_ARB;
    }

    @Override
    public void resetTexture() {
        OpenGL.glUseProgram(0);
        this.activeToolkit.activateTexture(1);
        this.activeToolkit.setActiveTexture(null);
        this.activeToolkit.activateTexture(0);
    }

    @Override
	public
    void destroy() {
    	this.height = -1;
    	this.width = -1;
    	this.textureDisplay = null;
    	this.mipmaps = null;
    	this.display = null;
    	this.aClass137Sub110038 = null;
    	this.aClass137Sub110034 = null;
    	this.scenetexShader = null;
    	this.mipmappedScenetexShader = null;
    	this.bloomScenetexShader = null;
    	this.basetexShader = null;
    }
}