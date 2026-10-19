package com.rs.game.content.skills.slayer.npcs;

import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class HarpieBug extends NPC {

	public HarpieBug(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void handlePreHit(Hit hit) {
		if (hit.getSource() instanceof Player player) {
            if (player.getEquipment().getShieldId() != 7053)
				hit.setDamage(0);
		}
		super.handlePreHit(hit);
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(3153, HarpieBug::new);

}
