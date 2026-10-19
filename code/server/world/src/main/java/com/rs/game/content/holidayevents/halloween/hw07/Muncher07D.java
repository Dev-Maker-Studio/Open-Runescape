package com.rs.game.content.holidayevents.halloween.hw07;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.managers.EmotesManager.Emote;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class Muncher07D extends Conversation {

	private static final Animation FLINCH = new Animation(6563);
	private static final Animation BITE = new Animation(6565);

	public Muncher07D(Player player, NPC muncher) {
		super(player);
		addPlayer(HeadE.CHEERFUL, "Here, boy!");
		addPlayer(HeadE.TERRIFIED, "Whoaah!", () -> {
			muncher.faceEntityTile(player);
			muncher.forceTalk("Grrrrrr");
			muncher.setNextAnimation(FLINCH);
		});

		Dialogue op = addOption("He looks mad, what would you like to do?", "Stroke him"/*, "Try to entertain him"*/, "Blow a raspberry at him");

		op.addPlayer(HeadE.NERVOUS, "Okay, touching him seems to be a bad idea.", () -> {
			player.setNextAnimation(new Animation(7271));
			muncher.forceTalk("Grrrrrr");
			muncher.setNextAnimation(FLINCH);
		});

		//op.addPlayer(HeadE.CALM_TALK, "Entertain");

		op.addPlayer(HeadE.LAUGH, "Hehe. This'll make him think twice!")
		.addNext(() -> {
			player.lock();
			WorldTasks.scheduleLooping(new Task() {
				int stage = 0;
				@Override
				public void run() {
					if (stage == 0)
						player.faceEntityTile(muncher);
					else if (stage == 1)
						player.setNextAnimation(Emote.RASPBERRY.getAnim());
					else if (stage == 3) {
						muncher.setNextAnimation(BITE);
						player.fakeHit(new Hit(player.getHitpoints(), HitLook.TRUE_DAMAGE));
					} else if (stage == 4)
						player.sendDeath(null);
					else if (stage == 11)
						player.startConversation(new Dialogue().addPlayer(HeadE.SKEPTICAL_THINKING, "Maybe that wasn't so wise."));
					stage++;
				}
			}, 0, 0);
		});

		create();
	}

	public static NPCClickHandler handleMuncher = new NPCClickHandler(new Object[] { 2329 }, e -> e.getPlayer().startConversation(new Muncher07D(e.getPlayer(), e.getNPC())));
}
