package com.rs.utils;

public class Ticks {

	public static int fromHours(int hours) {
		return fromMinutes(hours * 60);
	}

	public static int fromMinutes(int minutes) {
		return minutes * 100;
	}

	public static int fromMinutes(double minutes) {
		return (int) (minutes * 100);
	}

	public static int fromSeconds(int seconds) {
		return (int) (seconds / 0.6);
	}

	public static String breakDownOfTicks(int ticks) {
		final double SECONDS_PER_TICK = 0.6;
		final int SECONDS_PER_MINUTE = 60;
		final int MINUTES_PER_HOUR = 60;
		final int HOURS_PER_DAY = 24;

		double totalSeconds = ticks * SECONDS_PER_TICK;

		int days = (int) (totalSeconds / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY));
		int hours = (int) ((totalSeconds % (SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY)) / (SECONDS_PER_MINUTE * MINUTES_PER_HOUR));
		int minutes = (int) ((totalSeconds % (SECONDS_PER_MINUTE * MINUTES_PER_HOUR)) / SECONDS_PER_MINUTE);
		int seconds = (int) (totalSeconds % SECONDS_PER_MINUTE);

		return String.format("%d day(s), %d hour(s), %d minute(s), %d second(s)", days, hours, minutes, seconds);
	}

}
