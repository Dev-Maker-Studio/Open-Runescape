package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.DungeonUtils;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

public class HobgoblinGeomancer extends DungeonBoss {

	public HobgoblinGeomancer(Tile tile, DungeonManager manager, RoomReference reference) {
		super(DungeonUtils.getClosestToCombatLevel(Utils.range(10059, 10072), manager.getBossLevel()), tile, manager, reference);
		setHitpoints(getMaxHitpoints());
	}

	public void sendTeleport(final Tile tile, final RoomReference room) {
		setCantInteract(true);
		setNextAnimation(new Animation(12991, 70));
		setNextSpotAnim(new SpotAnim(1576, 70, 0));
		WorldTasks.scheduleTimer(5, (ticks) -> {
			setCantInteract(false);
			setNextAnimation(new Animation(-1));
			tele(World.getFreeTile(getManager().getRoomCenterTile(room), 6));
			resetReceivedHits();
			return false;
		});
	}
}
