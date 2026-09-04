# CS2 — ClientScript 2 engine

The client-side scripting VM. Nearly all interface behaviour in the #727 client
is CS2: opening a panel, filling the quest journal, reacting to a varp change,
driving the world map, applying a graphics setting.

## Layout

```
cs2/
  ClientScriptOpCode.kt   the opcode table: 1006 constants, opcode id +
                          operand-width flag + dispatch family
  CS2Script.kt            decodes a compiled script out of the cache
  CS2Executor.kt          VM state (stacks, locals, call frames) and the
                          fetch-decode-execute loop
  CS2CallFrame.kt         one saved call frame
  CS2Interface.kt         the "active component" pair CC_* opcodes operate on
  CS2QueryResults.kt      result set shared by the *_FIND / *_FINDNEXT family
  CS2Interpreter.kt       interpreter state + entry point
  ops/
    CS2Dispatch.kt        two-level opcode router
    *Ops.kt               the opcode handlers, grouped by opcode family
```

## Three things that are easy to get wrong

### 1. Opcode numbers are scrambled; declaration order is not

The `opcode` values are a dense but **scrambled** permutation of `0..1005`
(`PUSH_CONSTANT_INT` is 79, not 0). Darkan remapped them.

The enum's **declaration order**, however, is ascending *original #727 opcode*
order. That is provable: `hasIntConstant` is true for exactly declaration
indices `0..53` minus `{13, 23, 24}`, which is precisely the canonical rule
*"original opcode < 100 takes a 4-byte operand, except return /
pop_int_discard / pop_string_discard"*.

That ordering is the anchor for identifying the opcodes that are still unnamed —
and nothing in the language protects it, because `getByOpcode` is a `HashMap`.
Reordering the enum (alphabetising it, say) would compile and run fine while
destroying the evidence base. `tools/cs2/verify.py` pins it.

### 2. `hasIntConstant` decides operand width at decode time

`CS2Script.decodeInstruction` reads `readInt()` when the flag is set and
`readUByte()` otherwise. One wrong flag desynchronises the entire instruction
stream for every script. `verify.py` asserts the rule above holds with no
exceptions.

### 3. The reference name table is RS3-era, not #727

`~/darkan/world-server/developer-information/CS2InstructionType.java` is a
1256-constant table from a much later revision. Use it as a **nomenclature
dictionary only** — its ordinals do not map to #727 (its opcode 3 is
`PUSH_VARBIT`; #727's is push-string). The files that *do* match this build are
`usedinstructions.txt` and `unusedinstructions.txt` in the same directory, which
also record which opcodes are actually reachable in the #727 cache.

## Why dispatch is split across per-family objects

A single 1006-branch `when` compiles to roughly **14,000 bytecodes**. HotSpot's
`HugeMethodLimit` is 8000 and `DontCompileHugeMethods` is on by default, so such
a method is *never JIT-compiled* — the hottest dispatch in the client would run
in the bytecode interpreter forever.

`CS2Dispatch` therefore routes on `ClientScriptOpCode.familyId` to one of 17
per-family `dispatch` functions, the largest of which is ~1,350 bytecodes. All
of them compile.

Two things to preserve if you touch this:

- Switch on the **`Int`** family id, not an enum. `when` over an enum adds a
  `WhenMappings` array lookup plus an `ordinal()` call; an `Int` compiles
  straight to a `tableswitch`.
- Do **not** put the family dispatchers behind an interface. That turns a
  monomorphic `invokestatic` into a 17-way megamorphic `invokeinterface` and
  gives the win back.

`familyId` is generated from `tools/cs2/families.tsv`; don't hand-edit it.

## Naming

Opcode constants use official Jagex nomenclature, adopted verbatim from the
reference table where one exists — including British spellings (`CC_SETCOLOUR`)
and names that are less pretty than the alternatives (`GOSUB_WITH_PARAMS`
rather than `CALL_CS2`). The point is that the enum greps identically against
every other CS2 tool and cache dump in the ecosystem.

Handler functions are the lowerCamelCase form of their opcode name, prefix order
preserved: `CC_SETTEXT` → `ccSetText`. A handful of opcodes cannot follow this
(Kotlin keywords, stdlib infix functions) and are listed with reasons in
`tools/cs2/handler_overrides.tsv`.

Opcodes that are still `instrNNNN` have not been identified. `NNNN` is the
original Jagex client method number and is the join key to the reference dumps —
that is why they keep the name rather than getting a plausible-sounding guess.

## Tooling

`tools/cs2/` holds the audit harness: invariant checker, opcode manifest with
per-opcode provenance, identification techniques, and the scripted renames.
Run `python3 tools/cs2/verify.py` after any change here. See
`tools/cs2/README.md`.

## Debugging

`cs2trace` in the in-game console logs every opcode as it executes;
`cs2trace <name>` filters to scripts whose name contains that substring, and
`cs2trace off` stops. Capturing a trace before and after a change and diffing
the sequences is the strongest end-to-end check available, since there are no
tests.
