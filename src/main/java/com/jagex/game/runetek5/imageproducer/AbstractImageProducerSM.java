package com.jagex.game.runetek5.imageproducer;

import java.awt.*;

import com.jagex.game.runetek5.toolkit.SafeModeToolkit;

/**
 * 
 * @author Brainpower - 22/04/2022
 *
 */
public abstract class AbstractImageProducerSM extends DelegatingDisplay {

    public boolean bind;
    int width;
    int height;
    final Canvas canvas;
    public final SafeModeToolkit toolkit;
    int[] colorBuffer;
    public float[] depthBuffer;

    AbstractImageProducerSM(SafeModeToolkit safemodeToolkit, Canvas canvas, int width, int height) {
        this.canvas = canvas;
        this.toolkit = safemodeToolkit;
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean reload() {
    	this.toolkit.bind(this.width, this.height, this.colorBuffer, this.depthBuffer);
    	this.bind = true;
        return true;
    }

    @Override
    public void resize(int width, int height) {
        if (this.width != width || height != this.height) {
        	this.width = width;
        	this.height = height;
            initialize();
        }
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean setDepthStencilSurface() {
    	this.bind = false;
        return true;
    }

    void initialize() {
    	this.colorBuffer = new int[this.height * this.width];
    	this.depthBuffer = new float[this.height * this.width];
        if (this.bind) {
        	this.toolkit.bind(this.width, this.height, this.colorBuffer, this.depthBuffer);
        }
    }
}
