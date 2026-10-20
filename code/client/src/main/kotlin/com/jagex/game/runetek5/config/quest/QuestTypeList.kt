package com.jagex.game.runetek5.config.quest

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.vardomain.VarDomain

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

    fun list(questId: Int): QuestType {
        return questTypeCache[questId]!!
    }
}
