package com.rs.game.model.entity.player.managers;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.player.Player;

public final class CutsceneManager {

	private final Player player;
	private Cutscene cutscene;
	
	public CutsceneManager(Player player) {
		this.player = player;
	}

	public void process() {
		if ((cutscene == null) || cutscene.process())
			return;
		cutscene = null;
	}

	public void logout() {
		if (hasCutscene())
			cutscene.logout();
	}

	public boolean hasCutscene() {
		return cutscene != null;
	}

	public boolean play(Cutscene cutscene) {
		if (hasCutscene() || (cutscene == null))
			return false;
		cutscene.setPlayer(player);
		cutscene.createObjectMap();
		cutscene.construct(player);
		this.cutscene = cutscene;
		return true;
	}

}
