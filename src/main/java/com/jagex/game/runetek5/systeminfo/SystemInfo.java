package com.jagex.game.runetek5.systeminfo;

import jaclib.hardware_info.HardwareInfo;

import java.awt.EventQueue;
import java.util.HashMap;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.tools.Language;
import com.jagex.game.runetek5.utils.TextUtils;
import com.jagex.game.runetek5.utils.Utils;

public class SystemInfo extends Node {

    public final int javaBuild;
    public final int javaUpdate;
    public int ram;
    public String aString8157;
    public String aString8160;
    public String dwDirectxVersion;
    public String aString8153;
    public final int[] rawCPUInformationData = new int[3];
    public final int operatingSystemType;
    public final boolean x64os;
    public int osVendor;
    public final int javaVersionType;
    public final int javaSubBuild;
    public final boolean idk;
    public final int maxMem;
    public final int processors;
    public int cpuCores;
    public int cpuClock;
    public String cpuType;
    public int rawCPUInformation2;
    public int rawCPUInformation;
    public String cpuData;
    public int dxDriverMonth;
    public int dxDriverYear;
	public static SystemInfo SYSTEM_INFO;
	public static int numIndices;
	public static String[] cacheFilestoreSubpath;
	public static String[] cacheBaseDirectory;
	public static EventQueue systemEventQueue;
	public static String operatingSystemLowercase;
	public static String osVersion;
	public static String operatingSystem;
	public static int storeId;
	public static int clientBuildSub;
	public static int clientBuild;
	public static String javaVendor;
	public static String javaVersion;
	public static String OS_ARCH;

    public SystemInfo() {
        if (SystemInfo.operatingSystemLowercase.startsWith("win")) {
            this.operatingSystemType = 1;
        } else if (SystemInfo.operatingSystemLowercase.startsWith("mac")) {
            this.operatingSystemType = 2;
        } else if (SystemInfo.operatingSystemLowercase.startsWith("linux")) {
            this.operatingSystemType = 3;
        } else {
            this.operatingSystemType = 4;
        }
        this.x64os = SystemInfo.OS_ARCH.startsWith("amd64") || SystemInfo.OS_ARCH.startsWith("x86_64");
        if (this.operatingSystemType == 1) {
            if (SystemInfo.osVersion.indexOf("4.0") != -1) {
                this.osVendor = 1;
            } else if (SystemInfo.osVersion.indexOf("4.1") != -1) {
                this.osVendor = 2;
            } else if (SystemInfo.osVersion.indexOf("4.9") != -1) {
                this.osVendor = 3;
            } else if (SystemInfo.osVersion.indexOf("5.0") != -1) {
                this.osVendor = 4;
            } else if (SystemInfo.osVersion.indexOf("5.1") != -1) {
                this.osVendor = 5;
            } else if (SystemInfo.osVersion.indexOf("5.2") != -1) {
                this.osVendor = 8;
            } else if (SystemInfo.osVersion.indexOf("6.0") != -1) {
                this.osVendor = 6;
            } else if (SystemInfo.osVersion.indexOf("6.1") != -1) {
                this.osVendor = 7;
            } else if (SystemInfo.osVersion.indexOf("6.2") != -1) {
                this.osVendor = 9;
            }
        } else if (this.operatingSystemType == 2) {
            if (SystemInfo.osVersion.indexOf("10.4") != -1) {
                this.osVendor = 20;
            } else if (SystemInfo.osVersion.indexOf("10.5") != -1) {
                this.osVendor = 21;
            } else if (SystemInfo.osVersion.indexOf("10.6") != -1) {
                this.osVendor = 22;
            } else if (SystemInfo.osVersion.indexOf("10.7") != -1) {
                this.osVendor = 23;
            }
        }
        if (SystemInfo.javaVendor.toLowerCase().indexOf("sun") != -1) {
            this.javaVersionType = 1;
        } else if (SystemInfo.javaVendor.toLowerCase().indexOf("microsoft") != -1) {
            this.javaVersionType = 2;
        } else if (SystemInfo.javaVendor.toLowerCase().indexOf("apple") != -1) {
            this.javaVersionType = 3;
        } else {
            this.javaVersionType = 4;
        }
        int minorVerIndex = SystemInfo.javaVersion.startsWith("1.") ? 2 : 0;
        int update = 0;
        char var_4;
        try {
            while (minorVerIndex < SystemInfo.javaVersion.length()) {
                var_4 = SystemInfo.javaVersion.charAt(minorVerIndex);
                if (var_4 < 48 || var_4 > 57) {
                    break;
                }
                update = var_4 - 48 + update * 10;
                ++minorVerIndex;
            }
        } catch (Exception exception) {
        	// NOOP
        }
        this.javaBuild = update;
        minorVerIndex = SystemInfo.javaVersion.indexOf(46, 2) + 1;
        update = 0;
        try {
            while (minorVerIndex < SystemInfo.javaVersion.length()) {
                var_4 = SystemInfo.javaVersion.charAt(minorVerIndex);
                if (var_4 < 48 || var_4 > 57) {
                    break;
                }
                update = var_4 - 48 + update * 10;
                ++minorVerIndex;
            }
        } catch (Exception exception) {
        	// NOOP
        }
        this.javaSubBuild = update;
        minorVerIndex = SystemInfo.javaVersion.indexOf(95, 4) + 1;
        update = 0;
        try {
            while (minorVerIndex < SystemInfo.javaVersion.length()) {
                var_4 = SystemInfo.javaVersion.charAt(minorVerIndex);
                if (var_4 < 48 || var_4 > 57) {
                    break;
                }
                update = var_4 - 48 + update * 10;
                ++minorVerIndex;
            }
        } catch (Exception exception) {
        	// NOOP
        }
        this.javaUpdate = update;
        this.idk = false;
        this.maxMem = GameShell.maxMemory;
        if (this.javaBuild > 3) {
            this.processors = GameShell.availableProcessors;
        } else {
            this.processors = 0;
        }
        try {
            int[] cpuInfo = HardwareInfo.getCPUInfo();
            if (cpuInfo != null && cpuInfo.length == 3) {
                this.cpuCores = cpuInfo[0];
                this.cpuClock = cpuInfo[1];
                this.ram = cpuInfo[2];
            }
            int[] rawCpuInfo = HardwareInfo.getRawCPUInfo();
            if (rawCpuInfo != null && rawCpuInfo.length % 5 == 0) {
                HashMap<Integer, RawCpuInfo> hashmap6 = new HashMap<>();
                for (int i_7 = 0; i_7 < rawCpuInfo.length / 5; i_7++) {
                    int i_8 = rawCpuInfo[i_7 * 5];
                    int i_9 = rawCpuInfo[i_7 * 5 + 1];
                    int i_10 = rawCpuInfo[i_7 * 5 + 2];
                    int i_11 = rawCpuInfo[i_7 * 5 + 3];
                    int i_12 = rawCpuInfo[i_7 * 5 + 4];
                    RawCpuInfo class43013 = new RawCpuInfo(i_8, i_9, i_10, i_11, i_12);
                    hashmap6.put(Integer.valueOf(i_8), class43013);
                }
                RawCpuInfo class43015 = hashmap6.get(Integer.valueOf(0));
                if (class43015 != null) {
                    Packet rsbytebuffer16 = new Packet(13);
                    rsbytebuffer16.put24BitInt(class43015.anInt5137);
                    rsbytebuffer16.put24BitInt(class43015.anInt5136);
                    rsbytebuffer16.put24BitInt(class43015.anInt5138);
                    rsbytebuffer16.currentPosition = 0;
                    this.cpuType = rsbytebuffer16.readString();
                }
                RawCpuInfo class43027 = hashmap6.get(Integer.valueOf(1));
                if (class43027 != null) {
                    this.rawCPUInformation2 = class43027.anInt5139;
                    int i_9 = class43027.anInt5137;
                    this.rawCPUInformation = i_9 >> 16 & 0xff;
                    this.rawCPUInformationData[0] = class43027.anInt5138;
                    this.rawCPUInformationData[1] = class43027.anInt5136;
                }
                RawCpuInfo class43017 = hashmap6.get(Integer.valueOf(-2147483647));
                if (class43017 != null) {
                    this.rawCPUInformationData[2] = class43017.anInt5136;
                }
                Packet rsbytebuffer18 = new Packet(49);
                for (int i_11 = -2147483646; i_11 <= -2147483644; i_11++) {
                    RawCpuInfo class43019 = hashmap6.get(Integer.valueOf(i_11));
                    if (class43019 != null) {
                        rsbytebuffer18.put24BitInt(class43019.anInt5139);
                        rsbytebuffer18.put24BitInt(class43019.anInt5137);
                        rsbytebuffer18.put24BitInt(class43019.anInt5138);
                        rsbytebuffer18.put24BitInt(class43019.anInt5136);
                    }
                }
                rsbytebuffer18.currentPosition = 0;
                this.cpuData = rsbytebuffer18.readString();
            }
            String[][] dxdiagDisplaydeviceProps = HardwareInfo.getDXDiagDisplayDevicesProps();
            if (dxdiagDisplaydeviceProps != null && dxdiagDisplaydeviceProps.length > 0 && dxdiagDisplaydeviceProps[0] != null) {
                for (int i_7 = 0; i_7 < dxdiagDisplaydeviceProps[0].length; i_7 += 2) {
                    if ("szDescription".equalsIgnoreCase(dxdiagDisplaydeviceProps[0][i_7])) {
                        this.aString8157 = dxdiagDisplaydeviceProps[0][i_7 + 1];
                    } else if ("szDriverDateEnglish".equalsIgnoreCase(dxdiagDisplaydeviceProps[0][i_7])) {
                        String string_28 = dxdiagDisplaydeviceProps[0][i_7 + 1];
                        try {
                        	int i_9 = string_28.indexOf('/');
                        	int i_10 = string_28.indexOf('/', i_9 + 1);
                            this.dxDriverMonth = Integer.parseInt(string_28.substring(0, i_9));
                            this.dxDriverYear = Integer.parseInt(string_28.substring(i_10 + 1, string_28.indexOf(' ', i_10)));
                        } catch (Exception exception) {
                        	// NOOP
                        }
                    }
                }
            }
            String[] dxdiagSystemProps = HardwareInfo.getDXDiagSystemProps();
            if (dxdiagSystemProps != null) {
            	String major = "";
                String minor = "";
                String letter = "";
                for (int index = 0; index < dxdiagSystemProps.length; index += 2) {
                    if ("dwDirectXVersionMajor".equalsIgnoreCase(dxdiagSystemProps[index])) {
                        major = dxdiagSystemProps[index + 1];
                    } else if ("dwDirectXVersionMinor".equalsIgnoreCase(dxdiagSystemProps[index])) {
                        minor = dxdiagSystemProps[index + 1];
                    } else if ("dwDirectXVersionLetter".equalsIgnoreCase(dxdiagSystemProps[index])) {
                        letter = dxdiagSystemProps[index + 1];
                    }
                }
                this.dwDirectxVersion = major + "." + minor + letter;
            }
        } catch (Throwable throwable) {
            this.ram = 0;
        }
        if (this.aString8157 == null) {
            this.aString8157 = "";
        }
        this.aString8160 = "";
        if (this.dwDirectxVersion == null) {
            this.dwDirectxVersion = "";
        }
        this.aString8153 = "";
        if (this.cpuType == null) {
            this.cpuType = "";
        }
        if (this.cpuData == null) {
            this.cpuData = "";
        }
        method13455();
    }

    public int encode() {
        byte b_2 = 38;
        int i_3 = b_2 + Utils.stringLengthPlus2(this.aString8157);
        i_3 += Utils.stringLengthPlus2(this.aString8160);
        i_3 += Utils.stringLengthPlus2(this.dwDirectxVersion);
        i_3 += Utils.stringLengthPlus2(this.aString8153);
        i_3 += Utils.stringLengthPlus2(this.cpuType);
        i_3 += Utils.stringLengthPlus2(this.cpuData);
        return i_3;
    }

    public void method13455() {
        if (this.aString8157.length() > 40) {
            this.aString8157 = this.aString8157.substring(0, 40);
        }
        if (this.aString8160.length() > 40) {
            this.aString8160 = this.aString8160.substring(0, 40);
        }
        if (this.dwDirectxVersion.length() > 10) {
            this.dwDirectxVersion = this.dwDirectxVersion.substring(0, 10);
        }
        if (this.aString8153.length() > 10) {
            this.aString8153 = this.aString8153.substring(0, 10);
        }
    }

    public void writeMachineInformation(Packet machineInfoBuffer) {
        machineInfoBuffer.writeByte(6);
        machineInfoBuffer.writeByte(this.operatingSystemType);
        machineInfoBuffer.writeByte(this.x64os ? 1 : 0);
        machineInfoBuffer.writeByte(this.osVendor);
        machineInfoBuffer.writeByte(this.javaVersionType);
        machineInfoBuffer.writeByte(this.javaBuild);
        machineInfoBuffer.writeByte(this.javaSubBuild);
        machineInfoBuffer.writeByte(this.javaUpdate);
        machineInfoBuffer.writeByte(this.idk ? 1 : 0);
        machineInfoBuffer.writeShort(this.maxMem);
        machineInfoBuffer.writeByte(this.processors);
        machineInfoBuffer.write24BitInt(this.ram);
        machineInfoBuffer.writeShort(this.cpuClock);
        machineInfoBuffer.writeJagString(this.aString8157);
        machineInfoBuffer.writeJagString(this.aString8160);
        machineInfoBuffer.writeJagString(this.dwDirectxVersion);
        machineInfoBuffer.writeJagString(this.aString8153);
        machineInfoBuffer.writeByte(this.dxDriverMonth);
        machineInfoBuffer.writeShort(this.dxDriverYear);
        machineInfoBuffer.writeJagString(this.cpuType);
        machineInfoBuffer.writeJagString(this.cpuData);
        machineInfoBuffer.writeByte(this.cpuCores);
        machineInfoBuffer.writeByte(this.rawCPUInformation);
        for (int i_3 = 0; i_3 < this.rawCPUInformationData.length; i_3++) {
            machineInfoBuffer.writeInt(this.rawCPUInformationData[i_3]);
        }
        machineInfoBuffer.writeInt(this.rawCPUInformation2);
    }

	public static String getCacheCompletionPercent(long percentage, int i_2, boolean bool3, Language language) {
	    char prefix = 44;
	    char postfix = 46;
	    if (language == Language.ENGLISH) {
	        prefix = 46;
	        postfix = 44;
	    }
	    if (language == Language.FRENCH) {
	        postfix = 160;
	    }
	    boolean invert = false;
	    if (percentage < 0L) {
	        invert = true;
	        percentage = -percentage;
	    }
	    StringBuilder progressionStringbuilder = new StringBuilder(26);
	    int i_10;
	    int i_11;
	    if (i_2 > 0) {
	        for (i_10 = 0; i_10 < i_2; i_10++) {
	            i_11 = (int) percentage;
	            percentage /= 10L;
	            progressionStringbuilder.append((char) (i_11 + 48 - (int) percentage * 10));
	        }
	        progressionStringbuilder.append(prefix);
	    }
	    i_10 = 0;
	    while (true) {
	        i_11 = (int) percentage;
	        percentage /= 10L;
	        progressionStringbuilder.append((char) (i_11 + 48 - (int) percentage * 10));
	        if (percentage == 0L) {
	            if (invert) {
	                progressionStringbuilder.append('-');
	            }
	            return progressionStringbuilder.reverse().toString();
	        }
	        if (bool3) {
	            ++i_10;
	            if (i_10 % 3 == 0) {
	                progressionStringbuilder.append(postfix);
	            }
	        }
	    }
	}

	public static void drawDebugInformation(int x, int y, Component inter) {
	    int currDrawX = inter.width + x;
	    int currDrawY = y + 15;
	    if (client.DRAW_DEBUG) {
	        int fpsColor = -256;
	        if (GameShell.FPS < 20) {
	            fpsColor = -65536;
	        }
	        FontManager.debugOverlayFont.renderLeftAligned("Fps:" + GameShell.FPS, currDrawX, currDrawY, fpsColor, -1);
	        currDrawY += 15;
	        Runtime runtime = Runtime.getRuntime();
	        long totalMem = runtime.totalMemory() / 1024L;
	        long freeMem = totalMem - runtime.freeMemory() / 1024L;
	        int memColor = -256;
	        if (freeMem > 262144L) {
	            memColor = -65536;
	        }
	        FontManager.debugOverlayFont.renderLeftAligned("Mem:" + freeMem + "/" + totalMem + "k", currDrawX, currDrawY, memColor, -1);
	        currDrawY += 15;
	        long gamePing = client.GAME_CONNECTION_CONTEXT.pinger.getPing();
	        String gamePingStr = "N/A";
	        if (gamePing != -1L) {
	            gamePingStr = gamePing + "ms";
	            if (gamePing > 500L) {
	                gamePingStr = TextUtils.setTextColor(16711680) + gamePingStr + TextUtils.setTextColor(16776960);
	            }
	        }
	        FontManager.debugOverlayFont.renderLeftAligned("Game: In:" + client.GAME_CONNECTION_CONTEXT.inBytesPerSec + "B/s " + "Out:" + client.GAME_CONNECTION_CONTEXT.outBytesPerSec + "B/s " + "Ping:" + gamePingStr, currDrawX, currDrawY, -256, -1);
	        currDrawY += 15;
	        long lobbyPing = client.LOBBY_CONNECTION_CONTEXT.pinger.getPing();
	        String lobbyPingStr = "N/A";
	        if (lobbyPing != -1L) {
	            lobbyPingStr = lobbyPing + "ms";
	            if (lobbyPing > 500L) {
	                lobbyPingStr = TextUtils.setTextColor(16711680) + lobbyPingStr + TextUtils.setTextColor(16776960);
	            }
	        }
	        FontManager.debugOverlayFont.renderLeftAligned("Lobby: In:" + client.LOBBY_CONNECTION_CONTEXT.inBytesPerSec + "B/s " + "Out:" + client.LOBBY_CONNECTION_CONTEXT.outBytesPerSec + "B/s " + "Ping:" + lobbyPingStr, currDrawX, currDrawY, -256, -1);
	        currDrawY += 15;
	        int i_19 = Renderers.activeToolkit.getOffheap() / 1024;
	        FontManager.debugOverlayFont.renderLeftAligned("Offheap:" + i_19 + "k", currDrawX, currDrawY, i_19 > 65536 ? -65536 : -256, -1);
	        currDrawY += 15;
	        int totalArchives = 0;
	        int filesReceived = 0;
	        int currentFiles = 0;
	        for (int index = 0; index < Resource.remoteFileWorkersByIndex.length; index++) {
	            if (Resource.remoteFileWorkersByIndex[index] != null) {
	                totalArchives += Resource.remoteFileWorkersByIndex[index].getTotalArchives();
	                filesReceived += Resource.remoteFileWorkersByIndex[index].filesReceived();
	                currentFiles += Resource.remoteFileWorkersByIndex[index].currentRequestId();
	            }
	        }
	        int completion = currentFiles * 100 / totalArchives;
	        int i_24 = filesReceived * 10000 / totalArchives;
	        String cacheCompletion = "Cache:" + SystemInfo.getCacheCompletionPercent(i_24, 2, true, Language.ENGLISH) + "% (" + completion + "%)";
	        Renderers.FONT_RENDERER.renderLeftAligned(cacheCompletion, currDrawX, currDrawY, -256, -1);
	        currDrawY += 12;
	    }
	}
}
