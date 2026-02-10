package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.staticmethods.StaticMethods
import java.awt.Font

object FontManager {

    @JvmField var messageboxFont: FontMetrics? = null
    @JvmField var debugOverlayFont: AbstractFontRenderer? = null
    @JvmField var p12FullIndex: Int = 0
    @JvmField var p11FullIndex: Int = 0
    @JvmField var b12FullIndex: Int = 0
    @JvmField var helvetaBold13Font: Font? = null
    @JvmField var worldMapAreaFont: FontMetrics? = null

    @JvmStatic
    fun getFontSpecifications(): IntArray {
        return intArrayOf(p11FullIndex, p12FullIndex, b12FullIndex)
    }

    @JvmStatic
    fun loadFontArchiveIds(resourceProvider: Js5ResourceProvider) {
        p11FullIndex = resourceProvider.getArchiveId("p11_full")
        p12FullIndex = resourceProvider.getArchiveId("p12_full")
        b12FullIndex = resourceProvider.getArchiveId("b12_full")
    }

    @JvmStatic
    fun initialize() {
        Renderers.FONT_RENDERER = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, p11FullIndex, true, true) as AbstractFontRenderer
        worldMapAreaFont = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, p11FullIndex)
        debugOverlayFont = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, p12FullIndex, true, true) as AbstractFontRenderer
        messageboxFont = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, p12FullIndex)
        MiniMenu.minimenuHeader = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, b12FullIndex, true, true) as AbstractFontRenderer
        FontMetrics.defaultFontMetrics = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, b12FullIndex)
    }
}
