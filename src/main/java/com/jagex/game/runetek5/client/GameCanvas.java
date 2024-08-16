package com.jagex.game.runetek5.client;

import java.awt.*;

public class GameCanvas extends Canvas {

    public final Component compont;

    public GameCanvas(Component component) {
        this.compont = component;
    }

    @Override
    public void update(Graphics graphics) {
    	this.compont.update(graphics);
    }

    @Override
    public void paint(Graphics graphics) {
    	this.compont.paint(graphics);
    }

}
