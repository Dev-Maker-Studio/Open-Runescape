package com.rs.game.content.clans;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.darkan.core.cache.EnumDefinitions;
import world.gregs.voidps.cache.definition.data.EnumDefinition;
import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.statements.SimpleStatement;
import com.rs.engine.dialogue.statements.Statement;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.NPCBodyMeshModifier;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.model.gameobject.ObjectMeshModifier;
import org.darkan.core.clan.ClanAction;
import org.darkan.core.model.Rights;
import org.darkan.core.model.ClanMemberData;
import org.darkan.core.model.Clan;
import org.darkan.core.model.ClanPermission;
import org.darkan.core.model.ClanRank;
import org.darkan.core.model.ClanSetting;
import com.rs.utils.RSColor;
import org.darkan.core.Utils;
import com.rs.utils.KtExtensionsKt;
import org.darkan.world.clan.ClanRegistry;
import org.darkan.world.server.WorldServer;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ButtonClickHandler;
import com.rs.plugin.handlers.InterfaceOnPlayerHandler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.darkan.core.gameval.gen.Enum;
import org.darkan.core.gameval.gen.Varbit;
import org.darkan.core.gameval.gen.Varp;

/**
 * The clan interfaces as a world player sees them (docs/clans.md).
 *
 * Nothing here writes to a clan. The lobby owns every clan document and every channel, so each
 * button becomes a typed {@link ClanAction} handed to {@link ClanRegistry#send} - which checks the
 * shared {@code ClanRules} first so the player gets a denial straight away - and the change comes
 * back as the snapshot the lobby broadcasts. What the interfaces read is the world's replica,
 * {@link ClanRegistry}.
 */
@PluginEventHandler
public class ClansManager {

	/** The clan the world knows under this name, asking the lobby for it if it has never seen it. */
	public static Clan getClan(String name) {
		return ClanRegistry.clanOrFetch(name);
	}

	public static void getClan(String name, Consumer<Clan> cb) {
		ClanRegistry.fetch(name, cb == null ? null : cb::accept);
	}

	public static void clanMotto(Player player) {
		player.startConversation(new Dialogue(new Statement() {
			@Override
			public void send(Player player) {
				player.sendInputLongText("Enter clan motto:", motto -> updateSetting(player, ClanSetting.MOTTO, motto));
				player.getInterfaceManager().sendChatBoxInterface(1103);
			}

			@Override
			public int getOptionId(int componentId) {
				return 0;
			}

			@Override
			public void close(Player player) {

			}
		}));
	}

	public static void create(Player player, String name) {
		player.sendOptionDialogue("The name " + name + " is available. Create the clan?", ops -> {
			ops.add("Yes, create " + name + ".", () -> {
				player.getTempAttribs().setB("ccCreateLock", true);
				// The snapshot the lobby broadcasts after the create is what regenerates the
				// appearance and draws the clan; all this callback owns is the lock.
				if (!ClanRegistry.send(player, new ClanAction.Create(name), result -> {
					player.getTempAttribs().removeB("ccCreateLock");
					if (result.getOk())
						player.sendMessage("Your clan has been created!");
				}))
					player.getTempAttribs().removeB("ccCreateLock");
			});
			ops.add("No, I want to pick another name.");
        });
	}

	public static void promptName(Player player) {
		player.sendInputName("Which name would you like for your clan?", name -> {
			if (player.getTempAttribs().getB("ccCreateLock")) {
				player.simpleDialogue("Your previous request to create a clan is still in progress... Please wait or relog.");
				return;
			}
			player.getTempAttribs().setB("ccCreateLock", true);
			// The result has already shown the player why a name was rejected, so a rejection
			// just puts them back on the name prompt.
			if (!ClanRegistry.send(player, new ClanAction.CheckName(name), result -> {
				player.getTempAttribs().removeB("ccCreateLock");
				if (result.getOk())
					create(player, name);
				else
					promptName(player);
			}))
				player.getTempAttribs().removeB("ccCreateLock");
		});
	}

	public static ButtonClickHandler handleClanChatButtons = new ButtonClickHandler(1110, e -> {
		if (e.getPlayer().hasRights(Rights.DEVELOPER))
			e.getPlayer().sendMessage("handleClanChatButtons: " + e.getComponentId() + " - " + e.getSlotId() + " - " + e.getOpNum());
		switch(e.getComponentId()) {
		case 82 -> {
			if (e.getPlayer().getSocial().getConnectedToClan() && e.getPlayer().getSocial().getClanName() != null)
				leaveChannel(e.getPlayer(), false);
			else
				joinChannel(e.getPlayer(), null);
		}
		case 91 -> {
			if (e.getPlayer().getSocial().getGuestedClanChat() != null)
				leaveChannel(e.getPlayer(), true);
			else
				e.getPlayer().sendInputName("Which clan chat would you like to join?",
						"Please enter the name of the clan whose Clan chat you wish to join as a guest. <br><br>To talk as a guest, start  your<br>line<br>of chat with ///",
						name -> joinChannel(e.getPlayer(), name));
		}
		case 59 -> e.getPlayer().getPackets().sendRunScript(4443, -1);
		case 95 -> e.getPlayer().sendInputName("Which player would you like to ban?", name -> banPlayer(e.getPlayer(), name));
		case 78 -> openSettings(e.getPlayer());
		case 75 -> openClanDetails(e.getPlayer(), null, e.getPlayer().getClan());
		case 109 -> leaveClan(e.getPlayer());
		case 11 -> unban(e.getPlayer(), e.getSlotId());
		case 99 -> e.getPlayer().sendInputName("Which player would you like to unban?", name -> unban(e.getPlayer(), name));
		}
	});

	public static ButtonClickHandler handleClanFlagButtons = new ButtonClickHandler(1089, e -> {
		if (e.getPlayer().getClan() == null)
			return;
		switch(e.getComponentId()) {
		case 30 -> e.getPlayer().getTempAttribs().setI("clanflagselection", e.getSlotId());
		case 26 -> {
			int flag = e.getPlayer().getTempAttribs().removeI("clanflagselection", -1);
			e.getPlayer().stopAll();
			if (flag != -1)
				updateSetting(e.getPlayer(), ClanSetting.NATIONAL_FLAG, flag);
		}
		}
	});

	public static ButtonClickHandler handleClanSettingsButtonsMain = new ButtonClickHandler(1096, e -> {
		if (e.getPlayer().hasRights(Rights.DEVELOPER))
			e.getPlayer().sendMessage("handleClanSettingsButtonsMain: " + e.getComponentId() + " - " + e.getSlotId() + " - " + e.getOpNum());
		switch(e.getComponentId()) {
		case 41 -> viewClanmateDetails(e.getPlayer(), e.getSlotId());
		case 94 -> toggleSetting(e.getPlayer(), ClanSetting.GUESTS_CAN_ENTER_CC);
		case 95 -> toggleSetting(e.getPlayer(), ClanSetting.GUESTS_CAN_TALK_CC);
		case 96 -> toggleSetting(e.getPlayer(), ClanSetting.IS_RECRUITING);
		case 97 -> toggleSetting(e.getPlayer(), ClanSetting.USES_CLANTIMEZONE);
		case 113 -> showClanSettingsClanMates(e.getPlayer());
		case 120 -> showClanSettingsSettings(e.getPlayer());
		case 124 -> openClanMotifInterface(e.getPlayer());
		case 131 -> openClanMottoInterface(e.getPlayer());
		case 160 -> openKeywordEditor(e.getPlayer());
		case 222 -> ClanRegistry.send(e.getPlayer(), new ClanAction.SetVar(2811, Utils.clampI(e.getSlotId(), 0, 4))); //signpost permissions
		case 207 -> updateSetting(e.getPlayer(), ClanSetting.GUEST_CITADEL_ACCESS, Utils.clampI(e.getSlotId(), 0, 3));
		case 240 -> updateSetting(e.getPlayer(), ClanSetting.GAME_TIME, -720 + e.getSlotId() * 10);
		case 290 -> updateSetting(e.getPlayer(), ClanSetting.HOME_WORLD, Utils.clampI(e.getSlotId(), 0, 200));
		case 297 -> openForumThreadInterface(e.getPlayer());
		case 346 -> openNationalFlagInterface(e.getPlayer());
		case 386 -> showClanSettingsPermissions(e.getPlayer());
		case 489 -> selectPermissionTab(e.getPlayer(), 1);
		case 498 -> selectPermissionTab(e.getPlayer(), 2);
		case 506 -> selectPermissionTab(e.getPlayer(), 3);
		case 514 -> selectPermissionTab(e.getPlayer(), 4);
		case 522 -> selectPermissionTab(e.getPlayer(), 5);

		case 262 -> e.getPlayer().getTempAttribs().setI("editClanMateJob", e.getSlotId());
		case 276 -> e.getPlayer().getTempAttribs().setI("editClanMateRank", e.getSlotId());
		case 309 -> kick(e.getPlayer());
		case 318 -> saveClanmateDetails(e.getPlayer());
		case 366 -> e.getPlayer().getPackets().sendVarc(1516, e.getSlotId());
		case 558 -> setChannelRank(e.getPlayer(), true);
		case 570 -> setChannelRank(e.getPlayer(), false);
		case 534, 547, 752, 739, 483, 659, 581, 592, 648, 684, 671,
			 793, 696, 603, 614, 782, 625, 815, 720, 708, 804 -> togglePermissionSetting(e.getPlayer(), e.getComponentId());
		default -> {
			if (e.getComponentId() >= 395 && e.getComponentId() <= 475) {
				int selectedRank = (e.getComponentId() - 395) / 8;
				if (selectedRank == 10)
					selectedRank = 125;
				else if (selectedRank > 5)
					selectedRank = 100 + selectedRank - 6;
				selectPermissionRank(e.getPlayer(), ClanRank.Companion.fromIconId(selectedRank));
			}
		}
		}
	});

	public static ButtonClickHandler handleMotifButtons = new ButtonClickHandler(1105, e -> {
		if (e.getPlayer().hasRights(Rights.DEVELOPER))
			e.getPlayer().sendMessage("handleMotifButtons: " + e.getComponentId() + " - " + e.getSlotId() + " - " + e.getOpNum());
		if (e.getComponentId() == 63 || e.getComponentId() == 66)
			ClansManager.setClanMotifTextureInterface(e.getPlayer(), e.getComponentId() == 66, e.getSlotId());
		if (e.getComponentId() == 35)
			ClansManager.openSetMotifColor(e.getPlayer(), 0);
		else if (e.getComponentId() == 80)
			ClansManager.openSetMotifColor(e.getPlayer(), 1);
		else if (e.getComponentId() == 92)
			ClansManager.openSetMotifColor(e.getPlayer(), 2);
		else if (e.getComponentId() == 104)
			ClansManager.openSetMotifColor(e.getPlayer(), 3);
		else if (e.getComponentId() == 120) {
			saveMotif(e.getPlayer());
			e.getPlayer().stopAll();
		}
	});

	private static final BiMap<ClanSetting, Integer> KEYWORD_INDICES = HashBiMap.create(10);

	static {
		KEYWORD_INDICES.put(ClanSetting.KEYWORD0_CATEGORY, 9155);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD0_INDEX, 9156);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD1_CATEGORY, 9157);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD1_INDEX, 9158);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD2_CATEGORY, 9159);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD2_INDEX, 9160);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD3_CATEGORY, 9161);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD3_INDEX, 9162);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD4_CATEGORY, 9163);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD4_INDEX, 9164);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD5_CATEGORY, 9165);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD5_INDEX, 9166);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD6_CATEGORY, 9167);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD6_INDEX, 9168);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD7_CATEGORY, 9169);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD7_INDEX, 9170);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD8_CATEGORY, 9171);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD8_INDEX, 9172);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD9_CATEGORY, 9173);
		KEYWORD_INDICES.put(ClanSetting.KEYWORD9_INDEX, 9174);
	}

	/**
	 * The motif interface edits a copy: the icons and the four colours are held in temp attribs
	 * (and mirrored into the player's own vars so the interface draws them) until the save button
	 * sends them as one {@link ClanAction.UpdateSettings}. The clan itself is read-only here.
	 */
	private static final ClanSetting[] MOTIF_SETTINGS = {
		ClanSetting.MOTIF_TOP_ICON, ClanSetting.MOTIF_BOTTOM_ICON,
		ClanSetting.MOTIF_TOP_COLOR, ClanSetting.MOTIF_BOTTOM_COLOR,
		ClanSetting.MOTIF_PRIMARY_COLOR, ClanSetting.MOTIF_SECONDARY_COLOR,
	};

	private static final int[] MOTIF_COLOR_VARPS = {
		Varp.clan_custom_motif_colour1_varp, Varp.clan_custom_motif_colour2_varp,
		Varp.clan_custom_colour1_varp, Varp.clan_custom_colour2_varp,
	};

	private static final int MOTIF_FIRST_COLOR = 2;

	public static ButtonClickHandler handleKeywordButtons = new ButtonClickHandler(1097, e -> {
		if (e.getPlayer().hasRights(Rights.DEVELOPER))
			e.getPlayer().sendMessage("handleKeywordButtons: " + e.getComponentId() + " - " + e.getSlotId() + " - " + e.getOpNum());
		switch(e.getComponentId()) {
		case 193 -> e.getPlayer().getVars().setVar(Varp.clan_keyword_temp_cat_varp, e.getSlotId());
		case 207 -> {
			int enumId = EnumDefinitions.getEnum(3703).getIntValue(e.getPlayer().getVars().getVar(Varp.clan_keyword_temp_cat_varp));
			if (enumId == -1) {
				e.getPlayer().getTempAttribs().removeI("keywordTempVal");
				return;
			}
			EnumDefinition infoDef = EnumDefinitions.getEnum(enumId);
			if (infoDef.getStringValue(e.getSlotId()) != null && !infoDef.getStringValue(e.getSlotId()).equals("null"))
				e.getPlayer().getTempAttribs().setI("keywordTempVal", e.getSlotId());
			else
				e.getPlayer().getTempAttribs().removeI("keywordTempVal");
		}
		case 15 -> {
			int category = e.getPlayer().getVars().getVar(Varp.clan_keyword_temp_cat_varp);
			if (category <= 0)
				return;
			int index = e.getPlayer().getTempAttribs().getI("keywordTempVal");
			for (ClanSetting setting : KEYWORD_INDICES.keySet()) {
				if (setting.name().contains("INDEX"))
					continue;
				int varbit = KEYWORD_INDICES.get(setting);
				if (e.getPlayer().getVars().getVarBit(varbit) <= 0) {
					e.getPlayer().getVars().setVarBit(varbit, category);
					e.getPlayer().getVars().setVarBit(varbit+1, index);
					break;
				}
			}
		}
		case 160, 171, 172, 173, 174, 175, 176, 177, 178, 179 -> {
			int index = e.getComponentId() == 160 ? 0 : e.getComponentId() - 170;
			int baseVar = 9155 + (index * 2);
			e.getPlayer().getVars().setVarBit(baseVar, 0);
			e.getPlayer().getVars().setVarBit(baseVar+1, 0);
		}
		case 42 -> {
			saveKeywords(e.getPlayer());
			e.getPlayer().stopAll();
		}
		}
	});

	public static ButtonClickHandler handleCloseButton = new ButtonClickHandler(1079, e -> e.getPlayer().closeInterfaces());

	public static ButtonClickHandler handleInviteInter = new ButtonClickHandler(1095, e -> {
		if (e.getComponentId() == 33) {
			Player inviter = e.getPlayer().getTempAttribs().removeO("clanInviter");
			if (inviter == null) {
				e.getPlayer().sendMessage("There is a pending request to join a clan submitted to the clan service. Relog or try again later.");
				return;
			}
			String clanName = inviter.getSocial().getClanName();
			if (clanName == null) {
				e.getPlayer().sendMessage(inviter.getDisplayName() + " is no longer in a clan.");
				e.getPlayer().closeInterfaces();
				return;
			}
			ClanRegistry.send(e.getPlayer(), new ClanAction.AcceptInvite(clanName, inviter.getUsername()));
			e.getPlayer().closeInterfaces();
		}
	});

	public static InterfaceOnPlayerHandler handleInvite = new InterfaceOnPlayerHandler(false, new int[] { 1110 }, e -> {
		if (e.getComponentId() == 87) {
			if (e.getTarget().getSocial().getClanName() != null) {
				e.getPlayer().sendMessage(e.getTarget().getDisplayName() + " is already in a clan.");
				return;
			}
			e.getPlayer().sendMessage("Sending clan invite to " + e.getTarget().getDisplayName() + ".");
			e.getTarget().getPackets().sendClanInviteMessage(e.getPlayer());
		}
	});

	public static void viewClanmateDetails(Player player, int index) {
		Clan clan = player.getClan();
		if (clan == null)
			return;
		// Rows are in the order ClanSettingsFull listed the members, not the map's iteration order.
		List<String> members = ClanRegistry.orderedMembers(clan);
		if (index >= members.size()) {
			player.sendMessage("This is a placeholder character to unlock all clan features without needing 5 players.");
			return;
		}
		String username = members.get(index);
		ClanMemberData member = clan.getMembers().get(username);
		if (member == null)
			return;
		WorldServer.INSTANCE.displayNameAsync(username, displayName -> {
			if (displayName == null) {
				player.sendMessage("Error finding player profile for target character.");
				return;
			}
			viewClanmateDetails(player, displayName, member);
			player.getTempAttribs().removeI("editClanMateRank");
			player.getTempAttribs().removeI("editClanMateJob");
			if (clan.hasPermissions(player.getUsername(), ClanRank.ADMIN))
				player.getTempAttribs().setO("editClanMate", username);
		});
		player.getTempAttribs().removeI("editClanMateRank");
		player.getTempAttribs().removeI("editClanMateJob");
		if (clan.hasPermissions(player.getUsername(), ClanRank.ADMIN))
			player.getTempAttribs().setO("editClanMate", username);
	}

	protected static void togglePermissionSetting(Player player, int componentId) {
		ClanRank rank = player.getTempAttribs().getO("permissionRankEditing");
		if (rank == null) {
			player.sendMessage("Please select a rank to edit before editing permissions.");
			return;
		}
		String settingName = switch(componentId) {
		case 534 -> "PERMISSION_LOCK_KEEP";
		case 547 -> "PERMISSION_LOCK_CITADEL";
		case 739 -> "PERMISSION_ENTER_KEEP";
		case 752 -> "PERMISSION_ENTER_CITADEL";
		case 483 -> "PERMISSION_INVITE";
		case 659 -> "PERMISSION_LEAD_CLANWARS";
		case 581 -> "PERMISSION_SIGNPOST";
		case 592 -> "PERMISSION_NOTICEBOARD";
		case 648 -> "PERMISSION_START_BATTLE";
		case 671 -> "PERMISSION_CALL_VOTE";
		case 684 -> "PERMISSION_BEGIN_MEETING";
		case 793 -> "PERMISSION_THEATER";
		case 696 -> "PERMISSION_PARTY_ROOM";
		case 603 -> "PERMISSION_CITADEL_UPGRADE";
		case 614 -> "PERMISSION_CITADEL_DOWNGRADE";
		case 625 -> "PERMISSION_EDIT_BATTLEFIELD";
		case 782 -> "PERMISSION_CITADEL_LANGUAGE";
		case 815 -> "PERMISSION_CHECK_RESOURCES";
		case 720 -> "PERMISSION_BUILD_TIME";
		case 804 -> "PERMISSION_LOCK_PLOTS";
		case 708 -> "PERMISSION_RESOURCE_GATHER";
		default -> null;
		};
		if (settingName == null) {
			player.sendMessage("Unknown clan setting for component: " + componentId);
			return;
		}
		ClanSetting setting = ClanSetting.Companion.forName(rank.name() + "_" + settingName);
		if (setting == null) {
			player.sendMessage("Cannot edit permission " + settingName + " for rank " + rank + ".");
			return;
		}
		toggleSetting(player, setting);
	}

	public static void viewClanmateDetails(Player player, String username, ClanMemberData member) {
		player.getPackets().sendVarc(1500, member.getRank().getIconId());
		player.getPackets().sendVarc(1501, member.getJob());
		player.getPackets().sendVarc(1564, 0);
		player.getPackets().sendVarc(1565, member.getBanFromKeep() ? 1 : 0);
		player.getPackets().sendVarc(1566, member.getBanFromCitadel() ? 1 : 0);
		player.getPackets().sendVarc(1567, member.getBanFromIsland() ? 1 : 0);
		player.getPackets().sendVarc(1568, member.getFirstWeek() ? 1 : 0);
		player.getPackets().sendVarcString(347, username);
		player.getPackets().sendRunScriptBlank(4319);
	}

	/**
	 * The rank and job picked in the clanmate panel, as one UpdateMember. Everything the legacy
	 * code checked by hand - who may edit whom, which ranks may be assigned, the owner needing a
	 * deputy before stepping down, and handing the clan over when they do - is now the lobby's
	 * (and {@code ClanRules}') job, so this only says what changed.
	 */
	private static void saveClanmateDetails(Player player) {
		Clan clan = player.getClan();
		if (clan == null)
			return;
		String editingUser = player.getTempAttribs().getO("editClanMate");
		if (editingUser == null) {
			player.sendMessage("Please select a clanmate to edit.");
			return;
		}
		int jobId = player.getTempAttribs().getI("editClanMateJob", -1);
		int rankId = player.getTempAttribs().getI("editClanMateRank", -1);
		ClanRank newRank = rankId == -1 ? null : ClanRank.Companion.fromIconId(rankId);
		Integer newJob = jobId != -1 && EnumDefinitions.getEnum(3720).getStringValue(jobId) != null ? jobId : null;
		if (newRank == null && newJob == null)
			return;
		ClanRegistry.send(player, new ClanAction.UpdateMember(editingUser, newRank, newJob, null, null, null));
	}

	private static void kick(Player player) {
		String editingUser = player.getTempAttribs().getO("editClanMate");
		if (editingUser == null) {
			player.sendMessage("Please select a clanmate to kick.");
			return;
		}
		player.sendOptionDialogue("Are you sure you would like to kick this player from the clan?", ops -> {
			ops.add("Yes, I am sure I want to kick them.", () -> ClanRegistry.send(player, new ClanAction.RemoveMember(editingUser)));
			ops.add("Nevermind");
		});
	}

	private static void banPlayer(Player player, String name) {
		ClanRegistry.send(player, new ClanAction.Ban(Utils.formatPlayerNameForProtocol(name)));
	}

	private static void unban(Player player, String name) {
		ClanRegistry.send(player, new ClanAction.Unban(Utils.formatPlayerNameForProtocol(name)));
	}

	private static void unban(Player player, int slotId) {
		Clan clan = player.getClan();
		if (clan == null)
			return;
		// The ban list interface shows bannedUsers in the order the settings packet sent them.
		List<String> banned = ClanRegistry.orderedBans(clan);
		if (slotId < 0 || slotId >= banned.size())
			return;
		ClanRegistry.send(player, new ClanAction.Unban(banned.get(slotId)));
	}

	public static void unlockBanList(Player player) {
		player.getPackets().setIFRightClickOps(1110, 11, 0, 100, 0);
	}

	private static void joinChannel(Player player, String guestClanName) {
		if (guestClanName == null && player.getSocial().getClanName() == null) {
			createClan(player);
			return;
		}
		if (player.getTempAttribs().getB("ccJoinLock")) {
			player.sendMessage("Please wait...");
			return;
		}
		ClanAction action = guestClanName == null
				? ClanAction.JoinChannel.INSTANCE
				: new ClanAction.JoinGuestChannel(guestClanName.trim());
		player.getTempAttribs().setB("ccJoinLock", true);
		if (!ClanRegistry.send(player, action, result -> player.getTempAttribs().removeB("ccJoinLock")))
			player.getTempAttribs().removeB("ccJoinLock");
	}

	private static void createClan(Player player) {
		player.startConversation(new Conversation(new Dialogue(new SimpleStatement("You are not currently in a clan. Would you like to create one?")))
				.addOption("Create a clan?", "Yes", "Not right now.")
				.addNext(() -> promptName(player)));
	}

	private static void leaveChannel(Player player, boolean guest) {
		if (player.getTempAttribs().getB("ccLeaveLock")) {
			player.sendMessage("Please wait...");
			return;
		}
		ClanAction action = guest ? ClanAction.LeaveGuestChannel.INSTANCE : ClanAction.LeaveChannel.INSTANCE;
		player.getTempAttribs().setB("ccLeaveLock", true);
		if (!ClanRegistry.send(player, action, result -> player.getTempAttribs().removeB("ccLeaveLock")))
			player.getTempAttribs().removeB("ccLeaveLock");
	}

	private static void leaveClan(Player player) {
		if (player.getSocial().getClanName() == null) {
			player.sendMessage("You aren't in a clan.");
			return;
		}
		if (player.getTempAttribs().getB("ccLeaveLock")) {
			player.sendMessage("Please wait...");
			return;
		}
		player.sendOptionDialogue("Are you sure you want to leave your clan?", conf1 -> {
			conf1.add("I am sure I want to leave the clan.").addOptions("Are you ABSOLUTELY sure you want to leave?", conf2 -> {
				conf2.add("Yes, I am absolutely sure I want to leave my clan.", () -> {
					player.getTempAttribs().setB("ccLeaveLock", true);
					if (!ClanRegistry.send(player, ClanAction.Leave.INSTANCE, result -> player.getTempAttribs().removeB("ccLeaveLock")))
						player.getTempAttribs().removeB("ccLeaveLock");
				});
				conf2.add("No, I've changed my mind.");
			});
			conf1.add("Nevermind.");
		});
	}

	private static void openSettings(Player player) {
		if (player.getClan() == null) {
			player.sendMessage("You must be in a clan to do that.");
			return;
		}
		// Every setting button sends its own UpdateSettings, so there is nothing left to save on
		// close the way the legacy batch sync did.
		player.getInterfaceManager().sendInterface(1096);
		showClanSettingsClanMates(player);
		selectPermissionTab(player, 1);
		player.getPackets().setIFRightClickOps(1096, 240, 0, 144, 0); // unlocks timezone setting
		player.getPackets().setIFRightClickOps(1096, 290, 0, 200, 0); // unlocks worldid setting
		player.getPackets().setIFRightClickOps(1096, 41, 0, Clan.MAX_MEMBERS, 0); // unlocks clanmates
		player.getPackets().setIFRightClickOps(1096, 276, 0, 125, 0); // set member rank
		player.getPackets().setIFRightClickOps(1096, 262, 0, 500, 0); // set member profession
		player.getPackets().setIFRightClickOps(1096, 366, 0, 127, 0); // unlocks rank filters
		player.getPackets().setIFRightClickOps(1096, 222, 0, 4, 0); // unlocks signpost permissions
		player.getPackets().setIFRightClickOps(1096, 207, 0, 3, 0); // unlocks guest access to citadel settings
	}

	private static void selectPermissionRank(Player player, ClanRank rank) {
		Clan clan = player.getClan();
		if (clan == null || rank == null)
			return;
		player.getTempAttribs().setO("permissionRankEditing", rank);
		player.getPackets().sendRunScript(5130);
		player.getPackets().sendVarc(1569, rank.getIconId());
		for (ClanPermission perm : ClanPermission.values())
			player.getPackets().sendVarc(perm.getVarc(), perm.hasPermission(clan, rank) ? 1 : 0);
	}

	private static void selectPermissionTab(Player player, int tab) {
		player.getPackets().sendRunScript(5136, tab);
		player.getPackets().setIFHidden(1096, 26, true);
		for (int i = 16;i <= 20;i++)
            player.getPackets().setIFHidden(1096, i, tab != i - 15);
	}

	private static void showClanSettingsClanMates(Player player) {
		player.getPackets().setIFHidden(1096, 85, true);
		player.getPackets().setIFHidden(1096, 385, true);
		player.getPackets().sendRunScript(4298);
	}

	private static void showClanSettingsSettings(Player player) {
		player.getPackets().setIFHidden(1096, 85, true);
		player.getPackets().setIFHidden(1096, 385, true);
		player.getPackets().sendRunScript(4296);
	}

	private static void showClanSettingsPermissions(Player player) {
		player.getPackets().setIFHidden(1096, 85, false);
		player.getPackets().setIFHidden(1096, 385, false);
	}

	public static void openClanInvite(Player player, Player inviter, Clan clan) {
		if (clan == null) {
			player.sendMessage("Error loading clan details.");
			return;
		}
		// The invite panel only reads the clan, so it gets the guest form of the settings block.
		player.getSession().queuePacket(ClanRegistry.settingsFull(clan, false));
		player.getInterfaceManager().sendInterface(1095);
		player.getTempAttribs().setO("clanInviter", inviter);
		player.setCloseInterfacesEvent(() -> player.getTempAttribs().removeO("clanInviter"));
	}

	public static void openClanDetails(Player player, Player vexPlanter, Clan clan) {
		if (clan == null) {
			player.sendMessage("Error loading clan details.");
			return;
		}
		player.getSession().queuePacket(ClanRegistry.settingsFull(clan, false));
		player.getInterfaceManager().sendInterface(1107);
		if (vexPlanter != null) {
			player.getPackets().setIFText(1107, 92, vexPlanter.getDisplayName());
			//player.getPackets().sendRunScript(4423);
		} else
			//player.getPackets().sendRunScript(4422);
		player.getPackets().sendRunScript(4413);
	}

	public static void openNationalFlagInterface(Player player) {
		if (player.getClan() == null)
			return;
		if (!player.getClan().hasPermissions(player.getUsername(), ClanRank.ADMIN)) {
			player.sendMessage("Only clan admins can edit the clan settings.");
			return;
		}
		player.stopAll();
		player.getInterfaceManager().sendInterface(1089);
		player.getPackets().setIFRightClickOps(1089, 30, 0, 241, 0);
		player.setCloseInterfacesEvent(() -> openSettings(player));
	}

	public static void openKeywordEditor(Player player) {
		if (player.getClan() == null)
			return;
		if (!player.getClan().hasPermissions(player.getUsername(), ClanRank.ADMIN)) {
			player.sendMessage("Only clan admins can edit the clan settings.");
			return;
		}
		player.stopAll();
		player.getTempAttribs().removeI("keywordTempVal");
		player.getVars().setVar(Varp.clan_keyword_temp_cat_varp, 0);
		for (int var : KEYWORD_INDICES.inverse().keySet())
			player.getVars().setVarBit(var, ((Number) player.getClan().getSetting(KEYWORD_INDICES.inverse().get(var))).intValue());
		player.getInterfaceManager().sendInterface(1097);
		player.getPackets().setIFRightClickOps(1097, 193, 0, 100, 0);
		player.getPackets().setIFRightClickOps(1097, 207, 0, 100, 0);
		player.setCloseInterfacesEvent(() -> openSettings(player));
	}

	/** The ten keyword pairs the editor built up in varbits, as one UpdateSettings. */
	private static void saveKeywords(Player player) {
		Clan clan = player.getClan();
		if (clan == null)
			return;
		Map<String, Object> settings = new LinkedHashMap<>();
		for (ClanSetting setting : KEYWORD_INDICES.keySet())
			settings.put(setting.name(), player.getVars().getVarBit(KEYWORD_INDICES.get(setting)));
		ClanRegistry.send(player, new ClanAction.UpdateSettings(settings));
	}

	public static void openForumThreadInterface(Player player) {
		if (player.getClan() == null)
			return;
		if (!player.getClan().hasPermissions(player.getUsername(), ClanRank.ADMIN))
			return;
		player.getInterfaceManager().sendChatBoxInterface(1100);
		player.sendInputForumQFC(qfc -> {
			player.getInterfaceManager().closeChatBoxInterface();
			long val;
			try {
				val = Long.valueOf(qfc, 36);
			} catch(NumberFormatException e) {
				player.sendMessage("Please enter a valid formatted code.");
				return;
			}
			updateSetting(player, ClanSetting.FORUM_QFC, val);
		});
	}

	public static void openClanMottoInterface(Player player) {
		Clan clan = player.getClan();
		if (clan == null)
			return;
		if (!clan.hasPermissions(player.getUsername(), ClanRank.ADMIN))
			return;
		player.stopAll();
		player.sendInputLongText("Please enter your desired clan motto:", motto -> updateSetting(player, ClanSetting.MOTTO, motto));
	}

	public static void openClanMotifInterface(Player player) {
		Clan clan = player.getClan();
		if (clan == null)
			return;
		player.stopAll();
		player.getInterfaceManager().sendInterface(1105);
		player.getPackets().sendRunScript(4400, 72417446);
		player.getPackets().setIFRightClickOps(1105, 66, 0, 116, 0); // top
		player.getPackets().setIFRightClickOps(1105, 63, 0, 116, 0); // button
		player.getVars().setVarBit(Varbit.clan_custom_motif_a_varp, motifValue(player, clan, 0));
		player.getVars().setVarBit(Varbit.clan_custom_motif_b_varp, motifValue(player, clan, 1));
		for (int i = 0; i < MOTIF_COLOR_VARPS.length; i++)
			player.getVars().setVar(MOTIF_COLOR_VARPS[i], motifValue(player, clan, MOTIF_FIRST_COLOR + i));
		player.setCloseInterfacesEvent(() -> openSettings(player));
	}

	public static void openSetMotifColor(Player player, int part) {
		Clan clan = player.getClan();
		if (clan == null)
			return;
		player.getInterfaceManager().sendInterface(1106);
		player.sendInputHSL(color -> {
			player.getTempAttribs().setI(motifKey(MOTIF_FIRST_COLOR + part), color);
			openClanMotifInterface(player);
		});
	}

	public static void setClanMotifTextureInterface(Player player, boolean top, int slot) {
		if (slot > 116)
			return;
		setClanMotifTexture(player, top, slot);
	}

	public static void setClanMotifTexture(Player player, boolean top, int slot) {
		Clan clan = player.getClan();
		if (clan == null || !clan.hasPermissions(player.getUsername(), ClanRank.ADMIN))
			return;
		player.getTempAttribs().setI(motifKey(top ? 0 : 1), slot + 1);
		player.getVars().setVarBit(Varbit.clan_custom_motif_a_varp, motifValue(player, clan, 0));
		player.getVars().setVarBit(Varbit.clan_custom_motif_b_varp, motifValue(player, clan, 1));
	}

	/** Everything the motif editor changed, in one UpdateSettings; nothing changed sends nothing. */
	private static void saveMotif(Player player) {
		Clan clan = player.getClan();
		if (clan == null)
			return;
		Map<String, Object> settings = new LinkedHashMap<>();
		for (int i = 0; i < MOTIF_SETTINGS.length; i++) {
			int pending = player.getTempAttribs().removeI(motifKey(i), -1);
			if (pending == -1)
				continue;
			if (((Number) clan.getSetting(MOTIF_SETTINGS[i])).intValue() == pending)
				continue;
			settings.put(MOTIF_SETTINGS[i].name(), pending);
		}
		if (settings.isEmpty())
			return;
		ClanRegistry.send(player, new ClanAction.UpdateSettings(settings));
	}

	private static String motifKey(int index) {
		return "clanMotif" + index;
	}

	private static int motifValue(Player player, Clan clan, int index) {
		int pending = player.getTempAttribs().getI(motifKey(index), -1);
		if (pending != -1)
			return pending;
		return ((Number) clan.getSetting(MOTIF_SETTINGS[index])).intValue();
	}

	private static void setChannelRank(Player player, boolean talk) {
		ClanRank rank = player.getTempAttribs().getO("permissionRankEditing");
		if (rank == null) {
			player.sendMessage("Please select a rank before setting the " + (talk ? "chat" : "kick") + " ranking.");
			return;
		}
		ClanRegistry.send(player, new ClanAction.SetChannelRanks(talk ? rank : null, talk ? null : rank));
	}

	/** Flips a 0/1 clan setting. */
	private static void toggleSetting(Player player, ClanSetting setting) {
		Clan clan = player.getClan();
		if (clan == null) {
			player.sendMessage("Error getting clan info to edit.");
			return;
		}
		updateSetting(player, setting, ((Number) clan.getSetting(setting)).intValue() == 1 ? 0 : 1);
	}

	/** Asks the lobby to change one setting, unless it already holds that value. */
	private static void updateSetting(Player player, ClanSetting setting, Object value) {
		Clan clan = player.getClan();
		if (clan == null) {
			player.sendMessage("Error getting clan info to edit.");
			return;
		}
		if (clan.getSetting(setting).equals(value))
			return;
		ClanRegistry.send(player, new ClanAction.UpdateSettings(Map.of(setting.name(), value)));
	}

	public static boolean canEditSettings(Player player) {
		if (player.getClan() == null) {
			player.sendMessage("Error getting clan info to edit.");
			return false;
		}
		if (!player.getClan().hasPermissions(player.getUsername(), ClanRank.ADMIN)) {
			player.sendMessage("Only clan administrators can edit clan settings.");
			return false;
		}
		return true;
	}

	public static int[] generateColorGradient(Clan clan, int length, boolean hasTextures) {
		RSColor primary = RSColor.fromHSL(((Number) clan.getSetting(ClanSetting.MOTIF_PRIMARY_COLOR)).intValue());
		RSColor secondary = RSColor.fromHSL(((Number) clan.getSetting(ClanSetting.MOTIF_SECONDARY_COLOR)).intValue());

		int[] colors = new int[length];

		if (hasTextures) {
			colors[0] = ((Number) clan.getSetting(ClanSetting.MOTIF_TOP_COLOR)).intValue();
			colors[1] = ((Number) clan.getSetting(ClanSetting.MOTIF_BOTTOM_COLOR)).intValue();
		}
		int offset = hasTextures ? 2 : 0;
		int luminanceAdjustor = 40 / (colors.length - offset);
		int sizeDiff = (colors.length - offset) / 2;
		boolean initedPrimary = false, initedSecondary = false;
		for (int i = colors.length-1;i >= offset;i--) {
			if (sizeDiff-- > 0) {
				colors[i] = secondary.adjustLuminance(initedSecondary ? luminanceAdjustor : 0).getValue();
				initedSecondary = true;
			} else {
				colors[i] = primary.adjustLuminance(initedPrimary ? luminanceAdjustor : 0).getValue();
				initedPrimary = true;
			}
		}
		return colors;
	}

	public static int[] getMotifTextures(Clan clan) {
		EnumDefinition textures = EnumDefinitions.getEnum(Enum.clan_motif_int2tex);
		return new int[] { textures.getIntValue(clan.getMotifTopIcon()), textures.getIntValue(clan.getMotifBottomIcon()) };
	}

	public static void clanifyObject(Clan clan, GameObject object) {
		if (object.getDefinitions().getModifiedColours() == null)
			return;
		boolean hasTextures = object.getDefinitions().getModifiedTextureColours() != null;
		int[] colors = generateColorGradient(clan, object.getDefinitions().getModifiedColours().length, hasTextures);
		ObjectMeshModifier modifier = object.modifyMesh().addColors(colors);
		if (hasTextures)
			modifier.addTextures(getMotifTextures(clan));
		object.refresh();
	}

	public static NPCBodyMeshModifier clanifyNPC(Clan clan, NPC npc) {
		if (KtExtensionsKt.getModifiedColors(npc.getDefinitions()) == null)
			return null;
		boolean hasTextures = KtExtensionsKt.getModifiedTextures(npc.getDefinitions()) != null;
		int[] colors = generateColorGradient(clan, KtExtensionsKt.getModifiedColors(npc.getDefinitions()).length, hasTextures);
		NPCBodyMeshModifier modifier = npc.modifyMesh().addColors(colors);
		if (hasTextures)
			modifier.addTextures(getMotifTextures(clan));
		return modifier;
	}
}
