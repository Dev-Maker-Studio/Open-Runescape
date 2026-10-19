package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.DungeonConstants.GuardianMonster;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.DungeonUtils;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

import java.util.ArrayList;

public class ForgottenWarrior extends Guardian {

	public ForgottenWarrior(int id, Tile tile, DungeonManager manager) {
		super(id, tile, manager);
	}

	@Override
	public void drop() {
		super.drop();
		GuardianMonster m = GuardianMonster.forId(getId());
		if (m == null)
			return;
		int size = getSize();
		ArrayList<Item> drops = new ArrayList<>();
		if (m.name().contains("WARRIOR"))
			drops.add(new Item(DungeonUtils.getRandomMeleeGear(Math.min(1+Utils.random(getTier()), 10))));
		else if (m.name().contains("MAGE"))
			drops.add(new Item(DungeonUtils.getRandomMagicGear(Math.min(1+Utils.random(getTier()), 10))));
		else
			drops.add(new Item(DungeonUtils.getRandomRangeGear(Math.min(1+Utils.random(getTier()), 10))));
		for (Item item : drops)
			World.addGroundItemNoExpire(item, Tile.of(getCoordFaceX(size), getCoordFaceY(size), getPlane()));
	}

}
