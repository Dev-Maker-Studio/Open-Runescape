package com.jagex.game.runetek5.utils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Hashtable;

public class FileOperations {
	
	public static final Hashtable<String, File> cachedFileTable = new Hashtable<>(16);
	public static File currentPreferenceFile;
	public static boolean preferenceFilePresent;

	public static void setPreferenceFile(File file) {
	    FileOperations.currentPreferenceFile = file;
	    if (!FileOperations.currentPreferenceFile.exists()) {
	        throw new RuntimeException("");
	    } else {
	        FileOperations.preferenceFilePresent = true;
	    }
	}

	public static File loadFromCacheDir(String location) {
	    if (!FileOperations.preferenceFilePresent) {
	        throw new RuntimeException("");
	    } 
	    File cachedFile = cachedFileTable.get(location);
	    if (cachedFile != null) {
	        return cachedFile;
	    } 
	    File childFile = new File(FileOperations.currentPreferenceFile, location);
	    RandomAccessFile randomAccessFile = null;
	    try {
	        File parentFile = new File(childFile.getParent());
	        if (!parentFile.exists()) {
	            throw new RuntimeException("");
	        } else {
	            randomAccessFile = new RandomAccessFile(childFile, "rw");
	            int nextByte = randomAccessFile.read();
	            randomAccessFile.seek(0L);
	            randomAccessFile.write(nextByte);
	            randomAccessFile.seek(0L);
	            randomAccessFile.close();
	            cachedFileTable.put(location, childFile);
	            return childFile;
	        }
	    } catch (Exception exception) {
	    	exception.printStackTrace();
	        try {
	            if (randomAccessFile != null) {
	                randomAccessFile.close();
	            }
	        } catch (Exception ex) {
	        	ex.printStackTrace();
	        }
	        throw new RuntimeException();
	    }
	}

	public static byte[] getDataFromFile(File file, int fileSize) {
	    try {
	        byte[] fileData = new byte[fileSize];
	        FileOperations.decodeFile(file, fileData, fileSize);
	        return fileData;
	    } catch (IOException ioe) {
	    	ioe.printStackTrace();
	        return null;
	    }
	}

	public static byte[] getDataFromFile(File file) {
	    return getDataFromFile(file, (int) file.length());
	}

	public static void decodeFile(File file, byte[] readInto, int fileSize) throws IOException {
	    DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
	    try {
	        dataInputStream.readFully(readInto, 0, fileSize);
	    } catch (EOFException eof) {
	    	eof.printStackTrace();
	    }
	    dataInputStream.close();
	}
}
