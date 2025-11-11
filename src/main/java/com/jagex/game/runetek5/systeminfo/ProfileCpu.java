package com.jagex.game.runetek5.systeminfo;

import java.awt.*;

import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.Utils;

public class ProfileCpu {

    ProfileCpu() {
        throw new Error();
    }

    /**
     * Gets your CPU profile in execution time ms
     * @return
     */
    public static int getProfile() {
        Canvas canvas = new Canvas();
        canvas.setSize(100, 100);
        AbstractToolkit toolkit = AbstractToolkit.create(0, canvas, null, null, 0);
        long timeBeforeBenchmark = Utils.time();
        for (int loops = 0; loops < 10000; loops++) {
            toolkit.benchmark();
        }
        int executionTime = (int) (Utils.time() - timeBeforeBenchmark);
        toolkit.clearPixels(0, 0, 100, 100, ~0xffffff);
        toolkit.cleanupAndShutdown();
        return executionTime;
    }
}
