package com.jagex.game.runetek5.client.input.mouse;

/**
 * Mouse action that will be processed client side
 * @author Brainpower
 *
 */
public class MouseActionClient extends MouseRecord {

	public static int capacity;
	public static MouseActionClient[] cache = new MouseActionClient[0];
    public static int cursorPos;
    public int mouseY;
	public int clickType;
	public int mouseX;
	public long time;
	public int meta;

    public static void initialize() {
        capacity = 100;
        cache = new MouseActionClient[100];
        cursorPos = 0;
    }

    public static MouseActionClient insert(int clickType, int mouseX, int mouseY, long time, int meta) {
        synchronized (cache) {
            MouseActionClient record;
            if (cursorPos == 0) {
                record = new MouseActionClient();
            } else {
                record = cache[--cursorPos];
            }
            record.clickType = clickType;
            record.mouseX = mouseX;
            record.mouseY = mouseY;
            record.time = time;
            record.meta = meta;
            return record;
        }
    }

    @Override
    public int getClickType() {
        return this.clickType;
    }

    @Override
    public int getX() {
        return this.mouseX;
    }

    @Override
    public int getY() {
        return this.mouseY;
    }

    @Override
    public int getMeta() {
        return this.meta;
    }

    @Override
    public void pushToTop() {
        synchronized (cache) {
            if (cursorPos < capacity - 1) {
                cache[++cursorPos - 1] = this;
            }
        }
    }

    @Override
    public long getTime() {
        return this.time;
    }
}
