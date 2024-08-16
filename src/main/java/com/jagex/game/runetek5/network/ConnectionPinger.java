package com.jagex.game.runetek5.network;

import jaclib.ping.Ping;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.jagex.game.runetek5.tools.TimeTools;

public class ConnectionPinger implements Runnable {

    public String ip;
    public InetAddress address;
    public volatile long ping = -1L;
    public volatile boolean enabled = true;

    public void setIp(String ip) {
        this.ip = ip;
        this.address = null;
        this.ping = -1L;
        if (this.ip != null) {
            try {
            	this.address = InetAddress.getByName(this.ip);
            } catch (UnknownHostException unknownHostException) {
            	// NOOP
            }
        }
    }

    public void disable() {
        this.enabled = false;
    }

    @Override
    public void run() {
        while (this.enabled) {
            requestPing();
        }
    }

    public long getPing() {
        return this.ping;
    }

    public void requestPing() {
        if (this.address != null) {
            try {
                byte[] ip = this.address.getAddress();
                this.ping = Ping.ping(ip[0], ip[1], ip[2], ip[3], 10000L);
            } catch (Throwable throwable) {
            	// NOOP
            }
        }
        TimeTools.sleep(1000L);
    }
}
