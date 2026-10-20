package com.jagex.game.runetek5.js5

class NamedFileReference(nameHashes: IntArray) {

    @JvmField val nameHashes: IntArray

    init {
        var capacity = 1
        while (capacity <= (nameHashes.size shr 1) + nameHashes.size) {
            capacity = capacity shl 1
        }
        this.nameHashes = IntArray(capacity + capacity)
        for (index in 0 until capacity + capacity) {
            this.nameHashes[index] = -1
        }
        var index = 0
        while (index < nameHashes.size) {
            var position = nameHashes[index] and capacity - 1
            while (this.nameHashes[position + position + 1] != -1) {
                position = position + 1 and capacity - 1
            }
            this.nameHashes[position + position] = nameHashes[index]
            this.nameHashes[position + position + 1] = index++
        }
    }

    fun getGroupId(nameHash: Int): Int {
        val center = (this.nameHashes.size shr 1) - 1
        var position = nameHash and center
        while (true) {
            val hash = this.nameHashes[position + position + 1]
            if (hash == -1) {
                return -1
            }
            if (this.nameHashes[position + position] == nameHash) {
                return hash
            }
            position = position + 1 and center
        }
    }
}
