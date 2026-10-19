package com.rs.game.content.minigames.barrows.npcs;

import com.rs.game.content.minigames.barrows.BarrowsController;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.OwnedNPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

import java.util.ArrayList;
import java.util.List;

public class BarrowsBrother extends OwnedNPC {

	private BarrowsController barrows;

	public BarrowsBrother(int id, Tile tile, BarrowsController barrows) {
		super(barrows.getPlayer(), id, tile, false);
		this.barrows = barrows;
		setIntelligentRouteFinder(true);
	}

	@Override
	public void sendDeath(Entity source) {
		if (barrows != null) {
			barrows.targetDied();
			barrows = null;
		}
		super.sendDeath(source);
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return getId() != 2030 ? 0 : Utils.random(3) == 0 ? 1 : 0;
	}

	@Override
	public void handlePreHitOut(Entity target, Hit hit) {
		super.handlePreHitOut(target, hit);
		if (getId() == 2027 && hit.getDamage() > 0 && Utils.random(4) == 0) {
			target.setNextSpotAnim(new SpotAnim(398));
			heal(hit.getDamage());
		}
	}

	public void disappear() {
		barrows = null;
		finish();
	}

	@Override
	public void finish() {
		if (hasFinished())
			return;
		if (barrows != null) {
			barrows.targetFinishedWithoutDie();
			barrows = null;
		}
		super.finish();
	}

	@Override
	public List<Entity> getPossibleTargets() {
		List<Entity> owners = new ArrayList<>();
		Player owner = getOwner();
		if (owner != null)
			owners.add(owner);
		return owners;
	}

}
