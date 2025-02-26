package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.midi.SongReference;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;

/**
 * 
 * @author Brainpower - 09/04/2022
 *
 */
public class CutscenePlaySong extends CutsceneAction {

    public final int musicId;

    public final int volume;

    public SongReference songReference;

    public CutscenePlaySong(Packet buffer) {
        super(buffer);
        this.musicId = buffer.readUShort();
        this.volume = buffer.readUByte();
    }

    @Override
    public boolean isFullyCached() {
        if (this.songReference == null) {
            this.songReference = new SongReference(Resource.MUSIC, this.musicId);
        }
        return this.songReference.initialize();
    }

    @Override
    public void perform() {
        SongPlayer.play(this.songReference, this.volume);
    }
}
