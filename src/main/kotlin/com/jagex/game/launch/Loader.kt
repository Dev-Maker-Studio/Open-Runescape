package com.jagex.game.launch

import com.darkan.bot.Bot
import com.darkan.bot.UI
import com.darkan.bot.initializeUI
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.network.world.WorldDescriptor
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Panel
import java.io.File
import java.math.BigInteger
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import javax.swing.*

class Loader : Panel() {

    var clientFrame: JFrame? = null

    fun doFrame() {
        INSTANCE = this
        openFrame()
        startClient()
        clientFrame?.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }

    fun getCodeBase(): URL? {
        return try {
            URL("http://$IP_ADDRESS")
        } catch (e: MalformedURLException) {
            println("Invalid ip address")
            null
        }
    }

    fun getDocumentBase(): URL? {
        return try {
            URL("http://$IP_ADDRESS")
        } catch (e: MalformedURLException) {
            println("Invalid ip address")
            null
        }
    }

    fun getParameter(arg0: String): String? {
        return clientParams[arg0] as? String
    }

    fun openFrame() {
        clientFrame = JFrame("Darkan Client with Bot").apply {
            layout = BorderLayout()

            // Create client panel for the game
            val clientPanel = JPanel().apply {
                layout = BorderLayout()
                add(this@Loader)
                preferredSize = Dimension(774, 588)
            }

            if (Bot.ENABLED) {
                // Initialize bot UI
                initializeUI()
                val botPanel = UI

                // Create split pane with game on left, bot controls on right
                val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientPanel, botPanel).apply {
                    resizeWeight = 0.6 // Give more space to the game
                    dividerLocation = 774 // Initial divider position
                }

                contentPane.add(splitPane, BorderLayout.CENTER)
            } else {
                // Just add the game client without bot UI
                contentPane.add(clientPanel, BorderLayout.CENTER)
            }

            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            pack()
            isVisible = true
        }
    }

    fun showError(string: String) {
        JOptionPane.showMessageDialog(this, string)
    }

    fun startClient() {
        System.setProperty("sun.java2d.uiScale", "1.0")
        System.setProperty("sun.java2d.win.uiScaleX", "1.0")
        System.setProperty("sun.java2d.win.uiScaleY", "1.0")
        System.setProperty("sun.java2d.uiScale.enabled", "false")
        loadParams()
        val clnt = client()
        clnt.supplyApplet(this)
        clnt.init()
        clnt.start()
    }

    companion object {
        const val HOME_DIR = ".darkanrs"
        @JvmField
        val CACHE_DIR = HOME_DIR + File.separatorChar + "caches"
        private const val serialVersionUID = 1L

        const val MAJOR_BUILD = 727
        const val MINOR_BUILD = 1
        const val WORLD_PORT = 43595
        @JvmField
        var LOBBY_PORT = 43594
        const val LOBBY_WORLD = 1115
        const val JS5_SOURCE_WORLD = 420
        const val DEFAULT_WORLD = 1

        @JvmField
        val RSA_PUBLIC_MODULUS = BigInteger(
            "117525752735533423040644219776209926525585489242340044375332234679786347045466594509203355398209678968096551043842518449703703964361320462967286756268851663407950384008240524570966471744081769815157355561961607944067477858512067883877129283799853947605780903005188603658779539811385137666347647991072028080201"
        )

        @JvmField
        val RSA_PUBLIC_EXPONENT = BigInteger("65537")

        const val DEBUG = true

        @JvmField
        var IP_ADDRESS: String? = null

        @JvmField
        val clientParams = Properties()

        @JvmField
        var DEBUG_IF_OPS = false

        @JvmField
        var RENDER_DISTANCE_MULTIPLIER = 2 // Default 1

        @JvmField
        var FOG_DISTANCE_MULTIPLIER = 10 // Default 1

        @JvmField
        var X10_HITS_MODE = false

        @JvmField
        var INSTANCE: Loader? = null

        /**
         * Setting to play around with removing loading screens,
         * haven't been able to make it work properly thus far
         * (crashes game after several regions loaded)
         */
        const val MULTI_THREADED_MAPLOADING = false

        @JvmField
        val MATERIAL_EFFECTS_DISABLED = BooleanArray(10)

        @JvmStatic
        fun getPort(worldId: Int): Int {
            val world = WorldDescriptor.getWorld(worldId)
            if (world != null && world.port != -1) {
                return world.port
            }
            return when (worldId) {
                LOBBY_WORLD, JS5_SOURCE_WORLD -> LOBBY_PORT
                else -> WORLD_PORT
            }
        }

        @JvmStatic
        fun loadParams() {
            clientParams.clear()
            clientParams["boxborder"] = "false"
            clientParams["boxbgcolor"] = "black"
            clientParams["separate_jvm"] = "true"
            clientParams["centerimage"] = "true"
            clientParams["java_arguments"] = "-Xmx256m -Xss2m -Dsun.java2d.noddraw=true -XX:CompileThreshold=1500 -Xincgc -XX:+UseConcMarkSweepGC -XX:+UseParNewGC"
            clientParams["image"] = "http://www.runescape.com/img/game/splash2.gif"
            clientParams["-1"] = "-Vt*kwflxoHi*o7XkGw79w"
            clientParams["11"] = "false"
            clientParams["1"] = ""
            clientParams["21"] = IP_ADDRESS
            clientParams["9"] = "true"
            clientParams["14"] = "$LOBBY_WORLD"
            clientParams["22"] = "false"
            clientParams["7"] = ""
            clientParams["12"] = ""
            clientParams["31"] = "0"
            clientParams["6"] = "0"
            clientParams["33"] = "wwGlrZHF5gKN6D3mDdihco3oPeYN2KFybL9hUUFqOvk"
            clientParams["28"] = "false"
            clientParams["10"] = "false"
            clientParams["27"] = "1451434218"
            clientParams["26"] = "637809750146160229"
            clientParams["20"] = "true,false,0,43,200,18,0,21,354,-15,Verdana,11,0xF4ECE9,candy_bar_middle.gif,candy_bar_back.gif,candy_bar_outline_left.gif,candy_bar_outline_right.gif,candy_bar_outline_top.gif,candy_bar_outline_bottom.gif,loadbar_body_left.gif,loadbar_body_right.gif,loadbar_body_fill.gif,6"
            clientParams["0"] = "-bQCRYmC-hBwK2NKrdF7iQ"
            clientParams["30"] = "38888"
            clientParams["25"] = "true"
            clientParams["16"] = "$JS5_SOURCE_WORLD"
            clientParams["32"] = ""
            clientParams["24"] = "0"
            clientParams["5"] = IP_ADDRESS
            clientParams["15"] = "0"
            clientParams["18"] = "false"
            clientParams["23"] = "0"
            clientParams["35"] = "0"
            clientParams["4"] = ""
            clientParams["13"] = "29EDD9FDC775629058FBBF106C5B0E0A3A8028FE0037D1737B8EC3EA2F4E8B8FD6F54EF2F4E65862"
            clientParams["17"] = "ev9+VAp5/tMKeNR/7MOuH6lKWS+rGkHK"
            clientParams["8"] = "0"
            clientParams["29"] = "false"
        }

        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("sun.java2d.uiScale", "1.0")
            System.setProperty("sun.java2d.win.uiScaleX", "1.0")
            System.setProperty("sun.java2d.win.uiScaleY", "1.0")
            System.setProperty("sun.java2d.uiScale.enabled", "false")

            IP_ADDRESS = if (args.isNotEmpty() && args[0] != null) {
                args[0]
            } else {
                if (Bot.PROD) "prod.darkan.org" else "localhost"
            }

            if (args.size > 1 && args[1] != null) {
                LOBBY_PORT = args[1].toInt()
            }

            Loader().doFrame()
        }
    }
}
