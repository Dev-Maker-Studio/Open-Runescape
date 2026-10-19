package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;

import java.util.Map;

public final class InterfaceAction extends CutsceneAction {

	private final int interfaceId;
	private final int delay;

	public InterfaceAction(int interfaceId, int actionDelay) {
		super(null, actionDelay);
		this.interfaceId = interfaceId;
		delay = actionDelay;
	}

	@Override
	public void process(final Player player, Map<String, Object> objects) {
		player.getInterfaceManager().sendInterface(interfaceId);
		WorldTasks.schedule(delay, () -> player.getInterfaceManager().removeCentralInterface());
	}

}