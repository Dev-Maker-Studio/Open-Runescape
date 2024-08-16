package com.jagex.game.runetek5.systeminfo;

import java.io.File;

public class OperatingSystemInfo {

    public static final String OS_NAME;
    public static final String OS_ARCHITECTURE;

    static {
        String infoCollect = "Unknown";
        try {
            infoCollect = System.getProperty("java.vendor").toLowerCase();
        } catch (Exception exception) {
        	// NOOP
        }
        infoCollect.toLowerCase();
        infoCollect = "Unknown";
        try {
            infoCollect = System.getProperty("java.version").toLowerCase();
        } catch (Exception exception) {
        	// NOOP
        }
        infoCollect.toLowerCase();
        infoCollect = "Unknown";
        try {
            infoCollect = System.getProperty("os.name").toLowerCase();
        } catch (Exception exception) {
        	// NOOP
        }
        OS_NAME = infoCollect.toLowerCase();
        infoCollect = "Unknown";
        try {
            infoCollect = System.getProperty("os.arch").toLowerCase();
        } catch (Exception exception) {
        	// NOOP
        }
        OS_ARCHITECTURE = infoCollect.toLowerCase();
        infoCollect = "Unknown";
        try {
            infoCollect = System.getProperty("os.version").toLowerCase();
        } catch (Exception exception) {
        	// NOOP
        }
        infoCollect.toLowerCase();
        infoCollect = "~/";
        try {
            infoCollect = System.getProperty("user.home").toLowerCase();
        } catch (Exception exception) {
        	// NOOP
        }
        new File(infoCollect);
    }

    OperatingSystemInfo() {
        throw new Error();
    }

}