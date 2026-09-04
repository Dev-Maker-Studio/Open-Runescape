package com.jagex.game.runetek5.toolkit.opengl

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer
import com.jagex.game.runetek5.scene.ground.GroundFunctionMask
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboArrayBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer

import jaclib.memory.Stream
import jaclib.memory.heap.NativeHeapBuffer
import jaggl.OpenGL

import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.buffer.ArrayBufferGL

class GroundMaterialGL(
    ground: GroundGL,
    materialId: Int,
    scaling: Int,
    waterTile: WaterTile
) : Node<Any?>() {
    @JvmField var arrayBufferPointer: OpenGlArrayDrawBuffer? = null
    @JvmField var nativeHeapBuffer: NativeHeapBuffer? = null
    @JvmField var nativeStream: Stream? = null
    @JvmField val ground: GroundGL
    @JvmField val toolkit: OpenGlToolkit
    @JvmField val materialId: Int
    @JvmField val textureScale: Float
    @JvmField val waterTile: WaterTile
    @JvmField val groundFunctionMask: IntArray
    @JvmField val texture2d: GlElementsArrayBuffer

    /** Modern path: visible-set stamp the cached index list below was built for. */
    @JvmField var modernIndexStamp: Long = 0L
    @JvmField var modernIndexCount: Int = -1
    /** Modern path: resident GL element buffer holding the cached index list. */
    @JvmField var modernIboId: Int = 0
    @JvmField var modernIboCapacity: Int = 0
    /** Tile indices where this material owns at least one triangle (static per material). */
    @JvmField var modernTiles: IntArray? = null
    @JvmField var modernTilesMaskSize: Int = -1
    /** Per 8x8 chunk index lists of this material's triangles (see ModernGLToolkit ground draw). */
    @JvmField var modernChunkIdx: Array<ShortArray?>? = null
    @JvmField var modernChunkRevision: Int = -1

    init {
        this.ground = ground
        toolkit = ground.activeToolkit
        this.materialId = materialId
        textureScale = scaling.toFloat()
        this.waterTile = waterTile
        groundFunctionMask = IntArray(this.ground.width * this.ground.length)
        texture2d = GlElementsArrayBuffer(toolkit, null, 1)
    }

    private val GROUND_COLOR_DEBUG = System.getProperty("render.groundcolor")?.let { it.equals("true", true) || it.equals("raw", true) } == true
    private val RAW_COLOR_DEBUG = System.getProperty("render.groundcolor")?.equals("raw", true) == true
    private val GROUND_COMBINE_OVERRIDE = System.getProperty("render.groundcombine")?.toIntOrNull() ?: -1
    private val GROUND_PROBE = System.getProperty("render.sunprobe") != null
    private var groundProbeTick = 0

    fun appendToNativeStream(vertexIndex: Int) {
        nativeStream!!.setOffset(vertexIndex * 4 + 3)
        nativeStream!!.putByte(-1)
    }

    fun generateFunctionMask(x: Int, y: Int, faceCount: Int) {
        groundFunctionMask[y * ground.width + x] = groundFunctionMask[y * ground.width + x] or (1 shl faceCount)
    }

    fun writeVertexColor(vertexIndex: Int, color: Int, lightness: Int, colorMultiplier: Float) {
        var color = color
        if (materialId != -1) {
            val material = toolkit.materialProvider!!.getMaterial(materialId)
            val contrast = material.alpha.toInt() and 0xff
            if (contrast != 0 && material.effectId.toInt() != 4) {
                val lightColor: Int
                if (lightness < 0) {
                    lightColor = 0
                } else if (lightness > 127) {
                    lightColor = 16777215
                } else {
                    lightColor = lightness * 131586
                }

                val blendContrast = 256 - contrast
                color = ((lightColor and 0xff00ff) * contrast + blendContrast * (color and 0xff00ff) and -16711936) + (blendContrast * (color and 0xff00) + contrast * (lightColor and 0xff00) and 0xff0000) shr 8
            }

            var brightness = material.brightness.toInt() and 0xff
            if (brightness != 0) {
                brightness += 256
                var red = ((color and 0xff0000) shr 16) * brightness
                if (red > 65535) {
                    red = 65535
                }
                var green = ((color and 0xff00) shr 8) * brightness
                if (green > 65535) {
                    green = 65535
                }
                var blue = brightness * (color and 0xff)
                if (blue > 65535) {
                    blue = 65535
                }
                color = (blue shr 8) + (green and 0xff00) + (red shl 8 and 0xff0000)
            }
        }

        if (colorMultiplier != 1.0f) {
            var red = color shr 16 and 0xff
            var green = color shr 8 and 0xff
            var blue = color and 0xff
            red = (red * colorMultiplier).toInt()
            if (red < 0) {
                red = 0
            } else if (red > 255) {
                red = 255
            }

            green = (green * colorMultiplier).toInt()
            if (green < 0) {
                green = 0
            } else if (green > 255) {
                green = 255
            }

            blue = (blue * colorMultiplier).toInt()
            if (blue < 0) {
                blue = 0
            } else if (blue > 255) {
                blue = 255
            }
            color = red shl 16 or (green shl 8) or blue
        }
        nativeStream!!.setOffset(vertexIndex * 4)
        nativeStream!!.putByte(color shr 16)
        nativeStream!!.putByte(color shr 8)
        nativeStream!!.putByte(color)
    }

    fun drawMaterialTriangles(tileIndices: IntArray, tileCount: Int) {
        // Under the modern toolkit the modern path is the ONLY path — no legacy
        // fixed-function fallback. If the modern pipeline cannot draw (renderer not
        // initialized, no vertex snapshot), the material draws NOTHING and the miss
        // is counted in the [ModernDiag] gate counters (ground=drawn/noPipe/noSnap).
        if (toolkit is ModernGLToolkit) {
            renderGroundMaterialModern(tileIndices, tileCount)
            return
        }

        // Legacy rendering path (non-modern toolkits only)
        var addedTextures = 0
        val textureBuffer = toolkit.textureBuffer
        textureBuffer.currentPosition = 0
        if (toolkit.bigEndianess) {
            for (i in 0 until tileCount) {
                val tileIndex = tileIndices[i]
                val materialsByTriangle = ground.materialIndices[tileIndex]
                val functionMask = groundFunctionMask[tileIndex]
                if (functionMask != 0 && materialsByTriangle != null) {
                    var loops = 0
                    var verticeCount = 0
                    while (verticeCount < materialsByTriangle.size) {
                        if ((functionMask and (1 shl loops++)) != 0) {
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                            textureBuffer.writeShort(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                        } else {
                            verticeCount += 3
                        }
                    }
                }
            }
        } else {
            for (i in 0 until tileCount) {
                val tileIndex = tileIndices[i]
                val materialsByTriangle = ground.materialIndices[tileIndex]
                val functionMask = groundFunctionMask[tileIndex]
                if (functionMask != 0 && materialsByTriangle != null) {
                    var loops = 0
                    var verticeCount = 0

                    while (verticeCount < materialsByTriangle.size) {
                        if ((functionMask and (1 shl loops++)) != 0) {
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                            textureBuffer.writeShortLittle(materialsByTriangle[verticeCount++].toInt() and 0xffff)
                            addedTextures++
                        } else {
                            verticeCount += 3
                        }
                    }
                }
            }
        }
        if (addedTextures > 0) {
            if (GROUND_PROBE && groundProbeTick++ % 1200 < 40) {
                val sb = StringBuilder()
                try {
                    org.lwjgl.opengl.GL.createCapabilities()
                    val handle = toolkit.textureCache?.get(materialId)?.activeHandle ?: 0
                    sb.append(" tex=$handle")
                    if (handle > 0) {
                        val tp = IntArray(1)
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D, tp)
                        val prev = tp[0]
                        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, handle)
                        for ((name, pname) in listOf(
                            "minF" to org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, "magF" to org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER,
                            "maxLvl" to org.lwjgl.opengl.GL12.GL_TEXTURE_MAX_LEVEL, "baseLvl" to org.lwjgl.opengl.GL12.GL_TEXTURE_BASE_LEVEL
                        )) { org.lwjgl.opengl.GL11.glGetTexParameteriv(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, pname, tp); sb.append(" $name=${tp[0]}") }
                        val fb = FloatArray(1)
                        org.lwjgl.opengl.GL11.glGetTexParameterfv(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS, fb); sb.append(" lodBias=${fb[0]}")
                        org.lwjgl.opengl.GL11.glGetTexEnvfv(org.lwjgl.opengl.GL14.GL_TEXTURE_FILTER_CONTROL, org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS, fb); sb.append(" unitLodBias=${fb[0]}")
                        org.lwjgl.opengl.GL11.glGetTexLevelParameteriv(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL11.GL_TEXTURE_INTERNAL_FORMAT, tp); sb.append(" fmt=${tp[0]}")
                        for (lvl in 0 until 4) {
                            org.lwjgl.opengl.GL11.glGetTexLevelParameteriv(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, lvl, org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH, tp)
                            val w = tp[0]
                            if (w <= 0) { sb.append(" L$lvl=none"); continue }
                            val buf = java.nio.ByteBuffer.allocateDirect(w * w * 4).order(java.nio.ByteOrder.nativeOrder())
                            org.lwjgl.opengl.GL11.glGetTexImage(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, lvl, org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, buf)
                            var r = 0L; var g = 0L; var b = 0L; var a = 0L
                            for (k in 0 until w * w) { r += buf.get(k * 4).toInt() and 0xff; g += buf.get(k * 4 + 1).toInt() and 0xff; b += buf.get(k * 4 + 2).toInt() and 0xff; a += buf.get(k * 4 + 3).toInt() and 0xff }
                            val n = (w * w).toLong()
                            sb.append(" L$lvl=${w}px(${r / n},${g / n},${b / n},${a / n})")
                        }
                        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, prev)
                    }
                    // Texture-environment state per unit: what the combiner actually multiplies.
                    val ip = IntArray(1)
                    val fp = FloatArray(4)
                    org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE, ip)
                    val activeUnit = ip[0]
                    for (u in 0 until 3) {
                        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + u)
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, ip); val en2d = ip[0]
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D, ip); val bound = ip[0]
                        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE, ip); val enRect = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnviv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL11.GL_TEXTURE_ENV_MODE, ip); val mode = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnviv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_COMBINE_RGB, ip); val crgb = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnviv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_COMBINE_ALPHA, ip); val calpha = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnviv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_SOURCE0_RGB, ip); val s0 = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnviv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_SOURCE1_RGB, ip); val s1 = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnviv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_SOURCE2_RGB, ip); val s2 = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnviv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_OPERAND0_RGB, ip); val o0 = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnviv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_OPERAND1_RGB, ip); val o1 = ip[0]
                        org.lwjgl.opengl.GL11.glGetTexEnvfv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL13.GL_RGB_SCALE, fp); val scale = fp[0]
                        org.lwjgl.opengl.GL11.glGetTexEnvfv(org.lwjgl.opengl.GL11.GL_TEXTURE_ENV, org.lwjgl.opengl.GL11.GL_TEXTURE_ENV_COLOR, fp)
                        sb.append(" U$u[2d=$en2d rect=$enRect bound=$bound mode=${Integer.toHexString(mode)} rgb=${Integer.toHexString(crgb)} a=${Integer.toHexString(calpha)} src=${Integer.toHexString(s0)},${Integer.toHexString(s1)},${Integer.toHexString(s2)} op=${Integer.toHexString(o0)},${Integer.toHexString(o1)} scale=$scale envCol=${fp.toList()}]")
                    }
                    org.lwjgl.opengl.GL13.glActiveTexture(activeUnit)
                    org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_BLEND, ip); sb.append(" blend=${ip[0]}")
                    org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_BLEND_SRC, ip); sb.append(" bsrc=${Integer.toHexString(ip[0])}")
                    org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_BLEND_DST, ip); sb.append(" bdst=${Integer.toHexString(ip[0])}")
                    org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_FOG, ip); sb.append(" fog=${ip[0]}")
                    org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_FOG_START, fp); sb.append(" fogStart=${fp[0]}")
                    org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_FOG_END, fp); sb.append(" fogEnd=${fp[0]}")
                    org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_FOG_MODE, ip); sb.append(" fogMode=${Integer.toHexString(ip[0])}")
                    org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_FOG_COLOR, fp); sb.append(" fogCol=${fp.toList()}")
                    org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_CURRENT_COLOR, fp); sb.append(" curCol=${fp.toList()}")
                    org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_LIGHTING, ip); sb.append(" lighting=${ip[0]}")
                } catch (e: Throwable) { sb.append(" probeFail=$e") }
                System.err.println("[GroundDraw] legacy mat=$materialId indices=$addedTextures tiles=$tileCount scale=$textureScale$sb")
            }
            if (GROUND_PROBE && (materialId == 505 || materialId == 301) && groundProbeTick % 300 < 2) for (v0 in intArrayOf(1, 54, 838, 1778, (textureBuffer.buffer[0].toInt() and 0xff) or ((textureBuffer.buffer[1].toInt() and 0xff) shl 8))) {
                try {
                    org.lwjgl.opengl.GL.createCapabilities()
                    val col = ByteArray(4)
                    val cbuf = arrayBufferPointer?.buffer
                    val cid = cbuf?.getId() ?: 0
                    if (cid != 0) {
                        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, cid)
                        val bb = java.nio.ByteBuffer.allocateDirect(4).order(java.nio.ByteOrder.nativeOrder())
                        org.lwjgl.opengl.GL15.glGetBufferSubData(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, v0.toLong() * 4, bb)
                        bb.get(col)
                    } else if (cbuf != null) {
                        val addr = cbuf.getBufferAddress()
                        for (k in 0 until 4) col[k] = org.lwjgl.system.MemoryUtil.memGetByte(addr + v0 * 4 + k)
                    }
                    val vb = ground.vertexBuffer?.buffer
                    val vid = vb?.getId() ?: 0
                    val stride = vb?.getStride() ?: 0
                    val floats = FloatArray(8)
                    if (vid != 0 && stride > 0) {
                        org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, vid)
                        val bb = java.nio.ByteBuffer.allocateDirect(stride).order(java.nio.ByteOrder.nativeOrder())
                        org.lwjgl.opengl.GL15.glGetBufferSubData(org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER, v0.toLong() * stride, bb)
                        floats[0] = bb.getFloat(0); floats[1] = bb.getFloat(4); floats[2] = bb.getFloat(8)
                        val nOff = ground.normalsBuffer?.offset?.toInt() ?: 24
                        floats[3] = bb.getFloat(nOff); floats[4] = bb.getFloat(nOff + 4); floats[5] = bb.getFloat(nOff + 8)
                    }
                    System.err.println(
                        "[GroundProbe] legacy mat=$materialId combine=${toolkit.materialProvider!!.getMaterial(materialId).combineMode} " +
                            "hd=${(ground.waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0} v0=$v0 " +
                            "colour=(${col[0].toInt() and 0xff},${col[1].toInt() and 0xff},${col[2].toInt() and 0xff},${col[3].toInt() and 0xff}) " +
                            "pos=(${floats[0]},${floats[1]},${floats[2]}) normal=(${floats[3]},${floats[4]},${floats[5]}) stride=$stride sunB=${toolkit.sunBrightness}"
                    )
                } catch (e: Throwable) {
                    System.err.println("[GroundProbe] legacy failed: $e")
                }
            }
            texture2d.putElements(textureBuffer.buffer, textureBuffer.currentPosition)
            toolkit.toggleDrawingClientstates(ground.vertexBuffer, ground.normalsBuffer, arrayBufferPointer, ground.textureBuffer)
            toolkit.renderTexture(materialId, (ground.waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0, (ground.waterFunctionMask and 0x8) != 0)
            // -Drender.groundcolor=true: draw the lit primary colour alone (no texture),
            // the legacy twin of the modern ground shader's colour-only debug mode.
            if (GROUND_COLOR_DEBUG) {
                toolkit.renderTexture(-1, (ground.waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0, (ground.waterFunctionMask and 0x8) != 0)
                if (RAW_COLOR_DEBUG) toolkit.setLightingEnabled(false)
            }
            // -Drender.groundcombine=<mode>: force one combine mode (A/B aid; twin of the modern flag).
            if (GROUND_COMBINE_OVERRIDE >= 0) toolkit.loadTextureCombineMode(GROUND_COMBINE_OVERRIDE)
            if (toolkit.stillWaterToggled) {
                toolkit.addWaterTile(Int.MAX_VALUE, waterTile)
            }

            OpenGL.glMatrixMode(GL_TEXTURE)
            OpenGL.glPushMatrix()
            OpenGL.glScalef(1.0f / textureScale, 1.0f / textureScale, 1.0f)
            OpenGL.glMatrixMode(GL_MODELVIEW)
            toolkit.toggleDrawingClientstates(ground.vertexBuffer, ground.normalsBuffer, arrayBufferPointer, ground.textureBuffer)
            toolkit.drawElements(texture2d, 0, addedTextures)
            OpenGL.glMatrixMode(GL_TEXTURE)
            OpenGL.glPopMatrix()
            OpenGL.glMatrixMode(GL_MODELVIEW)
        }

    }

    /**
     * Modern rendering path for ground materials using ModernGLToolkit.
     *
     * @return true when the modern pipeline handled the draw; false when the caller
     *         must fall back to the legacy fixed-function path.
     */
    private fun renderGroundMaterialModern(tileIndices: IntArray, tileCount: Int): Boolean {
        val modernToolkit = toolkit as ModernGLToolkit
        return modernToolkit.renderGroundMaterialModern(
            this,
            ground,
            tileIndices,
            tileCount,
            groundFunctionMask,
            materialId,
            textureScale,
            (ground.waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0
        )
    }

    fun createDataArrayBuffer(totalTiles: Int) {
        nativeStream!!.flush()
        val arrayBuffer = toolkit.createDataArrayBuffer(4, nativeHeapBuffer!!, totalTiles * 4)
        if (arrayBuffer is GlVboArrayBuffer) {
            nativeHeapBuffer!!.deallocateBuffer()
        }
        arrayBufferPointer = OpenGlArrayDrawBuffer(arrayBuffer, GL_UNSIGNED_BYTE, 4, 0)
        nativeHeapBuffer = null
        nativeStream = null
    }

    fun generateNativeBuffer(colorCount: Int) {
        nativeHeapBuffer = toolkit.nativeHeap!!.createBuffer(colorCount * 4, true)
        nativeStream = Stream(nativeHeapBuffer!!)
    }

}
