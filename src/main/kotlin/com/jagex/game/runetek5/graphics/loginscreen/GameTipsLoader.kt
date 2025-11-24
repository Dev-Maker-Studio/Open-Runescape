package com.jagex.game.runetek5.graphics.loginscreen

import com.jagex.game.runetek5.config.gametip.ITipDetails
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub1
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub2
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3
import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3_Sub1
import com.jagex.game.runetek5.config.gametip.component.AnchoredTextMeta
import com.jagex.game.runetek5.config.gametip.component.OutlinedNewsItem
import com.jagex.game.runetek5.config.gametip.component.RotatableSpritePositioning
import com.jagex.game.runetek5.config.gametip.component.SpritePositioning
import com.jagex.game.runetek5.config.gametip.component.TipsBackground
import com.jagex.game.runetek5.config.gametip.component.TipsSprite
import com.jagex.game.runetek5.graphics.loginscreen.component.Class43
import com.jagex.game.runetek5.graphics.loginscreen.component.AnchoredText
import com.jagex.game.runetek5.graphics.loginscreen.component.Class52_Sub1
import com.jagex.game.runetek5.graphics.loginscreen.component.LoadingBar
import com.jagex.game.runetek5.graphics.loginscreen.component.LoadingBarSub1
import com.jagex.game.runetek5.graphics.loginscreen.component.Class52_Sub3
import com.jagex.game.runetek5.graphics.loginscreen.component.DrawableNewsItem
import com.jagex.game.runetek5.graphics.loginscreen.component.NewsItemBackground
import com.jagex.game.runetek5.graphics.loginscreen.component.NewsSprite
import com.jagex.game.runetek5.graphics.loginscreen.component.RotatedNewsSprite
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.network.newsitem.NewsItemFetcher

/**
 * Loads game tips and creates drawable items from tip details.
 */
class GameTipsLoader(
    @JvmField val spriteResource: Js5ResourceProvider,
    @JvmField val fontResource: Js5ResourceProvider
) {

    @JvmField var newsitemFetcher: NewsItemFetcher? = null

    fun fromDetails(tipDetails: ITipDetails?): DrawableItem? {
        if (tipDetails == null) {
            return null
        }
        val details = tipDetails.getRenderDetails()
        if (details == TipRenderDetails.BACKGROUND) {
            return NewsItemBackground(tipDetails as TipsBackground)
        }
        if (details == TipRenderDetails.OUTLINED) {
            return DrawableNewsItem(getNewsitemFetcher(), tipDetails as OutlinedNewsItem)
        }
        if (details == TipRenderDetails.SPRITE) {
            return NewsSprite(spriteResource, tipDetails as SpritePositioning)
        }
        if (details == TipRenderDetails.ROTATED_SPRITE) {
            return RotatedNewsSprite(spriteResource, tipDetails as RotatableSpritePositioning)
        }
        if (details == TipRenderDetails.aClass60602) {
            return Class52_Sub3(spriteResource, fontResource, tipDetails as Class350_Sub2)
        }
        if (details == TipRenderDetails.aClass60603) {
            return Class52_Sub1(spriteResource, fontResource, tipDetails as Class350_Sub1)
        }
        if (details == TipRenderDetails.aClass60608) {
            return LoadingBar(spriteResource, fontResource, tipDetails as Class350_Sub3)
        }
        if (details == TipRenderDetails.ANCHORED_TEXT) {
            return AnchoredText(spriteResource, fontResource, tipDetails as AnchoredTextMeta)
        }
        if (details == TipRenderDetails.aClass60601) {
            return Class43(spriteResource, tipDetails as TipsSprite)
        }
        if (details == TipRenderDetails.aClass60610) {
            return LoadingBarSub1(spriteResource, fontResource, tipDetails as Class350_Sub3_Sub1)
        }
        return null
    }

    private fun getNewsitemFetcher(): NewsItemFetcher {
        if (newsitemFetcher == null) {
            newsitemFetcher = NewsItemFetcher()
        }
        return newsitemFetcher!!
    }
}
