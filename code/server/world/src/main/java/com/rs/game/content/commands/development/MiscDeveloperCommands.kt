package com.rs.game.content.commands.development

import com.rs.Launcher
import com.rs.Settings
import com.rs.engine.command.Commands
import com.rs.engine.cutscene.ExampleCutscene
import com.rs.engine.miniquest.Miniquest
import org.darkan.core.map.pathfinder.Direction
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.content.minigames.barrows.BarrowsController
import com.rs.game.content.pets.Pet
import com.rs.game.content.randomevents.RandomEvents
import com.rs.game.content.skills.runecrafting.runespan.RunespanController
import com.rs.game.content.skills.summoning.Familiar
import com.rs.game.content.tutorialisland.TutorialIslandController
import com.rs.game.map.ChunkManager
import com.rs.game.map.instance.Instance
import com.rs.game.map.instance.InstancedChunk
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.ModelRotator
import com.rs.game.model.entity.Rotation
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions
import com.rs.game.content.bosses.instance.BossInstanceDefs
import com.rs.game.content.bosses.instance.InstanceInterface
import com.rs.game.model.entity.player.Bank
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.InstancedController
import org.darkan.core.cache.ItemDefinitions
import org.darkan.core.model.Item
import org.darkan.core.model.Rights
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Component
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.PluginScriptHost
import com.rs.plugin.kts.PluginScriptHost.Companion.loadAndExecuteScripts
import com.rs.tools.MapSearcher
import com.rs.tools.NPCDropDumper
import com.rs.utils.DropSets
import com.rs.utils.shop.ShopsHandler
import com.rs.utils.spawns.ItemSpawns
import com.rs.utils.spawns.NPCSpawns
import java.io.IOException

@ServerStartupEvent
fun loadDeveloperCommands() {
    Commands.add(Rights.DEVELOPER, "reloadplugins", "legit test meme") { p, _ ->
        try {
            loadAndExecuteScripts()
            p.sendMessage("Reloaded plugins successfully.")
        } catch (_: Throwable) {
            p.sendMessage("Error compiling plugins.")
        }
    }

    Commands.add(Rights.DEVELOPER, "dumpdrops [npcId]", "exports a drop dump file for the specified NPC") { _, args ->
        NPCDropDumper.dumpNPC(args[0])
    }

    Commands.add(Rights.DEVELOPER, "createinstance [chunkX, chunkY, width, height]", "create a test instance for getting coordinates and setting up cutscenes") { p, args ->
        p.controllerManager.startController(object : InstancedController(
            Instance.of(p.tile, args[2].toInt(), args[3].toInt(), false)
        ) {
            override fun onBuildInstance() {
                instance.copyMapAllPlanes(args[0].toInt(), args[1].toInt())
                    .thenAccept {
                        player.playCutscene {
                            instance.teleportLocal(
                                player,
                                (args[2].toInt() * 8) / 2,
                                (args[3].toInt() * 8) / 2,
                                0
                            )
                        }
                    }
            }

            override fun canMove(dir: Direction): Boolean {
                player.sendMessage("${player.tile.getXInRegion()}, ${player.tile.getYInRegion()}")
                return true
            }

            override fun onDestroyInstance() {}
        })
    }

    Commands.add(Rights.DEVELOPER, "togglejfr", "Toggles JFR for the staff webhook tick profiler") { p, _ ->
        val config = Settings.getConfig()
        config.setJFR(!config.isEnableJFR)
        p.sendMessage("JFR is now ${if (config.isEnableJFR) "enabled" else "disabled"}.")
    }

    Commands.add(Rights.DEVELOPER, "clanify", "Toggles the ability to clanify objects and npcs by examining them.") { p, _ ->
        val current = !p.nsv.getB("clanifyStuff")
        p.nsv.setB("clanifyStuff", current)
        p.sendMessage("CLANIFY: $current")
    }

    Commands.add(Rights.DEVELOPER, "allstopfaceme", "Stops all body model rotators.") { _, _ ->
        World.players.forEach { player ->
            if (player?.hasStarted() == true && !player.hasFinished()) {
                player.bodyModelRotator = null
            }
        }
        World.NPCs.forEach { npc ->
            if (npc != null && !npc.hasFinished()) {
                npc.bodyModelRotator = null
            }
        }
    }

    Commands.add(Rights.DEVELOPER, "allfaceme", "Sets body model rotators for all entities in the server.") { p, _ ->
        World.players.forEach { player ->
            if (player?.hasStarted() == true && !player.hasFinished()) {
                player.bodyModelRotator = ModelRotator().addRotator(Rotation(p).enableAll())
            }
        }
        World.NPCs.forEach { npc ->
            if (npc != null && !npc.hasFinished()) {
                npc.bodyModelRotator = ModelRotator().addRotator(Rotation(p).enableAll())
            }
        }
    }

    Commands.add(Rights.DEVELOPER, "spawnmax", "Spawns another max into the world on top of the player.") { p, _ ->
        World.spawnNPC(3373, p.tile, true, true)
    }

    Commands.add(Rights.DEVELOPER, "playcs", "Plays a cutscene using new cutscene system") { p, _ ->
        p.cutsceneManager.play(ExampleCutscene())
    }

    Commands.add(Rights.DEVELOPER, "bonusxp [amount]", "Sets your bonus XP rate.") { p, args ->
        p.bonusXpRate = args[0].toDouble()
        p.sendMessage("Your bonus XP rate is now: ${p.bonusXpRate}")
    }

    Commands.add(Rights.DEVELOPER, "forcememclean", "Forces a mem clean op") { _, _ ->
        Launcher.cleanMemory(true)
    }

    Commands.add(Rights.DEVELOPER, "tilefree", "Checks if tile is free") { p, _ ->
        for (x in -10 until 10) {
            for (y in -10 until 10) {
                if (World.floorAndWallsFree(Tile.of(p.x + x, p.y + y, p.plane), 1)) {
                    World.sendSpotAnim(
                        Tile.of(p.x + x, p.y + y, p.plane),
                        SpotAnim(2000, 0, 96)
                    )
                }
            }
        }
    }

    Commands.add(Rights.DEVELOPER, "tutisland", "Start tutorial island") { p, _ ->
        p.controllerManager.startController(TutorialIslandController())
    }

    Commands.add(Rights.DEVELOPER, "drtor [texId]", "Set equipment texture override") { p, args ->
        val texId = args[0].toInt()
        listOf(Equipment.CHEST, Equipment.LEGS, Equipment.SHIELD, Equipment.HEAD).forEach { slot ->
            p.equipment.get(slot)?.addMetaData("drTOr", texId)
        }
        p.appearance.generateAppearanceData()
    }

    Commands.add(Rights.DEVELOPER, "drcor [r, g, b]", "Set equipment color override") { p, args ->
        val colorHsl = Utils.getRSColor(args[0].toInt(), args[1].toInt(), args[2].toInt())
        listOf(Equipment.CHEST, Equipment.LEGS, Equipment.SHIELD, Equipment.HEAD).forEach { slot ->
            p.equipment.get(slot)?.addMetaData("drCOr", colorHsl)
        }
        p.appearance.generateAppearanceData()
    }

    Commands.add(Rights.DEVELOPER, "tileman", "Set to tileman mode") { p, _ ->
        p.isTileMan = true
    }

    Commands.add(Rights.DEVELOPER, "players", "Lists online players") { p, _ ->
        val firstLine = Utils.componentIdFromHash(Component.interface_275.component_275_10)
        val humans = World.players.filter { !it.isBot }
        p.packets.setIFText(Interface.interface_275, Utils.componentIdFromHash(Component.interface_275.component_275_1), "Online Players (${humans.size})")
        humans.forEachIndexed { line, player ->
            val statusColor = when (player.account.social.status) {
                0.toByte() -> "00FF00"
                1.toByte() -> "FFFF00"
                2.toByte() -> "FF0000"
                else -> "FFFFFF"
            }
            val playTime = Utils.ticksToTime(player.timePlayedThisSession.toDouble())
            p.packets.setIFText(Interface.interface_275, firstLine + line, "<shad=000000><col=$statusColor>${player.displayName}</col></shad> logged in for $playTime")
        }
        p.packets.sendRunScript(Cs2.cs2_1207, humans.size)
        p.interfaceManager.sendInterface(Interface.interface_275)
    }

    Commands.add(Rights.DEVELOPER, "names", "Sets NPCs names to something.") { _, args ->
        val name = if (Utils.concat(args) == "null") null else Utils.concat(args)
        World.NPCs.forEach { it.setPermName(name) }
    }

    Commands.add(Rights.DEVELOPER, "barrowcheat", "Loots a full barrows chest.") { p, _ ->
        if (p.controllerManager.isIn(BarrowsController::class.java)) {
            p.controllerManager.getController(BarrowsController::class.java).cheat()
        }
    }

    Commands.add(Rights.DEVELOPER, "random", "Forces a random event.") { p, _ ->
        RandomEvents.attemptSpawnRandom(p, true)
    }

    Commands.add(Rights.DEVELOPER, "freezeme [ticks]", "Freezes you for specific timeframe.") { p, args ->
        p.freeze(args[0].toInt())
    }

    Commands.add(Rights.DEVELOPER, "farm", "Force a farming tick.") { p, _ ->
        p.tickFarming()
    }

    Commands.add(Rights.DEVELOPER, "runespan", "Teleports to runespan.") { p, _ ->
        p.tele(Tile.of(3995, 6103, 1))
        p.controllerManager.startController(RunespanController())
    }

    Commands.add(Rights.DEVELOPER, "killnpcs", "Kills all npcs around the player.") { p, _ ->
        World.getNPCsInChunkRange(p.getChunkId(), 3).forEach { npc ->
            if (npc !is Familiar && npc !is Pet) {
                if (Utils.getDistance(npc.tile, p.tile) < 9 && npc.plane == p.plane) {
                    repeat(100) {
                        npc.applyHit(Hit(p, 10000, Hit.HitLook.TRUE_DAMAGE))
                    }
                }
            }
        }
    }

    Commands.add(Rights.DEVELOPER, "deathnpcs", "Kills all npcs around the player.") { p, _ ->
        World.NPCs.forEach { npc ->
            if (npc !is Familiar && npc !is Pet) {
                if (Utils.getDistance(npc.tile, p.tile) < 9 && npc.plane == p.plane) {
                    npc.sendDeath(p)
                }
            }
        }
    }

    Commands.add(Rights.DEVELOPER, "spawntestnpc", "Spawns a combat test NPC.") { p, _ ->
        val npc = World.spawnNPC(14256, p.tile, true, true)
        npc.setLoadsUpdateZones()
        npc.setPermName("Losercien (punching bag)")
        npc.hitpoints = Int.MAX_VALUE / 2
        npc.combatDefinitions.hitpoints = Int.MAX_VALUE / 2
        npc.isForceMultiArea = true
        npc.isForceMultiAttacked = true
        npc.anim(10993)
    }

    Commands.add(Rights.DEVELOPER, "dropstobank,bankdrops", "Will send all drops received from monsters directly to the bank.") { p, _ ->
        p.nsv.setB("sendingDropsToBank", true)
    }

    Commands.add(Rights.DEVELOPER, "killallstaff", "Kills all staff members.") { p, _ ->
        World.allPlayers { other ->
            if (other?.hasRights(Rights.ADMIN) == true)
                other.applyHit(Hit(null, p.hitpoints, Hit.HitLook.TRUE_DAMAGE))
        }
    }

    Commands.add(Rights.DEVELOPER, "npc [npcId]", "Spawns an NPC with specified ID.") { p, args ->
        World.spawnNPC(args[0].toInt(), p.tile, true, false)
    }

    Commands.add(Rights.DEVELOPER, "npcwithfunc [npcId]", "Spawns an NPC with specified ID with its custom functionality.") { p, args ->
        World.spawnNPC(args[0].toInt(), p.tile, true, true)
    }

    Commands.add(Rights.DEVELOPER, "addnpc [npcId]", "Spawns an NPC permanently with specified ID.") { p, args ->
        if (Settings.getConfig().isDebug) {
            if (NPCSpawns.addSpawn(p.username, args[0].toInt(), p.tile)) {
                p.sendMessage("Added spawn.")
            }
        }
    }

    Commands.add(Rights.DEVELOPER, "dropitem", "Spawns an item on the floor until it is picked up.") { p, args ->
        World.addGroundItem(Item(args[0].toInt(), 1), Tile.of(p.x, p.y, p.plane))
    }

    Commands.add(Rights.DEVELOPER, "addgrounditem,addgitem [itemId respawnTicks]", "Spawns a ground item permanently with specified ID.") { p, args ->
        if (Settings.getConfig().isDebug) {
            if (ItemSpawns.addSpawn(p.username, args[0].toInt(), 1, args[1].toInt(), p.tile)) {
                p.sendMessage("Added spawn.")
            }
        }
    }

    if (!Settings.getConfig().isDebug) {
        Commands.add(Rights.DEVELOPER, "exec [command to execute]", "Executes a command-line command on the remote server.") { p, args ->
            Launcher.executeCommand(p, Utils.concat(args))
        }
    }

    Commands.add(Rights.DEVELOPER, "shop [name]", "Opens a shop container of specified id.") { p, args ->
        ShopsHandler.openShop(p, args[0])
    }

    Commands.add(Rights.DEVELOPER, "reloadshops", "Reloads the shop data file.") { _, _ ->
        ShopsHandler.reloadShops()
    }

    Commands.add(Rights.DEVELOPER, "reloadcombat", "Reloads the NPC combat definitions files.") { _, _ ->
        NPCCombatDefinitions.reload()
        World.NPCs.forEach { npc ->
            npc?.resetLevels()
        }
    }

    Commands.add(Rights.DEVELOPER, "reloaddrops", "Reloads the drop table files.") { _, _ ->
        DropSets.reloadDrops()
    }

    Commands.add(Rights.DEVELOPER, "loginmessage,loginmes [announcement]", "Sets the server login announcement.") { p, args ->
        val config = Settings.getConfig()
        config.loginMessage = "<shad=000000><col=ff0000>${Utils.concat(args)}"
        try {
            Settings.saveConfig()
        } catch (_: IOException) {
            p.sendMessage("Failed to save config")
        }
        p.sendMessage(config.loginMessage)
    }

    Commands.add(Rights.DEVELOPER, "coords,getpos,mypos,pos,loc", "Gets the coordinates for the tile.") { p, _ ->
        p.sendMessage("Coords: ${p.x},${p.y},${p.plane}, regionId: ${p.getRegionId()}, chunkX: ${p.chunkX}, chunkY: ${p.chunkY}, hash: ${p.getTileHash()}")
        p.sendMessage("ChunkId: ${p.getChunkId()}")

        val chunk = ChunkManager.getChunk(p.getChunkId())
        if (chunk is InstancedChunk) {
            p.sendMessage("In instanced chunk copied from: ${chunk.originalBaseX}, ${chunk.originalBaseY} rotation: ${chunk.rotation}")
        }

        p.sendMessage("JagCoords: ${p.plane},${p.regionX},${p.regionY},${p.getXInRegion()},${p.getYInRegion()}")
        p.sendMessage("Local coords: ${p.getXInRegion()}, ${p.getYInRegion()}")
        p.sendMessage("16x16: ${p.getXInScene(p.sceneBaseChunkId) % 16}, ${p.getYInScene(p.sceneBaseChunkId) % 16}")
    }

    Commands.add(Rights.DEVELOPER, "resetquest [questName]", "Resets the specified quest.") { p, args ->
        val questName = args[0]

        Quest.entries.firstOrNull { it.name.contains(questName, ignoreCase = true) && it.isImplemented }?.let { quest ->
            p.questManager.resetQuest(quest)
            p.sendMessage("Reset quest: ${quest.name}")
            return@add
        }

        Miniquest.entries.firstOrNull { it.name.contains(questName, ignoreCase = true) && it.isImplemented }?.let { miniquest ->
            p.miniquestManager.reset(miniquest)
            p.sendMessage("Reset miniquest: ${miniquest.name}")
        }
    }

    Commands.add(Rights.DEVELOPER, "completequest [questName]", "Completes the specified quest.") { p, args ->
        val questName = args[0]

        Quest.entries.firstOrNull { it.name.contains(questName, ignoreCase = true) }?.let { quest ->
            p.questManager.completeQuest(quest)
            p.sendMessage("Completed quest: ${quest.name}")
            return@add
        }

        Miniquest.entries.firstOrNull { it.name.contains(questName, ignoreCase = true) }?.let { miniquest ->
            p.miniquestManager.complete(miniquest)
            p.sendMessage("Completed miniquest: ${miniquest.name}")
        }
    }

    Commands.add(Rights.DEVELOPER, "completequestother [player_name questName]", "Completes the specified quest for an online player.") { p, args ->
        val target = World.getPlayerByDisplay(args[0]) ?: return@add p.sendMessage("Couldn't find player ${args[0]}.")
        val questName = args[1]

        Quest.entries.firstOrNull { it.name.contains(questName, ignoreCase = true) }?.let { quest ->
            target.questManager.completeQuest(quest)
            p.sendMessage("Completed quest ${quest.name} for ${target.displayName}.")
            return@add
        }

        Miniquest.entries.firstOrNull { it.name.contains(questName, ignoreCase = true) }?.let { miniquest ->
            target.miniquestManager.complete(miniquest)
            p.sendMessage("Completed miniquest ${miniquest.name} for ${target.displayName}.")
            return@add
        }

        p.sendMessage("No quest or miniquest matches $questName.")
    }

    Commands.add(Rights.DEVELOPER, "completeallquests", "Completes all quests.") { p, _ ->
        Quest.entries.filter { it.isImplemented }.forEach { quest ->
            p.questManager.completeQuest(quest)
            p.sendMessage("Completed quest: ${quest.name}")
        }

        Miniquest.entries.filter { it.isImplemented }.forEach { miniquest ->
            p.miniquestManager.complete(miniquest)
            p.sendMessage("Completed miniquest: ${miniquest.name}")
        }
    }

    Commands.add(Rights.DEVELOPER, "resetallquests", "Resets all quests.") { p, _ ->
        Quest.entries.filter { it.isImplemented }.forEach { quest ->
            p.questManager.resetQuest(quest)
            p.sendMessage("Reset quest: ${quest.name}")
        }

        Miniquest.entries.filter { it.isImplemented }.forEach { miniquest ->
            p.miniquestManager.reset(miniquest)
            p.sendMessage("Reset miniquest: ${miniquest.name}")
        }
    }

    Commands.add(Rights.DEVELOPER, "searchobj,so [objectId index]", "Searches the entire gameworld for an object matching the ID and teleports you to it.") { p, args ->
        val objectId = args[0].toInt()
        val objs = MapSearcher.getObjectsById(objectId)

        if (objs.isEmpty()) {
            p.sendMessage("Nothing found for $objectId")
            return@add
        }

        objs.forEachIndexed { i, obj ->
            p.packets.sendDevConsoleMessage("$i: $obj")
        }

        val index = if (args.size == 1) 0 else args[1].toInt()
        p.tele(objs[index].tile)
    }

    Commands.add(Rights.DEVELOPER, "searchnpc,sn [npcId index]", "Searches the entire (loaded) gameworld for an NPC matching the ID and teleports you to it.") { p, args ->
        val npcId = args[0].toInt()
        val npcs = NPCSpawns.getAllSpawns().filter { it.npcId == npcId }

        npcs.forEachIndexed { i, npc ->
            p.packets.sendDevConsoleMessage("$i: $npc")
        }

        val index = if (args.size == 1) 0 else args[1].toInt()
        p.tele(npcs[index].tile)
    }

    Commands.add(Rights.DEVELOPER, "getip [player name]", "Gets the player's IP addresses.") { p, args ->
        val playerName = Utils.concat(args)
        World.forceGetPlayerByDisplay(playerName) { target ->
            if (target == null) {
                p.sendMessage("Couldn't find player.")
            } else {
                p.sendMessage("<col=ff0000>IP addresses for $playerName")
                target.ipAddresses.forEach { ip ->
                    p.sendMessage("<col=ff0000>$ip")
                }
            }
        }
    }

    Commands.add(Rights.DEVELOPER, "fillbank (slots)", "Fills the bank with random items, up to the bank cap by default.") { p, args ->
        val target = (args.getOrNull(0)?.toIntOrNull() ?: Bank.MAX_BANK_SIZE).coerceIn(0, Bank.MAX_BANK_SIZE)
        val bank = p.bank
        var added = 0
        for (id in (0 until Utils.getItemDefinitionsSize()).shuffled()) {
            if (bank.bankSize >= target) break
            val defs = ItemDefinitions.getDefs(id)
            if (defs.noted || defs.lent || defs.name == "null") continue
            if (bank.addItem(Item(id, Utils.randomInclusive(1, 100000)), false)) added++
        }
        bank.refreshTabs()
        bank.refreshItems()
        p.sendMessage("Bank filled to ${bank.bankSize} slots, $added added.")
    }

    Commands.add(Rights.DEVELOPER, "instance (boss key)", "Opens the boss instance window, or lists the keys.") { p, args ->
        val key = args.getOrNull(0)
        val def = key?.let { BossInstanceDefs[it] }
        if (def == null) p.sendMessage("Bosses: " + BossInstanceDefs.all.joinToString(", ") { it.key })
        else InstanceInterface.open(p, def)
    }

    Commands.add(Rights.DEVELOPER, "portrait [zoom] (pitch) (roll)", "Re-frames the open instance window's boss model so values can be tuned live.") { p, args ->
        val zoom = args[0].toInt()
        val pitch = args.getOrNull(1)?.toIntOrNull() ?: 0
        val roll = args.getOrNull(2)?.toIntOrNull() ?: 0
        InstanceInterface.applyPortrait(p, zoom, pitch, roll)
        p.sendMessage("portraitZoom = $zoom, pitch = $pitch, roll = $roll")
    }

    Commands.add(Rights.DEVELOPER, "locsat (radius)", "Lists the locs around you with their ids, names and options.") { p, args ->
        val radius = args.getOrNull(0)?.toIntOrNull() ?: 1
        val seen = HashSet<Int>()
        World.getChunkRadius(p.tile.getChunkId(), radius)
            .mapNotNull { ChunkManager.getChunk(it) }
            .flatMap { it.baseObjects + it.spawnedObjects }
            .filter { seen.add(it.id) }
            .sortedBy { Utils.getDistance(p.tile, it.tile) }
            .take(30)
            .forEach { obj ->
                val options = obj.definitions.options?.filterNotNull()?.filter { it.isNotBlank() && it != "null" } ?: emptyList()
                p.packets.sendDevConsoleMessage("${obj.id} ${obj.definitions.name} @ ${obj.tile.x},${obj.tile.y},${obj.tile.plane} [${options.joinToString(", ")}]")
            }
        p.sendMessage("Listed ${seen.size} locs within $radius chunk(s) - see the dev console.")
    }
}
