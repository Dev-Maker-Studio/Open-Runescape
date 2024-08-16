package com.jagex.game.runetek5.interaction;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.TextUtils;

/**
 * 
 * @author Steven - 18/09/2023
 *
 */
public class FriendList {
	
	public static final Friend[] FRIENDS = new Friend[200];
	public static int FRIEND_COUNT = 0;
	public static int LOADED = 0;
	
	public static void add(String displayName) {
	    if (displayName != null) {
	        if (FRIEND_COUNT < 200) {
	            String formattedDisplayName = TextUtils.formatDisplayname(displayName);
	            if (formattedDisplayName != null) {
	                for (int index = 0; index < FRIEND_COUNT; index++) {
	                    Friend friend = FRIENDS[index];
	                    String friendFormattedDisplayName = TextUtils.formatDisplayname(friend.displayName);
	                    if (friendFormattedDisplayName != null && friendFormattedDisplayName.equals(formattedDisplayName)) {
	                        ChatBox.writeGameMessage(displayName + LocalizedText.ALREADY_ON_FRIENDS_LIST.translate(client.CURRENT_LANGUAGE));
	                        return;
	                    }
	                    if (friend.username != null) {
	                        String friendFormattedUsername = TextUtils.formatDisplayname(friend.username);
	                        if (friendFormattedUsername != null && friendFormattedUsername.equals(formattedDisplayName)) {
	                            ChatBox.writeGameMessage(displayName + LocalizedText.ALREADY_ON_FRIENDS_LIST.translate(client.CURRENT_LANGUAGE));
	                            return;
	                        }
	                    }
	                }
	                for (int index = 0; index < IgnoreList.IGNORE_COUNT; index++) {
	                    Ignore ignore = IgnoreList.IGNORED[index];
	                    String ignoreFilteredDisplayName = TextUtils.formatDisplayname(ignore.unfilteredUsername);
	                    if (ignoreFilteredDisplayName != null && ignoreFilteredDisplayName.equals(formattedDisplayName)) {
	                        ChatBox.writeGameMessage(LocalizedText.PLEASE_REMOVE_IGNORE.translate(client.CURRENT_LANGUAGE)
	                        		+ displayName + LocalizedText.FROM_IGNORE_FIRST.translate(client.CURRENT_LANGUAGE));
	                        return;
	                    }
	                    if (ignore.displayName != null) {
	                        String ignoreFormattedDisplayName = TextUtils.formatDisplayname(ignore.displayName);
	                        if (ignoreFormattedDisplayName != null && ignoreFormattedDisplayName.equals(formattedDisplayName)) {
	                            ChatBox.writeGameMessage(LocalizedText.PLEASE_REMOVE_IGNORE.translate(client.CURRENT_LANGUAGE)
	                            		+ displayName + LocalizedText.FROM_IGNORE_FIRST.translate(client.CURRENT_LANGUAGE));
	                            return;
	                        }
	                    }
	                }
	                if (TextUtils.formatDisplayname(PlayerUpdate.MY_PLAYER.displayName).equals(formattedDisplayName)) {
	                    ChatBox.writeGameMessage(LocalizedText.CANT_ADD_SELF_TO_FRIEND.translate(client.CURRENT_LANGUAGE));
	                } else {
	                    BufferedConnectionContext context = client.getConnectionContext();
	                    TcpPacket packet = PacketEncoder.createPacket(ClientProt.ADD_FRIEND, context.isaac);
	                    packet.buffer.writeByte(TextUtils.getLength(displayName));
	                    packet.buffer.writeString(displayName);
	                    context.queuePacket(packet);
	                }
	            }
	        } else {
	            ChatBox.writeGameMessage(LocalizedText.FRIEND_LIST_FULL.translate(client.CURRENT_LANGUAGE));
	        }
	    }
	}
	
	public static void remove(String displayName) {
	    if (displayName != null) {
	        String deleted = TextUtils.formatDisplayname(displayName);
	        if (deleted != null) {
	            for (int index = 0; index < FRIEND_COUNT; index++) {
	                Friend friend = FRIENDS[index];
	                String display = friend.displayName;
	                String current = TextUtils.formatDisplayname(display);
	                if (TextUtils.matches(displayName, deleted, display, current, (byte) -79)) {
	                    if (--FRIEND_COUNT - index >= 0) {
	                        System.arraycopy(FRIENDS, index + 1, FRIENDS, index, FRIEND_COUNT - index);
	                    }
	                    client.anInt7386 = client.anInt7347;
	                    BufferedConnectionContext context = client.getConnectionContext();
	                    TcpPacket packet = PacketEncoder.createPacket(ClientProt.REMOVE_FRIEND, context.isaac);
	                    packet.buffer.writeByte(TextUtils.getLength(displayName));
	                    packet.buffer.writeString(displayName);
	                    context.queuePacket(packet);
	                    break;
	                }
	            }
	        }
	    }
	}
	
	public static boolean contains(String displayName) {
	    if (displayName == null) {
	        return false;
	    } 
	    for (int index = 0; index < FRIEND_COUNT; index++) {
	        if (displayName.equalsIgnoreCase(FRIENDS[index].displayName)) {
	            return true;
	        }
	    }
	    return displayName.equalsIgnoreCase(PlayerUpdate.MY_PLAYER.displayName);
	}
	
	public static int getIndex(String displayName) {
	    if (displayName != null) {
	        for (int index = 0; index < FRIEND_COUNT; index++) {
	            if (displayName.equalsIgnoreCase(FRIENDS[index].displayName)) {
	                return index;
	            }
	        }
	    }
	    return -1;
	}
}
