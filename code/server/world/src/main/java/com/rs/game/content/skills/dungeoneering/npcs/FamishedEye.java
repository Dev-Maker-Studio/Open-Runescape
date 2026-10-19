package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;

public class FamishedEye extends DungeonNPC {

	private final int weakness, type;
	private int sleepCycles;
	private boolean firstHit;
	private final WorldGorgerShukarhazh boss;

	public FamishedEye(final WorldGorgerShukarhazh boss, int id, Tile tile, final DungeonManager manager) {
		super(id, tile, manager);
		this.boss = boss;
		this.sleepCycles = -1;
		int rotationType = (id - 12436) / 15;
		this.weakness = findWeakness(rotationType);
		this.type = findType(rotationType);
		setCantFollowUnderCombat(true);
		setForceAgressive(true);
		int rotation = manager.getRoom(manager.getCurrentRoomReference(getTile())).getRotation() + rotationType;
		setFaceAngle(Utils.getAngleTo(Utils.ROTATION_DIR_X[(rotation + 1) & 0x3], Utils.ROTATION_DIR_Y[(rotation + 1) & 0x3]));
	}

	@Override
	public boolean lineOfSightTo(Object tile, boolean checkClose) {
		//because npc is under cliped data
		return getManager().isAtBossRoom(WorldUtil.targetToTile(tile));
	}

	@Override
	public void setNextFaceEntity(Entity entity) {
		//this boss doesnt face
	}

	@Override
	public void processHit(Hit hit) {
		if (weakness != hit.getLook().getMark())
			hit.setDamage((int) (hit.getDamage() * 0.2));
		super.processHit(hit);
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (sleepCycles == 45)
			resetSleepCycles();
		else if (sleepCycles >= 0)
			sleepCycles++;
	}

	private void resetSleepCycles() {
		sleepCycles = -1;
		setCantInteract(false);
		setNextAnimation(new Animation(-1));
		setHitpoints(getMaxHitpoints());
		boss.refreshCapDamage();
		for (Entity t : getPossibleTargets())
			if (t instanceof Player player)
				player.sendMessage("The creature shifts, and one of it's many eyes opens.");
	}

	@Override
	public void drop() {

	}

	@Override
	public void sendDeath(Entity source) {
		sleepCycles++;//start the cycles
		boss.refreshCapDamage();
		setCantInteract(true);
		setNextAnimation(new Animation(14917));
		for (Entity t : getPossibleTargets())
			if (t instanceof Player player)
				player.sendMessage("The creature shifts, and one of it's many eyes closes.");
	}

	public boolean isInactive() {
		return sleepCycles != -1;
	}

	public WorldGorgerShukarhazh getBoss() {
		return boss;
	}

	public int getType() {
		return type;
	}

	public int getWeaknessStyle() {
		return weakness;
	}

	private int findType(int rotation) {
		// mage, warrior, range
		return switch (type) {
			case 0 -> 2;
			case 1 -> 0;
			default -> 1;
		};
	}

	private int findWeakness(int type) {
		return switch (type) {
			// mage, warrior, range
			case 0 -> 1; //range was mage 2
			case 1 -> 2; //mage was melee 0
			default -> 0; //melee was range 1
		};
	}

	public boolean isFirstHit() {
		return firstHit;
	}

	public void setFirstHit(boolean firstHit) {
		this.firstHit = firstHit;
	}
}
