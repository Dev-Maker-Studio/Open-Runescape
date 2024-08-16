package com.jagex.game.runetek5.config;

public interface Identifiable {

    static Identifiable identify(Identifiable[] identifiables, int value) {
        Identifiable[] identifiablesArray = identifiables;
        for (int index = 0; index < identifiablesArray.length; index++) {
            Identifiable identifiable = identifiablesArray[index];
            if (value == identifiable.getValue()) {
                return identifiable;
            }
        }
        return null;
    }

    int getValue();
}
