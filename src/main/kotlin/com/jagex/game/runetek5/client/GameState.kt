package com.jagex.game.runetek5.client

import com.jagex.game.runetek5.client.login.AccountCreationStage
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.staticmethods.StaticMethods

/**
 * Enumeration of game states representing different stages of the game session.
 */
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
        /**
         * Checks if the game state represents being logged out.
         *
         * @param gameState The game state to check
         * @return True if logged out
         */
        @JvmStatic
        fun isLoggedOut(gameState: GameState): Boolean {
            return gameState == UNK_5 || gameState == UNK_11 || gameState == UNK_14 ||
                    gameState == UNK_19 || gameState == IN_ACCOUNT_CREATION ||
                    gameState == UNK_8 || gameState == UNK_9
        }

        @JvmStatic
        @JvmName("method12426")
        fun method12426() {
            if (Client.gameState == UNK_5 && !LoginManager.method3360() && !AccountCreationStage.isCreatingAccount()) {
                Client.setGameState(UNK_8)
            }
        }

        /**
         * Checks if the game state is currently rendering the scene.
         *
         * @param gameState The game state to check
         * @return True if rendering scene
         */
        @JvmStatic
        fun isRenderingScene(gameState: GameState): Boolean {
            return gameState == LOGGED_INGAME || gameState == LOADING_PLEASE_WAIT || gameState == UNK_12
        }

        @JvmStatic
        @JvmName("method6270")
        fun method6270() {
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
        @JvmName("method6693")
        fun method6693(): Boolean {
            return Client.gameState == UNK_5 && !LoginManager.method3360() && !AccountCreationStage.isCreatingAccount()
        }

        /**
         * Checks if the game state represents being in the lobby.
         *
         * @param gameState The game state to check
         * @return True if in lobby
         */
        @JvmStatic
        fun inLobby(gameState: GameState): Boolean {
            return gameState == UNK_0 || gameState == UNK_2 || gameState == UNK_7 || gameState == UNK_6
        }

        /**
         * Checks if the game state is currently loading a region.
         *
         * @param gameState The game state to check
         * @return True if loading region
         */
        @JvmStatic
        fun isLoadingRegion(gameState: GameState): Boolean {
            return gameState == UNK_11 || gameState == UNK_2 || gameState == LOADING_PLEASE_WAIT ||
                    gameState == UNK_6 || gameState == UNK_9
        }

        /**
         * Checks if the player is currently in game (not on login screen).
         *
         * @return True if in game
         */
        @JvmStatic
        fun isIngame(): Boolean {
            return !isOnloginScreen(Client.gameState)
        }

        /**
         * Checks if the game state is on the login screen.
         *
         * @param gameState The game state to check
         * @return True if on login screen
         */
        @JvmStatic
        fun isOnloginScreen(gameState: GameState): Boolean {
            return gameState == UNK_4 || gameState == UNK_15 || gameState == UNK_1
        }

        /**
         * Checks if the game state allows window updates.
         *
         * @param gameState The game state to check
         * @return True if window updates are allowed
         */
        @JvmStatic
        fun isWindowUpdate(gameState: GameState): Boolean {
            return gameState == UNK_5 || gameState == UNK_0 || gameState == UNK_7 || gameState == LOGGED_INGAME
        }

        /**
         * Checks if the game state ID is 1, 3, or 5.
         *
         * @param gameState The game state ordinal
         * @return True if game state is 1, 3, or 5
         */
        @JvmStatic
        fun isGamestate135(gameState: Int): Boolean {
            return gameState == 1 || gameState == 3 || gameState == 5
        }
    }
}
