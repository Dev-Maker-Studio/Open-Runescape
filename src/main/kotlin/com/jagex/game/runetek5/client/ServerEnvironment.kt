package com.jagex.game.runetek5.client

import com.jagex.game.runetek5.config.Identifiable

enum class ServerEnvironment(
    @JvmField val environmentName: String,
    @JvmField val environmentIdentifier: Int
) : Identifiable {
    LOCAL("LOCAL", 4),
    WTWIP("WTWIP", 3),
    WTI("WTI", 5),
    WTQA("WTQA", 2),
    WTRC("WTRC", 1),
    INTBETA("INTBETA", 6),
    LIVE("LIVE", 0);

    override fun getValue(): Int {
        return this.environmentIdentifier
    }

    companion object {
        @JvmField var SERVER_ENVIRONMENT: ServerEnvironment? = null
        @JvmField var anInt9282: Int = 0

        @JvmStatic
        fun getUrl(): String {
            var web = "www"
            when (SERVER_ENVIRONMENT) {
                WTRC -> web = "www-wtrc"
                WTQA -> web = "www-wtqa"
                WTWIP -> web = "www-wtwip"
                WTI -> web = "www-wti"
                else -> {}
            }
            var affiliate = ""
            if (client.affiliateP != null) {
                affiliate = "/p=" + client.affiliateP
            }
            val mainSite = client.CURRENT_GAME.name + ".com"
            return "http://$web.$mainSite/l=${client.CURRENT_LANGUAGE}/a=${client.AFFILIATE}$affiliate/"
        }

        @JvmStatic
        fun isHostedTestEnvironment(environment: ServerEnvironment): Boolean {
            return environment == WTRC || environment == WTQA || environment == WTWIP || environment == WTI || environment == INTBETA
        }
    }
}
