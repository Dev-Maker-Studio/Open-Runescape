package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.DungeonUtils;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

public final class UnholyCrossbearer extends DungeonBoss {

	public UnholyCrossbearer(int id, Tile tile, DungeonManager manager, RoomReference reference) {
		super(DungeonUtils.getClosestToCombatLevel(Utils.range(10111, 10127), (int)Math.ceil(manager.getBossLevel() - manager.getBossLevel()*0.15)), tile, manager, reference);
	}

}
