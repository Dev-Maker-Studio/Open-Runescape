package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit

object StaticMedia {

    @JvmField var headiconPkGroupid: Int = 0
    @JvmField var headiconsPrayerGroupid: Int = 0
    @JvmField var hintHeadiconsGroupid: Int = 0
    @JvmField var hintMapmarkersGroupid: Int = 0
    @JvmField var mapflagGroupid: Int = 0
    @JvmField var crossGroupid: Int = 0
    @JvmField var mapdotsGroupid: Int = 0
    @JvmField var nameIconsGroupid: Int = 0
    @JvmField var floorShadowsGroupid: Int = 0
    @JvmField var compassGroupid: Int = 0
    @JvmField var otherLevelGroupid: Int = 0
    @JvmField var hintMapedgeGroupid: Int = 0

    @JvmField var headiconPkSprites: Array<NativeSprite>? = null
    @JvmField var headiconPrayerSprites: Array<NativeSprite>? = null
    @JvmField var hintHeadiconSprites: Array<NativeSprite>? = null
    @JvmField var hintMapmarkerSprites: Array<NativeSprite>? = null
    @JvmField var hintMapedgeSprites: Array<NativeSprite>? = null
    @JvmField var otherLevelSprite: NativeSprite? = null
    @JvmField var nameIconSprites: Array<NativeSprite>? = null
    @JvmField var mapflagSprites: Array<NativeSprite>? = null
    @JvmField var compassSprite: NativeSprite? = null
    @JvmField var mapdotSprites: Array<NativeSprite>? = null
    @JvmField var crossSprites: Array<NativeSprite>? = null

    @JvmField var chatIcons: Array<NativeSprite?>? = null
    @JvmField var totalChatIcons: Int = 0
    @JvmField var chatScaleHeights: IntArray? = null
    @JvmField var boldFontMetrics: FontMetrics? = null
    @JvmField var loadingBarBackgroundSprite: NativeSprite? = null

    @JvmField var menuRightSideBorderSprite: NativeSprite? = null
    @JvmField var menuLeftSideBorderSprite: NativeSprite? = null

    @JvmField var loadingBarEdgeSprite: NativeSprite? = null

    @JvmStatic
    fun loadGroupIds(spriteResource: Js5ResourceProvider) {
        headiconPkGroupid = spriteResource.getArchiveId("headicons_pk")
        headiconsPrayerGroupid = spriteResource.getArchiveId("headicons_prayer")
        hintHeadiconsGroupid = spriteResource.getArchiveId("hint_headicons")
        hintMapmarkersGroupid = spriteResource.getArchiveId("hint_mapmarkers")
        mapflagGroupid = spriteResource.getArchiveId("mapflag")
        crossGroupid = spriteResource.getArchiveId("cross")
        mapdotsGroupid = spriteResource.getArchiveId("mapdots")
        nameIconsGroupid = spriteResource.getArchiveId("name_icons")
        floorShadowsGroupid = spriteResource.getArchiveId("floorshadows")
        compassGroupid = spriteResource.getArchiveId("compass")
        otherLevelGroupid = spriteResource.getArchiveId("otherlevel")
        hintMapedgeGroupid = spriteResource.getArchiveId("hint_mapedge")
    }

    @JvmStatic
    fun loadSprites(toolkit: AbstractToolkit, spriteResource: Js5ResourceProvider) {
        var spriteTypes = SpriteType.decodeGroup(spriteResource, headiconPkGroupid)!!
        headiconPkSprites = Array(spriteTypes.size) { index ->
            toolkit.generateSprite(spriteTypes[index], true)
        }

        spriteTypes = SpriteType.decodeGroup(spriteResource, headiconsPrayerGroupid)!!
        headiconPrayerSprites = Array(spriteTypes.size) { index ->
            toolkit.generateSprite(spriteTypes[index], true)
        }

        spriteTypes = SpriteType.decodeGroup(spriteResource, hintHeadiconsGroupid)!!
        hintHeadiconSprites = Array(spriteTypes.size) { index ->
            toolkit.generateSprite(spriteTypes[index], true)
        }

        spriteTypes = SpriteType.decodeGroup(spriteResource, hintMapmarkersGroupid)!!
        hintMapmarkerSprites = Array(spriteTypes.size) { index ->
            toolkit.generateSprite(spriteTypes[index], true)
        }

        spriteTypes = SpriteType.decodeGroup(spriteResource, mapflagGroupid)!!
        var colorOffset: Byte = 25
        mapflagSprites = Array(spriteTypes.size) { index ->
            spriteTypes[index].offsetRgbColor(-colorOffset + (Math.random() * colorOffset * 2.0).toInt(), -colorOffset + (Math.random() * colorOffset * 2.0).toInt(), -colorOffset + (Math.random() * colorOffset * 2.0).toInt())
            toolkit.generateSprite(spriteTypes[index], true)
        }

        spriteTypes = SpriteType.decodeGroup(spriteResource, crossGroupid)!!
        crossSprites = Array(spriteTypes.size) { index ->
            toolkit.generateSprite(spriteTypes[index], true)
        }

        spriteTypes = SpriteType.decodeGroup(spriteResource, mapdotsGroupid)!!
        colorOffset = 12
        mapdotSprites = Array(spriteTypes.size) { index ->
            spriteTypes[index].offsetRgbColor(-colorOffset + (Math.random() * colorOffset * 2.0).toInt(), -colorOffset + (Math.random() * colorOffset * 2.0).toInt(), -colorOffset + (Math.random() * colorOffset * 2.0).toInt())
            toolkit.generateSprite(spriteTypes[index], true)
        }

        spriteTypes = SpriteType.decodeGroup(spriteResource, nameIconsGroupid)!!
        colorOffset = 12
        nameIconSprites = Array(spriteTypes.size) { index ->
            spriteTypes[index].offsetRgbColor(-colorOffset + (Math.random() * colorOffset * 2.0).toInt(), -colorOffset + (Math.random() * colorOffset * 2.0).toInt(), -colorOffset + (Math.random() * colorOffset * 2.0).toInt())
            toolkit.generateSprite(spriteTypes[index], true)
        }

        compassSprite = toolkit.generateSprite(SpriteType.listSingle(spriteResource, compassGroupid, 0)!!, true)
        otherLevelSprite = toolkit.generateSprite(SpriteType.listSingle(spriteResource, otherLevelGroupid, 0)!!, true)

        spriteTypes = SpriteType.decodeGroup(spriteResource, hintMapedgeGroupid)!!
        hintMapedgeSprites = Array(spriteTypes.size) { index ->
            toolkit.generateSprite(spriteTypes[index], true)
        }
    }

    @JvmStatic
    fun fullyCached(spritesResource: Js5ResourceProvider): Int {
        var fullyCached = 0
        if (spritesResource.isFileCached(headiconPkGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(headiconsPrayerGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(hintHeadiconsGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(hintMapmarkersGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(mapflagGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(crossGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(mapdotsGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(nameIconsGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(floorShadowsGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(compassGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(otherLevelGroupid)) {
            fullyCached++
        }
        if (spritesResource.isFileCached(hintMapedgeGroupid)) {
            fullyCached++
        }
        return fullyCached
    }

    @JvmStatic
    fun initializeChatIcons(nativeSprites: Array<NativeSprite>) {
        totalChatIcons = nativeSprites.size
        chatIcons = arrayOfNulls(totalChatIcons + 10)
        chatScaleHeights = IntArray(totalChatIcons + 10)
        System.arraycopy(nativeSprites, 0, chatIcons, 0, totalChatIcons)
        for (index in 0 until totalChatIcons) {
            chatScaleHeights!![index] = chatIcons!![index]!!.getSpriteHeight()
        }
        for (index in totalChatIcons until chatIcons!!.size) {
            chatScaleHeights!![index] = 12
        }
    }

    @JvmStatic
    fun reset() {
        Renderers.FONT_RENDERER = null
        FontManager.debugOverlayFont = null
        MiniMenu.minimenuHeader = null
        headiconPkSprites = null
        headiconPrayerSprites = null
        hintHeadiconSprites = null
        hintMapmarkerSprites = null
        mapflagSprites = null
        crossSprites = null
        mapdotSprites = null
        nameIconSprites = null
        compassSprite = null
        otherLevelSprite = null
        hintMapedgeSprites = null
    }

    @JvmStatic
    fun resetChatMedia() {
        loadingBarEdgeSprite = null
        StaticMethods.loadingBarTileSprite = null
        loadingBarBackgroundSprite = null
        menuLeftSideBorderSprite = null
        menuRightSideBorderSprite = null
        StaticMethods.menuTopBorderSprite = null
        StaticMethods.menuBottomLeftCornerSprite = null
        StaticMethods.menuBottomRightCornerSprite = null
        FontMetrics.boldFontRenderer = null
        boldFontMetrics = null
    }

    @JvmStatic
    fun drawClickCross() {
        val x = Client.crossX
        val y = Client.crossY
        if (Client.crossType == 1) {
            val sprite = crossSprites!![Client.crossSpritePtr / 100]
            sprite.draw(x - 8, y - 8)
        }
        if (Client.crossType == 2) {
            val sprite = crossSprites!![Client.crossSpritePtr / 100 + 4]
            sprite.draw(x - 8, y - 8)
        }
    }

    @JvmStatic
    fun resetChatMediaAndIcons() {
        resetChatMedia()
        chatIcons = null
    }
}
