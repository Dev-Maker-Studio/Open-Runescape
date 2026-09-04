package com.jagex.game.runetek5.config.quest

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.vardomain.VarDomain

/**
 *
 * @author Brainpower - 01/05/2022
 *
 */
class QuestType {

    @JvmField
    var name: String? = null

    @JvmField
    var sortName: String? = null

    @JvmField
    var type: Int = 0

    @JvmField
    var difficulty: Int = 0

    @JvmField
    var members: Boolean = false

    @JvmField
    var questPointReward: Int = 0

    @JvmField
    var questPrerequisiteIds: IntArray? = null

    @JvmField
    var levelRequirements: Array<IntArray>? = null

    @JvmField
    var questpointRequirement: Int = 0

    @JvmField
    var graphicId: Int = -1

    @JvmField
    var varpRequirements: IntArray? = null

    @JvmField
    var varpRequirementNames: Array<String>? = null

    @JvmField
    var varbitRequirements: IntArray? = null

    @JvmField
    var varbitRequirementNames: Array<String>? = null

    @JvmField
    var questTypelist: QuestTypeList? = null

    @JvmField
    var varpValues: Array<IntArray>? = null

    @JvmField
    var varbitValues: Array<IntArray>? = null

    @JvmField
    var unusedOpcode10: IntArray? = null

    @JvmField
    var minVarpValue: IntArray? = null

    @JvmField
    var maxVarpValue: IntArray? = null

    @JvmField
    var minVarbitValue: IntArray? = null

    @JvmField
    var maxVarbitValue: IntArray? = null

    @JvmField
    var params: IterableNodeMap<Node<*>>? = null

    fun decode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> {
                name = buffer.readJagString()
            }
            2 -> {
                sortName = buffer.readJagString()
            }
            3 -> {
                val count = buffer.readUByte()
                varpValues = Array(count) { IntArray(3) }
                for (index in 0 until count) {
                    varpValues!![index][0] = buffer.readUShort()
                    varpValues!![index][1] = buffer.readInt()
                    varpValues!![index][2] = buffer.readInt()
                }
            }
            4 -> {
                val count = buffer.readUByte()
                varbitValues = Array(count) { IntArray(3) }
                for (index in 0 until count) {
                    varbitValues!![index][0] = buffer.readUShort()
                    varbitValues!![index][1] = buffer.readInt()
                    varbitValues!![index][2] = buffer.readInt()
                }
            }
            5 -> {
                buffer.readUShort()
            }
            6 -> {
                type = buffer.readUByte()
            }
            7 -> {
                difficulty = buffer.readUByte()
            }
            8 -> {
                members = true
            }
            9 -> {
                questPointReward = buffer.readUByte()
            }
            10 -> {
                val count = buffer.readUByte()
                unusedOpcode10 = IntArray(count)
                for (index in 0 until count) {
                    unusedOpcode10!![index] = buffer.readInt()
                }
            }
            12 -> {
                buffer.readInt()
            }
            13 -> {
                val count = buffer.readUByte()
                questPrerequisiteIds = IntArray(count)
                for (index in 0 until count) {
                    questPrerequisiteIds!![index] = buffer.readUShort()
                }
            }
            14 -> {
                val count = buffer.readUByte()
                levelRequirements = Array(count) { IntArray(2) }
                for (index in 0 until count) {
                    levelRequirements!![index][0] = buffer.readUByte()
                    levelRequirements!![index][1] = buffer.readUByte()
                }
            }
            15 -> {
                questpointRequirement = buffer.readUShort()
            }
            17 -> {
                graphicId = buffer.readBigSmart()
            }
            18 -> {
                val count = buffer.readUByte()
                varpRequirements = IntArray(count)
                minVarpValue = IntArray(count)
                maxVarpValue = IntArray(count)
                varpRequirementNames = Array(count) { "" }
                for (index in 0 until count) {
                    varpRequirements!![index] = buffer.readInt()
                    minVarpValue!![index] = buffer.readInt()
                    maxVarpValue!![index] = buffer.readInt()
                    varpRequirementNames!![index] = buffer.readString()
                }
            }
            19 -> {
                val count = buffer.readUByte()
                varbitRequirements = IntArray(count)
                minVarbitValue = IntArray(count)
                maxVarbitValue = IntArray(count)
                varbitRequirementNames = Array(count) { "" }
                for (index in 0 until count) {
                    varbitRequirements!![index] = buffer.readInt()
                    minVarbitValue!![index] = buffer.readInt()
                    maxVarbitValue!![index] = buffer.readInt()
                    varbitRequirementNames!![index] = buffer.readString()
                }
            }
            249 -> {
                val count = buffer.readUByte()
                if (params == null) {
                    val mapSize = MathUtils.nextPowerOfTwo(count)
                    params = IterableNodeMap<Node<*>>(mapSize)
                }
                for (index in 0 until count) {
                    val isObject = buffer.readUByte() == 1
                    val key = buffer.readUMedium()
                    val value = if (isObject) {
                        ObjectNode(buffer.readString())
                    } else {
                        IntNode(buffer.readInt())
                    }
                    params!!.put(value, key.toLong())
                }
            }
        }
    }

    fun postDecode() {
        if (sortName == null) {
            sortName = name
        }
    }

    fun hasAllRequirements(varprovider: VarDomain, playerActualSkillLevels: IntArray): Boolean {
        if (questTypelist!!.getQuestpointsCompletedQuests(varprovider) < questpointRequirement) {
            return false
        }
        if (levelRequirements != null) {
            for (index in levelRequirements!!.indices) {
                if (playerActualSkillLevels[levelRequirements!![index][0]] < levelRequirements!![index][1]) {
                    return false
                }
            }
        }
        if (questPrerequisiteIds != null) {
            for (index in questPrerequisiteIds!!.indices) {
                if (!questTypelist!!.list(questPrerequisiteIds!![index]).isComplete(varprovider)) {
                    return false
                }
            }
        }
        if (varpRequirements != null) {
            for (index in varpRequirements!!.indices) {
                val varp = varprovider.getVarValue(varpRequirements!![index])
                if (varp < minVarpValue!![index] || varp > maxVarpValue!![index]) {
                    return false
                }
            }
        }
        if (varbitRequirements != null) {
            for (index in varbitRequirements!!.indices) {
                val varibit = varprovider.getVarBitValue(varbitRequirements!![index])
                if (varibit < minVarbitValue!![index] || varibit > maxVarbitValue!![index]) {
                    return false
                }
            }
        }
        return true
    }

    fun isStarted(provider: VarDomain): Boolean {
        if (varpValues != null) {
            for (index in varpValues!!.indices) {
                if (provider.getVarValue(varpValues!![index][0]) >= varpValues!![index][1]) {
                    return true
                }
            }
        }
        if (varbitValues != null) {
            for (index in varbitValues!!.indices) {
                if (provider.getVarBitValue(varbitValues!![index][0]) >= varbitValues!![index][1]) {
                    return true
                }
            }
        }
        return false
    }

    fun isComplete(varprovider: VarDomain): Boolean {
        if (varpValues != null) {
            for (index in varpValues!!.indices) {
                if (varprovider.getVarValue(varpValues!![index][0]) >= varpValues!![index][2]) {
                    return true
                }
            }
        }
        if (varbitValues != null) {
            for (index in varbitValues!!.indices) {
                if (varprovider.getVarBitValue(varbitValues!![index][0]) >= varbitValues!![index][2]) {
                    return true
                }
            }
        }
        return false
    }

    fun hasQuestpointRequirement(varprovider: VarDomain): Boolean {
        return questTypelist!!.getQuestpointsCompletedQuests(varprovider) >= questpointRequirement
    }

    fun hasPrerequisiteRequirement(varprovider: VarDomain, prereqIndex: Int): Boolean {
        return (questPrerequisiteIds != null && prereqIndex >= 0 && prereqIndex < questPrerequisiteIds!!.size)
                && questTypelist!!.list(questPrerequisiteIds!![prereqIndex]).isComplete(varprovider)
    }

    fun hasVarpRequirement(varprovider: VarDomain, varpIndex: Int): Boolean {
        if (varpRequirements != null && varpIndex >= 0 && varpIndex < varpRequirements!!.size) {
            val varp = varprovider.getVarValue(varpRequirements!![varpIndex])
            return varp >= minVarpValue!![varpIndex] && varp <= maxVarpValue!![varpIndex]
        }
        return false
    }

    fun hasVarbitRequirement(varprovider: VarDomain, varbitIndex: Int): Boolean {
        if (varbitRequirements != null && varbitIndex >= 0 && varbitIndex < varbitRequirements!!.size) {
            val varbit = varprovider.getVarBitValue(varbitRequirements!![varbitIndex])
            return varbit >= minVarbitValue!![varbitIndex] && varbit <= maxVarbitValue!![varbitIndex]
        }
        return false
    }

    fun getParamstring(key: Int, defaultString: String?): String? {
        if (params == null) {
            return defaultString
        }
        val valueObject = params!!.get(key.toLong()) as ObjectNode?
        return if (valueObject == null) defaultString else valueObject.value as String?
    }

    fun hasLevelRequirement(actualPlayerLevels: IntArray, skillReqIndex: Int): Boolean {
        return (levelRequirements != null && skillReqIndex >= 0 && skillReqIndex < levelRequirements!!.size)
                && actualPlayerLevels[levelRequirements!![skillReqIndex][0]] >= levelRequirements!![skillReqIndex][1]
    }

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    fun getParamint(key: Int, defaultValue: Int): Int {
        if (params == null) {
            return defaultValue
        }
        val valueObject = params!!.get(key.toLong()) as IntNode?
        return valueObject?.value ?: defaultValue
    }
}
