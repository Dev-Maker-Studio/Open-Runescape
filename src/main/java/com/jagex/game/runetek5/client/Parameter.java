package com.jagex.game.runetek5.client;

public class Parameter {

    public static final Parameter aClass3584146 = new Parameter("1");
    public static final Parameter aClass3584135 = new Parameter("2");
    public static final Parameter aClass3584155 = new Parameter("3");
    public static final Parameter aClass3584148 = new Parameter("4");
    public static final Parameter LOBBY_CONNECTION_HOST = new Parameter("5");
    public static final Parameter COUNTRY = new Parameter("6");
    public static final Parameter BUILD_ENVIRONMENT_MINOR = new Parameter("7");
    public static final Parameter CURRENT_GAME = new Parameter("8");
    public static final Parameter APPLET = new Parameter("9");
    public static final Parameter ZAP_ERRORHANDLING = new Parameter("10");
    public static final Parameter GAMESCREEN_PADDING = new Parameter("11");
    public static final Parameter COOKIE_PREFIX = new Parameter("12");
    public static final Parameter aClass3584157 = new Parameter("13");
    public static final Parameter LOBBY_WORLD = new Parameter("14");
    public static final Parameter BUILD_ENVIRONMENT = new Parameter("15");
    public static final Parameter GAME_WORLD = new Parameter("16");
    public static final Parameter aClass3584154 = new Parameter("17");
    public static final Parameter IS_MEMBER = new Parameter("18");
    public static final Parameter LOGIN_SCREEN_IMAGE_RESOURCES = new Parameter("20");
    public static final Parameter COOKIE_DOMAIN = new Parameter("21");
    public static final Parameter aClass3584152 = new Parameter("22");
    public static final Parameter AFFILIATE = new Parameter("23");
    public static final Parameter aClass3584126 = new Parameter("24");
    public static final Parameter aClass3584141 = new Parameter("25");
    public static final Parameter aClass3584147 = new Parameter("26");
    public static final Parameter aClass3584153 = new Parameter("27");
    public static final Parameter aClass3584143 = new Parameter("28");
    public static final Parameter aClass3584145 = new Parameter("29");
    public static final Parameter aClass3584130 = new Parameter("30");
    public static final Parameter LANGUAGE = new Parameter("31");
    public static final Parameter aClass3584149 = new Parameter("32");
    public static final Parameter aClass3584139 = new Parameter("33");
    public static final Parameter ERROR_WEBSITE = new Parameter("34");
    public static final Parameter DEBUG_DISPLAY_COLOR_INDEX = new Parameter("35");

    public final String parameterName;

    public Parameter(String name) {
        this.parameterName = name;
    }

    public static Parameter[] values() {
        return new Parameter[]{aClass3584157, aClass3584153, aClass3584155, aClass3584141, BUILD_ENVIRONMENT, CURRENT_GAME, AFFILIATE, LANGUAGE, aClass3584152, ZAP_ERRORHANDLING, aClass3584148, LOBBY_WORLD, aClass3584126, LOBBY_CONNECTION_HOST, LOGIN_SCREEN_IMAGE_RESOURCES, aClass3584135, APPLET, IS_MEMBER, aClass3584147, aClass3584146, ERROR_WEBSITE, aClass3584139, COOKIE_PREFIX, aClass3584149, aClass3584145, BUILD_ENVIRONMENT_MINOR, GAMESCREEN_PADDING, COOKIE_DOMAIN, COUNTRY, aClass3584154, aClass3584130, GAME_WORLD, aClass3584143, DEBUG_DISPLAY_COLOR_INDEX};
    }
}
