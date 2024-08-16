package com.jagex.game.runetek5.sound.player;

public class PcmPlayerManager {
	
	public static boolean sterio;
	public static int threadPriority;
	public static int sampleRate;
	public static AbstractMonoSterioPlayerRunnable runnable;

	public static PcmPlayer start(int type, int sampleRate) {
	    if (sampleRate == 0) {
	        throw new IllegalStateException();
	    } 
	    if (type >= 0 && type < 2) {
	        if (sampleRate < 256) {
	            sampleRate = 256;
	        }
	
	        try {
	            MonoSterioPlayer player = new MonoSterioPlayer();
	            player.bitpackedSoundData = new int[256 * (sterio ? 2 : 1)];
	            player.sampleRate = sampleRate;
	            player.initialize();
	            player.capacity = (sampleRate & -1024) + 1024;
	            if (player.capacity > 16384) {
	                player.capacity = 16384;
	            }
	
	            player.play(player.capacity);
	            if (threadPriority > 0 && runnable == null) {
	                runnable = new MonoSterioPlayerRunnable();
	                Thread thread = new Thread(runnable);
	                thread.setDaemon(true);
	                thread.start();
	                thread.setPriority(threadPriority);
	            }
	
	            if (runnable != null) {
	                if (runnable.monoSterioPlayers[type] != null) {
	                    throw new IllegalArgumentException();
	                }
	
	                runnable.monoSterioPlayers[type] = player;
	            }
	
	            return player;
	        } catch (Throwable throwable) {
	            return new PcmPlayer();
	        }
	    } 
	    throw new IllegalArgumentException();
	}

	public static void setSterio(boolean sterio) {
		PcmPlayerManager.sampleRate = 22050;
		PcmPlayerManager.sterio = sterio;
		PcmPlayerManager.threadPriority = 2;
	}
}
