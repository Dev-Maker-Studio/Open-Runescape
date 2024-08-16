package com.jagex.game.runetek5.client.input.keyboard;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.inputsubscriber.InputSubscriber;
import com.jagex.game.runetek5.config.inputsubscriber.KeyHoldInputSubscriber;

public class KeyDownCommand {

	public static boolean isActive(InputSubscriber subscriber, MouseRecord mouseRecord) {
	    return subscriber != null && subscriber.valid(mouseRecord, client.KEY_RECORDS, client.maximumHeldKeys, KeyRecorder.keyRecorder);
	}

	public static boolean isActive(KeyHoldInputSubscriber keyHoldSubscriber) {
	    return isActive(keyHoldSubscriber, null);
	}

	public static boolean minimenuKeyDown() {
	    return isActive(TypeLists.INTERACTION_DEFAULTS.minimenuKey);
	}

	public static boolean teleportKeyDown() {
	    return isActive(TypeLists.INTERACTION_DEFAULTS.teleportKey);
	}

	public static boolean runKeyDown() {
	    return isActive(TypeLists.INTERACTION_DEFAULTS.toggleRunKey);
	}
}