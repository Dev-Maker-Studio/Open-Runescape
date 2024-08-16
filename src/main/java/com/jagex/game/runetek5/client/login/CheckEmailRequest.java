package com.jagex.game.runetek5.client.login;

import com.jagex.game.runetek5.config.Identifiable;

public enum CheckEmailRequest implements Identifiable {

    DEFAULT(-2),
    SENT_REQUEST(-3),
    UNUSED_2(2),
    RECEIVED_RESPONSE(3),
    UNUSED_21(21),
    UNUSED_20(20);

    public final int value;

    CheckEmailRequest(int value) {
        this.value = value;
    }

    public static CheckEmailRequest[] getValues() {
        return new CheckEmailRequest[]{SENT_REQUEST, UNUSED_21, UNUSED_20, RECEIVED_RESPONSE, DEFAULT, UNUSED_2};
    }

    @Override
    public int getValue() {
        return this.value;
    }
}
