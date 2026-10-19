package com.rs.game.content.bosses.nomad;

import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;

public class FakeNomad extends NPC {

	private final Nomad nomad;

	public FakeNomad(Tile tile, Nomad nomad) {
		super(8529, tile, true);
		this.nomad = nomad;
		setForceMultiArea(true);
	}

	@Override
	public void handlePreHit(Hit hit) {
		nomad.destroyCopy(this);
	}

}
