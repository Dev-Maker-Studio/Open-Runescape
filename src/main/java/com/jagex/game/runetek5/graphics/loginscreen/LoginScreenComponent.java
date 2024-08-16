package com.jagex.game.runetek5.graphics.loginscreen;

public interface LoginScreenComponent {

    void initialize();

    void draw();

    void cleanup();

    boolean finished(long initTime);

    int timeBetweenUpdatesMs();

    int calculateLoadingPercent();

}
