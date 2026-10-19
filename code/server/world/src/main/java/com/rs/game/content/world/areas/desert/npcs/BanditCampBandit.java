package com.rs.game.content.world.areas.desert.npcs;

import com.rs.game.content.bosses.godwars.factions.GodFaction;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

import java.util.ArrayList;
import java.util.List;

@PluginEventHandler
public class BanditCampBandit extends NPC {

	public BanditCampBandit(int id, Tile tile, boolean spawned) {
		super(id, tile, spawned);
		setForceAgressive(true); // to ignore combat lvl
		setIgnoreDocile(true);
	}

	@Override
	public List<Entity> getPossibleTargets() {
		List<Entity> targets = super.getPossibleTargets();
		ArrayList<Entity> targetsCleaned = new ArrayList<>();
		for (Entity t : targets) {
			if (!(t instanceof Player p) || (!GodFaction.ZAMORAK.isWearing(p) && !GodFaction.SARADOMIN.isWearing(p)))
				continue;
			targetsCleaned.add(t);
		}
		return targetsCleaned;
	}

	@Override
	public void setCombatTarget(Entity entity) {
		if (entity instanceof Player p && (GodFaction.ZAMORAK.isWearing(p) || GodFaction.SARADOMIN.isWearing(p)))
			forceTalk(GodFaction.ZAMORAK.isWearing(p) ? "Prepare to suffer, Zamorakian scum!" : "Time to die, Saradominist filth!");
		super.setCombatTarget(entity);
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 1926, 1931 }, (npcId, tile) -> new BanditCampBandit(npcId, tile, false));

}
