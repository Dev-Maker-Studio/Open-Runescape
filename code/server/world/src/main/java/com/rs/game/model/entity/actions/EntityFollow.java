package com.rs.game.model.entity.actions;

import org.darkan.core.map.pathfinder.Route;
import com.rs.engine.pathfinder.RouteFinderKt;
import com.rs.game.content.Effect;
import com.rs.game.model.entity.Entity;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;

public class EntityFollow extends Action {

	private final Entity target;

	public EntityFollow(Entity target) {
		this.target = target;
	}

	@Override
	public boolean start(Entity entity) {
		entity.faceEntity(target);
		if (checkAll(entity))
			return true;
		entity.stopFaceEntity();
		return false;
	}

	private boolean checkAll(Entity entity) {
		if (entity.isDead() || entity.hasFinished() || target.isDead() || target.hasFinished())
			return false;
		if (entity.getPlane() != target.getPlane())
			return false;
		if (entity.hasEffect(Effect.FREEZE))
			return true;
		int distanceX = entity.getX() - target.getX();
		int distanceY = entity.getY() - target.getY();
		int size = entity.getSize();
		int maxDistance = 16;
		if (entity.getPlane() != target.getPlane() || distanceX > size + maxDistance || distanceX < -1 - maxDistance || distanceY > size + maxDistance || distanceY < -1 - maxDistance)
			return false;
		int lastFaceEntity = target.getLastFaceEntity();
		Tile toTile = target.getTileBehind() != null && Utils.getDistance(target.getTile(), target.getTileBehind()) <= 3 ? target.getTileBehind() : target.getBackfacingTile();
		if (lastFaceEntity == entity.getClientIndex() && target.getActionManager().getAction() instanceof EntityFollow)
			entity.addWalkSteps(toTile.getX(), toTile.getY());
		else if (!entity.lineOfSightTo(target, true) || !WorldUtil.isInRange(entity.getX(), entity.getY(), size, target.getX(), target.getY(), target.getSize(), 0)) {
			Route route = RouteFinderKt.routeEntityToEntity(entity, target, entity.getRun() ? 2 : 1);
			if (!route.getSuccess())
				return false;
			entity.resetWalkSteps();
			RouteFinderKt.addSteps(entity, route, true, entity.getRun() ? 2 : 1);
			entity.faceEntity(target);
			return true;
		}
		return true;
	}

	@Override
	public boolean process(Entity entity) {
		return checkAll(entity);
	}

	@Override
	public int processWithDelay(Entity entity) {
		return 0;
	}

	@Override
	public void stop(final Entity entity) {
		entity.stopFaceEntity();
	}
}
