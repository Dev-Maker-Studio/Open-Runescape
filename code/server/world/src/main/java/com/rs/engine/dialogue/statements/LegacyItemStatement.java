package com.rs.engine.dialogue.statements;

import com.rs.game.model.entity.player.Player;

public class LegacyItemStatement implements Statement {

	private final int[] itemIds;
	private final String title;
	private final String[] text;

	public LegacyItemStatement(int item, String title, String... text) {
		itemIds = new int[] { item };
		this.title = title;
		this.text = text;
	}

	public LegacyItemStatement(int item1, int item2, String title, String... text) {
		itemIds = new int[] { item1, item2 };
		this.title = title;
		this.text = text;
	}

	@Override
	public void send(Player player) {
		int interfaceId = itemIds.length > 1 ? 131 : 519;
		StringBuilder text = new StringBuilder();
		text.append(title).append("<br>");
		for (String s : this.text)
			text.append(s).append("<br>");
		player.getInterfaceManager().sendChatBoxInterface(interfaceId);
		player.getPackets().setIFText(interfaceId, 1, text.toString());
		player.getPackets().setIFItem(interfaceId, 0, itemIds[0], 1);
		if (itemIds.length > 1)
			player.getPackets().setIFItem(interfaceId, 2, itemIds[1], 1);
	}

	@Override
	public int getOptionId(int componentId) {
		return 0;
	}

	@Override
	public void close(Player player) {
		
	}
}
