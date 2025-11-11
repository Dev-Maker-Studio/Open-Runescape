package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.runetek5.config.material.texture.operation.TextureOpPerlinNoise;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils;
import com.jagex.game.runetek5.toolkit.opengl.ProgramGL;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.launch.Loader;

/**
 * 
 * @author Steven - 19/07/2023
 * Handles the display of water that moves just barely back and forth
 * Example at coords: 2819, 3450, 0
 *
 */
public class LappingWaterEffect extends MaterialEffectGL {

    public static final float[] SUN_COLOR = new float[4];
    public GlListUtils listUtils;
    public float texture3dAnimatingProgress;
    public int animatingProgression;
    public final TextureContainerGL textureContainer;
    public ProgramGL program;
    public float[] noise;

    public LappingWaterEffect(OpenGlToolkit toolkit, TextureContainerGL textureContainer) {
        super(toolkit);
        this.textureContainer = textureContainer;
        if (super.glToolkit.arbVertexProgram && super.glToolkit.maxTextureUnits >= 2) {
            this.program = ProgramGL.generateProgram(super.glToolkit, "// Vertex Program in ARB_vp1.0 (deprecated)"
            		+ "// Enable the ARB_position_invariant option to specify that the vertex program output is position invariant."
            		+ "!!ARBvp1.0"
            		+ "OPTION ARB_position_invariant;"
            		+ "// Define input attributes"
            		+ "ATTRIB iPos      = vertex.position; // Input vertex position"
            		+ "ATTRIB iColour   = vertex.color;    // Input vertex color"
            		+ "ATTRIB iTexCoord = vertex.texcoord[0]; // Input vertex texture coordinates"
            		+ "// Define output attributes"
            		+ "OUTPUT oColour    = result.color;    // Output vertex color"
            		+ "OUTPUT oTexCoord0 = result.texcoord[0]; // Output vertex texture coordinates for texture unit 0"
            		+ "OUTPUT oTexCoord1 = result.texcoord[1]; // Output vertex texture coordinates for texture unit 1"
            		+ "OUTPUT oFogCoord  = result.fogcoord;  // Output vertex fog coordinates"
            		+ "// Define parameter constants (uniforms)"
            		+ "PARAM time         = program.local[65];  // Time parameter"
            		+ "PARAM turbulence   = program.local[64];  // Turbulence parameter"
            		+ "PARAM lightAmbient = program.local[66];  // Light ambient parameter"
            		+ "PARAM pMatrix[4]   = { state.matrix.projection };  // Projection matrix"
            		+ "PARAM mvMatrix[4]  = { state.matrix.modelview };   // Model-View matrix"
            		+ "PARAM ivMatrix[4]  = { state.matrix.texture[1] };  // Inverse texture matrix for texture unit 1"
            		+ "PARAM texMatrix[4] = { state.matrix.texture[0] };  // Texture matrix for texture unit 0"
            		+ "PARAM fNoise[64]   = { program.local[0..63] };    // Noise array"
            		+ "// Temporary registers for intermediate calculations"
            		+ "TEMP noise, viewPos, worldPos, texCoord;"
            		+ "ADDRESS noiseAddr;"
            		+ "// Calculate vertex position in view space"
            		+ "DP4 viewPos.x, mvMatrix[0], iPos;"
            		+ "DP4 viewPos.y, mvMatrix[1], iPos;"
            		+ "DP4 viewPos.z, mvMatrix[2], iPos;"
            		+ "DP4 viewPos.w, mvMatrix[3], iPos;"
            		+ "// Output fog coordinate from the z-component of the view position"
            		+ "MOV oFogCoord.x, viewPos.z;"
            		+ "// Transform the vertex position from view space to world space using the inverse texture matrix"
            		+ "DP4 worldPos.x, ivMatrix[0], viewPos;"
            		+ "DP4 worldPos.y, ivMatrix[1], viewPos;"
            		+ "DP4 worldPos.z, ivMatrix[2], viewPos;"
            		+ "DP4 worldPos.w, ivMatrix[3], viewPos;"
            		+ "// Create noise offsets by combining the x and z components of the world position"
            		+ "ADD noise.x, worldPos.x, worldPos.z;"
            		+ "SUB noise.y, worldPos.z, worldPos.x;"
            		+ "// Scale the noise offsets and wrap them around using the fractional part function (FRC)"
            		+ "MUL noise, noise, 0.0001220703125; // 1/8192, approximate reciprocal of 8192"
            		+ "FRC noise, noise;"
            		+ "MUL noise, noise, 64;"
            		+ "// Calculate the address for the noise lookup in the fNoise array"
            		+ "ARL noiseAddr.x, noise.x;"
            		+ "// Lookup the noise value from the fNoise array"
            		+ "MOV noise.x, fNoise[noiseAddr.x].x;"
            		+ "// Repeat the same process for the y component of the noise offset"
            		+ "ARL noiseAddr.x, noise.y;"
            		+ "MOV noise.y, fNoise[noiseAddr.x].y;"
            		+ "// Apply turbulence to the noise offsets"
            		+ "MUL noise, noise, turbulence.x;"
            		+ "// Calculate texture coordinates for texture unit 0"
            		+ "DP4 texCoord.x, texMatrix[0], iTexCoord;"
            		+ "DP4 texCoord.y, texMatrix[1], iTexCoord;"
            		+ "// Apply the noise offsets to the texture coordinates for texture unit 0"
            		+ "ADD oTexCoord0.xy, texCoord, noise;"
            		+ "MOV oTexCoord0.z, 0;"
            		+ "MOV oTexCoord0.w, 1;"
            		+ "// Calculate texture coordinates for texture unit 1 (used for animation)"
            		+ "MUL oTexCoord1.xy, texCoord, 0.125; // Scaled down by 1/8"
            		+ "MOV oTexCoord1.zw, time.xxxw;      // Set the time parameter for animation"
            		+ "// Apply lighting by multiplying the input vertex color with the light ambient parameter"
            		+ "MUL oColour.xyz, iColour, lightAmbient;"
            		+ "// Copy the alpha component of the input vertex color to the output vertex color"
            		+ "MOV oColour.w, iColour.w;"
            		+ "// End of the vertex shader"
            		+ "END");
            if (this.program != null) {
                int[][] ints_3 = TextureOpPerlinNoise.generateNoiseMap();
                int[][] ints_4 = TextureOpPerlinNoise.generateNoiseMap();
                int i_5 = 0;
                this.noise = new float[32768];

                for (int index = 0; index < 256; index++) {
                    int[] ints_7 = ints_3[index];
                    int[] ints_8 = ints_4[index];

                    for (int i_9 = 0; i_9 < 64; i_9++) {
                        this.noise[i_5++] = ints_7[i_9] / 4096.0F;
                        this.noise[i_5++] = ints_8[i_9] / 4096.0F;
                    }
                }

                enableProgram();
            }
        }
    }
    
    public void enableProgram() {
        this.listUtils = new GlListUtils(2);
        this.listUtils.newList(0);
        super.glToolkit.activateTexture(1);
        super.glToolkit.setTextureEnvColor(~0xffffff);
        super.glToolkit.loadTextureCombineMode(GL_ADD, GL_REPLACE);
        super.glToolkit.setTextureTransparency(0, GL_CONSTANT);
        super.glToolkit.activateTexture(0);
        OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, this.program.programHandle);
        OpenGL.glEnable(GL_VERTEX_PROGRAM_ARB);
        this.listUtils.endList();
        this.listUtils.newList(1);
        super.glToolkit.activateTexture(1);
        OpenGL.glMatrixMode(GL_TEXTURE);
        OpenGL.glLoadIdentity();
        OpenGL.glMatrixMode(GL_MODELVIEW);
        super.glToolkit.loadTextureCombineMode(0);
        super.glToolkit.setTextureTransparency(0, GL_TEXTURE);
        super.glToolkit.activateTexture(0);
        OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, 0);
        OpenGL.glDisable(GL_VERTEX_PROGRAM_ARB);
        OpenGL.glDisable(GL_FRAGMENT_PROGRAM_ARB);
        this.listUtils.endList();
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public void toggle(boolean bool1) {
    	//System.out.println("Enabled material effect "+this.getClass().getSimpleName());
    	if(Loader.MATERIAL_EFFECTS_DISABLED[2]) return;
        if (this.listUtils != null) {
            this.listUtils.callList(0);
            super.glToolkit.activateTexture(1);
            OpenGL.glMatrixMode(GL_TEXTURE);
            OpenGL.glLoadMatrixf(super.glToolkit.aClass3848391.faceList, 0);
            OpenGL.glMatrixMode(GL_MODELVIEW);
            super.glToolkit.activateTexture(0);
            if (super.glToolkit.textureAnimatingProgression != this.animatingProgression) {
                int progress = super.glToolkit.textureAnimatingProgression % 5000 * 128 / 5000;

                for (int index = 0; index < 64; index++) {
                    OpenGL.glProgramLocalParameter4fvARB(GL_VERTEX_PROGRAM_ARB, index, this.noise, progress);
                    progress += 2;
                }

                if (this.textureContainer.texture3dSupported) {
                    this.texture3dAnimatingProgress = (super.glToolkit.textureAnimatingProgression % 4000) / 4000.0F;
                } else {
                    OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 65, 0.0F, 0.0F, 0.0F, 1.0F);
                }

                this.animatingProgression = super.glToolkit.textureAnimatingProgression;
            }
        }
    }

    @Override
    public void loadCombineMode() {
    	// NOOP
    }

    @Override
    public void disable() {
        if (this.listUtils != null) {
            this.listUtils.callList(1);
            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(null);
            super.glToolkit.activateTexture(0);
        }
    }

    @Override
    public void applyEffectParams(int param1, int param2) {
        if (this.listUtils != null) {
            super.glToolkit.activateTexture(1);
            if ((param1 & 0x80) == 0) {
                if ((param2 & 0x1) == 1) {
                    if (!this.textureContainer.texture3dSupported) {
                        int progress = super.glToolkit.textureAnimatingProgression % 4000 * 16 / 4000;
                        super.glToolkit.setActiveTexture(this.textureContainer.textures2d[progress]);
                        OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 65, 0.0F, 0.0F, 0.0F, 1.0F);
                    } else {
                        super.glToolkit.setActiveTexture(this.textureContainer.texture3d);
                        OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 65, this.texture3dAnimatingProgress, 0.0F, 0.0F, 1.0F);
                    }
                } else {
                    if (this.textureContainer.texture3dSupported) {
                        super.glToolkit.setActiveTexture(this.textureContainer.texture3d);
                    } else {
                        super.glToolkit.setActiveTexture(this.textureContainer.textures2d[0]);
                    }

                    OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 65, 0.0F, 0.0F, 0.0F, 1.0F);
                }
            } else {
                super.glToolkit.setActiveTexture(null);
            }

            super.glToolkit.activateTexture(0);
            if ((param1 & 0x40) == 0) {
                SUN_COLOR[0] = super.glToolkit.sunBrightness * super.glToolkit.sunColorRed;
                SUN_COLOR[1] = super.glToolkit.sunBrightness * super.glToolkit.sunColorGreen;
                SUN_COLOR[2] = super.glToolkit.sunBrightness * super.glToolkit.sunColorBlue;
                OpenGL.glProgramLocalParameter4fvARB(GL_VERTEX_PROGRAM_ARB, 66, SUN_COLOR, 0);
            } else {
                OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 66, 1.0F, 1.0F, 1.0F, 1.0F);
            }

            int type = param1 & 0x3;
            if (type == 2) {
                OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 64, 0.05F, 1.0F, 1.0F, 1.0F);
            } else if (type == 3) {
                OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 64, 0.1F, 1.0F, 1.0F, 1.0F);
            } else {
                OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 64, 0.025F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public void loadCombiner(OpenGlTexture texture, int combineMode) {
        super.glToolkit.setActiveTexture(texture);
        super.glToolkit.loadTextureCombineMode(combineMode);
    }
}