package com.rs.game.content.minigames.fightkiln.npcs;

import com.rs.game.content.minigames.fightkiln.FightKilnController;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.map.pathfinder.Direction;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

import java.util.ArrayList;
import java.util.List;

public class HarAken extends NPC {
	
	private static final int TENTACLE_CAP = 25;

	private long time;
	private long spawnTentacleTime;
	private boolean underLava;
	private final List<HarAkenTentacle> tentacles;

	private final FightKilnController controller;

	public void resetTimer() {
		underLava = !underLava;
		if (time == 0)
			spawnTentacleTime = System.currentTimeMillis() + 9000;
		if(tentacles.size() == TENTACLE_CAP)
			spawnTentacleTime = System.currentTimeMillis() + 25000;
		time = System.currentTimeMillis() + (underLava ? 43000 : 31000);
	}

	@Override
	public boolean ignoreWallsWhenMeleeing() {
		return true;
	}

	@Override
	public boolean canMove(Direction dir) {
		return false;
	}

	public HarAken(int id, Tile tile, FightKilnController controller) {
		super(id, tile, true);
		setForceMultiArea(true);
		this.controller = controller;
		tentacles = new ArrayList<>();
		setCantFollowUnderCombat(true);
	}

	@Override
	public void sendDeath(Entity source) {
		setNextSpotAnim(new SpotAnim(2924 + getSize()));
		if (time != 0) {
			removeTentacles();
			controller.removeNPC();
			time = 0;
		}
		super.sendDeath(source);
	}

	@Override
	public void processNPC() {
		if (isDead())
			return;
		stopFaceEntity();
	}

	public void process() {
		if (isDead())
			return;
		if (time != 0) {
			if (time < System.currentTimeMillis())
				if (underLava) {
					controller.showHarAken();
					resetTimer();
				} else
					controller.hideHarAken();
			if (tentacles.size() < TENTACLE_CAP && spawnTentacleTime < System.currentTimeMillis())
				spawnTentacle();

		}
	}

	public void spawnTentacle() {
		tentacles.add(new HarAkenTentacle(Utils.random(2) == 0 ? 15209 : 15210, controller.getTentacleTile(), this));
		spawnTentacleTime = System.currentTimeMillis() + Utils.random(18000, 23000);
	}

	public void removeTentacles() {
		for (HarAkenTentacle t : tentacles)
			t.finish();
		tentacles.clear();
	}

	public void removeTentacle(HarAkenTentacle tentacle) {
		tentacles.remove(tentacle);

	}

}
