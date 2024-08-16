package com.jagex.game.runetek5.scene.tile;

import com.jagex.game.runetek5.entity.InteractiveEntityContainer;
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration;
import com.jagex.game.runetek5.entity.gameobject.AbstractWall;
import com.jagex.game.runetek5.entity.gameobject.AbstractWallDecoration;
import com.jagex.game.runetek5.entity.grounditem.AbstractGroundItem;

public class Tile {

    public AbstractGroundDecoration groundDecoration;
    public AbstractWallDecoration secondaryWallDecoration;
    
    /**
     * Always null
     */
    public AbstractWall secondaryWall;
    
    public AbstractWallDecoration primaryWallDecoration;
    public InteractiveEntityContainer interactives;
    public AbstractWall primaryWall;
    public Tile bridge;
    public byte plane;
    public short cullX;
    public AbstractGroundItem groundItem;
    public short cullY; //FIXME:name
    public short interactiveSideViewHeightOffset; //FIXME:name
    public short cullZ; //FIXME:name

    public Tile(int plane) {
        this.plane = (byte) plane;
    }

}
