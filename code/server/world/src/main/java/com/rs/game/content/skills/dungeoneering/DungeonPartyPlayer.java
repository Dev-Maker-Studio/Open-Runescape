package com.rs.game.content.skills.dungeoneering;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.gameval.gen.Varbit;

public class DungeonPartyPlayer {

	private final Player player;
	private int deaths;

	public DungeonPartyPlayer(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public void refreshDeaths() {
		player.getVars().setVarBit(Varbit.ntk_trap_active, deaths); //deaths
	}

	public void increaseDeaths() {
		if (deaths == 15)
			return;
		deaths++;
		refreshDeaths();
	}
}
