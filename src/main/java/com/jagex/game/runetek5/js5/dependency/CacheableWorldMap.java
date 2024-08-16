package com.jagex.game.runetek5.js5.dependency;

import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class CacheableWorldMap implements IDependencyLoader {

    public final Js5ResourceProvider resourceProvider;

    public final String archiveName;

    public CacheableWorldMap(Js5ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
        this.archiveName = "details";
    }

    @Override
    public DependencyType getDependencyType() {
        return DependencyType.LABELS;
    }

    @Override
    public int getCompletion() {
        return this.resourceProvider.isCached(this.archiveName) ? 100 : this.resourceProvider.getArchiveLoadingPercent(this.archiveName);
    }
}
