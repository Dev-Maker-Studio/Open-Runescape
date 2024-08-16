package com.jagex.game.runetek5.sound.jagtheora;

public class MonoSterioAudioReference implements Comparable {

    public long refreshTime;
    public long ticker;
    public MonoSterioData data;

    public final Long key;

    public MonoSterioAudioReference(MonoSterioData dataNode, Long key) {
    	this.data = dataNode;
        this.key = key;
    }

    public boolean equals(Object object) {
        if (object instanceof MonoSterioAudioReference) {
            return this.key.equals(((MonoSterioAudioReference) object).key);
        } 
        throw new IllegalArgumentException();
    }

    @Override
    public int compareTo(Object object) {
        return compare((MonoSterioAudioReference) object);
    }

    public int compare(MonoSterioAudioReference toCompare) {
        return this.ticker < toCompare.ticker ? -1 : (this.ticker > toCompare.ticker ? 1 : 0);
    }
}
