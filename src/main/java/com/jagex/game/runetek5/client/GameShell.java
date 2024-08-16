package com.jagex.game.runetek5.client;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.tools.TimeTools;
import com.jagex.game.runetek5.utils.FileOperations;

import jaclib.nanotime.QueryPerformanceCounter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.FileOnDisk;
import com.jagex.game.runetek5.cache.JFS32;
import com.jagex.game.runetek5.client.cycle.AbstractCycleManager;
import com.jagex.game.runetek5.client.cycle.JaclibCycleManager;
import com.jagex.game.runetek5.client.cycle.SystemCycleManager;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.client.window.DisplayModeDetails;
import com.jagex.game.runetek5.client.window.ApplicationType;
import com.jagex.game.runetek5.client.window.WindowSettings;
import com.jagex.game.runetek5.client.window.WindowMode;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.rendering.RenderInfo;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.utils.Utils;

public abstract class GameShell implements Engine, Runnable, FocusListener, WindowListener {

    public static int FPS;
    public static int gameCanvasX;
    public static int gameCanvasY;
    public static int maxMemory = -1;
    public static int availableProcessors = 1;
    public static int screenCanvasHeight;
    public static Frame engineFrame;
    public static AbstractCycleManager cycleManager;
    public static String translatedLoadingText;
    public static volatile boolean clippingUpdateNeeded;
    public static boolean raiseBrowserErrorscreen;
    public static FileOnDisk cacheFileData;
    
    /**
     * Time in nanoseconds, comes down to 0.02s, 1 / 0.02 = 50 fps
     */
    public static final long PREFERED_CYCLE_DURATION = 20000000L;
    public static final long[] MAIN_REDRAW_CACHE = new long[32];
    public static final long[] PULSE_CACHE = new long[32];
    public static int canvasRefreshCycle = 500;
    public static volatile long canvasInitializationTime;
    public static FileOnDisk playerRandomDat;
    public static FileOnDisk cacheFileReferenceTable;
    public static ApplicationType currentApplicationType;
    public static long killTime;
    public static boolean isShutdown;
    public static int loopsInCycle;
    public static Canvas gameCanvas;
    public static int pulseCachePos;
    public static File userPreferenceFile;
    public boolean errorScreenRaised;
    public boolean jaclibLoaded;
	public static int screenCanvasWidth;
	public static boolean appletHasFocus;
	public static boolean directxMode;
	public static boolean openglMode;
	public static boolean softwareMode;
	public static int mainRedrawCachePos;
	public static Loader suppliedApplet;

    public static int calculateGraphics() {
        boolean highMemory = true;
        boolean enhancedCpuMode = false;
        boolean windowsOs = false;
        if (SystemInfo.SYSTEM_INFO.ram < 512 && SystemInfo.SYSTEM_INFO.ram != 0) {
            highMemory = false;
        }
        if (SystemInfo.operatingSystemLowercase.startsWith("win")) {
            windowsOs = true;
            enhancedCpuMode = true;
        } else {
            enhancedCpuMode = true;
        }
        if (GameShell.softwareMode) {
            highMemory = false;
        }
        if (GameShell.openglMode) {
            enhancedCpuMode = false;
        }
        if (GameShell.directxMode) {
            windowsOs = false;
        }
        if (!highMemory && !enhancedCpuMode && !windowsOs) {
            return PreferenceManager.calculateDisplayMode();
        } else {
            int performanceSoftwaremode = -1;
            int performanceLdOpengl = -1;
            int performanceDirectx = -1;
            if (highMemory) {
                try {
                    performanceSoftwaremode = DisplayModeDetails.getProfilingPerformance(2, 1000);
                } catch (Exception exception) {
                	// NOOP
                }
            }
            if (windowsOs) {
                try {
                    performanceDirectx = DisplayModeDetails.getProfilingPerformance(3, 1000);
                    if (PreferenceManager.preferences.activeToolkit.getValue() == 3) {
                        RenderInfo renderInfo = Renderers.activeToolkit.getRenderInfo();
                        long driverVersion = renderInfo.driverVersion & 0xffffffffffffL;
                        switch (renderInfo.vendor) {
                            case 4098:
                                enhancedCpuMode &= driverVersion >= 60129613779L;
                                break;
                            case 4318:
                                enhancedCpuMode &= driverVersion >= 64425238954L;
                        }
                    }
                } catch (Exception exception) {
                	exception.printStackTrace();
                }
            }
            if (enhancedCpuMode) {
                try {
                    performanceLdOpengl = DisplayModeDetails.getProfilingPerformance(1, 1000);
                } catch (Exception exception) {
                	exception.printStackTrace();
                }
            }
            if (performanceSoftwaremode == -1 && performanceLdOpengl == -1 && performanceDirectx == -1) {
                return PreferenceManager.calculateDisplayMode();
            } 
            performanceDirectx = (int) (performanceDirectx * 1.1F);
            performanceLdOpengl = (int) (performanceLdOpengl * 1.1F);
            return performanceSoftwaremode > performanceDirectx && performanceSoftwaremode > performanceLdOpengl ?
            		PreferenceManager.setSoftwaremodePerformance(performanceSoftwaremode) :
            			(performanceDirectx > performanceLdOpengl ? PreferenceManager.setHardwarePerformance(3, performanceDirectx) :
            				PreferenceManager.setHardwarePerformance(1, performanceLdOpengl));
        }
    }

    public static void loadRandomFile() {
        try {
            File randomFile = new File(PreferenceManager.preferencesParentFile, "random.dat");
            if (randomFile.exists()) {
                playerRandomDat = new FileOnDisk(new JFS32(randomFile, 25L), 24);
            } else {
            	CreateRandomFile:
                for (int sub = 0; sub < SystemInfo.cacheFilestoreSubpath.length; sub++) {
                    for (int base = 0; base < SystemInfo.cacheBaseDirectory.length; base++) {
                        File createdFile = new File(SystemInfo.cacheBaseDirectory[base] + SystemInfo.cacheFilestoreSubpath[sub] + File.separatorChar + "random.dat");
                        if (createdFile.exists()) {
                            playerRandomDat = new FileOnDisk(new JFS32(createdFile, 25L), 24);
                            break CreateRandomFile;
                        }
                    }
                }
            }
            if (playerRandomDat == null) {
                RandomAccessFile raf = new RandomAccessFile(randomFile, "rw");
                int nByte = raf.read();
                raf.seek(0L);
                raf.write(nByte);
                raf.seek(0L);
                raf.close();
                playerRandomDat = new FileOnDisk(new JFS32(randomFile, 25L), 24);
            }
        } catch (IOException ioe) {
        	ioe.printStackTrace();
        }
    }


	public static void resetDrawCache() {
        cycleManager.reset();
        for (int index = 0; index < 32; index++) {
            MAIN_REDRAW_CACHE[index] = 0L;
        }
        for (int index = 0; index < 32; index++) {
            PULSE_CACHE[index] = 0L;
        }
        loopsInCycle = 0;
    }

    public static void updateRandomDatUID192(Packet data, int startPosition) {
        if (playerRandomDat != null) {
            try {
                playerRandomDat.setPosition(0L);
                playerRandomDat.write(data.buffer, startPosition, 24);
            } catch (Exception exception) {
            	// NOOP
            }
        }
    }

    void initializeFrame(WindowSettings windowSettings, String gameName, String buildEnvironmentName, int storeId, int numIndices, boolean padding) {
        try {
            setApplicationType(ApplicationType.BORDERLESS, padding);
            WindowMode.resizableScreenWidth = GameShell.screenCanvasWidth = windowSettings.getWidth();
            int windowHeight = windowSettings.getHeight();
            screenCanvasHeight = windowHeight * -1929118563;
            WindowMode.resizableScreenHeight = windowHeight;
            gameCanvasX = 0;
            gameCanvasY = 0;
            if (GameShell.getApplicationType() == ApplicationType.BORDERED) {
                WindowMode.resizableScreenWidth += 2 * windowSettings.borderWidth();
                WindowMode.resizableScreenHeight += 2 * windowSettings.borderHeight();
                setFrameTitle(windowSettings.getName());
            }
            StaticMethods.initialApplet = GameShell.suppliedApplet;
            initializeResources(gameName, buildEnvironmentName, storeId, numIndices);
        } catch (Throwable throwable) {
            PacketEncoder.sendErrorToServer(null, throwable);
            raiseErrorScreen("crash");
        }
    }

    public void initializeResources(String gameName, String buildEnvironmentName, int storeId, int numIndices) throws Exception {
        SystemInfo.numIndices = numIndices;
        SystemInfo.storeId = storeId;
        SystemInfo.clientBuild = 727;
        SystemInfo.clientBuildSub = 1;
        SystemInfo.javaVendor = "Unknown";
        SystemInfo.javaVersion = "1.1";
        try {
            SystemInfo.javaVendor = System.getProperty("java.vendor");
            SystemInfo.javaVersion = System.getProperty("java.version");
        } catch (Exception exception) {
        	// NOOP
        }
        try {
            SystemInfo.operatingSystem = System.getProperty("os.name");
        } catch (Exception exception) {
            SystemInfo.operatingSystem = "Unknown";
        }
        SystemInfo.operatingSystemLowercase = SystemInfo.operatingSystem.toLowerCase();
        try {
            SystemInfo.OS_ARCH = System.getProperty("os.arch").toLowerCase();
        } catch (Exception exception) {
            SystemInfo.OS_ARCH = "";
        }
        try {
            SystemInfo.osVersion = System.getProperty("os.version").toLowerCase();
        } catch (Exception exception) {
            SystemInfo.osVersion = "";
        }
        try {
            PreferenceManager.preferencesParentFile = System.getProperty("user.home");
            if (PreferenceManager.preferencesParentFile != null) {
                PreferenceManager.preferencesParentFile += "/";
            }
        } catch (Exception exception) {
        	// NOOP
        }
        try {
            if (SystemInfo.operatingSystemLowercase.startsWith("win")) {
                if (PreferenceManager.preferencesParentFile == null) {
                    PreferenceManager.preferencesParentFile = System.getenv("USERPROFILE");
                }
            } else if (PreferenceManager.preferencesParentFile == null) {
                PreferenceManager.preferencesParentFile = System.getenv("HOME");
            }
            if (PreferenceManager.preferencesParentFile != null) {
                PreferenceManager.preferencesParentFile += "/";
            }
        } catch (Exception exception) {
        	// NOOP
        }
        if (PreferenceManager.preferencesParentFile == null) {
            PreferenceManager.preferencesParentFile = "~/";
        }
        try {
            SystemInfo.systemEventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
        } catch (Throwable throwable) {
        	// NOOP
        }
        SystemInfo.cacheBaseDirectory = new String[]{"c:/rscache/", "/rscache/", "c:/windows/", "c:/winnt/", "c:/", PreferenceManager.preferencesParentFile, "/tmp/", ""};
        SystemInfo.cacheFilestoreSubpath = new String[]{".dk_cache_" + SystemInfo.storeId, ".file_store_" + SystemInfo.storeId};
        int failures = 0;
        label131:
        while (failures < 4) {
            PreferenceManager.preferenceFile = loadPreferenceFile(gameName, buildEnvironmentName, failures);
            if (!PreferenceManager.preferenceFile.exists()) {
                PreferenceManager.preferenceFile.mkdirs();
            }
            File[] files = PreferenceManager.preferenceFile.listFiles();
            if (files == null) {
                break;
            }
            File[] Files = files;
            int fileIndex = 0;
            while (true) {
                if (fileIndex >= Files.length) {
                    break label131;
                }
                File file = Files[fileIndex];
                if (!generateRandomAccessFile(file, false)) {
                    ++failures;
                    break;
                }
                ++fileIndex;
            }
        }
        FileOperations.setPreferenceFile(PreferenceManager.preferenceFile);
        loadRandomFile();
        cacheFileData = new FileOnDisk(new JFS32(FileOperations.loadFromCacheDir("main_file_cache.dat2"), 524288000L), 5200);
        cacheFileReferenceTable = new FileOnDisk(new JFS32(FileOperations.loadFromCacheDir("main_file_cache.idx255"), 1048576L), 6000);
        FileOnDisk.cacheFileIndices = new FileOnDisk[SystemInfo.numIndices];
        for (failures = 0; failures < SystemInfo.numIndices; failures++) {
            FileOnDisk.cacheFileIndices[failures] = new FileOnDisk(new JFS32(FileOperations.loadFromCacheDir("main_file_cache.idx" + failures), 1048576L), 6000);
        }
        try {
            WindowMode.activeWindowMode = new WindowMode();
        } catch (Exception exception) {
            WindowMode.fullScreenSupported = false;
        }
        CursorManager.instance = new CursorManager();
        ThreadGroup currentThreadgroup = Thread.currentThread().getThreadGroup();
        for (ThreadGroup parentGroup = currentThreadgroup.getParent(); parentGroup != null; parentGroup = parentGroup.getParent()) {
            currentThreadgroup = parentGroup;
        }
        Thread[] threadPool = new Thread[1000];
        currentThreadgroup.enumerate(threadPool);
        for (int threadIndex = 0; threadIndex < threadPool.length; threadIndex++) {
            if (threadPool[threadIndex] != null && threadPool[threadIndex].getName().startsWith("AWT")) {
                threadPool[threadIndex].setPriority(1);
            }
        }
        Thread engineThread = new Thread(this);
        engineThread.setDaemon(true);
        engineThread.start();
        engineThread.setPriority(1);
    }

    public File loadPreferenceFile(String gameName, String buildEnvironmentName, int version) {
        String preferenceEnd = version == 0 ? "" : "" + version;
        userPreferenceFile = new File(PreferenceManager.preferencesParentFile + File.separatorChar + Loader.CACHE_DIR, "dk_cl_" + gameName + "_" + buildEnvironmentName + preferenceEnd + ".dat");
        String parentPath = null;
        String childPath = null;
        boolean preferenceWrite = false;
        if (userPreferenceFile.exists()) {
            try {
                JFS32 prefFile = new JFS32(userPreferenceFile, 10000L);
                Packet buffer;
                int bufferOffset;
                for (buffer = new Packet((int) prefFile.length()); buffer.currentPosition < buffer.buffer.length; buffer.currentPosition += bufferOffset) {
                    bufferOffset = prefFile.getSeekOffset(buffer.buffer, buffer.currentPosition, buffer.buffer.length - buffer.currentPosition);
                    if (bufferOffset == -1) {
                        throw new IOException();
                    }
                }
                buffer.currentPosition = 0;
                int contentType = buffer.readUnsignedByte();
                if (contentType < 1 || contentType > 3) {
                    throw new IOException("" + contentType);
                }
                int childType = 0;
                if (contentType > 1) {
                    childType = buffer.readUnsignedByte();
                }
                if (contentType <= 2) {
                    parentPath = buffer.readGJString();
                    if (childType == 1) {
                        childPath = buffer.readGJString();
                    }
                } else {
                    parentPath = buffer.method13248();
                    if (childType == 1) {
                        childPath = buffer.method13248();
                    }
                }
                prefFile.close();
            } catch (IOException ioexception) {
                ioexception.printStackTrace();
            }
            if (parentPath != null) {
            	File parentFile = new File(parentPath);
                if (!parentFile.exists()) {
                    parentPath = null;
                }
            }
            if (parentPath != null) {
            	File testFile = new File(parentPath, "test.dat");
                if (!generateRandomAccessFile(testFile, true)) {
                    parentPath = null;
                }
            }
        }
        if (parentPath == null && version == 0) {
            label103:
            for (int i_17 = 0; i_17 < SystemInfo.cacheFilestoreSubpath.length; i_17++) {
                for (int i_10 = 0; i_10 < SystemInfo.cacheBaseDirectory.length; i_10++) {
                    File file23 = new File(SystemInfo.cacheBaseDirectory[i_10] + SystemInfo.cacheFilestoreSubpath[i_17] + File.separatorChar + gameName + File.separatorChar);
                    if (file23.exists() && generateRandomAccessFile(new File(file23, "test.dat"), true)) {
                        parentPath = file23.toString();
                        preferenceWrite = true;
                        break label103;
                    }
                }
            }
        }
        if (parentPath == null) {
            parentPath = PreferenceManager.preferencesParentFile + File.separatorChar + Loader.CACHE_DIR + preferenceEnd + File.separatorChar + gameName + File.separatorChar + buildEnvironmentName + File.separatorChar;
            preferenceWrite = true;
        }
        if (childPath != null) {
            File childFile = new File(childPath);
            File parentFile = new File(parentPath);
            try {
                File[] childFiles = childFile.listFiles();
                File[] ChildFiles = childFiles;
                for (int index = 0; index < ChildFiles.length; index++) {
                    File ChildFile = ChildFiles[index];
                    File newFile = new File(parentFile, ChildFile.getName());
                    boolean renamed = ChildFile.renameTo(newFile);
                    if (!renamed) {
                        throw new IOException();
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            preferenceWrite = true;
        }
        if (preferenceWrite) {
            writeToPreferences(new File(parentPath), null);
        }
        return new File(parentPath);
    }

    public boolean method4658(File file1) {
        if (file1 == null) {
            return false;
        } else if (!file1.exists()) {
            return false;
        } else if (!file1.isDirectory()) {
            return false;
        } else if (file1.listFiles().length != 0) {
            return false;
        } else if (!generateRandomAccessFile(new File(file1, "test.dat"), true)) {
            return false;
        } else {
            writeToPreferences(file1, PreferenceManager.preferenceFile);
            return true;
        }
    }

    public void writeToPreferences(File file1, File file2) {
        try {
            JFS32 fileSystem = new JFS32(userPreferenceFile, 10000L);
            Packet buffer = new Packet(500);
            buffer.writeByte(3);
            buffer.writeByte(file2 != null ? 1 : 0);
            buffer.writePathReference(file1.getPath());
            if (file2 != null) {
                buffer.writePathReference(file2.getPath());
            }
            fileSystem.write(buffer.buffer, 0, buffer.currentPosition);
            fileSystem.close();
        } catch (IOException ioexception) {
            ioexception.printStackTrace();
        }
    }

    public synchronized void setFrameTitle(String frameTitle) {
        engineFrame = new Frame();
        engineFrame.setTitle(frameTitle);
        engineFrame.setResizable(true);
        engineFrame.addWindowListener(this);
        engineFrame.setBackground(Color.black);
        engineFrame.setVisible(true);
        engineFrame.toFront();
        Insets insets = engineFrame.getInsets();
        engineFrame.setSize(insets.right + insets.left + WindowMode.resizableScreenWidth, insets.top + insets.bottom + WindowMode.resizableScreenHeight);
    }

    public boolean loadJagtheora() {
        return AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jagtheora");
    }

    public void resizeApplet(Container container) {
        container.setBackground(Color.black);
        container.setLayout(null);
        container.add(gameCanvas);
        gameCanvas.setSize(GameShell.screenCanvasWidth, screenCanvasHeight * -969250379);
        gameCanvas.setVisible(true);
        if (container == engineFrame) {
            Insets insets = engineFrame.getInsets();
            gameCanvas.setLocation(insets.left + gameCanvasX, insets.top + gameCanvasY);
        } else {
            gameCanvas.setLocation(gameCanvasX, gameCanvasY);
        }
        gameCanvas.addFocusListener(this);
        gameCanvas.requestFocus();
        GameShell.appletHasFocus = true;
        WindowMode.hasFocus = true;
        gameCanvas.setFocusTraversalKeysEnabled(false);
        WindowMode.redrawWindowBackground = true;
        clippingUpdateNeeded = false;
        canvasInitializationTime = Utils.time();
    }

    @Override
    public void supplyApplet(Loader applet_1) {
        suppliedApplet = applet_1;
    }

    @Override
    public void run() {
        try {
            try {
                loadGame();
            } catch (ThreadDeath threadDeath) {
                throw threadDeath;
            } catch (Throwable throwable) {
                PacketEncoder.sendErrorToServer(getDebugString(), throwable);
                raiseErrorScreen("crash");
                prepareExit();
                return;
            }
            prepareExit();
        } catch (Exception exception4) {
            prepareExit();
        }
    }

    public void onPulseWrapper() {
        long currentTime = Utils.time();
        long lastTime = PULSE_CACHE[pulseCachePos];
        PULSE_CACHE[pulseCachePos] = currentTime;
        pulseCachePos = pulseCachePos + 1 & 0x1f;
        if (lastTime != 0L) {
        }
        synchronized (this) {
            GameShell.appletHasFocus = WindowMode.hasFocus;
        }
        onPulse();
    }

    public void mainRedrawWrapper() {
        long currentTime = Utils.time();
        long lastTime = MAIN_REDRAW_CACHE[GameShell.mainRedrawCachePos];
        MAIN_REDRAW_CACHE[GameShell.mainRedrawCachePos] = currentTime;
        GameShell.mainRedrawCachePos = GameShell.mainRedrawCachePos + 1 & 0x1f;
        if (lastTime != 0L && currentTime > lastTime) {
            int timeDelta = (int) (currentTime - lastTime);
            FPS = ((timeDelta >> 1) + 32000) / timeDelta;
        }
        if (++canvasRefreshCycle - 1 > 50) {
            canvasRefreshCycle -= 50;
            WindowMode.redrawWindowBackground = true;
            gameCanvas.setSize(GameShell.screenCanvasWidth, screenCanvasHeight * -969250379);
            gameCanvas.setVisible(true);
            if (engineFrame != null && WindowMode.fullScreenFrame == null) {
                Insets insets = engineFrame.getInsets();
                gameCanvas.setLocation(insets.left + gameCanvasX, insets.top + gameCanvasY);
            } else {
                gameCanvas.setLocation(gameCanvasX, gameCanvasY);
            }
        }
        mainRedraw();
    }

    String getDebugString() {
        return null;
    }

    @Override
    public void destroy() {
        if (!isShutdown) {
            killTime = Utils.time();
            TimeTools.sleep(5000L);
            prepareExit();
        }
    }

    @Override
    public synchronized void paint(Graphics graphics) {
        if (!isShutdown) {
            WindowMode.redrawWindowBackground = true;
            if (Utils.time() - canvasInitializationTime > 1000L) {
                Rectangle rectangle = graphics.getClipBounds();
                if (rectangle == null || rectangle.width >= WindowMode.resizableScreenWidth && rectangle.height >= WindowMode.resizableScreenHeight) {
                    clippingUpdateNeeded = true;
                }
            }
        }
    }

    @Override
    public void focusGained(FocusEvent focusevent1) {
        WindowMode.hasFocus = true;
        WindowMode.redrawWindowBackground = true;
    }

    @Override
    public void focusLost(FocusEvent focusevent) {
        WindowMode.hasFocus = false;
    }

    @Override
    public void windowActivated(WindowEvent windowEvent) {
    	// NOOP
    }

    @Override
    public void windowClosing(WindowEvent windowEvent) {
        raiseBrowserErrorscreen = true;
        destroy();
    }

    @Override
    public void windowDeactivated(WindowEvent windowEvent) {
    	// NOOP
    }

    public void prepareExit() {
        synchronized (this) {
            if (isShutdown) {
                return;
            }
            isShutdown = true;
        }
        try {
            shutdown();
        } catch (Exception ignored) {
        	// NOOP
        }
        try {
            cacheFileData.close();
            for (int index = 0; index < SystemInfo.numIndices; index++) {
                FileOnDisk.cacheFileIndices[index].close();
            }
            cacheFileReferenceTable.close();
            playerRandomDat.close();
        } catch (Exception ignored) {
        	// NOOP
        }
        if (this.jaclibLoaded) {
            try {
                QueryPerformanceCounter.quit();
            } catch (Throwable ignored) {
            	// NOOP
            }
        }
        FinalizeCacheSw3d.shutdown();
        if (AbstractToolkit.nativeLibraryLoaded()) {
        	AbstractToolkit.getNativeLibraryLoader().shutdown();
        }
        if (gameCanvas != null) {
            try {
                gameCanvas.removeFocusListener(this);
                gameCanvas.getParent().remove(gameCanvas);
            } catch (Exception ignored) {
            	// NOOP
            }
        }
        if (engineFrame != null) {
            engineFrame.setVisible(false);
            engineFrame.dispose();
            engineFrame = null;
        }
    }

    @Override
    public void windowOpened(WindowEvent windowEvent) {
    	// NOOP
    }

    @Override
    public abstract void init();

    abstract void initializeEssentials();

    abstract void onPulse();

    abstract void mainRedraw();

    void raiseErrorScreen(String errorName) {
        if (!this.errorScreenRaised) {
            this.errorScreenRaised = true;
            System.out.println("error_game_" + errorName);
        }
    }

    public boolean startJaclib() {
        this.jaclibLoaded = AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jaclib");
        if (this.jaclibLoaded) {
            try {
                QueryPerformanceCounter.init();
            } catch (Throwable throwable) {
            	// NOOP
            }
        }
        return this.jaclibLoaded;
    }

    public void setApplicationType(ApplicationType applicationType, boolean padding) {
        if (applicationType == null) {
            throw new NullPointerException();
        } 
        if (applicationType != ApplicationType.BORDERLESS && applicationType != ApplicationType.APPLET) {
            throw new IllegalStateException();
        } 
        currentApplicationType = applicationType;
        if (currentApplicationType != ApplicationType.APPLET && padding) {
            currentApplicationType = ApplicationType.BORDERED;
        }
    }

    @Override
    public void windowDeiconified(WindowEvent windowEvent) {
    	// NOOP
    }

    @Override
    public void start() {
        if (!isShutdown) {
            killTime = 0L;
        }
    }

    public synchronized void buildCanvas() {
        resetCanvas();
        Container container = WindowMode.getActiveContainer();
        gameCanvas = new GameCanvas(container);
        resizeApplet(container);
    }

    abstract void shutdown();

    public boolean generateRandomAccessFile(File source, boolean deleteSource) {
        try {
            RandomAccessFile raf = new RandomAccessFile(source, "rw");
            int value = raf.read();
            raf.seek(0L);
            raf.write(value);
            raf.seek(0L);
            raf.close();
            if (deleteSource) {
                source.delete();
            }
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public void resetCanvas() {
        if (gameCanvas != null) {
            gameCanvas.removeFocusListener(this);
            gameCanvas.getParent().setBackground(Color.black);
            gameCanvas.getParent().remove(gameCanvas);
        }
    }

    public void loadGame() {
        if (SystemInfo.javaVendor != null) {
            String vendor = SystemInfo.javaVendor.toLowerCase();
            if (vendor.indexOf("sun") != -1 || vendor.indexOf("apple") != -1) {
                String version = SystemInfo.javaVersion;
                if ("1.1".equals(version) || version.startsWith("1.1.") || "1.2".equals(version) || version.startsWith("1.2.") || "1.3".equals(version) || version.startsWith("1.3.") || "1.4".equals(version) || version.startsWith("1.4.") || "1.5".equals(version) || version.startsWith("1.5.") || "1.6.0".equals(version)) {
                    raiseErrorScreen("wrongjava");
                    return;
                }
                if (version.startsWith("1.6.0_")) {
                    int charIndex;
                    for (charIndex = 6; charIndex < version.length() && Utils.isNumber(version.charAt(charIndex)); charIndex++) { }
                    String minorVersion = version.substring(6, charIndex);
                    if (Utils.isWrongJava(minorVersion) && Utils.parseInt(minorVersion) < 10) {
                        raiseErrorScreen("wrongjava");
                        return;
                    }
                }
            }
        }
        WindowMode.getActiveContainer().setFocusCycleRoot(true);
        maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1048576L) + 1;
        availableProcessors = Runtime.getRuntime().availableProcessors();
        buildCanvas();
        initializeEssentials();
        cycleManager = GameShell.getCycleManager();
        while (killTime == 0L || Utils.time() < killTime) {
            loopsInCycle = cycleManager.loopcycle(PREFERED_CYCLE_DURATION);
            for (int loop = 0; loop < loopsInCycle; loop++) {
                onPulseWrapper();
            }
            mainRedrawWrapper();
            GameShell.performDummyActionevent(gameCanvas);
        }
    }

    @Override
    public void windowClosed(WindowEvent windowEvent) {
    }

    @Override
    public void windowIconified(WindowEvent windowEvent) {
    }


	public static File getPreferenceFile() {
	    return PreferenceManager.preferenceFile;
	}

	public static void performDummyActionevent(Canvas canvas) {
	    if (SystemInfo.systemEventQueue != null) {
	        for (int i = 0; i < 50 && SystemInfo.systemEventQueue.peekEvent() != null; i++) {
	            TimeTools.sleep(1L);
	        }
	
	        try {
	            if (canvas != null) {
	                SystemInfo.systemEventQueue.postEvent(new ActionEvent(canvas, 1001, "dummy"));
	            }
	        } catch (Exception ignored) {
	        	// NOOP
	        }
	    }
	}

	public static int gameCyclesPerSecond() {
	    return (int) (1000000000L / PREFERED_CYCLE_DURATION);
	}

	public static ApplicationType getApplicationType() {
	    return currentApplicationType;
	}

	public static AbstractCycleManager getCycleManager() {
	    try {
	        JaclibCycleManager cycleManager = new JaclibCycleManager();
	        return cycleManager;
	    } catch (Throwable trowable) {
	        return new SystemCycleManager();
	    }
	}


	public static void setComponentsOnTop() {
	    for (int componentIndex = 0; componentIndex < 107; componentIndex++) {
	        client.IS_COMPONENT_ON_TOP[componentIndex] = true;
	    }
	}
}
