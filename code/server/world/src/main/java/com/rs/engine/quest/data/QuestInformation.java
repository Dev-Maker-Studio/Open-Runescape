package com.rs.engine.quest.data;

import com.rs.engine.quest.Quest;
import world.gregs.voidps.type.Tile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QuestInformation {
	private final int id;
	private final String name;
	private int startNpc;
	private Tile startLocation;
	private final Set<Quest> preReqs = new HashSet<>();
	private final Map<Integer, Integer> skillReqs = new HashMap<>();
	private final Map<Integer, Integer> preReqSkillReqs = new HashMap<>();
	private int qpReq;
	private final int slotId;
	private boolean usesNewInterface;

	public QuestInformation(int id, String name, int slotId) {
		this.id = id;
		this.name = name;
		this.slotId = slotId;
	}

	public int getId() {
		return id;
	}

	public int getStartNpc() {
		return startNpc;
	}

	public void setStartNpc(int startNpc) {
		this.startNpc = startNpc;
	}

	public void setUsesNewInterface(boolean usesNewInterface) {
		this.usesNewInterface = usesNewInterface;
	}

	public boolean usesNewInterface() {
		return usesNewInterface;
	}

	public Tile getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Tile startLocation) {
		this.startLocation = startLocation;
	}

	public Set<Quest> getPreReqs() {
		return preReqs;
	}

	public void addPreReq(Quest quest) {
		preReqs.add(quest);
	}

	public Map<Integer, Integer> getSkillReq() {
		return skillReqs;
	}

	public void addSkillReq(int skillId, int level) {
		if (skillReqs.get(skillId) == null || level > skillReqs.get(skillId))
			skillReqs.put(skillId, level);
	}

	public int getQpReq() {
		return qpReq;
	}

	public void setQpReq(int qpReq) {
		this.qpReq = qpReq;
	}

	public String getName() {
		return name;
	}

	public int getSlotId() {
		return slotId;
	}

	public Map<Integer, Integer> getPreReqSkillReqs() {
		return preReqSkillReqs;
	}

	public void addPreReqSkillReq(int skillId, int level) {
		if (preReqSkillReqs.get(skillId) == null || level > preReqSkillReqs.get(skillId))
			preReqSkillReqs.put(skillId, level);
	}
}