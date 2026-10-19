package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.DungeonUtils;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NecroLord extends DungeonBoss {

	private int resetTicks;
	private final List<SkeletalMinion> skeletons;

	public NecroLord(Tile tile, DungeonManager manager, RoomReference reference) {
		super(DungeonUtils.getClosestToCombatLevel(Utils.range(11737, 11751), manager.getBossLevel()), tile, manager, reference);
		setCantFollowUnderCombat(true); //force can't walk
		setLureDelay(Integer.MAX_VALUE);//doesn't stop focusing on target
		skeletons = new CopyOnWriteArrayList<>();
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (!isUnderCombat() && skeletons != null && !skeletons.isEmpty()) {
			resetTicks++;
			if (resetTicks == 50) {
				resetSkeletons();
				resetTicks = 0;
			}
		}
	}

	@Override
	public double getMagePrayerMultiplier() {
		return 0.6;
	}

	public void addSkeleton(Tile tile) {
		SkeletalMinion npc = new SkeletalMinion(this, 11722, tile, getManager()); //TODO scale to level
		npc.setForceAgressive(true);
		skeletons.add(npc);
		World.sendSpotAnim(tile, new SpotAnim(2399));
	}

	public void resetSkeletons() {
		for (SkeletalMinion skeleton : skeletons)
			skeleton.sendDeath(this);
		skeletons.clear();
	}

	public void removeSkeleton(DungeonNPC sk) {
		skeletons.remove(sk);
	}

	/*
	 * because necrolord room has a safespot which shouldnt
	 */
	@Override
	public boolean lineOfSightTo(Object tile, boolean checkClose) {
		//because npc is under cliped data
		return getManager().isAtBossRoom(WorldUtil.targetToTile(tile));
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		resetSkeletons();
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
}
