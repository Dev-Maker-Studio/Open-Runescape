package com.jagex.game.runetek5.client.input.mouse;

import java.awt.*;

public abstract class AbstractMouseManager {

    public static AbstractMouseManager inializeMouseRecorder(Component canvasComponent) {
        return new MouseManager(canvasComponent);
    }

    public abstract boolean leftButtonDown();

    public abstract boolean rightButtonDown();

    public abstract boolean middleButtonDown();

    public boolean isMouseButtonPressed() {
        return leftButtonDown() || rightButtonDown() || middleButtonDown();
    }

    public abstract int getMouseX();

    public abstract int getMouseY();

    public abstract MouseRecord nextSubmission();

    public abstract void reset();

    public abstract void method3589();
}
