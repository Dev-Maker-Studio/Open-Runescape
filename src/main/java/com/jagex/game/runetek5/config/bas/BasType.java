package com.jagex.game.runetek5.config.bas;

import java.util.Iterator;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.utils.MathUtils;

public class BasType {

    public int standSequenceId = -1;
    public int walkSequence = -1;
    public int teleportSequence = -1;
    public int teleDir3 = -1;
    public int teleDir2 = -1;
    public int teleDir1 = -1;
    public int runningSequence = -1;
    public int runDir3 = -1;
    public int runDir2 = -1;
    public int runDir1 = -1;
    public int anInt2786;
    public int anInt2829;
    public int[] objVisibility;
    
    /**
     * [0] translate x
     * [1] translate y
     * [2] translate z
     * [3] rotate x
     * [4] rotate y
     * [5] rotate z
     */
    public int[][] objVerticeTransformations;
    
    public int anInt2820;
    public int anInt2804;
    public int anInt2825;
    public int anInt2823;
    public int anInt2824;
    public int anInt2816;
    public int anInt2815;
    public int anInt2827;
    public int unused = -1;
    public int standTurnCcwSequence = -1;
    public int standTurnCwSequence = -1;
    public int walkDir3 = -1;
    public int walkDir2 = -1;
    public int walkDir1 = -1;
    public int anInt2798 = -1;
    public int teleTurnCcwSequence = -1;
    public int teleTurnCwSequence = -1;
    public int runTurnCcwSequence = -1;
    public int runTurnCwSequence = -1;
    public int walkTurnCcwSequence = -1;
    public int walkTurnCwSequence = -1;
    public int[] randomStandSequences;
    public int[] randomStandSequenceChances;
    public boolean rendersShadow = true;
    public int anInt2813;
    public int anInt2790;
    public int[] turnAngleAdjustment;
    public int[][] projectionOffset;
    public BasTypeList basTypeList;
    public int totalCache;
    public Matrix43[] matrices;

    public static void addEntry(int id, IterableNodeMap<Node<?>> map) {
        if (id != -1 && map.get(id) == null) {
            map.put(new Node<Node<?>>(), id);
        }
    }

    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.standSequenceId = buffer.readBigSmart();
            this.walkSequence = buffer.readBigSmart();
        } else if (opcode == 2) {
            this.teleportSequence = buffer.readBigSmart();
        } else if (opcode == 3) {
            this.teleDir3 = buffer.readBigSmart();
        } else if (opcode == 4) {
            this.teleDir2 = buffer.readBigSmart();
        } else if (opcode == 5) {
            this.teleDir1 = buffer.readBigSmart();
        } else if (opcode == 6) {
            this.runningSequence = buffer.readBigSmart();
        } else if (opcode == 7) {
            this.runDir3 = buffer.readBigSmart();
        } else if (opcode == 8) {
            this.runDir2 = buffer.readBigSmart();
        } else if (opcode == 9) {
            this.runDir1 = buffer.readBigSmart();
        } else if (opcode == 26) {
            this.anInt2786 = (short) (buffer.readUByte() * 4);
            this.anInt2829 = (short) (buffer.readUByte() * 4);
        } else if (opcode == 27) {
            if (this.objVerticeTransformations == null) {
                this.objVerticeTransformations = new int[this.basTypeList.equipmentDefaults.customizableObjSlots.length][];
            }
            int slot = buffer.readUByte();
            this.objVerticeTransformations[slot] = new int[6];
            for (int transformation = 0; transformation < 6; transformation++) {
                this.objVerticeTransformations[slot][transformation] = buffer.readShort();
            }
        } else if (opcode == 28) {
        	int slotCount = buffer.readUByte();
            this.objVisibility = new int[slotCount];
            for (int index = 0; index < slotCount; index++) {
                this.objVisibility[index] = buffer.readUByte();
                if (this.objVisibility[index] == 255) {
                    this.objVisibility[index] = -1;
                }
            }
        } else if (opcode == 29) {
            this.anInt2820 = buffer.readUByte();
        } else if (opcode == 30) {
            this.anInt2804 = buffer.readUShort();
        } else if (opcode == 31) {
            this.anInt2825 = buffer.readUByte();
        } else if (opcode == 32) {
            this.anInt2823 = buffer.readUShort();
        } else if (opcode == 33) {
            this.anInt2824 = buffer.readShort();
        } else if (opcode == 34) {
            this.anInt2816 = buffer.readUByte();
        } else if (opcode == 35) {
            this.anInt2815 = buffer.readUShort();
        } else if (opcode == 36) {
            this.anInt2827 = buffer.readShort();
        } else if (opcode == 37) {
            this.unused = buffer.readUByte();
        } else if (opcode == 38) {
            this.standTurnCcwSequence = buffer.readBigSmart();
        } else if (opcode == 39) {
            this.standTurnCwSequence = buffer.readBigSmart();
        } else if (opcode == 40) {
            this.walkDir3 = buffer.readBigSmart();
        } else if (opcode == 41) {
            this.walkDir2 = buffer.readBigSmart();
        } else if (opcode == 42) {
            this.walkDir1 = buffer.readBigSmart();
        } else if (opcode == 43) {
            buffer.readUShort();
        } else if (opcode == 44) {
            buffer.readUShort();
        } else if (opcode == 45) {
            this.anInt2798 = buffer.readUShort();
        } else if (opcode == 46) {
            this.teleTurnCcwSequence = buffer.readBigSmart();
        } else if (opcode == 47) {
            this.teleTurnCwSequence = buffer.readBigSmart();
        } else if (opcode == 48) {
            this.runTurnCcwSequence = buffer.readBigSmart();
        } else if (opcode == 49) {
            this.runTurnCwSequence = buffer.readBigSmart();
        } else if (opcode == 50) {
            this.walkTurnCcwSequence = buffer.readBigSmart();
        } else if (opcode == 51) {
            this.walkTurnCwSequence = buffer.readBigSmart();
        } else if (opcode == 52) {
        	int standCount = buffer.readUByte();
            this.randomStandSequences = new int[standCount];
            this.randomStandSequenceChances = new int[standCount];
            for (int index = 0; index < standCount; index++) {
                this.randomStandSequences[index] = buffer.readBigSmart();
                int chance = buffer.readUByte();
                this.randomStandSequenceChances[index] = chance;
                this.totalCache += chance;
            }
        } else if (opcode == 53) {
            this.rendersShadow = false;
        } else if (opcode == 54) {
            this.anInt2813 = buffer.readUByte() << 6;
            this.anInt2790 = buffer.readUByte() << 6;
        } else if (opcode == 55) {
            if (this.turnAngleAdjustment == null) {
                this.turnAngleAdjustment = new int[this.basTypeList.equipmentDefaults.customizableObjSlots.length];
            }
            int i_4 = buffer.readUByte();
            this.turnAngleAdjustment[i_4] = buffer.readUShort();
        } else if (opcode == 56) {
            if (this.projectionOffset == null) {
                this.projectionOffset = new int[this.basTypeList.equipmentDefaults.customizableObjSlots.length][];
            }
            int slot = buffer.readUByte();
            this.projectionOffset[slot] = new int[3];
            for (int direction = 0; direction < 3; direction++) {
                this.projectionOffset[slot][direction] = buffer.readShort();
            }
        }
    }

    public int getStandSequence() {
        if (this.standSequenceId != -1) {
            return this.standSequenceId;
        } 
        if (this.randomStandSequences == null) {
            return -1;
        } 
        int chance = (int) (Math.random() * this.totalCache);
        int sequenceIndex;
        for (sequenceIndex = 0; chance >= this.randomStandSequenceChances[sequenceIndex]; sequenceIndex++) {
            chance -= this.randomStandSequenceChances[sequenceIndex];
        }
        return this.randomStandSequences[sequenceIndex];
    }

    public boolean isStandAnimation(int sequenceId) {
        if (sequenceId == -1) {
            return false;
        } 
        if (this.standSequenceId == sequenceId) {
            return true;
        } 
        if (this.randomStandSequences != null) {
            for (int index = 0; index < this.randomStandSequences.length; index++) {
                if (this.randomStandSequences[index] == sequenceId) {
                    return true;
                }
            }
        }
        return false;
    }

    public Matrix43[] getSubmeshMatrices() {
        if (this.matrices != null) {
            return this.matrices;
        } 
        if (this.objVerticeTransformations == null) {
            return null;
        } 
        this.matrices = new Matrix43[this.objVerticeTransformations.length];
        for (int index = 0; index < this.objVerticeTransformations.length; index++) {
            int offsetX = 0;
            int offsetY = 0;
            int offsetZ = 0;
            int rotationX = 0;
            int rotationY = 0;
            int rotationZ = 0;
            if (this.objVerticeTransformations[index] != null) {
                offsetX = this.objVerticeTransformations[index][0];
                offsetY = this.objVerticeTransformations[index][1];
                offsetZ = this.objVerticeTransformations[index][2];
                rotationX = this.objVerticeTransformations[index][3] << 3;
                rotationY = this.objVerticeTransformations[index][4] << 3;
                rotationZ = this.objVerticeTransformations[index][5] << 3;
            }
            if (offsetX != 0 || offsetY != 0 || offsetZ != 0 || rotationX != 0 || rotationY != 0 || rotationZ != 0) {
                Matrix43 matrix = this.matrices[index] = new Matrix43();
                if (rotationZ != 0) {
                    matrix.rotate(0.0F, 0.0F, 1.0F, MathUtils.getTheta(rotationZ));
                }
                if (rotationX != 0) {
                    matrix.rotate(1.0F, 0.0F, 0.0F, MathUtils.getTheta(rotationX));
                }
                if (rotationY != 0) {
                    matrix.rotate(0.0F, 1.0F, 0.0F, MathUtils.getTheta(rotationY));
                }
                matrix.translate(offsetX, offsetY, offsetZ);
            }
        }
        return this.matrices;
    }

    public void decode(Packet stream) {
        while (true) {
            int opcode = stream.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(stream, opcode);
        }
    }

    public int[] getAnimations() {
        IterableNodeMap<Node<?>> sequenceMap = new IterableNodeMap<>(16);
        addEntry(this.standSequenceId, sequenceMap);
        if (this.randomStandSequences != null) {
            int[] standSeqs = this.randomStandSequences;
            for (int index = 0; index < standSeqs.length; index++) {
                int standSeq = standSeqs[index];
                addEntry(standSeq, sequenceMap);
            }
        }
        addEntry(this.standTurnCcwSequence, sequenceMap);
        addEntry(this.standTurnCwSequence, sequenceMap);
        addEntry(this.walkSequence, sequenceMap);
        addEntry(this.walkDir3, sequenceMap);
        addEntry(this.walkDir2, sequenceMap);
        addEntry(this.walkDir1, sequenceMap);
        addEntry(this.runningSequence, sequenceMap);
        addEntry(this.runDir3, sequenceMap);
        addEntry(this.runDir2, sequenceMap);
        addEntry(this.runDir1, sequenceMap);
        addEntry(this.teleportSequence, sequenceMap);
        addEntry(this.teleDir3, sequenceMap);
        addEntry(this.teleDir2, sequenceMap);
        addEntry(this.teleDir1, sequenceMap);
        addEntry(this.teleTurnCcwSequence, sequenceMap);
        addEntry(this.teleTurnCwSequence, sequenceMap);
        addEntry(this.runTurnCcwSequence, sequenceMap);
        addEntry(this.runTurnCwSequence, sequenceMap);
        addEntry(this.walkTurnCcwSequence, sequenceMap);
        addEntry(this.walkTurnCwSequence, sequenceMap);
        int[] sequences = new int[sequenceMap.size()];
        int count = 0;
        Node<?> node;
        for (Iterator<Node<?>> its = sequenceMap.iterator(); its.hasNext(); sequences[count++] = (int) node.pointer) {
            node = its.next();
        }
        return sequences;
    }
}
