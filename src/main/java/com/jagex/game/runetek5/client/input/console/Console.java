package com.jagex.game.runetek5.client.input.console;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.GameState;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.TextUtils;
import com.jagex.game.runetek5.utils.Utils;

/**
 * [J]
 * @author Steven
 *
 */
public class Console {
	
	public static FileOutputStream outputStream;
	public static String[] displayedOutput;
	public static int consoleMessageCount;
	
	public static int anInt2230;
	public static int anInt2222;
	public static int anInt2223;
	
	public static Clipboard clipboard;
	public static String[] scriptArguments;
	public static String commandString = "";
	public static int commandLength;
	public static boolean isOpen;
	
	public static int pausedAtCommandIndex = -1;
	public static int anInt2226;
	public static long pauseEndsAt;
	
	public static int backgroundAlpha;

	public static void open() {
	    if (GameState.isIngame()) {
	        if (displayedOutput == null) {
	            initialize();
	        }
	        isOpen = true;
	        backgroundAlpha = 0;
	    }
	}

	public static void close() {
	    isOpen = false;
	    GameShell.setComponentsOnTop();
	}
	
	public static void initialize() {
	    Console.anInt2230 = FontMetrics.aClass4143438.topPadding + FontMetrics.aClass4143438.bottomPadding + 2;
	    Console.anInt2222 = FontManager.messageboxFont.topPadding + FontManager.messageboxFont.bottomPadding + 2;
	    displayedOutput = new String[500];
	    Arrays.fill(displayedOutput, "");
	    writeLine(LocalizedText.DEVELOPER_CONSOLE_INTRO.translate(client.CURRENT_LANGUAGE));
	}

	public static void executeCommand(boolean save) {
	    if (!Console.commandString.isEmpty()) {
	        Console.writeLine("--> " + Console.commandString);
	        CommandExecutor.executeCommand(Console.commandString, false, save);
	        Console.anInt2226 = 0;
	        if (!save) {
	            Console.commandLength = 0;
	            Console.commandString = "";
	        }
	    }
	}

	public static void writeLine(String command) {
	    if (Console.displayedOutput == null) {
	        Console.initialize();
	    }
	    client.ACTIVE_TIMEZONE_CALENDER.setTime(new Date(Utils.time()));
	    int hour = client.ACTIVE_TIMEZONE_CALENDER.get(Calendar.HOUR_OF_DAY);
	    int minute = client.ACTIVE_TIMEZONE_CALENDER.get(Calendar.MINUTE);
	    int second = client.ACTIVE_TIMEZONE_CALENDER.get(Calendar.SECOND);
	    String timeDisplay = Integer.toString(hour / 10) + hour % 10 + ":" + minute / 10 + minute % 10 + ":" + second / 10 + second % 10;
	    String[] commandParts = Utils.split(command, '\n');
	    for (int index = 0; index < commandParts.length; index++) {
	        if (Console.consoleMessageCount >= 0) {
	            System.arraycopy(Console.displayedOutput, 0, Console.displayedOutput, 1, Console.consoleMessageCount);
	        }
	        Console.displayedOutput[0] = timeDisplay + ": " + commandParts[index];
	        if (Console.outputStream != null) {
	            try {
	                Console.outputStream.write(Utils.characterSequenceToBytearray(Console.displayedOutput[0] + "\n"));
	            } catch (IOException ioe) {
	            	ioe.printStackTrace();
	            }
	        }
	        if (Console.consoleMessageCount < Console.displayedOutput.length - 1) {
	            Console.consoleMessageCount++;
	            if (Console.anInt2223 > 0) {
	                Console.anInt2223++;
	            }
	        }
	    }
	}

	public static void adjustCommandString() {
	    if (Console.anInt2226 > 0) {
	        int arrowCount = 0;
	        for (int index = 0; index < displayedOutput.length; index++) {
	            if (displayedOutput[index].indexOf("--> ") != -1) {
	                ++arrowCount;
	                if (arrowCount == Console.anInt2226) {
	                    Console.commandString = displayedOutput[index].substring(displayedOutput[index].indexOf(62) + 2);
	                    break;
	                }
	            }
	        }
	    } else {
	        Console.commandString = "";
	    }
	}

	public static void processConsoleInput() {
	    if (Console.backgroundAlpha < 102) {
	        Console.backgroundAlpha += 6;
	    }

	    if (Console.pausedAtCommandIndex != -1 && Console.pauseEndsAt < Utils.time()) {
	        for (int argIndex = Console.pausedAtCommandIndex; argIndex < Console.scriptArguments.length; argIndex++) {
	            if (Console.scriptArguments[argIndex].startsWith("pause")) {
	                int pauseSeconds = 5;
	                try {
	                    pauseSeconds = Integer.parseInt(Console.scriptArguments[argIndex].substring(6));
	                } catch (Exception ignored) {
	                	// NOOP
	                }
	                writeLine("Pausing for " + pauseSeconds + " seconds...");
	                Console.pausedAtCommandIndex = argIndex + 1;
	                Console.pauseEndsAt = Utils.time() + (pauseSeconds * 1000L);
	                return;
	            }
	            Console.commandString = Console.scriptArguments[argIndex];
	            executeCommand(false);
	        }
	        Console.pausedAtCommandIndex = -1;
	    }
	    if (client.anInt7191 != 0) {
	        anInt2223 -= client.anInt7191 * 5;
	        if (anInt2223 >= consoleMessageCount) {
	            anInt2223 = consoleMessageCount - 1;
	        }
	        if (anInt2223 < 0) {
	            anInt2223 = 0;
	        }
	        client.anInt7191 = 0;
	    }
	    for (int index = 0; index < client.keyCachePointerPoss; index++) {
	        KeyRecord keyRecord = client.KEYS_PRESSED[index];
	        int code = keyRecord.getCode();
	        char character = keyRecord.getCharacter();
	        int flags = keyRecord.getFlags();
	        if (code == 84) {
	            executeCommand(false);
	        }
	        if (code == 80) {
	            executeCommand(true);
	        } else if (code == 66 && (flags & 0x4) != 0) {
	            if (clipboard != null) {
	                String line = "";
	                for (int lineIndex = displayedOutput.length - 1; lineIndex >= 0; --lineIndex) {
	                    if (displayedOutput[lineIndex] != null && !displayedOutput[lineIndex].isEmpty()) {
	                        line = line + displayedOutput[lineIndex] + '\n';
	                    }
	                }
	                clipboard.setContents(new StringSelection(line), null);
	            }
	        } else if (code == 67 && (flags & 0x4) != 0) {
	            if (clipboard != null) {
	                try {
	                    Transferable transferable = clipboard.getContents(null);
	                    if (transferable != null) {
	                        String data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
	                        String[] script = Utils.split(data, '\n');
	                        Console.runscript(script);
	                    }
	                } catch (Exception ignored) {
	                	// NOOP
	                }
	            }
	        } else if (code == 85 && Console.commandLength > 0) {
	            Console.commandString = Console.commandString.substring(0, Console.commandLength - 1) + Console.commandString.substring(Console.commandLength);
	            Console.commandLength--;
	        } else if (code == 101 && Console.commandLength < Console.commandString.length()) {
	            Console.commandString = Console.commandString.substring(0, Console.commandLength) + Console.commandString.substring(Console.commandLength + 1);
	        } else if (code == 96 && Console.commandLength > 0) {
	            Console.commandLength--;
	        } else if (code == 97 && Console.commandLength < Console.commandString.length()) {
	            Console.commandLength++;
	        } else if (code == 102) {
	            Console.commandLength = 0;
	        } else if (code == 103) {
	            Console.commandLength = Console.commandString.length();
	        } else if (code == 104 && Console.anInt2226 < displayedOutput.length) {
	            Console.anInt2226++;
	            adjustCommandString();
	            Console.commandLength = Console.commandString.length();
	        } else if (code == 105 && Console.anInt2226 > 0) {
	            Console.anInt2226--;
	            adjustCommandString();
	            Console.commandLength = Console.commandString.length();
	        } else if (TextUtils.isAlphabeticalOrNumeric(character) || "\\/.:, _-+[]~@".indexOf(character) != -1) {
	            Console.commandString = Console.commandString.substring(0, Console.commandLength) + client.KEYS_PRESSED[index].getCharacter() + Console.commandString.substring(Console.commandLength);
	            Console.commandLength++;
	        }
	    }
	    client.keyCachePointerPoss = 0;
	    client.maximumHeldKeys = 0;
	    GameShell.setComponentsOnTop();
	}


	public static boolean isOpen() {
	    return isOpen;
	}

	public static void runscript(String[] arguments) {
	    if (arguments.length > 1) {
	        for (int index = 0; index < arguments.length; index++) {
	            if (arguments[index].startsWith("pause")) {
	                int waitDuration = 5;
	                try {
	                    waitDuration = Integer.parseInt(arguments[index].substring(6));
	                } catch (Exception exception) {
	                	// NOOP
	                }
	                writeLine("Pausing for " + waitDuration + " seconds...");
	                Console.scriptArguments = arguments;
	                Console.pausedAtCommandIndex = index + 1;
	                Console.pauseEndsAt = Utils.time() + (waitDuration * 1000L);
	                break;
	            }
	            Console.commandString = arguments[index];
	            executeCommand(false);
	        }
	    } else {
	        Console.commandString += arguments[0];
	        Console.commandLength += arguments[0].length();
	    }
	}

	public static void renderConsole(AbstractToolkit toolkit) {
	    toolkit.setClipping(0, 0, GameShell.screenCanvasWidth, 350);
	    toolkit.drawFilledRectangle(0, 0, GameShell.screenCanvasWidth, 350, Console.backgroundAlpha << 24 | 0x332277, 1);
	    int i_2 = 350 / anInt2222;
	    if (consoleMessageCount > 0) {
	        int i_3 = 346 - anInt2222 - 4;
	        int i_4 = i_2 * i_3 / (i_2 + consoleMessageCount - 1);
	        int i_5 = 4;
	        if (consoleMessageCount > 1) {
	            i_5 += (i_3 - i_4) * (consoleMessageCount - 1 - anInt2223) / (consoleMessageCount - 1);
	        }
	        toolkit.drawFilledRectangle(GameShell.screenCanvasWidth - 16, i_5, 12, i_4, Console.backgroundAlpha << 24 | 0x332277, 2);
	        for (int i_6 = anInt2223; i_6 < i_2 + anInt2223 && i_6 < consoleMessageCount; i_6++) {
	            String[] arr_7 = Utils.split(displayedOutput[i_6], '\b');
	            int i_8 = (GameShell.screenCanvasWidth - 8 - 16) / arr_7.length;
	            for (int i_9 = 0; i_9 < arr_7.length; i_9++) {
	                int i_10 = i_9 * i_8 + 8;
	                toolkit.setClipping(i_10, 0, i_8 + i_10 - 8, 350);
	                FontManager.debugOverlayFont.renderPlain(Console.formatCommand(arr_7[i_9]), i_10, 350 - anInt2230 - 2 - FontManager.messageboxFont.bottomPadding - (i_6 - anInt2223) * anInt2222, -1, -16777216);
	            }
	        }
	    }
	    Renderers.FONT_RENDERER.renderLeftAligned("727 1", GameShell.screenCanvasWidth - 25, 330, -1, -16777216);
	    toolkit.setClipping(0, 0, GameShell.screenCanvasWidth, 350);
	    toolkit.drawOpaqueHorizontalLine(0, 350 - anInt2230, GameShell.screenCanvasWidth, -1);
	    MiniMenu.minimenuHeader.renderPlain("--> " + Console.formatCommand(Console.commandString), 10, 350 - FontMetrics.aClass4143438.bottomPadding - 1, -1, -16777216);
	    if (GameShell.appletHasFocus) {
	        int lineColor = -1;
	        if (client.cycles % 30 > 15) {
	            lineColor = 16777215;
	        }
	        toolkit.drawOpaqueVerticalLine(10 + FontMetrics.aClass4143438.getWidthNoSprites("--> " + Console.formatCommand(Console.commandString).substring(0, Console.commandLength)), 350 - FontMetrics.aClass4143438.bottomPadding - 11, 12, lineColor);
	    }
	    toolkit.resetClipping();
	}

	public static void setConsoleText(String string_0) {
	    Console.commandString = string_0;
	    Console.commandLength = Console.commandString.length();
	}

	public static void shutdown() {
	    if (outputStream != null) {
	        try {
	            outputStream.close();
	        } catch (IOException ignored) {
	        	// NOOP
	        }
	    }
	    outputStream = null;
	}

	public static String formatCommand(String command) {
	    String prefix = null;
	    int arrowIndex = command.indexOf("--> ");
	    if (arrowIndex >= 0) {
	        prefix = command.substring(0, arrowIndex + 4);
	        command = command.substring(arrowIndex + 4);
	    }
	
	    if (command.startsWith("directlogin ")) {
	        int directLoginIndex = command.indexOf(' ', "directlogin ".length());
	        if (directLoginIndex >= 0) {
	            int end = command.length();
	            command = command.substring(0, directLoginIndex) + " ";
	
	            for (int start = directLoginIndex + 1; start < end; start++) {
	                command += "*";
	            }
	        }
	    }
	
	    return prefix != null ? prefix + command : command;
	}
}