package com.rs.engine.dialogue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public abstract class Options {

	private final Map<String, Option> options = new LinkedHashMap<>(); //LinkedHashMap O(1) but allows ordered keys
	private String stageName;
	private Conversation conv;

	public Options() {
		create();
	}

	public Options(String stageName, Conversation conv) {
		this.conv = conv;
		this.stageName = stageName;
		create();
	}

	public abstract void create();

	public void option(String name, Dialogue dialogue) {
		options.put(name, new Option(dialogue.getHead()));
	}

	public void option(String name, Runnable consumer) {
		options.put(name, new Option(new Dialogue(null, consumer)));
	}

	public void option(Supplier<Boolean> constraint, String name, Dialogue dialogue) {
		options.put(name, new Option(constraint, dialogue.getHead()));
	}

	public void option(String name) {
		options.put(name, new Option(null));
	}
	
	public void add(String name, Dialogue dialogue) {
		options.put(name, new Option(dialogue.getHead()));
	}

	public void add(String name, Runnable consumer) {
		options.put(name, new Option(new Dialogue(null, consumer)));
	}

	public void add(Supplier<Boolean> constraint, String name, Dialogue dialogue) {
		options.put(name, new Option(constraint, dialogue.getHead()));
	}

	public Dialogue add(String name) {
		Dialogue dial = new Dialogue();
		options.put(name, new Option(dial));
		return dial;
	}
	
	public void empty(String name) {
		options.put(name, new Option(null));
	}

	public Map<String, Option> getOptions() {
		return options;
	}

	public String getStageName() {
		return stageName;
	}

	public Conversation getConv() {
		return conv;
	}
}
