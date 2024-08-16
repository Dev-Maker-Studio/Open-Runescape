package com.jagex.game.runetek5.network;

import java.io.IOException;
import java.net.Socket;

import com.jagex.game.launch.Loader;

public class ConnectionInfo {

    public String host;
    public int worldId;
    public int devWorld2 = 1;
    public int devWorld1 = -190419398;
    public boolean alternate = true;
    public boolean proxied;

    public boolean equals(ConnectionInfo connectionInfo) {
        return connectionInfo != null && (this.worldId == connectionInfo.worldId && this.host.equals(connectionInfo.host));
    }

    public void swap() {
        if (!this.alternate) {
            this.alternate = true;
            this.proxied = true;
        } else if (this.proxied) {
            this.proxied = false;
        } else {
            this.alternate = false;
        }
    }

    public Socket createSocket() throws IOException {
        int port = Loader.getPort(worldId);
        if (Loader.DEBUG)
            System.out.println("Creating socket for world: " + worldId + " at " + host + ":" + port);
        return new Socket(this.host, port);
    }
}
