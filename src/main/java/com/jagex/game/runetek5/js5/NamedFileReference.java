package com.jagex.game.runetek5.js5;

/**
 * 
 * @author Steven - 11/10/2023
 *
 */
public class NamedFileReference {

    public final int[] nameHashes;

    public NamedFileReference(int[] nameHashes) {
        int capacity;
        for (capacity = 1; capacity <= (nameHashes.length >> 1) + nameHashes.length; capacity <<= 1) { }
        this.nameHashes = new int[capacity + capacity];
        for (int index = 0; index < capacity + capacity; index++) {
            this.nameHashes[index] = -1;
        }
        int position;
        for (int index = 0; index < nameHashes.length; this.nameHashes[position + position + 1] = index++) {
            for (position = nameHashes[index] & capacity - 1; this.nameHashes[position + position + 1] != -1; position = position + 1 & capacity - 1) { }
            this.nameHashes[position + position] = nameHashes[index];
        }
    }

    /**
     * [J]
     */
    public int getGroupId(int nameHash) {
        int center = (this.nameHashes.length >> 1) - 1;
        int position = nameHash & center;
        while (true) {
            int hash = this.nameHashes[position + position + 1];
            if (hash == -1) {
                return -1;
            }
            if (this.nameHashes[position + position] == nameHash) {
                return hash;
            }
            position = position + 1 & center;
        }
    }
}
