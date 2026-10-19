package com.rs.utils.music;

public class Song {
	private String name;
	private String hint;
	private String comment;
	private boolean allowAmbient;
	private int id;
	private int[] regionIds;

	public String getName() {
		return name;
	}

	public String getHint() {
		return hint;
	}

	public String getComment() {
		return comment;
	}

	public boolean isAllowAmbient() {
		return allowAmbient;
	}

	public int getId() {
		return id;
	}

	public int[] getRegionIds() {
		return regionIds;
	}
}
