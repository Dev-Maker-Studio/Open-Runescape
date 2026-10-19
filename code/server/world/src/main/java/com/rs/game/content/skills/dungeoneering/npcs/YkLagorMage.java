package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import kotlin.Pair;

public class YkLagorMage extends DungeonNPC {

	private final YkLagorThunderous boss;
	private int cycle;

	public YkLagorMage(YkLagorThunderous ykLagorThunderous, int id, Tile tile, DungeonManager manager, double multiplier) {
		super(id, tile, manager);
		boss = ykLagorThunderous;
		setNextFaceEntity(boss);
		setCantFollowUnderCombat(true);
	}

	@Override
	public void processNPC() {
		if (isDead() || boss == null)
			return;
		if (isUnderCombat()) {
			super.processNPC();
			return;
		}
		if (cycle > 0) {
			cycle--;
			return;
		}
		cycle = 5;
		setNextFaceEntity(boss);
		setNextAnimation(new Animation(3645));
		World.sendProjectile(this, boss, 2704, new Pair<>(39, 39), 55, 10, 0);
	}

	@Override
	public void drop() {

	}

	@Override
	public int getMaxHitpoints() {
		return 650;
	}

	@Override
	public int getCombatLevel() {
		return 65;
	}

	/*@Override
	public void sendDeath(Entity source) {
	super.sendDeath(source);
	}*/
}
