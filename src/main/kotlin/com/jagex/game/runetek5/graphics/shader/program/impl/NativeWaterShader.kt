package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.Shader
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException
import com.jagex.game.runetek5.graphics.shader.ShaderMapping
import com.jagex.game.runetek5.graphics.shader.program.AbstractWaterShader
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

abstract class NativeWaterShader(toolkit: AbstractNativeToolkit) : AbstractWaterShader(toolkit) {
    var activeShader: Shader? = null
    var eyePositionOsMatrixAttribute: ShaderAttribute? = null
    var textureMatrixAttribute: ShaderAttribute? = null
    var modelMatrix: ShaderAttribute? = null
    var viewMatrixAttribute: ShaderAttribute? = null
    var projectionMatrixAttribute: ShaderAttribute? = null
    var modelViewMatrixAttribute: ShaderAttribute? = null
    var modelViewProjectionMatrixAttribute: ShaderAttribute? = null
    var viewProjectionMatrixAttribute: ShaderAttribute? = null
    var ambientColour: ShaderAttribute? = null
    var sunDirection: ShaderAttribute? = null
    var sunColour: ShaderAttribute? = null
    var antiSunAttribute: ShaderAttribute? = null
    var sunExponentAttribute: ShaderAttribute? = null
    var eyePositionAttribute: ShaderAttribute? = null
    var eyePositionOS: ShaderAttribute? = null
    var sunDirectionOS: ShaderAttribute? = null
    var mappedWaterCount: Int = 0
    var mappedWaterFlags: IntArray? = null
    val eyePosition = FloatArray(3)
    val matrices: Array<Matrix44>

    init {
        Matrix43()
        matrices = Array(toolkit.maxConcurrentTextures) { Matrix44() }
    }

    override fun updateWaterState() {
    }

    @Throws(ShaderBuilderException::class)
    fun loadShaderWithAttributes(shaderName: String): Boolean {
        activeShader = toolkit.loadShader(shaderName)
        if (activeShader == null) {
            throw ShaderBuilderException("")
        }
        textureMatrixAttribute = activeShader!!.loadAttribute("textureMatrix")
        modelMatrix = activeShader!!.loadAttribute("modelMatrix")
        viewMatrixAttribute = activeShader!!.loadAttribute("viewMatrix")
        projectionMatrixAttribute = activeShader!!.loadAttribute("projectionMatrix")
        modelViewMatrixAttribute = activeShader!!.loadAttribute("modelViewMatrix")
        modelViewProjectionMatrixAttribute = activeShader!!.loadAttribute("modelViewProjectionMatrix")
        viewProjectionMatrixAttribute = activeShader!!.loadAttribute("viewProjectionMatrix")
        ambientColour = activeShader!!.loadAttribute("ambientColour")
        sunDirection = activeShader!!.loadAttribute("sunDirection")
        sunColour = activeShader!!.loadAttribute("sunColour")
        antiSunAttribute = activeShader!!.loadAttribute("antiSunColour")
        sunExponentAttribute = activeShader!!.loadAttribute("sunExponent")
        eyePositionAttribute = activeShader!!.loadAttribute("eyePosition")
        eyePositionOS = activeShader!!.loadAttribute("eyePositionOS")
        sunDirectionOS = activeShader!!.loadAttribute("sunDirectionOS")
        if (!buildProgram()) {
            activeShader = null
            textureMatrixAttribute = null
            modelMatrix = null
            viewMatrixAttribute = null
            projectionMatrixAttribute = null
            modelViewMatrixAttribute = null
            modelViewProjectionMatrixAttribute = null
            viewProjectionMatrixAttribute = null
            ambientColour = null
            sunDirection = null
            sunColour = null
            antiSunAttribute = null
            sunExponentAttribute = null
            eyePositionAttribute = null
            eyePositionOS = null
            sunDirectionOS = null
            return false
        }
        if (activeShader!!.getActiveMapping() == null) {
            val mappedWater = activeShader!!.createSingleMapping()
                ?: throw ShaderBuilderException(activeShader!!.getShaderName())
            activeShader!!.loadProgram(mappedWater)
        }
        mappedWaterCount = activeShader!!.getMappingCount()
        mappedWaterFlags = IntArray(mappedWaterCount)
        return true
    }

    @Throws(ShaderBuilderException::class)
    abstract fun buildProgram(): Boolean

    fun loadShaderUniforms() {
        loadLights(toolkit.lightingEnabled)
        loadTextureMatrix(toolkit.getTextureMatrix())
        loadMatrices()
        updateWaterState()
    }

    override fun deactivate() {
        activeShader?.reset()
    }

    open fun loadTextureMatrix(matrix44: Matrix44) {
        val shader = activeShader
        if (shader != null && shader.isActiveMapping() && toolkit.getCurrentTextureSlot() == 0) {
            val mappedWaterCursorPosition = shader.getOccupyingSlot()
            val flags = mappedWaterFlags
            if (flags != null && (flags[mappedWaterCursorPosition] and 0x80) != 0 && textureMatrixAttribute != null) {
                shader.loadTextureMatrix(textureMatrixAttribute!!, matrix44)
                flags[mappedWaterCursorPosition] = flags[mappedWaterCursorPosition] and -129
            }
        }
    }

    fun loadMatrices() {
        val shader = activeShader
        if (shader != null && shader.isActiveMapping()) {
            val occupiedSlot = shader.getOccupyingSlot()
            val flags = mappedWaterFlags
            if (flags == null) return
            val waterFlags = flags[occupiedSlot]

            if ((waterFlags and 0x1) != 0 && modelMatrix != null) {
                shader.loadMatrix(modelMatrix!!, toolkit.getModelMatrix())
            }

            if ((waterFlags and 0x2) != 0) {
                if (viewMatrixAttribute != null) {
                    shader.loadMatrix(viewMatrixAttribute!!, toolkit.getViewMatrix())
                }

                if (eyePositionAttribute != null) {
                    eyePosition[0] = 0.0f
                    eyePosition[1] = 0.0f
                    eyePosition[2] = 0.0f
                    toolkit.copySunPosition().calculateEyePosition(eyePosition[0], eyePosition[1], eyePosition[2], eyePosition)
                    shader.uniform3f(eyePositionAttribute!!, eyePosition[0], eyePosition[1], eyePosition[2])
                }
            }

            if ((waterFlags and 0x10) != 0 && modelViewMatrixAttribute != null) {
                shader.loadMatrix(modelViewMatrixAttribute!!, toolkit.getModelViewMatrix())
            }

            if ((waterFlags and 0x8) != 0 && eyePositionOsMatrixAttribute != null) {
                shader.loadMatrix(eyePositionOsMatrixAttribute!!, toolkit.getEyePositionOsMatrix())
            }

            if ((waterFlags and 0x4) != 0 && projectionMatrixAttribute != null) {
                shader.loadMatrix(projectionMatrixAttribute!!, toolkit.getProjectionMatrix())
            }

            if ((waterFlags and 0x20) != 0 && modelViewProjectionMatrixAttribute != null) {
                VIEW_PROJECTION_MATRIX.calculatePerspective(toolkit.getModelViewMatrix(), toolkit.getProjectionMatrix())
                shader.loadMatrix(modelViewProjectionMatrixAttribute!!, VIEW_PROJECTION_MATRIX)
            }

            if ((waterFlags and 0x40) != 0 && viewProjectionMatrixAttribute != null) {
                VIEW_PROJECTION_MATRIX.calculatePerspective(toolkit.getViewMatrix(), toolkit.getProjectionMatrix())
                shader.loadMatrix(viewProjectionMatrixAttribute!!, VIEW_PROJECTION_MATRIX)
            }

            if (toolkit.getCurrentTextureSlot() == 0 && (flags[occupiedSlot] and 0x80) != 0 && textureMatrixAttribute != null) {
                shader.loadTextureMatrix(textureMatrixAttribute!!, toolkit.getTextureMatrix())
                flags[occupiedSlot] = flags[occupiedSlot] and -129
            }

            flags[occupiedSlot] = 0
        }
    }

    fun loadEyePositionOS(matrix44var2: Matrix43) {
        if (eyePositionOS != null) {
            val eyePosOsMatrix = toolkit.getEyePositionOsMatrix()
            val eyePos = floatArrayOf(eyePosOsMatrix.faceList[12], eyePosOsMatrix.faceList[13], eyePosOsMatrix.faceList[14])
            val floats5 = floatArrayOf(0.0f, 0.0f, 0.0f)
            eyePosOsMatrix.method6528(floats5)
            val floats6 = floatArrayOf(0.0f, 0.0f, 0.0f)
            toolkit.copySunPosition().calculatePerspective(0.0f, 0.0f, 0.0f, floats6)
            matrix44var2.method5243(eyePos)
            activeShader!!.uniform3f(eyePositionOS!!, eyePos[0], eyePos[1], eyePos[2])
        }
    }

    override fun resetModelMatrix() {
        loadModelMatrix(Matrix43.aClass2943518)
    }

    open fun loadLights(dummy: Boolean) {
        val shader = activeShader
        if (shader != null && shader.isActiveMapping()) {
            if (ambientColour != null) {
                shader.uniform3f(
                    ambientColour!!,
                    toolkit.baseLightColorRed * toolkit.lightIntensity,
                    toolkit.baseLightColorGreen * toolkit.lightIntensity,
                    toolkit.baseLightColorBlue * toolkit.lightIntensity
                )
            }

            if (sunDirection != null) {
                shader.uniform3f(sunDirection!!, toolkit.sunPosition[0], toolkit.sunPosition[1], toolkit.sunPosition[2])
            }

            if (sunColour != null) {
                shader.uniform3f(
                    sunColour!!,
                    toolkit.baseLightColorRed * toolkit.sunColorIntensity,
                    toolkit.baseLightColorGreen * toolkit.sunColorIntensity,
                    toolkit.baseLightColorBlue * toolkit.sunColorIntensity
                )
            }

            if (antiSunAttribute != null) {
                shader.uniform3f(
                    antiSunAttribute!!,
                    -toolkit.baseLightColorRed * toolkit.antiSunColorIntensity,
                    -toolkit.baseLightColorGreen * toolkit.antiSunColorIntensity,
                    -toolkit.baseLightColorBlue * toolkit.antiSunColorIntensity
                )
            }

            if (sunExponentAttribute != null) {
                shader.uniform1f(sunExponentAttribute!!, 64.0f + Math.abs(toolkit.sunPosition[1]) * 928.0f)
            }
        }
    }

    override fun loadDefaultModelMatrix() {
        loadModelMatrix(Matrix43.aClass2943518)
    }

    fun loadModelMatrix(matrix44var1: Matrix43) {
        val matrix442 = toolkit.perspectiveMatrix
        matrix442.fromMatrix(matrix44var1)
        val slot = activeShader!!.getOccupyingSlot()
        val flags = mappedWaterFlags
        if (flags == null) return

        if (modelMatrix != null) {
            activeShader!!.loadMatrix(modelMatrix!!, matrix442)
            flags[slot] = flags[slot] and -2
        }

        if (modelViewMatrixAttribute != null) {
            VIEW_PROJECTION_MATRIX.calculatePerspective(matrix442, toolkit.getViewMatrix())
            activeShader!!.loadMatrix(modelViewMatrixAttribute!!, VIEW_PROJECTION_MATRIX)
            flags[slot] = flags[slot] and -17
        }

        if (modelViewProjectionMatrixAttribute != null) {
            VIEW_PROJECTION_MATRIX.calculatePerspective(matrix442, toolkit.getFrustumClipMatrix())
            activeShader!!.loadMatrix(modelViewProjectionMatrixAttribute!!, VIEW_PROJECTION_MATRIX)
            flags[slot] = flags[slot] and -33
        }

        loadEyePositionOS(matrix44var1)
        loadSunDirectionOS(matrix44var1)
    }

    override fun activateForPass() {
        activeShader!!.setActiveMapping()
        mappedWaterFlags!![activeShader!!.getOccupyingSlot()] = -1
        resetTextureState()
        loadShaderUniforms()
    }

    override fun resetModelMatrixForPass() {
        loadModelMatrix(Matrix43.aClass2943518)
    }

    fun setTextured() {
        val flags = mappedWaterFlags
        if (flags != null) {
            for (slot in 0 until mappedWaterCount) {
                flags[slot] = flags[slot] or 128
            }
        }
    }

    override fun activate() {
        activeShader!!.setActiveMapping()
        mappedWaterFlags!![activeShader!!.getOccupyingSlot()] = -1
        resetTextureState()
        loadShaderUniforms()
    }

    fun loadSunDirectionOS(matrix44var2: Matrix43) {
        if (sunDirectionOS != null) {
            val sunPosition = floatArrayOf(toolkit.sunPosition[0], toolkit.sunPosition[1], toolkit.sunPosition[2])
            matrix44var2.method5230(sunPosition)
            activeShader!!.uniform3f(sunDirectionOS!!, sunPosition[0], sunPosition[1], sunPosition[2])
        }
    }

    fun resetTextureState() {
        matrices[0].identity()
        setTextured()
    }

    companion object {
        @JvmField
        val VIEW_PROJECTION_MATRIX = Matrix44()
    }
}
