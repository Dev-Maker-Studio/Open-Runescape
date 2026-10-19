package com.rs.game.content.skills.runecrafting.runespan;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.interactions.PlayerEntityInteractionAction;
import org.darkan.core.model.Animation;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

import java.util.Arrays;

@PluginEventHandler
public class SiphonCreaturesInteraction extends PlayerEntityInteractionAction<SiphonAction> {

	public static NPCClickHandler clickHandler = new NPCClickHandler(false, Arrays.stream(Creature.values()).map(c -> c.npcId).toArray(), new String[] { "Siphon", "Chip off" }, e -> {
		Creature creature = getCreature(e.getNPC().getId());
		if (creature == null)
			return;
		e.getPlayer().getInteractionManager().setInteraction(new SiphonCreaturesInteraction(creature, e.getNPC()));
	});

	public SiphonCreaturesInteraction(Creature creatures, NPC creature) {
		super(creature, new SiphonAction(creatures, creature), 7);
	}

	public static boolean siphon(Player player, NPC npc) {
		Creature creature = getCreature(npc.getId());
		if (creature == null)
			return false;
		player.getInteractionManager().setInteraction(new SiphonCreaturesInteraction(creature, npc));
		return true;
	}

	private static Creature getCreature(int id) {
		for (Creature creature : Creature.values())
			if (creature.npcId == id)
				return creature;
		return null;
	}

	@Override
	public boolean canStart(Player player) {
		return getAction().start(player);
	}

	@Override
	public boolean checkAll(Player player) {
		return getAction().checkAll(player);
	}

	@Override
	public void onStop(Entity player) {
		player.setNextAnimation(new Animation(16599));
		player.getActionManager().setActionDelay(3);
	}
}
