package com.rs.game.content.commands.normal;

import org.darkan.core.cache.ItemDefinitions;
import org.darkan.core.cache.NPCDefinitions;
import com.rs.db.WorldDB;
import com.rs.engine.command.Command;
import com.rs.engine.command.Commands;
import com.rs.game.ge.Offer;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.model.Rights;
import org.darkan.core.Logger;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.utils.drop.DropTable;

@PluginEventHandler
public class Normal {

    @ServerStartupEvent
    public static void loadCommands() {

        Commands.add(Rights.PLAYER, "commandlist,commands", "Displays all the commands the player has permission to use.", (p, args) -> {
            p.getPackets().setIFText(275, 1, "Commands List");
            int componentId = 10;

            for (int rights = p.getRights().ordinal(); rights >= 0; rights--) {
                p.getPackets().setIFText(275, componentId++, "<u><col=FF0000><shad=000000>" + Utils.formatPlayerNameForDisplay(Rights.values()[rights].name()) + " Commands</shad></col></u>");
                for (Command c : Commands.getCommands(Rights.values()[rights])) {
                    if (componentId > 288)
                        break;
                    p.getPackets().setIFText(275, componentId++, "::" + c.getUsage());
                }
            }

            p.getPackets().sendRunScript(1207, componentId - 10);
            p.getInterfaceManager().sendInterface(275);
        });

        Commands.add(Rights.PLAYER, "drops [npcId/droptablename numberKilled]", "Emulates a number of NPC kills and displays the collected loot.", (p, args) -> {
            int npcId = -1;
            try {
                npcId = Integer.valueOf(args[0]);
            } catch(Throwable e) {

            }
            int npcAmount = Integer.valueOf(args[1].replace("k", "000"));
            if (npcId != -1 && (npcId < 0 || npcId > Utils.getNPCDefinitionsSize()))
                return;
            if ((npcAmount < 0 || npcAmount > 50000) && !p.hasRights(Rights.DEVELOPER)) {
                p.sendMessage("You can only see drops for up to 50000 NPCs.");
                return;
            }
            if (npcId != -1)
                NPC.displayDropsFor(p, npcId, npcAmount);
            else
                DropTable.displayDropsFor(p, args[0], npcAmount);
        });

        Commands.add(Rights.PLAYER, "buyoffers", "Displays all buy offers currently active in the Grand Exchange.", (p, args) -> WorldDB.getGE().getAllOffersOfType(false, offers -> {
            p.getPackets().sendRunScript(1207, offers.size());
            p.getInterfaceManager().sendInterface(275);
            p.getPackets().setIFText(275, 1, "Grand Exchange Buy Offers");
            int num = 10;
            for (Offer offer : offers) {
                if (num > 288)
                    break;
                p.getPackets().setIFText(275, num, "[" + Utils.formatPlayerNameForDisplay(offer.getOwner()) + "]: " + offer.amountLeft() + " " + ItemDefinitions.getDefs(offer.getItemId()).getName() + " for " + offer.getPrice() + " ea");
                num++;
            }
        }));

        Commands.add(Rights.PLAYER, "selloffers", "Displays all sell offers currently active in the Grand Exchange.", (p, args) -> WorldDB.getGE().getAllOffersOfType(true, offers -> {
            p.getPackets().sendRunScript(1207, offers.size());
            p.getInterfaceManager().sendInterface(275);
            p.getPackets().setIFText(275, 1, "Grand Exchange Sell Offers");
            int num = 10;
            for (Offer offer : offers) {
                if (num > 288)
                    break;
                p.getPackets().setIFText(275, num, "[" + Utils.formatPlayerNameForDisplay(offer.getOwner()) + "]: " + offer.amountLeft() + " " + ItemDefinitions.getDefs(offer.getItemId()).getName() + " for " + offer.getPrice() + " ea");
                num++;
            }
        }));

        Commands.add(Rights.PLAYER, "searchnpc,searchn,findnpc,getnpcid [npc name]", "Displays all NPC ids containing the query searched.", (p, args) -> {
            p.getPackets().sendDevConsoleMessage("Searching for npcs containing name: " + Utils.concat(args));
            for (int i = 0; i < Utils.getNPCDefinitionsSize(); i++)
                if (NPCDefinitions.getDefs(i).getName().toLowerCase().contains(Utils.concat(args).toLowerCase()))
                    p.getPackets().sendDevConsoleMessage("Result found: " + i + " - " + NPCDefinitions.getDefs(i).getName() + " (" + NPCDefinitions.getDefs(i).getCombatLevel() + ")");
        });

//        Commands.add(Rights.PLAYER, "yell,shout [text]", "Will broadcast your message to the whole server.", (p, args) -> {
//            Commands.sendYell(p, Utils.fixChatMessage(Utils.concat(args)), false);
//        });
//
//        Commands.add(Rights.PLAYER, "hideyell", "Hides yell from your chat box.", (p, args) -> {
//            p.setYellOff(!p.isYellOff());
//            p.sendMessage("You have turned " + (p.isYellOff() ? "off" : "on") + " yell.");
//        });

        Commands.add(Rights.PLAYER, "ping", "Checks your ping if you have gotten it recently.", (p, args) -> p.sendMessage("Ping: " + p.getNSV().getI("ping", -1)));

        Commands.add(Rights.PLAYER, "organizetab", "Organizes the currently open tab of your bank by item id", (p, args) -> p.sendOptionDialogue("Organize tab " + p.getBank().getCurrentTab() + "?", ops -> {
            ops.add("Yes, organize it.", () -> p.getBank().sortCurrentTab());
            ops.add("Nevermind.");
        }));

        Commands.add(Rights.PLAYER, "dunginfo", "Shows dungeon seed", (p, args) -> {
            try {
                int floor = p.getDungManager().getParty().getFloor();
                long seed = p.getDungManager().getParty().getDungeon().getDungeon().getSeed();
                p.getPackets().sendGameMessage("floor: " + String.valueOf(floor));
                p.getPackets().sendGameMessage("seed: " + String.valueOf(seed));
                p.getPackets().sendGameMessage("difficulty: " + p.getDungManager().getParty().getDificulty());
                p.getPackets().sendGameMessage("size: " + p.getDungManager().getParty().getSize());
                p.getPackets().sendGameMessage("complexity: " + p.getDungManager().getParty().getComplexity());

                Logger.debug(Normal.class, "dungInfo", "Dungeon Info:");
                Logger.debug(Normal.class, "dungInfo", "floor: " + String.valueOf(floor));
                Logger.debug(Normal.class, "dungInfo", "seed: " + String.valueOf(seed));
                Logger.debug(Normal.class, "dungInfo", "difficulty: " + p.getDungManager().getParty().getDificulty());
                Logger.debug(Normal.class, "dungInfo", "size: " + p.getDungManager().getParty().getSize());
                Logger.debug(Normal.class, "dungInfo", "complexity: " + p.getDungManager().getParty().getComplexity());
            } catch (NullPointerException e) {
                p.getPackets().sendGameMessage("You need to be in a dungeon");
            }
        });
    }

}
