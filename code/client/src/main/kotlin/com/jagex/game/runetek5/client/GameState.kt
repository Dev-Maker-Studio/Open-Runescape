package com.jagex.game.runetek5.client

import com.jagex.game.runetek5.client.login.AccountCreationStage
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.staticmethods.StaticMethods

enum class GameState {
    UNK_0,
    UNK_1,
    UNK_2,
    IN_ACCOUNT_CREATION,
    UNK_4,
    UNK_5,
    UNK_6,
    UNK_7,
    UNK_8,
    UNK_9,
    UNK_10,
    UNK_11,
    UNK_12,
    LOGGED_INGAME,
    UNK_14,
    UNK_15,
    UNK_16,
    UNK_17,
    LOADING_PLEASE_WAIT,
    UNK_19,
    UNK_20,
    UNK_21;

    companion object {
        @JvmStatic
        fun isLoggedOut(gameState: GameState): Boolean {
            return gameState == UNK_5 || gameState == UNK_11 || gameState == UNK_14 ||
                    gameState == UNK_19 || gameState == IN_ACCOUNT_CREATION ||
                    gameState == UNK_8 || gameState == UNK_9
        }

        @JvmStatic
        fun startGameLogin() {
            if (Client.gameState == UNK_5 && !LoginManager.isLoginInProgress() && !AccountCreationStage.isCreatingAccount()) {
                Client.setGameState(UNK_8)
            }
        }

        @JvmStatic
        fun isRenderingScene(gameState: GameState): Boolean {
            return gameState == LOGGED_INGAME || gameState == LOADING_PLEASE_WAIT || gameState == UNK_12
        }

        @JvmStatic
        fun handleDisconnection() {
            if (StaticMethods.isInLobbyState(Client.gameState)) {
                if (Client.LOBBY_CONNECTION_CONTEXT.getConnection() == null) {
                    Client.setGameState(UNK_14)
                } else {
                    Client.setGameState(UNK_0)
                }
            } else if (Client.gameState != UNK_14 && Client.gameState != UNK_19) {
                if (Client.gameState == UNK_12) {
                    Client.setGameState(UNK_5)
                }
            } else {
                Client.setGameState(UNK_5)
            }
        }

        @JvmStatic
        fun isReadyForLogin(): Boolean {
            return Client.gameState == UNK_5 && !LoginManager.isLoginInProgress() && !AccountCreationStage.isCreatingAccount()
        }

        @JvmStatic
        fun inLobby(gameState: GameState): Boolean {
            return gameState == UNK_0 || gameState == UNK_2 || gameState == UNK_7 || gameState == UNK_6
        }

        @JvmStatic
        fun isLoadingRegion(gameState: GameState): Boolean {
            return gameState == UNK_11 || gameState == UNK_2 || gameState == LOADING_PLEASE_WAIT ||
                    gameState == UNK_6 || gameState == UNK_9
        }

        @JvmStatic
        fun isIngame(): Boolean {
            return !isOnloginScreen(Client.gameState)
        }

        @JvmStatic
        fun isOnloginScreen(gameState: GameState): Boolean {
            return gameState == UNK_4 || gameState == UNK_15 || gameState == UNK_1
        }

        @JvmStatic
        fun isWindowUpdate(gameState: GameState): Boolean {
            return gameState == UNK_5 || gameState == UNK_0 || gameState == UNK_7 || gameState == LOGGED_INGAME
        }

        @JvmStatic
        fun isGamestate135(gameState: Int): Boolean {
            return gameState == 1 || gameState == 3 || gameState == 5
        }
    }
}
