package com.jagex.game.runetek5.client.cycle;

import com.jagex.game.runetek5.tools.TimeTools;

public abstract class AbstractCycleManager {

    public int loopcycle(long preferedDuration) {
        long sleepTime = getIdleDurationMs();
        if (sleepTime > 0L) {
            TimeTools.sleep(sleepTime);
        }
        return loopsInCycle(preferedDuration);
    }

    abstract long getIdleDurationMs();

    abstract int loopsInCycle(long preferedDuration);

    public abstract long getCurrentCycle();

    public abstract void reset();
}
