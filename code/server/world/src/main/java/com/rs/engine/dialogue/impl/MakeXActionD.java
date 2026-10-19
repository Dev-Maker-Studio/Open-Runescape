package com.rs.engine.dialogue.impl;

import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.statements.MakeXStatement;

import java.util.ArrayList;
import java.util.List;

public class MakeXActionD extends Dialogue {

	private final List<MakeXItem> options = new ArrayList<>();

	public MakeXActionD addOption(MakeXItem option) {
		clearChildren();
		options.add(option);
		MakeXItem[] opArr = new MakeXItem[options.size()];
		options.toArray(opArr);
		addStatementWithOptions(new MakeXStatement(opArr, 28), opArr);
		return this;
	}

	public boolean isEmpty() {
		return options.isEmpty();
	}

}
