package com.jagex.game.runetek5.config.quest;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.vardomain.VarDomain;

/**
 * 
 * @author Brainpower - 01/05/2022
 *
 */
public class QuestTypeList {

    public final Js5ResourceProvider questTypeResource;

    public final int size;

    public final QuestType[] questTypeCache;

    public QuestTypeList(Js5ResourceProvider questTypeResource) {
        this.questTypeResource = questTypeResource;
        if (this.questTypeResource != null) {
        	this.size = this.questTypeResource.getArchiveFileCount(SharedConfigsType.QUESTS.id);
        } else {
        	this.size = 0;
        }
        this.questTypeCache = new QuestType[this.size];
        for (int questId = 0; questId < this.size; questId++) {
            byte[] data;
            synchronized (this.questTypeResource) {
                data = this.questTypeResource.getFile(SharedConfigsType.QUESTS.id, questId);
            }
            QuestType questType = new QuestType();
            if (data != null) {
                questType.decode(new Packet(data));
            }
            questType.postDecode();
            this.questTypeCache[questId] = questType;
            this.questTypeCache[questId].questTypelist = this;
        }
    }

    int getQuestpointsCompletedQuests(VarDomain varprovider) {
        int totalQuestpoints = 0;
        for (int questId = 0; questId < this.size; questId++) {
            QuestType questType = list(questId);
            if (questType.isComplete(varprovider)) {
                totalQuestpoints += questType.questPointReward;
            }
        }
        return totalQuestpoints;
    }

    public QuestType list(int questId) {
        return this.questTypeCache[questId];
    }
}
