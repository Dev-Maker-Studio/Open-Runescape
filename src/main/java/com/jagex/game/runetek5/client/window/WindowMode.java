package com.jagex.game.runetek5.client.window;

import java.awt.*;
import java.lang.reflect.Field;

import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.tools.Browser;
import com.jagex.game.runetek5.utils.ArrayUtils;

/**
 * [J]
 * @author Steven
 *
 */
public class WindowMode {

    public static int resizableScreenWidth;
    public static boolean fullScreenSupported = true;
    public static Frame fullScreenFrame;
    public static long nextDimensionUpdateCheck;
    public static int resizableScreenHeight;
    public static int fullScreenWidth;
    public static int fullScreenHeight;
    public static WindowMode activeWindowMode;
    public DisplayMode displayMode;

    public GraphicsDevice graphicsDevice;
	public static volatile boolean hasFocus = true;
	public static boolean fullScreenWindowMode;
	public static DisplayModeDetails[] activeDisplayModeDetails;
	public static volatile boolean redrawWindowBackground = true;

    public WindowMode() throws Exception {
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        this.graphicsDevice = environment.getDefaultScreenDevice();
        if (!this.graphicsDevice.isFullScreenSupported()) {
            GraphicsDevice[] devices = environment.getScreenDevices();
            for (int deviceIndex = 0; deviceIndex < devices.length; deviceIndex++) {
                GraphicsDevice activeDevice = devices[deviceIndex];
                if (activeDevice != null && activeDevice.isFullScreenSupported()) {
                    this.graphicsDevice = activeDevice;
                    return;
                }
            }
            throw new Exception();
        }
    }

    public static Frame calculateFullscreenFrame(WindowMode windowMode, int displayWidth, int displayHeight, int bitDepth) {
        if (bitDepth == 0) {
            DisplayModeDetails[] totalDetails = DisplayModeDetails.forWindowMode(windowMode);
            boolean found = false;
            for (int index = 0; index < totalDetails.length; index++) {
                if (totalDetails[index].width == displayWidth && displayHeight == totalDetails[index].height && (!found || totalDetails[index].bitDepth > 0)) {
                    bitDepth = totalDetails[index].bitDepth;
                    found = true;
                }
            }
            if (!found) {
                return null;
            }
        }
        Frame frame = new Frame("Jagex Full Screen");
        frame.setResizable(false);
        windowMode.setGraphicsDeviceDisplayMode(frame, displayWidth, displayHeight, bitDepth);
        return frame;
    }

    public static DisplayModeDetails[] getActiveDisplayModeDetails() {
        if (WindowMode.activeDisplayModeDetails == null) {
            DisplayModeDetails[] allDetails = DisplayModeDetails.forWindowMode(activeWindowMode);
            DisplayModeDetails[] allDetailsRebuilt = new DisplayModeDetails[allDetails.length];
            int detailCount = 0;
            int screenSizeType = PreferenceManager.preferences.maxScreenSize.getValue();
            Cache_Into:
            for (int detailIndex = 0; detailIndex < allDetails.length; detailIndex++) {
                DisplayModeDetails displayDetails = allDetails[detailIndex];
                if ((displayDetails.bitDepth <= 0 || displayDetails.bitDepth >= 24)
                		&& displayDetails.width >= 800 && displayDetails.height >= 600
                		&& (screenSizeType != 2 || displayDetails.width <= 800 && displayDetails.height <= 600)
                		&& (screenSizeType != 1 || displayDetails.width <= 1024 && displayDetails.height <= 768)) {
                    for (int index = 0; index < detailCount; index++) {
                        DisplayModeDetails copy = allDetailsRebuilt[index];
                        if (displayDetails.width == copy.width && displayDetails.height == copy.height) {
                            if (displayDetails.bitDepth > copy.bitDepth) {
                                allDetailsRebuilt[index] = displayDetails;
                            }
                            continue Cache_Into;
                        }
                    }
                    allDetailsRebuilt[detailCount] = displayDetails;
                    detailCount++;
                }
            }
            WindowMode.activeDisplayModeDetails = new DisplayModeDetails[detailCount];
            System.arraycopy(allDetailsRebuilt, 0, WindowMode.activeDisplayModeDetails, 0, detailCount);
            int[] displayDimensions = new int[WindowMode.activeDisplayModeDetails.length];
            for (int index = 0; index < WindowMode.activeDisplayModeDetails.length; index++) {
                DisplayModeDetails details = WindowMode.activeDisplayModeDetails[index];
                displayDimensions[index] = details.height * details.width;
            }
            ArrayUtils.quicksort(displayDimensions, WindowMode.activeDisplayModeDetails);
        }
        return WindowMode.activeDisplayModeDetails;
    }

    /**
     * requested_window_mode 
     * 1 = fixed
     * 2 = resizable
     * 3 = fullscreen
     */
    public static void requestWindowMode(int activeCurrentMode, int requestedWindowMode, int width, int height, boolean isFixed) {
        if (fullScreenSupported && fullScreenFrame != null && (requestedWindowMode != 3 || width != fullScreenWidth || height != fullScreenHeight)) {
            disposeFrame(activeWindowMode, fullScreenFrame);
            fullScreenFrame = null;
        }
    	System.out.println("Mode: "+ requestedWindowMode);
        if (fullScreenSupported && requestedWindowMode == 3 && fullScreenFrame == null) {
            fullScreenFrame = calculateFullscreenFrame(activeWindowMode, width, height, 0);
            if (fullScreenFrame != null) {
                fullScreenWidth = width;
                fullScreenHeight = height;
                PreferenceManager.savePreferences();
            }
        }
        if (requestedWindowMode == 3 && (!fullScreenSupported || fullScreenFrame == null)) {
            requestWindowMode(activeCurrentMode, PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, true);
        } else {
            Container container = getActiveContainer();
            if (fullScreenFrame != null) {
                resizableScreenWidth = width;
                resizableScreenHeight = height;
            } else if (client.engineFrame != null) {
            	Insets insets = client.engineFrame.getInsets();
                int widthDelta = insets.left + insets.right;
                resizableScreenWidth = client.engineFrame.getSize().width - widthDelta;
                int heightDelta = insets.bottom + insets.top;
                resizableScreenHeight = client.engineFrame.getSize().height - heightDelta;
            } else {
                resizableScreenWidth = container.getSize().width;
                resizableScreenHeight = container.getSize().height;
            }
            if (resizableScreenWidth <= 0) {
                resizableScreenWidth = 1;
            }
            if (resizableScreenHeight <= 0) {
                resizableScreenHeight = 1;
            }
            if (requestedWindowMode != 1) {
                WindowMode.canvasAdjustment();
            } else {
                GameShell.screenCanvasWidth = client.GAME_WIDTH;
                client.gameCanvasX = (resizableScreenWidth - client.GAME_WIDTH) / 2;
                client.screenCanvasHeight = client.GAME_HEIGHT * -1929118563;
                client.gameCanvasY = 0;
            }
            if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE && GameShell.screenCanvasWidth < 1024) {
                int canvasH = client.screenCanvasHeight;
            }
            if (!isFixed) {
                GameShell.gameCanvas.setSize(GameShell.screenCanvasWidth, client.screenCanvasHeight * -969250379);
                Renderers.activeToolkit.resizeGameCanvas(GameShell.gameCanvas, GameShell.screenCanvasWidth, client.screenCanvasHeight * -969250379);
                if (container == client.engineFrame) {
                	Insets insets = client.engineFrame.getInsets();
                    GameShell.gameCanvas.setLocation(insets.left + client.gameCanvasX, insets.top + client.gameCanvasY);
                } else {
                    GameShell.gameCanvas.setLocation(client.gameCanvasX, client.gameCanvasY);
                }
            } else {
                exitFullscreen();
            }
            WindowMode.fullScreenWindowMode = requestedWindowMode >= 2;
            if (client.BASE_WINDOW_ID != -1) {
                interfaceResize(true);
            }
            if (client.GAME_CONNECTION_CONTEXT.getConnection() != null && GameState.isRenderingScene(client.gameState)) {
                PacketEncoder.sendWindowmodePreferenceToServer();
            }
            for (int componentIndex = 0; componentIndex < 107; componentIndex++) {
                client.IS_COMPONENT_ON_TOP[componentIndex] = true;
            }
            WindowMode.redrawWindowBackground = true;
        }
    }

    public static void interfaceResize(boolean hasBaseWindowId) {
        InterfaceRenderer.updateInterfaceDimensions(client.BASE_WINDOW_ID, GameShell.screenCanvasWidth, client.screenCanvasHeight * -969250379, hasBaseWindowId);
    }

    public static Container getActiveContainer() {
        return fullScreenFrame != null ? fullScreenFrame : (GameShell.engineFrame != null ? GameShell.engineFrame : GameShell.suppliedApplet);
    }

    public static void notifyError() {
        if (fullScreenSupported && fullScreenFrame != null) {
            requestWindowMode(PreferenceManager.preferences.fixedScreen.getValue(), -1, -1, false);
        }
        if (GameShell.getApplicationType() == ApplicationType.APPLET) {
            PacketEncoder.writeVarcPreferences();
            System.out.println("[WindowMode] Exit");
            System.exit(0);
        } else {
            Browser.openBrowserErrorscreenOpenjs();
        }
    }

    public static void requestWindowMode(int requestedMode, int width, int height, boolean isFixed) {
        nextDimensionUpdateCheck = 0L;
        int activeMode = getWindowMode();
        if (requestedMode == 3 || activeMode == 3) {
            isFixed = true;
        }
        if (!Renderers.activeToolkit.forceFixedScreenmode()) {
            isFixed = true;
        }
        requestWindowMode(activeMode, requestedMode, width, height, isFixed);
    }

    public static void disposeFrame(WindowMode windowMode, Frame frame) {
        windowMode.destroy();
        frame.setVisible(false);
        frame.dispose();
    }

    public static int getWindowMode() {
        return fullScreenSupported && fullScreenFrame != null ? 3 : (WindowMode.fullScreenWindowMode ? 2 : 1);
    }

    public static void exitFullscreen() {
        if (Renderers.activeToolkit.replaceCanvasOnDisplayUpdate()) {
            Renderers.activeToolkit.removeCanvas(GameShell.gameCanvas);
            client.buildCanvasComponents();
            Dimension canvasDimension = GameShell.gameCanvas.getSize();
            Renderers.activeToolkit.ensureImageproducerCreated(GameShell.gameCanvas, canvasDimension.width, canvasDimension.height);
            Renderers.activeToolkit.setActiveCanvas(GameShell.gameCanvas);
        } else {
            client.loadAssets(PreferenceManager.preferences.activeToolkit.getValue(), false);
        }
        GameShell.setComponentsOnTop();
    }

    public int[] getDisplayModes() {
        DisplayMode[] displayModes = this.graphicsDevice.getDisplayModes();
        int[] displayModeDetails = new int[displayModes.length << 2];
        for (int displayModeIndex = 0; displayModeIndex < displayModes.length; displayModeIndex++) {
            displayModeDetails[displayModeIndex << 2] = displayModes[displayModeIndex].getWidth();
            displayModeDetails[(displayModeIndex << 2) + 1] = displayModes[displayModeIndex].getHeight();
            displayModeDetails[(displayModeIndex << 2) + 2] = displayModes[displayModeIndex].getBitDepth();
            displayModeDetails[(displayModeIndex << 2) + 3] = displayModes[displayModeIndex].getRefreshRate();
        }
        return displayModeDetails;
    }

    public void setGraphicsDeviceDisplayMode(Frame frame, int displayWidth, int displayHeight, int currentBitDepth) {
        int newRefreshRate = 0;
        this.displayMode = this.graphicsDevice.getDisplayMode();
        if (this.displayMode == null) {
            throw new NullPointerException();
        } 
        frame.setUndecorated(true);
        frame.enableInputMethods(false);
        setGraphicsDeviceFullscreen(frame);
        int refreshRate = this.displayMode.getRefreshRate();
        DisplayMode[] allDisplayModes = this.graphicsDevice.getDisplayModes();
        boolean replaceRefreshRate = false;
        for (int modeIndex = 0; modeIndex < allDisplayModes.length; modeIndex++) {
            if (allDisplayModes[modeIndex].getWidth() == displayWidth && allDisplayModes[modeIndex].getHeight() == displayHeight && allDisplayModes[modeIndex].getBitDepth() == currentBitDepth) {
                int displayModeRefreshRate = allDisplayModes[modeIndex].getRefreshRate();
                if (!replaceRefreshRate || Math.abs(displayModeRefreshRate - refreshRate) < Math.abs(-refreshRate)) {
                    newRefreshRate = displayModeRefreshRate;
                    replaceRefreshRate = true;
                }
            }
        }
        if (!replaceRefreshRate) {
            newRefreshRate = refreshRate;
        }
        this.graphicsDevice.setDisplayMode(new DisplayMode(displayWidth, displayHeight, currentBitDepth, newRefreshRate));
    }

    public void setGraphicsDeviceFullscreen(Frame frame) {
        boolean invalidated = false;
        Field isValid;
        try {
            isValid = Class.forName("sun.awt.Win32GraphicsDevice").getDeclaredField("valid");
            isValid.setAccessible(true);
            boolean setInvalid = ((Boolean) isValid.get(this.graphicsDevice)).booleanValue();
            if (setInvalid) {
                isValid.set(this.graphicsDevice, Boolean.FALSE);
                invalidated = true;
            }
        } catch (Throwable throwable) {
        	throwable.printStackTrace();
        }
        try {
            this.graphicsDevice.setFullScreenWindow(frame);
            if (invalidated) {
                try {
                    isValid = Class.forName("sun.awt.Win32GraphicsDevice").getDeclaredField("valid");
                    isValid.set(this.graphicsDevice, Boolean.TRUE);
                } catch (Throwable throwable) {
                	throwable.printStackTrace();
                }
            }
        } catch (Exception exception) {
            if (invalidated) {
                try {
                    Field valid = Class.forName("sun.awt.Win32GraphicsDevice").getDeclaredField("valid");
                    valid.set(this.graphicsDevice, Boolean.TRUE);
                } catch (Throwable throwable) {
                	throwable.printStackTrace();
                }
            }
        }
    }

    public void destroy() {
        if (this.displayMode != null) {
            DisplayMode[] displayModes = this.graphicsDevice.getDisplayModes();
            boolean modeAdjusted = false;
            for (int modeIndex = 0; modeIndex < displayModes.length; modeIndex++) {
                DisplayMode mode = displayModes[modeIndex];
                if (mode.equals(this.displayMode)) {
                    this.graphicsDevice.setDisplayMode(this.displayMode);
                    modeAdjusted = true;
                    break;
                }
            }
            if (!modeAdjusted) {
                try {
                    Field defaultDisplayMode = Class.forName("sun.awt.Win32GraphicsDevice").getDeclaredField("defaultDisplayMode");
                    defaultDisplayMode.setAccessible(true);
                    defaultDisplayMode.set(this.graphicsDevice, null);
                } catch (Throwable throwable) {
                	throwable.printStackTrace();
                }
            }
            this.displayMode = null;
        }
        setGraphicsDeviceFullscreen(null);
    }

	public static void fillWindowRect() {
	    if (fullScreenFrame == null) {
	        int canvasX = client.gameCanvasX;
	        int canvasY = client.gameCanvasY;
	        int deltaX = resizableScreenWidth - GameShell.screenCanvasWidth - canvasX;
	        int deltaY = resizableScreenHeight - client.screenCanvasHeight * -969250379 - canvasY;
	        if (canvasX > 0 || deltaX > 0 || canvasY > 0 || deltaY > 0) {
	            try {
	                Container activeContainer = getActiveContainer();
	                int x = 0;
	                int y = 0;
	                if (activeContainer == client.engineFrame) {
	                    Insets frameInsets = client.engineFrame.getInsets();
	                    x = frameInsets.left;
	                    y = frameInsets.top;
	                }
	                Graphics activeGraphics = activeContainer.getGraphics();
	                activeGraphics.setColor(Color.black);
	                if (canvasX > 0) {
	                    activeGraphics.fillRect(x, y, canvasX, resizableScreenHeight);
	                }
	                if (canvasY > 0) {
	                    activeGraphics.fillRect(x, y, resizableScreenWidth, canvasY);
	                }
	                if (deltaX > 0) {
	                    activeGraphics.fillRect(x + resizableScreenWidth - deltaX, y, deltaX, resizableScreenHeight);
	                }
	                if (deltaY > 0) {
	                    activeGraphics.fillRect(x, y + resizableScreenHeight - deltaY, resizableScreenWidth, deltaY);
	                }
	            } catch (Exception exception) {
	            	// NOOP
	            }
	        }
	    }
	}

	public static void adjustActiveWindow() {
	    synchronized (ClientStartup.activeClient) {
	        if (fullScreenFrame == null) {
	            Container container = getActiveContainer();
	            if (container != null) {
	                resizableScreenWidth = container.getSize().width;
	                resizableScreenHeight = container.getSize().height;
	                if (container == client.engineFrame) {
	                	Insets insets = client.engineFrame.getInsets();
	                    resizableScreenWidth -= insets.left + insets.right;
	                    resizableScreenHeight -= insets.bottom + insets.top;
	                }
	                if (resizableScreenWidth <= 0) {
	                    resizableScreenWidth = 1;
	                }
	                if (resizableScreenHeight <= 0) {
	                    resizableScreenHeight = 1;
	                }
	                if (getWindowMode() != 1) {
	                    canvasAdjustment();
	                } else {
	                    GameShell.screenCanvasWidth = client.GAME_WIDTH;
	                    client.gameCanvasX = (resizableScreenWidth - client.GAME_WIDTH) / 2;
	                    client.screenCanvasHeight = client.GAME_HEIGHT * -1929118563;
	                    client.gameCanvasY = 0;
	                }
	                if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE && GameShell.screenCanvasWidth < 1024) {
	                    int canvasHeight = client.screenCanvasHeight;
	                }
	                GameShell.gameCanvas.setSize(GameShell.screenCanvasWidth, client.screenCanvasHeight * -969250379);
	                if (Renderers.activeToolkit != null) {
	                    Renderers.activeToolkit.resizeGameCanvas(GameShell.gameCanvas, GameShell.screenCanvasWidth, client.screenCanvasHeight * -969250379);
	                }
	                if (container == client.engineFrame) {
	                	Insets insets = client.engineFrame.getInsets();
	                    GameShell.gameCanvas.setLocation(insets.left + client.gameCanvasX, insets.top + client.gameCanvasY);
	                } else {
	                    GameShell.gameCanvas.setLocation(client.gameCanvasX, client.gameCanvasY);
	                }
	                if (client.BASE_WINDOW_ID != -1) {
	                    interfaceResize(true);
	                }
	                WindowMode.fillWindowRect();
	            }
	        }
	    }
	}

	public static void canvasAdjustment() {
	    int screensizePreference = 0;
	    if (PreferenceManager.preferences != null) {
	        screensizePreference = PreferenceManager.preferences.maxScreenSize.getValue();
	    }
	    if (screensizePreference == 2) {
	    	int preferedWidth = Math.min(800, resizableScreenWidth);
	    	int preferedHeight = Math.min(600, resizableScreenHeight);
	        GameShell.screenCanvasWidth = preferedWidth;
	        client.gameCanvasX = (resizableScreenWidth - preferedWidth) / 2;
	        client.screenCanvasHeight = preferedHeight * -1929118563;
	        client.gameCanvasY = 0;
	    } else if (screensizePreference == 1) {
	    	int preferedWidth = Math.min(1024, resizableScreenWidth);
	    	int preferedHeight = Math.min(768, resizableScreenHeight);
	        GameShell.screenCanvasWidth = preferedWidth;
	        client.gameCanvasX = (resizableScreenWidth - preferedWidth) / 2;
	        client.screenCanvasHeight = preferedHeight * -1929118563;
	        client.gameCanvasY = 0;
	    } else {
	        GameShell.screenCanvasWidth = resizableScreenWidth;
	        client.gameCanvasX = 0;
	        client.screenCanvasHeight = resizableScreenHeight * -1929118563;
	        client.gameCanvasY = 0;
	    }
	}
}
