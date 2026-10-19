package com.rs.game.content.holidayevents.halloween.hw09;

import org.darkan.core.cache.NPCDefinitions;
import com.rs.engine.dialogue.statements.Statement;
import com.rs.game.model.entity.player.Player;

public class SpiderStatement implements Statement {

	private final String[] texts;

	public SpiderStatement(String... texts) {
		this.texts = texts;
	}

	@Override
	public void send(Player player) {
		StringBuilder builder = new StringBuilder();
		for (String text2 : texts)
			builder.append(" ").append(text2);
		String text = builder.toString();
		player.getInterfaceManager().sendChatBoxInterface(1184);
		player.getPackets().setIFText(1184, 17, NPCDefinitions.getDefs(8985, player.getVars()).getName());
		player.getPackets().setIFText(1184, 13, text);
		player.getPackets().setIFModel(1184, 11, 24613);
		player.getPackets().setIFAngle(1184, 11, 100, 1900, 500);
		player.getPackets().setIFAnimation(6247, 1184, 11);
	}

	@Override
	public int getOptionId(int componentId) {
		return 0;
	}

	@Override
	public void close(Player player) {
		
	}

}
