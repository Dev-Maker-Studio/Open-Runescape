package com.jagex.game.runetek5.utils

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.Hashtable

object FileOperations {

    @JvmField val cachedFileTable: Hashtable<String, File> = Hashtable(16)
    @JvmField var currentPreferenceFile: File? = null
    @JvmField var preferenceFilePresent: Boolean = false

    @JvmStatic
    fun setPreferenceFile(file: File) {
        currentPreferenceFile = file
        if (!currentPreferenceFile!!.exists()) {
            throw RuntimeException("")
        } else {
            preferenceFilePresent = true
        }
    }

    @JvmStatic
    fun loadFromCacheDir(location: String): File {
        if (!preferenceFilePresent) {
            throw RuntimeException("")
        }
        val cachedFile = cachedFileTable[location]
        if (cachedFile != null) {
            return cachedFile
        }
        val childFile = File(currentPreferenceFile, location)
        var randomAccessFile: RandomAccessFile? = null
        try {
            val parentFile = File(childFile.parent)
            if (!parentFile.exists()) {
                throw RuntimeException("")
            } else {
                randomAccessFile = RandomAccessFile(childFile, "rw")
                val nextByte = randomAccessFile.read()
                randomAccessFile.seek(0L)
                randomAccessFile.write(nextByte)
                randomAccessFile.seek(0L)
                randomAccessFile.close()
                cachedFileTable[location] = childFile
                return childFile
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            try {
                if (randomAccessFile != null) {
                    randomAccessFile.close()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun getDataFromFile(file: File, fileSize: Int): ByteArray? {
        try {
            val fileData = ByteArray(fileSize)
            decodeFile(file, fileData, fileSize)
            return fileData
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            return null
        }
    }

    @JvmStatic
    fun getDataFromFile(file: File): ByteArray? {
        return getDataFromFile(file, file.length().toInt())
    }

    @JvmStatic
    @Throws(IOException::class)
    fun decodeFile(file: File, readInto: ByteArray, fileSize: Int) {
        val dataInputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))
        try {
            dataInputStream.readFully(readInto, 0, fileSize)
        } catch (eof: EOFException) {
            eof.printStackTrace()
        }
        dataInputStream.close()
    }
}
