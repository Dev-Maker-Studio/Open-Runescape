package com.jagex.game.runetek5.interaction.chat.clanchat.settings

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.LongNode
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.utils.ArrayUtils
import com.jagex.game.runetek5.utils.MathUtils

class ClanSettings(buffer: Packet) {

    @JvmField var currentOwner = -1
    @JvmField var replacementOwner = -1
    @JvmField var banCount = 0
    @JvmField var bannedUserNames: Array<String>? = null
    @JvmField var memberCount = 0
    @JvmField var clanName: String? = null
    @JvmField var memberNames: Array<String>? = null
    @JvmField var memberRanks: ByteArray? = null
    @JvmField var memberJoinDays: IntArray? = null
    @JvmField var allowGuests = false
    @JvmField var talkRank: Byte = 0
    @JvmField var kickRank: Byte = 0
    @JvmField var lootshareRank: Byte = 0
    @JvmField var coinShare: Byte = 0
    @JvmField var deltaVersion: Long = 0
    @JvmField var bannedUserHashes: LongArray? = null
    @JvmField var variables: IterableNodeMap<Node<*>>? = null
    @JvmField var sortedAffinedSlots: IntArray? = null
    @JvmField var updateCount = 0
    @JvmField var creationDay = 0
    @JvmField var memberHashes: LongArray? = null
    @JvmField var clanFlags: IntArray? = null
    @JvmField var useHashes = false
    @JvmField var useNames = false

    init {
        decode(buffer)
    }

    fun getMemberIndex(memberName: String?): Int {
        if (memberName != null && memberName.isNotEmpty()) {
            for (index in 0 until memberCount) {
                if (memberNames!![index] == memberName) {
                    return index
                }
            }
        }
        return -1
    }

    // varbit
    fun getClanFlagBitfield(i1: Int, i2: Int, shift: Int): Int {
        val mask = if (shift == 31) -1 else (1 shl (shift + 1)) - 1
        return (clanFlags!![i1] and mask) ushr i2
    }

    fun getIntVar(key: Int): Int? {
        if (variables == null) {
            return null
        }
        val node3 = variables!!.get(key.toLong())
        return if (node3 is IntNode) node3.value else null
    }

    fun getLongVar(key: Int): Long? {
        if (variables == null) {
            return null
        }
        val node3 = variables!!.get(key.toLong())
        return if (node3 is LongNode) node3.bitpacked else null
    }

    fun getStringVar(key: Int): String? {
        if (variables == null) {
            return null
        }
        val node3 = variables!!.get(key.toLong())
        return if (node3 is ObjectNode) node3.value as? String else null
    }

    fun updateOwners() {
        if (memberCount == 0) {
            currentOwner = -1
            replacementOwner = -1
        } else {
            currentOwner = -1
            replacementOwner = -1
            var maximumIndex = 0
            var maximum = memberRanks!![0]
            for (i4 in 1 until memberCount) {
                if (memberRanks!![i4] > maximum) {
                    if (maximum == 125.toByte()) {
                        replacementOwner = maximumIndex
                    }
                    maximumIndex = i4
                    maximum = memberRanks!![i4]
                } else if (replacementOwner == -1 && memberRanks!![i4] == 125.toByte()) {
                    replacementOwner = i4
                }
            }
            currentOwner = maximumIndex
            memberRanks!![currentOwner] = 126
        }
    }

    fun banUser(bannedUserHash: Long, bannedUserName: String?) {
        var name = bannedUserName
        if (name != null && name.isEmpty()) {
            name = null
        }
        if (useHashes != (bannedUserHash > 0L)) {
            throw RuntimeException("")
        }
        if ((name == null) == useNames) {
            throw RuntimeException("")
        }
        if ((bannedUserHash > 0L && (bannedUserHashes == null || banCount >= bannedUserHashes!!.size))
            || (name != null && (bannedUserNames == null || banCount >= bannedUserNames!!.size))
        ) {
            expandBanTrackers(banCount + 5)
        }
        if (bannedUserHashes != null) {
            bannedUserHashes!![banCount] = bannedUserHash
        }
        if (bannedUserNames != null) {
            bannedUserNames!![banCount] = name!!
        }
        banCount++
    }

    fun unbanUser(bannedUserIndex: Int) {
        if (--banCount == 0) {
            bannedUserHashes = null
            bannedUserNames = null
        } else {
            if (bannedUserHashes != null) {
                ArrayUtils.arrayCopy(bannedUserHashes!!, bannedUserIndex + 1, bannedUserHashes!!, bannedUserIndex, banCount - bannedUserIndex)
            }
            if (bannedUserNames != null) {
                ArrayUtils.arrayCopy(bannedUserNames!!, bannedUserIndex + 1, bannedUserNames!!, bannedUserIndex, banCount - bannedUserIndex)
            }
        }
    }

    fun updateRank(memberIndex: Int, rank: Byte): Int {
        if (rank == 126.toByte() || rank == 127.toByte()) {
            return -1
        }
        if (currentOwner == memberIndex && (replacementOwner == -1 || memberRanks!![replacementOwner] < 125)) {
            return -1
        }
        if (memberRanks!![memberIndex] == rank) {
            return -1
        }
        memberRanks!![memberIndex] = rank
        updateOwners()
        return memberIndex
    }

    fun updateClanFlag(index: Int, addedFlag: Int, shift: Int, bitmaskSize: Int): Int {
        val mask = (1 shl shift) - 1
        val setMask = if (bitmaskSize == 31) -1 else (1 shl (bitmaskSize + 1)) - 1
        val clearMask = setMask xor mask
        var flag = addedFlag
        flag = flag shl shift
        flag = flag and clearMask
        val flags = clanFlags!![index]
        if ((flags and clearMask) == flag) {
            return -1
        }
        clanFlags!![index] = (flags and clearMask.inv()) or flag
        return index
    }

    fun expandBanTrackers(newSize: Int) {
        if (useHashes) {
            if (bannedUserHashes != null) {
                val newArray = LongArray(newSize)
                ArrayUtils.arrayCopy(bannedUserHashes!!, 0, newArray, 0, banCount)
                bannedUserHashes = newArray
            } else {
                bannedUserHashes = LongArray(newSize)
            }
        }
        if (useNames) {
            if (bannedUserNames != null) {
                val newArray = arrayOfNulls<String>(newSize)
                ArrayUtils.arrayCopy(bannedUserNames!!, 0, newArray, 0, banCount)
                @Suppress("UNCHECKED_CAST")
                bannedUserNames = newArray as Array<String>
            } else {
                @Suppress("UNCHECKED_CAST")
                bannedUserNames = arrayOfNulls<String>(newSize) as Array<String>
            }
        }
    }

    fun updateBitpackedVariable(key: Int, value: Int, shift: Int, bitmaskSize: Int): Boolean {
        val mask = (1 shl shift) - 1
        val setMask = if (bitmaskSize == 31) -1 else (1 shl (bitmaskSize + 1)) - 1
        val clearMask = setMask xor mask
        var v = value
        v = v shl shift
        v = v and clearMask
        if (variables != null) {
            val node = variables!!.get(key.toLong())
            if (node != null) {
                if (node is IntNode) {
                    if ((node.value and clearMask) == v) {
                        return false
                    }
                    node.value = (node.value and clearMask.inv()) or v
                    return true
                }
                node.unlink()
            }
        } else {
            variables = IterableNodeMap(4)
        }
        variables!!.put(IntNode(v), key.toLong())
        return true
    }

    fun setLongVariable(key: Int, value: Long): Boolean {
        if (variables != null) {
            val node4 = variables!!.get(key.toLong())
            if (node4 != null) {
                if (node4 is LongNode) {
                    if (node4.bitpacked == value) {
                        return false
                    }
                    node4.bitpacked = value
                    return true
                }
                node4.unlink()
            }
        } else {
            variables = IterableNodeMap(4)
        }
        variables!!.put(LongNode(value), key.toLong())
        return true
    }

    fun getSortedAffinedSlots(): IntArray {
        if (sortedAffinedSlots == null) {
            val names = Array(memberCount) { memberNames!![it] }
            sortedAffinedSlots = IntArray(memberCount) { it }
            @Suppress("UNCHECKED_CAST")
            ArrayUtils.quicksortt(names as Array<String?>, sortedAffinedSlots!!)
        }
        return sortedAffinedSlots!!
    }

    fun addMember(memberHash: Long, memberName: String?, joinDay: Int) {
        var name = memberName
        if (name != null && name.isEmpty()) {
            name = null
        }
        if ((memberHash > 0L) != useHashes) {
            throw RuntimeException("")
        }
        if (useNames == (name == null)) {
            throw RuntimeException("")
        }
        if ((memberHash > 0L && (memberHashes == null || memberCount >= memberHashes!!.size))
            || (name != null && (memberNames == null || memberCount >= memberNames!!.size))
        ) {
            expandMemberTrackers(memberCount + 5)
        }
        if (memberHashes != null) {
            memberHashes!![memberCount] = memberHash
        }
        if (memberNames != null) {
            memberNames!![memberCount] = name!!
        }
        if (currentOwner == -1) {
            currentOwner = memberCount
            memberRanks!![memberCount] = 126
        } else {
            memberRanks!![memberCount] = 0
        }
        clanFlags!![memberCount] = 0
        memberJoinDays!![memberCount] = joinDay
        memberCount++
        sortedAffinedSlots = null
    }

    fun putIntVariable(key: Int, value: Int): Boolean {
        if (variables != null) {
            val node4 = variables!!.get(key.toLong())
            if (node4 != null) {
                if (node4 is IntNode) {
                    if (value == node4.value) {
                        return false
                    }
                    node4.value = value
                    return true
                }
                node4.unlink()
            }
        } else {
            variables = IterableNodeMap(4)
        }
        variables!!.put(IntNode(value), key.toLong())
        return true
    }

    fun putStringValue(key: Int, value: String?): Boolean {
        var v = value ?: ""
        if (v.length > 80) {
            v = v.substring(0, 80)
        }
        if (variables != null) {
            val node4 = variables!!.get(key.toLong())
            if (node4 != null) {
                if (node4 is ObjectNode) {
                    if (node4.value is String) {
                        if (v == node4.value) {
                            return false
                        }
                        node4.unlink()
                        node4.unlink()
                        variables!!.put(ObjectNode(v), node4.pointer)
                        return true
                    }
                }
                node4.unlink()
            }
        } else {
            variables = IterableNodeMap(4)
        }
        variables!!.put(ObjectNode(v), key.toLong())
        return true
    }

    fun getVariableBitfield(i1: Int, i2: Int, i3: Int): Int? {
        if (variables == null) {
            return null
        }
        val node5 = variables!!.get(i1.toLong())
        if (node5 is IntNode) {
            val i6 = if (i3 == 31) -1 else (1 shl (i3 + 1)) - 1
            return (node5.value and i6) ushr i2
        }
        return null
    }

    fun expandMemberTrackers(size: Int) {
        if (useHashes) {
            if (memberHashes != null) {
                val newArray = LongArray(size)
                ArrayUtils.arrayCopy(memberHashes!!, 0, newArray, 0, memberCount)
                memberHashes = newArray
            } else {
                memberHashes = LongArray(size)
            }
        }
        if (useNames) {
            if (memberNames != null) {
                val newArray = arrayOfNulls<String>(size)
                ArrayUtils.arrayCopy(memberNames!!, 0, newArray, 0, memberCount)
                @Suppress("UNCHECKED_CAST")
                memberNames = newArray as Array<String>
            } else {
                @Suppress("UNCHECKED_CAST")
                memberNames = arrayOfNulls<String>(size) as Array<String>
            }
        }
        if (memberRanks != null) {
            val newArray = ByteArray(size)
            ArrayUtils.arrayCopy(memberRanks!!, 0, newArray, 0, memberCount)
            memberRanks = newArray
        } else {
            memberRanks = ByteArray(size)
        }
        if (clanFlags != null) {
            val newArray = IntArray(size)
            ArrayUtils.arrayCopy(clanFlags!!, 0, newArray, 0, memberCount)
            clanFlags = newArray
        } else {
            clanFlags = IntArray(size)
        }
        if (memberJoinDays != null) {
            val newArray = IntArray(size)
            ArrayUtils.arrayCopy(memberJoinDays!!, 0, newArray, 0, memberCount)
            memberJoinDays = newArray
        } else {
            memberJoinDays = IntArray(size)
        }
    }

    fun removeMember(memberIndex: Int) {
        if (memberIndex < 0 || memberIndex >= memberCount) {
            throw RuntimeException("")
        }
        sortedAffinedSlots = null
        if (--memberCount == 0) {
            memberHashes = null
            memberNames = null
            memberRanks = null
            clanFlags = null
            memberJoinDays = null
            currentOwner = -1
            replacementOwner = -1
        } else {
            ArrayUtils.arrayCopy(memberRanks!!, memberIndex + 1, memberRanks!!, memberIndex, memberCount - memberIndex)
            ArrayUtils.arrayCopy(clanFlags!!, memberIndex + 1, clanFlags!!, memberIndex, memberCount - memberIndex)
            ArrayUtils.arrayCopy(memberJoinDays!!, memberIndex + 1, memberJoinDays!!, memberIndex, memberCount - memberIndex)
            if (memberHashes != null) {
                ArrayUtils.arrayCopy(memberHashes!!, memberIndex + 1, memberHashes!!, memberIndex, memberCount - memberIndex)
            }
            if (memberNames != null) {
                ArrayUtils.arrayCopy(memberNames!!, memberIndex + 1, memberNames!!, memberIndex, memberCount - memberIndex)
            }
            if (currentOwner == memberIndex || replacementOwner == memberIndex) {
                updateOwners()
            }
        }
    }

    fun decode(buffer: Packet) {
        val version = buffer.readUByte()
        if (version in 1..5) {
            val attr = buffer.readUByte()
            if ((attr and 0x1) != 0) {
                useHashes = true
            }
            if ((attr and 0x2) != 0) {
                useNames = true
            }
            if (!useHashes) {
                memberHashes = null
                bannedUserHashes = null
            }
            if (!useNames) {
                memberNames = null
                bannedUserNames = null
            }
            updateCount = buffer.readInt()
            creationDay = buffer.readInt()
            if (version <= 3 && creationDay != 0) {
                creationDay += 16912800
            }
            memberCount = buffer.readUShort()
            banCount = buffer.readUByte()
            clanName = buffer.readString()
            if (version >= 4) {
                buffer.readInt()
            }
            allowGuests = buffer.readUByte() == 1
            talkRank = buffer.readByte()
            kickRank = buffer.readByte()
            lootshareRank = buffer.readByte()
            coinShare = buffer.readByte()

            if (memberCount > 0) {
                if (useHashes && (memberHashes == null || memberHashes!!.size < memberCount)) {
                    memberHashes = LongArray(memberCount)
                }
                if (useNames && (memberNames == null || memberNames!!.size < memberCount)) {
                    @Suppress("UNCHECKED_CAST")
                    memberNames = arrayOfNulls<String>(memberCount) as Array<String>
                }
                if (memberRanks == null || memberRanks!!.size < memberCount) {
                    memberRanks = ByteArray(memberCount)
                }
                if (clanFlags == null || clanFlags!!.size < memberCount) {
                    clanFlags = IntArray(memberCount)
                }
                if (memberJoinDays == null || memberJoinDays!!.size < memberCount) {
                    memberJoinDays = IntArray(memberCount)
                }
                for (index in 0 until memberCount) {
                    if (useHashes) {
                        memberHashes!![index] = buffer.readLong()
                    }
                    if (useNames) {
                        memberNames!![index] = buffer.readNullString()!!
                    }
                    memberRanks!![index] = buffer.readByte()
                    if (version >= 2) {
                        clanFlags!![index] = buffer.readInt()
                    }
                    if (version >= 5) {
                        memberJoinDays!![index] = buffer.readUShort()
                    } else {
                        memberJoinDays!![index] = 0
                    }
                }
                updateOwners()
            }
            if (banCount > 0) {
                if (useHashes && (bannedUserHashes == null || bannedUserHashes!!.size < banCount)) {
                    bannedUserHashes = LongArray(banCount)
                }
                if (useNames && (bannedUserNames == null || bannedUserNames!!.size < banCount)) {
                    @Suppress("UNCHECKED_CAST")
                    bannedUserNames = arrayOfNulls<String>(banCount) as Array<String>
                }
                for (index in 0 until banCount) {
                    if (useHashes) {
                        bannedUserHashes!![index] = buffer.readLong()
                    }
                    if (useNames) {
                        bannedUserNames!![index] = buffer.readNullString()!!
                    }
                }
            }
            if (version >= 3) {
                var size = buffer.readUShort()
                if (size > 0) {
                    variables = IterableNodeMap(if (size < 16) MathUtils.nextPowerOfTwo(size) else 16)
                    while (size-- > 0) {
                        val i6 = buffer.readInt()
                        val key = i6 and 0x3fffffff
                        val type = i6 ushr 30
                        when (type) {
                            0 -> {
                                val i9 = buffer.readInt()
                                variables!!.put(IntNode(i9), key.toLong())
                            }
                            1 -> {
                                val long11 = buffer.readLong()
                                variables!!.put(LongNode(long11), key.toLong())
                            }
                            2 -> {
                                val string13 = buffer.readString()
                                variables!!.put(ObjectNode(string13), key.toLong())
                            }
                        }
                    }
                }
            }
        } else {
            throw RuntimeException("$version")
        }
    }
}
