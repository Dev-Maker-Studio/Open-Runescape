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

Nothing in the language protects it, because `getByOpcode` is a `HashMap`:
reordering the enum (alphabetising it, say) would compile and run fine while
breaking that rule. Never reorder it.

### 2. `hasIntConstant` decides operand width at decode time

`CS2Script.decodeInstruction` reads `readInt()` when the flag is set and
`readUByte()` otherwise. One wrong flag desynchronises the entire instruction
stream for every script.

### 3. Opcode names are the server's

Every constant name equals the darkanrs/server `Cs2Op` enum, which holds Jagex's
727 names, checked against
<https://raw.githubusercontent.com/zwyz/rs3-cache/refs/heads/master/data/opcodes-727-309.txt>.
Rename an opcode in both together. Never take a name from
`developer-information/CS2InstructionType.java`: it is an RS3-era table whose
names do not map to #727.

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

An opcode's `familyId` is the family whose `*Ops.kt` holds its handler.

## Naming

Handler functions are the lowerCamelCase form of their opcode name, prefix order
preserved: `CC_SETTEXT` → `ccSetText`. The few that cannot follow this (Kotlin
keywords, stdlib infix functions) keep a descriptive name.

## Debugging

`cs2trace` in the in-game console logs every opcode as it executes;
`cs2trace <name>` filters to scripts whose name contains that substring, and
`cs2trace off` stops. Capturing a trace before and after a change and diffing
the sequences is the strongest end-to-end check available, since there are no
tests.
