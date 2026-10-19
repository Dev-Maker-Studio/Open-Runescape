package com.rs.game.content.skills.hunter.traps;

import com.rs.game.content.skills.hunter.BoxTrapType;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;

public class DeadfallTrap extends BoxStyleTrap {

	public DeadfallTrap(Player player, GameObject orig) {
		super(player, BoxTrapType.DEAD_FALL, orig.getTile());
		setRotation(orig.getRotation());
		routeType = RouteType.NORMAL;
	}
}
