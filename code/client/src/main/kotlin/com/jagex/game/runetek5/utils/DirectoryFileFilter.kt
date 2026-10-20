package com.jagex.game.runetek5.utils

import javax.swing.filechooser.FileFilter
import java.io.File

class DirectoryFileFilter(
    @JvmField val dummy: DirectoryPicker,
    @JvmField val filePicker: DirectoryPicker
) : FileFilter() {

    override fun accept(pickedFile: File): Boolean {
        return filePicker.ready() && pickedFile.isDirectory || !filePicker.ready()
    }

    override fun getDescription(): String? {
        return null
    }
}
