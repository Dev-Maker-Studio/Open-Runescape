package com.jagex.game.runetek5.config.inputsubscriber;

public enum InputSubscriberType {
    COMBINED(0),
    KEY_PRESS(1),
    KEY_HOLD(2);

    public final int value;

    InputSubscriberType(int value) {
        this.value = value;
    }

    static InputSubscriberType forId(int valueToFind) {
        InputSubscriberType[] values = values();
        for (int index = 0; index < values.length; index++) {
            InputSubscriberType inputSubscriberType = values[index];
            if (valueToFind == inputSubscriberType.value) {
                return inputSubscriberType;
            }
        }
        return null;
    }
}
