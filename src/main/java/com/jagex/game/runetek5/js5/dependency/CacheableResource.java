package com.jagex.game.runetek5.js5.dependency;

import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class CacheableResource implements IDependencyLoader {

    public final Js5ResourceProvider resourceProvider;

    public CacheableResource(Js5ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    @Override
    public DependencyType getDependencyType() {
        return DependencyType.CONFIG;
    }

    @Override
    public int getCompletion() {
        return this.resourceProvider.fullyLoaded() ? 100 : this.resourceProvider.getLoadingPercentage();
    }
}
