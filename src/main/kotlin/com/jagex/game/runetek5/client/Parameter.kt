package com.jagex.game.runetek5.client

class Parameter(
    @JvmField val parameterName: String
) {

    companion object {
        @JvmField val aClass3584146 = Parameter("1")
        @JvmField val aClass3584135 = Parameter("2")
        @JvmField val aClass3584155 = Parameter("3")
        @JvmField val aClass3584148 = Parameter("4")
        @JvmField val LOBBY_CONNECTION_HOST = Parameter("5")
        @JvmField val COUNTRY = Parameter("6")
        @JvmField val BUILD_ENVIRONMENT_MINOR = Parameter("7")
        @JvmField val CURRENT_GAME = Parameter("8")
        @JvmField val APPLET = Parameter("9")
        @JvmField val ZAP_ERRORHANDLING = Parameter("10")
        @JvmField val GAMESCREEN_PADDING = Parameter("11")
        @JvmField val COOKIE_PREFIX = Parameter("12")
        @JvmField val aClass3584157 = Parameter("13")
        @JvmField val LOBBY_WORLD = Parameter("14")
        @JvmField val BUILD_ENVIRONMENT = Parameter("15")
        @JvmField val GAME_WORLD = Parameter("16")
        @JvmField val aClass3584154 = Parameter("17")
        @JvmField val IS_MEMBER = Parameter("18")
        @JvmField val LOGIN_SCREEN_IMAGE_RESOURCES = Parameter("20")
        @JvmField val COOKIE_DOMAIN = Parameter("21")
        @JvmField val aClass3584152 = Parameter("22")
        @JvmField val AFFILIATE = Parameter("23")
        @JvmField val aClass3584126 = Parameter("24")
        @JvmField val aClass3584141 = Parameter("25")
        @JvmField val aClass3584147 = Parameter("26")
        @JvmField val aClass3584153 = Parameter("27")
        @JvmField val aClass3584143 = Parameter("28")
        @JvmField val aClass3584145 = Parameter("29")
        @JvmField val aClass3584130 = Parameter("30")
        @JvmField val LANGUAGE = Parameter("31")
        @JvmField val aClass3584149 = Parameter("32")
        @JvmField val aClass3584139 = Parameter("33")
        @JvmField val ERROR_WEBSITE = Parameter("34")
        @JvmField val DEBUG_DISPLAY_COLOR_INDEX = Parameter("35")

        @JvmStatic
        fun values(): Array<Parameter> {
            return arrayOf(
                aClass3584157, aClass3584153, aClass3584155, aClass3584141, BUILD_ENVIRONMENT, CURRENT_GAME,
                AFFILIATE, LANGUAGE, aClass3584152, ZAP_ERRORHANDLING, aClass3584148, LOBBY_WORLD,
                aClass3584126, LOBBY_CONNECTION_HOST, LOGIN_SCREEN_IMAGE_RESOURCES, aClass3584135, APPLET,
                IS_MEMBER, aClass3584147, aClass3584146, ERROR_WEBSITE, aClass3584139, COOKIE_PREFIX,
                aClass3584149, aClass3584145, BUILD_ENVIRONMENT_MINOR, GAMESCREEN_PADDING, COOKIE_DOMAIN,
                COUNTRY, aClass3584154, aClass3584130, GAME_WORLD, aClass3584143, DEBUG_DISPLAY_COLOR_INDEX
            )
        }
    }
}
