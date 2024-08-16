package com.jagex.game.runetek5.client.input.mouse;

import java.awt.*;
import java.util.LinkedList;
import java.util.Queue;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.network.packet.MouseClickPacket;
import com.jagex.game.runetek5.network.packet.MouseMovementPacket;
import com.jagex.game.runetek5.utils.Utils;

/**
 * Mouse action that will be processed server side
 * @author Brainpower
 *
 */
public class MouseActionServer extends MouseRecord {

    public int clickMeta;
    public int constant;
    public long time;
    public int x;
    public int y;
	public static int cachePtr;
	public static MouseActionServer[] cache = new MouseActionServer[0];
	public static long lastSubmitted = -1L;
	public static boolean appletHasFocus = true;
	public static final MouseMovementPacket MOVE_PACKET = new MouseMovementPacket();
	public static final MouseClickPacket CLICK_PACKET = new MouseClickPacket();
	public static final Queue<MouseActionServer> MOUSE_ACTIONS = new LinkedList<MouseActionServer>();
	public static int cacheSize;

    public static void refreshCache() {
        synchronized (MouseActionServer.MOUSE_ACTIONS) {
            while (true) {
                MouseActionServer record = MouseActionServer.MOUSE_ACTIONS.poll();
                if (record == null) {
                    return;
                }
                record.pushToTop();
            }
        }
    }

    public static void cycle() {
        synchronized (MOUSE_ACTIONS) {
            Point screenLocation;
            if (GameShell.gameCanvas.isShowing()) {
                screenLocation = GameShell.gameCanvas.getLocationOnScreen();
            } else {
                screenLocation = null;
            }
            while (true) {
                while (true) {
                    MouseActionServer record = MOUSE_ACTIONS.poll();
                    if (record == null) {
                        return;
                    }
                    if (screenLocation != null && GameShell.gameCanvas.isShowing() && GameShell.appletHasFocus) {
                        record.offsetBy(screenLocation);
                        if (!record.clientSidedOnly() && record.getX() < GameShell.screenCanvasWidth && record.getY() < GameShell.screenCanvasHeight * -969250379 && record.getX() >= 0 && record.getY() >= 0) {
                            int clickType = record.getClickType();
                            if (record.getClickType() == -1) {
                                MouseActionServer.CLICK_PACKET.append(record);
                            } else if (MouseRecord.clicktypeRequiresRecord(clickType)) {
                                MouseActionServer.CLICK_PACKET.addToQueue(record);
                            }
                        }
                    } else {
                        record.pushToTop();
                    }
                }
            }
        }
    }

    public static void initialize() {
        MouseActionServer.cacheSize = 100;
        MouseActionServer.cache = new MouseActionServer[100];
        MouseActionServer.cachePtr = 0;
    }

    public int getConstant() {
        return this.constant;
    }

    @Override
    public void pushToTop() {
        synchronized (cache) {
            if (cachePtr < cacheSize - 1) {
                cache[++cachePtr - 1] = this;
            }
        }
    }

    @Override
    public int getX() {
        return this.x;
    }

    public int getClickMeta() {
        return this.clickMeta;
    }

    public boolean clientSidedOnly() {
        switch (this.clickMeta) {
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 172:
            case 173:
            case 174:
            case 672:
            case 674:
                return true;
            default:
                return false;
        }
    }

    @Override
    public int getMeta() {
        switch (this.clickMeta) {
            case 160:
            case 512:
                return 0;
            case 163:
            case 166:
            case 169:
            case 173:
            case 515:
            case 518:
            case 521:
            case 525:
                return 2;
            default:
                return 1;
        }
    }

    @Override
    public long getTime() {
        return this.time;
    }

    public void offsetBy(Point point) {
        this.x -= point.x;
        this.y -= point.y;
    }

    @Override
    public int getClickType() {
        switch (this.clickMeta) {
            case 160:
            case 512:
                return -1;
            case 161:
            case 163:
            case 513:
            case 515:
                return 0;
            case 162:
            case 514:
                return 3;
            case 164:
            case 166:
            case 516:
            case 518:
                return 2;
            case 165:
            case 517:
                return 5;
            case 167:
            case 169:
            case 519:
            case 521:
                return 1;
            case 168:
            case 520:
                return 4;
            case 170:
            case 522:
                return 6;
            default:
                return -2;
        }
    }

    @Override
    public int getY() {
        return this.y;
    }

	public static MouseActionServer createMouseRecord(int x, int y, int meta, int constant) {
	    synchronized (MouseActionServer.cache) {
	        MouseActionServer record;
	        if (MouseActionServer.cachePtr == 0) {
	            record = new MouseActionServer();
	        } else {
	            record = MouseActionServer.cache[--cachePtr];
	        }
	        record.x = x;
	        record.y = y;
	        record.clickMeta = meta;
	        record.constant = constant;
	        record.time = Utils.time();
	        return record;
	    }
	}

	public static void reset() {
	    MouseActionServer.MOVE_PACKET.reset();
	    MouseActionServer.CLICK_PACKET.reset();
	    MouseActionServer.lastSubmitted = -1L;
	    MouseActionServer.appletHasFocus = true;
	}
}
