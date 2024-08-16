package com.jagex.game.runetek5.config.quickchat;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.material.texture.ColorImageCache;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interpreter;
import com.jagex.game.runetek5.config.staticelements.StaticElements;
import com.jagex.game.runetek5.utils.Utils;

public class QuickchatMessageType extends CacheableNode {

    public int[] responses;
    public boolean searchable = true;
    QuickchatMessageTypeList quickchatMessageTypelist;
    public String[] message;
    public int[] valueTypes;
    public int[][] configs;

    public void decodeOpcode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.message = Utils.split(buffer.readString(), '<');
        } else if (opcode == 2) {
            int count = buffer.readUnsignedByte();
            this.responses = new int[count];
            for (int index = 0; index < count; index++) {
                this.responses[index] = buffer.readUnsignedShort();
            }
        } else if (opcode == 3) {
            int count = buffer.readUnsignedByte();
            this.valueTypes = new int[count];
            this.configs = new int[count][];
            for (int index = 0; index < count; index++) {
                int typeId = buffer.readUnsignedShort();
                QuickChatValueType type = QuickChatValueType.get(typeId);
                if (type != null) {
                    this.valueTypes[index] = typeId;
                    this.configs[index] = new int[type.parameterCount];
                    for (int config = 0; config < type.parameterCount; config++) {
                        this.configs[index][config] = buffer.readUnsignedShort();
                    }
                }
            }
        } else if (opcode == 4) {
            this.searchable = false;
        }
    }

    public void method14896(Packet buffer, int[] ints_2) {
        if (this.valueTypes != null) {
            for (int typeIndex = 0; typeIndex < this.valueTypes.length && typeIndex < ints_2.length; typeIndex++) {
                int size = getValueType(typeIndex).clientTransmitSize;
                if (size > 0) {
                    buffer.method13068(ints_2[typeIndex], size);
                }
            }
        }

    }

    public String fillDynamicValues(Packet buffer) {
        StringBuilder builder = new StringBuilder(80);
        if (this.valueTypes != null) {
            for (int valueTypeIndex = 0; valueTypeIndex < this.valueTypes.length; valueTypeIndex++) {
                builder.append(this.message[valueTypeIndex]);
                QuickChatValueType valueType = getValueType(valueTypeIndex);
                int serverTransmitSize = QuickChatValueType.get(this.valueTypes[valueTypeIndex]).serverTransmitSize;
                long bitpackedData = buffer.bitpackLong(serverTransmitSize);
                builder.append(this.quickchatMessageTypelist.getValueDisplay(valueType, this.configs[valueTypeIndex], bitpackedData));
            }
        }

        builder.append(this.message[this.message.length - 1]);
        return builder.toString();
    }

    public String method14898() {
        StringBuilder builder = new StringBuilder(80);
        if (this.message == null) {
            return "";
        } 
        builder.append(this.message[0]);

        for (int messageIndex = 1; messageIndex < this.message.length; messageIndex++) {
            for (int i_4 = 0; i_4 < 3; i_4++) {
                builder.append('.');
            }
            builder.append(this.message[messageIndex]);
        }

        return builder.toString();
    }

    public int method14901(int i_1, int i_2) {
        return this.valueTypes != null && i_1 >= 0 && i_1 <= this.valueTypes.length ? (this.configs[i_1] != null && i_2 >= 0 && i_2 <= this.configs[i_1].length ? this.configs[i_1][i_2] : -1) : -1;
    }

    void postDecode() {
        if (this.responses != null) {
            for (int i_2 = 0; i_2 < this.responses.length; i_2++) {
                this.responses[i_2] |= 0x8000;
            }
        }
    }

    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                return;
            }

            decodeOpcode(buffer, opcode);
        }
    }

    public int method14916() {
        return this.valueTypes == null ? 0 : this.valueTypes.length;
    }

    public QuickChatValueType getValueType(int qcTypeIndex) {
        return this.valueTypes != null && qcTypeIndex >= 0 && qcTypeIndex <= this.valueTypes.length ? QuickChatValueType.get(this.valueTypes[qcTypeIndex]) : null;
    }
    
    public static void method1360(String string_0, boolean bool1) {
        string_0 = string_0.toLowerCase();
        short[] shorts_3 = new short[16];
        int i_4 = 0;
        int i_5 = bool1 ? 32768 : 0;
        int i_6 = (bool1 ? TypeLists.QUICK_CHAT_MESSAGE.menuCount : TypeLists.QUICK_CHAT_MESSAGE.messageCount) + i_5;
        for (int i_7 = i_5; i_7 < i_6; i_7++) {
            QuickchatMessageType quickchatdefinitions12 = TypeLists.QUICK_CHAT_MESSAGE.listMessage(i_7);
            if (quickchatdefinitions12.searchable && quickchatdefinitions12.method14898().toLowerCase().indexOf(string_0) != -1) {
                if (i_4 >= 50) {
                    CS2Interpreter.CS2_QUERY_RESULTS_LEN = -1;
                    ColorImageCache.CS2_QUERY_RESULTS = null;
                    return;
                }
                if (i_4 >= shorts_3.length) {
                    short[] shorts_9 = new short[shorts_3.length * 2];
                    System.arraycopy(shorts_3, 0, shorts_9, 0, i_4);
                    shorts_3 = shorts_9;
                }
                shorts_3[i_4++] = (short) i_7;
            }
        }
        ColorImageCache.CS2_QUERY_RESULTS = shorts_3;
        StaticElements.CS2_QUERY_RESULT_IDX = 0;
        CS2Interpreter.CS2_QUERY_RESULTS_LEN = i_4;
        String[] arr_11 = new String[CS2Interpreter.CS2_QUERY_RESULTS_LEN];
        for (int i_8 = 0; i_8 < CS2Interpreter.CS2_QUERY_RESULTS_LEN; i_8++) {
            arr_11[i_8] = TypeLists.QUICK_CHAT_MESSAGE.listMessage(shorts_3[i_8]).method14898();
        }
        Utils.sortAlphabetically(arr_11, ColorImageCache.CS2_QUERY_RESULTS);
    }

}
