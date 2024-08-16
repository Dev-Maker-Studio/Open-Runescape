package com.jagex.game.runetek5.icmp;

public class IcmpServiceRunnable implements Runnable {
	
	public void run() {
		try {
			IcmpServiceJaclib.icmpService.run();
		} catch (Throwable throwable) {
			// NOOP
		}
		IcmpServiceJaclib.icmpService = null;
	}
}
