package com.rs.game.model.entity.interactions;

import org.darkan.core.map.pathfinder.Direction;
import com.rs.game.content.Effect;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.utils.WorldUtil;

public abstract class EntityInteraction extends Interaction {

	protected Entity target;
	private int distance;
	private boolean stopFaceOnReached = true;
	private boolean stopWhenReached = true;

	public EntityInteraction(Entity target, int distance) {
		this.target = target;
		this.distance = distance;
	}

	public EntityInteraction keepFacing() {
		stopFaceOnReached = false;
		return this;
	}

	public EntityInteraction continueAfterReached() {
		stopWhenReached = false;
		return this;
	}

	public abstract boolean canStart(Entity entity);
	public abstract boolean checkAll(Entity entity);
	public abstract void interact(Entity entity);
	public abstract void onStop(Entity entity);

	@Override
	public final boolean start(Entity entity) {
		entity.faceEntity(target);
		if (!canStart(entity))
			return false;
        return checkDistance(entity) && checkAll(entity);
    }

	@Override
	public void stop(Entity entity) {
		super.stop(entity);
		entity.stopFaceEntity();
	}

	@Override
	public final boolean process(Entity player) {
		if (checkDistance(player) && checkAll(player)) {
			if (isWithinDistance(player, target, true)) {
				interact(player);
				if (stopFaceOnReached)
					player.stopFaceEntity();
				if (stopWhenReached) {
					stop(player);
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public boolean isWithinDistance(Entity entity, Entity target, boolean addRunSteps) {
		if (entity.hasEffect(Effect.FREEZE))
			addRunSteps = false;
		boolean los = entity.lineOfSightTo(target, distance == 0);
		boolean inRange = WorldUtil.isInRange(entity, target, distance + (addRunSteps ? (target.getRun() ? target.hasWalkSteps() ? 2 : 1 : target.hasWalkSteps() ? 1 : 0) : 0));
		//boolean collides = WorldUtil.collides(player, target);
        return los && inRange;
    }

	public final boolean checkDistance(Entity entity) {
		if (entity.isDead() || entity.hasFinished() || target.isDead() || target.hasFinished())
			return false;
		int distanceX = entity.getX() - target.getX();
		int distanceY = entity.getY() - target.getY();
		int size = target.getSize();
		int maxDistance = 16;
		if (entity.getPlane() != target.getPlane() || distanceX > size + maxDistance || distanceX < -1 - maxDistance || distanceY > size + maxDistance || distanceY < -1 - maxDistance)
			return false;
		if (entity.hasEffect(Effect.FREEZE))
			return !WorldUtil.collides(entity, target);
		if (WorldUtil.collides(entity, target)) {
			if (!entity.hasWalkSteps() && !target.hasWalkSteps()) {
				entity.resetWalkSteps();
				return entity.calcFollow(target, !(entity instanceof NPC n) || n.isIntelligentRouteFinder());
			}
			return target instanceof Player || (entity instanceof Player ? target.hasWalkSteps() ? target instanceof NPC : entity.calcFollow(target, true) : entity.calcFollow(target, !(entity instanceof NPC n) || n.isIntelligentRouteFinder()));
		}
		if (distance == 0 && !target.hasWalkSteps() && target.getSize() == 1) {
			Direction dir = Direction.forDelta(target.getX() - entity.getX(), target.getY() - entity.getY());
			if (dir != null)
				switch(dir) {
				case NORTH:
				case SOUTH:
				case EAST:
				case WEST:
					break;
				default:
					entity.resetWalkSteps();
					entity.calcFollow(target, entity.getRun() ? 2 : 1, !(entity instanceof NPC n) || n.isIntelligentRouteFinder());
					return true;
				}
		}
		if (!isWithinDistance(entity, target, false)) {
			entity.resetWalkSteps();
			entity.calcFollow(target, entity.getRun() ? 2 : 1, !(entity instanceof NPC n) || n.isIntelligentRouteFinder());
		} else {
			entity.resetWalkSteps();
			if (distance == 0 && target.getRun() == entity.getRun())
				entity.calcFollow(target, entity.getRun() ? 2 : 1, !(entity instanceof NPC n) || n.isIntelligentRouteFinder());
		}
		return true;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}
}