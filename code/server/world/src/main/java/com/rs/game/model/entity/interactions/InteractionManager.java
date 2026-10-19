package com.rs.game.model.entity.interactions;

import com.rs.game.model.entity.Entity;

public final class InteractionManager {

	private final Entity player;
	private Interaction interaction;

	public InteractionManager(Entity player) {
		this.player = player;
	}

	public void process() {
		if (interaction != null && (interaction.isStopped() || player.isDead() || !interaction.process(player)))
			forceStop();
	}

	public boolean setInteraction(Interaction skill) {
		forceStop();
		if (!skill.start(player)) {
			skill.stop(player);
			return false;
		}
		interaction = skill;
		return true;
	}

	public void forceStop() {
		if (interaction == null)
			return;
		player.stopFaceEntity();
		interaction.stop(player);
		interaction = null;
	}

	public Interaction getInteraction() {
		return interaction;
	}

	public boolean doingInteraction(Class<?> type) {
		if (interaction == null)
			return false;
		return type.isInstance(interaction);
	}
}
