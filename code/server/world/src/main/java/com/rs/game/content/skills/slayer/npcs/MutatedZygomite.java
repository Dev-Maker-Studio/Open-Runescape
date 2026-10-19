package com.rs.game.content.skills.slayer.npcs;

import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class MutatedZygomite extends ConditionalDeath {

	boolean lvl74;

	public static NPCClickHandler mutatedZygomite = new NPCClickHandler(new Object[]{3344, 3345}, new String[] {"Pick"}, e -> MutatedZygomite.transform(e.getPlayer(), e.getNPC()));

	public MutatedZygomite(int id, Tile tile) {
		super(7421, null, false, id, tile);
		lvl74 = id == 3344;
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (!isUnderCombat() && !isDead())
			resetNPC();
	}

	private void resetNPC() {
		transformIntoNPC(lvl74 ? 3344 : 3345);
		tele(getRespawnTile());
	}

	@Override
	public void onRespawn() {
		resetNPC();
	}

	public static void transform(final Player player, final NPC npc) {
		if (npc.isCantInteract())
			return;
		player.setNextAnimation(new Animation(2988));
		npc.transformIntoNPC(npc.getId() + 2);
		npc.setNextAnimation(new Animation(2982));
		npc.setCantInteract(true);
		npc.getCombat().setTarget(player);
		npc.setHitpoints(npc.getMaxHitpoints());
		npc.resetLevels();
		WorldTasks.schedule(new Task() {
			@Override
			public void run() {
				npc.setCantInteract(false);
			}
		}, 1);
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 3344, 3345, 3346, 3347 }, MutatedZygomite::new);
}
