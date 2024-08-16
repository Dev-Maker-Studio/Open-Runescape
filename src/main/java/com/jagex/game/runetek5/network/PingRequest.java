package com.jagex.game.runetek5.network;

public class PingRequest {

    public volatile int ping = -1;
    public final String ip;

    public PingRequest(String ip) {
        this.ip = ip;
    }

}
