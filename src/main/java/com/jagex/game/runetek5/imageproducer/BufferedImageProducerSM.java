package com.jagex.game.runetek5.imageproducer;

import java.awt.*;
import java.awt.image.*;
import java.util.Hashtable;

import com.jagex.game.runetek5.toolkit.SafeModeToolkit;

public class BufferedImageProducerSM extends AbstractImageProducerSM {

    public Image image;

    public BufferedImageProducerSM(SafeModeToolkit hardwarerenderer1, Canvas canvas2, int i_3, int i_4) {
        super(hardwarerenderer1, canvas2, i_3, i_4);
        new Rectangle();
        initialize();
    }

    @Override
    void initialize() {
        super.initialize();
        DataBufferInt dataBuffer = new DataBufferInt(super.colorBuffer, super.colorBuffer.length);
        DirectColorModel colorModel = new DirectColorModel(32, 16711680, 65280, 255);
        WritableRaster writableRaster = Raster.createWritableRaster(colorModel.createCompatibleSampleModel(super.width, super.height), dataBuffer, null);
        this.image = new BufferedImage(colorModel, writableRaster, false, new Hashtable());
    }

    @Override
    public int draw() {
        return draw(0, 0);
    }

    @Override
    public int draw(int x, int y) {
        Graphics graphics = super.canvas.getGraphics();
        graphics.drawImage(this.image, x, y, super.canvas);
        return 0;
    }

    @Override
    public void cleanup() {
    }
}
