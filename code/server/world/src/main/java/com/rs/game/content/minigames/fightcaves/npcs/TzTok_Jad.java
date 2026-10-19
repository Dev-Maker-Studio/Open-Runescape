package com.rs.game.content.minigames.fightcaves.npcs;

import com.rs.game.content.minigames.fightcaves.FightCavesController;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;

public class TzTok_Jad extends FightCavesNPC {

	private boolean spawnedMinions;
	private final FightCavesController controller;

	public TzTok_Jad(int id, Tile tile, FightCavesController controller) {
		super(id, tile);
		this.controller = controller;
		setNoDistanceCheck(true);
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (!spawnedMinions && getHitpoints() < getMaxHitpoints() / 2) {
			spawnedMinions = true;
			controller.spawnHealers();
		}
	}

	@Override
	public void sendDeath(Entity source) {
		final NPCCombatDefinitions defs = getCombatDefinitions();
		resetWalkSteps();
		getCombat().removeTarget();
		setNextAnimation(null);
		WorldTasks.scheduleTimer(loop -> {
			if (loop == 0) {
				setNextAnimation(new Animation(defs.getDeathEmote()));
				setNextSpotAnim(new SpotAnim(2924 + getSize()));
			} else if (loop >= defs.getDeathDelay()) {
				reset();
				finish();
				if (source instanceof Player p)
					p.sendNPCKill(getDefinitions().getName());
				controller.win();
				return false;
			}
			return true;
		});
	}

}
