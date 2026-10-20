package com.jagex.game.runetek5.utils

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.window.WindowMode
import java.awt.Container
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

class DirectoryPicker(
    @JvmField val diaglogTitle: String
) : Runnable {

    @JvmField var pickedDirectory: File? = null
    @JvmField var fileChoserOpen: Boolean = false
    @JvmField val ready: Boolean = true

    init {
        Thread(this).start()
    }

    override fun run() {
        var container: Container? = WindowMode.getActiveContainer()
        var mainFrame: Frame? = null
        if (GameShell.engineFrame != null) {
            mainFrame = GameShell.engineFrame
        } else {
            while (container != null) {
                if (container is Frame) {
                    mainFrame = container
                    break
                }
                container = container.parent
            }
        }
        if (mainFrame == null) {
            throw RuntimeException("")
        }
        val fileChoser = JFileChooser("")
        fileChoser.dialogTitle = diaglogTitle
        fileChoser.fileFilter = DirectoryFileFilter(this, this)
        fileChoser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fileChoser.isAcceptAllFileFilterUsed = false
        val state = fileChoser.showOpenDialog(mainFrame)
        if (state == 0) {
            pickedDirectory = fileChoser.selectedFile
        }
        fileChoserOpen = true
    }

    fun fileChoserOpen(): Boolean {
        return fileChoserOpen
    }

    fun ready(): Boolean {
        return ready
    }

    fun getPickedDirectory(): File? {
        return pickedDirectory
    }
}
