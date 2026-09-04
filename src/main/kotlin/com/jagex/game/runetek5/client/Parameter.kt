package com.jagex.game.runetek5.client

class Parameter(
    @JvmField val parameterName: String
) {

    companion object {
        @JvmField val BASE64_URL_DATA = Parameter("1")
        @JvmField val USE_BROWSER_APPLET = Parameter("2")
        @JvmField val CUSTOM_STRING_PARAM = Parameter("3")
        @JvmField val SUPPLEMENTARY_LOGIN_DATA = Parameter("4")
        @JvmField val LOBBY_CONNECTION_HOST = Parameter("5")
        @JvmField val COUNTRY = Parameter("6")
        @JvmField val BUILD_ENVIRONMENT_MINOR = Parameter("7")
        @JvmField val CURRENT_GAME = Parameter("8")
        @JvmField val APPLET = Parameter("9")
        @JvmField val ZAP_ERRORHANDLING = Parameter("10")
        @JvmField val GAMESCREEN_PADDING = Parameter("11")
        @JvmField val COOKIE_PREFIX = Parameter("12")
        @JvmField val LOGIN_TOKEN = Parameter("13")
        @JvmField val LOBBY_WORLD = Parameter("14")
        @JvmField val BUILD_ENVIRONMENT = Parameter("15")
        @JvmField val GAME_WORLD = Parameter("16")
        @JvmField val JS5_AUTH_TOKEN = Parameter("17")
        @JvmField val IS_MEMBER = Parameter("18")
        @JvmField val LOGIN_SCREEN_IMAGE_RESOURCES = Parameter("20")
        @JvmField val COOKIE_DOMAIN = Parameter("21")
        @JvmField val SIGNED_TERMS_AGREEMENT = Parameter("22")
        @JvmField val AFFILIATE = Parameter("23")
        @JvmField val SERVER_ENVIRONMENT = Parameter("24")
        @JvmField val SUPPRESS_TUTORIALS = Parameter("25")
        @JvmField val MACHINE_UID = Parameter("26")
        @JvmField val SESSION_TOKEN = Parameter("27")
        @JvmField val ACCOUNT_CREATED_FLAG = Parameter("28")
        @JvmField val BILLING_FLAG = Parameter("29")
        @JvmField val LOGIN_TYPE = Parameter("30")
        @JvmField val LANGUAGE = Parameter("31")
        @JvmField val MAP_OWNER_IDENTIFIER = Parameter("32")
        @JvmField val AFFILIATE_P = Parameter("33")
        @JvmField val ERROR_WEBSITE = Parameter("34")
        @JvmField val DEBUG_DISPLAY_COLOR_INDEX = Parameter("35")

        @JvmStatic
        fun values(): Array<Parameter> {
            return arrayOf(
                LOGIN_TOKEN, SESSION_TOKEN, CUSTOM_STRING_PARAM, SUPPRESS_TUTORIALS, BUILD_ENVIRONMENT, CURRENT_GAME,
                AFFILIATE, LANGUAGE, SIGNED_TERMS_AGREEMENT, ZAP_ERRORHANDLING, SUPPLEMENTARY_LOGIN_DATA, LOBBY_WORLD,
                SERVER_ENVIRONMENT, LOBBY_CONNECTION_HOST, LOGIN_SCREEN_IMAGE_RESOURCES, USE_BROWSER_APPLET, APPLET,
                IS_MEMBER, MACHINE_UID, BASE64_URL_DATA, ERROR_WEBSITE, AFFILIATE_P, COOKIE_PREFIX,
                MAP_OWNER_IDENTIFIER, BILLING_FLAG, BUILD_ENVIRONMENT_MINOR, GAMESCREEN_PADDING, COOKIE_DOMAIN,
                COUNTRY, JS5_AUTH_TOKEN, LOGIN_TYPE, GAME_WORLD, ACCOUNT_CREATED_FLAG, DEBUG_DISPLAY_COLOR_INDEX
            )
        }
    }
}
