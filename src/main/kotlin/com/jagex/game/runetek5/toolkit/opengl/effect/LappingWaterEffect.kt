package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.runetek5.config.material.texture.operation.TextureOpPerlinNoise
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.toolkit.opengl.ProgramGL
import com.jagex.game.launch.Loader
import jaggl.OpenGL

/**
 * @author Steven - 19/07/2023
 * Handles the display of water that moves just barely back and forth
 * Example at coords: 2819, 3450, 0
 */
class LappingWaterEffect(
    toolkit: OpenGlToolkit,
    @JvmField val textureContainer: TextureContainerGL
) : MaterialEffectGL(toolkit) {

    @JvmField
    var listUtils: GlListUtils? = null

    @JvmField
    var texture3dAnimatingProgress: Float = 0f

    @JvmField
    var animatingProgression: Int = 0

    @JvmField
    var program: ProgramGL? = null

    @JvmField
    var noise: FloatArray? = null

    init {
        if (glToolkit.arbVertexProgram && glToolkit.maxTextureUnits >= 2) {
            program = ProgramGL.generateProgram(
                glToolkit, """// Vertex Program in ARB_vp1.0 (deprecated)// Enable the ARB_position_invariant option to specify that the vertex program output is position invariant.!!ARBvp1.0OPTION ARB_position_invariant;// Define input attributesATTRIB iPos      = vertex.position; // Input vertex positionATTRIB iColour   = vertex.color;    // Input vertex colorATTRIB iTexCoord = vertex.texcoord[0]; // Input vertex texture coordinates// Define output attributesOUTPUT oColour    = result.color;    // Output vertex colorOUTPUT oTexCoord0 = result.texcoord[0]; // Output vertex texture coordinates for texture unit 0OUTPUT oTexCoord1 = result.texcoord[1]; // Output vertex texture coordinates for texture unit 1OUTPUT oFogCoord  = result.fogcoord;  // Output vertex fog coordinates// Define parameter constants (uniforms)PARAM time         = program.local[65];  // Time parameterPARAM turbulence   = program.local[64];  // Turbulence parameterPARAM lightAmbient = program.local[66];  // Light ambient parameterPARAM pMatrix[4]   = { state.matrix.projection };  // Projection matrixPARAM mvMatrix[4]  = { state.matrix.modelview };   // Model-View matrixPARAM ivMatrix[4]  = { state.matrix.texture[1] };  // Inverse texture matrix for texture unit 1PARAM texMatrix[4] = { state.matrix.texture[0] };  // Texture matrix for texture unit 0PARAM fNoise[64]   = { program.local[0..63] };    // Noise array// Temporary registers for intermediate calculationsTEMP noise, viewPos, worldPos, texCoord;ADDRESS noiseAddr;// Calculate vertex position in view spaceDP4 viewPos.x, mvMatrix[0], iPos;DP4 viewPos.y, mvMatrix[1], iPos;DP4 viewPos.z, mvMatrix[2], iPos;DP4 viewPos.w, mvMatrix[3], iPos;// Output fog coordinate from the z-component of the view positionMOV oFogCoord.x, viewPos.z;// Transform the vertex position from view space to world space using the inverse texture matrixDP4 worldPos.x, ivMatrix[0], viewPos;DP4 worldPos.y, ivMatrix[1], viewPos;DP4 worldPos.z, ivMatrix[2], viewPos;DP4 worldPos.w, ivMatrix[3], viewPos;// Create noise offsets by combining the x and z components of the world positionADD noise.x, worldPos.x, worldPos.z;SUB noise.y, worldPos.z, worldPos.x;// Scale the noise offsets and wrap them around using the fractional part function (FRC)MUL noise, noise, 0.0001220703125; // 1/8192, approximate reciprocal of 8192FRC noise, noise;MUL noise, noise, 64;// Calculate the address for the noise lookup in the fNoise arrayARL noiseAddr.x, noise.x;// Lookup the noise value from the fNoise arrayMOV noise.x, fNoise[noiseAddr.x].x;// Repeat the same process for the y component of the noise offsetARL noiseAddr.x, noise.y;MOV noise.y, fNoise[noiseAddr.x].y;// Apply turbulence to the noise offsetsMUL noise, noise, turbulence.x;// Calculate texture coordinates for texture unit 0DP4 texCoord.x, texMatrix[0], iTexCoord;DP4 texCoord.y, texMatrix[1], iTexCoord;// Apply the noise offsets to the texture coordinates for texture unit 0ADD oTexCoord0.xy, texCoord, noise;MOV oTexCoord0.z, 0;MOV oTexCoord0.w, 1;// Calculate texture coordinates for texture unit 1 (used for animation)MUL oTexCoord1.xy, texCoord, 0.125; // Scaled down by 1/8MOV oTexCoord1.zw, time.xxxw;      // Set the time parameter for animation// Apply lighting by multiplying the input vertex color with the light ambient parameterMUL oColour.xyz, iColour, lightAmbient;// Copy the alpha component of the input vertex color to the output vertex colorMOV oColour.w, iColour.w;// End of the vertex shaderEND"""
            )
            if (program != null) {
                val noiseMapX = TextureOpPerlinNoise.generateNoiseMap()
                val noiseMapY = TextureOpPerlinNoise.generateNoiseMap()
                var offset = 0
                noise = FloatArray(32768)

                for (index in 0 until 256) {
                    val rowX = noiseMapX[index]
                    val rowY = noiseMapY[index]

                    for (i in 0 until 64) {
                        noise!![offset++] = rowX[i] / 4096.0f
                        noise!![offset++] = rowY[i] / 4096.0f
                    }
                }

                enableProgram()
            }
        }
    }

    fun enableProgram() {
        listUtils = GlListUtils(2)
        listUtils!!.newList(0)
        glToolkit.activateTexture(1)
        glToolkit.setTextureEnvColor(0xffffff.inv())
        glToolkit.loadTextureCombineMode(GL_ADD, GL_REPLACE)
        glToolkit.setTextureTransparency(0, GL_CONSTANT)
        glToolkit.activateTexture(0)
        OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, program!!.programHandle)
        OpenGL.glEnable(GL_VERTEX_PROGRAM_ARB)
        listUtils!!.endList()
        listUtils!!.newList(1)
        glToolkit.activateTexture(1)
        OpenGL.glMatrixMode(GL_TEXTURE)
        OpenGL.glLoadIdentity()
        OpenGL.glMatrixMode(GL_MODELVIEW)
        glToolkit.loadTextureCombineMode(0)
        glToolkit.setTextureTransparency(0, GL_TEXTURE)
        glToolkit.activateTexture(0)
        OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, 0)
        OpenGL.glDisable(GL_VERTEX_PROGRAM_ARB)
        OpenGL.glDisable(GL_FRAGMENT_PROGRAM_ARB)
        listUtils!!.endList()
    }

    override fun isInitialized(): Boolean {
        return true
    }

    override fun toggle(enabled: Boolean) {
        //System.out.println("Enabled material effect "+this.getClass().getSimpleName());
        if (Loader.MATERIAL_EFFECTS_DISABLED[2]) return
        if (listUtils != null) {
            listUtils!!.callList(0)
            glToolkit.activateTexture(1)
            OpenGL.glMatrixMode(GL_TEXTURE)
            OpenGL.glLoadMatrixf(glToolkit.inverseSunMatrix44.elements, 0)
            OpenGL.glMatrixMode(GL_MODELVIEW)
            glToolkit.activateTexture(0)
            if (glToolkit.textureAnimatingProgression != animatingProgression) {
                var noiseOffset = glToolkit.textureAnimatingProgression % 5000 * 128 / 5000

                for (index in 0 until 64) {
                    OpenGL.glProgramLocalParameter4fvARB(GL_VERTEX_PROGRAM_ARB, index, noise, noiseOffset)
                    noiseOffset += 2
                }

                if (textureContainer.texture3dSupported) {
                    texture3dAnimatingProgress = (glToolkit.textureAnimatingProgression % 4000) / 4000.0f
                } else {
                    OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 65, 0.0f, 0.0f, 0.0f, 1.0f)
                }

                animatingProgression = glToolkit.textureAnimatingProgression
            }
        }
    }

    override fun loadCombineMode() {
        // NOOP
    }

    override fun disable() {
        if (listUtils != null) {
            listUtils!!.callList(1)
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(null)
            glToolkit.activateTexture(0)
        }
    }

    override fun applyEffectParams(effectParam1: Int, effectParam2: Int) {
        if (listUtils != null) {
            glToolkit.activateTexture(1)
            if ((effectParam1 and 0x80) == 0) {
                if ((effectParam2 and 0x1) == 1) {
                    if (!textureContainer.texture3dSupported) {
                        val frame = glToolkit.textureAnimatingProgression % 4000 * 16 / 4000
                        glToolkit.setActiveTexture(textureContainer.textures2d!![frame])
                        OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 65, 0.0f, 0.0f, 0.0f, 1.0f)
                    } else {
                        glToolkit.setActiveTexture(textureContainer.texture3d)
                        OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 65, texture3dAnimatingProgress, 0.0f, 0.0f, 1.0f)
                    }
                } else {
                    if (textureContainer.texture3dSupported) {
                        glToolkit.setActiveTexture(textureContainer.texture3d)
                    } else {
                        glToolkit.setActiveTexture(textureContainer.textures2d!![0])
                    }

                    OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 65, 0.0f, 0.0f, 0.0f, 1.0f)
                }
            } else {
                glToolkit.setActiveTexture(null)
            }

            glToolkit.activateTexture(0)
            if ((effectParam1 and 0x40) == 0) {
                SUN_COLOR[0] = glToolkit.sunBrightness * glToolkit.sunColorRed
                SUN_COLOR[1] = glToolkit.sunBrightness * glToolkit.sunColorGreen
                SUN_COLOR[2] = glToolkit.sunBrightness * glToolkit.sunColorBlue
                OpenGL.glProgramLocalParameter4fvARB(GL_VERTEX_PROGRAM_ARB, 66, SUN_COLOR, 0)
            } else {
                OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 66, 1.0f, 1.0f, 1.0f, 1.0f)
            }

            val turbulenceLevel = effectParam1 and 0x3
            if (turbulenceLevel == 2) {
                OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 64, 0.05f, 1.0f, 1.0f, 1.0f)
            } else if (turbulenceLevel == 3) {
                OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 64, 0.1f, 1.0f, 1.0f, 1.0f)
            } else {
                OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 64, 0.025f, 1.0f, 1.0f, 1.0f)
            }
        }
    }

    override fun loadCombiner(combiner: OpenGlTexture?, combineMode: Int) {
        glToolkit.setActiveTexture(combiner)
        glToolkit.loadTextureCombineMode(combineMode)
    }

    companion object {
        @JvmField
        val SUN_COLOR = FloatArray(4)
    }
}
