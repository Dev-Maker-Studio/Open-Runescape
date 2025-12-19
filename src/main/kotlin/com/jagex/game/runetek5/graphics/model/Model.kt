package com.jagex.game.runetek5.graphics.model

import com.jagex.game.runetek5.config.sequence.frameset.AnimBase
import com.jagex.game.runetek5.config.sequence.frameset.AnimFrame
import com.jagex.game.runetek5.config.sequence.frameset.FrameSet
import com.jagex.game.runetek5.entity.BoundingBoxPolygon
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.graphics.texture.TextureTransformationMatrix
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.utils.ArrayUtils
import kotlin.math.*

abstract class Model {

    @JvmField
    protected var isLocked: Boolean = false

    /**
     * *****************************************************************
     * 					(TODO:): GPU handling
     * *****************************************************************
     */

    abstract fun uploadToGpu()

    /**
     * *****************************************************************
     * 					(TODO:) Multi threading
     * *****************************************************************
     */

    abstract fun awaitLock()

    abstract fun unlock()

    /**
     * *****************************************************************
     * 					(TODO:): Drawing
     * *****************************************************************
     */

    abstract fun calculateProjection(camX: Int, camY: Int, modelMatrix: Matrix43, notRendered: Boolean, sizeShift: Int): Boolean

    abstract fun drawMatrixThreaded(projectionMatrix: Matrix43, boundingBox: BoundingBoxPolygon?, rasterizerFunctionMask: Int)

    /**
     * *****************************************************************
     * 					(TODO:) Adjustment
     * *****************************************************************
     */

    abstract fun translate(x: Int, y: Int, z: Int)

    abstract fun mirror()

    abstract fun scale(x: Int, y: Int, z: Int)

    abstract fun setContrast(contrast: Int)

    /**
     * [J]
     * @param src
     * @param dst
     */
    abstract fun recolor(src: Short, dst: Short)

    /**
     * [J]
     * @param src
     * @param dst
     */
    abstract fun rematerial(src: Short, dst: Short)

    abstract fun tint(hue: Int, saturation: Int, lightness: Int, opacity: Int)

    abstract fun applyShading(functionMask: Int)

    abstract fun setAmbient(ambient: Int)

    abstract fun applyEnvironmentAlpha(alpha: Byte, transparency: ByteArray)

    abstract fun createShadowSprite(shadow: Shadow?): Shadow?

    abstract fun applyShading(model: Model, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean)

    abstract fun copy(copyType: Byte, functionMask: Int, recalculateBeforeCopy: Boolean): Model?

    abstract fun offsetSubmesh(var1: Matrix43, functionMask: Int, var3: Boolean)

    abstract fun updateParticlePosition(projectionMatrix: Matrix43)

    /**
     * *****************************************************************
     * 					(TODO:) Getters
     * *****************************************************************
     */

    abstract fun getSize2d(): Int

    abstract fun getSize3d(): Int

    abstract fun getMinX(): Int

    abstract fun getMaxX(): Int

    abstract fun getMinY(): Int

    abstract fun getMaxY(): Int

    abstract fun getMinZ(): Int

    abstract fun getMaxZ(): Int

    abstract fun getFunctionMask(): Int

    abstract fun getAmbient(): Int

    abstract fun getContrast(): Int

    abstract fun getAlpha(): ByteArray?

    abstract fun getEmissiveTriangles(): Array<EmissiveTriangle>?

    abstract fun getEffectiveVertices(): Array<EffectiveVertex>?

    abstract fun areTexturesCached(): Boolean

    abstract fun hasAnimatedTexture(): Boolean

    abstract fun accountForLayers(): Boolean

    /**
     * *****************************************************************
     * 					(TODO:) Rotate
     * *****************************************************************
     */

    abstract fun rotateWithNormals(rotation: Int)

    abstract fun zaxisRotateWithoutNormals(rotation: Int)

    abstract fun xaxisRotateWithoutNormals(rotation: Int)

    abstract fun yaxisRotateWithoutNormals(rotation: Int)


    /**
     * *****************************************************************
     * 					(TODO:) Animate
     * *****************************************************************
     */

    abstract fun startAnimation(): Boolean

    abstract fun endAnimation()

    abstract fun animatePartialtransform(baseTransformType: Int, label: IntArray, x: Int, y: Int, z: Int, rotation: Int, lights: Boolean)

    abstract fun animateTransform(baseTransformType: Int, label: IntArray, x: Int, y: Int, z: Int, lights: Boolean, submesh: Int, verticesData: IntArray)

    open fun animateUnrotatedSubmesh(firstFrameset: FrameSet?, firstFrameId: Int, nextFrameset: FrameSet?, nextFrameId: Int, cycle: Int, length: Int, submesh: Int, lights: Boolean, alwaysNull: IntArray?) {
        if (firstFrameId != -1) {
            awaitLock()
            if (!startAnimation()) {
                unlock()
            } else {
                val firstFrame = firstFrameset!!.frames!![firstFrameId]
                val firstBase = firstFrame!!.base
                var nextFrame: AnimFrame? = null
                if (nextFrameset != null) {
                    nextFrame = nextFrameset.frames!![nextFrameId]
                    if (firstBase !== nextFrame!!.base) {
                        nextFrame = null
                    }
                }
                interpolateFrames(firstBase, firstFrame, nextFrame, cycle, length, 0, null, false, lights, submesh, alwaysNull)
                endAnimation()
                unlock()
            }
        }
    }

    open fun animateModelFrame(firstFrameset: FrameSet?, firstFrameId: Int, nextFrameset: FrameSet?, nextFrameId: Int, cycle: Int, length: Int, rotation: Int, lights: Boolean) {
        if (firstFrameId != -1) {
            awaitLock()
            if (!startAnimation()) {
                unlock()
            } else {
                val firstFrame = firstFrameset!!.frames!![firstFrameId]
                val firstBase = firstFrame!!.base
                var nextFrame: AnimFrame? = null
                if (nextFrameset != null) {
                    nextFrame = nextFrameset.frames!![nextFrameId]
                    if (firstBase !== nextFrame!!.base) {
                        nextFrame = null
                    }
                }
                interpolateFrames(firstBase, firstFrame, nextFrame, cycle, length, rotation, null, false, lights, 65535, null)
                endAnimation()
                unlock()
            }
        }
    }

    open fun doubleAnimation(animatingSeqCurrentFrameset: FrameSet?, animatingSeqCurrentFrameId: Int, animatingSeqNextFrameset: FrameSet?, animatingSeqNextFrameId: Int, animatingSeqCycle: Int, animatingSeqDuration: Int, idleSeqCurrentFrameset: FrameSet?, idleSeqCurrentFrameId: Int, idleSeqNextFrameset: FrameSet?, idleSeqNextFrameId: Int, idleSeqCycle: Int, idleSeqDuration: Int, animateLabels: BooleanArray?, lights: Boolean) {
        if (animatingSeqCurrentFrameId != -1) {
            if (animateLabels != null && idleSeqCurrentFrameId != -1) {
                awaitLock()
                if (!startAnimation()) {
                    unlock()
                } else {
                    val animatingSeqCurrentFrame = animatingSeqCurrentFrameset!!.frames!![animatingSeqCurrentFrameId]
                    val animatingSeqBase = animatingSeqCurrentFrame!!.base
                    var animatingSeqNextFrame: AnimFrame? = null
                    if (animatingSeqNextFrameset != null) {
                        animatingSeqNextFrame = animatingSeqNextFrameset.frames!![animatingSeqNextFrameId]
                        if (animatingSeqBase !== animatingSeqNextFrame!!.base) {
                            animatingSeqNextFrame = null
                        }
                    }
                    interpolateFrames(animatingSeqBase, animatingSeqCurrentFrame, animatingSeqNextFrame, animatingSeqCycle, animatingSeqDuration, 0, animateLabels, false, lights, 65535, null)
                    val idleSeqCurrentFrame = idleSeqCurrentFrameset!!.frames!![idleSeqCurrentFrameId]
                    var idleSeqNextFrame: AnimFrame? = null
                    if (idleSeqNextFrameset != null) {
                        idleSeqNextFrame = idleSeqNextFrameset.frames!![idleSeqNextFrameId]
                        if (animatingSeqBase !== idleSeqNextFrame!!.base) {
                            idleSeqNextFrame = null
                        }
                    }
                    animatePartialtransform(0, IntArray(0), 0, 0, 0, 0, lights)
                    interpolateFrames(idleSeqCurrentFrame!!.base, idleSeqCurrentFrame, idleSeqNextFrame, idleSeqCycle, idleSeqDuration, 0, animateLabels, true, lights, 65535, null)
                    endAnimation()
                    unlock()
                }
            } else {
                animateModelFrame(animatingSeqCurrentFrameset, animatingSeqCurrentFrameId, animatingSeqNextFrameset, animatingSeqNextFrameId, animatingSeqCycle, animatingSeqDuration, 0, lights)
            }
        }
    }

    open fun interpolateFrames(base: AnimBase?, currentFrame: AnimFrame?, nextFrame: AnimFrame?, cycle: Int, length: Int, rotation: Int, animateLabels: BooleanArray?, condition: Boolean, lights: Boolean, submesh: Int, alwaysNull: IntArray?) {
        var currentTransformation: Int
        if (nextFrame != null && cycle != 0) {
            currentTransformation = 0
            var nextTransformation = 0
            for (transformIndex in 0 until base!!.numTransformation) {
                val transformCurrent = currentTransformation < currentFrame!!.numTransformations && transformIndex == currentFrame.baseIndices!![currentTransformation].toInt()
                val transformNext = nextTransformation < nextFrame.numTransformations && transformIndex == nextFrame.baseIndices!![nextTransformation].toInt()
                if (transformCurrent || transformNext) {
                    if (animateLabels != null && animateLabels[transformIndex] != condition && base.transformationType!![transformIndex] != 0) {
                        if (transformCurrent) {
                            currentTransformation++
                        }
                        if (transformNext) {
                            nextTransformation++
                        }
                    } else {
                        var defaultTransformState: Short = 0
                        val transformationType = base.transformationType!![transformIndex]
                        if (transformationType == 3 || transformationType == 10) {
                            defaultTransformState = 128
                        }
                        val currentTransformX: Short
                        val currentTransformY: Short
                        val currentTransformZ: Short
                        val currentLabel: Short
                        val currentInterpolation: Byte
                        if (transformCurrent) {
                            currentTransformX = currentFrame.transformationX!![currentTransformation]
                            currentTransformY = currentFrame.transformationY!![currentTransformation]
                            currentTransformZ = currentFrame.transformationZ!![currentTransformation]
                            currentLabel = currentFrame.labels!![currentTransformation]
                            currentInterpolation = currentFrame.tweeningProperties!![currentTransformation]
                            currentTransformation++
                        } else {
                            currentTransformX = defaultTransformState
                            currentTransformY = defaultTransformState
                            currentTransformZ = defaultTransformState
                            currentLabel = -1
                            currentInterpolation = 0
                        }
                        val nextTransformX: Short
                        val nextTransformY: Short
                        val nextTransformZ: Short
                        val nextLabel: Short
                        val nextInterpolation: Byte
                        if (transformNext) {
                            nextTransformX = nextFrame.transformationX!![nextTransformation]
                            nextTransformY = nextFrame.transformationY!![nextTransformation]
                            nextTransformZ = nextFrame.transformationZ!![nextTransformation]
                            nextLabel = nextFrame.labels!![nextTransformation]
                            nextInterpolation = nextFrame.tweeningProperties!![nextTransformation]
                            nextTransformation++
                        } else {
                            nextTransformX = defaultTransformState
                            nextTransformY = defaultTransformState
                            nextTransformZ = defaultTransformState
                            nextLabel = -1
                            nextInterpolation = 0
                        }
                        val interpolatedX: Int
                        val interpolatedY: Int
                        val interpolatedZ: Int
                        if ((currentInterpolation.toInt() and 0x2) == 0 && (nextInterpolation.toInt() and 0x1) == 0) {
                            if (transformationType == 2) {
                                var deltaX = nextTransformX - currentTransformX and 0x3fff
                                var deltaY = nextTransformY - currentTransformY and 0x3fff
                                var deltaZ = nextTransformZ - currentTransformZ and 0x3fff
                                if (deltaX >= 8192) {
                                    deltaX -= 16384
                                }
                                if (deltaY >= 8192) {
                                    deltaY -= 16384
                                }
                                if (deltaZ >= 8192) {
                                    deltaZ -= 16384
                                }
                                interpolatedX = currentTransformX + deltaX * cycle / length and 0x3fff
                                interpolatedY = currentTransformY + deltaY * cycle / length and 0x3fff
                                interpolatedZ = currentTransformZ + deltaZ * cycle / length and 0x3fff
                            } else if (transformationType == 9) {
                                var deltaX = nextTransformX - currentTransformX and 0x3fff
                                if (deltaX >= 8192) {
                                    deltaX -= 16384
                                }
                                interpolatedX = currentTransformX + deltaX * cycle / length and 0x3fff
                                interpolatedZ = 0
                                interpolatedY = 0
                            } else if (transformationType == 7) {
                                var deltaX = nextTransformX - currentTransformX and 0x3f
                                if (deltaX >= 32) {
                                    deltaX -= 64
                                }
                                interpolatedX = currentTransformX + deltaX * cycle / length and 0x3f
                                interpolatedY = currentTransformY + (nextTransformY - currentTransformY) * cycle / length
                                interpolatedZ = currentTransformZ + (nextTransformZ - currentTransformZ) * cycle / length
                            } else {
                                interpolatedX = currentTransformX + (nextTransformX - currentTransformX) * cycle / length
                                interpolatedY = currentTransformY + (nextTransformY - currentTransformY) * cycle / length
                                interpolatedZ = currentTransformZ + (nextTransformZ - currentTransformZ) * cycle / length
                            }
                        } else {
                            interpolatedX = currentTransformX.toInt()
                            interpolatedY = currentTransformY.toInt()
                            interpolatedZ = currentTransformZ.toInt()
                        }
                        if (currentLabel.toInt() != -1) {
                            animateTransform(0, base.labels!![currentLabel.toInt()], 0, 0, 0, rotation, lights, submesh and base.submeshes!![currentLabel.toInt()], alwaysNull)
                        } else if (nextLabel.toInt() != -1) {
                            animateTransform(0, base.labels!![nextLabel.toInt()], 0, 0, 0, rotation, lights, submesh and base.submeshes!![nextLabel.toInt()], alwaysNull)
                        }
                        animateTransform(transformationType, base.labels!![transformIndex], interpolatedX, interpolatedY, interpolatedZ, rotation, lights, submesh and base.submeshes!![transformIndex], alwaysNull)
                    }
                }
            }
        } else {
            currentTransformation = 0
            while (currentTransformation < currentFrame!!.numTransformations) {
                val currentIndice = currentFrame.baseIndices!![currentTransformation]
                if (animateLabels == null || animateLabels[currentIndice.toInt()] == condition || base!!.transformationType!![currentIndice.toInt()] == 0) {
                    val currentLabel = currentFrame.labels!![currentTransformation]
                    if (currentLabel.toInt() != -1) {
                        animateTransform(0, base!!.labels!![currentLabel.toInt()], 0, 0, 0, rotation, lights, submesh and base.submeshes!![currentLabel.toInt()], alwaysNull)
                    }
                    animateTransform(base!!.transformationType!![currentIndice.toInt()], base.labels!![currentIndice.toInt()], currentFrame.transformationX!![currentTransformation].toInt(), currentFrame.transformationY!![currentTransformation].toInt(), currentFrame.transformationZ!![currentTransformation].toInt(), rotation, lights, submesh and base.submeshes!![currentIndice.toInt()], alwaysNull)
                }
                currentTransformation++
            }
        }
    }

    open fun animateTransform(baseTransformType: Int, label: IntArray?, transformX: Int, transformY: Int, transformZ: Int, adjustmentType: Int, lights: Boolean, submesh: Int, alwaysNull: IntArray?) {
        var x = transformX
        var z = transformZ
        val y: Int
        if (adjustmentType == 1) {
            if (baseTransformType != 0 && baseTransformType != 1) {
                if (baseTransformType == 3) {
                    y = x
                    x = z
                    z = y
                } else if (baseTransformType == 2) {
                    y = x
                    x = -z and 0x3fff
                    z = y and 0x3fff
                }
            } else {
                y = -x
                x = z
                z = y
            }
        } else if (adjustmentType == 2) {
            if (baseTransformType != 0 && baseTransformType != 1) {
                if (baseTransformType == 2) {
                    x = -x and 0x3fff
                    z = -z and 0x3fff
                }
            } else {
                x = -x
                z = -z
            }
        } else if (adjustmentType == 3) {
            if (baseTransformType != 0 && baseTransformType != 1) {
                if (baseTransformType == 3) {
                    y = x
                    x = z
                    z = y
                } else if (baseTransformType == 2) {
                    y = x
                    x = z and 0x3fff
                    z = -y and 0x3fff
                }
            } else {
                y = x
                x = -z
                z = y
            }
        }
        if (submesh != 65535) {
            animateTransform(baseTransformType, label ?: IntArray(0), x, transformY, z, lights, submesh, alwaysNull ?: IntArray(0))
        } else {
            animatePartialtransform(baseTransformType, label ?: IntArray(0), x, transformY, z, adjustmentType, lights)
        }
    }

    open fun animateShadowFrame(frameset: FrameSet?, shadowFrameId: Int) {
        if (shadowFrameId != -1) {
            awaitLock()
            if (!startAnimation()) {
                unlock()
            } else {
                val frame = frameset!!.frames!![shadowFrameId]
                val base = frame!!.base
                for (transformIndex in 0 until frame.numTransformations) {
                    val transform = frame.baseIndices!![transformIndex]
                    if (base!!.shadowed!![transform.toInt()]) {
                        if (frame.labels!![transformIndex].toInt() != -1) {
                            animateShadowtransform(0, 0, 0, 0)
                        }
                        animateShadowtransform(base.transformationType!![transform.toInt()], frame.transformationX!![transformIndex].toInt(), frame.transformationY!![transformIndex].toInt(), frame.transformationZ!![transformIndex].toInt())
                    }
                }
                endAnimation()
                unlock()
            }
        }
    }

    abstract fun animateShadowtransform(transformType: Int, x: Int, y: Int, z: Int)


    /**
     * *****************************************************************
     * 					(TODO:) Ground adjustment
     * *****************************************************************
     */

    abstract fun contourToGround(contourType: Int, contourModifier: Int, currentLvl: Ground, nextLvl: Ground, sceneX: Int, averageHeight: Int, sceneY: Int)

    /**
     * [J]
     */
    protected open fun hillChange(ground: Ground, offX: Int, y: Int, offY: Int, width: Int, length: Int, xan: Int, zan: Int) {
        val deltaXSw = -width / 2
        val deltaYSw = -length / 2
        val southWest = ground.getAverageHeight(deltaXSw + offX, deltaYSw + offY)
        val deltaXSe = width / 2
        val deltaYSe = -length / 2
        val southEast = ground.getAverageHeight(deltaXSe + offX, deltaYSe + offY)
        val deltaXNw = -width / 2
        val deltaYNw = length / 2
        val northWest = ground.getAverageHeight(deltaXNw + offX, deltaYNw + offY)
        val deltaXNe = width / 2
        val deltaYNe = length / 2
        val northEast = ground.getAverageHeight(deltaXNe + offX, deltaYNe + offY)
        val lowestSouth = min(southWest, southEast)
        val lowestNorth = min(northWest, northEast)
        val lowestEast = min(southEast, northEast)
        val lowestWest = min(southWest, northWest)
        if (length != 0) {
            var rotation = (atan2((lowestSouth - lowestNorth).toDouble(), length.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
            if (rotation != 0) {
                if (xan != 0) {
                    if (rotation > 8192) {
                        val inverse = 16384 - xan
                        if (rotation < inverse) {
                            rotation = inverse
                        }
                    } else if (rotation > xan) {
                        rotation = xan
                    }
                }
                xaxisRotateWithoutNormals(rotation)
            }
        }
        if (width != 0) {
            var rotation = (atan2((lowestWest - lowestEast).toDouble(), width.toDouble()) * 2607.5945876176133).toInt() and 0x3fff
            if (rotation != 0) {
                if (zan != 0) {
                    if (rotation > 8192) {
                        val inverse = 16384 - zan
                        if (rotation < inverse) {
                            rotation = inverse
                        }
                    } else if (rotation > zan) {
                        rotation = zan
                    }
                }
                zaxisRotateWithoutNormals(rotation)
            }
        }
        var height = southWest + northEast
        if (southEast + northWest < height) {
            height = southEast + northWest
        }
        height = (height shr 1) - y
        if (height != 0) {
            translate(0, height, 0)
        }
    }


    /**
     * *****************************************************************
     * 					(TODO:) Texture matrix
     * *****************************************************************
     */

    protected open fun generateTextureMatrix(mesh: Mesh, faces: IntArray?, numFaces: Int): TextureTransformationMatrix? {
        var centerX: IntArray? = null
        var centerY: IntArray? = null
        var centerZ: IntArray? = null
        var matrix: Array<FloatArray>? = null
        if (mesh.facesTexture != null) {
            val numTexturedFaces = mesh.numTextures
            val minX = IntArray(numTexturedFaces)
            val maxX = IntArray(numTexturedFaces)
            val minY = IntArray(numTexturedFaces)
            val maxY = IntArray(numTexturedFaces)
            val minZ = IntArray(numTexturedFaces)
            val maxZ = IntArray(numTexturedFaces)
            for (faceIndex in 0 until numTexturedFaces) {
                minX[faceIndex] = Int.MAX_VALUE
                maxX[faceIndex] = -2147483647
                minY[faceIndex] = Int.MAX_VALUE
                maxY[faceIndex] = -2147483647
                minZ[faceIndex] = Int.MAX_VALUE
                maxZ[faceIndex] = -2147483647
            }
            for (faceIndex in 0 until numFaces) {
                val face = faces!![faceIndex]
                if (mesh.facesTexture[face].toInt() != -1) {
                    val index = mesh.facesTexture[face].toInt() and 0xff
                    for (axis in 0..2) {
                        val verticeIndex: Short = when (axis) {
                            0 -> mesh.facesA[face]
                            1 -> mesh.facesB[face]
                            else -> mesh.facesC[face]
                        }
                        val x = mesh.verticesX[verticeIndex.toInt()]
                        val y = mesh.verticesY[verticeIndex.toInt()]
                        val z = mesh.verticesZ[verticeIndex.toInt()]
                        if (x < minX[index]) {
                            minX[index] = x
                        }
                        if (x > maxX[index]) {
                            maxX[index] = x
                        }
                        if (y < minY[index]) {
                            minY[index] = y
                        }
                        if (y > maxY[index]) {
                            maxY[index] = y
                        }
                        if (z < minZ[index]) {
                            minZ[index] = z
                        }
                        if (z > maxZ[index]) {
                            maxZ[index] = z
                        }
                    }
                }
            }
            centerX = IntArray(numTexturedFaces)
            centerY = IntArray(numTexturedFaces)
            centerZ = IntArray(numTexturedFaces)
            matrix = Array(numTexturedFaces) { FloatArray(0) }
            for (faceIndex in 0 until numTexturedFaces) {
                val textureMapping = mesh.texturesMapping[faceIndex]
                if (textureMapping > 0) {
                    centerX[faceIndex] = (minX[faceIndex] + maxX[faceIndex]) / 2
                    centerY[faceIndex] = (minY[faceIndex] + maxY[faceIndex]) / 2
                    centerZ[faceIndex] = (minZ[faceIndex] + maxZ[faceIndex]) / 2
                    val scaleX: Float
                    val scaleY: Float
                    val scaleZ: Float
                    if (textureMapping.toInt() == 1) {
                        val x = mesh.texturesScaleX[faceIndex].toInt()
                        if (x == 0) {
                            scaleX = 1.0f
                            scaleZ = 1.0f
                        } else if (x > 0) {
                            scaleX = 1.0f
                            scaleZ = x / 1024.0f
                        } else {
                            scaleZ = 1.0f
                            scaleX = (-x) / 1024.0f
                        }
                        scaleY = 64.0f / mesh.texturesScaleY[faceIndex]
                    } else if (textureMapping.toInt() == 2) {
                        scaleX = 64.0f / mesh.texturesScaleX[faceIndex]
                        scaleY = 64.0f / mesh.texturesScaleY[faceIndex]
                        scaleZ = 64.0f / mesh.texturesScaleZ[faceIndex]
                    } else {
                        scaleX = mesh.texturesScaleX[faceIndex] / 1024.0f
                        scaleY = mesh.texturesScaleY[faceIndex] / 1024.0f
                        scaleZ = mesh.texturesScaleZ[faceIndex] / 1024.0f
                    }
                    matrix[faceIndex] = createTextureMatrix(mesh.textureFaceX[faceIndex].toInt(), mesh.textureFaceY[faceIndex].toInt(), mesh.textureFaceZ[faceIndex].toInt(), mesh.texturesRotation[faceIndex].toInt() and 0xff, scaleX, scaleY, scaleZ)
                }
            }
        }
        return TextureTransformationMatrix(this, centerX, centerY, centerZ, matrix)
    }

    open fun createTextureMatrix(n: Int, m: Int, p: Int, rotation: Int, scaleX: Float, scaleY: Float, scaleZ: Float): FloatArray {
        val rotationMap = FloatArray(9)
        val result = FloatArray(9)
        var cos = cos(rotation * 0.024543693f)
        var sin = sin(rotation * 0.024543693f)
        var omc = 1.0f - cos
        rotationMap[0] = cos
        rotationMap[1] = 0.0f
        rotationMap[2] = sin
        rotationMap[3] = 0.0f
        rotationMap[4] = 1.0f
        rotationMap[5] = 0.0f
        rotationMap[6] = -sin
        rotationMap[7] = 0.0f
        rotationMap[8] = cos
        val matrix3x3 = FloatArray(9)
        var x = 1.0f
        var y = 0.0f
        cos = m / 32767.0f
        sin = -sqrt(1.0f - cos * cos)
        omc = 1.0f - cos
        val size = sqrt((p * p + n * n).toFloat())
        if (size == 0.0f && cos == 0.0f) {
            return rotationMap
        } else {
            if (size != 0.0f) {
                x = (-p) / size
                y = n / size
            }
            matrix3x3[0] = cos + x * x * omc
            matrix3x3[1] = y * sin
            matrix3x3[2] = y * x * omc
            matrix3x3[3] = -y * sin
            matrix3x3[4] = cos
            matrix3x3[5] = x * sin
            matrix3x3[6] = x * y * omc
            matrix3x3[7] = -x * sin
            matrix3x3[8] = cos + y * y * omc
            result[0] = rotationMap[0] * matrix3x3[0] + rotationMap[1] * matrix3x3[3] + rotationMap[2] * matrix3x3[6]
            result[1] = rotationMap[0] * matrix3x3[1] + rotationMap[1] * matrix3x3[4] + rotationMap[2] * matrix3x3[7]
            result[2] = rotationMap[0] * matrix3x3[2] + rotationMap[1] * matrix3x3[5] + rotationMap[2] * matrix3x3[8]
            result[3] = rotationMap[3] * matrix3x3[0] + rotationMap[4] * matrix3x3[3] + rotationMap[5] * matrix3x3[6]
            result[4] = rotationMap[3] * matrix3x3[1] + rotationMap[4] * matrix3x3[4] + rotationMap[5] * matrix3x3[7]
            result[5] = rotationMap[3] * matrix3x3[2] + rotationMap[4] * matrix3x3[5] + rotationMap[5] * matrix3x3[8]
            result[6] = rotationMap[6] * matrix3x3[0] + rotationMap[7] * matrix3x3[3] + rotationMap[8] * matrix3x3[6]
            result[7] = rotationMap[6] * matrix3x3[1] + rotationMap[7] * matrix3x3[4] + rotationMap[8] * matrix3x3[7]
            result[8] = rotationMap[6] * matrix3x3[2] + rotationMap[7] * matrix3x3[5] + rotationMap[8] * matrix3x3[8]
        }
        result[0] *= scaleX
        result[1] *= scaleX
        result[2] *= scaleX
        result[3] *= scaleY
        result[4] *= scaleY
        result[5] *= scaleY
        result[6] *= scaleZ
        result[7] *= scaleZ
        result[8] *= scaleZ
        return result
    }


    /**
     * *****************************************************************
     * 					(TODO:) UV mapping
     * *****************************************************************
     */

    protected open fun mapCylindericalUv(verticeX: Int, verticeY: Int, verticeZ: Int, centerX: Int, centerY: Int, centerZ: Int, matrix: FloatArray?, offset: Float, direction: Int, f_10: Float, uv: FloatArray?) {
        var vX = verticeX - centerX
        var vY = verticeY - centerY
        var vZ = verticeZ - centerZ
        val y = vX * matrix!![0] + vY * matrix[1] + vZ * matrix[2]
        val f_13 = vX * matrix[3] + vY * matrix[4] + vZ * matrix[5]
        val x = vX * matrix[6] + vY * matrix[7] + vZ * matrix[8]
        var u = atan2(y, x) / 6.2831855f + 0.5f
        if (offset != 1.0f) {
            u *= offset
        }
        var v = f_13 + 0.5f + f_10
        val tmp: Float
        when (direction) {
            1 -> {
                tmp = u
                u = -v
                v = tmp
            }
            2 -> {
                u = -u
                v = -v
            }
            3 -> {
                tmp = u
                u = v
                v = -tmp
            }
        }
        uv!![0] = u
        uv[1] = v
    }

    protected open fun mapSphericalUv(verticeX: Int, verticeY: Int, verticeZ: Int, centerX: Int, centerY: Int, centerZ: Int, matrix: FloatArray?, direction: Int, f_9: Float, uv: FloatArray?) {
        var vX = verticeX - centerX
        var vY = verticeY - centerY
        var vZ = verticeZ - centerZ
        val f_11 = vX * matrix!![0] + vY * matrix[1] + vZ * matrix[2]
        val f_12 = vX * matrix[3] + vY * matrix[4] + vZ * matrix[5]
        val f_13 = vX * matrix[6] + vY * matrix[7] + vZ * matrix[8]
        val f_14 = sqrt(f_11 * f_11 + f_12 * f_12 + f_13 * f_13)
        var u = atan2(f_11, f_13) / 6.2831855f + 0.5f
        var v = asin(f_12 / f_14) / 3.1415927f + 0.5f + f_9
        val tmp: Float
        when (direction) {
            1 -> {
                tmp = u
                u = -v
                v = tmp
            }
            2 -> {
                u = -u
                v = -v
            }
            3 -> {
                tmp = u
                u = v
                v = -tmp
            }
        }
        uv!![0] = u
        uv[1] = v
    }

    protected open fun mapRectangularUv(verticeX: Int, verticeY: Int, verticeZ: Int, centerX: Int, centerY: Int, centerZ: Int, type: Int, matrix: FloatArray?, direction: Int, f_10: Float, f_11: Float, f_12: Float, uv: FloatArray?) {
        var vX = verticeX - centerX
        var vY = verticeY - centerY
        var vZ = verticeZ - centerZ
        val f_14 = vX * matrix!![0] + vY * matrix[1] + vZ * matrix[2]
        val f_15 = vX * matrix[3] + vY * matrix[4] + vZ * matrix[5]
        val f_16 = vX * matrix[6] + vY * matrix[7] + vZ * matrix[8]
        var u: Float
        var v: Float
        when (type) {
            0 -> {
                u = f_14 + f_10 + 0.5f
                v = -f_16 + f_12 + 0.5f
            }
            1 -> {
                u = f_14 + f_10 + 0.5f
                v = f_16 + f_12 + 0.5f
            }
            2 -> {
                u = -f_14 + f_10 + 0.5f
                v = -f_15 + f_11 + 0.5f
            }
            3 -> {
                u = f_14 + f_10 + 0.5f
                v = -f_15 + f_11 + 0.5f
            }
            4 -> {
                u = f_16 + f_12 + 0.5f
                v = -f_15 + f_11 + 0.5f
            }
            else -> {
                u = -f_16 + f_12 + 0.5f
                v = -f_15 + f_11 + 0.5f
            }
        }
        val tmp: Float
        when (direction) {
            1 -> {
                tmp = u
                u = -v
                v = tmp
            }
            2 -> {
                u = -u
                v = -v
            }
            3 -> {
                tmp = u
                u = v
                v = -tmp
            }
        }
        uv!![0] = u
        uv[1] = v
    }

    protected open fun getCubeFace(f_1: Float, f_2: Float, f_3: Float): Int {
        val f_4 = if (f_1 < 0.0f) -f_1 else f_1
        val f_5 = if (f_2 < 0.0f) -f_2 else f_2
        val f_6 = if (f_3 < 0.0f) -f_3 else f_3
        return if (f_5 > f_4 && f_5 > f_6) (if (f_2 > 0.0f) 0 else 1) else (if (f_6 > f_4 && f_6 > f_5) (if (f_3 > 0.0f) 2 else 3) else (if (f_1 > 0.0f) 4 else 5))
    }

    /**
     * *****************************************************************
     * 					(TODO:) Misc / static
     * *****************************************************************
     */

    companion object {
        @JvmStatic
        fun addBillboard(billboardUids: LongArray, faces: IntArray) {
            ArrayUtils.quicksort(billboardUids, faces, 0, billboardUids.size - 1)
        }
    }
}
