package com.jagex.game.runetek5.interaction;

import com.jagex.game.runetek5.utils.TextUtils;

public class Friend {
    public String displayName;
    public boolean referrer;
    public int worldId;
    public String worldName;
    public int friendsChatRank;
    public int platform;
    public String previousDisplayName;
    public int worldFlags;

    public static Friend forName(String displayName) {
        if (displayName == null) {
            return null;
        }
        String formattedDisplayName = TextUtils.formatDisplayname(displayName);
        if (formattedDisplayName == null) {
            return null;
        }
        for (int index = 0; index < FriendList.FRIEND_COUNT; index++) {
            Friend friend = FriendList.FRIENDS[index];
            if (friend != null) {
                String friendFormattedDisplayName = TextUtils.formatDisplayname(friend.displayName);
                if (friendFormattedDisplayName != null && friendFormattedDisplayName.equals(formattedDisplayName)) {
                    return friend;
                }
            }
        }
        return null;
    }
}