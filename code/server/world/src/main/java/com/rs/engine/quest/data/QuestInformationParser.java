package com.rs.engine.quest.data;

import org.darkan.core.cache.EnumDefinitions;
import org.darkan.core.cache.StructDefinitions;
import world.gregs.voidps.cache.definition.data.EnumDefinition;
import world.gregs.voidps.cache.definition.data.StructDefinition;
import com.rs.engine.quest.Quest;
import com.rs.game.model.entity.player.Skills;
import world.gregs.voidps.type.Tile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class QuestInformationParser {

	public static final int JOURNAL_SLOTID = 847;
	public static final int STARTLOC_PRE_HASH = 854;
	public static final int STARTLOC_HASH = 850;
	public static final int STEP2_LOC_HASH = 851;
	public static final int MEMBERS = 856;
	public static final int NAME = 845;
	public static final int NAME_SORT = 846;
	public static final int START_NPC = 691;
	public static final int REQUIRED_ITEMS = 949;
	public static final int REWARDS = 951;
	public static final int ENEMIES_TO_DEFEAT = 950;
	public static final int START_HINT = 948;
	public static final int DIFFICULTY = 848;

	public static final int USES_NEW_INTERFACE = 694;
	public static final int QUESTPOINT_REQ = 895;

	public static final int QUEST_REQ_START = 859;
	public static final int QUEST_REQ_END = 870;
	public static final int REQ_SKILL_START = 871;
	public static final int REQ_LEVEL_START = 872;
	public static final int REQ_SKILL_END = 883;
	public static final int REQ_LEVEL_END = 884;

	public static HashMap<Integer, QuestInformation> QUESTS_ID = new HashMap<>();
	public static HashMap<String, QuestInformation> QUESTS_NAME = new HashMap<>();
	public static HashMap<Integer, QuestInformation> QUESTS_SLOTID = new HashMap<>();

	public static void init() {
		EnumDefinition questEnum = EnumDefinitions.getEnum(2252);
		for (int i : questEnum.getValues().keySet()) {
			Quest quest = Quest.forSlot(i);
			if (questEnum.getIntValue(i) != -1) {
				StructDefinition detail = StructDefinitions.getStruct(questEnum.getIntValue(i));
				Map<Integer, Object> map = detail.getValues();
				QuestInformation qi = new QuestInformation(i, (String) map.get(NAME), (Integer) map.get(JOURNAL_SLOTID));
				if (map.get(START_NPC) != null)
					qi.setStartNpc((Integer) map.get(START_NPC));
				else
					qi.setStartNpc(-1);
				if (map.get(QUESTPOINT_REQ) != null)
					qi.setQpReq((Integer) map.get(QUESTPOINT_REQ));
				else
					qi.setQpReq(0);
				if (map.get(STARTLOC_HASH) != null)
					qi.setStartLocation(Tile.of((Integer) map.get(STARTLOC_HASH)));
				if (map.get(QUEST_REQ_START) != null) {
					int numReqs = 0;
					for (int q = QUEST_REQ_START;q <= QUEST_REQ_END;q++)
						if (map.get(q) != null)
							numReqs++;
					int[] reqs = new int[numReqs];
					for (int q = 0;q < reqs.length;q++) {
						reqs[q] = (int) map.get(QUEST_REQ_START+q);
						qi.addPreReq(Quest.forSlot((int) map.get(QUEST_REQ_START+q)));
					}
				}
				if (map.get(REQ_SKILL_START) != null) {
					int numReqs = 0;
					for (int q = REQ_SKILL_START;q <= REQ_SKILL_END;q += 2)
						if (map.get(q) != null)
							numReqs++;
					int[][] reqs = new int[numReqs][2];
					for (int q = 0;q < reqs.length;q++) {
						reqs[q][0] = (int) map.get(REQ_SKILL_START+(q * 2));
						reqs[q][1] = (int) map.get(REQ_LEVEL_START+(q * 2));
						qi.addSkillReq(reqs[q][0], reqs[q][1]);
						qi.addPreReqSkillReq(reqs[q][0], reqs[q][1]);
					}
				}

				if (map.get(USES_NEW_INTERFACE) != null && (int) map.get(USES_NEW_INTERFACE) == 1)
					qi.setUsesNewInterface(true);

				QUESTS_ID.put(i, qi);
				QUESTS_SLOTID.put(qi.getSlotId(), qi);
				QUESTS_NAME.put(qi.getName(), qi);
				QuestDefinitions.getQuestDefinitions(quest.getId()).setExtraInfo(qi);
			}
		}
		for (QuestInformation info : QUESTS_ID.values())
			for (Quest quest : new HashSet<>(info.getPreReqs()))
				addPreReqs(info, quest);
		Quest.VOID_STARES_BACK.getDefs().getExtraInfo().addSkillReq(Skills.DEFENSE, 25);
	}

	public static void addPreReqs(QuestInformation info, Quest quest) {
		for (Quest q : new HashSet<>(quest.getDefs().getExtraInfo().getPreReqs()))
			addPreReqs(info, q);
		for (Quest preReq : new HashSet<>(quest.getDefs().getExtraInfo().getPreReqs()))
			info.addPreReq(preReq);
		for (int skill : quest.getDefs().getExtraInfo().getSkillReq().keySet())
			info.addPreReqSkillReq(skill, quest.getDefs().getExtraInfo().getSkillReq().get(skill));
	}

}
