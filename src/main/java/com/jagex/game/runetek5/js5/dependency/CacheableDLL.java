package com.jagex.game.runetek5.js5.dependency;

import com.jagex.game.runetek5.config.Resource;

public class CacheableDLL implements IDependencyLoader {

    public boolean doneLoading;

    public final String nativeName;

    public CacheableDLL(String nativeName) {
        this.nativeName = nativeName;
    }

    @Override
    public DependencyType getDependencyType() {
        return DependencyType.NATIVE;
    }

    public boolean isReady() {
        return this.doneLoading;
    }

    @Override
    public int getCompletion() {
        if (this.doneLoading) {
            return 100;
        } 
        int loadPercentage = Resource.NATIVE_LIBRARY_LOADER.getLibraryLoadingPercent(this.nativeName);
        if (loadPercentage >= 0 && loadPercentage <= 100) {
            return loadPercentage;
        } 
        this.doneLoading = true;
        return 100;
    }
}
