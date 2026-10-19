package com.rs.game.content.holidayevents.christmas;

import com.rs.game.World;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.interactions.PlayerEntityInteraction;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemEquipHandler;
import com.rs.plugin.handlers.PlayerClickHandler;
import kotlin.Pair;

@PluginEventHandler
public class SnowballInteraction extends PlayerEntityInteraction {

	public static ItemEquipHandler handleSnowballWield = new ItemEquipHandler(11951, e -> e.getPlayer().setPlayerOption(e.equip() ? "Pelt" : "null", 8, true));
	public static PlayerClickHandler handlePelt = new PlayerClickHandler(false, "Pelt", e -> e.getPlayer().getInteractionManager().setInteraction(new SnowballInteraction(e.getTarget())));

	public SnowballInteraction(Entity target) {
		super(target, 7);
	}

	@Override
	public void interact(Player player) {
		player.setNextFaceTile(target.getTile());
		if (player.getInventory().containsItem(11951, 1))
			player.getInventory().deleteItem(11951, 1);
		else {
			player.getEquipment().deleteItem(player.getEquipment().getWeaponId(), 1);
			player.getAppearance().generateAppearanceData();
		}
		if (target instanceof Player)
			player.sendMessage("You pelt " + ((Player)target).getDisplayName() + " with a snowball.");
		player.setNextAnimation(new Animation(7530));
		player.resetWalkSteps();
		WorldTasks.schedule(1, () -> World.sendProjectile(player, target, 861, new Pair<>(6, 10), 0, 5, 20, 0, p -> target.setNextSpotAnim(new SpotAnim(862))));
	}

	@Override
	public boolean canStart(Player player) {
		return true;
	}

	@Override
	public boolean checkAll(Player player) {
		return true;
	}

	@Override
	public void onStop(Player player) {

	}
}
