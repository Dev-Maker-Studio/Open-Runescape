package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;
import com.jagex.game.runetek5.toolkit.opengl.ProgramGL;

import jaggl.OpenGL;

/**
 * 
 * @author Steven - 19/07/2023
 * Displays unanimated water with blended edges, 
 * without this on you'll see the underlay under the water
 * Seems to only be used for water tiles
 *
 */
public class StillWaterEffect extends MaterialEffectGL {

    public static final float[] XYZW = {0.0F, -1.0f, 0.0F, 0.0F};
    public boolean calculateLights;
    public boolean bound;
    public boolean aBool9073;
    public final boolean initialized;
    public ProgramGL waterNodepthColor;
    public ProgramGL waterNodepthReflection;
    public ProgramGL waterDepthColor;
    public ProgramGL waterDepthReflection;
    public OpenGl2DTexture openglTexture;

    public StillWaterEffect(OpenGlToolkit openglToolkit) {
        super(openglToolkit);
        if (super.glToolkit.arbVertexProgram) {
            this.waterNodepthColor = ProgramGL.generateProgram(super.glToolkit,
            		"!!ARBvp1.0\n"
            		+ "ATTRIB  iPos         = vertex.position;\n"
            		+ "ATTRIB  iColour      = vertex.color;\n"
            		+ "ATTRIB  iTexCoord    = vertex.texcoord[0];\n"
            		+ "OUTPUT  oPos         = result.position;\n"
            		+ "OUTPUT  oColour      = result.color;\n"
            		+ "OUTPUT  oTexCoord0   = result.texcoord[0];\n"
            		+ "OUTPUT  oTexCoord1   = result.texcoord[1];\n"
            		+ "OUTPUT  oFogCoord    = result.fogcoord;\n"
            		+ "PARAM   fogParams    = program.local[0];\n"
            		+ "PARAM   waterPlane   = program.local[1];\n"
            		+ "PARAM   tMatrix[4]   = { state.matrix.texture[0] };\n"
            		+ "PARAM   pMatrix[4]   = { state.matrix.projection };\n"
            		+ "PARAM   mvMatrix[4]  = { state.matrix.modelview };\n"
            		+ "TEMP    viewPos, fogFactor;\nDP4   viewPos.x, mvMatrix[0], iPos;\n"
            		+ "DP4   viewPos.y, mvMatrix[1], iPos;\n"
            		+ "DP4   viewPos.z, mvMatrix[2], iPos;\n"
            		+ "DP4   viewPos.w, mvMatrix[3], iPos;\n"
            		+ "SUB   fogFactor.x, viewPos.z, fogParams.x;\n"
            		+ "MUL   fogFactor.x, fogFactor.x, 0.001953125;\n"
            		+ "MAD   fogFactor.y, iTexCoord.z, fogParams.z, fogParams.w;\n"
            		+ "SUB   fogFactor.z, viewPos.z, fogParams.y;\n"
            		+ "MUL   fogFactor.z, fogFactor.z, 0.00390625;\n"
            		+ "MUL   fogFactor.x, fogFactor.x, fogFactor.y;\n"
            		+ "MIN   fogFactor, fogFactor, 1;\n"
            		+ "MAX   fogFactor, fogFactor, 0;\n"
            		+ "MUL   fogFactor.z, fogFactor.z, iTexCoord.z;\n"
            		+ "MAD   viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw;\n"
            		+ "MAX   oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy;\n"
            		+ "MOV   oTexCoord1.w, 1;\nMOV   oColour, iColour;\n"
            		+ "DP4   oPos.x, pMatrix[0], viewPos;\n"
            		+ "DP4   oPos.y, pMatrix[1], viewPos;\n"
            		+ "DP4   oPos.z, pMatrix[2], viewPos;\n"
            		+ "DP4   oPos.w, pMatrix[3], viewPos;\n"
            		+ "MOV   oFogCoord.x, viewPos.z;\n"
            		+ "DP3   oTexCoord0.x, tMatrix[0], iTexCoord;\n"
            		+ "DP3   oTexCoord0.y, tMatrix[1], iTexCoord;\n"
            		+ "MOV   oTexCoord0.zw, iTexCoord;\n"
            		+ "END\n");
            this.waterNodepthReflection = ProgramGL.generateProgram(super.glToolkit,
            		"!!ARBvp1.0\n"
            		+ "ATTRIB  iPos         = vertex.position;\n"
            		+ "ATTRIB  iNormal      = vertex.normal;\n"
            		+ "ATTRIB  iColour      = vertex.color;\n"
            		+ "ATTRIB  iTexCoord    = vertex.texcoord[0];\n"
            		+ "OUTPUT  oPos         = result.position;\n"
            		+ "OUTPUT  oColour      = result.color;\n"
            		+ "OUTPUT  oTexCoord0   = result.texcoord[0];\n"
            		+ "OUTPUT  oTexCoord1   = result.texcoord[1];\n"
            		+ "OUTPUT  oFogCoord    = result.fogcoord;\n"
            		+ "PARAM   fogParams    = program.local[0];\n"
            		+ "PARAM   waterPlane   = program.local[1];\n"
            		+ "PARAM   tMatrix[4]   = { state.matrix.texture[0] };\n"
            		+ "PARAM   pMatrix[4]   = { state.matrix.projection };\n"
            		+ "PARAM   mvMatrix[4]  = { state.matrix.modelview };\n"
            		+ "TEMP    viewPos, viewNormal, fogFactor, colour, ndotl;\n"
            		+ "DP4   viewPos.x, mvMatrix[0], iPos;\n"
            		+ "DP4   viewPos.y, mvMatrix[1], iPos;\n"
            		+ "DP4   viewPos.z, mvMatrix[2], iPos;\n"
            		+ "DP4   viewPos.w, mvMatrix[3], iPos;\n"
            		+ "SUB   fogFactor.x, viewPos.z, fogParams.x;\n"
            		+ "MUL   fogFactor.x, fogFactor.x, 0.001953125;\n"
            		+ "MAD   fogFactor.y, iTexCoord.z, fogParams.z, fogParams.w;\n"
            		+ "SUB   fogFactor.z, viewPos.z, fogParams.y;\n"
            		+ "MUL   fogFactor.z, fogFactor.z, 0.00390625;\n"
            		+ "MUL   fogFactor.x, fogFactor.x, fogFactor.y;\n"
            		+ "MIN   fogFactor, fogFactor, 1;\n"
            		+ "MAX   fogFactor, fogFactor, 0;\n"
            		+ "MUL   fogFactor.z, fogFactor.z, iTexCoord.z;\n"
            		+ "MAD   viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw;\n"
            		+ "MAX   oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy;\n"
            		+ "MOV   oTexCoord1.w, 1;\n"
            		+ "DP3   viewNormal.x, mvMatrix[0], iNormal;\n"
            		+ "DP3   viewNormal.y, mvMatrix[1], iNormal;\n"
            		+ "DP3   viewNormal.z, mvMatrix[2], iNormal;\n"
            		+ "DP3   ndotl.x, viewNormal, state.light[0].position;\n"
            		+ "DP3   ndotl.y, viewNormal, state.light[1].position;\n"
            		+ "MAX   ndotl, ndotl, 0;\n"
            		+ "MOV   colour, state.lightmodel.ambient;\n"
            		+ "MAD   colour, state.light[0].diffuse, ndotl.xxxx, colour;\n"
            		+ "MAD   colour, state.light[1].diffuse, ndotl.yyyy, colour;\n"
            		+ "MUL   oColour, iColour, colour;\n"
            		+ "DP4   oPos.x, pMatrix[0], viewPos;\n"
            		+ "DP4   oPos.y, pMatrix[1], viewPos;\n"
            		+ "DP4   oPos.z, pMatrix[2], viewPos;\n"
            		+ "DP4   oPos.w, pMatrix[3], viewPos;\n"
            		+ "MOV   oFogCoord.x, viewPos.z;\n"
            		+ "DP3   oTexCoord0.x, tMatrix[0], iTexCoord;\n"
            		+ "DP3   oTexCoord0.y, tMatrix[1], iTexCoord;\n"
            		+ "MOV   oTexCoord0.zw, iTexCoord;\n"
            		+ "END\n");
            this.waterDepthColor = ProgramGL.generateProgram(super.glToolkit,
            		"!!ARBvp1.0\n"
            		+ "ATTRIB  iPos         = vertex.position;\n"
            		+ "ATTRIB  iColour      = vertex.color;\n"
            		+ "ATTRIB  iTexCoord    = vertex.texcoord[0];\n"
            		+ "OUTPUT  oPos         = result.position;\n"
            		+ "OUTPUT  oColour      = result.color;\n"
            		+ "OUTPUT  oTexCoord0   = result.texcoord[0];\n"
            		+ "OUTPUT  oTexCoord1   = result.texcoord[1];\n"
            		+ "OUTPUT  oFogCoord    = result.fogcoord;\n"
            		+ "PARAM   fogParams    = program.local[0];\n"
            		+ "PARAM   waterPlane   = program.local[1];\n"
            		+ "PARAM   pMatrix[4]   = { state.matrix.projection };\n"
            		+ "PARAM   mvMatrix[4]  = { state.matrix.modelview };\n"
            		+ "PARAM   texMatrix[4] = { state.matrix.texture[0] };\n"
            		+ "TEMP    viewPos, fogFactor, depth;\n"
            		+ "DP4   viewPos.x, mvMatrix[0], iPos;\n"
            		+ "DP4   viewPos.y, mvMatrix[1], iPos;\n"
            		+ "DP4   viewPos.z, mvMatrix[2], iPos;\n"
            		+ "DP4   viewPos.w, mvMatrix[3], iPos;\n"
            		+ "SUB   fogFactor.x, viewPos.z, fogParams.x;\n"
            		+ "MUL   fogFactor.x, fogFactor.x, 0.001953125;\n"
            		+ "DP4   depth, waterPlane, viewPos;\n"
            		+ "MAD   fogFactor.y, -depth, fogParams.z, fogParams.w;\n"
            		+ "SUB   fogFactor.z, viewPos.z, fogParams.y;\n"
            		+ "MUL   fogFactor.z, fogFactor.z, 0.00390625;\n"
            		+ "MIN   fogFactor, fogFactor, 1;\n"
            		+ "MAX   fogFactor, fogFactor, 0;\n"
            		+ "MUL   fogFactor.z, fogFactor.z, -depth;\n"
            		+ "MAD   viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw;\n"
            		+ "MAX   oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy;\n"
            		+ "MOV   oTexCoord1.w, 1;\nMOV   oColour, iColour;\n"
            		+ "DP4   oPos.x, pMatrix[0], viewPos;\n"
            		+ "DP4   oPos.y, pMatrix[1], viewPos;\n"
            		+ "DP4   oPos.z, pMatrix[2], viewPos;\n"
            		+ "DP4   oPos.w, pMatrix[3], viewPos;\n"
            		+ "MOV   oFogCoord.x, viewPos.z;\n"
            		+ "DP4   oTexCoord0.x, texMatrix[0], iTexCoord;\n"
            		+ "DP4   oTexCoord0.y, texMatrix[1], iTexCoord;\n"
            		+ "DP4   oTexCoord0.z, texMatrix[2], iTexCoord;\n"
            		+ "MOV   oTexCoord0.w, 1;\n"
            		+ "END\n");
            this.waterDepthReflection = ProgramGL.generateProgram(super.glToolkit,
            		"!!ARBvp1.0\n"
            		+ "ATTRIB  iPos         = vertex.position;\n"
            		+ "ATTRIB  iNormal      = vertex.normal;\n"
            		+ "ATTRIB  iColour      = vertex.color;\n"
            		+ "ATTRIB  iTexCoord    = vertex.texcoord[0];\n"
            		+ "OUTPUT  oPos         = result.position;\n"
            		+ "OUTPUT  oColour      = result.color;\n"
            		+ "OUTPUT  oTexCoord0   = result.texcoord[0];\n"
            		+ "OUTPUT  oTexCoord1   = result.texcoord[1];\n"
            		+ "OUTPUT  oFogCoord    = result.fogcoord;\n"
            		+ "PARAM   fogParams    = program.local[0];\n"
            		+ "PARAM   waterPlane   = program.local[1];\n"
            		+ "PARAM   pMatrix[4]   = { state.matrix.projection };\n"
            		+ "PARAM   mvMatrix[4]  = { state.matrix.modelview };\n"
            		+ "PARAM   texMatrix[4] = { state.matrix.texture[0] };\n"
            		+ "TEMP    viewPos, viewNormal, fogFactor, depth, colour, ndotl;\n"
            		+ "DP4   viewPos.x, mvMatrix[0], iPos;\n"
            		+ "DP4   viewPos.y, mvMatrix[1], iPos;\n"
            		+ "DP4   viewPos.z, mvMatrix[2], iPos;\n"
            		+ "DP4   viewPos.w, mvMatrix[3], iPos;\n"
            		+ "SUB   fogFactor.x, viewPos.z, fogParams.x;\n"
            		+ "MUL   fogFactor.x, fogFactor.x, 0.001953125;\n"
            		+ "DP4   depth, waterPlane, viewPos;\n"
            		+ "MAD   fogFactor.y, -depth, fogParams.z, fogParams.w;\n"
            		+ "SUB   fogFactor.z, viewPos.z, fogParams.y;\n"
            		+ "MUL   fogFactor.z, fogFactor.z, 0.00390625;\n"
            		+ "MIN   fogFactor, fogFactor, 1;\n"
            		+ "MAX   fogFactor, fogFactor, 0;\n"
            		+ "MUL   fogFactor.z, fogFactor.z, -depth;\n"
            		+ "MAD   viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw;\n"
            		+ "MAX   oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy;\n"
            		+ "MOV   oTexCoord1.w, 1;\n"
            		+ "DP3   viewNormal.x, mvMatrix[0], iNormal;\n"
            		+ "DP3   viewNormal.y, mvMatrix[1], iNormal;\n"
            		+ "DP3   viewNormal.z, mvMatrix[2], iNormal;\n"
            		+ "DP3   ndotl.x, viewNormal, state.light[0].position;\n"
            		+ "DP3   ndotl.y, viewNormal, state.light[1].position;\n"
            		+ "MAX   ndotl, ndotl, 0;\n"
            		+ "MOV   colour, state.lightmodel.ambient;\n"
            		+ "MAD   colour, state.light[0].diffuse, ndotl.xxxx, colour;\n"
            		+ "MAD   colour, state.light[1].diffuse, ndotl.yyyy, colour;\n"
            		+ "MUL   oColour, iColour, colour;\n"
            		+ "DP4   oPos.x, pMatrix[0], viewPos;\n"
            		+ "DP4   oPos.y, pMatrix[1], viewPos;\n"
            		+ "DP4   oPos.z, pMatrix[2], viewPos;\n"
            		+ "DP4   oPos.w, pMatrix[3], viewPos;\n"
            		+ "MOV   oFogCoord.x, viewPos.z;\n"
            		+ "DP4   oTexCoord0.x, texMatrix[0], iTexCoord;\n"
            		+ "DP4   oTexCoord0.y, texMatrix[1], iTexCoord;\n"
            		+ "DP4   oTexCoord0.z, texMatrix[2], iTexCoord;\n"
            		+ "MOV   oTexCoord0.w, 1;\n"
            		+ "END\n");
            if (this.waterNodepthColor != null & this.waterNodepthReflection != null & this.waterDepthColor != null & this.waterDepthReflection != null) {
                this.openglTexture = new OpenGl2DTexture(openglToolkit, GL_TEXTURE_2D, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, 2, 1, false, new byte[]{(byte) 0, (byte) -1}, GLFormat.RGBA, false);
                this.openglTexture.setRepeat(false, false);
                this.initialized = true;
            } else {
                this.initialized = false;
            }
        } else {
            this.initialized = false;
        }
    }

    @Override
    public void loadCombineMode() {
    }

    @Override
    public void toggle(boolean lights) {
    	//System.out.println("Enabled material effect "+this.getClass().getSimpleName());
    	if(Loader.MATERIAL_EFFECTS_DISABLED[3]) return;
        this.calculateLights = lights;
        super.glToolkit.activateTexture(1);
        super.glToolkit.setActiveTexture(this.openglTexture);
        super.glToolkit.loadTextureCombineMode(GL_INTERPOLATE, GL_REPLACE);
        super.glToolkit.setTexture(0, GL_CONSTANT, GL_SRC_COLOR);
        super.glToolkit.setTexture(2, GL_TEXTURE, GL_SRC_ALPHA);
        super.glToolkit.setTextureTransparency(0, GL_PREVIOUS);
        super.glToolkit.activateTexture(0);
        enable();
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    public void enable() {
        Matrix44 matrix = super.glToolkit.aClass3848348;
        if (this.calculateLights) {
            OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, super.glToolkit.waterDepth == Integer.MAX_VALUE ? this.waterNodepthReflection.programHandle : this.waterDepthReflection.programHandle);
        } else {
            OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, super.glToolkit.waterDepth == Integer.MAX_VALUE ? this.waterNodepthColor.programHandle : this.waterDepthColor.programHandle);
        }

        float depth = super.glToolkit.waterDepth;
        float scaleX = matrix.faceList[4] * depth + matrix.faceList[12];
        float scaleY = matrix.faceList[5] * depth + matrix.faceList[13];
        float scaleZ = matrix.faceList[6] * depth + matrix.faceList[14];
        XYZW[0] = -matrix.faceList[4];
        XYZW[1] = -matrix.faceList[5];
        XYZW[2] = -matrix.faceList[6];
        XYZW[3] = -(XYZW[0] * scaleX + XYZW[1] * scaleY + XYZW[2] * scaleZ);
        OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 1, XYZW[0], XYZW[1], XYZW[2], XYZW[3]); 
        OpenGL.glEnable(GL_VERTEX_PROGRAM_ARB);
        this.bound = true;
        activate();
    }

    public void activate() {
        if (this.bound) {
            float f_1 = super.glToolkit.aFloat8404;
            float f_2 = super.glToolkit.fogMinimumStartPosition;
            float f_3 = f_1 - (f_1 - f_2) * 0.125F;
            float f_4 = f_1 - (f_1 - f_2) * 0.25F;
            OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 0, f_4, f_3, 256.0F / super.glToolkit.waterTile.scale, super.glToolkit.waterTile.intensity / 255.0F);
            super.glToolkit.activateTexture(1);
            super.glToolkit.setTextureEnvColor(super.glToolkit.waterTile.color);
            super.glToolkit.activateTexture(0);
        }
    }

    @Override
    public void loadCombiner(OpenGlTexture texture, int combineMode) {
        if (texture == null) {
            if (!this.aBool9073) {
                super.glToolkit.setActiveTexture(super.glToolkit.texture2d);
                super.glToolkit.loadTextureCombineMode(1);
                super.glToolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR);
                super.glToolkit.setTextureTransparency(0, GL_PREVIOUS);
                this.aBool9073 = true;
            }
        } else {
            if (this.aBool9073) {
                super.glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
                super.glToolkit.setTextureTransparency(0, GL_TEXTURE);
                this.aBool9073 = false;
            }
            super.glToolkit.setActiveTexture(texture);
            super.glToolkit.loadTextureCombineMode(combineMode);
        }
    }

    @Override
    public void applyEffectParams(int param1, int param2) {
    	// NOOP
    }

    @Override
    public void disable() {
        if (this.bound) {
            OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, 0);
            OpenGL.glDisable(GL_FRAGMENT_PROGRAM_ARB);
            OpenGL.glDisable(GL_VERTEX_PROGRAM_ARB);
            this.bound = false;
        }

        super.glToolkit.activateTexture(1);
        super.glToolkit.setActiveTexture(null);
        super.glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE);
        super.glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
        super.glToolkit.setTexture(2, GL_CONSTANT, GL_SRC_ALPHA);
        super.glToolkit.setTextureTransparency(0, GL_TEXTURE);
        super.glToolkit.activateTexture(0);
        if (this.aBool9073) {
            super.glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR);
            super.glToolkit.setTextureTransparency(0, GL_TEXTURE);
            this.aBool9073 = false;
        }
    }
}
