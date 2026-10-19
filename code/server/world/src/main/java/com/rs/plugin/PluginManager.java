package com.rs.plugin;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.Logger;
import org.darkan.core.Utils;
import org.darkan.core.UtilsAndExtensionsKt;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.plugin.events.PluginEvent;
import com.rs.plugin.handlers.PluginHandler;
import com.rs.plugin.kts.PluginScriptHost;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

public class PluginManager {

	private static final Map<String, HashSet<PluginHandler<PluginEvent>>> UNMAPPED_HANDLERS = new HashMap<>();
	private static final List<Method> STARTUP_HOOKS = new ArrayList<>();
	private static final PluginMethodRepository REPOSITORY = new PluginMethodRepository();

	/** Handler fields that could not be registered during {@link #loadPlugins()}. */
	private static int FAILURES = 0;

	public static int getRegistrationFailures() {
		return FAILURES;
	}

	private static void addUnmappedHandler(String type, PluginHandler<PluginEvent> method) {
		HashSet<PluginHandler<PluginEvent>> methods = UNMAPPED_HANDLERS.get(type);
		if (methods == null)
			methods = new HashSet<>();
		methods.add(method);
		UNMAPPED_HANDLERS.put(type, methods);
	}

	public static void loadPlugins() {
		try {
			long start = System.currentTimeMillis();
			Logger.info(PluginManager.class, "loadPlugins", "Loading plugins...");
			List<Class<?>> eventTypes = UtilsAndExtensionsKt.getClasses("com.rs.plugin.events");
			List<Class<?>> classes = Utils.getClassesWithAnnotation("com.rs", PluginEventHandler.class);
			Set<Field> visitedFields = new HashSet<>();
			Logger.info(PluginManager.class, "loadPlugins", "Loading " + eventTypes.size() + " event types and " + classes.size() + " plugin enabled classes.");
			int handlers = 0;

			List<Method> startupMethods = UtilsAndExtensionsKt.getMethodsWithAnnotation("com.rs", ServerStartupEvent.class);
			Set<Method> visitedMethods = new HashSet<>();
			for (Method method : startupMethods) {
				if (visitedMethods.contains(method))
					continue;
				if (!Modifier.isStatic(method.getModifiers()))
					throw new RuntimeException("Startup method should be static: " + method.getDeclaringClass() + "." + method.getName());
				if (method.getParameterCount() > 0)
					throw new RuntimeException("Startup method should have no parameters: " + method.getDeclaringClass() + "." + method.getName());
				visitedMethods.add(method);
				STARTUP_HOOKS.add(method);
				handlers++;
			}

			// One broken handler field used to abort the whole registration loop and be swallowed by
			// the catch below, leaving a silently half-registered server: every plugin after the bad
			// one simply never existed. Failures are isolated per field and reported instead.
			for (Class<?> clazz : classes) {
				Field[] fields;
				try {
					fields = clazz.getFields();
				} catch (Throwable e) {
					FAILURES++;
					Logger.handle(PluginManager.class, "loadPlugins", "Could not read fields of " + clazz.getName(), e);
					continue;
				}
				for (Field field : fields) {
					if (!Modifier.isStatic(field.getModifiers()) || visitedFields.contains(field))
						continue;
					visitedFields.add(field);
					try {
						handlers += processField(field, eventTypes);
					} catch (Throwable e) {
						FAILURES++;
						Logger.handle(PluginManager.class, "loadPlugins", "Could not register handler " + clazz.getName() + "." + field.getName(), e);
					}
				}
			}
			int kotlinScripts = PluginScriptHost.Companion.loadAndExecuteScripts();
			handlers += kotlinScripts;
			Logger.info(PluginManager.class, "loadPlugins", "Loaded " + kotlinScripts + " kotlin script plugins in " + (System.currentTimeMillis()-start) + "ms.");
			Logger.info(PluginManager.class, "loadPlugins", "Loaded " + handlers + " plugin event handlers in " + (System.currentTimeMillis()-start) + "ms.");
			if (FAILURES > 0)
				Logger.error(PluginManager.class, "loadPlugins", FAILURES + " plugin handler(s) failed to register; that content is dead until they are fixed.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public static int processField(Field field, List<Class<?>> eventTypes) throws IllegalAccessException {
		int found = 0;
		for (Class<?> eventType : eventTypes) {
			if (field.getType() == PluginHandler.class) {
				Class<?> type = ((Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]);
				if (type != eventType)
					continue;
			} else if (field.getType().getSuperclass() == PluginHandler.class) {
				Class<?> type = ((Class<?>) ((ParameterizedType) field.getType().getGenericSuperclass()).getActualTypeArguments()[0]);
				if (type != eventType)
					continue;
			} else
				continue;
			field.setAccessible(true);
			PluginHandler<PluginEvent> handler = (PluginHandler<PluginEvent>) field.get(null);
			if (handler == null)
				throw new IllegalStateException("Handler field " + field.getDeclaringClass().getName() + "." + field.getName() + " is null");
			if (handler.keys() == null || handler.keys().length <= 0) {
				addUnmappedHandler(eventType.getName(), handler);
			} else {
				REPOSITORY.addMappedHandler(eventType, handler);
			}
			found++;
		}
		return found;
	}

	public static void executeStartupHooks() {
		STARTUP_HOOKS.sort(Comparator.comparingInt(m -> m.getAnnotationsByType(ServerStartupEvent.class)[0].value().ordinal()));
		for (Method m : STARTUP_HOOKS) {
			long start = System.currentTimeMillis();
			try {
				m.invoke(null);
			} catch (Throwable e) {
				// Startup hooks are where every Commands.add lives. An Error out of one of them
				// (a class initialiser failing, typically) must not take the remaining hooks - and
				// with them the rest of the command registry - down with it.
				FAILURES++;
				Logger.handle(PluginManager.class, "executeStartupHooks", "Error executing startup hook: " + m.getDeclaringClass().getName() + "." + m.getName(), e);
			}
			long time = System.currentTimeMillis() - start;
			if (time > 100L)
				Logger.info(m.getDeclaringClass(), m.getName(), m.getDeclaringClass().getSimpleName() + ": Executed " + m.getName() + " in " + time + "ms...");
		}
	}

	public static boolean handle(PluginEvent event) {
		if (REPOSITORY.handle(event))
			return true;
		HashSet<PluginHandler<PluginEvent>> methods = UNMAPPED_HANDLERS.get(event.getClass().getName());
		if (methods == null)
			return false;
		boolean usedPlugins = false;
		for (PluginHandler<PluginEvent> method : methods)
			if (method.handleGlobal(event))
				usedPlugins = true;
		return usedPlugins;
	}

	public static Object getObj(PluginEvent event) {
		Object obj = REPOSITORY.getObj(event);
		if (obj != null)
			return obj;
		HashSet<PluginHandler<PluginEvent>> methods = UNMAPPED_HANDLERS.get(event.getClass().getName());
		if (methods == null)
			return null;
		for (PluginHandler<PluginEvent> method : methods) {
			obj = method.getObj(event);
			if (obj != null)
				return obj;
		}
		return null;
	}

	public static boolean handle(Method method, Object event) {
		try {
			return (boolean) method.invoke(null, event);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			Logger.handle(method.getDeclaringClass(), method.getName(), e);
		}
		return false;
	}
}
