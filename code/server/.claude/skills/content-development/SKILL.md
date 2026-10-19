---
name: content-development
description: How server content is developed and proven in Darkan - the world module's plugin and dialogue patterns, cache-side definitions (objs, seqs, structs, enums, models) and their registration, importing assets from another revision, the pack/verify/read-back loop, and how the user checks it in the client. Load before writing or porting any game content. Maintained as we learn.
---

# Content development

Content is a server half (the `world` module) and a cache half (`cache-source/`), and neither is
done until the other agrees with it. Code standards
(`darkan-code-standards`) apply unchanged; clientscript, interface and graphic work follows
`cs2-engineer`. The RS3 urn port (`docs/rs3-port.md`, `docs/reference/rs3-urns.json`) is the
worked example of everything below.

## 1. The loop

1. Establish what the cache holds before designing: definitions in `cache-source/config_*`,
   names in `cache-source/gamevals/<type>.json`, the client's own tables in
   `cache-source/clientscripts` (grep the strings you see in game - a menu text, a guide row -
   the source may be a hard-coded script, not a definition). Assume nothing about ids or byte
   layouts; read them.
2. Allocate every new id next-free at the end of its index, in a deterministic order written
   down first (a spec under `docs/reference/`), and give every id its real name in the catalog.
3. Cache side: write the files, register them, pack, **read back** with the decoders (`jshell`
   against the tools install, `tools cs2 decompile <id>`), and `cache verify` against the
   baseline signature (§5).
4. Server side: Kotlin against the generated gameval constants; `scratchpad/remote-build.sh
   build` (never Gradle on the desktop; one build at a time). No content tests (§5).
5. Hand it to the user: local lobby + world + client on their display (§6). Never drive the
   client yourself to validate content; use the tools viewer for interface visuals.

## 2. Server content in the world module

- **Plugin handlers** follow `kotlin-plugins` (load it):
  - Register them from a top-level `@ServerStartupEvent fun mapFeature()` in a compiled `.kt` file,
    calling the `com.rs.plugin.kts` DSL (`onObjectClick`, `onNpcClick`, `onItemClick`,
    `onItemOnItem`, `onButtonClick(Interface.x)`, `onNpcDrop`, ...).
  - `onXpDrop { }` fires on every `Skills.addXp` with the raw xp, before rates.
  - Static `new XHandler(...)` fields and `@JvmField val x = XHandler(...)` are deprecated; convert
    any file using them when you touch it.
  - Never put content in a `.plugin.kts` script. Those are for content added outside the build.
- **Ids** are `Obj.x`, `Seq.x`, `Interface.x`, `Component.interface_x.y`; a `Urn`-style table is
  an `enum class` with explicit fields per state (never `base + n` arithmetic) and
  `entries.associateBy` lookups in the companion.
- **Dialogues**: `player.startConversation { makeX(intArrayOf(obj), max); exec { ... } }` (the
  Kotlin builder, `DialogueDsl.kt`); the chosen MakeX amount is
  `MakeXStatement.getQuantity(player)` (varbits 8094/8095, 60 when nothing was set). Category
  pickers are `MakeXStatement(int[] icons, String[] labels)` with one `Dialogue` per option
  (`Conversation.addNext(statement, *options)`); `MakeXActionD`/`MakeXItem` do the timed
  crafting with xp, anim, level and skill.
- **Items**: `Item.addMetaData("key", value)` / `getMetaDataD` for per-item state (an urn's xp);
  `item.id = ...` then `inventory.refresh()` to transform in place; `inventory.getNumberOf`,
  `containsOneItem`, `hasFreeSlots`, `deleteItem(id, n)`, `addItem(Item(id, n))` (non-stackable
  n > 1 fills n slots). `player.getItemFromInv(id)` is the first match.
- **Obj tooltips**: `ItemTooltips.metadata("key", TooltipSection.X) { player, item, value -> "line" }`
  adds a line under the obj's name for every item carrying that metadata key;
  `register(objs) { player, item -> }` does the same per obj. Lines sort by section (CHARGES,
  CONTENTS, STATE, FOOTER; an item's own `tooltip` metadata string is the footer), and
  `ItemTooltips.count("Charges", n, max)` / `value(x)` keep the formatting uniform. The text takes
  the client's tags (`<col>`, `<br>`, `<sprite=graphic>`, `<obj=id,amount>`). Nothing needs
  refreshing: an item in an `ItemsContainer` marks its own slot dirty on any metadata,
  `setAmount` or `changeId` write, and the once-per-tick flush (`WorldEncoder.sendDirtyItems`)
  sends only the slots whose obj, amount or tooltip differ from what the client last got. A value
  that changes every tick but shows coarsely (degrade charges) writes the map silently and calls
  `item.markChanged()` only when the shown text changes (`DegradeCharges.drain`).
- **Options** on a definition are addressed by **op number, 1-based**: `getOption(1)` is op1,
  `hasOption(5, "Build")` is op5, and `optionsIndex` answers in op numbers too. Never write 0 for
  the first option - the underlying array is an implementation detail. `hasOption` is named apart
  from the legacy 0-based `containsOption(int, ...)` on purpose, so a re-synced legacy call fails
  to compile instead of quietly reading the option next door.
- **Timed sequences**: `player.schedule { wait(n) }` (`game.model.entity.async`) for per-entity
  steps, `WorldTasks.schedule(delay) { }` for world timers. `Entity.reset()` clears the entity's
  scheduled tasks (`clearPendingTasks`), so nothing in a `schedule` block may `wait()` after it calls
  `reset()`; hand the tail to `WorldTasks` (`StealingCreationController.sendDeath`).
- **Animation**: `entity.anim(Animation)`; `setNextAnimation` is deprecated. XP: `skills.addXp`
  fires the event chain, `addXpLamp` does not.
- **Messages** use the definition names (`ItemDefinitions.getDefs(id).name.lowercase()`,
  `Utils.addArticle`), never a hard-coded copy of a name.

## 3. Cache definitions

- **New obj/seq/struct**: copy the same-role sibling file, set the fields, **delete
  `opcodeOrder`** (it is a whitelist; a field whose opcode is not listed is dropped at pack), and
  write it in the tree's JSON shape (`DefinitionJson`: field order, arrays/maps inline while the
  line fits 100 columns, else one entry per line). A new file inside an existing archive needs
  no `index.json` line; a new archive (every model) does. Struct filenames are the id; the `id`
  field inside is wrong for most and never read.
- **Seqs** that only differ by the held item are cloned from the same-tier sibling with
  `rightHandItem` moved; worn models must be set on the obj for the item to show in hand.
- **Skill guide rows**: one struct + one row in the skill's enum, level-sorted (see
  `cs2-engineer` §4 "Skill guides - one data source"); both guides read it.
- **Catalogs**: every new id gets a `gamevals/<type>.json` entry named for its purpose in the
  identified-name convention (an imported asset keeps its source revision's dev name) and the
  coverage block bumped; `clientscripts/gamevals.d.ts` gets the sorted declaration. A pre-existing
  id missing from a catalog (obj 25358 was) is a defect to fix, not a pattern to follow.
- **Data the client also holds elsewhere**: menu/guide/popup text that is hard-coded in a
  clientscript must be moved to the definitions it duplicates, not edited in two places.

## 4. Importing assets from another revision

`tools rs3port` (`docs/rs3-port.md`) is the pattern: a read-only source tree, a spec that maps
source ids to allocated 727 ids, a converter proven by **calibration** on assets both revisions
share (convert them, diff against the shipped files, gate on 100% where the data allows), a
learned crosswalk for anything that is not a copy (material ids, texture projections), and an
idempotent importer that refuses to run when an allocated id is not exactly one past the tree's
ceiling. Facts that only calibration found: RS3 negates y and reverses winding, keeps the 727
colour in a vertex stream, re-mapped every material id. Do not trust a plan's claims about a
format; measure on the shared assets.

- **Find the whole dependency closure first.** A spotanim is a model + seq, the seq is frames in
  one or more anim archives on a base, and the model can carry a material (-> texture -> sprite),
  particle emitters (-> producer -> its material), effectors and billboards (-> billboard config
  -> material). RS3 has no spotanim catalog: find spotanims through the seq/model names
  (`njloot_2013_*` is the golden loot beam) and grep `config_spotanim` for those ids.
- **Reuse before you append**, but only on proof: an RS3 id maps to an existing 727 one when every
  asset both caches share agrees (3131 -> 1168 on all 9 shared billboards); anything without a
  precedent is appended. Record each decision and its evidence in the spec.
- **Learn cross-revision maps from same-id pairs**: producers, effectors and billboards keep their
  ids across revisions, so a field or material crosswalk is a vote over every shared id.
  Frame archives do not keep their ids (RS3 renumbered them); pair them through seqs whose
  durations and frame file ids match.
- **RS3 formats (949)**: glb submesh material = material id + 1, 0 = none; model
  `attachmentsA/B/C` = billboards (config inlined, `b` = material + 1) / emitters (producer id +
  three positions) / effectors (particle id + one position) - 727 needs a real face or vertex for
  each; v2 frames hold a rotation as a (sin, cos) x 16384 pair per axis where 727 holds one smart
  at 4096 per turn; RS3 billboard sizes are 727's minus one; producer opcodes 29/35/36 and
  spotanim opcode 46 do not exist in 727 and would desync its reader - strip them. RS3 seq opcode
  13 bytes are already 727's sound grammar.
- **727 textures** are op graphs; an imported image becomes a sprite in `graphics/` read by op 39.
  Op 39 hands over RGB only: a texture with any partial alpha needs a second sprite holding the
  alpha as grey (`<material>_alpha`, like `xmas09_snow_alpha`) wired to the texture's `opacity`
  output, so allocate two graphic ids per imported texture. RS3 texture PNGs are left-right
  mirrors of 727 sprites (`roof` = graphic 453 once flipped); RS3 `TEXCOORD_0` are half-floats in
  ushorts; material repeat S/T come from RS3 `packed` bits 0x1/0x8, brightness from `unknown31`
  and alpha from `unknown32` as signed bytes, effectId/effectParam1 from `unknown23`/`unknown24`.
- **The RS3 face colour on a textured face is faithful even when it looks wrong in the tools
  viewer.** The viewer multiplies it in; the 727 client (`ModelSM.getMaterialColor`) mixes it
  towards grey by `material.alpha & 0xff`, so under an alpha -1 material only its lightness shows
  (a green ring in `tools model` is golden in game). Shipped 727 models do the same (model 45771).
- **Emitter faces are real faces and 727 usually hides them with alpha 255**; a converted emitter
  is only visible when the RS3 source drew it (`byteStream` 255 -> alpha 0). Carry the source.
- **Prove imported records against the 727 client's reader, not only the server decoder**: the
  server decoders read what Jagex's tools wrote, and the client can read the same bytes
  differently (`rs3port`'s `ClientGrammar` re-reads every record with the darkan-bot grammar and
  fails the import on a desync, leftover bytes or an out-of-range table index).
- **The rs3port calibrations are the gate**, one per converted kind: `calibrate` (urn models),
  `calibrate-frames`, `calibrate-particles`, `calibrate-materials`, and the attachment pass over
  the 1,858 shared models with emitters or billboards. Rerun them after any converter change.
- **Save the verify signature before the importer writes anything** - once the tree has changed
  there is no clean baseline left to diff against. Afterwards the only lines that may move are
  the new archives and the one config archive each new id lands in (config archives hold 128
  seqs, 256 spotanims; materials and both particle kinds are single archives).
- **Look at the result from a packed build**: `tools cache build cache-source data/cache`, then
  run `texdump`/`capture-headless.sh -- model <id>` with `CACHE_PATH=./data/cache/` and an empty
  `CACHE_SOURCE_PATH`. The tools install built on the remote host needs `JAVA_HOME` pointed at
  JDK 26 (`/usr/lib/jvm/java-26-openjdk`); the desktop default java is older.

## 5. Proving it

- **Tests**: content gets none - no table, flow or per-ticket tests for plugins, dialogues,
  shops, drops or definitions. Content is proven by the cache read-back and by the user in the
  client (§6). The fast suite (`world/src/test`, `WorldHarness`) is for engine-tier behaviour
  only (collision, line of sight, packet dispatch, containers), in a class named for the engine
  part it covers, and such a test must fail against the broken code before the fix.
  `ChunkManager.getChunk(tile.getChunkId())` requests a region: its collision is fully blocked at
  once and the map, base locs and clipping land a tick later off the world thread, so a test
  follows it with `WorldHarness.tickUntil { ... }` (`LineOfSightTest`); instance copies likewise
  complete their future on a later tick. **No test in this repo is "known flaky".** An intermittent failure is a bug
  in the code or the harness and is fixed, never re-run until green or annotated away - the
  `SocialGatewayTest` presence failures were `RecordingSession` calling `Job.cancel()`, which
  returns while the writer coroutine is still draining the outbound channel, so whatever it
  swallowed never reached the recorder.
- **Verify signature**: `cache verify` already differs from `../cache` (authored graphics,
  scripts, obj 25358); save its per-index lines before you start and diff after - the only lines
  that may change are the archives your ids land in.
- **Remote host**: `/tmp` there is volatile; after a reboot recreate `.env`
  (`CACHE_PATH=/tmp/darkan-build/cache/`, no `CACHE_SOURCE_PATH`), rsync `data/cache/` and
  `data/` (minus cache) up, then build.

## 6. Handing it to the user

Start the lobby jar, then the world jar (`docs`/`local-e2e-loop`), restart both after every
pack, and launch the client on the user's display with **no** `-Dautologin` (a saved login plus
the flag makes the world reject the token: "LoginToken for X presented by Y"). Say what to look
at and stop. For interface visuals use the tools interface editor (`tools interface <id>`, live
source mode) - a screenshot of a rendered interface is cheap; a scripted walkthrough of the
game is not wanted.

## 7. Client rules met today

Instruction cap 200,000 per hook (2,000,000 per load script) - never search a list per row;
`cc_create` ids contiguous per layer; `opcodeOrder` whitelist; two skill guides, one source;
noted items are `null`-named objs with `notedTemplateId 799` and `noteId` both ways.

## 8. Keep this skill current

When content work teaches a rule - an API that is deprecated, a client behaviour, a format fact -
add it here in the same change, with the file that proves it.
