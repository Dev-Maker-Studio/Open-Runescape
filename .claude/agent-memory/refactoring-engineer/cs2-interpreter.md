# CS2Interpreter.kt Deobfuscation Notes

## Overview
CS2Interpreter.kt is ~11,195 lines. The file itself has only 4 object-level fields, but references
~337 obfuscated fields on OTHER classes within its opcode handler methods.

## Component Event Hook Decode Order (Binary Format)
The Component interface definition binary format decodes event hooks in this fixed order:
1. onLoadScript
2. onMouseOver
3. onMouseLeaveScript
4. hookParams
5. **onTargetEnter** (was anObjectArray1400) - fires when USE mode activated
6. **onVarpTransmit** (was anObjectArray1397) - fires on varp/config changes
7. mouseLeaveScript
8. **onStatTransmit** (was anObjectArray1387) - fires on skill/stat changes
9. **onTimer** (was anObjectArray1409) - fires every client tick
10. params
11. **onTargetLeave** (was anObjectArray1393) - fires on IF_ON_IF (component-on-component)
12. popupScript
13. onClick
14. **onClickRepeat** (was anObjectArray1319) - fires each tick while held on clicked component
15. onRelease
16. onHold
17. **onDrag** (was anObjectArray1451) - fires while dragging
18. **onDragComplete** (was anObjectArray1394) - fires when drag ends
19. onMouseMove
20. onKey
21. onScrollWheel
22. varps (IntArray filter)
23. mouseLeaveArrayParams (IntArray filter)
24. skillUpdateFilter (IntArray filter)
25. keyPressArray (IntArray filter)
26. mouseWheelArray (IntArray filter)

## Component Event Hook Trigger Mapping (InterfaceRenderer.kt)
| Field | Trigger Condition | Filter Array |
|-------|-------------------|--------------|
| onClickRepeat | isPressed && leftClickVar | none |
| onVarpTransmit | pulseEventConfigsPtr changed | varps |
| onStatTransmit | skillUpdateCycle changed | skillUpdateFilter |
| onClanVarTransmit | CLAN_VAR_COUNTER changed | clanVarFilter |
| onChatTransmit | lastChatMessageTick changed | none |
| onFriendTransmit | lastFriendIgnoreUpdateTick changed | none |
| onFriendsChatTransmit | lastFriendsChatUpdateTick changed | none |
| onClanSettingsTransmit | lastClanSettingsUpdateTick changed | none |
| onClanChannelTransmit | lastClanChannelUpdateTick changed | none |
| onStockTransmit | lastGrandExchangeUpdateTick changed | none |
| onMiscTransmit | lastMiscUpdateTick changed | none |
| onKeyType | keyCachePointerPoss > 0 | none |
| onCamFinished | cutsceneCameraCompleted | none |
| onTargetEnter | setUseOptionFlags() called | none |
| onTargetLeave | IF_ON_IF (PacketEncoder) | none |
| onTimer | TICK_HOOK_REQUESTS every tick | none |
| onDrag | drag in progress (Client.kt) | none |
| onDragComplete | drag released (Client.kt) | none |
| onResize | width/height changed (InterfaceRenderer) | none |

## Component Tick Tracking Fields
| Field | Tracks |
|-------|--------|
| lastServerTick (was anInt1442) | Client.serverTick |
| lastVarpTransmitCycle (was anInt1443) | Client.pulseEventConfigsPtr |
| lastContainerTransmitCycle (was anInt1444) | Client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS |
| lastStatTransmitCycle (was anInt1445) | Client.skillUpdateCycle |
| lastClanVarTransmitCycle (was anInt1448) | Client.CLAN_VAR_COUNTER |

## Menu Chrome Sprite Layout
method8206 configures all menu/scrollbar sprites:
- Arg 0: menuBackgroundColor (TextureOpIrregularBricks)
- Arg 1: menuTransparencyOffset (NativeSprite)
- Arg 2: hoverHighlightColor (MiniMenu)
- Arg 3: menuBodyTransparencyOffset (StaticMethods)
- Arg 4: menuHeaderTileSpriteId (StaticMethods) -> menuHeaderTileSprite (StaticMedia)
- Arg 5: menuBorderSpriteId (NativeSprite) -> menuHeaderLeftSprite (StaticMedia) + menuHeaderRightSprite (StaticMethods, flipped)
- Arg 6: scrollbarBottomBorderSpriteId (StaticMethods) -> scrollbarBottomBorderSprite (StaticMethods)
- Arg 7: scrollbarSideBorderSpriteId (StaticMethods) -> scrollbarLeftBorderSprite (StaticMedia) + scrollbarRightBorderSprite (StaticMedia, flipped)
- Arg 8: scrollbarCornerSpriteId (StaticMethods) -> scrollbarLeftCornerSprite (StaticMethods) + scrollbarRightCornerSprite (StaticMethods, flipped)
- Arg 9: headerTextColor (MiniMenu)
- Arg 10: menuEntryShadowColor (StaticMethods)
- Arg 11: menuFontId (StaticMethods)

## Remaining Unknowns
- Client.aBool7158 (param 2): passed as `isApplet` to Browser API. Conflicts with existing Client.isApplet (param 9)
- Client.aString7165 (param 3): unknown string parameter, only read by one CS2 opcode (instr6721)
