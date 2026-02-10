# Client.kt Deobfuscation Progress -- APPLIED AND BUILD VERIFIED

## Status: 68 fields + 13 methods renamed, 14 uncertain fields left obfuscated
## All renames applied across 37 files with clean build (0 errors)

## Renamed Fields (68 total)

### Tick/Timestamp System
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `anInt7347` | `serverTick` | Incremented every game tick, used as global timestamp |
| `anInt7391` | `lastChatMessageTick` | Set to serverTick on FRIENDS_CHAT_CHANNEL packet |
| `anInt7386` | `lastFriendIgnoreUpdateTick` | Set on FRIENDLIST_LOADED, UPDATE_IGNORE_LIST |
| `anInt7179` | `lastFriendsChatUpdateTick` | Set on friends chat update packets |
| `anInt7192` | `lastClanSettingsUpdateTick` | Set on CLANSETTINGS_FULL/DELTA |
| `anInt7395` | `lastClanChannelUpdateTick` | Set on CLANCHANNEL_FULL/DELTA |
| `anInt7320` | `lastGrandExchangeUpdateTick` | Set on UPDATE_GE_SLOT |
| `anInt7397` | `lastMiscUpdateTick` | Set on RUN_ENERGY, PLAYER_WEIGHT, REBOOT_TIMER |

### Camera System
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `aFloat7266` | `cameraYawVelocity` | Applied to camAngleY, decays by /2 each tick |
| `aFloat7284` | `cameraPitchVelocity` | Applied to camAngleX, decays by /2 each tick |
| `aBool7269` | `cameraYawVelocityApplied` | Set true when yaw velocity applied |
| `aBool7331` | `cameraPitchVelocityApplied` | Set true when pitch velocity applied |
| `anInt7262` | `cameraTargetAbsX` | Camera target absolute X position |
| `anInt7376` | `cameraTargetAbsY` | Camera target absolute Y position |
| `anInt7418` | `cameraRandomizationCycle` | Camera jitter cycle counter |
| `anInt7429` | `lastCameraSmoothCycle` | Last smooth camera transition cycle |

### Cutscene Camera Spline
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `anIntArrayArrayArray7302` | `cameraSplinePaths` | 3D array[2] of keyframe data |
| `anInt7277` | `cameraPositionPathIndex` | Index into splinePaths for position |
| `anInt7448` | `cameraLookAtPathIndex` | Index into splinePaths for look-at |
| `anInt7279` | `cameraPositionKeyframe` | Current keyframe in position path |
| `anInt7280` | `cameraLookAtKeyframe` | Current keyframe in look-at path |
| `anInt7256` | `cameraSplineProgress` | 0-65535 fixed-point interpolation |
| `anInt7327` | `cameraSplineSpeedStart` | Starting interpolation speed |
| `anInt7182` | `cameraSplineSpeedEnd` | Ending interpolation speed |
| `anInt7270` | `cameraBaseZoom` | Base zoom value (46) |
| `anInt7271` | `cameraMaxZoom` | Max zoom = base + 800 = 846 |
| `aBool7321` | `cutsceneCameraActive` | Active cutscene camera flag |
| `aBool7355` | `cutsceneCameraCompleted` | Spline segment completed this tick |
| `aBool7285` | `cutsceneCameraPreviouslyCompleted` | Spline completed on previous tick |

### Viewport/FOV Scaling
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `aShort7436` | `viewportFovMin` | FOV at min viewport height (256) |
| `aShort7437` | `viewportFovMax` | FOV at max viewport height (205) |
| `aShort7394` | `cameraZoomMin` | Min camera zoom level (256) |
| `aShort7324` | `cameraZoomMax` | Max camera zoom level (320) |
| `aShort7276` | `fovScaleMin` | Min FOV scale (1) |
| `aShort7441` | `fovScaleMax` | Max FOV scale (32767) |
| `aShort7214` | `aspectFovMin` | Min aspect-adjusted FOV (1) |
| `aShort7474` | `aspectFovMax` | Max aspect-adjusted FOV (32767) |
| `anInt7451` | `viewportScaledFov` | Final viewport-scaled FOV |

### Minimap
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `anInt7259` | `minimapRandomizationCycle` | Minimap jitter cycle |
| `anInt7187` | `minimapRotationStep` | Step size for minimap rotation |
| `anInt7258` | `minimapZoomStep` | Step size for minimap zoom |

### Input/UI
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `anInt7191` | `mouseWheelDelta` | Mouse wheel delta per tick |
| `aBool7364` | `isDragThresholdMet` | Drag threshold exceeded during component drag |
| `anInt7297` | `menuItemHoverCycle` | Cycles since hovering component |
| `anInt7339` | `menuOpenMode` | VarP type 5: 1=single-click opens menu |
| `anInt56` | `selectedComponentHash` | Component idHash for interface-on-X ops |
| `anInt7345` | `selectedSlotId` | Selected slot ID |
| `anInt7346` | `selectedItemId` | Selected item ID |

### Rendering
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `aLong7170` | `lastRedrawTimeMillis` | Time of last redraw in ms |
| `anInt7407` | `componentBackgroundCount` | Number of interface BG components |
| `anInt7408` | `lastRenderedCycle` | Last cycle interfaces were rendered |
| `anInt7396` | `drawsSinceLastPacket` | Draws since last server packet processed |
| `anInt7412` | `interfaceDebugMode` | Debug overlay mode (3=draw rectangles) |
| `anInt7304` | `overlappingEntityCount` | Entities in overlap list |
| `anInt7351` | `overheadChatFilter` | VarP type 6: overhead chat display mode |

### Chat/Skills
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `anInt7389` | `chatMessageHashIndex` | Ring buffer index for chat dedup |
| `anInt7384` | `skillUpdateCycle` | Ring buffer index for skill updates |
| `anIntArray7283` | `recentSkillUpdates` | Ring buffer of recent skill IDs |
| `anIntArray7150` | `recentKeyUpdates` | Ring buffer of recent key updates |

### Camera Shake
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `anIntArray7268` | `CAM_SHAKE_JITTER` | Random jitter amplitude per slot |
| `anIntArray7462` | `CAM_SHAKE_FREQUENCY` | Sine wave frequency per slot |
| `anIntArray7435` | `CAM_SHAKE_PHASE` | Phase counter per slot |

### Constants/Other
| Old Name | New Name | Evidence |
|----------|----------|---------|
| `aColorArray7200` | `DEBUG_TEXT_COLOR` | Debug text color array |
| `anIntArray7148` | `OVERHEAD_TEXT_COLORS` | 6 colors for overhead text |
| `anIntArray7438` | `ANTI_MACRO_TEXT_BOUNDS` | 4-element bounds for anti-macro text |
| `aShortArray7239` | `PLAYER_OP_BASE_TYPES` | Base click types for player options (44-51) |
| `aLong7409` | `clientRandomSeed` | Random seed for ISAAC encryption |
| `aClass4827233` | `TICK_HOOK_REQUESTS` | Tick-based interface hook queue |
| `aClass4827404` | `MOUSE_HOOK_REQUESTS` | Mouse event hook queue |
| `aClass1187352` | `activeDialogueContinue` | Active dialogue continue component |
| `js5AuthToken` | `js5AuthToken` | Param 17: JS5 auth string |
| `machineUid` | `machineUid` | Param 26: hardware identifier long |

## Renamed Methods (13 total)

| Old Name | New Name | Evidence |
|----------|----------|---------|
| `method12044` | `processLobbyTick` | Lobby/login tick processing |
| `method15386` | `processGameTick` | In-game tick processing |
| `method11501` | `drawInterfaceFrame` | Interface frame rendering |
| `method2601` | `drawGameFrame` | Game frame rendering with camera |
| `method2827` | `processDrag` | Component drag logic |
| `method7323` | `resetGraphicsState` | Full graphics state reset |
| `method11634` | `getDragTarget` | Get drag target from depth flags |
| `method3367` | `clearWorldMapCaches` | Clear WorldMapSub1 caches |
| `method4195` | `applyCameraYawRight` | Camera yaw velocity (rightward) |
| `method11750` | `buildOverlappingEntityList` | Build entity overlap sort list |
| `method14562` | `resetGameState` | Reset all game state on logout |
| `method4198` | `isInLobbyOrCreation` | Check lobby/account creation states |
| `method5789` | `isInLobbyConnection` | Check lobby connection states |

## Remaining Obfuscated (16 fields, uncertain)

- `aBool7151` (param 29), `aBool7158` (param 2: browser mode?), `aBool7159` (param 25)
- `aString7165` (param 3), `aString7156` (param 4), `aString5555` (param 13), `aString3643` (param 32)
- `aBool7189` (param 28/CS2), `aBool7160` (param 22), `aBool7241` (hide NPCs?), `aBool7322`, `aBool7323`
- `anInt7163` (param 27), `anInt2831` (param 30), `anInt3644` (login response), `aByte7458` (deprecated packet)
