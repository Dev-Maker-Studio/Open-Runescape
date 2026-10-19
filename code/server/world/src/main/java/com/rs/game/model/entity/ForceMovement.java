package com.rs.game.model.entity;

import org.darkan.core.map.pathfinder.Direction;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

public class ForceMovement {
	private final Tile start;
	private final Tile[] path;
	private final int startClientCycles;
	private final int speedClientCycles;
	private final int direction;

	public ForceMovement(Tile start, Tile[] path, int startClientCycles, int speedClientCycles, int direction) {
		this.start = start;
		this.path = path;
		this.startClientCycles = startClientCycles;
		this.speedClientCycles = speedClientCycles;
		this.direction = direction;
	}

	public ForceMovement(Tile start, Tile destination, int startClientCycles, int speedClientCycles, int direction) {
		this(start, new Tile[] { destination }, startClientCycles, speedClientCycles, direction);
	}

	public ForceMovement(Tile start, Tile destination, int startClientCycles, int speedClientCycles, Direction direction) {
		this(start, destination, startClientCycles, speedClientCycles, direction.getAngle());
	}

	public ForceMovement(Tile start, Tile destination, int startClientCycles, int speedClientCycles) {
		this(start, destination, startClientCycles, speedClientCycles, Utils.getAngleTo(start, destination));
	}

	public int getTickDuration() {
		return (int) Math.ceil((double) Math.max(startClientCycles, speedClientCycles) / 30.0);
	}

	public int getDiffX1() {
		return start.getX() - path[0].getX();
	}

	public int getDiffX2() {
		if (path.length <= 1)
			return 0;
		return start.getX() - path[1].getX();
	}

	public int getDiffY1() {
		return start.getY() - path[0].getY();
	}

	public int getDiffY2() {
		if (path.length <= 1)
			return 0;
		return start.getY() - path[1].getY();
	}

	public Tile getStart() {
		return start;
	}

	public Tile[] getPath() {
		return path;
	}

	public int getStartClientCycles() {
		return startClientCycles;
	}

	public int getSpeedClientCycles() {
		return speedClientCycles;
	}

	public int getDirection() {
		return direction;
	}
}
