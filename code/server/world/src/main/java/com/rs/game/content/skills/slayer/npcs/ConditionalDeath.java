package com.rs.game.content.skills.slayer.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class ConditionalDeath extends NPC {

	private final int requiredItem;
	private final String deathMessage;
	private final boolean remove;

	public ConditionalDeath(int requiredItem, String deathMessage, boolean remove, int id, Tile tile) {
		super(id, tile);
		this.requiredItem = requiredItem;
		this.deathMessage = deathMessage;
		this.remove = remove;
	}

	public int getRequiredItem() {
		return requiredItem;
	}

	private boolean removeItem(Player player) {
		if (getHitpoints() < (getMaxHitpoints() * 0.1) && (player.getEquipment().getWeaponId() == requiredItem || player.getInventory().containsItem(requiredItem, 1))) {
			if (remove)
				player.getInventory().deleteItem(requiredItem, 1);
			return true;
		}
		return false;
	}

	public boolean useHammer(Player player) {
		if (removeItem(player)) {
			if (deathMessage != null)
				player.sendMessage(deathMessage);
			// missing emote
			if (getId() == 14849)
				player.setNextAnimation(new Animation(15845));
			if (getId() == 1610) {
				player.setNextAnimation(new Animation(1755));
				transformIntoNPC(1827);
				setNextAnimation(new Animation(9513));
				WorldTasks.schedule(10, () -> setNPC(1610));
			}
			setHitpoints(0);
			super.sendDeath(player);
			return true;
		}
		return false;
	}

	@Override
	public void sendDeath(Entity source) {
		if (source instanceof Player player) {
			if ((player.hasLearnedQuickBlows() || player.getEquipment().getWeaponId() == requiredItem || player.getEquipment().getGlovesId() == requiredItem) && useHammer(player))
				return;
			player.sendMessage("The " + getName() + " is on its last legs! Finish it quickly!");
		}
		setHitpoints(1);
	}

	public static NPCClickHandler gargSmash = new NPCClickHandler(new Object[] {1610}, new String[]{"Smash"}, e -> {
		if (e.getNPC() instanceof ConditionalDeath cd)
			cd.useHammer(e.getPlayer());
	});

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 1610, 1631, 1632, 2803, 2804, 2805, 2806, 2807, 2808, 14849 }, (npcId, tile) -> {
		if (npcId == 1631 || npcId == 1632)
			return new ConditionalDeath(4161, "The rockslug shrivels and dies.", true, npcId, tile);
		if (npcId == 1610)
			return new ConditionalDeath(4162, "The gargoyle breaks into pieces as you slam the hammer onto its head.", false, npcId, tile);
		if (npcId == 14849)
			return new ConditionalDeath(23035, null, false, npcId, tile);
		else
			return new ConditionalDeath(6696, null, true, npcId, tile);
	});
}