package com.jagex.game.runetek5.client;

import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

public enum GameState {

	UNK_0, UNK_1, UNK_2, IN_ACCOUNT_CREATION, UNK_4, UNK_5, UNK_6, UNK_7, UNK_8, UNK_9, UNK_10, UNK_11, UNK_12, LOGGED_INGAME, UNK_14, UNK_15, UNK_16, UNK_17, UNK_18, UNK_19, UNK_20, UNK_21;

	public static boolean isLoggedOut(GameState gameState) {
        return gameState == GameState.UNK_5 || gameState == GameState.UNK_11 || gameState == GameState.UNK_14 || gameState == GameState.UNK_19 || gameState == GameState.IN_ACCOUNT_CREATION || gameState == GameState.UNK_8 || gameState == GameState.UNK_9;
    }

    public static void method12426() {
        if (client.gameState == GameState.UNK_5 && !LoginManager.method3360() && !AccountCreationStage.method1466()) {
            client.setGameState(GameState.UNK_8);
        }
    }

    public static boolean isRenderingScene(GameState gameState) {
        return gameState == GameState.LOGGED_INGAME || gameState == GameState.UNK_18 || gameState == GameState.UNK_12;
    }

	public static void method6270() {
	    if (StaticMethods.method4180(client.gameState)) {
	        if (client.LOBBY_CONNECTION_CONTEXT.getConnection() == null) {
	            client.setGameState(GameState.UNK_14);
	        } else {
	            client.setGameState(GameState.UNK_0);
	        }
	    } else if (client.gameState != GameState.UNK_14 && client.gameState != GameState.UNK_19) {
	        if (client.gameState == GameState.UNK_12) {
	            client.setGameState(GameState.UNK_5);
	        }
	    } else {
	        client.setGameState(GameState.UNK_5);
	    }
	}

	public static boolean method6693() {
	    return client.gameState == GameState.UNK_5 && (!LoginManager.method3360() && !AccountCreationStage.method1466());
	}

	public static boolean inLobby(GameState gameState) {
	    return gameState == GameState.UNK_0 || gameState == GameState.UNK_2 || gameState == GameState.UNK_7 || gameState == GameState.UNK_6;
	}

	public static boolean isLoadingRegion(GameState gameState) {
	    return gameState == GameState.UNK_11 || gameState == GameState.UNK_2 || gameState == GameState.UNK_18 || gameState == GameState.UNK_6 || gameState == GameState.UNK_9;
	}

	public static boolean isIngame() {
	    return !GameState.isOnloginScreen(client.gameState);
	}

	public static boolean isOnloginScreen(GameState gameState) {
	    return gameState == GameState.UNK_4 || gameState == GameState.UNK_15 || gameState == GameState.UNK_1;
	}

	public static boolean isWindowUpdate(GameState gameState) {
	    return gameState == GameState.UNK_5 || gameState == GameState.UNK_0 || gameState == GameState.UNK_7 || gameState == GameState.LOGGED_INGAME;
	}

	public static boolean isGamestate135(int gameState) {
	    return gameState == 1 || gameState == 3 || gameState == 5;
	}
}
