package com.jagex.game.runetek5.client.input.keyboard;

public interface KeyRecord {

    char getCharacter();

    int getType();

    int getCode();

    long getTimeSubmitted();

    int getFlags();

}