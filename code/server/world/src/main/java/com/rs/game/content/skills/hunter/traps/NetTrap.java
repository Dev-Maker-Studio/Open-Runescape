package com.rs.game.content.skills.hunter.traps;

import org.darkan.core.map.WorldCollision;
import com.rs.game.World;
import com.rs.game.content.skills.hunter.BoxHunterNPC;
import com.rs.game.content.skills.hunter.BoxHunterType;
import com.rs.game.content.skills.hunter.BoxTrapType;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.model.gameobject.OwnedObject;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;

import java.util.HashMap;
import java.util.Map;

public class NetTrap extends BoxStyleTrap {

	private enum TreeType {
		SWAMP(19679, 19681, 19678, 19676, 19677, 19674, 19675),
		DESERT(19652, 19651, 19650, 19657, 19656, 19655, 19654),
		LAVA(19663, 19665, 19662, 19660, 19661, 19658, 19659),
		WILDY(19671, 19673, 19670, 19668, 19669, 19666, 19667),
		SQUIRRELS(28564, 28566, 28563, 28561, 28562, 28750, 28751);

		private final int base;
		private final int net;
		private final int setUp;
		private final int failing;
		private final int failed;
		private final int catching;
		private final int caught;

		private static final Map<Integer, TreeType> BY_BASE = new HashMap<>();

		static {
			for (TreeType t : TreeType.values())
				BY_BASE.put(t.base, t);
		}

		public static TreeType fromBase(int base) {
			return BY_BASE.get(base);
		}

		private TreeType(int base, int net, int setUp, int failing, int failed, int catching, int caught) {
			this.base = base;
			this.setUp = setUp;
			this.failing = failing;
			this.failed = failed;
			this.catching = catching;
			this.caught = caught;
			this.net = net;
		}
	}

	private OwnedObject tree;
	private final TreeType treeType;

	public NetTrap(Player player, Tile tile, GameObject tree) {
		super(player, BoxTrapType.TREE_NET, tile, TreeType.fromBase(tree.getId()).net, tree.getRotation());
		treeType = TreeType.fromBase(tree.getId());
		if (tree != null)
			this.tree = new OwnedObject(player, tree, treeType.setUp);
	}

	public NetTrap(Player player, Tile tile, TreeType type, int caught) {
		super(player, BoxTrapType.TREE_NET, tile, caught, 0);
		this.treeType = type;
	}

	@Override
	public void onCreate() {
		if (tree != null)
			tree.createReplace();
	}

	@Override
	public void onDestroy() {
		if (tree != null) {
			WorldCollision.addBlockedTile(tree.getTile());
			tree.destroy();
		}
	}

	@Override
	public void expire(Player player) {
		Tile tile = Tile.of(this.getTile());
		if (tree == null)
			tile = tile.transform(getRotation() == 1 ? 1 : getRotation() == 3 ? -1 : 0, getRotation() == 0 ? 1 : getRotation() == 2 ? -1 : 0, 0);
		World.addGroundItem(new Item(954, 1), tile, player, true, 60);
		World.addGroundItem(new Item(303, 1), tile, player, true, 60);
	}

	@Override
	public void handleCatch(BoxHunterNPC npc, boolean success) {
		BoxHunterType npcType = npc.getType(getOwner());
		if (npcType == null)
			return;
		destroy();
		NetTrap compTrap = new NetTrap(getOwner(), tree.getTile().transform(getRotation() == 3 ? -1 : 0, getRotation() == 2 ? -1 : 0, 0), treeType, success ? treeType.catching : treeType.failing);
		compTrap.setRouteType(RouteType.NORMAL);
		compTrap.setRotation(tree.getRotation());
		compTrap.createReplace();
		if (success) {
			npc.setNextAnimation(new Animation(-1));
			npc.setRespawnTask();
		}
		WorldTasks.schedule(0, () -> {
			compTrap.setNpcTrapped(npcType);
			compTrap.changeId(success ? treeType.caught : treeType.failed);
			compTrap.setStatus(success ? Status.SUCCESS : Status.FAIL);
		});
	}
}
