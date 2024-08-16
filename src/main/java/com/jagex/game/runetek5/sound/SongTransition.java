package com.jagex.game.runetek5.sound;

import com.jagex.game.runetek5.sound.player.song.SongPlayer;

public class SongTransition extends AbstractSongTransition {

    @Override
    public void transition() {
        SongPlayer.primarySound = SongPlayer.secondarySound;
        SongPlayer.secondarySound = null;
    }
}