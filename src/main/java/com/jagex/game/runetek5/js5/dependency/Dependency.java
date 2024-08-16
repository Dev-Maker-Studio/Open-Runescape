package com.jagex.game.runetek5.js5.dependency;

public class Dependency {
	
    public IDependencyLoader dependencyLoader;
    public int archiveCount;
    public DependencyType dependencyType;

    public Dependency(DependencyType dependencyType) {
        this.dependencyType = dependencyType;
        this.archiveCount = 1;
    }

    public int getArchiveCount() {
        return this.archiveCount;
    }

    public void inializeDependency(IDependencyLoader dependencyLoader) {
        if (dependencyLoader.getDependencyType() != this.dependencyType) {
            throw new IllegalArgumentException();
        } 
        this.dependencyLoader = dependencyLoader;
    }

    public void setArchiveCount(int archiveCount) {
        this.archiveCount = archiveCount;
    }

    public IDependencyLoader getDependency() {
        return this.dependencyLoader;
    }
}