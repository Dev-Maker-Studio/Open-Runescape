package com.rs.game.content.skills.construction;

import org.darkan.core.model.ObjectShape;
import com.rs.game.content.skills.construction.poh.HouseConstants;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;

public class SitChair extends PlayerAction {

	private final Tile originalTile;
	private final Tile chairTile;
	private boolean tped;
	private final int animation;

	public SitChair(Player player, GameObject object) {
		animation = HouseConstants.getSitAnimation(object.getId());
		originalTile = Tile.of(player.getTile());
		chairTile = object.getTile();
		Tile face = Tile.of(player.getTile());
		if (object.getType() == ObjectShape.SCENERY_INTERACT) {
			if (object.getRotation() == 0)
				face = face.transform(0, -1, 0);
			else if (object.getRotation() == 2)
				face = face.transform(0, 1, 0);
			else if (object.getRotation() == 3)
				face = face.transform(1, 0, 0);
			else if (object.getRotation() == 1)
				face = face.transform(-1, 0, 0);
		} else if (object.getType() == ObjectShape.GROUND_INTERACT)
			if (object.getRotation() == 1)
				face = face.transform(-1, 1, 0);
			else if (object.getRotation() == 2)
				face = face.transform(1, 1, 0);
			else if (object.getRotation() == 0)
				face = face.transform(-1, -1, 0);
			else if (object.getRotation() == 3)
				face = face.transform(1, -1, 0);
		player.setNextFaceTile(face);
	}

	@Override
	public boolean start(Player player) {
		setActionDelay(player, 1);
		return true;
	}

	@Override
	public boolean process(Player player) {
		return true;
	}

	@Override
	public int processWithDelay(Player player) {
		if (!tped) {
			player.tele(chairTile);
			tped = true;
		}
		player.setNextAnimation(new Animation(animation));
		return 0;
	}

	@Override
	public void stop(final Player player) {
		player.lock(1);
		player.tele(originalTile);
		player.setNextAnimation(new Animation(-1));
	}
}