package com.rs.game.content.minigames.fightpits;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;

import java.util.ArrayList;
import java.util.List;

public class FightPitsNPC extends NPC {

	public FightPitsNPC(int id, Tile tile) {
		super(id, tile, true);
		setForceMultiArea(true);
		setNoDistanceCheck(true);
	}

	@Override
	public void sendDeath(Entity source) {
		setNextSpotAnim(new SpotAnim(2924 + getSize()));
		super.sendDeath(source);
	}

	@Override
	public List<Entity> getPossibleTargets() {
		ArrayList<Entity> possibleTarget = new ArrayList<>();
        possibleTarget.addAll(FightPits.arena);
		return possibleTarget;
	}

}
