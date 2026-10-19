package com.rs.game.content.skills.dungeoneering.npcs.bosses;

import com.rs.game.content.skills.dungeoneering.DungeonConstants;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.DungeonNPC;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.DropSets;
import com.rs.utils.drop.DropTable;

import java.util.List;

public class DungeonBoss extends DungeonNPC {

	private RoomReference reference;

	public DungeonBoss(int id, Tile tile, DungeonManager manager, RoomReference reference) {
		super(id, tile, manager);
		setReference(reference);
		resetBonuses();
		setForceAgressive(true);
		setIntelligentRouteFinder(true);
		setLureDelay(3000);
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		getManager().openStairs(getReference());
	}

	@Override
	public void processNPC() {
		super.processNPC();
		resetBonuses();
	}

	@Override
	public void drop() {
		DropTable[] drops = DropSets.getDropSet(getId()).getTables();
		if (drops == null || drops.length == 0)
			return;

		// TODO: Possible drop table mismatch with MEDIUM_DUNGEON?
		DropTable drop;
		if (getManager().getParty().getSize() == DungeonConstants.Size.Large)
			drop = drops[Utils.random(100) < 90 ? drops.length - 1 : Utils.random(drops.length)];
		else if (getManager().getParty().getSize() == DungeonConstants.Size.Large)
			drop = drops[Utils.random(100) < 60 ? drops.length - 1 : Utils.random(drops.length)];
		else
			drop = drops[Utils.random(drops.length)];
		List<Player> players = getManager().getParty().getTeam();
		if (players.isEmpty())
			return;
		Player killer = players.get(Utils.random(players.size()));
		if (drop != null)
			for (Item item : drop.toItemArr())
				sendDrop(killer, item);
	}


	@Override
	public void sendDrop(Player player, Item item) {
		List<Player> players = getManager().getParty().getTeam();
		if (players.isEmpty())
			return;
		player.getInventory().addItemDrop(item);
		player.sendMessage("<col=D2691E>You received: " + item.getAmount() + " " + item.getName() + ".");
		for (Player p2 : players) {
			if (p2 == player)
				continue;
			p2.sendMessage("<col=D2691E>" + player.getDisplayName() + " received: " + item.getAmount() + " " + item.getName() + ".");
		}
	}

	public RoomReference getReference() {
		return reference;
	}

	public void setReference(RoomReference reference) {
		this.reference = reference;
	}
}
