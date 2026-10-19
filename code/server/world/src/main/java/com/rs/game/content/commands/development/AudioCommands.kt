package com.rs.game.content.commands.development

import com.rs.Settings
import com.rs.engine.command.Commands
import com.rs.game.World
import com.rs.game.content.skills.summoning.Pouch
import com.rs.game.model.entity.async.schedule
import com.rs.game.tasks.Task
import com.rs.game.tasks.WorldTasks
import org.darkan.core.model.Rights
import org.darkan.core.net.prot.packet.ResetSounds
import org.darkan.core.Logger
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.utils.music.Music
import com.rs.utils.music.Voices
import kotlin.math.ceil
import org.darkan.core.gameval.gen.Varbit
import org.darkan.core.gameval.gen.Varp

@ServerStartupEvent
fun loadAudioCommands() {
    val audioRights = if (Settings.getConfig().isDebug) Rights.PLAYER else Rights.DEVELOPER

    Commands.add(audioRights, "sound [id]", "Plays a sound effect.") { p, args ->
        p.soundEffect(args[0].toInt(), true)
    }

    Commands.add(audioRights, "tilesound [id]", "Plays a tile sound effect.") { p, args ->
        World.soundEffect(p.tile, args[0].toInt())
    }

    Commands.add(audioRights, "music [id (volume)]", "Plays a music track.") { p, args ->
        p.musicsManager.playSongWithoutUnlocking(args[0].toInt())
    }

    Commands.add(Rights.DEVELOPER, "voice, v [id]", "Plays voices.") { p, args ->
        p.session.queuePacket(ResetSounds())
        p.voiceEffect(args[0].toInt(), true)
    }

    Commands.add(Rights.DEVELOPER, "familiarhead", "Tests familiar/pet chathead icons") { p, args ->
        val pouch = Pouch.valueOf(args[0])
        p.vars.setVar(Varp.follower_obj, pouch.id) // configures familiar type
        p.vars.setVar(1174, 0, true) // refresh familiar head
        p.vars.setVarBit(Varbit.lore_interface_boolean, args[1].toInt()) // refresh familiar emote
    }

    Commands.add(Rights.DEVELOPER, "playthroughvoices [start finish tick_delay]", "Plays through voice range.") { p, args ->
        val tickDelay = args[2].toInt()
        p.schedule {
            var voiceId = args[0].toInt()
            while(voiceId <= args[1].toInt()) {
                if (Voices.voicesMarked.contains(voiceId)) {
                    for (i in voiceId until 100_000) {
                        if (!Voices.voicesMarked.contains(voiceId)) break
                        voiceId++
                    }
                }
                if (!Voices.voicesMarked.contains(voiceId)) {
                    p.sendMessage("Playing voice $voiceId")
                    p.voiceEffect(voiceId++, true)
                }
                wait(tickDelay)
            }
        }
    }

    Commands.add(Rights.DEVELOPER, "unusedmusic", "Shows unused music.") { p, _ ->
        var count = 0

        for (i in 0 until 1099) {
            if (Music.getSongGenres(i).isEmpty()) {
                val song = Music.getSong(i)
                count++

                if (song == null) {
                    Logger.error(Commands::class.java, "unusedmusic", "Error @$i")
                } else {
                    Logger.debug(Commands::class.java, "unusedmusic", "$i ${song.name}: ${song.hint}")
                }
            }
        }

        Logger.debug(Commands::class.java, "unusedmusic", "Total unused: $count")
        Logger.debug(Commands::class.java, "unusedmusic", "Unused is ${ceil(count / 1099.0 * 100)}%")
    }

    Commands.add(Rights.DEVELOPER, "nextm", "Plays next ambient music track.") { p, _ ->
        p.musicsManager.nextAmbientSong()
    }

    Commands.add(Rights.DEVELOPER, "genre", "Shows current music genre") { p, _ ->
        val genre = p.musicsManager.playingGenre
        if (genre == null) {
            p.sendMessage("No genre, remember this updates after an ambient song is played...")
        } else {
            p.sendMessage(genre.genreName)
        }
    }

    Commands.add(Rights.DEVELOPER, "frogland", "Plays frogland to everyone on the server.") { _, _ ->
        World.allPlayers { target ->
            target?.packets?.sendRunScript(1764, 12451857, 12451853, 20, 0) // music/sound volumes
            target?.musicTrack(409)
        }
    }

    Commands.add(Rights.DEVELOPER, "musicall [id]", "Plays music to everyone on the server.") { _, args ->
        World.allPlayers { target ->
            target?.packets?.sendRunScript(1764, 12451857, 12451853, 20, 0) // music/sound volumes
            target?.musicTrack(args[0].toInt())
        }
    }

    Commands.add(Rights.DEVELOPER, "jingle [id]", "Plays jingles") { p, args ->
        p.jingle(args[0].toInt())
    }
}