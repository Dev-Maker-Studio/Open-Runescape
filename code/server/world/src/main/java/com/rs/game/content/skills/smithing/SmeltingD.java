package com.rs.game.content.skills.smithing;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.statements.MakeXStatement;
import com.rs.game.content.skills.smithing.Smelting.SmeltingBar;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.Skills;
import com.rs.game.model.gameobject.GameObject;

import java.util.ArrayList;
import java.util.List;

public class SmeltingD extends Conversation {
	
	public SmeltingD(Player player, GameObject object) {
		super(player);
		
		List<SmeltingBar> bars = new ArrayList<>();
		for (SmeltingBar bar : SmeltingBar.values())
			if (Smithing.hasRequiredItems(player, bar.getItemsRequired()) && player.getSkills().getLevel(Skills.SMITHING) >= bar.getLevelRequired())
				bars.add(bar);
		
		if (bars.isEmpty()) {
			addSimple("You don't have any ores that you are skilled enough to smelt.");
			create();
			return;
		}
		
		Dialogue makeX = addNext(new MakeXStatement(bars.stream().mapToInt(bar -> bar.getProducedBar().getId()).toArray()));
		for (SmeltingBar bar : bars)
			makeX.addNext(() -> player.getActionManager().setAction(new Smelting(bar, object, MakeXStatement.getQuantity(player))));
		
		create();
	}
}
