package com.jagex.game.runetek5.icmp;

import com.jagex.game.runetek5.client.input.mouse.MouseActionServer;

public class MouseMovedICmpServiceListener implements ICmpServiceListener {

    @Override
	public void dummy(boolean unused1, int unused2) {
		// NOOP
	}

    @Override
	public void onMouseMoved(int bitpackedPosition, int meta, int constant, int unused) {
		int positionX = bitpackedPosition >> 16;
		int positionY = bitpackedPosition & 0xffff;
		synchronized (MouseActionServer.MOUSE_ACTIONS) {
			MouseActionServer.MOUSE_ACTIONS.add(MouseActionServer.createMouseRecord(positionX, positionY, meta, constant));
		}
	}
}
