package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping
import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.unidentified.Class111
import com.jagex.game.runetek5.utils.ArrayUtils
import jagdx.IDirect3DDevice
import jagdx.IUnknown

class ShaderMappingDX(
    nativeToolkit: DirectXNativeToolkit,
    programBuilder: NativeShaderMapping,
    private val activeShader: ShaderDX? = null
) : ShaderMapping(), AutoCloseable {

    var isBuild: Boolean = false
    val programVariables: Array<FloatArray> = Array(2) { FloatArray(0) }
    val variableLoadedIntoProgram: BooleanArray = BooleanArray(2)
    var vertexShaderData: ByteArray? = null
    var fragmentShaderData: ByteArray? = null
    var vertexShaderHandle: Long = 0L
    var fragmentShaderHandle: Long = 0L
    var activeToolkit: DirectXNativeToolkit

    init {
        this.isBuild = false
        shaderName = programBuilder.mappingName

        if (programBuilder.vertexShaderName != null) {
            vertexShaderName = programBuilder.vertexShaderName
            vertexShaderData = nativeToolkit.getShaderData(vertexShaderName!!)
        }

        if (programBuilder.fragmentShaderName != null) {
            fragmentShaderName = programBuilder.fragmentShaderName
            fragmentShaderData = nativeToolkit.getShaderData(fragmentShaderName!!)
        }

        this.activeToolkit = nativeToolkit
        nativeToolkit.addWeakReference(this)
    }

    override fun uniform1f(shaderAttribute: ShaderAttribute, f: Float) {
        var index3 = (shaderAttribute as ShaderAttributeDX).method15782()
        var index4 = shaderAttribute.method15783()

        if (shaderAttribute.method15457() != Class111.aClass1111219 && shaderAttribute.method15457() != Class111.aClass1111182) {
            throw IllegalShaderArgumentException()
        } else {
            if (index3 >= 0) {
                index3 *= 4
                programVariables[0][index3] = f
                variableLoadedIntoProgram[0] = true
            }

            if (index4 >= 0) {
                index4 *= 4
                programVariables[1][index4] = f
                variableLoadedIntoProgram[1] = true
            }
        }
    }

    override fun unform2f(shaderAttribute: ShaderAttribute, f2: Float, f3: Float) {
        var index4 = (shaderAttribute as ShaderAttributeDX).method15782()
        var index5 = shaderAttribute.method15783()

        if (shaderAttribute.method15457() != Class111.aClass1111132) {
            throw IllegalShaderArgumentException()
        }

        if (index4 >= 0) {
            index4 *= 4
            programVariables[0][index4] = f2
            programVariables[0][index4 + 1] = f3
            variableLoadedIntoProgram[0] = true
        }

        if (index5 >= 0) {
            index5 *= 4
            programVariables[1][index5] = f2
            programVariables[1][index5 + 1] = f3
            variableLoadedIntoProgram[1] = true
        }
    }

    override fun initializeTextureSampler(i1: Int, i2: Int, textureSampler: TextureSampler) {
        activeToolkit.setTextureSlot(i2)
        activeToolkit.setTextureSampler(textureSampler)
    }

    override fun uniform2fv(shaderAttribute: ShaderAttribute, matrix: Matrix44) {
        var index3 = (shaderAttribute as ShaderAttributeDX).method15782()
        var index4 = shaderAttribute.method15783()

        if (shaderAttribute.method15457() != Class111.aClass1111168) {
            throw IllegalShaderArgumentException()
        } else {
            if (index3 >= 0) {
                method14589(index3, matrix.copyXYInto(MATRIX), 8)
            }

            if (index4 >= 0) {
                method14590(index4, matrix.copyXYInto(MATRIX), 8)
            }
        }
    }

    override fun uniform4fv(shaderAttribute: ShaderAttribute, matrix: Matrix44) {
        var index3 = (shaderAttribute as ShaderAttributeDX).method15782()
        var index4 = shaderAttribute.method15783()

        if (shaderAttribute.method15457() != Class111.aClass1111150) {
            throw IllegalShaderArgumentException()
        }

        if (index3 >= 0) {
            method14589(index3, matrix.copyFullyInto(MATRIX), 16)
        }

        if (index4 >= 0) {
            method14590(index4, matrix.copyFullyInto(MATRIX), 16)
        }
    }

    override fun addPositionAttribute3f(i1: Int, f2: Float, f3: Float, f4: Float) {
        val i5 = i1 shr 16
        val i6 = (i1 and 0xffff) * 4
        programVariables[i5][i6] = f2
        programVariables[i5][i6 + 1] = f3
        programVariables[i5][i6 + 2] = f4
        variableLoadedIntoProgram[i5] = true
    }

    override fun addPositionAttribute4f(i1: Int, f2: Float, f3: Float, f4: Float, f5: Float) {
        val i6 = i1 shr 16
        val i7 = (i1 and 0xffff) * 4
        programVariables[i6][i7] = f2
        programVariables[i6][i7 + 1] = f3
        programVariables[i6][i7 + 2] = f4
        programVariables[i6][i7 + 3] = f5
        variableLoadedIntoProgram[i6] = true
    }

    override fun uniform3fSavedSize12(i1: Int, matrix: Matrix44) {
        val i3 = i1 shr 16
        val i4 = (i1 and 0xffff) * 4
        System.arraycopy(matrix.copyFullyInto(MATRIX), 0, programVariables[i3], i4, 12)
        variableLoadedIntoProgram[i3] = true
    }

    override fun uniform3fSavedSize8(i1: Int, matrix: Matrix44) {
        val i3 = i1 shr 16
        val i4 = (i1 and 0xffff) * 4
        System.arraycopy(matrix.copyXYInto(MATRIX), 0, programVariables[i3], i4, 8)
        variableLoadedIntoProgram[i3] = true
    }

    override fun uniform3fSavedSize16(i1: Int, matrix: Matrix44) {
        val i3 = i1 shr 16
        val i4 = (i1 and 0xffff) * 4
        System.arraycopy(matrix.copyFullyInto(MATRIX), 0, programVariables[i3], i4, 16)
        variableLoadedIntoProgram[i3] = true
    }

    override fun addPositionAttributeDynamic(i1: Int, floats: FloatArray, i3: Int) {
        val i4 = i1 shr 16
        val i5 = (i1 and 0xffff) * 4
        System.arraycopy(floats, 0, programVariables[i4], i5, i3)
        variableLoadedIntoProgram[i4] = true
    }

    fun method14589(i1: Int, floats: FloatArray, i3: Int) {
        ArrayUtils.arrayCopy(floats, 0, programVariables[0], i1 * 4, i3)
        variableLoadedIntoProgram[0] = true
    }

    fun method14590(i1: Int, floats: FloatArray, i3: Int) {
        ArrayUtils.arrayCopy(floats, 0, programVariables[1], i1 * 4, i3)
        variableLoadedIntoProgram[1] = true
    }

    fun method14591() {
        if (variableLoadedIntoProgram[0]) {
            for (i in programVariables[0].indices) {
                activeToolkit.theUnsafe.putFloat(activeToolkit.directBufferAddress + (i * 4L), programVariables[0][i])
            }
            IDirect3DDevice.SetVertexShaderConstantF(activeToolkit.renderTarget, 0, activeToolkit.directBufferAddress, programVariables[0].size / 4)
            variableLoadedIntoProgram[0] = false
        }

        if (variableLoadedIntoProgram[1]) {
            for (i in programVariables[1].indices) {
                activeToolkit.theUnsafe.putFloat(activeToolkit.directBufferAddress + (i * 4L), programVariables[1][i])
            }
            IDirect3DDevice.SetPixelShaderConstantF(activeToolkit.renderTarget, 0, activeToolkit.directBufferAddress, programVariables[1].size / 4)
            variableLoadedIntoProgram[1] = false
        }
    }

    override fun deallocate() {
        if (vertexShaderHandle != 0L) {
            IUnknown.Release(vertexShaderHandle)
            vertexShaderHandle = 0L
        }

        if (fragmentShaderHandle != 0L) {
            IUnknown.Release(fragmentShaderHandle)
            fragmentShaderHandle = 0L
        }

        activeToolkit.clearReference(this)
    }

    fun method14592() {
        if (vertexShaderHandle != 0L) {
            activeToolkit.requestHandleCleanup(vertexShaderHandle)
            vertexShaderHandle = 0L
        }

        if (fragmentShaderHandle != 0L) {
            activeToolkit.requestHandleCleanup(fragmentShaderHandle)
            fragmentShaderHandle = 0L
        }
    }

    override fun close() {
        method14592()
    }

    override fun uniform4fv(shaderAttribute: ShaderAttribute, floats: FloatArray, i3: Int) {
        var index4 = (shaderAttribute as ShaderAttributeDX).method15782()
        var index5 = shaderAttribute.method15783()

        if (shaderAttribute.method15457() != Class111.aClass1111109) {
            throw IllegalShaderArgumentException()
        } else {
            if (index4 >= 0) {
                method14589(index4, floats, i3)
            }

            if (index5 >= 0) {
                method14590(index5, floats, i3)
            }
        }
    }

    override fun addPreloadedPositionAttribute(shaderAttribute: ShaderAttribute, f2: Float, f3: Float, f4: Float, f5: Float) {
        var index6 = (shaderAttribute as ShaderAttributeDX).method15782()
        var index7 = shaderAttribute.method15783()

        if (shaderAttribute.method15457() != Class111.aClass1111134) {
            throw IllegalShaderArgumentException()
        }

        if (index6 >= 0) {
            index6 *= 4
            programVariables[0][index6] = f2
            programVariables[0][index6 + 1] = f3
            programVariables[0][index6 + 2] = f4
            programVariables[0][index6 + 3] = f5
            variableLoadedIntoProgram[0] = true
        }

        if (index7 >= 0) {
            index7 *= 4
            programVariables[1][index7] = f2
            programVariables[1][index7 + 1] = f3
            programVariables[1][index7 + 2] = f4
            programVariables[1][index7 + 3] = f5
            variableLoadedIntoProgram[1] = true
        }
    }

    override fun uniform3f(shaderAttribute: ShaderAttribute, f2: Float, f3: Float, f4: Float) {
        var index5 = (shaderAttribute as ShaderAttributeDX).method15782()
        var index6 = shaderAttribute.method15783()

        if (shaderAttribute.method15457() != Class111.aClass1111133) {
            throw IllegalShaderArgumentException()
        }

        if (index5 >= 0) {
            index5 *= 4
            programVariables[0][index5] = f2
            programVariables[0][index5 + 1] = f3
            programVariables[0][index5 + 2] = f4
            variableLoadedIntoProgram[0] = true
        }

        if (index6 >= 0) {
            index6 *= 4
            programVariables[1][index6] = f2
            programVariables[1][index6 + 1] = f3
            programVariables[1][index6 + 2] = f4
            variableLoadedIntoProgram[1] = true
        }
    }

    override fun createShaders(): Boolean {
        if (isBuild) {
            return true
        }

        vertexShaderHandle = if (vertexShaderData == null) 0L else IDirect3DDevice.CreateVertexShader(activeToolkit.renderTarget, vertexShaderData!!)
        fragmentShaderHandle = if (fragmentShaderData == null) 0L else IDirect3DDevice.CreatePixelShader(activeToolkit.renderTarget, fragmentShaderData!!)

        if (vertexShaderHandle == 0L && fragmentShaderHandle == 0L) {
            return false
        }

        var slotInUse = -1
        for (slot in 0 until activeShader!!.getMappingCount()) {
            if (activeShader.getMapping(slot) === this) {
                slotInUse = slot
                break
            }
        }

        val i2 = activeShader.primaryShaderVariableCount
        val i3 = activeShader.secondaryShaderVariableCount

        for (i4 in 0 until i2) {
            val shaderAttr = activeShader.getShaderAttribute(i4)
            shaderAttr.loadUniformLocation(slotInUse)
        }

        for (i4 in 0 until i3) {
            val shaderAttr = activeShader.getSecondaryAttribute(i4)
            shaderAttr.loadUniformLocation(slotInUse)
        }

        var vertexShaderVariableCount = 0
        var fragmentShaderVariableCount = 0
        val i6 = activeShader.getMappingSlot(this)

        for (i7 in 0 until i2 + i3) {
            var shaderAttrDX: ShaderAttributeDX = if (i7 < i2) {
                activeShader.getShaderAttribute(i7) as ShaderAttributeDX
            } else {
                activeShader.getSecondaryAttribute(i7 - i2) as ShaderAttributeDX
            }

            var class1119 = shaderAttrDX.method15457()
            var i10 = 1

            if (class1119 == Class111.aClass1111109) {
                class1119 = shaderAttrDX.method15458()
                i10 = shaderAttrDX.method15459()
            }

            when (class1119.anInt1232) {
                5, 26, 49, 64 -> {
                    i10 *= 3
                }
                8, 10, 54, 95 -> {
                    // No multiplier
                }
                16, 17, 56, 75 -> {
                    i10 *= 4
                }
                34, 41, 80, 94 -> {
                    i10 *= 2
                }
                else -> {
                    i10 = 0
                }
            }

            if (shaderAttrDX.anIntArray10306[i6] >= 0) {
                val i11 = i10 + shaderAttrDX.anIntArray10306[i6]
                if (vertexShaderVariableCount < i11) {
                    vertexShaderVariableCount = i11
                }
            }

            if (shaderAttrDX.anIntArray10307[i6] >= 0) {
                val i11 = i10 + shaderAttrDX.anIntArray10307[i6]
                if (fragmentShaderVariableCount < i11) {
                    fragmentShaderVariableCount = i11
                }
            }
        }

        programVariables[0] = FloatArray(vertexShaderVariableCount * 4)
        programVariables[1] = FloatArray(fragmentShaderVariableCount * 4)
        isBuild = true
        return true
    }

    override fun loadTextureSampler(shaderAttribute: ShaderAttribute, slot: Int, sampler: TextureSampler) {
        activeToolkit.setTextureSlot(slot)
        activeToolkit.setTextureSampler(sampler)
    }

    companion object {
        @JvmField
        val MATRIX = FloatArray(16)
    }
}
