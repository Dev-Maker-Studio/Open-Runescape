package com.jagex.game.runetek5.scene;

import java.util.Stack;

import com.jagex.game.runetek5.entity.RenderedEntity;

public class RenderedEntityCache {
	
	public static Stack<RenderedEntity> stack = new Stack<RenderedEntity>();

	public static RenderedEntity generate(boolean interactive) {
	    synchronized (stack) {
	        RenderedEntity entry;
	        if (stack.isEmpty()) {
	            entry = new RenderedEntity();
	        } else {
	            entry = stack.pop();
	        }
	        entry.interactive = interactive;
	        return entry;
	    }
	}
	
	public static void clear(RenderedEntity interaction) {
	    interaction.entity = null;
	    synchronized (stack) {
	        if (stack.size() < 200) {
	            stack.push(interaction);
	        }
	    }
	}

	public static void reset() {
	    synchronized (stack) {
	        stack = new Stack<>();
	    }
	}
}
