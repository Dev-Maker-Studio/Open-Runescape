package com.jagex.game.runetek5.client.input.keyboard;

public abstract class KeyRecorder {

    public static KeyRecorder keyRecorder;

    public abstract void prepareAwaitingKeys();

    public abstract boolean held(int key);

    public abstract KeyRecord popHead();

    public abstract void addKeyListeners();

}
