package com.jagex.game.runetek5.config.quest

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.vardomain.VarDomain

/**
 * Manages quest type configurations. Unlike other type lists, this preloads
 * all quest types into an array cache on initialization.
 *
 * @author Brainpower - 01/05/2022
 */
class QuestTypeList(questTypeResource: Js5ResourceProvider?) {

    @JvmField val questTypeResource: Js5ResourceProvider? = questTypeResource
    @JvmField val size: Int
    @JvmField val questTypeCache: Array<QuestType?>

    init {
        size = if (questTypeResource != null) {
            questTypeResource.getArchiveFileCount(SharedConfigsType.QUESTS.id)
        } else {
            0
        }
        questTypeCache = arrayOfNulls(size)
        for (questId in 0 until size) {
            val data: ByteArray?
            synchronized(questTypeResource!!) {
                data = questTypeResource.getFile(SharedConfigsType.QUESTS.id, questId)
            }
            val questType = QuestType()
            if (data != null) {
                questType.decode(Packet(data))
            }
            questType.postDecode()
            questTypeCache[questId] = questType
            questTypeCache[questId]!!.questTypelist = this
        }
    }

    /**
     * Calculates the total quest points from all completed quests.
     *
     * @param varprovider The variable provider to check quest completion status
     * @return The total quest points earned
     */
    fun getQuestpointsCompletedQuests(varprovider: VarDomain): Int {
        var totalQuestpoints = 0
        for (questId in 0 until size) {
            val questType = list(questId)
            if (questType.isComplete(varprovider)) {
                totalQuestpoints += questType.questPointReward
            }
        }
        return totalQuestpoints
    }

    /**
     * Retrieves a quest type by ID from the preloaded cache.
     *
     * @param questId The quest ID
     * @return The quest type configuration
     */
    fun list(questId: Int): QuestType {
        return questTypeCache[questId]!!
    }
}
