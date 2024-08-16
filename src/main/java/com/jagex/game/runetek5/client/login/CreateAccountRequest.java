package com.jagex.game.runetek5.client.login;

import com.jagex.game.runetek5.config.Identifiable;

public enum CreateAccountRequest implements Identifiable {

    aClass4945786(-2),
    aClass4945790(-3),
    aClass4945781(2),
    aClass4945782(3),
    aClass4945783(9),
    aClass4945784(10),
    aClass4945785(20),
    aClass4945792(21),
    aClass4945787(30),
    aClass4945788(31),
    aClass4945789(32),
    aClass4945780(33),
    aClass4945779(34),
    aClass4945791(38);

    public final int anInt5793;

    CreateAccountRequest(int i_1) {
        this.anInt5793 = i_1;
    }

    public static CreateAccountRequest[] method8038() {
        return new CreateAccountRequest[]{aClass4945784, aClass4945792, aClass4945787, aClass4945789, aClass4945786, aClass4945782, aClass4945788, aClass4945779, aClass4945790, aClass4945783, aClass4945791, aClass4945781, aClass4945785, aClass4945780};
    }

    @Override
    public int getValue() {
        return this.anInt5793;
    }
}
