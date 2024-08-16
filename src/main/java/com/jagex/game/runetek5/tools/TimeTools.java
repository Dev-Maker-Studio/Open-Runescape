package com.jagex.game.runetek5.tools;

public class TimeTools {

    public static void sleepDirect(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException exception) {
        	exception.printStackTrace();
        }
    }

    public static void sleep(long ms) {
        if (ms > 0L) {
            if (ms % 10L == 0L) {
                sleepDirect(ms - 1L);
                sleepDirect(1L);
            } else {
                sleepDirect(ms);
            }
        }
    }
}
