package com.jagex.game.runetek5.client

import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.graphics.loginscreen.AssetLoadingScreenRenderer
import com.jagex.game.runetek5.graphics.loginscreen.LoginScreenComponent
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.Utils

class LoadingStage(
    @JvmField val number: Int,
    @JvmField val primaryTextDisplay: LocalizedText,
    @JvmField val secondaryTextDisplay: LocalizedText,
    @JvmField val primaryCompletionDisplay: Int,
    @JvmField val secondaryCompletionDisplay: Int,
    @JvmField val showPercentage: Boolean = true,
    @JvmField val hasProgressTracking: Boolean = false
) {

    constructor(number: Int, textDisplay: LocalizedText, completionDisplay: Int) :
            this(number, textDisplay, textDisplay, completionDisplay, completionDisplay, true, false)

    constructor(
        number: Int,
        primaryTextDisplay: LocalizedText,
        secondaryTextDisplay: LocalizedText,
        primaryCompletionDisplay: Int,
        secondaryCompletionDisplay: Int
    ) : this(
        number,
        primaryTextDisplay,
        secondaryTextDisplay,
        primaryCompletionDisplay,
        secondaryCompletionDisplay,
        true,
        false
    )

    companion object {
        @JvmField val INITIALIZE_JS5 = LoadingStage(0, LocalizedText.CHECKING_FOR_UPDATES, 2)
        @JvmField val LOAD_TIPS = LoadingStage(1, LocalizedText.CHECKING_FOR_UPDATES, LocalizedText.CHECKING_FOR_UPDATES, 2, 3)
        @JvmField val CREATE_FONTS = LoadingStage(2, LocalizedText.CHECKING_FOR_UPDATES, 3)
        @JvmField val CACHE_FONTS = LoadingStage(3, LocalizedText.CHECKING_FOR_UPDATES, LocalizedText.CHECKING_FOR_UPDATES, 3, 4)
        @JvmField val PREPARE_FONTS = LoadingStage(4, LocalizedText.CHECKING_FOR_UPDATES, 4)
        @JvmField val CREATE_JS5 = LoadingStage(5, LocalizedText.CHECKING_FOR_UPDATES, LocalizedText.CHECKING_FOR_UPDATES, 4, 5)
        @JvmField val LOAD_SPRITES = LoadingStage(6, LocalizedText.CHECKING_FOR_UPDATES, LocalizedText.CHECKING_FOR_UPDATES, 5, 98, true, true)
        @JvmField val INITIALIZE_AUDIO = LoadingStage(7, LocalizedText.CHECKING_FOR_UPDATES, 99)
        @JvmField val PREPARE_NATIVE_LIBRARY_LOADER = LoadingStage(8, LocalizedText.CHECKING_FOR_UPDATES, 100)
        @JvmField val LOAD_NATIVES = LoadingStage(9, LocalizedText.FETCHING_UPDATES, LocalizedText.FETCHING_UPDATES, 0, 92, true, true)
        @JvmField val LOAD_TYPELISTS = LoadingStage(10, LocalizedText.FETCHING_UPDATES, LocalizedText.FETCHING_UPDATES, 92, 93)
        @JvmField val CACHE_STATIC_MEDIA = LoadingStage(11, LocalizedText.FETCHING_UPDATES, LocalizedText.FETCHING_UPDATES, 94, 95)
        @JvmField val INITIALIZE_WORLDMAP = LoadingStage(12, LocalizedText.FETCHING_UPDATES, LocalizedText.FETCHING_UPDATES, 96, 97)
        @JvmField val DISCARD_UNPACKED = LoadingStage(13, LocalizedText.FETCHING_UPDATES, 97)
        @JvmField val DISPLAY_LOGIN_WINDOW = LoadingStage(14, LocalizedText.FETCHING_UPDATES, 97)
        @JvmField val INITIALIZE_PLAYER = LoadingStage(15, LocalizedText.FETCHING_UPDATES, 100)
        @JvmField val PREPARE_TOOLKIT = LoadingStage(16, LocalizedText.FETCHING_UPDATES, 100)
        @JvmField val PREPARE_PLAYER = LoadingStage(17, LocalizedText.FETCHING_UPDATES, 100)

        @JvmField var LOADING_STAGES: Array<LoadingStage>? = null
        @JvmField var displayedLoadingPercentage: Int = 0
        @JvmField var LOADING_TEXT: String? = null
        @JvmField var gametipsToDisplay: Array<LoginScreenComponent>? = null
        @JvmField var lastDisplayedGametip: Int = -1
        @JvmField var loginScreenRendererThread: Thread? = null
        @JvmField var LOADING_STAGE_CHANGED: Long = 0
        @JvmField var LOADING_STAGE: LoadingStage? = null
        @JvmField var plainLoginScreenRenderer: AssetLoadingScreenRenderer? = null

        @JvmField var bannerOverride: String? = null

        @JvmStatic
        fun bannerFor(stage: LoadingStage, secondary: Boolean = false): String =
            bannerOverride ?: (if (secondary) stage.secondaryTextDisplay else stage.primaryTextDisplay).translate(Client.CURRENT_LANGUAGE!!)

        @JvmStatic
        fun restart(banner: String) {
            plainLoginScreenRenderer?.stop()
            plainLoginScreenRenderer = null
            loginScreenRendererThread = null
            gametipsToDisplay = null
            lastDisplayedGametip = -1
            displayedLoadingPercentage = 0
            bannerOverride = banner
            LOADING_TEXT = banner + "0%"
            LOADING_STAGES = getValues()
            LOADING_STAGE = LOADING_STAGES!![0]
            LOADING_STAGE_CHANGED = Utils.time()
        }

        @JvmStatic
        fun initialize(requestBackgroundFillrect: Boolean) {
            if (plainLoginScreenRenderer == null) {
                initialize()
            }
            if (requestBackgroundFillrect) {
                plainLoginScreenRenderer?.requestFillrect()
            }
        }

        @JvmStatic
        fun initialize() {
            LOADING_STAGE?.let { stage ->
                loginScreenRendererThread?.let { previous ->
                    if (previous.isAlive && previous !== Thread.currentThread()) {
                        plainLoginScreenRenderer?.stop()
                        try {
                            previous.join(2000L)
                        } catch (_: InterruptedException) {
                        }
                        if (previous.isAlive) {
                            println("[LoadingStage] WARNING: previous login-screen renderer thread did not stop; abandoning it")
                        }
                    }
                }
                plainLoginScreenRenderer = AssetLoadingScreenRenderer()
                plainLoginScreenRenderer!!.initialize(
                    LOADING_STAGE_CHANGED,
                    bannerFor(stage),
                    stage.primaryCompletionDisplay,
                    stage
                )
                loginScreenRendererThread = Thread(plainLoginScreenRenderer, "")
                loginScreenRendererThread!!.start()
            }
        }

        @JvmStatic
        fun cycle() {
            if (LOADING_STAGES == null) {
                LOADING_STAGES = getValues()
                LOADING_STAGE = LOADING_STAGES!![0]
                LOADING_STAGE_CHANGED = Utils.time()
            }
            if (plainLoginScreenRenderer == null && !CacheSwitch.inProgress) {
                initialize()
            }

            val loadingStage = LOADING_STAGE
            val loadingPercent = ClientStartup.initialize()
            LoadTimer.stage(LOADING_STAGE!!.number)

            if (loadingStage === LOADING_STAGE) {
                LOADING_TEXT = bannerFor(LOADING_STAGE!!)
                if (LOADING_STAGE!!.hasProgressTracking) {
                    displayedLoadingPercentage = loadingPercent * (LOADING_STAGE!!.secondaryCompletionDisplay - LOADING_STAGE!!.primaryCompletionDisplay) / 100 + LOADING_STAGE!!.primaryCompletionDisplay
                }
                if (LOADING_STAGE!!.showPercentage) {
                    LOADING_TEXT = "$LOADING_TEXT$displayedLoadingPercentage%"
                }
            } else if (LOADING_STAGE === PREPARE_PLAYER) {
                plainLoginScreenRenderer?.stop()
                plainLoginScreenRenderer = null
                bannerOverride = null
                if (!CacheSwitch.finish()) {
                    Client.setGameState(GameState.UNK_5)
                }
            } else {
                LOADING_TEXT = bannerFor(loadingStage!!, secondary = true)
                if (LOADING_STAGE!!.showPercentage) {
                    LOADING_TEXT = "$LOADING_TEXT${loadingStage.secondaryCompletionDisplay}%"
                }
                displayedLoadingPercentage = loadingStage.secondaryCompletionDisplay
                if (LOADING_STAGE!!.hasProgressTracking || loadingStage.hasProgressTracking) {
                    LOADING_STAGE_CHANGED = Utils.time()
                }
            }

            plainLoginScreenRenderer?.let { renderer ->
                renderer.initialize(LOADING_STAGE_CHANGED, LOADING_TEXT, displayedLoadingPercentage, LOADING_STAGE)

                gametipsToDisplay?.let { tips ->
                    for (index in (lastDisplayedGametip + 1) until tips.size) {
                        if (tips[index].calculateLoadingPercent() >= 100 &&
                            index - 1 == lastDisplayedGametip &&
                            Client.gameState != GameState.UNK_4 &&
                            renderer.replaceComponent()) {

                            try {
                                tips[index].initialize()
                            } catch (exception: Exception) {
                                gametipsToDisplay = null
                                break
                            }

                            renderer.setDisplayedGametip(tips[index])
                            if (++lastDisplayedGametip >= tips.size - 1 && tips.size > 1) {
                                lastDisplayedGametip = if (TypeLists.GAME_TIPS!!.hasTipsFile()) 0 else -1
                            }
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun killLoginscreenThread() {
            plainLoginScreenRenderer?.stop()

            loginScreenRendererThread?.let { thread ->
                if (thread === Thread.currentThread()) {
                    println("[LoadingStage] WARNING: killLoginscreenThread called from the login-screen thread itself; skipping self-join")
                    return
                }
                val unstoppable = plainLoginScreenRenderer == null
                var waited = 0L
                while (thread.isAlive) {
                    try {
                        thread.join(500L)
                    } catch (_: InterruptedException) {
                        continue
                    }
                    waited += 500L
                    if (thread.isAlive && unstoppable && waited >= 2000L) {
                        println("[LoadingStage] WARNING: orphaned login-screen renderer thread cannot be stopped (renderer ref lost); abandoning join to avoid deadlock")
                        break
                    }
                }
                if (!thread.isAlive) {
                    loginScreenRendererThread = null
                    plainLoginScreenRenderer = null
                }
            }
        }

        @JvmStatic
        fun getValues(): Array<LoadingStage> {
            return arrayOf(
                INITIALIZE_JS5,
                LOAD_TIPS,
                CREATE_FONTS,
                CACHE_FONTS,
                PREPARE_FONTS,
                CREATE_JS5,
                LOAD_SPRITES,
                INITIALIZE_AUDIO,
                PREPARE_NATIVE_LIBRARY_LOADER,
                LOAD_NATIVES,
                LOAD_TYPELISTS,
                CACHE_STATIC_MEDIA,
                INITIALIZE_WORLDMAP,
                DISCARD_UNPACKED,
                DISPLAY_LOGIN_WINDOW,
                INITIALIZE_PLAYER,
                PREPARE_TOOLKIT,
                PREPARE_PLAYER
            )
        }
    }
}
