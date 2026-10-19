package com.rs.game.content.minigames.partyroom;

import org.darkan.core.model.ObjectShape;
import com.rs.game.World;
import com.rs.game.World.DropMethod;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;

public class Balloon extends GameObject {

	private Item item;
	private boolean popped = false;

	public Balloon(int id, int rotation, int x, int y, int plane) {
		super(id, ObjectShape.SCENERY_INTERACT, rotation, x, y, plane);
	}

	public Item getItem() {
		return item;
	}

	public Balloon setItem(Item item) {
		this.item = item;
		return this;
	}

	public void handlePop(final Player player) {
		if (!popped) {
			player.setNextAnimation(new Animation(794));
			popped = true;
			player.lock();
			World.removeObject(this);
			final GameObject poppedBalloon = new GameObject(getId() + 8, ObjectShape.SCENERY_INTERACT, this.getRotation(), getX(), getY(), getPlane());
			World.spawnObject(poppedBalloon);
			player.incrementCount("Party balloons popped");
			WorldTasks.schedule(new Task() {
				@Override
				public void run() {
					if (item != null)
						World.addGroundItem(item, Tile.of(getX(), getY(), getPlane()), player, true, 60, DropMethod.NORMAL);
					World.removeObject(poppedBalloon);
					player.unlock();
				}
			}, 1);
		}
	}

}
