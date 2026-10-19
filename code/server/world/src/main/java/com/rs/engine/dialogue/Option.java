package com.rs.engine.dialogue;

import java.util.function.Supplier;

public class Option {

	private final Dialogue dialogue;
	private final Supplier<Boolean> constraint;

	public Option(Supplier<Boolean> constraint, Dialogue dialogue) {
		this.constraint = constraint;
		this.dialogue = dialogue;
	}

	public Option(Dialogue dialogue) {
		this(null, dialogue);
	}

	public Dialogue getDialogue() {
		return dialogue;
	}

	public Supplier<Boolean> getConstraint() {
		return constraint;
	}

	public boolean show() {
		return constraint == null ? true : constraint.get();
	}

}
