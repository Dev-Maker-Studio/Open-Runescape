package com.jagex.game.runetek5.imageproducer;

/**
 * 
 * @author Brainpower - 22/04/2022
 *
 */
public abstract class DelegatingDisplay extends AbstractDisplay {

    public abstract int draw();

    public abstract int draw(int x, int y);

    public abstract void resize(int width, int height);

}
