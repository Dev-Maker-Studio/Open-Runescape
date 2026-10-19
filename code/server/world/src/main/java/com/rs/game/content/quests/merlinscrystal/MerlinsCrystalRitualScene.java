package com.rs.game.content.quests.merlinscrystal;

import org.darkan.core.map.pathfinder.Direction;
import com.rs.game.World;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.OwnedNPC;
import com.rs.game.model.entity.player.Controller;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;

public class MerlinsCrystalRitualScene extends Controller {
	final int THRANTAX_SPIRIT = 238;

	@Override
	public void start() {
		player.lock();
		playCutscene();
	}

	private void playCutscene() {
		boolean hasSpirit = false;
		for(NPC npc : World.getNPCsInChunkRange(player.getChunkId(), 1))
            if (npc.getId() == THRANTAX_SPIRIT) {
                hasSpirit = true;
                break;
            }
		if(hasSpirit)
			;
		else {
            OwnedNPC spirit = new OwnedNPC(player, THRANTAX_SPIRIT, Tile.of(2780, 3516, 0), true);
			player.musicTrack(449);
			spirit.setNextSpotAnim(new SpotAnim(1605, 0, 0));
			spirit.setCantInteract(true);
			spirit.faceDir(Direction.SOUTH);
			spirit.setRandomWalk(false);
		}
		player.startConversation(new ThrantaxMerlinsCrystalD(player).getStart());
	}

	@Override
	public boolean login() {
		forceClose();
		return false;
	}

	@Override
	public boolean logout() {
		player.unlock();
		return false;
	}

	@Override
	public void forceClose() {
		player.unlock();
	}

}
