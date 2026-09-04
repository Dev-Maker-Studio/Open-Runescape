package com.jagex.game.runetek5.graphics.modern.postprocess

import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.framebuffer.FilterType
import com.jagex.game.runetek5.graphics.modern.framebuffer.Framebuffer
import com.jagex.game.runetek5.utils.MathUtils
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_CULL_FACE
import org.lwjgl.opengl.GL11.GL_DEPTH_TEST
import org.lwjgl.opengl.GL11.GL_LINEAR
import org.lwjgl.opengl.GL11.GL_SCISSOR_TEST
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glDrawArrays
import org.lwjgl.opengl.GL11.glViewport
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.GL_TEXTURE1
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER
import org.lwjgl.opengl.GL30.glBindFramebuffer
import org.lwjgl.opengl.GL30.glBlitFramebuffer

/**
 * Native bloom for the modern renderer, replicating the legacy RectangularTextureGL
 * post-process without touching the fixed-function FramebufferCompositor:
 *
 *  1. capture: the game window is redirected into a window-sized colour+depth FBO
 *     (the multisampled scene target resolves into it like into any bound target);
 *  2. downsample: the capture is halved (bilinear) through power-of-two levels until
 *     both sides are <= 256 — the same chain the legacy mipmapped path draws;
 *  3. bright pass: `col * step(whitePoint, luminance)` into a 256x256 buffer;
 *  4. separable 17-tap Gaussian blur, horizontal then vertical, 1/256 steps;
 *  5. composite over the window: `scene * (postLum/preLum) + bloom * brightpass`,
 *     the exact legacy tone curve.
 *
 * All draws are fullscreen triangles from `gl_VertexID` on an empty VAO.
 */
class ModernBloomPass {
    private var sceneFbo: Framebuffer? = null
    private val levels = ArrayList<Framebuffer>()
    private var bloomA: Framebuffer? = null
    private var bloomB: Framebuffer? = null
    private var brightShader: ShaderProgram? = null
    private var blurShader: ShaderProgram? = null
    private var compositeShader: ShaderProgram? = null
    private var vao: VertexArray? = null

    var width = 0
        private set
    var height = 0
        private set

    /** True between a successful [begin] and the matching [composite]/[abort]. */
    var active = false
        private set

    /** Set once a GL resource could not be created; the pass then refuses to run. */
    var failed = false
        private set

    var composites = 0L
        private set

    private fun ensureShaders(): Boolean {
        if (brightShader != null) return true
        return try {
            brightShader = ShaderProgram.createFromSource(FULLSCREEN_VERT, BRIGHT_FRAG)
            blurShader = ShaderProgram.createFromSource(FULLSCREEN_VERT, BLUR_FRAG)
            compositeShader = ShaderProgram.createFromSource(FULLSCREEN_VERT, COMPOSITE_FRAG)
            vao = VertexArray()
            true
        } catch (t: Throwable) {
            System.err.println("[ModernBloomPass] shader creation failed: $t")
            failed = true
            false
        }
    }

    private fun colorTarget(w: Int, h: Int, depth: Boolean): Framebuffer? {
        val fb = Framebuffer(w, h).addColorAttachment(0, FilterType.LINEAR)
        if (depth) fb.addDepthRenderbuffer()
        if (!fb.create()) {
            fb.destroy()
            return null
        }
        return fb
    }

    private fun ensureTargets(w: Int, h: Int): Boolean {
        if (sceneFbo != null && width == w && height == h) return true
        releaseTargets()
        width = w
        height = h
        sceneFbo = colorTarget(w, h, depth = true) ?: run { failed = true; return false }
        // Legacy chain: start at the largest power of two <= each side and halve
        // while either side exceeds 256; the bright pass then samples the last level.
        var cw = MathUtils.furtestBitValue(w)
        var ch = MathUtils.furtestBitValue(h)
        while (cw > 256 || ch > 256) {
            levels.add(colorTarget(cw, ch, depth = false) ?: run { failed = true; return false })
            if (cw > 256) cw = cw shr 1
            if (ch > 256) ch = ch shr 1
        }
        bloomA = colorTarget(256, 256, depth = false) ?: run { failed = true; return false }
        bloomB = colorTarget(256, 256, depth = false) ?: run { failed = true; return false }
        System.err.println("[ModernBloomPass] targets ${w}x${h}, downsample levels=${levels.size}")
        return true
    }

    private fun releaseTargets() {
        sceneFbo?.destroy()
        sceneFbo = null
        for (l in levels) l.destroy()
        levels.clear()
        bloomA?.destroy()
        bloomA = null
        bloomB?.destroy()
        bloomB = null
        width = 0
        height = 0
    }

    /**
     * Redirects rendering into the capture target. Returns false (and binds nothing)
     * when the pass is unavailable, so the caller can keep drawing to the window.
     */
    fun begin(w: Int, h: Int): Boolean {
        if (failed || w <= 0 || h <= 0) return false
        if (!ensureShaders() || !ensureTargets(w, h)) return false
        rebind()
        active = true
        return true
    }

    /** Re-binds the capture target after something else took the framebuffer binding. */
    fun rebind() {
        val fb = sceneFbo ?: return
        glBindFramebuffer(GL_FRAMEBUFFER, fb.getId())
        glViewport(0, 0, width, height)
    }

    private fun drawFullscreen() {
        glDrawArrays(GL_TRIANGLES, 0, 3)
    }

    private fun blit(src: Framebuffer, dst: Framebuffer) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, src.getId())
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, dst.getId())
        glBlitFramebuffer(0, 0, src.width, src.height, 0, 0, dst.width, dst.height, GL_COLOR_BUFFER_BIT, GL_LINEAR)
    }

    /**
     * Runs the post-process and writes the result over [targetFbo] (0 = window) with
     * viewport [targetW]x[targetH]. Leaves the target bound, texture unit 0 active,
     * no program and no VAO bound; depth test, blending, scissor and culling disabled.
     */
    fun composite(targetFbo: Int, targetW: Int, targetH: Int, whitePoint: Float, bloom: Float, brightpass: Float) {
        val scene = sceneFbo
        val a = bloomA
        val b = bloomB
        val bright = brightShader
        val blur = blurShader
        val comp = compositeShader
        val quad = vao
        if (!active || scene == null || a == null || b == null || bright == null || blur == null || comp == null || quad == null) {
            abort(targetFbo, targetW, targetH)
            return
        }
        active = false
        glDisable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glDisable(GL_SCISSOR_TEST)
        glDisable(GL_CULL_FACE)

        // Downsample chain (pure blits: bilinear minification == legacy textured quads).
        var src: Framebuffer = scene
        for (level in levels) {
            blit(src, level)
            src = level
        }

        quad.bind()
        glActiveTexture(GL_TEXTURE0)

        // Bright pass -> A.
        a.bind()
        bright.bind()
        bright.setUniform("uSceneTex", 0)
        bright.setUniform("uWhitePoint", whitePoint)
        glBindTexture(GL_TEXTURE_2D, src.getColorTexture(0))
        drawFullscreen()

        // Blur horizontal A -> B, vertical B -> A.
        blur.bind()
        blur.setUniform("uBaseTex", 0)
        b.bind()
        blur.setUniform("uStep", BLUR_STEP, 0f)
        glBindTexture(GL_TEXTURE_2D, a.getColorTexture(0))
        drawFullscreen()
        a.bind()
        blur.setUniform("uStep", 0f, BLUR_STEP)
        glBindTexture(GL_TEXTURE_2D, b.getColorTexture(0))
        drawFullscreen()

        // Composite over the target.
        glBindFramebuffer(GL_FRAMEBUFFER, targetFbo)
        glViewport(0, 0, targetW, targetH)
        comp.bind()
        comp.setUniform("uSceneTex", 0)
        comp.setUniform("uBloomTex", 1)
        comp.setUniform("uParams", brightpass, bloom)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, a.getColorTexture(0))
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, scene.getColorTexture(0))
        drawFullscreen()

        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
        comp.unbind()
        quad.unbind()
        composites++
    }

    /** Drops the post-process and copies the captured colour straight to the target. */
    fun abort(targetFbo: Int, targetW: Int, targetH: Int) {
        val scene = sceneFbo
        val wasActive = active
        active = false
        if (wasActive && scene != null) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, scene.getId())
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, targetFbo)
            glBlitFramebuffer(0, 0, scene.width, scene.height, 0, 0, targetW, targetH, GL_COLOR_BUFFER_BIT, GL_LINEAR)
        }
        glBindFramebuffer(GL_FRAMEBUFFER, targetFbo)
        glViewport(0, 0, targetW, targetH)
    }

    fun destroy() {
        active = false
        releaseTargets()
        brightShader?.destroy()
        brightShader = null
        blurShader?.destroy()
        blurShader = null
        compositeShader?.destroy()
        compositeShader = null
        vao?.destroy()
        vao = null
    }

    companion object {
        private const val BLUR_STEP = 1f / 256f

        private const val FULLSCREEN_VERT = """#version 330 core
out vec2 vUv;
void main() {
    vec2 p = vec2(gl_VertexID == 1 ? 3.0 : -1.0, gl_VertexID == 2 ? 3.0 : -1.0);
    vUv = p * 0.5 + 0.5;
    gl_Position = vec4(p, 0.0, 1.0);
}
"""

        private const val BRIGHT_FRAG = """#version 330 core
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uSceneTex;
uniform float uWhitePoint;
const vec3 lumCoef = vec3(0.2126, 0.7152, 0.0722);
void main() {
    vec4 col = texture(uSceneTex, vUv);
    fragColor = col * step(uWhitePoint, dot(lumCoef, col.rgb));
}
"""

        private const val BLUR_FRAG = """#version 330 core
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uBaseTex;
uniform vec2 uStep;
void main() {
    vec4 c = texture(uBaseTex, vUv) * 0.091396265;
    c += texture(uBaseTex, vUv - 1.0 * uStep) * 0.088584304;
    c += texture(uBaseTex, vUv + 1.0 * uStep) * 0.088584304;
    c += texture(uBaseTex, vUv - 2.0 * uStep) * 0.08065692;
    c += texture(uBaseTex, vUv + 2.0 * uStep) * 0.08065692;
    c += texture(uBaseTex, vUv - 3.0 * uStep) * 0.068989515;
    c += texture(uBaseTex, vUv + 3.0 * uStep) * 0.068989515;
    c += texture(uBaseTex, vUv - 4.0 * uStep) * 0.055434637;
    c += texture(uBaseTex, vUv + 4.0 * uStep) * 0.055434637;
    c += texture(uBaseTex, vUv - 5.0 * uStep) * 0.04184426;
    c += texture(uBaseTex, vUv + 5.0 * uStep) * 0.04184426;
    c += texture(uBaseTex, vUv - 6.0 * uStep) * 0.029672023;
    c += texture(uBaseTex, vUv + 6.0 * uStep) * 0.029672023;
    c += texture(uBaseTex, vUv - 7.0 * uStep) * 0.019765828;
    c += texture(uBaseTex, vUv + 7.0 * uStep) * 0.019765828;
    c += texture(uBaseTex, vUv - 8.0 * uStep) * 0.012369139;
    c += texture(uBaseTex, vUv + 8.0 * uStep) * 0.012369139;
    fragColor = c;
}
"""

        private const val COMPOSITE_FRAG = """#version 330 core
in vec2 vUv;
out vec4 fragColor;
uniform sampler2D uSceneTex;
uniform sampler2D uBloomTex;
uniform vec2 uParams; // x = brightpass, y = bloom
const vec3 lumCoef = vec3(0.2126, 0.7152, 0.0722);
void main() {
    vec4 bloomCol = texture(uBloomTex, vUv);
    vec4 sceneCol = texture(uSceneTex, vUv);
    float preLum = 0.99 * dot(lumCoef, sceneCol.rgb) + 0.01;
    float postLum = preLum * (1.0 + (preLum / uParams.y)) / (preLum + 1.0);
    vec4 outCol = sceneCol * (postLum / preLum) + bloomCol * uParams.x;
    // The capture carries alpha 0 wherever only the fog/sky clear touched it (the
    // clear colour has no alpha byte); the window must never inherit that.
    fragColor = vec4(outCol.rgb, 1.0);
}
"""
    }
}
