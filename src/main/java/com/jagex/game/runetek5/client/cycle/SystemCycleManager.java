package com.jagex.game.runetek5.client.cycle;

public class SystemCycleManager extends AbstractCycleManager {

    public long lastNanotime;

    public long currentCycleAt;

    public long nextCycleAt;

    public int cachePos;

    public int entryCount = 1;

    public final long[] timeCache = new long[10];

    public SystemCycleManager() {
        this.currentCycleAt = System.nanoTime();
        this.nextCycleAt = System.nanoTime();
    }


    @Override
    public long getCurrentCycle() {
        return this.currentCycleAt;
    }

    @Override
    long getIdleDurationMs() {
        this.currentCycleAt += averageExecutionTime();
        return this.nextCycleAt > this.currentCycleAt ? (this.nextCycleAt - this.currentCycleAt) / 1000000L : 0L;
    }

    @Override
    int loopsInCycle(long preferedDuration) {
        if (this.nextCycleAt > this.currentCycleAt) {
            this.lastNanotime += this.nextCycleAt - this.currentCycleAt;
            this.currentCycleAt += this.nextCycleAt - this.currentCycleAt;
            this.nextCycleAt += preferedDuration;
            return 1;
        }
        int loops = 0;
        do {
            loops++;
            this.nextCycleAt += preferedDuration;
        } while (loops < 10 && this.nextCycleAt < this.currentCycleAt);
        if (this.nextCycleAt < this.currentCycleAt) {
            this.nextCycleAt = this.currentCycleAt;
        }
        return loops;
    }

    @Override
    public void reset() {
        this.lastNanotime = 0L;
        if (this.nextCycleAt > this.currentCycleAt) {
            this.currentCycleAt += this.nextCycleAt - this.currentCycleAt;
        }
    }

    public long averageExecutionTime() {
        long currentNanotime = System.nanoTime();
        long timedelta = currentNanotime - this.lastNanotime;
        this.lastNanotime = currentNanotime;
        if (timedelta > -5000000000L && timedelta < 5000000000L) {
            this.timeCache[this.cachePos] = timedelta;
            this.cachePos = (this.cachePos + 1) % 10;
            if (this.entryCount < 1) {
                this.entryCount++;
            }
        }
        long total = 0L;
        for (int index = 1; index <= this.entryCount; index++) {
            total += this.timeCache[(this.cachePos - index + 10) % 10];
        }
        return total / this.entryCount;
    }
}
