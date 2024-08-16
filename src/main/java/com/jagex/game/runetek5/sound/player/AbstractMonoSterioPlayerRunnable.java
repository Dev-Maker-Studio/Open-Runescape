package com.jagex.game.runetek5.sound.player;

public abstract class AbstractMonoSterioPlayerRunnable implements Runnable {

    public final PcmPlayer[] monoSterioPlayers = new PcmPlayer[2];

    public volatile boolean exit;

    public volatile boolean running;
}
