package com.rs.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AccountLimiter {

	private static final Map<String, Integer> CONNECTIONS = new ConcurrentHashMap<>();

	public static void add(String ip) {
        CONNECTIONS.merge(ip, 1, Integer::sum);
	}

	public static void remove(String ip) {
		Integer amount = CONNECTIONS.get(ip);
		if (amount == null)
			amount = 0;
		if (amount <= 1)
			CONNECTIONS.remove(ip);
		else
			CONNECTIONS.put(ip, amount-1);
	}

	public static int getSessionsIP(String ip) {
		Integer connections = CONNECTIONS.get(ip);
		if (connections == null)
			return 1;
		return connections;
	}
}