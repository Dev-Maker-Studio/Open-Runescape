package com.jagex.game.runetek5.config.rsinterface.cs2;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.trigger.ClientTriggerType;
import com.jagex.game.runetek5.utils.MathUtils;

public class CS2Script extends CacheableNode {

    public ClientTriggerType triggerType;
    public int intLocalsCount;
    public int stringLocalsCount;
    public int longLocalsCount;
    public int intArgsCount;
    public int stringArgsCount;
    public int longArgsCount;
    public IterableNodeMap[] switchMaps;
    public String scriptName;
    public ClientScriptOpCode[] operations;
    public Object[] stringOpValues;
    public int[] intOpValues;
    public long[] longOpValues;

    public CS2Script(Packet buffer) {
        int end = decodeHeader(buffer);
        int i_3 = 0;

        for (; buffer.currentPosition < end; i_3++) {
            ClientScriptOpCode cs2opinfo5 = getOpcode(buffer);
            decodeInstruction(buffer, i_3, cs2opinfo5);
        }
    }

    public static CS2Script getCs2Script(int scriptId) {
        CS2Script cs2script = (CS2Script) CS2Executor.CS2_CACHE.get(scriptId);
        if (cs2script != null) {
            return cs2script;
        } 
        byte[] cs2Data = Resource.CS2.getFile(scriptId, 0);
        if (cs2Data != null && cs2Data.length > 1) {
            try {
                cs2script = decodeScript(cs2Data);
            } catch (Exception exception) {
                throw new RuntimeException(exception.getMessage() + " " + scriptId);
            }
            CS2Executor.CS2_CACHE.put(cs2script, scriptId);
            return cs2script;
        } 
        return null;
    }

    public static CS2Script decodeScript(byte[] scriptData) {
        return new CS2Script(new Packet(scriptData));
    }

    public static CS2Script getScript(ClientTriggerType event, int scriptId, int i_2) {
        int scriptFileId = event.id | scriptId << 10;
        CS2Script script = (CS2Script) CS2Executor.CS2_CACHE.get((long) scriptFileId << 16);
        if (script != null) {
            return script;
        } 
        byte[] scriptData = Resource.CS2.getFile(Resource.CS2.getFileId(scriptFileId));
        if (scriptData != null) {
            if (scriptData.length <= 1) {
                return null;
            } 
            try {
                script = decodeScript(scriptData);
            } catch (Exception exception) {
                throw new RuntimeException(exception.getMessage() + " " + scriptFileId);
            }

            script.triggerType = event;
            CS2Executor.CS2_CACHE.put(script, (long) scriptFileId << 16);
            return script;
        } 
        scriptFileId = event.id | i_2 + 65536 << 10;
        script = (CS2Script) CS2Executor.CS2_CACHE.get((long) scriptFileId << 16);
        if (script != null) {
            return script;
        } 
        scriptData = Resource.CS2.getFile(Resource.CS2.getFileId(scriptFileId));
        if (scriptData != null) {
            if (scriptData.length <= 1) {
                return null;
            } 
            try {
                script = decodeScript(scriptData);
            } catch (Exception exception) {
                throw new RuntimeException(exception.getMessage() + " " + scriptFileId);
            }

            script.triggerType = event;
            CS2Executor.CS2_CACHE.put(script, (long) scriptFileId << 16);
            return script;
        } 
        scriptFileId = event.id | 0x3fffc00;
        script = (CS2Script) CS2Executor.CS2_CACHE.get((long) scriptFileId << 16);
        if (script != null) {
            return script;
        } 
        scriptData = Resource.CS2.getFile(Resource.CS2.getFileId(scriptFileId));
        if (scriptData != null) {
            if (scriptData.length <= 1) {
                return null;
            } 
            try {
                script = decodeScript(scriptData);
            } catch (Exception exception) {
                throw new RuntimeException(exception.getMessage() + " " + scriptFileId);
            }

            script.triggerType = event;
            CS2Executor.CS2_CACHE.put(script, (long) scriptFileId << 16);
            return script;                        
        } 
        return null;                     
    }

    public ClientScriptOpCode getOpcode(Packet buffer) {
        int opcodeId = buffer.readUnsignedShort();
        ClientScriptOpCode operation = ClientScriptOpCode.getByOpcode(opcodeId);
        if (operation != null) {
            return operation;
        } 
        throw new RuntimeException("");
    }

    public int decodeHeader(Packet stream) {
        stream.currentPosition = stream.buffer.length - 2;
        int i_2 = stream.readUnsignedShort();
        int i_3 = stream.buffer.length - 2 - i_2 - 16;
        stream.currentPosition = i_3;
        int i_4 = stream.readInt();
        this.intLocalsCount = stream.readUnsignedShort();
        this.stringLocalsCount = stream.readUnsignedShort();
        this.longLocalsCount = stream.readUnsignedShort();
        this.intArgsCount = stream.readUnsignedShort();
        this.stringArgsCount = stream.readUnsignedShort();
        this.longArgsCount = stream.readUnsignedShort();
        int i_5 = stream.readUnsignedByte();
        if (i_5 > 0) {
            this.switchMaps = new IterableNodeMap[i_5];

            for (int i_6 = 0; i_6 < i_5; i_6++) {
                int i_7 = stream.readUnsignedShort();
                IterableNodeMap iterablenodemap8 = new IterableNodeMap(MathUtils.nextPowerOfTwo(i_7));
                this.switchMaps[i_6] = iterablenodemap8;

                while (i_7-- > 0) {
                    int i_9 = stream.readInt();
                    int i_10 = stream.readInt();
                    iterablenodemap8.put(new IntNode(i_10), i_9);
                }
            }
        }

        stream.currentPosition = 0;
        this.scriptName = stream.readNullString();
        this.operations = new ClientScriptOpCode[i_4];
        return i_3;
    }

    public void decodeInstruction(Packet buffer, int op, ClientScriptOpCode instruction) {
        int operationsCount = this.operations.length;
        if (instruction == ClientScriptOpCode.PUSH_STRING) {
            if (this.stringOpValues == null) {
                this.stringOpValues = new String[operationsCount];
            }

            String string_5 = buffer.readString();
            if (string_5.toLowerCase().contains("runescape")) {
                string_5 = string_5.replace("runescape", "darkan");
                string_5 = string_5.replace("RuneScape", "Darkan");
                string_5 = string_5.replace("Runescape", "Darkan");
            }

            this.stringOpValues[op] = string_5.intern();
        } else if (instruction == ClientScriptOpCode.PUSH_LONG) {
            if (this.longOpValues == null) {
                this.longOpValues = new long[operationsCount];
            }

            this.longOpValues[op] = buffer.readLong();
        } else {
            if (this.intOpValues == null) {
                this.intOpValues = new int[operationsCount];
            }

            if (instruction.hasIntConstant) {
                this.intOpValues[op] = buffer.readInt();
            } else {
                this.intOpValues[op] = buffer.readUnsignedByte();
            }
        }

        this.operations[op] = instruction;
    }
}
