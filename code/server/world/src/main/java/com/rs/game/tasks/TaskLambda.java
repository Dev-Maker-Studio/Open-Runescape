package com.rs.game.tasks;

public class TaskLambda extends Task {

	private final Runnable task;
	private String stack;

	public TaskLambda(Runnable task) {
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
		task.run();
	}

	@Override
	public String toString() {
		return "TaskLambda\n" + stack;
	}
}
