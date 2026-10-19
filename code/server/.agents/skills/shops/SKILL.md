---
name: shops
description: How Darkan shops are defined, wired to their shopkeeper, and identified out of the raw inv dump in data/items/shops/unkshops - the gameval evidence chain, the authentic naming convention, and the rule that an identified inv leaves the unidentified folder. Load before adding, fixing or naming any shop. Maintained as we learn.
---

# Shops

A shop is one JSON file in `data/items/shops/`, loaded by `ShopsHandler` at startup
(`Priority.FILE_IO`). The **file name is the shop key** and the only handle code has:

```json
{
  "name": "Greengrocer of Etceteria",
  "generalStore": false,
  "buyOnly": false,
  "currency": 995,
  "items": [ { "item": { "id": 1965, "amount": 0 } } ],
  "npcIds": [ 1370 ]
}
```

- `npcIds` is what makes a shop reachable: `ShopsHandler` maps each id to the key, and the global
  `NPCClickHandler` on `"Trade"` / `"Shop"` opens it. A shop with no `npcIds` is unreachable
  unless some handler calls `ShopsHandler.openShop(player, "<key>")` itself (quests, dialogues).
- `openShop` returns false **silently** when the key is missing, so a missing shop file looks
  exactly like a dead click. That is the usual cause of a "stall/NPC does not open a shop" report.
- `currency` is the item id paid in (995 coins, or tokens for reward shops). `amount` is the
  stocked quantity restock returns to - `0` is legitimate (sell-to-only stock, e.g. Etceteria).

## The unidentified dump

`data/items/shops/unkshops/` is a **raw dump of every inv in the cache**, one file per inv
(`inventory_<invId>.json`), not a list of shops. Most invs are not shops at all (bank tabs, quest
containers, interface scratch space), and the dump carries no name, no currency and no NPC. It is
a to-do pile, not data the server should be serving.

**Rule: when you identify one, it leaves the folder.** Write (or correct) the real shop file and
`rm` the `unkshops/inventory_<id>.json`. Never leave both.

## Identifying an inv

Work the evidence chain; do not guess a shop into existence:

1. **Name the inv.** `cache-source/gamevals/inv.json` names it, or leaves an `inv_<id>`
   placeholder. The RS3-949 catalog at `~/darkan/darkan-3-undercut/re-resources/gamevals/inv.json`
   is the crosswalk source and often names what ours still lists as a placeholder
   (`219 → etc_fishmonger`). A 949 name is a lead, not proof on its own - confirm it against the
   727 contents and neighbours before you rely on it.
2. **Read the real stock** from `cache-source/config/inv/<id>.json` (`ids` + `amounts`), never from
   another shop you assume is similar. That file is the authentic stock and the amounts are the
   restock targets.
3. **Find the shopkeeper.** `cache-source/gamevals/npc.json` names the NPC (`etc_veg_monger`),
   `cache-source/config_npc/<id>.json` proves it has `Trade` among its options, and
   `data/npcs/spawns/` proves it is actually spawned where the report says. All three, or the
   shop stays unreachable.
4. **Check it is not already wired.** An unkshops file whose item list matches an existing shop
   file exactly is identified by that fact alone - delete it, add nothing.
5. Stall *objects* are usually not shops in 727: the Etceteria and Varrock stalls carry only
   `Steal-from` and belong to thieving. The shop is on the NPC standing at them.

## Naming

The file key and `name` are ours to choose and they should read like the in-game shop, not like
the dump:

- Role plus place, matching the neighbours already in the folder: `greengrocer_of_miscellania`,
  `fishmonger_of_etceteria`, `nurmofs_pickaxe_shop`, `ardougne_gem_stall`.
- `name` is the title players see in the shop window, in title case: `Greengrocer of Etceteria`.
- Never `Inventory 220`, never `inventory_220`, never a gameval name as the title - the gameval is
  the evidence, not the copy.
- Twin locations get their own file each, even when the stock is identical (inv 217 and 219 are
  byte-identical and still belong to two different mongers).

## Adding or fixing a shop

1. Identify the inv (above) and write `data/items/shops/<key>.json` from its ids/amounts.
2. Put every shopkeeper id in `npcIds`; NPC-driven shops need no handler code.
3. Delete the `unkshops/` file.
4. Data only - no build is needed, but the world must restart to reload the shop files.
5. If an id in the stock is not a gameval-known obj or the NPC is unnamed, name it first; the
   gameval catalogs are the proof trail for everything content refers to.

## State of the pile

The 100 unkshops files that duplicated a shop already wired have been deleted, along with the
Etceteria/Miscellania mongers' invs (217, 219, 218, 220) once those four shops were written.
~316 remain, roughly half of them named in our own catalog and worth walking in batches; the rest
need a 949 cross-check or are not shops at all. Record what you identify here as you go.
