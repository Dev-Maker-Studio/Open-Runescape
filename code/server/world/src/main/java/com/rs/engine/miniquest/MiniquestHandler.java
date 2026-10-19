package com.rs.engine.miniquest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MiniquestHandler {
	Miniquest miniquest();
	String startText();
	String itemsText();
	String combatText();
	String rewardsText();
	int completedStage();
}
