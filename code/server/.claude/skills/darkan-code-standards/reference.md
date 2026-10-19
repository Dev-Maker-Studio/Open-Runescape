# Worked example: bank placeholders

Commit `7659d448` ("feat: bank placeholders") is the reference for what the rules forbid; the
revision that replaced it is the reference for what they require.

| Rule | What the commit did | What it should have done |
|---|---|---|
| Comments | A KDoc/javadoc block on every new method and a paragraph comment on most branches explaining legacy behaviour | No comments; `removeItem`'s one non-obvious clamp gets a single line at most |
| Kotlin | 300 new lines in `Bank.java`, `Player.java`, `Skills.java` | `Bank.kt`: same Gson field names (`bankTabs`, `bankPin`, `lastX`, `enteredPin`), `@Transient` session state, hooks registered from a top-level `@ServerStartupEvent` function with the `com.rs.plugin.kts` DSL (`kotlin-plugins` skill) |
| Placement | Skillcape trimming loop in `Skills.trimCapes`; dung/house-item bank sweep duplicated in `Player` | `Skillcapes.trimCapes(player)` next to the cape table; `Bank.convertToCoins(predicate)` called from both sweeps |
| Gamevals | `762`, `124`, `126`, `1431`, `1433`, `95`, `995`, `1249`, `4885` literals | `Interface.interface_762`, `Component.interface_762.placeholder_toggle_button`, `Graphic.bank_buttons_new2_0`, `Inv.bank`, `Obj.coins`, `Varp.varp_1249`, `Varbit.varbit_4885` |
| Persistence | `private boolean placeholders` on the Gson-saved `Bank`, painted with `setIFGraphic(762, 124, graphic)` from the server on open and toggle | Varbit `bank_placeholders` (`config_varbit/11713.json`: varp 1248, bit 20, the first free bit above the tab-size varbits 4891/4892 and below 4893/4894), `player.vars.saveVarBit(Varbit.bank_placeholders, 1)`; component 762:124 gets `onLoadScript` + `onVarpTransmit` (`varps: [1248]`) running `[clientscript,bank_placeholder_button]`, whose proc reads `varbit_bank_placeholders` and sets the frame graphic, op text and tooltip exactly as `proc_bank_cert_button` does for varp 115 |

## Choosing the bit

```
varp 1248 (bank tab sizes / current tab)
  4891  bits  0-9   tab 7 size
  4892  bits 10-19  tab 8 size
  ----  bits 20-26  free            <- 11713 bank_placeholders takes bit 20
  4893  bits 27-30  current tab
  4894  bit  31
```

Varp 638 (`swansong_temp`) also holds bank varbits but `Bank.open()` resets it with
`setVar(Varp.swansong_temp, 0)`, so a saved toggle there would be wiped on every open.

## The client side of a toggle

```
interfaces/762/124.json         onLoadScript + onVarpTransmit -> 6567, varps [1248]
clientscripts/clientscript/bank_placeholder_button.ts   calls the proc
clientscripts/proc/bank_placeholder_button.ts           reads varbit_bank_placeholders, ifSetGraphic/ifSetOp/tooltip
clientscripts/index.json        archives 6567/6568 with the name hashes of "[clientscript,...]" / "[proc,...]"
clientscripts/vars.d.ts         declare let varbit_bank_placeholders
clientscripts/gamevals.d.ts     the component constants the proc references
gamevals/clientscript.json      the two names, coverage bumped
gamevals/varbit.json            "11713": "bank_placeholders", coverage bumped
```
