package com.jagex.game.runetek5.interaction

import com.jagex.game.runetek5.utils.TextUtils

class Friend {
    @JvmField var displayName: String = ""
    @JvmField var referrer: Boolean = false
    @JvmField var worldId: Int = 0
    @JvmField var worldName: String = ""
    @JvmField var friendsChatRank: Int = 0
    @JvmField var platform: Int = 0
    @JvmField var previousDisplayName: String = ""
    @JvmField var worldFlags: Int = 0

    companion object {
        @JvmStatic
        fun forName(displayName: String?): Friend? {
            if (displayName == null) {
                return null
            }
            val formattedDisplayName = TextUtils.formatDisplayname(displayName) ?: return null
            for (index in 0 until FriendList.FRIEND_COUNT) {
                val friend = FriendList.FRIENDS[index]
                if (friend != null) {
                    val friendFormattedDisplayName = TextUtils.formatDisplayname(friend.displayName)
                    if (friendFormattedDisplayName != null && friendFormattedDisplayName == formattedDisplayName) {
                        return friend
                    }
                }
            }
            return null
        }
    }
}
