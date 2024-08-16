package com.jagex.game.runetek5.graphics.loginscreen;

import com.jagex.game.runetek5.config.gametip.ITipDetails;
import com.jagex.game.runetek5.config.gametip.TipRenderDetails;
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub1;
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub2;
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3;
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3_Sub1;
import com.jagex.game.runetek5.config.gametip.component.AnchoredTextMeta;
import com.jagex.game.runetek5.config.gametip.component.OutlinedNewsItem;
import com.jagex.game.runetek5.config.gametip.component.RotatableSpritePositioning;
import com.jagex.game.runetek5.config.gametip.component.SpritePositioning;
import com.jagex.game.runetek5.config.gametip.component.TipsBackground;
import com.jagex.game.runetek5.config.gametip.component.TipsSprite;
import com.jagex.game.runetek5.graphics.loginscreen.component.Class43;
import com.jagex.game.runetek5.graphics.loginscreen.component.AnchoredText;
import com.jagex.game.runetek5.graphics.loginscreen.component.Class52_Sub1;
import com.jagex.game.runetek5.graphics.loginscreen.component.LoadingBar;
import com.jagex.game.runetek5.graphics.loginscreen.component.LoadingBarSub1;
import com.jagex.game.runetek5.graphics.loginscreen.component.Class52_Sub3;
import com.jagex.game.runetek5.graphics.loginscreen.component.DrawableNewsItem;
import com.jagex.game.runetek5.graphics.loginscreen.component.NewsItemBackground;
import com.jagex.game.runetek5.graphics.loginscreen.component.NewsSprite;
import com.jagex.game.runetek5.graphics.loginscreen.component.RotatedNewsSprite;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.network.newsitem.NewsItemFetcher;

public class GameTipsLoader {

    public NewsItemFetcher newsitemFetcher;

    public final Js5ResourceProvider spriteResource;

    public final Js5ResourceProvider fontResource;

    public GameTipsLoader(Js5ResourceProvider spriteResource, Js5ResourceProvider fontResource) {
        this.spriteResource = spriteResource;
        this.fontResource = fontResource;
    }

    public DrawableItem fromDetails(ITipDetails tipDetails) {
        if (tipDetails == null) {
            return null;
        } 
        TipRenderDetails details = tipDetails.getRenderDetails();
        if (details == TipRenderDetails.BACKGROUND) {
        	return new NewsItemBackground((TipsBackground) tipDetails);
        }
        if (details == TipRenderDetails.OUTLINED) {
        	return new DrawableNewsItem(getNewsitemFetcher(), (OutlinedNewsItem) tipDetails);
        }
        if (details == TipRenderDetails.SPRITE) {
        	return new NewsSprite(this.spriteResource, (SpritePositioning) tipDetails);
        }
        if (details == TipRenderDetails.ROTATED_SPRITE) {
        	return new RotatedNewsSprite(this.spriteResource, (RotatableSpritePositioning) tipDetails);
        }
        if (details == TipRenderDetails.aClass60602) {
        	return new Class52_Sub3(this.spriteResource, this.fontResource, (Class350_Sub2) tipDetails);
        }
        if (details == TipRenderDetails.aClass60603) {
        	return new Class52_Sub1(this.spriteResource, this.fontResource, (Class350_Sub1) tipDetails);
        }
        if (details == TipRenderDetails.aClass60608) {
        	return new LoadingBar(this.spriteResource, this.fontResource, (Class350_Sub3) tipDetails);
        }
        if (details == TipRenderDetails.ANCHORED_TEXT) {
        	return new AnchoredText(this.spriteResource, this.fontResource, (AnchoredTextMeta) tipDetails);
        }
        if (details == TipRenderDetails.aClass60601) {
        	return new Class43(this.spriteResource, (TipsSprite) tipDetails);
        }
        if (details == TipRenderDetails.aClass60610) {
        	return new LoadingBarSub1(this.spriteResource, this.fontResource, (Class350_Sub3_Sub1) tipDetails);
        }
        return null;
    }

    private NewsItemFetcher getNewsitemFetcher() {
        if (this.newsitemFetcher == null) {
            this.newsitemFetcher = new NewsItemFetcher();
        }
        return this.newsitemFetcher;
    }
}
