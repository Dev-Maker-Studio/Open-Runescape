package com.rs.game.content.world.areas.dungeons.ancientcavern;

import com.rs.game.content.skills.magic.Magic;
import com.rs.game.content.skills.magic.TeleType;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Controller;
import com.rs.game.model.entity.player.Equipment;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemClickHandler;

@PluginEventHandler
public class KuradalDungeonController extends Controller {

	public static ItemClickHandler handleFerociousRings = new ItemClickHandler(new Object[] { 15398, 15399, 15400, 15401, 15402 }, new String[] { "Rub", "Kuradal" }, e -> {
			if (e.getItem().getId() == 15402) {
				Magic.sendItemTeleportSpell(e.getPlayer(), true, 9603, 1684, 3, Tile.of(1739, 5312, 1));
				if (e.isEquipped())
					e.getPlayer().getEquipment().deleteSlot(Equipment.RING);
				else
					e.getPlayer().getInventory().deleteItem(e.getItem().getId(), 1);
			}
			else
			{
				Magic.sendItemTeleportSpell(e.getPlayer(), true, 9603, 1684, 3, Tile.of(1739, 5312, 1));
				e.getItem().setId(e.getItem().getId()+1);
				e.getPlayer().getInventory().refresh();
				e.getPlayer().getEquipment().refresh(Equipment.RING);
			}
	});

	public KuradalDungeonController() {}

	@Override
	public void start() {
		player.tele(Tile.of(1661, 5257, 0));
	}

	@Override
	public boolean canAttack(Entity target) {
		if (player.getSlayer().isOnTaskAgainst((NPC)target))
			return true;
		player.sendMessage("This creature is not assigned to you.");
		return false;
	}

	@Override
	public boolean keepCombating(Entity target) {
		if (player.getSlayer().isOnTaskAgainst((NPC)target))
			return true;
		player.sendMessage("This creature is not assigned to you.");
		return false;
	}

	@Override
	public boolean processObjectClick1(GameObject object) {
		if (object.getId() == 47231) {
			player.tele(Tile.of(1735, 5313, 1));
			removeController();
		}
		return true;
	}

	@Override
	public void onTeleported(TeleType type) {
		removeController();
	}

	@Override
	public void moved() {
		if (player.getRegionId() != 6482)
			removeController();
	}

	@Override
	public void processOutgoingHit(Hit hit, Entity target) {
		if (target != player && hit.getDamage() > 0 && player.getEquipment().getRingId() >= 15398 && player.getEquipment().getRingId() <= 15402)
			hit.setDamage(hit.getDamage()+40);
	}

	@Override
	public void processNPCDeath(NPC npc) {
		if (!player.getSlayer().isOnTaskAgainst(npc))
			return;
		if (Utils.random(128) == 0)
			npc.sendDrop(player, new Item(15398));
	}

	@Override
	public boolean login() {
		return false;
	}

	@Override
	public boolean logout() {
		return false;
	}

}
