package com.rs.utils;

import org.darkan.core.Utils;

public class EffigyDrop {

	public static boolean dropEffigy(int combatLevel) {
		return Math.random() <= (1.0 / getRate(combatLevel));
	}

	public static double getRate(int combatLevel) {
		return Utils.clampD(169075.845*(Math.pow(0.9807522225, combatLevel)), 128.0, 1000000.0);
	}

}
