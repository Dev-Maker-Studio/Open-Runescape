package com.rs.game.content.minigames.fightkiln.npcs;

import com.rs.game.content.minigames.fightkiln.FightKilnController;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;

import java.util.List;

public class FightKilnNPC extends NPC {

	private final FightKilnController controller;

	public FightKilnNPC(int id, Tile tile, FightKilnController controller) {
		super(id, tile, true);
		setForceMultiArea(true);
		setNoDistanceCheck(true);
		setIgnoreDocile(true);
		setForceAgressive(true);
		this.controller = controller;
	}

	private int getDeathGfx() {
        return switch (getId()) {
            case 15201 -> 2926;
            case 15202 -> 2927;
            case 15203 -> 2957;
            case 15213, 15214, 15204 -> 2928;
            case 15205 -> 2959;
            case 15206, 15207 -> 2929;
            case 15208, 15211, 15212 -> 2973;
            default -> 2926;
        };
	}

	@Override
	public void sendDeath(Entity source) {
		final NPCCombatDefinitions defs = getCombatDefinitions();
		resetWalkSteps();
		getCombat().removeTarget();
		setNextAnimation(null);
		controller.checkCrystal();
		setNextSpotAnim(new SpotAnim(getDeathGfx()));
		WorldTasks.scheduleTimer(loop -> {
			if (loop == 0)
				setNextAnimation(new Animation(defs.getDeathEmote()));
			else if (loop >= defs.getDeathDelay()) {
				reset();
				finish();
				controller.removeNPC();
				return false;
			}
			return true;
		});
	}

	@Override
	public List<Entity> getPossibleTargets() {
		return queryNearbyPlayersByTileRangeAsEntityList(64, player -> !player.isDead());
	}

	@Override
	public double getMagePrayerMultiplier() {
		return 0.1;
	}

	@Override
	public double getRangePrayerMultiplier() {
		return 0.1;
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return 0.1;
	}

}
