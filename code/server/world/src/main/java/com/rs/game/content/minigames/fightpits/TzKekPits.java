package com.rs.game.content.minigames.fightpits;

import com.rs.game.World;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;

public class TzKekPits extends FightPitsNPC {

	public TzKekPits(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void sendDeath(Entity source) {
		final NPCCombatDefinitions defs = getCombatDefinitions();
		resetWalkSteps();
		getCombat().removeTarget();
		setNextAnimation(null);
		Tile tile = Tile.of(getTile());
		WorldTasks.scheduleTimer(loop -> {
			if (loop == 0) {
				setNextAnimation(new Animation(defs.getDeathEmote()));
				setNextSpotAnim(new SpotAnim(2924 + getSize()));
			} else if (loop >= defs.getDeathDelay()) {
				reset();
				FightPits.addNPC(new FightPitsNPC(2738, tile));
				Tile finTile = tile;
				if (World.floorAndWallsFree(getPlane(), tile.getX() + 1, tile.getY(), 1))
					finTile = tile.transform(1, 0, 0);
				else if (World.floorAndWallsFree(getPlane(), tile.getX() - 1, tile.getY(), 1))
					finTile = tile.transform(-1, 0, 0);
				else if (World.floorAndWallsFree(getPlane(), tile.getX(), tile.getY() - 1, 1))
					finTile = tile.transform(0, -1, 0);
				else if (World.floorAndWallsFree(getPlane(), tile.getX(), tile.getY() + 1, 1))
					finTile = tile.transform(0, 1, 0);
				FightPits.addNPC(new FightPitsNPC(2738, finTile));
				finish();
				return false;
			}
			return true;
		});
	}

	@Override
	public void removeHitpoints(Hit hit) {
		super.removeHitpoints(hit);
		if (hit.getLook() != HitLook.MELEE_DAMAGE || hit.getSource() == null)
			return;
		hit.getSource().applyHit(new Hit(this, 10, HitLook.TRUE_DAMAGE));
	}
}
