package com.jagex.game.runetek5.scene.ground

import com.jagex.game.runetek5.cache.node.HashTable
import com.jagex.game.runetek5.cache.node.LinkedList
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ShortNode
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.graphics.light.LightGL
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.graphics.shadow.ShadowGL
import com.jagex.game.runetek5.graphics.shadow.ShadowMapperGL
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer
import com.jagex.game.runetek5.imageproducer.AbstractDisplay
import com.jagex.game.runetek5.scene.ground.GroundFunctionMask.*
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GroundMaterialGL
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboArrayBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import com.jagex.game.runetek5.utils.ArrayUtils
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.buffer.ArrayBufferGL
import jaclib.memory.Stream
import jaclib.memory.heap.NativeHeapBuffer
import jaggl.OpenGL
import org.lwjgl.system.MemoryUtil
import com.jagex.game.runetek5.toolkit.opengl.*
open class GroundGL(
    toolkit: OpenGlToolkit,
    floorFunctionMask: Int,
    waterFunctionMask: Int,
    regionSizeX: Int,
    regionSizeY: Int,
    tileUnits: Int,
    heightsCurrentLvl: Array<IntArray>,
    heightsNextLvl: Array<IntArray>
) : Ground(regionSizeX, regionSizeY, tileUnits, heightsCurrentLvl) {

    companion object {
        @JvmStatic var shadowProbeTick = 0
        private val SUN_PROBE = System.getProperty("render.sunprobe") != null
        /** -Drender.groundshadow=false skips the legacy shadow overlay (A/B aid). */
        private val GROUND_SHADOW_OVERLAY = System.getProperty("render.groundshadow")?.equals("false", true) != true
        private var sunProbeTick = 0
        @JvmField val GROUND_MATRIX = FloatArray(16)
    }

    @JvmField var shadowMapper: ShadowMapperGL? = null
    @JvmField var tileOffsetsZ: Array<Array<IntArray?>>? = null
    @JvmField var tileOffsetsX: Array<Array<IntArray?>>? = null
    @JvmField var tileColors: Array<Array<IntArray?>>? = null
    @JvmField var lightOffsetsB: Array<Array<IntArray?>>? = null
    @JvmField var tileBlendColors: Array<Array<IntArray?>>? = null
    @JvmField var lightOffsetsA: Array<Array<IntArray?>>? = null
    @JvmField var materials: Array<Array<Array<GroundMaterialGL>?>>? = null
    @JvmField var shadowing: Array<ByteArray>? = null
    @JvmField var materialTable: HashTable? = null
    @JvmField var lightingZ: Array<FloatArray>? = null
    @JvmField var lightingY: Array<FloatArray>? = null
    @JvmField var lightingX: Array<FloatArray>? = null
    @JvmField var goundMaterialNodes: Array<Node<*>?>? = null
    @JvmField var dataArrayBuffer: ArrayBufferGL? = null
    // Modern rendering: raw vertex data stored for LWJGL-only rendering (no legacy VBO dependency)
    @JvmField var modernVertexData: java.nio.ByteBuffer? = null
    @JvmField var modernVertexStride: Int = 0
    @JvmField var modernVertexCount: Int = 0

    /**
     * Bumped whenever [modernVertexData] is rebuilt.
     *
     * The terrain mesh for a ground is static between rebuilds, but the modern path
     * used to re-upload the WHOLE snapshot with glBufferData on every material draw
     * — ~35 times a frame, tens of megabytes of PCIe traffic per frame for data that
     * had not changed. The renderer now uploads only when this revision moves.
     */
    @JvmField var modernVertexRevision: Int = 0
    /** Hash of the visible tile set of the frame being drawn (see renderShadows). */
    @JvmField var modernVisibleStamp: Long = 0L

    /** GL name of the resident copy of [modernVertexData], 0 until first upload. */
    @JvmField var modernVboId: Int = 0

    /** [modernVertexRevision] the resident copy was uploaded from; -1 = never. */
    @JvmField var modernVboRevision: Int = -1
    @JvmField var vertexBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var colorBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var textureBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var normalsBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var amountOfTexturedFaces = 0
    @JvmField var totalMaterializedTiles = 0
    @JvmField var maxColorCount = 0
    @JvmField var currentColorCount = 0
    @JvmField val activeLights = LinkedList()
    @JvmField val activeToolkit: OpenGlToolkit
    @JvmField val quarterTileScale: Int
    @JvmField val quarterTileUnits: Int
    @JvmField val floorFunctionMask: Int
    @JvmField val waterFunctionMask: Int
    @JvmField val materialIndices: Array<ShortArray?>
    @JvmField val shadowFunctionMasks: Array<ByteArray>

    init {
        activeToolkit = toolkit
        this.floorFunctionMask = floorFunctionMask
        this.waterFunctionMask = waterFunctionMask
        quarterTileScale = tileScale - 2
        quarterTileUnits = 1 shl quarterTileScale
        lightOffsetsA = Array(regionSizeX) { Array(regionSizeY) { null } }
        materials = Array(regionSizeX) { Array(regionSizeY) { null } }
        tileOffsetsX = Array(regionSizeX) { Array(regionSizeY) { null } }
        tileOffsetsZ = Array(regionSizeX) { Array(regionSizeY) { null } }
        tileColors = Array(regionSizeX) { Array(regionSizeY) { null } }
        tileBlendColors = Array(regionSizeX) { Array(regionSizeY) { null } }
        materialIndices = Array(regionSizeY * regionSizeX) { null }
        shadowFunctionMasks = Array(regionSizeX) { ByteArray(regionSizeY) }
        shadowing = Array(regionSizeX + 1) { ByteArray(regionSizeY + 1) }
        lightingX = Array(width + 1) { FloatArray(length + 1) }
        lightingY = Array(width + 1) { FloatArray(length + 1) }
        lightingZ = Array(width + 1) { FloatArray(length + 1) }
        for (tileY in 1 until length) {
            for (tileX in 1 until width) {
                val heightDeltaHorizontal = heightsNextLvl[tileX + 1][tileY] - heightsNextLvl[tileX - 1][tileY]
                val hiehgtDeltaVertical = heightsNextLvl[tileX][tileY + 1] - heightsNextLvl[tileX][tileY - 1]
                val intensity = (1.0 / Math.sqrt((512 * 512 * 4 + heightDeltaHorizontal * heightDeltaHorizontal + hiehgtDeltaVertical * hiehgtDeltaVertical).toDouble())).toFloat()
                lightingX!![tileX][tileY] = heightDeltaHorizontal * intensity
                lightingY!![tileX][tileY] = (-512 * 2) * intensity
                lightingZ!![tileX][tileY] = hiehgtDeltaVertical * intensity
            }
        }
        materialTable = HashTable(128)
        if ((waterFunctionMask and GroundFunctionMask.HD_SHADOWS) != 0) {
            shadowMapper = ShadowMapperGL(activeToolkit, this)
        }
    }

    fun projectShadow(shadowGl: ShadowGL, offsetsXCount: Int, offsetsYCount: Int) {
        val offsetsX = tileOffsetsX!![offsetsXCount][offsetsYCount]!!
        val offsetsZ = tileOffsetsZ!![offsetsXCount][offsetsYCount]!!
        val totalOffsets = offsetsX.size
        val scratch = activeToolkit.projectionScratch(totalOffsets)
        val sceneShadowOffX = scratch.verticesX
        val sceneShadowOffZ = scratch.verticesY
        for (index in 0 until totalOffsets) {
            sceneShadowOffX[index] = offsetsX[index] shr activeToolkit.shadowScale
            sceneShadowOffZ[index] = offsetsZ[index] shr activeToolkit.shadowScale
        }
        var count = 0
        while (count < totalOffsets) {
            val xA = sceneShadowOffX[count]
            val zA = sceneShadowOffZ[count++]
            val xB = sceneShadowOffX[count]
            val zB = sceneShadowOffZ[count++]
            val xC = sceneShadowOffX[count]
            val zC = sceneShadowOffZ[count++]
            if ((xA - xB) * (zB - zC) - (zB - zA) * (xC - xB) > 0) {
                shadowGl.markShadedTriangle(zA, zB, zC, xA, xB, xC)
            }
        }
    }

    override fun setTileShadow(x: Int, y: Int, shadow: Int) {
        if ((shadowing!![x][y].toInt() and 0xff) < shadow) {
            shadowing!![x][y] = shadow.toByte()
        }
    }

    override fun calculateBlockTiles(
        x: Int,
        y: Int,
        offsetsX: IntArray,
        heightOffsets: IntArray,
        offsetsZ: IntArray,
        unused: IntArray,
        trianglesA: IntArray,
        trianglesB: IntArray,
        trianglesC: IntArray,
        floorColor: IntArray,
        minimapColor: IntArray,
        overlayTextureIds: IntArray,
        textureScales: IntArray,
        waterTile: WaterTile?
    ) {
        val triangleCount = floorColor.size
        val expandedOffsetsX = IntArray(triangleCount * 3)
        val expandedOffsetsZ = IntArray(triangleCount * 3)
        val expandedMainColors = IntArray(triangleCount * 3)
        val expandedBlendColors = IntArray(triangleCount * 3)
        val expandedOverlayTextures = IntArray(triangleCount * 3)
        val expandedTextureScales = IntArray(triangleCount * 3)
        val expandedHeightOffsets = IntArray(triangleCount * 3)
        val expandedUnused = IntArray(triangleCount * 3)
        var vertexIndex = 0

        for (triangleIndex in 0 until triangleCount) {
            val vertexA = trianglesA[triangleIndex]
            val vertexB = trianglesB[triangleIndex]
            val vertexC = trianglesC[triangleIndex]
            expandedOffsetsX[vertexIndex] = offsetsX[vertexA]
            expandedOffsetsZ[vertexIndex] = offsetsZ[vertexA]
            expandedMainColors[vertexIndex] = floorColor[triangleIndex]
            expandedOverlayTextures[vertexIndex] = overlayTextureIds[triangleIndex]
            expandedTextureScales[vertexIndex] = textureScales[triangleIndex]
            expandedBlendColors[vertexIndex] = minimapColor[triangleIndex]
            expandedHeightOffsets[vertexIndex] = heightOffsets[vertexA]
            expandedUnused[vertexIndex] = unused[vertexA]

            ++vertexIndex
            expandedOffsetsX[vertexIndex] = offsetsX[vertexB]
            expandedOffsetsZ[vertexIndex] = offsetsZ[vertexB]
            expandedMainColors[vertexIndex] = floorColor[triangleIndex]
            expandedOverlayTextures[vertexIndex] = overlayTextureIds[triangleIndex]
            expandedTextureScales[vertexIndex] = textureScales[triangleIndex]
            expandedBlendColors[vertexIndex] = minimapColor[triangleIndex]
            expandedHeightOffsets[vertexIndex] = heightOffsets[vertexB]
            expandedUnused[vertexIndex] = unused[vertexB]

            ++vertexIndex
            expandedOffsetsX[vertexIndex] = offsetsX[vertexC]
            expandedOffsetsZ[vertexIndex] = offsetsZ[vertexC]
            expandedMainColors[vertexIndex] = floorColor[triangleIndex]
            expandedOverlayTextures[vertexIndex] = overlayTextureIds[triangleIndex]
            expandedTextureScales[vertexIndex] = textureScales[triangleIndex]
            expandedBlendColors[vertexIndex] = minimapColor[triangleIndex]
            expandedHeightOffsets[vertexIndex] = heightOffsets[vertexC]
            expandedUnused[vertexIndex] = unused[vertexC]

            ++vertexIndex
        }

        addBlendedTile(x, y, expandedOffsetsX, expandedHeightOffsets, expandedOffsetsZ, expandedUnused, expandedMainColors, expandedBlendColors, expandedOverlayTextures, expandedTextureScales, waterTile, false)
    }

    fun renderShadows(cameraGridX: Int, cameraGridY: Int, visibleTileRange: Int, tilesOnScreen: Array<BooleanArray>, belowLowestPlane: Boolean) {
        if (goundMaterialNodes != null) {
            var tileAreaSize = visibleTileRange + visibleTileRange + 1
            tileAreaSize *= tileAreaSize
            if (activeToolkit.projectedVerticesZ.size < tileAreaSize) {
                activeToolkit.projectedVerticesZ = IntArray(tileAreaSize)
            }

            if (activeToolkit.textureBuffer.buffer.size < amountOfTexturedFaces * 2) {
                activeToolkit.textureBuffer = GlBitBuffer(amountOfTexturedFaces * 2)
            }

            var startX = cameraGridX - visibleTileRange
            val originalStartX = startX
            if (startX < 0) {
                startX = 0
            }

            var startY = cameraGridY - visibleTileRange
            val originalStartY = startY
            if (startY < 0) {
                startY = 0
            }

            var endX = visibleTileRange + cameraGridX
            if (endX > width - 1) {
                endX = width - 1
            }

            var endY = visibleTileRange + cameraGridY
            if (endY > length - 1) {
                endY = length - 1
            }

            var visibleTileCount = 0
            val visibleTileIndices = activeToolkit.projectedVerticesZ

            for (tileX in startX..endX) {
                val visibilityRow = tilesOnScreen[tileX - originalStartX]

                for (tileY in startY..endY) {
                    if (visibilityRow[tileY - originalStartY]) {
                        visibleTileIndices[visibleTileCount++] = tileX + tileY * width
                    }
                }
            }

            activeToolkit.setupMatrixRendering()
            activeToolkit.setLightingEnabled((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0)

            // -Drender.sunprobe: the exact lighting inputs this ground pass sees, so the
            // two toolkits can be compared on the same scene (a brightness gap between
            // them is either these numbers or the shader, never both).
            if (SUN_PROBE && sunProbeTick++ % 300 == 100) {
                val tk = activeToolkit
                val sd = tk.sunDirection
                System.err.println(
                    "[SunProbe] toolkit=${tk.javaClass.simpleName} hd=${(waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0} " +
                        "sunBrightness=${tk.sunBrightness} sunColorIntensity=${tk.sunColorIntensity} " +
                        "antiSunColorIntensity=${tk.antiSunColorIntensity} " +
                        "sunDir=(${sd[0]},${sd[1]},${sd[2]}) sunColor=(${tk.sunColorRed},${tk.sunColorGreen},${tk.sunColorBlue}) " +
                        "fog=${Integer.toHexString(tk.fogColor)} density=${tk.fogDensity} start=${tk.fogStartPosition} end=${tk.fogEndPosition} " +
                        "materials=${goundMaterialNodes?.size} visibleTiles=$visibleTileCount"
                )
                if (tk !is ModernGLToolkit) {
                    // Fixed-function state as GL holds it right now, not as the toolkit
                    // fields claim. Queried through LWJGL against the current context.
                    try {
                        org.lwjgl.opengl.GL.createCapabilities()
                        val f = FloatArray(4)
                        val i = IntArray(4)
                        val sb = StringBuilder("[SunProbeGL]")
                        org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_LIGHT_MODEL_AMBIENT, f); sb.append(" modelAmbient=${f.toList()}")
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_LIGHTING, i); sb.append(" lighting=${i[0]}")
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_NORMALIZE, i); sb.append(" normalize=${i[0]}")
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL, i); sb.append(" rescale=${i[0]}")
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_COLOR_MATERIAL, i); sb.append(" colorMaterial=${i[0]}")
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_COLOR_MATERIAL_PARAMETER, i); sb.append(" cmParam=${i[0]}")
                        for (l in 0 until 3) {
                            val light = org.lwjgl.opengl.GL11.GL_LIGHT0 + l
                            org.lwjgl.opengl.GL11.glGetIntegerv(light, i); sb.append(" L$l[on=${i[0]}")
                            org.lwjgl.opengl.GL11.glGetLightfv(light, org.lwjgl.opengl.GL11.GL_AMBIENT, f); sb.append(" amb=${f.toList()}")
                            org.lwjgl.opengl.GL11.glGetLightfv(light, org.lwjgl.opengl.GL11.GL_DIFFUSE, f); sb.append(" dif=${f.toList()}")
                            org.lwjgl.opengl.GL11.glGetLightfv(light, org.lwjgl.opengl.GL11.GL_SPECULAR, f); sb.append(" spec=${f.toList()}")
                            org.lwjgl.opengl.GL11.glGetLightfv(light, org.lwjgl.opengl.GL11.GL_POSITION, f); sb.append(" pos=${f.toList()}]")
                        }
                        org.lwjgl.opengl.GL11.glGetMaterialfv(org.lwjgl.opengl.GL11.GL_FRONT, org.lwjgl.opengl.GL11.GL_EMISSION, f); sb.append(" matEmission=${f.toList()}")
                        org.lwjgl.opengl.GL11.glGetMaterialfv(org.lwjgl.opengl.GL11.GL_FRONT, org.lwjgl.opengl.GL11.GL_SPECULAR, f); sb.append(" matSpec=${f.toList()}")
                        val mv = FloatArray(16)
                        org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX, mv); sb.append(" modelview=${mv.toList()}")
                        System.err.println(sb)
                    } catch (e: Throwable) {
                        System.err.println("[SunProbeGL] failed: $e")
                    }
                }
            }

            // Native ground shadows for the modern pipeline: select THIS ground's shadow
            // map BEFORE its materials draw; the ground shader samples it per fragment.
            // No fixed function.
            val sm = shadowMapper
            if (sm != null && activeToolkit is ModernGLToolkit) {
                val texelSize = (width * tileUnits).toFloat() / (sm.width - 2).toFloat()
                (activeToolkit as ModernGLToolkit).groundRendererForShadows()
                    ?.setShadowMapper(sm, texelSize)
            }

            // Stamp of this frame's visible tile set. The modern ground path keys its
            // per-material index lists and resident index buffers on it, so a frame
            // whose camera did not move rebuilds and re-uploads nothing.
            if (activeToolkit is ModernGLToolkit) {
                var h = 1469598103934665603L xor visibleTileCount.toLong()
                for (i in 0 until visibleTileCount) {
                    h = (h xor visibleTileIndices[i].toLong()) * 1099511628211L
                }
                modernVisibleStamp = h xor (modernVertexRevision.toLong() shl 40)
            }

            for (tileX in goundMaterialNodes!!.indices) {
                (goundMaterialNodes!![tileX] as GroundMaterialGL).drawMaterialTriangles(visibleTileIndices, visibleTileCount)
            }

            // FIXED-FUNCTION, LEGACY TOOLKIT ONLY. The modern pipeline must never run
            // fixed-function work: it lights the ground in its own shaders instead
            // (ModernGroundRenderer point lights). No crossover between pipelines.
            if (!activeLights.isEmpty() && activeToolkit !is ModernGLToolkit) {
                val fogColor = activeToolkit.fogColor
                val fogDensity = activeToolkit.fogDensity
                activeToolkit.calculateFog(0, fogDensity, activeToolkit.fogOffset)
                activeToolkit.setLightingEnabled(false)
                activeToolkit.setDepthMask(false)
                activeToolkit.setBlendMode(128)
                activeToolkit.renderTexture(-2)
                activeToolkit.setActiveTexture(activeToolkit.texture2d)
                activeToolkit.loadTextureCombineMode(GL_MODULATE, GL_REPLACE)
                activeToolkit.setTexture(0, GL_CONSTANT, 770)
                activeToolkit.setTextureTransparency(0, 34167)

                @Suppress("UNCHECKED_CAST")
                var light: Node<*>? = activeLights.getTail() as Node<*>?
                while (light != null) {
                    val glLight = light as LightGL
                    glLight.drawLightMesh(cameraGridX, cameraGridY, visibleTileRange, tilesOnScreen)
                    @Suppress("UNCHECKED_CAST")
                    light = activeLights.getPrevious() as Node<*>?
                }

                activeToolkit.setTexture(0, GL_TEXTURE, 768)
                activeToolkit.setTextureTransparency(0, GL_TEXTURE)
                activeToolkit.setActiveTexture(null)
                activeToolkit.calculateFog(fogColor, fogDensity, activeToolkit.fogOffset)
            }

            if (System.getProperty("shadow.dims") != null && shadowMapper != null && shadowProbeTick++ % 300 == 250) {
                val sm = shadowMapper!!
                var nz = 0; var mx = 0
                for (b in sm.shadowMapperData) { val v = b.toInt() and 0xff; if (v != 0) nz++; if (v > mx) mx = v }
                System.err.println("[ShadowData] nonZero=$nz max=$mx version=${sm.shadowVersion}")
                System.err.println("[ShadowDims] map=${sm.width}x${sm.length} bytes=${sm.shadowMapperData.size} " +
                    "groundTiles=${width}x${length} tileUnits=$tileUnits tileScale=$tileScale " +
                    "worldUnits=${width * tileUnits}x${length * tileUnits}")
            }
            // FIXED-FUNCTION, LEGACY TOOLKIT ONLY (see above).
            if (shadowMapper != null && activeToolkit !is ModernGLToolkit && GROUND_SHADOW_OVERLAY) {
                OpenGL.glPushMatrix()
                OpenGL.glTranslatef(0.0f, -1.0f, 0.0f)
                activeToolkit.toggleDrawingClientstates(vertexBuffer, null, null, textureBuffer)
                shadowMapper!!.renderShadowOverlay(cameraGridX, cameraGridY, visibleTileRange, tilesOnScreen, belowLowestPlane)
                OpenGL.glPopMatrix()
            }
        }
    }

    override fun resetLight() {
        if (currentColorCount > 0) {
            val intensity = Array(width + 1) { ByteArray(length + 1) }
            for (x in 1 until width) {
                for (y in 1 until length) {
                    intensity[x][y] = (
                        (shadowing!![x][y - 1].toInt() shr 2) +    // south of center
                            (shadowing!![x][y].toInt() shr 1) +    // center
                            (shadowing!![x - 1][y].toInt() shr 2) + // west of center
                            (shadowing!![x][y + 1].toInt() shr 3) + // north of center
                            (shadowing!![x + 1][y].toInt() shr 3)   // east of center
                        ).toByte()
                }
            }
            goundMaterialNodes = arrayOfNulls<Node<*>>(materialTable!!.nodeCount())
            materialTable!!.values(goundMaterialNodes!!)
            for (nodeIndex in goundMaterialNodes!!.indices) {
                (goundMaterialNodes!![nodeIndex] as GroundMaterialGL).generateNativeBuffer(currentColorCount)
            }
            var bufferEntrySize = 24
            if (lightOffsetsB != null) {
                bufferEntrySize += 4
            }
            if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                bufferEntrySize += 12
            }
            val nativeHeapBuffer = activeToolkit.nativeHeap!!.createBuffer(bufferEntrySize * currentColorCount, false)
            val nativeStream = Stream(nativeHeapBuffer)
            val materialsByVertice = Array<GroundMaterialGL?>(currentColorCount) { null }
            var furthestBitValue = MathUtils.furtestBitValue(currentColorCount / 4)
            if (furthestBitValue < 1) {
                furthestBitValue = 1
            }

            val hashTable = HashTable(furthestBitValue)
            val someMaterialArray = Array<GroundMaterialGL?>(maxColorCount) { null }
            for (x in 0 until width) {
                for (y in 0 until length) {
                    if (tileColors!![x][y] != null) {
                        val vertexMaterials = materials!![x][y]!!
                        val offsetsX = tileOffsetsX!![x][y]!!
                        val offsetsZ = tileOffsetsZ!![x][y]!!
                        val mainColors = tileColors!![x][y]!!
                        var blendingColors = tileBlendColors!![x][y]
                        if (blendingColors == null) {
                            blendingColors = mainColors
                        }
                        val floorOffsets = if (lightOffsetsA != null) lightOffsetsA!![x][y] else null
                        val lightOffsetsForTile = if (lightOffsetsB != null) lightOffsetsB!![x][y] else null

                        val lightingXSouthWest = lightingX!![x][y]
                        val lightingYSouthWest = lightingY!![x][y]
                        val lightingZSouthWest = lightingZ!![x][y]
                        val lightingXNorthWest = lightingX!![x][y + 1]
                        val lightingYNorthWest = lightingY!![x][y + 1]
                        val lightingZNorthWest = lightingZ!![x][y + 1]
                        val lightingXNorthEast = lightingX!![x + 1][y + 1]
                        val lightingYNorthEast = lightingY!![x + 1][y + 1]
                        val lightingZNorthEast = lightingZ!![x + 1][y + 1]
                        val lightingXSouthEast = lightingX!![x + 1][y]
                        val lightingYSouthEast = lightingY!![x + 1][y]
                        val lightingZSouthEast = lightingZ!![x + 1][y]
                        val lightStrengthSouthWest = intensity[x][y].toInt() and 0xff
                        val lightStrengthNorthWest = intensity[x][y + 1].toInt() and 0xff
                        val lightStrengthNorthEast = intensity[x + 1][y + 1].toInt() and 0xff
                        val lightStrengthSouthEast = intensity[x + 1][y].toInt() and 0xff
                        var uniqueMaterialCount = 0

                        loop@ for (colorIndex in mainColors.indices) {
                            val currentMaterialNode = vertexMaterials[colorIndex]
                            for (index in 0 until uniqueMaterialCount) {
                                if (currentMaterialNode === someMaterialArray[index]) {
                                    continue@loop
                                }
                            }
                            someMaterialArray[uniqueMaterialCount++] = currentMaterialNode
                        }

                        val vertexMaterialIndices = ShortArray(mainColors.size)
                        @Suppress("UNCHECKED_CAST")
                        (materialIndices as Array<ShortArray?>)[x + y * width] = vertexMaterialIndices

                        for (loop in mainColors.indices) {
                            val sceneX = (x shl tileScale) + offsetsX[loop]
                            val sceneY = (y shl tileScale) + offsetsZ[loop]
                            val downscaledX = sceneX shr quarterTileScale
                            val downscaledY = sceneY shr quarterTileScale
                            val mainColor = mainColors[loop]
                            val blendColor = blendingColors[loop]
                            val floorHeightOffset = if (floorOffsets != null) floorOffsets[loop] else 0
                            val uid = (blendColor.toLong() shl 48) or ((mainColor.toLong() shl 32)) or ((downscaledX.toLong() shl 16)) or downscaledY.toLong()
                            val offsetX = offsetsX[loop]
                            val offsetZ = offsetsZ[loop]
                            val baseStrength = 74
                            var directColor = 0
                            var sunDotProduct = 1.0f
                            var lightX: Float
                            var lightY: Float
                            var lightZ: Float
                            var lightStrength: Int
                            if (offsetX == 0 && offsetZ == 0) {
                                lightX = lightingXSouthWest
                                lightY = lightingYSouthWest
                                lightZ = lightingZSouthWest
                                lightStrength = baseStrength - lightStrengthSouthWest
                            } else if (offsetX == 0 && offsetZ == tileUnits) {
                                lightX = lightingXNorthWest
                                lightY = lightingYNorthWest
                                lightZ = lightingZNorthWest
                                lightStrength = baseStrength - lightStrengthNorthWest
                            } else if (offsetX == tileUnits && offsetZ == tileUnits) {
                                lightX = lightingXNorthEast
                                lightY = lightingYNorthEast
                                lightZ = lightingZNorthEast
                                lightStrength = baseStrength - lightStrengthNorthEast
                            } else if (offsetX == tileUnits && offsetZ == 0) {
                                lightX = lightingXSouthEast
                                lightY = lightingYSouthEast
                                lightZ = lightingZSouthEast
                                lightStrength = baseStrength - lightStrengthSouthEast
                            } else {
                                val partualOffsetX = offsetX.toFloat() / tileUnits
                                val partualOffsetZ = offsetZ.toFloat() / tileUnits
                                val lightDeltaXSouth = lightingXSouthWest + (lightingXSouthEast - lightingXSouthWest) * partualOffsetX
                                val lightDeltaYSouth = lightingYSouthWest + (lightingYSouthEast - lightingYSouthWest) * partualOffsetX
                                val lightDeltaZSouth = lightingZSouthWest + (lightingZSouthEast - lightingZSouthWest) * partualOffsetX
                                val lightDeltaXNorth = lightingXNorthWest + (lightingXNorthEast - lightingXNorthWest) * partualOffsetX
                                val lightDeltaYNorth = lightingYNorthWest + (lightingYNorthEast - lightingYNorthWest) * partualOffsetX
                                val lightDeltaZNorth = lightingZNorthWest + (lightingZNorthEast - lightingZNorthWest) * partualOffsetX

                                lightX = lightDeltaXSouth + (lightDeltaXNorth - lightDeltaXSouth) * partualOffsetZ
                                lightY = lightDeltaYSouth + (lightDeltaYNorth - lightDeltaYSouth) * partualOffsetZ
                                lightZ = lightDeltaZSouth + (lightDeltaZNorth - lightDeltaZSouth) * partualOffsetZ
                                val mainLight = lightStrengthSouthWest + (offsetX * (lightStrengthSouthEast - lightStrengthSouthWest) shr tileScale)
                                val adjustmentLight = lightStrengthNorthWest + (offsetX * (lightStrengthNorthEast - lightStrengthNorthWest) shr tileScale)
                                lightStrength = baseStrength - (mainLight + (offsetZ * (adjustmentLight - mainLight) shr tileScale))
                            }

                            if (mainColor != -1) {
                                var lightness = lightStrength * (mainColor and 0x7f) shr 7
                                if (lightness < 2) {
                                    lightness = 2
                                } else if (lightness > 126) {
                                    lightness = 126
                                }

                                directColor = ColorUtil.hslToRgb!![mainColor and 0xff80 or lightness]
                                if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) == 0) {
                                    sunDotProduct = activeToolkit.sunDirection[0] * lightX + activeToolkit.sunDirection[1] * lightY + activeToolkit.sunDirection[2] * lightZ
                                    sunDotProduct = activeToolkit.sunBrightness + sunDotProduct * (if (sunDotProduct > 0.0f) activeToolkit.sunColorIntensity else activeToolkit.antiSunColorIntensity)
                                }
                            }

                            var blendedTile: Node<*>? = null
                            if ((sceneX and (quarterTileUnits - 1)) == 0 && (sceneY and (quarterTileUnits - 1)) == 0) {
                                blendedTile = hashTable.get(uid)
                            }

                            var materialVertexIndex: Int
                            var colorRgb: Int
                            if (blendedTile == null) {
                                if (blendColor != mainColor) {
                                    var lightness = lightStrength * (blendColor and 0x7f) shr 7
                                    if (lightness < 2) {
                                        lightness = 2
                                    } else if (lightness > 126) {
                                        lightness = 126
                                    }

                                    colorRgb = ColorUtil.hslToRgb!![blendColor and 0xff80 or lightness]
                                    if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) == 0) {
                                        val blendSunDotProduct = activeToolkit.sunDirection[0] * lightX + activeToolkit.sunDirection[1] * lightY + activeToolkit.sunDirection[2] * lightZ
                                        val lightStrengthMultiplier = activeToolkit.sunBrightness + sunDotProduct * (if (sunDotProduct > 0.0f) activeToolkit.sunColorIntensity else activeToolkit.antiSunColorIntensity)
                                        var red = colorRgb shr 16 and 0xff
                                        var green = colorRgb shr 8 and 0xff
                                        var blue = colorRgb and 0xff
                                        red = (red * lightStrengthMultiplier).toInt()
                                        if (red < 0) {
                                            red = 0
                                        } else if (red > 255) {
                                            red = 255
                                        }
                                        green = (green * lightStrengthMultiplier).toInt()
                                        if (green < 0) {
                                            green = 0
                                        } else if (green > 255) {
                                            green = 255
                                        }
                                        blue = (blue * lightStrengthMultiplier).toInt()
                                        if (blue < 0) {
                                            blue = 0
                                        } else if (blue > 255) {
                                            blue = 255
                                        }
                                        colorRgb = red shl 16 or (green shl 8) or blue
                                    }
                                } else {
                                    colorRgb = directColor
                                }
                                if (activeToolkit.bigEndianess) {
                                    nativeStream.putFloatBigEndian(sceneX.toFloat())
                                    nativeStream.putFloatBigEndian((getAverageHeight(sceneX, sceneY) + floorHeightOffset).toFloat())
                                    nativeStream.putFloatBigEndian(sceneY.toFloat())
                                    nativeStream.putByte(colorRgb shr 16)
                                    nativeStream.putByte(colorRgb shr 8)
                                    nativeStream.putByte(colorRgb)
                                    nativeStream.putByte(-1)
                                    nativeStream.putFloatBigEndian(sceneX.toFloat())
                                    nativeStream.putFloatBigEndian(sceneY.toFloat())
                                    if (lightOffsetsB != null) {
                                        nativeStream.putFloatBigEndian(if (lightOffsetsForTile != null) (lightOffsetsForTile[loop] - 1).toFloat() else 0.0f)
                                    }
                                    if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                                        nativeStream.putFloatBigEndian(lightX)
                                        nativeStream.putFloatBigEndian(lightY)
                                        nativeStream.putFloatBigEndian(lightZ)
                                    }
                                } else {
                                    nativeStream.putFloatLittleEndian(sceneX.toFloat())
                                    nativeStream.putFloatLittleEndian((getAverageHeight(sceneX, sceneY) + floorHeightOffset).toFloat())
                                    nativeStream.putFloatLittleEndian(sceneY.toFloat())
                                    nativeStream.putByte(colorRgb shr 16)
                                    nativeStream.putByte(colorRgb shr 8)
                                    nativeStream.putByte(colorRgb)
                                    nativeStream.putByte(-1)
                                    nativeStream.putFloatLittleEndian(sceneX.toFloat())
                                    nativeStream.putFloatLittleEndian(sceneY.toFloat())
                                    if (lightOffsetsB != null) {
                                        nativeStream.putFloatLittleEndian(if (lightOffsetsForTile != null) (lightOffsetsForTile[loop] - 1).toFloat() else 0.0f)
                                    }
                                    if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                                        nativeStream.putFloatLittleEndian(lightX)
                                        nativeStream.putFloatLittleEndian(lightY)
                                        nativeStream.putFloatLittleEndian(lightZ)
                                    }
                                }
                                materialVertexIndex = totalMaterializedTiles++
                                vertexMaterialIndices[loop] = materialVertexIndex.toShort()
                                if (mainColor != -1) {
                                    materialsByVertice[materialVertexIndex] = vertexMaterials[loop]
                                }
                                hashTable.put(ShortNode(vertexMaterialIndices[loop]), uid)
                            } else {
                                vertexMaterialIndices[loop] = (blendedTile as ShortNode).value
                                materialVertexIndex = vertexMaterialIndices[loop].toInt() and 0xffff
                                if (mainColor != -1 && vertexMaterials[loop].pointer < materialsByVertice[materialVertexIndex]!!.pointer) {
                                    materialsByVertice[materialVertexIndex] = vertexMaterials[loop]
                                }
                            }

                            for (materialIndex in 0 until uniqueMaterialCount) {
                                someMaterialArray[materialIndex]!!.writeVertexColor(materialVertexIndex, directColor, lightStrength, sunDotProduct)
                            }

                            amountOfTexturedFaces++
                        }
                    }
                }
            }

            for (vertexIndex in 0 until totalMaterializedTiles) {
                val groundMaterial = materialsByVertice[vertexIndex]
                if (groundMaterial != null) {
                    groundMaterial.appendToNativeStream(vertexIndex)
                }
            }

            for (x in 0 until width) {
                for (y in 0 until length) {
                    val vertices = materialIndices[x + y * width]
                    if (vertices != null) {
                        var faceCount = 0
                        var verticeCount = 0
                        while (verticeCount < vertices.size) {
                            val faceA = vertices[verticeCount++].toInt() and 0xffff
                            val faceB = vertices[verticeCount++].toInt() and 0xffff
                            val faceC = vertices[verticeCount++].toInt() and 0xffff
                            val texturedFaceA = materialsByVertice[faceA]
                            val texturedFaceB = materialsByVertice[faceB]
                            val texturedFaceC = materialsByVertice[faceC]
                            var texturedFaceD: GroundMaterialGL? = null
                            if (texturedFaceA != null) {
                                texturedFaceA.generateFunctionMask(x, y, faceCount)
                                texturedFaceD = texturedFaceA
                            }
                            if (texturedFaceB != null) {
                                texturedFaceB.generateFunctionMask(x, y, faceCount)
                                if (texturedFaceD == null || texturedFaceB.pointer < texturedFaceD.pointer) {
                                    texturedFaceD = texturedFaceB
                                }
                            }
                            if (texturedFaceC != null) {
                                texturedFaceC.generateFunctionMask(x, y, faceCount)
                                if (texturedFaceD == null || texturedFaceC.pointer < texturedFaceD.pointer) {
                                    texturedFaceD = texturedFaceC
                                }
                            }
                            if (texturedFaceD != null) {
                                if (texturedFaceA != null) {
                                    texturedFaceD.appendToNativeStream(faceA)
                                }
                                if (texturedFaceB != null) {
                                    texturedFaceD.appendToNativeStream(faceB)
                                }
                                if (texturedFaceC != null) {
                                    texturedFaceD.appendToNativeStream(faceC)
                                }
                                texturedFaceD.generateFunctionMask(x, y, faceCount)
                            }
                            faceCount++
                        }
                    }
                }
            }
            nativeStream.flush()

            // For modern rendering: copy vertex data to a ByteBuffer we own (no legacy VBO
            // dependency). This is a pure CPU-side capture — it must run whenever the
            // active toolkit is ModernGLToolkit, regardless of whether the modern
            // renderer has lazily initialized yet (init happens after the first render;
            // the map can be built before that). Without this snapshot the modern ground
            // path cannot draw AT ALL (no legacy fallback exists under toolkit 6), so
            // failures here must be loud.
            val dataSize = nativeStream.written()
            if (activeToolkit is ModernGLToolkit) {
                try {
                    val nativeAddr = nativeHeapBuffer.getBufferAddress()
                    if (nativeAddr != 0L && dataSize > 0) {
                        // Free old buffer if exists
                        modernVertexData?.let { MemoryUtil.memFree(it) }
                        // Create new buffer and copy data
                        modernVertexData = MemoryUtil.memAlloc(dataSize)
                        MemoryUtil.memCopy(nativeAddr, MemoryUtil.memAddress(modernVertexData!!), dataSize.toLong())
                        modernVertexStride = bufferEntrySize
                        modernVertexCount = totalMaterializedTiles
                        modernVertexRevision++
                    } else {
                        println("[GroundGL] MODERN SNAPSHOT MISSING: nativeAddr=$nativeAddr dataSize=$dataSize — terrain for this ground will not render under toolkit 6")
                    }
                } catch (e: Exception) {
                    modernVertexData = null
                    println("[GroundGL] MODERN SNAPSHOT FAILED: ${e.message} — terrain for this ground will not render under toolkit 6")
                    e.printStackTrace()
                }
            }

            dataArrayBuffer = activeToolkit.createDataArrayBuffer(bufferEntrySize, nativeHeapBuffer, dataSize)
            if (dataArrayBuffer is GlVboArrayBuffer) {
                nativeHeapBuffer.deallocateBuffer()
            }
            vertexBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_FLOAT, 3, 0)
            colorBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_UNSIGNED_BYTE, 4, 12)
            val pointerOffset: Int
            if (lightOffsetsB != null) {
                textureBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_FLOAT, 3, 16)
                pointerOffset = 28
            } else {
                textureBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_FLOAT, 2, 16)
                pointerOffset = 24
            }

            if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                normalsBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_FLOAT, 3, pointerOffset)
            }

            val pointers = LongArray(goundMaterialNodes!!.size)

            for (nodeIndex in goundMaterialNodes!!.indices) {
                val materialNode = goundMaterialNodes!![nodeIndex] as GroundMaterialGL
                pointers[nodeIndex] = materialNode.pointer
                materialNode.createDataArrayBuffer(totalMaterializedTiles)
            }

            ArrayUtils.quicksort(pointers, goundMaterialNodes!!)
            if (shadowMapper != null) {
                shadowMapper!!.initializeShadowTiles()
            }
        } else {
            shadowMapper = null
        }

        if ((floorFunctionMask and GroundFunctionMask.HIDE) == 0) {
            tileOffsetsZ = null
            tileOffsetsX = null
            tileColors = null
        }

        lightOffsetsB = null
        tileBlendColors = null
        lightOffsetsA = null
        materials = null
        shadowing = null
        materialTable = null
        lightingZ = null
        lightingY = null
        lightingX = null
    }

    override fun renderMinimapFloor(
        centerOffsetX: Int,
        centerOffsetY: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        materialized: Array<BooleanArray>
    ) {
        val activeDisplay = activeToolkit.getDisplay()
        if (currentColorCount > 0 && activeToolkit is ModernGLToolkit) {
            // Modern toolkit: never the fixed-function path below. If the modern
            // renderer cannot draw yet, the slice is skipped (and the miss is loud in
            // the diag), never rendered through legacy GL.
            if (!activeToolkit.renderMinimapFloorModern(this, centerOffsetX, centerOffsetY, startX, startY, endX, endY, materialized)) {
                activeToolkit.countLegacySkip("GroundGL.renderMinimapFloor")
            }
            return
        }
        if (currentColorCount > 0 && activeDisplay != null) {
            activeToolkit.setOrthoMatrixMode()
            activeToolkit.toggleFog(false)
            activeToolkit.setLightingEnabled(false)
            activeToolkit.toggleZbuffer(false)
            activeToolkit.setDepthMask(false)
            activeToolkit.setBlendMode(0)
            activeToolkit.renderTexture(-2)
            activeToolkit.setActiveTexture(null)
            GROUND_MATRIX[0] = 1024 / (128.0f * tileUnits * activeDisplay.getWidth())
            GROUND_MATRIX[1] = 0.0f
            GROUND_MATRIX[2] = 0.0f
            GROUND_MATRIX[3] = 0.0f
            GROUND_MATRIX[4] = 0.0f
            GROUND_MATRIX[5] = 1024 / (128.0f * tileUnits * activeDisplay.getHeight())
            GROUND_MATRIX[6] = 0.0f
            GROUND_MATRIX[7] = 0.0f
            GROUND_MATRIX[8] = 0.0f
            GROUND_MATRIX[9] = 0.0f
            GROUND_MATRIX[10] = 0.0f
            GROUND_MATRIX[11] = 0.0f
            GROUND_MATRIX[12] = -1.0f - ((1024 * startX) / 128.0f - (centerOffsetX * 2)) / activeDisplay.getWidth()
            GROUND_MATRIX[13] = 1.0f - ((centerOffsetY * 2) + (1024 * endY) / 128.0f) / activeDisplay.getHeight()
            GROUND_MATRIX[14] = 0.0f
            GROUND_MATRIX[15] = 1.0f
            OpenGL.glMatrixMode(5889)
            OpenGL.glLoadMatrixf(GROUND_MATRIX, 0)
            GROUND_MATRIX[0] = 1.0f
            GROUND_MATRIX[1] = 0.0f
            GROUND_MATRIX[2] = 0.0f
            GROUND_MATRIX[3] = 0.0f
            GROUND_MATRIX[4] = 0.0f
            GROUND_MATRIX[5] = 0.0f
            GROUND_MATRIX[6] = 1.0f
            GROUND_MATRIX[7] = 0.0f
            GROUND_MATRIX[8] = 0.0f
            GROUND_MATRIX[9] = 1.0f
            GROUND_MATRIX[10] = 0.0f
            GROUND_MATRIX[11] = 0.0f
            GROUND_MATRIX[12] = 0.0f
            GROUND_MATRIX[13] = 0.0f
            GROUND_MATRIX[14] = 0.0f
            GROUND_MATRIX[15] = 1.0f
            OpenGL.glMatrixMode(5888)
            OpenGL.glLoadMatrixf(GROUND_MATRIX, 0)
            if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                activeToolkit.setLightingEnabled(true)
                activeToolkit.setLightingSourceParams()
            } else {
                activeToolkit.setLightingEnabled(false)
            }

            activeToolkit.toggleDrawingClientstates(vertexBuffer, normalsBuffer, colorBuffer, textureBuffer)
            if (activeToolkit.textureBuffer.buffer.size < amountOfTexturedFaces * 2) {
                activeToolkit.textureBuffer = GlBitBuffer(amountOfTexturedFaces * 2)
            } else {
                activeToolkit.textureBuffer.currentPosition = 0
            }

            val textureBuffer = activeToolkit.textureBuffer
            var texturesLoaded = 0
            if (activeToolkit.bigEndianess) {
                for (y in startY until endY) {
                    var mi = y * width + startX
                    for (x in startX until endX) {
                        if (materialized[x - startX][y - startY]) {
                            val materials = this.materialIndices[mi]
                            if (materials != null) {
                                for (index in materials.indices) {
                                    textureBuffer.writeShort(materials[index].toInt() and 0xffff)
                                    texturesLoaded++
                                }
                            }
                        }
                        mi++
                    }
                }
            } else {
                for (y in startY until endY) {
                    var mi = y * width + startX
                    for (x in startX until endX) {
                        if (materialized[x - startX][y - startY]) {
                            val materials = this.materialIndices[mi]
                            if (materials != null) {
                                for (index in materials.indices) {
                                    textureBuffer.writeShortLittle(materials[index].toInt() and 0xffff)
                                    texturesLoaded++
                                }
                            }
                        }
                        mi++
                    }
                }
            }
            if (texturesLoaded > 0) {
                val texture2d = GlElementsArrayBuffer(activeToolkit, textureBuffer.buffer, textureBuffer.currentPosition)
                activeToolkit.drawElements(texture2d, 0, texturesLoaded)
            }
        }
    }

    override fun addShadow(shadow: Shadow?, scaledX: Int, averageHeight: Int, scaledY: Int) {
        if (shadowMapper != null && shadow != null) {
            val shadowX = scaledX - (averageHeight * activeToolkit.shadowOffsetX shr 8) shr activeToolkit.shadowScale
            val shadowY = scaledY - (averageHeight * activeToolkit.shadowOffsetZ shr 8) shr activeToolkit.shadowScale
            shadowMapper!!.addShadow(shadow, shadowX, shadowY)
        }
    }

    override fun hasRenderingShadows(shadow: Shadow?, x: Int, planesHeightDelta: Int, y: Int): Boolean {
        if (shadowMapper != null && shadow != null) {
            val shadowX = x - (planesHeightDelta * activeToolkit.shadowOffsetX shr 8) shr activeToolkit.shadowScale
            val shadowY = y - (planesHeightDelta * activeToolkit.shadowOffsetZ shr 8) shr activeToolkit.shadowScale
            return shadowMapper!!.applyShadow(shadow, shadowX, shadowY)
        }
        return false
    }

    override fun removeShadow(shadow: Shadow?, x: Int, groundOffset: Int, y: Int) {
        if (shadowMapper != null && shadow != null) {
            val shadowX = x - (groundOffset * activeToolkit.shadowOffsetX shr 8) shr activeToolkit.shadowScale
            val shadowY = y - (groundOffset * activeToolkit.shadowOffsetZ shr 8) shr activeToolkit.shadowScale
            shadowMapper!!.removeShadow(shadow, shadowX, shadowY)
        }
    }

    override fun addLight(lightNode: PointLight, lightDisplacement: IntArray) {
        activeLights.insertTail(LightGL(activeToolkit, this, lightNode, lightDisplacement))
    }

    override fun addBlendedTile(
        tileX: Int,
        tileY: Int,
        offsetsX: IntArray?,
        lightGroundOffsetsA: IntArray?,
        offsetsZ: IntArray?,
        lightGroundOffsetsB: IntArray?,
        mainColors: IntArray,
        blendColors: IntArray?,
        waterMaterials: IntArray,
        waterScaling: IntArray,
        waterTile: WaterTile?,
        hasShadows: Boolean
    ) {
        val materialProvider = activeToolkit.materialProvider
        if (lightGroundOffsetsB != null && lightOffsetsB == null) {
            lightOffsetsB = Array(width) { Array(length) { null } }
        }
        if (lightGroundOffsetsA != null && lightOffsetsA == null) {
            lightOffsetsA = Array(width) { Array(length) { null } }
        }
        tileOffsetsX!![tileX][tileY] = offsetsX
        tileOffsetsZ!![tileX][tileY] = offsetsZ
        tileColors!![tileX][tileY] = mainColors
        tileBlendColors!![tileX][tileY] = blendColors
        if (lightGroundOffsetsB != null && lightOffsetsB == null) {
            // Already handled above
        }
        if (lightGroundOffsetsA != null && lightOffsetsA == null) {
            // Already handled below
        }
        if (lightOffsetsB != null) {
            lightOffsetsB!![tileX][tileY] = lightGroundOffsetsB
        }
        if (lightOffsetsA != null) {
            lightOffsetsA!![tileX][tileY] = lightGroundOffsetsA
        }
        val groundMaterial = Array<GroundMaterialGL?>(mainColors.size) { null }
        materials!![tileX][tileY] = @Suppress("UNCHECKED_CAST") (groundMaterial as Array<GroundMaterialGL>)
        for (index in mainColors.indices) {
            var materialId = waterMaterials[index]
            var scaling = waterScaling[index]
            if ((waterFunctionMask and 0x20) != 0 && materialId != -1 && activeToolkit.materialProvider?.getMaterial(materialId)?.detailsOnly == true) {
                scaling = 128
                materialId = -1
            }
            if (waterTile != null) {
                val uid = (waterTile.intensity.toLong() shl 48) or ((waterTile.scale.toLong() shl 42)) or ((waterTile.color.toLong() shl 28)) or (((scaling.toLong()) shl 14)) or (materialId.toLong())
                @Suppress("UNCHECKED_CAST")
                var activeGroundMaterial: Node<*>? = materialTable!!.get(uid) as Node<*>?
                while (activeGroundMaterial != null) {
                    val openglGroundMaterial = activeGroundMaterial as GroundMaterialGL
                    if (materialId == openglGroundMaterial.materialId && openglGroundMaterial.textureScale.toInt() == scaling && openglGroundMaterial.waterTile.equals(waterTile)) {
                        break
                    }
                    @Suppress("UNCHECKED_CAST")
                    activeGroundMaterial = materialTable!!.nextInBucket() as Node<*>?
                }
                if (activeGroundMaterial == null) {
                    groundMaterial[index] = GroundMaterialGL(this, materialId, scaling, waterTile)
                    materialTable!!.put(groundMaterial[index], uid)
                } else {
                    groundMaterial[index] = activeGroundMaterial as GroundMaterialGL
                }
            }
        }
        if (hasShadows) {
            shadowFunctionMasks[tileX][tileY] = (shadowFunctionMasks[tileX][tileY].toInt() or GroundFunctionMask.CONTAINS_SHADOW).toByte()
        }
        if (mainColors.size > maxColorCount) {
            maxColorCount = mainColors.size
        }
        currentColorCount += mainColors.size
    }

    override fun renderFloor(cameraGridX: Int, cameraGridY: Int, visibleTileRange: Int, tilesOnScreen: Array<BooleanArray>, belowLowestPlane: Boolean) {
        renderShadows(cameraGridX, cameraGridY, visibleTileRange, tilesOnScreen, belowLowestPlane)
    }

    /** [renderShadows] is a no-op without material nodes, so the plane draws nothing. */
    override fun hasRenderableFloor(): Boolean = goundMaterialNodes != null

    override fun createShadowAt(tileX: Int, tileY: Int, shadow: Shadow?): Shadow? {
        if ((shadowFunctionMasks[tileX][tileY].toInt() and GroundFunctionMask.CONTAINS_SHADOW) == 0) {
            // Tiles without shadow-casting floor geometry produce no shadow. Returning the
            // caller's scratch shadow here re-stamped the previous tile's shadow onto every
            // later tile of the plane.
            return null
        }
        val dimension = tileUnits shr activeToolkit.shadowScale
        val openglShadow = shadow as? ShadowGL
        val generatedOpenglShadow: ShadowGL
        if (openglShadow != null && openglShadow.fitsDimension(dimension, dimension)) {
            generatedOpenglShadow = openglShadow
            openglShadow.reset()
        } else {
            generatedOpenglShadow = ShadowGL(dimension, dimension)
        }

        generatedOpenglShadow.setDimension(0, 0, dimension, dimension)
        projectShadow(generatedOpenglShadow, tileX, tileY)
        return generatedOpenglShadow
    }
}
