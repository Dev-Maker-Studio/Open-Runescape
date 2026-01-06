package com.jagex.game.runetek5.client

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.TimeTools
import com.jagex.game.runetek5.utils.FileOperations

import jaclib.nanotime.QueryPerformanceCounter

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.FileOnDisk
import com.jagex.game.runetek5.cache.JFS32
import com.jagex.game.runetek5.client.cycle.AbstractCycleManager
import com.jagex.game.runetek5.client.cycle.JaclibCycleManager
import com.jagex.game.runetek5.client.cycle.SystemCycleManager
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.window.DisplayModeDetails
import com.jagex.game.runetek5.client.window.ApplicationType
import com.jagex.game.runetek5.client.window.WindowSettings
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.utils.Utils

abstract class GameShell : Engine, Runnable, FocusListener, WindowListener {

    var errorScreenRaised = false
    var jaclibLoaded = false

    companion object {
        @JvmField
        var FPS: Int = 0

        @JvmField
        var gameCanvasX: Int = 0

        @JvmField
        var gameCanvasY: Int = 0

        @JvmField
        var maxMemory: Int = -1

        @JvmField
        var availableProcessors: Int = 1

        @JvmField
        var screenCanvasHeight: Int = 0

        @JvmField
        var engineFrame: Frame? = null

        @JvmField
        var cycleManager: AbstractCycleManager? = null

        @JvmField
        var translatedLoadingText: String? = null

        @JvmField
        @Volatile
        var clippingUpdateNeeded: Boolean = false

        @JvmField
        var raiseBrowserErrorscreen: Boolean = false

        @JvmField
        var cacheFileData: FileOnDisk? = null

        /**
         * Time in nanoseconds, comes down to 0.02s, 1 / 0.02 = 50 fps
         */
        @JvmField
        val PREFERED_CYCLE_DURATION: Long = 20000000L

        @JvmField
        val MAIN_REDRAW_CACHE: LongArray = LongArray(32)

        @JvmField
        val PULSE_CACHE: LongArray = LongArray(32)

        @JvmField
        var canvasRefreshCycle: Int = 500

        @JvmField
        @Volatile
        var canvasInitializationTime: Long = 0L

        @JvmField
        var playerRandomDat: FileOnDisk? = null

        @JvmField
        var cacheFileReferenceTable: FileOnDisk? = null

        @JvmField
        var currentApplicationType: ApplicationType? = null

        @JvmField
        var killTime: Long = 0L

        @JvmField
        var isShutdown: Boolean = false

        @JvmField
        var loopsInCycle: Int = 0

        @JvmField
        var gameCanvas: Canvas? = null

        @JvmField
        var pulseCachePos: Int = 0

        @JvmField
        var userPreferenceFile: File? = null

        @JvmField
        var screenCanvasWidth: Int = 0

        @JvmField
        var appletHasFocus: Boolean = false

        @JvmField
        var directxMode: Boolean = false

        @JvmField
        var openglMode: Boolean = false

        @JvmField
        var softwareMode: Boolean = false

        @JvmField
        var mainRedrawCachePos: Int = 0

        @JvmField
        var suppliedApplet: Loader? = null

        @JvmStatic
        fun calculateGraphics(): Int {
            var highMemory = true
            var enhancedCpuMode = false
            var windowsOs = false
            if (SystemInfo.SYSTEM_INFO!!.ram < 512 && SystemInfo.SYSTEM_INFO!!.ram != 0) {
                highMemory = false
            }
            if (SystemInfo.operatingSystemLowercase!!.startsWith("win")) {
                windowsOs = true
                enhancedCpuMode = true
            } else {
                enhancedCpuMode = true
            }
            if (softwareMode) {
                highMemory = false
            }
            if (openglMode) {
                enhancedCpuMode = false
            }
            if (directxMode) {
                windowsOs = false
            }
            if (!highMemory && !enhancedCpuMode && !windowsOs) {
                return PreferenceManager.calculateDisplayMode()
            } else {
                var performanceSoftwaremode = -1
                var performanceLdOpengl = -1
                var performanceDirectx = -1
                if (highMemory) {
                    try {
                        performanceSoftwaremode = DisplayModeDetails.getProfilingPerformance(2, 1000)
                    } catch (exception: Exception) {
                        // NOOP
                    }
                }
                if (windowsOs) {
                    try {
                        performanceDirectx = DisplayModeDetails.getProfilingPerformance(3, 1000)
                        if (PreferenceManager.preferences?.activeToolkit?.getValue() == 3) {
                            val renderInfo = Renderers.activeToolkit?.getRenderInfo()
                            val driverVersion = renderInfo?.driverVersion?.and(0xffffffffffffL)
                            when (renderInfo?.vendor) {
                                4098 -> {
                                    enhancedCpuMode = enhancedCpuMode and (driverVersion != null && driverVersion >= 60129613779L)
                                }
                                4318 -> {
                                    enhancedCpuMode = enhancedCpuMode and (driverVersion != null && driverVersion >= 64425238954L)
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                }
                if (enhancedCpuMode) {
                    try {
                        performanceLdOpengl = DisplayModeDetails.getProfilingPerformance(1, 1000)
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                }
                if (performanceSoftwaremode == -1 && performanceLdOpengl == -1 && performanceDirectx == -1) {
                    return PreferenceManager.calculateDisplayMode()
                }
                performanceDirectx = (performanceDirectx * 1.1f).toInt()
                performanceLdOpengl = (performanceLdOpengl * 1.1f).toInt()
                return if (performanceSoftwaremode > performanceDirectx && performanceSoftwaremode > performanceLdOpengl)
                    PreferenceManager.setSoftwaremodePerformance(performanceSoftwaremode)
                else
                    if (performanceDirectx > performanceLdOpengl)
                        PreferenceManager.setHardwarePerformance(3, performanceDirectx)
                    else
                        PreferenceManager.setHardwarePerformance(1, performanceLdOpengl)
            }
        }

        @JvmStatic
        fun loadRandomFile() {
            try {
                val randomFile = File(PreferenceManager.preferencesParentFile, "random.dat")
                if (randomFile.exists()) {
                    playerRandomDat = FileOnDisk(JFS32(randomFile, 25L), 24)
                } else {
                    val baseDir = SystemInfo.cacheBaseDirectory
                    val subPath = SystemInfo.cacheFilestoreSubpath
                    if (baseDir != null && subPath != null) {
                        loop@ for (sub in subPath.indices) {
                            for (base in baseDir.indices) {
                                val createdFile = File(baseDir[base] + subPath[sub] + File.separatorChar + "random.dat")
                                if (createdFile.exists()) {
                                    playerRandomDat = FileOnDisk(JFS32(createdFile, 25L), 24)
                                    break@loop
                                }
                            }
                        }
                    }
                }
                if (playerRandomDat == null) {
                    val raf = RandomAccessFile(randomFile, "rw")
                    val nByte = raf.read()
                    raf.seek(0L)
                    raf.write(nByte)
                    raf.seek(0L)
                    raf.close()
                    playerRandomDat = FileOnDisk(JFS32(randomFile, 25L), 24)
                }
            } catch (ioe: IOException) {
                ioe.printStackTrace()
            }
        }

        @JvmStatic
        fun resetDrawCache() {
            cycleManager?.reset()
            for (index in 0 until 32) {
                MAIN_REDRAW_CACHE[index] = 0L
            }
            for (index in 0 until 32) {
                PULSE_CACHE[index] = 0L
            }
            loopsInCycle = 0
        }

        @JvmStatic
        fun updateRandomDatUID192(data: Packet, startPosition: Int) {
            if (playerRandomDat != null) {
                try {
                    playerRandomDat?.setPosition(0L)
                    playerRandomDat?.write(data.buffer, startPosition, 24)
                } catch (exception: Exception) {
                    // NOOP
                }
            }
        }

        @JvmStatic
        fun getPreferenceFile(): File? {
            return PreferenceManager.preferenceFile
        }

        @JvmStatic
        fun performDummyActionevent(canvas: Canvas) {
            val eventQueue = SystemInfo.systemEventQueue
            if (eventQueue != null) {
                var i = 0
                while (i < 50 && eventQueue.peekEvent() != null) {
                    TimeTools.sleep(1L)
                    i++
                }

                try {
                    if (canvas != null) {
                        eventQueue.postEvent(ActionEvent(canvas, 1001, "dummy"))
                    }
                } catch (ignored: Exception) {
                    // NOOP
                }
            }
        }

        @JvmStatic
        fun gameCyclesPerSecond(): Int {
            return (1000000000L / PREFERED_CYCLE_DURATION).toInt()
        }

        @JvmStatic
        fun getApplicationType(): ApplicationType? {
            return currentApplicationType
        }

        @JvmStatic
        fun getCycleManager(): AbstractCycleManager {
            return try {
                JaclibCycleManager()
            } catch (trowable: Throwable) {
                SystemCycleManager()
            }
        }

        @JvmStatic
        fun setComponentsOnTop() {
            for (componentIndex in 0 until 107) {
                Client.IS_COMPONENT_ON_TOP[componentIndex] = true
            }
        }
    }

    fun initializeFrame(windowSettings: WindowSettings, gameName: String, buildEnvironmentName: String, storeId: Int, numIndices: Int, padding: Boolean) {
        try {
            setApplicationType(ApplicationType.BORDERLESS, padding)
            WindowMode.resizableScreenWidth = screenCanvasWidth.also { screenCanvasWidth = windowSettings.width }
            val windowHeight = windowSettings.height
            screenCanvasHeight = windowHeight * -1929118563
            WindowMode.resizableScreenHeight = windowHeight
            gameCanvasX = 0
            gameCanvasY = 0
            if (getApplicationType() == ApplicationType.BORDERED) {
                WindowMode.resizableScreenWidth += 2 * windowSettings.borderWidth()
                WindowMode.resizableScreenHeight += 2 * windowSettings.borderHeight()
                setFrameTitle(windowSettings.name)
            }
            StaticMethods.initialApplet = suppliedApplet
            initializeResources(gameName, buildEnvironmentName, storeId, numIndices)
        } catch (throwable: Throwable) {
            PacketEncoder.sendErrorToServer(null, throwable)
            raiseErrorScreen("crash")
        }
    }

    fun initializeResources(gameName: String, buildEnvironmentName: String, storeId: Int, numIndices: Int) {
        SystemInfo.numIndices = numIndices
        SystemInfo.storeId = storeId
        SystemInfo.clientBuild = Loader.MAJOR_BUILD
        SystemInfo.clientBuildSub = Loader.MINOR_BUILD
        SystemInfo.javaVendor = "Unknown"
        SystemInfo.javaVersion = "1.1"
        try {
            SystemInfo.javaVendor = System.getProperty("java.vendor")
            SystemInfo.javaVersion = System.getProperty("java.version")
        } catch (exception: Exception) {
            // NOOP
        }
        try {
            SystemInfo.operatingSystem = System.getProperty("os.name")
        } catch (exception: Exception) {
            SystemInfo.operatingSystem = "Unknown"
        }
        SystemInfo.operatingSystemLowercase = SystemInfo.operatingSystem.lowercase()
        try {
            SystemInfo.OS_ARCH = System.getProperty("os.arch").lowercase()
        } catch (exception: Exception) {
            SystemInfo.OS_ARCH = ""
        }
        try {
            SystemInfo.osVersion = System.getProperty("os.version").lowercase()
        } catch (exception: Exception) {
            SystemInfo.osVersion = ""
        }
        try {
            PreferenceManager.preferencesParentFile = System.getProperty("user.home")
            if (PreferenceManager.preferencesParentFile != null) {
                PreferenceManager.preferencesParentFile += "/"
            }
        } catch (exception: Exception) {
            // NOOP
        }
        try {
            if (SystemInfo.operatingSystemLowercase.startsWith("win")) {
                if (PreferenceManager.preferencesParentFile == null) {
                    PreferenceManager.preferencesParentFile = System.getenv("USERPROFILE")
                }
            } else if (PreferenceManager.preferencesParentFile == null) {
                PreferenceManager.preferencesParentFile = System.getenv("HOME")
            }
            if (PreferenceManager.preferencesParentFile != null) {
                PreferenceManager.preferencesParentFile += "/"
            }
        } catch (exception: Exception) {
            // NOOP
        }
        if (PreferenceManager.preferencesParentFile == null) {
            PreferenceManager.preferencesParentFile = "~/"
        }
        try {
            SystemInfo.systemEventQueue = Toolkit.getDefaultToolkit().systemEventQueue
        } catch (throwable: Throwable) {
            // NOOP
        }
        val prefParentFile = PreferenceManager.preferencesParentFile ?: ""
        val cacheBaseDirs = arrayOf("c:/rscache/", "/rscache/", "c:/windows/", "c:/winnt/", "c:/", prefParentFile, "/tmp/", "")
        SystemInfo.cacheBaseDirectory = cacheBaseDirs
        val cacheSubpaths = arrayOf(".dk_cache_" + SystemInfo.storeId, ".file_store_" + SystemInfo.storeId)
        SystemInfo.cacheFilestoreSubpath = cacheSubpaths
        var failures = 0
        label131@ while (failures < 4) {
            PreferenceManager.preferenceFile = loadPreferenceFile(gameName, buildEnvironmentName, failures)
            if (!PreferenceManager.preferenceFile!!.exists()) {
                PreferenceManager.preferenceFile!!.mkdirs()
            }
            val files = PreferenceManager.preferenceFile!!.listFiles()
            if (files == null) {
                break
            }
            val Files = files
            var fileIndex = 0
            while (true) {
                if (fileIndex >= Files.size) {
                    break@label131
                }
                val file = Files[fileIndex]
                if (!generateRandomAccessFile(file, false)) {
                    ++failures
                    break
                }
                ++fileIndex
            }
        }
        FileOperations.setPreferenceFile(PreferenceManager.preferenceFile!!)
        loadRandomFile()
        cacheFileData = FileOnDisk(JFS32(FileOperations.loadFromCacheDir("main_file_cache.dat2"), 524288000L), 5200)
        cacheFileReferenceTable = FileOnDisk(JFS32(FileOperations.loadFromCacheDir("main_file_cache.idx255"), 1048576L), 6000)
        FileOnDisk.cacheFileIndices = arrayOfNulls(SystemInfo.numIndices)
        for (i in 0 until SystemInfo.numIndices) {
            FileOnDisk.cacheFileIndices!![i] = FileOnDisk(JFS32(FileOperations.loadFromCacheDir("main_file_cache.idx$i"), 1048576L), 6000)
        }
        try {
            WindowMode.activeWindowMode = WindowMode()
        } catch (exception: Exception) {
            WindowMode.fullScreenSupported = false
        }
        CursorManager.instance = CursorManager()
        var currentThreadgroup = Thread.currentThread().threadGroup
        var parentGroup = currentThreadgroup.parent
        while (parentGroup != null) {
            currentThreadgroup = parentGroup
            parentGroup = parentGroup.parent
        }
        val threadPool = arrayOfNulls<Thread>(1000)
        currentThreadgroup.enumerate(threadPool)
        for (threadIndex in threadPool.indices) {
            if (threadPool[threadIndex] != null && threadPool[threadIndex]!!.name.startsWith("AWT")) {
                threadPool[threadIndex]!!.priority = 1
            }
        }
        val engineThread = Thread(this)
        engineThread.isDaemon = true
        engineThread.start()
        engineThread.priority = 1
    }

    fun loadPreferenceFile(gameName: String, buildEnvironmentName: String, version: Int): File {
        val preferenceEnd = if (version == 0) "" else "" + version
        userPreferenceFile = File(PreferenceManager.preferencesParentFile + File.separatorChar + Loader.CACHE_DIR, "dk_cl_" + gameName + "_" + buildEnvironmentName + preferenceEnd + ".dat")
        var parentPath: String? = null
        var childPath: String? = null
        var preferenceWrite = false
        if (userPreferenceFile!!.exists()) {
            try {
                val prefFile = JFS32(userPreferenceFile!!, 10000L)
                var buffer: Packet
                var bufferOffset: Int
                buffer = Packet(prefFile.length().toInt())
                while (buffer.currentPosition < buffer.buffer.size) {
                    bufferOffset = prefFile.getSeekOffset(buffer.buffer, buffer.currentPosition, buffer.buffer.size - buffer.currentPosition)
                    if (bufferOffset == -1) {
                        throw IOException()
                    }
                    buffer.currentPosition += bufferOffset
                }
                buffer.currentPosition = 0
                val contentType = buffer.readUByte()
                if (contentType < 1 || contentType > 3) {
                    throw IOException("" + contentType)
                }
                var childType = 0
                if (contentType > 1) {
                    childType = buffer.readUByte()
                }
                if (contentType <= 2) {
                    parentPath = buffer.readJagString()
                    if (childType == 1) {
                        childPath = buffer.readJagString()
                    }
                } else {
                    parentPath = buffer.readPathReference()
                    if (childType == 1) {
                        childPath = buffer.readPathReference()
                    }
                }
                prefFile.close()
            } catch (ioexception: IOException) {
                ioexception.printStackTrace()
            }
            if (parentPath != null) {
                val parentFile = File(parentPath)
                if (!parentFile.exists()) {
                    parentPath = null
                }
            }
            if (parentPath != null) {
                val testFile = File(parentPath, "test.dat")
                if (!generateRandomAccessFile(testFile, true)) {
                    parentPath = null
                }
            }
        }
        if (parentPath == null && version == 0) {
            label103@ for (i_17 in SystemInfo.cacheFilestoreSubpath!!.indices) {
                for (i_10 in SystemInfo.cacheBaseDirectory!!.indices) {
                    val file23 = File(SystemInfo.cacheBaseDirectory!![i_10] + SystemInfo.cacheFilestoreSubpath!![i_17] + File.separatorChar + gameName + File.separatorChar)
                    if (file23.exists() && generateRandomAccessFile(File(file23, "test.dat"), true)) {
                        parentPath = file23.toString()
                        preferenceWrite = true
                        break@label103
                    }
                }
            }
        }
        if (parentPath == null) {
            parentPath = PreferenceManager.preferencesParentFile + File.separatorChar + Loader.CACHE_DIR + preferenceEnd + File.separatorChar + gameName + File.separatorChar + buildEnvironmentName + File.separatorChar
            preferenceWrite = true
        }
        if (childPath != null) {
            val childFile = File(childPath)
            val parentFile = File(parentPath!!)
            try {
                val childFiles = childFile.listFiles()
                val ChildFiles = childFiles
                if (ChildFiles != null) {
                    for (index in ChildFiles.indices) {
                        val ChildFile = ChildFiles[index]
                        val newFile = File(parentFile, ChildFile.name)
                        val renamed = ChildFile.renameTo(newFile)
                        if (!renamed) {
                            throw IOException()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            preferenceWrite = true
        }
        if (preferenceWrite) {
            writeToPreferences(File(parentPath!!), null)
        }
        return File(parentPath!!)
    }

    fun method4658(file1: File?): Boolean {
        return when {
            file1 == null -> false
            !file1.exists() -> false
            !file1.isDirectory -> false
            file1.listFiles()!!.size != 0 -> false
            !generateRandomAccessFile(File(file1, "test.dat"), true) -> false
            else -> {
                writeToPreferences(file1, PreferenceManager.preferenceFile!!)
                true
            }
        }
    }

    fun writeToPreferences(file1: File?, file2: File?) {
        try {
            val fileSystem = JFS32(userPreferenceFile!!, 10000L)
            val buffer = Packet(500)
            buffer.writeByte(3)
            buffer.writeByte(if (file2 != null) 1 else 0)
            if (file1 != null) {
                buffer.writePathReference(file1.path)
            }
            if (file2 != null) {
                buffer.writePathReference(file2.path)
            }
            fileSystem.write(buffer.buffer, 0, buffer.currentPosition)
            fileSystem.close()
        } catch (ioexception: IOException) {
            ioexception.printStackTrace()
        }
    }

    @Synchronized
    fun setFrameTitle(frameTitle: String) {
        engineFrame = Frame()
        engineFrame!!.title = frameTitle
        engineFrame!!.isResizable = true
        engineFrame!!.addWindowListener(this)
        engineFrame!!.background = Color.black
        engineFrame!!.isVisible = true
        engineFrame!!.toFront()
        val insets = engineFrame!!.insets
        engineFrame!!.setSize(insets.right + insets.left + WindowMode.resizableScreenWidth, insets.top + insets.bottom + WindowMode.resizableScreenHeight)
    }

    fun loadJagtheora(): Boolean {
        return AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jagtheora")
    }

    fun resizeApplet(container: Container) {
        container.background = Color.black
        container.layout = null
        container.add(gameCanvas)
        gameCanvas!!.setSize(screenCanvasWidth, screenCanvasHeight * -969250379)
        gameCanvas!!.isVisible = true
        if (container === engineFrame) {
            val insets = engineFrame!!.insets
            gameCanvas!!.setLocation(insets.left + gameCanvasX, insets.top + gameCanvasY)
        } else {
            gameCanvas!!.setLocation(gameCanvasX, gameCanvasY)
        }
        gameCanvas!!.addFocusListener(this)
        gameCanvas!!.requestFocus()
        appletHasFocus = true
        WindowMode.hasFocus = true
        gameCanvas!!.focusTraversalKeysEnabled = false
        WindowMode.redrawWindowBackground = true
        clippingUpdateNeeded = false
        canvasInitializationTime = Utils.time()
    }

    override fun supplyApplet(applet_1: Loader) {
        suppliedApplet = applet_1
    }

    override fun run() {
        try {
            try {
                loadGame()
            } catch (threadDeath: ThreadDeath) {
                throw threadDeath
            } catch (throwable: Throwable) {
                PacketEncoder.sendErrorToServer(getDebugString(), throwable)
                raiseErrorScreen("crash")
                prepareExit()
                return
            }
            prepareExit()
        } catch (exception4: Exception) {
            prepareExit()
        }
    }

    fun onPulseWrapper() {
        val currentTime = Utils.time()
        val lastTime = PULSE_CACHE[pulseCachePos]
        PULSE_CACHE[pulseCachePos] = currentTime
        pulseCachePos = pulseCachePos + 1 and 0x1f
        if (lastTime != 0L) {
        }
        synchronized(this) {
            appletHasFocus = WindowMode.hasFocus
        }
        onPulse()
    }

    fun mainRedrawWrapper() {
        val currentTime = Utils.time()
        val lastTime = MAIN_REDRAW_CACHE[mainRedrawCachePos]
        MAIN_REDRAW_CACHE[mainRedrawCachePos] = currentTime
        mainRedrawCachePos = mainRedrawCachePos + 1 and 0x1f
        if (lastTime != 0L && currentTime > lastTime) {
            val timeDelta = (currentTime - lastTime).toInt()
            FPS = ((timeDelta shr 1) + 32000) / timeDelta
        }
        if (++canvasRefreshCycle - 1 > 50) {
            canvasRefreshCycle -= 50
            WindowMode.redrawWindowBackground = true
            gameCanvas!!.setSize(screenCanvasWidth, screenCanvasHeight * -969250379)
            gameCanvas!!.isVisible = true
            if (engineFrame != null && WindowMode.fullScreenFrame == null) {
                val insets = engineFrame!!.insets
                gameCanvas!!.setLocation(insets.left + gameCanvasX, insets.top + gameCanvasY)
            } else {
                gameCanvas!!.setLocation(gameCanvasX, gameCanvasY)
            }
        }
        mainRedraw()
    }

    open fun getDebugString(): String? {
        return null
    }

    override fun destroy() {
        if (!isShutdown) {
            killTime = Utils.time()
            TimeTools.sleep(5000L)
            prepareExit()
        }
    }

    @Synchronized
    override fun paint(graphics: Graphics) {
        if (!isShutdown) {
            WindowMode.redrawWindowBackground = true
            if (Utils.time() - canvasInitializationTime > 1000L) {
                val rectangle = graphics.clipBounds
                if (rectangle == null || rectangle.width >= WindowMode.resizableScreenWidth && rectangle.height >= WindowMode.resizableScreenHeight) {
                    clippingUpdateNeeded = true
                }
            }
        }
    }

    override fun focusGained(focusevent1: FocusEvent) {
        WindowMode.hasFocus = true
        WindowMode.redrawWindowBackground = true
    }

    override fun focusLost(focusevent: FocusEvent) {
        WindowMode.hasFocus = false
    }

    override fun windowActivated(windowEvent: WindowEvent) {
        // NOOP
    }

    override fun windowClosing(windowEvent: WindowEvent) {
        raiseBrowserErrorscreen = true
        destroy()
    }

    override fun windowDeactivated(windowEvent: WindowEvent) {
        // NOOP
    }

    fun prepareExit() {
        synchronized(this) {
            if (isShutdown) {
                return
            }
            isShutdown = true
        }
        try {
            shutdown()
        } catch (ignored: Exception) {
            // NOOP
        }
        try {
            cacheFileData?.close()
            for (index in 0 until SystemInfo.numIndices) {
                FileOnDisk.cacheFileIndices?.get(index)?.close()
            }
            cacheFileReferenceTable?.close()
            playerRandomDat?.close()
        } catch (ignored: Exception) {
            // NOOP
        }
        if (jaclibLoaded) {
            try {
                QueryPerformanceCounter.quit()
            } catch (ignored: Throwable) {
                // NOOP
            }
        }
        // SW3D removed - no longer need FinalizeCacheSw3d.shutdown()
        if (AbstractToolkit.nativeLibraryLoaded()) {
            AbstractToolkit.getNativeLibraryLoader().shutdown()
        }
        if (gameCanvas != null) {
            try {
                gameCanvas!!.removeFocusListener(this)
                gameCanvas!!.parent.remove(gameCanvas)
            } catch (ignored: Exception) {
                // NOOP
            }
        }
        if (engineFrame != null) {
            engineFrame!!.isVisible = false
            engineFrame!!.dispose()
            engineFrame = null
        }
    }

    override fun windowOpened(windowEvent: WindowEvent) {
        // NOOP
    }

    abstract override fun init()

    abstract fun initializeEssentials()

    abstract fun onPulse()

    abstract fun mainRedraw()

    fun raiseErrorScreen(errorName: String) {
        if (!errorScreenRaised) {
            errorScreenRaised = true
            println("error_game_$errorName")
        }
    }

    fun startJaclib(): Boolean {
        jaclibLoaded = AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jaclib")
        if (jaclibLoaded) {
            try {
                QueryPerformanceCounter.init()
            } catch (throwable: Throwable) {
                // NOOP
            }
        }
        return jaclibLoaded
    }

    fun setApplicationType(applicationType: ApplicationType?, padding: Boolean) {
        if (applicationType == null) {
            throw NullPointerException()
        }
        if (applicationType != ApplicationType.BORDERLESS && applicationType != ApplicationType.APPLET) {
            throw IllegalStateException()
        }
        currentApplicationType = applicationType
        if (currentApplicationType != ApplicationType.APPLET && padding) {
            currentApplicationType = ApplicationType.BORDERED
        }
    }

    override fun windowDeiconified(windowEvent: WindowEvent) {
        // NOOP
    }

    override fun start() {
        if (!isShutdown) {
            killTime = 0L
        }
    }

    @Synchronized
    fun buildCanvas() {
        resetCanvas()
        val container = WindowMode.getActiveContainer()
        gameCanvas = GameCanvas(container)
        resizeApplet(container)
    }

    abstract fun shutdown()

    fun generateRandomAccessFile(source: File, deleteSource: Boolean): Boolean {
        return try {
            val raf = RandomAccessFile(source, "rw")
            val value = raf.read()
            raf.seek(0L)
            raf.write(value)
            raf.seek(0L)
            raf.close()
            if (deleteSource) {
                source.delete()
            }
            true
        } catch (exception: Exception) {
            false
        }
    }

    fun resetCanvas() {
        if (gameCanvas != null) {
            gameCanvas!!.removeFocusListener(this)
            gameCanvas!!.parent.background = Color.black
            gameCanvas!!.parent.remove(gameCanvas)
        }
    }

    fun loadGame() {
        if (SystemInfo.javaVendor != null) {
            val vendor = SystemInfo.javaVendor!!.lowercase()
            if (vendor.indexOf("sun") != -1 || vendor.indexOf("apple") != -1) {
                val version = SystemInfo.javaVersion
                if ("1.1" == version || version.startsWith("1.1.") || "1.2" == version || version.startsWith("1.2.") || "1.3" == version || version.startsWith("1.3.") || "1.4" == version || version.startsWith("1.4.") || "1.5" == version || version.startsWith("1.5.") || "1.6.0" == version) {
                    raiseErrorScreen("wrongjava")
                    return
                }
                if (version.startsWith("1.6.0_")) {
                    var charIndex = 6
                    while (charIndex < version.length && Utils.isNumber(version[charIndex])) {
                        charIndex++
                    }
                    val minorVersion = version.substring(6, charIndex)
                    if (Utils.isWrongJava(minorVersion) && Utils.parseInt(minorVersion) < 10) {
                        raiseErrorScreen("wrongjava")
                        return
                    }
                }
            }
        }
        val container = WindowMode.getActiveContainer()
        container.isFocusCycleRoot = true
        maxMemory = (Runtime.getRuntime().maxMemory() / 1048576L).toInt() + 1
        availableProcessors = Runtime.getRuntime().availableProcessors()
        buildCanvas()
        initializeEssentials()
        cycleManager = getCycleManager()
        while (killTime == 0L || Utils.time() < killTime) {
            loopsInCycle = cycleManager!!.loopcycle(PREFERED_CYCLE_DURATION)
            for (loop in 0 until loopsInCycle) {
                onPulseWrapper()
            }
            mainRedrawWrapper()
            performDummyActionevent(gameCanvas!!)
        }
    }

    override fun windowClosed(windowEvent: WindowEvent) {
    }

    override fun windowIconified(windowEvent: WindowEvent) {
    }
}
