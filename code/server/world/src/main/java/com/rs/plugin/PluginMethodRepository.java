package com.rs.plugin;

import org.darkan.core.Logger;
import com.rs.plugin.events.NPCDropEvent;
import com.rs.plugin.events.PluginEvent;
import com.rs.plugin.handlers.PluginHandler;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class PluginMethodRepository {

	public void addMappedHandler(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		try {
			eventType.getMethod("registerMethod", Class.class, PluginHandler.class).invoke(null, eventType, method);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			System.err.println("Error registering method for event type: " + eventType.getName() + " from registering method");
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public boolean handle(PluginEvent event) {
		PluginHandler<PluginEvent> method = (PluginHandler<PluginEvent>) event.getMethod();
		if (method != null) {
			try {
				method.getHandler().accept(event);
				return true;
			} catch (Exception e) {
				Logger.handle(PluginMethodRepository.class, "handle:" + event.getClass().getSimpleName(), e);
				return false;
			}
		}
		List<PluginHandler<? extends PluginEvent>> methods = event.getMethods();
		if (methods == null || methods.isEmpty())
			return false;
		for (PluginHandler<? extends PluginEvent> m : methods)
			try {
				PluginHandler<PluginEvent> pHandle = (PluginHandler<PluginEvent>) m;
				if (event instanceof NPCDropEvent drop && drop.getItem() == null) continue;
				pHandle.getHandler().accept(event);
			} catch (Exception e) {
				Logger.handle(PluginMethodRepository.class, "handle:" + event.getClass().getSimpleName(), e);
				return false;
			}
		return true;
	}

	@SuppressWarnings("unchecked")
	public Object getObj(PluginEvent event) {
		Object obj = null;
		PluginHandler<PluginEvent> method = (PluginHandler<PluginEvent>) event.getMethod();
		if (method == null) {
			List<PluginHandler<? extends PluginEvent>> methods = event.getMethods();
			if (methods == null || methods.size() <= 0)
				return null;
			for (PluginHandler<? extends PluginEvent> m : methods)
				try {
					PluginHandler<PluginEvent> pHandle = (PluginHandler<PluginEvent>) m;
					obj = pHandle.getObj(event);
				} catch (Exception e) {
					Logger.handle(PluginMethodRepository.class, "getObj:" + event.getClass().getSimpleName(), e);
					return null;
				}
			return obj;
		}
		try {
			obj = method.getObj(event);
			if (obj != null)
				return obj;
		} catch (Exception e) {
			Logger.handle(PluginMethodRepository.class, "getObj:" + event.getClass().getSimpleName(), e);
			return null;
		}
		return null;
	}
}
