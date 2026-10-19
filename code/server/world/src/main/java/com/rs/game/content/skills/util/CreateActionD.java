package com.rs.game.content.skills.util;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.impl.MakeXActionD;
import com.rs.engine.dialogue.impl.MakeXItem;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;

public class CreateActionD extends Conversation {

	private final int[] anims;
	private final Item[][] materials;
	private final Item[][] products;
	private int[] reqs;
	private final double[] xp;
	private final int skill;
	private final int delay;

	public CreateActionD(Player player, Item[][] materials, Item[][] products, int delay) {
		super(player);
		this.materials = materials;
		this.products = products;
		this.delay = delay;
		skill = -1;
		anims = null;
		xp = null;
		initialize();
		create();
	}

	public CreateActionD(Player player, Item[][] materials, Item[][] products, double[] xp, int skill, int delay) {
		super(player);
		this.materials = materials;
		this.products = products;
		this.delay = delay;
		this.skill = skill;
		anims = null;
		this.xp = xp;
		initialize();
		create();
	}

	public CreateActionD(Player player, Item[][] materials, Item[][] products, double[] xp, int[] anims, int skill, int delay) {
		super(player);
		this.materials = materials;
		this.products = products;
		this.delay = delay;
		this.skill = skill;
		this.anims = anims;
		this.xp = xp;
		initialize();
		create();
	}

	public CreateActionD(Player player, Item[][] materials, Item[][] products, double[] xp, int[] anims, int[] reqs, int skill, int delay) {
		super(player);
		this.materials = materials;
		this.products = products;
		this.delay = delay;
		this.skill = skill;
		this.anims = anims;
		this.xp = xp;
		this.reqs = reqs;
		initialize();
		create();
	}

	public CreateActionD(Player player, Item[][] materials, Item[][] products, double[] xp, int anim, int skill, int delay) {
		super(player);
		this.materials = materials;
		this.products = products;
		this.delay = delay;
		this.skill = skill;
		anims = new int[products.length];
		for (int i = 0;i < products.length;i++)
			anims[i] = anim;
		this.xp = xp;
		initialize();
		create();
	}

	public CreateActionD(Player player, Item[][] materials, Item[][] products, double[] xp, int anim, int[] reqs, int skill, int delay) {
		super(player);
		this.materials = materials;
		this.products = products;
		this.delay = delay;
		this.skill = skill;
		anims = new int[products.length];
		for (int i = 0;i < products.length;i++)
			anims[i] = anim;
		this.xp = xp;
		this.reqs = reqs;
		initialize();
		create();
	}

	public void initialize() {
		MakeXActionD makeX = new MakeXActionD();
		for (int i = 0; i < this.products.length; i++)
			makeX.addOption(new MakeXItem(player, materials[i], products[i], xp == null ? 0.0 : xp[i], anims == null ? -1 : anims[i], reqs == null ? 1 : reqs[i], skill, delay));
		addNext(makeX);
	}
}
