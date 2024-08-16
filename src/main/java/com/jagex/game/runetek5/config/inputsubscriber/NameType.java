package com.jagex.game.runetek5.config.inputsubscriber;

import com.jagex.game.runetek5.config.Identifiable;

public enum NameType implements Identifiable {

    aClass4134971(1, 0),
    aClass4134965(6, 1),
    aClass4134968(7, 2),
    aClass4134967(3, 3),
    aClass4134970(4, 4),
    aClass4134969(5, 5),
    aClass4134964(0, 6),
    aClass4134966();

    public final int type;
    public final int value;
    public final String str;

    NameType(int type, int value) {
        this.type = type;
        this.value = value;
        this.str = "";
    }

    NameType() {
    	this.type = 2;
    	this.value = -1;
    	this.str = "";
    }

    @Override
    public int getValue() {
        return this.value;
    }

    public String toString() {
        return this.str;
    }
}
