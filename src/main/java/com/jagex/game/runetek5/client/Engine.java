package com.jagex.game.runetek5.client;

import com.jagex.game.launch.Loader;

import java.awt.*;

@SuppressWarnings("removal")
public interface Engine {

    void supplyApplet(Loader applet);

    void destroy();

    void start();

    void init();

    void paint(Graphics graphics);

}
