package com.jagex.game.runetek5.sound

import com.jagex.game.runetek5.sound.player.song.SongPlayer

class SongTransition : AbstractSongTransition() {
    override fun transition() {
        SongPlayer.primarySound = SongPlayer.secondarySound
        SongPlayer.secondarySound = null
    }
}
