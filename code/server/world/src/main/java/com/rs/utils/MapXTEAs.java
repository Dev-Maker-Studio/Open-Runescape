package com.rs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

public final class MapXTEAs {

	private static Map<Integer, int[]> KEYS = new HashMap<>();
	private final static String PATH = "./data/map/xteaKeys.json";

	public static final int[] getMapKeys(int regionId) {
		int[] arr = (int[]) KEYS.get(regionId);
		if (arr == null || (arr[0] == 0 && arr[1] == 0 && arr[2] == 0 && arr[3] == 0))
			return null;
		return arr;
	}

	public static void loadKeys() throws JsonIOException, IOException {
		System.out.println("Loading map XTEAs...");
		if (!new File(PATH).exists())
			throw new FileNotFoundException("No map keys file found!");
		else {
			Gson gson = new Gson();
			JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(new File(PATH)), "UTF-8"));
			KEYS = gson.fromJson(reader, new TypeToken<Map<Integer, int[]>>(){}.getType());
			reader.close();
		}
	}
}
