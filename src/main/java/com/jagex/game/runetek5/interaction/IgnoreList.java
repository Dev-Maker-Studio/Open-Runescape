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
public class IgnoreList {
	
	public static final Ignore[] IGNORED = new Ignore[100];
	public static int IGNORE_COUNT = 0;

	public static void add(String displayName, boolean flag) {
	    if (displayName != null) {
	        if (IgnoreList.IGNORE_COUNT >= 100) {
	            ChatBox.writeGameMessage(LocalizedText.IGNORE_LIST_FULL.translate(client.CURRENT_LANGUAGE));
	        } else {
	            String formattedDisplayName = TextUtils.formatDisplayname(displayName);
	            if (formattedDisplayName != null) {
	                for (int index = 0; index < IgnoreList.IGNORE_COUNT; index++) {
	                    Ignore ignore = IgnoreList.IGNORED[index];
	                    String formattedIgnoreUnfilteredUsername = TextUtils.formatDisplayname(ignore.unfilteredUsername);
	                    if (formattedIgnoreUnfilteredUsername != null && formattedIgnoreUnfilteredUsername.equals(formattedDisplayName)) {
	                        ChatBox.writeGameMessage(displayName + LocalizedText.ALREADY_IGNORED.translate(client.CURRENT_LANGUAGE));
	                        return;
	                    }
	                    if (ignore.displayName != null) {
	                        String formattedIgnoreDisplayName = TextUtils.formatDisplayname(ignore.displayName);
	                        if (formattedIgnoreDisplayName != null && formattedIgnoreDisplayName.equals(formattedDisplayName)) {
	                            ChatBox.writeGameMessage(displayName + LocalizedText.ALREADY_IGNORED.translate(client.CURRENT_LANGUAGE));
	                            return;
	                        }
	                    }
	                }
	                for (int index = 0; index < FriendList.FRIEND_COUNT; index++) {
	                    Friend friend = FriendList.FRIENDS[index];
	                    String formattedFriendDisplayname = TextUtils.formatDisplayname(friend.displayName);
	                    if (formattedFriendDisplayname != null && formattedFriendDisplayname.equals(formattedDisplayName)) {
	                        ChatBox.writeGameMessage(LocalizedText.PLEASE_REMOVE.translate(client.CURRENT_LANGUAGE) + displayName
	                        		+ LocalizedText.FROM_FRIENDS_FIRST.translate(client.CURRENT_LANGUAGE));
	                        return;
	                    }
	                    if (friend.previousDisplayName != null) {
	                        String formattedFriendUsername = TextUtils.formatDisplayname(friend.previousDisplayName);
	                        if (formattedFriendUsername != null && formattedFriendUsername.equals(formattedDisplayName)) {
	                            ChatBox.writeGameMessage(LocalizedText.PLEASE_REMOVE.translate(client.CURRENT_LANGUAGE) + displayName
	                            		+ LocalizedText.FROM_FRIENDS_FIRST.translate(client.CURRENT_LANGUAGE));
	                            return;
	                        }
	                    }
	                }
	                if (TextUtils.formatDisplayname(PlayerUpdate.MY_PLAYER.displayName).equals(formattedDisplayName)) {
	                    ChatBox.writeGameMessage(LocalizedText.CANT_ADD_SELF_TO_IGNORE.translate(client.CURRENT_LANGUAGE));
	                } else {
	                    BufferedConnectionContext context = client.getConnectionContext();
	                    TcpPacket packet = PacketEncoder.createPacket(ClientProt.ADD_IGNORE, context.isaac);
	                    packet.buffer.writeByte(TextUtils.getLength(displayName) + 1);
	                    packet.buffer.writeString(displayName);
	                    packet.buffer.writeByte(flag ? 1 : 0);
	                    context.queuePacket(packet);
	                }
	            }
	        }
	    }
	}
	
	public static void remove(String displayName) {
	    if (displayName != null) {
	        String formattedDisplayName = TextUtils.formatDisplayname(displayName);
	        if (formattedDisplayName != null) {
	            for (int index = 0; index < IGNORE_COUNT; index++) {
	                Ignore ignore = IGNORED[index];
	                String unfilteredUsername = ignore.unfilteredUsername;
	                String formattedIgnoredDisplayName = TextUtils.formatDisplayname(unfilteredUsername);
	                if (TextUtils.matches(displayName, formattedDisplayName, unfilteredUsername, formattedIgnoredDisplayName, (byte) -110)) {
	                    if (--IGNORE_COUNT - index >= 0)
	                        System.arraycopy(IGNORED, index + 1, IGNORED, index, IGNORE_COUNT - index);
	                    client.anInt7386 = client.anInt7347;
	                    BufferedConnectionContext context = client.getConnectionContext();
	                    TcpPacket packet = PacketEncoder.createPacket(ClientProt.REMOVE_IGNORE, context.isaac);
	                    packet.buffer.writeByte(TextUtils.getLength(displayName));
	                    packet.buffer.writeString(displayName);
	                    context.queuePacket(packet);
	                    break;
	                }
	            }
	        }
	    }
	}
	
	public static boolean contains(String username) {
	    if (username != null) {
	        for (int index = 0; index < IGNORE_COUNT; index++) {
	            Ignore ignore = IGNORED[index];
	            if (username.equalsIgnoreCase(ignore.unfilteredUsername)) {
	                return true;
	            }
	            if (username.equalsIgnoreCase(ignore.displayName)) {
	                return true;
	            }
	        }
	    }
	    return false;
	}
}
