package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.utils.ArrayUtils
import jaggl.OpenGL

class ShaderMappingGL : ShaderMapping, AutoCloseable {

    var programHandle: Int = 0
    var vertexShaderHandle: Int = 0
    var fragmentShaderHandle: Int = 0
    var isShadersCompiled: Boolean = false
    val attributeLocations: IntArray = IntArray(13)
    var vertexShaderProgramString: String? = null
    var fragmentShaderProgramString: String? = null
    var uniformCacheX: FloatArray? = null
    var uniformCacheY: FloatArray? = null
    var uniformCacheZ: FloatArray? = null
    var uniformCacheW: FloatArray? = null
    var textureSlots: IntArray? = null
    var openglShader: ShaderGL? = null
    var toolkit: OpenGLNativeToolkit? = null

    constructor(toolkit: OpenGLNativeToolkit, shaderMapping: NativeShaderMapping) {
        shaderName = shaderMapping.mappingName
        val programString = StringBuilder()
        if (shaderMapping.vertexShaderName != null) {
            vertexShaderName = shaderMapping.vertexShaderName
            if (shaderMapping.vertexShaderVariables != null) {
                val vertexVariables = shaderMapping.vertexShaderVariables!!
                for (variableIndex in vertexVariables.indices) {
                    val shaderVariable = vertexVariables[variableIndex]!!
                    programString.append("#define ").append(shaderVariable.programVariableName).append(" ").append(shaderVariable.programVariableValue).append('\n')
                }
            }
            programString.append(String(toolkit.getGLShaderData(vertexShaderName!!)!!))
            vertexShaderProgramString = programString.toString()
        }
        if (shaderMapping.fragmentShaderName != null) {
            fragmentShaderName = shaderMapping.fragmentShaderName
            programString.setLength(0)
            if (shaderMapping.fragmentShaderVariables != null) {
                val fragmentVariables = shaderMapping.fragmentShaderVariables!!
                for (variableIndex in fragmentVariables.indices) {
                    val shaderVariable = fragmentVariables[variableIndex]!!
                    programString.append("#define ").append(shaderVariable.programVariableName).append(" ").append(shaderVariable.programVariableValue).append('\n')
                }
            }
            programString.append(String(toolkit.getGLShaderData(fragmentShaderName!!)!!))
            fragmentShaderProgramString = programString.toString()
        }
        ArrayUtils.setVariables(attributeLocations, 0, attributeLocations.size, -1)
    }

    constructor(toolkit: OpenGLNativeToolkit, openglShader: ShaderGL, hardwareShaderMapping: NativeShaderMapping) : this(toolkit, hardwareShaderMapping) {
        this.openglShader = openglShader
        this.toolkit = toolkit
    }

    fun compileShader(shaderType: Int, source: String?): Int {
        if (source == null) {
            return 0
        } else {
            var handle = OpenGL.glCreateShader(shaderType)
            OpenGL.glShaderSource(handle, source)
            OpenGL.glCompileShader(handle)
            OpenGL.glGetShaderiv(handle, GL_COMPILE_STATUS, Companion.STATUS_BUFFER, 0)
            if (Companion.STATUS_BUFFER[0] == 0) {
                OpenGL.glDeleteShader(handle)
                handle = 0
            }

            return handle
        }
    }

    override fun uniform1f(attribute: ShaderAttribute, value: Float) {
        val location = (attribute as ShaderAttributeGL).getActiveUniformLocation()
        if (location != -1 && uniformCacheX!![location] != value) {
            uniformCacheX!![location] = value
            if (attribute.getVariableType() != ShaderVariableType.FLOAT && attribute.getVariableType() != ShaderVariableType.BOOL) {
                throw IllegalShaderArgumentException()
            }

            OpenGL.glUniform1f(location, value)
        }
    }

    override fun unform2f(attribute: ShaderAttribute, x: Float, y: Float) {
        val location = (attribute as ShaderAttributeGL).getActiveUniformLocation()
        if (location != -1 && (uniformCacheX!![location] != x || uniformCacheY!![location] != y)) {
            uniformCacheX!![location] = x
            uniformCacheY!![location] = y
            if (attribute.getVariableType() != ShaderVariableType.VEC2) {
                throw IllegalShaderArgumentException()
            }

            OpenGL.glUniform2f(location, x, y)
        }
    }

    override fun uniform3f(attribute: ShaderAttribute, x: Float, y: Float, z: Float) {
        val location = (attribute as ShaderAttributeGL).getActiveUniformLocation()
        if (location != -1 && (uniformCacheX!![location] != x || uniformCacheY!![location] != y || uniformCacheZ!![location] != z)) {
            uniformCacheX!![location] = x
            uniformCacheY!![location] = y
            uniformCacheZ!![location] = z
            if (attribute.getVariableType() != ShaderVariableType.VEC3) {
                throw IllegalShaderArgumentException()
            }

            OpenGL.glUniform3f(location, x, y, z)
        }
    }

    override fun uniform4fv(attribute: ShaderAttribute, values: FloatArray, count: Int) {
        val location = (attribute as ShaderAttributeGL).getActiveUniformLocation()
        if (location != -1) {
            OpenGL.glUniform4fv(location, count, values, 0)
        }
    }

    override fun uniform2fv(attribute: ShaderAttribute, matrix: Matrix44) {
        if (!Companion.`$assertionsDisabled` && attribute.getVariableType() != ShaderVariableType.MAT4X2) {
            throw AssertionError()
        }

        val location = (attribute as ShaderAttributeGL).getActiveUniformLocation()
        if (location != -1) {
            OpenGL.glUniform2fv(location, 4, matrix.getComponentsXY(Companion.matrixBuffer), 0)
        }
    }

    override fun uniform4fv(attribute: ShaderAttribute, matrix: Matrix44) {
        if (!Companion.`$assertionsDisabled` && attribute.getVariableType() != ShaderVariableType.MAT4X4) {
            throw AssertionError()
        }

        val location = (attribute as ShaderAttributeGL).getActiveUniformLocation()
        if (location != -1) {
            OpenGL.glUniform4fv(location, 4, matrix.copyInto(Companion.matrixBuffer), 0)
        }
    }

    override fun loadTextureSampler(attribute: ShaderAttribute, slot: Int, defaultSampler: TextureSampler) {
        var sampler: TextureSampler? = defaultSampler
        val index = (attribute as ShaderAttributeGL).getActiveUniformLocation()
        if (index != -1) {
            if (sampler == null) {
                sampler = toolkit!!.activeTextureSampler
            }

            if (slot < toolkit!!.maxConcurrentTextures) {
                toolkit!!.setTextureSlot(slot)
                toolkit!!.setTextureSampler(sampler)
            } else {
                OpenGL.glActiveTexture(slot + 33984)
                OpenGL.glBindTexture((sampler as MipmapTextureSampler).textureTarget, sampler.texture)
            }

            if (slot != textureSlots!![index]) {
                textureSlots!![index] = slot
                OpenGL.glUniform1i(index, slot)
            }
        }
    }

    override fun addPositionAttribute3f(location: Int, x: Float, y: Float, z: Float) {
        OpenGL.glUniform3f(location, x, y, z)
    }

    override fun addPositionAttribute4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        OpenGL.glUniform4f(location, x, y, z, w)
    }

    override fun addPreloadedPositionAttribute(attribute: ShaderAttribute, x: Float, y: Float, z: Float, w: Float) {
        val location = (attribute as ShaderAttributeGL).getActiveUniformLocation()
        if (location != -1 && (uniformCacheX!![location] != x || uniformCacheY!![location] != y || uniformCacheZ!![location] != z || uniformCacheW!![location] != w)) {
            uniformCacheX!![location] = x
            uniformCacheY!![location] = y
            uniformCacheZ!![location] = z
            uniformCacheW!![location] = w
            if (attribute.getVariableType() != ShaderVariableType.VEC4) {
                throw IllegalShaderArgumentException()
            }

            OpenGL.glUniform4f(location, x, y, z, w)
        }
    }

    override fun uniform3fSavedSize12(location: Int, matrix: Matrix44) {
        OpenGL.glUniform3fv(location, 3, matrix.copy3x3NearMatrixInto(Companion.matrixBuffer), 0)
    }

    override fun uniform3fSavedSize8(location: Int, matrix: Matrix44) {
        OpenGL.glUniform2fv(location, 4, matrix.getComponentsXY(Companion.matrixBuffer), 0)
    }

    override fun uniform3fSavedSize16(location: Int, matrix: Matrix44) {
        OpenGL.glUniform4fv(location, 4, matrix.copyInto(Companion.matrixBuffer), 0)
    }

    override fun close() {
        deallocate()
    }

    override fun initializeTextureSampler(location: Int, slot: Int, sampler: TextureSampler) {
        toolkit!!.setTextureSlot(slot)
        toolkit!!.setTextureSampler(sampler)
        OpenGL.glUniform1i(location, slot)
    }

    override fun addPositionAttributeDynamic(location: Int, values: FloatArray, count: Int) {
        OpenGL.glUniform4fv(location, count, values, 0)
    }

    override fun deallocate() {
        if (programHandle != 0) {
            toolkit!!.destroyProgram(programHandle)
            if (vertexShaderHandle != 0) {
                toolkit!!.requestShaderDeletion(vertexShaderHandle.toLong())
            }

            if (fragmentShaderHandle != 0) {
                toolkit!!.requestShaderDeletion(fragmentShaderHandle.toLong())
            }

            programHandle = 0
            vertexShaderHandle = 0
            fragmentShaderHandle = 0
        }
    }

    override fun createShaders(): Boolean {
        if (isShadersCompiled) {
            return true
        }
        toolkit!!.activeShaderMapping = null
        vertexShaderHandle = compileShader(GL_VERTEX_SHADER, vertexShaderProgramString)
        fragmentShaderHandle = compileShader(GL_FRAGMENT_SHADER, fragmentShaderProgramString)
        if (vertexShaderHandle != 0 && fragmentShaderHandle != 0) {
            programHandle = OpenGL.glCreateProgram()
            if (vertexShaderHandle != 0) {
                OpenGL.glAttachShader(programHandle, vertexShaderHandle)
            }
            if (fragmentShaderHandle != 0) {
                OpenGL.glAttachShader(programHandle, fragmentShaderHandle)
            }
            OpenGL.glLinkProgram(programHandle)
            OpenGL.glGetProgramiv(programHandle, GL_LINK_STATUS, Companion.STATUS_BUFFER, 0)
            if (Companion.STATUS_BUFFER[0] == 0) {
                if (vertexShaderHandle != 0) {
                    OpenGL.glDetachShader(programHandle, vertexShaderHandle)
                    OpenGL.glDeleteShader(vertexShaderHandle)
                }
                if (fragmentShaderHandle != 0) {
                    OpenGL.glDetachShader(programHandle, fragmentShaderHandle)
                    OpenGL.glDeleteShader(fragmentShaderHandle)
                }
                OpenGL.glDeleteProgram(programHandle)
                return false
            } else {
                OpenGL.glUseProgram(programHandle)
                var activeInSlot = -1
                for (slot in 0 until openglShader!!.getMappingCount()) {
                    if (openglShader!!.getMapping(slot) === this) {
                        activeInSlot = slot
                        break
                    }
                }
                if (activeInSlot == -1) {
                    return false
                } else {
                    var slot = 0
                    var maxTextureSlotLocation = 0
                    var variableType: ShaderVariableType
                    for (variableIndex in 0 until openglShader!!.getPrimaryShaderVariableCount()) {
                        val shaderAttribute = openglShader!!.getShaderAttribute(variableIndex)
                        if (shaderAttribute != null) {
                            shaderAttribute.loadUniformLocation(activeInSlot)
                            variableType = shaderAttribute.getVariableType()
                            val uniformLocation = (shaderAttribute as ShaderAttributeGL).uniformLocations!![activeInSlot]
                            if (variableType != ShaderVariableType.FLOAT && variableType != ShaderVariableType.BOOL && variableType != ShaderVariableType.VEC2 && variableType != ShaderVariableType.VEC3 && variableType != ShaderVariableType.VEC4) {
                                maxTextureSlotLocation = Math.max(uniformLocation, maxTextureSlotLocation)
                            } else {
                                slot = Math.max(uniformLocation, slot)
                            }
                        }
                    }

                    for (variableIndex in 0 until openglShader!!.getSecondaryShaderVariableCount()) {
                        val shaderAttribute = openglShader!!.getSecondaryAttribute(variableIndex)
                        if (shaderAttribute != null) {
                            shaderAttribute.loadUniformLocation(activeInSlot)
                            variableType = shaderAttribute.getVariableType()
                            val uniformLocation = (shaderAttribute as ShaderAttributeGL).uniformLocations!![activeInSlot]
                            if (variableType != ShaderVariableType.FLOAT && variableType != ShaderVariableType.BOOL && variableType != ShaderVariableType.VEC2 && variableType != ShaderVariableType.VEC3 && variableType != ShaderVariableType.VEC4) {
                                maxTextureSlotLocation = Math.max(uniformLocation, maxTextureSlotLocation)
                            } else {
                                slot = Math.max(uniformLocation, slot)
                            }
                        }
                    }

                    uniformCacheX = FloatArray(slot + 1)
                    uniformCacheY = FloatArray(slot + 1)
                    uniformCacheZ = FloatArray(slot + 1)
                    uniformCacheW = FloatArray(slot + 1)
                    textureSlots = IntArray(maxTextureSlotLocation + 1)

                    for (index in textureSlots!!.indices) {
                        textureSlots!![index] = -1
                    }

                    vertexShaderProgramString = null
                    fragmentShaderProgramString = null
                    isShadersCompiled = true
                    return true
                }
            }
        } else {
            if (vertexShaderHandle != 0) {
                OpenGL.glDeleteShader(vertexShaderHandle)
            }

            if (fragmentShaderHandle != 0) {
                OpenGL.glDeleteShader(fragmentShaderHandle)
            }

            return false
        }
    }

    companion object {
        val `$assertionsDisabled` = !ShaderMappingGL::class.java.desiredAssertionStatus()
        val STATUS_BUFFER = IntArray(2)
        val matrixBuffer = FloatArray(16)
    }
}
