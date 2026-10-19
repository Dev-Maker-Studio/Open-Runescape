package com.rs.game.content.skills.hunter;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class Jadinko extends NPC {

	public Jadinko(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		if (source instanceof Player player)
			player.addJadinkoFavor((getId() == 13820 ? 3 : getId() == 13821 ? 7 : 10));
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 13820, 13821, 13822 }, Jadinko::new);
}
