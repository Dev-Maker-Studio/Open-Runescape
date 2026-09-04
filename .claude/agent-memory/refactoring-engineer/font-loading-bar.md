# Font System + Loading Bar Deobfuscation (Phase 4A)

## TransparentFontRendererSM (COMPLETE)
- aGraphicalRendererSub38889 -> safeModeToolkit (SafeModeToolkit reference)
- anIntArray8886 -> glyphWidths (per-char glyph widths)
- anIntArray8888 -> glyphHeights (per-char glyph heights)
- aByteArrayArray8887 -> glyphAlphaData (per-char alpha channel byte arrays)
- anIntArray8890 -> glyphOffsetY (per-char Y offset, from SpriteType.minY)
- anIntArray8891 -> glyphOffsetX (per-char X offset, from SpriteType.minX)
- method14257 -> blendGlyphPixels (alpha-blends glyph onto colour buffer)
- method14258 -> blendGlyphPixelsMasked (alpha-blends with ComponentSurface mask)

## FontMetrics (COMPLETE)
- aStringArray4980 -> lineBuffer (static 100-element line split buffer)
- aClass4143438 -> defaultFontMetrics (b12Full font, used for chat/console/menus)
- aFontRenderer2348 -> boldFontRenderer (bold font renderer for menus)
- method6949 -> getLineCount (splits text, returns line count)
- method6954 -> getPositionAtIndex (maps char index to Point coordinates)
- method6956 -> getIndexAtPosition (maps x,y coordinates to char index)

## StaticMedia
- aClass4142200 -> boldFontMetrics (bold font metrics for menus)

## AbstractFontRenderer
- anInt65 -> originalShadowColor (saved shadow color during calibrate)
- anInt67 -> justifiedSpaceWidth (extra pixel width per space, 8-bit fixed point)
- anInt68 -> justifiedSpaceAccumulator (fractional pixel accumulator for justified text)
- method370 -> calculateJustifiedSpacing
- method372 -> renderLineWithOffsets
- method386 -> renderAntiMacroText

## FontRendererSM
- aByteArrayArray8967 -> glyphPixelData
- anIntArray8972 -> glyphOffsetY
- anIntArray8973 -> glyphOffsetX
- anIntArray8970 -> glyphPalette
- method14325 -> drawGlyphOpaque
- method14334 -> drawGlyphPaletted
- method14326 -> drawGlyphPalettedMasked
- method14327 -> drawGlyphOpaqueMasked

## OpaqueFontRendererSM
- anIntArray8511 -> glyphWidths
- anIntArray8512 -> glyphHeights
- method13771 -> drawGlyphOpaque
- method13774 -> drawGlyphBlendedMasked

## LoadingBar (COMPLETE)
- aNativeSprite9162 -> fillSprite (tiled as loaded/filled portion)
- aNativeSprite9161 -> emptySprite (tiled as unloaded portion)
- aNativeSprite9160 -> leftCapSprite (left end cap)
- aNativeSprite9163 -> rightCapSprite (right end cap)
- aNativeSprite9164 -> topBorderSprite (top border between caps)
- aNativeSprite9165 -> bottomBorderSprite (bottom border between caps)
- method14498 -> drawFillArea (draws tiled fill sprite)

## AnchoredElement
- anInt4092 -> textOffsetY (vertical offset for centered loading text)
- anInt4091 -> textColor (text color for loading text)

## AnimatedLoadingBarElement
- anInt10160 -> scrollSpeed (animation speed multiplier for scrolling fill)

## ComponentDimensionSM
- anIntArray9077 -> rowStartX (per-row starting X positions for masked rendering)
- anIntArray9078 -> rowWidth (per-row widths for masked rendering)
