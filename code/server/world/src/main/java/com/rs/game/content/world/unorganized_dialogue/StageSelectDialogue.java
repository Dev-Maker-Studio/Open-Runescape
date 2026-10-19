package com.rs.game.content.world.unorganized_dialogue;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class StageSelectDialogue extends Dialogue {

	private String stageName;
	private Map<String, Dialogue> markedStages;
	private Dialogue directNextReference;

	public StageSelectDialogue(String stageName, Map<String, Dialogue> markedStages) {
		this.stageName = stageName;
		this.markedStages = markedStages;
	}

	public StageSelectDialogue(Dialogue directNextReference) {
		this.directNextReference = directNextReference;
	}

	public String getStageName() {
		return stageName;
	}

	public Map<String, Dialogue> getStages() {
		return markedStages;
	}

	public Dialogue getDirectNextReference() {
		return directNextReference;
	}

}
