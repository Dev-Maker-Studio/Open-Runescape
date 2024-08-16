package com.jagex.game.runetek5.js5.library;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.systeminfo.OperatingSystemInfo;
import com.jagex.game.runetek5.tools.LibraryExecutor;
import com.jagex.game.runetek5.utils.FileOperations;

public class NativeLibraryLoader implements INativeLibraryLoader {

    public Hashtable<String, Class<LibraryExecutor>> loadedLibraryExecutor = new Hashtable<String, Class<LibraryExecutor>>();
    public final Hashtable<String, File> loadedLibraryFiles = new Hashtable<>();
    public final Js5ResourceProvider nativeDllResource;
    public final String OS_AND_ARCHITECTURE;
    
	public NativeLibraryLoader(Js5ResourceProvider nativeDlls) {
        this.nativeDllResource = nativeDlls;
        String nativeLibraryPath = "";
        if (OperatingSystemInfo.OS_NAME.startsWith("win") || OperatingSystemInfo.OS_NAME.startsWith("windows 7")) {
            nativeLibraryPath += "windows/";
        } else if (OperatingSystemInfo.OS_NAME.startsWith("linux")) {
            nativeLibraryPath += "linux/";
        } else if (OperatingSystemInfo.OS_NAME.startsWith("mac")) {
            nativeLibraryPath += "macos/";
        }
        if (OperatingSystemInfo.OS_ARCHITECTURE.startsWith("amd64") || OperatingSystemInfo.OS_ARCHITECTURE.startsWith("x86_64")) {
            nativeLibraryPath += "x86_64/";
        } else if (OperatingSystemInfo.OS_ARCHITECTURE.startsWith("i386") || OperatingSystemInfo.OS_ARCHITECTURE.startsWith("i486") || OperatingSystemInfo.OS_ARCHITECTURE.startsWith("i586") || OperatingSystemInfo.OS_ARCHITECTURE.startsWith("x86")) {
            nativeLibraryPath += "x86/";
        } else if (OperatingSystemInfo.OS_ARCHITECTURE.startsWith("ppc")) {
            nativeLibraryPath += "ppc/";
        } else {
            nativeLibraryPath += "universal/";
        }
        this.OS_AND_ARCHITECTURE = nativeLibraryPath;
    }

    public void addLibrary(String libraryName, File libraryFile) {
        this.loadedLibraryFiles.put(libraryName, libraryFile);
    }

    public boolean isJagtheoraLoaded() {
        return this.loadedLibraryExecutor.containsKey("jagtheora");
    }


    @Override
    public boolean shutdown() {
        Hashtable<String, Class<LibraryExecutor>> activeExecutors = new Hashtable<>();
        Enumeration<String> currentlyLoaded = this.loadedLibraryExecutor.keys();
        while (currentlyLoaded.hasMoreElements()) {
            String loadedLibraryName = currentlyLoaded.nextElement();
            activeExecutors.put(loadedLibraryName, this.loadedLibraryExecutor.get(loadedLibraryName));
        }
        try {
            Class<?> accessibleObject = Class.forName("java.lang.reflect.AccessibleObject");
            Class<?> classloader = Class.forName("java.lang.ClassLoader");
            Field nativeLibraries = classloader.getDeclaredField("nativeLibraries");
            Method setAccessible = accessibleObject.getDeclaredMethod("setAccessible", Boolean.TYPE);
            setAccessible.invoke(nativeLibraries, Boolean.TRUE);
            try {
                currentlyLoaded = this.loadedLibraryExecutor.keys();
                while (currentlyLoaded.hasMoreElements()) {
                    String nextLibraryName = currentlyLoaded.nextElement();
                    try {
                        File nextLibraryFile = this.loadedLibraryFiles.get(nextLibraryName);
                        Class<LibraryExecutor> nextLibraryExecutor = this.loadedLibraryExecutor.get(nextLibraryName);
                        Vector<Object> vector = (Vector<Object>) nativeLibraries.get(nextLibraryExecutor.getClassLoader());
                        for (int elementIndex = 0; elementIndex < vector.size(); elementIndex++) {
                            try {
                                Object loadedElement = vector.elementAt(elementIndex);
                                Field nameField = loadedElement.getClass().getDeclaredField("name");
                                setAccessible.invoke(nameField, Boolean.TRUE);
                                try {
                                    String loadedElementName = (String) nameField.get(loadedElement);
                                    if (loadedElementName != null && loadedElementName.equalsIgnoreCase(nextLibraryFile.getCanonicalPath())) {
                                        Field handleField = loadedElement.getClass().getDeclaredField("handle");
                                        Method finalizeMethod = loadedElement.getClass().getDeclaredMethod("finalize");
                                        setAccessible.invoke(handleField, Boolean.TRUE);
                                        setAccessible.invoke(finalizeMethod, Boolean.TRUE);
                                        try {
                                            finalizeMethod.invoke(loadedElement);
                                            handleField.set(loadedElement, Integer.valueOf(0));
                                            activeExecutors.remove(nextLibraryName);
                                        } catch (Throwable throwable) {
                                        	throwable.printStackTrace();
                                        }
                                        setAccessible.invoke(finalizeMethod, Boolean.FALSE);
                                        setAccessible.invoke(handleField, Boolean.FALSE);
                                    }
                                } catch (Throwable throwable) {
                                	throwable.printStackTrace();
                                }
                                setAccessible.invoke(nameField, Boolean.FALSE);
                            } catch (Throwable ignored) {
                            	ignored.printStackTrace();
                            }
                        }
                    } catch (Throwable ignored) {
                    	ignored.printStackTrace();
                    }
                }
            } catch (Throwable ignored) {
            	ignored.printStackTrace();
            }
            setAccessible.invoke(nativeLibraries, Boolean.FALSE);
        } catch (Throwable ignored) {
        	ignored.printStackTrace();
        }
        this.loadedLibraryExecutor = activeExecutors;
        return this.loadedLibraryExecutor.isEmpty();
    }

    @Override
    public boolean isJagdxLoaded() {
        return this.loadedLibraryFiles.containsKey("jagdx");
    }

    public boolean loadIntoMemory(String libraryName) {
        Class<LibraryExecutor> executor = this.loadedLibraryExecutor.get(libraryName);
        if (executor != null) {
            return executor.getClassLoader() == LibraryExecutor.class.getClassLoader();
        } 
        File libraryFile = this.loadedLibraryFiles.get(libraryName);
        if (libraryFile != null) {
            try {
                libraryFile = new File(libraryFile.getCanonicalPath());
                System.load(libraryFile.getPath());
                this.loadedLibraryExecutor.put(libraryName, LibraryExecutor.class);
                return true;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        return false;
    }
    
    public int getLibraryLoadingPercent(String libraryName) {
        if (this.loadedLibraryFiles.containsKey(libraryName)) {
            return 100;
        }
        String nativeLocation = NativeLibraryLoader.getCompatibleDll(libraryName);
        if (nativeLocation == null) {
            return -1;
        } 
        String fullLibraryPath = this.OS_AND_ARCHITECTURE + nativeLocation;
        if (!this.nativeDllResource.namedFileExists(fullLibraryPath, "")) {
            return -1;
        }
        if (!this.nativeDllResource.isCached(fullLibraryPath)) {
            return this.nativeDllResource.getArchiveLoadingPercent(fullLibraryPath);
        } 
    	/** Check if the file inside the cache is the same as the old unpacked one, if not replace **/
        byte[] nativeFileData = this.nativeDllResource.getNamedFile(fullLibraryPath, "");
        File outputFile = FileOperations.loadFromCacheDir(nativeLocation);
        if (nativeFileData != null) {
            boolean filesMatch = true;
            byte[] fileData = FileOperations.getDataFromFile(outputFile);
            if (fileData != null && fileData.length == nativeFileData.length) {
                for (int index = 0; index < fileData.length; index++) {
                    if (fileData[index] != nativeFileData[index]) {
                        filesMatch = false;
                        break;
                    }
                }
            } else {
                filesMatch = false;
            }
            try {
                if (!filesMatch) {
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                        fileOutputStream.write(nativeFileData, 0, nativeFileData.length);
                        fileOutputStream.close();
                    } catch (IOException ioe) {
                        throw new RuntimeException();
                    }
                }
            } catch (Throwable throwable) {
            	throwable.printStackTrace();
                return -1;
            }
            addLibrary(libraryName, outputFile);
            return 100;
        } 
        return -1;
    }

	public static String getCompatibleDll(String dllName) {
        if (OperatingSystemInfo.OS_NAME.startsWith("win"))
            return dllName + ".dll";
        if (OperatingSystemInfo.OS_NAME.startsWith("linux"))
            return "lib" + dllName + ".so";
        if (OperatingSystemInfo.OS_NAME.startsWith("mac"))
            return "lib" + dllName + ".dylib";
        return null;
    }
}
