package com.jagex.game.runetek5.tools;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.net.URI;

import com.jagex.game.runetek5.client.ServerEnvironment;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.systeminfo.SystemInfo;

public class Browser {
	
	public static String errorWebsiteBaseUrl;

	public static boolean openBrowser(String url, int actionType, String programName) {
	    boolean success;
	    if (actionType == 0) {
	        try {
	            if (!SystemInfo.operatingSystemLowercase.startsWith("win")) {
	                throw new Exception();
	            } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
	                throw new Exception();
	            } else {
	                String str13 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?&=,.%+-_#:/*";
	
	                for (int index = 0; index < url.length(); index++) {
	                    if (str13.indexOf(url.charAt(index)) == -1) {
	                        throw new Exception();
	                    }
	                }
	
	                Runtime.getRuntime().exec("cmd /c start \"j\" \"" + url + "\"");
	                success = true;
	                return true;
	            }
	        } catch (Throwable throwable) {
	            return false;
	        }
	    } else
			return false;
	}

	public static void openBrowser(String url, boolean loggedIn, String programName, boolean isApplet) {
	    if (loggedIn) {
	        if (!isApplet && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE)) {
	            try {
	                Desktop.getDesktop().browse(new URI(url));
	                return;
	            } catch (Exception exception) {
	            	// NOOP
	            }
	        }
	        if (SystemInfo.operatingSystemLowercase.startsWith("win") && !isApplet) {
	            Browser.openBrowser(url, 0);
	        } else if (SystemInfo.operatingSystemLowercase.startsWith("mac")) {
	        	Browser.openBrowser(url, 1, programName);
	        } else {
	            Browser.openBrowser(url, 2);
	        }
	    } else {
	        Browser.openBrowser(url, 3);
	    }
	}

	public static void openOpenjs(String url, boolean loggedIn, boolean isApplet) {
	    openBrowser(url, loggedIn, "openjs", isApplet);
	}

	public static void openBrowserErrorscreenOpenjs() {
	    String url = Browser.errorWebsiteBaseUrl != null ? Browser.errorWebsiteBaseUrl : ServerEnvironment.getUrl();
	    openOpenjs(url, false, client.aBool7158);
	}

	public static boolean openBrowser(String url, int actionType) {
	    return openBrowser(url, actionType, "openjs");
	}

}
