package com.rs.game.content.skills.util;

import org.darkan.core.cache.ItemDefinitions;
import org.darkan.core.cache.AnimationDefinitions;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.Item;
import org.darkan.core.model.SpotAnim;

public class CreationAction extends PlayerAction {

	private final Item[] mats;
	private final Item product;
	private final int nonRemReq;
	private final int skill;
	private int cycles;
    private final int level;
	private final double experience;
	private final int animationId;
	private final int gfx;
	private final int delay;
	private boolean consistentAnim = false;

	public CreationAction(ReqItem product, int animationId, int gfx, int delay, int cycles) {
		skill = product.getSkill();
		mats = product.getMaterials();
		level = product.getReq();
		experience = product.getXp();
		this.product = product.getProduct();
		nonRemReq = product.getTool();
		this.animationId = animationId;
		this.delay = delay;
		this.cycles = cycles;
		this.gfx = gfx;
	}

	public CreationAction setConsistentAnimation(boolean consistentAnim) {
		this.consistentAnim = consistentAnim;
		return this;
	}

	public boolean checkAll(Player player) {
		if (skill >= 0) {
			int pLvl = player.getSkills().getLevel(skill);
			if (skill == Constants.SUMMONING)
				pLvl = player.getSkills().getLevelForXp(skill);
			if (pLvl < level) {
				player.sendMessage("You need a " + Constants.SKILL_NAME[skill] + " level of " + level + " to make that.");
				return false;
			}
		}
		return continueNextCycle(player);
	}

	@Override
	public boolean start(Player player) {
		if (!checkAll(player))
			return false;
		if (delay == -2) {
			if (animationId > 0)
				player.setNextAnimation(new Animation(animationId));
			else if (animationId == -1) {
				int anim = -1;
				if (nonRemReq != -1)
					anim = AnimationDefinitions.getAnimationWithItem(nonRemReq);
				if (anim == -1)
					anim = AnimationDefinitions.getAnimationWithItem(product.getId());
				for (Item item : mats)
					if (item != null) {
						if (anim != -1)
							break;
						anim = AnimationDefinitions.getAnimationWithItem(item.getId());
					}
				if (anim != -1)
					player.setNextAnimation(new Animation(anim));
			}
			if (gfx != -1)
				player.setNextSpotAnim(new SpotAnim(gfx));
			double xp = 0;
			while (cycles > 0) {
				cycles--;
				if (!continueNextCycle(player))
					break;
				for (Item item : mats)
					if (item != null)
						player.getInventory().deleteItem(item);
				xp += experience;
				player.getInventory().addItem(product.getId(), product.getAmount(), true);
			}
			if (skill >= 0 && xp > 0)
				player.getSkills().addXp(skill, xp);
			return false;
		}
		return true;
	}

	@Override
	public boolean process(Player player) {
		return continueNextCycle(player) && cycles > 0;
	}

	private boolean continueNextCycle(Player player) {
		if (!player.getInventory().hasFreeSlots(mats, product))
			return false;
		if (consistentAnim)
			player.setNextAnimation(new Animation(animationId));
		if (mats != null && !player.getInventory().containsItems(mats)) {
			StringBuilder mat = new StringBuilder();
			for (Item item : mats)
				if (item != null) {
					mat.append(item.getAmount()).append(" ");
					mat.append(item.getDefinitions().getName()).append(", ");
				}
			player.sendMessage("You need " + mat.toString().toLowerCase().trim().substring(0, mat.length() - 2) + " to create a " + product.getName().toLowerCase() + ".");
			return false;
		}
		if (nonRemReq != -1 && !player.getInventory().containsItem(nonRemReq, 1)) {
			player.sendMessage("You require a " + ItemDefinitions.getDefs(nonRemReq).getName().toLowerCase() + " to create this.");
			return false;
		}
		return true;
	}

	@Override
	public int processWithDelay(Player player) {
		cycles--;
		if (animationId > 0)
			player.setNextAnimation(new Animation(animationId));
		else if (animationId == -1) {
			int anim = -1;
			if (nonRemReq != -1)
				anim = AnimationDefinitions.getAnimationWithItem(nonRemReq);
			if (anim == -1)
				anim = AnimationDefinitions.getAnimationWithItem(product.getId());
			for (Item item : mats)
				if (item != null) {
					if (anim != -1)
						break;
					anim = AnimationDefinitions.getAnimationWithItem(item.getId());
				}
			if (anim != -1)
				player.setNextAnimation(new Animation(anim));
		}
		if (gfx != -1)
			player.setNextSpotAnim(new SpotAnim(gfx));
		for (Item item : mats)
			if (item != null)
				player.getInventory().deleteItem(item);
		if (skill >= 0)
			player.getSkills().addXp(skill, experience);
		player.getInventory().addItem(product.getId(), product.getAmount());
		return delay;
	}

	@Override
	public void stop(Player player) {
		setActionDelay(player, delay);
	}
}
