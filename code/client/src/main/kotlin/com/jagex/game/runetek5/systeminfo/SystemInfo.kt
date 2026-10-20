package com.jagex.game.runetek5.systeminfo

import jaclib.hardware_info.HardwareInfo
import java.awt.EventQueue
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.Client
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
    var gpuDriverVersion: String? = null
    var dwDirectxVersion: String? = null
    var dxLetterVersion: String? = null
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
        var versionPos = if (javaVersion.startsWith("1.")) 2 else 0
        var versionPart = 0
        var ch: Char
        try {
            while (versionPos < javaVersion.length) {
                ch = javaVersion[versionPos]
                if (ch < '0' || ch > '9') {
                    break
                }
                versionPart = ch.code - 48 + versionPart * 10
                ++versionPos
            }
        } catch (exception: Exception) {
        }
        javaBuild = versionPart
        versionPos = javaVersion.indexOf('.', 2) + 1
        versionPart = 0
        try {
            while (versionPos < javaVersion.length) {
                ch = javaVersion[versionPos]
                if (ch < '0' || ch > '9') {
                    break
                }
                versionPart = ch.code - 48 + versionPart * 10
                ++versionPos
            }
        } catch (exception: Exception) {
        }
        javaSubBuild = versionPart
        versionPos = javaVersion.indexOf('_', 4) + 1
        versionPart = 0
        try {
            while (versionPos < javaVersion.length) {
                ch = javaVersion[versionPos]
                if (ch < '0' || ch > '9') {
                    break
                }
                versionPart = ch.code - 48 + versionPart * 10
                ++versionPos
            }
        } catch (exception: Exception) {
        }
        javaUpdate = versionPart
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
                val cpuidByLeaf = HashMap<Int, RawCpuInfo>()
                for (i in 0 until rawCpuInfo.size / 5) {
                    val leaf = rawCpuInfo[i * 5]
                    val eax = rawCpuInfo[i * 5 + 1]
                    val ebx = rawCpuInfo[i * 5 + 2]
                    val ecx = rawCpuInfo[i * 5 + 3]
                    val edx = rawCpuInfo[i * 5 + 4]
                    val cpuidResult = RawCpuInfo(leaf, eax, ebx, ecx, edx)
                    cpuidByLeaf[leaf] = cpuidResult
                }
                val vendorLeaf = cpuidByLeaf[0]
                if (vendorLeaf != null) {
                    val vendorBuffer = Packet(13)
                    vendorBuffer.writeIntLittle(vendorLeaf.resultEBX)
                    vendorBuffer.writeIntLittle(vendorLeaf.resultEDX)
                    vendorBuffer.writeIntLittle(vendorLeaf.resultECX)
                    vendorBuffer.currentPosition = 0
                    cpuType = vendorBuffer.readString()
                }
                val featureLeaf = cpuidByLeaf[1]
                if (featureLeaf != null) {
                    cpuModelInfo = featureLeaf.resultEAX
                    val ebx = featureLeaf.resultEBX
                    cpuFamilyId = ebx shr 16 and 0xff
                    cpuFeatureFlags[0] = featureLeaf.resultECX
                    cpuFeatureFlags[1] = featureLeaf.resultEDX
                }
                val extendedLeaf = cpuidByLeaf[-2147483647]
                if (extendedLeaf != null) {
                    cpuFeatureFlags[2] = extendedLeaf.resultEDX
                }
                val brandBuffer = Packet(49)
                for (leaf in -2147483646..-2147483644) {
                    val brandLeaf = cpuidByLeaf[leaf]
                    if (brandLeaf != null) {
                        brandBuffer.writeIntLittle(brandLeaf.resultEAX)
                        brandBuffer.writeIntLittle(brandLeaf.resultEBX)
                        brandBuffer.writeIntLittle(brandLeaf.resultECX)
                        brandBuffer.writeIntLittle(brandLeaf.resultEDX)
                    }
                }
                brandBuffer.currentPosition = 0
                cpuData = brandBuffer.readString()
            }
            val dxdiagDisplayDeviceProps = HardwareInfo.getDXDiagDisplayDevicesProps()
            if (dxdiagDisplayDeviceProps != null && dxdiagDisplayDeviceProps.isNotEmpty() && dxdiagDisplayDeviceProps[0] != null) {
                var i = 0
                while (i < dxdiagDisplayDeviceProps[0].size) {
                    if ("szDescription".equals(dxdiagDisplayDeviceProps[0][i], ignoreCase = true)) {
                        gpuModel = dxdiagDisplayDeviceProps[0][i + 1]
                    } else if ("szDriverDateEnglish".equals(dxdiagDisplayDeviceProps[0][i], ignoreCase = true)) {
                        val driverDate = dxdiagDisplayDeviceProps[0][i + 1]
                        try {
                            val firstSlash = driverDate.indexOf('/')
                            val secondSlash = driverDate.indexOf('/', firstSlash + 1)
                            dxDriverMonth = driverDate.substring(0, firstSlash).toInt()
                            dxDriverYear = driverDate.substring(secondSlash + 1, driverDate.indexOf(' ', secondSlash)).toInt()
                        } catch (exception: Exception) {
                        }
                    }
                    i += 2
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
        gpuDriverVersion = ""
        if (dwDirectxVersion == null) {
            dwDirectxVersion = ""
        }
        dxLetterVersion = ""
        if (cpuType == null) {
            cpuType = ""
        }
        if (cpuData == null) {
            cpuData = ""
        }
        truncateStrings()
    }

    fun encode(): Int {
        val baseLength: Byte = 38
        var length = baseLength.toInt() + Utils.stringLengthPlus2(gpuModel)
        length += Utils.stringLengthPlus2(gpuDriverVersion)
        length += Utils.stringLengthPlus2(dwDirectxVersion)
        length += Utils.stringLengthPlus2(dxLetterVersion)
        length += Utils.stringLengthPlus2(cpuType)
        length += Utils.stringLengthPlus2(cpuData)
        return length
    }

    fun truncateStrings() {
        if (gpuModel!!.length > 40) {
            gpuModel = gpuModel!!.substring(0, 40)
        }
        if (gpuDriverVersion!!.length > 40) {
            gpuDriverVersion = gpuDriverVersion!!.substring(0, 40)
        }
        if (dwDirectxVersion!!.length > 10) {
            dwDirectxVersion = dwDirectxVersion!!.substring(0, 10)
        }
        if (dxLetterVersion!!.length > 10) {
            dxLetterVersion = dxLetterVersion!!.substring(0, 10)
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
        machineInfoBuffer.writeJagString(gpuDriverVersion ?: "")
        machineInfoBuffer.writeJagString(dwDirectxVersion ?: "")
        machineInfoBuffer.writeJagString(dxLetterVersion ?: "")
        machineInfoBuffer.writeByte(dxDriverMonth)
        machineInfoBuffer.writeShort(dxDriverYear)
        machineInfoBuffer.writeJagString(cpuType ?: "")
        machineInfoBuffer.writeJagString(cpuData ?: "")
        machineInfoBuffer.writeByte(cpuCores)
        machineInfoBuffer.writeByte(cpuFamilyId)
        for (i in cpuFeatureFlags.indices) {
            machineInfoBuffer.writeInt(cpuFeatureFlags[i])
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
        fun getCacheCompletionPercent(percentage: Long, decimalPlaces: Int, groupDigits: Boolean, language: Language): String {
            var decimalSeparator: Char = 44.toChar()
            var groupSeparator: Char = 46.toChar()
            if (language == Language.ENGLISH) {
                decimalSeparator = 46.toChar()
                groupSeparator = 44.toChar()
            }
            if (language == Language.FRENCH) {
                groupSeparator = 160.toChar()
            }
            var negative = false
            var mutablePercentage = percentage
            if (mutablePercentage < 0L) {
                negative = true
                mutablePercentage = -mutablePercentage
            }
            val builder = StringBuilder(26)
            var digitsSinceSeparator: Int
            var remaining: Int
            if (decimalPlaces > 0) {
                for (i in 0 until decimalPlaces) {
                    remaining = mutablePercentage.toInt()
                    mutablePercentage /= 10L
                    builder.append((remaining + 48 - mutablePercentage.toInt() * 10).toChar())
                }
                builder.append(decimalSeparator)
            }
            digitsSinceSeparator = 0
            while (true) {
                remaining = mutablePercentage.toInt()
                mutablePercentage /= 10L
                builder.append((remaining + 48 - mutablePercentage.toInt() * 10).toChar())
                if (mutablePercentage == 0L) {
                    if (negative) {
                        builder.append('-')
                    }
                    return builder.reverse().toString()
                }
                if (groupDigits) {
                    ++digitsSinceSeparator
                    if (digitsSinceSeparator % 3 == 0) {
                        builder.append(groupSeparator)
                    }
                }
            }
        }

        @JvmStatic
        fun drawDebugInformation(x: Int, y: Int, component: Component) {
            val drawX = component.width + x
            var drawY = y + 15
            if (Client.DRAW_DEBUG) {
                var fpsColor = -256
                if (GameShell.FPS < 20) {
                    fpsColor = -65536
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Fps:" + GameShell.FPS, drawX, drawY, fpsColor, -1)
                drawY += 15
                val runtime = Runtime.getRuntime()
                val totalMem = runtime.totalMemory() / 1024L
                val usedMem = totalMem - runtime.freeMemory() / 1024L
                var memColor = -256
                if (usedMem > 262144L) {
                    memColor = -65536
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Mem:$usedMem/${totalMem}k", drawX, drawY, memColor, -1)
                drawY += 15
                val gamePing = Client.GAME_CONNECTION_CONTEXT!!.pinger.getPing()
                var gamePingStr = "N/A"
                if (gamePing != -1L) {
                    gamePingStr = gamePing.toString() + "ms"
                    if (gamePing > 500L) {
                        gamePingStr = TextUtils.setTextColor(16711680) + gamePingStr + TextUtils.setTextColor(16776960)
                    }
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Game: In:" + Client.GAME_CONNECTION_CONTEXT!!.inBytesPerSec + "B/s Out:" + Client.GAME_CONNECTION_CONTEXT!!.outBytesPerSec + "B/s Ping:" + gamePingStr, drawX, drawY, -256, -1)
                drawY += 15
                val lobbyPing = Client.LOBBY_CONNECTION_CONTEXT!!.pinger.getPing()
                var lobbyPingStr = "N/A"
                if (lobbyPing != -1L) {
                    lobbyPingStr = lobbyPing.toString() + "ms"
                    if (lobbyPing > 500L) {
                        lobbyPingStr = TextUtils.setTextColor(16711680) + lobbyPingStr + TextUtils.setTextColor(16776960)
                    }
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Lobby: In:" + Client.LOBBY_CONNECTION_CONTEXT!!.inBytesPerSec + "B/s Out:" + Client.LOBBY_CONNECTION_CONTEXT!!.outBytesPerSec + "B/s Ping:" + lobbyPingStr, drawX, drawY, -256, -1)
                drawY += 15
                val offheapKb = Renderers.activeToolkit!!.getOffheap() / 1024
                FontManager.debugOverlayFont!!.renderLeftAligned("Offheap:${offheapKb}k", drawX, drawY, if (offheapKb > 65536) -65536 else -256, -1)
                drawY += 15
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
                val requestPercent = currentFiles * 100 / totalArchives
                val cachePercentHundredths = filesReceived * 10000 / totalArchives
                val cacheCompletion = "Cache:" + getCacheCompletionPercent(cachePercentHundredths.toLong(), 2, true, Language.ENGLISH) + "% (" + requestPercent + "%)"
                Renderers.FONT_RENDERER!!.renderLeftAligned(cacheCompletion, drawX, drawY, -256, -1)
                drawY += 12
            }
        }
    }
}
