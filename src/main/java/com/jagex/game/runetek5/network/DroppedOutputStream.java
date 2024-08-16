package com.jagex.game.runetek5.network;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * @author Brainpower - 07/05/2022
 *
 */
public class DroppedOutputStream extends OutputStream {

    @Override
    public void write(int data) throws IOException {
        throw new IOException();
    }
}