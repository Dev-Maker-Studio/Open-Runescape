package com.jagex.game.runetek5.js5.library

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Hashtable

import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.systeminfo.OperatingSystemInfo
import com.jagex.game.runetek5.tools.LibraryExecutor
import com.jagex.game.runetek5.utils.FileOperations

class NativeLibraryLoader(@JvmField val nativeDllResource: Js5ResourceProvider) : INativeLibraryLoader {

    @JvmField var loadedLibraryExecutor = Hashtable<String, Class<LibraryExecutor>>()
    @JvmField val loadedLibraryFiles = Hashtable<String, File>()
    @JvmField val OS_AND_ARCHITECTURE: String

    init {
        var nativeLibraryPath = ""
        if (OperatingSystemInfo.OS_NAME.startsWith("win") || OperatingSystemInfo.OS_NAME.startsWith("windows 7")) {
            nativeLibraryPath += "windows/"
        } else if (OperatingSystemInfo.OS_NAME.startsWith("linux")) {
            nativeLibraryPath += "linux/"
        } else if (OperatingSystemInfo.OS_NAME.startsWith("mac")) {
            nativeLibraryPath += "macos/"
        }
        if (OperatingSystemInfo.OS_ARCHITECTURE.startsWith("amd64") || OperatingSystemInfo.OS_ARCHITECTURE.startsWith("x86_64")) {
            nativeLibraryPath += "x86_64/"
        } else if (OperatingSystemInfo.OS_ARCHITECTURE.startsWith("i386") || OperatingSystemInfo.OS_ARCHITECTURE.startsWith("i486") || OperatingSystemInfo.OS_ARCHITECTURE.startsWith("i586") || OperatingSystemInfo.OS_ARCHITECTURE.startsWith("x86")) {
            nativeLibraryPath += "x86/"
        } else if (OperatingSystemInfo.OS_ARCHITECTURE.startsWith("ppc")) {
            nativeLibraryPath += "ppc/"
        } else {
            nativeLibraryPath += "universal/"
        }
        OS_AND_ARCHITECTURE = nativeLibraryPath
    }

    fun addLibrary(libraryName: String, libraryFile: File) {
        loadedLibraryFiles[libraryName] = libraryFile
    }

    fun isJagtheoraLoaded(): Boolean {
        return loadedLibraryExecutor.containsKey("jagtheora")
    }

    override fun shutdown(): Boolean {
        loadedLibraryExecutor.clear()
        loadedLibraryFiles.clear()
        return loadedLibraryExecutor.isEmpty()
    }

    override fun loadIntoMemory(libraryName: String): Boolean {
        val executor = loadedLibraryExecutor[libraryName]
        if (executor != null) {
            return executor.classLoader == LibraryExecutor::class.java.classLoader
        }
        val libraryFile = loadedLibraryFiles[libraryName]
        if (libraryFile != null) {
            try {
                val canonicalFile = File(libraryFile.canonicalPath)
                System.load(canonicalFile.path)
                loadedLibraryExecutor[libraryName] = LibraryExecutor::class.java
                return true
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
        return false
    }

    fun getLibraryLoadingPercent(libraryName: String): Int {
        if (loadedLibraryFiles.containsKey(libraryName)) {
            return 100
        }
        val nativeLocation = getCompatibleDll(libraryName)
            ?: return -1
        val fullLibraryPath = OS_AND_ARCHITECTURE + nativeLocation
        if (!nativeDllResource.namedFileExists(fullLibraryPath, "")) {
            return -1
        }
        if (!nativeDllResource.isCached(fullLibraryPath)) {
            return nativeDllResource.getArchiveLoadingPercent(fullLibraryPath)
        }
        val nativeFileData = nativeDllResource.getNamedFile(fullLibraryPath, "")
        val outputFile = FileOperations.loadFromCacheDir(nativeLocation)
        if (nativeFileData != null) {
            var filesMatch = true
            val fileData = FileOperations.getDataFromFile(outputFile)
            if (fileData != null && fileData.size == nativeFileData.size) {
                for (index in fileData.indices) {
                    if (fileData[index] != nativeFileData[index]) {
                        filesMatch = false
                        break
                    }
                }
            } else {
                filesMatch = false
            }
            try {
                if (!filesMatch) {
                    try {
                        val fileOutputStream = FileOutputStream(outputFile)
                        fileOutputStream.write(nativeFileData, 0, nativeFileData.size)
                        fileOutputStream.close()
                    } catch (ioe: IOException) {
                        throw RuntimeException()
                    }
                }
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                return -1
            }
            addLibrary(libraryName, outputFile)
            return 100
        }
        return -1
    }

    companion object {
        @JvmStatic
        fun getCompatibleDll(dllName: String): String? {
            if (OperatingSystemInfo.OS_NAME.startsWith("win"))
                return "$dllName.dll"
            if (OperatingSystemInfo.OS_NAME.startsWith("linux"))
                return "lib$dllName.so"
            if (OperatingSystemInfo.OS_NAME.startsWith("mac"))
                return "lib$dllName.dylib"
            return null
        }
    }
}
