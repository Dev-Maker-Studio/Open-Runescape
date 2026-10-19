package com.rs.game.content.skills.hunter;

import org.darkan.core.model.Animation;

import java.util.HashMap;
import java.util.Map;

public enum BoxTrapType {

	BIRD_SNARE("bird snare", 10006, 19175, new Animation(5207), 1),
	DEAD_FALL("dead fall", 1511, 19206, new Animation(451), 23),
	BOX("box", 10008, 19187, new Animation(451), 27),
	TREE_NET("net", 303, -1, new Animation(5207), 29),
	MARASAMAW_PLANT("marasamaw plant", 19965, 56806, new Animation(451), 70),
	MAGIC_BOX("magic box", 10025, 19223, new Animation(451), 71);

	private final String name;
	private final int itemId;
    private final int objectId;
    private final int baseLevel;
	private final Animation pickUpAnimation;

	private static final Map<Integer, BoxTrapType> TRAPS_BY_ITEM = new HashMap<>();

	static {
		for (BoxTrapType t : BoxTrapType.values())
			TRAPS_BY_ITEM.put(t.itemId, t);
	}

	public static BoxTrapType forId(int id) {
		return TRAPS_BY_ITEM.get(id);
	}

	private BoxTrapType(String name, int itemId, int objectId, Animation pickUpAnimation, int baseLevel) {
		this.name = name;
		this.itemId = itemId;
		this.objectId = objectId;
		this.pickUpAnimation = pickUpAnimation;
		this.baseLevel = baseLevel;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return itemId;
	}

	public int getObjectId() {
		return objectId;
	}

	public Animation getPickUpAnimation() {
		return pickUpAnimation;
	}

	public int getBaseLevel() {
		return baseLevel;
	}
}
