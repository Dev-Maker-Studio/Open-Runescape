package com.jagex.game.runetek5.network;

import com.jagex.game.launch.Loader;

import java.io.IOException;
import java.net.Socket;

public abstract class SocketManager {

    public String siteHostName;
    public  int sitePort;

    public abstract Socket getProxySocket() throws IOException;

    public Socket getLobbySocket() throws IOException {
        return new Socket(Loader.IP_ADDRESS, Loader.LOBBY_PORT);
    }

}
