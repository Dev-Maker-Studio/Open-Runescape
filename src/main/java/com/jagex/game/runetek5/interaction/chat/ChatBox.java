package com.jagex.game.runetek5.interaction.chat;

import com.darkan.bot.scripts.ScriptExecutor;
import com.darkan.bot.scripts.event.Chat;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.ChatMessageType;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.quest.QuestType;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.rendering.Renderers;

/**
 * 
 * @author Brainpower - 18/09/2023
 *
 */
public class ChatBox {
	
	// Chat
	public static final ChatLine[] CHAT_LINES = new ChatLine[100];
	public static final LRUCache CHAT_ICONS = new LRUCache(8);
	public static int NUM_CHAT_LINES;
	public static int CHATLINE_IDENTIFIER;
	
	// Chat filter
	public static int PUBLIC_FILTER = 0;
	public static ChatFilter PRIVATE_FILTER;
	public static int TRADE_FILTER = 0;

	public static void writeMessage(ChatMessageType type, int effectFlags, String crownedName, String name, String simpleName, String message, String clan, int quickchatMessageId) {
        ScriptExecutor.INSTANCE.pushEvent(new Chat(type, simpleName, crownedName, name, message));
        ChatLine line = ChatBox.CHAT_LINES[99];
	    System.arraycopy(ChatBox.CHAT_LINES, 0, ChatBox.CHAT_LINES, 1, 99);
	    if (line == null) {
	        line = new ChatLine(type, effectFlags, crownedName, name, simpleName, clan, quickchatMessageId, message);
	    } else {
	        line.set(type, effectFlags, crownedName, name, simpleName, clan, quickchatMessageId, message);
	    }
	    ChatBox.CHAT_LINES[0] = line;
	    ChatBox.NUM_CHAT_LINES++;
	    client.anInt7391 = client.anInt7347;
	}
	
	public static void writeGameMessage(String message) {
	    ChatBox.writeMessage(ChatMessageType.GAME, 0, "", "", "", message);
	}

	public static void writeUnfilterableMessage(String message) {
	    ChatBox.writeMessage(ChatMessageType.UNFILTERABLE, 0, "", "", "", message);
	}

	public static void writeMessage(ChatMessageType type, int effectFlags, String crownedName, String name, String nameSimple, String message) {
	    ChatBox.writeMessage(type, effectFlags, crownedName, name, nameSimple, message, null, -1);
	}

	public static ChatLine getLine(int index) {
	    return index >= 0 && index < 100 ? ChatBox.CHAT_LINES[index] : null;
	}

	public static void reset() {
	    for (int index = 0; index < 100; index++) {
	        ChatBox.CHAT_LINES[index] = null;
	    }
	    ChatBox.NUM_CHAT_LINES = 0;
	}

	public static int getLineCount() {
	    return ChatBox.NUM_CHAT_LINES;
	}

	public static int getChatlineIdentifier() {
	    return ++ChatBox.CHATLINE_IDENTIFIER - 1;
	}

	public static String addQuestChatIcons(int[] questId) {
	    StringBuilder questStringbuilder = new StringBuilder();
	    int iconIndex = StaticMedia.totalChatIcons;
	    for (int index = 0; index < questId.length; index++) {
	        QuestType questType = TypeLists.QUEST.list(questId[index]);
	        if (questType.graphicId != -1) {
	            NativeSprite chatIcon = (NativeSprite) ChatBox.CHAT_ICONS.get(questType.graphicId);
	            if (chatIcon == null) {
	                SpriteType spriteType = SpriteType.listSingle(Resource.SPRITES, questType.graphicId, 0);
	                if (spriteType != null) {
	                    chatIcon = Renderers.activeToolkit.generateSprite(spriteType, true);
	                    ChatBox.CHAT_ICONS.put(chatIcon, questType.graphicId);
	                }
	            }
	            if (chatIcon != null) {
	                StaticMedia.chatIcons[iconIndex] = chatIcon;
	                questStringbuilder.append(" <img=").append(iconIndex++).append(">");
	            }
	        }
	    }
	    return questStringbuilder.toString();
	}

	public static void setFilter(CS2Executor executor) {
	    executor.intStackPtr -= 3;
	    PUBLIC_FILTER = executor.intStack[executor.intStackPtr];
	    PRIVATE_FILTER = ChatFilter.forId(executor.intStack[executor.intStackPtr + 1]);
	    if (PRIVATE_FILTER == null) {
	        PRIVATE_FILTER = ChatFilter.FRIENDS_ONLY;
	    }
	    TRADE_FILTER = executor.intStack[executor.intStackPtr + 2];
	    BufferedConnectionContext context = client.getConnectionContext();
	    TcpPacket packet = PacketEncoder.createPacket(ClientProt.CHAT_SETFILTER, context.isaac);
	    packet.buffer.writeByte(PUBLIC_FILTER);
	    packet.buffer.writeByte(PRIVATE_FILTER.id);
	    packet.buffer.writeByte(TRADE_FILTER);
	    context.queuePacket(packet);
	}
}
