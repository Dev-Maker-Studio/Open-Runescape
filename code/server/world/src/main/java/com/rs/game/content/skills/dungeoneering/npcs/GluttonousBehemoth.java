package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.DungeonUtils;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

public final class GluttonousBehemoth extends DungeonBoss {

	private GameObject heal;
	private int ticks;

	public GluttonousBehemoth(Tile tile, DungeonManager manager, RoomReference reference) {
		super(DungeonUtils.getClosestToCombatLevel(Utils.range(9948, 9964), manager.getBossLevel()), tile, manager, reference);
		setHitpoints(getMaxHitpoints());
		setCantFollowUnderCombat(true);
	}

	public void setHeal(GameObject food) {
		ticks = 0;
		heal = food;
		removeCombatTarget();
	}

	@Override
	public void processNPC() {
		if (heal != null) {
			setNextFaceEntity(null);
			ticks++;
			if (ticks == 1)
				calcFollow(heal, true);
			else if (ticks == 5)
				setNextAnimation(new Animation(13720));
			else if (ticks < 900 && ticks > 7) {
				if (getHitpoints() >= (getMaxHitpoints() * 0.75)) {
					setNextAnimation(new Animation(-1));
					calcFollow(getRespawnTile(), true);
					ticks = 995;
					return;
				}
				heal(50 + Utils.random(50));
				setNextAnimation(new Animation(13720));
			} else if (ticks > 1000)
				heal = null;
			return;
		}
		super.processNPC();
	}

}
