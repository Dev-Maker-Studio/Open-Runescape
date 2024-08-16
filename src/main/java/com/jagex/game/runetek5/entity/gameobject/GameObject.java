package com.jagex.game.runetek5.entity.gameobject;

import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public interface GameObject {

    int getId();

    void upload();

    boolean hasRenderingShadow();

    void removeShadow(AbstractToolkit toolkit);

    int getShape();

    int getRotation();
    
    void addShadow(AbstractToolkit toolkit);

}
