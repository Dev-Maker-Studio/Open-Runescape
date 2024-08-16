package com.jagex.game.runetek5.client;

import com.jagex.game.runetek5.config.Identifiable;

public enum ServerEnvironment implements Identifiable {

    LOCAL("LOCAL", 4),
    WTWIP("WTWIP", 3),
    WTI("WTI", 5),
    WTQA("WTQA", 2),
    WTRC("WTRC", 1),
    INTBETA("INTBETA", 6),
    LIVE("LIVE", 0);

    public static ServerEnvironment SERVER_ENVIRONMENT;
    public final String environmentName;

    public final int environmentIdentifier;
	public static int anInt9282;

    ServerEnvironment(String environmentName, int environmentIdentifier) {
        this.environmentName = environmentName;
        this.environmentIdentifier = environmentIdentifier;
    }

    @Override
    public int getValue() {
        return this.environmentIdentifier;
    }

    public static String getUrl() {
        String web = "www";
        if (SERVER_ENVIRONMENT == WTRC) {
            web = "www-wtrc";
        } else if (SERVER_ENVIRONMENT == WTQA) {
            web = "www-wtqa";
        } else if (SERVER_ENVIRONMENT == WTWIP) {
            web = "www-wtwip";
        } else if (SERVER_ENVIRONMENT == WTI) {
            web = "www-wti";
        }
        String affiliate = "";
        if (client.affiliateP != null) {
            affiliate = "/p=" + client.affiliateP;
        }
        String mainSite = client.CURRENT_GAME.name + ".com";
        return "http://" + web + "." + mainSite + "/l=" + client.CURRENT_LANGUAGE + "/a=" + client.AFFILIATE + affiliate + "/";
    }
    
    public static boolean isHostedTestEnvironment(ServerEnvironment environment) {
        return environment == WTRC || environment == WTQA || environment == WTWIP || environment == WTI || environment == INTBETA;
    }
}
