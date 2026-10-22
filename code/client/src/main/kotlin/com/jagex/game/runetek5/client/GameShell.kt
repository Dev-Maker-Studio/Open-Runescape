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
import com.jagex.game.runetek5.cache.CacheLimits
import com.jagex.game.runetek5.cache.Decompressor
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.client.cycle.AbstractCycleManager
import com.jagex.game.runetek5.client.cycle.DecoupledCycleManager
import com.jagex.game.runetek5.client.cycle.FrameLimiter
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.window.DisplayModeDetails
import com.jagex.game.runetek5.client.window.ApplicationType
import com.jagex.game.runetek5.client.window.WindowSettings
import com.jagex.game.runetek5.client.window.WindowMode
import com.darkan.crash.CrashHandler
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.preference.impl.ToolkitPreference

abstract class GameShell : Engine, Runnable, FocusListener, WindowListener {

    var errorScreenRaised = false
    var jaclibLoaded = false

    companion object {
        @JvmStatic var pulseNs = 0L
        @JvmStatic var pulseCount = 0L

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

        @JvmField
        val PREFERED_CYCLE_DURATION: Long = 20000000L

        @JvmField
        val MAIN_REDRAW_CACHE: LongArray = LongArray(32)

        @JvmField
        val PULSE_CACHE: LongArray = LongArray(32)

        @JvmField
        var nextCanvasRefreshAt: Long = 0L

        @JvmField
        var renderPulseAlpha: Float = 1.0f

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
        var modernOpenglTried: Boolean = false

        @JvmField
        var mainRedrawCachePos: Int = 0

        @JvmField
        var suppliedApplet: Loader? = null

        @JvmStatic
        fun calculateGraphics(): Int {
            if (modernOpenglTried) {
                return PreferenceManager.calculateDisplayMode()
            }
            val performance = try {
                DisplayModeDetails.getProfilingPerformance(1000)
            } catch (exception: Exception) {
                exception.printStackTrace()
                -1
            }
            return if (performance == -1) PreferenceManager.calculateDisplayMode() else PreferenceManager.setHardwarePerformance(performance)
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
                    val firstByte = raf.read()
                    raf.seek(0L)
                    raf.write(firstByte)
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
                }
            }
        }

        @JvmStatic
        fun getPreferenceFile(): File? {
            return PreferenceManager.preferenceFile
        }

        @JvmStatic
        fun performDummyActionevent(canvas: Canvas?) {
            val eventQueue = SystemInfo.systemEventQueue
            if (eventQueue != null && eventQueue.peekEvent() == null) {
                try {
                    if (canvas != null) {
                        eventQueue.postEvent(ActionEvent(canvas, 1001, "dummy"))
                    }
                } catch (ignored: Exception) {
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
            return DecoupledCycleManager()
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
            screenCanvasWidth = windowSettings.width
            WindowMode.resizableScreenWidth = screenCanvasWidth
            var windowHeight = windowSettings.height
            System.getProperty("window.size")?.let { spec ->
                val parts = spec.split('x').mapNotNull { it.trim().toIntOrNull() }
                if (parts.size == 2 && parts[0] > 0 && parts[1] > 0) {
                    screenCanvasWidth = parts[0]
                    WindowMode.resizableScreenWidth = parts[0]
                    windowHeight = parts[1]
                }
            }
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
            raiseErrorScreen("crash")
            CrashHandler.fatal(throwable, "setting up the game window")
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
        }
        if (PreferenceManager.preferencesParentFile == null) {
            PreferenceManager.preferencesParentFile = "~/"
        }
        try {
            SystemInfo.systemEventQueue = Toolkit.getDefaultToolkit().systemEventQueue
        } catch (throwable: Throwable) {
        }
        val prefParentFile = PreferenceManager.preferencesParentFile ?: ""
        val cacheBaseDirs = arrayOf("c:/rscache/", "/rscache/", "c:/windows/", "c:/winnt/", "c:/", prefParentFile, "/tmp/", "")
        SystemInfo.cacheBaseDirectory = cacheBaseDirs
        val cacheSubpaths = arrayOf(".dk_cache_" + SystemInfo.storeId, ".file_store_" + SystemInfo.storeId)
        SystemInfo.cacheFilestoreSubpath = cacheSubpaths
        var failures = 0
        preferenceScan@ while (failures < 4) {
            PreferenceManager.preferenceFile = loadPreferenceFile(gameName, buildEnvironmentName, failures)
            if (!PreferenceManager.preferenceFile!!.exists()) {
                PreferenceManager.preferenceFile!!.mkdirs()
            }
            val files = PreferenceManager.preferenceFile!!.listFiles()
            if (files == null) {
                break
            }
            val prefFiles = files
            var fileIndex = 0
            while (true) {
                if (fileIndex >= prefFiles.size) {
                    break@preferenceScan
                }
                val file = prefFiles[fileIndex]
                if (!generateRandomAccessFile(file, false)) {
                    ++failures
                    break
                }
                ++fileIndex
            }
        }
        FileOperations.setPreferenceFile(PreferenceManager.preferenceFile!!)
        loadRandomFile()
        cacheFileData = FileOnDisk(JFS32(FileOperations.loadFromCacheDir("main_file_cache.dat2"), CacheLimits.DATA_BYTES), 5200)
        cacheFileReferenceTable = FileOnDisk(JFS32(FileOperations.loadFromCacheDir("main_file_cache.idx255"), 1048576L), 6000)
        Resource.referenceTableDecompressor = Decompressor(255, cacheFileData!!, cacheFileReferenceTable!!)
        FileOnDisk.cacheFileIndices = arrayOfNulls(SystemInfo.numIndices)
        for (i in 0 until SystemInfo.numIndices) {
            FileOnDisk.cacheFileIndices!![i] = FileOnDisk(JFS32(FileOperations.loadFromCacheDir("main_file_cache.idx$i"), CacheLimits.INDEX_BYTES), 6000)
        }
        try {
            WindowMode.activeWindowMode = WindowMode()
        } catch (exception: Exception) {
            WindowMode.fullScreenSupported = false
        }
        CursorManager.instance = CursorManager()
        var currentThreadGroup = Thread.currentThread().threadGroup
        var parentGroup = currentThreadGroup.parent
        while (parentGroup != null) {
            currentThreadGroup = parentGroup
            parentGroup = parentGroup.parent
        }
        val threads = arrayOfNulls<Thread>(1000)
        currentThreadGroup.enumerate(threads)
        for (threadIndex in threads.indices) {
            if (threads[threadIndex] != null && threads[threadIndex]!!.name.startsWith("AWT")) {
                threads[threadIndex]!!.priority = 1
            }
        }
        val engineThread = Thread(this, CrashHandler.GAME_THREAD_NAME)
        engineThread.isDaemon = true
        engineThread.start()
        engineThread.priority = 1
    }

    fun loadPreferenceFile(gameName: String, buildEnvironmentName: String, attempt: Int): File {
        val preferenceEnd = if (attempt == 0) "" else "" + attempt
        userPreferenceFile = File(PreferenceManager.preferencesParentFile + File.separatorChar + Loader.CACHE_DIR, "dk_cl_" + gameName + "_" + buildEnvironmentName + preferenceEnd + ".dat")
        var parentPath: String? = null
        var childPath: String? = null
        var preferenceWrite = false
        if (userPreferenceFile!!.exists()) {
            try {
                val fileSystem = JFS32(userPreferenceFile!!, 10000L)
                var buffer: Packet
                var bytesRead: Int
                buffer = Packet(fileSystem.length().toInt())
                while (buffer.currentPosition < buffer.buffer.size) {
                    bytesRead = fileSystem.getSeekOffset(buffer.buffer, buffer.currentPosition, buffer.buffer.size - buffer.currentPosition)
                    if (bytesRead == -1) {
                        throw IOException()
                    }
                    buffer.currentPosition += bytesRead
                }
                buffer.currentPosition = 0
                val formatVersion = buffer.readUByte()
                if (formatVersion < 1 || formatVersion > 3) {
                    throw IOException("" + formatVersion)
                }
                var childFlag = 0
                if (formatVersion > 1) {
                    childFlag = buffer.readUByte()
                }
                if (formatVersion <= 2) {
                    parentPath = buffer.readJagString()
                    if (childFlag == 1) {
                        childPath = buffer.readJagString()
                    }
                } else {
                    parentPath = buffer.readPathReference()
                    if (childFlag == 1) {
                        childPath = buffer.readPathReference()
                    }
                }
                fileSystem.close()
            } catch (ioexception: IOException) {
                ioexception.printStackTrace()
            }
            if (parentPath != null) {
                val parentDir = File(parentPath)
                if (!parentDir.exists()) {
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
        if (parentPath == null && attempt == 0) {
            cacheScan@ for (sub in SystemInfo.cacheFilestoreSubpath!!.indices) {
                for (base in SystemInfo.cacheBaseDirectory!!.indices) {
                    val cacheDir = File(SystemInfo.cacheBaseDirectory!![base] + SystemInfo.cacheFilestoreSubpath!![sub] + File.separatorChar + gameName + File.separatorChar)
                    if (cacheDir.exists() && generateRandomAccessFile(File(cacheDir, "test.dat"), true)) {
                        parentPath = cacheDir.toString()
                        preferenceWrite = true
                        break@cacheScan
                    }
                }
            }
        }
        if (parentPath == null) {
            parentPath = PreferenceManager.preferencesParentFile + File.separatorChar + Loader.CACHE_DIR + preferenceEnd + File.separatorChar + gameName + File.separatorChar + buildEnvironmentName + File.separatorChar
            preferenceWrite = true
        }
        if (childPath != null) {
            val childDir = File(childPath)
            val parentDir = File(parentPath!!)
            try {
                val childFiles = childDir.listFiles()
                val filesToMove = childFiles
                if (filesToMove != null) {
                    for (index in filesToMove.indices) {
                        val sourceFile = filesToMove[index]
                        val destFile = File(parentDir, sourceFile.name)
                        val renamed = sourceFile.renameTo(destFile)
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

    fun validateCacheDirectory(cacheDir: File?): Boolean {
        return when {
            cacheDir == null -> false
            !cacheDir.exists() -> false
            !cacheDir.isDirectory -> false
            cacheDir.listFiles()!!.size != 0 -> false
            !generateRandomAccessFile(File(cacheDir, "test.dat"), true) -> false
            else -> {
                writeToPreferences(cacheDir, PreferenceManager.preferenceFile!!)
                true
            }
        }
    }

    fun writeToPreferences(parentDir: File?, childDir: File?) {
        try {
            val fileSystem = JFS32(userPreferenceFile!!, 10000L)
            val buffer = Packet(500)
            buffer.writeByte(3)
            buffer.writeByte(if (childDir != null) 1 else 0)
            if (parentDir != null) {
                buffer.writePathReference(parentDir.path)
            }
            if (childDir != null) {
                buffer.writePathReference(childDir.path)
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
        System.getProperty("window.size")?.let { spec ->
            val parts = spec.split('x').mapNotNull { it.trim().toIntOrNull() }
            if (parts.size == 2 && parts[0] > 0 && parts[1] > 0) {
                WindowMode.resizableScreenWidth = parts[0]
                WindowMode.resizableScreenHeight = parts[1]
                screenCanvasWidth = parts[0]
                screenCanvasHeight = parts[1] * -1929118563
            }
        }
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

    override fun supplyApplet(applet: Loader) {
        suppliedApplet = applet
    }

    override fun run() {
        try {
            try {
                loadGame()
            } catch (threadDeath: ThreadDeath) {
                throw threadDeath
            } catch (throwable: Throwable) {
                raiseErrorScreen("crash")
                CrashHandler.fatal(throwable, "game loop (${getDebugString() ?: "no debug info"})")
                prepareExit()
                return
            }
            prepareExit()
        } catch (exception: Exception) {
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
        val pulseStartNs = System.nanoTime()
        onPulse()
        pulseNs += System.nanoTime() - pulseStartNs
        pulseCount++
    }

    private val FRAME_HIST = System.getProperty("frame.hist") != null

    fun mainRedrawWrapper() {
        val currentTime = Utils.time()
        val lastTime = MAIN_REDRAW_CACHE[mainRedrawCachePos]
        MAIN_REDRAW_CACHE[mainRedrawCachePos] = currentTime
        mainRedrawCachePos = mainRedrawCachePos + 1 and 0x1f
        if (lastTime != 0L && currentTime > lastTime) {
            val timeDelta = (currentTime - lastTime).toInt()
            FPS = ((timeDelta shr 1) + 32000) / timeDelta
        }
        if (currentTime >= nextCanvasRefreshAt) {
            val refreshT0 = System.nanoTime()
            nextCanvasRefreshAt = currentTime + 1000L
            WindowMode.redrawWindowBackground = true
            gameCanvas!!.setSize(screenCanvasWidth, screenCanvasHeight * -969250379)
            gameCanvas!!.isVisible = true
            if (engineFrame != null && WindowMode.fullScreenFrame == null) {
                val insets = engineFrame!!.insets
                gameCanvas!!.setLocation(insets.left + gameCanvasX, insets.top + gameCanvasY)
            } else {
                gameCanvas!!.setLocation(gameCanvasX, gameCanvasY)
            }
            Client.canvasRefreshNs += System.nanoTime() - refreshT0
        }
        val redrawT0 = System.nanoTime()
        mainRedraw()
        Client.redrawNs += System.nanoTime() - redrawT0
        frameHistogram(redrawT0)
    }

    private var fhLast = 0L
    private var fhCount = 0
    private var fhOver15 = 0
    private var fhOver30 = 0
    private var fhMax = 0.0
    private var fhSum = 0.0
    private fun frameHistogram(nowNs: Long) {
        if (!FRAME_HIST) return
        if (fhLast != 0L) {
            val ms = (nowNs - fhLast) / 1e6
            fhCount++; fhSum += ms
            if (ms > 15) fhOver15++
            if (ms > 30) fhOver30++
            if (ms > fhMax) fhMax = ms
            if (fhCount >= 300) {
                System.err.println("[FrameHist] frames=%d avg=%.2fms over15=%d over30=%d max=%.1fms".format(fhCount, fhSum / fhCount, fhOver15, fhOver30, fhMax))
                fhCount = 0; fhOver15 = 0; fhOver30 = 0; fhMax = 0.0; fhSum = 0.0
            }
        }
        fhLast = nowNs
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

    override fun focusGained(focusEvent: FocusEvent) {
        WindowMode.hasFocus = true
        WindowMode.redrawWindowBackground = true
    }

    override fun focusLost(focusEvent: FocusEvent) {
        WindowMode.hasFocus = false
    }

    override fun windowActivated(windowEvent: WindowEvent) {
    }

    override fun windowClosing(windowEvent: WindowEvent) {
        raiseBrowserErrorscreen = true
        destroy()
    }

    override fun windowDeactivated(windowEvent: WindowEvent) {
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
        }
        try {
            cacheFileData?.close()
            for (index in 0 until SystemInfo.numIndices) {
                FileOnDisk.cacheFileIndices?.get(index)?.close()
            }
            cacheFileReferenceTable?.close()
            playerRandomDat?.close()
        } catch (ignored: Exception) {
        }
        if (jaclibLoaded) {
            try {
                QueryPerformanceCounter.quit()
            } catch (ignored: Throwable) {
            }
        }
        if (AbstractToolkit.nativeLibraryLoaded()) {
            AbstractToolkit.getNativeLibraryLoader().shutdown()
        }
        if (gameCanvas != null) {
            try {
                gameCanvas!!.removeFocusListener(this)
                gameCanvas!!.parent.remove(gameCanvas)
            } catch (ignored: Exception) {
            }
        }
        if (engineFrame != null) {
            engineFrame!!.isVisible = false
            engineFrame!!.dispose()
            engineFrame = null
        }
    }

    override fun windowOpened(windowEvent: WindowEvent) {
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
            val firstByte = raf.read()
            raf.seek(0L)
            raf.write(firstByte)
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
                val javaVersion = SystemInfo.javaVersion
                if ("1.1" == javaVersion || javaVersion.startsWith("1.1.") || "1.2" == javaVersion || javaVersion.startsWith("1.2.") || "1.3" == javaVersion || javaVersion.startsWith("1.3.") || "1.4" == javaVersion || javaVersion.startsWith("1.4.") || "1.5" == javaVersion || javaVersion.startsWith("1.5.") || "1.6.0" == javaVersion) {
                    raiseErrorScreen("wrongjava")
                    return
                }
                if (javaVersion.startsWith("1.6.0_")) {
                    var charIndex = 6
                    while (charIndex < javaVersion.length && Utils.isNumber(javaVersion[charIndex])) {
                        charIndex++
                    }
                    val minorVersion = javaVersion.substring(6, charIndex)
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
        var nextDummyEventAt = 0L
        while (killTime == 0L || Utils.time() < killTime) {
            loopsInCycle = cycleManager!!.loopcycle(PREFERED_CYCLE_DURATION)
            for (loop in 0 until loopsInCycle) {
                onPulseWrapper()
            }
            renderPulseAlpha = cycleManager!!.pulseFraction(PREFERED_CYCLE_DURATION)
            mainRedrawWrapper()
            val now = Utils.time()
            if (now >= nextDummyEventAt) {
                performDummyActionevent(gameCanvas!!)
                nextDummyEventAt = now + 20L
            }
            FrameLimiter.pace()
        }
    }

    override fun windowClosed(windowEvent: WindowEvent) {
    }

    override fun windowIconified(windowEvent: WindowEvent) {
    }
}
