package com.jagex.game.runetek5.client.login

enum class LoginProt(
    @JvmField val id: Int
) {
    INIT_GAME_CONNECTION(14),
    INIT_JS5REMOTE_CONNECTION(15),
    GAMELOGIN(16),
    LOBBYLOGIN(19),
    REQUEST_WORLDLIST(23),
    CHECK_WORLD_SUITABILITY(24),
    GAMELOGIN_CONTINUE(26),
    SSL_WEBCONNECTION(27),
    CREATE_ACCOUNT_CONNECT(28),
    INIT_SOCIAL_NETWORK_CONNECTION(29),
    SOCIAL_NETWORK_LOGIN(30);

    companion object {
        @JvmField
        val OUTGOING_LOGIN_PACKETS: Array<LoginProt?> = arrayOfNulls(32)

        init {
            for (packet in entries)
                OUTGOING_LOGIN_PACKETS[packet.id] = packet
        }
    }
}
