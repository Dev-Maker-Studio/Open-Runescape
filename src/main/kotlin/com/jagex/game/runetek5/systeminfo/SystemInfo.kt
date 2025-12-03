package com.jagex.game.runetek5.systeminfo

import jaclib.hardware_info.HardwareInfo
import java.awt.EventQueue
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.utils.Utils

class SystemInfo : Node<SystemInfo>() {

    @JvmField
    val javaBuild: Int
    @JvmField
    val javaUpdate: Int
    @JvmField
    var ram: Int = 0
    var gpuModel: String? = null
    var aString8160: String? = null
    var dwDirectxVersion: String? = null
    var aString8153: String? = null
    @JvmField
    val cpuFeatureFlags: IntArray = IntArray(3)
    val operatingSystemType: Int
    val x64os: Boolean
    var osVendor: Int = 0
    val javaVersionType: Int
    val javaSubBuild: Int
    val idk: Boolean
    val maxMem: Int
    val processors: Int
    var cpuCores: Int = 0
    var cpuClock: Int = 0
    var cpuType: String? = null
    var cpuModelInfo: Int = 0
    var cpuFamilyId: Int = 0
    var cpuData: String? = null
    var dxDriverMonth: Int = 0
    var dxDriverYear: Int = 0

    init {
        operatingSystemType = when {
            operatingSystemLowercase.startsWith("win") -> 1
            operatingSystemLowercase.startsWith("mac") -> 2
            operatingSystemLowercase.startsWith("linux") -> 3
            else -> 4
        }
        x64os = OS_ARCH.startsWith("amd64") || OS_ARCH.startsWith("x86_64")
        if (operatingSystemType == 1) {
            osVendor = when {
                osVersion.indexOf("4.0") != -1 -> 1
                osVersion.indexOf("4.1") != -1 -> 2
                osVersion.indexOf("4.9") != -1 -> 3
                osVersion.indexOf("5.0") != -1 -> 4
                osVersion.indexOf("5.1") != -1 -> 5
                osVersion.indexOf("5.2") != -1 -> 8
                osVersion.indexOf("6.0") != -1 -> 6
                osVersion.indexOf("6.1") != -1 -> 7
                osVersion.indexOf("6.2") != -1 -> 9
                else -> 0
            }
        } else if (operatingSystemType == 2) {
            osVendor = when {
                osVersion.indexOf("10.4") != -1 -> 20
                osVersion.indexOf("10.5") != -1 -> 21
                osVersion.indexOf("10.6") != -1 -> 22
                osVersion.indexOf("10.7") != -1 -> 23
                else -> 0
            }
        }
        javaVersionType = when {
            javaVendor.lowercase().indexOf("sun") != -1 -> 1
            javaVendor.lowercase().indexOf("microsoft") != -1 -> 2
            javaVendor.lowercase().indexOf("apple") != -1 -> 3
            else -> 4
        }
        var minorVerIndex = if (javaVersion.startsWith("1.")) 2 else 0
        var update = 0
        var var_4: Char
        try {
            while (minorVerIndex < javaVersion.length) {
                var_4 = javaVersion[minorVerIndex]
                if (var_4 < '0' || var_4 > '9') {
                    break
                }
                update = var_4.code - 48 + update * 10
                ++minorVerIndex
            }
        } catch (exception: Exception) {
            // NOOP
        }
        javaBuild = update
        minorVerIndex = javaVersion.indexOf('.', 2) + 1
        update = 0
        try {
            while (minorVerIndex < javaVersion.length) {
                var_4 = javaVersion[minorVerIndex]
                if (var_4 < '0' || var_4 > '9') {
                    break
                }
                update = var_4.code - 48 + update * 10
                ++minorVerIndex
            }
        } catch (exception: Exception) {
            // NOOP
        }
        javaSubBuild = update
        minorVerIndex = javaVersion.indexOf('_', 4) + 1
        update = 0
        try {
            while (minorVerIndex < javaVersion.length) {
                var_4 = javaVersion[minorVerIndex]
                if (var_4 < '0' || var_4 > '9') {
                    break
                }
                update = var_4.code - 48 + update * 10
                ++minorVerIndex
            }
        } catch (exception: Exception) {
            // NOOP
        }
        javaUpdate = update
        idk = false
        maxMem = GameShell.maxMemory
        processors = if (javaBuild > 3) {
            GameShell.availableProcessors
        } else {
            0
        }
        try {
            val cpuInfo = HardwareInfo.getCPUInfo()
            if (cpuInfo != null && cpuInfo.size == 3) {
                cpuCores = cpuInfo[0]
                cpuClock = cpuInfo[1]
                ram = cpuInfo[2]
            }
            val rawCpuInfo = HardwareInfo.getRawCPUInfo()
            if (rawCpuInfo != null && rawCpuInfo.size % 5 == 0) {
                val hashmap6 = HashMap<Int, RawCpuInfo>()
                for (i_7 in 0 until rawCpuInfo.size / 5) {
                    val i_8 = rawCpuInfo[i_7 * 5]
                    val i_9 = rawCpuInfo[i_7 * 5 + 1]
                    val i_10 = rawCpuInfo[i_7 * 5 + 2]
                    val i_11 = rawCpuInfo[i_7 * 5 + 3]
                    val i_12 = rawCpuInfo[i_7 * 5 + 4]
                    val class43013 = RawCpuInfo(i_8, i_9, i_10, i_11, i_12)
                    hashmap6[i_8] = class43013
                }
                val class43015 = hashmap6[0]
                if (class43015 != null) {
                    val rsbytebuffer16 = Packet(13)
                    rsbytebuffer16.writeIntLittle(class43015.resultEBX)
                    rsbytebuffer16.writeIntLittle(class43015.resultEDX)
                    rsbytebuffer16.writeIntLittle(class43015.resultECX)
                    rsbytebuffer16.currentPosition = 0
                    cpuType = rsbytebuffer16.readString()
                }
                val class43027 = hashmap6[1]
                if (class43027 != null) {
                    cpuModelInfo = class43027.resultEAX
                    val i_9 = class43027.resultEBX
                    cpuFamilyId = i_9 shr 16 and 0xff
                    cpuFeatureFlags[0] = class43027.resultECX
                    cpuFeatureFlags[1] = class43027.resultEDX
                }
                val class43017 = hashmap6[-2147483647]
                if (class43017 != null) {
                    cpuFeatureFlags[2] = class43017.resultEDX
                }
                val rsbytebuffer18 = Packet(49)
                for (i_11 in -2147483646..-2147483644) {
                    val class43019 = hashmap6[i_11]
                    if (class43019 != null) {
                        rsbytebuffer18.writeIntLittle(class43019.resultEAX)
                        rsbytebuffer18.writeIntLittle(class43019.resultEBX)
                        rsbytebuffer18.writeIntLittle(class43019.resultECX)
                        rsbytebuffer18.writeIntLittle(class43019.resultEDX)
                    }
                }
                rsbytebuffer18.currentPosition = 0
                cpuData = rsbytebuffer18.readString()
            }
            val dxdiagDisplaydeviceProps = HardwareInfo.getDXDiagDisplayDevicesProps()
            if (dxdiagDisplaydeviceProps != null && dxdiagDisplaydeviceProps.isNotEmpty() && dxdiagDisplaydeviceProps[0] != null) {
                var i_7 = 0
                while (i_7 < dxdiagDisplaydeviceProps[0].size) {
                    if ("szDescription".equals(dxdiagDisplaydeviceProps[0][i_7], ignoreCase = true)) {
                        gpuModel = dxdiagDisplaydeviceProps[0][i_7 + 1]
                    } else if ("szDriverDateEnglish".equals(dxdiagDisplaydeviceProps[0][i_7], ignoreCase = true)) {
                        val string_28 = dxdiagDisplaydeviceProps[0][i_7 + 1]
                        try {
                            val i_9 = string_28.indexOf('/')
                            val i_10 = string_28.indexOf('/', i_9 + 1)
                            dxDriverMonth = string_28.substring(0, i_9).toInt()
                            dxDriverYear = string_28.substring(i_10 + 1, string_28.indexOf(' ', i_10)).toInt()
                        } catch (exception: Exception) {
                            // NOOP
                        }
                    }
                    i_7 += 2
                }
            }
            val dxdiagSystemProps = HardwareInfo.getDXDiagSystemProps()
            if (dxdiagSystemProps != null) {
                var major = ""
                var minor = ""
                var letter = ""
                var index = 0
                while (index < dxdiagSystemProps.size) {
                    if ("dwDirectXVersionMajor".equals(dxdiagSystemProps[index], ignoreCase = true)) {
                        major = dxdiagSystemProps[index + 1]
                    } else if ("dwDirectXVersionMinor".equals(dxdiagSystemProps[index], ignoreCase = true)) {
                        minor = dxdiagSystemProps[index + 1]
                    } else if ("dwDirectXVersionLetter".equals(dxdiagSystemProps[index], ignoreCase = true)) {
                        letter = dxdiagSystemProps[index + 1]
                    }
                    index += 2
                }
                dwDirectxVersion = "$major.$minor$letter"
            }
        } catch (throwable: Throwable) {
            ram = 0
        }
        if (gpuModel == null) {
            gpuModel = ""
        }
        aString8160 = ""
        if (dwDirectxVersion == null) {
            dwDirectxVersion = ""
        }
        aString8153 = ""
        if (cpuType == null) {
            cpuType = ""
        }
        if (cpuData == null) {
            cpuData = ""
        }
        method13455()
    }

    fun encode(): Int {
        val b_2: Byte = 38
        var i_3 = b_2.toInt() + Utils.stringLengthPlus2(gpuModel)
        i_3 += Utils.stringLengthPlus2(aString8160)
        i_3 += Utils.stringLengthPlus2(dwDirectxVersion)
        i_3 += Utils.stringLengthPlus2(aString8153)
        i_3 += Utils.stringLengthPlus2(cpuType)
        i_3 += Utils.stringLengthPlus2(cpuData)
        return i_3
    }

    fun method13455() {
        if (gpuModel!!.length > 40) {
            gpuModel = gpuModel!!.substring(0, 40)
        }
        if (aString8160!!.length > 40) {
            aString8160 = aString8160!!.substring(0, 40)
        }
        if (dwDirectxVersion!!.length > 10) {
            dwDirectxVersion = dwDirectxVersion!!.substring(0, 10)
        }
        if (aString8153!!.length > 10) {
            aString8153 = aString8153!!.substring(0, 10)
        }
    }

    fun writeMachineInformation(machineInfoBuffer: Packet) {
        machineInfoBuffer.writeByte(6)
        machineInfoBuffer.writeByte(operatingSystemType)
        machineInfoBuffer.writeByte(if (x64os) 1 else 0)
        machineInfoBuffer.writeByte(osVendor)
        machineInfoBuffer.writeByte(javaVersionType)
        machineInfoBuffer.writeByte(javaBuild)
        machineInfoBuffer.writeByte(javaSubBuild)
        machineInfoBuffer.writeByte(javaUpdate)
        machineInfoBuffer.writeByte(if (idk) 1 else 0)
        machineInfoBuffer.writeShort(maxMem)
        machineInfoBuffer.writeByte(processors)
        machineInfoBuffer.writeMedium(ram)
        machineInfoBuffer.writeShort(cpuClock)
        machineInfoBuffer.writeJagString(gpuModel ?: "")
        machineInfoBuffer.writeJagString(aString8160 ?: "")
        machineInfoBuffer.writeJagString(dwDirectxVersion ?: "")
        machineInfoBuffer.writeJagString(aString8153 ?: "")
        machineInfoBuffer.writeByte(dxDriverMonth)
        machineInfoBuffer.writeShort(dxDriverYear)
        machineInfoBuffer.writeJagString(cpuType ?: "")
        machineInfoBuffer.writeJagString(cpuData ?: "")
        machineInfoBuffer.writeByte(cpuCores)
        machineInfoBuffer.writeByte(cpuFamilyId)
        for (i_3 in cpuFeatureFlags.indices) {
            machineInfoBuffer.writeInt(cpuFeatureFlags[i_3])
        }
        machineInfoBuffer.writeInt(cpuModelInfo)
    }

    companion object {
        @JvmField
        var SYSTEM_INFO: SystemInfo? = null
        @JvmField
        var numIndices: Int = 0
        @JvmField
        var cacheFilestoreSubpath: Array<String>? = null
        @JvmField
        var cacheBaseDirectory: Array<String>? = null
        @JvmField
        var systemEventQueue: EventQueue? = null
        @JvmField
        var operatingSystemLowercase: String = ""
        @JvmField
        var osVersion: String = ""
        @JvmField
        var operatingSystem: String = ""
        @JvmField
        var storeId: Int = 0
        @JvmField
        var clientBuildSub: Int = 0
        @JvmField
        var clientBuild: Int = 0
        @JvmField
        var javaVendor: String = ""
        @JvmField
        var javaVersion: String = ""
        @JvmField
        var OS_ARCH: String = ""

        @JvmStatic
        fun getCacheCompletionPercent(percentage: Long, i_2: Int, bool3: Boolean, language: Language): String {
            var prefix: Char = 44.toChar()
            var postfix: Char = 46.toChar()
            if (language == Language.ENGLISH) {
                prefix = 46.toChar()
                postfix = 44.toChar()
            }
            if (language == Language.FRENCH) {
                postfix = 160.toChar()
            }
            var invert = false
            var mutablePercentage = percentage
            if (mutablePercentage < 0L) {
                invert = true
                mutablePercentage = -mutablePercentage
            }
            val progressionStringbuilder = StringBuilder(26)
            var i_10: Int
            var i_11: Int
            if (i_2 > 0) {
                for (i_10 in 0 until i_2) {
                    i_11 = mutablePercentage.toInt()
                    mutablePercentage /= 10L
                    progressionStringbuilder.append((i_11 + 48 - mutablePercentage.toInt() * 10).toChar())
                }
                progressionStringbuilder.append(prefix)
            }
            i_10 = 0
            while (true) {
                i_11 = mutablePercentage.toInt()
                mutablePercentage /= 10L
                progressionStringbuilder.append((i_11 + 48 - mutablePercentage.toInt() * 10).toChar())
                if (mutablePercentage == 0L) {
                    if (invert) {
                        progressionStringbuilder.append('-')
                    }
                    return progressionStringbuilder.reverse().toString()
                }
                if (bool3) {
                    ++i_10
                    if (i_10 % 3 == 0) {
                        progressionStringbuilder.append(postfix)
                    }
                }
            }
        }

        @JvmStatic
        fun drawDebugInformation(x: Int, y: Int, inter: Component) {
            val currDrawX = inter.width + x
            var currDrawY = y + 15
            if (client.DRAW_DEBUG) {
                var fpsColor = -256
                if (GameShell.FPS < 20) {
                    fpsColor = -65536
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Fps:" + GameShell.FPS, currDrawX, currDrawY, fpsColor, -1)
                currDrawY += 15
                val runtime = Runtime.getRuntime()
                val totalMem = runtime.totalMemory() / 1024L
                val freeMem = totalMem - runtime.freeMemory() / 1024L
                var memColor = -256
                if (freeMem > 262144L) {
                    memColor = -65536
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Mem:$freeMem/${totalMem}k", currDrawX, currDrawY, memColor, -1)
                currDrawY += 15
                val gamePing = client.GAME_CONNECTION_CONTEXT!!.pinger.getPing()
                var gamePingStr = "N/A"
                if (gamePing != -1L) {
                    gamePingStr = gamePing.toString() + "ms"
                    if (gamePing > 500L) {
                        gamePingStr = TextUtils.setTextColor(16711680) + gamePingStr + TextUtils.setTextColor(16776960)
                    }
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Game: In:" + client.GAME_CONNECTION_CONTEXT!!.inBytesPerSec + "B/s Out:" + client.GAME_CONNECTION_CONTEXT!!.outBytesPerSec + "B/s Ping:" + gamePingStr, currDrawX, currDrawY, -256, -1)
                currDrawY += 15
                val lobbyPing = client.LOBBY_CONNECTION_CONTEXT!!.pinger.getPing()
                var lobbyPingStr = "N/A"
                if (lobbyPing != -1L) {
                    lobbyPingStr = lobbyPing.toString() + "ms"
                    if (lobbyPing > 500L) {
                        lobbyPingStr = TextUtils.setTextColor(16711680) + lobbyPingStr + TextUtils.setTextColor(16776960)
                    }
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Lobby: In:" + client.LOBBY_CONNECTION_CONTEXT!!.inBytesPerSec + "B/s Out:" + client.LOBBY_CONNECTION_CONTEXT!!.outBytesPerSec + "B/s Ping:" + lobbyPingStr, currDrawX, currDrawY, -256, -1)
                currDrawY += 15
                val i_19 = Renderers.activeToolkit!!.getOffheap() / 1024
                FontManager.debugOverlayFont!!.renderLeftAligned("Offheap:${i_19}k", currDrawX, currDrawY, if (i_19 > 65536) -65536 else -256, -1)
                currDrawY += 15
                var totalArchives = 0
                var filesReceived = 0
                var currentFiles = 0
                val workers = Resource.remoteFileWorkersByIndex
                for (index in workers!!.indices) {
                    if (workers[index] != null) {
                        totalArchives += workers[index]!!.getTotalArchives()
                        filesReceived += workers[index]!!.filesReceived()
                        currentFiles += workers[index]!!.currentRequestId()
                    }
                }
                val completion = currentFiles * 100 / totalArchives
                val i_24 = filesReceived * 10000 / totalArchives
                val cacheCompletion = "Cache:" + getCacheCompletionPercent(i_24.toLong(), 2, true, Language.ENGLISH) + "% (" + completion + "%)"
                Renderers.FONT_RENDERER!!.renderLeftAligned(cacheCompletion, currDrawX, currDrawY, -256, -1)
                currDrawY += 12
            }
        }
    }
}
