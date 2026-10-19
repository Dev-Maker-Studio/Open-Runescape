package com.rs.game.content.skills.agility.agilitypyramid;

import com.rs.game.World;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;
import com.rs.utils.WorldUtil;
import java.util.ArrayList;
import java.util.List;
import org.darkan.core.gameval.gen.Varbit;

@PluginEventHandler
public class AgilityPyramidBlock extends NPC {

	private int timer;
	private final Tile dangerTile;

	public AgilityPyramidBlock(int id, Tile tile) {
		super(id, tile);
		dangerTile = transform(getId() == 3125 ? 2 : 0, getId() == 3125 ? 0 : 2, 0);
	}

	@Override
	public void processNPC() {
		if (timer-- <= 0) {
			for (Player player : World.getPlayersInChunkRange(getChunkId(), 1))
				player.getVars().setVarBit(Varbit.agility_pyramid_level, getId() == 3125 ? 1 : 3);
			forceMoveVisually(dangerTile, 20, 0);
			timer = 10;
		}
		if (timer > 7)
			for (Player p : getHittablePlayers()) {
				int dist = 0;
				if (getId() == 3125)
					dist = p.getX() - dangerTile.getX();
				else
					dist = p.getY() - dangerTile.getY();
				if (dist == 0)
					dist = 2;
				p.lock();
				p.setNextAnimation(new Animation(3066));
				final Tile tile = p.transform(getId() == 3125 ? dist : 0, getId() == 3125 ? 0 : dist, -1);
				p.forceMove(tile, 10, 30, () -> p.applyHit(new Hit(null, 80, HitLook.TRUE_DAMAGE)));
				int virtualPlane = p.getTempAttribs().getI("AgilityPyramidVirtualPlane");
				p.getTempAttribs().setI("AgilityPyramidVirtualPlane", Math.max(virtualPlane - 1, 0));
			}
		if (timer == 4)
			for (Player player : World.getPlayersInChunkRange(getChunkId(), 1))
				player.getVars().setVarBit(Varbit.agility_pyramid_level, 0);
	}

	public List<Player> getHittablePlayers() {
		List<Player> players = new ArrayList<>();
		for (Player player : World.getPlayersInChunkRange(getChunkId(), 1)) {
			if (player.getPlane() != getPlane() || player.isLocked())
				continue;
			if (WorldUtil.collides(player.getTile(), dangerTile, 1, 2))
				players.add(player);
		}
		return players;
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 3124, 3125 }, AgilityPyramidBlock::new);

}
