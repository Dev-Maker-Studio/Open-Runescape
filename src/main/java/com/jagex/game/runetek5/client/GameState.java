package com.jagex.game.runetek5.client;

import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

public class GameState {

    public static boolean isLoggedOut(int gameState) {
        return gameState == 5 || gameState == 11 || gameState == 14 || gameState == 19 || gameState == 3 || gameState == 8 || gameState == 9;
    }

    public static void method12426() {
        if (client.gameState == 5 && !LoginManager.method3360() && !AccountCreationStage.method1466()) {
            client.setGameState(8);
        }
    }

    public static boolean isRenderingScene(int gameState) {
        return gameState == 13 || gameState == 18 || gameState == 12;
    }

	public static void method6270() {
	    if (StaticMethods.method4180(client.gameState)) {
	        if (client.LOBBY_CONNECTION_CONTEXT.getConnection() == null) {
	            client.setGameState(14);
	        } else {
	            client.setGameState(0);
	        }
	    } else if (client.gameState != 14 && client.gameState != 19) {
	        if (client.gameState == 12) {
	            client.setGameState(5);
	        }
	    } else {
	        client.setGameState(5);
	    }
	}

	public static boolean method6693() {
	    return client.gameState == 5 && (!LoginManager.method3360() && !AccountCreationStage.method1466());
	}

	public static boolean inLobby(int gameState) {
	    return gameState == 0 || gameState == 2 || gameState == 7 || gameState == 6;
	}

	public static boolean isLoadingRegion(int gameState) {
	    return gameState == 11 || gameState == 2 || gameState == 18 || gameState == 6 || gameState == 9;
	}

	public static boolean isIngame() {
	    return !GameState.isOnloginScreen(client.gameState);
	}

	public static boolean isOnloginScreen(int gameState) {
	    return gameState == 4 || gameState == 15 || gameState == 1;
	}

	public static boolean isWindowUpdate(int gameState) {
	    return gameState == 5 || gameState == 0 || gameState == 7 || gameState == 13;
	}

	public static boolean isGamestate135(int gameState) {
	    return gameState == 1 || gameState == 3 || gameState == 5;
	}
}
