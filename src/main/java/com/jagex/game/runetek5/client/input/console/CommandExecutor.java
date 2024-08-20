package com.jagex.game.runetek5.client.input.console;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.imageio.ImageIO;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.network.BufferedConnectionContext;
import com.jagex.game.runetek5.network.WorldConnection;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.rendering.RenderInfo;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.CollisionMap;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.tools.DebugDisplay;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.FileOperations;
import com.jagex.game.runetek5.utils.DirectoryPicker;
import com.jagex.game.runetek5.utils.Utils;
import com.jagex.game.runetek5.vardomain.VarProviders;

public class CommandExecutor {
	
	public static boolean teleportCommandRequested;
	public static int teleportCommandPositionZ;
	public static int teleportCommandPositionX;
	public static int teleportCommandPositionY;

	public static void executeCommand(String command, boolean bool1, boolean save) {
	    try {
	        label381:
	        {
	            if (!"commands".equalsIgnoreCase(command) && !"help".equalsIgnoreCase(command)) {
	                if ("cls".equalsIgnoreCase(command)) {
	                    Console.consoleMessageCount = 0;
	                    Console.anInt2223 = 0;
	                    return;
	                }
	                if ("displayfps".equalsIgnoreCase(command)) {
	                    client.DRAW_DEBUG = !client.DRAW_DEBUG;
	                    if (client.DRAW_DEBUG) {
	                        Console.writeLine("FPS on");
	                    } else {
	                        Console.writeLine("FPS off");
	                    }
	                    return;
	                }
	                if (!"renderer".equals(command)) {
	                    if ("heap".equals(command)) {
	                        Console.writeLine("Heap: " + GameShell.maxMemory + "MB");
	                        return;
	                    }
	                    if ("getcamerapos".equalsIgnoreCase(command)) {
	                        CoordGrid base = Resource.mapRegion.getBaseCoordinates();
	                        Console.writeLine("Pos: " + PlayerUpdate.MY_PLAYER.plane + "," + ((Camera.cameraPositionAbsoluteX >> 9) + base.x >> 6) + "," + ((Camera.cameraPositionAbsoluteY >> 9) + base.y >> 6) + "," + ((Camera.cameraPositionAbsoluteX >> 9) + base.x & 0x3f) + "," + ((Camera.cameraPositionAbsoluteY >> 9) + base.y & 0x3f) + " Height: " + (MapRegion.tileheightForAbsolutePosition(Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER.plane) - Camera.cameraPositionAbsoluteZ));
	                        Console.writeLine("Look: " + PlayerUpdate.MY_PLAYER.plane + "," + (base.x + Camera.lookX >> 6) + "," + (base.y + Camera.lookY >> 6) + "," + (base.x + Camera.lookX & 0x3f) + "," + (base.y + Camera.lookY & 0x3f) + " Height: " + (MapRegion.tileheightForAbsolutePosition(Camera.lookX, Camera.lookY, PlayerUpdate.MY_PLAYER.plane) - Camera.anInt1527));
	                        return;
	                    }
	                    break label381;
	                }
	                RenderInfo renderInfo = Renderers.activeToolkit.getRenderInfo();
	                Console.writeLine("Toolkit ID: " + PreferenceManager.preferences.activeToolkit.getValue());
	                Console.writeLine("Vendor: " + renderInfo.vendor);
	                Console.writeLine("Name: " + renderInfo.name);
	                Console.writeLine("Version: " + renderInfo.version);
	                Console.writeLine("Device: " + renderInfo.device);
	                Console.writeLine("Driver Version: " + renderInfo.driverVersion);
	                return;
	            }
	            Console.writeLine("commands - This command");
	            Console.writeLine("cls - Clear console");
	            Console.writeLine("displayfps - Toggle FPS and other information");
	            Console.writeLine("renderer - Print graphics renderer information");
	            Console.writeLine("heap - Print java memory information");
	            Console.writeLine("getcamerapos - Print location and direction of camera for use in bug reports");
	            return;
	        }
	    } catch (Exception exception) {
	        Console.writeLine(LocalizedText.ERROR_EXECUTING_COMMAND.translate(client.CURRENT_LANGUAGE));
	        return;
	    }
	    if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE || client.PLAYER_RIGHTS >= 2) {
	        try {
	            if ("wm1".equalsIgnoreCase(command)) {
	                WindowMode.requestWindowMode(1, -1, -1, false);
	                if (WindowMode.getWindowMode() == 1) {
	                    Console.writeLine("Success");
	                } else {
	                    Console.writeLine("Failure");
	                }
	                return;
	            }
	            if ("wm2".equalsIgnoreCase(command)) {
	                WindowMode.requestWindowMode(2, -1, -1, false);
	                if (WindowMode.getWindowMode() == 2) {
	                    Console.writeLine("Success");
	                } else {
	                    Console.writeLine("Failure");
	                }
	                return;
	            }
	            if (WindowMode.fullScreenSupported && "wm3".equalsIgnoreCase(command)) {
	                WindowMode.requestWindowMode(3, 1024, 768, false);
	                if (WindowMode.getWindowMode() == 3) {
	                    Console.writeLine("Success");
	                } else {
	                    Console.writeLine("Failure");
	                }
	                return;
	            }
	            if (command.startsWith("clipmask")) {
	                Console.writeLine("(" + PlayerUpdate.MY_PLAYER.endX + ", " + PlayerUpdate.MY_PLAYER.endZ + "): " + Resource.mapRegion.getCollisionMap(PlayerUpdate.MY_PLAYER.plane).collisionFlags[PlayerUpdate.MY_PLAYER.endX + 1][PlayerUpdate.MY_PLAYER.endZ + 1] + " - " + CollisionMap.getFlags(Resource.mapRegion.getCollisionMap(PlayerUpdate.MY_PLAYER.plane).collisionFlags[PlayerUpdate.MY_PLAYER.endX + 1][PlayerUpdate.MY_PLAYER.endZ + 1]));
	            }
	            if (command.startsWith("dumpitems")) {
	                for (int itemId = 0; itemId < 24806; itemId++) {
	                    int[] pixels = TypeLists.OBJ.list(itemId).getSpritePixels(Renderers.GPU_ITEM_RENDERER, Renderers.activeToolkit, 1, 1, -13623264, false, Component.StackMode.NONUM, Renderers.FONT_RENDERER, PlayerUpdate.MY_PLAYER.playerAppearance);
	                    try {
	                    	int dimension = (int) Math.sqrt(pixels.length) - 1;
	                        BufferedImage itemImage = new BufferedImage(36, 32, 2);
	                        itemImage.setRGB(0, 0, dimension, dimension, pixels, 0, dimension);
	                        for (int x = 0; x < dimension; x++) {
	                            for (int y = 0; y < dimension; y++) {
	                                itemImage.setRGB(y, x, pixels[x * 36 + y]);
	                            }
	                        }
	                        File imageFile = new File("./items/" + itemId + ".png");
	                        ImageIO.write(itemImage, "png", imageFile);
	                    } catch (Exception exception) {
	                        exception.printStackTrace();
	                    }
	                }
	                return;
	            }
	            if (command.startsWith("setlobby ")) {
	            	int port = -1;
	                command = command.substring(9);
	                int prefix = command.indexOf(32);
	                if (prefix == -1) {
	                    Console.writeLine("Failure");
	                    return;
	                }
	                try {
	                    port = Integer.parseInt(command.substring(0, prefix));
	                } catch (NumberFormatException numberFormatException) {
	                	// NOOP
	                }
	                if (port == -1) {
	                    Console.writeLine("Failure");
	                    return;
	                }
	                String host = command.substring(prefix + 1).trim() + ".runescape.com";
	                WorldConnection.setLobbyConnection(host, port);
	                Console.writeLine("Success");
	                return;
	            }
	            if ("tk0".equalsIgnoreCase(command)) { // Safe mode
	                client.loadAssets(0, false);
	                if (PreferenceManager.preferences.activeToolkit.getValue() == 0) {
	                    Console.writeLine("Success");
	                    PreferenceManager.preferences.setValue(PreferenceManager.preferences.toolkit, 0);
	                    PreferenceManager.savePreferences();
	                    client.preferencesSavedOnServer = false;
	                } else {
	                    Console.writeLine("Failure");
	                }
	                return;
	            }
	            if ("tk1".equalsIgnoreCase(command)) { // OpenGL - no shaders
	                client.loadAssets(1, false);
	                if (PreferenceManager.preferences.activeToolkit.getValue() == 1) {
	                    Console.writeLine("Success");
	                    PreferenceManager.preferences.setValue(PreferenceManager.preferences.toolkit, 1);
	                    PreferenceManager.savePreferences();
	                    client.preferencesSavedOnServer = false;
	                } else {
	                    Console.writeLine("Failure");
	                }
	                return;
	            }
	            if ("tk2".equalsIgnoreCase(command)) { // Sw3d
	                client.loadAssets(2, false);
	                if (PreferenceManager.preferences.activeToolkit.getValue() == 2) {
	                    Console.writeLine("Success");
	                    PreferenceManager.preferences.setValue(PreferenceManager.preferences.toolkit, 2);
	                    PreferenceManager.savePreferences();
	                    client.preferencesSavedOnServer = false;
	                } else {
	                    Console.writeLine("Failure");
	                }
	                return;
	            }
	            if ("tk3".equalsIgnoreCase(command)) { // DirectX
	                client.loadAssets(3, false);
	                if (PreferenceManager.preferences.activeToolkit.getValue() == 3) {
	                    Console.writeLine("Success");
	                    PreferenceManager.preferences.setValue(PreferenceManager.preferences.toolkit, 3);
	                    PreferenceManager.savePreferences();
	                    client.preferencesSavedOnServer = false;
	                } else {
	                    Console.writeLine("Failure");
	                }
	                return;
	            }
	            if ("tk5".equalsIgnoreCase(command)) { // OpenGL - with shaders
	                client.loadAssets(5, false);
	                if (PreferenceManager.preferences.activeToolkit.getValue() == 5) {
	                    Console.writeLine("Success");
	                    PreferenceManager.preferences.setValue(PreferenceManager.preferences.toolkit, 5);
	                    PreferenceManager.savePreferences();
	                    client.preferencesSavedOnServer = false;
	                } else {
	                    Console.writeLine("Failure");
	                }
	                return;
	            }
	            if ("clientdrop".equalsIgnoreCase(command)) {
	                if (client.gameState == GameState.LOGGED_INGAME) {
	                    WorldConnection.dropClient();
	                } else if (client.gameState == GameState.LOADING_PLEASE_WAIT) {
	                    client.GAME_CONNECTION_CONTEXT.dropped = true;
	                }
	                return;
	            }
	            if ("breakcon".equalsIgnoreCase(command)) {
	                BufferedConnectionContext[] connectionContexts = client.connectionContexts;
	                for (int index = 0; index < connectionContexts.length; index++) {
	                    BufferedConnectionContext context = connectionContexts[index];
	                    if (context.getConnection() != null) {
	                        context.getConnection().drop();
	                    }
	                }
	                Resource.remoteFileRequesterJs5.dropConnection();
	                return;
	            }
	            if (command.startsWith("getclientvarpbit")) {
	            	int varpbitId = Integer.parseInt(command.substring(17));
	                Console.writeLine("varpbit=" + VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(varpbitId));
	                return;
	            }
	            if (command.startsWith("getclientvarp")) {
	            	int varpId = Integer.parseInt(command.substring(14));
	                Console.writeLine("varp=" + VarProviders.PLAYER_VAR_PROVIDER.getVarValue(varpId));
	                return;
	            }
	            if (command.startsWith("directlogin")) {
	            	String[] commandParts = Utils.split(command.substring(12), ' ');
	                if (commandParts.length == 2) {
	                    LoginManager.directLogin(commandParts[0], commandParts[1]);
	                }
	                return;
	            }
	            if (command.startsWith("snlogin ")) {
	            	String[] commandParts = Utils.split(command.substring(8), ' ');
	                int state = Integer.parseInt(commandParts[0]);
	                LoginManager.snLogin(state);
	                return;
	            }
	            if (command.startsWith("setoutput ")) {
	            	File file = new File(command.substring(10));
	                if (file.exists()) {
	                    file = new File(command.substring(10) + "." + Utils.time() + ".log");
	                    if (file.exists()) {
	                        Console.writeLine("file already exists!");
	                        return;
	                    }
	                }
	                if (Console.outputStream != null) {
	                    Console.outputStream.close();
	                    Console.outputStream = null;
	                }
	                try {
	                    Console.outputStream = new FileOutputStream(file);
	                } catch (FileNotFoundException notFound) {
	                    Console.writeLine("Could not create " + file.getName());
	                } catch (SecurityException securityException) {
	                    Console.writeLine("Cannot write to " + file.getName());
	                }
	                return;
	            }
	            if ("closeoutput".equals(command)) {
	                if (Console.outputStream != null) {
	                    Console.outputStream.close();
	                }
	                Console.outputStream = null;
	                return;
	            }
	            if (command.startsWith("runscript ")) {
	            	File file = new File(command.substring(10));
	                if (!file.exists()) {
	                    Console.writeLine("No such file");
	                    return;
	                }
	                byte[] fileData = FileOperations.getDataFromFile(file);
	                if (fileData == null) {
	                    Console.writeLine("Failed to read file");
	                    return;
	                }
	                String[] scriptLines = Utils.split(Utils.formatScript(Utils.decodeWithUnicodeTable(fileData), '\r', ""), '\n');
	                Console.runscript(scriptLines);
	            }
	            /*Added*/
	            if (command.startsWith("debug ")) {
	            	DebugDisplay.applyDebugOpcode(Integer.parseInt(command.substring(6)));
	            	return;
	            }
	            if (command.startsWith("checknpprio")) {
	            	for (int id = 0; id < 100_000; id++) {
	            		NPCType npctype = TypeLists.NPC.list(id);
	            		if(npctype != null && npctype.highPriority) {
	            			Console.writeLine("NPC: "+"["+id+"] "+npctype.name);
	            		}
	            	}
	            	Console.writeLine("Done checking: "+100_000+ "npcs");
	            	return;
	            }
	            if (command.startsWith("npctoggle")) {
	            	for (int id = 0; id < 100_000; id++) {
	            		NPCType npctype = TypeLists.NPC.list(id);
	            		if(npctype != null && !npctype.delayMovement) {
	            			npctype.delayMovement = !npctype.delayMovement;
	            		}
	            	}
	            	Console.writeLine("Done toggling to: ");
	            	return;
	            }
	            if (command.startsWith("checklocop")) {
	            	for (int id = 0; id < 100_000; id++) {
	            		ObjectType locType = TypeLists.LOC.list(id);
	            		if (locType.requiresTextures) {
	            			Console.writeLine("LOC: "+id);
	            		}
	            	}
	            	return;
	            }
	            if (command.startsWith("checknpcop")) {
	            	Console.writeLine("Executing "+command);
	            	for (int id = 0; id < 100_000; id++) {
	            		NPCType npc = TypeLists.NPC.list(id);
	            		/*if (npc.delay_movement) {
	            			Console.write_line("NPC: "+id+" with name "+npc.name+" - has aBool4894");
	            		}*/
	            		if (npc.npcBasId > 0) {
	            			BasType basType = TypeLists.BAS.list(npc.npcBasId);
	            			if (basType.unused != -1) {
	            				Console.writeLine("NPC: "+id+" with name "+npc.name+" - has anInt2826: "+basType.unused);
	            			}
	            		}
	            	}
	            	return;
	            }
	            if (command.startsWith("effect") || command.startsWith("e")) {
	            	int effect = Integer.parseInt(command.length() > 5 ? command.substring(7) : command.substring(2));
	                Loader.MATERIAL_EFFECTS_DISABLED[effect] =! Loader.MATERIAL_EFFECTS_DISABLED[effect];
	                Console.writeLine("Set effect: "+effect+" to "+(Loader.MATERIAL_EFFECTS_DISABLED[effect] ? "disabled" : "enabled"));
	                return;
	            }
	            if (command.startsWith("pickdir")) {
	            	new DirectoryPicker("FilePicker");
	            	return;
	            }
	            /**/
	            if (client.gameState == GameState.LOGGED_INGAME) {
	                TcpPacket clientPacket = PacketEncoder.createPacket(ClientProt.COMMAND, client.GAME_CONNECTION_CONTEXT.isaac);
	                clientPacket.buffer.writeByte(command.length() + 3);
	                clientPacket.buffer.writeByte(bool1 ? 1 : 0);
	                clientPacket.buffer.writeByte(save ? 1 : 0);
	                clientPacket.buffer.writeString(command);
	                client.GAME_CONNECTION_CONTEXT.queuePacket(clientPacket);
	            }
	        } catch (Exception exception) {
	            Console.writeLine(LocalizedText.ERROR_EXECUTING_COMMAND.translate(client.CURRENT_LANGUAGE));
	            return;
	        }
	    }
	    if (client.gameState != GameState.LOGGED_INGAME) {
	        Console.writeLine(LocalizedText.UNKNOWN_DEV_COMMAND.translate(client.CURRENT_LANGUAGE) + command);
	    }
	}

	public static void executeTeleportCommand(int plane, int x, int y) {
	    String commandString = "tele " + plane + "," + (x >> 6) + "," + (y >> 6) + "," + (x & 0x3f) + "," + (y & 0x3f);
	    executeCommand(commandString, true, false);
	}

}
