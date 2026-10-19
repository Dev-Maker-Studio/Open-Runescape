package com.rs.utils;

import org.darkan.core.Logger;

import java.util.*;

/**
 * My custom Runtime debugging tool
 * Author: Jawarrior1
 */
public class Getlineonce {
	static Dictionary<String, List<Integer>> lines = new Hashtable<>();

	public Getlineonce(boolean repeat) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

		//Check if this line as been taking, so as not to repeat with loops or recursion
		int line = stackTrace[2].getLineNumber();
		String fileName = stackTrace[2].getFileName();
		List<Integer> tempLines = new ArrayList<>();//only used if key doesn't exist
		if (lines.isEmpty()) {
			tempLines.add(line);
			lines.put(fileName, tempLines);
			//            lines.put("tester", oneline);
		} else {
			boolean keyExists = false;
			for (Enumeration<String> k = lines.keys(); k.hasMoreElements(); ) {
				String key = k.nextElement();

				//Check if there is a kay
				if (!key.equalsIgnoreCase(fileName))
					continue;
				keyExists = true;
				if (!lines.get(key).contains(line)) {
					tempLines = lines.get(key);
					tempLines.add(line);
					lines.put(fileName, tempLines);
					Logger.debug(Getlineonce.class, "constructor()", fileName + ": " + line);// should run once
				} else if (repeat)
					Logger.debug(Getlineonce.class, "constructor()", fileName + ": " + line);
			}
			//If after searching all of those you found nothing
			if (!keyExists) {
				tempLines.add(line);
				lines.put(fileName, tempLines);
			}
		}
	}
}