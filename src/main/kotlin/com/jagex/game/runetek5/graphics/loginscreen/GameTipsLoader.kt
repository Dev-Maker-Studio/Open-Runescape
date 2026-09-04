package com.jagex.game.runetek5.graphics.loginscreen

import com.jagex.game.runetek5.config.gametip.ITipDetails
import com.jagex.game.runetek5.config.gametip.TipRenderDetails
import com.jagex.game.runetek5.config.gametip.component.SpriteLoadingBarElement
import com.jagex.game.runetek5.config.gametip.component.ColorLoadingBarElement
import com.jagex.game.runetek5.config.gametip.component.MultiSpriteLoadingBarElement
import com.jagex.game.runetek5.config.gametip.component.AnimatedLoadingBarElement
import com.jagex.game.runetek5.config.gametip.component.AnchoredTextMeta
import com.jagex.game.runetek5.config.gametip.component.OutlinedNewsItem
import com.jagex.game.runetek5.config.gametip.component.RotatableSpritePositioning
import com.jagex.game.runetek5.config.gametip.component.SpritePositioning
import com.jagex.game.runetek5.config.gametip.component.TipsBackground
import com.jagex.game.runetek5.config.gametip.component.TipsSprite
import com.jagex.game.runetek5.graphics.loginscreen.component.LoginBackgroundSprite
import com.jagex.game.runetek5.graphics.loginscreen.component.AnchoredText
import com.jagex.game.runetek5.graphics.loginscreen.component.SpriteLoadingBar
import com.jagex.game.runetek5.graphics.loginscreen.component.LoadingBar
import com.jagex.game.runetek5.graphics.loginscreen.component.AnimatedLoadingBar
import com.jagex.game.runetek5.graphics.loginscreen.component.SolidLoadingBar
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
        if (details == TipRenderDetails.COLOR_LOADING_BAR) {
            return SolidLoadingBar(spriteResource, fontResource, tipDetails as ColorLoadingBarElement)
        }
        if (details == TipRenderDetails.SPRITE_LOADING_BAR) {
            return SpriteLoadingBar(spriteResource, fontResource, tipDetails as SpriteLoadingBarElement)
        }
        if (details == TipRenderDetails.MULTI_SPRITE_LOADING_BAR) {
            return LoadingBar(spriteResource, fontResource, tipDetails as MultiSpriteLoadingBarElement)
        }
        if (details == TipRenderDetails.ANCHORED_TEXT) {
            return AnchoredText(spriteResource, fontResource, tipDetails as AnchoredTextMeta)
        }
        if (details == TipRenderDetails.TIPS_SPRITE) {
            return LoginBackgroundSprite(spriteResource, tipDetails as TipsSprite)
        }
        if (details == TipRenderDetails.ANIMATED_LOADING_BAR) {
            return AnimatedLoadingBar(spriteResource, fontResource, tipDetails as AnimatedLoadingBarElement)
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
