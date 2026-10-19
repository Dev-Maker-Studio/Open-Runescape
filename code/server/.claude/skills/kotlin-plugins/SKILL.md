---
name: kotlin-plugins
description: How world content registers plugin hooks in compiled Kotlin (never .plugin.kts scripts) - top-level `@ServerStartupEvent` functions calling the `com.rs.plugin.kts` DSL (`onObjectClick`, `onNpcClick`, `onItemClick`, `onButtonClick`, ...) - and how to PROPERLY convert, simplify and optimise a deprecated `@PluginEventHandler` class with static `new XHandler(...)` fields (Java) or `@JvmField val x = XHandler(...)` (Kotlin) into it. Load whenever you write a plugin handler or open, touch or come across a file that still uses the static handler pattern - converting it is part of the change.
---

# Kotlin plugins: `@ServerStartupEvent` + the `com.rs.plugin.kts` DSL

World content registers its hooks from **top-level `@ServerStartupEvent` functions in ordinary
`.kt` source files** under `world/src/main/java/com/rs/game/content/**`. Those functions call the
DSL in `world/src/main/java/com/rs/plugin/kts/PluginGlobals.kt`. That is the only way new handlers
are written.

The `kts` in that package name is only where the functions live. Official content is **never** a
Kotlin script: `plugins/**/*.plugin.kts` exists so people can add content from outside the build,
and the single file there is an example. Nothing we ship goes there.

The static-field pattern is **deprecated**, whatever language it is in:

```java
@PluginEventHandler
public class Foo {
    public static ObjectClickHandler handleX = new ObjectClickHandler(new Object[] { 1234 }, e -> { ... });
}
```
```kotlin
@PluginEventHandler
object Foo { @JvmField val handleX = ObjectClickHandler(arrayOf<Any>(1234)) { e -> ... } }
// or a companion object { @JvmField val ... } on a class
```

**Whenever you touch or come across a file using it, convert the whole file in the same change.**
That means the whole file, not only the handler you needed. A Java plugin becomes a Kotlin file.
Wrapping the static fields in a Kotlin `object` with `@JvmField` is **not** a conversion. It is the
same deprecated pattern in another language.

Reference conversions to read before starting:
- `f805c6c0` TreeGnomeStronghold.
- `85a8e9f9` SpinningWheel.
- `a8ce4a3e` SheepShearing.
- `dd6365ed` LightSource.

## 1. How registration actually works

These facts come from `PluginManager.java`, `PluginMethodRepository.java` and `plugin/events/*`.
- **Startup functions.** `PluginManager.loadPlugins` scans `com.rs` for methods annotated
  `@ServerStartupEvent`. Each one must be **static with no parameters**, or the server throws on
  boot.
  - A Kotlin **top-level** `fun` compiles to a static method on `<File>Kt`, so it qualifies. That
    is the form to use.
  - A function inside an `object` would need `@JvmStatic`. Do not do that. Use top-level functions.
  - `executeStartupHooks` runs them sorted by `ServerStartupEvent.Priority`: `SYSTEM`, `FILE_IO`,
    `GENERAL` (the default), then `POST_PROCESS`. Only pass a priority when the hook depends on
    something loaded earlier, such as file-backed data (`@ServerStartupEvent(ServerStartupEvent.Priority.FILE_IO)`).
  - An exception in one hook is logged and counted in `FAILURES`, and the other hooks still run.
- **The DSL.** Each DSL call builds the same `XHandler` the static field used and passes it to
  `XEvent.registerMethod`, so dispatch is identical. The DSL only removes the reflection and the
  boilerplate. Every key must be an `Int` or a `String`; anything else fails a `require` at startup.
- **Object clicks** (`ObjectClickEvent`):
  - The **id** key is looked up first. The **definition name** is only used when no handler is
    registered for that id. So registering an id hides every name-keyed handler for that loc
    (e.g. `"Stairs"`), which is how you override a generic handler.
  - Within a key, handlers registered for the loc's **tile** win, then those for its **shape**
    (`type =`), then those with neither.
  - **Every** matching handler in that list runs.
  - There is **no option filter** on objects: branch on `option` inside the handler.
  - With `checkDistance = true` (the default) the handler only fires once the player has reached
    the loc; `false` fires on click.
- **NPC clicks** (`NPCClickEvent`):
  - Lookup order is id, then definition name, then an option-only global.
  - With `options = arrayOf("Talk-to")` the handler is stored per option. Without options it is the
    `"global"` handler for that key.
  - There is **one handler per key+option**. Registering the same one twice silently replaces the
    first and prints `ERROR: Duplicate ... methods` to stderr, so grep for existing registrations
    of an id before adding one.
  - `onNpcClick(options = arrayOf("Pickpocket")) { }` with no keys handles that option on every NPC.
- **NPC click interceptors:** `interceptNpcClick("Talk-to") { player, npc -> Boolean }` runs once the
  player has reached the NPC and before the NPC's own handler. Returning `true` consumes the click;
  `false` passes it to the next interceptor and then to the normal id/name/option lookup. Use it
  when an activity borrows NPCs that have their own dialogue (a clue step, a delivery customer)
  instead of registering a second handler for the id. No options means every option. Only return
  `true` after doing something the player can see (`NPCClickEvent.intercepted`, `NPCHandler`).
- **Item clicks:** these do have `options`. `onItemClick(options = arrayOf("Bind"))` with no keys is an
  option-global handler.
- **Buttons:** one handler per **interface id**. Branch on `e.componentId`, or compare `e.component`
  against the `Component.*` packed hash. `onComponentClick(Component.x, ...)` registers a handler for
  single components instead; it wins over the interface's handler for those components, so a feature
  can own one button of an interface another file handles (`BoneSpells.kt` on the modern spellbook).
- **Interface on interface interceptors:** `interceptInterfaceOnInterface(Component.x, ...) { e -> Boolean }`
  runs before the normal `onInterfaceOnInterface` lookup for those source components. Returning `true`
  consumes the event; `false` passes it on. Use it when an activity changes what an existing spell or
  item use does (`MageTrainingArena.kt` on the alchemy and enchant spells).
- **Ground items:** `onGroundItemClick(Obj.x, options = arrayOf("Light"))` is keyed by item id and
  floor option. `checkDistance = true` (the default) fires once the player has reached the item;
  `false` fires on click, before the engine's own Take handling. `onInterfaceOnGroundItem(fromInterfaceIds,
  fromComponents, itemIds)` handles a spell or inventory item used on a ground item; a registration
  with `itemIds` wins over one without for those items.
- **Nothing content-specific goes in a packet handler.** `org.darkan.world.server.packet` and
  `com.rs.net.decoders.handlers` validate the packet and fire a plugin event; they never import
  from `com.rs.game.content`. If no event fits, add a mapped event and a DSL function for it.
- A handler that throws is logged and that one event is dropped. Nothing retries it.

## 2. Static handler -> DSL call

| Deprecated field type | DSL call (import from `com.rs.plugin.kts`) |
|---|---|
| `ObjectClickHandler(keys[, tiles or tile])` | `onObjectClick(*keys, tiles = arrayOf(...), checkDistance = true) { }` |
| `ObjectClickHandler(keys, ObjectShape)` | `onObjectClick(*keys, type = ObjectShape.X) { }` |
| `ObjectClickHandler(false, keys, ...)` | `onObjectClick(*keys, checkDistance = false) { }` |
| `NPCClickHandler([checkDistance,] keys[, options])` | `onNpcClick(*keys, checkDistance = ..., options = arrayOf(...)) { }` |
| `ItemClickHandler(keys[, options])` / `ItemClickHandler(options)` | `onItemClick(*keys, options = arrayOf(...)) { }` / `onItemClick(options = arrayOf(...)) { }` |
| `ButtonClickHandler(interfaceId or ids)` | `onButtonClick(Interface.x, ...) { }` |
| (single buttons of someone else's interface) | `onComponentClick(Component.x, ...) { }` |
| (ground item option) | `onGroundItemClick(Obj.x, options = arrayOf(...), checkDistance = ...) { }` |
| (spell or item on a ground item) | `onInterfaceOnGroundItem(fromInterfaceIds = ..., fromComponents = ..., itemIds = ...) { }` |
| `ItemOnObjectHandler([checkDistance,] objs, items[, tiles])` | `onItemOnObject(arrayOf(...), arrayOf(...) or null, tiles = ..., checkDistance = ...) { }` (arrays, not varargs) |
| `ItemOnNPCHandler([checkDistance,] npcs)` | `onItemOnNpc(*npcs, checkDistance = ...) { }` |
| `ItemOnItemHandler(used)` / `(used[], usedWith[])` / `(directKeySet, keys)` | `onItemOnItem(*used) { }` / `onItemOnItem(used, usedWith) { }` / `onItemOnItem(directKeys, keys) { }` |
| `ItemOnPlayerHandler(keys)` | `onItemOnPlayer(*keys) { }` |
| `ItemEquipHandler`, `DestroyItemHandler`, `DropItemHandler` | `onItemEquip`, `onDestroyItem`, `onDropItem` |
| `ItemAddedToInventoryHandler`, `ItemAddedToBankHandler` | `onItemAddedToInventory`, `onItemAddedToBank` |
| `PickupItemHandler(keys, tiles)` | `onPickupItem(*keys, tiles = ...) { }` |
| `NPCDeathHandler`, `NPCKillParticipatedHandler` | `onNpcDeath`, `onNpcKillParticipation` |
| `NPCDropHandler(npcs, items)` | `onNpcDrop(npcs, items) { }` |
| `NPCInstanceHandler(keys, (id, tile) -> new X(id, tile))` | `instantiateNpc(*keys) { id, tile -> X(id, tile) }` |
| `NPCInteractionDistanceHandler(keys, (p, n) -> d)` | `getInteractionDistance(*keys) { player, npc -> d }` |
| `InterfaceOnInterfaceHandler`, `InterfaceOnNPCHandler`, `InterfaceOnObjectHandler`, `InterfaceOnPlayerHandler` | `onInterfaceOnInterface`, `onInterfaceOnNPC`, `onInterfaceOnObject`, `onInterfaceOnPlayer` (named arrays) |
| `PlayerClickHandler(checkDistance, option)` | `onPlayerClick(checkDistance, option) { }` |
| `PlayerStepHandler(tiles)` | `onPlayerStep(arrayOf(...)) { }` |
| `EnterChunkHandler`, `LoginHandler`, `XPGainHandler` | `onChunkEnter`, `onLogin`, `onXpDrop` |
| `CombatScriptsHandler.addCombatScript(keys, ...)` | `npcCombat(*keys) { npc, target -> delay }` |
| `Entity.addLOSOverride(...)` | `overrideNpcLOS(...)` |

If a handler has no DSL function, add one to `PluginGlobals.kt` in the same shape as its neighbours
(`require` the key types, `XEvent.registerMethod(XEvent::class.java, XHandler(...) { eventHandler(it) })`).
Never fall back to a static field.

## 3. Event parameters: destructure them

Events expose Kotlin properties (`e.player`, `e.obj`, `e.option`, `e.npc`, `e.item`, `e.opNum`,
`e.objectId`, `e.npcId`, `e.componentId`). Never write `e.getPlayer()`. Most events also destructure:

| Event | `(a, b, c, ...)` |
|---|---|
| `ObjectClickEvent` | `(player, obj, option)` |
| `NPCClickEvent` | `(player, npc, option)` |
| `ItemClickEvent` | `(player, item, option, isEquipped)` |
| `ButtonClickEvent` | `(player, interfaceId, componentId, slotId, slotId2, opNum)` |
| `ItemOnObjectEvent` | `(player, obj, item)` |
| `ItemOnNPCEvent` | `(player, item, npc)` |
| `PlayerClickEvent` | `(player, target, option)` |
| `PlayerStepEvent` | `(player, step, tile)` |
| `DestroyItem`/`DropItem`/`ItemAddedTo*` | `(player, item)` |
| `NPCDeathEvent`, `NPCKillParticipatedEvent` | `(killer, npc)` |
| `EnterChunkEvent` | `(entity, chunkId)` |
| `LoginEvent` | `(player)` |

Use `_` for the parts you skip: `{ (player, _, option) -> }`. Use `it` for one-liners.
`IFOn*`, `NPCDrop`, `NPCInstance` and `XPGain` have no components, so use `e.`.

## 4. The conversion, step by step

1. **Read the whole file** and list every static member: handler fields, constants, helper methods,
   inner types.
2. **Find the outside users** of each non-handler member:
   `grep -rn 'Foo\.' world/src --include='*.java' --include='*.kt'`.
   - Handler fields are only read by reflection, so nobody else references them.
   - A helper other code calls becomes a top-level `fun`. Java calls it as `FooKt.helper(...)`;
     update those call sites. If the natural owner of the helper is a class (it holds state, or it
     is the feature's model), move it there per "logic lives where it belongs".
   - A constant other code reads becomes a top-level `const val` (`FooKt.X` from Java), or moves to
     the class that owns it.
3. **Create `Foo.kt`** in the same package, with no license header and no comments. Lay it out as:
   - imports;
   - `private` top-level constants and tables;
   - one `@ServerStartupEvent fun mapFoo()` with every registration;
   - `private` top-level helper functions below it.

   Name the startup function `map<Feature>()` (292 of ~370 existing hooks), one per file.
4. **Translate each handler into a DSL call** and simplify while you do (§5). Several fields that
   differ only by id collapse into one call with more keys, or a loop over a table.
5. **Every id anywhere in the refactored code becomes a gameval constant**, not only the handler
   keys. That covers:
   - anims (`Seq`), graphics (`Spotanim`), sounds (`Sound`);
   - items given, checked or deleted (`Obj`), NPC transforms and spawns (`Npc`);
   - locs spawned or swapped (`Loc`);
   - interfaces and components (`Interface`, `Component`), vars (`Varp`, `Varbit`, `Varc`),
     inventories (`Inv`), enums and structs (`Enum`, `Struct`);
   - ids inside tables, `const val`s, helper functions and other classes the code calls into
     with a literal.

   Look names up in `cache-source/gamevals/<type>.json`; placeholders (`Loc.loc_69558`,
   `Seq.seq_883`) count.

   Before you finish, grep the new file for bare numbers. What may stay literal: coordinates
   (`Tile.of(x, y, plane)`), tick counts and delays, levels and xp, damage, skill indices from
   `Constants`, `-1` meaning "none", and loop or offset arithmetic. A `private const val FOO_ID = 1234`
   is still a magic number: use the gameval directly.
6. **Delete the Java file** (`git rm`). Keep behaviour identical unless you are fixing the ticket;
   the conversion is not a licence to change content silently.
7. **Build on the remote host** (`scratchpad/remote-build.sh :world:build`). An unregistered or
   broken hook shows at boot as `N plugin handler(s) failed to register` or
   `Error executing startup hook`. Content gets no tests.

## 5. Simplify and optimise: what "properly" means

- **Property access and Kotlin idioms everywhere.**
  - `e.player.plane` not `e.getPlayer().getPlane()`, `obj.rotation`, `player.inventory`, `npc.id`.
  - `if`/`when` as expressions; `?.let`/`?: return@onObjectClick` for lookups.
  - `firstOrNull`, `indexOfFirst`, `in 1..2` instead of index loops and chained `||`.
- **Branch on the option with `when (option)`**, one arm per option. Do not register one handler per
  option on objects: there is no option filter, so all of them would run.
- **Exit a handler with `return@onObjectClick`** (the label is the DSL function's name), or restructure
  so no early exit is needed.
- **One helper for repeated geometry.** A rotation `switch` copied into every stair handler becomes one
  private function, e.g. `Tile.ahead(rotation, distance, planeOffset)`. Legacy `Keldagrim.kt` shows the
  copy-paste to avoid.
- **Tables instead of branches.**
  - An id-to-destination ladder becomes `private val X = mapOf(Loc.a to Tile.of(...))`, registered
    with `onObjectClick(*X.keys.toTypedArray()) { (player, obj) -> player.useStairs(X.getValue(obj.id)) }`.
  - Parallel arrays (`materials[]`, `products[]`, `xp[]`) become an `enum class` or a data class
    list with `entries.associateBy`.
- **Key arrays**:
  - `private val KEYS = arrayOf<Any>(Loc.a, Loc.b, "Name")`, spread into varargs with `*KEYS`.
  - `onItemOnObject`/`onNpcDrop` take the arrays themselves.
  - Int-only varargs (`onButtonClick`, `onItemOnItem`) take `IntArray` spread.
- **Private by default.** Nothing in the file is public unless §4.2 found a caller.
- **No wrappers or dead code**:
  - no empty `else -> {}`;
  - no `Unit` returns;
  - no methods that only forward;
  - no `object`/`companion` whose only job was to hold handlers;
  - no unused parameters kept "for compatibility".
- **Behaviour kept exactly:** same `checkDistance`, same `tiles`/`type` scoping, same delays in
  `useStairs(anim, tile, useDelay, totalDelay)`, same option-case handling (keep `lowercase()` if the
  Java lowercased).

## 6. Traps

- `arrayOf(Loc.a)` is an `Array<Int>` and does not spread into `vararg Any`; declare `arrayOf<Any>(...)`.
- Registering a loc id to fix one staircase removes the name-based handler for that id. The id
  handler must cover every option the generic one did.
- Two `onNpcClick` registrations for the same id with no options (or the same option) overwrite each
  other, even across files. Merge them into one handler with a `when (option)`.
- `options` on `onNpcClick` and `onItemClick` must match the definition text exactly
  (`"Talk-to"`, `"Pickpocket"`).
- Never convert a plugin into a `.plugin.kts` script. `PluginScriptHost` loads `plugins/**/*.plugin.kts`
  so people can add content outside the build (`plugins/generic-dialogues/man-dialogue.plugin.kts` is
  only an example). Official content is compiled Kotlin in `world/src/main/java/com/rs/game/content/**`.
- Interaction with `Player`/`NPC` state still uses the engine APIs (`useStairs`, `startConversation { }`,
  `anim(Seq.x)`, `delayLock`). The DSL changes registration, not those calls.

## 7. Worked example: Staircases

Deprecated (Java, and an equally wrong `object` + `@JvmField` Kotlin port of it):

```java
@PluginEventHandler
public class Staircases {
    public static ObjectClickHandler worldStaircasesUP = new ObjectClickHandler(new Object[] { 24357, 24358, 11734 }, e -> {
        Player player = e.getPlayer();
        switch (e.getObj().getRotation()) {
            case 0 -> player.useStairs(-1, player.transform(0, 4, 1), 1, 1);
            case 1 -> player.useStairs(-1, player.transform(4, 0, 1), 1, 1);
            case 2 -> player.useStairs(-1, player.transform(0, -4, 1), 1, 1);
            case 3 -> player.useStairs(-1, player.transform(-4, 0, 1), 1, 1);
        }
    });
    // ... worldStaircasesDOWN with the mirrored switch, a name-keyed "Staircase"/"Stairs" handler
    // delegating to a public static handleStaircases(player, object, option)
}
```

Converted:

```kotlin
package com.rs.game.content.world.areas.global

import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import world.gregs.voidps.type.Tile

@ServerStartupEvent
fun mapStaircases() {
    onObjectClick(Loc.fai_varrock_stairs_taller, Loc.fai_varrock_woodenstairs_castle, Loc.fai_falador_castle_stairs) { (player, obj) ->
        player.useStairs(-1, player.tile.ahead(obj.rotation, 4, 1), 1, 1)
    }
    onObjectClick(Loc.fai_varrock_stairs_top, Loc.fai_varrock_bank_stairs, Loc.fai_falador_castle_stairstop_gloffset) { (player, obj) ->
        val destination = if (obj.tile.isAt(3189, 3432)) player.transform(2, 6400, 0) else player.tile.ahead(obj.rotation, -4, -1)
        player.useStairs(-1, destination, 1, 1)
    }
    onObjectClick("Staircase", "Stairs") { (player, _, option) ->
        when (option.lowercase()) {
            "climb-up" -> if (player.plane < 3) player.useStairs(-1, player.transform(0, 0, 1), 0, 1)
            "climb-down" -> if (player.plane > 0) player.useStairs(-1, player.transform(0, 0, -1), 0, 1)
            "climb" -> if (player.plane in 1..2) player.promptUpDown("Go up the stairs.", player.transform(0, 0, 1), "Go down the stairs.", player.transform(0, 0, -1))
        }
    }
}

private fun Tile.ahead(rotation: Int, distance: Int, planeOffset: Int): Tile = when (rotation and 0x3) {
    0 -> transform(0, distance, planeOffset)
    1 -> transform(distance, 0, planeOffset)
    2 -> transform(0, -distance, planeOffset)
    else -> transform(-distance, 0, planeOffset)
}
```

What changed and why:
- The class, the annotation and the static fields are gone; the hooks are registered by one
  startup function.
- The two four-case switches are one `ahead` helper.
- Ids are `Loc` constants.
- `handleStaircases` had no outside caller, so it is inlined as a `when (option)`.

## 8. Keep this skill current

When a conversion teaches something - a handler without a DSL function, a dispatch rule, an event
without destructuring - add it here in the same change, citing the file that proves it.
