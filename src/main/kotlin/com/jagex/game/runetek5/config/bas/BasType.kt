package com.jagex.game.runetek5.config.bas

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.utils.MathUtils

class BasType {

    @JvmField
    var standSequenceId: Int = -1
    @JvmField
    var walkSequence: Int = -1
    @JvmField
    var teleportSequence: Int = -1
    @JvmField
    var teleDir3: Int = -1
    @JvmField
    var teleDir2: Int = -1
    @JvmField
    var teleDir1: Int = -1
    @JvmField
    var runningSequence: Int = -1
    @JvmField
    var runDir3: Int = -1
    @JvmField
    var runDir2: Int = -1
    @JvmField
    var runDir1: Int = -1
    @JvmField
    var anInt2786: Int = 0
    @JvmField
    var anInt2829: Int = 0
    @JvmField
    var objVisibility: IntArray? = null

    /**
     * [0] translate x
     * [1] translate y
     * [2] translate z
     * [3] rotate x
     * [4] rotate y
     * [5] rotate z
     */
    @JvmField
    var objVerticeTransformations: Array<IntArray?>? = null

    @JvmField
    var anInt2820: Int = 0
    @JvmField
    var anInt2804: Int = 0
    @JvmField
    var anInt2825: Int = 0
    @JvmField
    var anInt2823: Int = 0
    @JvmField
    var anInt2824: Int = 0
    @JvmField
    var anInt2816: Int = 0
    @JvmField
    var anInt2815: Int = 0
    @JvmField
    var anInt2827: Int = 0
    @JvmField
    var unused: Int = -1
    @JvmField
    var standTurnCcwSequence: Int = -1
    @JvmField
    var standTurnCwSequence: Int = -1
    @JvmField
    var walkDir3: Int = -1
    @JvmField
    var walkDir2: Int = -1
    @JvmField
    var walkDir1: Int = -1
    @JvmField
    var anInt2798: Int = -1
    @JvmField
    var teleTurnCcwSequence: Int = -1
    @JvmField
    var teleTurnCwSequence: Int = -1
    @JvmField
    var runTurnCcwSequence: Int = -1
    @JvmField
    var runTurnCwSequence: Int = -1
    @JvmField
    var walkTurnCcwSequence: Int = -1
    @JvmField
    var walkTurnCwSequence: Int = -1
    @JvmField
    var randomStandSequences: IntArray? = null
    @JvmField
    var randomStandSequenceChances: IntArray? = null
    @JvmField
    var rendersShadow: Boolean = true
    @JvmField
    var anInt2813: Int = 0
    @JvmField
    var anInt2790: Int = 0
    @JvmField
    var turnAngleAdjustment: IntArray? = null
    @JvmField
    var projectionOffset: Array<IntArray?>? = null
    @JvmField
    var basTypeList: BasTypeList? = null
    @JvmField
    var totalCache: Int = 0
    @JvmField
    var matrices: Array<Matrix43?>? = null

    fun decode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> {
                this.standSequenceId = buffer.readBigSmart()
                this.walkSequence = buffer.readBigSmart()
            }
            2 -> this.teleportSequence = buffer.readBigSmart()
            3 -> this.teleDir3 = buffer.readBigSmart()
            4 -> this.teleDir2 = buffer.readBigSmart()
            5 -> this.teleDir1 = buffer.readBigSmart()
            6 -> this.runningSequence = buffer.readBigSmart()
            7 -> this.runDir3 = buffer.readBigSmart()
            8 -> this.runDir2 = buffer.readBigSmart()
            9 -> this.runDir1 = buffer.readBigSmart()
            26 -> {
                this.anInt2786 = (buffer.readUByte() * 4).toShort().toInt()
                this.anInt2829 = (buffer.readUByte() * 4).toShort().toInt()
            }
            27 -> {
                if (this.objVerticeTransformations == null) {
                    this.objVerticeTransformations = arrayOfNulls(this.basTypeList!!.equipmentDefaults.customizableObjSlots!!.size)
                }
                val slot = buffer.readUByte()
                this.objVerticeTransformations!![slot] = IntArray(6)
                for (transformation in 0..5) {
                    this.objVerticeTransformations!![slot]!![transformation] = buffer.readShort()
                }
            }
            28 -> {
                val slotCount = buffer.readUByte()
                this.objVisibility = IntArray(slotCount)
                for (index in 0 until slotCount) {
                    this.objVisibility!![index] = buffer.readUByte()
                    if (this.objVisibility!![index] == 255) {
                        this.objVisibility!![index] = -1
                    }
                }
            }
            29 -> this.anInt2820 = buffer.readUByte()
            30 -> this.anInt2804 = buffer.readUShort()
            31 -> this.anInt2825 = buffer.readUByte()
            32 -> this.anInt2823 = buffer.readUShort()
            33 -> this.anInt2824 = buffer.readShort()
            34 -> this.anInt2816 = buffer.readUByte()
            35 -> this.anInt2815 = buffer.readUShort()
            36 -> this.anInt2827 = buffer.readShort()
            37 -> this.unused = buffer.readUByte()
            38 -> this.standTurnCcwSequence = buffer.readBigSmart()
            39 -> this.standTurnCwSequence = buffer.readBigSmart()
            40 -> this.walkDir3 = buffer.readBigSmart()
            41 -> this.walkDir2 = buffer.readBigSmart()
            42 -> this.walkDir1 = buffer.readBigSmart()
            43 -> buffer.readUShort()
            44 -> buffer.readUShort()
            45 -> this.anInt2798 = buffer.readUShort()
            46 -> this.teleTurnCcwSequence = buffer.readBigSmart()
            47 -> this.teleTurnCwSequence = buffer.readBigSmart()
            48 -> this.runTurnCcwSequence = buffer.readBigSmart()
            49 -> this.runTurnCwSequence = buffer.readBigSmart()
            50 -> this.walkTurnCcwSequence = buffer.readBigSmart()
            51 -> this.walkTurnCwSequence = buffer.readBigSmart()
            52 -> {
                val standCount = buffer.readUByte()
                this.randomStandSequences = IntArray(standCount)
                this.randomStandSequenceChances = IntArray(standCount)
                for (index in 0 until standCount) {
                    this.randomStandSequences!![index] = buffer.readBigSmart()
                    val chance = buffer.readUByte()
                    this.randomStandSequenceChances!![index] = chance
                    this.totalCache += chance
                }
            }
            53 -> this.rendersShadow = false
            54 -> {
                this.anInt2813 = buffer.readUByte() shl 6
                this.anInt2790 = buffer.readUByte() shl 6
            }
            55 -> {
                if (this.turnAngleAdjustment == null) {
                    this.turnAngleAdjustment = IntArray(this.basTypeList!!.equipmentDefaults.customizableObjSlots!!.size)
                }
                val i_4 = buffer.readUByte()
                this.turnAngleAdjustment!![i_4] = buffer.readUShort()
            }
            56 -> {
                if (this.projectionOffset == null) {
                    this.projectionOffset = arrayOfNulls(this.basTypeList!!.equipmentDefaults.customizableObjSlots!!.size)
                }
                val slot = buffer.readUByte()
                this.projectionOffset!![slot] = IntArray(3)
                for (direction in 0..2) {
                    this.projectionOffset!![slot]!![direction] = buffer.readShort()
                }
            }
        }
    }

    fun getStandSequence(): Int {
        if (this.standSequenceId != -1) {
            return this.standSequenceId
        }
        if (this.randomStandSequences == null) {
            return -1
        }
        var chance = (Math.random() * this.totalCache).toInt()
        var sequenceIndex = 0
        while (chance >= this.randomStandSequenceChances!![sequenceIndex]) {
            chance -= this.randomStandSequenceChances!![sequenceIndex]
            sequenceIndex++
        }
        return this.randomStandSequences!![sequenceIndex]
    }

    fun isStandAnimation(sequenceId: Int): Boolean {
        if (sequenceId == -1) {
            return false
        }
        if (this.standSequenceId == sequenceId) {
            return true
        }
        if (this.randomStandSequences != null) {
            for (index in this.randomStandSequences!!.indices) {
                if (this.randomStandSequences!![index] == sequenceId) {
                    return true
                }
            }
        }
        return false
    }

    fun getSubmeshMatrices(): Array<Matrix43?>? {
        if (this.matrices != null) {
            return this.matrices
        }
        if (this.objVerticeTransformations == null) {
            return null
        }
        this.matrices = arrayOfNulls(this.objVerticeTransformations!!.size)
        for (index in this.objVerticeTransformations!!.indices) {
            var offsetX = 0
            var offsetY = 0
            var offsetZ = 0
            var rotationX = 0
            var rotationY = 0
            var rotationZ = 0
            if (this.objVerticeTransformations!![index] != null) {
                offsetX = this.objVerticeTransformations!![index]!![0]
                offsetY = this.objVerticeTransformations!![index]!![1]
                offsetZ = this.objVerticeTransformations!![index]!![2]
                rotationX = this.objVerticeTransformations!![index]!![3] shl 3
                rotationY = this.objVerticeTransformations!![index]!![4] shl 3
                rotationZ = this.objVerticeTransformations!![index]!![5] shl 3
            }
            if (offsetX != 0 || offsetY != 0 || offsetZ != 0 || rotationX != 0 || rotationY != 0 || rotationZ != 0) {
                val matrix = Matrix43()
                if (rotationZ != 0) {
                    matrix.rotate(0.0f, 0.0f, 1.0f, MathUtils.getTheta(rotationZ))
                }
                if (rotationX != 0) {
                    matrix.rotate(1.0f, 0.0f, 0.0f, MathUtils.getTheta(rotationX))
                }
                if (rotationY != 0) {
                    matrix.rotate(0.0f, 1.0f, 0.0f, MathUtils.getTheta(rotationY))
                }
                matrix.translate(offsetX.toFloat(), offsetY.toFloat(), offsetZ.toFloat())
                this.matrices!![index] = matrix
            }
        }
        return this.matrices
    }

    fun decode(stream: Packet) {
        while (true) {
            val opcode = stream.readUByte()
            if (opcode == 0) {
                return
            }
            decode(stream, opcode)
        }
    }

    fun getAnimations(): IntArray {
        val sequenceMap = IterableNodeMap<Node<*>>(16)
        addEntry(this.standSequenceId, sequenceMap)
        if (this.randomStandSequences != null) {
            val standSeqs = this.randomStandSequences!!
            for (index in standSeqs.indices) {
                val standSeq = standSeqs[index]
                addEntry(standSeq, sequenceMap)
            }
        }
        addEntry(this.standTurnCcwSequence, sequenceMap)
        addEntry(this.standTurnCwSequence, sequenceMap)
        addEntry(this.walkSequence, sequenceMap)
        addEntry(this.walkDir3, sequenceMap)
        addEntry(this.walkDir2, sequenceMap)
        addEntry(this.walkDir1, sequenceMap)
        addEntry(this.runningSequence, sequenceMap)
        addEntry(this.runDir3, sequenceMap)
        addEntry(this.runDir2, sequenceMap)
        addEntry(this.runDir1, sequenceMap)
        addEntry(this.teleportSequence, sequenceMap)
        addEntry(this.teleDir3, sequenceMap)
        addEntry(this.teleDir2, sequenceMap)
        addEntry(this.teleDir1, sequenceMap)
        addEntry(this.teleTurnCcwSequence, sequenceMap)
        addEntry(this.teleTurnCwSequence, sequenceMap)
        addEntry(this.runTurnCcwSequence, sequenceMap)
        addEntry(this.runTurnCwSequence, sequenceMap)
        addEntry(this.walkTurnCcwSequence, sequenceMap)
        addEntry(this.walkTurnCwSequence, sequenceMap)
        val sequences = IntArray(sequenceMap.size())
        var count = 0
        val iterator = sequenceMap.iterator()
        while (iterator.hasNext()) {
            val node = iterator.next()
            sequences[count++] = node.pointer.toInt()
        }
        return sequences
    }

    companion object {
        @JvmStatic
        fun addEntry(id: Int, map: IterableNodeMap<Node<*>>) {
            if (id != -1 && map.get(id.toLong()) == null) {
                map.put(Node<Node<*>>(), id.toLong())
            }
        }
    }
}
