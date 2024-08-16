package com.jagex.game.runetek5.config.rsinterface.cs2;

import java.util.ArrayList;

import com.jagex.game.runetek5.cache.node.CacheableNodeQueue;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.config.quickchat.QuickChatMessage;
import com.jagex.game.runetek5.config.rsinterface.HookRequest;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.entity.grounditem.GroundItem;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel;
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.trigger.ClientTriggerType;
import com.jagex.game.runetek5.utils.DirectoryPicker;

public class CS2Executor {

    public static final CacheableNodeQueue CS2_CACHE = new CacheableNodeQueue(128);
    public static int CURRENT_CS2_EXEC_IDX;
    public static final ArrayList<CS2Executor> CS2_EXECUTORS = new ArrayList<CS2Executor>();
    static int anInt5904;
    public static String aString5897;
    public static boolean aBool5898;
    static final int[] anIntArray5900 = new int[3];
    public static int anInt5906;

    int[] intLocals;
    long[] longLocals;
    public int anInt7004;
    ClientScriptOpCode[] operations;
    public PathingEntity currentEntity;
    DirectoryPicker filePicker;
    QuickChatMessage currentQuickChatMessage;
    ClanSettings currentClanSettings;
    ClanChannel clanChannel;
    public GameObject currentLocation;
    public GroundItem currentGroundItem;
    Object[] objectLocals;
    int[] intOpValues;
    CS2Script current;
    boolean aBool7022;
    final int[] globalArrayLengths = new int[5];
    final int[][] globalArrays = new int[5][5000];

    public final int[] intStack = new int[1000];
    public int intStackPtr;
    final long[] longStack = new long[1000];
    int longStackPtr;
    final Object[] stringStack = new Object[1000];
    int stringStackPtr;

    int returnValuesPtr;
    final CS2ReturnValue[] returnValues = new CS2ReturnValue[50];
    final CS2Interface hookedInterface1 = new CS2Interface();
    final CS2Interface hookedInterface2 = new CS2Interface();
    int hookRequestCount;
    int instrPtr = -1;

    public static void logError(String errorString) {
        System.out.println("\u001B[31m" + "CS2 Error: " + CS2Executor.formatCs2Error(errorString, " ", "\n     ") + "\u001B[0m");
    }

    public static void execute_trigger(ClientTriggerType triggerType, int scriptId, int i_2, CS2Executor cs2executor3) {
        CS2Script cs2script5 = CS2Script.getScript(triggerType, scriptId, i_2);
        if (cs2script5 == null) {
            decrementCS2ExecIdx();
        } else {
            cs2executor3.intLocals = new int[cs2script5.intLocalsCount];
            cs2executor3.objectLocals = new Object[cs2script5.stringLocalsCount];
            if (cs2script5.triggerType != ClientTriggerType.MEC_MOUSE_ENTER && cs2script5.triggerType != ClientTriggerType.MEC_MOUSE_OVER && cs2script5.triggerType != ClientTriggerType.MEC_MOUSE_LEAVE) {
                if (cs2script5.triggerType == ClientTriggerType.IF_PLAYER_CYCLE) {
                    cs2executor3.intLocals[0] = cs2executor3.anInt7004;
                }
            } else {
                int i_6 = 0;
                int i_7 = 0;
                if (WorldMap.worldmapComponent != null) {
                    i_6 = WorldMap.worldmapComponent.x;
                    i_7 = WorldMap.worldmapComponent.y;
                }
                cs2executor3.intLocals[0] = MouseRecord.recorder.getMouseX() - i_6;
                cs2executor3.intLocals[1] = MouseRecord.recorder.getMouseY() - i_7;
            }
            executeScript(cs2script5, 200000, cs2executor3);
        }
    }

    public static void executeScript(CS2Script cs2script0, int stackLimit, CS2Executor cs2executor2) {
        cs2executor2.intStackPtr = 0;
        cs2executor2.stringStackPtr = 0;
        cs2executor2.instrPtr = -1;
        cs2executor2.current = cs2script0;
        cs2executor2.operations = cs2executor2.current.operations;
        cs2executor2.intOpValues = cs2executor2.current.intOpValues;
        ClientScriptOpCode cs2opinfo4 = null;
        cs2executor2.returnValuesPtr = 0;
        try {
            try {
                anInt5904 = 0;
                while (true) {
                    ++anInt5904;
                    if (anInt5904 > stackLimit) {
                        throw new RuntimeException("");
                    }
                    cs2opinfo4 = cs2executor2.operations[++cs2executor2.instrPtr];
                    if (aBool5898 && (aString5897 == null || cs2executor2.current.scriptName != null && cs2executor2.current.scriptName.indexOf(aString5897) != -1)) {
                        System.out.println(cs2executor2.current.scriptName + ": " + cs2opinfo4);
                    }
                    cs2executor2.aBool7022 = cs2executor2.intOpValues[cs2executor2.instrPtr] == 1;
                    if (cs2opinfo4 == ClientScriptOpCode.RETURN && cs2executor2.returnValuesPtr == 0) {
                        decrementCS2ExecIdx();
                        break;
                    }
                    CS2Interpreter.executeOperation(cs2opinfo4, cs2executor2);
                }
            } catch (Exception exception8) {
                StringBuilder stringbuilder6 = new StringBuilder(30);
                stringbuilder6.append(cs2executor2.current.pointer).append(" ");
                for (int i_7 = cs2executor2.returnValuesPtr - 1; i_7 >= 0; --i_7) {
                    stringbuilder6.append(cs2executor2.returnValues[i_7].aCacheableNodeSub55869.pointer).append(" ");
                }
                assert cs2opinfo4 != null;
                stringbuilder6.append(Integer.valueOf(cs2opinfo4.opcode));
                PacketEncoder.sendErrorToServer(stringbuilder6.toString(), exception8);
                decrementCS2ExecIdx();
            }
        } catch (Exception exception9) {
            decrementCS2ExecIdx();
        }
    }

    public static void executeHookInner(HookRequest hook) {
        execute_hook_inner(hook, 200000);
    }

    public static void execute_hook_inner(HookRequest hook, int stackLimit) {
        Object[] params = hook.params;
        int scriptId = ((Integer) params[0]).intValue();
        CS2Script script = CS2Script.getCs2Script(scriptId);
        if (script != null) {
            CS2Executor executor = getNextScriptExecutor();
            executor.intLocals = new int[script.intLocalsCount];
            int intLocalCount = 0;
            executor.objectLocals = new String[script.stringLocalsCount];
            int objectLocalCount = 0;
            executor.longLocals = new long[script.longLocalsCount];
            int longLocalCount = 0;

            for (int paramIdx = 1; paramIdx < params.length; paramIdx++) {
                if (params[paramIdx] instanceof Integer) {
                    int intLocal = ((Integer) params[paramIdx]).intValue();
                    if (intLocal == -2147483647) {
                        intLocal = hook.mouseX;
                    }

                    if (intLocal == -2147483646) {
                        intLocal = hook.mouseY;
                    }

                    if (intLocal == -2147483645) {
                        intLocal = hook.source != null ? hook.source.idHash : -1;
                    }

                    if (intLocal == -2147483644) {
                        intLocal = hook.anInt8051;
                    }

                    if (intLocal == -2147483643) {
                        intLocal = hook.source != null ? hook.source.slotId : -1;
                    }

                    if (intLocal == -2147483642) {
                        intLocal = hook.aClass1188057 != null ? hook.aClass1188057.idHash : -1;
                    }

                    if (intLocal == -2147483641) {
                        intLocal = hook.aClass1188057 != null ? hook.aClass1188057.slotId : -1;
                    }

                    if (intLocal == -2147483640) {
                        intLocal = hook.typedKeyCode;
                    }

                    if (intLocal == -2147483639) {
                        intLocal = hook.typedKeyChar;
                    }

                    executor.intLocals[intLocalCount++] = intLocal;
                } else if (params[paramIdx] instanceof String string_13) {
                    if ("event_opbase".equals(string_13)) {
                        string_13 = hook.optionName;
                    }

                    executor.objectLocals[objectLocalCount++] = string_13;
                } else if (params[paramIdx] instanceof Long) {
                    long long_14 = ((Long) params[paramIdx]).longValue();
                    executor.longLocals[longLocalCount++] = long_14;
                }
            }

            executor.hookRequestCount = hook.requestId;
            executeScript(script, stackLimit, executor);
        }
    }

    public static void executeCutsceneScript(int cutsceneId, String string_1, int i_2) {
        CS2Script script = CS2Script.getScript(ClientTriggerType.RUN_CUTSCENE_SCRIPT, cutsceneId, -1);
        if (script != null) {
            CS2Executor executor = getNextScriptExecutor();
            executor.intLocals = new int[script.intLocalsCount];
            executor.objectLocals = new String[script.stringLocalsCount];
            executor.objectLocals[0] = string_1;
            executor.intLocals[0] = i_2;
            executeScript(script, 200000, executor);
        }
    }

    public static void decrementCS2ExecIdx() {
        --CURRENT_CS2_EXEC_IDX;
    }

    public static CS2Executor getNextScriptExecutor() {
        if (CURRENT_CS2_EXEC_IDX == CS2_EXECUTORS.size()) {
            CS2_EXECUTORS.add(new CS2Executor());
        }
        return CS2_EXECUTORS.get(CURRENT_CS2_EXEC_IDX++);
    }

    static long method1480() {
        return (long) (++anInt5906 - 1) << 32 | 0xffffffffL;
    }

    public static void executeIfTrigger(ClientTriggerType class3970, int i_1, PathingEntity animable3, int i_4) {
        CS2Executor cs2executor6 = getNextScriptExecutor();
        cs2executor6.currentEntity = animable3;
        cs2executor6.anInt7004 = i_4;
        execute_trigger(class3970, i_1, -1, cs2executor6);
        cs2executor6.currentEntity = null;
        cs2executor6.anInt7004 = -1;
    }

    public static void ifObjectTrigger(ClientTriggerType class3970, int i_1, GameObject sceneobject3) {
        CS2Executor cs2executor5 = getNextScriptExecutor();
        cs2executor5.currentLocation = sceneobject3;
        execute_trigger(class3970, i_1, -1, cs2executor5);
        cs2executor5.currentLocation = null;
    }

    public static void executeIfOnGitem(ClientTriggerType triggerType, int interfaceId, GroundItem groundItem) {
        CS2Executor cs2executor6 = getNextScriptExecutor();
        cs2executor6.currentGroundItem = groundItem;
        execute_trigger(triggerType, interfaceId, -1, cs2executor6);
        cs2executor6.currentGroundItem = null;
    }

    public static void queryItemParamString(String itemName, boolean tradeableOnly, int paramId, String paramString) {
        CS2Interpreter.queryItem(itemName, tradeableOnly, paramId, -1, paramString, true);
    }

    public static void executeTrigger(ClientTriggerType triggerType, int i_1, int i_2) {
        CS2Executor cs2Executor = getNextScriptExecutor();
        execute_trigger(triggerType, i_1, i_2, cs2Executor);
    }

    public static void resetCs2Cache() {
        CS2_CACHE.reset();
    }

    public static String formatCs2Error(String errorString, String errorPrevious, String errorNext) {
        String exceptionStr = "";
        if (errorString.contains("|")) {
            exceptionStr = errorString.substring(errorString.indexOf("|") + 1, errorString.indexOf("|", errorString.indexOf("|") + 1));
            errorString = errorString.replaceFirst(exceptionStr, "");
            exceptionStr = exceptionStr.replaceAll(" ", "");
            errorString = exceptionStr + " " + errorString;
        }
        for (int i = errorString.indexOf(errorPrevious); i != -1; i = errorString.indexOf(errorPrevious, i + errorNext.length())) {
            errorString = errorString.substring(0, i) + errorNext + errorString.substring(i + errorPrevious.length());
        }
        return errorString;
    }
}
