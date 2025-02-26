package com.jagex.game.runetek5.config.quickchat;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.utils.Utils;

public class QuickChatCategoryType extends CacheableNode {

    public String name;
    public int[] subCategories;
    public char[] subCategoryHotkeys;
    public int[] messages;
    public char[] messageHotkeys;

    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }

            decodeOpcode(buffer, opcode);
        }
    }

    public void decodeOpcode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.name = buffer.readString();
        } else if (opcode == 2) {
            int count = buffer.readUByte();
            this.subCategories = new int[count];
            this.subCategoryHotkeys = new char[count];

            for (int index = 0; index < count; index++) {
                this.subCategories[index] = buffer.readUShort();
                byte hotkey = buffer.readByte();
                this.subCategoryHotkeys[index] = hotkey == 0 ? 0 : Utils.cp1252ToChar(hotkey);
            }
        } else if (opcode == 3) {
            int count = buffer.readUByte();
            this.messages = new int[count];
            this.messageHotkeys = new char[count];

            for (int index = 0; index < count; index++) {
                this.messages[index] = buffer.readUShort();
                byte hotkey = buffer.readByte();
                this.messageHotkeys[index] = hotkey == 0 ? 0 : Utils.cp1252ToChar(hotkey);
            }
        } else if (opcode == 4) {
        }
    }

    public int subCategoryForHotkey(char hotkey) {
        if (this.subCategories != null) {
            for (int index = 0; index < this.subCategories.length; index++) {
                if (this.subCategoryHotkeys[index] == hotkey) {
                    return this.subCategories[index];
                }
            }

        }
        return -1;
    }

    public int messageForHotkey(char hotkey) {
        if (this.messages != null) {
            for (int index = 0; index < this.messages.length; index++) {
                if (this.messageHotkeys[index] == hotkey) {
                    return this.messages[index];
                }
            }

        }
        return -1;
    }

    void postDecode() {
        if (this.messages != null) {
            for (int index = 0; index < this.messages.length; index++) {
                this.messages[index] |= 0x8000;
            }
        }

        if (this.subCategories != null) {
            for (int index = 0; index < this.subCategories.length; index++) {
                this.subCategories[index] |= 0x8000;
            }
        }
    }
}
