package com.rs.game.content.skills.firemaking;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.statements.MakeXStatement;
import com.rs.engine.dialogue.statements.MakeXStatement.MakeXType;
import com.rs.game.content.skills.firemaking.Bonfire.Log;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;

import java.util.Arrays;

public class BonfireD extends Conversation {
	public BonfireD(Player player, GameObject object, Log[] logs) {
		super(player);
		
		Dialogue makeX = addNext(new MakeXStatement(MakeXType.SELECT, "Which logs do you want to add to the bonfire?", Arrays.stream(logs).mapToInt(Log::getLogId).toArray(), -1));
		for (Log log : logs)
			makeX.addNext(() -> player.getActionManager().setAction(new Bonfire(log, object)));
	}
}
