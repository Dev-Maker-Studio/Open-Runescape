package com.jagex.game.runetek5.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostNameIdentifier implements Runnable {

    public volatile String host;

    public InetAddress address;

    public HostNameIdentifier(int ip) throws UnknownHostException {
        this.address = InetAddress.getByAddress(new byte[]{(byte) (ip >> 24 & 0xff), (byte) (ip >> 16 & 0xff), (byte) (ip >> 8 & 0xff), (byte) (ip & 0xff)});
    }

    public String getHostName() {
        return this.host;
    }

    @Override
    public void run() {
        this.host = this.address.getHostName();
    }
}
