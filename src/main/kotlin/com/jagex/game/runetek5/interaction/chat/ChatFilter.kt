package com.jagex.game.runetek5.interaction.chat

/**
 *
 * @author Brainpower - 18/09/2023
 *
 */
enum class ChatFilter(
    @JvmField val id: Int
) {
    ONLINE(0),
    FRIENDS_ONLY(1),
    OFFLINE(2);

    companion object {
        @JvmStatic
        fun forId(filterId: Int): ChatFilter? {
            val allStatuses = values()
            for (index in allStatuses.indices) {
                val status = allStatuses[index]
                if (filterId == status.id) {
                    return status
                }
            }
            return null
        }
    }
}
