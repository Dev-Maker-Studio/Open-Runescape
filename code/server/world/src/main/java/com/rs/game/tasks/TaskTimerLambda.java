package com.rs.game.tasks;

import java.util.function.Function;

public class TaskTimerLambda extends Task {

	int tick = 0;
	private final Function<Integer, Boolean> task;
	private String stack;

	public TaskTimerLambda(Function<Integer, Boolean> task) {
		this.task = task;
		stack = "";
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		for(int i = 1; i < elements.length; ++i) {
			StackTraceElement s = elements[i];
			String var10001 = s.getClassName();
			stack += "\tat " + var10001 + "." + s.getMethodName() + "(" + s.getFileName() + ":" + s.getLineNumber() + ")";
		}
	}

	@Override
	public void run() {
		if (!task.apply(tick))
			stop();
		tick++;
	}

	@Override
	public String toString() {
		return "TaskTimerLambda\n" + stack;
	}
}
