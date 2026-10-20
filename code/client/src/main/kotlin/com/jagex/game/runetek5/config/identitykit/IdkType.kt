package com.jagex.game.runetek5.config.identitykit

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.model.Mesh

class IdkType {

    @JvmField var identityTypeList: IdkTypeList? = null
    @JvmField var bodyModels: IntArray? = null
    @JvmField var recolorSrc: ShortArray? = null
    @JvmField var recolorDst: ShortArray? = null
    @JvmField var retexSrc: ShortArray? = null
    @JvmField var retexDst: ShortArray? = null
    @JvmField val headModels = intArrayOf(-1, -1, -1, -1, -1)

    fun getHeadMesh(): Mesh {
        val meshes = arrayOfNulls<Mesh>(5)
        var meshCount = 0
        synchronized(identityTypeList!!.modelResource) {
            for (modelIndex in 0 until 5) {
                if (headModels[modelIndex] != -1) {
                    meshes[meshCount++] = Mesh.fromResource(identityTypeList!!.modelResource, headModels[modelIndex])
                }
            }
        }
        for (meshIndex in 0 until 5) {
            if (meshes[meshIndex] != null && meshes[meshIndex]!!.version < 13) {
                meshes[meshIndex]!!.upscale()
            }
        }
        val mesh = Mesh(meshes, meshCount)
        if (recolorSrc != null) {
            for (colorIndex in recolorSrc!!.indices) {
                mesh.recolor(recolorSrc!![colorIndex], recolorDst!![colorIndex])
            }
        }
        if (retexSrc != null) {
            for (textureIndex in retexSrc!!.indices) {
                mesh.retexture(retexSrc!![textureIndex], retexDst!![textureIndex])
            }
        }
        return mesh
    }

    fun decode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> buffer.readUByte()
            2 -> {
                val count = buffer.readUByte()
                bodyModels = IntArray(count)
                for (modelIndex in 0 until count) {
                    bodyModels!![modelIndex] = buffer.readBigSmart()
                }
            }
            40 -> {
                val count = buffer.readUByte()
                recolorSrc = ShortArray(count)
                recolorDst = ShortArray(count)
                for (colorIndex in 0 until count) {
                    recolorSrc!![colorIndex] = buffer.readUShort().toShort()
                    recolorDst!![colorIndex] = buffer.readUShort().toShort()
                }
            }
            41 -> {
                val count = buffer.readUByte()
                retexSrc = ShortArray(count)
                retexDst = ShortArray(count)
                for (textureIndex in 0 until count) {
                    retexSrc!![textureIndex] = buffer.readUShort().toShort()
                    retexDst!![textureIndex] = buffer.readUShort().toShort()
                }
            }
            in 60..69 -> {
                headModels[opcode - 60] = buffer.readBigSmart()
            }
        }
    }

    fun isBodyCached(): Boolean {
        if (bodyModels == null) {
            return true
        }
        var fullyCached = true
        synchronized(identityTypeList!!.modelResource) {
            for (modelIndex in bodyModels!!.indices) {
                if (!identityTypeList!!.modelResource.isCached(bodyModels!![modelIndex], 0)) {
                    fullyCached = false
                }
            }
            return fullyCached
        }
    }

    fun getBodyMesh(): Mesh? {
        if (bodyModels == null) {
            return null
        }
        val meshes = arrayOfNulls<Mesh>(bodyModels!!.size)
        synchronized(identityTypeList!!.modelResource) {
            for (meshIndex in bodyModels!!.indices) {
                meshes[meshIndex] = Mesh.fromResource(identityTypeList!!.modelResource, bodyModels!![meshIndex])
            }
        }
        for (modelIndex in bodyModels!!.indices) {
            if (meshes[modelIndex]!!.version < 13) {
                meshes[modelIndex]!!.upscale()
            }
        }
        val mesh: Mesh = if (meshes.size == 1) {
            meshes[0]!!
        } else {
            Mesh(meshes, meshes.size)
        }
        if (recolorSrc != null) {
            for (colorIndex in recolorSrc!!.indices) {
                mesh.recolor(recolorSrc!![colorIndex], recolorDst!![colorIndex])
            }
        }
        if (retexSrc != null) {
            for (textureIndex in retexSrc!!.indices) {
                mesh.retexture(retexSrc!![textureIndex], retexDst!![textureIndex])
            }
        }
        return mesh
    }

    fun isHeadCached(): Boolean {
        var fullyCached = true
        synchronized(identityTypeList!!.modelResource) {
            for (modelIndex in 0 until 5) {
                if (headModels[modelIndex] != -1 && !identityTypeList!!.modelResource.isCached(headModels[modelIndex], 0)) {
                    fullyCached = false
                }
            }
            return fullyCached
        }
    }

    internal fun decode(stream: Packet) {
        while (true) {
            val opcode = stream.readUByte()
            if (opcode == 0) {
                return
            }
            decode(stream, opcode)
        }
    }
}
