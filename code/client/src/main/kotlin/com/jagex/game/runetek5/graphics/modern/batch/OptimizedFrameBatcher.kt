package com.jagex.game.runetek5.graphics.modern.batch

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.material.TextureScroll
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

class OptimizedFrameBatcher {

    companion object {
        @JvmStatic val MAX_POINT_LIGHTS = System.getProperty("render.maxlights")?.toIntOrNull()?.coerceIn(1, 256) ?: 128

        @JvmStatic val CULL_FACE = System.getProperty("render.cullface")?.equals("false", true) != true
        @JvmStatic val CULL_MODE = 0x0405
        @JvmStatic val CULL_WIND = if (System.getProperty("render.cullwind") == "cw") 0x0900 else 0x0901
        @JvmStatic val TEX_ALPHA = System.getProperty("render.texalpha")?.equals("false", true) != true
        const val ALPHA_CUTOFF = 0f

        const val BYTES_PER_VERTEX = 40
        private const val INITIAL_VERTEX_CAPACITY = 500000
        private const val INITIAL_INDEX_CAPACITY = 1000000
        private const val MAX_MODELS = 512
        private const val MAX_MATERIALS = 256
    }

    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null
    private var matrixUbo: Int = 0

    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uCombineModeLocation = -1
    private var uTexAlphaLocation = -1
    private var texHasAlpha: (Int) -> Boolean = { true }
    private var uTextureSpeedLocation = -1
    private var uTextureTimeLocation = -1
    private var textureScrollOf: (Int) -> Int = { TextureScroll.NONE }
    private var textureTime = 0f
    private var uAlphaThresholdLocation = -1
    private var uViewProjectionLocation = -1

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

    private var sunDirX = 0f
    private var sunDirY = 1f
    private var sunDirZ = 0f
    private var sunBrightness = 0.5f
    private var sunColorR = 1f
    private var sunColorG = 1f
    private var sunColorB = 1f
    private var contrastPos = 1f
    private var contrastNeg = 1f

    private var vertexBuffer: ByteBuffer? = null

    private var vertexBufferAddress = 0L
    private var indexBuffer: IntBuffer? = null
    private var matrixBuffer: FloatBuffer? = null

    private var materialStartIndices = IntArray(MAX_MATERIALS)
    private var materialIndexCounts = IntArray(MAX_MATERIALS)
    private var materialTextureIds = IntArray(MAX_MATERIALS)
    private var activeMaterialCount = 0
    private var materialIndexMap = IntArray(65536) { -1 }

    private var tempIndicesPerMaterial = Array(MAX_MATERIALS) { IntArray(16384) }
    private var tempIndexCountPerMaterial = IntArray(MAX_MATERIALS)

    private var orderedIndices = IntArray(65536)
    private var orderedIndexCount = 0
    private var orderedSegMaterial = IntArray(1024)
    private var orderedSegStart = IntArray(1024)
    private var orderedSegCount = IntArray(1024)
    private var orderedSegDepthWrite = BooleanArray(1024)
    private var orderedSegSeq = IntArray(1024)
    private var orderedSegKey = IntArray(1024)
    private var orderedSegments = 0
    private var orderedOrder = IntArray(1024)
    private var orderedSortScratch = LongArray(1024)
    private var orderedCursor = 0
    private var orderedBase = 0
    private var orderedPacked = 0

    private var totalVertexCount = 0
    private var totalIndexCount = 0
    private var currentModelIndex = 0
    private var isInitialized = false
    private var isFrameActive = false
    private var uClipPlaneLocation = -1
    private var reflectionMaterials = IntArray(0)
    private var reflectionTextureCache: ((Int) -> Int)? = null
    private var reflectionCombineModeOf: ((Int) -> Int)? = null

    private var vpMatrix: FloatArray? = null

    private var drawCallsThisBatch = 0
    private var trianglesThisBatch = 0
    private var stateChangesThisBatch = 0
    private var modelsThisBatch = 0
    private var cachedModelsThisBatch = 0

    private var drawCallsCumulative = 0
    private var trianglesCumulative = 0
    private var stateChangesCumulative = 0
    private var modelsCumulative = 0
    private var cachedModelsCumulative = 0

    private var timeAddModels = 0L
    private var timeEndFrame = 0L
    private var timeUpload = 0L
    private var timeRender = 0L
    private var frameStartTime = 0L

    private var timeBulkCopy = 0L
    private var timePatchModelIdx = 0L
    private var timeIndexCopy = 0L

    private val tempWorldPos = FloatArray(9)

    fun initialize() {
        if (isInitialized) return

        shader = createOptimizedShader()

        shader!!.bind()
        uTextureLocation = shader!!.getUniformLocation("uTexture")
        uTextureEnabledLocation = shader!!.getUniformLocation("uTextureEnabled")
        uCombineModeLocation = shader!!.getUniformLocation("uCombineMode")
        uTexAlphaLocation = shader!!.getUniformLocation("uTexAlpha")
        uTextureSpeedLocation = shader!!.getUniformLocation("uTextureSpeed")
        uTextureTimeLocation = shader!!.getUniformLocation("uTextureTime")
        uAlphaThresholdLocation = shader!!.getUniformLocation("uAlphaThreshold")
        uViewProjectionLocation = shader!!.getUniformLocation("uViewProjection")
        uClipPlaneLocation = shader!!.getUniformLocation("uClipPlane")
        uSunDirectionLocation = shader!!.getUniformLocation("uSunDirection")
        uSunBrightnessLocation = shader!!.getUniformLocation("uSunBrightness")
        uSunColorLocation = shader!!.getUniformLocation("uSunColor")
        uPointLightCountLocation = shader!!.getUniformLocation("uPointLightCount")
        uPointLightPosLocation = shader!!.getArrayUniformLocation("uPointLightPos")
        uPointLightColorLocation = shader!!.getArrayUniformLocation("uPointLightColor")
        uContrastPosLocation = shader!!.getUniformLocation("uContrastPos")
        uContrastNegLocation = shader!!.getUniformLocation("uContrastNeg")
        uViewMatrixLocation = shader!!.getUniformLocation("uViewMatrix")
        uFogColorLocation = shader!!.getUniformLocation("uFogColor")
        uFogStartLocation = shader!!.getUniformLocation("uFogStart")
        uFogEndLocation = shader!!.getUniformLocation("uFogEnd")
        uFogEnabledLocation = shader!!.getUniformLocation("uFogEnabled")
        shader!!.unbind()

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STREAM, IndexBuffer.IndexType.UNSIGNED_INT)

        vao!!.bind()
        vbo!!.bind()
        setupOptimizedVertexFormat(vao!!)
        ibo!!.bind()
        vao!!.unbind()

        matrixUbo = glGenBuffers()
        glBindBuffer(GL_UNIFORM_BUFFER, matrixUbo)
        glBufferData(GL_UNIFORM_BUFFER, (MAX_MODELS * 20 * 4).toLong(), GL_STREAM_DRAW)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

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
            uniform vec4 uClipPlane;

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
                gl_ClipDistance[0] = dot(worldPos, uClipPlane);

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
                    float att = 1.0 / max(uPointLightPos[i].w * d2, 1e-6); // legacy: kc=0, kl=0, kq=1/size^2
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
            uniform vec2 uTextureSpeed;
            uniform float uTextureTime;

            out vec4 fragColor;

            void main() {
                vec4 color;
                if (uTextureEnabled) {
                    vec2 scroll = uTextureTime * uTextureSpeed / 64.0;
                    vec4 texColor = texture(uTexture, vTexCoord + scroll - trunc(scroll));
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
        val stride = BYTES_PER_VERTEX
        GLWrapper.vertexAttribPointer(0, 3, FLOAT, false, stride, 0)
        GLWrapper.enableVertexAttribArray(0)
        GLWrapper.vertexAttribPointer(1, 3, FLOAT, false, stride, 12)
        GLWrapper.enableVertexAttribArray(1)
        GLWrapper.vertexAttribPointer(2, 2, FLOAT, false, stride, 24)
        GLWrapper.enableVertexAttribArray(2)
        GLWrapper.vertexAttribPointer(3, 4, UNSIGNED_BYTE, true, stride, 32)
        GLWrapper.enableVertexAttribArray(3)
        GLWrapper.vertexAttribPointer(4, 1, FLOAT, false, stride, 36)
        GLWrapper.enableVertexAttribArray(4)
    }

    fun resetCumulativeStats() {
        drawCallsCumulative = 0
        trianglesCumulative = 0
        stateChangesCumulative = 0
        modelsCumulative = 0
        cachedModelsCumulative = 0
    }

    fun drawReflection(mirroredViewProjection: FloatArray, clipPlane: FloatArray) {
        val sh = shader ?: return
        val textureCache = reflectionTextureCache ?: return
        val combineModeOf = reflectionCombineModeOf ?: return
        val vp = vpMatrix ?: return
        sh.bind()
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthFunc(GLWrapper.Constants.LEQUAL)
        if (CULL_FACE) {
            GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
            GLWrapper.cullFace(CULL_MODE)
            GLWrapper.frontFace(if (CULL_WIND == 0x0900) 0x0901 else 0x0900)
        }
        glUniformMatrix4fv(uViewProjectionLocation, false, mirroredViewProjection)
        glUniform4f(uClipPlaneLocation, clipPlane[0], clipPlane[1], clipPlane[2], clipPlane[3])
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixUbo)
        vao!!.bind()
        ibo!!.bind()
        var lastTextureHandle = -1
        for (slot in reflectionMaterials) {
            val indexCount = materialIndexCounts[slot]
            if (indexCount == 0) continue
            val texId = materialTextureIds[slot]
            val textureHandle = if (texId >= 0) textureCache(texId) else 0
            if (textureHandle != lastTextureHandle) {
                if (textureHandle > 0) {
                    GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                    GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                    sh.setUniform(uTextureEnabledLocation, 1)
                    sh.setUniform(uCombineModeLocation, combineModeOf(texId))
                    sh.setUniform(uTexAlphaLocation, if (TEX_ALPHA && texHasAlpha(texId)) 1 else 0)
                    setTextureSpeedUniform(sh, texId)
                } else {
                    sh.setUniform(uTextureEnabledLocation, 0)
                }
                lastTextureHandle = textureHandle
            }
            GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_INT, materialStartIndices[slot].toLong() * 4)
        }
        vao!!.unbind()
        setViewProjectionUniform(sh, vp)
        glUniform4f(uClipPlaneLocation, 0f, 0f, 0f, 0f)
        GLWrapper.frontFace(CULL_WIND)
        sh.unbind()
    }

    fun beginFrame() {
        check(isInitialized) { "OptimizedFrameBatcher not initialized" }
        check(!isFrameActive) { "Frame already active" }

        frameStartTime = System.nanoTime()
        reflectionTextureCache = null

        vertexBuffer!!.clear()
        indexBuffer!!.clear()
        matrixBuffer!!.clear()

        for (i in 0 until activeMaterialCount) {
            val mapKey = materialTextureIds[i] + 1
            if (mapKey >= 0 && mapKey < materialIndexMap.size) {
                materialIndexMap[mapKey] = -1
            }
            tempIndexCountPerMaterial[i] = 0
        }
        activeMaterialCount = 0
        orderedIndexCount = 0
        orderedSegments = 0
        orderedPacked = 0
        orderedCursor = 0

        totalVertexCount = 0
        totalIndexCount = 0
        currentModelIndex = 0
        drawCallsThisBatch = 0
        trianglesThisBatch = 0
        stateChangesThisBatch = 0
        modelsThisBatch = 0
        cachedModelsThisBatch = 0

        timeAddModels = 0L
        timeEndFrame = 0L
        timeUpload = 0L
        timeRender = 0L
        timeBulkCopy = 0L
        timePatchModelIdx = 0L
        timeIndexCopy = 0L

        isFrameActive = true
    }

    fun setViewProjection(vp: FloatArray) {
        val flipped = vp.copyOf()
        flipped[1] = -flipped[1]
        flipped[5] = -flipped[5]
        flipped[9] = -flipped[9]
        flipped[13] = -flipped[13]
        this.vpMatrix = flipped
    }

    fun setFog(view: FloatArray, enabled: Boolean, colorRgb: Int, start: Float, end: Float) {
        val snapshot = viewMatrix?.takeIf { it !== view && it.size == view.size } ?: FloatArray(view.size)
        view.copyInto(snapshot)
        viewMatrix = snapshot
        fogOn = enabled
        fogR = ((colorRgb shr 16) and 0xff) / 255f
        fogG = ((colorRgb shr 8) and 0xff) / 255f
        fogB = (colorRgb and 0xff) / 255f
        fogStart = start
        fogEnd = end
    }

    private var pointLightCount = 0
    private val pointLightPos = FloatArray(MAX_POINT_LIGHTS * 4)
    private val pointLightColor = FloatArray(MAX_POINT_LIGHTS * 3)

    fun setTextureAlphaPredicate(p: (Int) -> Boolean) { texHasAlpha = p }

    fun setTextureScrollLookup(packedSpeedOf: (Int) -> Int) { textureScrollOf = packedSpeedOf }

    fun setTextureTime(seconds: Float) { textureTime = seconds }

    private fun setTextureSpeedUniform(sh: ShaderProgram, texId: Int) {
        val packed = textureScrollOf(texId)
        sh.setUniform(uTextureSpeedLocation, TextureScroll.speedU(packed), TextureScroll.speedV(packed))
    }

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

    fun beginModel(modelMatrix: Matrix43, lightIdx: FloatArray? = null): Int {
        if (currentModelIndex >= MAX_MODELS) return -1

        val mb = matrixBuffer!!
        val idx = currentModelIndex

        val lightBase = MAX_MODELS * 16 + idx * 4
        if (lightIdx != null) {
            mb.put(lightBase, lightIdx[0]); mb.put(lightBase + 1, lightIdx[1])
            mb.put(lightBase + 2, lightIdx[2]); mb.put(lightBase + 3, lightIdx[3])
        } else {
            mb.put(lightBase, -1f); mb.put(lightBase + 1, -1f)
            mb.put(lightBase + 2, -1f); mb.put(lightBase + 3, -1f)
        }

        mb.put(idx * 16 + 0, modelMatrix.xA)
        mb.put(idx * 16 + 1, modelMatrix.yA)
        mb.put(idx * 16 + 2, modelMatrix.zA)
        mb.put(idx * 16 + 3, 0f)

        mb.put(idx * 16 + 4, modelMatrix.xB)
        mb.put(idx * 16 + 5, modelMatrix.yB)
        mb.put(idx * 16 + 6, modelMatrix.zB)
        mb.put(idx * 16 + 7, 0f)

        mb.put(idx * 16 + 8, modelMatrix.xC)
        mb.put(idx * 16 + 9, modelMatrix.yC)
        mb.put(idx * 16 + 10, modelMatrix.zC)
        mb.put(idx * 16 + 11, 0f)

        mb.put(idx * 16 + 12, modelMatrix.xD)
        mb.put(idx * 16 + 13, modelMatrix.yD)
        mb.put(idx * 16 + 14, modelMatrix.zD)
        mb.put(idx * 16 + 15, 1f)

        currentModelIndex++
        modelsThisBatch++
        return idx
    }

    fun addCachedModel(
        cachedModel: ModelCache.CachedModel,
        modelIndex: Int,
        translucent: Boolean = false,
        depthWrite: Boolean = true,
        seq: Int = 0,
        depthKey: Int = 0
    ): Int {
        if (!isFrameActive) return 0
        if (modelIndex < 0 || modelIndex >= MAX_MODELS) return 0

        val vb = vertexBuffer!!
        val faceCount = cachedModel.faceCount
        val vertexCount = cachedModel.vertexCount

        val neededBytes = vertexCount * BYTES_PER_VERTEX
        if (vb.remaining() < neededBytes) return 0

        if (!translucent) {
            var newSlots = 0
            val batches = cachedModel.materialBatches
            for (bi in batches.indices) {
                val mapKey = batches[bi].materialId + 1
                if (mapKey < materialIndexMap.size && materialIndexMap[mapKey] >= 0) continue
                var seen = false
                for (bj in 0 until bi) {
                    if (batches[bj].materialId == batches[bi].materialId) { seen = true; break }
                }
                if (!seen) newSlots++
            }
            if (activeMaterialCount + newSlots > MAX_MATERIALS) return 0
        }

        val baseVertex = totalVertexCount

        val t1 = System.nanoTime()
        val dstStart = vb.position()
        val words = cachedModel.vertexWords
        if (words != null) {
            val dup = vb.duplicate().order(vb.order())
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

        val t3 = System.nanoTime()
        if (translucent) {
            for (batch in cachedModel.materialBatches) {
                val batchCount = batch.indexCount
                if (batchCount == 0) continue
                if (orderedIndexCount + batchCount > orderedIndices.size) {
                    orderedIndices = orderedIndices.copyOf(
                        (orderedIndices.size * 2).coerceAtLeast(orderedIndexCount + batchCount)
                    )
                }
                if (orderedSegments == orderedSegMaterial.size) {
                    val grown = orderedSegments * 2
                    orderedSegMaterial = orderedSegMaterial.copyOf(grown)
                    orderedSegStart = orderedSegStart.copyOf(grown)
                    orderedSegCount = orderedSegCount.copyOf(grown)
                    orderedSegDepthWrite = orderedSegDepthWrite.copyOf(grown)
                    orderedSegSeq = orderedSegSeq.copyOf(grown)
                    orderedSegKey = orderedSegKey.copyOf(grown)
                }
                val batchIndices = batch.indices
                val out = orderedIndices
                val start = orderedIndexCount
                for (i in 0 until batchCount) out[start + i] = baseVertex + batchIndices[i]
                orderedSegMaterial[orderedSegments] = batch.materialId
                orderedSegStart[orderedSegments] = start
                orderedSegCount[orderedSegments] = batchCount
                orderedSegDepthWrite[orderedSegments] = depthWrite
                orderedSegSeq[orderedSegments] = seq
                orderedSegKey[orderedSegments] = depthKey
                orderedSegments++
                orderedIndexCount += batchCount
            }
            timeIndexCopy += System.nanoTime() - t3
            totalVertexCount += vertexCount
            totalIndexCount += vertexCount
            cachedModelsThisBatch++
            return faceCount
        }

        for (batch in cachedModel.materialBatches) {
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

            if (tempCount + batchCount > tempIndices.size) {
                val newSize = (tempIndices.size * 2).coerceAtLeast(tempCount + batchCount)
                tempIndices = tempIndices.copyOf(newSize)
                tempIndicesPerMaterial[materialSlot] = tempIndices
            }

            if (baseVertex == 0) {
                System.arraycopy(batchIndices, 0, tempIndices, tempCount, batchCount)
            } else {
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

    fun addFaceOptimized(
        textureId: Int,
        modelIndex: Int,
        x0: Float, y0: Float, z0: Float,
        nx0: Float, ny0: Float, nz0: Float,
        u0: Float, v0: Float,
        r0: Byte, g0: Byte, b0: Byte, a0: Byte,
        x1: Float, y1: Float, z1: Float,
        nx1: Float, ny1: Float, nz1: Float,
        u1: Float, v1: Float,
        r1: Byte, g1: Byte, b1: Byte, a1: Byte,
        x2: Float, y2: Float, z2: Float,
        nx2: Float, ny2: Float, nz2: Float,
        u2: Float, v2: Float,
        r2: Byte, g2: Byte, b2: Byte, a2: Byte
    ) {
        if (!isFrameActive) return
        if (modelIndex < 0 || modelIndex >= MAX_MODELS) return

        val vb = vertexBuffer!!

        if (vb.remaining() < 3 * BYTES_PER_VERTEX) return

        val modelIdxFloat = modelIndex.toFloat()

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

        vb.putFloat(x0).putFloat(y0).putFloat(z0)
        vb.putFloat(nx0).putFloat(ny0).putFloat(nz0)
        vb.putFloat(u0).putFloat(v0)
        vb.put(r0).put(g0).put(b0).put(a0)
        vb.putFloat(modelIdxFloat)

        vb.putFloat(x1).putFloat(y1).putFloat(z1)
        vb.putFloat(nx1).putFloat(ny1).putFloat(nz1)
        vb.putFloat(u1).putFloat(v1)
        vb.put(r1).put(g1).put(b1).put(a1)
        vb.putFloat(modelIdxFloat)

        vb.putFloat(x2).putFloat(y2).putFloat(z2)
        vb.putFloat(nx2).putFloat(ny2).putFloat(nz2)
        vb.putFloat(u2).putFloat(v2)
        vb.put(r2).put(g2).put(b2).put(a2)
        vb.putFloat(modelIdxFloat)

        var tempIndices = tempIndicesPerMaterial[materialSlot]
        var tempCount = tempIndexCountPerMaterial[materialSlot]

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

    fun endFrame(
        textureCache: (Int) -> Int,
        combineModeOf: (Int) -> Int = { 0 },
        needsBlendOf: (Int) -> Boolean = { false },
        alphaDiscard: Boolean = true
    ) {
        check(isFrameActive) { "No active frame" }

        timeAddModels = System.nanoTime() - frameStartTime

        if (totalIndexCount == 0 || vpMatrix == null) {
            isFrameActive = false
            timeEndFrame = System.nanoTime() - frameStartTime
            return
        }

        val sh = shader ?: return
        val vp = vpMatrix ?: return

        val uploadStartTime = System.nanoTime()

        vertexBuffer!!.flip()

        val ib = indexBuffer!!
        ib.clear()

        val sortedMaterials = (0 until activeMaterialCount).sortedBy {
            val texId = materialTextureIds[it]
            if (texId < 0) Int.MIN_VALUE else texId
        }

        for (slot in sortedMaterials) {
            val count = tempIndexCountPerMaterial[slot]
            if (count == 0) continue

            materialStartIndices[slot] = ib.position()
            materialIndexCounts[slot] = count

            val tempIndices = tempIndicesPerMaterial[slot]
            ib.put(tempIndices, 0, count)
        }

        orderedBase = ib.position()
        if (orderedIndexCount > 0) ib.put(orderedIndices, 0, orderedIndexCount)
        orderedPacked = orderedSegments
        orderedCursor = 0
        if (orderedSegments > 0) {
            val n = orderedSegments
            if (orderedOrder.size < n) {
                orderedOrder = IntArray(n * 2)
                orderedSortScratch = LongArray(n * 2)
            }
            val scratch = orderedSortScratch
            for (i in 0 until n) {
                val keyPart = (Int.MAX_VALUE.toLong() - orderedSegKey[i]) and 0xFFFFFFFFL
                val seqPart = orderedSegSeq[i].toLong() and 0xFFFFFL
                scratch[i] = (((keyPart shl 20) or seqPart) shl 12) or (i.toLong() and 0xFFFL) xor Long.MIN_VALUE
            }
            if (n <= 4096) {
                java.util.Arrays.sort(scratch, 0, n)
                for (i in 0 until n) orderedOrder[i] = (scratch[i] and 0xFFFL).toInt()
            } else {
                val idx = (0 until n).sortedWith(compareBy({ Int.MAX_VALUE.toLong() - orderedSegKey[it] }, { orderedSegSeq[it] }, { it }))
                for (i in 0 until n) orderedOrder[i] = idx[i]
            }
        }

        ib.flip()

        vbo!!.upload(vertexBuffer!!)
        ibo!!.upload(ib)

        timeUpload = System.nanoTime() - uploadStartTime

        matrixBuffer!!.position(0)
        matrixBuffer!!.limit(MAX_MODELS * 16 + currentModelIndex * 4)
        glBindBuffer(GL_UNIFORM_BUFFER, matrixUbo)
        glBufferData(GL_UNIFORM_BUFFER, (MAX_MODELS * 20 * 4).toLong(), GL_STREAM_DRAW)
        glBufferSubData(GL_UNIFORM_BUFFER, 0, matrixBuffer!!)
        glBindBuffer(GL_UNIFORM_BUFFER, 0)

        val renderStartTime = System.nanoTime()

        sh.bind()
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthFunc(GLWrapper.Constants.LEQUAL)
        if (CULL_FACE) {
            GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
            GLWrapper.cullFace(CULL_MODE)
            GLWrapper.frontFace(CULL_WIND)
        } else {
            GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
        }
        sh.setUniform(uTextureLocation, 0)
        sh.setUniform(uTextureTimeLocation, textureTime)
        sh.setUniform(
            uAlphaThresholdLocation,
            System.getProperty("render.alphaprobe")?.toFloatOrNull()
                ?: (if (alphaDiscard) ALPHA_CUTOFF else -1f)
        )

        setViewProjectionUniform(sh, vp)

        sh.setUniform(uSunDirectionLocation, sunDirX, sunDirY, sunDirZ)
        sh.setUniform(uSunBrightnessLocation, sunBrightness)
        sh.setUniform(uSunColorLocation, sunColorR, sunColorG, sunColorB)
        if (uPointLightCountLocation >= 0) GLWrapper.uniform1i(uPointLightCountLocation, pointLightCount)
        if (pointLightCount > 0) {
            if (uPointLightPosLocation >= 0) GLWrapper.uniform4fv(uPointLightPosLocation, pointLightPos, pointLightCount)
            if (uPointLightColorLocation >= 0) GLWrapper.uniform3fv(uPointLightColorLocation, pointLightColor, pointLightCount)
        }
        sh.setUniform(uContrastPosLocation, contrastPos)
        sh.setUniform(uContrastNegLocation, contrastNeg)
        viewMatrix?.let { glUniformMatrix4fv(uViewMatrixLocation, false, it) }
        sh.setUniform(uFogEnabledLocation, if (fogOn) 1 else 0)
        sh.setUniform(uFogStartLocation, fogStart)
        sh.setUniform(uFogEndLocation, fogEnd)
        sh.setUniform(uFogColorLocation, fogR, fogG, fogB)

        val blockIndex = glGetUniformBlockIndex(sh.getId(), "ModelMatrices")
        if (blockIndex != GL_INVALID_INDEX) {
            glUniformBlockBinding(sh.getId(), blockIndex, 0)
        }
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixUbo)

        vao!!.bind()
        ibo!!.bind()

        var lastTextureHandle = -1
        var lastBlend = -1

        if (ORDERED_DIAG && ofbDiagTick++ % 200 == 100) {
            val ip = IntArray(1)
            glGetIntegerv(GL_CURRENT_PROGRAM, ip); val cur = ip[0]
            glGetIntegerv(GL_VERTEX_ARRAY_BINDING, ip); val curVao = ip[0]
            glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, ip); val curIbo = ip[0]
            glGetIntegerv(GL_UNIFORM_BUFFER_BINDING, ip); val curUbo = ip[0]
            val ubo0 = IntArray(1); glGetIntegeri_v(GL_UNIFORM_BUFFER_BINDING, 0, ubo0)
            val attr4 = IntArray(1); glGetVertexAttribiv(4, GL_VERTEX_ATTRIB_ARRAY_ENABLED, attr4)
            val attr4buf = IntArray(1); glGetVertexAttribiv(4, GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, attr4buf)
            val attr0buf = IntArray(1); glGetVertexAttribiv(0, GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING, attr0buf)
            val readback = java.nio.ByteBuffer.allocateDirect(64).order(java.nio.ByteOrder.nativeOrder())
            glBindBuffer(GL_UNIFORM_BUFFER, matrixUbo)
            glGetBufferSubData(GL_UNIFORM_BUFFER, 0L, readback)
            glBindBuffer(GL_UNIFORM_BUFFER, 0)
            val m = FloatArray(16) { readback.getFloat(it * 4) }
            val mb = matrixBuffer!!
            val blockIdx = glGetUniformBlockIndex(sh.getId(), "ModelMatrices")
            val bind = IntArray(1); glGetActiveUniformBlockiv(sh.getId(), blockIdx, GL_UNIFORM_BLOCK_BINDING, bind)
            val blockSize = IntArray(1); glGetActiveUniformBlockiv(sh.getId(), blockIdx, GL_UNIFORM_BLOCK_DATA_SIZE, blockSize)
            val gpuIdx = java.nio.ByteBuffer.allocateDirect(12).order(java.nio.ByteOrder.nativeOrder())
            glGetBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0L, gpuIdx)
            val gpuVert = java.nio.ByteBuffer.allocateDirect(40).order(java.nio.ByteOrder.nativeOrder())
            glBindBuffer(GL_ARRAY_BUFFER, vbo!!.id)
            glGetBufferSubData(GL_ARRAY_BUFFER, 0L, gpuVert)
            val ib = indexBuffer!!
            val cpuIdx = "${ib.get(0)},${ib.get(1)},${ib.get(2)}"
            val vb = vertexBuffer!!
            val ibSize = IntArray(1); glGetBufferParameteriv(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_SIZE, ibSize)
            val vbSize = IntArray(1); glGetBufferParameteriv(GL_ARRAY_BUFFER, GL_BUFFER_SIZE, vbSize)
            System.err.println("[OfbDiag2] gpuIdx=${gpuIdx.getInt(0)},${gpuIdx.getInt(4)},${gpuIdx.getInt(8)} cpuIdx=$cpuIdx ibLimit=${ib.limit()} ibPos=${ib.position()} gpuIbBytes=${ibSize[0]} gpuVbBytes=${vbSize[0]} " +
                "gpuV0=(${gpuVert.getFloat(0)},${gpuVert.getFloat(4)},${gpuVert.getFloat(8)}) cpuV0=(${vb.getFloat(0)},${vb.getFloat(4)},${vb.getFloat(8)}) vbLimit=${vb.limit()} firstDraw[start=${materialStartIndices[sortedMaterials[0]]} count=${materialIndexCounts[sortedMaterials[0]]}] err=${glGetError()}")
            System.err.println("[OfbDiag] prog=${sh.getId()} current=$cur vao=${vao!!.id} curVao=$curVao ibo=$curIbo ubo=$matrixUbo curUbo=$curUbo ubo0=${ubo0[0]} " +
                "block=$blockIdx binding=${bind[0]} blockSize=${blockSize[0]} attr4[en=${attr4[0]} buf=${attr4buf[0]}] attr0buf=${attr0buf[0]} vboId=${vbo!!.id} " +
                "m0(gpu)=${m.take(4)}|${m.drop(12).take(4)} m0(cpu)=${(0 until 4).map { mb.get(it) }}|${(12 until 16).map { mb.get(it) }} models=$currentModelIndex verts=$totalVertexCount idx=$totalIndexCount err=${glGetError()}")
        }

        reflectionMaterials = sortedMaterials.toIntArray()
        reflectionTextureCache = textureCache
        reflectionCombineModeOf = combineModeOf

        for (slot in sortedMaterials) {
            val indexCount = materialIndexCounts[slot]
            if (indexCount == 0) continue

            val texId = materialTextureIds[slot]
            val textureHandle = if (texId >= 0) textureCache(texId) else 0

            if (textureHandle != lastTextureHandle) {
                if (textureHandle > 0) {
                    GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                    GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                    sh.setUniform(uTextureEnabledLocation, 1)
                    sh.setUniform(uCombineModeLocation, if (texId >= 0) combineModeOf(texId) else 0)
                    sh.setUniform(uTexAlphaLocation, if (TEX_ALPHA && texHasAlpha(texId)) 1 else 0)
                    setTextureSpeedUniform(sh, texId)
                } else {
                    sh.setUniform(uTextureEnabledLocation, 0)
                }
                lastTextureHandle = textureHandle
                stateChangesThisBatch++
            }

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

        drawCallsCumulative += drawCallsThisBatch
        trianglesCumulative += trianglesThisBatch
        stateChangesCumulative += stateChangesThisBatch
        modelsCumulative += modelsThisBatch
        cachedModelsCumulative += cachedModelsThisBatch

        isFrameActive = false
    }

    fun pendingTranslucent(): Int = orderedPacked - orderedCursor

    fun drawTranslucent(
        textureCache: (Int) -> Int,
        combineModeOf: (Int) -> Int = { 0 },
        upToKey: Int = Int.MIN_VALUE,
        upToSeq: Int = Int.MAX_VALUE
    ) {
        val start = orderedCursor
        var end = start
        while (end < orderedPacked) {
            val i = orderedOrder[end]
            val k = orderedSegKey[i]
            if (k > upToKey || (k == upToKey && orderedSegSeq[i] < upToSeq)) end++ else break
        }
        if (end == start) return
        orderedCursor = end
        val sh = shader ?: return
        val t0 = System.nanoTime()
        sh.bind()
        GLWrapper.enable(GLWrapper.Constants.BLEND)
        GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthMask(true)
        if (CULL_FACE) {
            GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
            GLWrapper.cullFace(CULL_MODE)
            GLWrapper.frontFace(CULL_WIND)
        } else {
            GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
        }
        glBindBufferBase(GL_UNIFORM_BUFFER, 0, matrixUbo)
        vao!!.bind()
        ibo!!.bind()
        var lastTex = Int.MIN_VALUE
        var depthWriteOn = true
        for (oi in start until end) {
            val seg = orderedOrder[oi]
            val depthWrite = orderedSegDepthWrite[seg]
            if (depthWrite != depthWriteOn) {
                GLWrapper.depthMask(depthWrite)
                depthWriteOn = depthWrite
            }
            val texId = orderedSegMaterial[seg]
            if (texId != lastTex) {
                val textureHandle = if (texId >= 0) textureCache(texId) else 0
                if (textureHandle > 0) {
                    GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                    GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                    sh.setUniform(uTextureEnabledLocation, 1)
                    sh.setUniform(uCombineModeLocation, combineModeOf(texId))
                    sh.setUniform(uTexAlphaLocation, if (TEX_ALPHA && texHasAlpha(texId)) 1 else 0)
                    setTextureSpeedUniform(sh, texId)
                } else {
                    sh.setUniform(uTextureEnabledLocation, 0)
                }
                lastTex = texId
                stateChangesCumulative++
            }
            val count = orderedSegCount[seg]
            GLWrapper.drawElements(
                TRIANGLES, count, UNSIGNED_INT,
                (orderedBase + orderedSegStart[seg]).toLong() * 4
            )
            drawCallsCumulative++
            trianglesCumulative += count / 3
        }
        if (!depthWriteOn) GLWrapper.depthMask(true)
        if (ORDERED_DIAG && orderedDiagTick++ % 200 == 100) {
            val seg0 = orderedOrder[start]
            System.err.println("[OrderedDiag] segments=${end - start} orderedBase=$orderedBase totalIdx=$totalIndexCount totalVerts=$totalVertexCount " +
                "seg0[mat=${orderedSegMaterial[seg0]} start=${orderedSegStart[seg0]} count=${orderedSegCount[seg0]} key=${orderedSegKey[seg0]} " +
                "idx0=${orderedIndices[orderedSegStart[seg0]]}] models=$currentModelIndex glErr=${glGetError()} " +
                run {
                    val vb = vertexBuffer!!
                    val v = orderedIndices[orderedSegStart[seg0]]
                    val base = v * BYTES_PER_VERTEX
                    if (base + 40 <= vb.capacity()) {
                        "v0[pos=(${vb.getFloat(base)},${vb.getFloat(base + 4)},${vb.getFloat(base + 8)}) " +
                            "rgba=(${vb.get(base + 32).toInt() and 0xff},${vb.get(base + 33).toInt() and 0xff},${vb.get(base + 34).toInt() and 0xff},${vb.get(base + 35).toInt() and 0xff}) " +
                            "modelIdx=${vb.getFloat(base + 36)}] vbLimit=${vb.limit()} vbPos=${vb.position()}"
                    } else "v0=out-of-range"
                })
        }
        vao!!.unbind()
        sh.unbind()
        timeRender += System.nanoTime() - t0
    }

    private val ORDERED_DIAG = System.getProperty("render.transdiag") != null
    private var orderedDiagTick = 0
    private var ofbDiagTick = 0

    private fun setViewProjectionUniform(shader: ShaderProgram, vp: FloatArray) {

        val colMajor = FloatArray(16)
        colMajor[0] = vp[0]
        colMajor[1] = vp[1]
        colMajor[2] = vp[2]
        colMajor[3] = vp[3]
        colMajor[4] = vp[4]
        colMajor[5] = vp[5]
        colMajor[6] = vp[6]
        colMajor[7] = vp[7]
        colMajor[8] = vp[8]
        colMajor[9] = vp[9]
        colMajor[10] = vp[10]
        colMajor[11] = vp[11]
        colMajor[12] = vp[12]
        colMajor[13] = vp[13]
        colMajor[14] = vp[14]
        colMajor[15] = vp[15]

        glUniformMatrix4fv(uViewProjectionLocation, false, colMajor)
    }

    fun isActive(): Boolean = isFrameActive

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
