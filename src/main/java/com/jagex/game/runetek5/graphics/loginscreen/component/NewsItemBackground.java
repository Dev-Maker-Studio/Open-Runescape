package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.config.gametip.component.TipsBackground;
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem;
import com.jagex.game.runetek5.rendering.Renderers;

/**
 * 
 * @author Steven - 01/01/2023
 *
 */
public class NewsItemBackground implements DrawableItem {

    public final TipsBackground background;

    public NewsItemBackground(TipsBackground background) {
        this.background = background;
    }
    
    @Override
    public void draw(boolean draw) {
        if (draw) {
            Renderers.activeToolkit.drawFilledRectangle(0, 0, GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379, this.background.color, 0);
        }
    }

    @Override
    public boolean isCached() {
        return true;
    }

    @Override
    public void initialize() {
    	// NOOP
    }
}
