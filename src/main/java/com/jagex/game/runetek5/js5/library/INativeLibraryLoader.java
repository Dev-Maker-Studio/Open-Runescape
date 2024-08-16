package com.jagex.game.runetek5.js5.library;

public interface INativeLibraryLoader {

    boolean isJagdxLoaded();

    boolean shutdown();
    
    boolean loadIntoMemory(String libraryName);

}
