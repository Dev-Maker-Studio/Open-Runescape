package com.rs.game.content.minigames.domtower;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.player.Player;

public class DTPreview extends Cutscene {
	@Override
	public void construct(Player player) {
		camLook(3386, 3104, 1000, 6, 6, -1);
		camPos(3395, 3104, 5000, 7, 8, 5);
		camLook(3390, 3115, 6000, 6, 6, -1);
		camPos(3395, 3115, 6000, 7, 8, 5);
		camLook(3380, 3125, 6000, 6, 6, -1);
		camPos(3380, 3125, 6000, 7, 6, 5);
		camLook(3375, 3125, 6000, 6, 6, -1);
		camPos(3375, 3125, 6000, 7, 6, 5);
		camLook(3370, 3125, 6000, 6, 6, -1);
		camPos(3370, 3125, 6000, 7, 6, 5);
		camLook(3358, 3120, 5500, 6, 6, -1);
		camPos(3358, 3125, 5500, 7, 6, 5);
		camLook(3358, 3095, 4000, 6, 6, -1);
		camPos(3358, 3095, 4000, 7, 8, 5);
		camLook(3374, 3084, 2500, 6, 6, -1);
		camPos(3374, 3084, 2500, 7, 8, 5);
		camLook(3374, 3097, 2300, 9, 9, -1);
		camPos(3374, 3097, 2300, 7, 8, 5);
	}
}
