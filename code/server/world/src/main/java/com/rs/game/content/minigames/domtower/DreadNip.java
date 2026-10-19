package com.rs.game.content.minigames.domtower;

import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

public class DreadNip extends NPC {

	public static final String[] DREADNIP_MESSAGES = { "Your dreadnip couldn't attack so it left.", "The dreadnip gave up as you were too far away.", "Your dreadnip served its purpose and fled." };

	private final Player owner;
	private int ticks;

	public DreadNip(Player owner, int id, Tile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea) {
		super(id, tile);
		this.owner = owner;
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (owner == null || owner.hasFinished()) {
			finish(-1);
			return;
		}
		if (getCombat().getTarget() == null || getCombat().getTarget().isDead()) {
			finish(0);
			return;
		}
		if (Utils.getDistance(owner.getTile(), getTile()) >= 10) {
			finish(1);
			return;
		} else if (ticks++ == 33) {
			finish(2);
			return;
		}
	}

	private void finish(int index) {
		if (index != -1) {
			owner.sendMessage(DREADNIP_MESSAGES[index]);
			owner.getTempAttribs().removeB("hasDN");
		}
		this.finish();
	}

	public Player getOwner() {
		return owner;
	}

	public int getTicks() {
		return ticks;
	}
}
