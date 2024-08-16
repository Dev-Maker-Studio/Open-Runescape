package com.jagex.game.runetek5.js5.dependency;

import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class CacheableHuffman implements IDependencyLoader {

    public final Js5ResourceProvider resourceProvider;

    public final String fileName;

    public CacheableHuffman(Js5ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
        this.fileName = "huffman";
    }

    @Override
    public int getCompletion() {
        return this.resourceProvider.isNamedCached(this.fileName) ? 100 : 0;
    }

    @Override
    public DependencyType getDependencyType() {
        return DependencyType.ENCODING;
    }
}
