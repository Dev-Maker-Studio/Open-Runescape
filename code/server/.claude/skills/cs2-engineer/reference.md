# Worked examples

## Bank placeholder button (varbit-driven, 2026-09-04)

Goal: the bank's placeholder toggle reflects a per-player setting the client owns.

```
config_varbit/11713.json                                bank_placeholders: varp 1248 bit 20 (first free bit)
gamevals/varbit.json                                    "11713": "bank_placeholders", coverage named +1
clientscripts/vars.d.ts                                 declare let varbit_bank_placeholders: number;
clientscripts/clientscript/bank_placeholder_button.ts   [clientscript,...] -> calls the proc
clientscripts/proc/bank_placeholder_button.ts           reads the varbit, ifSetGraphic / ifSetOp / tooltip hook
clientscripts/index.json                                6567, 6568 with nameHash of "[clientscript,...]" / "[proc,...]"
gamevals/clientscript.json                              the two names, coverage cache_ids +2, named +2
interfaces/762/124.json                                 onLoadScript + onVarpTransmit (varps [1248]) -> the clientscript
interfaces/762/125.json, 127.json                       icon components: defaultImage, size = graphic canvas, offset to centre
gamevals/component.json                                 762:124..127 named interface_762:placeholder_*
```

Server: `player.vars.saveVarBit(Varbit.bank_placeholders, 1)`; nothing else is sent per open.

Icons: `symbol_lock` (4346, 22x22) on the toggle; `symbol_lock_open` (12071, 22x26) drawn from its
pixels with the shackle lifted and the raised leg capped, bottom-aligned so the bodies match.

## World list bot icon (flag-driven, 2026-09-04)

Goal: rows whose flags carry bit 2 show a bot icon with a tooltip, in the list and in favourites.

```
graphics/12072/0.png                                     world_select_bots, 16x16 robot head
graphics/index.json                                      "12072": {"compression": "gzip", "nameHash": -1385813054}
gamevals/graphic.json + clientscripts/gamevals.d.ts     name + Graphic declaration
clientscripts/proc/lobby_worldswitcher_bots_icon.ts     (comp, id, x, y, xmode, flags): cc_add_graphic, ccSetPosition,
                                                        hide when testBit(flags, 2) == 0, else enter/exit tooltip hooks
clientscripts/clientscript/lobby_worldswitcher_timer.ts flag graphic at id 2n, bot icon at 2n+1 in column 910:73
clientscripts/cs2/cs2_3118.ts                           favourite rows: id 15 at ifGetX(910:74) - 24
clientscripts/cs2/cs2_3152.ts                           tooltip positioner subtracts ifGetScrollX/Y of every layer walked
```

First attempt used id `1000 + row` and hung the lobby with `RuntimeException:999`: cc ids must be
contiguous (see rule 3). The fix creates the icon for every row and hides it when unflagged.

Row geometry (interface 910, layer 63, rows 20 high): 68 favourite star (23w), 69/70 world number
and members star (x25, 53w), 71 players (x80, 73w), 72/73 country name and flag (x154, fills),
74 Type (right 94, 63w), 75 lootshare (right 52, 40w), 76 ping (right 0, 50w), 77/78 hover overlays.

## 2000-slot bank (capacity + instruction budget, 2026-09-21)

Goal: raise the bank from 800 to 2000 slots without the client aborting the redraw or stalling.

```
config/inv/95.json                        length 800 -> 2000 (invSize(Inv.bank) everywhere in script)
config/varp/2715..2718.json               bank_tab_sizes_1..4, + gamevals/varp.json and coverage
config_varbit/4885..4892.json             the eight tab sizes rehomed to 11 bits, two per new varp
interfaces/762/95.json                    varps [1246,1247,1248] -> [1248, 2715..2718]
clientscripts/cs2/cs2_1450.ts             load: cc_create + every static per-slot setter
clientscripts/cs2/cs2_1453.ts             redraw: obj, transparency, op base, ops that actually differ
clientscripts/cs2/cs2_1456/1457/1479.ts   each view draws what it shows and hides the rest itself
clientscripts/cs2/cs2_1467.ts             the all-items range, was a literal [0, 800]
clientscripts/cs2/cs2_1248/1329/1465/1665 free vs member slot arithmetic, was literal 800 and 713
world/.../player/Bank.kt                  MAX_BANK_SIZE 2000; depositAllInventory refreshes once
```

Three things the old code could not survive at 2000, all found by counting the bytecode:

1. **The tab-size varbits were 10 bits** (max 1023), so one tab holding more than 1023 items wrapped.
   They are ids the scripts name, and the client resolves id -> varp/bits from the cache, so
   rehoming them to wider bits needs no script edit - only the component's `varps` transmit list.
   The server writes them with `setVarBit` (transient, never saved), so no save migration.
2. **The redraw was ~114 instructions per filled slot** (`cs2_1664`'s loop plus `cs2_1453`), i.e.
   228,000 for a full bank - past the 200,000 cap, which is exactly "the interface stops rendering
   part way down". The split is: everything that never changes for a slot (size, outline, shadow,
   drag, the constant ops, the hover hooks) is set once in the create loop; the redraw sets only
   obj, transparency, op base and the ops that actually differ, guarded by
   `stringLength(ccGetOp(9)) == 0` so the placeholder/item op swap costs nothing in the common case.
   Load is now ~68/slot (136k) and the redraw ~67-69/slot (134-138k), each in its own budget.
3. **`cs2_1456` broke out of a tab's range with `int0 = 1000`**, a sentinel that is no longer past
   the end at 2000 slots. The views hide the empty tail instead, which also removed the separate
   `cs2_1455` full-hide pass each redraw paid for.

Still client-side, not fixed here: `ItemContainer.setItemInContainer` in darkan-bot grows the
container one slot at a time, so a full 2000-slot UPDATE_INV_FULL costs O(n^2) array copies on
every bank open.
