package com.jagex.game.runetek5.entity;

public class InteractiveEntityContainer {

    public AbstractInteractiveEntity interactiveObject;
    public InteractiveEntityContainer child;

    public static InteractiveEntityContainer add(AbstractInteractiveEntity interactive) {
        InteractiveEntityContainer wrapper;
        if (mainInteractiveWrapper == null) {
            wrapper = new InteractiveEntityContainer();
        } else {
            wrapper = mainInteractiveWrapper;
            mainInteractiveWrapper = mainInteractiveWrapper.child;
            wrapper.child = null;
            activeChildCount--;
        }
        wrapper.interactiveObject = interactive;
        return wrapper;
    }

    public void method3560() {
        if (activeChildCount < 500) {
            this.interactiveObject = null;
            this.child = mainInteractiveWrapper;
            mainInteractiveWrapper = this;
            activeChildCount++;
        }
    }
    
    public static InteractiveEntityContainer mainInteractiveWrapper;
    public static int activeChildCount;
}
