package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import com.jagex.game.runetek5.unidentified.Class111
import com.jagex.game.runetek5.utils.ArrayUtils
import jaggl.OpenGL

class ShaderMappingGL : ShaderMapping, AutoCloseable {

    var programHandle: Int = 0
    var vertexShaderHandle: Int = 0
    var fragmentShaderHandle: Int = 0
    var aBool9425: Boolean = false
    val anIntArray9426: IntArray = IntArray(13)
    var vertexShaderProgramString: String? = null
    var fragmentShaderProgramString: String? = null
    var aFloatArray9429: FloatArray? = null
    var aFloatArray9422: FloatArray? = null
    var aFloatArray9431: FloatArray? = null
    var aFloatArray9432: FloatArray? = null
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
        ArrayUtils.setVariables(anIntArray9426, 0, anIntArray9426.size, -1)
    }

    constructor(toolkit: OpenGLNativeToolkit, openglShader: ShaderGL, hardwareShaderMapping: NativeShaderMapping) : this(toolkit, hardwareShaderMapping) {
        this.openglShader = openglShader
        this.toolkit = toolkit
    }

    fun method14671(i_1: Int, string_2: String?): Int {
        if (string_2 == null) {
            return 0
        } else {
            var i_4 = OpenGL.glCreateShader(i_1)
            OpenGL.glShaderSource(i_4, string_2)
            OpenGL.glCompileShader(i_4)
            OpenGL.glGetShaderiv(i_4, GL_COMPILE_STATUS, Companion.STATUS_BUFFER, 0)
            if (Companion.STATUS_BUFFER[0] == 0) {
                OpenGL.glDeleteShader(i_4)
                i_4 = 0
            }

            return i_4
        }
    }

    override fun uniform1f(class282Sub21Sub11: ShaderAttribute, f_2: Float) {
        val i_3 = (class282Sub21Sub11 as ShaderAttributeGL).getActiveUniformLocation()
        if (i_3 != -1 && aFloatArray9429!![i_3] != f_2) {
            aFloatArray9429!![i_3] = f_2
            if (class282Sub21Sub11.method15457() != Class111.aClass1111219 && class282Sub21Sub11.method15457() != Class111.aClass1111182) {
                throw IllegalShaderArgumentException()
            }

            OpenGL.glUniform1f(i_3, f_2)
        }
    }

    override fun unform2f(class282Sub21Sub11: ShaderAttribute, f_2: Float, f_3: Float) {
        val i_4 = (class282Sub21Sub11 as ShaderAttributeGL).getActiveUniformLocation()
        if (i_4 != -1 && (aFloatArray9429!![i_4] != f_2 || aFloatArray9422!![i_4] != f_3)) {
            aFloatArray9429!![i_4] = f_2
            aFloatArray9422!![i_4] = f_3
            if (class282Sub21Sub11.method15457() != Class111.aClass1111132) {
                throw IllegalShaderArgumentException()
            }

            OpenGL.glUniform2f(i_4, f_2, f_3)
        }
    }

    override fun uniform3f(class282Sub21Sub11: ShaderAttribute, f_2: Float, f_3: Float, f_4: Float) {
        val i_5 = (class282Sub21Sub11 as ShaderAttributeGL).getActiveUniformLocation()
        if (i_5 != -1 && (aFloatArray9429!![i_5] != f_2 || aFloatArray9422!![i_5] != f_3 || aFloatArray9431!![i_5] != f_4)) {
            aFloatArray9429!![i_5] = f_2
            aFloatArray9422!![i_5] = f_3
            aFloatArray9431!![i_5] = f_4
            if (class282Sub21Sub11.method15457() != Class111.aClass1111133) {
                throw IllegalShaderArgumentException()
            }

            OpenGL.glUniform3f(i_5, f_2, f_3, f_4)
        }
    }

    override fun uniform4fv(class282Sub21Sub11: ShaderAttribute, floats_2: FloatArray, i_3: Int) {
        val i_4 = (class282Sub21Sub11 as ShaderAttributeGL).getActiveUniformLocation()
        if (i_4 != -1) {
            OpenGL.glUniform4fv(i_4, i_3, floats_2, 0)
        }
    }

    override fun uniform2fv(class282Sub21Sub11: ShaderAttribute, matrix442: Matrix44) {
        if (!Companion.`$assertionsDisabled` && class282Sub21Sub11.method15457() != Class111.aClass1111168) {
            throw AssertionError()
        }

        val i_3 = (class282Sub21Sub11 as ShaderAttributeGL).getActiveUniformLocation()
        if (i_3 != -1) {
            OpenGL.glUniform2fv(i_3, 4, matrix442.getComponenetsXY(Companion.aFloatArray9427), 0)
        }
    }

    override fun uniform4fv(class282Sub21Sub11: ShaderAttribute, matrix442: Matrix44) {
        if (!Companion.`$assertionsDisabled` && class282Sub21Sub11.method15457() != Class111.aClass1111150) {
            throw AssertionError()
        }

        val i_3 = (class282Sub21Sub11 as ShaderAttributeGL).getActiveUniformLocation()
        if (i_3 != -1) {
            OpenGL.glUniform4fv(i_3, 4, matrix442.copyInto(Companion.aFloatArray9427), 0)
        }
    }

    override fun loadTextureSampler(var1: ShaderAttribute, var2: Int, var3: TextureSampler) {
        var sampler: TextureSampler? = var3
        val index = (var1 as ShaderAttributeGL).getActiveUniformLocation()
        if (index != -1) {
            if (sampler == null) {
                sampler = toolkit!!.activeTextureSampler
            }

            if (var2 < toolkit!!.maxConcurrentTextures) {
                toolkit!!.setTextureSlot(var2)
                toolkit!!.setTextureSampler(sampler)
            } else {
                OpenGL.glActiveTexture(var2 + 33984)
                OpenGL.glBindTexture((sampler as MipmapTextureSampler).textureTarget, sampler.texture)
            }

            if (var2 != textureSlots!![index]) {
                textureSlots!![index] = var2
                OpenGL.glUniform1i(index, var2)
            }
        }
    }

    override fun addPositionAttribute3f(i_1: Int, f_2: Float, f_3: Float, f_4: Float) {
        OpenGL.glUniform3f(i_1, f_2, f_3, f_4)
    }

    override fun addPositionAttribute4f(i_1: Int, f_2: Float, f_3: Float, f_4: Float, f_5: Float) {
        OpenGL.glUniform4f(i_1, f_2, f_3, f_4, f_5)
    }

    override fun addPreloadedPositionAttribute(class282Sub21Sub11: ShaderAttribute, f_2: Float, f_3: Float, f_4: Float, f_5: Float) {
        val i_6 = (class282Sub21Sub11 as ShaderAttributeGL).getActiveUniformLocation()
        if (i_6 != -1 && (aFloatArray9429!![i_6] != f_2 || aFloatArray9422!![i_6] != f_3 || aFloatArray9431!![i_6] != f_4 || aFloatArray9432!![i_6] != f_5)) {
            aFloatArray9429!![i_6] = f_2
            aFloatArray9422!![i_6] = f_3
            aFloatArray9431!![i_6] = f_4
            aFloatArray9432!![i_6] = f_5
            if (class282Sub21Sub11.method15457() != Class111.aClass1111134) {
                throw IllegalShaderArgumentException()
            }

            OpenGL.glUniform4f(i_6, f_2, f_3, f_4, f_5)
        }
    }

    override fun uniform3fSavedSize12(i_1: Int, matrix442: Matrix44) {
        OpenGL.glUniform3fv(i_1, 3, matrix442.copy3x3NearMatrixInto(Companion.aFloatArray9427), 0)
    }

    override fun uniform3fSavedSize8(i_1: Int, matrix442: Matrix44) {
        OpenGL.glUniform2fv(i_1, 4, matrix442.getComponenetsXY(Companion.aFloatArray9427), 0)
    }

    override fun uniform3fSavedSize16(location: Int, matrix4x4: Matrix44) {
        OpenGL.glUniform4fv(location, 4, matrix4x4.copyInto(Companion.aFloatArray9427), 0)
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
        if (aBool9425) {
            return true
        }
        toolkit!!.activeShaderMapping = null
        vertexShaderHandle = method14671(GL_VERTEX_SHADER, vertexShaderProgramString)
        fragmentShaderHandle = method14671(GL_FRAGMENT_SHADER, fragmentShaderProgramString)
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
                    var i_3 = 0
                    var class1116: Class111
                    for (variableIndex in 0 until openglShader!!.getPrimaryShaderVariableCount()) {
                        val shaderAttribute = openglShader!!.getShaderAttribute(variableIndex)
                        if (shaderAttribute != null) {
                            shaderAttribute.loadUniformLocation(activeInSlot)
                            class1116 = shaderAttribute.method15457()
                            val i_7 = (shaderAttribute as ShaderAttributeGL).uniformLocations!![activeInSlot]
                            if (class1116 != Class111.aClass1111219 && class1116 != Class111.aClass1111182 && class1116 != Class111.aClass1111132 && class1116 != Class111.aClass1111133 && class1116 != Class111.aClass1111134) {
                                i_3 = Math.max(i_7, i_3)
                            } else {
                                slot = Math.max(i_7, slot)
                            }
                        }
                    }

                    for (variableIndex in 0 until openglShader!!.getSecondaryShaderVariableCount()) {
                        val shaderAttribute = openglShader!!.getSecondaryAttribute(variableIndex)
                        if (shaderAttribute != null) {
                            shaderAttribute.loadUniformLocation(activeInSlot)
                            class1116 = shaderAttribute.method15457()
                            val i_7 = (shaderAttribute as ShaderAttributeGL).uniformLocations!![activeInSlot]
                            if (class1116 != Class111.aClass1111219 && class1116 != Class111.aClass1111182 && class1116 != Class111.aClass1111132 && class1116 != Class111.aClass1111133 && class1116 != Class111.aClass1111134) {
                                i_3 = Math.max(i_7, i_3)
                            } else {
                                slot = Math.max(i_7, slot)
                            }
                        }
                    }

                    aFloatArray9429 = FloatArray(slot + 1)
                    aFloatArray9422 = FloatArray(slot + 1)
                    aFloatArray9431 = FloatArray(slot + 1)
                    aFloatArray9432 = FloatArray(slot + 1)
                    textureSlots = IntArray(i_3 + 1)

                    for (index in textureSlots!!.indices) {
                        textureSlots!![index] = -1
                    }

                    vertexShaderProgramString = null
                    fragmentShaderProgramString = null
                    aBool9425 = true
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
        val aFloatArray9427 = FloatArray(16)
    }
}
