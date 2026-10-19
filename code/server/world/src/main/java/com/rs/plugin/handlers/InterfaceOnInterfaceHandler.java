package com.rs.plugin.handlers;

import com.rs.plugin.events.IFOnIFEvent;
import com.rs.plugin.events.IFOnNPCEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InterfaceOnInterfaceHandler extends PluginHandler<IFOnIFEvent> {
	private final boolean bidirectional;
	private final Object[] fromKeys;
	private final Object[] toKeys;
	public InterfaceOnInterfaceHandler(boolean bidirectional, int[] fromInterfaceIds, int[] fromComponentIds, int[] toInterfaceIds, int[] toComponentIds, Consumer<IFOnIFEvent> handler) {
		super(new Object[] { "meme" }, handler);
		this.bidirectional = bidirectional;
		List<Object> fromList = new ArrayList<>();
		if (fromComponentIds == null || fromComponentIds.length <= 0)
			for (Integer id : fromInterfaceIds)
				fromList.add(id);
		else
			for (int id : fromInterfaceIds)
				for (int comp : fromComponentIds)
					fromList.add((id << 16) + comp);
		fromKeys = fromList.toArray();

		List<Object> toList = new ArrayList<>();
		if (toComponentIds == null || toComponentIds.length <= 0)
			for (Integer id : toInterfaceIds)
				toList.add(id);
		else
			for (int id : toInterfaceIds)
				for (int comp : toComponentIds)
					toList.add((id << 16) + comp);
		toKeys = toList.toArray();
	}

	public InterfaceOnInterfaceHandler(int[] fromInterfaceIds, int[] fromComponentIds, int[] toInterfaceIds, int[] toComponentIds, Consumer<IFOnIFEvent> handler) {
		this(false, fromInterfaceIds, fromComponentIds, toInterfaceIds, toComponentIds, handler);
	}

	public InterfaceOnInterfaceHandler(boolean bidirectional, int fromInterfaceId, int[] fromComponentIds, int toInterfaceId, int[] toComponentIds, Consumer<IFOnIFEvent> handler) {
		this(bidirectional, new int[] {fromInterfaceId}, fromComponentIds, new int[] {toInterfaceId}, toComponentIds, handler);
	}

	public InterfaceOnInterfaceHandler(boolean bidirectional, int fromInterfaceId, int fromComponentId, int toInterfaceId, int toComponentId, Consumer<IFOnIFEvent> handler) {
		this(bidirectional, fromInterfaceId, new int[] { fromComponentId }, toInterfaceId, new int[] {toComponentId}, handler);
	}

	public InterfaceOnInterfaceHandler(boolean bidirectional, int fromInterfaceId, int toInterfaceId, Consumer<IFOnIFEvent> handler) {
		this(bidirectional, fromInterfaceId, null, toInterfaceId, null, handler);
	}

	public InterfaceOnInterfaceHandler(int fromInterfaceId, int[] fromComponentIds, int toInterfaceId, int[] toComponentIds, Consumer<IFOnIFEvent> handler) {
		this(false, new int[] {fromInterfaceId}, fromComponentIds, new int[] {toInterfaceId}, toComponentIds, handler);
	}

	public InterfaceOnInterfaceHandler(int fromInterfaceId, int fromComponentId, int toInterfaceId, int toComponentId, Consumer<IFOnIFEvent> handler) {
		this(false, fromInterfaceId, new int[] { fromComponentId }, toInterfaceId, new int[] {toComponentId}, handler);
	}

	public InterfaceOnInterfaceHandler(int fromInterfaceId, int toInterfaceId, Consumer<IFOnIFEvent> handler) {
		this(false, fromInterfaceId, null, toInterfaceId, null, handler);
	}

	public Object[] getFromKeys() {
		return fromKeys;
	}

	public Object[] getToKeys() {
		return toKeys;
	}

	public boolean isBidirectional() {
		return bidirectional;
	}
}
