package com.jagex.game.runetek5.interaction.chat;

/**
 * 
 * @author Brainpower - 18/09/2023
 *
 */
public enum ChatFilter {

	ONLINE(0),
	FRIENDS_ONLY(1),
	OFFLINE(2);
	
	public final int id;
	
	ChatFilter(int id) {

		this.id = id;
	}
	
	public static ChatFilter forId(int filterId) {
	    ChatFilter[] allStatuses = values();
	    for (int index = 0; index < allStatuses.length; index++) {
	        ChatFilter status = allStatuses[index];
	        if (filterId == status.id) {
	            return status;
	        }
	    }
	    return null;
	}
}
