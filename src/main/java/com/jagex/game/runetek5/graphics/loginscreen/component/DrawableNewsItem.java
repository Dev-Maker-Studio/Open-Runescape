package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.gametip.component.OutlinedNewsItem;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem;
import com.jagex.game.runetek5.network.newsitem.NewsItem;
import com.jagex.game.runetek5.network.newsitem.NewsItemFetcher;
import com.jagex.game.runetek5.rendering.Renderers;

public class DrawableNewsItem implements DrawableItem {

    public final OutlinedNewsItem outlinedNewsItem;

    public final NewsItemFetcher newsItemFetcher;

    public DrawableNewsItem(NewsItemFetcher newsItemFetcher, OutlinedNewsItem outlinedNewsItem) {
        this.outlinedNewsItem = outlinedNewsItem;
        this.newsItemFetcher = newsItemFetcher;
    }

    @Override
    public boolean isCached() {
        return this.newsItemFetcher.fetch();
    }

    @Override
    public void draw(boolean bool1) {
        NewsItem newsitem = this.newsItemFetcher.get(this.outlinedNewsItem.newsitemId);
        if (newsitem != null) {
            int x = this.outlinedNewsItem.horizontalAnchor.calculateDrawX(this.outlinedNewsItem.width, client.GAME_WIDTH) + this.outlinedNewsItem.offsetX;
            int y = this.outlinedNewsItem.verticalAnchor.calculateDrawY(this.outlinedNewsItem.height, client.GAME_HEIGHT) + this.outlinedNewsItem.offsetY;
            if (this.outlinedNewsItem.drawOutline) {
                Renderers.activeToolkit.drawRectangleOutline(x, y, this.outlinedNewsItem.width, this.outlinedNewsItem.height, this.outlinedNewsItem.backgroundColor, 0);
            }
            y += drawText(MiniMenu.minimenuHeader, newsitem.line1, x, y) * 12;
            y += 8;
            if (this.outlinedNewsItem.drawOutline) {
                Renderers.activeToolkit.drawTransparentLine(x, y, this.outlinedNewsItem.width + x - 1, y, this.outlinedNewsItem.backgroundColor, 0);
            }
            ++y;
            y += drawText(MiniMenu.minimenuHeader, newsitem.line2, x, y) * 12;
            y += 5;
            int i_10000 = y + drawText(MiniMenu.minimenuHeader, newsitem.line3, x, y) * 12;
        }
    }


    public int drawText(AbstractFontRenderer renderer, String text, int x, int y) {
        return renderer.drawText(text,
    		x + 5, 
    		y + 5, 
    		this.outlinedNewsItem.width - 10,
    		this.outlinedNewsItem.height - 10,
    		this.outlinedNewsItem.textColor,
    		this.outlinedNewsItem.shadeColor,
    		0, 0, 0, null, null, null, 0, 0);
    }


    @Override
    public void initialize() {
    	// NOOP
    }
}
