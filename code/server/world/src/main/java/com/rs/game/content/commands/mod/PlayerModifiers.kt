package com.rs.game.content.commands.mod

import com.rs.engine.command.Commands
import com.rs.game.World
import com.rs.plugin.annotations.ServerStartupEvent
import org.darkan.core.Utils
import org.darkan.core.model.Rights
import org.darkan.core.social.gateway.AccountAction
import org.darkan.world.server.WorldServer

@ServerStartupEvent
fun mapModCommands() {
    Commands.add(Rights.MOD, "checkbank [player name]", "Displays the contents of another player's bank.") { p, args ->
        if (args.isEmpty()) {
            p.bank.openBankOther(p)
            return@add
        }
        World.forceGetPlayerByDisplay(Utils.concat(args)) { target ->
            if (target == null) p.sendMessage("Could not find player ${Utils.concat(args)}.")
            else p.bank.openBankOther(target)
        }
    }

    Commands.add(Rights.MOD, "kick [player name]", "Kicks a player from the game. Will not kick through combat.") { p, args ->
        val target = World.getPlayerByDisplay(Utils.concat(args))
        if (target == null) {
            p.sendMessage("${Utils.formatPlayerNameForDisplay(Utils.concat(args))} is not logged in.")
            return@add
        }
        p.sendMessage("Successfully kicked ${Utils.concat(args)}.")
        target.session.close()
    }

    Commands.add(Rights.MOD, "ban [player name]", "Bans a player for 2 days.") { p, args ->
        World.forceGetPlayerByDisplay(Utils.concat(args)) { target ->
            if (target == null) {
                p.sendMessage("Unable to find player.")
                return@forceGetPlayerByDisplay
            }
            val until = AccountAction.days(2)
            target.ban(until)
            p.sendMessage("You have banned ${Utils.formatPlayerNameForDisplay(Utils.concat(args))} for 2 days.")
            WorldServer.accountAction(p, AccountAction.Ban(target.username, until))
            if (target.hasStarted())
                target.session.close()
        }
    }

    Commands.add(Rights.MOD, "teleto", "Teleports the user to another player as long as they aren't in a controller or locked.") { p, args ->
        if (p.isLocked || p.controllerManager.controller != null) {
            p.sendMessage("You cannot tele anywhere from here.")
            return@add
        }
        val target = World.getPlayerByDisplay(Utils.concat(args))
        when {
            target == null -> p.sendMessage("Couldn't find player.")
            target.isLocked || target.controllerManager.controller != null -> p.sendMessage("You cannot teleport this player.")
            else -> p.tele(target.tile)
        }
    }

    Commands.add(Rights.MOD, "teletome", "Teleports another player to the user as long as they aren't in a controller or locked.") { p, args ->
        val target = World.getPlayerByDisplay(Utils.concat(args))
        when {
            target == null -> p.sendMessage("Couldn't find player.")
            target.isLocked || target.controllerManager.controller != null -> p.sendMessage("You cannot teleport this player.")
            target.hasRights(Rights.DEVELOPER) -> p.sendMessage("Unable to teleport a developer to you.")
            else -> target.tele(p.tile)
        }
    }

    Commands.add(Rights.MOD, "mute [player name]", "Mutes a player for 2 days.") { p, args ->
        World.forceGetPlayerByDisplay(Utils.concat(args)) { target ->
            if (target == null) {
                p.sendMessage("Unable to find player.")
                return@forceGetPlayerByDisplay
            }
            val until = AccountAction.days(2)
            target.mute(until)
            p.sendMessage("You have muted ${Utils.formatPlayerNameForDisplay(Utils.concat(args))} for 2 days.")
            WorldServer.accountAction(p, AccountAction.Mute(target.username, until))
        }
    }

    Commands.add(Rights.MOD, "actions [player_name ...]", "Displays the last recorded actions the players specified have done.") { p, args -> p.recorder.showActions(args) }
    Commands.add(Rights.MOD, "watch [player_name ...]", "Continuously displays recorded actions for the players specified.") { p, args -> p.recorder.watch(args) }
    Commands.add(Rights.MOD, "stopwatch", "Stops watching players.") { p, _ ->
        if (p.recorder.stopWatching()) p.packets.sendDevConsoleMessage("Stopped watching.")
        else p.sendMessage("You aren't watching anyone.")
    }
}
