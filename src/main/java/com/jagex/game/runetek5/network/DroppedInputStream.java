package com.jagex.game.runetek5.network;

import java.io.InputStream;

import com.jagex.game.runetek5.tools.TimeTools;

/**
 * 
 * @author Brainpower - 07/05/2022
 *
 */
public class DroppedInputStream extends InputStream {

    @Override
    public int read() {
        TimeTools.sleep(30000L);
        return -1;
    }
}