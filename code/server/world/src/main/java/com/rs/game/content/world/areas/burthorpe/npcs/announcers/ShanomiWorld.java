package com.rs.game.content.world.areas.burthorpe.npcs.announcers;

import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.tasks.WorldTasks;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;


@PluginEventHandler
public class ShanomiWorld extends NPC {

	private static final int npcId = 4290;

	public ShanomiWorld(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void processNPC(){
		super.processNPC();
		if(Utils.random(0, 100) == 0) { //TODO add a lock to ensure this cannot collide with itself.
			WorldTasks.scheduleTimer(1, tick -> {
				//8 seconds based on 0.6 TPS
				if (tick >= 10 * 13)
					return false;
				if (tick == 0) {
					forceTalk("Think not dishonestly.");
				} else if (tick == 13) {
					forceTalk("The Way in training is.");
				} else if (tick == 2 * 13) {
					forceTalk("Acquainted with every art become.");
				} else if (tick == 3 * 13) {
					forceTalk("Ways of all professions know you.");
				} else if (tick == 4 * 13) {
					forceTalk("Gain and loss between you must distinguish.");
				} else if (tick == 5 * 13) {
					forceTalk("Judgment and understanding for everything develop you must.");
				} else if (tick == 6 * 13) {
					forceTalk("Those things which cannot be seen, perceive them.");
				} else if (tick == 7 * 13) {
					forceTalk("Trifles pay attention even to.");
				} else if (tick == 8 * 13) {
					forceTalk("Do nothing which is of no use.");
				} else if (tick == 9 * 13) {
					forceTalk("Way of the Warrior this is.");
				}
				return true;
			});
		}
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { npcId }, ShanomiWorld::new);
}
