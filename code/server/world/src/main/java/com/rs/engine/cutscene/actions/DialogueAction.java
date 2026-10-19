package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.engine.dialogue.Dialogue;
import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class DialogueAction extends CutsceneAction {
	
	private final Dialogue dialogue;
	private final boolean pause;

	public DialogueAction(Dialogue dialogue, int delay, boolean pause) {
		super(null, delay);
		this.dialogue = dialogue;
		this.pause = pause;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		if (pause)
			((Cutscene) objects.get("cutscene")).setDialoguePause(true);
		player.startConversation(dialogue);
	}

}
