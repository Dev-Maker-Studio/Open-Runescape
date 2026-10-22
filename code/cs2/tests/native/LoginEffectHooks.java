import com.jagex.game.runetek5.client.Client;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.login.AccountCreationStage;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.Connection;
import java.util.ArrayList;
import java.util.List;

public final class LoginEffectHooks {
    static final List<String> LOG = new ArrayList<>();

    static final class Stub extends Connection {
        final String name;

        Stub(String name) {
            this.name = name;
        }

        public void write(byte[] buffer, int length) {
        }

        public int getInstreamCapacityLeft() {
            return 0;
        }

        public boolean available(int count) {
            return false;
        }

        public void close() {
            LOG.add("close:" + name);
        }

        public void drop() {
        }

        public int read(byte[] buffer, int src, int length) {
            return 0;
        }
    }

    public static void setGameState(GameState state) {
        var previous = Client.gameState;
        if (state == previous) return;
        LOG.add("state:" + state.ordinal());
        if (state == GameState.UNK_10 || state == GameState.UNK_17) LoginManager.startWorldLogin();
        if (state == GameState.UNK_5) LOG.add("prepare");
        if (state == GameState.UNK_0) LOG.add("lobby");
        if (state != GameState.UNK_14 && state != GameState.UNK_12) {
            if (state == GameState.UNK_19 || (state == GameState.UNK_7 && previous != GameState.UNK_6)) {
                LoginManager.startWorldLogin();
            } else if (state == GameState.UNK_8) {
                AccountCreationStage.initAccountCreation();
            }
        } else {
            LoginManager.startLobbyLogin();
        }
        Client.gameState = state;
    }

    public static void logout(boolean reconnect) {
        for (BufferedConnectionContext context : new BufferedConnectionContext[] {Client.GAME_CONNECTION_CONTEXT, Client.LOBBY_CONNECTION_CONTEXT}) context.closeConnection();
        LoginManager.resetLoginState();
        LOG.add("logout:" + reconnect);
        setGameState(reconnect ? GameState.UNK_12 : GameState.UNK_5);
    }
}
