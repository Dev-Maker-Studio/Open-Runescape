package com.jagex.game.runetek5.config.quickchat;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * 
 * @author Brainpower - 22/04/2022
 *
 */
public class QuickchatMessageTypeList {

    public int messageCount;
    public int menuCount;
    public final LRUCache messageTypeCache = new LRUCache(64);
    public final QuickchatFillerList quickchatFillerlist;
    public final Js5ResourceProvider messageResource;
    public final Js5ResourceProvider menuResource;

    public QuickchatMessageTypeList(Js5ResourceProvider messageResource, Js5ResourceProvider menuResource, QuickchatFillerList quickchatFillerlist) {
        this.messageResource = messageResource;
        this.menuResource = menuResource;
        this.quickchatFillerlist = quickchatFillerlist;
        if (this.messageResource != null) {
        	this.messageCount = this.messageResource.getArchiveFileCount(1);
        }

        if (this.menuResource != null) {
        	this.menuCount = this.menuResource.getArchiveFileCount(1);
        }

    }

    String getValueDisplay(QuickChatValueType value, int[] configs, long data) {
        if (this.quickchatFillerlist != null) {
            String displayName = this.quickchatFillerlist.getValueDisplay(value, configs, data);
            if (displayName != null) {
                return displayName;
            }
        }
        return Long.toString(data);
    }

    public QuickchatMessageType listMessage(int id) {
        QuickchatMessageType messageType = (QuickchatMessageType) this.messageTypeCache.get(id);
        if (messageType == null) {
            byte[] data;
            if (id >= 32768) {
                data = this.menuResource.getFile(1, id & 0x7fff);
            } else {
                data = this.messageResource.getFile(1, id);
            }

            messageType = new QuickchatMessageType();
            messageType.quickchatMessageTypelist = this;
            if (data != null) {
                messageType.decode(new Packet(data));
            }

            if (id >= 32768) {
                messageType.postDecode();
            }

            this.messageTypeCache.put(messageType, id);
        }
        return messageType;
    }

	public static QuickChatMessage decodeQuickchatMessage(Packet buffer) {
	    QuickChatMessage message = new QuickChatMessage();
	    message.id = buffer.readUShort();
	    message.message = TypeLists.QUICK_CHAT_MESSAGE.listMessage(message.id);
	    return message;
	}
}