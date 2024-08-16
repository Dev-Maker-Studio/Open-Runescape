package com.jagex.game.runetek5.tools;

import com.jagex.game.runetek5.ExplicitRuntimeException;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.console.Console;
import com.jagex.game.runetek5.client.login.LoginManager;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.culling.Culling;
import com.jagex.game.runetek5.utils.Utils;

import java.io.*;

public class DebugDisplay {

    public static void applyDebugOpcode(int opcode) {
        if (opcode == 17) {
            throw new Error();
        } else if (opcode == 25) {
            throw new OutOfMemoryError();
        } 
        try {
            if (opcode == 8) {
                Console.close();
            } else if (opcode == 24) {
                Console.writeLine("" + GameShell.FPS);
            } else if (opcode == 28) {
                Culling culling = Resource.mapRegion.getScene().culling;
                culling.cullingEnabled = !culling.cullingEnabled;
            } else if (opcode == 7) {
                client.DRAW_DEBUG = true;
            } else if (opcode == 4) {
                client.DRAW_DEBUG = false;
            } else if (opcode == 14) {
                client.TILE_MESSAGES.unlink();
            } else if (opcode == 21) {
                TypeLists.clear();
                for (int gc = 0; gc < 10; gc++) {
                    System.gc();
                }
                Runtime runtime = Runtime.getRuntime();
                int kbMemleft = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
                Console.writeLine("" + kbMemleft);
            } else if (opcode == 9) {
                TypeLists.clear();
                for (int gc = 0; gc < 10; gc++) {
                    System.gc();
                }
                Runtime runtime = Runtime.getRuntime();
                int kbMemleft = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
                Console.writeLine("" + kbMemleft);
                PlayerEntity.uploadScene();
                TypeLists.clear();
                for (int gc = 0; gc < 10; gc++) {
                    System.gc();
                }
                kbMemleft = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
                Console.writeLine("" + kbMemleft);
            } else if (opcode == 19) {
                Console.writeLine(Resource.NATIVE_LIBRARY_LOADER.shutdown() ? "Success" : "Failure");
            } else if (opcode == 20) {
                LoginManager.GAME_CONNECTION_INFO.swap();
            } else if (opcode == 22) {
                Resource.remoteFileRequesterJs5.closeStream();
            } else if (opcode == 3) {
                Resource.remoteFileRequesterJs5.requestTermination();
            } else if (opcode == 13) {
                GameShell.gameCanvas.setLocation(50, 50);
            } else if (opcode == 1) {
                GameShell.gameCanvas.setLocation(GameShell.gameCanvasX, GameShell.gameCanvasY);
            } else if (opcode == 27) {
                MapRegion.requestDebugMapUpdate();
            } else if (opcode == 6) {
                Resource.mapRegion.debugRegionLoadtimeStart = Utils.time();
                Resource.mapRegion.debugRegionLoadtime = true;
                MapRegion.requestDebugMapUpdate();
            } else if (opcode == 26) {
            	Vector3 abs = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
                Console.writeLine(((int) abs.x >> 9) + " " + ((int) abs.z >> 9));
            } else if (opcode == 2) {
            	Vector3 abs = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
                Console.writeLine("" + Resource.mapRegion.getScene().localHeightmap[PlayerUpdate.MY_PLAYER.plane].getHeight((int) abs.x >> 9, (int) abs.z >> 9));
            } else if (opcode == 23) {
                Console.writeLine(Component.NATIVE_SPRITE_CACHE.getCapacityLeft() + " " + Component.NATIVE_SPRITE_CACHE.getCapacity());
                Console.writeLine(Component.MODEL_CACHE.getCapacityLeft() + " " + Component.MODEL_CACHE.getCapacity());
                Console.writeLine(TypeLists.OBJ.spriteDrawQueue.getCurrentCapacity() + " " + TypeLists.OBJ.spriteDrawQueue.getMaxCapacity());
            } else if (opcode == 16) {
                TypeLists.resetCachedData(false);
            } else if (opcode == 15) {
                client.DEBUG_MODE = !client.DEBUG_MODE;
                Renderers.activeToolkit.debugModeChanged();
            } else if (opcode == 10) {
                client.sceneDummyOp = 0;
                Resource.mapRegion.reload();
            } else if (opcode == 5) {
                client.sceneDummyOp = 1;
                Resource.mapRegion.reload();
            } else if (opcode == 29) {
                client.sceneDummyOp = 2;
                Resource.mapRegion.reload();
            }            
        } catch (Exception exception) {
            Console.writeLine(LocalizedText.ERROR_EXECUTING_COMMAND.translate(client.CURRENT_LANGUAGE));
        }
    }

    public static String getStringFromThrowable(Throwable throwable) throws IOException {
        String error;
        if (throwable instanceof ExplicitRuntimeException runtimeException) {
            error = runtimeException.error + "|";
            throwable = runtimeException.throwable;
        } else {
            error = "";
        }
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.close();
        String stackTrace = stringWriter.toString();
        BufferedReader stackTraceReader = new BufferedReader(new StringReader(stackTrace));
        String firstTraceLine = stackTraceReader.readLine();
        while (true) {
            String stackTraceLine = stackTraceReader.readLine();
            if (stackTraceLine == null) {
                error = error + "| " + firstTraceLine;
                return error;
            }
            int seekJavaVersionPosition = stackTraceLine.indexOf(40);
            int javaVersionLength = stackTraceLine.indexOf(41, seekJavaVersionPosition + 1);
            String string_11;
            if (seekJavaVersionPosition != -1) {
                string_11 = stackTraceLine.substring(0, seekJavaVersionPosition);
            } else {
                string_11 = stackTraceLine;
            }
            string_11 = string_11.trim();
            string_11 = string_11.substring(string_11.lastIndexOf(32) + 1);
            string_11 = string_11.substring(string_11.lastIndexOf(9) + 1);
            error += string_11;
            if (seekJavaVersionPosition != -1 && javaVersionLength != -1) {
                int javaVersionStart = stackTraceLine.indexOf(".java:", seekJavaVersionPosition);
                if (javaVersionStart >= 0) {
                    error += stackTraceLine.substring(javaVersionStart + 5, javaVersionLength);
                }
            }
            error += ' ';
        }
    }

	public static int anInt3991;
}
