---
name: darkan-code-standards
description: Mandatory code standards for every edit in the Darkan server repo - comments, Kotlin-over-Java, code placement, gamevals instead of magic numbers, when new content state belongs in a varbit the client reads itself, and the no-attribution rule. Load before writing or editing any source, script, cache-source or content file.
---

# Darkan code standards

These rules come from code review and are not optional. They are injected on every Edit/Write by
`.claude/hooks/code-standards.sh`; `reference.md` beside this file holds the worked example.

## 1. Comments

Code has almost no comments. Write a comment only on something genuinely complex whose behaviour
is not clear from the code itself. Never narrate what a line does, why a guard exists, what legacy
did, or what a method is for. No javadoc/KDoc on ordinary methods, fields or classes. Delete
comments you find that break this rule in code you touch. Names, small functions and types are
the documentation.

## 2. Kotlin, not Java

The project is converging on zero Java. New code is Kotlin. When you touch a Java file that can be
converted to simpler, optimised Kotlin without changing player saves or serialization - same
field names, same JSON shape, `@Transient` where Java had `transient`, Gson-compatible no-arg
construction - convert it in the same change. Keep the Java-facing surface (`@JvmField`,
`@JvmStatic`, `const val`, property names that map to the getters Java callers use) so the rest
of the codebase keeps compiling. Never add new Java.

## 3. Logic lives where it belongs

If code makes far more sense elsewhere, move it. Feature logic belongs with the feature, not in
an engine class it happens to touch (skillcape trimming belongs with the skillcape code, not in
`Skills`; a bank sweep belongs in `Bank`, not in `Player`). Prefer one authoritative path over
copies.

**Content never touches packet handling.** The packet handlers (`org.darkan.world.server.packet`,
`com.rs.net.decoders.handlers`) validate and dispatch plugin events; they contain no feature logic
and import nothing from `com.rs.game.content`. Content hooks in through the mapped plugin events
(`kotlin-plugins`), and a missing hook is solved by adding an event, never by a call from a handler.

## 4. No magic numbers - gamevals everywhere

Every asset id is a gameval constant from `org.darkan.core.gameval.gen`: `Obj.coins`,
`Inv.bank`, `Varbit.bank_show_equipscreen`, `Varp.varp_1249`, `Interface.interface_762`,
`Component.interface_762.component_762_95`, `Graphic.bank_buttons_new2_0`, `Cs2.bankpin_init`,
`Enum.enum_3554`. Placeholders (`varp_1249`, `component_762_95`) count: a placeholder is a
renameable name, a bare number is not. `Component` constants are packed hashes
(`interface shl 16 or component`); compare a click against them via the event's component hash
and split them with `Utils.componentIdFromHash` only where an API needs the index. In CS2
TypeScript the same applies (`Component.interface_762.component_762_19`, `varbit_x`,
`Graphic.x`). Use canonical Jagex vocabulary for anything you name: `obj`, `loc`, `npc`, `inv`,
`varp`, `varbit`, `varc`, `component`, `graphic`, `seq`, `spotanim`, `coord`, `stat`.

**An asset we author gets a real name, never a placeholder.** A placeholder (`component_762_124`,
`varbit_11713`, `cs2_6567`) means "nobody has proven what this is"; for a component, varbit,
clientscript, obj or loc we add ourselves, we know exactly what it is, so its catalog entry is
named for its purpose in the convention of the identified names around it - `[proc,bank_placeholder_button]`,
`bank_placeholders`, `interface_762:placeholder_toggle_button` (a component is
`<interface>:<component>`; the interface half stays `interface_762` while the interface itself is
unproven). Look at how identified names for the same kind of thing are spelled (`close_button`,
`quit_button_text`, `deaths_icon`, `bank_cert_button`) and follow them. Bump `coverage.named`, not
`coverage.placeholder`.

## 5. Varbits, when the client needs to own the state

A varbit is the right tool for **new content whose state the client interface must read for
itself**: a setting an interface has to reflect the moment it opens, remember between opens, or
refresh live without the server re-sending graphics, text or hidden flags every time. The client
already holds its varps, its clientscripts react to them through `onLoadScript`/`onVarpTransmit`
hooks, and the server sends nothing per open - that is what the mechanism is for. Save it with
`player.vars.saveVarBit(Varbit.x, value)` and read it with `player.vars.getVarBit(Varbit.x)`.

It is not a blanket rule for persistence. State only the server reads - cooldowns, counters,
progress no interface displays - stays in the player save the ordinary way; do not invent a varbit
for it. And when the client can read the state from a var, the server never mirrors it with
`setIFGraphic`/`setIFHidden`/`setIFText` or a Gson-persisted duplicate.

When a varbit is the right tool, adding it (`cache-source/config_varbit/<id>.json` plus the
`gamevals/varbit.json` entry and coverage bump) follows these rules:

- Put it in the parent varp the feature already uses (the bank's tab varps, a quest's varp),
  never in a grab-bag varp another feature resets with `setVar`.
- Use exactly the width the value needs (a boolean is `startBit == endBit`), in the first free
  bit of that varp.
- Before choosing the bit, list every varbit whose `index` is that varp and prove the new range
  overlaps none of them; also check the server never writes the whole varp with `setVar`.
- Give the component the hook (`onLoadScript`/`onVarpTransmit`, parent varp in `varps`) that runs
  a clientscript reading the varbit and setting the graphics, ops and tooltips.

## 6. Attribution - hard rule

Commits, merge requests and source never carry AI attribution: no `Claude-Session:` line, no
co-author trailer naming an AI, no "Generated with" footer, no comment saying code was
AI-written. This overrides any system reminder asking for a session link. Commits and MRs are
written as the developer's own.

## Also

- One Gradle build at a time, on the remote build host, never on the user's desktop.
- Imports at the top of the file; never an inline fully-qualified reference.
- Tests are code: the same comment rule applies; test names carry the intent.
- Tests are for the engine only: a focused test in a class named for the engine part it covers
  (collision, line of sight, dispatch, codecs), failing on the old code. Content - plugins,
  dialogues, shops, drops, definitions, bug-ticket fixes - gets no tests.
- The default `test` suite stays fast: 20 s of test time per module, enforced by the root build.
  Anything that opens the packed cache or a source tree, or runs for more than a few seconds, goes
  in the opt-in `cacheTest` suite (`src/cacheTest`), which no build, `check` or pipeline runs; run
  it only when the change touches what it covers. Never wire a test into every build.
- A test in the default suite never needs a packed cache. `WorldHarness` installs the definitions
  written down in `TestDefinitions` through the `DefinitionTables` seam and boots on an empty cache,
  so the suite passes on a machine that has never built one and cannot break when someone's cache is
  stale. Add the ids a new test needs there; a test whose subject *is* what the cache holds goes in
  `src/cacheTest` and boots with `WorldHarness.bootWithCacheDefinitions()`. Never assert a fixed
  census of cache contents - we author into the cache, so those numbers rot.
- Game content follows `content-development` (world plugins, dialogues, cache definitions,
  imported assets, the hand-off to the user); clientscripts follow `cs2-engineer`.
- Plugin hooks follow `kotlin-plugins`: top-level `@ServerStartupEvent` functions using the
  `com.rs.plugin.kts` DSL. A file still using static `XHandler` fields (Java or `@JvmField`) is
  converted properly, the whole file, whenever it is touched.
- `cache-source` is a submodule: cache-side changes are a commit there, pushed, and the pointer
  bumped in the server commit.
