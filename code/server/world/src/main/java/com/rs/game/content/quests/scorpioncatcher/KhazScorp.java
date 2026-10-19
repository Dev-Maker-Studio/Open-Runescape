package com.rs.game.content.quests.scorpioncatcher;

import com.rs.engine.quest.Quest;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class KhazScorp extends NPC {

	public KhazScorp(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public boolean withinDistance(Player p, int distance) {
		boolean known = switch(getId()) {
			case ScorpionCatcher.SCORP_1 -> p.getQuestManager().getAttribs(Quest.SCORPION_CATCHER).getB("scorp1LocKnown");
			case ScorpionCatcher.SCORP_2 -> p.getQuestManager().getAttribs(Quest.SCORPION_CATCHER).getB("scorp2LocKnown");
			case ScorpionCatcher.SCORP_3 -> p.getQuestManager().getAttribs(Quest.SCORPION_CATCHER).getB("scorp3LocKnown");
			default -> true;
		};
		return p.getQuestManager().getStage(Quest.SCORPION_CATCHER) == ScorpionCatcher.LOOK_FOR_SCORPIONS && 
				known && !ScorpionCatcher.caughtScorp(p, getId()) && super.withinDistance(p, distance) ;
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { ScorpionCatcher.SCORP_1, ScorpionCatcher.SCORP_2, ScorpionCatcher.SCORP_3 }, KhazScorp::new);
}
