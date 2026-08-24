package com.jagex.game.runetek5.graphics.modern.batch

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_INT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FLOAT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_BYTE
import com.jagex.game.runetek5.math.Matrix43
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * Optimized frame-level geometry batcher that performs vertex transformation AND lighting on the GPU.
 *
 * Key optimizations over the original FrameBatcher:
 * 1. GPU-side vertex transformation - vertices stay in local space, MVP is in shader
 * 2. GPU-side lighting - normals sent to shader, lighting computed in vertex shader
 * 3. Zero per-face allocations - all buffers are pre-allocated and reused
 * 4. Primitive arrays instead of MutableList - avoids boxing and resize overhead
 * 5. Model matrix batching - up to MAX_MODELS matrices uploaded as uniform array
 *
 * Vertex format (36 bytes per vertex):
 * - position: vec3 float (12 bytes) - LOCAL space, not NDC
 * - normal: vec3 float (12 bytes) - for GPU lighting
 * - texcoord: vec2 float (8 bytes)
 * - baseColor: vec4 ubyte (4 bytes) - base RGBA (lighting applied in shader)
 * - modelIndex: float (4 bytes) - index into model matrix array
 *
 * Total: 40 bytes per vertex (increased from 28, but lighting moved to GPU)
 */
class OptimizedFrameBatcher {

    companion object {
        /** Fold the texture alpha channel into model fragment alpha.
         *  Legacy model faces take alpha from the face colour, so this is OFF. */
        /** Must match MAX_POINT_LIGHTS in the vertex shader. */
        /** Raise with -Drender.maxlights. */
        @JvmStatic val MAX_POINT_LIGHTS = System.getProperty("render.maxlights")?.toIntOrNull()?.coerceIn(1, 256) ?: 128

        @JvmStatic val CULL_FACE = System.getProperty("render.cullface")?.equals("false", true) != true
        /** GL_BACK. */
        @JvmStatic val CULL_MODE = 0x0405
        /** GL_CCW. Front faces come out counter-clockwise for this batcher's
         *  geometry; verified against legacy (GL_CW culled the wrong side and left
         *  the signpost's underside quads visible). -Drender.cullwind=cw to flip. */
        @JvmStatic val CULL_WIND = if (System.getProperty("render.cullwind") == "cw") 0x0900 else 0x0901
        /** Fold the texture alpha channel into model fragment alpha.
         *
         *  ON, because that is what legacy does: loadTextureCombineMode maps every
         *  material mode onto an alpha combiner that reads the texture (MODULATE ->
         *  texA*vertA, REPLACE -> texA), and glAlphaFunc(GL_GREATER, 0) discards only
         *  fully transparent fragments -- the alpha combiner source is never
         *  redirected away from the texture. Forcing alpha to come from the vertex
         *  colour instead made the Canifis lampposts solid, but it also made every
         *  texture that legitimately uses its alpha opaque (cape icon backgrounds,
         *  roof tiles). -Drender.texalpha=false restores that behaviour. */
        @JvmStatic val TEX_ALPHA = System.getProperty("render.texalpha")?.equals("false", true) != true
        /**
         * Alpha cutoff for the model pass.
         *
         * Legacy uses glAlphaFunc(GL_GREATER, 0) — it only discards fully
         * transparent texels — and gets away with it because Scene draws its
         * layered/alpha bucket LAST and sorted (Scene.drawTiles). This batcher
         * sorts by MATERIAL instead, so a low-alpha texel drawn early both blends
         * AND writes depth, occluding whatever is behind it: grass fringes were
         * punching holes through lamp posts and signposts, which reads as the model
         * being transparent.
         *
         * Discarding the fringe restores solid geometry. It deviates from legacy's
         * exact threshold, deliberately: reproducing GL_GREATER(0) faithfully would
         * require reproducing legacy's per-entity draw order too, which the
         * material-batched design cannot do.
         */
        const val ALPHA_CUTOFF = 0f

        const val BYTES_PER_VERTEX = 40  // position(12) + normal(12) + texcoord(8) + color(4) + modelIndex(4)
        private const val INITIAL_VERTEX_CAPACITY = 500000  // ~20MB vertex data
        private const val INITIAL_INDEX_CAPACITY = 1000000  // ~4MB index data
        // Using UBO for model matrices - most modern GPUs support 64KB+ UBOs
        // Each mat4 = 64 bytes, so 32KB / 64 = 512 matrices
        // Balance between batching and CPU overhead
        private const val MAX_MODELS = 512
        private const val MAX_MATERIALS = 256
    }

    // GPU resources
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null
    private var matrixUbo: Int = 0  // Uniform buffer for model matrices

    // Uniform locations
    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uCombineModeLocation = -1
    private var uTexAlphaLocation = -1
    /** Per-material: does this texture actually carry an alpha channel? */
    private var texHasAlpha: (Int) -> Boolean = { true }
    private var uAlphaThresholdLocation = -1
    private var uViewProjectionLocation = -1

    // Lighting uniform locations
    private var uSunDirectionLocation = -1
    private var uSunBrightnessLocation = -1
    private var uSunColorLocation = -1
    private var uPointLightCountLocation = -1
    private var uPointLightPosLocation = -1
    private var uPointLightColorLocation = -1
    private var uContrastPosLocation = -1
    private var uContrastNegLocation = -1
    private var uViewMatrixLocation = -1
    private var uFogColorLocation = -1
    private var uFogStartLocation = -1
    private var uFogEndLocation = -1
    private var uFogEnabledLocation = -1

    private var viewMatrix: FloatArray? = null
    private var fogR = 0f; private var fogG = 0f; private var fogB = 0f
    private var fogStart = 0f; private var fogEnd = 0f; private var fogOn = false

    // Per-frame lighting parameters
    private var sunDirX = 0f
    private var sunDirY = 1f
    private var sunDirZ = 0f
    private var sunBrightness = 0.5f
    private var sunColorR = 1f
    private var sunColorG = 1f
    private var sunColorB = 1f
    private var contrastPos = 1f
    private var contrastNeg = 1f

    // Pre-allocated CPU staging buffers (NEVER reallocated during normal operation)
    private var vertexBuffer: ByteBuffer? = null

    /**
     * Base address of [vertexBuffer], refreshed whenever it is (re)allocated.
     * Used for the modelIndex stamping loop in [addCachedModel] — see the comment
     * there for why that write bypasses the ByteBuffer API.
     */
    private var vertexBufferAddress = 0L
    private var indexBuffer: IntBuffer? = null
    private var matrixBuffer: FloatBuffer? = null  // Model matrices for UBO

    // Per-material index tracking using primitive arrays (no boxing)
    // Structure: for each material, store start/count in a flat array
    private var materialStartIndices = IntArray(MAX_MATERIALS)
    private var materialIndexCounts = IntArray(MAX_MATERIALS)
    private var materialTextureIds = IntArray(MAX_MATERIALS)
    private var activeMaterialCount = 0
    private var materialIndexMap = IntArray(65536) { -1 }  // textureId -> material slot

    // Temporary index storage per material (reused each frame)
    private var tempIndicesPerMaterial = Array(MAX_MATERIALS) { IntArray(16384) }
    private var tempIndexCountPerMaterial = IntArray(MAX_MATERIALS)

    // Current state
    private var totalVertexCount = 0
    private var totalIndexCount = 0
    private var currentModelIndex = 0
    private var isInitialized = false
    private var isFrameActive = false

    // View-projection matrix from legacy system
    private var vpMatrix: FloatArray? = null

    // Statistics (current batch)
    private var drawCallsThisBatch = 0
    private var trianglesThisBatch = 0
    private var stateChangesThisBatch = 0
    private var modelsThisBatch = 0
    private var cachedModelsThisBatch = 0

    // Cumulative stats across all flushes in a frame (for getStats)
    private var drawCallsCumulative = 0
    private var trianglesCumulative = 0
    private var stateChangesCumulative = 0
    private var modelsCumulative = 0
    private var cachedModelsCumulative = 0

    // Timing profiling (in nanoseconds)
    private var timeAddModels = 0L
    private var timeEndFrame = 0L
    private var timeUpload = 0L
    private var timeRender = 0L
    private var frameStartTime = 0L

    // Detailed timing breakdown
    private var timeBulkCopy = 0L
    private var timePatchModelIdx = 0L
    private var timeIndexCopy = 0L

    // Reusable arrays for addFaceOptimized (avoid per-call allocation)
    private val tempWorldPos = FloatArray(9)  // 3 vertices * 3 components

    /**
     * Initializes GPU resources. Must be called on GL thread.
     */
    fun initialize() {
        if (isInitialized) return

        // Create optimized shader with GPU-side MVP transformation
        shader = createOptimizedShader()

        // Cache uniform locations
        shader!!.bind()
        uTextureLocation = shader!!.getUniformLocation("uTexture")
        uTextureEnabledLocation = shader!!.getUniformLocation("uTextureEnabled")
        uCombineModeLocation = shader!!.getUniformLocation("uCombineMode")
        uTexAlphaLocation = shader!!.getUniformLocation("uTexAlpha")
        uAlphaThresholdLocation = shader!!.getUniformLocation("uAlphaThreshold")
        uViewProjectionLocation = shader!!.getUniformLocation("uViewProjection")
        // Lighting uniforms
        uSunDirectionLocation = shader!!.getUniformLocation("uSunDirection")
        uSunBrightnessLocation = shader!!.getUniformLocation("uSunBrightness")
        uSunColorLocation = shader!!.getUniformLocation("uSunColor")
        uPointLightCountLocation = shader!!.getUniformLocation("uPointLightCount")
        uPointLightPosLocation = shader!!.getUniformLocation("uPointLightPos")
        uPointLightColorLocation = shader!!.getUniformLocation("uPointLightColor")
        uContrastPosLocation = shader!!.getUniformLocation("uContrastPos")
        uContrastNegLocation = shader!!.getUniformLocation("uContrastNeg")
        uViewMatrixLocation = shader!!.getUniformLocation("uViewMatrix")
        uFogColorLocation = shader!!.getUniformLocation("uFogColor")
        uFogStartLocation = shader!!.getUniformLocation("uFogStart")
        uFogEndLocation = shader!!.getUniformLocation("uFogEnd")
        uFogEnabledLocation = shader!!.getUniformLocation("uFogEnabled")
        shader!!.unbind()

        // Create GPU resources
        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STREAM, IndexBuffer.IndexType.UNSIGNED_INT)

        // Setup VAO with optimized vertex format
        vao!!.bind()
        vbo!!.bind()
        setupOptimizedVertexFormat(vao!!)
        ibo!!.bind()
        vao!!.unbind()

        // Create UBO for model matrices
        matrixUbo = glGenBuffers()
        glBindBuffer(GL_UNIFORM_BUFFER, matrixUbo)
        glBufferData(GL_UNIFORM_BUFFER, (MAX_MODELS * 20 * 4).toLong(), GL_STREAM_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        // Allocate pooled staging buffers
        vertexBuffer = MemoryUtil.memAlloc(INITIAL_VERTEX_CAPACITY * BYTES_PER_VERTEX)
        vertexBufferAddress = MemoryUtil.memAddress(vertexBuffer!!)
        indexBuffer = MemoryUtil.memAllocInt(INITIAL_INDEX_CAPACITY)
        matrixBuffer = MemoryUtil.memAllocFloat(MAX_MODELS * 20)

        isInitialized = true
    }

    private fun createOptimizedShader(): ShaderProgram {
        val vertexSource = """
            #version 330 core
            #define MAX_POINT_LIGHTS $MAX_POINT_LIGHTS

            layout(location = 0) in vec3 aPosition;   // Local space position
            layout(location = 1) in vec3 aNormal;     // Vertex normal for lighting
            layout(location = 2) in vec2 aTexCoord;
            layout(location = 3) in vec4 aColor;      // Base RGBA (unlit)
            layout(location = 4) in float aModelIndex; // Model matrix index

            uniform mat4 uViewProjection;

            // Lighting uniforms - matches legacy lighting calculation exactly
            uniform vec3 uSunDirection;   // Normalized sun direction
            uniform float uSunBrightness; // Ambient/base brightness
            uniform vec3 uSunColor;       // RGB color multiplier
            uniform float uContrastPos;   // Contrast for positive dot product
            uniform float uContrastNeg;

            // Legacy point lights (OpenGlToolkit.renderLights): GL_LIGHT2+ with
            // GL_POSITION, GL_DIFFUSE = colour * (transparency/255) and
            // GL_QUADRATIC_ATTENUATION = 1/size2d^2 (constant/linear terms are 0),
            // so att = 1/(kq*d^2). GL_NORMALIZE is never enabled, so the scaled normal
            // feeds the diffuse term as-is, same as the sun term.
            uniform int uPointLightCount;
            uniform vec4 uPointLightPos[MAX_POINT_LIGHTS];   // xyz world, w = kq
            uniform vec3 uPointLightColor[MAX_POINT_LIGHTS]; // already * transparency/255

            // Legacy GL_LINEAR fog (setupMatrixRendering enables it per model draw).
            uniform mat4 uViewMatrix;
            uniform float uFogStart;
            uniform float uFogEnd;
            uniform int uFogEnabled;   // Contrast for negative dot product

            // Use uniform buffer object for model matrices to avoid uniform count limits
            layout(std140) uniform ModelMatrices {
                mat4 uModelMatrices[${MAX_MODELS}];
                // Per-model light indices (4 slots, -1 = unused). Same per-entity set
                // legacy programs via copyLights -> getActiveLights, so a vertex
                // evaluates at most 4 lights instead of every light in the scene.
                vec4 uModelLights[${MAX_MODELS}];
            };

            out vec2 vTexCoord;
            out vec4 vColor;
            out float vFogFactor;

            void main() {
                int modelIdx = int(aModelIndex);
                mat4 modelMat = uModelMatrices[modelIdx];
                vec4 worldPos = modelMat * vec4(aPosition, 1.0);
                gl_Position = uViewProjection * worldPos;

                vTexCoord = aTexCoord;

                // GPU-side lighting calculation matching legacy formula exactly:
                // lightIntensity = sunBrightness + dotProduct * (dotProduct < 0 ? contrastNeg : contrastPos)
                // finalColor = baseColor * sunColor * lightIntensity

                // Transform normal to world space (use upper-left 3x3 of model matrix)
                // Note: For proper normal transformation we should use inverse transpose,
                // but for uniform scaling this is equivalent and much cheaper
                mat3 normalMat = mat3(modelMat);
                vec3 worldNormal = normalMat * aNormal; // length carries 768/contrast — do NOT normalise

                // Compute dot product (legacy uses pre-normalized normal data)
                float dotProduct = dot(uSunDirection, worldNormal);

                // Apply contrast based on whether light hits front or back
                float lightIntensity = uSunBrightness + dotProduct * (dotProduct < 0.0 ? uContrastNeg : uContrastPos);

                // Apply lighting to base color
                vec3 baseRGB = aColor.rgb;
                vec3 litRGB = baseRGB * uSunColor * lightIntensity;

                // Point lights add on top of the sun term, exactly as fixed-function
                // accumulates GL_LIGHT2+ before the final clamp.
                vec4 lightIdx = uModelLights[modelIdx];
                for (int k = 0; k < 4; ++k) {
                    int i = int(lightIdx[k]);
                    if (i < 0) continue;
                    vec3 toLight = uPointLightPos[i].xyz - worldPos.xyz;
                    float d2 = dot(toLight, toLight);
                    float att = 1.0 / max(uPointLightPos[i].w * d2, 1e-6);
                    float ndotl = max(dot(worldNormal, normalize(toLight)), 0.0);
                    litRGB += baseRGB * uPointLightColor[i] * (att * ndotl);
                }

                // Clamp to valid range (matching legacy's 0-255 clamping)
                litRGB = clamp(litRGB, 0.0, 1.0);

                vColor = vec4(litRGB, aColor.a);

                if (uFogEnabled != 0) {
                    float eyeZ = -(uViewMatrix * worldPos).z;
                    vFogFactor = clamp((uFogEnd - eyeZ) / max(uFogEnd - uFogStart, 1e-4), 0.0, 1.0);
                } else {
                    vFogFactor = 1.0;
                }
            }
        """.trimIndent()

        val fragmentSource = """
            #version 330 core

            in vec2 vTexCoord;
            in vec4 vColor;
            in float vFogFactor;
            uniform vec3 uFogColor;

            uniform sampler2D uTexture;
            uniform bool uTextureEnabled;
            uniform float uAlphaThreshold;
            // Legacy fixed-function texture combine mode (MaterialType.combineMode):
            // 0 MODULATE, 1 REPLACE, 2 INTERPOLATE(~MODULATE), 3 ADD, 4 SUBTRACT.
            // Hardwired MODULATE (texColor*vColor) multiplied the per-face color into
            // every textured face; faces whose material is REPLACE carry arbitrary
            // (often green) face colors that legacy discards — that was the green
            // sign-board / textured-loc bug. Matches groundTextured.frag exactly.
            uniform int uCombineMode;
            uniform bool uTexAlpha;

            out vec4 fragColor;

            void main() {
                vec4 color;
                if (uTextureEnabled) {
                    vec4 texColor = texture(uTexture, vTexCoord);
                    if (uCombineMode == 1) {
                        color = texColor;
                    } else if (uCombineMode == 3) {
                        color = vec4(clamp(texColor.rgb + vColor.rgb, 0.0, 1.0), texColor.a * vColor.a);
                    } else if (uCombineMode == 4) {
                        color = clamp(texColor - vColor, 0.0, 1.0);
                    } else {
                        color = texColor * vColor;
                    }
                } else {
                    color = vColor;
                }

                // RS stores INVERTED alpha (0 = opaque, 255 = transparent); getPixelsArgb
                // hands that straight to GL, so flip it before use (see InstancedModelBatcher).
                if (uTextureEnabled && !uTexAlpha) color.a = vColor.a;

                if (color.a <= uAlphaThreshold) {
                    discard;
                }

                // Debug: uAlphaThreshold == -99 outputs ALPHA as greyscale, so the
                // actual per-fragment alpha of a surface can be read off a capture
                // instead of inferred.
                if (uAlphaThreshold < -90.0) {
                    // -99: VERTEX alpha only (pre-texture), to tell whether a
                    // translucent surface is translucent in its face data or only
                    // after the texture's alpha channel is modulated in.
                    fragColor = vec4(vColor.a, vColor.a, vColor.a, 1.0);
                } else if (uAlphaThreshold < -50.0) {
                    fragColor = vec4(color.a, color.a, color.a, 1.0);
                } else {
                    // GL fog: C = f*Cfrag + (1-f)*Cfog. Alpha is not fogged.
                    fragColor = vec4(mix(uFogColor, color.rgb, vFogFactor), color.a);
                }
            }
        """.trimIndent()

        return ShaderProgram.createFromSource(vertexSource, fragmentSource)
    }

    private fun setupOptimizedVertexFormat(vao: VertexArray) {
        val stride = BYTES_PER_VERTEX  // 40 bytes
        // Position: vec3 float at offset 0
        GLWrapper.vertexAttribPointer(0, 3, FLOAT, false, stride, 0)
        GLWrapper.enableVertexAttribArray(0)
        // Normal: vec3 float at offset 12 (for GPU lighting)
        GLWrapper.vertexAttribPointer(1, 3, FLOAT, false, stride, 12)
        GLWrapper.enableVertexAttribArray(1)
        // TexCoord: vec2 float at offset 24
        GLWrapper.vertexAttribPointer(2, 2, FLOAT, false, stride, 24)
        GLWrapper.enableVertexAttribArray(2)
        // Color: vec4 ubyte normalized at offset 32 (base color, unlit)
        GLWrapper.vertexAttribPointer(3, 4, UNSIGNED_BYTE, true, stride, 32)
        GLWrapper.enableVertexAttribArray(3)
        // Model index: float at offset 36 (using float for compatibility)
        GLWrapper.vertexAttribPointer(4, 1, FLOAT, false, stride, 36)
        GLWrapper.enableVertexAttribArray(4)
    }

    /**
     * Resets cumulative stats for a new outer frame.
     * Call this once at the start of the frame, before beginFrame().
     */
    fun resetCumulativeStats() {
        drawCallsCumulative = 0
        trianglesCumulative = 0
        stateChangesCumulative = 0
        modelsCumulative = 0
        cachedModelsCumulative = 0
    }

    /**
     * Begins a new frame. Resets all state without allocations.
     */
    fun beginFrame() {
        check(isInitialized) { "OptimizedFrameBatcher not initialized" }
        check(!isFrameActive) { "Frame already active" }

        frameStartTime = System.nanoTime()

        // Reset buffers (just position, no reallocation)
        vertexBuffer!!.clear()
        indexBuffer!!.clear()
        matrixBuffer!!.clear()

        // Reset material tracking. materialTextureIds holds the REAL material id
        // (-1 = untextured, 0..N = textured, material 0 valid); materialIndexMap is
        // indexed by (id + 1) so -1 maps to array index 0.
        for (i in 0 until activeMaterialCount) {
            val mapKey = materialTextureIds[i] + 1
            if (mapKey >= 0 && mapKey < materialIndexMap.size) {
                materialIndexMap[mapKey] = -1
            }
            tempIndexCountPerMaterial[i] = 0
        }
        activeMaterialCount = 0

        // Reset counters for this batch (not cumulative)
        totalVertexCount = 0
        totalIndexCount = 0
        currentModelIndex = 0
        drawCallsThisBatch = 0
        trianglesThisBatch = 0
        stateChangesThisBatch = 0
        modelsThisBatch = 0
        cachedModelsThisBatch = 0

        // Reset timing for this batch
        timeAddModels = 0L
        timeEndFrame = 0L
        timeUpload = 0L
        timeRender = 0L
        timeBulkCopy = 0L
        timePatchModelIdx = 0L
        timeIndexCopy = 0L

        isFrameActive = true
    }

    /**
     * Sets the view-projection matrix for this frame.
     * This is the combined VP matrix from the legacy system.
     */
    fun setViewProjection(vp: FloatArray) {
        // Copy VP and negate Y row: RS VP has Y-down clip space, OpenGL expects Y-up.
        // Indices 1,5,9,13 correspond to the Y output row in the row-major elements.
        val flipped = vp.copyOf()
        flipped[1] = -flipped[1]
        flipped[5] = -flipped[5]
        flipped[9] = -flipped[9]
        flipped[13] = -flipped[13]
        this.vpMatrix = flipped
    }

    /**
     * Sets the lighting parameters for this frame.
     * These are used by the GPU shader to compute per-vertex lighting.
     *
     * @param sunDirX Sun direction X component (normalized)
     * @param sunDirY Sun direction Y component (normalized)
     * @param sunDirZ Sun direction Z component (normalized)
     * @param brightness Base brightness/ambient level
     * @param colorR Sun color red component (0-1)
     * @param colorG Sun color green component (0-1)
     * @param colorB Sun color blue component (0-1)
     * @param contrastPositive Contrast factor for lit surfaces
     * @param contrastNegative Contrast factor for back-facing surfaces
     */

    /** Mirrors legacy's GL_LINEAR fog state for this frame. */
    fun setFog(view: FloatArray, enabled: Boolean, colorRgb: Int, start: Float, end: Float) {
        viewMatrix = view
        fogOn = enabled
        fogR = ((colorRgb shr 16) and 0xff) / 255f
        fogG = ((colorRgb shr 8) and 0xff) / 255f
        fogB = (colorRgb and 0xff) / 255f
        fogStart = start
        fogEnd = end
    }

    // ===== Point lights =====
    // Legacy sets GL_LIGHT2+ per model in copyLights; this batcher draws models in
    // material batches, so the frame's lights are uploaded once as a small array and
    // every fragment accumulates them. Quadratic falloff means distant lights
    // contribute ~0, which is what legacy's per-model light selection approximates.
    private var pointLightCount = 0
    private val pointLightPos = FloatArray(MAX_POINT_LIGHTS * 4)
    private val pointLightColor = FloatArray(MAX_POINT_LIGHTS * 3)

    /** [count] lights; [pos] is xyzw per light (w = 1/size2d^2), [color] is rgb per light. */
    fun setTextureAlphaPredicate(p: (Int) -> Boolean) { texHasAlpha = p }

    fun setPointLights(count: Int, pos: FloatArray, color: FloatArray) {
        val n = if (count > MAX_POINT_LIGHTS) MAX_POINT_LIGHTS else count
        pointLightCount = n
        if (n > 0) {
            System.arraycopy(pos, 0, pointLightPos, 0, n * 4)
            System.arraycopy(color, 0, pointLightColor, 0, n * 3)
        }
    }

    fun setLighting(
        sunDirX: Float, sunDirY: Float, sunDirZ: Float,
        brightness: Float,
        colorR: Float, colorG: Float, colorB: Float,
        contrastPositive: Float, contrastNegative: Float
    ) {
        this.sunDirX = sunDirX
        this.sunDirY = sunDirY
        this.sunDirZ = sunDirZ
        this.sunBrightness = brightness
        this.sunColorR = colorR
        this.sunColorG = colorG
        this.sunColorB = colorB
        this.contrastPos = contrastPositive
        this.contrastNeg = contrastNegative
    }

    /**
     * Begins a new model. Returns the model index for use with addFaceOptimized.
     *
     * @param modelMatrix The model's local-to-world transform
     * @return Model index, or -1 if max models reached
     */
    fun beginModel(modelMatrix: Matrix43, lightIdx: FloatArray? = null): Int {
        if (currentModelIndex >= MAX_MODELS) return -1

        val mb = matrixBuffer!!
        val idx = currentModelIndex

        // Convert Matrix43 (row-vector convention) to column-major mat4 for GLSL
        // Matrix43: v' = v * M  where v is a row vector
        // GLSL mat4: v' = M * v where v is a column vector
        // So we need to transpose: GLSL column i = Matrix43 row i

        // Per-model light indices live after the whole matrix array (std140).
        val lightBase = MAX_MODELS * 16 + idx * 4
        if (lightIdx != null) {
            mb.put(lightBase, lightIdx[0]); mb.put(lightBase + 1, lightIdx[1])
            mb.put(lightBase + 2, lightIdx[2]); mb.put(lightBase + 3, lightIdx[3])
        } else {
            mb.put(lightBase, -1f); mb.put(lightBase + 1, -1f)
            mb.put(lightBase + 2, -1f); mb.put(lightBase + 3, -1f)
        }

        // Column 0 (X transform)
        mb.put(idx * 16 + 0, modelMatrix.xA)
        mb.put(idx * 16 + 1, modelMatrix.yA)
        mb.put(idx * 16 + 2, modelMatrix.zA)
        mb.put(idx * 16 + 3, 0f)

        // Column 1 (Y transform)
        mb.put(idx * 16 + 4, modelMatrix.xB)
        mb.put(idx * 16 + 5, modelMatrix.yB)
        mb.put(idx * 16 + 6, modelMatrix.zB)
        mb.put(idx * 16 + 7, 0f)

        // Column 2 (Z transform)
        mb.put(idx * 16 + 8, modelMatrix.xC)
        mb.put(idx * 16 + 9, modelMatrix.yC)
        mb.put(idx * 16 + 10, modelMatrix.zC)
        mb.put(idx * 16 + 11, 0f)

        // Column 3 (translation)
        mb.put(idx * 16 + 12, modelMatrix.xD)
        mb.put(idx * 16 + 13, modelMatrix.yD)
        mb.put(idx * 16 + 14, modelMatrix.zD)
        mb.put(idx * 16 + 15, 1f)

        currentModelIndex++
        modelsThisBatch++
        return idx
    }

    /**
     * Adds an entire cached model to the batch in one call.
     * ULTRA-FAST PATH using pre-sorted material batches from cache.
     *
     * Optimizations:
     * 1. Bulk memory copy for vertex data (ByteBuffer.put)
     * 2. NO modelIndex patching - use gl_VertexID / 3 in shader instead
     * 3. Pre-sorted material batches eliminate per-face sorting
     * 4. System.arraycopy for bulk index copy
     *
     * @param cachedModel Pre-processed model data from ModelCache (40-byte format with material batches)
     * @param modelIndex Model matrix index from beginModel()
     * @return Number of faces added
     */
    fun addCachedModel(
        cachedModel: ModelCache.CachedModel,
        modelIndex: Int
    ): Int {
        if (!isFrameActive) return 0
        if (modelIndex < 0 || modelIndex >= MAX_MODELS) return 0

        val vb = vertexBuffer!!
        val faceCount = cachedModel.faceCount
        val vertexCount = cachedModel.vertexCount

        // Check if we have space (40 bytes per vertex)
        val neededBytes = vertexCount * BYTES_PER_VERTEX
        if (vb.remaining() < neededBytes) return 0

        val baseVertex = totalVertexCount

        // FAST PATH 1: Bulk copy vertex data. Model-owned geometry (animated poses)
        // keeps its authoritative copy as raw words rather than a ByteBuffer, so it
        // transfers straight from there.
        val t1 = System.nanoTime()
        val dstStart = vb.position()
        val words = cachedModel.vertexWords
        if (words != null) {
            val dup = vb.duplicate()
            dup.position(dstStart)
            dup.asIntBuffer().put(words, 0, vertexCount * (BYTES_PER_VERTEX / 4))
            vb.position(dstStart + vertexCount * BYTES_PER_VERTEX)
        } else {
            val srcData = cachedModel.vertexData
            srcData.position(0)
            srcData.limit(vertexCount * BYTES_PER_VERTEX)
            vb.put(srcData)
            srcData.clear()
        }
        timeBulkCopy += System.nanoTime() - t1

        // FAST PATH 2: stamp this model's matrix slot into every vertex.
        //
        // ModelIndex sits at offset 36 of each 40-byte vertex. Written through
        // ByteBuffer.putInt this was one bounds-checked absolute put per vertex, and
        // at a few hundred thousand vertices a frame the checks alone (Unsafe
        // .checkOffset) were ~9% of the render thread. The buffer is an LWJGL native
        // allocation whose base address is fixed for its lifetime, and the range was
        // already validated by the capacity check above, so write it directly.
        val t2 = System.nanoTime()
        val modelIdxBits = java.lang.Float.floatToRawIntBits(modelIndex.toFloat())
        var patchAddress = vertexBufferAddress + dstStart + 36
        var v = 0
        while (v < vertexCount) {
            MemoryUtil.memPutInt(patchAddress, modelIdxBits)
            patchAddress += BYTES_PER_VERTEX
            v++
        }
        timePatchModelIdx += System.nanoTime() - t2

        // FAST PATH 3: Use pre-sorted material batches with System.arraycopy
        val t3 = System.nanoTime()
        for (batch in cachedModel.materialBatches) {
            // materialId: -1 = untextured, 0..N = textured (material 0 valid). Index
            // materialIndexMap by (id + 1) so -1 → 0; store the REAL id.
            val materialId = batch.materialId
            val mapKey = materialId + 1
            var materialSlot = if (mapKey < materialIndexMap.size) materialIndexMap[mapKey] else -1

            if (materialSlot < 0) {
                if (activeMaterialCount >= MAX_MATERIALS) continue
                materialSlot = activeMaterialCount++
                if (mapKey < materialIndexMap.size) {
                    materialIndexMap[mapKey] = materialSlot
                }
                materialTextureIds[materialSlot] = materialId
                tempIndexCountPerMaterial[materialSlot] = 0
            }

            var tempIndices = tempIndicesPerMaterial[materialSlot]
            var tempCount = tempIndexCountPerMaterial[materialSlot]
            val batchIndices = batch.indices
            val batchCount = batch.indexCount

            // Ensure capacity
            if (tempCount + batchCount > tempIndices.size) {
                val newSize = (tempIndices.size * 2).coerceAtLeast(tempCount + batchCount)
                tempIndices = tempIndices.copyOf(newSize)
                tempIndicesPerMaterial[materialSlot] = tempIndices
            }

            // OPTIMIZATION: Use System.arraycopy for bulk copy, then adjust base vertex
            // Since we need to add baseVertex to each, we still need a loop
            // BUT we can use arraycopy and then do a single pass adjustment
            if (baseVertex == 0) {
                // No adjustment needed - direct copy
                System.arraycopy(batchIndices, 0, tempIndices, tempCount, batchCount)
            } else {
                // Need to add baseVertex - use loop but optimized
                val batchCount4 = batchCount and 3.inv()
                var i = 0
                while (i < batchCount4) {
                    tempIndices[tempCount + i] = baseVertex + batchIndices[i]
                    tempIndices[tempCount + i + 1] = baseVertex + batchIndices[i + 1]
                    tempIndices[tempCount + i + 2] = baseVertex + batchIndices[i + 2]
                    tempIndices[tempCount + i + 3] = baseVertex + batchIndices[i + 3]
                    i += 4
                }
                while (i < batchCount) {
                    tempIndices[tempCount + i] = baseVertex + batchIndices[i]
                    i++
                }
            }
            tempIndexCountPerMaterial[materialSlot] = tempCount + batchCount
        }
        timeIndexCopy += System.nanoTime() - t3

        totalVertexCount += vertexCount
        totalIndexCount += vertexCount
        cachedModelsThisBatch++
        return faceCount
    }

    /**
     * Adds a face to the batch with minimal overhead.
     * All parameters are primitives to avoid boxing.
     * Position is in LOCAL model space - GPU will transform.
     * Normal is used for GPU-side lighting.
     */
    fun addFaceOptimized(
        textureId: Int,
        modelIndex: Int,
        // Vertex 0 - LOCAL space position and normal
        x0: Float, y0: Float, z0: Float,
        nx0: Float, ny0: Float, nz0: Float,
        u0: Float, v0: Float,
        r0: Byte, g0: Byte, b0: Byte, a0: Byte,
        // Vertex 1
        x1: Float, y1: Float, z1: Float,
        nx1: Float, ny1: Float, nz1: Float,
        u1: Float, v1: Float,
        r1: Byte, g1: Byte, b1: Byte, a1: Byte,
        // Vertex 2
        x2: Float, y2: Float, z2: Float,
        nx2: Float, ny2: Float, nz2: Float,
        u2: Float, v2: Float,
        r2: Byte, g2: Byte, b2: Byte, a2: Byte
    ) {
        if (!isFrameActive) return
        if (modelIndex < 0 || modelIndex >= MAX_MODELS) return

        val vb = vertexBuffer!!

        // Check buffer capacity (3 vertices * 40 bytes = 120 bytes needed)
        if (vb.remaining() < 3 * BYTES_PER_VERTEX) return

        val modelIdxFloat = modelIndex.toFloat()

        // Get or create material slot. textureId is the face's material id: -1 =
        // untextured, 0..N = textured (material 0 is a VALID textured material — old-
        // format meshes start texture ids at 0; the previous `<= 0` folded it into the
        // untextured batch → flat green multimodel-loc quads). Store the REAL id;
        // index materialIndexMap by (id + 1) so -1 maps to array index 0.
        val texId = if (textureId < 0) -1 else textureId
        val mapKey = texId + 1
        var materialSlot = if (mapKey < materialIndexMap.size) materialIndexMap[mapKey] else -1

        if (materialSlot < 0) {
            if (activeMaterialCount >= MAX_MATERIALS) return
            materialSlot = activeMaterialCount++
            if (mapKey < materialIndexMap.size) {
                materialIndexMap[mapKey] = materialSlot
            }
            materialTextureIds[materialSlot] = texId
            tempIndexCountPerMaterial[materialSlot] = 0
        }

        val baseVertex = totalVertexCount

        // Write vertex 0 (40 bytes)
        vb.putFloat(x0).putFloat(y0).putFloat(z0)  // position (12)
        vb.putFloat(nx0).putFloat(ny0).putFloat(nz0) // normal (12)
        vb.putFloat(u0).putFloat(v0)               // texcoord (8)
        vb.put(r0).put(g0).put(b0).put(a0)         // base color (4)
        vb.putFloat(modelIdxFloat)                  // modelIndex (4)

        // Write vertex 1
        vb.putFloat(x1).putFloat(y1).putFloat(z1)
        vb.putFloat(nx1).putFloat(ny1).putFloat(nz1)
        vb.putFloat(u1).putFloat(v1)
        vb.put(r1).put(g1).put(b1).put(a1)
        vb.putFloat(modelIdxFloat)

        // Write vertex 2
        vb.putFloat(x2).putFloat(y2).putFloat(z2)
        vb.putFloat(nx2).putFloat(ny2).putFloat(nz2)
        vb.putFloat(u2).putFloat(v2)
        vb.put(r2).put(g2).put(b2).put(a2)
        vb.putFloat(modelIdxFloat)

        // Store indices for this material
        var tempIndices = tempIndicesPerMaterial[materialSlot]
        var tempCount = tempIndexCountPerMaterial[materialSlot]

        // Grow temp array if needed
        if (tempCount + 3 > tempIndices.size) {
            val newSize = (tempIndices.size * 2).coerceAtLeast(tempCount + 3)
            tempIndices = tempIndices.copyOf(newSize)
            tempIndicesPerMaterial[materialSlot] = tempIndices
        }

        tempIndices[tempCount++] = baseVertex
        tempIndices[tempCount++] = baseVertex + 1
        tempIndices[tempCount++] = baseVertex + 2
        tempIndexCountPerMaterial[materialSlot] = tempCount

        totalVertexCount += 3
        totalIndexCount += 3
    }

    /**
     * Ends the frame and renders all batched geometry.
     */
    fun endFrame(
        textureCache: (Int) -> Int,
        combineModeOf: (Int) -> Int = { 0 },
        needsBlendOf: (Int) -> Boolean = { false },
        alphaDiscard: Boolean = true
    ) {
        check(isFrameActive) { "No active frame" }

        // Record time spent adding models (from beginFrame to now)
        timeAddModels = System.nanoTime() - frameStartTime

        if (totalIndexCount == 0 || vpMatrix == null) {
            isFrameActive = false
            timeEndFrame = System.nanoTime() - frameStartTime
            return
        }

        val sh = shader ?: return
        val vp = vpMatrix ?: return

        val uploadStartTime = System.nanoTime()

        // Flip vertex buffer for reading
        vertexBuffer!!.flip()

        // Build consolidated index buffer (group by material)
        val ib = indexBuffer!!
        ib.clear()

        // Sort materials (untextured first, then by texture ID). Untextured is now -1.
        val sortedMaterials = (0 until activeMaterialCount).sortedBy {
            val texId = materialTextureIds[it]
            if (texId < 0) Int.MIN_VALUE else texId
        }

        for (slot in sortedMaterials) {
            val count = tempIndexCountPerMaterial[slot]
            if (count == 0) continue

            materialStartIndices[slot] = ib.position()
            materialIndexCounts[slot] = count

            // OPTIMIZATION: Bulk put using IntBuffer.put(int[], offset, length)
            val tempIndices = tempIndicesPerMaterial[slot]
            ib.put(tempIndices, 0, count)
        }

        ib.flip()

        // Upload geometry
        vbo!!.upload(vertexBuffer!!)
        ibo!!.upload(ib)

        timeUpload = System.nanoTime() - uploadStartTime

        // Upload model matrices
        matrixBuffer!!.position(0)
        // The per-model light indices live after the FULL matrix array (std140), so
        // the upload has to span the matrix block plus the used light entries.
        matrixBuffer!!.limit(MAX_MODELS * 16 + currentModelIndex * 4)
        glBindBuffer(GL_UNIFORM_BUFFER, matrixUbo)
        glBufferSubData(GL_UNIFORM_BUFFER, 0, matrixBuffer!!)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        val renderStartTime = System.nanoTime()

        // Bind shader and set uniforms
        sh.bind()
        // Legacy back-face culls model geometry (OpenGlToolkit init:
        // glEnable(GL_CULL_FACE) + glCullFace(GL_BACK), never turned off for the
        // scene). Asserted HERE rather than in openSceneBatch because state set
        // there is cleared before the draws land. The projection flips Y, which
        // reverses winding, so front faces are CW.
        if (CULL_FACE) {
            GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
            GLWrapper.cullFace(CULL_MODE)
            GLWrapper.frontFace(CULL_WIND)
        } else {
            GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
        }
        sh.setUniform(uTextureLocation, 0)
        // -1 disables the shader's `color.a <= uAlphaThreshold` discard entirely,
        // so geometry lost to a zero alpha becomes visible (render alphatest).
        // Legacy: glAlphaFunc(GL_GREATER, 0) -> keep a > 0, discard a <= 0.
        sh.setUniform(
            uAlphaThresholdLocation,
            // -Drender.alphaprobe=<t> discards any fragment with alpha <= t.
            // At 0.99 only FULLY opaque fragments survive, which answers
            // "is this surface actually alpha < 255?" without guessing.
            System.getProperty("render.alphaprobe")?.toFloatOrNull()
                ?: (if (alphaDiscard) ALPHA_CUTOFF else -1f)
        )

        // Set VP matrix - convert legacy row-vector format to column-major for GLSL
        // Legacy uses: clip = VP * world (where VP is stored row-major for row-vector math)
        // GLSL expects column-major mat4
        setViewProjectionUniform(sh, vp)

        // Set lighting uniforms for GPU-side lighting calculation
        sh.setUniform(uSunDirectionLocation, sunDirX, sunDirY, sunDirZ)
        sh.setUniform(uSunBrightnessLocation, sunBrightness)
        sh.setUniform(uSunColorLocation, sunColorR, sunColorG, sunColorB)
        // Point lights for this frame (see setPointLights).
        if (uPointLightCountLocation >= 0) {
            GLWrapper.uniform1i(uPointLightCountLocation, pointLightCount)
            if (pointLightCount > 0) {
                if (uPointLightPosLocation >= 0) GLWrapper.uniform4fv(uPointLightPosLocation, pointLightPos)
                if (uPointLightColorLocation >= 0) GLWrapper.uniform3fv(uPointLightColorLocation, pointLightColor)
            }
        }
        sh.setUniform(uContrastPosLocation, contrastPos)
        sh.setUniform(uContrastNegLocation, contrastNeg)
        viewMatrix?.let { glUniformMatrix4fv(uViewMatrixLocation, false, it) }
        sh.setUniform(uFogEnabledLocation, if (fogOn) 1 else 0)
        sh.setUniform(uFogStartLocation, fogStart)
        sh.setUniform(uFogEndLocation, fogEnd)
        sh.setUniform(uFogColorLocation, fogR, fogG, fogB)

        // Bind matrix UBO to binding point 0
        val blockIndex = glGetUniformBlockIndex(sh.getId(), "ModelMatrices")
        if (blockIndex != GL_INVALID_INDEX) {
            glUniformBlockBinding(sh.getId(), blockIndex, 0)
        }
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixUbo)

        // Bind VAO
        vao!!.bind()
        ibo!!.bind()

        var lastTextureHandle = -1
        // -1 = unknown, forces the first material batch to establish the state.
        var lastBlend = -1

        // Render each material batch
        for (slot in sortedMaterials) {
            val indexCount = materialIndexCounts[slot]
            if (indexCount == 0) continue

            // texId is the REAL material id: -1 = untextured, 0..N = textured (0 valid).
            val texId = materialTextureIds[slot]
            val textureHandle = if (texId >= 0) textureCache(texId) else 0

            if (textureHandle != lastTextureHandle) {
                if (textureHandle > 0) {
                    GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                    GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                    sh.setUniform(uTextureEnabledLocation, 1)
                    // Per-material combine mode (legacy parity). REPLACE (1) ignores the
                    // per-face vColor — required or textured faces with junk face colors
                    // render tinted (the green sign-board bug).
                    sh.setUniform(uCombineModeLocation, if (texId >= 0) combineModeOf(texId) else 0)
                    sh.setUniform(uTexAlphaLocation, if (TEX_ALPHA && texHasAlpha(texId)) 1 else 0)
                } else {
                    sh.setUniform(uTextureEnabledLocation, 0)
                }
                lastTextureHandle = textureHandle
                stateChangesThisBatch++
            }

            // Per-material blend state (legacy setBlendMode parity). Opaque
            // materials MUST draw with GL_BLEND off, or their alpha-carrying faces
            // go translucent and depth-reject the geometry behind them.
            // Blend state is uniform across models (legacy setBlendMode(1)), set once
            // per pass in openSceneBatch — see InstancedModelBatcher.

            GLWrapper.drawElements(
                TRIANGLES,
                indexCount,
                UNSIGNED_INT,
                materialStartIndices[slot].toLong() * 4
            )

            drawCallsThisBatch++
            trianglesThisBatch += indexCount / 3
        }

        vao!!.unbind()
        sh.unbind()

        timeRender = System.nanoTime() - renderStartTime
        timeEndFrame = System.nanoTime() - frameStartTime

        // Add batch stats to cumulative for reporting
        drawCallsCumulative += drawCallsThisBatch
        trianglesCumulative += trianglesThisBatch
        stateChangesCumulative += stateChangesThisBatch
        modelsCumulative += modelsThisBatch
        cachedModelsCumulative += cachedModelsThisBatch

        isFrameActive = false
    }

    private fun setViewProjectionUniform(shader: ShaderProgram, vp: FloatArray) {
        // The legacy VP matrix uses row-vector convention with indexing:
        //   clip_x = vp[12] + vp[0]*x + vp[4]*y + vp[8]*z
        //   clip_y = vp[13] + vp[1]*x + vp[5]*y + vp[9]*z
        //   clip_z = vp[14] + vp[2]*x + vp[6]*y + vp[10]*z
        //   clip_w = vp[15] + vp[3]*x + vp[7]*y + vp[11]*z
        //
        // For GLSL column-major mat4 where clip = M * pos:
        //   clip_x = M[0][0]*x + M[1][0]*y + M[2][0]*z + M[3][0]*w
        //
        // So we need: M[0][0]=vp[0], M[1][0]=vp[4], M[2][0]=vp[8], M[3][0]=vp[12]
        // Which is column 0 = [vp[0], vp[1], vp[2], vp[3]] (X components of result)
        // Column 1 = [vp[4], vp[5], vp[6], vp[7]]
        // etc.

        val colMajor = FloatArray(16)
        // Column 0
        colMajor[0] = vp[0]
        colMajor[1] = vp[1]
        colMajor[2] = vp[2]
        colMajor[3] = vp[3]
        // Column 1
        colMajor[4] = vp[4]
        colMajor[5] = vp[5]
        colMajor[6] = vp[6]
        colMajor[7] = vp[7]
        // Column 2
        colMajor[8] = vp[8]
        colMajor[9] = vp[9]
        colMajor[10] = vp[10]
        colMajor[11] = vp[11]
        // Column 3
        colMajor[12] = vp[12]
        colMajor[13] = vp[13]
        colMajor[14] = vp[14]
        colMajor[15] = vp[15]

        glUniformMatrix4fv(uViewProjectionLocation, false, colMajor)
    }

    fun isActive(): Boolean = isFrameActive

    /** True when the open frame has collected geometry that has not been flushed yet. */
    fun hasPendingGeometry(): Boolean = isFrameActive && totalVertexCount > 0

    fun getStats(): FrameStats = FrameStats(
        drawCalls = drawCallsCumulative,
        triangles = trianglesCumulative,
        stateChanges = stateChangesCumulative,
        models = modelsCumulative,
        cachedModels = cachedModelsCumulative,
        uniqueMaterials = activeMaterialCount,
        vertexBytes = totalVertexCount * BYTES_PER_VERTEX,
        indexBytes = totalIndexCount * 4,
        timeAddModelsMs = timeAddModels / 1_000_000f,
        timeUploadMs = timeUpload / 1_000_000f,
        timeRenderMs = timeRender / 1_000_000f,
        timeTotalMs = timeEndFrame / 1_000_000f,
        timeBulkCopyMs = timeBulkCopy / 1_000_000f,
        timePatchModelIdxMs = timePatchModelIdx / 1_000_000f,
        timeIndexCopyMs = timeIndexCopy / 1_000_000f
    )

    fun destroy() {
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()

        if (matrixUbo != 0) {
            glDeleteBuffers(matrixUbo)
            matrixUbo = 0
        }

        vertexBuffer?.let { MemoryUtil.memFree(it) }
        indexBuffer?.let { MemoryUtil.memFree(it) }
        matrixBuffer?.let { MemoryUtil.memFree(it) }

        shader = null
        vao = null
        vbo = null
        ibo = null
        vertexBuffer = null
        indexBuffer = null
        matrixBuffer = null

        isInitialized = false
    }

    data class FrameStats(
        val drawCalls: Int,
        val triangles: Int,
        val stateChanges: Int,
        val models: Int,
        val cachedModels: Int,
        val uniqueMaterials: Int,
        val vertexBytes: Int,
        val indexBytes: Int,
        val timeAddModelsMs: Float = 0f,
        val timeUploadMs: Float = 0f,
        val timeRenderMs: Float = 0f,
        val timeTotalMs: Float = 0f,
        val timeBulkCopyMs: Float = 0f,
        val timePatchModelIdxMs: Float = 0f,
        val timeIndexCopyMs: Float = 0f
    ) {
        val vertexMB: Float get() = vertexBytes / (1024f * 1024f)
        val indexMB: Float get() = indexBytes / (1024f * 1024f)
        val totalMB: Float get() = vertexMB + indexMB
        val cacheHitRate: Float get() = if (models > 0) cachedModels.toFloat() / models else 0f
    }
}
