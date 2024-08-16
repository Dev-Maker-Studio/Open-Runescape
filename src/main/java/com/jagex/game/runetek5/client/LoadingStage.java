package com.jagex.game.runetek5.client;

import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.graphics.loginscreen.LoginScreenComponent;
import com.jagex.game.runetek5.graphics.loginscreen.AssetLoadingScreenRenderer;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.Utils;

public class LoadingStage {

	public static final LoadingStage INITIALIZE_JS5;
	public static final LoadingStage LOAD_TIPS;
	public static final LoadingStage CREATE_FONTS;
	public static final LoadingStage CACHE_FONTS;
	public static final LoadingStage PREPARE_FONTS;
	public static final LoadingStage CREATE_JS5;
	public static final LoadingStage LOAD_SPRITES;
	public static final LoadingStage INITIALIZE_AUDIO;
	public static final LoadingStage PREPARE_NATIVE_LIBRARY_LOADER;
	public static final LoadingStage LOAD_NATIVES;
	public static final LoadingStage LOAD_TYPELISTS;
	public static final LoadingStage CACHE_STATIC_MEDIA;
	public static final LoadingStage INITIALIZE_WORLDMAP;
	public static final LoadingStage DISCARD_UNPACKED;
	public static final LoadingStage DISPLAY_LOGIN_WINDOW;
	public static final LoadingStage INITIALIZE_PLAYER;
	public static final LoadingStage PREPARE_TOOLKIT;
    public static final LoadingStage PREPARE_PLAYER;
    public static LoadingStage[] LOADING_STAGES;

    static {
        INITIALIZE_JS5 = new LoadingStage(0, LocalizedText.CHECKING_FOR_UPDATES, 2);
        LOAD_TIPS = new LoadingStage(1, LocalizedText.CHECKING_FOR_UPDATES, LocalizedText.CHECKING_FOR_UPDATES, 2, 3);
        CREATE_FONTS = new LoadingStage(2, LocalizedText.CHECKING_FOR_UPDATES, 3);
        CACHE_FONTS = new LoadingStage(3, LocalizedText.CHECKING_FOR_UPDATES, LocalizedText.CHECKING_FOR_UPDATES, 3, 4);
        PREPARE_FONTS = new LoadingStage(4, LocalizedText.CHECKING_FOR_UPDATES, 4);
        CREATE_JS5 = new LoadingStage(5, LocalizedText.CHECKING_FOR_UPDATES, LocalizedText.CHECKING_FOR_UPDATES, 4, 5);
        LOAD_SPRITES = new LoadingStage(6, LocalizedText.CHECKING_FOR_UPDATES, LocalizedText.CHECKING_FOR_UPDATES, 5, 98, true);
        INITIALIZE_AUDIO = new LoadingStage(7, LocalizedText.CHECKING_FOR_UPDATES, 99);
        PREPARE_NATIVE_LIBRARY_LOADER = new LoadingStage(8, LocalizedText.CHECKING_FOR_UPDATES, 100);
        LOAD_NATIVES = new LoadingStage(9, LocalizedText.FETCHING_UPDATES, LocalizedText.FETCHING_UPDATES, 0, 92, true);
        LOAD_TYPELISTS = new LoadingStage(10, LocalizedText.FETCHING_UPDATES, LocalizedText.FETCHING_UPDATES, 92, 93);
        CACHE_STATIC_MEDIA = new LoadingStage(11, LocalizedText.FETCHING_UPDATES, LocalizedText.FETCHING_UPDATES, 94, 95);
        INITIALIZE_WORLDMAP = new LoadingStage(12, LocalizedText.FETCHING_UPDATES, LocalizedText.FETCHING_UPDATES, 96, 97);
        DISCARD_UNPACKED = new LoadingStage(13, LocalizedText.FETCHING_UPDATES, 97);
        DISPLAY_LOGIN_WINDOW = new LoadingStage(14, LocalizedText.FETCHING_UPDATES, 97);
        INITIALIZE_PLAYER = new LoadingStage(15, LocalizedText.FETCHING_UPDATES, 100);
        PREPARE_TOOLKIT = new LoadingStage(16, LocalizedText.FETCHING_UPDATES, 100);
        PREPARE_PLAYER = new LoadingStage(17, LocalizedText.FETCHING_UPDATES, 100);
    }

    public int number;
    public LocalizedText primaryTextDisplay;
    public LocalizedText secondaryTextDisplay;
    public int primaryCompletionDisplay;
    public int secondaryCompletionDisplay;
    public boolean aBool4694;
    public boolean aBool4718;
	public static int displayedLoadingPercentage;
	public static String LOADING_TEXT;
	public static LoginScreenComponent[] gametipsToDisplay;
	public static int lastDisplayedGametip = -1;
	public static Thread loginScreenRendererThread;
	public static long LOADING_STAGE_CHANGED;
	public static LoadingStage LOADING_STAGE;
	public static AssetLoadingScreenRenderer plainLoginScreenRenderer;

    public LoadingStage(int number, LocalizedText primaryTextDisplay, LocalizedText secondaryTextDisplay, int primaryCompletionDisplay, int secondaryCompletionDisplay, boolean bool7) {
    	this.number = number;
    	this.primaryTextDisplay = primaryTextDisplay;
    	this.secondaryTextDisplay = secondaryTextDisplay;
    	this.primaryCompletionDisplay = primaryCompletionDisplay;
    	this.secondaryCompletionDisplay = secondaryCompletionDisplay;
    	this.aBool4694 = true;
    	this.aBool4718 = bool7;
    }

    public LoadingStage(int number, LocalizedText textDisplay, int completionDisplay) {
        this(number, textDisplay, textDisplay, completionDisplay, completionDisplay, false);
    }

    public LoadingStage(int number, LocalizedText primaryTextDisplay, LocalizedText secondaryTextDisplay, int primaryCompletionDisplay, int secondaryCompletionDisplay) {
        this(number, primaryTextDisplay, secondaryTextDisplay, primaryCompletionDisplay, secondaryCompletionDisplay, false);
    }
    
	public static void initialize(boolean requestBackgroundFillrect) {
	    if (plainLoginScreenRenderer == null) {
	        initialize();
	    }
	    if (requestBackgroundFillrect) {
	        plainLoginScreenRenderer.requestFillrect();
	    }
	}

	public static void initialize() {
	    if (LOADING_STAGE != null) {
	        plainLoginScreenRenderer = new AssetLoadingScreenRenderer();
	        plainLoginScreenRenderer.initialize(LOADING_STAGE_CHANGED, LOADING_STAGE.primaryTextDisplay.translate(client.CURRENT_LANGUAGE), LOADING_STAGE.primaryCompletionDisplay, LOADING_STAGE);
	        LoadingStage.loginScreenRendererThread = new Thread(plainLoginScreenRenderer, "");
	        LoadingStage.loginScreenRendererThread.start();
	    }
	}

    public static void cycle() {
        if (LOADING_STAGES == null) {
            LOADING_STAGES = getValues();
            LoadingStage.LOADING_STAGE = LOADING_STAGES[0];
            LoadingStage.LOADING_STAGE_CHANGED = Utils.time();
        }
        if (LoadingStage.plainLoginScreenRenderer == null) {
            LoadingStage.initialize();
        }
        LoadingStage loadingStage = LoadingStage.LOADING_STAGE;
        int loadingPercent = ClientStartup.initialize();
        if (loadingStage == LoadingStage.LOADING_STAGE) {
            LoadingStage.LOADING_TEXT = LoadingStage.LOADING_STAGE.primaryTextDisplay.translate(client.CURRENT_LANGUAGE);
            if (LoadingStage.LOADING_STAGE.aBool4718) {
                LoadingStage.displayedLoadingPercentage = loadingPercent * (LoadingStage.LOADING_STAGE.secondaryCompletionDisplay - LoadingStage.LOADING_STAGE.primaryCompletionDisplay) / 100 + LoadingStage.LOADING_STAGE.primaryCompletionDisplay;
            }
            if (LoadingStage.LOADING_STAGE.aBool4694) {
                LoadingStage.LOADING_TEXT = LoadingStage.LOADING_TEXT + LoadingStage.displayedLoadingPercentage + "%";
            }
        } else if (LoadingStage.LOADING_STAGE == PREPARE_PLAYER) {
            LoadingStage.plainLoginScreenRenderer = null;
            client.setGameState(5);
        } else {
            LoadingStage.LOADING_TEXT = loadingStage.secondaryTextDisplay.translate(client.CURRENT_LANGUAGE);
            if (LoadingStage.LOADING_STAGE.aBool4694) {
                LoadingStage.LOADING_TEXT = LoadingStage.LOADING_TEXT + loadingStage.secondaryCompletionDisplay + "%";
            }
            LoadingStage.displayedLoadingPercentage = loadingStage.secondaryCompletionDisplay;
            if (LoadingStage.LOADING_STAGE.aBool4718 || loadingStage.aBool4718) {
                LoadingStage.LOADING_STAGE_CHANGED = Utils.time();
            }
        }
        if (LoadingStage.plainLoginScreenRenderer != null) {
            LoadingStage.plainLoginScreenRenderer.initialize(LoadingStage.LOADING_STAGE_CHANGED, LoadingStage.LOADING_TEXT, LoadingStage.displayedLoadingPercentage, LoadingStage.LOADING_STAGE);
            if (LoadingStage.gametipsToDisplay != null) {
                for (int index = LoadingStage.lastDisplayedGametip + 1; index < LoadingStage.gametipsToDisplay.length; index++) {
                    if (LoadingStage.gametipsToDisplay[index].calculateLoadingPercent() >= 100 && index - 1 == LoadingStage.lastDisplayedGametip && client.gameState != 4 && LoadingStage.plainLoginScreenRenderer.replaceComponent()) {
                        try {
                            LoadingStage.gametipsToDisplay[index].initialize();
                        } catch (Exception exception) {
                            LoadingStage.gametipsToDisplay = null;
                            break;
                        }
                        LoadingStage.plainLoginScreenRenderer.setDisplayedGametip(LoadingStage.gametipsToDisplay[index]);
                        if (++LoadingStage.lastDisplayedGametip >= LoadingStage.gametipsToDisplay.length - 1 && LoadingStage.gametipsToDisplay.length > 1) {
                            LoadingStage.lastDisplayedGametip = TypeLists.GAME_TIPS.hasTipsFile() ? 0 : -1;
                        }
                    }
                }
            }
        }
    }

	public static void killLoginscreenThread() {
	    if (plainLoginScreenRenderer != null) {
	        plainLoginScreenRenderer.stop();
	    }
	    if (LoadingStage.loginScreenRendererThread != null) {
	        while (true) {
	            try {
	                LoadingStage.loginScreenRendererThread.join();
	                break;
	            } catch (InterruptedException interruptedException) {
	            	// NOOP
	            }
	        }
	    }
	}
	
    public static LoadingStage[] getValues() {
        return new LoadingStage[]{INITIALIZE_JS5, LOAD_TIPS, CREATE_FONTS, CACHE_FONTS, PREPARE_FONTS, CREATE_JS5, LOAD_SPRITES, INITIALIZE_AUDIO, PREPARE_NATIVE_LIBRARY_LOADER, LOAD_NATIVES, LOAD_TYPELISTS, CACHE_STATIC_MEDIA, INITIALIZE_WORLDMAP, DISCARD_UNPACKED, DISPLAY_LOGIN_WINDOW, INITIALIZE_PLAYER, PREPARE_TOOLKIT, PREPARE_PLAYER};
    }
}
