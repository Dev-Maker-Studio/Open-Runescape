# PathingEntity Deobfuscation

## Renamed Fields

| Old Name | New Name | Evidence |
|----------|----------|---------|
| `anInt10319` | `overheadSpriteCount` | Reset to 0 per update cycle (PlayerUpdate:1245), incremented when overhead sprite drawn (Client:1719,1725), decremented during rendering (EntityUpdating:723) |
| `anInt10325` | `groundContouredOffsetY` | Smoothly interpolates entity Y position toward ground decoration's floorOffsetY via exponential decay. Used as Y translation in rendering: `translate(0, -20 - value, 0)` |
| `anInt10366` | `stalledStepCount` | Counts steps delayed by animation interrupts. When >0, forces faster movement (positionDelta=32) to catch up. Reset to 0 when stationary/force-moving. |
| `anInt10368` | `walkSpeedAccumulator` | Walk smoothing accumulator, only active when `bas.unused != -1` (dead code path). Accumulates/decelerates toward target speed. C++ header confirms `GetWalkSmoothing`. |

## Renamed Methods

| Old Name | New Name | Evidence |
|----------|----------|---------|
| `method15797` | `setEquipmentOverrides` | Called from PlayerUpdate (flag 0x100000) and NPCUpdate (flag 0x4000). Receives override IDs + slot bitmasks, sets `equipmentUpdateDetails`. |
| `method15813` | `setOverheadInterfaceId` | Called from CS2Interpreter. Sets `interfaceIdToLoad`, clears old interface if changed. |
| `method15816` | `clearOverheadInterface` | Clears loaded interface components, resets `loadedInterfaceId = -1`. Only called from `setOverheadInterfaceId`. |
| `method15818` | `drawOverheadInterface` | Called from EntityUpdating for all players/NPCs during render. Draws entity overhead interface via InterfaceComponentRenderer. |
| `method15880` | `processOverheadInterfaceCycle` | Called from InterfaceRenderer during interface cycle processing. Passes clip bounds + mouse coords to InterfaceRenderer.processInterfaceCycle. |

## PathingEntitySequence.kt

| Old Name | New Name | Evidence |
|----------|----------|---------|
| `anInt10065` | `delayTicks` | Set from PlayerUpdate with delay parameter. Decremented each tick; while >0, sequence progression is skipped. |

## Files Modified
- PathingEntity.kt (declaration + usage)
- EntityUpdating.kt (movement + overhead rendering)
- Client.kt (overhead sprite counting)
- PlayerUpdate.kt (reset + equipment override call)
- NPCUpdate.kt (equipment override call)
- NpcEntity.kt (ground offset + stalled step)
- PlayerEntity.kt (ground offset + stalled step)
- CS2Interpreter.kt (setOverheadInterfaceId call)
- InterfaceRenderer.kt (processOverheadInterfaceCycle call)
- PathingEntitySequence.kt (delayTicks field)

## C++ Header Cross-Reference
The C++ function signatures in the PathingEntity.kt header comment confirm:
- `GetWalkSmoothing` -> validates `walkSpeedAccumulator`
- `SetFacingGradual` -> already named `setFacingGradual()`
- `SetExactMove` -> validates force movement fields
- `AddRouteWaypointFine` -> movement queue
- `UseUpWaypoint` -> step consumption
- `ProcessExtendedInfoPackets` -> update protocol masks
- `QueueExtendedInfoPacket` -> extended info buffering
