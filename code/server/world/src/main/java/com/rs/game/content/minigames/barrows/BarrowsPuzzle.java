package com.rs.game.content.minigames.barrows;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;

@PluginEventHandler
public class BarrowsPuzzle {

	private static final int BARROWS_PUZZLE_INTERFACE = 25;
	private static final int SEQUENCE_CHILD_START = 6;
	private static final int[] OPTIONS_COMPS = { 2, 3, 5 };

	private final BarrowsPuzzleType puzzle;
	private final int[] shuffledOptions;

	public BarrowsPuzzle() {
		puzzle = BarrowsPuzzleType.values()[Utils.random(BarrowsPuzzleType.values().length)];
		shuffledOptions = Utils.shuffleIntArray(puzzle.getOptions());
	}

	public BarrowsPuzzle display(Player player) {
		for (int i = 0; i < 3; i++) {
			int sequenceModel = puzzle.getSequenceModel(i);
			player.getPackets().setIFModel(BARROWS_PUZZLE_INTERFACE, SEQUENCE_CHILD_START + i, sequenceModel);

			int optionModel = shuffledOptions[i];
			player.getPackets().setIFModel(BARROWS_PUZZLE_INTERFACE, OPTIONS_COMPS[i], optionModel);
		}

		player.getInterfaceManager().sendInterface(BARROWS_PUZZLE_INTERFACE);
		return this;
	}

	public boolean isCorrect(int componentId) {
		int idx = 0;
		for (int i = 0;i < OPTIONS_COMPS.length;i++)
			if (OPTIONS_COMPS[i] == componentId) {
				idx = i;
				break;
			}
		return shuffledOptions[idx] == puzzle.getAnswer();
	}
}
