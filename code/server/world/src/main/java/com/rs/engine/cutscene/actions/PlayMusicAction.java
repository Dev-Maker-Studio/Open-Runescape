package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class PlayMusicAction extends CutsceneAction {

	private final int id;
	private final int delay;
	private final int volume;

	public PlayMusicAction(int id, int delay, int volume, int actionDelay) {
		super(null, actionDelay);
		this.id = id;
		this.delay = delay;
		this.volume = volume;
	}
	
	public PlayMusicAction(int id, int delay, int actionDelay) {
		super(null, actionDelay);
		this.id = id;
		this.delay = delay;
		this.volume = 255;
	}
	
	public PlayMusicAction(int id, int actionDelay) {
		super(null, actionDelay);
		this.id = id;
		this.delay = 5;
		this.volume = 255;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		player.musicTrack(id, delay, volume);
	}

}
