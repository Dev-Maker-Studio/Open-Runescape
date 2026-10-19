package com.rs.plugin.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface ServerStartupEvent {
	enum Priority {
		SYSTEM, FILE_IO, GENERAL, POST_PROCESS
	}
	Priority value() default Priority.GENERAL;
}
