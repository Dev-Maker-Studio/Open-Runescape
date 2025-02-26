package com.jagex.game.runetek5.config.sequence;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.utils.MathUtils;

public class SeqType {

    public int id;
    public int[] durations;
    public int replayInterval = -1;
    public boolean[] interLeaveOrder;
    public int[] frames;
    public int priority = 5;
    public int replacementShield = -1;
    public int replacementWeapon = -1;
    public int replayCount = 99;
    public int interruptType = -1;
    public int moveType = -1;
    public int loopType = 2;
    public int[] interfaceFrames;
    /**
     * << Contains >> 
     * Bitpacked information
     * > int sound_id = (frame_sounds[frame][0] >> 8)
     * > int times_repeat = (frame_sounds[frame][0] >> 5 & 0x7)
     * 
     * << Non bitpacked information >>
     * If a frame has more than 1 sound option 
     * an index past 0 is a non bitpacked sound id
     * > int sound_id = frame_sounds[frame][1]
     */
    public int[][] frameSounds;
    public boolean lights;
    public boolean tweened;
    public boolean vorbis;
    public int[] frameSoundVolume;
    public int[] soundMaxDelay;
    public int[] soundMinDelay;
    SeqTypeList seqtypelist;
    public IterableNodeMap parameters;

    void decode(Packet stream) {
        while (true) {
            int opcode = stream.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(stream, opcode);
        }
    }

    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
        	int count = stream.readUShort();
            this.durations = new int[count];
            for (int index = 0; index < count; index++) {
                this.durations[index] = stream.readUShort();
            }
            this.frames = new int[count];
            for (int index = 0; index < count; index++) {
                this.frames[index] = stream.readUShort();
            }
            for (int index = 0; index < count; index++) {
                this.frames[index] += stream.readUShort() << 16;
            }
        } else if (opcode == 2) {
            this.replayInterval = stream.readUShort();
        } else if (opcode == 3) {
            this.interLeaveOrder = new boolean[256];
            int count = stream.readUByte();
            for (int index = 0; index < count; index++) {
                this.interLeaveOrder[stream.readUByte()] = true;
            }
        } else if (opcode == 5) {
            this.priority = stream.readUByte();
        } else if (opcode == 6) {
            this.replacementShield = stream.readUShort();
        } else if (opcode == 7) {
            this.replacementWeapon = stream.readUShort();
        } else if (opcode == 8) {
            this.replayCount = stream.readUByte();
        } else if (opcode == 9) {
            this.interruptType = stream.readUByte();
        } else if (opcode == 10) {
            this.moveType = stream.readUByte();
        } else if (opcode == 11) {
            this.loopType = stream.readUByte();
        } else if (opcode == 12) {
        	int count = stream.readUByte();
            this.interfaceFrames = new int[count];
            for (int index = 0; index < count; index++) {
                this.interfaceFrames[index] = stream.readUShort();
            }
            for (int index = 0; index < count; index++) {
                this.interfaceFrames[index] += stream.readUShort() << 16;
            }
        } else if (opcode == 13) {
        	int frameCount = stream.readUShort();
            this.frameSounds = new int[frameCount][];
            for (int frame = 0; frame < frameCount; frame++) {
                int soundCount = stream.readUByte();
                if (soundCount > 0) {
                    this.frameSounds[frame] = new int[soundCount];
                    // >> 8 = sound_id
                    // >> 5 & 0x7 = times_repeated
                    this.frameSounds[frame][0] = stream.readUMedium();
                    for (int alternativeSound = 1; alternativeSound < soundCount; alternativeSound++) {
                    	// randomly picked non bitpacked frame sound id
                        this.frameSounds[frame][alternativeSound] = stream.readUShort();
                    }
                }
            }
        } else if (opcode == 14) {
            this.lights = true;
        } else if (opcode == 15) {
            this.tweened = true;
        } else if (opcode == 18) {
            this.vorbis = true;
        } else if (opcode == 19) {
            if (this.frameSoundVolume == null) {
                this.frameSoundVolume = new int[this.frameSounds.length];
                for (int count = 0; count < this.frameSounds.length; count++) {
                    this.frameSoundVolume[count] = 255;
                }
            }
            this.frameSoundVolume[stream.readUByte()] = stream.readUByte();
        } else if (opcode == 20) {
            if (this.soundMaxDelay == null || this.soundMinDelay == null) {
                this.soundMaxDelay = new int[this.frameSounds.length];
                this.soundMinDelay = new int[this.frameSounds.length];
                for (int count = 0; count < this.frameSounds.length; count++) {
                    this.soundMaxDelay[count] = 256;
                    this.soundMinDelay[count] = 256;
                }
            }
            int count = stream.readUByte();
            this.soundMaxDelay[count] = stream.readUShort();
            this.soundMinDelay[count] = stream.readUShort();
        } else if (opcode == 249) {
        	int count = stream.readUByte();
            if (this.parameters == null) {
            	int index = MathUtils.nextPowerOfTwo(count);
                this.parameters = new IterableNodeMap(index);
            }
            for (int index = 0; index < count; index++) {
                boolean isString = stream.readUByte() == 1;
                int key = stream.readUMedium();
                Object value;
                if (isString) {
                    value = new ObjectNode(stream.readString());
                } else {
                    value = new IntNode(stream.readInt());
                }
                this.parameters.put((Node) value, key);
            }
        }        
    }
    
    void postDecode() {
        if (this.interruptType == -1) {
            if (this.interLeaveOrder != null) {
                this.interruptType = 2;
            } else {
                this.interruptType = 0;
            }
        }
        if (this.moveType == -1) {
            if (this.interLeaveOrder != null) {
                this.moveType = 2;
            } else {
                this.moveType = 0;
            }
        }
    }

    public boolean ready() {
        if (this.frames == null) {
            return true;
        } 
        boolean fullyCached = true;
        for (int index = 0; index < this.frames.length; index++) {
            int frame = this.frames[index];
            if (this.seqtypelist.listFrameset(frame >>> 16) == null) {
                fullyCached = false;
            }
        }
        return fullyCached;
    }

    public int getCs2Integer(int key, int defaultValue) {
        if (this.parameters == null) {
            return defaultValue;
        } 
        IntNode cs2Entry = (IntNode) this.parameters.get(key);
        return cs2Entry == null ? defaultValue : cs2Entry.value;
    }
    
    public String getCs2Object(int key, String defaultResult) {
        if (this.parameters == null) {
            return defaultResult;
        } 
        ObjectNode objectNode = (ObjectNode) this.parameters.get(key);
        return objectNode == null ? defaultResult : (String) objectNode.value;
    }
}
