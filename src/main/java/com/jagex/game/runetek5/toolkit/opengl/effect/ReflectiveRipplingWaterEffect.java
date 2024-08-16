package com.jagex.game.runetek5.toolkit.opengl.effect;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.graphics.shader.OpenGLShaderManager;
import com.jagex.game.runetek5.graphics.shader.OpenGlShader;
import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture;
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import jaggl.OpenGL;

/**
 * 
 * @author Steven - 19/07/2023
 * Handles display of floor water tiles
 * Shows water with waves that reflects light
 * Tested by adding this.material_effects[4] = this.material_effects[8]; 
 * at the bottom of the constructor of
 * {@link com.jagex.game.runetek5.toolkit.opengl.EffectManagerGL}
 *
 */
public class ReflectiveRipplingWaterEffect extends MaterialEffectGL {

    public boolean active;
    public boolean initialized;
    public final TextureContainerGL textures;
    public OpenGLShaderManager shaderManager;

    public ReflectiveRipplingWaterEffect(OpenGlToolkit openglToolkit, TextureContainerGL textureContainer) {
        super(openglToolkit);
        this.textures = textureContainer;
        if (this.textures.hdrTexture3d != null && super.glToolkit.arbVertexShader && super.glToolkit.arbFragmentShader) {
            OpenGlShader vertexShader = OpenGlShader.createShader(super.glToolkit, GL_VERTEX_SHADER,
            		"uniform float time;\n"
            		+ "uniform float scale;\n"
            		+ "varying vec3 wvVertex;\n"
            		+ "varying float waterDepth;\n"
            		+ "void main() {\n"
            		+ "waterDepth = gl_MultiTexCoord0.z;\n"
            		+ "vec4 ecVertex = gl_ModelViewMatrix*gl_Vertex;\n"
            		+ "wvVertex.x = dot(gl_NormalMatrix[0], ecVertex.xyz);\n"
            		+ "wvVertex.y = dot(gl_NormalMatrix[1], ecVertex.xyz);\n"
            		+ "wvVertex.z = dot(gl_NormalMatrix[2], ecVertex.xyz);\n"
            		+ "gl_TexCoord[0].x = dot(gl_TextureMatrix[0][0], gl_MultiTexCoord0)*scale;\n"
            		+ "gl_TexCoord[0].y = dot(gl_TextureMatrix[0][1], gl_MultiTexCoord0)*scale;\n"
            		+ "gl_TexCoord[0].z = time;\ngl_TexCoord[0].w = 1.0;\n"
            		+ "gl_FogFragCoord = clamp((ecVertex.z-gl_Fog.start)*gl_Fog.scale, 0.0, 1.0);\n"
            		+ "gl_Position = ftransform();\n}\n");
            OpenGlShader fragmentShader = OpenGlShader.createShader(super.glToolkit, GL_FRAGMENT_SHADER,
            		"varying vec3 wvVertex;\n"
            		+ "varying float waterDepth;\n"
            		+ "uniform vec3 sunDir;\n"
            		+ "uniform vec4 sunColour;\n"
            		+ "uniform float sunExponent;\n"
            		+ "uniform float breakWaterDepth;\n"
            		+ "uniform float breakWaterOffset;\n"
            		+ "uniform sampler3D normalSampler;\n"
            		+ "uniform samplerCube envMapSampler;\n"
            		+ "void main() {\nvec4 wnNormal = texture3D(normalSampler, gl_TexCoord[0].xyz).rbga;\n"
            		+ "wnNormal.xyz = 2.0*wnNormal.xyz-1.0;\n"
            		+ "vec3 wnVector = normalize(wvVertex);\n"
            		+ "vec3 wnReflection = reflect(wnVector, wnNormal.xyz);\n"
            		+ "vec3 envColour = textureCube(envMapSampler, wnReflection).rgb;\n"
            		+ "vec4 specularColour = sunColour*pow(clamp(-dot(sunDir, wnReflection), 0.0, 1.0), sunExponent);\n"
            		+ "float shoreFactor = clamp(waterDepth/breakWaterDepth-breakWaterOffset*wnNormal.w, 0.0, 1.0);\n"
            		+ "float ndote = dot(wnVector, wnNormal.xyz);\n"
            		+ "float fresnel = pow(1.0-abs(ndote), 2.0);\n"
            		+ "vec4 surfaceColour = vec4(envColour, fresnel*shoreFactor)+specularColour*shoreFactor;\n"
            		+ "gl_FragColor = vec4(mix(surfaceColour.rgb, gl_Fog.color.rgb, gl_FogFragCoord), surfaceColour.a);\n}\n");
            this.shaderManager = OpenGLShaderManager.addShaders(super.glToolkit, new OpenGlShader[]{vertexShader, fragmentShader});
            this.initialized = this.shaderManager != null;
        }
    }

    @Override
    public void toggle(boolean enabled) {
    	System.out.println("Enabled material effect "+this.getClass().getSimpleName());
    	if(Loader.MATERIAL_EFFECTS_DISABLED[8]) return;
        OpenGlArbCubeMapTexture texture = super.glToolkit.getCubeMap();
        if (this.initialized && texture != null) {
            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(texture);
            super.glToolkit.activateTexture(0);
            super.glToolkit.setActiveTexture(this.textures.hdrTexture3d);
            int handle = this.shaderManager.programHandle;
            OpenGL.glUseProgram(handle);
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "normalSampler"), 0);
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "envMapSampler"), 1);
            OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "sunDir"), -super.glToolkit.sunDirection[0], -super.glToolkit.sunDirection[1], -super.glToolkit.sunDirection[2]);
            OpenGL.glUniform4f(OpenGL.glGetUniformLocation(handle, "sunColour"), super.glToolkit.sunColorRed, super.glToolkit.sunColorGreen, super.glToolkit.sunColorBlue, 1.0F);
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "sunExponent"), 96.0F + Math.abs(super.glToolkit.sunDirection[1]) * 928.0F);
            this.active = true;
        }
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void disable() {
        if (this.active) {
            super.glToolkit.activateTexture(1);
            super.glToolkit.setActiveTexture(null);
            super.glToolkit.activateTexture(0);
            super.glToolkit.setActiveTexture(null);
            OpenGL.glUseProgram(0);
            this.active = false;
        }
    }

    @Override
    public void loadCombineMode() {
    }

    @Override
    public void loadCombiner(OpenGlTexture texture, int combineMode) {
        if (!this.active) {
            super.glToolkit.setActiveTexture(texture);
            super.glToolkit.loadTextureCombineMode(combineMode);
        }
    }

    @Override
    public void applyEffectParams(int effectParam1, int effectParam2) {
        if (this.active) {
            int delta = 1 << (effectParam1 & 0x3);
            float waterScale = (1 << (effectParam1 >> 3 & 0x7)) / 32.0F;
            int breakWaterDepth = effectParam2 & 0xffff;
            float breakWaterOffset = (effectParam2 >> 16 & 0x3) / 8.0F;
            int handle = this.shaderManager.programHandle;
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "time"), (super.glToolkit.textureAnimatingProgression * delta % 40000) / 40000.0F);
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "scale"), waterScale);
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "breakWaterDepth"), breakWaterDepth);
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(handle, "breakWaterOffset"), breakWaterOffset);
        }
    }
}
