package com.rs.game.content.quests.holygrail;

import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.map.pathfinder.Direction;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class BlackKnightTitan extends NPC {
	public BlackKnightTitan(int id, Tile tile, boolean permaDeath) {
		super(id, tile, permaDeath);
	}

	@Override
	public boolean canMove(Direction dir) {
		return false;
	}

	@Override
	public void sendDeath(final Entity source) {
		if(source instanceof Player player && player.getEquipment().getWeaponId() == 35) {
			super.sendDeath(source);
			player.sendMessage("Well done, you have defeated the Black Knight Titan!");
			boolean isRun = player.getRun();
			player.setRunHidden(false);
			player.addWalkSteps(Tile.of(player.getX() >= 2791 ? 2790 : 2792, 4722, 0), 4, false);
			WorldTasks.delay(2, () -> player.setRunHidden(isRun));
			return;
		}
		resetHP();
	}

	@Override
	public boolean ignoreWallsWhenMeleeing() {
		return true;
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(221, (npcId, tile) -> new BlackKnightTitan(npcId, tile, false));

	public static NPCClickHandler handleDialogue = new NPCClickHandler(new Object[]{221}, new String[]{"Talk-to"}, e -> e.getPlayer().startConversation(new Dialogue()
            .addNPC(221, HeadE.CHILD_CALM_TALK, "I am the Black Knight Titan! You must pass through me before you can continue in this realm!")
            .addPlayer(HeadE.HAPPY_TALKING, "Ok, have at ye oh evil knight!")
            .addPlayer(HeadE.HAPPY_TALKING, "Actually I think I'll run away!")
    ));
}
