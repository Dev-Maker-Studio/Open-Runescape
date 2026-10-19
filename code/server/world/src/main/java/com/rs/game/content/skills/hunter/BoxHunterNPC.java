package com.rs.game.content.skills.hunter;

import com.rs.game.World;
import com.rs.game.content.minigames.herblorehabitat.JadinkoType;
import com.rs.game.content.skills.hunter.traps.BoxStyleTrap;
import com.rs.game.content.skills.hunter.traps.BoxStyleTrap.Status;
import com.rs.game.model.entity.npc.NPC;
import com.rs.engine.pathfinder.RouteEvent;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.Constants;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@PluginEventHandler
public class BoxHunterNPC extends NPC {

	private int captureTicks;

	public BoxHunterNPC(int id, Tile tile, boolean spawned) {
		super(id, tile, spawned);
		setIgnoreNPCClipping(true);
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (timeSinceSpawned() < 3000)
			return;
		if (captureTicks > 0 || hasFinished()) {
			if (captureTicks > 20) {
				setRouteEvent(null);
				captureTicks = 0;
				return;
			}
			captureTicks++;
			return;
		}
		List<GameObject> objects = World.getSpawnedObjectsInChunkRange(getChunkId(), 1);
		if (objects == null)
			return;
		for (final GameObject o : objects) {
			if (!(o instanceof BoxStyleTrap trapO))
				continue;
			Player owner = trapO.getOwner();
			if (owner == null)
				continue;
			BoxHunterType type = getType(trapO.getOwner());
			if (type == null)
				continue;
			if (type.getQuestReqs() != null && !type.getQuestReqs().stream().allMatch(owner::isQuestComplete)) {
				continue;
			}
			if (type.getSkillReqs() != null && !type.getSkillReqs().entrySet().stream().allMatch(entry -> owner.getSkills().getLevel(entry.getKey()) >= entry.getValue())) {
				continue;
			}
			if (owner.getSkills().getLevel(Constants.HUNTER) < type.getLevel() || trapO.getStatus() != Status.IDLE || trapO.getLife() < 10 || trapO.getLife() > 75 || trapO.getTrapType() != type.getTrap() || trapO.getBait() != type.getBaitId() || !withinDistance(o.getTile(), 2))
				continue;
			captureTicks = 1;
			trapO.setStatus(Status.CATCHING);
			resetWalkSteps();
			setRouteEvent(new RouteEvent(trapO, () -> {
				if (trapO.getOwner() == null || trapO.isDestroyed() || trapO.getTrapType() != type.getTrap())
					return;
				boolean success = Utils.skillSuccess(owner.getSkills().getLevel(Constants.HUNTER), type.getRate1(), type.getRate99());
				faceObject(trapO);
				anim(success ? type.getAnimSuccess() : type.getAnimFail());
				trapO.handleCatch(this, success);
			}));
			break;
		}
	}

	@Override
	public void setRespawnTask() {
		super.setRespawnTask(4);
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(
			Stream.concat(
					Arrays.stream(JadinkoType.values()).map(JadinkoType::getNpcId).filter(i -> i != -1).distinct(),
					BoxHunterType.ID_MAP.keySet().stream())
			.toArray(), (npcId, tile) -> new BoxHunterNPC(npcId, tile, false));

	public BoxHunterType getType(Player owner) {
		if (owner == null)
			return null;
		return BoxHunterType.forId(getDefinitions(owner).getId());
	}
}