package com.rs.game.content.skills.hunter.traps;

import com.rs.game.World;
import com.rs.game.content.skills.hunter.BoxHunterNPC;
import com.rs.game.content.skills.hunter.BoxHunterType;
import com.rs.game.content.skills.hunter.BoxTrapType;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;

public class MarasamawPlant extends BoxStyleTrap {

	public MarasamawPlant(Player player, Tile tile) {
		super(player, BoxTrapType.MARASAMAW_PLANT, tile);
	}

	@Override
	public void handleCatch(BoxHunterNPC npc, boolean success) {
		BoxHunterType npcType = npc.getType(getOwner());
		if (npcType == null)
			return;
		if (success) {
			changeId(npcType.getObjectCatch());
			npc.anim(-1);
			npc.setRespawnTask();
		} else
			changeId(npcType.getObjectFail());
		setStatus(Status.CATCHING);
		if (success)
			WorldTasks.schedule(new Task() {
				@Override
				public void run() {
					World.sendObjectAnimation(MarasamawPlant.this, new Animation(3300));
				}
			}, 0);
		WorldTasks.schedule(new Task() {
			@Override
			public void run() {
				if (success)
					changeId(npcType.getObjectSuccess());
				setNpcTrapped(npcType);
				setStatus(success ? Status.SUCCESS : Status.FAIL);
			}
		}, 3);
	}
}
