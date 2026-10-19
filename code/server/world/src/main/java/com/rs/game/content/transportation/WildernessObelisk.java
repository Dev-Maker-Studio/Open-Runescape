package com.rs.game.content.transportation;

import com.rs.game.World;
import com.rs.game.content.skills.magic.Magic;
import com.rs.game.content.skills.magic.TeleType;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

public class WildernessObelisk {

	private static final Tile[] OBELISK_CENTER_TILES = { Tile.of(2978, 3864, 0), Tile.of(3033, 3730, 0), Tile.of(3104, 3792, 0), Tile.of(3154, 3618, 0), Tile.of(3217, 3654, 0), Tile.of(3305, 3914, 0) };
	private static final boolean[] IS_ACTIVE = new boolean[6];

	public static void activateObelisk(int id, final Player player) {
		final int index = id - 65616;
		final Tile center = OBELISK_CENTER_TILES[index];
		player.soundEffect(204, true);
		if (IS_ACTIVE[index]) {
			player.sendMessage("The obelisk is already active.");
			return;
		}
		IS_ACTIVE[index] = true;
		GameObject object = World.getObjectWithId(center, id);
		if (object == null) // still loading objects i guess
			return;
		World.sendObjectAnimation(object, new Animation(2226));
		World.sendObjectAnimation(World.getObjectWithId(center.transform(4, 0, 0), id), new Animation(2226));
		World.sendObjectAnimation(World.getObjectWithId(center.transform(0, 4, 0), id), new Animation(2226));
		World.sendObjectAnimation(World.getObjectWithId(center.transform(4, 4, 0), id), new Animation(2226));
		WorldTasks.schedule(new Task() {

			@Override
			public void run() {
				for (int x = 1; x < 4; x++)
					for (int y = 1; y < 4; y++)
						World.sendSpotAnim(center.transform(x, y, 0), new SpotAnim(661));
				Tile newCenter = OBELISK_CENTER_TILES[Utils.random(OBELISK_CENTER_TILES.length)];
					for (Player p : World.getPlayersInChunkRange(center.getChunkId(), 1)) {
						if (p == null || p.isLocked() || p.isDead() || (p.getX() < center.getX() + 1 || p.getX() > center.getX() + 3 || p.getY() < center.getY() + 1 || p.getY() > center.getY() + 3))
							continue;
						int offsetX = p.getX() - center.getX();
						int offsetY = p.getY() - center.getY();
						Magic.sendTeleportSpell(p, 8939, 8941, 1690, -1, 0, 0, Tile.of(newCenter.getX() + offsetX, newCenter.getY() + offsetY, 0), 3, false, TeleType.OBJECT, null);
					}
				IS_ACTIVE[index] = false;
			}

		}, 8);

	}
}