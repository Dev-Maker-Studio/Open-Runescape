package com.rs.game.model.entity.player.actions;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.actions.Action;

public final class ActionManager {

	private final Entity entity;
	private Action action;
	private int actionDelay;

	public ActionManager(Entity entity) {
		this.entity = entity;
	}

	public void process() {
		if (action != null)
			if (entity.isDead())
				forceStop();
			else if (!action.process(entity))
				forceStop();
		if (actionDelay > 0) {
			actionDelay--;
			return;
		}
		if (action == null || entity == null)
			return;
		int delay = action.processWithDelay(entity);
		if (delay == -1) {
			forceStop();
			return;
		}
		actionDelay += delay;
	}

	public boolean setAction(Action skill) {
		forceStop();
		if (!skill.start(entity))
			return false;
		action = skill;
		return true;
	}

	public void forceStop() {
		if (action == null)
			return;
		action.stop(entity);
		action = null;
	}

	public int getActionDelay() {
		return actionDelay;
	}

	public void addActionDelay(int skillDelay) {
		actionDelay += skillDelay;
	}

	public void setActionDelay(int skillDelay) {
		actionDelay = skillDelay;
	}

	public boolean hasSkillWorking() {
		return action != null;
	}

	public Action getAction() {
		return action;
	}

	public boolean doingAction(Class<? extends Action> type) {
		if (action == null)
			return false;
		return type.isInstance(action);
	}
}
