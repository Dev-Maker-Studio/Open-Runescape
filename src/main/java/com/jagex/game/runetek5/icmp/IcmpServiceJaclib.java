package com.jagex.game.runetek5.icmp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jaclib.ping.IcmpService;

/**
 * 
 * @author Brainpower - 28/01/2023
 * 
 * Important note: 
 * Several seemingly unused methods here are actually used,
 * be careful with removing code or you'll find your client
 * getting stuck loading
 *
 */
public class IcmpServiceJaclib extends IcmpService {
	public List<ICmpServiceListener> listeners = new ArrayList<ICmpServiceListener>();
	public static volatile IcmpServiceJaclib icmpService;
    
	IcmpServiceJaclib() {
		// NOOP
	}

	protected void notify(int i) { 
		Iterator<ICmpServiceListener> iterator = this.listeners.iterator();
		while (iterator.hasNext()) {
			ICmpServiceListener listener = iterator.next();
			listener.dummy(0 == i, -1942989186);
		}
	}

	protected void notify(int bitpackedPosition, int meta, int dummyAlways0) {
		Iterator<ICmpServiceListener> iterator = this.listeners.iterator();
		while (iterator.hasNext()) {
			ICmpServiceListener listener = iterator.next();
			listener.onMouseMoved(bitpackedPosition, meta, dummyAlways0, 53718982);
		}
	}


	protected void method11597(int i) {
		Iterator<ICmpServiceListener> iterator = this.listeners.iterator();
		while (iterator.hasNext()) {
			ICmpServiceListener listener = iterator.next();
			listener.dummy(0 == i, -1493708806);
		}
	}

	protected void method11598(int i) {
		Iterator<ICmpServiceListener> iterator = this.listeners.iterator();
		while (iterator.hasNext()) {
			ICmpServiceListener listener = iterator.next();
			listener.dummy(0 == i, 1478443735);
		}
	}

	protected void method11599(int i, int i2, int i3) {
		Iterator<ICmpServiceListener> iterator = this.listeners.iterator();
		while (iterator.hasNext()) {
			ICmpServiceListener listener = iterator.next();
			listener.onMouseMoved(i, i2, i3, 53718982);
		}
	}

	protected void method11600(int i, int i4, int i5) {
		Iterator<ICmpServiceListener> iterator = this.listeners.iterator();
		while (iterator.hasNext()) {
			ICmpServiceListener listener = iterator.next();
			listener.onMouseMoved(i, i4, i5, 53718982);
		}
	}

	public static void appendListener(ICmpServiceListener toAdd) {
	    if (icmpService == null) {
	        throw new IllegalStateException("");
	    } else {
	        icmpService.listeners.add(toAdd);
	    }
	}
	
    public static void initialize() {
        if (start(-1)) {
            IcmpServiceJaclib.appendListener(new MouseMovedICmpServiceListener());
        }
    }

	public static boolean start(int dummy) { 
		do {
			boolean available;
			try {
				if (IcmpServiceJaclib.available())
					break;
				available = false;
			} catch (Throwable throwable) {
				return false;
			}
			return available;
		} while (false);
		if (icmpService != null)
			throw new IllegalStateException("");
		icmpService = new IcmpServiceJaclib();
		Thread thread = new Thread(new IcmpServiceRunnable());
		thread.setDaemon(true);
		thread.start();
		return true;
	}


    public static void exit() {
        if (icmpService != null) {
            try {
                icmpService.quit();
            } catch (Throwable ignored) {
            	// NOOP
            }
            icmpService = null;
        }
    }
}
